
// Commented for the Learning branch

package com.limegroup.bittorrent.handshaking;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.BTConnectionFetcher;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.gnutella.io.ConnectObserver;
import com.limegroup.gnutella.io.NIOSocket;

/**
 * Make an OutgoingBTHandshaker object to perform the BitTorrent handshake as soon as our connection attempt to a remote computer goes through.
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
 * verifyIncoming() makes sure the remote computer's H matches the H we sent it.
 * 
 * OutgoingBTHandshaker implements the ConnectObserver interface.
 * This means the "NIODispatch" thread can tell an OutgoingBTHandshaker object that its channel has connected.
 * The thread does this by calling the object's handleConnect() method.
 */
public class OutgoingBTHandshaker extends BTHandshaker implements ConnectObserver {

	/** A debugging log that saves lines of text as the program runs. */
	private static final Log LOG = LogFactory.getLog(OutgoingBTHandshaker.class);

	/** The TorrentLocation object that represents the remote computer we're connecting to, and will shake hands with. */
	private TorrentLocation loc;

	/**
	 * Make a new OutgoingBTHandshaker object that will perform the BitTorrent handshake when Java makes our connection to a remote computer.
	 * Only BTConnectionFetcher.fetchConnection() calls this constructor.
	 * This constructor just saves the 2 given objects, it doesn't do anything else.
	 * 
	 * @param loc     A TorrentLocation object that represents the remote computer we're trying to connect to
	 * @param torrent The ManagedTorrent object that represents the .torrent file we're making this connection for
	 */
	public OutgoingBTHandshaker(TorrentLocation loc, ManagedTorrent torrent) {

		// Save the given objects in this new one
		this.loc = loc;
		this.torrent = torrent;
	}

	/**
	 * Get ready to shake hands with the remote computer.
	 * 
	 * The "NIODispatch" thread calls an OutgoingBTHandshaker's handleConnect() method when the connection it initiated goes through.
	 * Composes the handshake we'll send the remote computer, and makes buffers to read the remote computer's handshake.
	 * Registers this object with NIO so it will call our handleRead() and handleWrite() methods when we can transfer data.
	 * 
	 * @param socket The socket we connected.
	 *               socket looks like a java.net.Socket object, but it's actually a LimeWire NIOSocket object.
	 */
	public void handleConnect(Socket socket) throws IOException {

		// Make a note we established a connection to the remote computer's IP address
		if (LOG.isDebugEnabled()) LOG.debug("established connection to " + socket.getInetAddress());

		// Save the NIOSocket object that we connected to the remote computer
		sock = (NIOSocket)socket;

		// Initialize the buffers and interests for NIO
		initOutgoingHandshake(); // Compose the handshake we'll send the remote computer
		initIncomingHandshake(); // Make the incomingHandshake buffers, where we'll put the data of the remote computer's handshake
		setWriteInterest();      // Register this object with NIO so it will call our handleWrite() method when we can send the remote computer data
		setReadInterest();       // Register this object with NIO so it will call our handleRead() method when the remote computer sends us data
	}

	/**
	 * Make the empty incomingHandshake buffers, where we'll put the data of the remote computer's handshake.
	 * 
	 * incomingHandshake[2] is a ByteBuffer object made by wrapping the extBytes byte array.
	 * This means that when handleRead() writes data from the remote computer into the incomingHandshake[2] ByteBuffer, it will also go in the extBytes byte array.
	 * incomingHandshake[4] does this for the peerID byte array the same way.
	 */
	protected void initIncomingHandshake() {

		// Make incomingHandshake an array of 5 ByteBuffer objects
		incomingHandshake = new ByteBuffer[5];
		incomingHandshake[0] = ByteBuffer.allocate(1);    // Make incomingHandshake[0] to hold 1 byte
		byte[] tmp = new byte[19];
		incomingHandshake[1] = ByteBuffer.wrap(tmp);      // Make incomingHandshake[1] to hold 19 bytes
		extBytes = new byte[8];
		incomingHandshake[2] = ByteBuffer.wrap(extBytes); // Make incomingHandshake[2] to hold 8 bytes, linked to the extBytes byte array
		tmp = new byte[20];
		incomingHandshake[3] = ByteBuffer.wrap(tmp);      // Make incomingHandshake[3] to hold 20 bytes
		peerId = new byte[20];
		incomingHandshake[4] = ByteBuffer.wrap(peerId);   // Make incomingHandshake[4] to hold 20 bytes, linked to the peerId byte array
	}

