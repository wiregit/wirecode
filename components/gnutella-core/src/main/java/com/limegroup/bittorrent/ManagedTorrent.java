package com.limegroup.bittorrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.io.Throttle;
import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.bittorrent.messages.BTHave;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.ProcessingQueue;

public class ManagedTorrent {
	
	/**
	 * States of a torrent download.  Some of them are identical
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
	static final int GAVE_UP = 11;
	
	private static final Log LOG = LogFactory.getLog(ManagedTorrent.class);

	/*
	 * the number of failures after which we consider giving up
	 */
	private static final int MAX_TRACKER_FAILURES = 5;

	/*
	 * used to order BTConnections according to the average 
	 * download or upload speed.
	 */
	private static final Comparator DOWNLOAD_SPEED_COMPARATOR = 
		new SpeedComparator(true);
	private static final Comparator UPLOAD_SPEED_COMPARATOR = 
		new SpeedComparator(false);
	
	/*
	 * orders BTConnections by the round they were unchoked.
	 */
	private static final Comparator UNCHOKE_COMPARATOR =
		new UnchokeComparator();

	/*
	 * time in milliseconds between choking/unchoking of connections
	 */
	private static final int RECHOKE_TIMEOUT = 10 * 1000;

	/**
	 * the TorrentManager managing this torrent
	 */
	private final TorrentManager _manager;
	
	/**
	 * The list of known good TorrentLocations that we are not connected to at
	 * the moment get this' monitor befor accessing
	 */
	private Set _peers;

	/**
	 * The list of known bad TorrentLocations
	 */
	private Set _badPeers;

	/**
	 * the meta info for this torrent
	 */
	private BTMetaInfo _info;

	/**
	 * the handle for all files.
	 */
	private volatile VerifyingFolder _folder;

	/**
	 * if a problem occured with disk operations.
	 */
	private volatile boolean _diskProblem;
	
	private final TrackerManager trackerManager;
	
	/**
	 * 
	 */
	private volatile BTConnectionFetcher _connectionFetcher;

	/**
	 * whether to choke all connections
	 */
	private boolean _globalChoke = false;

	/**
	 * it is possible that a complete download is restarted (for what purpose so
	 * ever) so we need to remember that we already saved the file.
	 */
	private boolean _saved = false;

	/**
	 * The list of BTConnections that this torrent has.
	 * LOCKING: the list is synchronized on itself; it is modified
	 * only from the NIODispatcher thread, so no locking is required
	 * when iterating on that thread.
	 */
	private final List _connections;

	private ProcessingQueue torrentStateQueue;

	private BTDownloader _downloader;

	private BTUploader _uploader;
	
	/** 
	 * A runnable that takes care of scheduled periodic rechoking
	 */
	private volatile PeriodicChoker choker;
	
	/** 
	 * The current state of this torrent.
	 */
	private volatile int _state = QUEUED;

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
	public ManagedTorrent(BTMetaInfo info, TorrentManager manager) {
		_info = info;
		_info.setManagedTorrent(this);
		_manager = manager;
		_folder = info.getVerifyingFolder();
		if (_folder.isVerifying())
			_state = VERIFYING;
		_connections = Collections.synchronizedList(new ArrayList());
		torrentStateQueue = new ProcessingQueue("ManagedTorrent");
		_downloader = new BTDownloader(this, _info);
		_uploader = new BTUploader(this, _info);
		_peers = Collections.EMPTY_SET;
		_badPeers = Collections.EMPTY_SET;
		trackerManager = new TrackerManager(this);
	}

	void setState(int newState) {
		_state = newState;
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
		return _folder.isComplete();
	}

	/**
	 * Starts the torrent 
	 */
	public void start() {
		if (LOG.isDebugEnabled())
			LOG.debug("requesting torrent start", new Exception());
		
		enqueueTask(new Runnable() {
			public void run() {
				
				LOG.debug("executing torrent start");
				
				initializeTorrent();
				initializeFolder();
				
				if (_state == SEEDING || _state == VERIFYING) 
					return;
				
				startConnecting();
			}
		});
	}
	
