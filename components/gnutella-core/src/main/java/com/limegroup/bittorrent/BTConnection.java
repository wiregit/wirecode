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
import com.limegroup.gnutella.io.DelayedBufferWriter;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.io.NIOSocket;
import com.limegroup.gnutella.io.ThrottleWriter;
import com.limegroup.bittorrent.statistics.BTMessageStat;
import com.limegroup.bittorrent.messages.*;
import com.limegroup.gnutella.uploader.StalledUploadWatchdog;
import com.limegroup.gnutella.uploader.UploadSlotListener;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.BitSet;

/**
 * Class wrapping a Bittorrent connection.
 */
public class BTConnection implements UploadSlotListener {
	
	private static final Log LOG = LogFactory.getLog(BTConnection.class);

	/**
	 * This is the max size of a block that we will ever request. Requesting
	 * larger ranges is not encouraged by the protocol.
	 */
	private static final int BLOCK_SIZE = 16384;

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

	/**
	 * the Set of BTInterval containing requests that we did not yet send but
	 * which we intend to send soon. 
	 */
	private final Set<BTInterval> _toRequest;

	/**
	 * the Set of BTIntervals we requested but which was not yet satisfied.
	 */
	private final Set<BTInterval> _requesting;

	/**
	 * the Set of BTInterval requested by the remote
	 * host, we avoid queueing up all requested pieces in the writer to
	 * save memory
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
	private SimpleBandwidthTracker up, down;
	
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
		_requesting = new HashSet<BTInterval>();
		_toRequest = new HashSet<BTInterval>();
		_requested = new HashSet<BTInterval>();
		_startTime = System.currentTimeMillis();
		_reader = new BTMessageReader(this);
		_writer = new BTMessageWriter(this);
		
		ThrottleWriter throttle = new ThrottleWriter(_torrent
				.getUploadThrottle());
		delayer = new DelayedBufferWriter(1400, 3000);
		_writer.setWriteChannel(delayer);
		delayer.setWriteChannel(throttle);
		socket.setReadObserver(_reader);
		socket.setWriteObserver(_writer);
		_info = info;

		// connections start choked and not interested
		_isChoked = true;
		_isChoking = true;
		_isInterested = false;
		_isInteresting = false;
		up = new SimpleBandwidthTracker();
		down = new SimpleBandwidthTracker();

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
			watchdog.deactivate();
		}
		_torrent.connectionClosed(this);
	}

	/**
	 * @return the measured bandwidth on this connection for
	 * downloadining or uploading
	 */
	public float getMeasuredBandwidth(boolean read) {
		SimpleBandwidthTracker tracker = read ? down : up;
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
		down.count(read);
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
		if (!_availableRanges.get(pieceNum)) {
			numMissing++;
			_writer.enqueue(have);
		}  

		// we should indicate that we are not interested anymore, so we are
		// not unchoked when we do not want to request anything.
		if (!_info.getVerifyingFolder().containsAnyWeMiss(_availableRanges)) {
			sendNotInterested();
			return;
		}

		// whether we canceled some ranges that were requested or we were
		// about to request
		boolean modified = false;

		// remove all subranges that we may be requesting
		for (Iterator<BTInterval> iter = _requesting.iterator(); iter.hasNext();) {
			BTInterval req = iter.next();
			if (req.getId() == pieceNum) {
				iter.remove();
				sendCancel(req);
				modified = true;
			}
		}
		
		for (Iterator<BTInterval> iter = _toRequest.iterator(); iter.hasNext();) {
			BTInterval req = iter.next();
			if (req.getId() == pieceNum) {
				iter.remove();
				modified = true;
			}
		}

		// if we removed any ranges, choose some more rages to request...
		if (!_torrent.isComplete() && modified)
			request();
	}

	/**
	 * Sends a bitfield message to the remote host.
	 */
	private void sendBitfield() {
		_writer.enqueue(BTBitField.createMessage(_info));
	}

	/**
	 * Requests a piece from the remote host
	 * 
	 * @param in an <tt>BTInterval</tt> specifying the ranges we want to
	 *            request.
	 */
	void sendRequest(BTInterval in) {
		// we do not request any pieces larger than BLOCK_SIZE!
		for (long i = in.low; i < in.high; i += BLOCK_SIZE) {
			// watch out, all Intervals are inclusive on both ends...
			// safe cast, length is always <= BLOCK_SIZE
			int length = (int) Math.min(in.high - i + 1, BLOCK_SIZE);
			BTInterval toReq = new BTInterval(i, i + length - 1,in.getId());
			if (!_requesting.contains(toReq))
				_toRequest.add(toReq);
		}
		enqueueRequests();
	}

