package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.URN;

public abstract class AbstractDownloader implements Downloader, Serializable {

	protected static final String ATTRIBUTES = "attributes";
	
	/**
	 * The current priority of this download -- only valid if inactive.
	 * Has no bearing on the download itself, and is used only so that the
	 * download doesn't have to be indexed in DownloadManager's inactive list
	 * every second, for GUI updates.
	 */
	private volatile int inactivePriority;
	
	/**
	 * A map of attributes associated with the download. The attributes
	 * may be used by GUI, to keep some additional information about
	 * the download.
	 */
	protected Map attributes = new HashMap();

	/**
	 * Sets the inactive priority of this download.
	 */
	public void setInactivePriority(int priority) {
	    inactivePriority = priority;
	}

	/**
	 * Gets the inactive priority of this download.
	 */
	public int getInactivePriority() {
	    return inactivePriority;
	}

	/**
	 * Sets a new attribute associated with the download.
	 * The attributes are used eg. by GUI to store some extra
	 * information about the download.
	 * @param key A key used to identify the attribute.
	 * @patam value The value of the key.
	 * @return A prvious value of the attribute, or <code>null</code>
	 *         if the attribute wasn't set.
	 */
	public Object setAttribute(String key, Object value) {
	    return attributes.put( key, value );
	}

	/**
	 * Gets a value of attribute associated with the download.
	 * The attributes are used eg. by GUI to store some extra
	 * information about the download.
	 * @param key A key which identifies the attribue.
	 * @return The value of the specified attribute,
	 *         or <code>null</code> if value was not specified.
	 */
	public Object getAttribute(String key) {
	    return attributes.get( key );
	}

	/**
	 * Removes an attribute associated with this download.
	 * @param key A key which identifies the attribute do remove.
	 * @return A value of the attribute or <code>null</code> if
	 *         attribute was not set.
	 */
	public Object removeAttribute(String key) {
	    return attributes.remove( key );
	}

	public GUID getQueryGUID() {
		return null;
	}
	
	/**
     * Starts the download.
     */
	public abstract void startDownload();
	
	/**
	 * @return whether the download is still alive and cannot be
	 * restarted.
	 */
	public abstract boolean isAlive();
	
	/**
	 * @return whether it makes sense to restart this download.
	 */
	public abstract boolean shouldBeRestarted();
	
	/**
	 * @return whether the download should be removed from 
	 * the waiting list.
	 */
	public abstract boolean shouldBeRemoved();
	
	/**
	 * Handles state changes and other operations while
	 * inactive.
	 */
	public abstract void handleInactivity();
	
	public abstract boolean canBeInQueue();
	
    /**
     * Cleans up any resources before this downloader 
     * is completely disposed.
     */
	public abstract void finish();
	
	public abstract boolean conflicts(URN urn, File fileName, int fileSize);
	
	public abstract boolean conflictsWithIncompleteFile(File incomplete);
	
	public abstract void initialize(DownloadManager m, 
			FileManager fm, DownloadCallback ac);
}
