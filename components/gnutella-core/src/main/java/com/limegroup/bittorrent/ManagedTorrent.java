package com.limegroup.bittorrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.bittorrent.messages.BTHave;
import com.limegroup.gnutella.util.EventDispatcher;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.IntWrapper;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.gnutella.util.SchedulingThreadPool;
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
public class ManagedTorrent implements Torrent {
	
	private static final Log LOG = LogFactory.getLog(ManagedTorrent.class);
	
	private static final SchedulingThreadPool INVOKER = 
		new NIODispatcherThreadPool();

	/** the executor of our tasks. */
	private SchedulingThreadPool networkInvoker = INVOKER;
	
	/** 
	 * Executor that changes the state of this torrent and does 
	 * the moving of files to the complete location.
	 */
	private ThreadPool diskInvoker = new ProcessingQueue("ManagedTorrent");
	
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

	/**
	 * The list of BTConnections that this torrent has.
	 * LOCKING: the list is synchronized on itself; it is modified
	 * only from the NIODispatcher thread, so no locking is required
	 * when iterating on that thread.
	 */
	private final List<BTLink> _connections;

	/** 
	 * The manager of choking logic
	 */
	private Choker choker;
	
	/** 
	 * The current state of this torrent.
	 */
	private final IntWrapper _state = new IntWrapper(QUEUED);
	
	/** The total uploaded and downloaded data */
	private volatile long totalUp, totalDown;
	
	/** Event dispatcher for events generated by this torrent */
	private final EventDispatcher<TorrentEvent, TorrentEventListener> dispatcher;

	/**
	 * Constructs new ManagedTorrent
	 * 
	 * @param info
	 *            the <tt>BTMetaInfo</tt> for this torrent
	 * @param manager
	 *            the <tt>TorrentManager</tt> managing this
	 */
	public ManagedTorrent(BTMetaInfo info, 
			EventDispatcher<TorrentEvent, TorrentEventListener> dispatcher) {
		_info = info;
		_folder = info.getDiskManager();
		_connections = Collections.synchronizedList(new ArrayList<BTLink>());
		_peers = Collections.EMPTY_SET;
		trackerManager = new TrackerManager(this);
		choker = new LeechChoker(_connections, networkInvoker);
		this.dispatcher = dispatcher;
	}

