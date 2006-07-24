package com.limegroup.bittorrent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.io.DelayedBufferWriter;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.io.NIOSocket;
import com.limegroup.gnutella.io.ThrottleReader;
import com.limegroup.gnutella.io.ThrottleWriter;
import com.limegroup.bittorrent.statistics.BTMessageStat;
import com.limegroup.bittorrent.messages.*;
import com.limegroup.gnutella.uploader.StalledUploadWatchdog;
import com.limegroup.gnutella.uploader.UploadSlotListener;
import com.limegroup.gnutella.util.BitField;
import com.limegroup.gnutella.util.BitFieldSet;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.BitSet;

/**
 * Class wrapping a Bittorrent connection.
 */
public class BTConnection implements UploadSlotListener {
	
	private static final Log LOG = LogFactory.getLog(BTConnection.class);

	/**
	 * This is the max size of a block that we will ever upload, requests larger
	 * than this are dropped.
	 */
	private static final int MAX_BLOCK_SIZE = 64 * 1024;

	/**
	 * the number of requests to send to any host without waiting for reply
	 */
	private static final int MAX_REQUESTS = 3;

	/**
	 * connections that die after less than a minute won't be retried
	 */
	private static final long MIN_RETRYABLE_LIFE_TIME = 60 * 1000;
	
	/**
	 * Maximum time it should take to send a piece
	 * (pieces are 32kb max)
	 */
	private static final long MAX_PIECE_SEND_TIME = 60 * 1000; 

	/*
	 * the NIOSocket
	 */
	private final NIOSocket _socket;

	/*
	 * Reader for the messages
	 */
	private final BTMessageReader _reader;

	/*
	 * Writer for the messages
	 */
	private final BTMessageWriter _writer;

	/**
	 * The pieces the remote host has
	 */
	private volatile BitSet _availableRanges;
	
	/** A bitfield view of what they have */
	private volatile BitField _available;
	
	/**
	 * the Set of BTIntervals we requested but which was not yet satisfied.
	 */
	private final Set<BTInterval> _requesting;

	/**
	 * the Set of BTInterval requested by the remote host.
	 */
	private final Set<BTInterval> _requested;

	/**
	 * the metaInfo of this torrent
	 */
	private final BTMetaInfo _info;

	/**
	 * the id of the remote client
	 */
	private final TorrentLocation _endpoint;

	/**
	 * whether or not this is an outgoing connection
	 */
	private final boolean _outgoing;

	/*
	 * our torrent
	 */
	private final ManagedTorrent _torrent;

	/**
	 * whether we choke them: if we are choking, all requests from the remote
	 * host will be ignored
	 */
	private boolean _isChoked;

	/**
	 * whether they choke us: only send requests if they are not choking us
	 */
	private volatile boolean _isChoking;

	/**
	 * Indicates whether the remote host is interested in one of the ranges we
	 * offer.
	 */
	private boolean _isInterested;

	/**
	 * Indicates whether or not the remote host offers ranges we want
	 */
	private volatile boolean _isInteresting;

	/**
	 * the time when this Connection was created
	 */
	private final long _startTime;
	
	/**
	 * The # of pieces the remote host is missing.
	 */
	private int numMissing;
	
	/**
	 * The # of the round this connection was unchoked last time.
	 */
	private int unchokeRound;
	
	/** Whether this connection is currently using an upload slot */
	private volatile boolean usingSlot;
	
	/** Bandwidth trackers for the outgoing and incoming bandwidth */
	private SimpleBandwidthTracker up, downShort, downLong;
	
	/** Watchdog for this connection */
	private StalledUploadWatchdog watchdog;
	
	/** Delayer for this connection */
	private final DelayedBufferWriter delayer;
	
	/** Whether this connection is currently closing */
	private volatile boolean closing;
	
