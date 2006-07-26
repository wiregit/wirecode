package com.limegroup.bittorrent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.io.IOErrorObserver;
import com.limegroup.gnutella.io.InterestWriteChannel;
import com.limegroup.bittorrent.statistics.BTMessageStat;
import com.limegroup.bittorrent.statistics.BTMessageStatBytes;
import com.limegroup.bittorrent.statistics.BandwidthStat;
import com.limegroup.bittorrent.messages.BTMessage;

public class BTMessageWriter implements BTChannelWriter {

	private static final Log LOG = LogFactory.getLog(BTMessageWriter.class);

	/** A keepalive with 4x0 bytes */
	private static final ByteBuffer KEEP_ALIVE = ByteBuffer.allocate(4).asReadOnlyBuffer();
	
	/** keepAlive for this writer */
	private final ByteBuffer myKeepAlive = KEEP_ALIVE.duplicate();
	
	// InterestWriteChannel to write to.
	private InterestWriteChannel _channel;

	// ByteBuffer storing the message currently being written
	private final ByteBuffer[] _out = new ByteBuffer[2];
	
	/** 
	 * the internal message queue
	 */
	private final LinkedList<BTMessage> _queue;

	/**
	 * Observer to notify in case of errors
	 */
	private final IOErrorObserver ioxObserver;
	
	/**
	 * Observer to notify with piece events.
	 */
	private final PieceSendListener pieceListener;
	
	/** Whether this writer is shutdown */
	private volatile boolean shutdown;
	
	/**
	 * The current message being sent, if any.
	 */
	private BTMessage currentMessage;
	
	/**
	 * Constructor
	 */
	public BTMessageWriter(IOErrorObserver ioxObserver, PieceSendListener pieceListener) {
		_queue = new LinkedList<BTMessage>();
		this.ioxObserver = ioxObserver;
		this.pieceListener = pieceListener;
		_out[0] = ByteBuffer.allocate(5);
	}

	/**
	 * Implements ChannelWriter interface
	 */
	public boolean handleWrite() throws IOException {
		if (shutdown)
			return false;
		
		if (LOG.isDebugEnabled())
			LOG.debug("entering handleWrite call to "+this);
		int written = 0;
		while(true) {
			if (myKeepAlive.hasRemaining()) {
				if (LOG.isDebugEnabled())
					LOG.debug("sending a keepalive on "+this);
				written += _channel.write(myKeepAlive);
				if (myKeepAlive.hasRemaining()) // need to finish keepalive first.
					return true;
			}
			
			if (_out[1] == null || _out[1].remaining() == 0) {
				currentMessage = null;
				
				if (!sendNextMessage()) {
					if (LOG.isDebugEnabled())
						LOG.debug("no more messages to send on "+this);
					_channel.interest(this, false);
					return false;
				}
			}
			// this should ideally be done with gathering writes, but
			// because somewhere in the chain we have a delayer its ok.
			written = _channel.write(_out[0]);
			written += _channel.write(_out[1]);
			
			if (!_out[1].hasRemaining()) {
				_out[1] = null; // can be gc'd now
				
				if (currentMessage.getType() == BTMessage.PIECE) 
					pieceListener.pieceSent();
			}
			
			if (written > 0) {
				count(written);
				if (LOG.isDebugEnabled())
					LOG.debug("wrote "+written+" bytes");
			} else 
				break;
			
		} 
		return true;
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.MessageWriter#sendKeepAlive()
	 */
	public void sendKeepAlive() {
		if (_queue.isEmpty() && _out[1] == null) {
			myKeepAlive.clear();
			_channel.interest(this, true);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.MessageWriter#enqueue(com.limegroup.bittorrent.messages.BTMessage)
	 */
	public void enqueue(BTMessage m) {
		
		_queue.addLast(m);
		
		if (LOG.isDebugEnabled())
			LOG.debug("enqueing message of type " + m.getType() + " to "
					+ this + " : " + m.toString());
		
		// if there was a keepalive waiting to be sent and none of it 
		// was written get rid of it
		if (myKeepAlive.remaining() == 4)
			myKeepAlive.limit(0);
		
		if (_channel != null)
			_channel.interest(this, true);
	}
	
	/**
	 * Implement ChannelWriter interface
	 */
	public void handleIOException(IOException iox) {
		ioxObserver.handleIOException(iox);

	}

	/**
	 * Implement ChannelWriter interface
	 */
	public void shutdown() {
		if (shutdown)
			return;
		shutdown = true;
		ioxObserver.shutdown();
	}

	/**
	 * Implement ChannelWriter interface
	 */
	public void setWriteChannel(InterestWriteChannel newChannel) {
		_channel = newChannel;
		_channel.interest(this, true);
	}

	/**
	 * Implement ChannelWriter interface
	 */
	public InterestWriteChannel getWriteChannel() {
		return _channel;
	}

	/**
	 * count written bytes in statistics and for banwdidth tracking
	 */
	private void count(int written) {
		BandwidthStat.BITTORRENT_MESSAGE_UPSTREAM_BANDWIDTH.addData(written);
		pieceListener.wroteBytes(written);
	}
	
	/**
	 * takes the next message from the queue and prepares it for writing by
	 * moving it to a ByteBuffer.
	 * 
	 * @return true if the next message was prepared for writing, false if there
	 *         are no more messages left to write
	 */
	private boolean sendNextMessage() {
		if (_queue.isEmpty())
			return false;
		
		currentMessage = _queue.removeFirst();
		
		if (LOG.isDebugEnabled())
			LOG.debug("sending message "+currentMessage+" on "+this);

		_out[1] = currentMessage.getPayload();
		_out[0].clear();
		_out[0].order(ByteOrder.BIG_ENDIAN);
		_out[0].putInt(_out[1].remaining() + 1); // message size
		_out[0].put(currentMessage.getType());
		_out[0].flip();
		countMessage(currentMessage, _out[1].remaining()+5);
		return true;
	}
	
	/**
	 * update statistics
	 */
	private void countMessage(BTMessage message, int length) {
		switch (message.getType()) {
		case BTMessage.CHOKE:
			BTMessageStat.OUTGOING_CHOKE.incrementStat();
			BTMessageStatBytes.OUTGOING_CHOKE.addData(length);
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
	
	public String toString() {
		return "BTMessageWriter for "+ pieceListener;
	}
}
