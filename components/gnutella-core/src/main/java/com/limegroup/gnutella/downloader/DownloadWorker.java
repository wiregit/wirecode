
package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.statistics.DownloadStat;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.TigerTreeCache;
import com.limegroup.gnutella.util.IntervalSet;

/**
 * Class that performs the logic of downloading a file from a single host.
 */
public class DownloadWorker implements Runnable {
    /*
      
      Each potential downloader thats working in parallel does these steps
      1. Establish a TCP connection with an rfd
         if unable to connect end this parallel execution
      2. This step has two parts
            a.  Grab a part of the file to download. If there is unclaimed area on
                the file grab that, otherwise try to steal claimed area from another 
                worker
            b.  Send http headers to the uploader on the tcp connection 
                established  in step 1. The uploader may or may not be able to 
                upload at this time. If the uploader can't upload, it's 
                important that the leased area be restored to the state 
                they were in before we started trying. However, if the http 
                handshaking was successful, the downloader can keep the 
                part it obtained.
          The two steps above must be  atomic wrt other downloaders. 
          Othersise, other downloaders in parallel will be  able to lease the 
          same areas, or try to steal the same area from the same downloader.
      3. Download the file by delegating to the HTTPDownloader, and then do 
         the book-keeping. Termination may be normal or abnormal. 
     
     
                              connectAndDownload
                          /           |             \
        establishConnection     assignAndRequest    doDownload
             |                        |             |       \
       HTTPDownloader.connectTCP      |             |        requestHashTree
                                      |             |- HTTPDownloader.download
                            assignWhite/assignGrey
                                      |
                           HTTPDownloader.connectHTTP
                           
      For push downloads, the acceptDownload(file, Socket,index,clientGUI) 
      method of ManagedDownloader is called from the Acceptor instance. This
      method needs to notify the appropriate downloader so that it can use
      the socket. 
      
      When establishConnection() realizes that it needs to do a push, it puts  
      into miniRFDToLock, asks the DownloadManager to send a push and 
      then waits on the same lock.
       
      Eventually acceptDownload will be called. 
      acceptDownload uses the file, index and clientGUID to look up the map and
      notifies the DownloadWorker that its socket has arrived.
      
      Note: The establishConnection thread waits for a limited amount of time 
      (about 9 seconds) and then checks the map for the socket anyway, if 
      there is no entry, it assumes the push failed and terminates.

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
    private static final int RETRY_AFTER_NONE_ACTIVE = 60 * 1; // 1 minute
    
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
     * The thread Object of this worker
     */
    private volatile Thread _myThread;
    
    /**
     * Whether I was interrupted before starting
     */
    private volatile boolean _interrupted;
    
    /**
     * Reference to the stealLock all workers for a download will synchronize on
     */
    private final Object _stealLock;
    
    /**
     * Socket to use when doing a push download.
     */
    private Socket _pushSocket;
    
    /**
     * The downloader that will do the actual downloading
     */
    private HTTPDownloader _downloader;
    
    /**
     * Whether I should release the ranges that I have leased for download
     */
    private boolean _shouldRelease;
    
    DownloadWorker(ManagedDownloader manager, RemoteFileDesc rfd, 
            VerifyingFile vf, Object lock){
        _manager = manager;
        _rfd = rfd;
        _stealLock = lock;
        _commonOutFile = vf;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        
        // first get a handle of our thread object
        _myThread = Thread.currentThread();
        
        boolean iterate = false;
        
        try {
            // if I was interrupted before being started, don't do anything.
            if (_interrupted)
                throw new InterruptedException();
            
            iterate = connectAndDownload();
        } catch (Throwable e) {
            iterate = true;
             // Ignore InterruptedException -- the JVM throws
             // them for some reason at odd times, even though
             // we've caught and handled all of them
             // appropriately.
            if(!(e instanceof InterruptedException)) {
                //This is a "firewall" for reporting unhandled
                //errors.  We don't really try to recover at
                //this point, but we do attempt to display the
                //error in the GUI for debugging purposes.
                ErrorService.error(e);
            }
        } finally {
            _manager.workerFinished(this, iterate);
        }
    }
    
