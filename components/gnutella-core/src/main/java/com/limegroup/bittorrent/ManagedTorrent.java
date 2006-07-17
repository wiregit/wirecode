package com.limegroup.bittorrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.io.NBThrottle;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.io.Throttle;
import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.bittorrent.messages.BTHave;
import com.limegroup.gnutella.util.EventDispatcher;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.IntWrapper;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.gnutella.util.SchedulingThreadPool;
import com.limegroup.gnutella.util.ThreadPool;

public class ManagedTorrent {
	
	private static final Log LOG = LogFactory.getLog(ManagedTorrent.class);
	
	/**
	 * How often to send keepalives.  2 minutes as suggested by spec.
	 */
	private static final int KEEP_ALIVE_INTERVAL = 2 * 60 * 1000; 
	
	/**
	 * States of a torrent download.  Some of them are functionally equivalent
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
	 * The list of known bad TorrentLocations
	 */
	private Set<TorrentLocation> _badPeers;

	/**
	 * the meta info for this torrent
	 */
	private BTMetaInfo _info;

	/**
	 * the handle for all files.
	 */
	private volatile VerifyingFolder _folder;

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
	private final List<BTConnection> _connections;

	/** 
	 * A runnable that takes care of scheduled periodic rechoking
	 */
	private Choker choker;
	
	/**
	 * A runnable that sends periodic keepalives.
	 */
	private Future keepAliveSender;
	
	/** 
	 * The current state of this torrent.
	 */
	private final IntWrapper _state = new IntWrapper(QUEUED);
	
	/** The total uploaded and downloaded data */
	private volatile long totalUp, totalDown;
	
	private final EventDispatcher<TorrentEvent, TorrentEventListener> dispatcher;

	/**
	 * Constructs new ManagedTorrent
	 * 
	 * @param info
	 *            the <tt>BTMetaInfo</tt> for this torrent
	 * @param manager
	 *            the <tt>TorrentManager</tt> managing this
	 * @param callback
	 *            the <tt>ActivityCallback</tt>
	 */
	public ManagedTorrent(BTMetaInfo info, 
			EventDispatcher<TorrentEvent, TorrentEventListener> dispatcher) {
		_info = info;
		_info.setManagedTorrent(this);
		_folder = info.getVerifyingFolder();
		_connections = Collections.synchronizedList(new ArrayList<BTConnection>());
		_peers = Collections.EMPTY_SET;
		_badPeers = Collections.EMPTY_SET;
		trackerManager = new TrackerManager(this);
		choker = new Choker(this, networkInvoker);
		this.dispatcher = dispatcher;
	}

	void setState(int newState) {
		_state.setInt(newState);
	}
	
	/**
	 * if the torrent is currently waiting for the next tracker request,
	 * tell it that the request has started.
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

	/**
	 * @return true if the torrent is complete.
	 */
	public boolean isComplete() {
		return _state.getInt() != DISK_PROBLEM && _folder.isComplete();
	}

	/**
	 * Starts the torrent 
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
		choker.scheduleRechoke();
		
		keepAliveSender = networkInvoker.invokeLater(
				new KeepAliveSender(),KEEP_ALIVE_INTERVAL);
	}

	/**
	 * Stops the torrent
	 */
	public void stop() {
				
		if (!isActive())
			throw new IllegalStateException("torrent cannot be stopped");
		
		_state.setInt(STOPPED);
		
		stopImpl(STOPPED);
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
		stopImpl(DISK_PROBLEM);
	}
	
