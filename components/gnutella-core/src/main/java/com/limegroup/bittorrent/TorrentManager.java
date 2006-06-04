
// Commented for the Learning branch

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

/**
 * The program's single TorrentManager object keeps a list of the torrents we're sharing.
 * 
 * === Getting a torrent ===
 * 
 * To get a torrent, call one of the 3 download() methods.
 * download(URL) takes the address to a .torrent file on the Web.
 * download(File) takes the path to a .torrent file saved on the disk.
 * download(byte[]) tatkes the contents of a .torrent file.
 * 
 * === Slots and lists ===
 * 
 * TorrentManager keeps 2 lists of ManagedTorrent objects.
 * They are named _active and _waiting.
 * We're actively sharing the torrents in the _active list online.
 * Those in the _waiting list are not being shared.
 * 
 * The private getMaxActiveTorrents() method determines how many slots we have.
 * It looks at what kind of Internet connection the user told the settings wizard.
 * If the user specified a fast internet connection, it will return 6.
 * 
 * This means that the program will share up to 6 torrents at a time.
 * The ManagedTorrent objects for these torrents will be in the _active list.
 * If the user adds a 7th torrent, it will go into the _waiting list.
 * 
 * The torrents in the _waiting list don't get cycled into the _active list.
 * But, if the user pauses or removes one of the active torrents, the program will move one from waiting to active.
 * 
 * === Our BitTorrent peer ID ===
 * 
 * TorrentManager contains the program's BitTorrent peer ID.
 * These are the 20 bytes that uniquely identify us among BitTorrent programs.
 * We'll tell them to a remote computer in the BitTorrent handshake.
 * They are like "LIMEguidguidguidguid", with our vendor code followed by our Gnutella client ID.
 * To get them, call getPeerId().
 * 
 * === Saving the list ===
 * 
 * The program saves 2 files in LimeWire's "Incomplete" directory, torrents.dat and torrents.bak.
 * If we have trouble reading torrents.dat, we'll try torrents.bak instead.
 * The files contain an ArrayList of BTMetaInfo objects that Java serialized.
 * A BTMetaInfo object represents the bencoded data inside a .torrent file.
 * writeSnapshot() puts the torrents from both the active and waiting lists in the file.
 */
public class TorrentManager implements ConnectionAcceptor {

	/** Not used by code in this class. */
	private static final Throttle UPLOAD_THROTTLE = new NBThrottle(true, DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.getValue());

	/**
	 * Our BitTorrent peer ID, the 20 bytes that uniquely identify us amongst other BitTorrent programs.
	 * 
	 * Composed like "LIMEguidguidguidguid".
	 * The first 4 bytes are "LIME", our vendor code.
	 * The 16 bytes after that are are Gnutella client ID GUID.
	 * 
	 * BitTorrent programs exchange peer IDs as the last 20 bytes of the handshake.
	 */
	private final byte[] PEER_ID;

	/** A debugging log we can save lines of text to as the program runs. */
	private static final Log LOG = LogFactory.getLog(TorrentManager.class);

	/** 1 minute in milliseconds, we'll save our list of torrents to downloads.dat every minute. */
	private int SNAPSHOT_CHECKPOINT_TIME = 60 * 1000;

	/**
	 * The torrents the program is sharing online.
	 * A LinkedList of ManagedTorrent objects.
	 */
	private List _active = new LinkedList();

	/**
	 * The torrents the program has, but isn't sharing online, because we're already sharing 6 other torrents.
	 * A LinkedList of ManagedTorrent objects.
	 */
	private List _waiting = new LinkedList();

	/** How fast we're uploading data for all of our torrents right now. */
	private float _currentUpload;

	/** How fast we're downloading data for all of our torrents right now. */
	private float _currentDownload;

	/** The total average upload speed for our torrents. */
	private float _averageUpload;

	/** The total average download speed for our torrents. */
	private float _averageDownload;

	/** The number of measurements we took to calculate our average upload and download speeds. */
	private int _numMeasures;

	/** A link up to the ActivityCallback, which lets us talk to the GUI. */
	private ActivityCallback _callback;

