
// Commented for the Learning branch

package com.limegroup.bittorrent.handshaking;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.BTConnection;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.io.InterestScatteringByteChannel;
import com.limegroup.gnutella.io.InterestWriteChannel;
import com.limegroup.gnutella.io.NIOSocket;

/**
 * BTHandshaker is the base class for IncomingBTHandshaker and OutgoingBTHandshaker.
 * It has the common code to exchange data with a remote computer using LimeWire's NIO system.
 * 
 * The program never makes a BTHandshaker object.
 * Instead, it makes IncomingBTHandshaker and OutgoingBTHandshaker objects, which both extend this BTHandshaker class.
 * 
 * BTHandshaker implements the ChannelWriter interface.
 * This means a BTHandshaker object has a channel it writes to, and has the methods setWriteChannel and getWriteChannel().
 * It also means the "NIODispatch" thread can tell a BTHandshaker object to get data, the thread does this by calling handleWrite().
 * 
 * BTHandshaker also implements the ChannelReadObserver interface.
 * This means a BTHandshaker object has a channel it can read from, and has the methods setReadChannel() and getReadChannel().
 * It also means the "NIODispatch" thread can tell a BTHandshaker object to read data, the thread does this by calling handleRead().
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
 * The member variable extBytes holds E.
 * The member variable peerId holds P.
 */
public abstract class BTHandshaker implements ChannelWriter, ChannelReadObserver {

	/** A debugging log we can write lines of text to as the program runs. */
	private static final Log LOG = LogFactory.getLog(BTHandshaker.class);

	/**
	 * The object that represents the .torrent file we're shaking hands with a remote computer to get.
	 * A ManagedTorrent is an object that represents a .torrent file we're downloading and uploading.
	 */
	protected ManagedTorrent torrent;

	/**
	 * The data of our handshake for the remote computer.
	 */
	protected ByteBuffer outgoingHandshake;

	/**
	 * An array of ByteBuffers that hold the data of the handshake the remote computer sends us, split into different parts.
	 * 
	 * incomingHandshake is an array of ByteBuffer objects.
	 * The ScatteringByteChannel.read() method can move data from a channel into an array of ByteBuffers like this one.
	 * When one ByteBuffer fills up, it starts putting data into the next one.
	 * Determine if they're all full by seeing if the position in the last one has moved to its limit.
	 * 
	 * OutgoingBTHandshaker.initIncomingHandshake() makes 5 ByteBuffer objects for all 5 lines of the BitTorrent handshake.
	 * IncomingBTHandshaker.initIncomingHandshake() only makes 4, because the Acceptor has already the 19 byte and the text "BitTorrent ".
	 * 
	 * The handleRead() method here moves data from our channel into these buffers.
	 */
	protected ByteBuffer[] incomingHandshake;

	/**
	 * The index of the buffer we're on in incomingHandshake.
	 * For instance, if currentBufIndex is 1, we're in incomingHandshake[1], the 19-byte buffer that will hold the greeting "BitTorrent protocol".
	 */
	protected int currentBufIndex;

	/**
	 * The object we give data to send it to the remote computer.
	 * This is the next object in a chain of writers that leads to the remote computer.
	 * NIO will call our handleWrite() method when we can send data.
	 * We'll send data by calling writeChannel.write(data).
	 */
	protected InterestWriteChannel writeChannel;

	/**
	 * The object we get data from the remote computer from.
	 * This is the next object in a chain of readers that leads to the remote computer.
	 * NIO will call our handleRead() method when we can get data.
	 * We'll get data by calling readChannel.read(destinationBuffer).
	 */
	protected InterestScatteringByteChannel readChannel;

	/** The NIOSocket object which connects us to the remote computer. */
	protected NIOSocket sock;

	/**
	 * The remote computer has sent its entire handshake.
	 * 
	 * When the remote computer has sent us its entire BitTorrent handshake, handleRead() sets incomingDone to true.
	 * This also means that we've read the 64-byte BitTorrent handshake from the channel.
	 * The next data inside it is the start of the first BitTorrent packet.
	 */
	protected boolean incomingDone;

	/**
	 * We've exchanged complete handshakes with the remote computer.
	 * 
	 * When the remote computer has sent us its entire BitTorrent handshake, and we've sent it ours, tryToFinishHandshakes() will set finishingHandshake to true.
	 */
	protected boolean finishingHandshakes;

	/**
	 * True when the shutdown() method has been called, and this object is being shut down.
	 */
	private volatile boolean shutdown;

	/**
	 * The remote computer's extension bytes.
	 * These are 8 bytes that come after the text "BitTorrent protocol".
	 * 
	 * These bytes tell what extensions to the BitTorrent protocol the remote computer understands.
	 * Many BitTorrent clients will pass all 0s for these bytes.
	 */
	protected byte[] extBytes;

