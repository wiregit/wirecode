
// Commented for the Learning branch

package com.limegroup.bittorrent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.InterestWriteChannel;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.bittorrent.statistics.BTMessageStat;
import com.limegroup.bittorrent.statistics.BTMessageStatBytes;
import com.limegroup.bittorrent.statistics.BandwidthStat;
import com.limegroup.bittorrent.messages.BTMessage;

/**
 * A BTMessageWriter object holds and sends BitTorrent messages to the remote computer.
 * It joins a chain of writers in LimeWire's NIO design.
 * 
 * The BTConnection constructor makes a new BTMessageWriter, and saves it as _writer.
 * Then, it gives it messages we want to send the remote computer.
 * The BTMessageWriter has a queue that can hold up to 10 messages.
 * When NIO calls handleWrite(), it sends one.
 * 
 * BTMessageWriter implements the ChannelWriter interface.
 * This means a BTMessageWriter object has a channel it can write to, and the methods setWriteChannel() and getWriteChannel().
 * This also means NIO can tell a BTMessageWriter object when it should get data from its source and write to its channel.
 * NIO does this by calling the BTMessageWriter object's handleWrite() method.
 */
public class BTMessageWriter implements ChannelWriter {

	/** A debugging log we can write lines of text to as the program runs. */
	private static final Log LOG = LogFactory.getLog(BTMessageWriter.class);

	/**
	 * 10, the enqueue() method won't allow more than 10 messages in _queue.
	 * 
	 * TODO: Add a separate limit for Piece messages so we don't buffer too much data.
	 */
	private static final int MAX_QUEUE_SIZE = 10;

	/**
	 * The channel this BTMessageWriter will write to.
	 * We'll do this when NIO calls our handleWrite() method.
	 * _channel is the next object in a chain of writers that leads to the socket actually connected to the remote computer.
	 * 
	 * _channel is an InterestWriteChannel.
	 * The interest part means we can tell NIO if we're interested in writing or not.
	 * NIO will only call our handleWrite() method if we've set our interest to on.
	 */
	private InterestWriteChannel _channel;

	/** The data of a single message, the one we'll send next. */
	private ByteBuffer _out = null;

	/**
	 * The list of BitTorrent messages this BTMessageWriter will send to the remote computer.
	 * _queue is a LinkedList of objects that extend BTMessage, like BTChoke and BTPiece.
	 * 
	 * Lock on this object before accessing _queue.
	 */
	private final LinkedList _queue;

	/** A reference up to the BTConnection object that made this BTMessageWriter to send the remote computer messages. */
	private final BTConnection _connection;

	/** True if this object is shut down, and shouldn't perform any more network activity. */
	private volatile boolean shutdown;

	/**
	 * Make a new BTMessageWriter that will hold and send BitTorrent messages to the remote computer.
	 * Only the BTConnection constructor makes a new BTMessageWriter object.
	 * 
	 * @param connection The BTConnection object that is doing this
	 */
	public BTMessageWriter(BTConnection connection) {

		// Initialize member variables to null and empty
		_channel = null;
		_queue = new LinkedList();

		// Save the given reference
		_connection = connection;
	}

	/**
	 * Send a BitTorrent message to the remote computer.
	 * 
	 * The "NIODispatch" thread will call this handleWrite() method when this BTMessageWriter can send message data to the next object in the chain of writers.
	 * The ChannelWriter interface requirs this method.
	 * 
	 * Removes a single message from _queue, turns it into data in _out, and then writes that into the channel.
	 * 
	 * @return True if we filled the channel and still have more data to send.
	 *         False if we wrote everything we had and are empty.
	 */
	public boolean handleWrite() throws IOException {

		// Don't do anything if this BTMessage writer has been shut down
		if (shutdown) return false;

		// Make a note we're here
		if (LOG.isDebugEnabled()) LOG.debug("entering handleWrite call to " + _connection);

		// Record the number of bytes we write
		int written = 0;

		do {

			// Prepare _out with the data of a single BitTorrent message from _queue for us to send
			if (_out == null ||          // There's no _out buffer of data to send at all, or
				_out.remaining() == 0) { // There is, but it doesn't have the data of a BitTorrent message in it

				// Move one message from _queue to data in the _out buffer
				if (!sendNextMessage()) { // sendNextMessage() returns false if _queue and _out are empty

					// Tell our channel that we're not interested in giving it data at this time
					if (LOG.isDebugEnabled()) LOG.debug("no more messages to send to " + _connection);
					_channel.interest(this, false);

					// Return false, we wrote everything we had and are empty
					return false;
				}
			}

			// Send the data of the BitTorrent message into the channel
			written = _channel.write(_out);

			// Record the number of bytes we sent
			if (written > 0) {
				count(written);
				if (LOG.isDebugEnabled()) LOG.debug("wrote " + written + " bytes");
			}

		// Do that again until write() returns 0, either because _out is empty or _channel is full
		} while (written > 0);

		// Return true, we filled the channel and still have more to send
		return true;
	}

