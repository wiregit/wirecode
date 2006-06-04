
// Commented for the Learning branch

package com.limegroup.bittorrent;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.gnutella.util.NetworkUtils;
import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

/**
 * A TrackerResponse represents the bencoded response a BitTorrent tracker sent us in response to our message to it.
 * 
 * The response is a bencoded dictionary.
 * The TrackerResponse constructor reads the following keys:
 * 
 * failure reason  A string that describes why the tracker is unable to help us now.
 * peers           The value is a string like "IIIIPPIIIIPPIIIIPP" that has the IP addresses and port numbers of peers.
 *                 Or, the value is a list of dictionaries like this:
 *   ip            The IP address as a string, like "1.2.3.4".
 *   port          The port number.
 *   peer id       The 20-byte peer ID.
 * interval        We should contact the tracker every time this number of seconds expire.
 * min_interval    If "interval" is missing, we'll use "min_interval" instead.
 * num peers       The number of leachers the tracker knows about for this torrent, addresses of peers that don't have the whole file yet.
 * incomplete      If "num peers" is missing, read "incomplete" instead.
 * num done        The number of seeders the tracker knows about for this torrent, addresses of peers that have the whole file.
 * complete        If "num done" is missing, read "complete" instead.
 * 
 * A TrackerResponse object has the following public member variables you can read:
 * 
 * FAILURE_REASON  The reason the tracker can't help us.
 * PEERS           An ArrayList of TorrentLocation objects with the IP addresses and port numbers the tracker gave us.
 * INTERVAL        We should contact the tracker ever INTERVAL seconds.
 * NUM_PEERS       The number of leachers the tracker knows about for this torrent, peers that don't have the whole file yet.
 * DONE_PEERS      The number of seeders the tracker knows about for this torrent, peers with the whole file.
 */
public class TrackerResponse {

	/** A debugging log we can write lines of text to as the program runs. */
	private static final Log LOG = LogFactory.getLog(TrackerResponse.class);

	/**
	 * The list of peers the tracker gave us.
	 * These are the IP addresses and port numbers of remote computers on the Internet running BitTorrent programs that are sharing the torrent we asked the tracker about.
	 * 
	 * The tracker's response is a bencoded dictionary that has the key "peers".
	 * The value is probably a string like "IIIIPPIIIIPPIIIIPP", with 6-byte chunks that have an IP address and port number.
	 * 
	 * Or, the value can be a bencoded list.
	 * Each element in the list is a dictionary like this:
	 * 
	 * ip       The IP address, like "1.2.3.4"
	 * port     The port number
	 * peer id  The 20-byte peer ID, like "LIMEguidguidguidguid"
	 * 
	 * A TorrentLocation object keeps an IP address, port number, and 20-byte peer ID together.
	 * If "peers" was a string, we don't know the peer IDs.
	 * Each TorrentLocation's peer ID member will point to a byte array of 20 zero bytes.
	 */
	public final List PEERS;

	/**
	 * We should contact the tracker every INTERVAL seconds.
	 * Read from the "interval" or, if that's not found "min_interval" keys in the tracker's response.
	 * Between 5 minutes and 2 hours.
	 */
	public final int INTERVAL;

	/**
	 * The tracker has this many leachers for the torrent.
	 * The number of addresses the tracker has of peers that don't have the whole torrent yet.
	 * Read from the "num peers", or, if that's not found, "incomplete" keys in the tracker's response.
	 */
	public final int NUM_PEERS;

	/**
	 * The tracker has this many seeders for the torrent.
	 * The number of addresses the tracker has of peers that have the whole torrent.
	 * Read from the "num done", or, if that's not found, "complete" keys in the tracker's response.
	 */
	public final int DONE_PEERS;

	/**
	 * The reason the tracker can't help us.
	 * The String value of the key "failure reason".
	 */
	public final String FAILURE_REASON;