	/**
	 * Constructs instance of this
	 * 
	 * @param sock
	 *            the Socket to the remote host. We assume that the Bittorrent
	 *            connection is already initialized and the headers were
	 *            exchanged successfully
	 * @param info
	 *            the BTMetaInfo holding all information for this torrent
	 * @param torrent
	 * 			  the ManagedTorrent to whom this connection belongs.
	 * @param isOutgoing
	 *            whether or not this is an outgoing connection
	 */
	public BTConnection(NIOSocket socket, BTMetaInfo info, TorrentLocation ep,
			ManagedTorrent torrent, boolean isOutgoing) {
		_socket = socket;
		_endpoint = ep;
		_torrent = torrent;
		_outgoing = isOutgoing;
		_availableRanges = new BitSet(info.getNumBlocks());
		_available = new BitFieldSet(_availableRanges, info.getNumBlocks());
		_requesting = new HashSet<BTInterval>();
		_requested = new HashSet<BTInterval>();
		_startTime = System.currentTimeMillis();
		_reader = new BTMessageReader(this);
		_writer = new BTMessageWriter(this);
		
		ThrottleWriter throttle = new ThrottleWriter(
				RouterService.getBandwidthManager().getThrottle(false));
		delayer = new DelayedBufferWriter(1400, 3000);
		_writer.setWriteChannel(delayer);
		delayer.setWriteChannel(throttle);
		ThrottleReader readThrottle = new ThrottleReader(
				RouterService.getBandwidthManager().getThrottle(true));
		_reader.setReadChannel(readThrottle);
		readThrottle.interest(true);
		socket.setReadObserver(_reader);
		socket.setWriteObserver(_writer);
		_info = info;

		// connections start choked and not interested
		_isChoked = true;
		_isChoking = true;
		_isInterested = false;
		_isInteresting = false;
		up = new SimpleBandwidthTracker();
		downShort = new SimpleBandwidthTracker(1000);
		downLong = new SimpleBandwidthTracker(5000);

		// if we have downloaded anything send a bitfield
		if (_info.getVerifyingFolder().getVerifiedBlockSize() > 0)
			sendBitfield();
	}


	/**
	 * @return true if we are choking the remote host
	 */
	public boolean isChoked() {
		return _isChoked;
	}
	
	/**
	 * @return true if the remote host is choking us.
	 */
	public boolean isChoking() {
		return _isChoking;
	}

	/**
	 * @return true if the remote host is interested in us
	 */
	public boolean isInterested() {
		return _isInterested;
	}
	
	/**
	 * @return whether the remote host should be interested
	 * in downloading from us.
	 */
	public boolean shouldBeInterested() {
		return numMissing > 0;
	}
	
	/**
	 * @return true if the remote host may have ranges that we want 
	 */
	public boolean isInteresting() {
		return _isInteresting;
	}
	
	/**
	 * @return true if we initiated this connection
	 */
	public boolean isOutgoing() {
		return _outgoing;
	}

	/**
	 * @return true if the connection should be retried.
	 */
	public boolean isWorthRetrying() {
		// don't retry connections that were aborted immediately after starting
		// them, they were most likely terminated for a reason...
		return System.currentTimeMillis() - _startTime > MIN_RETRYABLE_LIFE_TIME;
	}

	/**
	 * @return <tt>TorrentLocation</tt> we are connected to
	 */
	public TorrentLocation getEndpoint() {
		return _endpoint;
	}

	/**
	 * Closes the connection.
	 */
	public void close() {
		
		if (closing)
			return;
		closing = true;
		
		try {
			_socket.shutdownOutput();
		} catch (IOException ioe1) {}
		try {
			_socket.shutdownInput();
		} catch (IOException ioe2) {}
		
		_reader.shutdown();
		_writer.shutdown();
		
		_socket.close();
		
		clearRequests();
		
		if (usingSlot) {
			RouterService.getUploadSlotManager().cancelRequest(this);
			if (watchdog != null)
				watchdog.deactivate();
		}
		_torrent.connectionClosed(this);
	}

	/**
	 * @param read whether to return download bandwidth
	 * @param shortTerm whether to return short-term average or long-term
	 * @return the measured bandwidth on this connection for
	 * downloadining or uploading
	 */
	public float getMeasuredBandwidth(boolean read, boolean shortTerm) {
		SimpleBandwidthTracker tracker;
		if (!read)
			tracker = up;
		else if (shortTerm)
			tracker = downShort;
		else
			tracker = downLong;
		
		tracker.measureBandwidth();
		try {
			return tracker.getMeasuredBandwidth();
		} catch (InsufficientDataException ide) {
			return 0;
		}
	}
	
	/**
	 * notification that some bytes have been read on this connection
	 */
	public void readBytes(int read) {
		downShort.count(read);
		downLong.count(read);
		_torrent.countDownloaded(read);
	}

	/**
	 * notification that some bytes have been written on this connection
	 */
	public void wroteBytes(int written) {
		up.count(written);
		_torrent.countUploaded(written);
	}

	/**
	 * Handles IOExceptions for this connection
	 */
	public void handleIOException(IOException iox) {
		if (iox instanceof BadBTMessageException)
			BTMessageStat.INCOMING_BAD.incrementStat();
		if (LOG.isDebugEnabled())
			LOG.debug(iox);
		close();
	}

