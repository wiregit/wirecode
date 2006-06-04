
// Commented for the Learning branch

package com.limegroup.bittorrent;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.ErrorService;

/**
 * A TorrentLocation object represents a remote computer on the Internet running BitTorrent software.
 * It's downloading files with BitTorrent, so we can use it as a location to get parts of files from.
 * 
 * The TorrentLocation class extends Endpoint to have an IP address and port number.
 * Call methods like getAddress() and getPort() to get the IP address and port number of the remote BitTorrent computer.
 * 
 * It adds the BitTorrent peer ID, and the extension bytes.
 * A peer ID is 20-bytes like "LIMEguidguidguidguid".
 * It starts with the vendor code written in 4 ASCII bytes.
 * After that is the data of the 16-byte GUID.
 * There are 8 extension bytes, like EEEEEEEE, with bits set for the advanced BitTorrent features that the remote computer understands and supports.
 * LimeWire supports alternate locations, set in 0x02 in the last byte.
 * 
 * TorrentLocation also has code that keeps track of how many times we've tried and failed to connect to this address.
 * After the second failure, isOut() returns true, and we shouldn't try a 3rd time.
 * isBusy() returns true if we've tried and failed in the last 5 minutes, and should wait longer before trying again.
 */
public class TorrentLocation extends Endpoint {

	/** A long unique number that identifies this version of a TorrentLocation object serialized to disk. */
	private static final long serialVersionUID = 7953314787152210101L;

	/** A 20-byte array filled with 0s, use when we don't know this remote computer's BitTorrent peer ID. */
	private static final byte [] NULL_PEER_STRING = new byte[20];

	/** An 8-byte array filled with 0x, use when we don't know this remote computer's BitTorrent extension bytes. */
	private static final byte[] ZERO_BYTES = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

	/** 1, after we're unable to contact this address 2 times, we'll give up on it. */
	private static final int MAX_STRIKES = 1;

	/** 300,000 milliseconds, 5 minutes, after we are unable to contact this peer, we'll wait 5 minutes before trying again. */
	private static final int BUSY_WAIT_TIME = 5 * 60 * 1000;

	/** The 20-byte peer ID that identifies the remote computer, like "LIMEguidguidguidguid". */
	private final byte[] PEER_ID;

	/** The remote computer's 8 extension bytes, that tell what advanced BitTorrent extensions it understands. */
	private final byte[] EXTENSION_BYTES;

	/** The number of times we've tried to connect to this address, and failed. */
	private int _strikes = 0;

	/** The time when we'll next let ourselves try to connect to this address again. */
	private long _nextRetryTime = 0;

	/** Not used. */
	private String _userAgent = null;

	/**
	 * Make a new TorrentLocation object to hold the IP address, peer ID, and extension bytes of a remote computer running BitTorrent software.
	 * 
	 * @param address        The IP address of the remote computer.
	 * @param port           The port number of the remote computer.
	 * @param peerId         The 20-byte peer ID of the remote computer, like "LIMEguidguidguidguid".
	 *                       null if we don't know it, this new TorrentLocation will have a peer ID of 20 zero bytes.
	 * @param extensionBytes The remote computer's 8 extension bytes, which tell which advanced BitTorrent features it supports.
	 */
	public TorrentLocation(InetAddress address, int port, byte[] peerId, byte[] extensionBytes) {

		// Call the Endpoint constructor to save the IP address and port number
		super(address.getHostAddress(), port);

		// Set this remote computer's peer ID, the 20 bytes that identify it uniquely among BitTorrent programs
		PEER_ID =
			(peerId == null) ? // If the caller passed null instead of a peer ID
			NULL_PEER_STRING : // Use 20 bytes of 0s instead, otherwise
			peerId;            // Use the given peer ID

		// Save the given extension bytes, 8 bytes that tell which advanced BitTorrent features the computer supports
		EXTENSION_BYTES = extensionBytes;
	}

	/**
	 * Make a new TorrentLocation object to hold the IP address, peer ID, and extension bytes of a remote computer running BitTorrent software.
	 * 
	 * @param address The IP address of the remote computer
	 * @param port    The port number of the remote computer
	 * @param peerId  The 20-byte peer ID of the remote computer, like "LIMEguidguidguidguid".
	 *                null if we don't know it, this new TorrentLocation will have a peer ID of 20 zero bytes.
	 */
	public TorrentLocation(InetAddress address, int port, byte[] peerId) {

		// Call the Endpoint constructor to save the IP address and port number
		super(address.getHostAddress(), port);

		// Set this remote computer's peer ID, the 20 bytes that identify it uniquely among BitTorrent programs
		PEER_ID =
			(peerId == null) ? // If the caller passed null instead of a peer ID
			NULL_PEER_STRING : // Use 20 bytes of 0s instead, otherwise
			peerId;            // Use the given peer ID

		// Save the given extension bytes, 8 bytes that tell which advanced BitTorrent features the computer supports
		EXTENSION_BYTES = ZERO_BYTES;
	}

	/**
	 * Make a copy of a given TorrentLocation object.
	 * 
	 * @param to The TorrentLocation object to copy
	 */
	public TorrentLocation(TorrentLocation to) {

		// Call another constructor to make this new object, giving it all the information of the given one
		this(to.getInetAddress(), to.getPort(), to.getPeerID(), to.EXTENSION_BYTES);
	}

