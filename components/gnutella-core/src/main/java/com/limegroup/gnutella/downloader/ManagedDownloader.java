package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import java.util.Date;
import java.io.*;
import java.net.*;

/**
 * A smart download.  Tries to get a group of similar files by delegating
 * to HTTPDownloader objects.  Does retries and resumes automatically.
 * Reports all changes to a DownloadManager.  This class is thread safe.<p>
 *
 * Smart downloads can use many policies, and these policies are free to change
 * as allowed by the Downloader specification.  This implementation tries the N
 * fastest hosts in parallel.  The first host to connect is declared the winner,
 * and the others are terminated.  The whole process is repeated until all hosts
 * are unreachable.  Note that the whole process will be repeated as long as we
 * get the busy signal from a host.<p>
 *
 * This class implements the Serializable interface but defines its own
 * writeObject and readObject methods.  This is necessary because parts of the
 * ManagedDownloader (e.g., sockets) are inherently unserializable.  For this
 * reason, serializing and deserializing a ManagedDownloader M results in a
 * ManagedDownloader M' that is the same as M except it is
 * unconnected. <b>Furthermore, it is necessary to explicitly call
 * initialize(..) after reading a ManagedDownloader from disk.</b> 
 */
public class ManagedDownloader implements Downloader, Serializable {
    /*********************************************************************
     * LOCKING: obtain this's monitor before modifying any of the following.
     ***********************************************************************/

    /** This' manager for callbacks and queueing. */
    private DownloadManager manager;
    /** The repository of incomplete files. */
    private IncompleteFileManager incompleteFileManager;
    /** The complete list of files passed to the constructor. */
    private RemoteFileDesc[] allFiles;

    ///////////////////////// Policy Controls ///////////////////////////
    /** The number of tries to make */
    private static final int TRIES=300;
    /** The max number of push tries to make, per host. */
    private static final int PUSH_TRIES=2;
    /** The max number of connections to try in parallel. */
    private static final int PARALLEL_CONNECT=6;
    /** The time to wait trying to establish each connection, in milliseconds.*/
    private static final int CONNECT_TIME=8000;  //8 seconds
    /** The maximum time, in SECONDS, allowed between a push request and an
     *  incoming push connection. */
    private final int PUSH_INVALIDATE_TIME=5*60;  //5 minutes
    /** Returns the amount of time to wait in milliseconds before retrying,
     *  based on tries.  This is also the time to wait for * incoming pushes to
     *  arrive, so it must not be too small.  A value of * tries==0 represents
     *  the first try.  */
    private long calculateWaitTime() {
        //60 seconds: same as BearShare.
        return 60*1000;
    }


    ////////////////////////// Core Variables /////////////////////////////
    /** The files to get, each represented by a RemoteFileDesc.
     *  INVARIANT: files is sorted by priority.
     *  LOCKING: obtain this' monitor */
    private List /* RemoteFileDesc */ files;
    /** If started, the thread trying to coordinate all downloads.  
     *  Otherwise null. */
    private Thread dloaderManagerThread;
    /** The thread actually transferring files, a child of dloadManagerThread, 
     *  or null if waiting for retry, etc. */
    private volatile Thread dloaderThread;
    /** The connection we're using for the current attempt, or last attempt if
     *  we aren't actively downloading.  Or null if we've never attempted a
     *  download. */
    private HTTPDownloader dloader;
    /** True iff this has been forcibly stopped. */
    private boolean stopped;
    /** The list of all files we've requested via push messages.  Only files
     * matching this description may be accepted from incoming connections. */
    private List /* of PushRequestedFile */ requested;


    ///////////////////////// Variables for GUI Display  /////////////////
    /** The current state.  One of Downloader.CONNECTING, Downloader.ERROR,
      *  etc.   Should be modified only through setState. */
    private int state;
    /** The time as returned by Date.getTime() that this entered the current
        state.  Should be modified only through setState. */
    private long stateTime;
    /** The current address we're trying, or last address if waiting, or null
		if unknown. */
    private String lastAddress;
    /** The number of tries we've made.  0 means on the first try. */
    private int tries;


    /**
     * Creates a new ManagedDownload to download the given files.  The download
     * attempts to begin immediately; there is no need to call initialize.
     * Non-blocking.
     *     @param manager the delegate for queueing purposes.  Also the callback
     *      for changes in state.
     *     @param files the list of files to get.  This stops after ANY of the
     *      files is downloaded.
     */
    public ManagedDownloader(DownloadManager manager, RemoteFileDesc[] files) {
        this.allFiles=files;
        incompleteFileManager=new IncompleteFileManager();
        initialize(manager);
    }

