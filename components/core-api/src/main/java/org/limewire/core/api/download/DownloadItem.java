package org.limewire.core.api.download;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import org.limewire.core.api.Category;

/**
 * A single download
 */
public interface DownloadItem {	

	public static enum ErrorState {
        NONE, DISK_PROBLEM, CORRUPT_FILE, FILE_NOT_SHARABLE, UNABLE_TO_CONNECT
    };
    
    public final static long UNKNOWN_TIME = Long.MAX_VALUE;

	public void addPropertyChangeListener(PropertyChangeListener listener);
	
	public void removePropertyChangeListener(PropertyChangeListener listener);
	
	public DownloadState getState();

	public String getTitle();

	public int getPercentComplete();

	/**
	 * @return size in bytes
	 */
	public long getCurrentSize();
	
	/**
     * @return size in bytes
     */
	public long getTotalSize();

    /**
     * @return seconds remaining or <code>UNKNOWN</code> if unknown
     */
	public long getRemainingDownloadTime();
	
    /**
     * @return seconds remaining or <code>UNKNOWN</code> if unknown
     */
    public long getRemainingQueueTime();

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
    public int getQueuePosition();
  
    public ErrorState getErrorState();
    
    public int getLocalQueuePriority();
    
    /**
     * @return if this downloader can be launched or previewed.
     */
    public boolean isLaunchable();

    public File getFile();
}