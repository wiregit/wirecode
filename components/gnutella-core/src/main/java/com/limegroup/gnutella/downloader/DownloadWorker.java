
package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.AssertFailure;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.io.ConnectObserver;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.statistics.DownloadStat;
import com.limegroup.gnutella.statistics.NumericalDownloadStat;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.util.DefaultThreadPool;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.Sockets;
import com.limegroup.gnutella.util.ThreadFactory;
import com.limegroup.gnutella.util.ThreadPool;

/**
 * Class that performs the logic of downloading a file from a single host.
 */
public class DownloadWorker {
    /*
      Each potential downloader downloader follows these steps:
      1. Establish a TCP connection to the host in the RFD.
         If unable to connect, end this execution.
         If able to connect, start a new Thread which executes the
         rest of the process.
      2. This step has two parts:
            a.  Grab a part of the file to download. If there is unclaimed area on
                the file grab that, otherwise try to steal claimed area from another 
                worker
            b.  Send http headers to the uploader on the tcp connection 
                established in step 1. The uploader may or may not be able to 
                upload at this time. If the uploader can't upload, it's 
                important that the leased area be restored to the state 
                they were in before we started trying. However, if the http 
                handshaking was successful, the downloader can keep the 
                part it obtained.
          The two steps above must be atomic wrt other downloaders. 
          Othersise, other downloaders in parallel will be  able to lease the 
          same areas, or try to steal the same area from the same downloader.
      3. Download the file by delegating to the HTTPDownloader, and then do 
         the book-keeping. Termination may be normal or abnormal.
          
                                          (in a new Thread)
                establishConnection   initializeAlternateLocations  
                     |               /          |             
                 [push|direct]      /        httpLoop
                     |             /            |             
                startDownload-->--/      assignAndRequest <----------\ 
                                                |                     \
                                       assignGrey/assignWhite          \
                                                |                       \
                                              doDownload                 \
                                              |         \                 |
                                              |         requestHashTree   |
                                              |          |                |
                                              \- HTTPDownloader.download  |
                                                  |                       |  
                                                  \-------->--------------/
                           

    PUSH DOWNLOADS NOTE:
      For push downloads, the acceptDownload(file, Socket, index, clientGUI) 
      method of ManagedDownloader is called from DownloadManager.
      This method needs to notify the appropriate downloader so that it can use
      the socket. 
      
      When establishConnection() realizes that it needs to do a push, it gives the
      manager its PushObserver (a ConnectObserver) and a mini-RFD.  When the manager
      is notified that a push was accepted (via acceptDownload) with that mini-RFD,
      it will notify the PushObserver using handleConnect(Socket).
      
      Note: The establishConnection method schedules a Runnable to remove the observer
      in a short amount of time (about 9 seconds).  If the observer hasn't already 
      connected, it assumes the push failed and terminates by calling shutdown().
      
      If the push was done by a multicast RFD, a failure to connect will proceed to trying
      a direct connection.  Otherwise (the push was done because no direct connect was
      possible, or because a direct connect failed), the failure of a push means that the
      download cannot proceed.
      
     CONNECTION ESTABLISHMENT NOTE:
       All connection establish, push or direct, is done via callbacks.  There is no thread
       blocking on connection establishment.  When a connection either succeeds a ConnectObserver's
       handleConnect(Socket) is called, which will ultimately attempt to start the download via
       startDownload.  If the connection attempt failed, the ConnectObserver's shutdown method
       is called and no thread is ever created.
    */
    private static final Log LOG = LogFactory.getLog(DownloadWorker.class);
    
    ///////////////////////// Policy Controls ///////////////////////////
    /** The smallest interval that can be split for parallel download */
    private static final int MIN_SPLIT_SIZE=16*1024;      //16 KB
    
    /** The lowest (cumulative) bandwith we will accept without stealing the
     * entire grey area from a downloader for a new one */
    private static final float MIN_ACCEPTABLE_SPEED = 
		DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.getValue() < 8 ? 
		0.1f:
		0.5f;
    
    /** 
     * The speed of download workers that haven't been started yet or do not 
     * have enough measurements
     */
    private static final int UNKNOWN_SPEED = -1;
    
    /** The time to wait trying to establish each normal connection, in
     *  milliseconds.*/
    private static final int NORMAL_CONNECT_TIME=10000; //10 seconds
    /** The time to wait trying to establish each push connection, in
     *  milliseconds.  This needs to be larger than the normal time. */
    private static final int PUSH_CONNECT_TIME=20000;  //20 seconds
    /** The time to wait trying to establish a push connection
     * if only a UDP push has been sent (as is in the case of altlocs) */
    private static final int UDP_PUSH_CONNECT_TIME=6000; //6 seconds
    
    /**
     * The number of seconds to wait for hosts that don't have any ranges we
     *  would be interested in.
     */
    private static final int NO_RANGES_RETRY_AFTER = 60 * 5; // 5 minutes
    
    /**
     * The number of seconds to wait for hosts that failed once.
     */
    private static final int FAILED_RETRY_AFTER = 60 * 1; // 1 minute
    
    /**
     * The number of seconds to wait for a busy host (if it didn't give us a
     * retry after header) if we don't have any active downloaders.
     *
     * Note that there are some acceptable problems with the way this
     * values are used.  Namely, if we have sources X & Y and source
     * X is tried first, but is busy, its busy-time will be set to
     * 1 minute.  Then source Y is tried and is accepted, source X
     * will still retry after 1 minute.  This 'problem' is considered
     * an acceptable issue, given the complexity of implementing
     * a method that will work under the circumstances.
     */
    public static final int RETRY_AFTER_NONE_ACTIVE = 60 * 1; // 1 minute
    
