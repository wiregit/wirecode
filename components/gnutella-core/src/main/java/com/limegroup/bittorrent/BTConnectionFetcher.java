
// Commented for the Learning branch

package com.limegroup.bittorrent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.handshaking.BTHandshaker;
import com.limegroup.bittorrent.handshaking.OutgoingBTHandshaker;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.util.Sockets;

/**
 * A BTConnectionFetcher keeps a list of the 5 IncomingBTHandshaker and OutgoingBTHandshaker objects doing the handshake with remote computers for this torrent.
 * 
 * A ManagedTorrent has a BTConnectionFetcher which initiates new TCP socket connections to remote computers that are sharing the same torrent.
 * The BTConnectionFetcher keeps a list, named fetchers, of OutgoingBTHandshaker and IncomingBTHandshaker objects.
 * OutgoingBTHandshaker objects perform the BitTorrent handshake with a remote computer once our connection to it goes through.
 * IncomingBTHandshaker objects complete the BitTorrent handshake with a remote computer that connected to us.
 * 
 * The fetch() method loops to fill our fetchers list with up to 5 OutgoingBTHandshaker objects.
 * Each of these objects will wait for our connection to a new remote computer to go through, and then perform the BitTorrent handshake with it.
 * 
 * ManagedTorrent.initializeTorrent() calls the BTConnectionFetcher constructor.
 * It saves the object in as _connectionFetcher.
 */
public class BTConnectionFetcher {

	/** A debugging log we can write lines of text to as the program runs. */
	private static final Log LOG = LogFactory.getLog(BTConnectionFetcher.class);

	/** "BitTorrent protocol", the BitTorrent handshake greeting as a String. */
	private static final String BITTORRENT_PROTOCOL = "BitTorrent protocol";

	/** The ASCII text "BitTorrent protocol" as a 19-byte array. */
	public static byte[] BITTORRENT_PROTOCOL_BYTES;
	static {
		try {
			BITTORRENT_PROTOCOL_BYTES = BITTORRENT_PROTOCOL.getBytes(Constants.ASCII_ENCODING);
		} catch (UnsupportedEncodingException e) {
			ErrorService.error(e);
		}
	}