	/**
	 * Make the program's single TorrentManager object, which keeps our list of torrents.
	 * RouterService calls this, and saves the new object as torrentManager.
	 * 
	 * Composes our BitTorrent peer ID as the 20 bytes "LIME[client ID GUID]".
	 */
	public TorrentManager() {

		// Get our Gnutella client ID GUID, which uniquely identifies us on the Gnutella network
		String clientId = ApplicationSettings.CLIENT_ID.getValue(); // The program chose it the first time it ran on this computer, and saved it in settings
		byte[] guid; // Turn it into 16 bytes
		if (clientId.length() != 0 && clientId != null) guid = GUID.fromHexString(clientId);
		else guid = GUID.makeGuid();

		// Compose our BitTorrent peer ID, the 20 bytes that will uniquely identify us among BitTorrent programs
		PEER_ID = new byte[20];
		String qhdVendorName = CommonUtils.QHD_VENDOR_NAME; // Make the first 4 bytes "LIME"
		PEER_ID[0] = (byte) qhdVendorName.charAt(0);
		PEER_ID[1] = (byte) qhdVendorName.charAt(1);
		PEER_ID[2] = (byte) qhdVendorName.charAt(2);
		PEER_ID[3] = (byte) qhdVendorName.charAt(3);
		System.arraycopy(guid, 0, PEER_ID, 4, 16); // Copy in the 16 byts of our client ID GUID after that

		// Make a note the program's TorrentManager is ready to go
		if (LOG.isDebugEnabled()) LOG.debug("TorrentManager created");
	}

	/**
	 * Open torrents.dat, schedule repeating tasks, and register the BitTorrent handshake greeting with the ConnectionDispatcher.
	 * Only RouterService.start() calls this method.
	 * 
	 * First, initialize() opens torrents.dat, the file the program saved in LimeWire's "Incomplete" folder the last time it ran.
	 * If we can't read torrents.dat, we read torrents.bak instead.
	 * Inside the file is an ArrayList of BTMetaInfo objects.
	 * The readSnapshot() method makes a ManagedTorrent for each one, and adds them to our list.
	 * 
	 * initialize() schedules 2 Runnable anonymous inner classes with the RouterService.
	 * Every minute, code here will save our list of torrents to the torrents.dat file.
	 * Every 10 seconds, code here will see if we have an open slot for a torrent, and start sharing a new one in it.
	 * 
	 * This method reigsters the TorrentManager with the ConnectionDispatcher as a ConnectionAcceptor.
	 * When a remote computer contacts the program's listening socket and says "#BitTorrent", we'll get the connection.
	 * This is the start of the BitTorrent handshake, with the first byte, #, holding the number 19.
	 */
	public void initialize() {

		// Make a note we're here
		if (LOG.isDebugEnabled()) LOG.debug("initializing TorrentManager");

		// Get a link up to the ActivityCallback, that will let us talk to the GUI
		_callback = RouterService.getCallback();

		// Get the paths of 2 files in LimeWire's "Incomplete" folder, torrents.dat, and its backup, torrents.bak
		File real   = BittorrentSettings.TORRENT_SNAPSHOT_FILE.getValue();        // torrents.dat
		File backup = BittorrentSettings.TORRENT_SNAPSHOT_BACKUP_FILE.getValue(); // torrents.bak

		/*
		 * Try once with the real file, then with the backup file.
		 */

		// Read the BTMetaInfo objects in torrents.dat, make ManagedTorrent objects for each, and add them to our list
		if (!readSnapshot(real)) {

			// readShapshot() returns false if there was an error reading the file
 			if (LOG.isDebugEnabled()) LOG.debug("Reading real torrents.dat failed");

 			// Try torrents.bak instead
			if (readSnapshot(backup)) {

				// That worked, copy torrents.bak to torrents.dat
				if (LOG.isDebugEnabled()) LOG.debug("Reading backup torrents.bak succeeded.");
				copyBackupToReal();

			// We couldn't read torrents.dat or torrents.bak, but one or both of the files are there
			} else if (backup.exists() || real.exists()) {

				// Show the user an error
				if (LOG.isDebugEnabled()) LOG.debug("Reading both torrents files failed.");
				MessageService.showError("TORRENTS_COULD_NOT_READ_SNAPSHOT");
			}

		// We read torrents.dat
		} else {

			// Make a note that it worked
			if (LOG.isDebugEnabled()) LOG.debug("Reading torrents.dat worked!");
		}

		// Have the RouterService run the code here every minute
		Runnable checkpointer = new Runnable() {
			public void run() {

				// Only save something if we have some torrents right now
				if (_active.size() > 0) {

					// Write the torrents we're sharing to torrents.dat
					if (!writeSnapshot()) {

						// It didn't work, copy torrents.bak to torrents.dat to restore a copy that might work
						copyBackupToReal();
					}
				}
			}
		};
		RouterService.schedule(checkpointer, SNAPSHOT_CHECKPOINT_TIME, SNAPSHOT_CHECKPOINT_TIME); // 1 minute from now, and every minute after that

		// Have the RouterService run the code here every 10 seconds
		Runnable waitingPimp = new Runnable() {
			public void run() {

				// If we have a slot for another torrent, start sharing it
				wakeUp();
			}
		};
		RouterService.schedule(waitingPimp, 10 * 1000, 10 * 1000); // 10 seconds from now, and every 10 seconds after that

		/*
		 * Register the TorrentManager as a ConnectionAcceptor.
		 * When a remote computer connects to our listening socket and says "#BitTorrent", we'll get the connection.
		 * 
		 * The BitTorrent handshake begins "#BitTorrent protocol".
		 * The first byte, #, has the value 19.
		 * After that are the 19 ASCII text characters "BitTorrent protocol".
		 * 
		 * We'll just give the first part, "#BitTorrent", to the ConnectionDispatcher.
		 * It will read this much from the socket, realize the remote computer wants BitTorrent, and give the connection to us.
		 */

		// Compose the greeting "#BitTorrent" in a StringBuffer
		StringBuffer word = new StringBuffer();
		word.append((char)19);     // The first byte has the value 19
		word.append("BitTorrent"); // After that are the 19 ASCII characters "BitTorrent protocol"

		// Register this object with the ConnectionDispatcher as a ConnectionAcceptor
		RouterService.getConnectionDispatcher().addConnectionAcceptor(
			this,                            // A link to this object, the program's TorrentManager
			new String[]{ word.toString() }, // The greeting to look for, "#BitTorrent"
			false,                           // Not local only, let Internet computers contact us
			false);                          // Not blocking, we're using NIO
	}

