
// Commented for the Learning branch

package com.limegroup.bittorrent;

import java.io.IOException;
import java.net.URL;
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

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.MessageService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.io.Throttle;
import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.bittorrent.messages.BTHave;
import com.limegroup.gnutella.util.CoWList;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.ProcessingQueue;

/**
 * A ManagedTorrent object represents a torrent we're downloading and sharing with BitTorrent.
 * 
 * === Objects related to ManagedTorrent ===
 * 
 * TorrentManager, _manager
 * The program's one TorrentManager object keeps a list of all the ManagedTorrent objects.
 * Each ManagedTorrent links up to it with _manager.
 * 
 * ManagedTorrent, this
 * This ManagedTorrent object represents a torrent the program is sharing online.
 * 
 * BTConnectionFetcher, _connectionFetcher
 * This ManagedTorrent's BTConnection Fetcher makes connections to 5 remote computers sharing the same torrent.
 * 
 * BTConnection, _connections
 * A BTConnection object represents a TCP socket connection to a remote computer running BitTorrent software.
 * The remote computer has this torrent, and we're sharing it data through this connection.
 * This connection started with the BitTorrent handshake, followed by BitTorrent packets.
 * 
 * BTDownloader and BTUploader, _downloader and _uploader
 * A BTDownloader lets LimeWire's GUI list this torrent alongside the Gnutella downloads.
 * 
 * BTMetaInfo, _info
 * This ManagedTorrent's BTMetaInfo represents the bencoded data from the .torrent file.
 * 
 * VerifyingFolder, _folder
 * This ManagedTorrent's VerifyingFolder saves files to disk and checks their hashes.
 * 
 * === Lists of addresses and connections ===
 * 
 * A ManagedTorrent keeps 2 lists:
 * _connections is a list of BTConnection objects.
 * These are open TCP socket connections to remote computers we're sharing this torrent through.
 * _peers is a list of TorrentLocation objects.
 * These are IP addresses and port numbers of remote computers we can try connecting to.
 * Our tracker on the Web gave us these addresses.
 * 
 * === Contacting our tracker ===
 * 
 * Every 5 minutes or longer, we'll contact our tracker to get an up-to-date list of IP addresses of peers sharing the same torrent as us.
 * ManagedTorrent contains the code that contacts the tracker.
 * 4 methods run forever in a loop, trading off control between several threads:
 * 
 * scheduleTrackerRequest(delay, url) - have the RouterService call the next method delay milliseconds from now.
 * announce(url) - make a new thread named "TrackerRequest", and have it call the next method.
 * announceBlocking(url, event) - contact the tracker, blocks waiting for the response, and give it to the next method.
 * handleTrackerResponse(response, url) - add the IP addresses our tracker told us to our peers list, and call scheduleTrackerRequest() with the delay the tracker gave us.
 * 
 * === Sending Choke and Unchoke messages ===
 * 
 * Every 30 seconds, a BitTorrent program sends each of its connections a Choke or Unchoke message.
 * scheduleRechoke(), rechoke(), PeriodicChoker, and Rechoker do this for LimeWire.
 * Rechoker.run() contains the code that decides which of our connectionsn we'll choke and which we'll unchoke.
 * It unchokes the connections that are sending us data the fastest.
 * It also selects 1 or 2 connections randomly and unchokes them.
 * The nested DownloadSpeedComparator class has the code that can sort a list of connections by how fast they are giving us data.
 * 
 * setGlobalChoke() isn't a part of BitTorrent.
 * It just suspends us from sending data to anyone while we move the files we saved from the "Incomplete" folder to the "Shared" folder.
 * 
 * === Sending Request and Have messages ===
 * 
 * request() picks a range to request from a given remote computer, and sends it a Request message.
 * notifyOfComplete() sends a Have message to all our connections, telling them we have a numbered piece.
 */
public class ManagedTorrent {

	/** A debugging log we can write lines of text to as the program runs. */
	private static final Log LOG = LogFactory.getLog(ManagedTorrent.class);

	/** Not used. */
	static final byte[] ZERO_BYTES = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

	/** 5, if we try and fail to contact our tracker 5 times, we'll give up. */
	private static final int MAX_TRACKER_FAILURES = 5;

	/**
	 * A DownloadSpeedComparator that can tell which of 2 computers is sending us data the fastest.
	 * Rechoker.run() uses DOWNLOAD_SPEED_COMPARATOR to sort our list of interested remote computers into order.
	 */
	private static final Comparator DOWNLOAD_SPEED_COMPARATOR = new DownloadSpeedComparator();

	/**
	 * 30 seconds in milliseconds.
	 * Every 30 seconds we'll send each of our connections a Choke or Unchoke message.
	 * scheduleRechoke() and the PeriodicChoker use this.
	 */
	private static final int RECHOKE_TIMEOUT = 30 * 1000;

	/** A link back up to the program's TorrentManager object, which has this ManagedTorrent in its list. */
	private final TorrentManager _manager;

	/**
	 * True if we've stopped this torrent.
	 * This means we're not sending or receiving file data for it any longer.
	 * 
	 * When the program calls stopNow() to stop transferring data for this torrent, it sets _stopped to true.
	 */
	private volatile boolean _stopped = true;

	/**
	 * Our list of IP addresses of remote BitTorrent computers that are sharing this torrent.
	 * We're not connected to these computers now, we just know their addresses.
	 * 
	 * Before our BTConnectinFetcher starts trying to connect to an address, getTorrentLocation() removes it from the _peers list.
	 * Additinally, isConnectedTo() makes sure we don't already have a connection to that address.
	 * _peers contains addresses we can try, and aren't connected to right now.
	 * 
	 * A synchronized HashSet of TorrentLocation objects.
	 * Synchronize on this ManagedTorrent to access _peers.
	 */
	private Set _peers;

	/**
	 * Addresses of peers that have this torrent, but we tried and failed to connect to in the last hour.
	 * 
	 * OutgoingBTHandshaker.handleIOException() calls addBadEndpoint() with the address in a TorrentLocation object.
	 * This TorrentLocation isn't in the _peers list anymore, because getTorrentLocation() removed it before we tried to connect.
	 * 
	 * A synchronized FixedSizeExpiringSet of TorrentLocation objects.
	 * TorrentLocation objects only last in _badPeers for 1 hour, and only keeps the most recent 500.
	 */
	private Set _badPeers;

	/** The BTMetaInfo object we made for this .torrent file, which has information like the list of file paths. */
	private BTMetaInfo _info;

	/** The VerifyingFolder object that represents the folder we're saving files to, and can check piece hashes. */
	private volatile VerifyingFolder _folder;

	/**
	 * Counts the number of times our tracker on the Web has failed.
	 * We tried to contact it, and couldn't.
	 * Or we did, and it said "failure reason".
	 */
	private int _trackerFailures = 0;

	/** True if the VerifyingFolder object tried to setup the folder where we'll save the files of this torrent, and was unable to. */
	private boolean _couldNotSave = false;

	/** This ManagedTorrent has a BTConnectionFetcher object that tries to open new TCP socket connections to remote computers. */
	private volatile BTConnectionFetcher _connectionFetcher;

	/**
	 * True when we're not uploading any file data to anyone.
	 * setGlobalChoke() does this when we're moving files from the "Incomplete" to "Saved" folder, so they're not open for reading.
	 */
	private volatile boolean _globalChoke = false;

	/**
	 * True if this torrent is paused, and not transferring any data because of that.
	 * Use the pause() and resume() methods to set this condition.
	 */
	private boolean _paused = false;

	/**
	 * True when we've finished the download, and moved the files from the "Incomplete" folder to the "Shared" folder.
	 * saveFiles() sets this to true, and uses it to avoid running a second time.
	 * 
	 * The user can restart a completed download.
	 * _saved lets this object remember that we've saved and moved the files already.
	 */
	private boolean _saved = false;

	/** A list of BTConnection objects that represent the TCP socket connections we have open to remote computers sharing this torrent with us. */
	private List _connections;

	/**
	 * A ProcessingQueue that can have another thread run code.
	 * enqueueTask() adds objects with run() methods to _processingQueue, which keeps them in a list.
	 * One by one, the "ManagedTorrent" thread calls run() on them and discards them.
	 */
	private ProcessingQueue _processingQueue;

	/** A BTDownloader object that lets LimeWire's GUI list this torrent with the Gnutella downloads. */
	private BTDownloader _downloader;

	/** A BTUploader object that lets LimeWire's GUI list this torrent with the Gnutella uploads. */
	private BTUploader _uploader;

	/**
	 * A PeriodicChoker makes a Rechoker object which sends a Choke or Unchoke message to each of our connections, and then does that again 30 seconds later.
	 * The scheduleRechoke() method makes this ManagedTorrent's PeriodicChoker object, and saves it as choker.
	 */
	private PeriodicChoker choker;

