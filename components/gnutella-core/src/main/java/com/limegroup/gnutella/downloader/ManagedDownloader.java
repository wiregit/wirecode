package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import java.util.Date;
import java.io.IOException;
import java.net.*;

/** 
 * A smart download.  Tries to get a group of similar files by delegating
 * to HTTPDownloader objects.  Does retries and resumes automatically.
 * Reports all changes to a DownloadManager.  This class is thread safe.
 */
public class ManagedDownloader implements Downloader {
    /* LOCKING: obtain this's monitor before modifying any of these. */

    /** This' manager for callbacks and queueing. */
    private DownloadManager manager;
    /** The files to get. */
    private RemoteFileDesc[] files;
    
    ///////////////////////// Policy Controls ///////////////////////////
    /** The number of tries to make */
    private static final int TRIES=5;
    /** The amount of time to wait before retrying in milliseconds, This is also
     *  the time to wait for incoming pushes to arrive.  TODO: increase 
     *  exponentially with number of tries.   WARNING: if WAIT_TIME and
     *  CONNECT_TIME are much smaller than the socket's natural timeout,
     *  memory will be consumed since threads don't die! */
    private static final int WAIT_TIME=30000;     //30 seconds
    /** The time to wait trying to establish each connection, in milliseconds.*/
    private static final int CONNECT_TIME=10000;  //8 seconds
    /** The maximum time, in SECONDS, allowed between a push request and an
     *  incoming push connection. */
    private final int PUSH_INVALIDATE_TIME=3*60;  //3 minutes


    ////////////////////////// Core Variables ////////////////////////////
    /** If started, the thread trying to do the downloads.  Otherwise null. */
    private Thread dloaderThread=null;
    /** The connection we're using for the current attempt, or last attempt if
     *  we aren't actively downloading.  Or null if we've never attempted a
     *  download. */
    private HTTPDownloader dloader=null;
    /** True iff this has been forcibly stopped. */
    private boolean stopped=false;
    /** A queue of incoming HTTPDownloaders from push downloads.  Call wait()
     *  and notify() on this if it changes.  Add to tail, remove from head. */
    private List /* of HTTPDownloader */ pushQueue=new LinkedList();
    /** The list of all files we've requested via push messages.  Only files
     * matching this description may be accepted from incoming connections. */
    private List /* of PushRequestedFile */ requested=new LinkedList();


    ///////////////////////// Variables for GUI Display  /////////////////
    /** The current state.  One of Downloader.CONNECTING, Downloader.ERROR,
      *  etc.   Should be modified only through setState. */
    private int state;
    /** The time as returned by Date.getTime() that this entered the current
        state.  Should be modified only through setState. */        
    private long stateTime;
    /** The current address we're trying, or last address if waiting, or null
     *  if unknown. */
    private String lastAddress=null;

    
    /** 
     * Creates a new ManagedDownload to download the given files.  The download
     * attempts to begin immediately.  Non-blocking.
     *     @param manager the delegate for queueing purposes.  Also the callback
     *      for changes in state.
     *     @param files the list of files to get.  This stops after ANY of the
     *      files is downloaded.
     */
    public ManagedDownloader(DownloadManager manager, RemoteFileDesc[] files) {
        this.manager=manager;
        this.files=files;
        setState(QUEUED);

        this.dloaderThread=new Thread(new ManagedDownloadRunner());
        dloaderThread.setDaemon(true);
        dloaderThread.start();
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
        //This ignores timestamps.  So First we clear out very old entries.
        PushRequestedFile prf=new PushRequestedFile(clientGUID, file, index);
        synchronized (this) {
            purgeOldPushRequests();            
            if (! requested.contains(prf))
                return false;
        }

        //Authentication ok.  Make and queue downloader.  Notify downloader
        //thread to consume this downloader.  If downloader isn't waiting, this
        //may not be serviced for some time.
        HTTPDownloader downloader=new HTTPDownloader(file, socket,
                                                     index, clientGUID);
        synchronized (this) {            
            if (stopped) {
                downloader.stop();
                return false;
            }
            pushQueue.add(downloader);
            this.notify();
        }
        return true;
    }
                                                                             