    /**
     * The minimum number of seconds to wait for a busy host if we do
     * have some active downloaders.
     *
     * Note that there are some acceptable problems with the way this
     * values are used.  Namely, if we have sources X & Y and source
     * X is tried first and is accepted.  Then source Y is tried and
     * is busy, so its busy-time is set to 10 minutes.  Then X disconnects,
     * leaving Y with 9 or so minutes left before being retried, despite
     * no other sources available.  This 'problem' is considered
     * an acceptable issue, given the complexity of implementing
     * a method that will work under the circumstances.
     */
    private static final int RETRY_AFTER_SOME_ACTIVE = 60 * 10; // 10 minutes

    private final ManagedDownloader _manager;
    private final RemoteFileDesc _rfd;
    private final VerifyingFile _commonOutFile;
    
    /**
     * Whether I was interrupted before starting
     */
    private volatile boolean _interrupted;
    
    /**
     * Reference to the stealLock all workers for a download will synchronize on
     */
    private final Object _stealLock;
    
    /**
     * The downloader that will do the actual downloading
     * TODO: un-volatilize after fixing the assertion failures
     */
    private volatile HTTPDownloader _downloader;
    
    /**
     * Whether I should release the ranges that I have leased for download
     * TODO: un-volatilize after fixing the assertion failures
     */
    private volatile boolean _shouldRelease;
    
    /**
     * The name this worker has in toString & threads.
     */
    private final String workerName;
    
    /** The observer used for direct connection establishment. */
    private DirectConnector _connectObserver;
    
    /** Lock waited on while queued. */
    private final Object Q_LOCK = new Object();
    
    DownloadWorker(ManagedDownloader manager, RemoteFileDesc rfd, VerifyingFile vf, Object lock) {
        _manager = manager;
        _rfd = rfd;
        _stealLock = lock;
        _commonOutFile = vf;
        
        // if we'll be debugging, we want to distinguish the different workers
        if (LOG.isDebugEnabled()) {
            workerName = "DownloadWorker for " + _manager.getSaveFile().getName() + " #" + System.identityHashCode(this);
        } else {
            workerName = "DownloaderWorker";
        }        
    }
    
    /**
     * Starts this DownloadWorker's connection establishment.
     */
    public void start() {
        establishConnection();
    }
    
    /**
     * Initializes the HTTPDownloader with whatever AltLocs we have discovered so far.
     * These will be cleared out after the first write.  From then on, only newly successful
     * RFDS will be sent as Alts.
     */
    private void initializeAlternateLocations() {
        int count = 0;
        for(Iterator iter = _manager.getValidAlts().iterator(); 
        iter.hasNext() && count < 10; count++) {
            AlternateLocation current = (AlternateLocation)iter.next();
            _downloader.addSuccessfulAltLoc(current);
        }
        
        count = 0;
        for(Iterator iter = _manager.getInvalidAlts().iterator(); 
        iter.hasNext() && count < 10; count++) {
            AlternateLocation current = (AlternateLocation)iter.next();
            _downloader.addFailedAltLoc(current);
        }
    }
    
    /**
     * The main loop that runs this download.
     */
    private void httpLoop() {        
        //Note: http11 is true or false depending on what we think thevalue
        //should be for rfd is at the start, before connecting. We may later
        //find that the we are wrong, in which case we update the rfd's http11
        //value. But while we are in connectAndDownload we continue to use this
        //local variable because the code is incapable of handling a change in
        //http11 status while inside connectAndDownload.
        boolean http11 = true;//must enter the loop
        
        while(http11) {
            //Step 2. OK. We have established TCP Connection. This 
            //downloader should choose a part of the file to download
            //and send the appropriate HTTP hearders
            //Note: 0=disconnected,1=tcp-connected,2=http-connected            
            ConnectionStatus status;
            http11 = _rfd.isHTTP11();
            while(true) { 
                //while queued, connect and sleep if we queued

                // request thex
                status = requestTHEXIfNeeded();
                
                // before requesting the next range,
                // consume the prior request's body
                // if there was any.
                _downloader.consumeBodyIfNecessary();
                _downloader.forgetRanges();
                
                // if we didn't get queued doing the tree request,
                // request another file.
                if (status == null || !status.isQueued()) {
                        try {
                            status = assignAndRequest(http11);
                            
                            // add any locations we may have received
                            _manager.addPossibleSources(_downloader.getLocationsReceived());
                        } finally {
                            // clear ranges did not connect
                        	try {
                        		if( status == null || !status.isConnected() )
                        			releaseRanges();
                        	} catch (AssertFailure bad) {
                        		throw new AssertFailure("status "+status+" worker failed "+getInfo()+
                        				" all workers: "+_manager.getWorkersInfo(),bad);
                        	}
                        }
                }
                
                if(status.isPartialData()) {
                    // loop again if they had partial ranges.
                    continue;
                } else if(status.isNoFile() || status.isNoData()) {
                    //if they didn't have the file or we didn't need data,
                    //break out of the loop.
                    break;
                }
                
                // must be queued or connected.
                Assert.that(status.isQueued() || status.isConnected());
                boolean addQueued = _manager.killQueuedIfNecessary(this, 
                        !status.isQueued()  ? -1 : status.getQueuePosition());
                
                // we should have been told to stay alive if we're connected
                // but it's possible that we are above our swarm capacity
                // and nothing else was queued, in which case we really should
                // kill ourselves, but there's no reason to not accept the
                // extra host.
                if(status.isConnected())
                    break;
                
                Assert.that(status.isQueued());
                // if we didn't want to stay queued
                // or we got interrupted while sleeping,
                // then try other sources
                if(!addQueued || handleQueued(status))
                    return;
            }
            
            
            //we have been given a slot remove this thread from queuedThreads
            _manager.removeQueuedWorker(this);

            switch(status.getType()) {
            case ConnectionStatus.TYPE_NO_FILE:
                // close the connection for now.            
                _downloader.stop();
                return;            
            case ConnectionStatus.TYPE_NO_DATA:
                // close the connection since we're finished.
                _downloader.stop();
                return;
            case ConnectionStatus.TYPE_CONNECTED:
                break;
            default:
                throw new IllegalStateException("illegal status: " + 
                                                status.getType());
            }

            Assert.that(status.isConnected());
            //Step 3. OK, we have successfully connected, start saving the
            // file to disk
            // If the download failed, don't keep trying to download.
            boolean downloaded = false;
            try {
                downloaded = doDownload(http11);
                if(!downloaded)
                    break;
            }finally {
                try {
                    releaseRanges();
                } catch (AssertFailure bad) {
                    throw new AssertFailure("downloaded "+downloaded+" worker failed "+getInfo()+
                            " all workers: "+_manager.getWorkersInfo(),bad);
                }
            }
        }
    }
    