	/**
	 * Make a new ManagedTorrent object to represent a torrent we're downloading and sharing with other BitTorrent programs on the Internet.
	 * 
	 * @param info    The BTMetaInfo object we made from the bencoded data inside the .torrent file
	 * @param manager A reference up to the program's one TorrentManager object, which keeps a list of ManagedTorrent objects like this one
	 */
	public ManagedTorrent(BTMetaInfo info, TorrentManager manager) {

		// Link the objects used for a torrent together
		_info = info;                        // Link us to the BTMetaInfo object that has the information from the .torrent file
		_info.setManagedTorrent(this);       // Link the BTMetaInfo object back up to us
		_manager = manager;                  // Link us up to the program's one TorrentManager object
		_folder = info.getVerifyingFolder(); // Link to the VerifyingFolder object the BTMetaInfo object makes to represent our save folder

		// Make a new empty ArrayList for the _connections, we'll list our BTConnection objects here
		_connections = new CoWList(CoWList.ARRAY_LIST);

		// Setup our ProcessingQueue, name the thread it will start "ManagedTorrent"
		_processingQueue = new ProcessingQueue("ManagedTorrent");

		// Make BTDownloader and BTUploader objects that will let LimeWire's GUI list this torrent in the download and upload lists
		_downloader = new BTDownloader(this, _info);
		_uploader = new BTUploader(this, _info);

		// Point our lists of remote computer addresses at empty lists so they're not null
		_peers = Collections.EMPTY_SET;
		_badPeers = Collections.EMPTY_SET;
	}

	/**
	 * Get the info hash, the hash BitTorrent uses to identify this torrent and its file.
	 * 
	 * To compute the info hash, take the SHA1 hash of the value of "info" in the bencoded data of the .torrent file.
	 * The value of "info" is a bencoded dictionary.
	 * 
	 * @return The 20-byte SHA1 info hash of this .torrent file
	 */
	public byte[] getInfoHash() {

		// Get it from the BTMetaInfo object, which hashes the "info" dictionary of bencoded data to make it
		return _info.getInfoHash();
	}

	/**
	 * Get the BTMetaInfo object that represents the .torrent file we opened.
	 * It holds the information from the bencoded data of the .torrent file, like the list of file names and sizes.
	 * 
	 * @return Our BTMetaInfo object
	 */
	public BTMetaInfo getMetaInfo() {

		// Return the reference the constructor saved
		return _info;
	}

	/**
	 * Determine if we've finished downloading this torrent and checking its data.
	 * 
	 * @return true if we're done, false if we're still getting data
	 */
	public boolean isComplete() {

		// Ask the VerifyingFolder this
		return _folder.isComplete();
	}

	/**
	 * Start sharing this torrent.
	 * 
	 * Sets up this object.
	 * Has the VerifyingFolder open the folder it represents.
	 * Connects to peers, and contacts our tracker.
	 */
	public void start() {

		// Make a note in our debugging log
		if (LOG.isDebugEnabled()) LOG.debug("requesting torrent start", new Exception());

		// The "ManagedTorrent" thread will call this run() method
		enqueueTask(new Runnable() {
			public void run() {

				// If we're stopped, leave without doing anything
				if (!_stopped) return;
				_stopped = false;

				// Set up the contents of this object
				initializeTorrent();

				// Have the VerifyingFolder open the folder it represents
				initializeFolder();
				if (_stopped || _folder.isVerifying()) return; // If the VerifyingFolder is still checking its contents, don't connect yet

				// Connect to peers, and contact our tracker
				startConnecting();
			}
		});
	}

	/**
	 * Connect to peers sharing our torrent, connect to our tracker, and send Choke and Unchoke messages in 30 seconds.
	 * 
	 * startConnecting does 3 things:
	 * It tries to open new TCP socket connections to remote computers sharing our torrent, and does the handshake with them.
	 * It contacts our tracker on the Web with "event=start".
	 * It sends each of our connections a Choke or Unchoke message 30 seconds from now, and each 30 seconds afterwards.
	 */
	private void startConnecting() {

		// If we already know of some peer computer addresses, have our BTConnectionFetcher try to open connections
		if (_peers.size() > 0) _connectionFetcher.fetch();

		// Connect to the tracker
		announceStart(); // The tracker's address is at the start of the .torrent file

		// Send each of our connections a Choke or Unchoke message 30 seconds from now, and every 30 seconds afterwards.
		scheduleRechoke();
	}

	/**
	 * Stop Internet communications related to this torrent, and remove this object from the program.
	 * 
	 * Has the "ManagedTorrent" thead perform the following tasks:
	 * Remove us from the GUI's list of uploads.
	 * Close all the open files we have.
	 * Contact all our trackers with "event=stop".
	 * Close all our TCP socket connections to peers sharing the same torrent.
	 * Remove us from the TorrentManager's list.
	 */
	public void stop() {

		// Have the "ManagedTorrent" thread call stopNow()
		if (LOG.isDebugEnabled()) LOG.debug("requested torrent stop", new Exception());
		enqueueTask(new Runnable() {
			public void run() {
				stopNow();
			}
		});
	}

	/**
	 * Stop Internet communications related to this torrent, and remove this object from the program.
	 * 
	 * Removes us from the GUI's list of uploads.
	 * Closes all the open files we have.
	 * Contacts all our trackers with "event=stop".
	 * Closes all our TCP socket connections to peers sharing the same torrent.
	 * Removes us from the TorrentManager's list.
	 * 
	 * Only let the "ManagedTorrent" thread call stopNow().
	 */
	private void stopNow() {

		// Only do this once
		if (_stopped) return;
		_stopped = true;

		// Remove this torrent from the GUI's list of uploads
		RouterService.getCallback().removeUpload(_uploader);

		// Have the VerifyingFolder close all the files it has open
		_folder.close();

		// Tell all our trackers that we're leaving by contacting their Web addresses with "event=stop"
		for (int i = 0; i < _info.getTrackers().length; i++) announceBlocking(_info.getTrackers()[i], TrackerRequester.EVENT_STOP);

		// Close all our TCP socket connections to remote computers we've been sharing this torrent with
		for (Iterator iter = getConnections().iterator(); iter.hasNext(); ) ((BTConnection)iter.next()).close();

		// Stop our BTConnectionFetcher from connecting to new remote computers sharing the torrent
		_connectionFetcher.shutdown();

		// Remove this ManagedTorrent object from the list of them the program's single TorrentManager object keeps
		_manager.removeTorrent(this, _folder.isComplete());

		// Make a note we're stopped
		if (LOG.isDebugEnabled()) LOG.debug("Torrent stopped!");
	}

	/**
	 * Stop Internet communications related to this torrent, and remove this object from the program.
	 * 
	 * Call pause() to force this torrent to make room for others, if the program has any.
	 */
	public void pause() {

		// Have the "ManagedTorrent" thread call this run() method we're defining right here
		enqueueTask(new Runnable() {
			public void run() {

				// Mark this ManagedTorrent as paused
				_paused = true;

				// Disconnect from remote computers, tell our tracker goodbye, and remove this ManagedTorrent from the program
				stopNow();
			}
		});
	}

	/**
	 * Unpause this torrent, starting new Internet communications to share it.
	 * 
	 * @return false if it's already going, so we can't unpause.
	 *         false if we had an error saving it, so we can't unpause.
	 *         true on success.
	 */
	public boolean resume() {

		// If we're not stopped, return false, there is nothing else to do
		if (!_stopped) return false;

		// If our VerifyingFolder didn't have an error
		if (!_couldNotSave) {

			// Remove our record of our paused status
			_paused = false;

			// Resume this ManagedTorrent with the program's single TorrentManager
			_manager.wakeUp(this);

			// Report success
			return true;
		}

		// Our VerifyingFolder had an error, we can't resume, return false
		return false;
	}

	/**
	 * Remove a given BTConnection object from our _connections list.
	 * 
	 * Only BTConnection.close() calls this method, giving it the connection that we're closing.
	 * We are closing the connection because we don't want it anymore, or because we tried to connect it and it didn't work.
	 * 
	 * @param btc The BTConnection object that represents the connection we're closing
	 */
	void connectionClosed(BTConnection btc) {

		// If we had this connection open for a while before closing it
		if (btc.isWorthRetrying()) {

			// Get the IP address and port number from the BTConnection, and make a new TorrentLocation from it
			TorrentLocation ep = new TorrentLocation(btc.getEndpoint());

			// Have it record that we've tried and failed to connect
			ep.strike();

			// Add it back into our _peers list of addresses to try
			_peers.add(ep);
		}

		// Remove a given BTConnection object from our _connections list
		removeConnection(btc); // If that remote computer was interested in our data, and we weren't choking it, send Choke and Unchoke messages right now

		// If this torrent isn't stopped, have our BTConnectionFetcher try to get more connections to have 5
		if (!_stopped) _connectionFetcher.fetch();
	}

