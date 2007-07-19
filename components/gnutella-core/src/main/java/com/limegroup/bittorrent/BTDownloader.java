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

import org.limewire.collection.NumericBuffer;
import org.limewire.nio.NIODispatcher;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;

import com.limegroup.bittorrent.Torrent.TorrentState;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.AbstractDownloader;
import com.limegroup.gnutella.downloader.IncompleteFileManager;

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
	private volatile Torrent _torrent;

	/**
	 * The <tt>BTMetaInfo</tt> for this torrent.
	 */
	private BTMetaInfo _info;
	
	/** Local ref to the urn */
	private URN urn;
	
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
	
	// TODO: figure out how to init this
	private TorrentContext context;
	
	private volatile long startTime, stopTime;
	
	private NumericBuffer<Float> averagedBandwidth = 
		new NumericBuffer<Float>(10);
    
    /** Whether finish() has been invoked on this */
    private volatile boolean finished;
	
	public BTDownloader(BTMetaInfo info) {
		context = new BTContext(info);
		_info = info;
		urn = info.getURN();
		fileSystem = info.getFileSystem();
		synchronized(this) {
		    propertiesMap.put(METAINFO, info);
		    propertiesMap.put(DEFAULT_FILENAME, info.getName());
		}
	}
	
	/**
	 * Stops a torrent download.  If the torrent is in
	 * seeding state, it does nothing.
	 * (To stop a seeding torrent it must be stopped from the
	 * uploads pane)
	 */
	public void stop() {
		if (_torrent.isActive() &&
				_torrent.getState() != TorrentState.SEEDING) {
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
		return isResumable() || _torrent.getState() == TorrentState.QUEUED;
	}
	
	public boolean isLaunchable() {
		return fileSystem.getFiles().size() == 1 && 
        context.getDiskManager().getLastVerifiedOffset() > 0;
	}
	
    public boolean isResumable() {
		switch(_torrent.getState()) {
		case PAUSED:
		case TRACKER_FAILURE:
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
			return null;
        if (_torrent.isComplete())
            return getFile();
		long size = context.getDiskManager().getLastVerifiedOffset();
        if (size <= 0)
            return null;
        File file=new File(fileSystem.getBaseFile().getParent(),
                IncompleteFileManager.PREVIEW_PREFIX
                    +fileSystem.getBaseFile().getName());
        // Copy first block, returning if nothing was copied.
        if (FileUtils.copy(fileSystem.getBaseFile(), size, file) <=0 ) 
            return null;
        return file;
	}

	/*
	 *  (non-Javadoc)
	 * @see com.limegroup.gnutella.Downloader#getState()
	 * 
	 * Specifically, this maps the states of a torrent
	 * download to the states of a regular download.
	 */
	public DownloadStatus getState() {
		// aborted seeding torrents are shown as complete in the
		// downloads pane.
		if (_torrent.isComplete()) 
			return DownloadStatus.COMPLETE;
		switch(_torrent.getState()) {
		case WAITING_FOR_TRACKER :
			return DownloadStatus.WAITING_FOR_GNET_RESULTS;
		case VERIFYING:
			return DownloadStatus.RESUMING;
		case CONNECTING:
			return DownloadStatus.CONNECTING;
		case DOWNLOADING:
			return DownloadStatus.DOWNLOADING;
		case SAVING:
			return DownloadStatus.SAVING;
		case SEEDING:
			return DownloadStatus.COMPLETE;
		case QUEUED:
			return DownloadStatus.QUEUED;
		case PAUSED:
			return DownloadStatus.PAUSED;
		case STOPPED:
			return DownloadStatus.ABORTED;
		case DISK_PROBLEM:
			return DownloadStatus.DISK_PROBLEM;
		case TRACKER_FAILURE:
			return DownloadStatus.WAITING_FOR_USER; // let the user trigger a scrape
		case SCRAPING:
			return DownloadStatus.ITERATIVE_GUESSING; // bad name but practically the same
		case INVALID:
			return DownloadStatus.INVALID;
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
		if (getState() != DownloadStatus.WAITING_FOR_GNET_RESULTS)
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
		// if the download is complete, just return the length
		if (_info == null )
			return getContentLength();
		
		// return the number of verified bytes
		long ret = context.getDiskManager().getBlockSize();
		
		// if this is initial checking, add the number of processed bytes
		// too.
		if (_torrent.getState() == TorrentState.VERIFYING)
			ret += context.getDiskManager().getNumCorruptedBytes();
		return ret;
	}

	public String getVendor() {
		return BITTORRENT_DOWNLOAD;
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
	
	public GUID getQueryGUID() {
		return null;
	}

	public boolean isCompleted() {
		switch(_torrent.getState()) {
		case SEEDING:
		case STOPPED:
		case DISK_PROBLEM:
		case TRACKER_FAILURE:
			return true;
		}
		return false;
	}
	
	public boolean shouldBeRemoved() {
		switch(_torrent.getState()) {
		case DISK_PROBLEM:
		case SEEDING:
			return true;
		}
		return false;
	}

	public long getAmountVerified() {
		return context.getDiskManager().getVerifiedBlockSize();
	}

	public int getChunkSize() {
		return _info.getPieceLength();
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
		return context.getDiskManager().getAmountPending();
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
        
        // the below aren't handled...
        case STARTING:
        case STOP_APPROVED:
        case STOP_REQUESTED:
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
		} else { // otherwise torrent was already completed.
		    assert(_torrent instanceof FinishedTorrentDownload);
		}
	}
	
	private void writeObject(ObjectOutputStream out) 
	throws IOException {
		Map<String, Serializable> m = new HashMap<String, Serializable>();
		synchronized(this) {
            if (finished) // do not write finished downloads
                return;
			m.putAll(propertiesMap);
		}
		assert(m.containsKey(METAINFO));
		out.writeObject(m);
	}
	
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		Object read = in.readObject();
		propertiesMap = GenericsUtils.scanForMap(read, 
				String.class, Serializable.class, 
				GenericsUtils.ScanMode.EXCEPTION);
		read = propertiesMap.get(ATTRIBUTES);
		attributes = GenericsUtils.scanForMap(read, 
				String.class, Serializable.class, 
				GenericsUtils.ScanMode.EXCEPTION);
		_info = (BTMetaInfo)propertiesMap.get(METAINFO);
		context = new BTContext(_info);
		urn = _info.getURN();
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
		_torrent = new ManagedTorrent(context, torrentManager,
				NIODispatcher.instance().getScheduledExecutorService()); 
		torrentManager.addEventListener(this);
		ifm.addTorrentEntry(_info.getURN());
	}
	
	public void startDownload() {
		new BTUploader((ManagedTorrent)_torrent,
				_info, 
				RouterService.getTorrentManager());
		_torrent.start();
	}
	
	public void handleInactivity() {
		// nothing happens when we're inactive
	}
	
	public boolean shouldBeRestarted() {
		return getState() == DownloadStatus.QUEUED && RouterService.getTorrentManager().allowNewTorrent(); 
	}
	
	public boolean isAlive() {
		return false; // doesn't apply to torrents
	}

	public boolean isQueuable() {
		return !isResumable();
	}
	
	public boolean conflicts(URN urn, long fileSize, File... file) {
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

	public synchronized void finish() {
        finished = true;
		RouterService.getTorrentManager().removeEventListener(this);
		_torrent = new FinishedTorrentDownload(_torrent);
		_info = null;
		propertiesMap.remove(METAINFO);
	}
	
	public String toString() {
		return "downloader facade for "+fileSystem.getCompleteFile().getName();
	}
	
	public boolean equals(Object o) {
		if (! (o instanceof Downloader))
			return false;
		Downloader other = (Downloader)o;
		return getSHA1Urn().equals(other.getSHA1Urn());
	}

	public int getTriedHostCount() {
		return _torrent.getTriedHostCount();
	}
	
	public String getCustomIconDescriptor() {
		if (fileSystem.getFiles().size() == 1)
			return null;
		return BITTORRENT_DOWNLOAD;
	}
}
