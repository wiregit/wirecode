package com.limegroup.bittorrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RouterService;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.io.Throttle;
import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.bittorrent.messages.BTHave;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.gnutella.util.ThreadPool;

public class ManagedTorrent {
	
	private static final Log LOG = LogFactory.getLog(ManagedTorrent.class);
	
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
	static final int TRACKER_FAILURE = 11;
	
	private static final ThreadPool INVOKER = 
		new NIODispatcherThreadPool();

	/** the executor of our tasks. */
	private ThreadPool invoker = INVOKER;
	
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
	 * The manager of tracker requests.
	 */
	private TrackerManager trackerManager;
	
	/**
	 * The fetcher of connections.
	 */
	private volatile BTConnectionFetcher _connectionFetcher;

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

	/** 
	 * Queue that changes the state of this torrent and does 
	 * the moving of files to the complete location.
	 */
	private ProcessingQueue torrentStateQueue;

	private BTDownloader _downloader;

	private BTUploader _uploader;
	
	/** 
	 * A runnable that takes care of scheduled periodic rechoking
	 */
	private Choker choker;
	
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
		choker = new Choker(this, invoker);
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
		choker.scheduleRechoke();
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
		
		choker.stop();
		
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
		invoker.invokeLater(closer);
		
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
		invoker.invokeLater(haveNotifier);
		
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

	
	void stopVoluntarily() {
		enqueueTask(new Runnable() {
			public void run() {
				if (_state == SEEDING)
					_state = STOPPED;
				else
					_state = TRACKER_FAILURE;
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

		// stop uploads 
		LOG.debug("global choke");
		choker.setGlobalChoke(true);
		
		// cancel requests
		Runnable r = new Runnable(){
			public void run() {
				for (Iterator iter = _connections.iterator(); iter.hasNext();) {
					BTConnection btc = (BTConnection) iter.next();
					// cancel all requests, if there are any left. (This should not
					// be the case at this point anymore)
					btc.cancelAllRequests();
					btc.sendNotInterested();
				}
			}
		};
		invoker.invokeLater(r);
		
		// save the files to the destination folder
		saveFiles();
		
		// resume uploads
		choker.setGlobalChoke(false);
		
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
		boolean diskProblem = !_info.moveToCompleteFolder();
		if (LOG.isDebugEnabled())
			LOG.debug("could not save: " + diskProblem);
		
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

		if (diskProblem) {
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
		return _state == PAUSED;
	}

	public boolean equals(Object o) {
		if (!(o instanceof ManagedTorrent))
			return false;
		ManagedTorrent mt = (ManagedTorrent) o;

		return Arrays.equals(mt.getInfoHash(), getInfoHash());
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
			if (trackerManager.isHopeless()) {
				LOG.debug("giving up, too many trackerFailures ");
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
	
	private static class NIODispatcherThreadPool implements ThreadPool {
		public void invokeLater(Runnable r) {
			NIODispatcher.instance().invokeLater(r);
		}
	}
}