	private void startConnecting() {
		
		// kick off connectors if we already have some addresses
		if (_peers.size() > 0) {
			_state = CONNECTING;
			_connectionFetcher.fetch();
		} else
			_state = WAITING_FOR_TRACKER;
		
		// connect to tracker(s)
		trackerManager.announceStart();
		
		// start the choking / unchoking of connections
		scheduleRechoke();
	}

	/**
	 * Stops the torrent
	 */
	public void stop() {
		if (LOG.isDebugEnabled())
			LOG.debug("requested torrent stop", new Exception());
		
		enqueueTask(new Runnable() {
			public void run() {
				
				if (!isActive())
					return;
				
				_state = STOPPED;
				
				stopNow();
			}
		});
	}
	
	/**
	 * Notifies the torrent that a disk error has happened
	 * and terminates it.
	 */
	public void diskExceptionHappened() {
		enqueueTask(new Runnable() {
			public void run() {
				if (_state == DISK_PROBLEM)
					return;
				_state = DISK_PROBLEM;
				stopNow();
			}
		});
	}
	
	/**
	 * Performs the actual stop.  To be invoked only from the
	 * torrent processing queue.
	 */
	private void stopNow() {
		
		RouterService.getCallback().removeUpload(_uploader);
		// we stopped, removing torrent from active list of
		// TorrentManager
		_manager.removeTorrent(this);
		
		_folder.close();
		
		if (choker != null)
			choker.stopped = true;
		
		// close connections
		Runnable closer = new Runnable() {
			public void run() {
				_connectionFetcher.shutdown();
				while(!_connections.isEmpty()) {
					BTConnection toClose = 
						(BTConnection) _connections.get(_connections.size() - 1);
					toClose.close(); // this removes itself from the list.
				}
			}
		};
		NIODispatcher.instance().invokeLater(closer);
		
		trackerManager.announceStop();
		
		LOG.debug("Torrent stopped!");
	}

	/**
	 * Forces this ManagedTorrent to make way for other ManagedTorrents, if
	 * there are any
	 */
	public void pause() {
		enqueueTask(new Runnable() {
			public void run() {
				_state = PAUSED;
				stopNow();
			}
		});
	}

	/**
	 * Resumes the torrent, if there is a free slot
	 */
	public boolean resume() {
		boolean canResume = false;
		switch(_state) {
		case PAUSED :
		case STOPPED :
			canResume = true;
		}
		if (!canResume)
			return false;
		
		enqueueTask(new Runnable() {
			public void run(){
				_state = QUEUED;
				_manager.wakeUp(ManagedTorrent.this);
			}
		});
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
		if (_state == DOWNLOADING || _state == CONNECTING)
			_connectionFetcher.fetch();
	}

	private void initializeTorrent() {
		_diskProblem = false;
		_globalChoke = false;
		_badPeers = Collections.synchronizedSet(
				new FixedSizeExpiringSet(500, 60 * 60 * 1000));
		_peers = Collections.synchronizedSet(new HashSet());
		if (_info.getLocations() != null)
			_peers.addAll(_info.getLocations());

		RouterService.getCallback().addUpload(_uploader);
		
		_connectionFetcher = new BTConnectionFetcher(this, _manager.getPeerId());

		if (LOG.isDebugEnabled())
			LOG.debug("Starting torrent");
	}

	private void initializeFolder() {
		try {
			_folder.open(this);
		} catch (IOException ioe) {
			// problem opening files cannot recover.
			if (LOG.isDebugEnabled()) 
				LOG.debug("unrecoverable error", ioe);
			
			_state = DISK_PROBLEM;
			return;
		} 
		

		if (_folder.isComplete())
			completeTorrentDownload();
		else if (_folder.isVerifying())
			_state = VERIFYING;
	}

	void verificationComplete() {
		enqueueTask(new Runnable() {
			public void run() {
				if (_state == VERIFYING) 
					startConnecting();
			}
		});
	}
	