	/**
	 * Chokes the connection
	 */
	void sendChoke() {
		_requested.clear();
		if (!_isChoked) {
			if (LOG.isDebugEnabled())
				LOG.debug(this+" choking");
			_writer.enqueue(BTChoke.createMessage());
			_isChoked = true;
		} 
	}

	
	/**
	 * Unchokes the connection
	 */
	void sendUnchoke(int now) {
		setUnchokeRound(now);
		if (_isChoked) {
			if (LOG.isDebugEnabled())
				LOG.debug(this +" unchoking, round "+now);
			_writer.enqueue(BTUnchoke.createMessage());
			_isChoked = false;
		}
	}
	
	/**
	 * sends a keepalive
	 */
	void sendKeepAlive() {
		_writer.sendKeepAlive();
	}
	
	/**
	 * @return the round during which the connection was last unchoked
	 */
	int getUnchokeRound() {
		return unchokeRound;
	}
	
	/**
	 * sets the round during which the connection was choked
	 */
	void setUnchokeRound(int round) {
		unchokeRound = round;
	}

	/**
	 * Informs the remote that we are interested in downloading. 
	 */
	private void sendInterested() {
		if (!_isInteresting) {
			if (LOG.isDebugEnabled())
				LOG.debug(this+ " we become interested");
			_writer.enqueue(BTInterested.createMessage());
			_isInteresting = true;
		} 
	}

	/**
	 * Informs the remote we are not interested in downloading.
	 */
	void sendNotInterested() {
		if (_isInteresting) {
			if (LOG.isDebugEnabled())
				LOG.debug(this+ " we lose interest");
			_writer.enqueue(BTNotInterested.createMessage());
			_isInteresting = false;
		}
	}

	/**
	 * Tells the remote host, that we have a new piece. 
	 * 
	 * @param have the <tt>BTHave</tt> message representing a complete piece.
	 */
	void sendHave(BTHave have) {
		int pieceNum = have.getPieceNum();
		
		// As a minor optimization we will not inform the remote host of any
		// pieces that it already has
		if (!_available.get(pieceNum)) {
			numMissing++;
			_writer.enqueue(have);
		}  

		// we should indicate that we are not interested anymore, so we are
		// not unchoked when we do not want to request anything.
		if (!_info.getVerifyingFolder().containsAnyWeMiss(_available)) {
			cancelAllRequests();
			sendNotInterested();
			return;
		}

		// remove all subranges that we may be requesting
		for (Iterator<BTInterval> iter = _requesting.iterator(); iter.hasNext();) {
			BTInterval req = iter.next();
			if (req.getId() == pieceNum) {
				iter.remove();
				sendCancel(req);
			}
		}
		
		if (!_isChoking)
			request();
	}

	/**
	 * Sends a bitfield message to the remote host.
	 */
	private void sendBitfield() {
		_writer.enqueue(BTBitField.createMessage(_info));
	}

	private void sendCancel(BTInterval in) {
		_writer.enqueue(new BTCancel(in));
	}
	
	/**
	 * Cancels all requests. 
	 */
	void cancelAllRequests() {
		for (BTInterval request : _requesting) 
			sendCancel(request);
		clearRequests();
	}

	void pieceSent() {
		if (LOG.isDebugEnabled())
			LOG.debug(this+" piece sent");
		usingSlot = false;
		prepareForPiece(false);
		RouterService.getUploadSlotManager().requestDone(this);
		readyForWriting();
	}
	
	/**
	 * notifies this, that the connection is ready to write the next chunk of
	 * the torrent
	 */
	void readyForWriting() {
		if (_isChoked || _requested.isEmpty()) 
			return;
		
		usingSlot = true;
		int proceed = RouterService.getUploadSlotManager().requestSlot(
					this,
					!_torrent.isComplete());
		
		if (proceed == -1) { // denied, choke the connection
			usingSlot = false;
			sendChoke();
		} else if (proceed == 0) 
			requestPieceRead();
		// else queued, will receive callback.
	}
	
	void requestPieceRead() {
		if (_isChoked || _requested.isEmpty()) 
			return;
		
		// pick a request from them
		Iterator<BTInterval> iter = _requested.iterator();
		BTInterval in = iter.next();
		iter.remove();
		
		if (LOG.isDebugEnabled())
			LOG.debug(this+" requesting disk read for "+in);
		
		try {
			_info.getVerifyingFolder().sendPiece(in, this);
		} catch (IOException bad) {
			close();
		}
	}