	/**
	 * Make a new TrackerResponse object to represent the bencoded response a BitTorrent tracker gave us.
	 * Only connectHTTP() calls this constructor.
	 * 
	 * Here are the parts of the bencoded data it looks for:
	 * 
	 * failure reason  A string that describes why the tracker is unable to help us now.
	 * peers           The value is a string like "IIIIPPIIIIPPIIIIPP" that has the IP addresses and port numbers of peers.
	 *                 Or, the value is a list of dictionaries like this:
	 *   ip            The IP address as a string, like "1.2.3.4".
	 *   port          The port number.
	 *   peer id       The 20-byte peer ID.
	 * interval        We should contact the tracker every time this number of seconds expire.
	 * min_interval    If "interval" is missing, we'll use "min_interval" instead.
	 * num peers       The number of leachers the tracker knows about for this torrent, addresses of peers that don't have the whole file yet.
	 * incomplete      If "num peers" is missing, read "incomplete" instead.
	 * num done        The number of seeders the tracker knows about for this torrent, addresses of peers that have the whole file.
	 * complete        If "num done" is missing, read "complete" instead.
	 * 
	 * @param t_response A Java HashMap made from parsing the bencoded data a BitTorrent tracker sent us
	 */
	public TrackerResponse(Object t_response) throws ValueException {

		// Make sure the caller gave us a Map
		if (!(t_response instanceof Map)) throw new ValueException("bad tracker response");
		Map response = (Map)t_response;

		// If the tracker sent the key "failure reason", get the string value
		if (response.containsKey("failure reason")) FAILURE_REASON = new String((byte[])response.get("failure reason"));
		else FAILURE_REASON = null;

		// The IP addresses and port numbers of computers with BitTorrent programs sharing the same torrent are under "peers"
		if (response.containsKey("peers")) {

			// The value of "peers" can be a bencoded List, or a byte array of 6-byte IP addresses and port numbers
			Object t_peers = response.get("peers");
			if      (t_peers instanceof List)   PEERS = parsePeers((List)t_peers);   // Parse the bencoded list
			else if (t_peers instanceof byte[]) PEERS = parsePeers((byte[])t_peers); // Parse the 6-byte chunks
			else throw new ValueException("bad tracker response - bad peers " + t_peers);

        // "peers" not found
		} else {

			// Point PEERS at an empty list
			PEERS = Collections.EMPTY_LIST;
		}

		// Read "interval" and "min_interval", the times in seconds we should put between requests to the tracker
		Object t_interval = response.get("interval");                            // The "interval" key is required
		Object t_minInterval = response.get("min_interval");                     // The tracker may have also sent "min_interval", which is the same thing
		int interval;                                                            // Make a variable for the value we'll use
		if (t_interval instanceof Long) {                                        // The response has "interval"
			interval = (int)((Long)t_interval).longValue();                      // Use it
		} else if (t_minInterval instanceof Long) {                              // The response doesn't have "interval", but does have "min_interval" instead
			interval = (int) ((Long)t_interval).longValue();                     // Use it instead
		} else {                                                                 // The response doesn't have either
			interval = BittorrentSettings.TRACKER_MIN_REASK_INTERVAL.getValue(); // Use our default of 5 minutes
		}

		// Move the interval into our required range of 5 minutes to 2 hours
		if      (interval < BittorrentSettings.TRACKER_MIN_REASK_INTERVAL.getValue()) INTERVAL = BittorrentSettings.TRACKER_MIN_REASK_INTERVAL.getValue(); // 5 minutes
		else if (interval > BittorrentSettings.TRACKER_MAX_REASK_INTERVAL.getValue()) INTERVAL = BittorrentSettings.TRACKER_MAX_REASK_INTERVAL.getValue(); // 2 hours
		else                                                                          INTERVAL = interval;

		// Read "num peers" or "incomplete" to get the number of peers that don't have the entire file yet, the leachers
		Object t_numPeers = response.get("num peers");
		if (t_numPeers instanceof Long) {
			NUM_PEERS = (int)((Long)t_numPeers).longValue();
		} else {                                                 // "num peers" not found
			t_numPeers = response.get("incomplete");             // Read "incomplete" instead
			if (t_numPeers instanceof Long) {
				NUM_PEERS = (int)((Long)t_numPeers).longValue();
			} else {
				NUM_PEERS = 0;                                   // "incomplete" not found, set to 0
			}
		}

		// Read "num done" or "complete", the number of peers with the entire file, the number of seeders
		Object t_donePeers = response.get("num done");
		if (t_donePeers instanceof Long) {
			DONE_PEERS = (int)((Long)t_donePeers).longValue();
		} else {                                                   // "num done" not found
			t_donePeers = response.get("complete");                // Read "complete" instead
			if (t_donePeers instanceof Long) {
				DONE_PEERS = (int)((Long)t_donePeers).longValue();
			} else {
				DONE_PEERS = 0;                                    // "complete" not found, set to 0
			}
		}
	}

