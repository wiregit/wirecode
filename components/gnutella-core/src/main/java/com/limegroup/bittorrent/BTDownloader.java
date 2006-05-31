package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.AbstractDownloader;

/**
 * This class enables the rest of LW to treat this as a regular download.
 */
public class BTDownloader extends AbstractDownloader
implements TorrentLifecycleListener {
	
    private static final ObjectStreamField[] serialPersistentFields = 
    	ObjectStreamClass.NO_FIELDS;
    
	private static final String METAINFO = "metainfo";
	
	private ManagedTorrent _torrent;

	private BTMetaInfo _info;

	private DownloadManager manager;
	
	private long startTime, stopTime;
	
	public BTDownloader(ManagedTorrent torrent, BTMetaInfo info) {
		_torrent = torrent;
		_info = info;
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
		return _torrent.getTotalDownloaded();
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
		_torrent.measureBandwidth();
	}

	public float getMeasuredBandwidth() throws InsufficientDataException {
		return _torrent.getMeasuredBandwidth(true);
	}

	public float getAverageBandwidth() {
		long now = stopTime > 0 ? stopTime : System.currentTimeMillis();
		long runTime = (now - startTime) / 1000;
		return getTotalAmountDownloaded() / runTime;
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
	
	public URN getSHA1Urn() {
		return _torrent.getMetaInfo().getURN();
	}
	
	public int getAmountPending() {
		return _info.getVerifyingFolder().getAmountPending();
	}


	public int getNumHosts() {
		return _torrent.getNumConnections();
	}

	public void torrentComplete(ManagedTorrent t) {
		if (_torrent == t) {
			stopTime = System.currentTimeMillis(); // the download stops now.
			manager.remove(this, true);
		}
	}

	public void torrentStarted(ManagedTorrent t) {
		startTime = System.currentTimeMillis();
		stopTime = 0;
	}

	public void torrentHitRatio(ManagedTorrent t){} // we don't care
	
	public void torrentStopped(ManagedTorrent t) {
		if (_torrent == t) {
			if (stopTime == 0) // do not update the stop time if was completed.
				stopTime = System.currentTimeMillis();
			manager.remove(this, t.isComplete());
		}
	}
	
	private void writeObject(ObjectOutputStream out) 
	throws IOException {
		Map m = new HashMap();
		m.put(ATTRIBUTES, attributes);
		m.put(METAINFO, _info);
		out.writeObject(m);
	}
	
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		Map m = (Map)in.readObject();
		attributes = (Map)m.get(ATTRIBUTES);
		_info = (BTMetaInfo)m.get(METAINFO);
		if (attributes == null || _info == null)
			throw new IOException("invalid serailized data");
		
	}

	public void initialize(DownloadManager manager, 
			FileManager fm, 
			DownloadCallback callback) {
		this.manager = manager;
		_torrent = new ManagedTorrent(_info); 
		_torrent.addLifecycleListener(this);
		BTUploader uploader = new BTUploader(_torrent,_info);
		_torrent.addLifecycleListener(uploader);
	}
	
	public void startDownload() {
		_torrent.start();
	}
	
	public void handleInactivity() {
		// nothing happens when we're inactive
	}
	
	public boolean shouldBeRestarted() {
		return true; // we're always willing to be restarted
	}
	
	public boolean isCancelled() {
		return false; // can't be
	}
	
	public boolean isAlive() {
		return false; // doesn't apply to torrents
	}

	public boolean conflicts(URN urn, String fileName, int fileSize) {
		String myName = _info.getCompleteFile().getName(); //TODO: look over this
		return myName.equals(fileName) && 
			_info.getTotalSize() == fileSize &&
			_info.getURN().equals(urn);
	}

	public boolean conflictsWithIncompleteFile(File incomplete) {
		return false; // we do our own checking for pre-existing incompletes
	}

	public void finish() {
		// nothing, torrents get stopped through the uploader facade only
	}
}
