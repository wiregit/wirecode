package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RouterService;
import com.limegroup.bittorrent.choking.Choker;
import com.limegroup.bittorrent.choking.ChokerFactory;
import com.limegroup.bittorrent.disk.DiskManagerListener;
import com.limegroup.bittorrent.disk.TorrentDiskManager;
import com.limegroup.bittorrent.handshaking.BTConnectionFetcher;
import com.limegroup.bittorrent.handshaking.BTConnectionFetcherFactory;
import com.limegroup.bittorrent.messages.BTHave;
import com.limegroup.bittorrent.tracking.TrackerManager;
import com.limegroup.bittorrent.tracking.TrackerManagerFactory;
import com.limegroup.gnutella.util.EventDispatcher;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.gnutella.util.SchedulingThreadPool;
import com.limegroup.gnutella.util.StrictIpPortSet;
import com.limegroup.gnutella.util.SyncWrapper;
import com.limegroup.gnutella.util.ThreadPool;


/**
 * Class that keeps track of state relevant to a single torrent.
 * 
 * It manages various components relevant to the torrent download
 * such as the Choker, Connection Fetcher, Verifying Folder.  
 * 
 * It keeps track of the known and connected peers and contains the
 * logic for starting and stopping the torrent.
 */
public class ManagedTorrent implements Torrent, DiskManagerListener,
BTLinkListener {
	
	private static final Log LOG = LogFactory.getLog(ManagedTorrent.class);
	
	/**
	 * A shared processing queue for disk-related tasks.
	 */
	private static final ThreadPool DEFAULT_DISK_INVOKER = 
		new ProcessingQueue("ManagedTorrent");
	
	/** 
	 * the executor of tasks involving network io. 
	 */
	private final SchedulingThreadPool networkInvoker;
	
	/** 
	 * Executor that changes the state of this torrent and does 
	 * the moving of files to the complete location, and other tasks
	 * involving disk io.
	 */
	private ThreadPool diskInvoker = DEFAULT_DISK_INVOKER;
	
	/**
	 * The list of known good TorrentLocations that we are not connected 
	 * or connecting to at the moment
	 */
	private Set<TorrentLocation> _peers;

	/**
	 * the meta info for this torrent
	 */
	private BTMetaInfo _info;

	/**
	 * The manager of disk operations.
	 */
	private volatile TorrentDiskManager _folder;

	/**
	 * The manager of tracker requests.
	 */
	private TrackerManager trackerManager;
	
	/**
	 * The fetcher of connections.
	 */
	private volatile BTConnectionFetcher _connectionFetcher;

	/** Manager of the BT links of this torrent */
	private final BTLinkManager linkManager;
	
	/** 
	 * The manager of choking logic
	 */
	private Choker choker;
	
	private final SyncWrapper<TorrentState> state = 
		new SyncWrapper<TorrentState>(TorrentState.QUEUED);
	
	/** The downloaded data this session */
	private volatile long totalDown;
	
	/** Event dispatcher for events generated by this torrent */
	private final EventDispatcher<TorrentEvent, TorrentEventListener> dispatcher;
	
	private final TorrentContext context;
	
	/**
	 * Constructs new ManagedTorrent
	 * 
	 * @param info the <tt>BTMetaInfo</tt> for this torrent
	 * @param dispatcher a dispatcher for events generated by this torrent
	 * @param networkInvoker a <tt>SchedulingThreadPool</tt> to execute
	 * network tasks on
	 * @param diskInvoker a <tt>SchedulingThreadPool</tt> to execute
	 * disk tasks on
	 */
	public ManagedTorrent(TorrentContext context, 
			EventDispatcher<TorrentEvent, TorrentEventListener> dispatcher,
			SchedulingThreadPool networkInvoker) {
		this.context = context;
		this.networkInvoker = networkInvoker;
		this.dispatcher = dispatcher;
		_info = context.getMetaInfo();
		_folder = getContext().getDiskManager();
		_peers = Collections.emptySet();
		linkManager = BTLinkManagerFactory.instance().getLinkManager();
		trackerManager = TrackerManagerFactory.instance().getTrackerManager(this);
	}

	/**
	 * notification that a request to the tracker(s) has started.
	 */
	public void setScraping() {
		synchronized(state.getLock()) {
			if (state.get() == TorrentState.WAITING_FOR_TRACKER)
				state.set(TorrentState.SCRAPING);
		}
	}
	
	/**
	 * Accessor for the info hash
	 * 
	 * @return byte[] containing the info hash
	 */
	public byte[] getInfoHash() {
		return _info.getInfoHash();
	}

	/**
	 * Accessor for meta info
	 * 
	 * @return <tt>BTMetaInfo</tt> for this torrent
	 */
	public BTMetaInfo getMetaInfo() {
		return _info;
	}
	
	/**
	 * @return the <tt>TorrentContext</tt> for this torrent
	 */
	public TorrentContext getContext() {
		return context;
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#isComplete()
	 */
	public boolean isComplete() {
		return state.get() != TorrentState.DISK_PROBLEM && _folder.isComplete();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#start()
	 */
	public void start() {
		if (LOG.isDebugEnabled())
			LOG.debug("requesting torrent start", new Exception());
		
		if (state.get() != TorrentState.QUEUED)
			throw new IllegalStateException();
		dispatchEvent(TorrentEvent.Type.STARTING);
		
		diskInvoker.invokeLater(new Runnable() {
			public void run() {
				
				if (state.get() != TorrentState.QUEUED) // something happened, do not start.
					return;
				
				LOG.debug("executing torrent start");
				
				initializeTorrent();
				initializeFolder();
				
				dispatchEvent(TorrentEvent.Type.STARTED); 
				
				TorrentState s = state.get();
				if (s == TorrentState.SEEDING || s == TorrentState.VERIFYING) 
					return;
				
				startConnecting();
			}
		});
	}
	
	/**
	 * Starts the tracker request if necessary and the fetching of
	 * connections.
	 */
	private void startConnecting() {
		boolean shouldFetch = false;
		synchronized(state.getLock()) {
			TorrentState currentState = state.get();
			if (currentState != TorrentState.VERIFYING && currentState != TorrentState.QUEUED)
				throw new IllegalArgumentException("cannot start connecting "+currentState);
			
			// kick off connectors if we already have some addresses
			if (_peers.size() > 0) {
				state.set(TorrentState.CONNECTING);
				shouldFetch = true;
			} else
				state.set(TorrentState.SCRAPING);
		}

		if (shouldFetch)
			_connectionFetcher.fetch();
		
		// connect to tracker(s)
		trackerManager.announceStart();
		
		// start the choking / unchoking of connections
		choker.start();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#stop()
	 */
	public void stop() {
				
		if (!isActive())
			throw new IllegalStateException("torrent cannot be stopped");
		
		state.set(TorrentState.STOPPED);
		
		stopImpl();
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskManagerListener#diskExceptionHappened()
	 */
	public void diskExceptionHappened() {
		synchronized(state.getLock()) {
			if (state.get() == TorrentState.DISK_PROBLEM)
				return;
			state.set(TorrentState.DISK_PROBLEM);
		}
		stopImpl();
	}
	
	/**
	 * Performs the actual stop.
	 */
	private synchronized void stopImpl() {
		
		if (!stopState())
			throw new IllegalStateException("stopping in wrong state "+state.get());
		
		// close the folder and stop the periodic tasks
		_folder.close();
		
		// fire off an announcement to the tracker
		trackerManager.announceStop();
		
		// write the snapshot if not complete
		if (!_folder.isComplete()) {
			Runnable saver = new Runnable() {
				public void run() {
					try {
						saveInfoMapInIncomplete();
					} catch (IOException ignored){}
				}
			};
			diskInvoker.invokeLater(saver);
		}
		
		// shutdown various components
		Runnable closer = new Runnable() {
			public void run() {
				choker.shutdown();
				linkManager.shutdown();
				_connectionFetcher.shutdown();
			}
		};
		networkInvoker.invokeLater(closer);
		
		dispatchEvent(TorrentEvent.Type.STOPPED); 
		
		LOG.debug("Torrent stopped!");
	}
	
	private void saveInfoMapInIncomplete() throws IOException {
		String path = context.getFileSystem().getBaseFile().getParent()+
		File.separator+".dat"+context.getFileSystem().getName();
		FileUtils.writeObject(path, context.getMetaInfo());
	}
	
	private void dispatchEvent(TorrentEvent.Type type) {
		TorrentEvent evt = new TorrentEvent(this, type, this);
		dispatcher.dispatchEvent(evt);
	}
	
	/**
	 * @return if the current state is a stopped state.
	 */
	private boolean stopState() {
		switch(state.get()) {
			case PAUSED:
			case STOPPED:
			case DISK_PROBLEM:
			case TRACKER_FAILURE:
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#pause()
	 */
	public void pause() {
		boolean wasActive = false;
		synchronized(state.getLock()) {
			if (!isActive() && state.get() != TorrentState.QUEUED)
				throw new IllegalStateException("torrent not pausable");
			
			wasActive = isActive();
			state.set(TorrentState.PAUSED);
		}
		if (wasActive)
			stopImpl();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#resume()
	 */
	public boolean resume() {
		synchronized(state.getLock()) {
			switch(state.get()) {
			case PAUSED :
			case TRACKER_FAILURE:
			case STOPPED:
				state.set(TorrentState.QUEUED);
				return true;
			default:
				throw new IllegalStateException("torrent not resumable "+state);
			}
		}
	}

	/**
	 * notification that a connection was closed.
	 */
	public void linkClosed(BTLink btc) {
		if (btc.isWorthRetrying()) {
			// this forgets any strikes on the location
			TorrentLocation ep = new TorrentLocation(btc.getEndpoint());
			ep.strike();
			_peers.add(ep);
		}
		removeConnection(btc);
		
		if (!needsMoreConnections())
			return;
		
		TorrentState s = state.get();
		if (s == TorrentState.DOWNLOADING || s == TorrentState.CONNECTING) 
			_connectionFetcher.fetch();
	}

	public void linkInterested(BTLink interested) {
		if (!interested.isChoked())
			rechoke();
	}

	public void linkNotInterested(BTLink notInterested) {
		if (!notInterested.isChoked())
			rechoke();
	}

	/**
	 * Initializes some state relevant to the torrent
	 */
	private void initializeTorrent() {
		_peers = Collections.synchronizedSet(new StrictIpPortSet<TorrentLocation>());
		choker = ChokerFactory.instance().getChoker(linkManager.getConnections(), 
				networkInvoker, false);
		_connectionFetcher = 
			BTConnectionFetcherFactory.instance().getBTConnectionFetcher(this, networkInvoker);
	}

	/**
	 * Initializes the verifying folder
	 */
	private void initializeFolder() {
		try {
			_folder.open(this);
			saveInfoMapInIncomplete();
			
		} catch (IOException ioe) {
			// problem opening files cannot recover.
			if (LOG.isDebugEnabled()) 
				LOG.debug("unrecoverable error", ioe);
			
			state.set(TorrentState.DISK_PROBLEM);
			return;
		} 
		

		// if we happen to have the complete torrent in the incomplete folder
		// move it to the complete folder.
		if (_folder.isComplete()) 
			completeTorrentDownload();
		else if (_folder.isVerifying())
			state.set(TorrentState.VERIFYING);
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskManagerListener#verificationComplete()
	 */
	public void verificationComplete() {
		diskInvoker.invokeLater(new Runnable() {
			public void run() {
				if (state.get() != TorrentState.VERIFYING) 
					return;
				startConnecting();
				if (_folder.isComplete())
					completeTorrentDownload();
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskManagerListener#notifyOfComplete(int)
	 */
	public void chunkVerified(int in) {
		if (LOG.isDebugEnabled())
			LOG.debug("got completed chunk " + in);
		
		if (_folder.isVerifying())
			return;
		
		final BTHave have = new BTHave(in);
		Runnable haveNotifier = new Runnable() {
			public void run() {
				linkManager.sendHave(have);
			}
		};
		networkInvoker.invokeLater(haveNotifier);
		
		if (_folder.isComplete()) {
			LOG.info("file is complete");
			diskInvoker.invokeLater(new Runnable(){
				public void run(){
					if (isDownloading())
						completeTorrentDownload();
				}
			});
		}
	}

	/**
	 * @return the state of this torrent
	 */
	public TorrentState getState() {
		return state.get();
	}
	
	/**
	 * adds location to try
	 * 
	 * @param to a TorrentLocation for this download
	 */
	public void addEndpoint(TorrentLocation to) {
		if (_peers.contains(to) || linkManager.isConnectedTo(to))
			return;
		if (!RouterService.getIpFilter().allow(to.getAddress()))
			return;
		if (NetworkUtils.isMe(to.getAddress(), to.getPort()))
			return;
		if (_peers.add(to)) {
			synchronized(state.getLock()) {
				if (state.get() == TorrentState.SCRAPING)
					state.set(TorrentState.CONNECTING);
			}
			_connectionFetcher.fetch();
		}
	}
	
	/**
	 * Stops the torrent because of tracker failure.
	 */
	public void stopVoluntarily() {
		synchronized(state.getLock()) {
			if (!isActive())
				return;
			if (state.get() == TorrentState.SEEDING) 
				state.set(TorrentState.STOPPED);
			else
				state.set(TorrentState.TRACKER_FAILURE);
		}
		stopImpl();
	}
	
	/**
	 * @return true if we need to fetch any more connections
	 */
	public boolean needsMoreConnections() {
		if (!isActive())
			return false;
		
		// if we are complete, do not open any sockets - the active torrents will need them.
		if (isComplete() && RouterService.getTorrentManager().hasNonSeeding())
			return false;
		
		// provision some slots for incoming connections unless we're firewalled
		int limit = TorrentManager.getMaxTorrentConnections();
		if (RouterService.acceptedIncomingConnection())
			limit = limit * 4 / 5;
		return linkManager.getNumConnections() < limit;
	}

	/**
	 * @return true if a fetched connection should be added.
	 */
	public boolean shouldAddConnection(TorrentLocation loc) {
		if (linkManager.isConnectedTo(loc))
			return false;
		return linkManager.getNumConnections() < TorrentManager.getMaxTorrentConnections();
	}
	
	/**
	 * adds a fetched connection
	 * @return true if it was added
	 */
	public boolean addConnection(final BTLink btc) {
		if (LOG.isDebugEnabled())
			LOG.debug("trying to add connection " + btc.toString());
		
		boolean shouldAdd = false;
		synchronized(state.getLock()) {
			switch(state.get()) {
			case CONNECTING :
			case SCRAPING :
			case WAITING_FOR_TRACKER :
				state.set(TorrentState.DOWNLOADING);
                dispatchEvent(TorrentEvent.Type.DOWNLOADING);
			case DOWNLOADING :
			case SEEDING:
				shouldAdd = true;
			}
		}

		if (!shouldAdd)
			return false;
		
		linkManager.addLink(btc);
		_peers.remove(btc.getEndpoint());
		if (LOG.isDebugEnabled())
			LOG.debug("added connection " + btc.toString());
		return true;
	}

	/**
	 * private helper method, removing connection
	 */
	private void removeConnection(final BTLink btc) {
		if (LOG.isDebugEnabled())
			LOG.debug("removing connection " + btc.toString());
		linkManager.removeLink(btc);
		if (btc.isUploading())
			rechoke();
		boolean connectionsEmpty = linkManager.getNumConnections() == 0;
		boolean peersEmpty = _peers.isEmpty();
		synchronized(state.getLock()) {
			if (connectionsEmpty && state.get() == TorrentState.DOWNLOADING) {
				if (peersEmpty)
					state.set(TorrentState.WAITING_FOR_TRACKER);
				else
					state.set(TorrentState.CONNECTING);
			}
		}
	}

	/**
	 * saves the complete files to the shared folder
	 */
	private synchronized void completeTorrentDownload() {

		// cancel all requests and uploads and disconnect from seeds
		Runnable r = new Runnable(){
			public void run() {
				linkManager.disconnectSeedsChokeRest();
				
				// clear the state as we no longer need it
				// (until source exchange is implemented)
				_peers.clear();
			}
		};
		networkInvoker.invokeLater(r);
		
		// save the files to the destination folder
		try {
			saveFiles();
		} catch (IOException failed) {
			diskExceptionHappened();
			return;
		}
		
		state.set(TorrentState.SEEDING);
		
		// switch the choker logic and resume uploads
		choker.shutdown();
		choker = ChokerFactory.instance().getChoker(
				linkManager.getConnections(),networkInvoker, true);
		choker.start();
		choker.rechoke();
		
		// tell the tracker we are a seed now
		trackerManager.announceComplete();
		
		dispatchEvent(TorrentEvent.Type.COMPLETE); 
	}

	/**
	 * Saves the complete files to destination folder.
	 */
	private void saveFiles() throws IOException {

		// close the folder 
		synchronized(_folder) {
			if (!_folder.isOpen())
				return;
			
			_folder.close();
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("folder closed");
		
		// move it to the complete location
		state.set(TorrentState.SAVING);
		context.getFileSystem().moveToCompleteFolder();
		context.getFileSystem().addToLibrary();
		LOG.trace("saved files");
		context.initializeDiskManager(true);
		LOG.trace("initialized folder");
		context.getMetaInfo().resetFileDesc();
		
		// and re-open it for seeding.
		_folder = context.getDiskManager();
		if (LOG.isDebugEnabled())
			LOG.debug("new veryfing folder");
		
		_folder.open(this);
		if (LOG.isDebugEnabled())
			LOG.debug("folder opened");
	}
	
	/**
	 * @return the next time we should announce to the tracker
	 */
	public long getNextTrackerRequestTime() {
		return trackerManager.getNextTrackerRequestTime();
	}

	/**
	 * @return a peer we should try to connect to next
	 */
	public TorrentLocation getTorrentLocation() {
		long now = System.currentTimeMillis();
		TorrentLocation ret = null;
		synchronized(_peers) {
			for (Iterator<TorrentLocation> iter = _peers.iterator(); iter.hasNext();) {
				TorrentLocation loc = iter.next();
				if (loc.isBusy(now))
					continue;
				iter.remove();
				if (!linkManager.isConnectedTo(loc)) {
					ret = loc;
					break;
				}
			}
		}
		return ret;
	}

	/**
	 * trigger a rechoking of the connections
	 */
	private void rechoke() {
		choker.rechoke();
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#isPaused()
	 */
	public boolean isPaused() {
		return state.get() == TorrentState.PAUSED;
	}

	/**
	 * two torrents are equal if their infoHashes are.
	 */
	public boolean equals(Object o) {
		if (!(o instanceof ManagedTorrent))
			return false;
		ManagedTorrent mt = (ManagedTorrent) o;

		return Arrays.equals(mt.getInfoHash(), getInfoHash());
	}

	/**
	 * @return if the torrent is active - either downloading or 
	 * seeding, saving or verifying
	 */
	public boolean isActive() {
		synchronized(state.getLock()) {
			if (isDownloading())
				return true;
			switch(state.get()) {
			case SEEDING: 
			case VERIFYING:
			case SAVING:
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return if the torrent can be paused
	 */
	public boolean isPausable() {
		synchronized(state.getLock()) {
			if (isDownloading())
				return true;
			switch(state.get()) {
			case QUEUED:
			case VERIFYING:
				return true;
			}
		}
		return false;
	}
	/**
	 * @return if the torrent is currently in one of the downloading states.
	 */
	boolean isDownloading() {
		switch(state.get()) {
		case WAITING_FOR_TRACKER:
		case SCRAPING: 
		case CONNECTING:
		case DOWNLOADING:
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getConnections()
	 */
	public List<BTLink> getConnections() {
		return linkManager.getConnections();
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getNumConnections()
	 */
	public int getNumConnections() {
		return linkManager.getNumConnections();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getNumPeers()
	 */
	public int getNumPeers() {
		return _peers.size();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getNumBusyPeers()
	 */
	public  int getNumNonInterestingPeers() {
		return linkManager.getNumNonInterestingPeers();
	}

	public int getNumChockingPeers() {
		return linkManager.getNumChockingPeers();
	}
	
	/**
	 * records some data was downloaded
	 */
	public void countDownloaded(int amount) {
		totalDown += amount;
	}
	
	public long getTotalUploaded(){
		return _info.getAmountUploaded();
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getTotalDownloaded()
	 */
	public long getTotalDownloaded() {
		return totalDown;
	}
	
	/**
	 * @return the ratio of uploaded / downloaded data.
	 */
	public float getRatio() {
		return _info.getRatio();
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getAmountLost()
	 */
	public long getAmountLost() {
		return _folder.getNumCorruptedBytes();
	}
	
	public boolean hasNonBusyLocations() {
		long now = System.currentTimeMillis();
		synchronized(_peers) {
			for (TorrentLocation to : _peers) {
				if (!to.isBusy(now))
					return true;
			}
		}
		return false;
	}
	
	/**
	 * @return the time until a recently failed location can be
	 * retried, or Long.MAX_VALUE if no such found.
	 */
	public long getNextLocationRetryTime() {
		long soonest = Long.MAX_VALUE;
		long now = System.currentTimeMillis();
		synchronized(_peers) {
			for (TorrentLocation to : _peers)  {
				soonest = Math.min(soonest, to.getWaitTime(now));
				if (soonest == 0)
					break;
			}
		}
		return soonest;
	}
	
	/**
	 * @return true if continuing is hopeless
	 */
	public boolean shouldStop() {
		return linkManager.getNumConnections() == 0 && _peers.size() == 0;
	}
	
	/**
	 * @return the <tt>BTConnectionFetcher</tt> for this torrent.
	 */
	public BTConnectionFetcher getFetcher() {
		return _connectionFetcher;
	}
	
	/**
	 * @return the <tt>SchedulingThreadPool</tt> executing network-
	 * related tasks
	 */
	public SchedulingThreadPool getScheduler() {
		return networkInvoker;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#measureBandwidth()
	 */
	public void measureBandwidth() {
		linkManager.measureBandwidth();
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getMeasuredBandwidth(boolean)
	 */
	public float getMeasuredBandwidth(boolean downstream) {
		return linkManager.getMeasuredBandwidth(downstream);
	}

	public int getTriedHostCount() {
		return _connectionFetcher.getTriedHostCount();
	}
	
	/**
	 * @return true if this torrent is currently uploading
	 */
	public boolean isUploading() {
		return linkManager.hasUploading();
	}
	
	/**
	 * @return true if this torrent is currently suspended
	 * A torrent is considered suspended if there are connections interested
	 * in it but all are choked.
	 */
	public boolean isSuspended() {
		return isComplete() && linkManager.hasInterested() &&
		!linkManager.hasUnchoked();
	}
}
