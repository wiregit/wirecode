package com.limegroup.bittorrent;

/**
 * A listener to the events in the life of a torrent
 */
public interface TorrentLifecycleListener {
	/**
	 * Notification that a torrent has started
	 */
	public void torrentStarted(ManagedTorrent t);
	
	/**
	 * Notification that the torrent has finished downloading 
	 * and is now seeding
	 */
	public void torrentComplete(ManagedTorrent t);
	
	/**
	 * Notification that the torrent has stopped
	 */
	public void torrentStopped(ManagedTorrent t);
}