	/**
	 * Set up the contents of this object.
	 * Only start() calls this method.
	 * 
	 * Sets flags for save error, choke everyone, and paused to false.
	 * Makes our lists to hold the addresses of peers and bad peers, those we couldn't connect to.
	 * Registers our BTUploader object with LimeWire's GUI.
	 * Makes a BTConnectionFetcher that will try to connect to remote computers with the same torrent.
	 */
	private void initializeTorrent() {

		// Set flags for the start of sharing this torrent
		_couldNotSave = false; // No, we haven't run into a problem setting up the folder where we'll save the files of this torrent yet
		_globalChoke = false; // No, we're not moving the files right now making us unable to give any pieces to anyone
		_paused = false; // No, we're not paused transferring data for this torrent

		// Setup our lists for peers we failed connecting to, and more we could try connecting to
		_badPeers = Collections.synchronizedSet(new FixedSizeExpiringSet(500, 60 * 60 * 1000)); // Holds 500, and only keeps them for an hour
		_peers = Collections.synchronizedSet(new HashSet());

		// Does nothing
		if (_info.getLocations() != null) _peers.addAll(_info.getLocations());

		// Add our BTUploader object to the list of them the GUI keeps
		RouterService.getCallback().addUpload(_uploader);

		// Make a new BTConnectionFetcher that will try to connect to remote computers sharing this torrent
		_connectionFetcher = new BTConnectionFetcher(this, _manager.getPeerId());

		// Make a note in the debugging log
		if (LOG.isDebugEnabled()) LOG.debug("Starting torrent");
	}

	/**
	 * Prepares the folder where we'll save this torrent.
	 * Calls VerifyingFolder.open(this).
	 * If that throws an IOException, sets _couldNotSave and _stopped to true.
	 */
	private void initializeFolder() {

		try {

			// Open the VerifyingFolder, which will save data to disk and hash and check it
			_folder.open(this);

		// There was an error preparing the save folder
		} catch (IOException ioe) {

			// Set flags to true and leave now
			if (LOG.isDebugEnabled()) LOG.debug("unrecoverable error", ioe);
			_couldNotSave = true;
			_stopped = true;
			return;
		}

		// If our VerifyingFolder object says we've already downloaded this torrent, move the files we downloaded from the "Incomplete" folder to the "Shared" folder
		if (_folder.isComplete()) saveCompleteFiles();
	}

	/**
	 * When our VerifyingFolder is done hashing the files on disk we saved before, call startConnecting().
	 * This connects to peers sharing our torrent, connects to our tracker, and sends Choke and Unchoke messages in 30 seconds.
	 * 
	 * Only VerifyingFolder.open() calls this method.
	 * It calls it as a notification that its verification of our saved data is complete.
	 */
	void verificationComplete() {

		// Have the "ManagedTorrent" thread call this run() method
		enqueueTask(new Runnable() {
			public void run() {

				// If no one has stopped this torrent, and we need more data to complete it, connect to everything to stare this torrent
				if (!_stopped && !_folder.isComplete()) startConnecting();
			}
		});
	}

	/**
	 * Hit our tracker on the Web with "event=start".
	 * The first time we talk to a tracker, we should do it this way.
	 */
	private void announceStart() {

		// Loop for each of our trackers, we can have more than one
		for (int i = 0; i < _info.getTrackers().length; i++) {

			// Contact our tracker with "event=start", and add the IP addresses of its response to our _peers list
			announceBlocking(_info.getTrackers()[i], TrackerRequester.EVENT_START);
		}
	}

	/**
	 * Pick a range to request from a remote computer, and send it a Request message to ask for it.
	 * 
	 * Calls btc.getAvailableRanges() to get a list of the parts of the file the remote computer has.
	 * Calls _folder.leaseRandom() to pick one for us to ask for.
	 * Calls btc.sendRequest() to send the remote computer a BitTorrent Request message, asking for a range in a piece.
	 * 
	 * Calls btc.sendNotInterested() to send the remote computer a Not Interested message, telling it we don't want anything it has.
	 * 
	 * @param btc The BTConnection object that represents the remote computer this method will request data from
	 */
	public void request(final BTConnection btc) {

		// Make a note that we're going to ask the given remote computer for data
		if (LOG.isDebugEnabled()) LOG.debug("requesting ranges from " + btc.toString());

		// If this torrent is done or stopped, don't ask a remote computer for anything
		if (_folder.isComplete() || _stopped) return;

		// Choose a random part to ask for from amongst the ranges of the file btc has
		BTInterval in = _folder.leaseRandom(btc.getAvailableRanges());

		// We found a range to ask for
		if (in != null) {

			// Send the given remote computer a Request message, asking it for the range it has and we randomly selected
			btc.sendRequest(in);

		// We didn't find a range to ask for because btc doesn't have any
		} else if (btc.getAvailableRanges().isEmpty()) {

			// Send the given remote computer a Not Interested message, telling it we don't need any of its data
			if (LOG.isDebugEnabled()) LOG.debug("leaseRarest returned null, btc connection not interesting anymore");
			btc.sendNotInterested();

		// We didn't find a range to ask for because of some other reason
		} else {

			// Make a note, this shouldn't happen
			if (LOG.isDebugEnabled()) LOG.debug("leaseRarest returned null, btc connection still interesting ??!?!?");
		}
	}

	/**
	 * Send a Have message with the given piece number to all our connections sharing this torrent.
	 * If the VerifyingFolder has downloaded the complete torrent, move it into our "Shared" folder.
	 * 
	 * Only BTMetaInfo.notifyOfComplete() calls this method.
	 * Here's how the call comes here:
	 * VerifyingFolder.handleVerified(int) gets a piece number we've received and hashed to find is correct.
	 * VerifyingFolder.notifyOfChunkCompletion(int) just calls the next method.
	 * BTMetaInfo.notifyOfComplete(int) just calls this method.
	 * 
	 * @param The piece number we've received and verified
	 */
	void notifyOfComplete(int in) {

		// Make a note we just received a good piece
		if (LOG.isDebugEnabled()) LOG.debug("got completed chunk " + in);

		// If our VerifyingFolder is using files on the disk right now, don't do anything
		if (_folder.isVerifying()) return;

		// Make a Have message that can tell a remote computer we have this piece number
		final BTHave have = BTHave.createMessage(in); // Give it the piece number

		// Have the "NIODispatch" thread run this run() method
		Runnable haveNotifier = new Runnable() {
			public void run() {

				// Loop through all our connections sharing this torrent
				for (Iterator iter = getConnections().iterator(); iter.hasNext(); ) {
					BTConnection btc = (BTConnection) iter.next();

					// Tell each remote computer we have this piece
					btc.sendHave(have);
				}
			}
		};
		NIODispatcher.instance().invokeLater(haveNotifier);

		// If we have the whole torrent now
		if (_folder.isComplete()) {

			// Make a note we're done
			LOG.info("file is complete");

			// Have the "ManagedTorrent" thread call this run() method
			enqueueTask(new Runnable() {
				public void run() {

					// Move the files we downloaded from the "Incomplete" folder to the "Shared" folder
					saveCompleteFiles();
				}
			});
		}
	}

	/**
	 * Find out how many remote computers we are connected to and sharing this torrent with.
	 * Counts the connections that we initiated, not remote computers that connected to us.
	 * 
	 * @return Our number of open, outgoing connections for this torrent
	 */
	public int getNumAltLocs() {

		// Start the count at 0
		int ret = 0;

		// Loop through all our connections to remote computers sharing this torrent
		for (Iterator iter = getConnections().iterator(); iter.hasNext(); ) {
			BTConnection btc = (BTConnection) iter.next();

			// If we initiated this connection to the remote computer, count it
			if (btc.isOutgoing()) ret++;
		}

		// Return the count we made
		return ret;
	}

	/**
	 * Determine what Downloader state this torrent is in right now.
	 * 
	 * The Downloader class defines a list of different states that a download can be in.
	 * They are like 4 Download.COMPLETE or 1 Download.CONNECTING.
	 * getState() figures out which of those states this torrent is in right now, and returns the appropriate one.
	 * 
	 * @return The number code of a download state, like Download.COMPLETE or Download.CONNECTING
	 */
	public int getState() {

		// This torrent is stopped, it can't perform network activity
		if (_stopped) {

			// Return a specific reason, or just QUEUED
			if      (_couldNotSave)                           return Downloader.DISK_PROBLEM; // The VerifyingFolder had an error from the disk
			else if (_folder.isComplete())                    return Downloader.COMPLETE;     // We're stopped because it's done
			else if (_trackerFailures > MAX_TRACKER_FAILURES) return Downloader.GAVE_UP;      // The tracker broke more than 5 times
			else if (_paused)                                 return Downloader.PAUSED;       // We're stopped because it's paused
			else                                              return Downloader.QUEUED;       // Otherwise, say we're queued
		}

		/*
		 * We're not stopped, we're still sharing this torrent.
		 */

		// If we have the whole thing, we're seeding
		if (_folder.isComplete()) return Downloader.SEEDING;

		/*
		 * We're still downloading this torrent
		 */

		// The VerifyingFolder is hashing data
		if (_folder.isVerifying()) {

			// We're hashing
			return Downloader.HASHING;

		// We have some open connections to remote computers we're sharing this torrent with
		} else if (_connections.size() > 0) {

			// If we have a connection to a remote computer not choking us, we're downloading data from it
			if (isDownloading()) return Downloader.DOWNLOADING;
			return Downloader.REMOTE_QUEUED; // Otherwise all our connections are choking us, we're in our remote computers' queues

		// We don't have any open connections, but we do have addresses we can try to connect to
		} else if (_peers != null && _peers.size() > 0) {

			// We're connecting
			return Downloader.CONNECTING;

		// We don't have any open connections, and we don't know of any addresses to connect to
		} else if (_peers == null || _peers.size() == 0) {

			// We're still waiting for our tracker to give us some addresses
			return Downloader.WAITING_FOR_TRACKER;
		}

		// Otherwise, say we're busy
		return Downloader.BUSY;
	}