	/**
	 * Get this remote computer's BitTorrent peer ID.
	 * 
	 * The peer ID is 20 bytes like "LIMEguidguidguidguid".
	 * The first 4 bytes are ASCII text that tell the vendor code.
	 * The 16 bytes after that are a GUID that makes sure the peer ID is unique.
	 * 
	 * @return A 20-byte array with this computer's BitTorrent peer ID.
	 *         If we don't know the peer ID, returns 20 bytes of 0s.
	 */
	public byte[] getPeerID() {

		// Return the peer ID we saved
		return PEER_ID;
	}

	/**
	 * Call strike() if you've tried to connect to this address, and failed.
	 * Counts the failure, and sets the time we'll next try to connect to 5 minutes from now.
	 */
	public void strike() {

		// Set the next retry time to 5 minutes from now
		_nextRetryTime = System.currentTimeMillis() + BUSY_WAIT_TIME;

		// Count that we've tried and failed to connect to this remote computer another time
		_strikes++;
	}

	/**
	 * Find out if we can try to connect to this address, or if we should wait longer before trying.
	 * 
	 * If we've tried and failed to connect in the last 5 minutes, returns true.
	 * If we've never tried, or never failed, or failed more than 5 minutes ago, returns false.
	 * 
	 * @param now The time now, the number of milliseconds since 1970.
	 * @return    False if you can try connecting to this address now.
	 *            True to wait longer before trying.
	 */
	public boolean isBusy(long now) {

		// Return true if now hasn't reached our next retry time yet
		return _nextRetryTime > now;
	}

	/**
	 * Find out how much longer we have to wait before we'll let ourselves try connecting to this address again.
	 * 
	 * @param now The time now, the number of milliseconds since 1970.
	 * @return    The number of milliseconds we have to wait before we can try connecting to this address again.
	 *            0 if we can try now.
	 */
	public long getWaitTime(long now) {

		// Compute how much longer we have to wait
		return Math.max(0, _nextRetryTime - now); // Return 0 instead of negative
	}

	/**
	 * Find out if we've failed connecting to this address enough times that we should give up on it entirely.
	 * 
	 * A TorentLocation can have one failure, and we'll try again 5 minutes later.
	 * After the second failure, isOut() will return true.
	 * 
	 * @return False if this TorrentLocation has 0 or 1 failures, and we can try again.
	 *         True if this TorrentLocation has 2 or more failures, and we shouldn't try it anymore.
	 */
	public boolean isOut() {

		// Return true if we've failed 2 times
		return _strikes > MAX_STRIKES;
	}

	/**
	 * Compose a 6-byte array like "IIIIPP", with the IP address in the first 4 bytes and the port in the last 2.
	 * The IP address is in network byte order, and the port number is in big endian.
	 * 
	 * @return The byte array
	 */
	public byte[] toBytes() {

		// Make a new 6-byte array to fill and return
		byte[] ret = new byte[6];

		// Copy the IP address into the first 4 bytes
		try {
			System.arraycopy(getHostBytes(), 0, ret, 0, 4);
		} catch (IOException ioe) { ErrorService.error(ioe); }

		// At 4 bytes, write the 2-byte port number in big endian order
		ByteOrder.short2beb((short)getPort(), ret, 4);

		// Return the byte array we made
		return ret;
	}

	/**
	 * Determine if this address is to a remote computer running LimeWire BitTorrent software.
	 * Looks for "LIME" in the first 4 bytes of the 20-byte BitTorrent peer ID.
	 * 
	 * @return True if the peer ID starts "LIME", meaning the remote computer is running LimeWire.
	 *         False if it's running something else.
	 */
	public boolean isLimePeer() {

		// Return true if the first 4 letters in the peer ID are "LIME"
		return
			PEER_ID[0] == (byte)'L' &&
			PEER_ID[1] == (byte)'I' &&
			PEER_ID[2] == (byte)'M' &&
			PEER_ID[3] == (byte)'E';
	}

	/**
	 * Determine if this remote computer supports alternate location requests.
	 * Looks in the extension bytes for the 2 bit set in the last byte.
	 * 
	 * TODO: Ask Gregorio where is this documented, if anywhere
	 * 
	 * @return True if this remote computer's extension bytes indicate support for alt-loc requests.
	 *         False if they don't.
	 */
	public boolean supportsAltLocRequests() {

		// Look at the last extension byte, and see if the 2 bit is set
		return (0x02 & EXTENSION_BYTES[7]) == 0x02;
	}

	/**
	 * Compare this TorrentLocation object to another.
	 * 
	 * Compares the IP addresses and peer IDs.
	 * Doesn't look at the port numbers or extension bytes.
	 * 
	 * @return True if they are the same, false if they are different
	 */
	public boolean equals(Object o) {

		// Make sure the given object is a TorrentLocation
		if (!(o instanceof TorrentLocation)) return false;
		TorrentLocation other = (TorrentLocation)o;

		// Compare their IP addresses
		if (!other.getAddress().equals(getAddress())) return false;

		// Compare their peer IDs
		if (!Arrays.equals(other.PEER_ID, this.PEER_ID)) return false;

		// If the IP address and peer IDs are the same, report the objects are the same
		return true;
	}

	/**
	 * Express this TorrentLocation as text.
	 * Composes a String like:
	 * 
	 * 1.2.3.4:5:LIMEguidguidguidguid:base32extensionbytes
	 * 
	 * The parts are separated by ":"
	 * First are the IP address and port number.
	 * After that is the 20-byte peer ID, converted into a String.
	 * The vendor code at the start will be readable, while the guid may not be.
	 * Last are the extension bytes, written in base 32 encoding.
	 * 
	 * @return A String
	 */
	public String toString() {

		// Compose and return the String
		return getAddress() + ":" + getPort() + ":" + new String(PEER_ID) + ":" + Base32.encode(EXTENSION_BYTES);
	}
}
