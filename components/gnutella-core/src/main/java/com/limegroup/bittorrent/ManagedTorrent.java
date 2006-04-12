package com.limegroup.bittorrent;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.MessageService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.io.Throttle;
import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.bittorrent.messages.BTHave;
import com.limegroup.gnutella.util.CoWList;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.ProcessingQueue;

public class ManagedTorrent {
	private static final Log LOG = LogFactory.getLog(ManagedTorrent.class);

	/*
	 * extension bytes
	 */
	static final byte[] ZERO_BYTES = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00 };

	/*
	 * the number of failures after which we consider giving up
	 */
	private static final int MAX_TRACKER_FAILURES = 5;

	/*
	 * used to order BTConnections according to the average download speed.
	 */
	private static final Comparator DOWNLOAD_SPEED_COMPARATOR = new DownloadSpeedComparator();

	/*
	 * time in milliseconds between choking/unchoking of connections
	 */
	private static final int RECHOKE_TIMEOUT = 30 * 1000;

	/*
	 * the TorrentManager managing this torrent
	 */
	private final TorrentManager _manager;

	/*
	 * Indicates whether this download was stopped.
	 */
	private volatile boolean _stopped = true;
	
	/** Whether the torrent is being verified */
	private volatile boolean verifying;

	/*
	 * The list of known good TorrentLocations that we are not connected to at
	 * the moment get this' monitor befor accessing
	 */
	private Set _peers;

	/*
	 * The list of known bad TorrentLocations
	 */
	private Set _badPeers;

	/*
	 * the meta info for this torrent
	 */
	private BTMetaInfo _info;

	/*
	 * the handle for all files.
	 */
	private VerifyingFolder _folder;

	/*
	 * counts the number of consecutive tracker failures
	 */
	private int _trackerFailures = 0;

	/*
	 * this is true if the download is complete
	 */
	private volatile boolean _complete = false;

	/*
	 * if we could not save this file
	 */
	private boolean _couldNotSave = false;

	/*
	 * 
	 */
	private BTConnectionFetcher _connectionFetcher;

	/*
	 * whether to choke all connections
	 */
	private volatile boolean _globalChoke = false;

	/*
	 * whether this download was paused.
	 */
	private boolean _paused = false;

	/*
	 * it is possible that a complete download is restarted (for what purpose so
	 * ever) so we need to remember that we already saved the file.
	 */
	private boolean _saved = false;

	/*
	 * get this' monitor befor accessing
	 */
	private List _connections;

	private ProcessingQueue _processingQueue;

	private BTDownloader _downloader;

	private BTUploader _uploader;

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
		_connections = new CoWList(CoWList.ARRAY_LIST);
		_processingQueue = new ProcessingQueue("ManagedTorrent");
		_downloader = new BTDownloader(this, _info);
		_uploader = new BTUploader(this, _info);
		_connectionFetcher = new BTConnectionFetcher(this,_manager.getPeerId());
		_peers = Collections.EMPTY_SET;
		_badPeers = Collections.EMPTY_SET;
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
		return _complete;
	}

	/**
	 * Starts the torrent synchronized so we wait until a previous runner has
	 * finished before starting a new one.
	 */
	public synchronized void start() {
		if (!_stopped)
			return;
		_stopped = false;

		enqueueTask(new Runnable() {
			public void run() {
				initializeTorrent();
				initializeFolder();
				if (_stopped || _complete) //TODO: decide if we should connect to seed.
					return;
				// kick off connectors if we already have some addresses
				if (_peers.size() > 0)
					_connectionFetcher.schedule(0);
				// connect to tracker
				announceStart();
				// start the choking / unchoking of connections
				scheduleRechoke();

				if (LOG.isDebugEnabled())
					LOG.debug("started connection fetcher");
			}
		});
	}

	/**
	 * Stops the torrent
	 */
	public void stop() {
		if (LOG.isDebugEnabled())
			LOG.debug("Stopping torrent", new Exception());
		
		_stopped = true;
		
		enqueueTask(new Runnable() {
			public void run() {

				RouterService.getCallback().removeUpload(_uploader);

				_folder.close();

				for (int i = 0; i < _info.getTrackers().length; i++)
					announceBlocking(_info.getTrackers()[i],
							TrackerRequester.EVENT_STOP);

				// close connections
				for (Iterator iter = getConnections().iterator(); iter
						.hasNext();) {
					((BTConnection) iter.next()).close();
				}

				// we stopped, removing torrent from active list of
				// TorrentManager
				_manager.removeTorrent(ManagedTorrent.this, _complete);
				if (LOG.isDebugEnabled())
					LOG.debug("Torrent stopped!");
			}
		});
	}

	/**
	 * Forces this ManagedTorrent to make way for other ManagedTorrents, if
	 * there are any
	 */
	public void pause() {
		stop();
		_paused = true;
	}

	/**
	 * Resumes the torrent, if there is a free slot
	 */
	public boolean resume() {
		if (!_stopped)
			return false;

		if (!_couldNotSave) {
			_paused = false;
			_manager.wakeUp(this);
			return true;
		}
		return false;
	}

	void connectionClosed(BTConnection btc) {
		if (btc.isWorthRetrying()) {
			TorrentLocation ep = new TorrentLocation(btc.getEndpoint());
			ep.strike();
			_peers.add(ep);
		}
		removeConnection(btc);
		_connectionFetcher.schedule(0);
	}

	private void initializeTorrent() {
		_couldNotSave = false;
		_globalChoke = false;
		_paused = false;
		_badPeers = Collections.synchronizedSet(
				new FixedSizeExpiringSet(500, 60 * 60 * 1000));
		_peers = Collections.synchronizedSet(new HashSet());
		if (_info.getLocations() != null)
			_peers.addAll(_info.getLocations());

		RouterService.getCallback().addUpload(_uploader);

		if (LOG.isDebugEnabled())
			LOG.debug("Starting torrent");
	}

	private void initializeFolder() {
		try {
			verifying = true;
			_folder.open();
		} catch (IOException ioe) {
			// problem opening files cannot recover.
			if (LOG.isDebugEnabled()) {

				LOG.debug("unrecoverable error", ioe);
			}
			_couldNotSave = true;
			_stopped = true;
			return;
		} finally {
			verifying = false;
		}
		_complete = _folder.isComplete();

		if (_complete)
			saveCompleteFiles();
	}

	private void announceStart() {
		// announce ourselves to the trackers
		for (int i = 0; i < _info.getTrackers().length; i++) {
			announceBlocking(_info.getTrackers()[i],
					TrackerRequester.EVENT_START);
		}
	}

	/**
	 * Handles requesting ranges from the remote host
	 * 
	 * @param btc
	 *            the BTConnection to request from
	 */
	public void request(final BTConnection btc) {
		enqueueTask(new Runnable() {
			public void run() {
				if (LOG.isDebugEnabled())
					LOG.debug("requesting ranges from " + btc.toString());
				// don't request if complete
				if (_complete || _stopped)
					return;
				BTInterval in = _folder.leaseRandom(btc.getAvailableRanges());
				if (in != null)
					btc.sendRequest(in);
				else if (btc.getAvailableRanges().isEmpty()) {
					if (LOG.isDebugEnabled())
						LOG
								.debug("leaseRarest returned null, btc connection not interesting anymore");
					btc.sendNotInterested();
				} else {
					if (LOG.isDebugEnabled())
						LOG
								.debug("leaseRarest returned null, btc connection still interesting ??!?!?");
				}
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
		BTHave have = BTHave.createMessage(in);
		for (Iterator iter = getConnections().iterator(); iter.hasNext();) {
			BTConnection btc = (BTConnection) iter.next();
			btc.sendHave(have);
		}
		_complete = _folder.isComplete();
		if (_complete && !verifying) {
			LOG.info("file is complete");
			enqueueTask(new Runnable(){
				public void run(){
					saveCompleteFiles();
				}
			});
		}
	}

	/**
	 * @return a Set of <tt>TorrentLocation</tt> containing all endpoints we
	 *         have outgoing connections to
	 */
	public Set getAltLocs() {
		Set set = new HashSet();
		for (Iterator iter = getConnections().iterator(); iter.hasNext();) {
			BTConnection btc = (BTConnection) iter.next();

			if (btc.isOutgoing())
				set.add(btc.getEndpoint());
		}
		return set;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.limegroup.gnutella.Downloader#getState()
	 */
	public int getState() {
		if (_stopped) {
			if (_couldNotSave)
				return Downloader.DISK_PROBLEM;
			else if (_complete)
				return Downloader.COMPLETE;
			else if (_trackerFailures > MAX_TRACKER_FAILURES)
				return Downloader.GAVE_UP;
			else if (_paused)
				return Downloader.PAUSED;
			else
				return Downloader.QUEUED;
		}

		if (_complete)
			return Downloader.SEEDING;
		else if (_connections.size() > 0) {
			if (isDownloading())
				return Downloader.DOWNLOADING;
			return Downloader.REMOTE_QUEUED;
		} else if (_peers != null && _peers.size() > 0)
			return Downloader.CONNECTING;
		else if (verifying)
			return Downloader.HASHING;
		else if(_peers == null || _peers.size() == 0)
			return Downloader.WAITING_FOR_TRACKER;
		return Downloader.BUSY;
	}

	private boolean isDownloading() {
		for (Iterator iter = getConnections().iterator(); iter.hasNext();)
			if (!((BTConnection) iter.next()).isChoking())
				return true;
		return false;
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
			_connectionFetcher.schedule(0);
			return true;
		}
		return false;
	}
	
	void addBadEndpoint(TorrentLocation to) {
		_badPeers.add(to);
	}

	/**
	 * This method handles the response from a tracker
	 * 
	 * @param is
	 */
	private void handleTrackerResponse(TrackerResponse response, URL url) {
		LOG.debug("handling tracker response " + url.toString());

		long minWaitTime = BittorrentSettings.TRACKER_MIN_REASK_INTERVAL
				.getValue() * 1000;
		try {
			// will be caught below.
			if (response == null) {
				LOG.debug("null response");
				throw new IOException();
			}

			for (Iterator iter = response.PEERS.iterator(); iter.hasNext();) {
				TorrentLocation next = (TorrentLocation) iter.next();
				addEndpoint(next);
			}

			minWaitTime = response.INTERVAL * 1000;

			if (response.FAILURE_REASON != null && _trackerFailures == 0) {
				MessageService.showError("TORRENTS_TRACKER_FAILURE", _info
						.getName()
						+ "\n" + response.FAILURE_REASON);
				throw new IOException("Tracker request failed.");
			}
			_trackerFailures = 0;
		} catch (ValueException ve) {
			if (LOG.isDebugEnabled())
				LOG.debug(ve);
			_trackerFailures++;
		} catch (IOException ioe) {
			if (LOG.isDebugEnabled())
				LOG.debug(ioe);
			_trackerFailures++;
		}

		if (!_stopped && _trackerFailures < MAX_TRACKER_FAILURES) {
			scheduleTrackerRequest(minWaitTime, url);
		}
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

	public void acceptConnection(Socket sock, byte[] bytes) {
		_connectionFetcher.acceptConnection(sock, bytes);
	}
	
	/**
	 * @param ep
	 *            the TorrentLocation for the connection to add
	 * @return true if we want this connection, false if not.
	 */
	boolean allowIncomingConnection(TorrentLocation ep) {
		// happens if we stopped this torrent but we still receive an incoming
		// connection because it took some time to read the headers & stuff
		if (_stopped)
			return false;

		// this could still happen, although we don't usually accept any
		// locations we are already connected to.
		if (isConnectedTo(ep))
			return false;

		// don't allow connections to self
		if (NetworkUtils.isMe(ep.getAddress(), ep.getPort()))
			return false;

		// we do a little bit of preferencing here, - we support some features
		// others don't - and really, LimeWire users should help each other.
		// we won't do any nasty stuff like preferring LimeWire's when
		// uploading b/c that would just be mean
		if (ep.isLimePeer()) {
			return _connections.size() < BittorrentSettings.TORRENT_RESERVED_LIME_SLOTS
					.getValue()
					+ BittorrentSettings.TORRENT_MAX_CONNECTIONS.getValue();
		}
		return _connections.size() < BittorrentSettings.TORRENT_MAX_CONNECTIONS
				.getValue();
	}

	/**
	 * private helper method, adding connection
	 */
	void addConnection(final BTConnection btc) {
		if (LOG.isDebugEnabled())
			LOG.debug("trying to add connection " + btc.toString());
		// this check prevents a few exceptions that may be thrown if a
		// connection initialization is completed after we have already stopped
		// happens especially when a user quits a torrent manually.
		if (_stopped) {
			btc.close();
			return;
		}
		_connections.add(btc);
		if (LOG.isDebugEnabled())
			LOG.debug("added connection " + btc.toString());
	}

	/**
	 * private helper method, removing connection
	 */
	private void removeConnection(final BTConnection btc) {
		if (LOG.isDebugEnabled())
			LOG.debug("removing connection " + btc.toString());
		_connections.remove(btc);
	}

	/**
	 * saves the complete files to the shared folder
	 */
	private void saveCompleteFiles() {
		if (_saved)
			return;

		// stop all uploads
		if (LOG.isDebugEnabled())
			LOG.debug("global choke");
		setGlobalChoke(true);

		for (Iterator iter = getConnections().iterator(); iter.hasNext();) {
			BTConnection btc = (BTConnection) iter.next();
			// close connections that aren't interested in any part of a
			// complete torrent
			if (!btc.isInterested())
				btc.close();
			else {
				// cancel all requests, if there are any left. (This should not
				// be the case at this point anymore)
				btc.cancelAllRequests();
				btc.sendNotInterested();
			}
		}
		saveFiles();
		announceComplete();
	}

	/**
	 * Save the complete files to disc. this is executed within the timer thread
	 * but since we don't want to upload or download anything while we are
	 * saving the files, it should be okay
	 */
	private void saveFiles() {
		if (_saved)
			return;
		// synchronizing on _folder because multiple threads are
		// accessing the VerifyingFolder since we offloaded verifying, reading
		// and writing from the main thread
		synchronized (_folder) {
			_folder.close();
			if (LOG.isDebugEnabled())
				LOG.debug("folder closed");
			_couldNotSave = !_info.moveToCompleteFolder();
			if (LOG.isDebugEnabled())
				LOG.debug("could not save: " + _couldNotSave);
			// folder has to be updated with the new files
			_folder = _info.getVerifyingFolder();
			if (LOG.isDebugEnabled())
				LOG.debug("new veryfing folder");

			try {
				_folder.open();
			} catch (IOException ioe) {
				LOG.debug(ioe);
				_couldNotSave = true;
			}
		}
		if (LOG.isDebugEnabled())
			LOG.debug("folder opened");

		if (_couldNotSave)
			stop();

		// resume uploads
		setGlobalChoke(false);

		// remember attempt to save the file
		_saved = true;
	}
	
	private void announceComplete() {
		//TODO: should we announce how much we've downloaded if we just resumed
		// the torrent?  Its not mentioned in the spec...
		for (int i = 0; i < _info.getTrackers().length; i++) {
			announceBlocking(_info.getTrackers()[i],
					TrackerRequester.EVENT_COMPLETE);
		}
	}

	/**
	 * schedules a new TrackerRequest
	 * 
	 * @param minDelay
	 *            the time in milliseconds to wait before sending another
	 *            request
	 * @param url
	 *            the URL of the tracker
	 */
	private void scheduleTrackerRequest(long minDelay, final URL url) {
		Runnable announcer = new Runnable() {
			public void run() {
				if (LOG.isDebugEnabled())
					LOG.debug("announcing to " + url.toString());
				announce(url);
			}
		};
		// a tracker request can take quite a few seconds (easily up to 30)
		// it will slow us down since we cannot enqueue any further pieces
		// during that time - it may become necessary to do tracker requests
		// in their own thread
		RouterService.schedule(announcer, minDelay, 0);
	}

	/**
	 * @param to
	 *            a <tt>TorrentLocation</tt> to check
	 * @return true if we are apparently already connected to a certain location
	 */
	private boolean isConnectedTo(TorrentLocation to) {
		for (Iterator iter = getConnections().iterator(); iter.hasNext();) {
			BTConnection btc = (BTConnection) iter.next();
			// compare by address only. there's no way of comparing ports
			// or peer ids
			if (btc.getEndpoint().getAddress().equals(to.getAddress()))
				return true;
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
		Runnable choker = new Runnable() {
			public void run() {
				if (_stopped)
					return;
				if (LOG.isDebugEnabled())
					LOG.debug("scheduling rechoke");
				rechoke();
				// re-schedule if not stopped
				RouterService.schedule(this, RECHOKE_TIMEOUT, 0);
			}
		};
		RouterService.schedule(choker, RECHOKE_TIMEOUT, 0);
	}

	/**
	 * rechokes the connections. Unchokes the first connection in _btConnections
	 * and the connections that have uploaded the most to us
	 */
	private void rechoke() {
		if (_globalChoke)
			return;

		LinkedList candidates = new LinkedList();

		// add all interested connections to candidates
		for (Iterator iter = getConnections().iterator(); iter.hasNext();) {
			BTConnection btc = (BTConnection) iter.next();
			if (btc.isInterested())
				candidates.add(btc);
		}
		Collections.shuffle(candidates);

		int unchoked = 0;
		List bad = new ArrayList();
		// unchoke TORRENT_MIN_UPLOADS connections in order to give everyone a
		// fair chance
		while (candidates.size() > 0
				&& unchoked < BittorrentSettings.TORRENT_MIN_UPLOADS.getValue()) {
			BTConnection c = (BTConnection) candidates.removeFirst();

			if (!c.getEndpoint().isShareazaPeer()) {
				// shareaza does not enforce any share ratio whatsoever, it
				// will have to earn its upload slots by uploading something
				// to us.
				c.sendUnchoke();
				unchoked++;
			} else {
				bad.add(c);
			}
		}

		// re-add bad hosts that don't get optimistic unchoke.
		candidates.addAll(bad);

		// unchoke the hosts that are uploading the most to us
		Collections.sort(candidates, DOWNLOAD_SPEED_COMPARATOR);

		while (candidates.size() > 0
				&& unchoked < BittorrentSettings.TORRENT_MAX_UPLOADS.getValue()) {
			BTConnection c = (BTConnection) candidates.removeFirst();
			c.sendUnchoke();
			unchoked++;
		}

		// choke the rest.
		while (candidates.size() > 0) {
			BTConnection c = (BTConnection) candidates.removeFirst();
			c.sendChoke();
		}
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
			for (Iterator iter = getConnections().iterator(); iter.hasNext();) {
				BTConnection btc = (BTConnection) iter.next();
				btc.sendChoke();
			}
		} else
			rechoke();
	}

	/**
	 * Announces ourselves to a tracker
	 * 
	 * @param url
	 *            the URL of the tracker
	 */
	private void announce(final URL url) {
		// offload tracker requests, - it simply takes too long even to execute
		// it in our timer thread
		Runnable trackerRequest = new Runnable() {
			public void run() {
				if (LOG.isDebugEnabled())
					LOG.debug("announce thread for " + url.toString());
				if (!_stopped)
					return;
				announceBlocking(url, TrackerRequester.EVENT_NONE);
			}
		};
		ManagedThread thread = new ManagedThread(trackerRequest,
				"TrackerRequest");
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Announce ourselve to a tracker
	 * 
	 * @param url
	 *            the <tt>URL</tt> for the tracker
	 * @param event
	 *            the event to send to the tracker, see TrackerRequester class
	 */
	private void announceBlocking(final URL url, final int event) {
		if (LOG.isDebugEnabled())
			LOG.debug("connecting to tracker " + url.toString()+" for event "+event);
		TrackerResponse tr = TrackerRequester.request(url, _info,
				ManagedTorrent.this, event);
		handleTrackerResponse(tr, url);
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
		return _paused;
	}

	public boolean equals(Object o) {
		if (!(o instanceof ManagedTorrent))
			return false;
		ManagedTorrent mt = (ManagedTorrent) o;

		return Arrays.equals(mt.getInfoHash(), getInfoHash());
	}

	/*
	 * Compares two BTConnections by their average download speeds
	 */
	public static class DownloadSpeedComparator implements Comparator {
		// requires both objects to be of type BTConnection
		public int compare(Object o1, Object o2) {
			BTConnection c1 = (BTConnection) o1;
			BTConnection c2 = (BTConnection) o2;
			float bw = 0;
			try {
				bw = c1.getMessageReader().getBandwidthTracker()
						.getMeasuredBandwidth()
						- c2.getMessageReader().getBandwidthTracker()
								.getMeasuredBandwidth();
				if (bw > 0.1) // c1 greater, -> preference c1 over c2,
					return -1;
				else if (bw < -0.1)
					return 1;
			} catch (InsufficientDataException ide) {
				// ignored
			}

			bw = c1.getMessageReader().getBandwidthTracker()
					.getAverageBandwidth()
					- c2.getMessageReader().getBandwidthTracker()
							.getAverageBandwidth();
			if (bw > 0)
				return -1;
			else if (bw < 0)
				return 1;
			return 0;
		}

		public boolean equals(Object o) {
			// not implemented
			return false;
		}
	}

	public Throttle getDownloadThrottle() {
		// TODO - in case we add a download throttle
		return null;
	}

	public Throttle getUploadThrottle() {
		return _manager.getUploadThrottle();
	}

	public void enqueueTask(Runnable runnable) {
		_processingQueue.add(runnable);
	}

	public boolean isConnected() {
		return _connections.size() > 0;
	}

	public boolean hasStopped() {
		return _stopped;
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
		long now = System.currentTimeMillis();
		synchronized(_peers) {
			for (Iterator iter = _peers.iterator(); iter.hasNext();)
				if (((TorrentLocation) iter.next()).isBusy(now))
					busy++;
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
	 * whether or not continuing is hopeless
	 */
	boolean shouldStop() {
		if (_connections.size() == 0 && _peers.size() == 0) {
			if (_trackerFailures > MAX_TRACKER_FAILURES) {
				if (LOG.isDebugEnabled())
					LOG.debug("giving up, trackerFailures "
							+ _trackerFailures);
				return true;
			} else if (_manager.shouldYield()) {
				if (LOG.isDebugEnabled())
					LOG.debug("making way for other downloader");
				return true;
			}
		} else if (_complete && _manager.shouldYield()) {
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
	
}