	/**
	 * Performs the actual stop.  It does not modify the torrent state.
	 */
	private synchronized void stopImpl(int finalState) {
		
		if (!stopState())
			throw new IllegalArgumentException("stopping in wrong state "+_state);
		
		
		_folder.close();
		_state.setInt(finalState);
		choker.stop();
		trackerManager.announceStop();
		if (keepAliveSender != null)
			keepAliveSender.cancel(true);
		
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
		
		// close connections if any
		if (!_connections.isEmpty()) {
			Runnable closer = new Runnable() {
				public void run() {
					_connectionFetcher.shutdown();
					List<BTConnection> copy = new ArrayList<BTConnection>(_connections);
					for(BTConnection con : copy) 
						con.close(); 
				}
			};
			networkInvoker.invokeLater(closer);
		}
		
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

	/**
	 * Forces this ManagedTorrent to make way for other ManagedTorrents, if
	 * there are any
	 * 
	 * The torrent must be active or queued.
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
			stopImpl(PAUSED);
	}

	/**
	 * Resumes the torrent, if there is a free slot
	 * 
	 * The torrent must be paused or stopped.
	 */
	public boolean resume() {
		synchronized(_state) {
			if (_state.getInt() != PAUSED && _state.getInt() != STOPPED)
				throw new IllegalStateException("torrent not resumable");
			
			_state.setInt(QUEUED);
		}
		return true;
	}

	void connectionClosed(BTConnection btc) {
		if (btc.isWorthRetrying()) {
			// this forgets any strikes on the location
			TorrentLocation ep = new TorrentLocation(btc.getEndpoint());
			ep.strike();
			_peers.add(ep);
		}
		removeConnection(btc);
		int state = _state.getInt();
		if (state == DOWNLOADING || state == CONNECTING)
			_connectionFetcher.fetch();
	}

	private void initializeTorrent() {
		_badPeers = Collections.synchronizedSet(
				new FixedSizeExpiringSet<TorrentLocation>(500, 60 * 60 * 1000));
		_peers = Collections.synchronizedSet(new HashSet<TorrentLocation>());

		_connectionFetcher = new BTConnectionFetcher(this);
		
		if (LOG.isDebugEnabled())
			LOG.debug("Starting torrent");
	}

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

	void verificationComplete() {
		diskInvoker.invokeLater(new Runnable() {
			public void run() {
				if (_state.getInt() == VERIFYING) 
					startConnecting();
			}
		});
	}
	
	/**
	 * notifies this, that a chunk has been completed and verified
	 * 
	 * @param in
	 *            the verified chunk
	 */
	void notifyOfComplete(int in) {
		if (LOG.isDebugEnabled())
			LOG.debug("got completed chunk " + in);
		if (_folder.isVerifying())
			return;
		
		final BTHave have = new BTHave(in);
		Runnable haveNotifier = new Runnable() {
			public void run() {
				for (BTConnection btc : _connections) 
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

	int getState() {
		return _state.getInt();
	}
	
	/**
	 * adds location to try
	 * 
	 * @param to
	 *            a TorrentLocation for this download
	 * @return true if the location was accepted
	 */
	public boolean addEndpoint(TorrentLocation to) {
		if (_peers.contains(to) || isConnectedTo(to))
			return false;
		if (!IPFilter.instance().allow(to.getAddress()))
			return false;
		if (NetworkUtils.isMe(to.getAddress(), to.getPort()))
			return false;
		if (_peers.add(to)) {
			synchronized(_state) {
				if (_state.getInt() == SCRAPING)
					_state.setInt(CONNECTING);
			}
			_connectionFetcher.fetch();
			return true;
		}
		return false;
	}
	
	public void addBadEndpoint(TorrentLocation to) {
		_badPeers.add(to);
	}

	
	void stopVoluntarily() {
		int finalState = 0;
		synchronized(_state) {
			if (!isActive())
				return;
			if (_state.getInt() == SEEDING) 
				_state.setInt(STOPPED);
			else
				_state.setInt(TRACKER_FAILURE);
			finalState = _state.getInt();
		}
		stopImpl(finalState);
	}
	
	/**
	 * @return true if we need to fetch any more connections
	 */
	public boolean needsMoreConnections() {
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
	 * adding connection
	 */
	public void addConnection(final BTConnection btc) {
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

		if (shouldAdd) {
				_connections.add(btc);
			if (LOG.isDebugEnabled())
				LOG.debug("added connection " + btc.toString());
		} else
			btc.close();
	}

	/**
	 * private helper method, removing connection
	 */
	private void removeConnection(final BTConnection btc) {
		if (LOG.isDebugEnabled())
			LOG.debug("removing connection " + btc.toString());
		_connections.remove(btc);
		if (btc.isInterested() && !btc.isChoked())
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
		
		// cancel requests
		Runnable r = new Runnable(){
			public void run() {
				List<BTConnection> seeds = new ArrayList<BTConnection>(_connections.size());
				for (BTConnection btc : _connections) {
					if (btc.isSeed())
						seeds.add(btc);
					else {
						// cancel all requests, if there are any left. (This should not
						// be the case at this point anymore)
						btc.sendChoke();
						btc.cancelAllRequests();
						btc.sendNotInterested();
					}
				}
				
				// close all seed connections
				for (BTConnection seed : seeds)
					seed.close();
				
				// clear the state as we no longer need it
				// (until source exchange is implemented)
				_peers.clear();
				_badPeers.clear();
			}
		};
		networkInvoker.invokeLater(r);
		
		// save the files to the destination folder
		saveFiles();
		
		if (_state.getInt() == DISK_PROBLEM)
			return;
		
		// resume uploads
		choker.rechoke();
		
		// tell the tracker we are a seed now
		trackerManager.announceComplete();
		
		dispatchEvent(TorrentEvent.Type.COMPLETE); 
	}

	/**
	 * Saves the complete files to destination folder.
	 */
	private void saveFiles() {

		synchronized(_folder) {
			if (!_folder.isOpen())
				return;
			
			_folder.close();
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("folder closed");
		_state.setInt(SAVING);
		boolean diskProblem = !_info.moveToCompleteFolder();
		
		if (!diskProblem) {
			// folder has to be updated with the new files
			_folder = _info.getVerifyingFolder();
			if (LOG.isDebugEnabled())
				LOG.debug("new veryfing folder");
			
			try {
				_folder.open(this);
			} catch (IOException ioe) {
				LOG.debug(ioe);
				diskProblem = true;
			}
			if (LOG.isDebugEnabled())
				LOG.debug("folder opened");
		}
		
		if (diskProblem) 
			diskExceptionHappened();
		else
			_state.setInt(SEEDING); 
	}
	
	/**
	 * @param to
	 *            a <tt>TorrentLocation</tt> to check
	 * @return true if we are apparently already connected to a certain location
	 */
	public boolean isConnectedTo(TorrentLocation to) {
		synchronized(_connections) {
			for (BTConnection btc : _connections) {
				IpPort addr = btc.getEndpoint(); 
				if (addr.getAddress().equals(to.getAddress()) && 
						addr.getPort() == to.getPort())
					return true;
			}
		}
		return false;
	}

	long calculateWaitTime() {
		if (_peers.size() == 0)
			return 0;
		long ret = Long.MAX_VALUE;
		long now = System.currentTimeMillis();

		synchronized(_peers) {
			for (TorrentLocation loc : _peers) {
				ret = Math.min(ret, loc.getWaitTime(now));
				if (ret == 0)
					return 0;
			}
		}
		return ret;
	}
	
	long getNextTrackerRequestTime() {
		return trackerManager.getNextTrackerRequestTime();
	}

	TorrentLocation getTorrentLocation() {
		long now = System.currentTimeMillis();
		synchronized(_peers) {
			Iterator<TorrentLocation> iter = _peers.iterator();
			while (iter.hasNext()) {
				TorrentLocation temp = iter.next();
				if (temp.isBusy(now))
					continue;
				iter.remove();
				// check before connecting
				if (!isConnectedTo(temp)) {
					return (temp);
				}
			}
		}
		return null;
	}

	
	void rechoke() {
		choker.rechoke();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.limegroup.gnutella.Downloader#getNumberOfAlternateLocations()
	 */
	public int getNumberOfAlternateLocations() {
		return _peers.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.limegroup.gnutella.Downloader#getNumberOfInvalidAlternateLocations()
	 */
	public int getNumberOfInvalidAlternateLocations() {
		return _badPeers.size();
	}

	/**
	 * @return true if paused
	 */
	public boolean isPaused() {
		return _state.getInt() == PAUSED;
	}

	public boolean equals(Object o) {
		if (!(o instanceof ManagedTorrent))
			return false;
		ManagedTorrent mt = (ManagedTorrent) o;

		return Arrays.equals(mt.getInfoHash(), getInfoHash());
	}

	public boolean isConnected() {
		return _connections.size() > 0;
	}

	/**
	 * @return if the torrent is active - either downloading or 
	 * seeding, saving, verifying...
	 */
	boolean isActive() {
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
	
	boolean isPausable() {
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

	public List<BTConnection> getConnections() {
		return _connections;
	}
	
	void shuffleConnections() {
		synchronized(_connections) {
			Collections.shuffle(_connections);
		}
	}
	
	public int getNumConnections() {
		return _connections.size();
	}

	public int getNumBadPeers() {
		return _badPeers.size();
	}

	public int getNumPeers() {
		return _peers.size();
	}

	public  int getNumBusyPeers() {
		int busy = 0;
		synchronized(_connections) {
			for (BTConnection con : _connections) {
				if (!con.isInteresting())
					busy++;
			}
		}
		return busy;
	}

	public void countUploaded(int now) {
		totalUp += now;
	}
	
	public void countDownloaded(int now) {
		totalDown += now;
	}
	
	public long getTotalUploaded(){
		return totalUp;
	}
	
	public long getTotalDownloaded() {
		return totalDown;
	}
	
	public float getRatio() {
		if (getTotalDownloaded() == 0)
			return 0;
		return (1f * getTotalUploaded()) / getTotalDownloaded();
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
	 * whether or not continuing is hopeless
	 */
	boolean shouldStop() {
		return _connections.size() == 0 && _peers.size() == 0;
	}
	
	public BTConnectionFetcher getFetcher() {
		return _connectionFetcher;
	}
	
	public void measureBandwidth() {
		synchronized(_connections) {
			for (BTConnection con : _connections) 
				con.measureBandwidth();
		}
	}
	
	public float getMeasuredBandwidth(boolean downstream) {
		float ret = 0;
		synchronized(_connections) {
			for (BTConnection con : _connections) 
				ret += con.getMeasuredBandwidth(downstream, true);
		}
		return ret;
	}
	
	private class KeepAliveSender implements Runnable {
		
		private void scheduleKeepAlive() {
			networkInvoker.invokeLater(this, KEEP_ALIVE_INTERVAL);
		}
		
		public void run() {
			
			for (BTConnection con : _connections) 
				con.sendKeepAlive();
			
			scheduleKeepAlive();
		}
	}
	
	private static class NIODispatcherThreadPool implements SchedulingThreadPool {
		public void invokeLater(Runnable r) {
			NIODispatcher.instance().invokeLater(r);
		}
		
		public Future invokeLater(Runnable r, long delay) {
			return NIODispatcher.instance().invokeLater(r, delay);
		}
	}
}
