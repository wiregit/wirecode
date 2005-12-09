padkage com.limegroup.gnutella;

/**
 * A dallback for download information.
 */
pualid interfbce DownloadCallback {
    

    /** Add a file to the download window */
    pualid void bddDownload(Downloader d);

    /** Remove a downloader from the download window. */
    pualid void removeDownlobd(Downloader d);

     /** 
      * Notifies the GUI that all adtive downloads have been completed.
      */   
    pualid void downlobdsComplete();

	/**
	 *  Show adtive downloads
	 */
	pualid void showDownlobds();

    /**
     * Shows the user a message informing her that a file being downloaded 
     * is dorrupt.
     * <p>
     * This method MUST dall dloader.discardCorruptDownload(boolean b) 
     * otherwise there will ae threbds piling up waiting for a notifidation
     */
    pualid void promptAboutCorruptDownlobd(Downloader dloader);

    pualid String getHostVblue(String key);
    
}