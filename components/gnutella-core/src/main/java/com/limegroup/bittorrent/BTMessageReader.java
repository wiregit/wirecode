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

public class BTMessageReader implements ChannelReadObserver {
	/*
	 * The standard buffer size.
	 */
	private static final int MIN_BUFFER_SIZE = 4;

	// size of 32K piece message, we never request more
	private static final int MAX_BUFFER_SIZE = 32 * 1024 + 9;

	// we use this array to reduce the number of objects constructed.
	private final byte[] _messageBuffer = new byte[MIN_BUFFER_SIZE];

	// the channel we read from
	private InterestReadChannel _channel;

	// the connection this reader is associated with and that will be informed
	// of any incoming messages
	private final BTConnection _connection;

	// the ByteBuffer for the message currently being read from the network.
	private ByteBuffer _in;

	// the length of the message we are currently reading.
	private int _length;

	/*
	 * my own private BandwidthTracker
	 */
	private SimpleBandwidthTracker _tracker;
	
	/** Whether this reader is shutdown */
	private volatile boolean shutdown;

	/**
	 * Constructor
	 */
	public BTMessageReader(BTConnection connection) {
		resetBuffer();
		_connection = connection;
		_tracker = new SimpleBandwidthTracker();
	}

	/**
	 * Notification that read can be performed
	 */
	public void handleRead() throws IOException {
		if (shutdown)
			return;
		
		// all messages should have been read
		Assert.that(_in.hasRemaining(), "ByteBuffer full!");

		int read = 0;
		do {
			read = _channel.read(_in);
			if (read > 0)
				count(read);
			if (!_in.hasRemaining()) {
				if (_in.position() == _length) {
					BTMessage message = BTMessage.parseMessage(_in, _length);
					_connection.processMessage(message);
					resetBuffer();
				} else {
					// we read the first for bytes of the message, containing
					// the message size, - adjust _in to read the whole
					// message
					adjustBuffer();
				}
			}
		} while (read > 0);
	}

	/**
	 * We extend SimpleBandwidthTracker to calculate the bandwidth we use more
	 * easily.
	 */
	public void count(int read) {
		BandwidthStat.BITTORRENT_MESSAGE_DOWNSTREAM_BANDWIDTH.addData(read);
		_tracker.count(read);
		_tracker.measureBandwidth();
	}

	/**
	 * Accessor for the BandwidthTracker we hold, used by ManagedTorrent which
	 * preferrably unchokes hosts that are sending us a lot of data
	 * 
	 * @return our BandwidthTracker
	 */
	public BandwidthTracker getBandwidthTracker() {
		return _tracker;
	}

	/**
	 * Mother's little helper is called, whenever we read the first 4 bytes of a
	 * new message from the network. Then we will create a new ByteBuffer _in to
	 * store the complete message without the length header. The length of the
	 * message that we read from the first 4 bytes of the message will be stored
	 * in the _length field.
	 * 
	 * @throws BadBTMessageException
	 *             if we encounter an illegal message length
	 */
	private void adjustBuffer() throws BadBTMessageException {
		_in.flip();
		_in.order(ByteOrder.BIG_ENDIAN);
		_length = _in.getInt();
		if (_length < 0 || _length > MAX_BUFFER_SIZE)
			throw new BadBTMessageException("bad message size " + _length);
		if (_length == 0) { // keep alive message, ignore
			BTMessageStat.INCOMING_KEEP_ALIVE.incrementStat();
			BTMessageStatBytes.INCOMING_KEEP_ALIVE.addData(4);
			_in.clear();
			_length = -1;
		} else
			_in = ByteBuffer.allocate(_length);
	}

	/**
	 * Another little helper, reset _in and _length fields after reading a
	 * complete message from network.
	 */
	private void resetBuffer() {
		_in = ByteBuffer.wrap(_messageBuffer);
		_length = -1;
	}

	/**
	 * Implement ChannelReadObserver interface
	 */
	public void handleIOException(IOException iox) {
		_connection.handleIOException(iox);
	}

	/**
	 * Implement ChannelReadObserver interface
	 */
	public void shutdown() {
		synchronized(this) {
			if (shutdown)
				return;
			shutdown = true;
		}
		_in = null;
	}

	/**
	 * Implement ChannelReadObserver interface
	 */
	public void setReadChannel(InterestReadChannel newChannel) {
		_channel = newChannel;
	}

	/**
	 * Implement ChannelReadObserver interface
	 */
	public InterestReadChannel getReadChannel() {
		return _channel;
	}
}