    /** See note on serialization at top of file */
    private void writeObject(ObjectOutputStream stream)
            throws IOException {
        stream.writeObject(allFiles);
        stream.writeObject(incompleteFileManager);
    }

    /** See note on serialization at top of file.  You must call initialize on
     *  this!  */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {        
        allFiles=(RemoteFileDesc[])stream.readObject();
        incompleteFileManager=(IncompleteFileManager)stream.readObject();
    }

    /** 
     * Initializes a ManagedDownloader read from disk.  Also used for internally
     * initializing or resuming a normal download; there is no need to
     * explicitly call this method in that case.  After the call, this is in the
     * queued state, at least for the moment.
     *     @requires this is uninitialized or stopped, 
     *      and allFiles, and incompleteFileManager are set
     *     @modifies everything but the above fields */
    public void initialize(DownloadManager manager) {
        this.manager=manager;

        //Sort files by size and store in this.files.  Note that Arrays.sort(..)
        //sorts in ASCENDING order, so we iterate through the array backwards.
        Arrays.sort(allFiles, new RemoteFileDesc.RemoteFileDescComparator());
        this.files=new LinkedList();
        for (int i=allFiles.length-1; i>=0; i--) {
            RemoteFileDesc rfd=allFiles[i];
            this.files.add(rfd);
        }

        this.dloader=null;
        stopped=false;
        requested=new LinkedList();
        setState(QUEUED);
        this.lastAddress=null;
        this.tries=0;
            
        this.dloaderManagerThread=new Thread() {
            public void run() {
                tryAllDownloadsWithRetry();
            }
        };
        dloaderManagerThread.setDaemon(true);
        dloaderManagerThread.start();       
    }

    /**
     * Returns true if 'other' could conflict with one of the files in this.  In
     * other if this.conflicts(other)==true, no other ManagedDownloader should
     * attempt to download other.
     */
    public boolean conflicts(RemoteFileDesc other) {
        synchronized (this) {
            File otherFile=incompleteFileManager.getFile(other);
            //These iterators include any download in progress.
            for (Iterator iter=files.iterator(); iter.hasNext(); ) {
                RemoteFileDesc rfd=(RemoteFileDesc)iter.next();
                File thisFile=incompleteFileManager.getFile(rfd);
                if (thisFile.equals(otherFile))
                    return true;
            }
        }
        return false;
    }

    /**
     * Accepts a push download.  If this chooses to download the given file
     * (with given index and clientGUID) from socket, returns true.  In this
     * case, the caller may not make any modifications to the socket.  If this
     * rejects the given file, returns false without modifying this or socket.
     * If this could has problems with the socket, throws IOException.  In this
     * case the caller should close the socket.  Non-blocking.
     *     @modifies this, socket
     *     @requires GIV string (and nothing else) has been read from socket
     */
    public boolean acceptDownload(
            String file, Socket socket, int index, byte[] clientGUID)
            throws IOException {
        //Authentication: check if we requested file from this host via push.
        //This ignores timestamps.  So first we clear out very old entries.
        RemoteFileDesc rfd=null;   //The original rfd.  See below.
        PushRequestedFile prf=new PushRequestedFile(clientGUID, file, index);
        synchronized (this) {
            purgeOldPushRequests();
            if (! requested.contains(prf))
                return false;

            //Get original RemoteFileDesc to get file size to find temporary
            //file.  Unfortunately the size is NOT available from the GIV line.
            //(That's sort of a flaw of the Gnutella protocol.)  So we search
            //through the list of files in this, comparing file index numbers
            //and client GUID's.  We also check filenames, though this is not
            //strictly needed.
            for (Iterator iter=files.iterator(); iter.hasNext(); ) {
                rfd=(RemoteFileDesc)iter.next();
                if (rfd.getIndex()==index 
                        && rfd.getFileName().equals(file)
                        && (new GUID(rfd.getClientGUID())).
                            equals(new GUID(clientGUID)))
                    break;
            }
            Assert.that(rfd!=null, "No match for supposedly requested file");
        }

        //Authentication ok.  Make and queue downloader.  Notify downloader
        //thread to consume this downloader.  If downloader isn't waiting, this
        //may not be serviced for some time.
        System.out.println("ACCEPT_DOWNLOAD: accepting download");
        final HTTPDownloader downloader=new HTTPDownloader(
            socket, rfd, incompleteFileManager.getFile(rfd));
        final RemoteFileDesc rfdAlias=rfd;  //make compiler happy
        Thread runner=new Thread() {
            public void run() {
                tryOneDownload(downloader, rfdAlias);
            }
         };
        runner.setDaemon(true);
        runner.start();
        return true;
    }

