package com.limegroup.bittorrent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.bittorrent.statistics.BandwidthStat;
import com.limegroup.bittorrent.messages.BTMessage;
import com.limegroup.bittorrent.messages.BadBTMessageException;

public class BTMessageReader implements ChannelReadObserver {

	// size of 32K piece message, we never request more
	private static final int BUFFER_SIZE = 32 * 1024 + 9;

	// the channel we read from
	private InterestReadChannel _channel;

	// the connection this reader is associated with and that will be informed
	// of any incoming messages
	private final BTConnection _connection;

	// the ByteBuffer for the message currently being read from the network.
	private ByteBuffer _in;

	// the length of the message we are currently reading.
	private int _length = -1;

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
		_in = ByteBuffer.allocate(BUFFER_SIZE);
		_connection = connection;
		_tracker = new SimpleBandwidthTracker();
	}

	/**
	 * Notification that read can be performed
	 */
	public void handleRead() throws IOException {
		if (shutdown)
			return;
		
		while(true) {
			int read = 0;
			int thisTime = 0;
			while( (read = _channel.read(_in)) > 0 && _in.hasRemaining())
				thisTime += read;
			if (thisTime > 0)
				count(thisTime);
			else
				break;
			
			_in.flip();
			
			while(_in.hasRemaining()) {
				if (_length == -1 && _in.remaining() >= 4)
					readMessageLength();
				if (_length > 0 && _in.remaining() >= _length)
					readMessage();
				else
					break;
			}
			
			_in.compact();
		}
	}
	
	private void readMessageLength() throws BadBTMessageException {
		_in.order(ByteOrder.BIG_ENDIAN);
		_length = _in.getInt();
		if (_length < 0 || _length > BUFFER_SIZE)
			throw new BadBTMessageException("bad message size " + _length);
		if (_length == 0) {
			BTMessage.countKeepAlive();
			_length = -1;
		}
	}
	
	private void readMessage() throws BadBTMessageException {
		int oldLimit = _in.limit();
		_in.limit(_length + _in.position());
		int type = _in.get();
		BTMessage message = BTMessage.parseMessage(_in.slice(), type);
		_connection.processMessage(message);
		_in.position(_in.limit());
		_in.limit(oldLimit);
		_length = -1;
	}

	/**
	 * We extend SimpleBandwidthTracker to calculate the bandwidth we use more
	 * easily.
	 */
	public void count(int read) {
		BandwidthStat.BITTORRENT_MESSAGE_DOWNSTREAM_BANDWIDTH.addData(read);
		_tracker.count(read);
	}

	/**
	 * Accessor for the BandwidthTracker we hold, used by ManagedTorrent which
	 * preferrably unchokes hosts that are sending us a lot of data
	 * 
	 * @return our BandwidthTracker
	 */
	public float getBandwidth() {
		_tracker.measureBandwidth();
		return _tracker.getMeasuredBandwidth();
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
