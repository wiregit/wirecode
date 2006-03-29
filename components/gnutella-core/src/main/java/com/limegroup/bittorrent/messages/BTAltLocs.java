package com.limegroup.bittorrent.messages;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 *  A message containing alternate locations for torrent
 */
public class BTAltLocs extends BTMessage {
	/**
	 * set of TorrentLocation
	 */
	private final Set _locations;
	
	/**
	 * caching message payload
	 */
	private ByteBuffer _payload = null;
	

	/**
	 * Create BTAltLocs from network
	 * 
	 * @param payload
	 *            ByteBuffer with data from network
	 * @return new instance of BTAltLocs
	 * @throws BadBTMessageException
	 *             if data from network was bad.
	 */
	public static  BTAltLocs readMessage(ByteBuffer payload) throws BadBTMessageException {
		if (payload.remaining() % 6 != 0) 
			throw new BadBTMessageException("unexpected message length for ALT_LOCS message: " + payload.remaining());
		
		Set locations = new HashSet();
		
		payload.order(ByteOrder.BIG_ENDIAN);
		
		while (payload.remaining() >= 6) {
			try {
				// big endian ip
				byte[] ip = new byte[4];
				payload.get(ip);
				// port is big endian
				int port = 0 | payload.getShort();
				if (!(NetworkUtils.isValidAddress(ip) && NetworkUtils
						.isValidPort(port)))
					continue;
				InetAddress addr = InetAddress.getByAddress(ip);
				TorrentLocation to = new TorrentLocation(addr, port, null);
				locations.add(to);
			} catch (UnknownHostException uhe) {
				// continue...
			}
		}
		return new BTAltLocs(locations);
	}

	private BTAltLocs(Set locations) {
		super(ALT_LOCS);
		_locations = locations;
	}

	/**
	 * factory method, creates a new altlocs message
	 * 
	 * @return new instance of BTAltLocs
	 */
	public static BTAltLocs createMessage(Set locations) {
		return new BTAltLocs(locations);
	}

	public ByteBuffer getPayload() {
		if (_payload == null) {
			_payload = ByteBuffer.allocate(_locations.size() * 6);
			_payload.order(ByteOrder.BIG_ENDIAN);
			for (Iterator iter = _locations.iterator(); iter.hasNext();) {
				TorrentLocation ep = (TorrentLocation) iter.next();
				_payload.put(ep.toBytes());
			}
			_payload = _payload.asReadOnlyBuffer();
		}
		_payload.clear();
		return _payload;
	}

	/**
	 * return locations
	 */
	public Set getLocations() {
		return _locations;
	}
	
	public String toString() {
		return "BTAltLocs: " + _locations.toString();
	}
}