	private void sendCancel(BTInterval in) {
		_writer.enqueue(new BTCancel(in));
	}
	
	/**
	 * Cancels all requests. Called when the download is complete
	 */
	void cancelAllRequests() {
		for (BTInterval request : _requesting) 
			sendCancel(request);
		clearRequests();
	}

	void pieceSent() {
		Assert.that(usingSlot, "incosistent slot state");
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
		
		// pick a request at sort-of-random
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
		for (BTInterval clear : _toRequest)
			clearRequest(clear);
		
		for (BTInterval clear : _requesting)
			clearRequest(clear);

		_toRequest.clear();

		_requesting.clear();
	}

	/**
	 * private utility method clearing a certain range
	 * 
	 * @param in an <tt>BTInterval</tt> representing the range to clear.
	 */
	private void clearRequest(BTInterval in) {
		_info.getVerifyingFolder().releaseInterval(in);
	}

	/*
	 * private helper trying to ensure there are exactly MAX_REQUESTS open 
	 * requests sent to the remote host all the time. Gets more ranges to 
	 * request from the ManagedTorrent if necessary
	 */
	private void enqueueRequests() {
		// the reason we randomize the list of requests to be sent is that we
		// are receiving far too many ranges multiple times when the download
		// is about to finish.
		List<BTInterval> random = new ArrayList<BTInterval>();
		random.addAll(_toRequest);
		Collections.shuffle(random);
		for (Iterator<BTInterval> iter = random.iterator(); _requesting.size() < MAX_REQUESTS
		&& iter.hasNext() && !_isChoking;) {
			BTInterval toReq = iter.next();
			_writer.enqueue(new BTRequest(toReq));
			_toRequest.remove(toReq);
			_requesting.add(toReq);
		}
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
			_requesting.clear();
			break;

		case BTMessage.UNCHOKE:
			_isChoking = false;
			if (_isInteresting) 
				requestIfPossible();
			break;

		case BTMessage.INTERESTED:
			_isInterested = true;
			if (!_isChoked)
				_torrent.rechoke();
			break;

		case BTMessage.NOT_INTERESTED:
			_isInterested = false;
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
		if (!_requesting.remove(interval) && !_toRequest.remove(interval)) {
			if (LOG.isDebugEnabled())
				LOG.debug("received unexpected range " + interval + " from "
						+ _socket.getInetAddress() + " expected "
						+ _requesting + " " + _toRequest);
			return false;
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug(this + " starting to receive piece " + interval);
		return true;
	}
	
	void requestIfPossible() {
		// get new ranges to request if necessary
		if (!_torrent.isComplete()
				&& _toRequest.size() + _requesting.size() < MAX_REQUESTS)
			request();
		
		// send next request upon receiving piece.
		enqueueRequests();
	}

	private void request() {
		// don't request if complete
		if (_torrent.isComplete() || !_torrent.isActive())
			return;
		
		if (LOG.isInfoEnabled())
			LOG.info("requesting ranges from " + this);
		
		BTInterval in = _info.getVerifyingFolder().leaseRandom(_availableRanges, _requesting);
		if (in != null)
			sendRequest(in);
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
		
		if (_availableRanges.cardinality() == numBits) {
			_availableRanges = _info.getFullBitSet();
			numMissing = 0;
		} else
			numMissing = _info.getVerifyingFolder().getNumMissing(_availableRanges);
		
		if (willBeInteresting)
			sendInterested();
	}

	/**
	 * handles a have message and adds the available range contained therein
	 */
	private void handleHave(BTHave message) {
		int pieceNum = message.getPieceNum();
		if (_availableRanges.get(pieceNum))
			return; // dublicate Have, ignore.
		
		VerifyingFolder v = _info.getVerifyingFolder();
		_availableRanges.set(pieceNum);

		
		// tell the remote host we are interested if we don't have that range
		if (v.hasBlock(pieceNum)) 
			numMissing--;
		else
			sendInterested();
		
		if (_availableRanges.cardinality() == _info.getNumBlocks()) {
			_availableRanges = _info.getFullBitSet();
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
		usingSlot = false;
		close();
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
		return _availableRanges.cardinality() == _info.getNumBlocks();
	}
}