    private ConnectionStatus requestTHEXIfNeeded() {
        HashTree ourTree = _commonOutFile.getHashTree();
        
        ConnectionStatus status = null;
        // request THEX from te _downloader if (the tree we have
        // isn't good enough or we don't have a tree) and another
        // worker isn't currently requesting one
        if (_downloader.hasHashTree() &&
                (ourTree == null || !ourTree.isDepthGoodEnough()) &&
                _manager.getSHA1Urn() != null) {
            
            
            synchronized(_commonOutFile) {
                if (_commonOutFile.isHashTreeRequested())
                    return status;
                _commonOutFile.setHashTreeRequested(true);
            }
            
            status = _downloader.requestHashTree(_manager.getSHA1Urn());
            _commonOutFile.setHashTreeRequested(false);
            if(status.isThexResponse()) {
                HashTree temp = status.getHashTree();
                if (temp.isBetterTree(ourTree)) {
                    _commonOutFile.setHashTree(temp);
                }
            }
        }
        return status;
    }
    
    /**
     * Release the ranges assigned to our downloader  
     */
    private void releaseRanges() {
        
        if (!_shouldRelease)
            return;
        _shouldRelease = false;
        
        // do not release if the file is complete
        if (_commonOutFile.isComplete())
            return;
        
        HTTPDownloader downloader = _downloader;
        int high, low;
        synchronized(downloader) {
        	
            // If this downloader was a thief and had to skip any ranges, do not
            // release them.
            low = downloader.getInitialReadingPoint() + downloader.getAmountRead();
            low = Math.max(low,downloader.getInitialWritingPoint());
            high = downloader.getInitialReadingPoint() + downloader.getAmountToRead()-1;
        }
        
        if( (high-low)>=0) {//dloader failed to download a part assigned to it?
            
            if (LOG.isDebugEnabled())
                LOG.debug("releasing ranges "+new Interval(low,high));
            
            _commonOutFile.releaseBlock(new Interval(low,high));
            downloader.forgetRanges();
        } else 
			LOG.debug("nothing to release!");
    }
    
    /**
     * Handles a queued downloader with the given ConnectionStatus.
     * BLOCKING (while waiting).
     *
     * @return true if we need to tell the manager to churn another
     *         connection and let this one die, false if we are
     *         going to try this connection again.
     */
    private boolean handleQueued(ConnectionStatus status) {
        // make sure that we're not in _downloaders if we're
        // sleeping/queued. this would ONLY be possible
        // if some uploader was misbehaved and queued
        // us after we succesfully managed to download some
        // information. despite the rarity of the situation,
        // we should be prepared.
        _manager.removeActiveWorker(this);
        boolean remQ = false;
        
        synchronized(Q_LOCK) {
            if(_interrupted)
                return true;
            
            // We look at _interrupted instead of InterruptedException
            // because the only method of interrupted we're interested in
            // is from the interrupt() method being called on this worker,
            // which would set the _interrupted flag.
            try {
                Q_LOCK.wait(status.getQueuePollTime());
            } catch(InterruptedException ie) {}
            
            if(_interrupted) {
                LOG.warn("WORKER: interrupted while waiting in queue " + _downloader);
                remQ = true;
            }
        }
        
        // downloader.stop() will already be called if it was interrupted.
        if(remQ)
            _manager.removeQueuedWorker(this);
        
        return remQ;
    }
    
    /** 
     * Attempts to establish a connection to the host in RFD.
     * 
     * This will return immediately, scheduling callbacks for the connection
     * events.  The appropriate ConnectObserver (Push or Direct) will be
     * notified via handleConnect if succesful or shutdown if not.  From there,
     * the rest of the download may start. 
     */
    private void establishConnection() {
        if(LOG.isTraceEnabled())
            LOG.trace("establishConnection(" + _rfd + ")");
        
        if (_manager.isCancelled() || _manager.isPaused()) {//this rfd may still be useful remember it
            _manager.addRFD(_rfd);
            _manager.workerFinished(this);
            return;
        }

        final boolean needsPush = _rfd.needsPush();
        
        synchronized (_manager) {
            int state = _manager.getState();
            //If we're just increasing parallelism, stay in DOWNLOADING
            //state.  Otherwise the following call is needed to restart
            //the timer.
            if (_manager.getNumDownloaders() == 0 && state != ManagedDownloader.COMPLETE && 
                state != ManagedDownloader.ABORTED && state != ManagedDownloader.GAVE_UP && 
                state != ManagedDownloader.DISK_PROBLEM && state != ManagedDownloader.CORRUPT_FILE && 
                state != ManagedDownloader.HASHING && state != ManagedDownloader.SAVING) {
                    if(_interrupted)
                        return; // we were signalled to stop.
                    _manager.setState(ManagedDownloader.CONNECTING, 
                            needsPush ? PUSH_CONNECT_TIME : NORMAL_CONNECT_TIME);
                }
        }

        if (LOG.isDebugEnabled())
            LOG.debug("WORKER: attempting connect to " + _rfd.getHost() + ":" + _rfd.getPort());        
        
        DownloadStat.CONNECTION_ATTEMPTS.incrementStat();

        if (_rfd.isReplyToMulticast()) {
            // Start with a push connect, fallback to a direct connect, and do
            // not forget the RFD upon push failure.
            connectWithPush(new PushConnector(false, true));
        } else if (!needsPush) {
            // Start with a direct connect, fallback to a push connect.
            connectDirectly(new DirectConnector(true));
        } else {
            // Start with a push connect, do not fallback to a direct connect, and do
            // forgot the RFD upon push failure.
            connectWithPush(new PushConnector(true, false));
        }
    }