	/**
	 * Notification that a piece is ready to be sent.
	 * @param in the interval to which the piece corresponds
	 * @param data the data of the piece.
	 */
	void pieceRead(final BTInterval in, final byte [] data) {
		RouterService.getBandwidthManager().applyUploadRate();
		Runnable pieceSender = new Runnable() {
			public void run() {
				if (LOG.isDebugEnabled())
					LOG.debug("disk read done for "+in);
				prepareForPiece(true);
				_writer.enqueue(new BTPieceMessage(in, data));
			}
		};
		NIODispatcher.instance().invokeLater(pieceSender);
	}
	
	/**
	 * Prepares the connection for sending a piece
	 * @param start whether we are starting a piece or finishing it.
	 */
	private void prepareForPiece(boolean start) {
		if (start) {
			if (watchdog == null)
				watchdog = new StalledUploadWatchdog(MAX_PIECE_SEND_TIME);
			watchdog.activate(_writer);
			delayer.setImmediateFlush(true);
		} else {
			watchdog.deactivate();
			delayer.setImmediateFlush(false);
		}
	}
	
	private void clearRequests() {
		for (BTInterval clear : _requesting)
			_info.getVerifyingFolder().releaseInterval(clear);
		_requesting.clear();
	}

	/**
	 * @param message the incoming message to process.
	 */
	public void processMessage(BTMessage message) {
		if (LOG.isDebugEnabled())
			LOG.debug(this +" handling message "+message);
		switch (message.getType()) {
		case BTMessage.CHOKE:
			_isChoking = true;
			clearRequests();
			break;

		case BTMessage.UNCHOKE:
			_isChoking = false;
			if (_isInteresting) 
				request();
			break;

		case BTMessage.INTERESTED:
			_isInterested = true;
			if (!_isChoked)
				_torrent.rechoke();
			break;

		case BTMessage.NOT_INTERESTED:
			_isInterested = false;
			_requested.clear(); // forget what they requested
			if (!_isChoked)
				_torrent.rechoke();
			// if we have all pieces and the remote is not interested,
			// disconnect, - they have obviously completed their download, too
			if (_torrent.isComplete())
				close();
			break;

		case BTMessage.BITFIELD:
			handleBitField((BTBitField) message);
			break;
		case BTMessage.HAVE:
			handleHave((BTHave) message);
			break;
		case BTMessage.REQUEST:
			handleRequest((BTRequest) message);
			break;
		case BTMessage.CANCEL:
			handleCancel((BTCancel) message);
			break;
		}
	}

	/**
	 * Removes the range specified in the <tt>BTCancel</tt> message
	 * from the list of requests.
	 * Note: if we are already sending this range, there's nothing
	 * that can be done.
	 */
	private void handleCancel(BTCancel message) {
		BTInterval in = message.getInterval();
		_requested.remove(in); 
		
		// remove any sub-ranges as well
		for (Iterator<BTInterval> iter = _requested.iterator(); iter.hasNext();) {
			BTInterval current = iter.next();
			if (in.getId() == current.getId() &&
					(in.low <= current.high && current.low <= in.high))
				iter.remove();
		}
	}

	/**
	 * Processes a request for a range.   
	 */
	private void handleRequest(BTRequest message) {
		// we do not process requests from choked connections; if we 
		// just choked a connection, we may still receive some requests.
		if (_isChoked) 
			return;

		BTInterval in = message.getInterval();
		if (LOG.isDebugEnabled())
			LOG.debug(this+ " got request for " + in);

		// ignore, that's a buggy client sending this request (didn't manage to
		// find out which one) - we could also throw an exception causing us to
		// disconnect...
		if (in.getId() > _info.getNumBlocks()) {
			if (LOG.isDebugEnabled())
				LOG.debug("got bad request " + message);
			return;
		}
		// we skip all requests for ranges larger than MAX_BLOCK_SIZE as
		// proposed by the BitTorrent spec.
		if (in.high - in.low + 1 > MAX_BLOCK_SIZE) {
			if (LOG.isDebugEnabled())
				LOG.debug("got long request");
			return;
		}

		if (_info.getVerifyingFolder().hasBlock(in.getId())) 
			_requested.add(in);
		
		if (!_requested.isEmpty() && !usingSlot)
			readyForWriting();
	}
	