	/**
	 * Determine if we have a connection to a remote computer not choking us.
	 * This means it's giving us data now.
	 * 
	 * Only getState() above calls this method.
	 * If it returns true, we're downloading right now.
	 * 
	 * @return true if we have a connection choking us, false otherwise
	 */
	private boolean isDownloading() {

		// Loop through our connections, returning true if we have one not choking us
		for (Iterator iter = getConnections().iterator(); iter.hasNext(); ) {
			if (!((BTConnection)iter.next()).isChoking()) return true;
		}

		// All of our connections are choking us, or we don't have any connections
		return false;
	}

	/**
	 * Add the given TorrentLocation object to our _peers list.
	 * 
	 * The TorrentLocation holds the IP address and port number of a remote computer sharing the same torrent as us.
	 * _peers is our list of addresses we can try to connect to in order to share our torrent.
	 * 
	 * @param to A TorrentLocation that has the IP address and port number of a remote computer sharing the same torent as us.
	 * @return   true if we didn't have it and added it.
	 *           false if we already have it, we're already connected to it, it's on our bad IP list, or it's us, so we can't connect to it.
	 */
	public boolean addEndpoint(TorrentLocation to) {

		// If we already have the given TorrentLocation in our _peers list, or we're already connected to it, return false, we already have it
		if (_peers.contains(to) || isConnectedTo(to)) return false;

		// If the given IP address is on our list of government, institutional, and spamer IP addresses to avoid, return false, we can't connect to it
		if (!IPFilter.instance().allow(to.getAddress())) return false;

		// If the given IP address is our external Internet IP address, return false, we can't connect to it
		if (NetworkUtils.isMe(to.getAddress(), to.getPort())) return false;

		// Add the given TorrentLocation to _peers if it's not already there
		if (_peers.add(to)) { // Returns true if it wasn't there yet, and add() added it

			// Have the BTConnectionFetcher get connections for this torrent
			_connectionFetcher.fetch();

			// Report true, we added it
			return true;
		}

		// Report false, we already had it
		return false;
	}

	/**
	 * Add a given IP address and port number to the list of addresses this ManagedTorrent wasn't able to connect to.
	 * 
	 * OutgingBTHandshaker.handleIOException() calls this method.
	 * It's tried to open a new TCP socket connection to a remote computer.
	 * NIO couldn't do this, and threw it an IOException.
	 * 
	 * Adds the given TorrentLocation to this ManagedTorrent's _badPeers list.
	 * 
	 * @param to A TorrentLocation object that has the IP address and port number we couldn't connect to
	 */
	public void addBadEndpoint(TorrentLocation to) {

		// Add the given TorrentLocation to the _badPeers list
		_badPeers.add(to); // 1 hour later, the _badPeers list will throw it out
	}

	/**
	 * Add the IP addresses our tracker told us to our peers list, and schedule the next time we'll contact it with a call to scheduleTrackerRequest().
	 * This is method 4 in the tracker contact loop, and calls the first method to complete the loop.
	 * 
	 * Parses a response from our tracker.
	 * Looks for "failure reason", and increments _trackerFailures if it's found.
	 * Adds all the IP addresses the tracker told us to our _peers list.
	 * Finds out when the tracker wants to hear from us again, and schedules the RouterService to call announce() then.
	 * 
	 * @param response A TrackerResponse object that represents the bencoded data of the tracker's response
	 * @param url      The Web address of the tracker
	 */
	private void handleTrackerResponse(TrackerResponse response, URL url) {

		// Make a note we're doing this
		LOG.debug("handling tracker response " + url.toString());

		// Make minWaitTime 5 minutes in milliseconds, we use this if we can't read the tracker's response
		long minWaitTime = BittorrentSettings.TRACKER_MIN_REASK_INTERVAL.getValue() * 1000;

		try {

			// We couldn't contact the tracker
			if (response == null) {
				LOG.debug("null response");
				throw new IOException();
			}

			// Loop through the list of peers the tracker gave us, these are the IP addresses and port numbers of remote computers sharing the same torrent as us
			for (Iterator iter = response.PEERS.iterator(); iter.hasNext(); ) {
				TorrentLocation next = (TorrentLocation)iter.next();

				// Add the IP address and port number to our _peers list of remote computers we'll try to connect to
				addEndpoint(next);
			}

			// Set minWaitTime from the tracker's response
			minWaitTime = response.INTERVAL * 1000; // Convert from seconds to milliseconds

			// If the tracker said "failure reason", show an error to the user and throw an IOException
			if (response.FAILURE_REASON != null && // The tracker sent a bencoded key "failure reason", indicating it couldn't help us, and
				_trackerFailures == 0) {           // This is the first time our tracker has done this

				// Show an error to the user and throw an IOException
				MessageService.showError("TORRENTS_TRACKER_FAILURE", _info.getName() + "\n" + response.FAILURE_REASON);
				throw new IOException("Tracker request failed.");
			}

			// Record that the tracker hasn't failed
			_trackerFailures = 0;

		// Nothing seems to throw this
		} catch (ValueException ve) {

			// Never runs
			if (LOG.isDebugEnabled()) LOG.debug(ve);
			_trackerFailures++;

		// We couldn't contact the tracker, or we did and it said "failure reason"
		} catch (IOException ioe) {

			// Count this tracker failure
			if (LOG.isDebugEnabled()) LOG.debug(ioe);
			_trackerFailures++;
		}

		// If this ManagedTorrent isn't stopped and our tracker hasn't failed us 5 times yet
		if (!_stopped && _trackerFailures < MAX_TRACKER_FAILURES) {

			// Schedule the next time we'll contact our tracker, minWaitTime milliseconds from now
			scheduleTrackerRequest(minWaitTime, url);
		}
	}

	/**
	 * Determine if we need more connections.
	 * Returns true if we have less than 80.
	 * 80 is a lot for one torrent, so needsMoreConnections() will almost always return true.
	 * 
	 * Only BTConnectionFetcher.fetch() calls this method.
	 * 
	 * @return true if we need more connections.
	 *         false if we have many and don't need more.
	 */
	boolean needsMoreConnections() {

		// If we're externally contactable for remote computers to connect to our TCP listening socket
		if (RouterService.acceptedIncomingConnection()) {

			// If we have fewer than 80 connections, return true, we need more
			return _connections.size() < BittorrentSettings.TORRENT_MAX_CONNECTIONS.getValue() * 4 / 5;

		// Remote computers can't connect to us
		} else {

			// If we have fewer than 100 connections, return true, we need more
			return _connections.size() < BittorrentSettings.TORRENT_MAX_CONNECTIONS.getValue();
		}
	}

	/**
	 * Not used.
	 * 
	 * @param ep the TorrentLocation for the connection to add
	 * @return true if we want this connection, false if not
	 */
	boolean allowIncomingConnection(TorrentLocation ep) {
		// happens if we stopped this torrent but we still receive an incoming
		// connection because it took some time to read the headers & stuff
		if (_stopped) return false;
		// this could still happen, although we don't usually accept any
		// locations we are already connected to.
		if (isConnectedTo(ep)) return false;
		// don't allow connections to self
		if (NetworkUtils.isMe(ep.getAddress(), ep.getPort())) return false;
		// we do a little bit of preferencing here, - we support some features
		// others don't - and really, LimeWire users should help each other.
		// we won't do any nasty stuff like preferring LimeWire's when
		// uploading b/c that would just be mean
		if (ep.isLimePeer()) {
			return _connections.size() < BittorrentSettings.TORRENT_RESERVED_LIME_SLOTS.getValue() + BittorrentSettings.TORRENT_MAX_CONNECTIONS.getValue();
		}
		return _connections.size() < BittorrentSettings.TORRENT_MAX_CONNECTIONS.getValue();
	}

