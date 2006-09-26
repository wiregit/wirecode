package com.limegroup.bittorrent;

import java.util.List;

interface Torrent {

	/**
	 * States of a torrent.  Some of them are functionally equivalent
	 * to Downloader states.
	 */
	static final int WAITING_FOR_TRACKER = 1;
	static final int VERIFYING = 2;
	static final int CONNECTING = 3;
	static final int DOWNLOADING = 4;
	static final int SAVING = 5;
	static final int SEEDING = 6;
	static final int QUEUED = 7;
	static final int PAUSED = 8;
	static final int STOPPED = 9;
	static final int DISK_PROBLEM = 10;
	static final int TRACKER_FAILURE = 11;
	static final int SCRAPING = 12; //scraping == requesting from tracker
	
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
	
	public int getState();
	
	public long getNextTrackerRequestTime();

	public List<BTLink> getConnections();

	public int getNumConnections();

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