	/**
	 * Make a new TrackerResponse object with information a UDP tracker sent us in a UDP packet.
	 * UDPTrackerRequest methods use this constructor.
	 * 
	 * @param peers         An ArrayList of TorrentLocation objects, the list of peers the tracker gave us
	 * @param interval      The interval in seconds the tracker told us to contact it on
	 * @param numPeers      The number of leachers, peers without the whole file yet, the tracker knows about for this torrent
	 * @param donePeers     The number of seeders, peers with the whole file, the tracker knows about for this torrent
	 * @param failureReason The reason the tracker can't help us
	 */
	TrackerResponse(List peers, int interval, int numPeers, int donePeers, String failureReason) {

		// Save the given information, adjusting interval to be between 5 minutes and 2 hours
		PEERS = peers;
		INTERVAL = Math.min(Math.max(interval, BittorrentSettings.TRACKER_MIN_REASK_INTERVAL.getValue()), BittorrentSettings.TRACKER_MAX_REASK_INTERVAL.getValue());
		NUM_PEERS = Math.max(0, numPeers);
		DONE_PEERS = Math.max(0, donePeers);
		FAILURE_REASON = failureReason;
	}

	/**
	 * Parse a bencoded "peers" list into an ArrayList of TorrentLocation objects.
	 * 
	 * In the tracker's response, "peers" can either be a byte array or a bencoded list.
	 * This method parses the bencoded list.
	 * Each element in the list is a bencoded dictionary, like this:
	 * 
	 * ip       The IP address, like "1.2.3.4"
	 * port     The port number
	 * peer id  The 20-byte peer ID, like "LIMEguidguidguidguid"
	 * 
	 * @param peers An ArrayList of HashMap objects made from the bencoded "peers" list in the tracker's response
	 * @return      An ArrayList of TorrentLocation objects, each of which has a IP address, port number, and peer ID
	 */
	private static List parsePeers(List peers) throws ValueException {

		// Make a new empty ArrayList we'll fill with TorrentLocation objects and return
		List ret = new ArrayList();

		// Loop for each item in the "peers" list the tracker sent us
		for (Iterator iter = peers.iterator(); iter.hasNext(); ) {
			Object t_peer = iter.next();
			if (!(t_peer instanceof Map)) throw new ValueException("bad tracker response - bad peer " + t_peer);

			// Make a TorrentLocation from this address, and add it to the ret list
			ret.add(parsePeer((Map)t_peer));
		}

		// Return a read-only view of the ArrayList we filled with TorrentLocation objects
		return Collections.unmodifiableList(ret);
	}