	/**
	 * Handles requesting ranges from the remote host
	 * 
	 * @param btc
	 *            the BTConnection to request from
	 */
	public void request(final BTConnection btc) {
		if (LOG.isDebugEnabled())
			LOG.debug("requesting ranges from " + btc.toString());
		
		// don't request if complete
		if (_folder.isComplete() || !isActive())
			return;
		BTInterval in = _folder.leaseRandom(btc.getAvailableRanges());
		if (in != null)
			btc.sendRequest(in);
		else if (_folder.getNumWeMiss(btc.getAvailableRanges()) == 0) {
			if (LOG.isDebugEnabled())
				LOG.debug("connection not interesting anymore");
			btc.sendNotInterested();
		}
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
				for (Iterator iter = _connections.iterator(); iter.hasNext();) {
					BTConnection btc = (BTConnection) iter.next();
					btc.sendHave(have);
				}
			}
		};
		NIODispatcher.instance().invokeLater(haveNotifier);
		
		if (_folder.isComplete()) {
			LOG.info("file is complete");
			enqueueTask(new Runnable(){
				public void run(){
					completeTorrentDownload();
				}
			});
		}
	}

	int getState() {
		return _state;
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
			if (_state == WAITING_FOR_TRACKER)
				_state = CONNECTING;
			_connectionFetcher.fetch();
			return true;
		}
		return false;
	}
	
	public void addBadEndpoint(TorrentLocation to) {
		_badPeers.add(to);
	}

	
	void giveUp() {
		enqueueTask(new Runnable() {
			public void run() {
				_state = GAVE_UP;
				stopNow();
			}
		});
	}
	
	/**
	 * @return true if we need any more connections
	 */
	boolean needsMoreConnections() {
		if (RouterService.acceptedIncomingConnection())
			return _connections.size() < BittorrentSettings.TORRENT_MAX_CONNECTIONS
					.getValue() * 4 / 5;
		return _connections.size() < BittorrentSettings.TORRENT_MAX_CONNECTIONS
				.getValue();
	}

	/**
	 * adding connection
	 */
	public void addConnection(final BTConnection btc) {
		if (LOG.isDebugEnabled())
			LOG.debug("trying to add connection " + btc.toString());
		// this check prevents a few exceptions that may be thrown if a
		// connection initialization is completed after we have already stopped
		// happens especially when a user quits a torrent manually.
		if (_state == CONNECTING || 
				_state == DOWNLOADING) {
			
			_connections.add(btc);
			
			if (_state == CONNECTING)
				_state = DOWNLOADING;
			
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
		if (_connections.isEmpty() && _state == DOWNLOADING) {
			if (_peers.isEmpty())
				_state = WAITING_FOR_TRACKER;
			else
				_state = CONNECTING;
		}
			
	}

	/**
	 * saves the complete files to the shared folder
	 */
	private void completeTorrentDownload() {
		_state = SEEDING;
		if (_saved)
			return;

		// stop uploads and cancel requests
		Runnable r = new Runnable(){
			public void run() {
				LOG.debug("global choke");
				setGlobalChoke(true);
				
				for (Iterator iter = _connections.iterator(); iter.hasNext();) {
					BTConnection btc = (BTConnection) iter.next();
					// cancel all requests, if there are any left. (This should not
					// be the case at this point anymore)
					btc.cancelAllRequests();
					btc.sendNotInterested();
				}
			}
		};
		NIODispatcher.instance().invokeLater(r);
		
		// save the files to the destination folder
		saveFiles();
		
		// resume uploads
		NIODispatcher.instance().invokeLater(new Runnable() {
			public void run() {
				setGlobalChoke(false);
			}
		});
		
		// tell the tracker we are a seed now
		trackerManager.announceComplete();
		
		// tell the manager I am complete
		_manager.torrentComplete(this);
	}

	/**
	 * Save the complete files to disc. this is executed within the timer thread
	 * but since we don't want to upload or download anything while we are
	 * saving the files, it should be okay
	 */
	private void saveFiles() {
		if (_saved)
			return;

		_folder.close();
		if (LOG.isDebugEnabled())
			LOG.debug("folder closed");
		_state = SAVING;
		_diskProblem = !_info.moveToCompleteFolder();
		if (LOG.isDebugEnabled())
			LOG.debug("could not save: " + _diskProblem);
		
		// folder has to be updated with the new files
		_folder = _info.getVerifyingFolder();
		if (LOG.isDebugEnabled())
			LOG.debug("new veryfing folder");
		
		try {
			_folder.open(this);
		} catch (IOException ioe) {
			LOG.debug(ioe);
			_diskProblem = true;
		}
		if (LOG.isDebugEnabled())
			LOG.debug("folder opened");

		if (_diskProblem) {
			_state = DISK_PROBLEM;
			stopNow();
		} else
			_state = SEEDING; 

		// remember attempt to save the file
		_saved = true;
	}
	
	/**
	 * @param to
	 *            a <tt>TorrentLocation</tt> to check
	 * @return true if we are apparently already connected to a certain location
	 */
	private boolean isConnectedTo(TorrentLocation to) {
		synchronized(_connections) {
			for (Iterator iter = _connections.iterator(); iter.hasNext();) {
				BTConnection btc = (BTConnection) iter.next();
				// compare by address only. there's no way of comparing ports
				// or peer ids
				if (btc.getEndpoint().getAddress().equals(to.getAddress()))
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
			for (Iterator iter = _peers.iterator(); iter.hasNext();) {
				ret = Math.min(ret, ((TorrentLocation) iter.next())
						.getWaitTime(now));
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
			Iterator iter = _peers.iterator();
			while (iter.hasNext()) {
				TorrentLocation temp = (TorrentLocation) iter.next();
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

	/**
	 * Schedules choking of connections
	 */
	private void scheduleRechoke() {
		if (choker != null)
			choker.stopped = true;
		choker = new PeriodicChoker();
		RouterService.schedule(choker, RECHOKE_TIMEOUT, 0);
	}
	
	void rechoke() {
		NIODispatcher.instance().invokeLater(new Rechoker(_connections));
	}
	
	private class PeriodicChoker implements Runnable {
		volatile int round;
		int unchokesSinceLast;
		volatile boolean stopped;
		public void run() {
			if (stopped)
				return;
			
			if (LOG.isDebugEnabled())
				LOG.debug("scheduling rechoke");
			
			List l; 
			if (round++ % 3 == 0) {
				synchronized(_connections) {
					l = new ArrayList(_connections);
				}
				Collections.shuffle(l);
			} else
				l = _connections;
			
			
			NIODispatcher.instance().invokeLater(new Rechoker(l, true));
			
			RouterService.schedule(this, RECHOKE_TIMEOUT, 0);
		}
	}
	
	private class Rechoker implements Runnable {
		private final List connections;
		private final boolean forceUnchokes;
		Rechoker(List connections) {
			this(connections, false);
		}
		
		Rechoker(List connections, boolean forceUnchokes) {
			this.connections = connections;
			this.forceUnchokes = forceUnchokes;
		}
		
		public void run() {
			if (_globalChoke) {
				LOG.debug("global choke");
				return;
			}
			
			if (_folder.isComplete())
				seedRechoke();
			else
				leechRechoke();
		}
		
		private void leechRechoke() {
			List fastest = new ArrayList(connections.size());
			for (Iterator iter = connections.iterator(); iter.hasNext();) {
				BTConnection con = (BTConnection) iter.next();
				if (con.isInterested() && con.shouldBeInterested())
					fastest.add(con);
			}
			Collections.sort(fastest,DOWNLOAD_SPEED_COMPARATOR);
			
			// unchoke the fastest connections that are interested in us
			int numFast = getNumUploads() - 1;
			for(int i = fastest.size() - 1; i >= numFast; i--)
				fastest.remove(i);
			
			// unchoke optimistically at least one interested connection
			int optimistic = Math.max(1,
					BittorrentSettings.TORRENT_MIN_UPLOADS.getValue() - fastest.size());
			
			
			for (Iterator iter = connections.iterator(); iter.hasNext();) {
				BTConnection con = (BTConnection) iter.next();
				if (fastest.remove(con)) 
					con.sendUnchoke(choker.round);
				else if (optimistic > 0 && con.shouldBeInterested()) {
					con.sendUnchoke(choker.round); // this is weird but that's how Bram does it
					if (con.isInterested()) 
						optimistic--;
				} else 
					con.sendChoke();
			}
		}
		
		private void seedRechoke() {
			int numForceUnchokes = 0;
			if (forceUnchokes) {
				int x = (getNumUploads() + 2) / 3;
				numForceUnchokes = Math.max(0, x + choker.round % 3) / 3 -
				choker.unchokesSinceLast;
			}
			
			List preferred = new ArrayList();
			int newLimit = choker.round - 3;
			for (Iterator iter = connections.iterator(); iter.hasNext();) {
				BTConnection con = (BTConnection) iter.next();
				if (!con.isChoked() && con.isInterested() && 
						con.shouldBeInterested()) {
					if (con.getUnchokeRound() < newLimit)
						con.setUnchokeRound(-1);
					preferred.add(con);
				}
			}
			
			int numKept = getNumUploads() - numForceUnchokes;
			if (preferred.size() > numKept) {
				Collections.sort(preferred,UNCHOKE_COMPARATOR);
				preferred = preferred.subList(0, numKept);
			}
			
			int numNonPref = getNumUploads() - preferred.size();
			
			if (forceUnchokes)
				choker.unchokesSinceLast = 0;
			else
				choker.unchokesSinceLast += numNonPref;
			
			for (Iterator iter = connections.iterator(); iter.hasNext();) {
				BTConnection con = (BTConnection) iter.next();
				if (preferred.contains(con))
					continue;
				if (!con.isInterested())
					con.sendChoke();
				else if (con.isChoked() && numNonPref > 0 && 
						con.shouldBeInterested()) {
						con.sendUnchoke(choker.round);
						numNonPref--;
				}
				else {
					if (numNonPref == 0 || !con.shouldBeInterested())
						con.sendChoke();
					else
						numNonPref--;
				}
			}
		}
	}
	
	/**
	 * @return the number of uploads that should be unchoked
	 * Note: Copied verbatim from mainline BT
	 */
	private static int getNumUploads() {
		int uploads = BittorrentSettings.TORRENT_MAX_UPLOADS.getValue();
		if (uploads > 0)
			return uploads;
		
		float rate = UploadManager.getUploadSpeed();
		if (rate == Float.MAX_VALUE)
			return 7; //"unlimited, just guess something here..." - Bram
		else if (rate < 9000)
			return 2;
		else if (rate < 15000)
			return 3;
		else if (rate < 42000)
			return 4;
		else 
			return (int) Math.sqrt(rate * 0.6f);

	}

	/**
	 * Chokes all connections instantly and does not unchoke any of them until
	 * it is set to false again. This effectively suspends all uploads and it is
	 * used while we are moving the files from the incomplete folder to the
	 * complete folder.
	 * 
	 * @param choke
	 *            whether to choke all connections.
	 */
	private void setGlobalChoke(boolean choke) {
		_globalChoke = choke;
		if (choke) {
			for (Iterator iter = _connections.iterator(); iter.hasNext();) {
				BTConnection btc = (BTConnection) iter.next();
				btc.sendChoke();
			}
		} else
			rechoke();
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
		return _state == PAUSED;
	}

	public boolean equals(Object o) {
		if (!(o instanceof ManagedTorrent))
			return false;
		ManagedTorrent mt = (ManagedTorrent) o;

		return Arrays.equals(mt.getInfoHash(), getInfoHash());
	}

	/*
	 * Compares two BTConnections by their average download 
	 * or upload speeds.  Higher speeds get preference.
	 */
	public static class SpeedComparator implements Comparator {
		
		private final boolean download;
		public SpeedComparator(boolean download) {
			this.download = download;
		}
		
		// requires both objects to be of type BTConnection
		public int compare(Object o1, Object o2) {
			if (o1 == o2)
				return 0;
			
			BTConnection c1 = (BTConnection) o1;
			BTConnection c2 = (BTConnection) o2;
			
			float bw1 = c1.getMeasuredBandwidth(download);
			float bw2 = c2.getMeasuredBandwidth(download);
			
			if (bw1 == bw2)
				return 0;
			else if (bw1 > bw2)
				return -1;
			else
				return 1;
		}
	}
	
	/**
	 * A comparator that compares BT connections by the number of
	 * unchoke round they were unchoked.  Connections with higher 
	 * round get preference.
	 */
	private static class UnchokeComparator implements Comparator {
		public int compare(Object a, Object b) {
			if (a == b)
				return 0;
			BTConnection con1 = (BTConnection) a;
			BTConnection con2 = (BTConnection) b;
			if (con1.getUnchokeRound() != con2.getUnchokeRound())
				return -1 * (con1.getUnchokeRound() - con2.getUnchokeRound());
			return UPLOAD_SPEED_COMPARATOR.compare(con1, con2);
		}
	}

	public Throttle getUploadThrottle() {
		return _manager.getUploadThrottle();
	}

	private void enqueueTask(Runnable runnable) {
		torrentStateQueue.add(runnable);
	}

	public boolean isConnected() {
		return _connections.size() > 0;
	}

	public boolean isActive() {
		switch(_state) {
		case CONNECTING:
		case DOWNLOADING:
		case SEEDING: 
		case WAITING_FOR_TRACKER:
		case VERIFYING:
		case SAVING:
			return true;
		}
		return false;
	}

	public List getConnections() {
		return _connections;
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
			for (Iterator iter = _connections.iterator(); iter.hasNext();) {
				BTConnection con = (BTConnection) iter.next();
				if (!con.isInteresting())
					busy++;
			}
		}
		return busy;
	}

	public BTUploader getUploader() {
		return _uploader;
	}

	public BTDownloader getDownloader() {
		return _downloader;
	}


	boolean hasNonBusyLocations() {
		long now = System.currentTimeMillis();
		synchronized(_peers) {
			Iterator iter = _peers.iterator();
			while (iter.hasNext()) {
				TorrentLocation to = (TorrentLocation) iter.next();
				if (!to.isBusy(now))
					return true;
			}
		}
		return false;
	}
	
	/**
	 * Notification that a new tracker has been added to the torrent.
	 * @param t the new <tt>Tracker</tt>
	 */
	void trackerAdded(Tracker t) {
		trackerManager.add(t);
	}
	
	/**
	 * whether or not continuing is hopeless
	 */
	boolean shouldStop() {
		if (_connections.size() == 0 && _peers.size() == 0) {
			int trackerFailures = trackerManager.getTrackerFailures();
			if (trackerFailures > MAX_TRACKER_FAILURES) {
				if (LOG.isDebugEnabled())
					LOG.debug("giving up, trackerFailures "
							+ trackerFailures);
				return true;
			} else if (_manager.shouldYield()) {
				if (LOG.isDebugEnabled())
					LOG.debug("making way for other downloader");
				return true;
			}
		} else if (_folder.isComplete() && _manager.shouldYield()) {
			// we stop if we uploaded more than we downloaded
			// AND there are other torrents waiting for a slot
			if (LOG.isDebugEnabled())
				LOG.debug("uploaded data "
						+ _uploader.getTotalAmountUploaded()
						+ " downloaded data "
						+ _downloader.getTotalAmountDownloaded());
			if (_uploader.getTotalAmountUploaded() > _downloader
					.getTotalAmountDownloaded())
				return true;
		}
		return false;
	}
	
	public BTConnectionFetcher getFetcher() {
		return _connectionFetcher;
	}
}
