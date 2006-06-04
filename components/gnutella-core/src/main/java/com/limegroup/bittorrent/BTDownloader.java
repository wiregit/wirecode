
// Commented for the Learning branch

package com.limegroup.bittorrent;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;

/**
 * A BTDownloader object provides information about what we're downloading for this torrent.
 * 
 * BTDownloader implements LimeWire's Download interface.
 * This lets LimeWire's GUI list this torrent in the same list as the Gnutella downloads.
 * 
 * A ManagedTorrent makes and keeps a single BTDownloader object.
 */
public class BTDownloader implements Downloader {

	/** A link back up to the ManagedTorrent object that made this BTDownloader. */
	private final ManagedTorrent _torrent;

	/** A link to the BTMetaInfo object that represents the .torrent file for this torrent. */
	private final BTMetaInfo _info;

	/** A BTDownloader has a SimpleBandwidthTracker that measures how fast we're downloading this torrent. */
	private SimpleBandwidthTracker _tracker;

    /** A HashMap the GUI uses to keep additional information about this download. */
    protected Map attributes = new HashMap();

    /**
     * Make a new BTDownloader object that the GUI can use to get information about how this download is progressing.
     * 
     * @param torrent The ManagedTorrent that's making this BTDownloader
     * @param info    A BTMetaInfo object we made from the bencoded data in the .torrent file
     */
	public BTDownloader(ManagedTorrent torrent, BTMetaInfo info) {

		// Save the given objects
		_torrent = torrent;
		_info = info;

		// Make a new SimpleBandwidthTracker to keep track of how fast we're downloading data
		_tracker = new SimpleBandwidthTracker();
	}

	/**
	 * Stop this download.
	 * Calls stop() on the ManagedTorrent, and then removes it from the TorrentManager's list of them.
	 */
	public void stop() {

		// Tell the ManagedTorrent to stop
		if (!_torrent.hasStopped()) _torrent.stop();

		// Remove it from the program's list
		RouterService.getTorrentManager().removeTorrent(_torrent, true);
	}

	/**
	 * Pause this download.
	 * Calls pause() on the ManagedTorrent.
	 */
	public void pause() {

		// Have the ManagedTorrent do it
		_torrent.pause();
	}

	/**
	 * Determine if this BitTorrent download is paused.
	 * Asks the ManagedTorrent object.
	 * 
	 * @return True if the download is paused
	 */
	public boolean isPaused() {

		// Ask the ManagedTorrent object
		return _torrent.isPaused();
	}

	/**
	 * Determine if this BitTorrent download is paused or stopped.
	 * 
	 * @return True if the ManagedTorrent is paused or has stopped.
	 *         False if it's going.
	 */
	public boolean isInactive() {

		// Return true if the ManagedTorrent is paused or stopped
		return _torrent.isPaused() || _torrent.hasStopped();
	}

	/**
	 * The TorrentManager keeps torrents in a queue, get our position in it.
	 * 
	 * @return Our queue position number
	 */
	public int getInactivePriority() {

		// Ask the program's TorrentManager where we are in its list
		return RouterService.getTorrentManager().getPositionInQueue(_torrent);
	}

	/**
	 * Resume downloading this torrent.
	 * Has the ManagedTorrent object do it.
	 */
	public boolean resume() {

		// Have the ManagedTorrent object do it
		return _torrent.resume();
	}

	/**
	 * Get the path of this BitTorrent download.
	 * While we're still getting the torrent, returns a path like "C:\Documents and Settings\Kevin\Incomplete\File Name.ext".
	 * After it's done, returns a path like "C:\Documents and Settings\Kevin\Shared\File Name.ext".
	 * 
	 * @return A Java File object with the path
	 */
	public File getFile() {

		// Return the path in the "Saved" or "Incomplete" folder
		if (_torrent.isComplete()) return _info.getCompleteFile();
		return _info.getBaseFile();
	}