	/**
	 * Copy torrents.bak to torrents.dat.
	 * This restores a backup of our torrents.dat file.
	 */
	private synchronized void copyBackupToReal() {

		// Make a note we're doing this
		if (LOG.isDebugEnabled()) LOG.debug("copying backup file to main saving file");

		// Get the paths to both files in Java File objects
		File real   = BittorrentSettings.TORRENT_SNAPSHOT_FILE.getValue();        // The path to the torrents.dat file
		File backup = BittorrentSettings.TORRENT_SNAPSHOT_BACKUP_FILE.getValue(); // The path to the torrents.bak file

		// Replace torrents.dat with torrents.bak
		real.delete();                  // Delete torrents.dat
		CommonUtils.copy(backup, real); // Rename torrents.bak to torrents.dat
	}

	/**
	 * Write the torrents we're sharinng to torrents.dat.
	 * Call this as the program runs to save what we're doing.
	 * 
	 * Renames torrents.dat to torrents.bak, creating a backup.
	 * Writes the contents of the .torrent files we're sharing to torrents.dat.
	 * 
	 * The files is an ArrayList of BTMetaInfo objects.
	 * 
	 * @return true if we successfully saved the file.
	 *         false if we weren't able to.
	 */
	public synchronized boolean writeSnapshot() {

		// Make a note we're writing the file that lists our torrents
		LOG.debug("writing snapshot");

		// Make a list for the .torrent files we're still downloading
		List buf = new ArrayList();

		// Loop for each torrent we're sharing on the network
		for (int i = 0; i < _active.size(); i++) {
			ManagedTorrent mt = (ManagedTorrent) _active.get(i);

			// If we haven't finished downloading it yet, add the information from its .torrent file to our list
			if (!mt.isComplete()) buf.add(mt.getMetaInfo());
		}

		// Loop for each torrent that's waiting to be shared
		for (int i = 0; i < _waiting.size(); i++) {
			ManagedTorrent mt = (ManagedTorrent) _waiting.get(i);

			// If we haven't finished downloading it yet, add the information from its .torrent file to our list
			if (!mt.isComplete()) buf.add(mt.getMetaInfo());
		}

		// Get the path to our torrents.dat file in LimeWire's "Incomplete" folder
		File outFile = BittorrentSettings.TORRENT_SNAPSHOT_FILE.getValue();

		// Delete torrents.bak, and rename torrents.dat to torrents.bak, creating the new backup
		BittorrentSettings.TORRENT_SNAPSHOT_BACKUP_FILE.getValue().delete();
		outFile.renameTo(BittorrentSettings.TORRENT_SNAPSHOT_BACKUP_FILE.getValue());

		// Write list of BTMetaInfo.
		try {

			// Open torrents.dat for writing, and make a new ObjectOutputStream that we can give objects to for it to serialize them to the file
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(BittorrentSettings.TORRENT_SNAPSHOT_FILE.getValue()));

			try {

				// Give it the ArrayList of BTMetaInfo objects
				out.writeObject(buf);
				out.flush(); // Have it write to the file now

				// If that worked with an exception, record success and return true
				if (LOG.isDebugEnabled()) LOG.debug("snapshot written");
				return true;

			// Run this code before leaving the try block
			} finally {

				// Close the file
				out.close();
			}

		// There was a problem writing torrents.dat
		} catch (IOException e) {

			// Restore the backup by renaming torrents.bak to torrents.dat
			if (!FileUtils.forceRename(BittorrentSettings.TORRENT_SNAPSHOT_BACKUP_FILE.getValue(), BittorrentSettings.TORRENT_SNAPSHOT_FILE.getValue())) {

				// That didn't work
				ErrorService.error(e, "could not save torrents.dat file, backup failed, please restart LimeWire.");
			}

			// Report that we didn't save anything
			if (LOG.isDebugEnabled()) LOG.debug("snapshot not written", e);
			return false;
		}
	}

	/**
	 * Read the file the program saved its list of torrents in the last time.
	 * 
	 * The file is an ArrayList of BTMetaInfo objects.
	 * Makes a ManagedTorrent object for each one, and adds it to our list, and the GUI's list of downloads.
	 * 
	 * Reads the torrents serialized in TORRENT_SNAPSHOT_FILE and adds them to
	 * this, queued. The queued torrents will restart immediately if slots are
	 * available. Returns false iff the file could not be read for any reason.
	 * THIS METHOD SHOULD BE CALLED BEFORE ANY GUI ACTION. It is public for
	 * testing purposes only!
	 * 
	 * @param file The torrents.dat or torrents.bat file we can open and read
	 * @return     true if it worked, false on error
	 */
	public synchronized boolean readSnapshot(File file) {

		// Make a note that we're going to read the file
		if (LOG.isDebugEnabled()) LOG.debug("reading Snapshot");

		// The ArrayList we'll read from inside the file
		List buf = null;

		try {

			/*
			 * This does not try to maintain backwards compatibility with older
			 * versions of LimeWire, which only wrote the list of torrents.
			 * This doesn't really cause an errors, however.
			 */

			// Open the file, read the contents, and turn it into a Java ArrayList object
			ObjectInputStream in = new ConverterObjectInputStream(new FileInputStream(file));
			buf = (List)in.readObject();

		// If there is an exception, catch it, make a note about it in the debugging log, and return false
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

		/*
		 * Initialize and start torrents. Must catch ClassCastException since
		 * the data could be corrupt.
		 */

		try {

			// Loop for each BTMetaInfo object in the file
			for (Iterator iter = buf.iterator(); iter.hasNext(); ) {
				BTMetaInfo info = (BTMetaInfo)iter.next();

				// Make a new ManagedTorrent from it
				ManagedTorrent torrent = new ManagedTorrent(info, this);

				// Add the ManagedTorrent to our list, and start sharing it
				addTorrent(torrent);

				// List the torrent as a download in LimeWire's GUI
				_callback.addDownload(torrent.getDownloader());
			}

			// Report success
			if (LOG.isDebugEnabled()) LOG.debug("snapshot read");
			return true;

		// The data was corrupt
		} catch (ClassCastException e) {

			// Report error
			return false;
		}
	}

	/**
	 * Add a given torrent to the list of them the TorrentManager keeps.
	 * If we're not sharing our maximum number of torrents right now, we'll start sharing this new one right away.
	 * If we are at our maximum, we'll add it to a list of torrents waiting to be shared when others are removed or paused.
	 * 
	 * @param mt The ManagedTorrent object that represents a torrent to start sharing
	 */
	public synchronized void addTorrent(ManagedTorrent mt) {

		// If we already have the given ManagedTorrent in our active or waiting lists, don't add it again
		if (_active.contains(mt) || _waiting.contains(mt)) return;

		// We're already sharing our maximum number of torrents
		if (_active.size() >= getMaxActiveTorrents()) {

			// Add the given torrent to the waiting list
			_waiting.add(mt);
			if (LOG.isDebugEnabled()) LOG.debug("torrent added to waiting");

		// We have room to share another torrent
		} else {

			// Start sharing it, and add it to the active list
			mt.start();
			if (LOG.isDebugEnabled()) LOG.debug("torrent added to active");
			_active.add(mt);
		}

		// Tell the GUI to add the torrent to the list of Gnutella downloads
		_callback.addDownload(mt.getDownloader());
	}

	/**
	 * Determine if a torrent we're sharing should go offline to let a waiting torrent that's not done yet finish.
	 * Looks for an incomplete torrent in our waiting list.
	 * 
	 * @return true if there's an incomplete torrent in our waiting list.
	 *         false if our waiting list is empty, or all the torrents there are done.
	 */
	public synchronized boolean shouldYield() {

		// Loop for each of our torrents we're not sharing online because we're already sharing 6 other ones
		for (Iterator iter = _waiting.iterator(); iter.hasNext(); ) {
			ManagedTorrent m2 = (ManagedTorrent)iter.next();

			// If this one isn't finished downloading yet, return true, we're out of slots to finish getting a torrent
			if (!m2.isComplete()) return true;
		}

		// No, all of our waiting torrents are done, or we don't have any waiting torrents
		return false;
	}

	/**
	 * Download a .torrent file from a given Web address, add it to our list, and start getting the torrent.
	 * This method blocks while we download the .torrent file, for up to 8 seconds.
	 * 
	 * @param  url         A Java URL object that has the address to the .torrent file on the Web
	 * @return             A BTDownloader object that lets the GUI list the download
	 * @throws IOException If there is a problem downloading the file
	 */
	public synchronized Downloader download(URL url) throws IOException {

		// Make a note in the debugging log
		if (LOG.isDebugEnabled()) LOG.debug("downloading torrent from " + url);

		// Download the .torrent file
		HttpMethod get = new GetMethod(url.toExternalForm());
		get.addRequestHeader("User-Agent", CommonUtils.getHttpServer());
		get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(), "close");
		get.setFollowRedirects(true);
		HttpClient http = HttpClientManager.getNewClient(Constants.TIMEOUT, Constants.TIMEOUT);
		http.executeMethod(get); // Control blocks here while the HttpMethod object downloads the file
		if (get.getStatusCode() < 200 || get.getStatusCode() >= 300) throw new IOException("bad status code, downloading .torrent file " + get.getStatusCode());

		// Add the torrent to our list, and return its BTDownloader object that will let the GUI list it
		return download(get.getResponseBody()); // The response body is the contents of the .torrent file
	}

	/**
	 * Add a new torrent to our list, and start downloading it.
	 * 
	 * @param torrentFile A Java File object with the path to a .torrent file saved on this computer
	 * @return            A BTDownloader object that lets the GUI list the download
	 */
	public synchronized Downloader download(File torrentFile) throws IOException {

		/*
		 * The single line of code does the following things:
		 * readFileFully() reads the .torrent file from the disk, and returns its contents as a byte array.
		 * download() takes that byte array, makes a ManagedTorrent, and returns its BTDownloader object.
		 */

		// Add the torrent to our list, and return its BTDownloader object that will let the GUI list it
		return download(FileUtils.readFileFully(torrentFile));
	}

	/**
	 * Add a new torrent to our list, and start downloading it.
	 * 
	 * Parses the contents of the .torrent file into a BTMetaInfo object.
	 * Uses that to make a new ManagedTorrent object that represents the torrent.
	 * Returns the ManagedTorrent's BTDownload object, which lets the GUI list the download.
	 * 
	 * @param torrentFile A byte array with the contents of a .torrent file we downloaded or opened from disk
	 * @return            A BTDownloader object that will let the GUI list this download
	 */
	public synchronized Downloader download(byte[] torrentFile) throws IOException {

		// Make a note in the debugging log
		if (LOG.isDebugEnabled()) LOG.debug("trying to open torrent");

		try {

			// Make a new BTMetaInfo object from the data of the .torrent file
			BTMetaInfo info = BTMetaInfo.readFromBytes(torrentFile);

			// Loop for each of our torrents
			List buf = new ArrayList(); // Make a new ArrayList
			buf.addAll(_active);        // Add our active torrents, and
			buf.addAll(_waiting);       // Add the ones we have paused
			for (Iterator iter = buf.iterator(); iter.hasNext(); ) {
				ManagedTorrent torrent = (ManagedTorrent)iter.next();

				// We alredy have the given torrent
				if (Arrays.equals(info.getInfoHash(), torrent.getInfoHash())) {

					// Get all of its trackers
					for (int i = 0; i < info.getTrackers().length; i++) torrent.getMetaInfo().addTracker(info.getTrackers()[i]);

					// Don't start a new download, just return the BTDownloader we already have
					return torrent.getDownloader();
				}
			}

			// Make a new ManagedTorrent object to represent the torrent
			ManagedTorrent mt = new ManagedTorrent(info, this);

			// Add it to our list, and start sharing it
			addTorrent(mt);

			// Return its BTDownloader that can communicate results to the GUI
			return mt.getDownloader();

		// BTMetaInfo.readFromBytes() found an error in the .torrent file
		} catch (IOException e) {

			// Make a note, and then throw the exception as though we didn't catch it
			if (LOG.isDebugEnabled()) LOG.debug("bad torrent file", e);
			throw e;
		}
	}

	/**
	 * Stop sharing a torrent, or remove it from the program entirely.
	 * 
	 * @param mt    The ManagedTorrent to remove
	 * @param clear true to completely remove the torrent from the program.
	 *              false to just stop sharing it online right now.
	 */
	public synchronized void removeTorrent(ManagedTorrent mt, boolean clear) {

		// If we don't have the given torrent, there is nothing for us to remove
		if (!_active.contains(mt) && !_waiting.contains(mt)) return;

		// Remove it from the _active or _waiting lists, whichever it is in right now
		_active.remove(mt); // This tries to remove it from both, even though it is only in one list and not the other
		_waiting.remove(mt);

		// If the caller doesn't want to completely remove the torrent from the program
		if (!clear) {

			// Add it to the waiting list
			_waiting.add(mt); // If this opened up a slot, it will move back onto the active list as soon as we call wakeUp() below
		}

		// Write the torrents we're sharing to torrents.dat
		writeSnapshot();

		// Share up to 6 torrents at once
		wakeUp();
	}

	/**
	 * Get our BitTorrent peer ID, the 20 bytes that uniquely identify us amongst other BitTorrent programs.
	 * BitTorrent programs exchange peer IDs as the last 20 bytes of the handshake.
	 * 
	 * Our peer ID is a 20-byte array like "LIMEguidguidguidguid".
	 * The first 4 bytes are "LIME", our vendor code.
	 * The 16 bytes after that are are Gnutella client ID GUID.
	 * 
	 * @return Our BitTorrent peer ID in a 20-byte array
	 */
	public byte[] getPeerId() {

		// Return the peer ID our constructor composed
		return PEER_ID;
	}

	/**
	 * Start sharing the given torrent if we're not already sharing 6 of them.
	 * Only ManagedTorrent.resume() calls this method.
	 * 
	 * @param torrent The ManagedTorrent the user wants to start sharing
	 */
	public synchronized void wakeUp(ManagedTorrent torrent) {

		// If we're sharing fewer torrents than settings limit, and the given one is in our waiting list
		if (_active.size() < getMaxActiveTorrents() && _waiting.contains(torrent)) {

			// Move it from the _waiting list to the _active list, and start sharing it online
			_waiting.remove(torrent);
			_active.add(torrent); // Open the files on the disk, contact the tracker, and connect to peers
			torrent.start();
		}
	}

	/**
	 * Share up to 6 torrents at once.
	 * Moves ManagedTorrent objects from our _waiting list to our _active list, and calls start() on them.
	 * 
	 * The RouterService calls this method every 10 seconds.
	 * removeTorrent() also calls this method.
	 */
	public synchronized void wakeUp() {

		/*
		 * we definitely need some kind of bandwidth throttle that can be
		 * applied to both HTTP and torrent uploads. The easiest way to
		 * achieve this would probably be to convert HTTP uploads to use NIO.
		 */

		// Does nothing, this line of code isn't connected to anything that matters yet
		UPLOAD_THROTTLE.limit((int)UploadManager.getUploadSpeed()); // By default, the user has not limited the upload speed, and getUploadSpeed() returns no limit

		// Get an iterator that will let us loop for each of our torrents waiting to be shared
		Iterator iter = _waiting.iterator();

		// Loop until _active is full or _waiting is empty
		while (
			_active.size() < getMaxActiveTorrents() &&       // If we're sharing fewer torrents online than settings allow right now, and 
			iter.hasNext()) {                                // We have a torrent in the _waiting list
			ManagedTorrent mt = (ManagedTorrent)iter.next(); // Get the torrent in the _waiting list

			// If this torrent hasn't given up, hasn't had a disk problem, and isn't paused by the user
			if (mt.getState() != Downloader.GAVE_UP && mt.getState() != Downloader.DISK_PROBLEM && mt.getState() != Downloader.PAUSED) {

				// Move it from the _waiting list to the _active list, and start sharing it online
				_active.add(mt);
				mt.start(); // Open the files on the disk, contact the tracker, and connect to peers
				iter.remove();
			}
		}
	}

	/**
	 * Stop sharing all our torrents.
	 * Disconnects from peers and tells the trackers "event=stop".
	 */
	public synchronized void shutdown() {

		// Loop through all the torrent we're sharing online right now
		for (Iterator iter = _active.iterator(); iter.hasNext(); ) {

			// Call stop() on each one, removing it from the network and the program
			((ManagedTorrent)iter.next()).stop();
		}
	}

	/**
	 * Find out how many torrents we're sharing online right now.
	 * 
	 * we will give torrents slight preference over http downloaders by reducing
	 * the number of allowed http downloads by the number of active torrents.
	 * 
	 * @return The number of ManagedTorrent objects in the TorrentManager's _active list
	 */
	public synchronized int getNumActiveTorrents() {

		// Return the number of ManagedTorrent objects in the TorrentManager's _active list
		return _active.size();
	}

	/**
	 * Not used.
	 * 
	 * Have the SimpleBandwidthTracker objects in each of our MangaedTorrent objects update the speeds they keep current.
	 * Call this measureBandwidth() method repetedly.
	 */
	public synchronized void measureBandwidth() {
		float currentTotalUp, currentTotalDown;
		currentTotalDown = currentTotalUp = 0.f;
		boolean shouldCountAvg = false;
		for (Iterator iter = _active.iterator(); iter.hasNext(); ) {
			shouldCountAvg = true;
			ManagedTorrent mt = (ManagedTorrent)iter.next();
			mt.getUploader().measureBandwidth();
			mt.getDownloader().measureBandwidth();
			currentTotalDown += mt.getDownloader().getMeasuredBandwidth();
			currentTotalUp += mt.getUploader().getMeasuredBandwidth();
		}
		if (shouldCountAvg) {
			_averageDownload = (_averageDownload * _numMeasures + currentTotalDown) / (_numMeasures + 1);
			_averageUpload = (_averageUpload * _numMeasures + currentTotalUp) / (_numMeasures + 1);
			_numMeasures++;
		}
		_currentDownload = currentTotalDown;
		_currentUpload = currentTotalUp;
	}

	/** Not used. */
	public float getCurrentDownload() {
		return _currentDownload;
	}

	/** Not used. */
	public float getAverageDownload() {
		return _averageDownload;
	}

	/** Not used. */
	public float getCurrentUpload() {
		return _currentUpload;
	}

	/** Not used. */
	public float getAverageUpload() {
		return _averageUpload;
	}

	/**
	 * Find out where in our queue of torrents a given one is.
	 * 
	 * @param  to A ManagedTorrent object.
	 * @return 0  if we're sharing the given torrent online right now.
	 *         1  or more if it's in the waiting list, depending on its position in the list.
	 */
	public synchronized int getPositionInQueue(ManagedTorrent to) {

		// If we're sharing the given torrent right now, return 0
		if (_active.contains(to)) return 0; // It's not waiting in line at all

		// Otherwise, return it's position in the waiting list
		return _waiting.indexOf(to) + 1; // Add 1 because the first ManagedTorrent in _waiting has index 0
	}

	/**
	 * Get the non-blocking throttle the TorrentManager made to keep us from uploading data too quickly.
	 * 
	 * @return Our NBThrottle object
	 */
	public Throttle getUploadThrottle() {

		// Get the NBThrottle this object made
		return UPLOAD_THROTTLE;
	}

	/**
	 * Find out how many torrents we should share at once.
	 * Returns a number like 1, 2, 4, or 6, depending on what kind of Internet connection the user told settings we have.
	 * 
	 * @return The number of torrents we should share
	 */
	private int getMaxActiveTorrents() {

		// Get the kind of connection the user told the setup wizard when we first ran
		int speed = ConnectionSettings.CONNECTION_SPEED.getValue();

		// Sort by the kind of connection
		if      (speed <= SpeedConstants.MODEM_SPEED_INT) return 1; // Modem, only share 1 torrent at a time
		else if (speed <= SpeedConstants.CABLE_SPEED_INT) return 2; // Cable Internet, share 2 torrents at once
		else if (speed <= SpeedConstants.T1_SPEED_INT)    return 4; // 1.5 Mbps or more, share 4 torrents
		else                                              return 6; // Unlimited, share 6 torrents
	}

	/**
	 * Look up a ManagedTorrent by the torrent's info hash.
	 * Determine if we're sharing a torrent online right now.
	 * 
	 * IncomingBTHandshaker.verifyIncoming() calls this method.
	 * A remote computer has connected to us.
	 * It's told us the info hash of the torrent it wants to share with us.
	 * We need to see if we have that torrent.
	 * If we don't, we'll close the connection.
	 * 
	 * @param infoHash The 20-byte SHA1 hash of the "info" bencoded dictionary of the .torrent file.
	 * @return         The ManagedTorrent object that represents that torrent we have and are sharing.
	 *                 null if we don't have that torrent, or we do but we're not sharing it online right now.
	 */
	public ManagedTorrent getTorrentForHash(byte[] infoHash) {

		synchronized (this) {

			// Loop through the torrents we're sharing online right now
			for (Iterator iter = _active.iterator(); iter.hasNext(); ) {
				ManagedTorrent current = (ManagedTorrent) iter.next();

				// If this one has the given info hash, return it
				if (Arrays.equals(infoHash, current.getInfoHash())) return current;
			}
		}

		// No match found
		return null;
	}

	/**
	 * Read the BitTorrent handshake from a remote computer that just connected to us, and respond with our own.
	 * 
	 * The "NIODispatch" thread calls this acceptConnection() method when a remote computer connects to us.
	 * The remote computer said "[19]BitTorrent ", so the ConnectionDispatcher knew to hand the new connection to the TorrentManager.
	 * 
	 * @param word The word the ConnectionDispatcher already read from the channel to determine what the remote computer wants
	 * @param sock The open connection socket to the remote computer we can communicate with the remote computer through
	 */
	public void acceptConnection(String word, Socket sock) {

		// Make a new IncomingBTHandshaker object to do the BitTorrent handshake with the remote computer
		IncomingBTHandshaker shaker = new IncomingBTHandshaker((NIOSocket)sock, this); // Just saves the given objects
		shaker.startHandshaking(); // Makes buffers to hold the incoming data, and registers shaker with NIO
	}
}