    /**
     * Top level method of the thread. Calls three methods 
     * a. Establish a TCP Connection.
     * b. Assign this thread a part of the file, and do HTTP handshaking
     * c. get the file.
     * Each of these steps can run into errors, which have to be dealt with
     * differently.
     * @return true if this worker thread should notify, false otherwise.
     * currently this method returns false iff NSEEx is  thrown. 
     */
    private boolean connectAndDownload() {
        if(LOG.isTraceEnabled())
            LOG.trace("connectAndDownload for: " + _rfd);
        
        //this make throw an exception if we were not able to establish a 
        //direct connection and push was unsuccessful too
        
        //Step 1. establish a TCP Connection, either by opening a socket,
        //OR by sending a push request.
        _downloader = establishConnection();
        
        if(_downloader == null)//any exceptions in the method internally?
            return true;//no work was done, try to get another thread

        //initilaize the newly created HTTPDownloader with whatever AltLocs we
        //have discovered so far. These will be cleared out after the first
        //write, from them on, only newly successful rfds will be sent as alts
              
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
        
        //Note: http11 is true or false depending on what we think thevalue
        //should be for rfd is at the start, before connecting. We may later
        //find that the we are wrong, in which case we update the rfd's http11
        //value. But while we are in connectAndDownload we continue to use this
        //local variable because the code is incapable of handling a change in
        //http11 status while inside connectAndDownload.
        boolean http11 = true;//must enter the loop
        
        try {

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
                
                // if we didn't get queued doing the tree request,
                // request another file.
                if (status == null || !status.isQueued()) {
                        try {
                            status = assignAndRequest(http11);
                            
                            // add any locations we may have received
                            _manager.addLocationsToDownload(_downloader.getAltLocsReceived(),
                                    _downloader.getPushLocsReceived(),
                                    _rfd.getSize());
                        } finally {
                            // clear ranges did not connect
                            if( status == null || !status.isConnected() )
                                releaseRanges();
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
                        status == null || !status.isQueued()? -1 : status.getQueuePosition());
                
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
                    return true;
            }
            
            
            //we have been given a slot remove this thread from queuedThreads
            _manager.removeQueuedWorker(this);

            switch(status.getType()) {
            case ConnectionStatus.TYPE_NO_FILE:
                // close the connection for now.            
                _downloader.stop();
                return true;            
            case ConnectionStatus.TYPE_NO_DATA:
                // close the connection since we're finished.
                _downloader.stop();
                return false;
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
            try {
                if(!doDownload(http11))
                    break;
            }finally {
                releaseRanges();
            }
        } // end of while(http11)
        
        } finally {
            // we must ensure that all _downloaders are removed from the data
            // structure before returning from this method.
            _manager.removeActiveWorker(this);
        }
        
        //tell manager to iterate for more sources
        return true;
    }
    
    private ConnectionStatus requestTHEXIfNeeded() {
        HashTree ourTree = _commonOutFile.getHashTree();
        
        ConnectionStatus status = null;
        // request THEX from te _downloader if the tree we have
        // isn't good enough (or we don't have a tree)
        if (_downloader.hasHashTree() &&
                (ourTree == null || !ourTree.isDepthGoodEnough())) {
            status = _downloader.requestHashTree();
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
        
        int high, low;
        synchronized(_downloader) {
            // do not release ranges for downloaders that we have stolen from
            // since they are still marked as leased
            low=_downloader.getInitialReadingPoint()+_downloader.getAmountRead();
            high = _downloader.getInitialReadingPoint()+_downloader.getAmountToRead()-1;
        }
        
        if( (high-low)>=0) {//dloader failed to download a part assigned to it?
            
            if (LOG.isDebugEnabled())
                LOG.debug("releasing ranges "+new Interval(low,high));
            
            // necessary to synchronize on steal lock, otherwise a downloader may 
            // try to steal while another downloader has died and released its
            // ranges.
            synchronized(_stealLock) {
                _commonOutFile.releaseBlock(new Interval(low,high));
            }
        } else 
			LOG.debug("nothing to release!");
    }
    
    /**
     * Handles a queued downloader with the given ConnectionStatus.
     * BLOCKING (while sleeping).
     *
     * @return true if we need to tell the manager to churn another
     *         connection and let this one die, false if we are
     *         going to try this connection again.
     */
    private boolean handleQueued(ConnectionStatus status) {
        try {
            // make sure that we're not in _downloaders if we're
            // sleeping/queued.  this would ONLY be possible
            // if some uploader was misbehaved and queued
            // us after we succesfully managed to download some
            // information.  despite the rarity of the situation,
            // we should be prepared.
            _manager.removeActiveWorker(this);
            
            Thread.sleep(status.getQueuePollTime());//value from QueuedException
            return false;
        } catch (InterruptedException ix) {
            if(LOG.isWarnEnabled())
                LOG.warn("worker: interrupted while asleep in "+
                  "queue" + _downloader);
            _manager.removeQueuedWorker(this);
            _downloader.stop(); //close connection
            // notifying will make no diff, coz the next 
            //iteration will throw interrupted exception.
            return true;
        }
    }
    
    /** 
     * Returns an un-initialized (only established a TCP Connection, 
     * no HTTP headers have been exchanged yet) connectable downloader 
     * from the given list of locations.
     * <p> 
     * method tries to establish connection either by push or by normal
     * ways.
     * <p>
     * If the connection fails for some reason, or needs a push the mesh needs 
     * to be informed that this location failed.
     * @param rfd the RemoteFileDesc to connect to
     * <p> 
     * The following exceptions may be thrown within this method, but they are
     * all dealt with internally. So this method does not throw any exception
     * <p>
     * NoSuchElementException thrown when (both normal and push) connections 
     * to the given rfd fail. We discard the rfd by doing nothing and return 
     * null.
     * @exception InterruptedException this thread was interrupted while waiting
     * to connect. Remember this rfd by putting it back into files and return
     * null 
     */
    private HTTPDownloader establishConnection() {
        if(LOG.isTraceEnabled())
            LOG.trace("establishConnection(" + _rfd + ")");
        
        if (_rfd == null) //bad rfd, discard it and return null
            return null; // throw new NoSuchElementException();
        
        if (_manager.isCancelled() || _manager.isPaused()) {//this rfd may still be useful remember it
            _manager.addRFD(_rfd);
            return null;
        }

        HTTPDownloader ret;
        boolean needsPush = _rfd.needsPush();
        
        
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
                        return null; // we were signalled to stop.
                    _manager.setState(ManagedDownloader.CONNECTING, 
                            needsPush ? PUSH_CONNECT_TIME : NORMAL_CONNECT_TIME);
                }
        }