	/**
	 * The remote computer's peer ID.
	 * These are the last 20 bytes of the BitTorrent handshake.
	 * 
	 * The first 4 bytes of the peer ID are the vendor code, like "LIME".
	 */
	protected byte[] peerId;

	/**
	 * Read handshake data the remote computer sent us.
	 * If we're done with the handshake, make objects to represent this new connection and register them with the ManagedTorrent.
	 * 
	 * The "NIODispatch" thread calls handleRead() when we can read from our readChannel.
	 * The data we get from it will be the next part of the remote computer's handshake with us.
	 */
	public void handleRead() throws IOException {

		// If we have been shut down, don't do anything
		if (shutdown) return;

		// Move data from readChannel into the incomingHandshake buffers
		long read = 0;
		while (

			// Move data from readChannel into the incomingHandshake buffers, when one fills, data will go into the next one
			(read = readChannel.read(incomingHandshake)) > 0 && // Get read, the number of bytes it moved, if it moved some data, go to the next line

			// If the last ByteBuffer in the incomingHandshake array of them isn't full yet, loop to keep reading
			incomingHandshake[incomingHandshake.length - 1].hasRemaining()) ; // There's no code in this while loop

		// Make sure the handshake the remote computer sent us is valid
		if (read == -1 ||        // The channel is closed, or
			!verifyIncoming()) { // Part of the remote computer's handshake is bad

			// Shut down this BTHandshaker and leave
			if (LOG.isDebugEnabled()) LOG.debug("bad incoming handshake on element " + currentBufIndex + " or channel closed " + read);
			shutdown();
			return;
		}

		// If the remote computer has sent us its entire handshake
		if (!incomingHandshake[incomingHandshake.length - 1].hasRemaining()) { // If the last ByteBuffer in the incomingHandshake array is filled with data

			// We're done reading handshake data from the remote computer
			if (LOG.isDebugEnabled()) LOG.debug("incoming handshake finished " + sock.getInetAddress());
			incomingDone = true;
		}

		// If we're done with the handshake, make objects to represent this new connection and register them with the ManagedTorrent
		tryToFinishHandshakes();
	}

	/**
	 * IncomingBTHandshaker and OutgoingBTHandshaker have verifyIncoming() methods that check the handshake data the remote computer sent us.
	 * 
	 * @return True if the remote computer's handshake looks OK, false if there is a problem with it
	 */
	protected abstract boolean verifyIncoming();

	/**
	 * Send our handshake to the remote computer.
	 * If we're done with the handshake, make objects to represent this new connection and register them with the ManagedTorrent.
	 * 
	 * The "NIODispatch" thread calls handleWrite() when we can write to our channel.
	 * This is when and how we send data to the remote computer.
	 */
	public boolean handleWrite() throws IOException {

		// If we have been shut down, don't do anything
		if (shutdown) return false;

		// Send the remote computer our handshake
		int wrote = 0; // The number of bytes write() sent
		while ((wrote = writeChannel.write(outgoingHandshake)) > 0 && outgoingHandshake.hasRemaining()); // Loop until we fill the channel or run out of handshake

		// If we sent our entire handshake, tell the channel we write to that we're not interested in giving it any more data
		if (!outgoingHandshake.hasRemaining()) writeChannel.interest(this, false);

		// If we're done with the handshake, make objects to represent this new connection and register them with the ManagedTorrent
		tryToFinishHandshakes();

		/*
		 * this falls through to SocketAdapter which ignores it.
		 */

		// It doesn't matter what this handleWrite() method returns
		return true; // A handleWrite() method returns true to report we filled the channel, and still have more data to write
	}

	/**
	 * IncomingBTHandshaker and OutgoingBTHandshaker have initIncomingHandshake() methods that make buffers for incomingHandshake.
	 */
	protected abstract void initIncomingHandshake();

	/**
	 * Compose the handshake we'll send to the remote computer.
	 * Has the torrent's connection fetcher put it together.
	 * Saves it in the outgoingHandshake ByteBuffer.
	 */
	protected void initOutgoingHandshake() {

		// Have the ManagedTorrent object's BTConnectionFetcher compose our handshake for the remote computer
		outgoingHandshake = torrent.getFetcher().getOutgoingHandshake();
	}

	/**
	 * Register this object with NIO so it will call our handleRead() method when the remote computer sends us data.
	 */
	protected final void setReadInterest() {

		// Tell the NIOSocket object connected to the remote computer we're the object it should forward NIO's commands to read to
		sock.setReadObserver(this);

		// Tell the channel we get data from to tell us when it has data for us
		readChannel.interest(true); // It will do this by calling our handleRead() method
	}