	/**
	 * Turn a byte array of IP addresses and port numbers into an ArrayList of TorrentLocation objects.
	 * A TorrentLocation has room for the 20-byte peer ID.
	 * The peer IDs aren't in the byte array, so the TorentLocation objects will point to a byte array of 20 0s.
	 * 
	 * @param bytes A byte array like "IIIIPPIIIIPPIIIIPP" with 6-byte chunks with IP addresses in 4 bytes and port numbers in 2 bytes
	 * @return      An ArrayList of TorrentLocation objects, each of which holds one IP address and port number
	 */
	static List parsePeers(byte[] bytes) throws ValueException {

		// Make a new empty ArrayList we'll fill with TorrentLocation objects and return
		ArrayList ret = new ArrayList();

		// Move down the given byte array, pointing i at each 6-byte chunk
		for (int i = 0; i < bytes.length - 5; i += 6) { // Let i reach bytes.length - 6 to reach the last chunk, but not bytes.length

			// Grab the IP address and port number
			byte[] address = new byte[4];
			System.arraycopy(bytes, i, address, 0, 4);
			int port = ByteOrder.beb2int(bytes, i + 4, 2);

			try {

				// Turn the 4 bytes into an InetAddress object
				InetAddress addr = InetAddress.getByAddress(address);

				// Wrap the IP address and port number into a TorrentLocation object, and add it to the list we'll return
				ret.add(parsePeer(addr, port, null)); // null because we don't know this computer's peer ID

			// getByAddress() couldn't turn those 4 bytes into an InetAddress object
			} catch (UnknownHostException uhe) {
			    throw new ValueException("bad tracker response - bad peer ip " + new String(address));
			}
		}

		// Return a read-only view of the ArrayList we filled with TorrentLocation objects
		return Collections.unmodifiableList(ret);
	}

	/**
	 * Turn a bencoded map from the "peers" list into a TorrentLocation object that represents that BitTorrent peer's address.
	 * Looks for the following elements:
	 * 
	 * ip       The IP address, like "1.2.3.4"
	 * port     The port number
	 * peer id  The 20-byte peer ID, like "LIMEguidguidguidguid"
	 * 
	 * @param peer A HashMap we made from an element in the bencoded "peers" list from the tracker's response
	 */
	private static TorrentLocation parsePeer(Map peer) throws ValueException {

		// Parse "ip", which has a value like "1.2.3.4", into an InetAddress structure
		Object t_ip = peer.get("ip");
		if (!(t_ip instanceof byte[])) throw new ValueException("bad tracker response - bad peer ip " + t_ip);
		InetAddress addr;
		try {
			String ipS = new String((byte[])t_ip, Constants.ASCII_ENCODING);
			addr = InetAddress.getByName(ipS);
		} catch (UnknownHostException uhe) {
			throw new ValueException("bad tracker response - bad peer ip " + t_ip);
		} catch (UnsupportedEncodingException impossible) {
			ErrorService.error(impossible);
			return null;
		}

		// Parse "port", the port number
		Object t_port = peer.get("port");
		if (!(t_port instanceof Long)) throw new ValueException("bad tracker response - bad peer port " + t_port);
		int port = (int)((Long)t_port).longValue();

		// Parse "peer id", the BitTorrent program's 20-byte unique ID
		Object t_peerId = peer.get("peer id");
		if (!(t_peerId instanceof byte[])) throw new ValueException("bad tracker response - bad peer id ");
		byte[] peerId = (byte[])t_peerId;
		if (peerId.length != 20) throw new ValueException("bad tracker response - bad peer id ");

		// Make a new TorrentLocation object from the IP address, port number, and peer ID, and return it
		return parsePeer(addr, port, peerId);
	}

	/**
	 * Wrap a given IP address, port number, and BitTorrent peer ID into a new TorrentLocation object.
	 * 
	 * @param addr   The IP address of the remote BitTorrent computer as a Java InetAddress object
	 * @param port   The remote computer's port number
	 * @param peerId The 20-byte BitTorrent peer ID which uniquely identifies the BitTorrent program
	 * @return       A new TorrentLocation object with all that information inside it
	 */
	private static TorrentLocation parsePeer(InetAddress addr, int port, byte[] peerId) throws ValueException {

		// Make sure the IP address and port number look correct
		if (!NetworkUtils.isValidAddress(addr)) throw new ValueException("bad tracker response - bad peer ip " + addr);
		if (!NetworkUtils.isValidPort(port)) throw new ValueException("bad tracker response - bad peer port " + port);

		// Wrap the given information into a new TorrentLocation object, and return it
		TorrentLocation to = new TorrentLocation(addr, port, peerId);
		if (LOG.isDebugEnabled()) LOG.debug("got peer " + to);
		return to;
	}
}