	/**
	 * Add a given BitTorrent connection to the list of them this ManagedTorrent object keeps.
	 * Adds the given BTConnection object to our _connections list.
	 * 
	 * Only BTHandshaker.tryToFinishHandshakes() calls this method.
	 * 
	 * @param btc A BTConnection object that represents a remote computer we have a new connection with to share this torrent
	 */
	public void addConnection(final BTConnection btc) {

		// Make a note we're adding the connection
		if (LOG.isDebugEnabled()) LOG.debug("trying to add connection " + btc.toString());

		/*
		 * this check prevents a few exceptions that may be thrown if a
		 * connection initialization is completed after we have already stopped
		 * happens especially when a user quits a torrent manually.
		 */

		// We're not supposed to make network communications any longer
		if (_stopped) {

			// Close the connection, and leave without adding it to anything
			btc.close();
			return;
		}

		// Add the given BTConnection object to our _connections list
		_connections.add(btc);
		if (LOG.isDebugEnabled()) LOG.debug("added connection " + btc.toString());
	}

	/**
	 * Remove a given BTConnection object from our _connections list.
	 * If that remote computer was interested in our data, and we weren't choking it, send all our connections a Choke or Unchoke message right now.
	 * 
	 * Only connectionClosed() calls this method.
	 * 
	 * @param btc A BTConnection object that we're closing
	 */
	private void removeConnection(final BTConnection btc) {

		// Make a note we're removing the given connection from our list
		if (LOG.isDebugEnabled()) LOG.debug("removing connection " + btc.toString());

		// Remove the given connection from the _connections list
		_connections.remove(btc);

		// If this remote computer is interested in our data, and we're not witholding data from it, send all our connections a Choke or Unchoke message right now
		if (btc.isInterested() && !btc.isChoked()) rechoke();
	}

	/**
	 * Move files and make network communications for finishing downloading this torrent.
	 * 
	 * Closes or chokes all our connections.
	 * Moves the files we downloaded from the "Incomplete" folder to the "Shared" folder.
	 * Contacts our tracker with "event=complete".
	 */
	private void saveCompleteFiles() {

		// If we've already moved the files of this torrent into the "Shared" folder, don't do it again
		if (_saved) return;

		// Have the "NIODispatcher" thread run this run() method that will close or choke our connections
		Runnable r = new Runnable() {
			public void run() {

				// Stop sending data
				LOG.debug("global choke");
				setGlobalChoke(true);

				// Loop for each connection to a remote computer we're sharing this torrent with
				for (Iterator iter = getConnections().iterator(); iter.hasNext(); ) {
					BTConnection btc = (BTConnection)iter.next();

					/*
					 * close connections that aren't interested in any part of a
					 * complete torrent
					 */

					// If this peer isn't interested in us even though we have the whole file
					if (!btc.isInterested()) {

						// Close our connection to it
						btc.close();

					// This peer is interested in our data
					} else {

						/*
						 * cancel all requests, if there are any left. (This should not
						 * be the case at this point anymore)
						 */

						// Leave the connection open, but tell the remote computer we don't want anything
						btc.cancelAllRequests();
						btc.sendNotInterested();
					}
				}
			}
		};
		NIODispatcher.instance().invokeLater(r);

		// Move the files we downloaded from the "Incomplete" folder to the "Shared" folder
		saveFiles();

		// Contact our tracker with "event=complete", telling it we've completed the file while in contact with it
		announceComplete();
	}

	/**
	 * Move the files we downloaded for this torrent from the "Incomplete" folder to the "Shared" folder.
	 * 
	 * Only runs once, and sets _saved to true.
	 * Closes all the VerifyingFolder object's open files.
	 * Moves what we downloaded from LimeWire's "Incomplete" folder to the "Shared" folder.
	 * Resumes a normal choking pattern of giving data to our peers.
	 * 
	 * this is executed within the timer thread
	 * but since we don't want to upload or download anything while we are
	 * saving the files, it should be okay
	 */
	private void saveFiles() {

		// Only do this once
		if (_saved) return;

		// Close all the VerifyingFolder object's open files
		_folder.close();
		if (LOG.isDebugEnabled()) LOG.debug("folder closed");

		// Move the torrent file or folder we downloaded from the "Incomplete" folder to the "Shared" folder
		_couldNotSave = !_info.moveToCompleteFolder(); // Returns false on error, set _couldNotSave to true
		if (LOG.isDebugEnabled()) LOG.debug("could not save: " + _couldNotSave);

		// Have the BTMetaInfo object remake its VerifyingFolder object, and save the new one as _folder
		_folder = _info.getVerifyingFolder();                       // Save the new VerifyingFolder object as _folder, overwriting the old one
		if (LOG.isDebugEnabled()) LOG.debug("new veryfing folder"); // Make a note that we have a new VerifyingFolder object

		try {

			// Open the new VerifyingFolder for writing
			_folder.open();

		// Unable to open it, record that we had a problem saving
		} catch (IOException ioe) {
			LOG.debug(ioe);
			_couldNotSave = true;
		}
		if (LOG.isDebugEnabled()) LOG.debug("folder opened");

		// If the VerifyingFolder had an error, stop all our Internet communications
		if (_couldNotSave) stopNow();

		// Instead of not giving data to anyone, resume a normal pattern of giving data to some peers
		setGlobalChoke(false);

		// Set _saved to true so we won't do this again
		_saved = true;
	}

	/**
	 * Contact our tracker with "event=complete".
	 * If we finish downloading the torrent while in contact with our tracker, we should tell it this.
	 * Don't tell a tracker "event=complete" if you connect to it with a complete file ready to share.
	 */
	private void announceComplete() {

		/*
		 * should we announce how much we've downloaded if we just resumed
		 * the torrent?  Its not mentioned in the spec...
		 */

		// Loop for each of our trackers
		for (int i = 0; i < _info.getTrackers().length; i++) {

			// Contact our tracker with "event=complete", and add the IP addresses of its response to our _peers list
			announceBlocking(_info.getTrackers()[i], TrackerRequester.EVENT_COMPLETE);
		}
	}

	/**
	 * Schedule the RouterService to call announce(url) minDelay milliseconds from now.
	 * This is method 1 in the tracker contact loop.
	 * 
	 * @param minDelay The time in milliseconds to wait before contacting our tracker
	 * @param url      Our tracker's address on the Web
	 */
	private void scheduleTrackerRequest(long minDelay, final URL url) {

		/*
		 * a tracker request can take quite a few seconds (easily up to 30)
		 * it will slow us down since we cannot enqueue any further pieces
		 * during that time - it may become necessary to do tracker requests
		 * in their own thread
		 */

		// Have the RouterService call this run() method minDelay milliseconds from now
		Runnable announcer = new Runnable() {
			public void run() {

				// Contact our BitTorent tracker on the Web, add the IP addresses it gives us to our _peers list, and schedule the next time we'll contact it
				if (LOG.isDebugEnabled()) LOG.debug("announcing to " + url.toString());
				announce(url);
			}
		};
		RouterService.schedule(announcer, minDelay, 0); // 0 to not repeat this, just run it once minDelay from now
	}

	/**
	 * Determine if we're connected to a given IP address.
	 * 
	 * Takes a TorrentLocation that has the IP address and port number of a remote computer sharing the same torrent as us.
	 * Loops through our _connections list, looking for a BTConnection object that has the given IP address.
	 * Returns true if it finds one.
	 * 
	 * @return true if we're connected to that address, false if we're not
	 */
	private boolean isConnectedTo(TorrentLocation to) {

		// Loop through the BTConnection objects in our _connections list, these are the remote computers we have TCP socket connections to sharing this torrent
		for (Iterator iter = getConnections().iterator(); iter.hasNext(); ) {
			BTConnection btc = (BTConnection)iter.next();

			/*
			 * compare by address only. there's no way of comparing ports or peer ids
			 */

			// If the addresses match, we're already connected, return true
			if (btc.getEndpoint().getAddress().equals(to.getAddress())) return true;
		}

		// No match found, we're not connected to the given address, return false
		return false;
	}

	/** Not used. */
	long calculateWaitTime() {
		if (_peers.size() == 0) return 0;
		long ret = Long.MAX_VALUE;
		long now = System.currentTimeMillis();
		synchronized(_peers) {
			for (Iterator iter = _peers.iterator(); iter.hasNext(); ) {
				ret = Math.min(ret, ((TorrentLocation) iter.next()).getWaitTime(now));
				if (ret == 0) return 0;
			}
		}
		return ret;
	}