	/**
	 * Get the start of the file that we can preview play for the user.
	 * BitTorrent downloads don't happen from the start, so we can't do this.
	 * Returns null if the file isn't done yet, and the whole file if it is.
	 * 
	 * @return null if the torrent is still downloading.
	 *         A Java File object with the path to the saved file.
	 */
	public File getDownloadFragment() {

		// Return null until we have the entire torrent
		if (!_torrent.isComplete()) return null;
		return getFile();
	}

	/**
	 * Determine what state this BitTorrent download is in right now.
	 * 
	 * The possible states are:
     * QUEUED
     * CONNECTING
     * DOWNLOADING
     * WAITING_FOR_RETRY
     * COMPLETE
     * ABORTED
     * GAVE_UP
     * COULDNT_MOVE_TO_LIBRARY
     * WAITING_FOR_RESULTS
     * CORRUPT_FILE
     * 
     * @return The int code for the state
	 */
	public int getState() {

		// Ask the ManagedTorrent object
		return _torrent.getState();
	}

	/**
	 * Get the total number of bytes we've downloaded while getting this torrent.
	 * If we download the same part of the file twice, this number counts it both times.
	 * 
	 * @return The size of data in bytes
	 */
	public long getTotalAmountDownloaded() {

		// Ask our SimpleBandwidthTracker
		return _tracker.getTotalAmount();
	}

	/**
	 * Returns 0, this is not applicable to BitTorrent.
	 * 
	 * getRemainingStateTime() is supposed to return the longest time we'll stay in our current state, in seconds.
	 * If it's not known, getRemainingStateTime() is supposed to return Integer.MAX_VALUE.
	 * 
	 * @return 0
	 */
	public int getRemainingStateTime() {

		// Return 0, we can't support this part of the interface
		return 0;
	}

	/**
	 * Get the file name of this torrent.
	 * If this is a multifile torrent, returns the folder name.
	 * 
	 * @return The file or folder name as a String
	 */
	public String getFileName() {

		// Ask the BTMetaInfo object, get the name from the bencoded data in the .torrent file
		return _info.getName();
	}

	/**
	 * Get the file size of this torrent.
	 * If this is a multifile torrent, returns the total size of all the files placed together.
	 * 
	 * @return The size in bytes
	 */
	public long getContentLength() {

		// Get this information from the bencoded data in the .torrent file
		return _info.getTotalSize();
	}

	/**
	 * Get the size of the portion of the file we've downloaded.
	 * If we download the same part of the file twice, this number counts it just once.
	 * 
	 * @return The size of data in bytes
	 */
	public long getAmountRead() {

		// Ask the VerifyingFolder object
		return _info.getVerifyingFolder().getBlockSize();
	}

	/**
	 * Get a list of the IP addresses of the remote computers we're connected to in order to get this torrent.
	 * 
	 * @return An Iterator you can move down a list of Endpoint objects
	 */
	public Iterator getHosts() {

		// Make an empty HashSet we'll fill with Endpoint objects and return
		Set ret = new HashSet();

		// Loop for each BTConnection in the ManagedTorrent's list of them
		for (Iterator iter = _torrent.getConnections().iterator(); iter.hasNext(); ) {
			BTConnection btc = (BTConnection) iter.next();

			// If we're not choking this connection right now, add it's IP address to our list
			if (!btc.isChoking()) ret.add(btc.getEndpoint());
		}

		// Return the HashSet of Endpoint objects with the addresses
		return ret.iterator();
	}

	/**
	 * Returns "BitTorrent" instead of a Gnutella vendor code like "LIME".
	 * 
	 * @return The String "BitTorrent"
	 */
	public String getVendor() {

		// Return the String "BitTorrent"
		return "BitTorrent";
	}

	/**
	 * BitTorent doesn't support chat.
	 * 
	 * @return null
	 */
	public Endpoint getChatEnabledHost() {

		// Return null instead of an Endpoint object
		return null;
	}

	/**
	 * Determine if we can chat with any of the remote computers that we're getting this torrent from.
	 * 
	 * @return false, BitTorrent can't do this
	 */
	public boolean hasChatEnabledHost() {

		// Return false, we can't chat
		return false;
	}

