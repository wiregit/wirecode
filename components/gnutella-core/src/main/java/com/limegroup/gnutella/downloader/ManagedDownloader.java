package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import java.util.Date;
import java.io.IOException;
import java.net.*;

/** 
 * A smart download.  Tries to get a group of similar files by delegating
 * to HTTPDownloader objects.  Does retries and resumes automatically.
 * Reports all changes to a DownloadManager.  This class is thread safe.<p>
 *
 * Smart downloads can use many policies, and these policies are free to
 * change as allowed by the Downloader specification.  This implementation
 * uses the following policy:
 *
 * <ol>
 *   <li>Try normal downloads from all locations that are not private and
 *       do not have the push flag set.  Try these one at a time, from
 *       fastest to slowest.
 *   <li>Send push requests to any N hosts that we weren't reached above,
 *       in parallel.  N is currently 6, but typically fewer pushes are 
 *       typically sent.  If N is small, this could be prioritized too.
 *   <li>Wait a fixed amount of time for pushes to come in.   The first
 *       push to come in (if any!) is handled.  This wait time is fairly
 *       large, as it behaves as the retry window.
 *   <li>If all of the above fails, repeat the whole process.  However
 *       after two failed push attempts, never try a host again.
 * </ol>
 *
 * Note that the whole process will be repeated as long as we get the busy
 * signal from a host.
 */
public class ManagedDownloader implements Downloader {
    /* LOCKING: obtain this's monitor before modifying any of these. */

    /** This' manager for callbacks and queueing. */
    private DownloadManager manager;
    /** The files to get, each represented by a RemoteFileDesc. 
     *  INVARIANT: files is sorted by priority. */
    private List /* RemoteFileDesc */ files;
    /** The files to get by pushes, each represented by a RFDPushPair.
     *  These are not sorted.  They contain former members of files. */
    private List /* of RFDPushPair */ pushFiles=new LinkedList();
    
    ///////////////////////// Policy Controls ///////////////////////////
    /** The number of tries to make */
    private static final int TRIES=100;
    /** The max number of push tries to make, per host. */
    private static final int PUSH_TRIES=2;
    /** The max number of pushes to send in parallel. */
    private static final int PARALLEL_PUSH=6;
    /** The amount of time to wait before retrying in milliseconds, This is also
     *  the time to wait for incoming pushes to arrive.  TODO: increase 
     *  exponentially with number of tries.   WARNING: if WAIT_TIME and
     *  CONNECT_TIME are much smaller than the socket's natural timeout,
     *  memory will be consumed since threads don't die! */
    private static final int WAIT_TIME=30000;    //30 seconds
    /** The time to wait trying to establish each connection, in milliseconds.*/
    private static final int CONNECT_TIME=8000;  //8 seconds
    /** The maximum time, in SECONDS, allowed between a push request and an
     *  incoming push connection. */
    private final int PUSH_INVALIDATE_TIME=5*60;  //5 minutes


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

        //Sort files by size and store in this.files.  Note that Arrays.sort(..)
        //sorts in ASCENDING order, so we iterate through the array backwards.
        Arrays.sort(files, new RemoteFileDesc.RemoteFileDescComparator());        
        this.files=new LinkedList();
        for (int i=files.length-1; i>=0; i--)
            this.files.add(files[i]);       

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
                    //Nothing left to do?
                    if (files.size()==0 && pushFiles.size()==0)
                        break;
                    
                    //1. Try all normal downloads first.  Exit if success.
                    success=tryNormalDownloads();
                    if (success) {
                        manager.remove(ManagedDownloader.this, true);
                        return;
                    }
                    
                    //2. Send pushes for those hosts that we couldn't connect to.
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
         *  InterruptedException if a call to stop() is detected.  
         *      @modifies this.files, this.pushFiles, network  */
        private boolean tryNormalDownloads() throws InterruptedException {   
            try {
                setState(QUEUED);
                manager.waitForSlot(ManagedDownloader.this);
                for (Iterator iter=files.iterator(); iter.hasNext(); ) {
                    RemoteFileDesc rfd=(RemoteFileDesc)iter.next();                
                    //As an optimization, we can skip normal download attempts
                    //from private IP addresses.  If the host is on the same
                    //private network as us, a push download should work anyway.
                    //If the host is on a different network, this wouldn't
                    //possibly work.  TODO2: look at push flag here as well.
                    if (isPrivate(rfd)) {
                        iter.remove();
                        pushFiles.add(new RFDPushPair(rfd));
                        continue;
                    }
                    try {
                        //Make a new downloader.  Only actually start it if this
                        //is still wanted.  The construction of the downloader
                        //cannot go in the synchronized statement since it may
                        //be blocking.
                        setState(CONNECTING, rfd.getHost());
//                          System.out.println("Trying normal download from "
//                                             +rfd.getHost());
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
                    } catch (TryAgainLaterException e) {
                        //Go on to next one.  We will try normal attempt later.
//                          System.out.println("    Got busy signal.");
                        continue;
                    } catch (IOException e) {
                        //Was this forcibly closed? 
                        synchronized (ManagedDownloader.this) {
                            if (stopped)
                                throw new InterruptedException();
                        }
                        //Nope, it was a normal IO problem.  Try push later.
                        //TODO2: if this is interrupted, retry same file.
                        iter.remove();  //Don't call files.remove(..)!
                        pushFiles.add(new RFDPushPair(rfd));
                    }
                }
                return false;
            } finally {
                manager.yieldSlot(ManagedDownloader.this);         
            }
        }

        /** Sends pushes to those locations we couldn't connect to above. 
         *      @modifies this.pushFiles, network */
        private void sendPushes() {
            //Just to ensure memory is bounded, remove old push request from
            //list.  This is different than requested.clear()!
            synchronized (ManagedDownloader.this) {
                purgeOldPushRequests();
            }
            //For each file that needs a push.  Just to be safe we limit
            //the number of pushes sent at any time.
            Iterator iter=pushFiles.iterator();
            for (int i=0; i<PARALLEL_PUSH && iter.hasNext(); i++) {
                //Send the push. Mark that we requested the file, for
                //authentication purposes.
                RFDPushPair pair=(RFDPushPair)iter.next();
                RemoteFileDesc rfd=pair.rfd;
                PushRequestedFile prf=new PushRequestedFile(rfd.getClientGUID(),
                                                            rfd.getFileName(),
                                                            rfd.getIndex());
                synchronized (ManagedDownloader.this) {
                    requested.add(prf);
                }
                manager.sendPush(rfd);
//                  System.out.println("Sending push to "+rfd.getHost());

                //Increment and check attempts variable.
                pair.pushAttempts++;
                if (pair.pushAttempts>=PUSH_TRIES) {
                    iter.remove();   //can't call pushFiles.remove(..)
                    continue;
                }          
                
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
        else if (! files.isEmpty())
            return ((RemoteFileDesc)files.get(0)).getFileName();
        else
            return null;
    }

    public synchronized int getContentLength() {
        if (dloader!=null)
            return dloader.getFileSize();
        else if (! files.isEmpty())
            return ((RemoteFileDesc)files.get(0)).getSize();
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