    public synchronized void stop() {
        //This method is tricky.  Look carefully at run.  The most important
        //thing is to set the stopped flag.  That guarantees run will terminate
        //eventually.
        stopped=true;   
        setState(ABORTED);
        //This guarantees any downloads in progress will be killed.  New
        //downloads will not start because of the flag above.
        if (dloader!=null)
            dloader.stop();
        //Interrupt thread if waiting to retry.  This is actually just an
        //optimization since the thread will terminate upon exiting wait.  In
        //fact, this may not do anything at all if the thread is just about to
        //enter the wait!  Still this is nice in case the thread is waiting for
        //a long time.
        if (dloaderThread!=null)
            dloaderThread.interrupt();        
        //Relinquish slot immediately.  Always safe.  Should be done after call
        //to interrupt in case download thread is about to call waitForSlot.
        manager.yieldSlot(this);   
        //Notify callback.
        manager.remove(this, false);
    }

//      public synchronized void resume() {
//          Assert.that(false, "TODO1: not implemented");
//      }

    /** Actually does the download. */
    private class ManagedDownloadRunner implements Runnable {
        public void run() {
            //Many policies are possible.  This is just one. The great
            //simplification is that incoming push downloads are only accepted
            //while waiting to retry.  Otherwise they are queued and handled
            //later.
            try {
                boolean success;
                for (int i=0; i<TRIES && !stopped; i++) {
                    //1. Try all normal downloads first.  Exit if success.
                    success=tryNormalDownloads();
                    if (success) {
                        manager.remove(ManagedDownloader.this, true);
                        return;
                    }
                    
                    //2. Send pushes for those hosts that we couldn't connect to.
                    //   But only try this twice.
                    if (i<2)
                        sendPushes();

                    //3. Wait a while before retrying.  Accept any pushes.
                    success=waitForPushDownloads();
                    if (success) {
                        manager.remove(ManagedDownloader.this, true);
                        return;
                    }
                }
                //We've failed.  Notify my manager.
                setState(GAVE_UP);
                manager.remove(ManagedDownloader.this, false);            
            } catch (InterruptedException e) {
                //We've been stopped.  No need to use callback, since stop calls
                //remove.
                return;
            } finally {
                //Clean up any queued push downloads.  Also clean up dloader if
                //still existing.  This isn't needed if an IOException thrown by
                //Downloader.start means stop need not be called.  But better to
                //be safe.
                synchronized (ManagedDownloader.this) {
                    stopped=true;
                    for (Iterator iter=pushQueue.iterator(); iter.hasNext(); ) 
                        ((HTTPDownloader)iter.next()).stop();
                    if (dloader!=null)
                        dloader.stop();
                }
            }           
        }

        /** Tries download from all locations.  Blocks waiting for a download
         *  slot first.  Returns true iff a location succeeded.  Throws
         *  InterruptedException if a call to stop() is detected.  */
        private boolean tryNormalDownloads() throws InterruptedException {            
            try {
                setState(QUEUED);
                manager.waitForSlot(ManagedDownloader.this);
                for (int j=0; j<files.length; j++) {                
                    RemoteFileDesc rfd=files[j];                
                    //As an optimization, we can skip normal download attempts
                    //from private IP addresses.  If the host is on the same
                    //private network as us, a push download should work anyway.
                    //If the host is on a different network, this wouldn't
                    //possibly work.  TODO2: look at push flag here as well.
                    if (isPrivate(rfd))
                        continue;
                    try {
                        //Make a new downloader.  Only actually start it if this
                        //is still wanted.  The construction of the downloader
                        //cannot go in the synchronized statement since it may
                        //be blocking.
                        setState(CONNECTING, rfd.getHost());
                        HTTPDownloader dloader2=new HTTPDownloader(
                            rfd.getFileName(), rfd.getHost(), rfd.getPort(),
                            rfd.getIndex(), rfd.getClientGUID(),
                            rfd.getSize(), false, CONNECT_TIME);
                        synchronized (ManagedDownloader.this) {
                            if (stopped) {
                                dloader2.stop();
                                throw new InterruptedException();
                            }
                            dloader=dloader2;
                            setState(DOWNLOADING);
                        }
                        dloader.start();
                        setState(COMPLETE);
                        return true;
                    } catch (IOException e) {
                        //Was this forcibly closed?  Or was it a normal IO
                        //problem?  TODO2: if this is interrupted, retry same
                        //file.
                        synchronized (ManagedDownloader.this) {
                            if (stopped)
                                throw new InterruptedException();
                        }
                    }
                }
                return false;
            } finally {
                manager.yieldSlot(ManagedDownloader.this);         
            }
        }

