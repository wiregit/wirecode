package com.limegroup.gnutella;

import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.sun.java.util.collections.List;

/** 
 * The list of all downloads in progress.  DownloadManager has a fixed number of
 * download slots given by the MAX_SIM_DOWNLOADS property.  It is responsible
 * for starting downloads and scheduling and queing them as needed.
 */
public class DownloadManager {
    /** The callback for notifying the GUI of major changes. */
    private ActivityCallback callback;
    /** The list of all ManagedDownloader's attempting to download.
     *  INVARIANT: active.size()<=slots() && active contains no duplicates */
    private List /* of ManagedDownloader */ active;
    /** The list of all queued ManagedDownloader. 
     *  INVARIANT: waiting contains no duplicates */
    private List /* of ManagedDownloader */ waiting;

    /** 
     * Creates a new empty download manager that will notify callback of any
     * changes. 
     */
    public DownloadManager(ActivityCallback callback) {
        this.callback=callback;
    }

//      public void accept(Socket socket, ...) {
//          for all downloaders in active and waiting...
//                  if (downloader.accept(socket, ...)
//                      return;
//          //I never requested this!
//          close socket forcibly
//      }
                
    /** 
     * Tries to download the given file.  Returns a Downloader that allows you
     * to stop and resume this download.  The download begins immediately, unless
     * it is queued.
     *      @modifies this, disk
     */
    public synchronized Downloader getFile(RemoteFileDesc file) {
        return getFiles(new RemoteFileDesc[] { file });
    }

    /** 
     * Tries to "smart download" any of the given files.  Returns a Downloader
     * that allows you to stop and resume this download.  The download begins
     * immediately, unless it is queued.  It stops after any of the files succeeds.
     *      @modifies this, disk 
     */
    public synchronized Downloader getFiles(RemoteFileDesc[] files) {
        //Start download asynchronously.  This automatically moves downloader to
        //active if it can.
        Assert.that(false, "TODO1: not implemented yet");
        return null;
//          ManagedDownloader downloader=new ManagedDownloader(files, this);
//          waiting.add(downloader);
//          downloader.start();
//          return downloader;
    }   
    
    ////////////////////// Callback Methods for Downloaders ///////////////////

    private boolean hasFreeSlot() {
        SettingsManager settings=SettingsManager.instance();
        return active.size() < settings.getMaxSimDownload();
    }

    /** 
     * Blocks until a download slot has been assigned to downloader.  Throws
     * InterruptedException if the current thread is interrupted while waiting.
     * If InterruptedException is thrown, this is not modified.
     *     @requires downloader queued
     *     @modifies this 
     */
    public synchronized void waitForSlot(ManagedDownloader downloader) 
            throws InterruptedException {
        while (! hasFreeSlot()) 
            wait();
        waiting.remove(downloader);
        active.add(downloader);
    }

    /**
     * Relinquishes downloader's slot.  This is idempotent and non-blocking.
     *     @modifies this
     */
    public synchronized void yieldSlot(ManagedDownloader downloader) {
        active.remove(downloader);
        waiting.add(downloader);
        notify();
    }

    /**
     * Removes downloader entirely from the list of current downloads.
     * Notifies callback of the change in status.
     *     @requires downloader active or queued
     *     @modifies this, callback
     */
    public synchronized void remove(ManagedDownloader downloader, boolean success) {
        boolean activated=active.remove(downloader);
        if (! activated)  //minor optimization.  Always safe to execute both paths.
            waiting.remove(downloader);
        else
            notify();
        //TODO1: callback.removeDownload(downloader);
    }

}