	/**
	 * Check the handshake the remote computer sent us after we connected to it.
	 * 
	 * Makes sure the first byte has the length 19, and the 19 bytes "BitTorrent protocol" follow.
	 * Makes sure the remote computer responded with the same torrent hash that we told it in our handshake.
	 * 
	 * @return True if a complete part of the remote computer's handshake looks valid.
	 *         True if we don't have another new complete part yet.
	 *         False if there's a mistake in the handshake data the remote computer sent us.
	 */
	protected boolean verifyIncoming() {

		// Loop for each buffer we've filled in the incomingHandshake array of them since the last time handleRead() called verifyIncoming()
		for ( ; currentBufIndex < incomingHandshake.length && !incomingHandshake[currentBufIndex].hasRemaining(); currentBufIndex++) {
			ByteBuffer current = incomingHandshake[currentBufIndex];

			// Verify the filled contents of incomingHandshake[0], the single byte that holds the length 19
			switch(currentBufIndex) {
			case 0:

				// Make sure the first byte holds the number 19
				current.flip();                              // Move position to the start again
				if (current.get() != (byte)19) return false; // get() gets 1 byte, and moves position past it
				current.position(1);                         // Move the position back to the end again
				break;

			// Verify the filled contents of incomingHandshake[1], the 19 characters of ASCII text "BitTorrent protocol"
			case 1:

				// Make sure the text is "BitTorrent protocol"
				if (!Arrays.equals(current.array(), BTConnectionFetcher.BITTORRENT_PROTOCOL_BYTES)) return false;
				break;

			// Verify the filled contents of incomingHandshake[2], the remote computer's 8 extension bytes
			case 2:

				// No way to check it, it's valid
				break;

			// Verify the filled contents of incomingHandshake[3], the 20-byte hash of the torrent
			case 3:

				// Make sure the remote computer responded with the same torrent hash as we have
				if (!Arrays.equals(current.array(), torrent.getInfoHash())) return false;
				break;

			// Verify the filled contents of incomingHandshake[4], the remote computer's 20-byte peer ID
			case 4:

				// No way to check it, it's valid
				break;
			}
		}

		// Return true, we found no problems with the remote computer's handshake data
		return true;
	}

	/**
	 * If our connection attempt to this remote computer fails, the "NIODispatch" thread will call this handleIOException() method.
	 * Logs the failed connection attempt in the TorrentLocation object and adds it to one of the ManagedTorrent object's lists.
	 * Tells this torrent's BTConnectionFetcher that this BTHandshaker object is done, and closes our socket to the remote computer.
	 * 
	 * @param iox The IOException Java threw to the "NIODispatch" thread
	 */
	public void handleIOException(IOException iox) {

		// Make a note this happened
		if (LOG.isDebugEnabled()) LOG.debug("Connection failed: " + loc);

		// Log the failed connection attempt in the TorrentLocation object and add it to one of the ManagedTorrent object's lists
		loc.strike();                               // Count that we tried to connect to this location, and couldn't
		if (!loc.isOut()) torrent.addEndpoint(loc); // Add this location to the torrent, we can try again later
		else torrent.addBadEndpoint(loc);           // Add this location to the torrent's list of unreachable addresses, we've tried and failed too many times

		// Tell this torrent's BTConnectionFetcher that this BTHandshaker object is done, and close our socket to the remote computer
		super.handleIOException(iox);
	}
}
