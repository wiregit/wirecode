package com.limegroup.gnutella;

import org.limewire.bittorrent.Torrent;

/**
 * A callback for download information.
 */
public interface DownloadCallback {
    

    /** Add a file to the download window */
    public void addDownload(Downloader d);

    /** Remove a downloader from the download window. */
    public void removeDownload(Downloader d);

     /** 
      * Notifies the GUI that all active downloads have been completed.
      */   
    public void downloadsComplete();

    /**
     * Warns the user that a file being downloaded is corrupt.
     * <p>
     * This method MUST call dloader.discardCorruptDownload(boolean b) to
     * discard or keep the corrupt file.
     */
    public void promptAboutCorruptDownload(Downloader dloader);
    
    /**
     * Warns the user that a file being previewed could not be scanned for
     * viruses.
     * <p>
     * This method MUST call dloader.discardUnscannedPreview(boolean)
     * to cancel or continue with the preview.
     */
    public void promptAboutUnscannedPreview(Downloader dloader);
    
    /**
     * Prompts the user about what priorities to assign the files in this
     * torrent. Returns true if ok was selected in the end false if cancel.
     */
    public boolean promptTorrentFilePriorities(Torrent torrent);
}