    /**
     * Performs actions necessary after the connection process is finished. This will tell the manager this is a bad RFD
     * if no downloader could be created, and stop the downloader if we were interrupted. Returns true if the download
     * should proceed, false otherwise.
     */
    private boolean finishConnect() {
        // if we didn't connect at all, tell the rest about this rfd
        if (_downloader == null) {
            _manager.informMesh(_rfd, false);
            return false;
        } else if (_interrupted) {
            // if the worker got killed, make sure the downloader is stopped.
            _downloader.stop();
            _downloader = null;
            return false;
        }
        return true;
    }
    
    /**
     * Attempts to asynchronously connect through TCP to the remote end.
     * This will return immediately and the given observer will be notified
     * of success or failure.
     */
    private void connectDirectly(DirectConnector observer) {
        if (!_interrupted) {
            LOG.trace("WORKER: attempt asynchronous direct connection");
            _connectObserver = observer;
            try {
                Socket socket = Sockets.connect(_rfd.getHost(), _rfd.getPort(), NORMAL_CONNECT_TIME, _connectObserver);
                _connectObserver.setSocket(socket);
            } catch (IOException iox) {
                _connectObserver.shutdown();
            }
        }
    }
    
    /**
     * Attempts to connect by using a push to the remote end.
     * This method will return immediately and the given observer will
     * be notified of success or failure.
     */
    private void connectWithPush(ConnectObserver observer) {
        if(!_interrupted) {
            LOG.trace("WORKER: attempt push connection");
            _connectObserver = null;
            
            //When the push is complete and we have a socket ready to use
            //the acceptor thread is going to notify us using this object
            final MiniRemoteFileDesc mrfd = new MiniRemoteFileDesc(_rfd.getFileName(), _rfd.getIndex(), _rfd.getClientGUID());       
            _manager.registerPushObserver(observer, mrfd);
            RouterService.getDownloadManager().sendPush(_rfd, observer);
            RouterService.schedule(new Runnable() {
                public void run() {
                    _manager.unregisterPushObserver(mrfd, true);
                }
            }, _rfd.isFromAlternateLocation() ? UDP_PUSH_CONNECT_TIME : PUSH_CONNECT_TIME, 0);
        }
    }

    /**
     * Attempts to run downloader.doDownload, notifying manager of termination
     * via downloaders.notify(). 
     * To determine when this downloader should be removed
     * from the _activeWorkers list: never remove the downloader
     * from _activeWorkers if the uploader supports persistence, unless we get an
     * exception - in which case we do not add it back to files.  If !http11,
     * then we remove from the _activeWorkers in the finally block and add to files as
     * before if no problem was encountered.   
     * 
     * @param downloader the normal or push downloader to use for the transfer,
     * which MUST be initialized (i.e., downloader.connectTCP() and
     * connectHTTP() have been called)
     *
     * @return true if there was no IOException while downloading, false
     * otherwise.  
     */
    private boolean doDownload(boolean http11) {
        if(LOG.isTraceEnabled())
            LOG.trace("WORKER: about to start downloading "+_downloader);
        
        boolean problem = false;
        try {
            _downloader.doDownload();
            _rfd.resetFailedCount();
            if(http11)
                DownloadStat.SUCCESSFUL_HTTP11.incrementStat();
            else
                DownloadStat.SUCCESSFUL_HTTP10.incrementStat();
            
            LOG.debug("WORKER: successfully finished download");
        } catch (DiskException e) {
            // something went wrong while writing to the file on disk.
            // kill the other threads and set
            _manager.diskProblemOccured();
        } catch (IOException e) {
            if(http11)
                DownloadStat.FAILED_HTTP11.incrementStat();
            else
                DownloadStat.FAILED_HTTP10.incrementStat();
            problem = true;
			_manager.workerFailed(this);
        } catch (AssertFailure bad) {
            throw new AssertFailure("worker failed "+getInfo()+
                    " all workers: "+_manager.getWorkersInfo(),bad);
        } finally {
            // if we got too corrupted, notify the user
            if (_commonOutFile.isHopeless())
                _manager.promptAboutCorruptDownload();
            
            int stop=_downloader.getInitialReadingPoint()
                        +_downloader.getAmountRead();
            if(LOG.isDebugEnabled())
                LOG.debug("    WORKER:+"+
                        " terminating from "+_downloader+" at "+stop+ 
                  " error? "+problem);
            synchronized (_manager) {
                if (problem) {
                    _downloader.stop();
                    _rfd.incrementFailedCount();
                    // if we failed less than twice in succession,
                    // try to use the file again much later.
                    if( _rfd.getFailedCount() < 2 ) {
                        _rfd.setRetryAfter(FAILED_RETRY_AFTER);
                        _manager.addRFD(_rfd);
                    } else
                        _manager.informMesh(_rfd, false);
                } else {
                    _manager.informMesh(_rfd, true);
                    if( !http11 ) // no need to add http11 _activeWorkers to files
                        _manager.addRFD(_rfd);
                }
            }
        }
        
        return !problem;
    }
    
    String getInfo() {
        if (_downloader != null) {
            synchronized(_downloader) {
                return this + "hashcode " + hashCode() + " will release? "
                + _shouldRelease + " interrupted? " + _interrupted
                + " active? " + _downloader.isActive() 
                + " victim? " + _downloader.isVictim()
                + " initial reading " + _downloader.getInitialReadingPoint()
                + " initial writing " + _downloader.getInitialWritingPoint()
                + " amount to read " + _downloader.getAmountToRead()
                + " amount read " + _downloader.getAmountRead()+"\n";
            }
        } else 
            return "worker not started";
    }
    
