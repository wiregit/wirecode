package com.limegroup.bittorrent;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
public class BTDownloader implements Downloader {
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
		if (!_torrent.hasStopped() && !_torrent.isComplete())
			_torrent.stop();
	}

	public void pause() {
		_torrent.pause();

	}

	public boolean isPaused() {
		return _torrent.isPaused();
	}

	public boolean isInactive() {
		return _torrent.isPaused() || 
		_torrent.hasStopped() || 
		_torrent.isComplete();
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

	public int getState() {
		return _torrent.getState();
	}
	
	public long getTotalAmountDownloaded() {
		return _tracker.getTotalAmount();
	}

	public int getRemainingStateTime() {
		// we don't have anything like that.
		return 0;
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

	public Iterator getHosts() {
		Set ret = new HashSet();
		for (Iterator iter = _torrent.getConnections().iterator(); iter
				.hasNext();) {
			BTConnection btc = (BTConnection) iter.next();
			if (! btc.isChoking())
				ret.add(btc.getEndpoint());
		}
		return ret.iterator();
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
		return _torrent.getNumAltLocs();
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
		return _torrent.isComplete();
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
}