	/**
	 * Send a BitTorrent message to the remote computer.
	 * 
	 * Adds the given message to the queue of up to 10 of them this BTMessageWriter keeps.
	 * When NIO calls handleWrite(), we'll send the data of one to the next object in the chain of writers.
	 * Eventually, it will make it to the remote computer.
	 * 
	 * @param m The object that extends BTMessage, like BTHave or BTBitField, to send.
	 * @return  True if we added the message to our queue of messages to send.
	 *          False if we didn't because our queue already has 10 messages in it.
	 */
	public boolean enqueue(BTMessage m) {

		// If we already have too many messages, return false
		if (_queue.size() > MAX_QUEUE_SIZE) return false; // TODO: _queue could grow to hold 11, the > should be >=

		// Add the given message last in the queue
		_queue.addLast(m); // We'll send it after the messages that are already there
		if (LOG.isDebugEnabled()) LOG.debug("enqueing message of type " + m.getType() + " to " + _connection.toString() + " : " + m.toString());

		// Tell the object we send data to that we have some data for it, so it should call our handleWrite() method when it wants some
		if (_channel != null) _channel.interest(this, true);

		// Return true, we added the message to our queue and it will get sent
		return true;
	}

	/**
	 * Java threw the "NIODispatch" thread an IOException while it was doing something for us.
	 * Passes it up to the BTConnection object.
	 * The ChannelWriter interface requires this method.
	 * 
	 * @param iox The IOException
	 */
	public void handleIOException(IOException iox) {

		// Pass it up to the BTConnection object that made this BTMessageWriter
		_connection.handleIOException(iox);
	}

	/**
	 * Make this BTMessageWriter stop all network activity.
	 * The ChannelWriter interface requires this method.
	 * 
	 * Has the "NIODispatch" thread remove all the messages from our queue, and tell the object we write to we have no data for it.
	 */
	public void shutdown() {

		// Only do this once, and record it has been done
		if (shutdown) return;
		shutdown = true;

		// Have the "NIODispatch" thread run the code in this run() method
		NIODispatcher.instance().invokeLater(new Runnable() {

			// The "NIODispatch" thread will call this run() method
			public void run() {

				// Remove all the messages from our queue
				_queue.clear();

				// Tell the object we write to we have no data for it
				_channel.interest(BTMessageWriter.this, false);
			}
		});
	}

	/**
	 * Give this BTMessageWriter a channel it can write data to.
	 * This is how it will send data to the remote computer.
	 * 
	 * @param newChannel An InterestWriteChannel this object can write data to
	 */
	public void setWriteChannel(InterestWriteChannel newChannel) {

		// Save the given channel
		_channel = newChannel;

		// Tell it we're interested in finding out when we can write to it
		_channel.interest(this, true); // It will call our handleWrite() method when it wants data from us
	}

	/**
	 * Get the channel this BTMessageWriter writes data to.
	 * This is how it sends data to the remote computer.
	 * 
	 * @return The InterestWriteChannel this object sends data to
	 */
	public InterestWriteChannel getWriteChannel() {

		// Return the channel we've been using
		return _channel;
	}

	/**
	 * Count that we've sent more bytes.
	 * 
	 * @param written The number of bytes we sent
	 */
	public void count(int written) {

		// Add the number to bandwidth statistics, and give it to the BTConnection object
		BandwidthStat.BITTORRENT_MESSAGE_UPSTREAM_BANDWIDTH.addData(written);
		_connection.wroteBytes(written);
	}