	/**
	 * Get an IP address of a remote computer sharing this torrent for us to try to connect to.
	 * 
	 * Takes a TorrentLocation object from the _peers list.
	 * Removes it from the _peers list and returns it, making the _peers list only contain addresses we're not trying to connect to.
	 * 
	 * Only BTConnectionFetcher.fetchConnection() calls this.
	 * 
	 * @return A TorrentLocation object that has an IP address and port number we can try to connect to.
	 *         null if the _peers list doesn't have any.
	 */
	TorrentLocation getTorrentLocation() {

		// Get the time now
		long now = System.currentTimeMillis();

		// Only let 1 thread access the _peers list at a time
		synchronized (_peers) {

			// Loop for each TorrentLocation in the _peers list
			Iterator iter = _peers.iterator();
			while (iter.hasNext()) { // These are the IP addresses of remote computers we're not connected to that our tracker said are sharing the same torrent as us
				TorrentLocation temp = (TorrentLocation)iter.next();

				// If we've tried and failed to connect to this address in the last 5 minutes, skip it and loop to get the next one
				if (temp.isBusy(now)) continue;

				// Remove this TorrentLocation from the _peers list, now that we are going to try to connect to it, we don't want it there anymore
				iter.remove();

				// If we're not already connected to the address
				if (!isConnectedTo(temp)) {

					// Return it
					return temp;
				}
			}
		}

		// The _peers list doesn't have an address we can connect to
		return null;
	}

	/**
	 * Send each of our connections a Choke or Unchoke message 30 seconds from now, and every 30 seconds afterwards.
	 * This method doesn't send the messages right now, for that, call rechoke().
	 * 
	 * Makes a PeriodicChoker object that send a Choke or Unchoke message to each of our connections, and then does it again 30 seconds later.
	 * Has the RouterService call its run() method 30 seconds from now.
	 */
	private void scheduleRechoke() {

		// If we have a PeriodicChoker from the last time, set it's stopped boolean to true
		if (choker != null) choker.stopped = true; // This prevents its run() method from doing anything when the RouterService calls it within the next 30 seconds

		// Make a new PeriodicChoker, and have the RouterService call its run() method 30 seconds from now
		choker = new PeriodicChoker();
		RouterService.schedule(choker, RECHOKE_TIMEOUT, 0); // It will reschedule itself with the RouterService to run every 30 seconds
	}

	/**
	 * Send each of this torrent's connections a Choke or Unchoke message right now.
	 * 
	 * Makes a new Rechoker object, giving it our current list of BTConnection objects.
	 * Has the "NIODispatch" thread call the run() method on that Rechoker object right now.
	 */
	void rechoke() {

		// Make a new Rechoker object, giving it our current list of connections, and have the "NIODispatch" thread call its run() method right now
		NIODispatcher.instance().invokeLater(new Rechoker(_connections));
	}

	/**
	 * A PeriodicChoker makes a Rechoker object which sends a Choke or Unchoke message to each of our connections, and then does that again 30 seconds later.
	 * 
	 * The scheduleRechoke() method above makes this ManagedTorrent's PeriodicChoker object, and saves it as choker.
	 * 30 seconds later, the RouterService calls the run() method here.
	 * It makes a new Rechoker object, which sends a Choke or Unchoke message to each of our connections right away.
	 * The run() method also schedules itself with the RouterService to get called 30 seconds later.
	 */
	private class PeriodicChoker implements Runnable {

		/**
		 * Count the number of times this runs.
		 * 
		 * scheduleRechoke() makes a PeriodicChoker each time it's called, but that PeriodicChoker can live a long time.
		 * Every 30 seconds, the RouterService will call the PeriodicChoker's run() method.
		 */
		private int run = 0;

		/**
		 * True if the ManagedTorrent has replaced this PeriodicChoker with a new one, so this old one shouldn't do anything.
		 * 
		 * scheduleRechoke() sets stopped to true before making a new PeriodicChoker().
		 * The new one will send out Choked and Unchoked messages 30 seconds from then, and every 30 seconds afterwards.
		 * When this happens, it won't schedule itself again, and it will get garbage collected.
		 */
		volatile boolean stopped;

		/**
		 * Send each of our connections a Choke or Unchoke method, and schedule this to run again 30 seconds from now.
		 * 
		 * This run() method does 2 things:
		 * It makes a new Rechoker object, which will send each of our connections a Choke or Unchoke method.
		 * It has the RouterService call this run() method again 30 seconds from now.
		 */
		public void run() {

			// If the scheduleRechoke() method is going to replace this PeriodicChoker with a new one, it has set stopped to true
			if (stopped) return;

			// Make a note we're going to send Choke and Unchoke mesages to our connections
			if (LOG.isDebugEnabled()) LOG.debug("scheduling rechoke");

			// A List we'll point at _connections, or at a shuffled copy of that list
			List l;

			// If run is a multiple of 3, like 0, 3, 6, 9, 12
			if (run++ % 3 == 0) { // After it determines if run is a multiple of 3, move run to the next number

				// Make a copy of the _connections list named l, and shuffle the BTConnections in it into random order
				l = new ArrayList(_connections);
				Collections.shuffle(l);

			// run wasn't a multiple of 3
			} else {

				// No need to shuffle it, just point l and the _connections list
				l = _connections;
			}

			// Make a new Rechoker object, giving it the _connections list, and have the "NIODispatch" thread call its run() method
			NIODispatcher.instance().invokeLater(new Rechoker(l));

			// Have the RouterService call this run() method 30 seconds from now
			RouterService.schedule(this, RECHOKE_TIMEOUT, 0);
		}
	}

	/**
	 * Make a Rechoker object and have a thread run it to send a Choke or Unchoke message to each of our connections.
	 * 
	 * Rechoker implements the Runnable interface, which means it has a run() method.
	 * You can give a Rechoker object to a thread, and the thread will call the run() method once and then exit.
	 * 
	 * The constructor takes a list of BTConnection objects, they represent our connected peers.
	 * The run() method chooses which to choke and unchoke, and sends the appropriate message to each.
	 */
	private class Rechoker implements Runnable {

		/** A list of BTConnection objects that represent our BitTorrent connections to remote computers sharing our torrent. */
		private final List connections;

		/**
		 * Make a new Rechoker object, which will send each of our connections a Choke or Unchoke message.
		 * 
		 * @param connections A list of BTConnection objects.
		 *                    These represent our TCP socket connections to remote computers sharing the same torrent as us.
		 *                    PeriodicChoker.run() shuffled this list before it gave it to this constructor, so it's in random order.
		 */
		Rechoker(List connections) {

			// Save the given shuffled list of BTConnection objects in this new object
			this.connections = connections;
		}

		/**
		 * Send each of our connetions a Choke or Unchoke message.
		 * 
		 * Loops through all this torrent's connections, sending each a Choke or Unchoke message.
		 * Unchokes the connections that have been giving us the most data.
		 * Also chooses one connection randomly to unchoke, giving it a chance to see how fast it can send us data after we start sending it some.
		 * 
		 * When you give a Rechoker object to a thread, it will call this run() method once and then exit.
		 * The "NIODispatch" thread calls this run() method.
		 */
		public void run() {

			// If we're moving files from the "Incomplete" folder to the "Saved" folder, we can't give any peer any data, leave without doing anything
			if (_globalChoke) {
				LOG.debug("global choke");
				return;
			}

			// Loop for each of our connections, adding those that are interested in our data to the fastest list
			List fastest = new ArrayList(connections.size()); // Make an ArrayList we'll add the interested connections to
			for (Iterator iter = connections.iterator(); iter.hasNext(); ) {
				BTConnection con = (BTConnection) iter.next();

				// If this remote computer is interested in the parts of the torrent that we have, add it to the fastest list
				if (con.isInterested() && con.shouldBeInterested()) fastest.add(con);
			}

			// Sort the fastest list into order, now the remote computer that's giving us data the fastest will be listed first
			Collections.sort(fastest, DOWNLOAD_SPEED_COMPARATOR); // Have Collections.sort call DownloadSpeedComparator.compare(c1, c2) to pick the fastest

			/*
			 * Unchoke the fastest connectsion that are interested in us.
			 * 
			 * In BitTorrent, unchoking a connection means we're going to give it data.
			 * An intersted connection is one that wants data from us.
			 * 
			 * We want to unchoke the connections that have been giving us data the fastest.
			 * This rewards them for being nice to us by being nice to them in return.
			 * 
			 * The code below doesn't actually change the fastest list.
			 * This means we're going to unchoke all the connections that are interested in us, regardless of how quickly they're giving us data.
			 */

			// Doesn't actually change the fastest list at all
			int numFast = getNumUploads() - 1;
			for (int i = fastest.size() - 1; i >= numFast; i--) fastest.remove(i);

			/*
			 * Optimistically unchoke one interested connection.
			 * 
			 * In BitTorrent, we randomly unchoke a connection interested in our data.
			 * We don't choose it based on how much data it's giving us, we just randomly choose it.
			 * This gives new connections a chance.
			 * 
			 * The line of code below is:
			 * 
			 * 4 - the number of connections interested in our data
			 * 
			 * So, if we only have 2 connections interested in our data, it will be 4 - 2 = 2.
			 * Math.max then takes that answer, if it's bigger than 1.
			 * 
			 * The idea here is that if we only have a few connections interested in us, we'll unchoke more than just 1.
			 */

			// Choose how many connections we will optimistically unchoke, set optimistic to 1 or more
			int optimistic = Math.max(1, BittorrentSettings.TORRENT_MIN_UPLOADS.getValue() - fastest.size());

			// Loop for each of our connections
			for (Iterator iter = connections.iterator(); iter.hasNext(); ) {
				BTConnection con = (BTConnection)iter.next();

				// Look for con in the fastest list, the list of connections that are interested in our data
				if (fastest.remove(con)) { // Returns true if the fastest list contained the con object, meaning con is interested in our data

					// Tell that connection that we're not choking it any longer
					con.sendUnchoke();

				// con wasn't in the fastest list, meaning it's not interested in our data, and
				} else if (
					optimistic > 0 &&           // Yes, optimistic will be 1 or more, and
					con.shouldBeInterested()) { // This connection should want our data, even though it hasn't told us it's interested officially

					/*
					 * This is weird, but this is how Bram does it.
					 */

					// Tell the connection that we're not choking it any longer
					con.sendUnchoke();

					// Does this run? if isInterested() is true, then con would have been in the fastest list
					if (con.isInterested()) optimistic--;

				// con isn't intersted and shouldn't be
				} else {

					// Tell it we're choking it
					con.sendChoke();
				}
			}
		}
	}