	/**
	 * The extension bytes that describe which BitTorrent features we support.
	 * 8 bytes that are all 0 for a basic BitTorrent program.
	 * We put a 2 in the last byte to indicate support for. (do)
	 */
	static final byte[] EXTENSION_BYTES = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02 };

	/** 5, We won't try to connect to more than 5 computers at once. */
	private static final int MAX_CONNECTORS = 5;

	/**
	 * The list of OutgoingBTHandshaker and IncomingBTHandshaker objects that this BTConnectionFetcher has trying to connect to remote computers.
	 * At most, there will be 5.
	 * 
	 * fetchers is a HashSet of OutgoingBTHandshaker objects.
	 * A HashSet is a list that blocks duplicates.
	 */
	final Set fetchers = Collections.synchronizedSet(new HashSet());

	/**
	 * The torrent this BTConnectionFetcher object is fetching connections for.
	 * 
	 * This is a link back to the ManagedTorrent object.
	 * managedTorrent._connectionFetcher is this BTConnectionFetcher.
	 * this._torrent is our link back up to the ManagedTorrent.
	 */
	final ManagedTorrent _torrent;

	/**
	 * The handshake we'll send remote computers we have connections with to download and share the torrent.
	 * _handshake is a ByteBuffer with 68 bytes of data in it:
	 * 
	 * S                     A byte that holds the length of the text that follows, 19
	 * BitTorrent protocol   ASCII text that identifies this as the BitTorrent protocol
	 * 00000002              8 bytes that tell which advanced BitTorrent features we support, they're all 0 except for the last one, where the 2 bit is set
	 * HHHHHHHHHHHHHHHHHHHH  The hash of the info section of the .torrent file, tells the remote computer which torrent we're interested in sharing with it
	 * LIMEguidguidguidguid  Our peer ID, our vendor code "LIME" followed by our GUID.
	 */
	final ByteBuffer _handshake;

	/**
	 * True when this object is shut down.
	 */
	private volatile boolean shutdown;

	/**
	 * Make a new BTConnectionFetcher that will try to connect to remote computers downloading and uploading the same torrent we are.
	 * 
	 * Only ManagedTorrent.initializeTorrent() calls this constructor.
	 * It saves the new BTConnectionFetcher object in its _connectionFetcher member variable.
	 * 
	 * @param torrent The torrent we're downloading
	 * @param peerId  Our peer ID like "LIMEguidguidguidguid", with the 16-byte GUID after the ASCII vendor code "LIME"
	 */
	BTConnectionFetcher(ManagedTorrent torrent, byte[] peerId) {

		// Save the torrent we're making this BTConnectionFetcher for
		_torrent = torrent;

		// Compose the handshake we'll send remote computers connected to us to share the torrent
		ByteBuffer handshake = ByteBuffer.allocate(68);    // Our handshake will be 68 bytes of data
		handshake.put((byte)BITTORRENT_PROTOCOL.length()); // S                    The BitTorrent handshake starts with the number 19 in a single byte
		handshake.put(BITTORRENT_PROTOCOL_BYTES);          // BitTorrent protocol  After that are the 19 bytes of ASCII text "BitTorrent protocol"
		handshake.put(EXTENSION_BYTES);                    // EEEEEEEE             The 8 bytes that describe which advanced BitTorrent features we support
		handshake.put(_torrent.getInfoHash());             // HHHHHHHHHHHHHHHHHHHH The 20-byte SHA1 hash of the info portion of the .torrent file
		handshake.put(peerId);                             // PPPPPPPPPPPPPPPPPPPP Our 20-byte peer ID, that has our vendor code LIME and our 16-byte GUID
		handshake.flip();                                  // Move position back to the start, getting the buffer ready for reading
		_handshake = handshake.asReadOnlyBuffer();         // Save our handshake data in this new BTConnectionFetcher object
	}

	/**
	 * Loop to fill our fetchers list with up to 5 IncomingBTHandshake and OutgoingBTHandshake objects.
	 * If we have less than 5, make new OutgoingBTHandshake objects that will connect to remote computers and do the BitTorrent handshake with them.
	 */
	public synchronized void fetch() {

		// Don't do anything if we're shut down
		if (shutdown) return;

		// Check the ManagedTorrent's shouldStop() method, and call stop() if necessary (do)
		if (_torrent.shouldStop()) {
			_torrent.stop();
			return;
		}

		// Loop to try fill our fetchers list with up to 5 BTConnectionFetcher objects, each of which will try to connect to a remote computer that has our torrent
		while (
			!_torrent.hasStopped()           && // No one has set _stopped to true in the ManagedTorrent object yet, and
			fetchers.size() < MAX_CONNECTORS && // We're trying to open connections and do the handshake with fewer than 5 new remote computers right now, and
			_torrent.needsMoreConnections()  && // Our torrent needs more connections, and
			_torrent.hasNonBusyLocations()) {   // Not all of our torrent's locations are busy (do)

			// Connect to a new remote computer that's sharing the torrent, and perform the handshake with it.
			fetchConnection();
			if (LOG.isDebugEnabled()) LOG.debug("started connection fetcher: " + fetchers.size());
		}
	}

	/**
	 * Connect to a new remote computer that's sharing the torrent, and perform the handshake with it.
	 * 
	 * Asks _torrent for the IP address and port number of a remote computer that's sharing it too.
	 * Makes a new OutgoingBTHandshaker object that will perform the BitTorrent handshake as soon as we connect to the remote computer.
	 * Calls Sockets.connect() to open a new TCP socket connection to the remote computer.
	 * When the "NIODispatch" thread makes the connection, it will call the OutgoingBTHandshaker's handleConnect() method.
	 */
	private void fetchConnection() {

		// Get the address of a remote compuer that has the same torrent as we do
		TorrentLocation ep = _torrent.getTorrentLocation();
		if (ep == null) {
			if (LOG.isDebugEnabled()) LOG.debug("no hosts to connect to");
			return;
		}

		// Make an OutgoingBTHandshaker object which will perform the BitTorrent handshake as soon as our connection attempt to ep goes through
		OutgoingBTHandshaker connector = new OutgoingBTHandshaker(ep, _torrent);
		fetchers.add(connector); // Add it to our list

		try {

			// Open a new TCP socket connection to the remote computer
			Sockets.connect(
				ep.getAddress(),   // The IP address and port number of the remote computer
				ep.getPort(),
				Constants.TIMEOUT, // Give up after 8 seconds
				connector);        // The "NIODispatch" thread will call connector.handleConnect() when the connection goes through

		} catch (IOException impossible) { ErrorService.error(impossible); }
	}

	/**
	 * Make this BTConnectionFetcher object stop all its network activity.
	 * 
	 * Loops through our fetchers list of OutgoingBTHandshaker objects, calling shutdown() on each one.
	 */
	synchronized void shutdown() {

		// Only do this once, and record that it has been done
		if (shutdown) return;
		shutdown = true;

		// Only let one thread access the fetchers list at a time
		synchronized (fetchers) {

			// Loop for each OutgoingBTHandshaker in our fetchers list
			List conns = new ArrayList(fetchers); // Make a copy of the fetchers list because connector.shutdown() will remove it from fetchers
			for (Iterator iter = conns.iterator(); iter.hasNext(); ) {
				Shutdownable connector = (Shutdownable)iter.next();

				// Have it stop all its network activity
				connector.shutdown();
			}
		}
	}

	/**
	 * Get the handshake we'll send remote computers we have connections with to download and share the torrent.
	 * Returns a ByteBuffer with 68 bytes of data in it:
	 * 
	 * S                     A byte that holds the length of the text that follows, 19.
	 * BitTorrent protocol   ASCII text that identifies this as the BitTorrent protocol.
	 * 00000002              8 bytes that tell which advanced BitTorrent features we support, they're all 0 except for the last one, where the 2 bit is set.
	 * HHHHHHHHHHHHHHHHHHHH  The hash of the info section of the .torrent file, tells the remote computer which torrent we're interested in sharing with it.
	 * LIMEguidguidguidguid  Our peer ID, our vendor code "LIME" followed by our GUID.
	 * 
	 * @return A ByteBuffer with the 68 bytes of our handshake data
	 */
	public ByteBuffer getOutgoingHandshake() {

		// Return a copy of the ByteBuffer so the caller can do whatever they want with it
		return _handshake.duplicate();
	}

	/**
	 * Add the given IncomingBTHandshaker to this BTConnectionFetcher's list of them.
	 * Only IncomingBTHandshaker.verifyIncoming() calls this method.
	 * If a remote computer contacts us, this is how its IncomingBTHandshaker objects gets in our list.
	 * 
	 * @param shaker An IncomingBTHandshaker object from a remote computer that contacted us
	 */
	public void handshakerStarted(BTHandshaker shaker) {

		// Add the given IncomingBTHandshaker to our fetchers list
		fetchers.add(shaker);
	}

	/**
	 * Remove the given BTHandshaker from our fetchers list.
	 * Also calls fetch() to get more if we still can and don't have enough.
	 * 
	 * @param shaker The IncomingBTHandshaker or OutgoingBTHandshaker object to remove
	 */
	public void handshakerDone(BTHandshaker shaker) {

		// Remove the given object from the featchers list, and call fetch() to get more connections if necessary
		if (fetchers.remove(shaker)) fetch();
	}
}