	/**
	 * notification that a request to the tracker(s) has started.
	 */
	void setScraping() {
		synchronized(_state) {
			if (_state.getInt() == WAITING_FOR_TRACKER)
				_state.setInt(SCRAPING);
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

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#isComplete()
	 */
	public boolean isComplete() {
		return _state.getInt() != DISK_PROBLEM && _folder.isComplete();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#start()
	 */
	public void start() {
		if (LOG.isDebugEnabled())
			LOG.debug("requesting torrent start", new Exception());
		
		if (_state.getInt() != QUEUED)
			throw new IllegalStateException();
		
		diskInvoker.invokeLater(new Runnable() {
			public void run() {
				
				if (_state.getInt() != QUEUED) // something happened, do not start.
					return;
				
				LOG.debug("executing torrent start");
				
				initializeTorrent();
				initializeFolder();
				
				dispatchEvent(TorrentEvent.Type.STARTED); 
				
				int state = _state.getInt();
				if (state == SEEDING || state == VERIFYING) 
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
		synchronized(_state) {
			if (_state.getInt() != VERIFYING && _state.getInt() != QUEUED)
				throw new IllegalArgumentException("cannot start connecting "+_state.getInt());
			
			// kick off connectors if we already have some addresses
			if (_peers.size() > 0) {
				_state.setInt(CONNECTING);
				shouldFetch = true;
			} else
				_state.setInt(SCRAPING);
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
		
		_state.setInt(STOPPED);
		
		stopImpl();
	}
	
	/**
	 * Notifies the torrent that a disk error has happened
	 * and terminates it.
	 */
	public void diskExceptionHappened() {
		synchronized(_state) {
			if (_state.getInt() == DISK_PROBLEM)
				return;
			_state.setInt(DISK_PROBLEM);
		}
		stopImpl();
	}
	
	/**
	 * Performs the actual stop.
	 */
	private synchronized void stopImpl() {
		
		if (!stopState())
			throw new IllegalStateException("stopping in wrong state "+_state);
		
		// close the folder and stop the periodic tasks
		_folder.close();
		choker.stop();
		
		// fire off an announcement to the tracker
		trackerManager.announceStop();
		
		// write the snapshot if not complete
		if (!_folder.isComplete()) {
			Runnable saver = new Runnable() {
				public void run() {
					try {
						_info.saveInfoMapInIncomplete();
					} catch (IOException ignored){}
				}
			};
			diskInvoker.invokeLater(saver);
		}
		
		// close connections and cancel keepaliveSender
		Runnable closer = new Runnable() {
			public void run() {
				_connectionFetcher.shutdown();
				List<BTLink> copy = new ArrayList<BTLink>(_connections);
				for(BTLink con : copy) 
					con.shutdown(); 
			}
		};
		networkInvoker.invokeLater(closer);
		
		dispatchEvent(TorrentEvent.Type.STOPPED); 
		
		LOG.debug("Torrent stopped!");
	}
	
	private void dispatchEvent(TorrentEvent.Type type) {
		TorrentEvent evt = new TorrentEvent(this, type, this);
		dispatcher.dispatchEvent(evt);
	}
	
	/**
	 * @return if the current state is a stopped state.
	 */
	private boolean stopState() {
		switch(_state.getInt()) {
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
		synchronized(_state) {
			if (!isActive() && _state.getInt() != QUEUED)
				throw new IllegalStateException("torrent not pausable");
			
			wasActive = isActive();
			_state.setInt(PAUSED);
		}
		if (wasActive)
			stopImpl();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#resume()
	 */
	public boolean resume() {
		synchronized(_state) {
			if (_state.getInt() != PAUSED && _state.getInt() != STOPPED)
				throw new IllegalStateException("torrent not resumable");
			
			_state.setInt(QUEUED);
		}
		return true;
	}

	/**
	 * notification that a connection was closed.
	 */
	void connectionClosed(BTLink btc) {
		if (btc.isWorthRetrying()) {
			// this forgets any strikes on the location
			TorrentLocation ep = new TorrentLocation(btc.getEndpoint());
			ep.strike();
			_peers.add(ep);
		}
		removeConnection(btc);
		
		if (!needsMoreConnections())
			return;
		
		int state = _state.getInt();
		if (state == DOWNLOADING || state == CONNECTING) 
			_connectionFetcher.fetch();
	}

	/**
	 * Initializes some state relevant to the torrent
	 */
	private void initializeTorrent() {
		_peers = Collections.synchronizedSet(new HashSet<TorrentLocation>());

		_connectionFetcher = new BTConnectionFetcher(this, networkInvoker);
		
		if (LOG.isDebugEnabled())
			LOG.debug("Starting torrent");
	}

	/**
	 * Initializes the verifying folder
	 */
	private void initializeFolder() {
		try {
			_folder.open(this);
			_info.saveInfoMapInIncomplete();
			
		} catch (IOException ioe) {
			// problem opening files cannot recover.
			if (LOG.isDebugEnabled()) 
				LOG.debug("unrecoverable error", ioe);
			
			_state.setInt(DISK_PROBLEM);
			return;
		} 
		

		// if we happen to have the complete torrent in the incomplete folder
		// move it to the complete folder.
		if (_folder.isComplete()) 
			completeTorrentDownload();
		else if (_folder.isVerifying())
			_state.setInt(VERIFYING);
	}

	/**
	 * Notification that verification previously existing data
	 * has completed.
	 */
	void verificationComplete() {
		diskInvoker.invokeLater(new Runnable() {
			public void run() {
				if (_state.getInt() == VERIFYING) 
					startConnecting();
			}
		});
	}
	
	/**
	 * notification that a chunk has been completed and verified
	 * @param in the # of the verified chunk
	 */
	void notifyOfComplete(int in) {
		if (LOG.isDebugEnabled())
			LOG.debug("got completed chunk " + in);
		
		if (_folder.isVerifying())
			return;
		
		final BTHave have = new BTHave(in);
		Runnable haveNotifier = new Runnable() {
			public void run() {
				for (BTLink btc : _connections) 
					btc.sendHave(have);
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
	public int getState() {
		return _state.getInt();
	}
	
	/**
	 * adds location to try
	 * 
	 * @param to a TorrentLocation for this download
	 */
	public void addEndpoint(TorrentLocation to) {
		if (_peers.contains(to) || isConnectedTo(to))
			return;
		if (!IPFilter.instance().allow(to.getAddress()))
			return;
		if (NetworkUtils.isMe(to.getAddress(), to.getPort()))
			return;
		if (_peers.add(to)) {
			synchronized(_state) {
				if (_state.getInt() == SCRAPING)
					_state.setInt(CONNECTING);
			}
			_connectionFetcher.fetch();
		}
	}
	
	/**
	 * Stops the torrent because of tracker failure.
	 */
	void stopVoluntarily() {
		synchronized(_state) {
			if (!isActive())
				return;
			if (_state.getInt() == SEEDING) 
				_state.setInt(STOPPED);
			else
				_state.setInt(TRACKER_FAILURE);
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
		return _connections.size() < limit;
	}

	/**
	 * @return true if a fetched connection should be added.
	 */
	public boolean shouldAddConnection(TorrentLocation loc) {
		if (isConnectedTo(loc))
			return false;
		return _connections.size() < TorrentManager.getMaxTorrentConnections();
	}
	
	/**
	 * adds a fetched connection
	 * @return true if it was added
	 */
	public boolean addConnection(final BTLink btc) {
		if (LOG.isDebugEnabled())
			LOG.debug("trying to add connection " + btc.toString());
		
		boolean shouldAdd = false;
		synchronized(_state) {
			switch(_state.getInt()) {
			case CONNECTING :
			case SCRAPING :
			case WAITING_FOR_TRACKER :
				_state.setInt(DOWNLOADING);
			case DOWNLOADING :
			case SEEDING:
				shouldAdd = true;
			}
		}

		if (!shouldAdd)
			return false;
		
		_connections.add(btc);
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
		_connections.remove(btc);
		if (btc.isUploading())
			rechoke();
		boolean connectionsEmpty = _connections.isEmpty();
		boolean peersEmpty = _peers.isEmpty();
		synchronized(_state) {
			if (connectionsEmpty && _state.getInt() == DOWNLOADING) {
				if (peersEmpty)
					_state.setInt(WAITING_FOR_TRACKER);
				else
					_state.setInt(CONNECTING);
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
				List<BTLink> seeds = new ArrayList<BTLink>(_connections.size());
				for (BTLink btc : _connections) {
					if (btc.isSeed())
						seeds.add(btc);
					else 
						btc.suspendTraffic();
				}
				
				// close all seed connections
				for (BTLink seed : seeds)
					seed.shutdown();
				
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
		
		_state.setInt(SEEDING);
		
		// switch the choker logic and resume uploads
		choker.stop();
		choker = new SeedChoker(_connections,networkInvoker);
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
		_state.setInt(SAVING);
		_info.moveToCompleteFolder();
		
		// and re-open it for seeding.
		_folder = _info.getDiskManager();
		if (LOG.isDebugEnabled())
			LOG.debug("new veryfing folder");
		
		_folder.open(this);
		if (LOG.isDebugEnabled())
			LOG.debug("folder opened");
	}
	
	/**
	 * @param to
	 *            a <tt>TorrentLocation</tt> to check
	 * @return true if we are already connected to it
	 */
	public boolean isConnectedTo(TorrentLocation to) {
		synchronized(_connections) {
			for (BTLink btc : _connections) {
				IpPort addr = btc.getEndpoint(); 
				if (addr.getAddress().equals(to.getAddress()) && 
						addr.getPort() == to.getPort())
					return true;
			}
		}
		return false;
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
	TorrentLocation getTorrentLocation() {
		long now = System.currentTimeMillis();
		TorrentLocation ret = null;
		synchronized(_peers) {
			for (TorrentLocation loc : _peers) {
				if (loc.isBusy(now))
					continue;
				else if (!isConnectedTo(loc)) {
					ret = loc;
					_peers.remove(ret);
					break;
				}
			}
		}
		return ret;
	}

	/**
	 * trigger a rechoking of the connections
	 */
	void rechoke() {
		choker.rechoke();
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#isPaused()
	 */
	public boolean isPaused() {
		return _state.getInt() == PAUSED;
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
		synchronized(_state) {
			if (isDownloading())
				return true;
			switch(_state.getInt()) {
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
		synchronized(_state) {
			if (isDownloading())
				return true;
			switch(_state.getInt()) {
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
		switch(_state.getInt()) {
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
		return _connections;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getNumConnections()
	 */
	public int getNumConnections() {
		return _connections.size();
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
		int busy = 0;
		synchronized(_connections) {
			for (BTLink con : _connections) {
				if (!con.isInteresting())
					busy++;
			}
		}
		return busy;
	}

	public int getNumChockingPeers() {
		int qd = 0;
		synchronized(_connections) {
			for (BTLink c : _connections) {
				if (c.isChoking())
					qd++;
			}
		}
		return qd;
	}
	/**
	 * records that some data was uploaded
	 */
	public void countUploaded(int amount) {
		totalUp += amount;
	}
	
	/**
	 * records some data was downloaded
	 */
	public void countDownloaded(int amount) {
		totalDown += amount;
	}
	
	public long getTotalUploaded(){
		return totalUp;
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
		if (getTotalDownloaded() == 0)
			return 0;
		return (1f * getTotalUploaded()) / getTotalDownloaded();
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getAmountLost()
	 */
	public long getAmountLost() {
		return _folder.getNumCorruptedBytes();
	}
	
	boolean hasNonBusyLocations() {
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
	boolean shouldStop() {
		return _connections.size() == 0 && _peers.size() == 0;
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
		synchronized(_connections) {
			for (BTLink con : _connections) 
				con.measureBandwidth();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getMeasuredBandwidth(boolean)
	 */
	public float getMeasuredBandwidth(boolean downstream) {
		float ret = 0;
		synchronized(_connections) {
			for (BTLink con : _connections) 
				ret += con.getMeasuredBandwidth(downstream, true);
		}
		return ret;
	}
	
	/**
	 * A scheduling threadpool that uses the NIODispatcher for execution.
	 */
	private static class NIODispatcherThreadPool implements SchedulingThreadPool {
		public void invokeLater(Runnable r) {
			NIODispatcher.instance().invokeLater(r);
		}
		
		public Future invokeLater(Runnable r, long delay) {
			return NIODispatcher.instance().invokeLater(r, delay);
		}
	}
}
