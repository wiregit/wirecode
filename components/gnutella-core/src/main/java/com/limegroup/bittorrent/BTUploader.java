
// Commented for the Learning branch

package com.limegroup.bittorrent;

import java.io.IOException;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.http.HTTPRequestMethod;

/**
 * A BTUploader object provides information about what we're uploading for this torrent.
 * 
 * BTUploader implements LimeWire's Uploader interface.
 * This lets LimeWire's GUI list this torrent in the same list as the Gnutella uploads.
 * 
 * A ManagedTorrent makes and keeps a single BTUploader object.
 */
public class BTUploader implements Uploader {

	/** A link back up to the ManagedTorrent object that made this BTDownloader. */
	private ManagedTorrent _torrent;

	/** A link to the BTMetaInfo object that represents the .torrent file for this torrent. */
	private BTMetaInfo _info;

	/** A BTUploader has a SimpleBandwidthTracker that measures how fast we're uploading the data of this torrent. */
	private SimpleBandwidthTracker _tracker;

	/**
     * Make a new BTUploader object that the GUI can use to get information about what we're uploading for this torrent.
	 * 
	 * @param torrent The ManagedTorrent that's making this BTUploader
	 * @param info    A BTMetaInfo object we made from the bencoded data in the .torrent file
	 */
	public BTUploader(ManagedTorrent torrent, BTMetaInfo info) {

		// Save the given objects
		_torrent = torrent;
		_info = info;

		// Make a new SimpleBandwidthTracker to keep track of how fast we're uploading data
		_tracker = new SimpleBandwidthTracker();
	}

