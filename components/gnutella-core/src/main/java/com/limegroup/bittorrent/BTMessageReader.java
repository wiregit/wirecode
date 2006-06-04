
// Commented for the Learning branch

package com.limegroup.bittorrent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.BandwidthTracker;
import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.bittorrent.statistics.BTMessageStat;
import com.limegroup.bittorrent.statistics.BTMessageStatBytes;
import com.limegroup.bittorrent.statistics.BandwidthStat;
import com.limegroup.bittorrent.messages.BTMessage;
import com.limegroup.bittorrent.messages.BadBTMessageException;

/**
 * A BTMessageReader object slices data from a remote computer into BitTorrent messages, and gives them to BTConnection.processMessage().
 * BTMesageReader joins a chain of readers in LimeWire's NIO design.
 * 
 * The BTConnection constructor makes a new BTMessageReader, and saves it as _reader.
 * 
 * BTMessageReader implements the ChannelReadObserver interface.
 * This means a BTMessageReader object has a channel it can read from, and the methods setReadChannel() and getReadChannel().
 * This also means that NIO can tell a BTMessageReader when it should read by calling its handleRead() method.
 */
public class BTMessageReader implements ChannelReadObserver {

	/** 4, the size of the _in buffer before we make it exactly the size of the message we're reading. */
	private static final int MIN_BUFFER_SIZE = 4;

	/** 32777 bytes, 32 KB, if we get a message bigger than this, we'll throw an exception. */
	private static final int MAX_BUFFER_SIZE = 32 * 1024 + 9;

	/** A 4-byte array that can hold message data. */
	private final byte[] _messageBuffer = new byte[MIN_BUFFER_SIZE];

	/** The channel we read from. */
	private InterestReadChannel _channel;

	/** The BTConnection object that made us, and that we are reading BitTorrent messages for. */
	private final BTConnection _connection;

	/**
	 * _in points to the ByteBuffer where we're putting the message we're currently reading.
	 * adjustBuffer() allocates a new ByteBuffer for _in that is exactly the size of the message we're reading.
	 * resetBuffer() points _in back at _messageBuffer, a 4-byte buffer that can hold just the length. (do)
	 */
	private ByteBuffer _in;

	/** The length of the message we're currently reading. */
	private int _length;

	/**
	 * A bandwidth tracker that can keep track of how fast we download data from the remote computer.
	 * This is not the same things as a BitTorrent tracker.
	 */
	private SimpleBandwidthTracker _tracker;

	/** True when this object has been shut down, and won't be doing any more activity on the network. */
	private volatile boolean shutdown;

	/**
	 * Make a new BTMessageReader object, which will get data from the remote computer and slice it into BitTorrent messages.
	 * Only the BTConnection constructor makes a new BTMessageReader object.
	 * 
	 * @param connection A reference back up to the BTConnection object that is making this object
	 */
	public BTMessageReader(BTConnection connection) {

		// Set up buffers to read the next message
		resetBuffer();

		// Save the given reference
		_connection = connection;

		// Make a new LimeWire SimpleBandwidthTracker object, which will keep track of how fast the remote computer is sending us data
		_tracker = new SimpleBandwidthTracker();
	}

	/**
	 * The "NIODispatch" thread calls handleRead() when this connection's BTMessageReader object can read more data from its channel.
	 * handleRead() reads BitTorrent packet data from the remote computer.
	 * The method slices it into individual packets, parses them into objects that extend BTMessage, and hands them off to this connection's processMessage() method.
	 * This is the message slicer.
	 */
	public void handleRead() throws IOException {

		// Only do something if this connect has not been shut down
		if (shutdown) return;

		// Make sure the _in buffer has room for more data
		Assert.that(_in.hasRemaining(), "ByteBuffer full!");

		// Make a variable for how many bytes we bring in each time the loop below runs
		int read = 0;

		// Loop until read is 0 no data right now, or -1 channel closed
		do {

			// Move data from the channel into the _in buffer
			read = _channel.read(_in); // Returns the number of bytes it moved
			if (read > 0) count(read); // Record that we got this many

			// If that filled the _in ByteBuffer
			if (!_in.hasRemaining()) {

				// We've read the entire message
				if (_in.position() == _length) { // The _in ByteBuffer's position reaches exactly over the length of the next BitTorrent message

					// Parse the message data at the start of the _in buffer into a message-type specific object that extends BTMessage
					BTMessage message = BTMessage.parseMessage(
						_in,      // The ByteBuffer that has the message at the start of it, position and limit clip out empty space afterwards
						_length); // The length of the message at the start of _in

					// Give the BitTorrent message to this connection's processMessage() method
					_connection.processMessage(message);

					// Point _in back at _messageBuffer, and reset _length to -1
					resetBuffer();

				// Otherwise, we read just the first 4 bytes of the message
				} else {

					// Read the first 4 bytes to _length, and make _in exactly that long
					adjustBuffer();
				}
			}

		// If we got something from our channel on that loop, loop again to get more
		} while (read > 0);
	}