	/**
	 * Determine if this BTMessageWriter object has messages in its queue waiting to go out.
	 * 
	 * @return True if the queue is empty, this object has nothing to send right now.
	 *         False if there are messages in the queue, and this object will send them.
	 */
	public boolean isIdle() {

		// Return true if the queue is empty
		return _queue.isEmpty();
	}

	/**
	 * Remove one message from _queue, and turn it into data in the ByteBuffer _out.
	 * Only handleWrite() calls this method.
	 * 
	 * @return True if _out contains a message for handleWrite() to write.
	 *         False if _out is empty because there were no messages in _queue.
	 */
	private boolean sendNextMessage() {

		// If our queue of messages to send is empty, return false, we have nothing to write
		if (_queue.isEmpty()) return false;

		// Get the first message from the queue
		BTMessage message = (BTMessage)_queue.removeFirst();

		// Make a note that we're going to send this message
		if (LOG.isDebugEnabled()) LOG.debug("sending message " + message + " to " + _connection);

		// Convert the message from an object into data
		ByteBuffer payload = message.getPayload();           // Get the payload of the message, the part beyond "LLLLT" the length and type
		_out = ByteBuffer.allocate(payload.remaining() + 5); // Make _out a ByteBuffer that can hold the 5 bytes of "LLLLT" and then the payload
		_out.order(ByteOrder.BIG_ENDIAN);                    // Have the _out ByteBuffer write the ints we give it in big endian order
		_out.putInt(payload.remaining() + 1);                // Write the "LLLL" part, add 1 for the "T" type byte
		_out.put(message.getType());                         // Write the "T" type byte
		_out.put(payload);                                   // Write the payload, this fills the buffer and moves position all the way to limit at the end
		_out.flip();                                         // Move position back to the start so the buffer is ready for reading

		// Count this message in statistics
		countMessage(message, _out.remaining());

		// If there are no more messages in our queue, tell our connection we need more (do)
		if (_queue.isEmpty()) _connection.readyForWriting();

		// Return true, the _out buffer has a message to send to the remote computer
		return true;
	}

	/**
	 * Count that we sent a message in statistics.
	 * 
	 * @param message The object that represents the message
	 * @param length  The size of the message
	 */
	private void countMessage(BTMessage message, int length) {

		// Sort by message type
		switch (message.getType()) {
		case BTMessage.CHOKE:
			BTMessageStat.OUTGOING_CHOKE.incrementStat();      // Count that we've sent another Choke message
			BTMessageStatBytes.OUTGOING_CHOKE.addData(length); // Count that we've sent length more bytes of Choke message data
			break;
		case BTMessage.UNCHOKE:
			BTMessageStat.OUTGOING_UNCHOKE.incrementStat();
			BTMessageStatBytes.OUTGOING_UNCHOKE.addData(length);
			break;
		case BTMessage.INTERESTED:
			BTMessageStat.OUTGOING_INTERESTED.incrementStat();
			BTMessageStatBytes.OUTGOING_INTERESTED.addData(length);
			break;
		case BTMessage.NOT_INTERESTED:
			BTMessageStat.OUTGOING_NOT_INTERESTED.incrementStat();
			BTMessageStatBytes.OUTGOING_NOT_INTERESTED.addData(length);
			break;
		case BTMessage.HAVE:
			BTMessageStat.OUTGOING_HAVE.incrementStat();
			BTMessageStatBytes.OUTGOING_HAVE.addData(length);
			break;
		case BTMessage.BITFIELD:
			BTMessageStat.OUTGOING_BITFIELD.incrementStat();
			BTMessageStatBytes.OUTGOING_BITFIELD.addData(length);
			break;
		case BTMessage.CANCEL:
			BTMessageStat.OUTGOING_CANCEL.incrementStat();
			BTMessageStatBytes.OUTGOING_CANCEL.addData(length);
			break;
		case BTMessage.PIECE:
			BTMessageStat.OUTGOING_PIECE.incrementStat();
			BTMessageStatBytes.OUTGOING_PIECE.addData(length);
			break;
		case BTMessage.REQUEST:
			BTMessageStat.OUTGOING_REQUEST.incrementStat();
			BTMessageStatBytes.OUTGOING_REQUEST.addData(length);
		}
	}
}