	/**
	 * Stop this upload.
	 * Calls stop() on the ManagedTorrent.
	 */
	public void stop() {

		// Stop Internet communications related to this torrent, and remove it from the program
		_torrent.stop();
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
	 * Get the total size in bytes of the files this .torrent file describes.
	 * 
	 * If this is a single file torrent, _totalSize is the file size.
	 * If this is a multi-file torrent, _totalSize is the size of all the files totaled, and the size of the data block made by putting all the files together.
	 * 
	 * @return The number of bytes of data of this .torrent file describes
	 */
	public long getFileSize() {

		// Ask the BTMetaInfo object, get the total size from the bencoded data in the .torrent file
		return _info.getTotalSize();
	}

	/**
	 * Get the length of the requested size for uploading.
	 * Not used in BitTorrent, returns 0.
	 * 
	 * @return 0
	 */
	public long getAmountRequested() {

		// Not used, return 0
		return 0;
	}

	/**
	 * Make a FileDesc object with the save path, like "C:\Documents and Settings\Kevin\Shared\File Name.ext".
	 * This is the kind of object the GUI needs to be able to read the path and list the file.
	 * 
	 * Returns a new FileDesc object that is actually a FakeFileDesc.
	 * FakeFileDesc is a nested class in this BTMetaInfo class.
	 * 
	 * @return A FakeFileDesc object that has the path
	 */
	public FileDesc getFileDesc() {

		// Have the BTMetaInfo object do this
		return _info.getFileDesc();
	}

	/**
	 * Get the Gnutella file index for this upload.
	 * Returns Integer.MAX_VALUE because this is a torrent.
	 * 
	 * @return Integer.MAX_VALUE
	 */
	public int getIndex() {

		// Get the value the FakeFileDesc set, Integer.MAX_VALUE
		return _info.getFileDesc().getIndex();
	}

	/**
	 * Find out how much data we've uploaded while sharing this torrent.
	 * amountUploaded() and getTotalAmountUploaded() both return the same answer.
	 * 
	 * @return The amount we uploaded, in bytes
	 */
	public long amountUploaded() {

		// Get the total distance our SimpleBandwidthTracker has recorded
		return _tracker.getTotalAmount();
	}

	/**
	 * Find out how much data we've uploaded while sharing this torrent.
	 * amountUploaded() and getTotalAmountUploaded() both return the same answer.
	 * 
	 * @return The amount we uploaded, in bytes
	 */
	public long getTotalAmountUploaded() {

		// Get the total distance our SimpleBandwidthTracker has recorded
		return _tracker.getTotalAmount();
	}

	/**
	 * Should return the IP address of the remote computer we're uploading data to, as a String.
	 * Returns "Multiple", because with BitTorrent, we're sending many of our connections pieces.
	 * 
	 * @return The String "Multiple"
	 */
	public String getHost() {

		// Return the word "Multiple" instead of an IP address
		return "Multiple";
	}

	/**
	 * Determine what state this BitTorrent upload is in right now.
	 * 
	 * The possible states are:
	 * INTERRUPTED
	 * UPLOADING
     * 
     * @return The int code for the state
	 */
	public int getState() {

		// If the ManagedTorrent is stopped, return INTERRUPTED
		if (_torrent.hasStopped()) return Uploader.INTERRUPTED;

		// Otherwise, we're sharing this torrent online, return UPLOADING
		return Uploader.UPLOADING;
	}

	/**
	 * Get the previous state this uploader was in before it's current state.
	 * 
	 * @return Uploader.UPLOADING
	 */
	public int getLastTransferState() {

		// Alwasy return UPLOADING, since we can only change from that to INTERRUPTED
		return Uploader.UPLOADING;
	}

	/**
	 * Does nothing.
	 */
	public void setState(int state) {}

	/**
	 * Does nothing.
	 */
	public void writeResponse() throws IOException {}

	/**
	 * Determine if the computer we're uploading to supports Gnutella chat.
	 * 
	 * @return false, this is BitTorrent
	 */
	public boolean isChatEnabled() {

		// This torrent can't do Gnutella chat
		return false;
	}

	/**
	 * Determine if the computer we're uploading to supports the Gnutella browse host feature.
	 * 
	 * @return false, this is BitTorrent
	 */
	public boolean isBrowseHostEnabled() {

		// This torrent can't do Gnutella browse host
		return false;
	}

	/**
	 * Return the port number the computer we're uploading to is listening on for new Gnutella connections.
	 * 
	 * @return 0, this is BitTorrent
	 */
	public int getGnutellaPort() {

		// Return 0, this is BitTorrent
		return 0;
	}

	/**
	 * Get the "User-Agent" the computer we're uploading to told us in the Gnutella handshake.
	 * 
	 * @return "BitTorrent"
	 */
	public String getUserAgent() {

		// There is no Gnutella handshake
		return "BitTorrent";
	}

	/**
	 * Determine if the headers have been parsed.
	 * 
	 * @return true
	 */
	public boolean isHeaderParsed() {

		// Just return true, even though this is BitTorrent
		return true;
	}

	/**
	 * Determine if the computer we're uploading to supports Gnutella queuing.
	 * 
	 * @return false
	 */
	public boolean supportsQueueing() {

		// No, it doesn't, this is BitTorrent
		return false;
	}

	/**
	 * Get the current request method.
	 * Returns "GET" even though BitTorrent doesn't use HTTP.
	 * 
	 * @return HTTPRequestMethod.GET
	 */
	public HTTPRequestMethod getMethod() {

		// Return the default "GET" method even though BitTorrent doesn't use it
		return HTTPRequestMethod.GET;
	}

	/**
	 * Returns our current position in the remote computer's download queue.
	 * 
	 * @return 0, BitTorrent doesn't have queues
	 */
	public int getQueuePosition() {

		// Just return 0
		return 0;
	}

	/**
	 * Determine if this upload is in an inactive state.
	 * 
	 * @return false
	 */
	public boolean isInactive() {

		// BitTorrent uploads never stop
		return false;
	}

	/**
	 * Have the SimpleBandwidthTracker this BTUploader keeps update the speed it keeps current.
	 */
	public void measureBandwidth() {

		// Have our SimpleBandwidthTracker do it
		_tracker.measureBandwidth();
	}

	/**
	 * Find out how fast we are uploading data for this torrent right now.
	 * 
	 * @return The speed, in KB/s
	 */
	public float getMeasuredBandwidth() {

		// If we've stopped sharing this torrent, return 0
		if (_torrent.hasStopped()) return 0.f;

		// Have our SimpleBandwidthTracker update it's information, and return its saved value
		measureBandwidth();
		return _tracker.getMeasuredBandwidth();
	}

	/**
	 * Get the total average bandwidth, the number of bytes we've downloaded divided by the time since we started downloading this torrent.
	 * 
	 * @return The total average bandwidth speed, in KB/s
	 */
	public float getAverageBandwidth() {

		// Ask our SimpleBandwidthTracker
		return _tracker.getAverageBandwidth();
	}

	/**
	 * Have our SimpleBandwidthTracker record that we've uploaded some bytes now.
	 * 
	 * @param written The number of bytes we've uploaded
	 */
	void wroteBytes(int written) {

		// Have our SimpleBandwidthTracker count them
		_tracker.count(written);
	}
}
