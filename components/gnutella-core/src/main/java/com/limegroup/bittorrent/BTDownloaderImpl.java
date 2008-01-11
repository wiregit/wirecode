package com.limegroup.bittorrent;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.limewire.collection.NumericBuffer;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.bittorrent.Torrent.TorrentState;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.AbstractCoreDownloader;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.IncompleteFileManager;

/**
 * This class enables the rest of LW to treat this as a regular download.
 */
public class BTDownloaderImpl extends AbstractCoreDownloader 
                          implements TorrentEventListener, BTDownloader {
	
    
	private static final String METAINFO = "metainfo";
	
	private volatile long startTime, stopTime;
	
	private NumericBuffer<Float> averagedBandwidth = new NumericBuffer<Float>(10);
    
    /** Whether finish() has been invoked on this */
    private volatile boolean finished;

    /** 
     * The torrent this is downloading.
     * Non-final because it changes to a FinishedTorrentDownload when done.
     */
    private volatile Torrent torrent;
    
    /**
     * The <tt>BTMetaInfo</tt> for this torrent.
     * Non-final because it's nulled when the torrent is done.
     */
    private volatile BTMetaInfo btMetaInfo;
        
    private final DownloadManager downloadManager;
    private final IncompleteFileManager incompleteFileManager;
    private final TorrentContext torrentContext;    
    private final Provider<TorrentManager> torrentManager;
    private final BTUploaderFactory btUploaderFactory;

    @Inject
	BTDownloaderImpl(BTContextFactory btContextFactory,
            SaveLocationManager saveLocationManager, Provider<TorrentManager> torrentManager,
            BTUploaderFactory btUploaderFactory, DownloadManager downloadManager,
            ManagedTorrentFactory managedTorrentFactory) {
	    super(saveLocationManager);
        this.torrentContext = btContextFactory.createBTContext(btMetaInfo);        
        this.downloadManager = downloadManager;
        this.torrentManager = torrentManager;
        this.btUploaderFactory = btUploaderFactory;
        this.incompleteFileManager = downloadManager.getIncompleteFileManager();
        
        this.torrent = managedTorrentFactory.create(torrentContext);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTDownloader#initBtMetaInfo(com.limegroup.bittorrent.BTMetaInfo)
     */
    public void initBtMetaInfo(BTMetaInfo btMetaInfo) {
        this.btMetaInfo = btMetaInfo;  
		synchronized(this) {
		    propertiesMap.put(METAINFO, btMetaInfo);
		    propertiesMap.put(CoreDownloader.DEFAULT_FILENAME, btMetaInfo.getName());
		}
	}
    
    @Override
    public void addNewProperties(Map<String, Serializable> newProperties) {
        if(newProperties.get(METAINFO) != null)
            btMetaInfo = (BTMetaInfo)newProperties.get(METAINFO);
        super.addNewProperties(newProperties);
    }
	
	/**
	 * Stops a torrent download.  If the torrent is in
	 * seeding state, it does nothing.
	 * (To stop a seeding torrent it must be stopped from the
	 * uploads pane)
	 */
	public void stop() {
		if (torrent.isActive() &&
				torrent.getState() != TorrentState.SEEDING) {
			torrent.stop();
		} else if (isInactive()) 
			downloadManager.remove(this, true);
			
	}

	public void pause() {
		torrent.pause();
	}

	public boolean isPaused() {
		return torrent.isPaused();
	}
	
	public boolean isPausable() {
		return torrent.isPausable();
	}

	public boolean isInactive() {
		return isResumable() || torrent.getState() == TorrentState.QUEUED;
	}
	
	public boolean isLaunchable() {
		return torrentFileSystem().getFiles().size() == 1 && 
        torrentContext.getDiskManager().getLastVerifiedOffset() > 0;
	}
	
    public boolean isResumable() {
		switch(torrent.getState()) {
		case PAUSED:
		case TRACKER_FAILURE:
			return true;
		}
        return false;
	}

	public boolean resume() {
		return torrent.resume();
	}

	public File getFile() {
		if (torrent.isComplete())
			return torrentFileSystem().getCompleteFile();
		return torrentFileSystem().getBaseFile();
	}

	public File getDownloadFragment() {
		if (!isLaunchable())
			return null;
        if (torrent.isComplete())
            return getFile();
		long size = torrentContext.getDiskManager().getLastVerifiedOffset();
        if (size <= 0)
            return null;
        File file=new File(torrentFileSystem().getBaseFile().getParent(),
                IncompleteFileManager.PREVIEW_PREFIX
                    +torrentFileSystem().getBaseFile().getName());
        // Copy first block, returning if nothing was copied.
        if (FileUtils.copy(torrentFileSystem().getBaseFile(), size, file) <=0 ) 
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
		if (torrent.isComplete()) 
			return DownloadStatus.COMPLETE;
		switch(torrent.getState()) {
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
	
	private long getTotalAmountDownloaded() {
		return torrent.getTotalDownloaded();
	}

	/**
	 * We only know how much time we'll be in the state between
	 * tracker requests.
	 */
	public int getRemainingStateTime() {
		if (getState() != DownloadStatus.WAITING_FOR_GNET_RESULTS)
			return 0;
		return Math.max(0,(int)(torrent.getNextTrackerRequestTime() - 
				System.currentTimeMillis()) / 1000);
	}

	public long getContentLength() {
		return torrentFileSystem().getTotalSize();
	}

	public long getAmountRead() {
		// if the download is complete, just return the length
		if (btMetaInfo == null )
			return getContentLength();
		
		// return the number of verified bytes
		long ret = torrentContext.getDiskManager().getBlockSize();
		
		// if this is initial checking, add the number of processed bytes
		// too.
		if (torrent.getState() == TorrentState.VERIFYING)
			ret += torrentContext.getDiskManager().getNumCorruptedBytes();
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
		return torrent.getNumPeers();
	}

	public int getBusyHostCount() {
		return torrent.getNumNonInterestingPeers();
	}

	public int getQueuedHostCount() {
		return torrent.getNumChockingPeers();
	}
	
	public GUID getQueryGUID() {
		return null;
	}

	public boolean isCompleted() {
		switch(torrent.getState()) {
		case SEEDING:
		case STOPPED:
		case DISK_PROBLEM:
		case TRACKER_FAILURE:
			return true;
		}
		return false;
	}
	
	public boolean shouldBeRemoved() {
		switch(torrent.getState()) {
		case DISK_PROBLEM:
		case SEEDING:
			return true;
		}
		return false;
	}

	public long getAmountVerified() {
		return torrentContext.getDiskManager().getVerifiedBlockSize();
	}

	public int getChunkSize() {
		return btMetaInfo.getPieceLength();
	}

	public long getAmountLost() {
		return torrent.getAmountLost();
	}

	public void measureBandwidth() {
		torrent.measureBandwidth();
		averagedBandwidth.add(torrent.getMeasuredBandwidth(true));
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
		torrentFileSystem().setCompleteFile(new File(saveDirectory, fileName));
	}

	public File getSaveFile() {
		return torrentFileSystem().getCompleteFile();
	}
	
	public URN getSHA1Urn() {
		return btMetaInfo.getURN();
	}
	
	public int getAmountPending() {
		return torrentContext.getDiskManager().getAmountPending();
	}


	public int getNumHosts() {
		return torrent.getNumConnections();
	}

	public void handleTorrentEvent(TorrentEvent evt) {
		if (evt.getTorrent() != torrent)
			return;
		
		switch(evt.getType()) {
		case STARTED : torrentStarted(); break;
		case COMPLETE : torrentComplete(); break;
		case STOPPED : torrentStopped(evt.getDescription()); break;
        
        // the below aren't handled...
        case STARTING:
        case STOP_APPROVED:
        case STOP_REQUESTED:
		}
	}
	
	private void torrentComplete() {
		// the download stops now. even though the torrent goes on
		stopTime = System.currentTimeMillis();
		incompleteFileManager.removeTorrentEntry(btMetaInfo.getURN());
		downloadManager.remove(this, true);
	}

	private void torrentStarted() {
		startTime = System.currentTimeMillis();
		stopTime = 0;
	}

	private void torrentStopped(String description) {
		if (stopTime == 0) {
            if (description != null)
                setAttribute(CUSTOM_INACTIVITY_KEY, description);
			averagedBandwidth.clear();
			boolean resumable = isResumable();
			stopTime = System.currentTimeMillis();
			downloadManager.remove(this, !resumable);
		} else { // otherwise torrent was already completed.
		    assert(torrent instanceof FinishedTorrentDownload);
		}
	}

	public void initialize() {
        torrentManager.get().addEventListener(this);
        incompleteFileManager.addTorrentEntry(btMetaInfo.getURN());
    }
	
	public void startDownload() {
		btUploaderFactory.createBTUploader((ManagedTorrent)torrent, btMetaInfo);
		torrent.start();
	}
	
	public void handleInactivity() {
		// nothing happens when we're inactive
	}
	
	public boolean shouldBeRestarted() {
		return getState() == DownloadStatus.QUEUED && torrentManager.get().allowNewTorrent(); 
	}
	
	public boolean isAlive() {
		return false; // doesn't apply to torrents
	}

	public boolean isQueuable() {
		return !isResumable();
	}
	
	public boolean conflicts(URN urn, long fileSize, File... file) {
		if (btMetaInfo.getURN().equals(urn))
			return true;
		for (File f : file) {
			if (conflictsSaveFile(f))
				return true;
		}
		return false;
	}
	
	public boolean conflictsSaveFile(File candidate) {
		return torrentFileSystem().conflicts(candidate);
	}

	public boolean conflictsWithIncompleteFile(File incomplete) {
		return torrentFileSystem().conflictsIncomplete(incomplete); 
	}
	
	public synchronized void finish() {
        finished = true;
		torrentManager.get().removeEventListener(this);
		torrent = new FinishedTorrentDownload(torrent);
		btMetaInfo = null;
		propertiesMap.remove(METAINFO);
	}
	
	public String toString() {
		return "downloader facade for "+torrentFileSystem().getCompleteFile().getName();
	}
	
	public boolean equals(Object o) {
		if (! (o instanceof Downloader))
			return false;
		Downloader other = (Downloader)o;
		return getSHA1Urn().equals(other.getSHA1Urn());
	}

	public int getTriedHostCount() {
		return torrent.getTriedHostCount();
	}
	
	public String getCustomIconDescriptor() {
		if (torrentFileSystem().getFiles().size() == 1)
			return null;
		return BITTORRENT_DOWNLOAD;
	}

    public DownloaderType getDownloadType() {
        return DownloaderType.BTDOWNLOADER;
    }
    
    private TorrentFileSystem torrentFileSystem() {
        return btMetaInfo.getFileSystem();
    }
}
