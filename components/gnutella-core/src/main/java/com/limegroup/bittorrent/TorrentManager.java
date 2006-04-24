package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.io.NBThrottle;
import com.limegroup.gnutella.io.NIOSocket;
import com.limegroup.gnutella.io.Throttle;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.*;
import com.limegroup.bittorrent.handshaking.IncomingBTHandshaker;
import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.ConverterObjectInputStream;
import com.limegroup.gnutella.util.FileUtils;

public class TorrentManager implements ConnectionAcceptor {
	/*
	 * the upload throttle we are using
	 */
	private static final Throttle UPLOAD_THROTTLE = new NBThrottle(true,
			DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.getValue());

	private final byte[] PEER_ID;

	private static final Log LOG = LogFactory.getLog(TorrentManager.class);

	/**
	 * The time in milliseconds between checkpointing downloads.dat. The more
	 * often this is written, the less the lost data during a crash, but the
	 * greater the chance that downloads.dat itself is corrupt.
	 */
	private int SNAPSHOT_CHECKPOINT_TIME = 60 * 1000;

	/**
	 * The list of all ManagedTorrent's attempting to download. INVARIANT:
	 * active.size() <=slots() && active contains no duplicates LOCKING: obtain
	 * this' monitor
	 */
	private List /* of ManagedTorrent */_active = new LinkedList();

	/**
	 * The list of all queued ManagedTorrent. INVARIANT: waiting contains no
	 * duplicates LOCKING: obtain this' monitor
	 */
	private List /* of ManagedTorrent */_waiting = new LinkedList();

	/**
	 * current and average upload and download speeds
	 */
	private float _currentUpload, _currentDownload, _averageUpload,
			_averageDownload;

	/**
	 * the number of measures that were taken to create the current values of
	 * _averageUpload/_averageDownload
	 */
	private int _numMeasures;

	/**
	 * the callback handling uploaders and downloaders
	 */
	private ActivityCallback _callback;

	/**
	 * Constructs instance of TorrentManager
	 */
	public TorrentManager() {
		String clientId = ApplicationSettings.CLIENT_ID.getValue();
		byte[] guid;
		if (clientId.length() != 0 && clientId != null)
			guid = GUID.fromHexString(clientId);
		else
			guid = GUID.makeGuid();
		PEER_ID = new byte[20];
		String qhdVendorName = CommonUtils.QHD_VENDOR_NAME;
		PEER_ID[0] = (byte) qhdVendorName.charAt(0);
		PEER_ID[1] = (byte) qhdVendorName.charAt(1);
		PEER_ID[2] = (byte) qhdVendorName.charAt(2);
		PEER_ID[3] = (byte) qhdVendorName.charAt(3);
		System.arraycopy(guid, 0, PEER_ID, 4, 16);

		if (LOG.isDebugEnabled())
			LOG.debug("TorrentManager created");
	}

	/**
	 * Initializes this. Always call this method before starting any torrents.
	 */
	public void initialize() {
		if (LOG.isDebugEnabled())
			LOG.debug("initializing TorrentManager");
		_callback = RouterService.getCallback();

		File real = BittorrentSettings.TORRENT_SNAPSHOT_FILE.getValue();
		File backup = BittorrentSettings.TORRENT_SNAPSHOT_BACKUP_FILE
				.getValue();
		// Try once with the real file, then with the backup file.
		if (!readSnapshot(real)) {
			if (LOG.isDebugEnabled())
				LOG.debug("Reading real torrents.dat failed");
			// if backup succeeded, copy into real.
			if (readSnapshot(backup)) {
				if (LOG.isDebugEnabled())
					LOG.debug("Reading backup torrents.bak succeeded.");
				copyBackupToReal();
				// only show the error if the files existed but couldn't be
				// read.
			} else if (backup.exists() || real.exists()) {
				if (LOG.isDebugEnabled())
					LOG.debug("Reading both torrents files failed.");
				MessageService.showError("TORRENTS_COULD_NOT_READ_SNAPSHOT");
			}
		} else {
			if (LOG.isDebugEnabled())
				LOG.debug("Reading torrents.dat worked!");
		}

		Runnable checkpointer = new Runnable() {
			public void run() {
				if (_active.size() > 0) { // optimization
					// If the write failed, move the backup to the real.
					if (!writeSnapshot())
						copyBackupToReal();
				}
			}
		};
		RouterService.schedule(checkpointer, SNAPSHOT_CHECKPOINT_TIME,
				SNAPSHOT_CHECKPOINT_TIME);

		Runnable waitingPimp = new Runnable() {
			public void run() {
				wakeUp();
			}
		};
		RouterService.schedule(waitingPimp, 10 * 1000, 10 * 1000);
		
		// register ourselves as an acceptor.
		StringBuffer word = new StringBuffer();
		word.append((char)19);
		word.append("BitTorrent");
		RouterService.getConnectionDispatcher().addConnectionAcceptor(
				this,
				new String[]{word.toString()},
				false,false);
	}

