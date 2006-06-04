
// Commented for the Learning branch

package com.limegroup.bittorrent.handshaking;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.io.NIOSocket;

/**
 * Make an IncomingBTHandshaker object to perform the BitTorrent handshake with a remote computer that connected to us.
 * 
 * A BitTorrent handshake looks like this:
 * 
 * S
 * BitTorrent protocol
 * EEEEEEEE
 * HHHHHHHHHHHHHHHHHHHH
 * PPPPPPPPPPPPPPPPPPPP
 * 
 * S is 19, the length of the text that comes next.
 * "BitTorrent protocol" is 19 bytes of ASCII text.
 * E is the remote computer's 8 extension bytes.
 * H is the 20-byte SHA1 hash of the info section of the .torrent file.
 * P is the remote computer's peer ID.
 * 
 * The acceptor already read S and "BitTorrent " from the channel, so verifyIncoming() just checks for "protocol".
 * The remote computer wants the torrent with the hash H, verifyIncoming() makes sure we have it.
 */
public class IncomingBTHandshaker extends BTHandshaker {

	/** A debugging log we can write lines of text to as the program runs. */
	private static final Log LOG = LogFactory.getLog(IncomingBTHandshaker.class);

	/**
	 * "protocol" as ASCII text in an 8-byte array.
	 * This is a part of the BitTorrent handshake, which begins "[19]BitTorrent protocol".
	 */
	private static final byte[] PROTOCOL = new byte[] { 'p', 'r', 'o', 't', 'o', 'c', 'o', 'l' };

	/** A link back up to the program's single TorrentManager object, which keeps a list of the torrents we're downloading and sharing. */
	private final TorrentManager manager;

	/**
	 * Make a new IncomingBTHandshaker object to do the BitTorrent handshake with a remote computer that connected to us.
	 * Only TorrentManager.acceptConnection() calls this constructor.
	 * This constructor just saves the 2 given objects, it doesn't do anything else.
	 * 
	 * @param sock    The NIOSocket object that connects us to the remote computer
	 * @param manager A reference to the program's single TorrentManager object, which keeps the list of all the torrents we're downloading and sharing
	 */
	public IncomingBTHandshaker(NIOSocket sock, TorrentManager manager) {

		// Save the given objects in this new one
		this.sock = sock;
		this.manager = manager;
	}

	/**
	 * Get ready to start the handshake with the remote computer.
	 * 
	 * Only TorrentManager.acceptConnection() calls this method.
	 * Makes buffers to hold the remote computer's handshake data, and registers this object with NIO so it will call our handleRead() method.
	 */
	public void startHandshaking() {

		// Make the empty incomingHandshake buffers, where we'll put the data of the remote computer's handshake
		initIncomingHandshake();

		// Register this object with NIO so it will call our handleRead() method when the remote computer send us data
		setReadInterest();
	}

	/**
	 * Check the handshake the remote computer sent us when it connected to us.
	 * 
	 * Looks for the text "protocol", the Acceptor already read the 19 length byte and the text "BitTorrent ".
	 * Makes sure we have the torrent the remote computer wants.
	 * 
	 * @return True if a complete part of the remote computer's handshake looks valid.
	 *         True if we don't have another new complete part yet.
	 *         False if there's a mistake in the handshake data the remote computer sent us.
	 */
	protected boolean verifyIncoming() {

		// Loop for each buffer we've filled in the incomingHandshake array of them since the last time handleRead() called verifyIncoming()
		for ( ; currentBufIndex < incomingHandshake.length && !incomingHandshake[currentBufIndex].hasRemaining(); currentBufIndex++) {
			ByteBuffer current = incomingHandshake[currentBufIndex];

			// Verify the filled contents of incomingHandshake[0], an 8-byte buffer for the text "protocol"
			switch(currentBufIndex) {
			case 0:

				// Make sure the text is "protocol"
				if (!Arrays.equals(current.array(), PROTOCOL)) return false;
				
				// It's valid
				break;

			// Verify the filled contents of incomingHandshake[1], an 8-byte buffer for the remote computer's extension bytes
			case 1:

				// It's valid
				break;

			// Verify the filled contents of incomingHandshake[2], a 20-byte buffer for the hash of the info section of the .torrent file
			case 2:

				// Have the program's TorrentManager object look up the hash
				torrent = manager.getTorrentForHash(current.array()); // Get the ManagedTorrent object we have to represent it

				// The TorrentManager doesn't have a ManagedTorrent for that hash, we aren't downloading this file at all
				if (torrent == null) {

					// Return false to refuse the remote computer's connection to us
					if (LOG.isDebugEnabled()) LOG.debug("incoming connection for unknown torrent");
					return false;

				// Yes, we have this file
				} else {

					// Compose the handshake we'll send the remote computer
					initOutgoingHandshake();

					// Register this IncomingBTHandshaker with NIO so it will call our handleWrite() method when we can write
					setWriteInterest();

					// Have the BTConnectionFetcher add this IncomingBTHandshaker object to its fetchers list
					torrent.getFetcher().handshakerStarted(this);
				}

				// It's valid
				break;

			// Verify the filled contents of incomingHandshake[3], a 20-byte buffer for the remote computer's peer ID
			case 3:

				// No way to check it, it's valid
				break;
			}
		}

		// Return true, we found no problems with the remote computer's handshake data
		return true;
	}

	/**
	 * Make the empty incomingHandshake buffers, where we'll put the data of the remote computer's handshake.
	 * 
	 * incomingHandshake[1] is a ByteBuffer object made by wrapping the extBytes byte array.
	 * This means that when handleRead() writes data from the remote computer into the incomingHandshake[2] ByteBuffer, it will also go in the extBytes byte array.
	 * incomingHandshake[3] does this for the peerID byte array the same way.
	 */
	protected void initIncomingHandshake() {

		// Make incomingHandshake an array of 4 ByteBuffer objects
		incomingHandshake = new ByteBuffer[4];
		byte[] tmp = new byte[8];
		incomingHandshake[0] = ByteBuffer.wrap(tmp);      // Make incomingHandshake[0] to hold 8 bytes
		extBytes = new byte[8];
		incomingHandshake[1] = ByteBuffer.wrap(extBytes); // Make incomingHandshake[1] to hold 8 bytes, linked to the extBytes byte array
		tmp = new byte[20];
		incomingHandshake[2] = ByteBuffer.wrap(tmp);      // Make incomingHandshake[2] to hold 20 bytes
		peerId = new byte[20];
		incomingHandshake[3] = ByteBuffer.wrap(peerId);   // Make incomingHandshake[3] to hold 20 bytes, linked to the peerID byte array
	}
}