    public synchronized void stop() {
        //This method is tricky.  Look carefully at run.  The most important
        //thing is to set the stopped flag.  That guarantees run will terminate
        //eventually.
        stopped=true;
        //This guarantees any downloads in progress will be killed.  New
        //downloads will not start because of the flag above.
        if (dloader!=null)
            dloader.stop();
        //Interrupt thread if waiting to retry.  This is actually just an
        //optimization since the thread will terminate upon exiting wait.  In
        //fact, this may not do anything at all if the thread is just about to
        //enter the wait!  Still this is nice in case the thread is waiting for
        //a long time.
        if (dloaderManagerThread!=null)
            dloaderManagerThread.interrupt();
    }

    public synchronized boolean resume() throws AlreadyDownloadingException {
        //Ignore request if already in the download cycle.
        if (! (state==WAITING_FOR_RETRY || state==GAVE_UP))
            return false;
        //Sometimes a resume can cause a conflict.  So we check.
        String conflict=this.manager.conflicts(allFiles, this);
        if (conflict!=null)
            throw new AlreadyDownloadingException(conflict);

        if (stopped) {
            //This stopped because all hosts were tried.  (Note that this
            //couldn't have been user aborted.)  Therefore no threads are
            //running in this and it may be safely resumed.
            initialize(this.manager);
        } else {
            //Interrupt any waits.
            if (dloaderManagerThread!=null)
                dloaderManagerThread.interrupt();
        }
        return true;
    }

    public synchronized void launch() {
        //We haven't started yet.
        if (dloader==null)
            return;

        //Unfortunately this must be done in a background thread because the
        //copying (see below) can take a lot of time.  If we can avoid the copy
        //in the future, we can avoid the thread.
        Thread worker=new Thread() {
            public void run() {              
                String name=dloader.getFileName();
                File file=null;

                //a) If the file is being downloaded, create *copy* of
                //incomplete file.  The copy is needed because some programs,
                //notably Windows Media Player, attempt to grab exclusive file
                //locks.  If the download hasn't started, the incomplete file
                //may not even exist--not a problem.
                if (dloader.getAmountRead()<dloader.getFileSize()) {
                    File incomplete=incompleteFileManager.
                        getFile(name, dloader.getFileSize());            
                    file=new File(incomplete.getParent(),
                                  IncompleteFileManager.PREVIEW_PREFIX
                                      +incomplete.getName());
                    if (! CommonUtils.copy(incomplete, file)) //note side-effect
                        return;
                }
                //b) Otherwise, choose completed file.
                else {
					File saveDir = null;
					try {
						saveDir = SettingsManager.instance().getSaveDirectory();
					} catch(java.io.FileNotFoundException fnfe) {
						// simply return if we could not get the save directory.
						return;
					}
					file=new File(saveDir,name);     
				}

                try {
                    Launcher.launchFile(file);
                } catch (IOException e) { }
            }
        };
        worker.setDaemon(true);
        worker.setName("Launcher thread");
        worker.start();
    }


    ///////////////////////////// Core Downloading Logic ///////////////////////

    private static final int SUCCESS=0;
    private static final int DISCONNECTED=-1;
    private static final int WAIT_FOR_RETRY=-2;
    private static final int NO_MORE_LOCATIONS=-3;       