	/**
	 * Register this object with NIO so it will call our handleWrite() method when we can send the remote computer data.
	 */
	protected final void setWriteInterest() {

		// Tell the NIOSocket object connected to the remote computer we're the object it should forward NIO's commands to write to
		sock.setWriteObserver(this);

		// Tell the channel we send data into to tell us when we can do that
		writeChannel.interest(this, true); // It will tell us when by calling our handleWrite() method
	}

	/**
	 * Sets up this new connection to a remote BitTorrent computer.
	 * 
	 * If we're done exchanging handshake data with the remote computer, does the following things.
	 * Sets finishingHandshakes to true.
	 * Makes TorrentLocation and BTConnection objects to represent the remote computer.
	 * Adds them to the ManagedTorrent object that represents the .torrent file this is all for.
	 * Tells the .torrent's BTConnectionFetcher that we're done.
	 */
	private void tryToFinishHandshakes() {

		// If this method has already run, or this object is shut down, leave without doing anything
		if (finishingHandshakes || shutdown) return;

		// If the remote computer has sent us its entire BitTorrent handshake, and we've sent it ours
		if (incomingDone && !outgoingHandshake.hasRemaining()) {

			// Record that we're done with the handshake
			finishingHandshakes = true;

			// Make a new TorrentLocation object to represent the remote computer we just finished shaking hands with
			TorrentLocation p = new TorrentLocation(
				sock.getInetAddress(), // The remote computer's Internet IP address and port number
				sock.getPort(),
				peerId,                // The remote computer's peer ID and extension bytes it told us in the handshake
				extBytes);

			// Make a new BTConnection object to represent our connection to the remote computer right now
			BTConnection btc = new BTConnection(
				sock,                  // The NIOSocket object that connects us
				torrent.getMetaInfo(), // The BTMetaInfo object we made from the bencoded data inside the .torrent file
				p,                     // The remote computer's IP address adn port number in a TorrentLocation object
				torrent,               // The ManagedTorrent object that represents the .torrent file
				true);                 // True, we initiated the connection

			// List this new connection with the ManagedTorrent object
			if (LOG.isDebugEnabled()) LOG.debug("added outgoing connection " + sock.getInetAddress().getHostAddress());
			torrent.addConnection(btc);

			// Tell the torrent's BTConnectionFetcher we made this connection
			torrent.getFetcher().handshakerDone(this);
		}
	}

	/**
	 * There was an error with our connection to this remote computer.
	 * 
	 * Tells this torrent's BTConnectionFetcher that this BTHandshaker object is done, and closes our socket to the remote computer.
	 * 
	 * @param iox The IOException Java threw us
	 */
	public void handleIOException(IOException iox) {

		// Tell this torrent's BTConnectionFetcher that this BTHandshaker object is done, and close our socket to the remote computer
		shutdown();
	}

	/**
	 * Shut down this BTHandshaker object.
	 * Tells this torrent's BTConnectionFetcher that this BTHandshaker object is done.
	 * Closes our socket connection to the remote computer.
	 */
	public void shutdown() {

		// Only perform these steps once
		synchronized(this) {
			if (shutdown) return;
			shutdown = true; // Mark that this BTHandshaker object has been shut down
		}

		// Tell this torrent's BTConnectionFetcher that this BTHandshaker is finished
		if (torrent != null) torrent.getFetcher().handshakerDone(this);

		// Close our socket to the remote computer
		if (sock != null) try { sock.close(); } catch (IOException impossible) {}
	}

	/**
	 * Set the channel this object will write to.
	 * 
	 * @param newChannel An InterestWriteChannel this object can send data to
	 */
	public void setWriteChannel(InterestWriteChannel newChannel) {

		// Save the given channel in this object
		writeChannel = newChannel;
	}

	/**
	 * Get the channel this object writes to.
	 * 
	 * @return The InterestWriteChannel this object sends data to
	 */
	public InterestWriteChannel getWriteChannel() {

		// Return the object we saved and have been using
		return writeChannel;
	}

	/**
	 * Set the channel this object will read from.
	 * 
	 * @param newChannel An InterestReadChannel this object can read data from
	 */
	public void setReadChannel(InterestReadChannel newChannel) {

		/*
		 * if this throws, we've got problems
		 */

		// Save the given channel in this object
		readChannel = (InterestScatteringByteChannel)newChannel;
	}

	/**
	 * Get the channel this object reads from.
	 * 
	 * @return The InterestReadChannel this object gets data from
	 */
	public InterestReadChannel getReadChannel() {

		// Return the object we saved and have been using
		return readChannel;
	}
}