	/**
	 * Copies the backup torrents.dat (torrents.bak) file to the the real
	 * torrents.dat location.
	 */
	private synchronized void copyBackupToReal() {
		if (LOG.isDebugEnabled())
			LOG.debug("copying backup file to main saving file");
		File real = BittorrentSettings.TORRENT_SNAPSHOT_FILE.getValue();
		File backup = BittorrentSettings.TORRENT_SNAPSHOT_BACKUP_FILE
				.getValue();
		real.delete();
		CommonUtils.copy(backup, real);
	}

	/**
	 * Writes a snapshot of all torrents in this to the file named
	 * TORRENT_SNAPSHOT_FILE. It is safe to call this method at any time for
	 * checkpointing purposes. Returns true iff the file was successfully
	 * written.
	 */
	public synchronized boolean writeSnapshot() {
		LOG.debug("writing snapshot");
		List buf = new ArrayList();
		for (int i = 0; i < _active.size(); i++) {
			ManagedTorrent mt = (ManagedTorrent) _active.get(i);
			if (!mt.isComplete())
				buf.add(mt.getMetaInfo());
		}

		for (int i = 0; i < _waiting.size(); i++) {
			ManagedTorrent mt = (ManagedTorrent) _waiting.get(i);
			if (!mt.isComplete())
				buf.add(mt.getMetaInfo());
		}

		File outFile = BittorrentSettings.TORRENT_SNAPSHOT_FILE.getValue();
		// must delete in order for renameTo to work.
		BittorrentSettings.TORRENT_SNAPSHOT_BACKUP_FILE.getValue().delete();
		outFile.renameTo(BittorrentSettings.TORRENT_SNAPSHOT_BACKUP_FILE
				.getValue());

		// Write list of BTMetaInfo.
		try {
			ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream(
							BittorrentSettings.TORRENT_SNAPSHOT_FILE.getValue()));
			try {
				out.writeObject(buf);
				out.flush();
				if (LOG.isDebugEnabled())
					LOG.debug("snapshot written");
				return true;
			} finally {
				out.close();
			}
		} catch (IOException e) {
			if (!FileUtils.forceRename(
					BittorrentSettings.TORRENT_SNAPSHOT_BACKUP_FILE.getValue(),
					BittorrentSettings.TORRENT_SNAPSHOT_FILE.getValue())) {
				ErrorService
						.error(e,
								"could not save torrents.dat file, backup failed, please restart LimeWire.");
			}
			if (LOG.isDebugEnabled())
				LOG.debug("snapshot not written",e);
			return false;
		}
	}

	/**
	 * Reads the torrents serialized in TORRENT_SNAPSHOT_FILE and adds them to
	 * this, queued. The queued torrents will restart immediately if slots are
	 * available. Returns false iff the file could not be read for any reason.
	 * THIS METHOD SHOULD BE CALLED BEFORE ANY GUI ACTION. It is public for
	 * testing purposes only!
	 * 
	 * @param file
	 *            the torrents.dat snapshot file
	 */
	public synchronized boolean readSnapshot(File file) {
		if (LOG.isDebugEnabled())
			LOG.debug("reading Snapshot");
		// Read BTMetaInfo from disk.
		List buf = null;
		try {
			ObjectInputStream in = new ConverterObjectInputStream(
					new FileInputStream(file));
			// This does not try to maintain backwards compatibility with older
			// versions of LimeWire, which only wrote the list of torrents.
			// This doesn't really cause an errors, however.
			buf = (List) in.readObject();
		} catch (IOException e) {
			LOG.debug(e);
			return false;
		} catch (ClassCastException e) {
			LOG.debug(e);
			return false;
		} catch (ClassNotFoundException e) {
			LOG.debug(e);
			return false;
		} catch (ArrayStoreException e) {
			LOG.debug(e);
			return false;
		} catch (IndexOutOfBoundsException e) {
			LOG.debug(e);
			return false;
		} catch (NegativeArraySizeException e) {
			LOG.debug(e);
			return false;
		} catch (IllegalStateException e) {
			LOG.debug(e);
			return false;
		} catch (SecurityException e) {
			LOG.debug(e);
			return false;
		}

		// Initialize and start torrents. Must catch ClassCastException since
		// the data could be corrupt.
		try {
			for (Iterator iter = buf.iterator(); iter.hasNext();) {
				BTMetaInfo info = (BTMetaInfo) iter.next();
				ManagedTorrent torrent = new ManagedTorrent(info, this);
				addTorrent(torrent); // 1
				_callback.addDownload(torrent.getDownloader()); // 2
			}
			if (LOG.isDebugEnabled())
				LOG.debug("snapshot read");
			return true;
		} catch (ClassCastException e) {
			return false;
		}
	}

	/**
	 * adds a Torrent. If we have an open slot, the torrent will be started
	 * immediately. Otherwise it will be queued.
	 * 
	 * @param mt
	 *            the <tt>ManagedTorrent</tt> to add.
	 */
	public synchronized void addTorrent(ManagedTorrent mt) {
		if (_active.contains(mt) || _waiting.contains(mt))
			return;
		if (_active.size() >= getMaxActiveTorrents()) {
			_waiting.add(mt);
			if (LOG.isDebugEnabled())
				LOG.debug("torrent added to waiting");
		} else {
			mt.start();
			if (LOG.isDebugEnabled())
				LOG.debug("torrent added to active");
			_active.add(mt);
		}
		_callback.addDownload(mt.getDownloader());
	}

	/**
	 * This method determines if a torrent should make way for another waiting
	 * torrent.
	 * 
	 * @return true if there is another incomplete torrent waiting in line,
	 *         false if not.
	 */
	public synchronized boolean shouldYield() {
		for (Iterator iter = _waiting.iterator(); iter.hasNext();) {
			ManagedTorrent m2 = (ManagedTorrent) iter.next();
			if (!m2.isComplete())
				return true;
		}
		return false;
	}

	/**
	 * Downloads a torrent file from given location and downloads it.
	 * 
	 * @param url
	 *            the location of the .torrent file
	 * @return a Downloader
	 * @throws IOException
	 *             an IOException if there is a problem downloading the file
	 */
	public synchronized Downloader download(URL url) throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("downloading torrent from " + url);
		HttpMethod get = new GetMethod(url.toExternalForm());
		get.addRequestHeader("User-Agent", CommonUtils.getHttpServer());
		get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(),
				"close");
		get.setFollowRedirects(true);

		HttpClient http = HttpClientManager.getNewClient(Constants.TIMEOUT,
				Constants.TIMEOUT);
		http.executeMethod(get);

		if (get.getStatusCode() < 200 || get.getStatusCode() >= 300)
			throw new IOException("bad status code, downloading .torrent file "
					+ get.getStatusCode());

		return download(get.getResponseBody());
	}

	public synchronized Downloader download(File torrentFile) throws IOException {
		return download(FileUtils.readFileFully(torrentFile));
	}
	
	/**
	 * Starts a new Torrent download
	 * 
	 * @param is
	 *            an <tt>InputStream</tt> for a torrent file
	 * @return a <tt>Downloader</tt> that will be displayed by the GUI
	 * @throws AlreadyDownloadingException
	 * @throws IOException
	 */
	public synchronized Downloader download(byte [] torrentFile) throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("trying to open torrent");
		try {
			BTMetaInfo info = BTMetaInfo.readFromBytes(torrentFile);
			List buf = new ArrayList();
			buf.addAll(_active);
			buf.addAll(_waiting);
			for (Iterator iter = buf.iterator(); iter.hasNext();) {
				ManagedTorrent torrent = (ManagedTorrent) iter.next();
				if (Arrays.equals(info.getInfoHash(), torrent.getInfoHash())) {
					// we will add any trackers of the torrent file we are
					// already downloading
					for (int i = 0; i < info.getTrackers().length; i++)
						torrent.getMetaInfo().addTracker(info.getTrackers()[i]);
					// but we don't start a new download.
					return torrent.getDownloader();
				}
			}
			ManagedTorrent mt = new ManagedTorrent(info, this);
			addTorrent(mt);
			return mt.getDownloader();
		} catch (IOException e) {
			if (LOG.isDebugEnabled())
				LOG.debug("bad torrent file", e);
			throw e;
		}

	}

	/**
	 * Removes an active torrent from the list of active torrents and queues it
	 * up unless it is not complete. If it is complete, it will be discarded. If
	 * the Torrent is not active, it will be removed from the list of waiting
	 * torrents.
	 * 
	 * @param mt
	 *            the <tt>ManagedTorrent</tt> to remove
	 */
	public synchronized void removeTorrent(ManagedTorrent mt, boolean clear) {
		if (!_active.contains(mt) && !_waiting.contains(mt))
			return;
		_active.remove(mt);
		_waiting.remove(mt);
		if (!clear) {
			_waiting.add(mt);
		}

		writeSnapshot();
		// wake up this to maintain the desired parallelism
		wakeUp();
	}

	/**
	 * Accessor for the peer id.
	 * 
	 * @return "LIME" + our GUID as ascii encoded bytes.
	 */
	public byte[] getPeerId() {
		return PEER_ID;
	}

	/**
	 * Starts a ManagedTorrent if we have an open slot
	 * 
	 * @param torrent
	 *            the <tt>ManagedTorrent</tt> that will be started
	 */
	public synchronized void wakeUp(ManagedTorrent torrent) {
		if (_active.size() < getMaxActiveTorrents()
				&& _waiting.contains(torrent)) {
			_waiting.remove(torrent);
			_active.add(torrent);
			torrent.start();
		}
	}

	/**
	 * Starts any ManagedTorrent if we have a slot.
	 */
	public synchronized void wakeUp() {
		// TODO
		// we definitely need some kind of bandwidth throttle that can be
		// applied to both HTTP and torrent uploads. The easiest way to
		// achieve this would probably be to convert HTTP uploads to use NIO.
		UPLOAD_THROTTLE.limit((int)UploadManager.getUploadSpeed());

		Iterator iter = _waiting.iterator();
		while (_active.size() < getMaxActiveTorrents() && iter.hasNext()) {
			ManagedTorrent mt = (ManagedTorrent) iter.next();
			if (mt.getState() != Downloader.GAVE_UP
					&& mt.getState() != Downloader.DISK_PROBLEM
					&& mt.getState() != Downloader.PAUSED) {
				_active.add(mt);
				mt.start();
				iter.remove();
			}
		}
	}

	/**
	 * shutdown all torrents, try to send word to all the trackers, so we don't
	 * stop the torrent without a word.
	 */
	public synchronized void shutdown() {
		for (Iterator iter = _active.iterator(); iter.hasNext();) {
			((ManagedTorrent) iter.next()).stop();
		}
	}

	/**
	 * we will give torrents slight preference over http downloaders by reducing
	 * the number of allowed http downloads by the number of active torrents.
	 * 
	 * @return number of active torrents
	 */
	public synchronized int getNumActiveTorrents() {
		return _active.size();
	}

	/**
	 * measures the bandwidth of all active torrents
	 */
	public synchronized void measureBandwidth() {
		float currentTotalUp, currentTotalDown;
		currentTotalDown = currentTotalUp = 0.f;
		boolean shouldCountAvg = false;
		for (Iterator iter = _active.iterator(); iter.hasNext();) {
			shouldCountAvg = true;
			ManagedTorrent mt = (ManagedTorrent) iter.next();
			mt.getUploader().measureBandwidth();
			mt.getDownloader().measureBandwidth();
			currentTotalDown += mt.getDownloader().getMeasuredBandwidth();
			currentTotalUp += mt.getUploader().getMeasuredBandwidth();
		}
		if (shouldCountAvg) {
			_averageDownload = (_averageDownload * _numMeasures + currentTotalDown) / (_numMeasures +1);
			_averageUpload = (_averageUpload * _numMeasures + currentTotalUp) / (_numMeasures + 1);
			_numMeasures ++;
		}
		_currentDownload = currentTotalDown;
		_currentUpload = currentTotalUp;
	}	

	public float getCurrentDownload() {
		return _currentDownload;
	}
	
	public float getAverageDownload() {
		return _averageDownload;
	}
	
	public float getCurrentUpload() {
		return _currentUpload;
	}
	
	public float getAverageUpload() {
		return _averageUpload;
	}

	/**
	 * Returns the position in queue of a given torrent
	 */
	public synchronized int getPositionInQueue(ManagedTorrent to) {
		if (_active.contains(to))
			return 0;
		return _waiting.indexOf(to) + 1;
	}

	/**
	 * Return upload throttle in use
	 */
	public Throttle getUploadThrottle() {
		return UPLOAD_THROTTLE;
	}

	/**
	 * get number of allowed torrents for this type of connections
	 */
	private int getMaxActiveTorrents() {
		int speed = ConnectionSettings.CONNECTION_SPEED.getValue();
		if (speed <= SpeedConstants.MODEM_SPEED_INT)
			return 1;
		else if (speed <= SpeedConstants.CABLE_SPEED_INT)
			return 2;
		else if (speed <= SpeedConstants.T1_SPEED_INT)
			return 4;
		else
			return 6;

	}
	
	public ManagedTorrent getTorrentForHash(byte [] infoHash) {
		synchronized (this) {
			for (Iterator iter = _active.iterator(); iter.hasNext();) {
				ManagedTorrent current = (ManagedTorrent) iter.next();

				if (Arrays.equals(infoHash, current.getInfoHash())) {
					return current;
				}
			}
		}
		return null;
	}
	
	public void acceptConnection(String word, Socket sock) {
		IncomingBTHandshaker shaker = 
			new IncomingBTHandshaker((NIOSocket)sock, this);
		shaker.startHandshaking();
	}
}
