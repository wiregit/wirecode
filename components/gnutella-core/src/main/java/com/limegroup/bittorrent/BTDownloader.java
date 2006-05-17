package com.limegroup.bittorrent;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;

/**
 * This class enables the GUI to treat a Torrent like any other download. I'm
 * not actually happy with this solution but I don't want to spend time on the GUI
 * 
 * TODO find a proper solution.
 */
public class BTDownloader implements Downloader, TorrentLifecycleListener {
	private final ManagedTorrent _torrent;

	private final BTMetaInfo _info;

	private SimpleBandwidthTracker _tracker;
	
    /**
     * A map of attributes associated with the download. The attributes
     * may be used by GUI, to keep some additional information about
     * the download.
     */
    protected Map attributes = new HashMap();
	
	public BTDownloader(ManagedTorrent torrent, BTMetaInfo info) {
		_torrent = torrent;
		_info = info;
		_tracker = new SimpleBandwidthTracker();
	}

	/**
	 * Stops a torrent download.  If the torrent is in
	 * seeding state, it does nothing.
	 * (To stop a seeding torrent it must be stopped from the
	 * uploads pane)
	 */
	public void stop() {
		if (_torrent.isActive() &&
				_torrent.getState() != ManagedTorrent.SEEDING)
			_torrent.stop();
	}

	public void pause() {
		_torrent.pause();

	}

	public boolean isPaused() {
		return _torrent.isPaused();
	}
	
	public boolean isPausable() {
		return _torrent.isPausable();
	}

	public boolean isInactive() {
		return isResumable() || _torrent.getState() == ManagedTorrent.QUEUED;
	}
	
	public boolean isResumable() {
		switch(_torrent.getState()) {
		case ManagedTorrent.PAUSED:
		case ManagedTorrent.STOPPED:
		case ManagedTorrent.TRACKER_FAILURE:
			return true;
		}
		return false;
	}

	public int getInactivePriority() {
		return RouterService.getTorrentManager().getPositionInQueue(_torrent);
	}

	public boolean resume() {
		return _torrent.resume();
	}

	public File getFile() {
		if (_torrent.isComplete())
			return _info.getCompleteFile();
		return _info.getBaseFile();
	}

	public File getDownloadFragment() {
		// previewing torrents is not so simple, since we are downloading
		// chunks in random order
		if (!_torrent.isComplete())
			return null;
		return getFile();
	}

	/*
	 *  (non-Javadoc)
	 * @see com.limegroup.gnutella.Downloader#getState()
	 * 
	 * Specifically, this maps the states of a torrent
	 * download to the states of a regular download.
	 */
	public int getState() {
		// aborted seeding torrents are shown as complete in the
		// downloads pane.
		if (_torrent.isComplete()) 
			return COMPLETE;
		switch(_torrent.getState()) {
		case ManagedTorrent.WAITING_FOR_TRACKER :
			return WAITING_FOR_RESULTS;
		case ManagedTorrent.VERIFYING:
			return HASHING;
		case ManagedTorrent.CONNECTING:
			return CONNECTING;
		case ManagedTorrent.DOWNLOADING:
			return DOWNLOADING;
		case ManagedTorrent.SAVING:
			return SAVING;
		case ManagedTorrent.SEEDING:
			return COMPLETE;
		case ManagedTorrent.QUEUED:
			return QUEUED;
		case ManagedTorrent.PAUSED:
			return PAUSED;
		case ManagedTorrent.STOPPED:
			if (_torrent.isComplete())
				return COMPLETE;
			else
				return ABORTED;
		case ManagedTorrent.DISK_PROBLEM:
			return DISK_PROBLEM;
		case ManagedTorrent.TRACKER_FAILURE:
			return WAITING_FOR_USER; // let the user trigger a scrape
		case ManagedTorrent.SCRAPING:
			return ITERATIVE_GUESSING; // bad name but practically the same
		}
		throw new IllegalStateException("unknown torrent state");
	}
	