	/**
	 * Count that we received some data from the remote computer.
	 * 
	 * @param read The number of bytes we read
	 */
	public void count(int read) {

		// Count it in statistics
		BandwidthStat.BITTORRENT_MESSAGE_DOWNSTREAM_BANDWIDTH.addData(read);

		// Have our SimpleBandwidthTracker count it
		_tracker.count(read);
		_tracker.measureBandwidth();
	}

	/**
	 * Get the SimpleBandwidthTracker this BTMessageReader uses to keep track of how fast we're receiving data from this remote computer.
	 * This doesn't have anything to do with a BitTorrent tracker, the Web script that connects peers that have the same torrent.
	 * 
	 * ManagedTorrent uses this to unchoke the hosts that are sending us lots of data.
	 * 
	 * @return A reference to our SimpleBandwidthTracker object
	 */
	public BandwidthTracker getBandwidthTracker() {

		// Return our SimpleBandwidthTracker
		return _tracker;
	}

	/**
	 * Read the length from the start of the _in buffer, set _length, and make _in exactly that long.
	 * 
	 * Reads the length of the next message from the first 4 bytes of the _in ByteBuffer.
	 * Sets _length to this length.
	 * Replaces the _in ByteBuffer with one exactly as long as the length.
	 * 
	 * Only handleRead() above calls this.
	 * We've just read the first 4 bytes of a BitTorrent message.
	 */
	private void adjustBuffer() throws BadBTMessageException {

		// Change position and length from clipping around the empty space at the end of the buffer to the data before it
		_in.flip(); // Moves position to the start and length to where position was

		// Read the first 4 bytes in the _in ByteBuffer as a big endian int, this is the message length
		_in.order(ByteOrder.BIG_ENDIAN);
		_length = _in.getInt();

		// Make sure the length is 0 or more, but not too big
		if (_length < 0 || _length > MAX_BUFFER_SIZE) throw new BadBTMessageException("bad message size " + _length);

		// If length is 0, this is a Keep Alive message
		if (_length == 0) {

			// Record that the remote computer sent us another Keep Alive message
			BTMessageStat.INCOMING_KEEP_ALIVE.incrementStat();
			BTMessageStatBytes.INCOMING_KEEP_ALIVE.addData(4);

			// Get ready to read the next message
			_in.clear();  // In the _in ByteBuffer, move the position to the start and the limit to the end
			_length = -1; // We don't know the length of the next message yet

		// This message has a length of 1 or more bytes
		} else {

			// Replace the _in ByteBuffer with one exactly as long as the message
			_in = ByteBuffer.allocate(_length);
		}
	}

	/**
	 * Set up buffers for reading the next message.
	 * Points _in back at _messageBuffer, and sets _length to -1.
	 * The BTMessageReader constructor calls this to set things up at the start.
	 * handleRead() calls this each time it finishes reading a complete message from the remote computer.
	 */
	private void resetBuffer() {

		// Point _in at a ByteBuffer built around the _messageBuffer array
		_in = ByteBuffer.wrap(_messageBuffer);

		// Set _length to -1, we don't know how big the next message is yet
		_length = -1;
	}

	/**
	 * NIO had an exception while sending or receiving data for this BTMessageReader object.
	 * The "NIODispatch" thread will call this method if it gets an IOException when doing something for us.
	 * 
	 * @param iox The IOException it got
	 */
	public void handleIOException(IOException iox) {

		// Pass it up to the BTConnection object we're reading messages for
		_connection.handleIOException(iox);
	}

	/**
	 * Mark this BTMessageReader as shut down.
	 */
	public void shutdown() {

		// Only let one thread do this at a time
		synchronized(this) {

			// Set the shutdown flag to true
			if (shutdown) return;
			shutdown = true;
		}
	}

	/**
	 * Give this BTMessageReader a channel it can get data from.
	 * This is how it will receive BitTorrent message data from the remote computer.
	 * 
	 * @param newChannel An InterestReadChannel this object can read data from
	 */
	public void setReadChannel(InterestReadChannel newChannel) {

		// Save the given channel
		_channel = newChannel;
	}

	/**
	 * Get the channel this BTMessageReader gets data from.
	 * This is how it receives BitTorrent message data from the remote computer.
	 * 
	 * @return The InterestReadChannel this object reads data from
	 */
	public InterestReadChannel getReadChannel() {

		// Return the channel we've been using
		return _channel;
	}
}
