package org.limewire.core.api.download;

import java.beans.PropertyChangeListener;
import java.util.List;



/**
 * A single download
 *
 */
public interface DownloadItem  {	

	public static enum Category {
		VIDEO, AUDIO, DOCUMENT, IMAGE
	};
	
	public static enum ErrorState {
        NONE, DISK_PROBLEM, CORRUPT_FILE, FILE_NOT_SHARABLE, UNABLE_TO_CONNECT
    };

	public void addPropertyChangeListener(PropertyChangeListener listener);
	
	public void removePropertyChangeListener(PropertyChangeListener listener);
	
	public DownloadState getState();

	public String getTitle();

	public int getPercentComplete();

	/**
	 * @return size in bytes
	 */
	public double getCurrentSize();
	
	/**
     * @return size in bytes
     */
	public double getTotalSize();

	public String getRemainingDownloadTime();

	public void cancel();

	public void pause();

	public void resume();

	public int getDownloadSourceCount();

	public List<DownloadSource> getSources();
	
	public Category getCategory();

    /**
     * @return speed in kb/s or 0 if speed could not be measured
     */
	public float getDownloadSpeed();

	/**
     * Returns the position of the download on the uploader, relevant only if
     * the downloader is remote queued.
     */
    int getQueuePosition();
    
    /**
     * Returns an upper bound on the amount of time (in seconds) this will stay 
     * in the current state.  Returns <code>Integer.MAX_VALUE</code> if unknown.
     */
    public String getRemainingStateTime();
    
    public ErrorState getErrorState();

}