        if(LOG.isDebugEnabled())
            LOG.debug("WORKER: attempting connect to "
              + _rfd.getHost() + ":" + _rfd.getPort());        
        
        DownloadStat.CONNECTION_ATTEMPTS.incrementStat();

        // for multicast replies, try pushes first
        // and then try direct connects.
        // this is because newer clients work better with pushes,
        // but older ones didn't understand them
        if( _rfd.isReplyToMulticast() ) {
            try {
                ret = connectWithPush();
            } catch(IOException e) {
                try {
                    ret = connectDirectly();
                } catch(IOException e2) {
                    return null; // impossible to connect.
                }
            }
            return ret;
        }        
        
        // otherwise, we're not multicast.
        // if we need a push, go directly to a push.
        // if we don't, try direct and if that fails try a push.        
        if( !needsPush ) {
            try {
                ret = connectDirectly();
                return ret;
            } catch(IOException e) {
                // fall through to the push ...
            }
        }
        try {
                 ret = connectWithPush();
                 return ret;
        } catch(IOException e) {
                // even the push failed :(
        }
        
        
        // if we're here, everything failed.
        
        _manager.informMesh(_rfd, false);
        
        return null;
    }
    
    /**
     * Attempts to directly connect through TCP to the remote end.
     */
    private HTTPDownloader connectDirectly() throws IOException {
        LOG.trace("WORKER: attempt direct connection");
        HTTPDownloader ret;
        //Establish normal downloader.              
        ret = new HTTPDownloader(_rfd, _commonOutFile);
        // Note that connectTCP can throw IOException
        // (and the subclassed CantConnectException)
        try {
            ret.connectTCP(NORMAL_CONNECT_TIME);
            DownloadStat.CONNECT_DIRECT_SUCCESS.incrementStat();
        } catch(IOException iox) {
            DownloadStat.CONNECT_DIRECT_FAILURES.incrementStat();
            throw iox;
        }
        return ret;
    }
    
    /**
     * Attempts to connect by using a push to the remote end.
     * BLOCKING.
     */
    private HTTPDownloader connectWithPush() throws IOException {
        LOG.trace("WORKER: attempt push connection");
        HTTPDownloader ret;
        
        //When the push is complete and we have a socket ready to use
        //the acceptor thread is going to notify us using this object
        MiniRemoteFileDesc mrfd = new MiniRemoteFileDesc(
                     _rfd.getFileName(),_rfd.getIndex(),_rfd.getClientGUID());
       
        _manager.registerPushWaiter(this,mrfd);

        boolean pushSent;
        Socket pushSocket = null;
        synchronized(this) {
            // only wait if we actually were able to send the push
            RouterService.getDownloadManager().sendPush(_rfd, this);
            
            //No loop is actually needed here, assuming spurious
            //notify()'s don't occur.  (They are not allowed by the Java
            //Language Specifications.)  Look at acceptDownload for
            //details.
            try {
                wait(_rfd.isFromAlternateLocation()? 
                        UDP_PUSH_CONNECT_TIME: 
                            PUSH_CONNECT_TIME);
                pushSocket = _pushSocket;
                _pushSocket = null;
            } catch(InterruptedException e) {
                DownloadStat.PUSH_FAILURE_INTERRUPTED.incrementStat();
                throw new IOException("push interupted.");
            }
            
        }
        
        //Done waiting or were notified.
        if (pushSocket==null) {
            DownloadStat.PUSH_FAILURE_NO_RESPONSE.incrementStat();
            
            throw new IOException("push socket is null");
        }
        
        _manager.unregisterPushWaiter(mrfd);//we are not going to use it after this
        ret = new HTTPDownloader(pushSocket, _rfd, _commonOutFile);
        
        //Socket.getInputStream() throws IOX if the connection is closed.
        //So this connectTCP *CAN* throw IOX.
        try {
            ret.connectTCP(0);//just initializes the byteReader in this case
            DownloadStat.CONNECT_PUSH_SUCCESS.incrementStat();
        } catch(IOException iox) {
            DownloadStat.PUSH_FAILURE_LOST.incrementStat();
            throw iox;
        }
        return ret;
    }
    
    /**
     * callback to notify that a push request was received
     */
    synchronized void setPushSocket(Socket s) {
        _pushSocket = s;
        notify();
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
        RemoteFileDesc rfd = _downloader.getRemoteFileDesc();            
        try {
            _downloader.doDownload();
            rfd.resetFailedCount();
            if(http11)
                DownloadStat.SUCCESFULL_HTTP11.incrementStat();
            else
                DownloadStat.SUCCESFULL_HTTP10.incrementStat();
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
                    rfd.incrementFailedCount();
                    // if we failed less than twice in succession,
                    // try to use the file again much later.
                    if( rfd.getFailedCount() < 2 ) {
                        rfd.setRetryAfter(FAILED_RETRY_AFTER);
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
            if (_commonOutFile.hasFreeBlocksToAssign() > 0) {
                assignWhite(http11);
            } else {
		synchronized(_stealLock) {
                	assignGrey(); 
		}
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
    private void assignWhite(boolean http11) throws 
    IOException, TryAgainLaterException, FileNotFoundException, 
    NotSharingException , QueuedException, NoSuchRangeException,
    NoSuchElementException {
        //Assign "white" (unclaimed) interval to new downloader.
        Interval interval = pickAvailableInterval(http11);

        //Intervals from the IntervalSet set are INCLUSIVE on the high end, but
        //intervals passed to HTTPDownloader are EXCLUSIVE.  Hence the +1 in the
        //code below.  Note connectHTTP can throw several exceptions.
        int low = interval.low;
        int high = interval.high; // INCLUSIVE
		_shouldRelease=true;
        _downloader.connectHTTP(low, high + 1, true);
        
        //The _downloader may have told us that we're going to read less data than
        //we expect to read.  We must release the not downloading leased intervals
        //note the confusion caused by downloading overlap
        //regions.  We only want to release a range if the reported subrange
        //was different, and was HIGHER than the low point.        
        int newLow = _downloader.getInitialReadingPoint();
        int newHigh = (_downloader.getAmountToRead() - 1) + newLow; // INCLUSIVE
        
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
        if (free <= chunkSize && _manager.getPossibleHostCount() > 1) 
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

        //Split largest "gray" interval, i.e., steal another
        //downloader's region for a new downloader.  
        HTTPDownloader biggest = findBiggestDownloader();
                        
        if (biggest==null) //Not using this downloader...but RFD maybe useful
            throw new NoSuchElementException();
        

		if (!shouldSteal(biggest))
            throw new NoSuchElementException();
		
        //replace (bad boy) biggest if possible
        int start,stop;
        synchronized(biggest) {
            start = Math.max(biggest.getInitialReadingPoint() + biggest.getAmountRead(),
                    biggest.getInitialWritingPoint());
            
            stop = biggest.getInitialReadingPoint() + biggest.getAmountToRead();
        }
        
        _shouldRelease=false;
        //Note: we are not interested in being queued at this point this
        //line could throw a bunch of exceptions (not queuedException)
        _downloader.connectHTTP(start, stop, false);
        
        int myLow = _downloader.getInitialReadingPoint();
        int myHigh = _downloader.getAmountToRead() + myLow; // EXCLUSIVE
        
        // If the stealer isn't going to give us everything we need,
        // there's no point in stealing, so throw an exception and
        // don't steal.
        if( myHigh < stop ) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("WORKER: not stealing because stealer " +
                        "gave a subrange.  Expected low: " + start +
                        ", high: " + stop + ".  Was low: " + myLow +
                        ", high: " + myHigh);
            }
            
            throw new IOException("bad stealer.");
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("WORKER:"+
                    " picking stolen grey "
                    +start+"-"+stop+" from "+biggest+" to "+_downloader);
        }
        
        // stop the victim
        int newStart;
        synchronized(biggest) {
            // free up whatever the victim wrote while we were connecting
            // unless that data is already verified or pending verification
            newStart = biggest.getInitialReadingPoint() + biggest.getAmountRead();
			
			// the victim may have even completed their download while we were
	        // connecting
	        if (stop <= newStart)
	            throw new NoSuchElementException();
			
            // tell the victim to stop downloading at the point the thief 
            // can start downloading
			biggest.stopAt(Math.max(newStart,myLow));
        }
        
        if (newStart > start && LOG.isDebugEnabled()) {
            LOG.debug("victim managed to download "+(newStart - start)
                    +" bytes while stealer was connecting");
        }
        
		// once we've told the victim where to stop, make our ranges release-able
        _downloader.startAt(Math.max(newStart,myLow));
        _shouldRelease = true;
	}
    
    /**
     * @return the httpdownloader that has leased the biggest chunk of the file
     */
    private HTTPDownloader findBiggestDownloader() {
        HTTPDownloader biggest = null;
        
        for (Iterator iter=_manager.getActiveWorkers().iterator(); iter.hasNext();) {
            HTTPDownloader h = ((DownloadWorker) iter.next()).getDownloader();
            
            // If this guy isn't downloading, don't steal from him.
            if(!h.isActive())
                continue;
            
            // If we have no one to steal from, use 
            if(biggest == null)
                biggest = h;
            
            // Otherwise, steal only if what's left is
            // larger and there's stuff left.
            else {
                int hLeft = h.getAmountToRead() - h.getAmountRead();
                int bLeft = biggest.getAmountToRead() - 
                biggest.getAmountRead();
                if( hLeft > 0 && hLeft > bLeft )
                    biggest = h;
            }
        }
        
        return biggest;
    }
    
    /**
     * If this downloader is faster than the victim
     * OR
     * If we don't know how fast is this downloader but the victim is slow,
     * let this steal.
     * 
     * @return whether our downloader should steal from the given downloader
     */
    private boolean shouldSteal(HTTPDownloader biggest) {
        // check if we need to steal from a slow downloader.
        float bandwidthVictim = -1;
        float bandwidthStealer = -1;
        
        try {
            bandwidthVictim = biggest.getAverageBandwidth();
            biggest.getMeasuredBandwidth(); // trigger IDE.
        } catch (InsufficientDataException ide) {
            LOG.debug("victim does not have datapoints", ide);
            bandwidthVictim = -1;
        }
        
        try {
            bandwidthStealer = _downloader.getAverageBandwidth();
            _downloader.getMeasuredBandwidth(); // trigger IDE.
        } catch(InsufficientDataException ide) {
            LOG.debug("stealer does not have datapoints", ide);
            bandwidthStealer = -1;
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("WORKER: "+
                    _downloader + " attempting to steal from " + 
                    biggest + ", stealer speed [" + bandwidthStealer +
                    "], victim speed [ " + bandwidthVictim + "]");
        }
        
        
        return biggest.getAmountRead() < biggest.getAmountToRead() && 
                (bandwidthStealer > bandwidthVictim ||
                    (bandwidthVictim != -1 &&
                            bandwidthVictim < MIN_ACCEPTABLE_SPEED && 
                            bandwidthStealer == -1));
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
                _rfd.getWaitTime() < RETRY_AFTER_SOME_ACTIVE)
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
        _manager.workerQueued(this,position);
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
        if (_myThread != null)
            _myThread.interrupt();
    }

    
    public RemoteFileDesc getRFD() {
        return _rfd;
    }
    
    HTTPDownloader getDownloader() {
        return _downloader;
    }

}
