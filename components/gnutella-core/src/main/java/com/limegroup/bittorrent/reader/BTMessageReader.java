package com.limegroup.bittorrent.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;

import org.limewire.concurrent.SchedulingThreadPool;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.CircularByteBuffer;
import org.limewire.nio.channel.ChannelReadObserver;
import org.limewire.nio.channel.InterestReadChannel;
import org.limewire.nio.observer.IOErrorObserver;

import com.limegroup.bittorrent.BTMessageHandler;
import com.limegroup.bittorrent.messages.BadBTMessageException;
import com.limegroup.bittorrent.statistics.BandwidthStat;

public class BTMessageReader implements ChannelReadObserver, PieceParseListener {
	
	// a small buffer for connections
	private static final int BUFFER_SIZE = 2 * 1024;
	
	// the channel we read from
	private InterestReadChannel _channel;

	/** Observer to notify in case of errors */
	private final IOErrorObserver ioxObserver;
	
	// the ByteBuffer for the message currently being read from the network.
	private CircularByteBuffer _in;

	/** Whether this reader is shutdown */
	private volatile boolean shutdown;
	
	/** 
	 * Whether we've stopped reading from the network
	 * (not same as connection choking) 
	 */
	private boolean choked;
	
	/** The current state that is parsing the input */
	private BTReadMessageState currentState;
	
	/** 
	 * <tt>ThreadPool</tt> on which its safe to schedule calls
	 * to handleRead
	 */
	private final SchedulingThreadPool networkInvoker;
	
	/**
	 * Cached runnable that drains any data read in the buffer
	 */
	private Runnable drainer;
	
	/**
	 * Shared reader data between the different states.
	 */
	private final ReaderData readerState;
	
	/**
	 * Constructor
	 */
	public BTMessageReader(IOErrorObserver ioxObserver, 
			BTMessageHandler handler,
            SchedulingThreadPool networkInvoker,
			ByteBufferCache cache) {
		_in = new CircularByteBuffer(BUFFER_SIZE, cache);
		this.networkInvoker = networkInvoker;
		this.ioxObserver = ioxObserver;
		readerState = new ReaderData(handler, new CBBDataSource(), this);
		currentState = new LengthState(readerState);
		readerState.setEntryState(currentState);
	}

	/**
	 * Notification that read can be performed
	 */
	public void handleRead() throws IOException {
		synchronized(readerState) {
			if (shutdown)
				return;
			while(true) {
				int read = 0;
				if(!bufferFull()) 
					read = _in.read(_channel);
				if (read > 0)
					count(read);
				else 
					break;
				processState();
				if (bufferFull())  
					choke(true);
			}
		}
	}
	
	/**
	 * chokes or unchokes read signals from this channel.  
	 * Not the same as torrent connection choking
	 * @param choke whether to choke or unchoke the channel
	 */
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
	private void count(int read) {
		BandwidthStat.BITTORRENT_MESSAGE_DOWNSTREAM_BANDWIDTH.addData(read);
	}

	/**
	 * Implement ChannelReadObserver interface
	 */
	public void handleIOException(IOException iox) {
		ioxObserver.handleIOException(iox);
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
		ioxObserver.shutdown();
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
	
	public void dataConsumed(boolean stateChange) {
		choke(false);
		if (stateChange) {
			currentState = readerState.getEntryState();
			if (_in.size() > 0 && !shutdown)
				networkInvoker.invokeLater(getDrainer());
		}
	}
	
	private Runnable getDrainer() {
		if (drainer == null) 
			drainer = new Drainer();
		return drainer;
	}
	
	/**
	 * Runnable that explicitly calls handleRead to
	 * process any data that was read after the end of a
	 * piece.
	 */
	private class Drainer implements Runnable {
		public void run() {
			synchronized(readerState) {
				if (_in.size() == 0 || shutdown)
					return;
				try {
					processState();
				} catch (BadBTMessageException bad) {
					shutdown();
				}
			}
		}
	}
	
	/**
	 * An implementation of <tt>BTDataSource</tt> that delegates to the 
	 * local CircularByteBuffer
	 */
	private class CBBDataSource implements BTDataSource {

		public void discard(int howMuch) {
			_in.discard(howMuch);
		}

		public byte get() {
			return _in.get();
		}

		public void get(byte[] dest) {
			_in.get(dest);
		}

		public int getInt() {
			_in.order(ByteOrder.BIG_ENDIAN);
			return _in.getInt();
		}

		public int size() {
			return _in.size();
		}
		
		public void get(ByteBuffer dest) {
			_in.get(dest);
		}
		
		public void write(WritableByteChannel to, int howMuch) throws IOException {
			_in.write(to, howMuch);
		}
	}
}


