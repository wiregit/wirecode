package org.limewire.core.api.download;

import java.beans.PropertyChangeListener;



/**
 * A single download
 *
 */
public interface DownloadItem  {	

	public static enum Category {
		VIDEO, AUDIO, DOCUMENT, IMAGE
	};

	public void addPropertyChangeListener(PropertyChangeListener listener);
	
	public void removePropertyChangeListener(PropertyChangeListener listener);
	
	public abstract DownloadState getState();

	public abstract String getTitle();

	public abstract int getPercentComplete();

	/**
	 * @return size in bytes
	 */
	public abstract double getCurrentSize();
	
	/**
     * @return size in bytes
     */
	public abstract double getTotalSize();

	public abstract String getRemainingTime();

	public abstract void cancel();

	public abstract void pause();

	public abstract void resume();
		
	public abstract void addDownloadSource(DownloadSource source);

	public abstract int getDownloadSourceCount();

	public abstract DownloadSource getDownloadSource(int index);
	
	public abstract Category getCategory();

}
