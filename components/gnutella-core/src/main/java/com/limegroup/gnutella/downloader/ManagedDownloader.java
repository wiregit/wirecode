package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import java.io.IOException;

/** 
 * A smart download.  Tries to get a group of similar files by delegating
 * to HTTPDownloader objects.  Does retries and resumes automatically.
 * Reports all changes to a DownloadManager.
 */
public class ManagedDownloader implements Downloader {
    /* LOCKING: obtain this's monitor before modifying any of these. */

    /** This' manager for callbacks and queueing. */
    private DownloadManager manager;
    /** The files to get. */
    private RemoteFileDesc[] files;
    /** The number of tries to make */
    private static final int TRIES=5;

    /** If started, the thread trying to do the downloads.  Otherwise null. */
    private Thread dloaderThread=null;
    /** The connection we're using for the current attempt, or last attempt if
     *  we aren't actively downloading.  Or null if we've never attempted a
     *  download. */
    private HTTPDownloader dloader=null;
    /** True iff this has been forcibly stopped. */
    private boolean stopped=false;


    
    /** 
     * Creates a new ManagedDownload to download the given files.  The download
     * doesn't actually happen until the start method is called.
     *     @param manager the delegate for queueing purposes.  Also the callback
     *      for changes in state.
     *     @param files the list of files to get.  This stops after ANY of the
     *      files is downloaded.
     */
    ManagedDownloader(DownloadManager manager, RemoteFileDesc[] files) {
        this.manager=manager;
        this.files=files;
    }

//      boolean accept(Socket, ...) {
//          if (current==null || current.notProgressing) {
//              downloader=new HTTPDownloader(current);
            
//          }
//      }
                                                                           

    /** 
     * Starts this.  Non-blocking.
     *    @requires this not started.
     *    @modifies this 
     */
    public synchronized void start() {
        this.dloaderThread=new Thread(new ManagedDownloadRunner());
        dloaderThread.setDaemon(true);
        dloaderThread.start();
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
        if (dloaderThread!=null)
            dloaderThread.interrupt();        
        //Relinquish slot immediately.  Always safe.  Should be done after call to
        //interrupt in case download thread is about to call waitForSlot.
        manager.yieldSlot(this);   
        //Notify callback.
        manager.remove(this, false);
    }

    public synchronized void resume() {
        Assert.that(false, "TODO1: not implemented");
    }

    /** Actually does the download. */
    private class ManagedDownloadRunner implements Runnable {
        public void run() {
            try {
                //Many policies are possible.  This is just one: repeatedly try all
                //addresses.  The only catch is that a call to stop must cause this
                //to die relatively quickly; see stop() for a detailed discussion.
                for (int i=0; i<TRIES && !stopped; i++) {
                    manager.waitForSlot(ManagedDownloader.this);
                    //While I've got a slot, try all locations.
                    for (int j=0; j<files.length && !stopped; j++) {
                        RemoteFileDesc rfd=files[j];                    
                        try {
                            //Make a new downloader.  Only actually start it if
                            //this is still wanted.  The construction of the
                            //downloader cannot go in the synchronized statement
                            //since it may be blocking. 
                            HTTPDownloader dloader2=new HTTPDownloader(
                                rfd.getFileName(), rfd.getHost(), rfd.getPort(),
                                rfd.getIndex(), rfd.getClientGUID(),
                                rfd.getSize(), false);
                            synchronized (ManagedDownloader.this) {
                                if (stopped)
                                    return;
                                dloader=dloader2;
                            }
                            dloader.start();
                            manager.remove(ManagedDownloader.this, true);
                            return;
                        } catch (IOException e) {
                            if (stopped)
                                return;
                            //No luck.  Try another one.
                        }
                    }                   

                    //TODO1: send pushes.

                    //No luck?  Give up slot, wait, and try again.
                    manager.yieldSlot(ManagedDownloader.this);                
                    Thread.sleep(10000);  //10 sec
                }
                manager.remove(ManagedDownloader.this, false);
            } catch (InterruptedException e) {
                //No need to use callback, since it means we've been forcibly
                //stopped.  And stop() calls remove(..., false).
                return;
            }
        }
    }

    /***************************************************************************
     * Accessors that delegate to dloader. Synchronized because dloader can
     * change.  
     ***************************************************************************/

    public synchronized int getAmountRead() {
        return dloader.getAmountRead();
    }
        
    public synchronized String getFileName() {
        return dloader.getFileName();
    }

    public synchronized int getState() {
        Assert.that(false, "TODO1: Not implemented");
        return 0;
    }
}