	/**
	 * Determine how many of our connections we should give data right now.
	 * This is the number of our peers that should be unchoked.
	 * 
	 * As written, this method returns 100.
	 * It looks like it should return a number like 7, 2, 3, or 4, though, depending on how fast our Internet connection is letting us send data.
	 * 
	 * @return 100
	 */
	private static int getNumUploads() {

		// Read 100 from settings, and return it
		int uploads = BittorrentSettings.TORRENT_MAX_CONNECTIONS.getValue();
		if (uploads > 0) return uploads;

		/*
		 * This method was copied directly from Bram Cohen's BitTorrent client.
		 */

		// Find out the speed the user is letting the program upload data at
		float rate = UploadManager.getUploadSpeed(); // Returns the speed in bytes/s

		// Sort by the speed
		if (rate == Float.MAX_VALUE) return 7; // "unlimited, just guess something here..." -Bram
		else if (rate <  9000) return 2; // Less than 9000 bytes/s, only send 2 computers data per torrent
		else if (rate < 15000) return 3;
		else if (rate < 42000) return 4;
		else return (int)Math.sqrt(rate * 0.6f); // Calculate the number with this equation
	}

	/**
	 * Send all our connections Choke messages right now, or pick which to choke and send each a Choke or Unchoke message right now.
	 * 
	 * Chokes all connections instantly and does not unchoke any of them until
	 * it is set to false again. This effectively suspends all uploads and it is
	 * used while we are moving the files from the incomplete folder to the
	 * complete folder.
	 * 
	 * @param choke true to send each of our connections a Choke message right now
	 *              false to pick which connections we want to choke or unchoke, and send a Choke or Unchoke message to each one
	 */
	private void setGlobalChoke(boolean choke) {

		// Save the given setting
		_globalChoke = choke;

		// The caller told us to choke all connections
		if (choke) {

			// Loop for each of our connections
			for (Iterator iter = getConnections().iterator(); iter.hasNext(); ) {
				BTConnection btc = (BTConnection)iter.next();

				// Send the remote computer a Choke message, telling it we won't be sending it any more data
				btc.sendChoke();
			}

		// The caller told us to no longer choke all connections
		} else {

			// Decide which of our connections to choke or rechoke, and send each one a Choke or Unchoke message right now
			rechoke();
		}
	}

