package com.limegroup.gnutella;

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
	 *  Show active downloads
	 */
	public void showDownloads();

    /**
     * Shows the user a message informing her that a file being downloaded 
     * is corrupt.
     * <p>
     * This method MUST call dloader.discardCorruptDownload(boolean b) 
     * otherwise there will be threads piling up waiting for a notification
     */
    public void promptAboutCorruptDownload(Downloader dloader);

    public String getHostValue(String key);
    
}