    /** 
     * Assigns a white area or a grey area to a downloader. Sets the state,
     * and checks if this downloader has been interrupted.
     * @param _downloader The downloader to which this method assigns either
     * a grey area or white area.
     * @return the ConnectionStatus.
     */
    private ConnectionStatus assignAndRequest(boolean http11) {
        if(LOG.isTraceEnabled())
            LOG.trace("assignAndRequest for: " + _rfd);
        
        try {
            Interval interval = null;
            synchronized(_commonOutFile) {
                if (_commonOutFile.hasFreeBlocksToAssign() > 0)
                    interval = pickAvailableInterval(http11);
            }
            
            // it is still possible that a worker has died and released their ranges
            // just before we try to steal
            if (interval == null) {
                synchronized(_stealLock) {
                    assignGrey();
                }
            } else {
                assignWhite(interval);
            }
            
        } catch(NoSuchElementException nsex) {
            DownloadStat.NSE_EXCEPTION.incrementStat();
            LOG.debug(_downloader,nsex);
            
            return handleNoMoreDownloaders();
            
        } catch (NoSuchRangeException nsrx) {
            LOG.debug(_downloader,nsrx);

            return handleNoRanges();
            
        } catch(TryAgainLaterException talx) {
            DownloadStat.TAL_EXCEPTION.incrementStat();
            LOG.debug(_downloader,talx);
            
            return handleTryAgainLater();
            
        } catch(RangeNotAvailableException rnae) {
            DownloadStat.RNA_EXCEPTION.incrementStat();
            LOG.debug(_downloader,rnae);
            
            return handleRangeNotAvailable();
            
        } catch (FileNotFoundException fnfx) {
            DownloadStat.FNF_EXCEPTION.incrementStat();
            LOG.debug(_downloader, fnfx);
            
            return handleFileNotFound();
            
        } catch (NotSharingException nsx) {
            DownloadStat.NS_EXCEPTION.incrementStat();
            LOG.debug(_downloader, nsx);
            
            return handleNotSharing();
            
        } catch (QueuedException qx) { 
            DownloadStat.Q_EXCEPTION.incrementStat();
            LOG.debug(_downloader, qx);
            
            return handleQueued(qx.getQueuePosition(),qx.getMinPollTime());
            
        } catch(ProblemReadingHeaderException prhe) {
            DownloadStat.PRH_EXCEPTION.incrementStat();
            LOG.debug(_downloader,prhe);
            
            return handleProblemReadingHeader();
            
        } catch(UnknownCodeException uce) {
            DownloadStat.UNKNOWN_CODE_EXCEPTION.incrementStat();
            LOG.debug(_downloader, uce);
            
            return handleUnknownCode();
            
        } catch (ContentUrnMismatchException cume) {
        	DownloadStat.CONTENT_URN_MISMATCH_EXCEPTION.incrementStat();
            LOG.debug(_downloader, cume);
        	
			return ConnectionStatus.getNoFile();
			
        } catch (IOException iox) {
            DownloadStat.IO_EXCEPTION.incrementStat();
            LOG.debug(_downloader, iox);
            
            return handleIO();
            
        } 
        
        //did not throw exception? OK. we are downloading
        DownloadStat.RESPONSE_OK.incrementStat();
        if(_rfd.getFailedCount() > 0)
            DownloadStat.RETRIED_SUCCESS.incrementStat();    
        
        _rfd.resetFailedCount();

        synchronized(_manager) {
            if (_manager.isCancelled() || _manager.isPaused() || _interrupted) {
                LOG.trace("Stopped in assignAndRequest");
                _manager.addRFD(_rfd);
                return ConnectionStatus.getNoData();
            }
            
            _manager.workerStarted(this);
        }
        
        return ConnectionStatus.getConnected();
    }
    
