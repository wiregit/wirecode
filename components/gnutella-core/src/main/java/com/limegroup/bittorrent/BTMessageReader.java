package com.limegroup.bittorrent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.util.BufferByteArrayOutputStream;
import com.limegroup.gnutella.util.CircularByteBuffer;
import com.limegroup.bittorrent.statistics.BTMessageStat;
import com.limegroup.bittorrent.statistics.BTMessageStatBytes;
import com.limegroup.bittorrent.statistics.BandwidthStat;
import com.limegroup.bittorrent.messages.BTBitField;
import com.limegroup.bittorrent.messages.BTMessage;
import com.limegroup.bittorrent.messages.BadBTMessageException;

public class BTMessageReader implements ChannelReadObserver {
	
	private static final Log LOG = LogFactory.getLog(BTMessageReader.class);

	// a small buffer for connections
	private static final int BUFFER_SIZE = 2 * 1024;
	
	/** Max size for a piece message */
	private static final int MAX_PIECE_SIZE = 32 * 1024 + 9;
	
	// the channel we read from
	private InterestReadChannel _channel;

	// the connection this reader is associated with and that will be informed
	// of any incoming messages
	private final BTConnection _connection;

	// the ByteBuffer for the message currently being read from the network.
	private CircularByteBuffer _in;

	// the length of the message we are currently reading.
	private int _length = -1;
	
	// whether this is the first message we're reading
	private boolean first = true;
	
	/** Whether this reader is shutdown */
	private volatile boolean shutdown;
	
	/** 
	 * Whether we've stopped reading from the network
	 * (not same as connection choking) 
	 */
	private boolean choked;
	
	/** The current state that is parsing the input */
	private BTReadMessageState currentState;
	
	/** Cached state objects for the length and type states */
	private final BTReadMessageState LENGTH_STATE, TYPE_STATE;
	
	/**
	 * Constructor
	 */
	public BTMessageReader(BTConnection connection) {
		_in = new CircularByteBuffer(BUFFER_SIZE, NIODispatcher.instance().getBufferCache());
		_connection = connection;
		LENGTH_STATE = new LengthState();
		TYPE_STATE = new TypeState();
		currentState = LENGTH_STATE;
	}

	/**
	 * Notification that read can be performed
	 */
	public synchronized void handleRead() throws IOException {
		if (shutdown)
			return;
		while(true) {
			int read = 0;
			int thisTime = 0;
			while( !bufferFull() && (read = _in.read(_channel)) > 0 )
				thisTime += read;
			if (thisTime > 0)
				count(thisTime);
			else if (read == -1)
				throw new IOException();
			else 
				break;
			processState();
			if (bufferFull())  
				choke(true);
		}
	}
	
	private void choke(boolean choke) {
		if (choked != choke) {
			_channel.interest(!choke);
			choked = choke;
		}
	}
	
	private boolean bufferFull() {
		return _in.size() == _in.capacity();
	}
	
	private void processState() throws BadBTMessageException {
		while(currentState != null) {
			BTReadMessageState next = currentState.addData();
			if (next == null)
				break;
			else currentState = next;
		} 
	}
	