	/**
	 * Does nothing, BitTorrent never gives up because of corrupt data.
	 * 
	 * @param delete Not used
	 */
	public void discardCorruptDownload(boolean delete) {

		// Do nothing
	}

	/**
	 * Returns null, BitTorrent programs don't support Gnutella's browse host.
	 * 
	 * @return null
	 */
	public RemoteFileDesc getBrowseEnabledHost() {

		// Return null instead of address information
		return null;
	}

	/**
	 * Determine if one of our sources can do the Gnutella browse host feature.
	 * 
	 * @return false, this is BitTorrent, so none can
	 */
	public boolean hasBrowseEnabledHost() {

		// BitTorrent can't do Gnutella browse host
		return false;
	}

	/**
	 * Find out what position we are in the queue.
	 * 
	 * @return 1, a queue position doesn't really make sense for BitTorrent
	 */
	public int getQueuePosition() {

		// Always return 1, say we're first in line
		return 1;
	}

	/**
	 * Find out how many computers we're connected to in order to get this torrent.
	 * 
	 * @return The number of connections we have for this torrent
	 */
	public int getNumberOfAlternateLocations() {

		// Count our connections
		return _torrent.getNumAltLocs();
	}

	/**
	 * Find out how many addresses we tried to connect to and we're unable to connect to while making connections to get this torrent.
	 * 
	 * @return The number of addresses we couldn't connect to
	 */
	public int getNumberOfInvalidAlternateLocations() {

		// Count the number of Endpoint objects the ManagedTorrent placed in its _badPeers list
		return _torrent.getNumBadPeers();
	}

	/**
	 * Find out how many more addresses we have of remote computers that have this torrent.
	 * 
	 * @return The number of addressess we could connect to
	 */
	public int getPossibleHostCount() {

		// Get the number of addresses in the ManagedTorrent object's _peers list
		return _torrent.getNumPeers();
	}

	/**
	 * Find out how many remote computers don't have any information that we need.
	 * Loops through our connections for this torrent, counting those that are not interesting to us.
	 * 
	 * @return The number of connections we have open, but aren't using
	 */
	public int getBusyHostCount() {

		// Have the ManagedTorrent count our connections that have no data we need
		return _torrent.getNumBusyPeers();
	}

	/**
	 * Get the number of hosts we are remotely queued on.
	 * For BitTorrent, these are remote computers we're connected to for this torrent, but that are choking us.
	 * 
	 * @param The number of connections we have but are witholding data from us
	 */
	public int getQueuedHostCount() {

		// Loop through the ManagedTorrent object's list of BTConnection objects
		int qd = 0;
		for (Iterator iter = _torrent.getConnections().iterator(); iter.hasNext(); ) {
			BTConnection c = (BTConnection)iter.next();

			// If this remote computer is choking us, count it
			if (c.isChoking()) qd++;
		}

		// Return the count
		return qd;
	}

	/**
	 * Determine if this BitTorrent download is finished yet.
	 * 
	 * @return true if it's done, false if not
	 */
	public boolean isCompleted() {

		// Ask the ManagedTorrent object
		return _torrent.isComplete();
	}

	/**
	 * Find out how much of this file we've verified as valid.
	 * 
	 * @return The size of verified data, in bytes
	 */
	public long getAmountVerified() {

		// Ask the VerifyingFolder object how many verified blocks we have, and multiply that by the block size to get bytes
		return _info.getVerifyingFolder().getVerifiedBlockSize();
	}

	/**
	 * Get the size of file pieces we're downloading.
	 * 
	 * @return The size of each file piece
	 */
	public int getChunkSize() {

		// Return the number we parsed from the .torrent file
		return (int)_info.getPieceLength();
	}

	/**
	 * Get the number of bytes we downloaded but then discarded because they didn't hash correctly.
	 * 
	 * @return The size in bytes
	 */
	public long getAmountLost() {

		// Ask the VerifyingFolder object
		return _info.getVerifyingFolder().getNumCorruptedBytes();
	}

	/**
	 * Have our SimpleBandwidthTracker calculate the current speed it's downloading data.
	 * 
	 * @return The speed, in KB/s
	 */
	public void measureBandwidth() {

		// Ask our SimpleBandwidthTracker
		_tracker.measureBandwidth();
	}

