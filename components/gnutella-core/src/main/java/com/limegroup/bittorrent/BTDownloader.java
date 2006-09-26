package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.AbstractDownloader;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.util.NumericBuffer;

/**
 * This class enables the rest of LW to treat this as a regular download.
 */
public class BTDownloader extends AbstractDownloader 
implements TorrentEventListener {
	
	private static final long serialVersionUID = -7785186190441081641L;

	private static final ObjectStreamField[] serialPersistentFields = 
    	ObjectStreamClass.NO_FIELDS;
    
	private static final String METAINFO = "metainfo";

	/**
	 * The <tt>ManagedTorrent</tt> instance this is representing
	 */
	private Torrent _torrent;

	/**
	 * The <tt>BTMetaInfo</tt> for this torrent.
	 */
	private BTMetaInfo _info;
	
	/** Local ref to the urn */
	private final URN urn;
	
	/**
	 * Object containing 
	 */
	private TorrentFileSystem fileSystem;

	/**
	 * Handle to the <tt>DownloadManager</tt> for adding, removing, etc.
	 */
	private DownloadManager manager;
	
	/**
	 * Handle to the incomplete file manager for saving crash state
	 */
	private IncompleteFileManager ifm;
	
	private volatile long startTime, stopTime;
	
	private NumericBuffer<Float> averagedBandwidth = 
		new NumericBuffer<Float>(10);
	
	public BTDownloader(BTMetaInfo info) {
		_info = info;
		urn = info.getURN();
		fileSystem = info.getFileSystem();
		propertiesMap.put(METAINFO, info);
		propertiesMap.put(DEFAULT_FILENAME, info.getName());
	}
	
	/**
	 * Stops a torrent download.  If the torrent is in
	 * seeding state, it does nothing.
	 * (To stop a seeding torrent it must be stopped from the
	 * uploads pane)
	 */
	public void stop() {
		if (_torrent.isActive() &&
				_torrent.getState() != ManagedTorrent.SEEDING) {
			_torrent.stop();
		} else if (isInactive()) 
			manager.remove(this, true);
			
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
	
	public boolean isLaunchable() {
		return fileSystem.getFiles().size() == 1 && _torrent.isComplete();
	}
	
	public boolean isResumable() {
		switch(_torrent.getState()) {
		case ManagedTorrent.PAUSED:
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
			return fileSystem.getCompleteFile();
		return fileSystem.getBaseFile();
	}

	public File getDownloadFragment() {
		if (!isLaunchable())
			throw new IllegalStateException("can't preview this torrent");
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
		return fileSystem.getName();
	}

	public long getContentLength() {
		return fileSystem.getTotalSize();
	}

	public long getAmountRead() {
		return _info != null ?_info.getDiskManager().getBlockSize() :
			getContentLength();
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
		return 0; // not applicable to torrents
	}

	public int getPossibleHostCount() {
		return _torrent.getNumPeers();
	}

	public int getBusyHostCount() {
		return _torrent.getNumNonInterestingPeers();
	}

	public int getQueuedHostCount() {
		return _torrent.getNumChockingPeers();
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
	
	public boolean shouldBeRemoved() {
		switch(_torrent.getState()) {
		case ManagedTorrent.DISK_PROBLEM:
		case ManagedTorrent.SEEDING:
			return true;
		}
		return false;
	}

	public long getAmountVerified() {
		return _info.getDiskManager().getVerifiedBlockSize();
	}

	public int getChunkSize() {
		return (int) _info.getPieceLength();
	}

	public long getAmountLost() {
		return _torrent.getAmountLost();
	}

	public void measureBandwidth() {
		_torrent.measureBandwidth();
		averagedBandwidth.add(_torrent.getMeasuredBandwidth(true));
	}

	public float getMeasuredBandwidth() throws InsufficientDataException {
		if (averagedBandwidth.size() < 3)
			throw new InsufficientDataException();
		return averagedBandwidth.average().floatValue();
	}

	public float getAverageBandwidth() {
		long now = stopTime > 0 ? stopTime : System.currentTimeMillis();
		long runTime = now - startTime ;
		return runTime > 0 ? getTotalAmountDownloaded() / runTime : 0;
	}

	public boolean isRelocatable() {
		return !isCompleted();
	}

	public void setSaveFile(File saveDirectory, String fileName,
			boolean overwrite) throws SaveLocationException {
		super.setSaveFile(saveDirectory, fileName, overwrite);
		// if this didn't throw target is ok.
		fileSystem.setCompleteFile(new File(saveDirectory, fileName));
	}

	public File getSaveFile() {
		return fileSystem.getCompleteFile();
	}
	
	public URN getSHA1Urn() {
		return urn;
	}
	
	public int getAmountPending() {
		return _info.getDiskManager().getAmountPending();
	}


	public int getNumHosts() {
		return _torrent.getNumConnections();
	}

	public void handleTorrentEvent(TorrentEvent evt) {
		if (evt.getTorrent() != _torrent)
			return;
		
		switch(evt.getType()) {
		case STARTED : torrentStarted(); break;
		case COMPLETE : torrentComplete(); break;
		case STOPPED : torrentStopped(); break;
		}
	}
	
	private void torrentComplete() {
		// the download stops now. even though the torrent goes on
		stopTime = System.currentTimeMillis();
		ifm.removeTorrentEntry(_info.getURN());
		manager.remove(this, true);
	}

	private void torrentStarted() {
		startTime = System.currentTimeMillis();
		stopTime = 0;
	}

	private void torrentStopped() {
		if (stopTime == 0) {
			averagedBandwidth.clear();
			boolean resumable = isResumable();
			stopTime = System.currentTimeMillis();
			manager.remove(this, !resumable);
		} // otherwise torrent was already completed.
		else Assert.that(_torrent instanceof FinishedTorrentDownload);
	}
	
	private void writeObject(ObjectOutputStream out) 
	throws IOException {
		Map<String, Serializable> m = new HashMap<String, Serializable>();
		synchronized(this) {
			m.putAll(propertiesMap);
		}
		Assert.that(m.containsKey(METAINFO));
		out.writeObject(m);
	}
	
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		propertiesMap = (Map<String, Serializable>)in.readObject();
		attributes = (Map)propertiesMap.get(ATTRIBUTES);
		_info = (BTMetaInfo)propertiesMap.get(METAINFO);
		fileSystem = _info.getFileSystem();
		if (attributes == null || _info == null)
			throw new IOException("invalid serailized data");
		averagedBandwidth = new NumericBuffer<Float>(10);
		
	}

	public void initialize(DownloadManager manager, 
			FileManager fm, 
			DownloadCallback callback) {
		this.manager = manager;
		ifm = manager.getIncompleteFileManager();
		TorrentManager torrentManager = RouterService.getTorrentManager();
		_torrent = new ManagedTorrent(_info, torrentManager); 
		torrentManager.addEventListener(this);
		ifm.addTorrentEntry(_info.getURN());
	}
	
	public void startDownload() {
		BTUploader uploader = new BTUploader((ManagedTorrent)_torrent,
				_info, 
				RouterService.getTorrentManager());
		_torrent.start();
	}
	
	public void handleInactivity() {
		// nothing happens when we're inactive
	}
	
	public boolean shouldBeRestarted() {
		return getState() == QUEUED && RouterService.getTorrentManager().allowNewTorrent(); 
	}
	
	public boolean isAlive() {
		return false; // doesn't apply to torrents
	}

	public boolean canBeInQueue() {
		return !isResumable();
	}
	
	public boolean conflicts(URN urn, int fileSize, File... file) {
		if (_info.getURN().equals(urn))
			return true;
		for (File f : file) {
			if (conflictsSaveFile(f))
				return true;
		}
		return false;
	}
	
	public boolean conflictsSaveFile(File candidate) {
		return fileSystem.conflicts(candidate);
	}

	public boolean conflictsWithIncompleteFile(File incomplete) {
		return fileSystem.conflictsIncomplete(incomplete); 
	}

	public void finish() {
		RouterService.getTorrentManager().removeEventListener(this);
		_torrent = new FinishedTorrentDownload(_torrent);
		_info = null;
		synchronized(this) {
			propertiesMap.remove(METAINFO);
		}
	}
	
	public String toString() {
		return "downloader facade for "+fileSystem.getCompleteFile().getName();
	}
}