    /** Actually does the download, trying all locations, resuming, waiting, and
     *  retrying as necessary.  Called from dloadManagerThread. */
    private void tryAllDownloadsWithRetry() {
        //Until there are no more downloads left to try...
        for (int i=0; i<TRIES && !stopped; i++) {
            synchronized (ManagedDownloader.this) {
                ManagedDownloader.this.tries=i;
            }

            try {
                int success=tryAllDownloads();
                if (success==SUCCESS) {
                    //a) Success!
                    manager.remove(ManagedDownloader.this, true);
                    return;
                } else if (success==DISCONNECTED) {
                    //b) Need to resume
                    continue;
                } else if (success==WAIT_FOR_RETRY) {
                    //c) All busy.  Wait.
                    setState(WAITING_FOR_RETRY);
                    Thread.sleep(calculateWaitTime());
                    continue;
                } else if (success==NO_MORE_LOCATIONS) {
                    //b) No more files to try
                    break;
                } else {
                    Assert.that(false,
                                "Bad return value from tryAllDownloads"+success);
                }
            } catch (InterruptedException e) {
                if (stopped) {
                    //d) Aborted by user
                    break;
                } else {
                    //e) Manually restarted (resumed) by user
                }
            }
        }

        //No luck.
        if (stopped) {
            //We've been aborted.  Remove this self from active queue or
            //waiting set.
            setState(ABORTED);
            manager.remove(ManagedDownloader.this, false);
        } else {                  
            //Gave up.  No more files or exceeded tries.
            setState(GAVE_UP);
            manager.remove(ManagedDownloader.this, false);
        }
    }


    /**
     * Tries one round of downloading.  Downloads from all locations until all
     * locations fail, one location succeeds, or one location is interrupted.
     * 
     * @return SUCCESS if a file was successfully downloaded.  In rare 
     *             cases, the file may not have been moved to the library.
     *         DISCONNECTED if a download started but was aborted remotely.
     *             This usually means a resume is in order.
     *         WAIT_FOR_RETRY if no file was downloaded, but it makes sense 
     *             to try again later because some hosts reported busy.
     *             The caller should usually wait before retrying.
     *         NO_MORE_LOCATIONS the download attempt failed, and there are 
     *             no more locations to try.
     * @param InterruptedException if the user stop()'ed this download. 
     *  (Calls to resume() do not result in InterruptedException.)
     */
    private int tryAllDownloads() throws InterruptedException {
        //A copy of files.  This lets failed connect threads remove entries
        //from files without causing ConcurrentModificationException.
        LinkedList filesLeft=new LinkedList(files); 

        //While there are more locations to try...
        setState(CONNECTING, filesLeft.size()+" locations");
        while (filesLeft.size()>0 && !stopped) {
            System.out.println("MANAGER: spawning connect");
            //1. Start up to five locations in parallel, asynchronously.
            for (int i=0; i<PARALLEL_CONNECT && filesLeft.size()>0; i++) {
                final RemoteFileDesc rfd=removeBest(filesLeft);
                if (! isPrivate(rfd)) {
                    //a) Normal download
                    final HTTPDownloader downloader=new HTTPDownloader(
                        rfd, incompleteFileManager.getFile(rfd));
                    Thread worker=new Thread() {
                        public void run() {
                            tryOneDownload(downloader, rfd);
                        }
                    };
                    worker.setDaemon(true);
                    worker.start();
                } else {
                    //b) Push download.  Note that no threads are started.
                    //   When (if) the server responds, acceptDownload will
                    //   be called, which will call tryOneDownload.
                    PushRequestedFile prf=
                        new PushRequestedFile(rfd.getClientGUID(), 
                                              rfd.getFileName(),
                                              rfd.getIndex());
                    synchronized (ManagedDownloader.this) {
                        requested.add(prf);
                    }
                    manager.sendPush(rfd);
                }
            }
                
            //2. Wait for a winner to be declared or all to die.  If
            //multiple thread connect, all but the first will terminate
            //themselves.  TODO: we can optimize by adding a
            //while(..)/wait() sequence
            try {
                Thread.sleep(CONNECT_TIME);
            } catch (InterruptedException e) {
                Assert.that(stopped==true,
                            "ManagedDownloader thread interrupted illegally");
                throw new InterruptedException();
            }
                
            //3.  If we got a downloader, wait for it to finish.  Note that
            //we don't obtain a lock here.  If dloaderThread becomes
            //non-null after we check for it, we'll discover this during the
            //next iteration of the loop.  Although we'll start more threads
            //during this time, they will quickly die when they see they are
            //no longer needed.
            System.out.println("MANAGER: waiting for join ");
            if (dloaderThread!=null) {
                try {
                    dloaderThread.join();
                    System.out.println("MANAGER: join complete");
                } catch (InterruptedException e) {
                    Assert.that(stopped==true,
                                "ManagedDownloader thread interrupted illegally");
                    throw new InterruptedException();
                }

                if (stopped)
                    throw new InterruptedException();

                if (dloader.getAmountRead()==dloader.getFileSize())
                    return SUCCESS;
                else {
                    //Interrupted.  Clear winner to allow others a chance.
                    synchronized (ManagedDownloader.this) {
                        dloader=null;
                        dloaderThread=null;
                    }
                    return DISCONNECTED;
                }                    
            }
        }

        //All locations failed.
        if (stopped)
            throw new InterruptedException();
        else if (files.size()>0)
            return WAIT_FOR_RETRY;
        else
            return NO_MORE_LOCATIONS;
    }

