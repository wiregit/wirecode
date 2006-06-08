package com.limegroup.bittorrent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.util.BufferByteArrayOutputStream;
import com.limegroup.bittorrent.statistics.BTMessageStat;
import com.limegroup.bittorrent.statistics.BTMessageStatBytes;
import com.limegroup.bittorrent.statistics.BandwidthStat;
import com.limegroup.bittorrent.messages.BTBitField;
import com.limegroup.bittorrent.messages.BTMessage;
import com.limegroup.bittorrent.messages.BTPiece;
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
	private ByteBuffer _in;

	// the length of the message we are currently reading.
	private int _length = -1;
	
	// whether this is the first message we're reading
	private boolean first = true;

	/** Whether this reader is shutdown */
	private volatile boolean shutdown;
	
	/** The current state that is parsing the input */
	private BTReadMessageState currentState;
	
	/** Cached state objects for the length and type states */
	private final BTReadMessageState LENGTH_STATE, TYPE_STATE;
	
	/**
	 * Constructor
	 */
	public BTMessageReader(BTConnection connection) {
		_in = ByteBuffer.allocate(BUFFER_SIZE);
		_connection = connection;
		LENGTH_STATE = new LengthState();
		TYPE_STATE = new TypeState();
		currentState = LENGTH_STATE;
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
			while( _in.hasRemaining() && (read = _channel.read(_in)) > 0 )
				thisTime += read;
			if (thisTime > 0)
				count(thisTime);
			else if (read == -1)
				throw new IOException();
			else
				break;
			
			_in.flip();
			processState();
			_in.compact();
		}
	}
	
	private void processState() throws BadBTMessageException {
		while(true) {
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
			if (_in.remaining() < 4)
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
			if (_in.remaining() < 1)
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
			if (_in.remaining() < _length) 
				return null;
			
			ByteBuffer buf;
			if (_length == 0)
				buf = BTMessage.EMPTY_PAYLOAD; // TODO: use the one from BufferUtils
			else {
				int oldLimit = _in.limit();
				_in.limit(_length + _in.position());
				buf = _in.slice();
				_in.position(_in.limit());
				_in.limit(oldLimit);
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
		 * If the BitField is small enough to fit our current buffer,
		 * we create it and process it in-place.
		 * 
		 * Otherwise, we use a BufferByteArrayOutputStream 
		 * that grows as data arrives on the wire. It is deliberately 
		 * not pre-allocated even though we know how large it will 
		 * eventually get.
		 */
		private BufferByteArrayOutputStream bbaos;
		
		public BTReadMessageState addData() throws BadBTMessageException {
			int limit = _in.limit();
			
			// if the entire BitField was received just process it
			if (bbaos == null && _in.remaining() >= _length) {
				LOG.debug("parsing BitField in-place");
				_in.limit(_in.position() + _length);
				countAndProcess(_in.slice());
				_in.position(_in.limit());
				_in.limit(limit);
				return LENGTH_STATE;
			} else {
				if (bbaos == null)
					bbaos = new BufferByteArrayOutputStream();
				
				int toWrite = Math.min(_in.position() + _length - bbaos.size(), 
						_in.limit());
				_in.limit(toWrite);
				bbaos.write(_in);
				_in.limit(limit);
				
				if (LOG.isDebugEnabled())
					LOG.debug("parsing bitfield incrementally, so far " + bbaos.size());
				
				if (bbaos.size() == _length) { 
					countAndProcess(ByteBuffer.wrap(bbaos.toByteArray()));
					return LENGTH_STATE;
				}
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
	private class PieceState implements BTReadMessageState {
		int chunkId = -1;
		int offset = -1;
		int currentOffset;
		
		/** Whether we actually requested this piece */
		boolean welcome;
		
		public BTReadMessageState addData() throws BadBTMessageException {
			if (_in.remaining() < 4 && (chunkId < 0 || offset < 0))
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
				BTInterval complete = new BTInterval(offset, offset + _length - 9, chunkId); 
				welcome = _connection.startReceivingPiece(complete);
			}
			
			if (!_in.hasRemaining())
				return null;
			
			int oldLimit = _in.limit();
			int newLimit = Math.min(oldLimit, 
					_in.position() + _length + offset - currentOffset - 8);
			
			_in.limit(newLimit);
			BTInterval interval = new BTInterval(currentOffset, 
					currentOffset + _in.remaining() - 1,
					chunkId);
			
			currentOffset += _in.remaining();
			
			// if the piece was requested, we process it.
			// otherwise we skip it.
			if (welcome) {
				byte []data = new byte[_in.remaining()];
				_in.get(data);
				BTPiece piece = new BTPiece(interval, data);
				_connection.processMessage(piece);
			} else
				_in.position(_in.limit());
			
			_in.limit(oldLimit);
			
			if (currentOffset == offset + _length - 8) {
				// we're done receiving this piece
				_connection.requestIfPossible();
				BTMessageStat.INCOMING_PIECE.incrementStat();
				BTMessageStatBytes.INCOMING_PIECE.addData(5 + _length);
				return LENGTH_STATE;
			}
			else
				return null;
		}
	}
	
}


