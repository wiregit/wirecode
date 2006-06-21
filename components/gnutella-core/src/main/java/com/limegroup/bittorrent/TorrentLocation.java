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
 */
public class TorrentLocation extends Endpoint {
	private static final long serialVersionUID = 7953314787152210101L;

	/**
	 * Use this for unknown peer ids
	 */
	private static final byte [] NULL_PEER_STRING = new byte[20];

	private static final byte[] ZERO_BYTES = new byte[] { 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00 };

	private static final int MAX_STRIKES = 1;

	private static final int BUSY_WAIT_TIME = 5 * 60 * 1000;

	/** 
	 * The peer ID of the location.  It may get changed during the 
	 * handshaking process, so this object should not be part of
	 * any collection during handshaking.
	 */
	private final byte [] PEER_ID;

	private final byte[] EXTENSION_BYTES;

	private int _strikes = 0;

	private long _nextRetryTime = 0;

	/*
	 * constructors
	 */
	public TorrentLocation(InetAddress address, int port, byte[] peerId,
			byte[] extensionBytes) {
		super(address.getHostAddress(), port);
		PEER_ID = (peerId == null) ? NULL_PEER_STRING : peerId;
		EXTENSION_BYTES = extensionBytes;
	}

	public TorrentLocation(InetAddress address, int port, byte [] peerId) {
		super(address.getHostAddress(), port);
		PEER_ID = (peerId == null) ? NULL_PEER_STRING : peerId;
		EXTENSION_BYTES = ZERO_BYTES;
	}

	public TorrentLocation(TorrentLocation to) {
		this(to.getInetAddress(), to.getPort(), to.getPeerID(),
				to.EXTENSION_BYTES);
	}

	/**
	 * @return BitTorrent peer ID
	 */
	public byte [] getPeerID() {
		return PEER_ID;
	}
	
	/**
	 * @return the extention bytes
	 */
	public byte [] getExtBytes() {
		return EXTENSION_BYTES;
	}

	/**
	 * call this method if a connection attempt for this location has failed
	 */
	public void strike() {
		_nextRetryTime = System.currentTimeMillis() + BUSY_WAIT_TIME;
		_strikes++;
	}

	/**
	 * @return true if we should wait before retrying
	 */
	public boolean isBusy(long now) {
		return _nextRetryTime > now;
	}
	
	/**
	 * @param now the current time in milliseconds
	 * @return number of milliseconds to wait before next connection attempt
	 */
	public long getWaitTime(long now) {
		return Math.max(0, _nextRetryTime - now);
	}
	
	/**
	 * @return true if this location has failed too many times
	 */
	public boolean isOut() {
		return _strikes > MAX_STRIKES;
	}

	/**
	 * @return a byte representation of this location, the four bytes big endian
	 *         ip address followed by the two byte big endian port
	 */
	public byte[] toBytes() {
		byte[] ret = new byte[6];
		try  {
			System.arraycopy(getHostBytes(), 0, ret, 0, 4);
		} catch (IOException ioe) {
			ErrorService.error(ioe);
		}
		ByteOrder.short2beb((short) getPort(), ret, 4);
		return ret;
	}

	/**
	 * @return true, if this is an Endpoint to a LimeWire node
	 */
	public boolean isLimePeer() {
		return PEER_ID[0] == (byte)'L' &&
		PEER_ID[1] == (byte)'I' &&
		PEER_ID[2] == (byte)'M' &&
		PEER_ID[3] == (byte)'E';
	}

	/**
	 * @return true if the remote supports alt-loc requests
     * TODO: ask gregorio where is this documented if anywhere
	 */
	public boolean supportsAltLocRequests() {
		return (0x02 & EXTENSION_BYTES[7]) == 0x02;
	}

	public boolean equals(Object o) {
		if (!(o instanceof TorrentLocation))
			return false;
		TorrentLocation other = (TorrentLocation) o;
		if (!other.getAddress().equals(getAddress()))
			return false;
		if (!Arrays.equals(other.PEER_ID,this.PEER_ID))
			return false;
		return true;
	}

	public String toString() {
		return getAddress() + ":" + getPort() + ":" + new String(PEER_ID) + ":"
				+ Base32.encode(EXTENSION_BYTES);
	}
}