	/**
	 * Find out how fast we are downloading data for this torrent right now.
	 * 
	 * @return The speed, in KB/s
	 */
	public float getMeasuredBandwidth() {

		// Have our SimpleBandwidthTracker update its current speed right now
		measureBandwidth();

		// Get the current speed our SimpleBandwidthTracker has calculated
		return _tracker.getMeasuredBandwidth();
	}

	/**
	 * Get the total average bandwidth, the number of bytes we've downloaded divided by the time since we started downloading this torrent.
	 * 
	 * @return The total average bandwidth speed, in KB/s
	 */
	public float getAverageBandwidth() {

		// Ask the SimpleBandwidthTracker
		return _tracker.getAverageBandwidth();
	}

	/**
	 * No, this BitTorrent download isn't relocatable.
	 * 
	 * @return false
	 */
	public boolean isRelocatable() {

		// Always return false
		return false;
	}

	/**
	 * Does nothing.
	 */
	public void setSaveFile(File saveDirectory, String fileName, boolean overwrite) throws SaveLocationException {

		// TODO: decide how to deal with this
	}

	/**
	 * Get the path to where the program will save this BitTorrent download when it's done.
	 * 
	 * @param The path in a Java File object, like "C:\Documents and Settings\Kevin\Shared\File Name.ext"
	 */
	public File getSaveFile() {

		// Get the path from the BTMetaInfo object
		return _info.getCompleteFile();
	}

	/**
	 * Have this object record that we downloaded some more bytes of file data.
	 * 
	 * @param read The number of bytes we read
	 */
	void readBytes(int read) {

		// Give the number to our SimpleBandwidthTracker
		_tracker.count(read);
	}

	/**
	 * Get the torrent's info hash as a text URN, like "urn:sha1:JAZSGOLT6UP4I5N5KGJRZPSF6RZCEJKQ".
	 * The info hash is the SHA1 hash of the "info" section of the bencoded data of the .torrent file.
	 * 
	 * @return A String like "urn:sha1:JAZSGOLT6UP4I5N5KGJRZPSF6RZCEJKQ"
	 */
	public URN getSHA1Urn() {

		// Get it from the BTMetaInfo object
		return _torrent.getMetaInfo().getURN();
	}

	/**
	 * Remove an attribute from the attributes HashMap this BTDownloader keeps for external code to save notes in.
	 * 
	 * @param key The key name to remove from the HashMap
	 * @return    The result Object of the remove() method
	 */
	public Object removeAttribute(String key) {

		// Remove the key, and return the result
		return attributes.remove(key);
	}

	/**
	 * Look up an attribute from the attributes HashMap this BTDownloader keeps for external code to save notes in.
	 * 
	 * @param key The key name to look up
	 * @return    The Object stored under that key
	 */
	public Object getAttribute(String key) {

		// Look up the key name in the HashMap, and return the Object stored under it
		return attributes.get(key);
	}

	/**
	 * Find out how many bytes of torrent data we've received, but are waiting for a thread to write to disk.
	 * 
	 * @return The number of bytes
	 */
	public int getAmountPending() {

		// Ask our torrent's VerifyingFolder object
		return _info.getVerifyingFolder().getAmountPending();
	}

	/**
	 * Find out how many remote computers we have open connections to sharing this torrent through.
	 * 
	 * @return The number of connections we have
	 */
	public int getNumHosts() {

		// Ask the ManagedTorrent that made us, and represents this torrent
		return _torrent.getNumConnections();
	}

	/**
	 * Set an attribute in the attributes HashMap this BTDownloader keeps for external code to save notes in.
	 * 
	 * @param key   The key name to save the object under.
	 * @param value The value object to store under the key.
	 * @return      The object that was stored under that key before this one displaced it.
	 *              null if this is the first object stored under that key.
	 */
	public Object setAttribute(String key, Object value) {

		// Put the key and value in the attributes HashMap
		return attributes.put(key, value);
	}
}
