package com.limegroup.bittorrent;

import java.util.List;

interface Torrent {

	/**
	 * States of a torrent.  Some of them are functionally equivalent
	 * to Downloader states.
	 */
	public enum TorrentState {
	WAITING_FOR_TRACKER,
	VERIFYING,
	CONNECTING,
	DOWNLOADING,
	SAVING,
	SEEDING,
	QUEUED,
	PAUSED,
	STOPPED,
	DISK_PROBLEM,
	TRACKER_FAILURE,
	SCRAPING //scraping == requesting from tracker
	};
	
	/**
	 * @return true if the torrent is complete.
	 */
	public abstract boolean isComplete();

	/**
	 * Starts the torrent 
	 */
	public abstract void start();

	/**
	 * Stops the torrent
	 */
	public abstract void stop();

	public void measureBandwidth();

	public float getMeasuredBandwidth(boolean downstream);
	
	public boolean isActive();
	
	public TorrentState getState();
	
	public long getNextTrackerRequestTime();

	public List<BTLink> getConnections();

	public int getNumConnections();
	
	public int getTriedHostCount();

	public int getNumPeers();

	public int getNumNonInterestingPeers();
	
	public int getNumChockingPeers();

	public long getTotalDownloaded();
	
	public long getAmountLost();
	
	/**
	 * Resumes the torrent.
	 */
	public abstract boolean resume();

	/**
	 * @return true if paused
	 */
	public boolean isPaused();
	
	public boolean isPausable();
	
	/**
	 * Pauses the torrent.
	 */
	public abstract void pause();
}