	/**
	 * update stats.
	 */
	public void count(int read) {
		BandwidthStat.BITTORRENT_MESSAGE_DOWNSTREAM_BANDWIDTH.addData(read);
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
		_connection.close();
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
	
	/**
	 * 
	 *	An interface describing a state of the BT message parser.
	 */
	interface BTReadMessageState {
		/**
		 * Add data to the current parsing state from the _in buffer.
		 * @return the next parsing state or null if we need to stay
		 * in this state
		 * @throws BadBTMessageException the message parsing fails.
		 */
		public BTReadMessageState addData() throws BadBTMessageException;
	}
	
	/**
	 * State that parses the length of a BT message.
	 * @modifies _length 
	 */
	private class LengthState implements BTReadMessageState {
		public BTReadMessageState addData() throws BadBTMessageException {
			if (_in.size() < 4)
				return null;
			
			_in.order(ByteOrder.BIG_ENDIAN);
			_length = _in.getInt();
			
			if (LOG.isDebugEnabled())
				LOG.debug(_connection+ " parsed length "+_length);
			
			if (_length < 0 || _length > MAX_PIECE_SIZE)
				throw new BadBTMessageException("bad message size " + _length);
			
			if (_length == 0) {
				BTMessage.countKeepAlive();
				first = false;
				return this;
			}
			
			_length--;
			return TYPE_STATE;
		}
	}
	
	/**
	 * State that parses the type of a BT message. 
	 */
	private class TypeState implements BTReadMessageState {
		private byte type = -1;
		public BTReadMessageState addData() throws BadBTMessageException {
			if (_in.size() < 1)
				return null;
			
			type = _in.get();
			
			if (LOG.isDebugEnabled())
				LOG.debug(_connection + " parsed type "+type);
			
			boolean wasFirst = first;
			first = false;
			if (wasFirst && type == BTMessage.BITFIELD)
				return new BitfieldState(); // only sent as first message if at all.
			else if (type == BTMessage.PIECE)
				return new PieceState();
			else 
				return new MessageState(type);
		}
	}

	/**
	 * State that parses all BT messages except Piece and BitField.
	 */
	private class MessageState implements BTReadMessageState {
		private final byte type;
		
		MessageState(byte type) {
			this.type = type;
		}
		
		public BTReadMessageState addData() throws BadBTMessageException {
			if (_in.size() < _length) 
				return null;
			
			ByteBuffer buf;
			if (_length == 0)
				buf = BTMessage.EMPTY_PAYLOAD; // TODO: use the one from BufferUtils
			else {
				buf = ByteBuffer.allocate(_length);
				_in.get(buf);
				buf.clear();
			}
			BTMessage message = BTMessage.parseMessage(buf, type);
			_connection.processMessage(message);
			return LENGTH_STATE;
		}
	}

	/**
	 * State that parses the Bitfield message. 
	 */
	private class BitfieldState implements BTReadMessageState {
		/* 
		 * We use a BufferByteArrayOutputStream 
		 * that grows as data arrives on the wire. It is deliberately 
		 * not pre-allocated even though we know how large it will 
		 * eventually get.
		 */
		private BufferByteArrayOutputStream bbaos;
		private WritableByteChannel bbaosChan;
		
		public BTReadMessageState addData() throws BadBTMessageException {
			

			if (bbaos == null) {
				bbaos = new BufferByteArrayOutputStream();
				bbaosChan = Channels.newChannel(bbaos);
			}
			
			int toWrite = _length - bbaos.size();
			try {
				_in.write(bbaosChan, toWrite);
			} catch (IOException impossible) {
				ErrorService.error(impossible);
			}
			
			if (LOG.isDebugEnabled())
				LOG.debug("parsing bitfield incrementally, so far " + bbaos.size());
			
			if (bbaos.size() == _length) { 
				countAndProcess(ByteBuffer.wrap(bbaos.toByteArray()));
				return LENGTH_STATE;
			}
			
			return null;
		}
			
		private void countAndProcess(ByteBuffer b) {
			BTBitField field = new BTBitField(b);
			BTMessageStat.INCOMING_BITFIELD.incrementStat();
			BTMessageStatBytes.INCOMING_BITFIELD.addData(5 + 
					_length);
			_connection.processMessage(field);
		}
	}

	/**
	 * State that parses the Piece message. 
	 */
	private class PieceState implements BTReadMessageState, BTPieceFactory {
		private int chunkId = -1;
		private int offset = -1;
		private int currentOffset;
		private BTInterval complete;
		
		/** Whether we actually requested this piece */
		private boolean welcome;
		/** Whether we are expecting someone to write our data to disk */
		private boolean writeExpected;
		
		public BTReadMessageState addData() throws BadBTMessageException {
			if (_length < 9)
				throw new BadBTMessageException("piece too short");
			
			if (_in.size() < 4 && (chunkId < 0 || offset < 0))
				return null;
			
			// read chunk id
			if (chunkId < 0) {
				_in.order(ByteOrder.BIG_ENDIAN);
				chunkId = _in.getInt();
				return this; // shortcut :)
			}
			
			// read offset
			if (offset < 0) {
				_in.order(ByteOrder.BIG_ENDIAN);
				offset = _in.getInt();
				currentOffset = offset;
				// check if the piece was requested
				complete = new BTInterval(offset, offset + _length - 9, chunkId); 
				welcome = _connection.startReceivingPiece(complete);
			}
			
			int available = getAmountLeft();
			if (available == 0)
				return null;
			
			// if the piece was requested, we process it.
			// otherwise we skip it.
			if (welcome) {
				// if the buffer is full, turn off read interest
				if (!writeExpected) { 
					writeExpected = true;
					_connection.handlePiece(this);
				}
			} else {
				_in.discard(available);
				currentOffset += available;
				available = 0;
			}
			
			if (currentOffset + available == complete.high + 1) {
				BTMessageStat.INCOMING_PIECE.incrementStat();
				BTMessageStatBytes.INCOMING_PIECE.addData(5 + _length);
				
				// we're done receiving this piece, request more.
				_connection.request();
				if (writeExpected)
					currentState = null;
				else
					return LENGTH_STATE;
			}
			return null;
		}
		
		private int getAmountLeft() {
			return Math.min(_in.size(), complete.high - currentOffset + 1);
		}
		
		public BTPiece getPiece() {
			synchronized(BTMessageReader.this) {
				Assert.that(writeExpected);
				writeExpected = false;
				int toRead = getAmountLeft();
				
				choke(false);
				
				BTInterval in = new BTInterval(currentOffset, 
						currentOffset + toRead - 1,
						chunkId);
				currentOffset += toRead;
				_connection.readBytes(toRead);
				byte []data = new byte[toRead];
				_in.get(data);
				if (currentOffset > complete.high) 
					currentState = LENGTH_STATE;
				
				return new ReceivedPiece(in, data);
			}
		}
		
	}
	
	private class ReceivedPiece implements BTPiece {
		private final BTInterval interval;
		private final byte [] data;
		
		ReceivedPiece(BTInterval interval, byte [] data) {
			this.interval = interval;
			this.data = data;
		}
		
		public BTInterval getInterval() {
			return interval;
		}
		
		public byte [] getData() {
			return data;
		}
	}
}