        /** Sends pushes to all private locations. */
        private void sendPushes() {
            //Just to ensure memory is bounded, remove old push request from
            //list.  This is different than requested.clear()!
            synchronized (ManagedDownloader.this) {
                purgeOldPushRequests();
            }
            //TODO2: don't send pushes to hosts that we connected to above
            //but couldn't download from.
            for (int j=0; j<files.length; j++) {
                RemoteFileDesc rfd=files[j];                
                PushRequestedFile prf=new PushRequestedFile(rfd.getClientGUID(),
                                                            rfd.getFileName(),
                                                            rfd.getIndex());
                synchronized (ManagedDownloader.this) {
                    //Mark that we requested the file, for authentication
                    //purposes.
                    requested.add(prf);
                }
                manager.sendPush(rfd);
            }
        }

        /** Waits at least WAIT_TIME seconds.  If any push downloads come in,
         * handle them, acquiring a download slot first.  Return true if one of
         * these downloads is successful.  Throws InterruptedException if a call
         * to stop() is detected. */
        private boolean waitForPushDownloads() throws InterruptedException {
            Date start=new Date();
            //Repeat until time has expired...
            while (true) {
                //1. Wait for downloader.  Time is calculated as needed.
                synchronized (ManagedDownloader.this) {
                    setState(WAITING_FOR_RETRY);
                    while (pushQueue.isEmpty()) {
                        Date now=new Date();
                        long elapsed=now.getTime()-start.getTime();                     
                        long waitTime=WAIT_TIME-elapsed;
                        if (waitTime<=0)
                            return false;
                        ManagedDownloader.this.wait(waitTime);
                    }
                    dloader=(HTTPDownloader)pushQueue.remove(0);                    
                }

                //2. Try download.  We may need to wait for a download slot
                //first.  Ideally we'd preempt some other download, but that is
                //complex.
                try {
                    setState(QUEUED);
                    manager.waitForSlot(ManagedDownloader.this);                    
                    setState(DOWNLOADING,
                             dloader.getInetAddress().getHostAddress());
                    dloader.start();
                    setState(COMPLETE);
                    return true;
                } catch (IOException e) {
                    //Was this forcibly closed?  Or was it a normal IO problem?
                    //TODO2: if this is interrupted, retry same file.
                    synchronized (ManagedDownloader.this) {
                        if (stopped)
                            throw new InterruptedException();
                    }
                } finally {
                    manager.yieldSlot(ManagedDownloader.this); 
                }
            }                    
        }        
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
        synchronized (requested) {
            Iterator iter=requested.iterator();
            while (iter.hasNext()) {
                PushRequestedFile prf=(PushRequestedFile)iter.next();
                if (prf.before(time))
                    iter.remove();
            }
        }
    }

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
            return timeDiff(now, WAIT_TIME);
        default:
            return Integer.MAX_VALUE;                
        }
    }
        
    private int timeDiff(long nowTime, long stateLength) {
        long elapsed=nowTime-stateTime;
        long remaining=stateLength-elapsed;
        return (int)Math.max(remaining, 0)/1000;
    }

    public synchronized String getFileName() {
        if (dloader!=null)
            return dloader.getFileName();
        else
            return files[0].getFileName();  //better than nothing
    }

    public synchronized int getContentLength() {
        if (dloader!=null)
            return dloader.getFileSize();
        else
            return files[0].getSize();     //better than nothing
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
}