	public long getTotalAmountDownloaded() {
		return _tracker.getTotalAmount();
	}

	/**
	 * We only know how much time we'll be in the state between
	 * tracker requests.
	 */
	public int getRemainingStateTime() {
		if (getState() != Downloader.WAITING_FOR_RESULTS)
			return 0;
		return Math.max(0,(int)(_torrent.getNextTrackerRequestTime() - 
				System.currentTimeMillis()) / 1000);
	}

	public String getFileName() {
		return _info.getName();
	}

	public long getContentLength() {
		return _info.getTotalSize();
	}

	public long getAmountRead() {
		return _info.getVerifyingFolder().getBlockSize();
	}

	public String getVendor() {
		return "BitTorrent";
	}

	public Endpoint getChatEnabledHost() {
		return null;
	}

	public boolean hasChatEnabledHost() {
		return false;
	}

	public void discardCorruptDownload(boolean delete) {
		// we never give up because of corruption
	}

	public RemoteFileDesc getBrowseEnabledHost() {
		return null;
	}

	public boolean hasBrowseEnabledHost() {
		return false;
	}

	public int getQueuePosition() {
		return 1;
	}

	public int getNumberOfAlternateLocations() {
		return getPossibleHostCount();
	}

	public int getNumberOfInvalidAlternateLocations() {
		return _torrent.getNumBadPeers();
	}

	public int getPossibleHostCount() {
		return _torrent.getNumPeers();
	}

	public int getBusyHostCount() {
		return _torrent.getNumBusyPeers();
	}

	public int getQueuedHostCount() {
		int qd = 0;
		for (Iterator iter = _torrent.getConnections().iterator(); iter
				.hasNext();) {
			BTConnection c = (BTConnection) iter.next();
			if (c.isChoking())
				qd++;
		}
		return qd;
	}

	public boolean isCompleted() {
		switch(_torrent.getState()) {
		case ManagedTorrent.SEEDING:
		case ManagedTorrent.STOPPED:
		case ManagedTorrent.DISK_PROBLEM:
		case ManagedTorrent.TRACKER_FAILURE:
			return true;
		}
		return false;
	}

	public long getAmountVerified() {
		return _info.getVerifyingFolder().getVerifiedBlockSize();
	}

	public int getChunkSize() {
		return (int) _info.getPieceLength();
	}

	public long getAmountLost() {
		return _info.getVerifyingFolder().getNumCorruptedBytes();
	}

	public void measureBandwidth() {
		_tracker.measureBandwidth();
	}

	public float getMeasuredBandwidth()  {
		measureBandwidth();
		return _tracker.getMeasuredBandwidth();
	}

	public float getAverageBandwidth() {
		return _tracker.getAverageBandwidth();
	}

	public boolean isRelocatable() {
		return false;
	}

	public void setSaveFile(File saveDirectory, String fileName,
			boolean overwrite) throws SaveLocationException {
		// TODO: decide how to deal with this...
	}

	public File getSaveFile() {
		return _info.getCompleteFile();
	}
	
	void readBytes(int read) {
		_tracker.count(read);
	}

	public URN getSHA1Urn() {
		return _torrent.getMetaInfo().getURN();
	}
	
	public Object removeAttribute(String key){
		return attributes.remove(key);
	}
	
	public Object getAttribute(String key) {
		return attributes.get(key);
	}

	public int getAmountPending() {
		//TODO: this locking isn't good...
		return _info.getVerifyingFolder().getAmountPending();
	}


	public int getNumHosts() {
		return _torrent.getNumConnections();
	}

	public Object setAttribute(String key, Object value) {
		return attributes.put(key, value);
	}

	public void torrentComplete(ManagedTorrent t) {
		if (_torrent == t)
			RouterService.getCallback().removeDownload(this);
	}

	public void torrentStarted(ManagedTorrent t) {
		// nothing
		
	}

	public void torrentStopped(ManagedTorrent t) {
		if (_torrent == t)
			RouterService.getCallback().removeDownload(this);
	}
	
}