    /**
     * Assigns a white part of the file to a HTTPDownloader and returns it.
     * This method has side effects.
     */
    private void assignWhite(Interval interval) throws 
    IOException, TryAgainLaterException, FileNotFoundException, 
    NotSharingException , QueuedException {
        //Intervals from the IntervalSet set are INCLUSIVE on the high end, but
        //intervals passed to HTTPDownloader are EXCLUSIVE.  Hence the +1 in the
        //code below.  Note connectHTTP can throw several exceptions.
        int low = interval.low;
        int high = interval.high; // INCLUSIVE
		_shouldRelease=true;
        _downloader.connectHTTP(low, high + 1, true,_commonOutFile.getBlockSize());
        
        //The _downloader may have told us that we're going to read less data than
        //we expect to read.  We must release the not downloading leased intervals
        //We only want to release a range if the reported subrange
        //was different, and was HIGHER than the low point.
        //in case this worker became a victim during the header exchange, we do not
        //clip any ranges.
        synchronized(_downloader) {
            int newLow = _downloader.getInitialReadingPoint();
            int newHigh = (_downloader.getAmountToRead() - 1) + newLow; // INCLUSIVE
            if (newHigh-newLow >= 0) {
                if(newLow > low) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("WORKER:"+
                                " Host gave subrange, different low.  Was: " +
                                low + ", is now: " + newLow);
                    
                    _commonOutFile.releaseBlock(new Interval(low, newLow-1));
                }
                
                if(newHigh < high) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("WORKER:"+
                                " Host gave subrange, different high.  Was: " +
                                high + ", is now: " + newHigh);
                    
                    _commonOutFile.releaseBlock(new Interval(newHigh+1, high));
                }
                
                if(LOG.isDebugEnabled()) {
                    LOG.debug("WORKER:"+
                            " assigning white " + newLow + "-" + newHigh +
                            " to " + _downloader);
                }
            } else 
                LOG.debug("debouched at birth");
        }
    }
    
    /**
     * picks an unclaimed interval from the verifying file
     * 
     * @param http11 whether the downloader is http 11
     * 
     * @throws NoSuchRangeException if the remote host is partial and doesn't 
     * have the ranges we need
     */
    private Interval pickAvailableInterval(boolean http11) throws NoSuchRangeException{
        Interval interval = null;
        //If it's not a partial source, take the first chunk.
        // (If it's HTTP11, take the first chunk up to CHUNK_SIZE)
        if( !_downloader.getRemoteFileDesc().isPartialSource() ) {
            if(http11) {
                interval = _commonOutFile.leaseWhite(findChunkSize());
            } else
                interval = _commonOutFile.leaseWhite();
        }
        
        // If it is a partial source, extract the first needed/available range
        // (If it's HTTP11, take the first chunk up to CHUNK_SIZE)
        else {
            try { 
                IntervalSet availableRanges =
                    _downloader.getRemoteFileDesc().getAvailableRanges();
                
                if(http11) {
                    interval =
                        _commonOutFile.leaseWhite(availableRanges, findChunkSize());
                } else
                    interval = _commonOutFile.leaseWhite(availableRanges);
                
            } catch(NoSuchElementException nsee) {
                // if nothing satisfied this partial source, don't throw NSEE
                // because that means there's nothing left to download.
                // throw NSRE, which means that this particular source is done.
                throw new NoSuchRangeException();
            }
        }
        
        return interval;
    }

    private int findChunkSize() {
        int chunkSize = _commonOutFile.getChunkSize();
        int free = _commonOutFile.hasFreeBlocksToAssign();
        
        // if we have less than one free chunk, take half of that
        if (free <= chunkSize && _manager.getActiveWorkers().size() > 1) 
            chunkSize = Math.max(MIN_SPLIT_SIZE, free / 2);
        
        return chunkSize;
    }
    
    /**
     * Steals a grey area from the biggesr HTTPDownloader and gives it to
     * the HTTPDownloader this method will return. 
     * <p> 
     * If there is less than MIN_SPLIT_SIZE left, we will assign the entire
     * area to a new HTTPDownloader, if the current downloader is going too
     * slow.
     */
    private void assignGrey() throws
    NoSuchElementException,  IOException, TryAgainLaterException, 
    QueuedException, FileNotFoundException, NotSharingException,  
    NoSuchRangeException  {
        
        //If this _downloader is a partial source, don't attempt to steal...
        //too confusing, too many problems, etc...
        if( _downloader.getRemoteFileDesc().isPartialSource() )
            throw new NoSuchRangeException();

        DownloadWorker slowest = findSlowestDownloader();
                        
        if (slowest==null) {//Not using this downloader...but RFD maybe useful
            LOG.debug("didn't find anybody to steal from");
            throw new NoSuchElementException();
        }
		
        // see what ranges is the victim requesting
        Interval slowestRange = slowest.getDownloadInterval();
        
        if (slowestRange.low == slowestRange.high)
            throw new NoSuchElementException();
        
        //Note: we are not interested in being queued at this point this
        //line could throw a bunch of exceptions (not queuedException)
        _downloader.connectHTTP(slowestRange.low, slowestRange.high, false,_commonOutFile.getBlockSize());
        
        Interval newSlowestRange;
        int newStart;
        synchronized(slowest.getDownloader()) {
            // if the victim died or was stopped while the thief was connecting, we can't steal
            if (!slowest.getDownloader().isActive()) {
                LOG.debug("victim is no longer active");
                throw new NoSuchElementException();
            }
            
            // see how much did the victim download while we were exchanging headers.
            // it is possible that in that time some other worker died and freed his ranges, and
            // the victim has already been assigned some new ranges.  If that happened we don't steal.
            newSlowestRange = slowest.getDownloadInterval();
            if (newSlowestRange.high != slowestRange.high) {
                if (LOG.isDebugEnabled())
                    LOG.debug("victim is now downloading something else "+
                            newSlowestRange+" vs. "+slowestRange);
                throw new NoSuchElementException();
            }
            
            if (newSlowestRange.low > slowestRange.low && LOG.isDebugEnabled()) {
                LOG.debug("victim managed to download "+(newSlowestRange.low - slowestRange.low)
                        +" bytes while stealer was connecting");
            }
            
            int myLow = _downloader.getInitialReadingPoint();
            int myHigh = _downloader.getAmountToRead() + myLow; // EXCLUSIVE
            
            // If the stealer isn't going to give us everything we need,
            // there's no point in stealing, so throw an exception and
            // don't steal.
            if( myHigh < slowestRange.high ) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("WORKER: not stealing because stealer " +
                            "gave a subrange.  Expected low: " + slowestRange.low +
                            ", high: " + slowestRange.high + ".  Was low: " + myLow +
                            ", high: " + myHigh);
                }
                
                throw new IOException("bad stealer.");
            }
            
            newStart = Math.max(newSlowestRange.low,myLow);
            if(LOG.isDebugEnabled()) {
                LOG.debug("WORKER:"+
                        " picking stolen grey "
                        +newStart + "-"+slowestRange.high+" from "+slowest+" to "+_downloader);
            }
            
            
            // tell the victim to stop downloading at the point the thief 
            // can start downloading
            slowest.getDownloader().stopAt(newStart);
        }
        
        // once we've told the victim where to stop, make our ranges release-able
        _downloader.startAt(newStart);
        _shouldRelease = true;
    }
    
    Interval getDownloadInterval() {
        HTTPDownloader downloader = _downloader;
        synchronized(downloader) {
            
            int start = Math.max(downloader.getInitialReadingPoint() + downloader.getAmountRead(),
                    downloader.getInitialWritingPoint());
            
            int stop = downloader.getInitialReadingPoint() + downloader.getAmountToRead();
            
            return new Interval(start,stop);
        }
    }
    
    /**
     * @return the httpdownloader that is going slowest.
     */
    private DownloadWorker findSlowestDownloader() {
        DownloadWorker slowest = null;
        final float ourSpeed = getOurSpeed();
        float slowestSpeed = ourSpeed;
        
        Set queuedWorkers = _manager.getQueuedWorkers().keySet();
        for (Iterator iter=_manager.getAllWorkers().iterator(); iter.hasNext();) {
            
            DownloadWorker worker = (DownloadWorker) iter.next();
            if (queuedWorkers.contains(worker))
                continue;
            
            HTTPDownloader h = worker.getDownloader();
            
            if (h == null || h == _downloader)
                continue;
            
            // if we don't have speed yet, steal from the first slow guy
            if (ourSpeed == UNKNOWN_SPEED) {
                if (worker.isSlow()) 
                    return worker;
            } else {
                // see if he is the slowest one
                float hisSpeed = 0;
                try {
                    h.getMeasuredBandwidth();
                    hisSpeed = h.getAverageBandwidth();
                } catch (InsufficientDataException ide) {
                    // we assume these guys would go almost as fast as we do, so we do not steal
                    // from them unless they are the last ones remaining
                    hisSpeed = Math.max(0f,ourSpeed - 0.1f);
                }
                
                if (hisSpeed < slowestSpeed) {
                    slowestSpeed = hisSpeed;
                    slowest = worker;
                }
            }
            
        }
        return slowest;
    }
    
    private float getOurSpeed() {
        if (_downloader == null)
            return UNKNOWN_SPEED;
        try {
            _downloader.getMeasuredBandwidth();
            return _downloader.getAverageBandwidth();
        } catch (InsufficientDataException bad) {
            return UNKNOWN_SPEED;
        }
    }
    
    boolean isSlow() {
        float ourSpeed = getOurSpeed();
        return ourSpeed < MIN_ACCEPTABLE_SPEED && ourSpeed != UNKNOWN_SPEED;
    }
    
    ////// various handlers for failure states of the assign process /////
    
    /**
     * no more ranges to download or no more people to steal from - finish download 
     */
    private ConnectionStatus handleNoMoreDownloaders() {
        _manager.addRFD(_rfd);
        return ConnectionStatus.getNoData();
    }
    
    /**
     * The file does not have such ranges 
     */
    private ConnectionStatus handleNoRanges() {
        //forget the ranges we are preteding uploader is busy.
        _rfd.setAvailableRanges(null);
        
        //if this RFD did not already give us a retry-after header
        //then set one for it.
        if(!_rfd.isBusy())
            _rfd.setRetryAfter(NO_RANGES_RETRY_AFTER);
        
        _rfd.resetFailedCount();                
        _manager.addRFD(_rfd);
        
        return ConnectionStatus.getNoFile();
    }
    
    private ConnectionStatus handleTryAgainLater() {
        //if this RFD did not already give us a retry-after header
        //then set one for it.
        if ( !_rfd.isBusy() ) {
            _rfd.setRetryAfter(RETRY_AFTER_NONE_ACTIVE);
        }
        
        //if we already have downloads going, then raise the
        //retry-after if it was less than the appropriate amount
        if(!_manager.getActiveWorkers().isEmpty() &&
                _rfd.getWaitTime(System.currentTimeMillis()) < RETRY_AFTER_SOME_ACTIVE)
            _rfd.setRetryAfter(RETRY_AFTER_SOME_ACTIVE);
        
        _manager.addRFD(_rfd);//try this rfd later
        
        _rfd.resetFailedCount();                
        return ConnectionStatus.getNoFile();
    }
    
    /**
     * The ranges exist in the file, but the remote host does not have them
     */
    private ConnectionStatus handleRangeNotAvailable() {
        _rfd.resetFailedCount();                
        _manager.informMesh(_rfd, true);
        //no need to add to files or busy we keep iterating
        return ConnectionStatus.getPartialData();
    }
    
    private ConnectionStatus handleFileNotFound() {
        _manager.informMesh(_rfd, false);
        return ConnectionStatus.getNoFile();
    }
    
    private ConnectionStatus handleNotSharing() {
        return handleFileNotFound();
    }
    
    private ConnectionStatus handleQueued(int position, int pollTime) {
        if(_manager.getActiveWorkers().isEmpty()) {
            if(_manager.isCancelled() || _manager.isPaused() ||  _interrupted)
                return ConnectionStatus.getNoData(); // we were signalled to stop.
            _manager.setState(ManagedDownloader.REMOTE_QUEUED);
        }
        _rfd.resetFailedCount();                
        return ConnectionStatus.getQueued(position, pollTime);
    }
    
    private ConnectionStatus handleProblemReadingHeader() {
        return handleFileNotFound();
    }
    
    private ConnectionStatus handleUnknownCode() {
        return handleFileNotFound();
    }
    
    private ConnectionStatus handleIO(){
        _rfd.incrementFailedCount();
        
        // if this RFD had an IOX while reading headers/downloading
        // less than twice in succession, try it again.
        if( _rfd.getFailedCount() < 2 ) {
            //set retry after, wait a little before retrying this RFD
            _rfd.setRetryAfter(FAILED_RETRY_AFTER);
            _manager.addRFD(_rfd);
        } else //tried the location twice -- it really is bad
            _manager.informMesh(_rfd, false);
        
        return ConnectionStatus.getNoFile();
    }
    
    //////// end handlers of various failure states ///////
    
    /**
     * interrupts this downloader.
     */
    void interrupt() {
        _interrupted = true;
        
        if (_downloader != null)
            _downloader.stop();
        
        //Ensure that the ConnectObserver is cleaned up.
        DirectConnector observer = _connectObserver;
        if(observer != null) {
            // Make sure it's not queued with Sockets.
            if(Sockets.removeConnectObserver(_connectObserver))
                _manager.workerFinished(this);
            // Make sure it immediately stops trying to connect.
            else if(observer.getSocket() != null)
                IOUtils.close(observer.getSocket());
        }
        
        // make sure that queued downloaders are stopped.
        synchronized(Q_LOCK) {
            Q_LOCK.notify();
        }
    }

    
    public RemoteFileDesc getRFD() {
        return _rfd;
    }
    
    HTTPDownloader getDownloader() {
        return _downloader;
    }
    
    public String toString() {
        return workerName + " -> "+_rfd;  
    }
    
    /**
     * Starts a new thread that will perform the download.
     * @param dl
     */
    private void startDownload(HTTPDownloader dl) {
        _downloader = dl;
        
        // If we should continue, then start the download.
        if(finishConnect()) {
            ThreadFactory.startThread(new DownloadRunner(), workerName);
        } else {
            _manager.workerFinished(this);
        }
    }
    
    /**
     * A Runnable that processes the download.
     */
    private class DownloadRunner implements Runnable {
        /** 
         * Runs the connector, initializes alternate locations, and does the download.
         * This will always call _manager.workerFinished after completing.
         */
        public void run() {
            try {
                initializeAlternateLocations();
                httpLoop();
            } finally {
                _manager.workerFinished(DownloadWorker.this);
            }
        }
    }
    
    /**
     * A ConnectObserver for starting the download via a push connect.
     */
    private class PushConnector implements ConnectObserver {
        private boolean forgetOnFailure;
        private boolean directConnectOnFailure;
        
        /**
         * Creates a new PushConnector.  If forgetOnFailure is true,
         * this will call _manager.forgetRFD(_rfd) if the push fails.
         * If directConnectOnFailure is true, this will attempt a direct
         * connection if the push fails.
         * Upon success, this will always start the download.
         * 
         * @param forgetOnFailure
         * @param directConnectOnFailure
         */
        PushConnector(boolean forgetOnFailure, boolean directConnectOnFailure) {
            this.forgetOnFailure = forgetOnFailure;
            this.directConnectOnFailure = directConnectOnFailure;
        }

        /**
         * Notification that the push succeeded.  Starts the download if the connection still exists.
         */
        public void handleConnect(Socket socket) {
            //LOG.debug(_rfd + " -- Handling connect from PushConnector");
            HTTPDownloader dl = new HTTPDownloader(socket, _rfd, _commonOutFile, _manager instanceof InNetworkDownloader);
            try {
               dl.connectTCP(0);
               DownloadStat.CONNECT_PUSH_SUCCESS.incrementStat();
            } catch(IOException iox) {
              //  LOG.debug(_rfd + " -- IOX after starting connected from PushConnector.");
                DownloadStat.PUSH_FAILURE_LOST.incrementStat();
                failed();
                return;
            }
            
            startDownload(dl);
        }

        /** Notification that the push failed. */
        public void shutdown() {
           // LOG.debug(_rfd + " -- Handling shutdown from PushConnector");            
            DownloadStat.PUSH_FAILURE_NO_RESPONSE.incrementStat();
            failed();
        }
        
        /**
         * Possibly tells the manager to forget this RFD, cleans up various things,
         * and tells the manager to forget this worker.
         */
        private void failed() {            
            _manager.unregisterPushObserver(new MiniRemoteFileDesc(_rfd), false);
            
            if(!directConnectOnFailure) {
                if(forgetOnFailure) {
                    _manager.forgetRFD(_rfd);
                }
                finishConnect();
                _manager.workerFinished(DownloadWorker.this);
            } else {
                connectDirectly(new DirectConnector(false));
            }
        }

        // unused
        public void handleIOException(IOException iox) {}
    }
    
    /**
     * A ConnectObserver for starting the download via a direct connect.
     */
    private class DirectConnector implements ConnectObserver {
        private long createTime = System.currentTimeMillis();
        private boolean pushConnectOnFailure;
        private Socket connectingSocket;
        
        /**
         * Creates a new DirectConnection.  If pushConnectOnFailure is true,
         * this will attempt a push connection if the direct connect fails.
         * Upon success, this will always start a new download.
         *   
         * @param pushConnectOnFailure
         */
        DirectConnector(boolean pushConnectOnFailure) {
            this.pushConnectOnFailure = pushConnectOnFailure;
        }
        
        /**
         * Upon succesful connect, create the HTTPDownloader with the right socket, and proceed to continue
         * downloading.
         */
        public void handleConnect(Socket socket) {
            this.connectingSocket = null;
            
           // LOG.debug(_rfd + " -- Handling connect from DirectConnector");
            NumericalDownloadStat.TCP_CONNECT_TIME.addData((int) (System.currentTimeMillis() - createTime));
            DownloadStat.CONNECT_DIRECT_SUCCESS.incrementStat();
            HTTPDownloader dl = new HTTPDownloader(socket, _rfd, _commonOutFile, _manager instanceof InNetworkDownloader);
            try {
                dl.connectTCP(0); // already connected, timeout doesn't matter.
            } catch(IOException iox) {
                shutdown(); // if it immediately IOX's, try a push instead.
                return;
            }
            
            startDownload(dl);
        }

        /**
         * Upon unsuccesful connect, try using a push (if pushConnectOnFailure is true).
         */
        public void shutdown() {
            this.connectingSocket = null;
            
           // LOG.debug(_rfd + " -- Handling shutdown from DirectConnnector");
            DownloadStat.CONNECT_DIRECT_FAILURES.incrementStat();
            if(pushConnectOnFailure) {
                connectWithPush(new PushConnector(false, false));
            } else {
                finishConnect();
                _manager.workerFinished(DownloadWorker.this);
            }
        }
        
        void setSocket(Socket socket) {
            this.connectingSocket = socket;
        }
        
        Socket getSocket() {
            return this.connectingSocket;
        }

        // unused.
        public void handleIOException(IOException iox) {}
    }
}