    /**
     * Attempts to initializes and run downloader.  Removes rfd from files
     * if the connection attempt failed with anything other than 503 Try
     * Again Later.  Closes this if some other downloader has already
     * started downloading.<p>
     *
     * Typically tryOneDownload is invoked in parallel for a number of
     * candidate locations; at most one will succeed.  These threads
     * are created by dloaderManagerThread.
     * 
     * @param downloader the normal or push downloader to use for the transfer,
     *  which MUST be uninitialized (i.e., downloader.connect() has not been
     *  called)
     * @param rfd the file to download.  This MUST match the name, index, 
     *  address, etc. of downloader.  It is passed in order to remove
     */
    private void tryOneDownload(HTTPDownloader downloader,
                                RemoteFileDesc rfd) {
        System.out.println("    WORKER ("+rfd+"): connecting");
        //Try to connect.
        if (stopped==true)
            return;
        try {
            downloader.connect();
        } catch (TryAgainLaterException e) {
            //Fine.  Leave rfd in the list so we can try later.
            return;
        } catch (IOException e) {
            //Couldn't connect.  Delete this guy from the list of files.
            //TODO: distguish between "can't connect" and other HTTP errors.
            //For "can't connect", try push download or reconnect before
            //giving up.
            synchronized (ManagedDownloader.this) {
                files.remove(rfd);
                return;
            }
        } 

        //Connect succeeded.  Now try to be the "winning" download.
        synchronized (ManagedDownloader.this) {
            if (dloader==null) {
                //Yay! I'm the first download to work.
                dloader=downloader;
                dloaderThread=Thread.currentThread();
            } else {
                //Someone beat me.  Die.
                System.out.println("    WORKER ("+rfd+"): aborting");
                downloader.stop();
                return;
            }
        }

        //I won the race.  Start download.
        System.out.println("    WORKER ("+rfd+"): downloading");
        setState(DOWNLOADING, rfd.getHost());
        try {
            if (stopped==true)
                return;
            dloader.doDownload();
            setState(COMPLETE);
        } catch (FileIncompleteException e) {
            //Retry will be triggered by run() method, presumably from this
            //location. 
        } catch (FileCantBeMovedException e) {
            //Couldn't move to library.  Treat like a success,
            //but put this in a different state.
            setState(COULDNT_MOVE_TO_LIBRARY);
        } catch (IOException e) {
            //Miscellaneous problems.  Might have been stopped by the user.
        } 
        System.out.println("    WORKER ("+rfd+"): terminating");
    }


    /** 
     * Removes and returns the RemoteFileDesc with the smallest estimated
     * remaining download time in filesLeft.  
     *     @requires !filesLeft.isEmpty()
     *     @modifies filesLeft
     */
    public RemoteFileDesc removeBest(List filesLeft) {
        //The best rfd found so far...
        RemoteFileDesc ret=null;
        //...with an estimated download time of "time" seconds.
        long lowestTime=Integer.MAX_VALUE;
        
        for (Iterator iter=filesLeft.iterator(); iter.hasNext(); ) {
            RemoteFileDesc rfd=(RemoteFileDesc)iter.next();
            //The size of the incomplete file for this rfd.  If it doesn't
            //exist, the incompleteFile.length() returns 0, so amountLeft is
            //the full length of the file.
            File incompleteFile=incompleteFileManager.getFile(rfd);
            long amountLeft=rfd.getSize()-incompleteFile.length();
            //The speed of this connection in kiloBYTES/sec.
            long speed=rfd.getSpeed()/8;
            //The estimated time
            long estimatedTime=999999999;
            if (speed != 0)  // Stop dividing by zero bug.
                estimatedTime=amountLeft/speed;

            if (estimatedTime < lowestTime) {
                lowestTime=estimatedTime;
                ret=rfd;
            }
        }
            
        Assert.that(ret!=null, "Precondition to removeBest violated.");
        filesLeft.remove(ret);
        return ret;
    }

