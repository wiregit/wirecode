package com.limegroup.bittorrent;

import java.io.IOException;
import java.net.InetAddress;

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
	private static final String NULL_PEER_STRING = "00000000000000000000";

	private static final byte[] ZERO_BYTES = new byte[] { 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00 };

	private static final int MAX_STRIKES = 1;

	private static final int BUSY_WAIT_TIME = 5 * 60 * 1000;

	private final String PEER_ID;

	private final byte[] EXTENSION_BYTES;

	private int _strikes = 0;

	private long _nextRetryTime = 0;

	private String _userAgent = null;

	/*
	 * constructors
	 */
	public TorrentLocation(InetAddress address, int port, String peerId,
			byte[] extensionBytes) {
		super(address.getHostAddress(), port);
		PEER_ID = (peerId == null) ? NULL_PEER_STRING : peerId;
		EXTENSION_BYTES = extensionBytes;
	}

	public TorrentLocation(InetAddress address, int port, String peerId) {
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
	public String getPeerID() {
		return PEER_ID;
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
		return (PEER_ID.startsWith("LIME"));
	}

	/**
	 * @return true if the remote supports alt-loc requests
     * TODO: ask gregorio where is this documented if anywhere
	 */
	public boolean supportsAltLocRequests() {
		return (0x02 & EXTENSION_BYTES[7]) == 0x02;
	}

    /**
     * determines user agent
     * @return user agent String
     */
	public String getUserAgent() {
		if (_userAgent == null) {
			if (PEER_ID.charAt(0) == '-' && PEER_ID.charAt(7) == '-') {
				switch (PEER_ID.charAt(1)) {
				case 'A':
					_userAgent = "Azureus";
					break;
				case 'B':
					if (PEER_ID.charAt(2) == 'X')
						_userAgent = "BitTorrent X";
					else
						_userAgent = "BitBuddy";
					break;
				case 'L':
					_userAgent = "libTorrent";
					break;
				case 'M':
					_userAgent = "MoonlightTorrent";
					break;
				case 'S':
					_userAgent = "SwarmScope";
					break;
				case 'T':
					if (PEER_ID.charAt(2) == 'S')
						_userAgent = "TorrentStorm";
					else
						_userAgent = "TorrentDotNet";
					break;
				case 'X':
					_userAgent = "XanTorrent";
					break;
				default:
					_userAgent = "BitTorrent";
					break;
				}
			} else if (PEER_ID.charAt(4) == '-' && PEER_ID.charAt(5) == '-'
					&& PEER_ID.charAt(6) == '-' && PEER_ID.charAt(7) == '-') {
				switch (PEER_ID.charAt(0)) {
				case 'A':
					_userAgent = "ABC";
					break;
				case 'T':
					_userAgent = "BitTornado";
					break;
				case 'S':
					_userAgent = "Shadow";
					break;
				case 'U':
					_userAgent = "UPnP NAT BT";
					break;
				default:
					_userAgent = "BitTorrent";
				}
			} else if (PEER_ID.charAt(0) == 'M' && PEER_ID.charAt(3) == '-'
					&& PEER_ID.charAt(5) == '-' && PEER_ID.charAt(7) == '-'
					&& PEER_ID.charAt(8) == '-')
				_userAgent = "MainLine";
            else if (isLimePeer())
				_userAgent = "LimeWire";
            else if (PEER_ID.startsWith("ACQX"))
                _userAgent = "LimeWire(Acquisition)";
			else if (isShareazaPeer())
				_userAgent = "Shareaza";
            else if (PEER_ID.startsWith("exbc"))
                _userAgent = "BitComet";
            else if (PEER_ID.startsWith("Mbrst"))
                _userAgent = "Burst!";
            else if (PEER_ID.startsWith("XBT"))
                _userAgent = "XBT";
            else if (PEER_ID.startsWith("turbobt"))
                _userAgent = "TurboBT";
            else if (PEER_ID.startsWith("Plus"))
                _userAgent = "Plus!";
            else if (PEER_ID.startsWith("martini"))
                _userAgent = "MartiniMan";
            else if (PEER_ID.startsWith("Deadman Walking-"))
                _userAgent = "Deadman";
            else if (PEER_ID.startsWith("BTDWV-"))
                _userAgent = "Deadman Walking";
            else if (PEER_ID.startsWith("oernu"))
                _userAgent = "BTugaXP";
            else if (PEER_ID.startsWith("btuga"))
                _userAgent = "BTugaXP";
            else if (PEER_ID.startsWith("PRC.P---"))
                _userAgent = "BitTorrent Plus! II";
            else if (PEER_ID.startsWith("P87.P---"))
                _userAgent = "BitTorrent Plus!";
            else if (PEER_ID.startsWith("S587Plus"))
                _userAgent = "BitTorrent Plus!";
            else if (PEER_ID.startsWith("Azureus"))
                _userAgent = "Azureus";
            else if (PEER_ID.startsWith("btfans"))
                _userAgent = "SimpleBT";
            else if (PEER_ID.startsWith("DansClient"))
                _userAgent = "XanTorrent";
            else if (PEER_ID.startsWith("-G3"))
                _userAgent = "G3 Torrent";
			else
				_userAgent = "BitTorrent";
		}
		return _userAgent;
	}

	/**
	 * @return whether this is a good peer
	 */
	public boolean isGoodPeer() {
		if (!isLimePeer()) {
			return !(getUserAgent().equals("BitTorrent"));
		}
        return false;
	}

	/**
	 * @return true if this is an Endpoint to a Shareaza node
	 */
	public boolean isShareazaPeer() {
		for (int i = 16; i < 20; i++)
			// copied from the Shareaza sources
			if (PEER_ID.charAt(i) != (PEER_ID.charAt(i % 16) ^ PEER_ID
					.charAt(15 - (i % 16))))
				return false;
		return true;
	}

	public boolean equals(Object o) {
		if (!(o instanceof TorrentLocation))
			return false;
		TorrentLocation other = (TorrentLocation) o;
		if (!other.getAddress().equals(getAddress()))
			return false;
		if (!other.PEER_ID.equals(this.PEER_ID))
			return false;
		return true;
	}

	public String toString() {
		return getAddress() + ":" + getPort() + ":" + PEER_ID + ":"
				+ Base32.encode(EXTENSION_BYTES);
	}
}