	/**
	 * Make a new thread named "TrackerRequest", and have it call announceBlocking(url, TrackerRequest.EVENT_NONE) right now.
	 * This is method 2 in the tracker contact loop.
	 * 
	 * @param url The tracker's address on the Web
	 */
	private void announce(final URL url) {

		/*
		 * offload tracker requests, - it simply takes too long even to execute
		 * it in our timer thread
		 */

		// Make a new thread named "TrackerRequest" that will run this run() method right now
		Runnable trackerRequest = new Runnable() {
			public void run() {

				// Make a note
				if (LOG.isDebugEnabled()) LOG.debug("announce thread for " + url.toString());

				// Shouldn't this be if _stopped, not if not stopped? (do)
				if (!_stopped) return;

				// Contact our BitTorent tracker on the Web, add the IP addresses it gives us to our _peers list, and schedule the next time we'll contact it
				announceBlocking(url, TrackerRequester.EVENT_NONE);
			}
		};
		ManagedThread thread = new ManagedThread(trackerRequest, "TrackerRequest");
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Contact our BitTorrent tracker on the Web, waiting here for up to 25 seconds for it to respond, and then give its response to handleTrackerResponse(tr, url).
	 * This is method 3 in the tracker contact loop.
	 * 
	 * In addition to getting called in the tracker contact loop, announceComplete(), stopNow(), and announceStart() call this method.
	 * 
	 * @param url   A Java URL object that has the Web address of the tracker
	 * @param event The event to tell the tracker, like TrackerRequester.EVENT_STOP to say "event=stop"
	 */
	private void announceBlocking(final URL url, final int event) {

		// Make a note in the debugging log
		if (LOG.isDebugEnabled()) LOG.debug("connecting to tracker " + url.toString() + " for event " + event);

		// Contact our BitTorent tracker on the Web, this method block while we're navigating to the tracker's address
		TrackerResponse tr = TrackerRequester.request( // Returns a TrackerResponse object that represents the bencoded data of the tracker's response
			url,                 // The Web address of the tracker
			_info,               // The BTMetaInfo object that has the information from the .torrent file
			ManagedTorrent.this, // This ManagedTorrent object (do) why isn't this just "this" instead of "ManagedTorrent.this"
			event);              // The requested event, like TrackerRequester.EVENT_STOP to include "event=stop"

		// Add the IP addresses the tracker told us to our _peers list, and schedule the next time we'll contact it
		handleTrackerResponse(tr, url);
	}

	/**
	 * Get the number of IP addresses to remote computers sharing this torrent that we know about.
	 * Our tracker told us these addresses.
	 * We can try connecting to them to share this torrent with them.
	 * 
	 * Returns the size of the _peers list, which contains TorrentLocation objects.
	 * 
	 * @return The number of addresses we know
	 */
	public int getNumberOfAlternateLocations() {

		// Return the number of TorrentLocations we have in the _peers list
		return _peers.size();
	}

	/**
	 * Get the number of addresses the tracker told us that we tried to connect to, but couldn't, in the last hour.
	 * 
	 * @return The number of TorrentLocation objects in our _badPeers list
	 */
	public int getNumberOfInvalidAlternateLocations() {

		// Return the number of TorrentLocation objects we've added to _badPeers after having trouble connecting to them
		return _badPeers.size();
	}

	/**
	 * Determine if this torrent is paused, and not transferring any data because of that.
	 * Use the pause() and resume() methods to set this condition.
	 * 
	 * @return true if this torrent is paused, false if it's not paused
	 */
	public boolean isPaused() {

		// Return the value of the _paused flag
		return _paused;
	}

	/**
	 * Determine if this ManagedTorrent object is the same as another one.
	 * Compares their info hashes, the SHA1 hash of the "info" bencoded dictionary in the .torrent file.
	 * 
	 * @param o The ManagedTorrent object to compare this one to
	 */
	public boolean equals(Object o) {

		// Make sure the given object is a ManagedTorrent
		if (!(o instanceof ManagedTorrent)) return false; // Different kind of object, false, not the same
		ManagedTorrent mt = (ManagedTorrent)o;

		// Compare their info hashes
		return Arrays.equals(mt.getInfoHash(), getInfoHash());
	}

	/**
	 * Determine which remote computers are giving us data the fastest.
	 * 
	 * The DownloadSpeedComparator object implements Java's Comparator class.
	 * This means you can hand a DownloadSpeedComparator object to a method like Collections.sort().
	 * It will use the compare() method here to determine which of two objects should be listed first.
	 * 
	 * The ManagedTorrent class makes a single DownloadSpeedComparator, named DOWNLOAD_SPEED_COMPARATOR.
	 * Rechoker.run() uses it to sort our list of interested remote computers into order based on how fast they are giving us data.
	 */
	public static class DownloadSpeedComparator implements Comparator {

		/**
		 * Determine which of two remote computers is giving us data the fastest.
		 * 
		 * @param o1 A BTConnection object that represents our TCP socket connection to a remote computer sharing the same torrent as us.
		 * @param o2 A BTConnection object that represents the same thing, the second computer to compare with the first.
		 * @return   -1 if the first computer is giving us data faster than the second.
		 *           1 if the second computer is giving us data the fastest.
		 *           0 if it is a tie.
		 */
		public int compare(Object o1, Object o2) {

			// Look at both given objects as BTConnection objects
			BTConnection c1 = (BTConnection)o1;
			BTConnection c2 = (BTConnection)o2;

			// A number to compare the download speeds
			float bw = 0;

			try {

				// Subtract the BTConnection object's current speeds, bw will be positive if c1 is giving us data faster
				bw = c1.getMessageReader().getBandwidthTracker().getMeasuredBandwidth() - c2.getMessageReader().getBandwidthTracker().getMeasuredBandwidth();

				// Only base our decision on download speed if the difference is bigger than 0.1 KB/s
				if (bw > 0.1) return -1;      // The first computer is giving us data faster than the second, return -1
				else if (bw < -0.1) return 1; // The second computer is giving us data the fastest, return 1

			// One of the calls to getMeasuredBandwidth() couldn't calculate the current speed because we don't have 3 seconds of data yet, decide below
			} catch (InsufficientDataException ide) {}

			/*
			 * The computer's current speeds are too close, or we don't have enough information about one or both.
			 * Use their total average speeds instead.
			 */

			// Compare their total average bandwidth instead, bw will be positive if c1 has given us more data faster
			bw = c1.getMessageReader().getBandwidthTracker().getAverageBandwidth() - c2.getMessageReader().getBandwidthTracker().getAverageBandwidth();

			// Look at the result
			if      (bw > 0) return -1; // The first computer has given us more data faster, return -1
			else if (bw < 0) return 1;  // The second computer is faster, return 1
			return 0;                   // It's a tie
		}

		/**
		 * Not implemented.
		 * 
		 * @return false
		 */
		public boolean equals(Object o) {

			// Always return false, say the objects are different
			return false;
		}
	}

	/**
	 * We don't have a download throttle yet.
	 * 
	 * @return null
	 */
	public Throttle getDownloadThrottle() {

		// Return null because we have no download throttle
		return null;
	}

	/**
	 * Get a reference to the TorrentManager's NBThrottle object.
	 * 
	 * The program has a single TorrentManager object that keeps a list of ManagedTorrent objects like this one.
	 * The TorrentManager object has a NBThrottle object, a non-blocking throttle.
	 * 
	 * @return The program's NBThrottle for BitTorrent
	 */
	public Throttle getUploadThrottle() {

		// Return a reference to the TorrentManager object's single NBThrottle object
		return _manager.getUploadThrottle();
	}

	/**
	 * Add a given object to our ProcessingQueue, which will have the "ManagedTorrent" thread call run() on it.
	 * 
	 * @param runnable An object with a run() method
	 */
	public void enqueueTask(Runnable runnable) {

		// Add the given object to our ProcessingQueue
		_processingQueue.add(runnable);
	}

	/**
	 * Determine if we have any TCP socket connections open to remote computers we're sharing this torrent with.
	 * 
	 * @return true if we're sharing this torrent with at least one computer, false if we're not
	 */
	public boolean isConnected() {

		// If our _connections list has at least 1 BTConnection object, return true
		return _connections.size() > 0;
	}

	/**
	 * Determine if we've stopped this torrent.
	 * This means we're not sending or receiving file data for it any longer.
	 * 
	 * When the program calls stopNow() to stop transferring data for this torrent, it sets _stopped to true, and hasStopped() returns true.
	 * 
	 * @return True if the program has called stopNow() to stop this torrent
	 */
	public boolean hasStopped() {

		// Check the _stopped flag
		return _stopped;
	}

	/**
	 * Get a list of the TCP socket connections we've made to remote computers sharing this torrent.
	 * 
	 * @return A CoWList of BTConnection objects
	 */
	public List getConnections() {

		// Return a reference to our connections list
		return _connections;
	}

	/**
	 * Find out how many remote computers we have open connections to sharing this torrent through.
	 * 
	 * @return The size of our connections list
	 */
	public int getNumConnections() {

		// Return the number of BTConnection objects in the _connections list
		return _connections.size();
	}

	/**
	 * Find out how many addresses we've failed connecting to in the last hour.
	 * We tried to connect to these addresses to share this torrent.
	 * 
	 * @return The number of addresses we were unable to connect to in the last hour
	 */
	public int getNumBadPeers() {

		// Return the number of TorrentLocation objects addBadEndpoint() has put in the _badPeers list
		return _badPeers.size();
	}

	/**
	 * Find out how many addresses we know of remote computers sharing this torrent.
	 * The tracker told us these addresses.
	 * We're not connected to any of these addresses now, and haven't tried any of them yet.
	 * We could try connecting to them to share this torrent.
	 * 
	 * @return The number of addresses in our list
	 */
	public int getNumPeers() {

		// Return the number of TorrentLocation objects in our _peers list
		return _peers.size();
	}

	/**
	 * Find out how many connections we have open to remote computers that don't have any parts of this torrent that we want.
	 * Loops through our connections, counting those that we're not interested in.
	 * These computers have nothing we want.
	 * 
	 * @return The number of remote computers we're connected to, but not interested in
	 */
	public int getNumBusyPeers() {

		// Make an int to count how many computers have nothing we want
		int busy = 0;

		// Loop for each of our open connections
		for (Iterator iter = _connections.iterator(); iter.hasNext(); ) {
			BTConnection con = (BTConnection)iter.next();

			// If this remote computer isn't interesting to us, count it
			if (!con.isInteresting()) busy++;
		}

		// Return the number of computers that have nothing we want
		return busy;
	}

	/**
	 * Get the BTUploader object that lets LimeWire's GUI list this torrent with the Gnutella uploads.
	 * 
	 * @return A reference to this ManagedTorrent's BTUploader
	 */
	public BTUploader getUploader() {

		// Return the object our constructor made
		return _uploader;
	}

	/**
	 * Get the BTDownloader object that lets LimeWire's GUI list this torrent with the Gnutella downloads.
	 * 
	 * @return A reference to this ManagedTorrent's BTDownloader
	 */
	public BTDownloader getDownloader() {

		// Return the object our constructor made
		return _downloader;
	}

	/**
	 * Determine if we have any addresses to try connecting to.
	 * Looks in our _peers list for a TorrentLocation object that we haven't tried in the last 5 minutes.
	 * 
	 * @return True if we have a TorrentLocation we can try now
	 */
	boolean hasNonBusyLocations() {

		// Get the time now
		long now = System.currentTimeMillis();

		// Make sure only one thread accesses the _peers list at a time
		synchronized (_peers) {

			// Loop for each of the TorrentLocation objects in our _peers list, these are addresses our tracker told us that we can try connecting to
			Iterator iter = _peers.iterator();
			while (iter.hasNext()) {
				TorrentLocation to = (TorrentLocation)iter.next();

				// If we haven't failed connecting to this address in the last 5 minutes, return true
				if (!to.isBusy(now)) return true;
			}
		}

		// Our _peers list doesn't have any addresses for us to try right now
		return false;
	}

	/**
	 * Determine if we should stop sharing this torrent.
	 * Looks for several conditions to return true, indicating we should give up.
	 * If we don't have any connections nor any addresses to try, and we've had trouble connecting to our tracker, give up.
	 * If we don't have any connections nor any addresses to try, and the TorrentManager says we should make room for other torrents, give up.
	 * If the TorrentManager says we should make room for other torrents, and we've uploaded more data for this torrent than we've downloaded, give up.
	 * 
	 * @return true if we should give up, false to keep sharing this torrent
	 */
	boolean shouldStop() {

		// We don't have any connections nor any addresses to try
		if (_connections.size() == 0 && // If we don't have any open connections to peers sharing this torrent, and
			_peers.size() == 0) {       // We don't have any addresses to try to connect to

			// If we've had trouble connecting to our tracker more than 5 times
			if (_trackerFailures > MAX_TRACKER_FAILURES) {

				// Give up
				if (LOG.isDebugEnabled()) LOG.debug("giving up, trackerFailures " + _trackerFailures);
				return true;

			// Our tracker is reachable, but the TorrentManager says we should make room for other torrents
			} else if (_manager.shouldYield()) {

				// Give up
				if (LOG.isDebugEnabled()) LOG.debug("making way for other downloader");
				return true;
			}

		// We have some connections or addresses, and we're done with this torrent and the TorrentManager says we should make room for another
		} else if (_folder.isComplete() && _manager.shouldYield()) {

			/*
			 * we stop if we uploaded more than we downloaded
			 * AND there are other torrents waiting for a slot
			 */

			// Make a note of the upload and download sizes we'll use to make a decision
			if (LOG.isDebugEnabled()) LOG.debug("uploaded data " + _uploader.getTotalAmountUploaded() + " downloaded data " + _downloader.getTotalAmountDownloaded());

			// If we've given out more data for this torrent than we've received, give up
			if (_uploader.getTotalAmountUploaded() > _downloader.getTotalAmountDownloaded()) return true;
		}

		// Keep sharing this torrent
		return false;
	}

	/**
	 * Get the BTConnectionFetcher this ManagedTorrent uses to keep 5 open connections to remote computers sharing this torrent.
	 * 
	 * @return A reference to our BTConnectionFetcher object
	 */
	public BTConnectionFetcher getFetcher() {

		// Return the BTConnectionFetcher that initializeTorrent() made
		return _connectionFetcher;
	}
}