    private static boolean isPrivate(RemoteFileDesc rfd) {
        String host=rfd.getHost();
        int port=rfd.getPort();
        return (new Endpoint(host, port)).isPrivateAddress();
    }

    /** Removes all push requests made more than PUSH_INVALIDATE_TIME
     *  seconds ago from the requested list.
     *      @requires this monitor held
     *      @modifies requested */
    private void purgeOldPushRequests() {
        Date time=new Date();
        time.setTime(time.getTime()-(PUSH_INVALIDATE_TIME*1000));
        Iterator iter=requested.iterator();
        while (iter.hasNext()) {
            PushRequestedFile prf=(PushRequestedFile)iter.next();
            if (prf.before(time))
                iter.remove();
        }
    }



    /////////////////////////////   Display Variables ////////////////////////////

    /** Sets this' state, taking care of locking.  lastAddress is only well-defined
     *  for some states and is ignored if null.
     *      @requires newState one of the constants defined in Downloader
     *      @modifies this.state, this.stateTime, this.lastAddress */
    private void setState(int newState, String lastAddress) {
        synchronized (this) {
            this.state=newState;
            if (lastAddress!=null)
                this.lastAddress=lastAddress;
            this.stateTime=(new Date()).getTime();
        }
    }

    /** Sets this' state, taking care of locking.
     *      @requires newState one of the constants defined in Downloader
     *      @modifies this.state, this.stateTime */
    private void setState(int newState) {
        setState(newState, lastAddress);
    }

    /***************************************************************************
     * Accessors that delegate to dloader. Synchronized because dloader can
     * change.
     ***************************************************************************/

    public synchronized int getState() {
        return state;
    }

    public synchronized int getRemainingStateTime() {
        long now=(new Date()).getTime();
        switch (state) {
        case CONNECTING:
            return timeDiff(now, CONNECT_TIME);
        case WAITING_FOR_RETRY:
            return timeDiff(now, calculateWaitTime());
        default:
            return Integer.MAX_VALUE;
        }
    }

    private int timeDiff(long nowTime, long stateLength) {
        long elapsed=nowTime-stateTime;
        long remaining=stateLength-elapsed;
        return (int)Math.max(remaining, 0)/1000;
    }



	public synchronized boolean chatEnabled() {
		if (dloader == null)
			return false;
		else 
			return dloader.chatEnabled();
	}

    public synchronized String getFileName() {
        if (dloader!=null)
            return dloader.getFileName();
        //If we're not actually downloading, we just pick some random name.
        else if (! files.isEmpty())
            return ((RemoteFileDesc)files.get(0)).getFileName();
//          else if (! pushFiles.isEmpty())
//              return ((RFDPushPair)pushFiles.get(0)).rfd.getFileName();
        //The downloader is about to die, but respond anyway.
        else
            return null;
    }

    public synchronized int getContentLength() {
        if (dloader!=null)
            return dloader.getFileSize();
        //If we're not actually downloading, we just pick some random value.
        else if (! files.isEmpty())
            return ((RemoteFileDesc)files.get(0)).getSize();
//          else if (! pushFiles.isEmpty())
//              return ((RFDPushPair)pushFiles.get(0)).rfd.getSize();
        //The downloader is about to die, but respond anyway.
        else
            return 0;
    }

    public synchronized int getAmountRead() {
        if (dloader!=null)
            return dloader.getAmountRead();
        else
            return 0;
    }

    public synchronized String getHost() {
        return lastAddress;
    }

	public synchronized int getPort() {
		if (dloader != null)
			return dloader.getPort();
		else
			return 0;
	}

    public synchronized int getPushesWaiting() {
        return 0;
//          return pushFiles.size();
    }

    public synchronized int getRetriesWaiting() {
        return files.size();
    }
}

/** A RemoteFileDesc and the number of times we've tries to push it. */
class RFDPushPair {
    /** The file to get and its location. */
    final RemoteFileDesc rfd;
    /** The number of times we've already attempted a push. */
    int pushAttempts;

    /** Creates a new RFDPushPair with zero push attempts. */
    public RFDPushPair(RemoteFileDesc rfd) {
        this.rfd=rfd;
        this.pushAttempts=0;
    }

    public String toString() {
        return "<"+rfd.getHost()+", "+rfd.getSpeed()+", "+pushAttempts+">";
    }
}