	/**
	 * Notification that we are now receiving the specified piece
	 * @return true if the piece was requested.
	 */
	boolean startReceivingPiece(BTInterval interval) {
		// its ok to remove the piece from the list of pieces we request
		// because if the receiving fails the connection will be closed.
		if (!_requesting.remove(interval)) {
			if (LOG.isDebugEnabled())
				LOG.debug("received unexpected range " + interval + " from "
						+ _socket.getInetAddress() + " expected "
						+ _requesting);
			return false;
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug(this + " starting to receive piece " + interval);
		return true;
	}
	
	void request() {
		if (LOG.isDebugEnabled())
			LOG.debug("requesting ranges from " + this);
		
		// get new ranges to request if necessary
		while (!_torrent.isComplete() && _torrent.isActive()
				&& _requesting.size() < MAX_REQUESTS) {
			BTInterval in = _info.getVerifyingFolder().leaseRandom(_available, _requesting);
			if (in == null)
				break;
			_requesting.add(in);
			_writer.enqueue(new BTRequest(in));
		}
	}

	/**
	 * handles a piece message and sends its payload to disk
	 */
	public void handlePiece(BTPieceFactory factory) {
		try {
			_info.getVerifyingFolder().writeBlock(factory);
		} catch (IOException ioe) {
			close();
			// inform the user and stop the download
			IOUtils.handleException(ioe, null);
			_torrent.diskExceptionHappened();
			return;
		}
	}

	/**
	 * handles a bitfield and reads in the available pieces contained therein
	 */
	private void handleBitField(BTBitField message) {
		ByteBuffer field = message.getPayload();

		// the number of pieces
		int numBits = _info.getNumBlocks();

		int bitFieldLength = (numBits + 7) / 8;

		if (field.remaining() != bitFieldLength)
			handleIOException(new BadBTMessageException(
					"bad bitfield received! " + _endpoint.toString()));

		boolean willBeInteresting = false;
		for (int i = 0; i < numBits; i++) {
			byte mask = (byte) (0x80 >>> (i % 8));
			if ((mask & field.get(i / 8)) == mask) {
				if (!willBeInteresting && !_info.getVerifyingFolder().hasBlock(i))
					willBeInteresting = true;
				_availableRanges.set(i);
			}
		}
		
		if (_available.cardinality() == numBits) {
			_availableRanges = null;
			_available = _info.getFullBitField();
			numMissing = 0;
		} else
			numMissing = _info.getVerifyingFolder().getNumMissing(_available);
		
		if (willBeInteresting)
			sendInterested();
	}

	/**
	 * handles a have message and adds the available range contained therein
	 */
	private void handleHave(BTHave message) {
		int pieceNum = message.getPieceNum();
		if (_available.get(pieceNum))
			return; // dublicate Have, ignore.
		
		VerifyingFolder v = _info.getVerifyingFolder();
		_availableRanges.set(pieceNum);

		
		// tell the remote host we are interested if we don't have that range
		if (v.hasBlock(pieceNum)) 
			numMissing--;
		else
			sendInterested();
		
		if (_available.cardinality() == _info.getNumBlocks()) {
			_availableRanges = null;
			_available = _info.getFullBitField();
			numMissing = 0;
		}
	}

	public boolean equals(Object o) {
		if (o instanceof BTConnection) {
			BTConnection other = (BTConnection) o;
			return other._endpoint.equals(_endpoint);
		}
		return false;
	}

	public String toString() {
		StringBuilder b = new StringBuilder("("+getHost());
		if (isChoked())
			b.append(" Ced");
		if (isChoking())
			b.append(" Cing");
		if (isInterested())
			b.append(" Ied");
		if (isInteresting())
			b.append(" Iing");
		if (isSeed())
			b.append(" Seed");
		b.append(")");
		return b.toString();
	}


	public String getHost() {
		return _socket.getInetAddress().getHostAddress();
	}
	
	
	public void releaseSlot() {
		NIODispatcher.instance().invokeLater(new Runnable() {
			public void run() {
				usingSlot = false;
				sendChoke();
			}
		});
	}
	
	public void slotAvailable() {
		NIODispatcher.instance().invokeLater(new Runnable() {
			public void run() {
				requestPieceRead();
			}
		});
	}


	public float getAverageBandwidth() {
		return up.getAverageBandwidth();
	}


	public float getMeasuredBandwidth() throws InsufficientDataException {
		return up.getMeasuredBandwidth();
	}


	public void measureBandwidth() {
		up.measureBandwidth();
	}
	
	/**
	 * @return if the remote host has the complete file, i.e. is a "seed".
	 */
	public boolean isSeed() {
		return _available.cardinality() == _info.getNumBlocks();
	}
}
