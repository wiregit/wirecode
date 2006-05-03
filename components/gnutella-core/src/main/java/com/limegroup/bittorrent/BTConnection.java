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

import com.limegroup.gnutella.io.DelayedBufferWriter;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.io.NIOSocket;
import com.limegroup.gnutella.io.ThrottleWriter;
import com.limegroup.bittorrent.statistics.BTMessageStat;
import com.limegroup.bittorrent.messages.*;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.BitSet;

/**
 * Class wrapping a Bittorrent connection.
 */
public class BTConnection {
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
	private static final int MAX_REQUESTS = 4;

	/**
	 * connections that die after less than a minute won't be retried
	 */
	private static final long MIN_RETRYABLE_LIFE_TIME = 60 * 1000;

	/*
	 * the NIOSocket
	 */
	private final NIOSocket _socket;

	/*
	 * Reader for the messages
	 */
	private BTMessageReader _reader;

	/*
	 * Writer for the messages
	 */
	private BTMessageWriter _writer;

	/**
	 * The pieces the remote host has
	 */
	private final BitSet _availableRanges;

	/**
	 * the Set of BTInterval containing requests that we did not yet send but
	 * which we intend to send soon. 
	 */
	private final Set _toRequest;

	/**
	 * the Set of BTIntervals we requested but which was not yet satisfied.
	 */
	private final Set _requesting;

	/**
	 * the Set of BTInterval requested by the remote
	 * host, we avoid queueing up all requested pieces in the writer to
	 * save memory
	 */
	private final Set _requested;

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
		_requesting = new HashSet();
		_toRequest = new HashSet();
		_requested = new HashSet();
		_startTime = System.currentTimeMillis();
		_reader = new BTMessageReader(this);
		_writer = new BTMessageWriter(this);
		
		ThrottleWriter throttle = new ThrottleWriter(_torrent
				.getUploadThrottle());
		DelayedBufferWriter delayer = new DelayedBufferWriter(1400);
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
		try {
			_socket.shutdownOutput();
		} catch (IOException ioe1) {}
		try {
			_socket.shutdownInput();
		} catch (IOException ioe2) {}
		
		_reader.shutdown();
		_writer.shutdown();
		
		try {
			_socket.close();
		} catch (IOException ioe) {}
		
		_torrent.connectionClosed(this);
	}

	/**
	 * @return the measured bandwidth on this connection for
	 * downloadining or uploading
	 */
	public float getMeasuredBandwidth(boolean read) {
		return read ? _reader.getBandwidth() : _writer.getBandwidth();
	}

	/**
	 * notification that some bytes have been read on this connection
	 */
	private void readBytes(int read) {
		_torrent.getDownloader().readBytes(read);
	}

	/**
	 * notification that some bytes have been written on this connection
	 */
	public void wroteBytes(int written) {
		_torrent.getUploader().wroteBytes(written);
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
			_writer.enqueue(BTChoke.createMessage());
			_isChoked = true;
		}
	}

	/**
	 * Unchokes the connection
	 */
	void sendUnchoke(int now) {
		if (_isChoked) {
			_writer.enqueue(BTUnchoke.createMessage());
			setUnchokeRound(now);
			_isChoked = false;
		}
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
	void sendInterested() {
		if (!_isInteresting) {
			LOG.debug("sending interested message");
			_writer.enqueue(BTInterested.createMessage());
			_isInteresting = true;
		} 
	}

	/**
	 * Informs the remote we are not interested in downloading.
	 */
	void sendNotInterested() {
		if (_isInteresting) {
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
		if (_info.getVerifyingFolder().getNumWeMiss(_availableRanges) == 0)
			sendNotInterested();

		// whether we canceled some ranges that were requested or we were
		// about to request
		boolean modified = false;

		// remove all subranges that we may be requesting
		for (Iterator iter = _requesting.iterator(); iter.hasNext();) {
			BTInterval req = (BTInterval) iter.next();
			if (req.getId() == pieceNum) {
				iter.remove();
				sendCancel(req);
				modified = true;
			}
		}
		
		for (Iterator iter = _toRequest.iterator(); iter.hasNext();) {
			BTInterval req = (BTInterval) iter.next();
			if (req.getId() == pieceNum) {
				iter.remove();
				modified = true;
			}
		}

		// if we removed any ranges, choose some more rages to request...
		// TODO: add some limiting of the number of downloads here
		if (!_torrent.isComplete() && modified && _isInteresting
				&& _toRequest.isEmpty())
			_torrent.request(this);
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
		Iterator iter = _requesting.iterator();
		while (iter.hasNext()) {
			BTInterval request = (BTInterval) iter.next();
			sendCancel(request);
		}
		clearRequests();
	}

	/**
	 * notifies this, that the connection is ready to write the next chunk of
	 * the torrent
	 */
	void readyForWriting() {
		if (_isChoked || _requested.isEmpty()) {
			if (LOG.isDebugEnabled())
				LOG.debug("cannot write while choked, requested size is "
						+ _requested.size());
			return;
		}
		
		// pick a request at sort-of-random
		Iterator iter = _requested.iterator();
		BTInterval in = (BTInterval) iter.next();
		iter.remove();
		
		if (LOG.isDebugEnabled())
			LOG.debug("requesting disk read for "+in);
		
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
				_writer.enqueue(new BTPiece(in, data));
			}
		};
		NIODispatcher.instance().invokeLater(pieceSender);
	}
	
	/**
	 * Accessor for the BitSet of available ranges
	 * 
	 * @return BitSet containing all ranges the remote host offers
	 */
	BitSet getAvailableRanges() {
		return _availableRanges;
	}

	/**
	 * Adds a piece to the list of available pieces and marks the
	 * connection interesting if we do not have this piece ourselves
	 * 
	 * @param pieceNum the piece number that is available
	 */
	private void addAvailablePiece(int pieceNum) {
		VerifyingFolder v = _info.getVerifyingFolder();
		_availableRanges.set(pieceNum);

		
		// tell the remote host we are interested if we don't have that range
		if (v.hasBlock(pieceNum)) 
			numMissing--;
		else
			sendInterested();
	}

	private void clearRequests() {
		for (Iterator iter = _toRequest.iterator(); iter.hasNext();)
			clearRequest((BTInterval) iter.next());

		for (Iterator iter = _requesting.iterator(); iter.hasNext();)
			clearRequest((BTInterval) iter.next());

		_toRequest.clear();

		_requesting.clear();
	}

	/**
	 * private utility method clearing a certain range
	 * 
	 * @param in an <tt>BTInterval</tt> representing the range to clear.
	 */
	private void clearRequest(BTInterval in) {
		_info.getVerifyingFolder().releaseChunk(in.getId());
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
		List random = new ArrayList();
		random.addAll(_toRequest);
		Collections.shuffle(random);
		for (Iterator iter = random.iterator(); _requesting.size() < MAX_REQUESTS
		&& iter.hasNext() && !_isChoking;) {
			BTInterval toReq = (BTInterval) iter.next();
			if (!_writer.enqueue(new BTRequest(toReq)))
				return;
			_toRequest.remove(toReq);
			_requesting.add(toReq);
		}
	}

	/**
	 * @param message the incoming message to process.
	 */
	public void processMessage(BTMessage message) {
		switch (message.getType()) {
		case BTMessage.CHOKE:
			_isChoking = true;
			break;

		case BTMessage.UNCHOKE:
			_isChoking = false;
			if (_isInteresting) {
				// try sending next request as soon as we are unchoked
				enqueueRequests();
				// get new ranges to request if necessary
				if (!_torrent.isComplete() && _toRequest.isEmpty())
					_torrent.request(this);
			}
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
		case BTMessage.PIECE:
			handlePiece((BTPiece) message);
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
		for (Iterator iter = _requested.iterator(); iter.hasNext();) {
			BTInterval current = (BTInterval) iter.next();
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
		if (_isChoked) {
			LOG.debug("got request while choked");
			return;
		}

		BTInterval in = message.getInterval();
		if (LOG.isDebugEnabled())
			LOG.debug("got request for " + in);

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
		
		if (!_requested.isEmpty() && _writer.isIdle())
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
		return true;
	}

	/**
	 * handles a piece message and sends its payload to disk
	 */
	private void handlePiece(BTPiece message) {
		final BTInterval in = message.getInterval();
		final byte[] data = message.getData();
		
		readBytes(data.length);
		
		
		try {
			VerifyingFolder v = _info.getVerifyingFolder();
			if (v.hasBlock(in.getId()))
				return;
			
			_info.getVerifyingFolder().writeBlock(
					in,
					data);
		} catch (IOException ioe) {
			close();
			// inform the user and stop the download
			IOUtils.handleException(ioe, null);
			_torrent.stop();
			return;
		}
		
		// get new ranges to request if necessary
		if (!_torrent.isComplete()
				&& _toRequest.size() + _requesting.size() < MAX_REQUESTS)
			_torrent.request(this);
		
		// send next request upon receiving piece.
		enqueueRequests();
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

		for (int i = 0; i < numBits; i++) {
			byte mask = (byte) (0x80 >>> (i % 8));
			if ((mask & field.get(i / 8)) == mask) {
				addAvailablePiece(i);
			}
		}
		
		numMissing = _info.getVerifyingFolder().getNumMissing(_availableRanges);
	}

	/**
	 * handles a have message and adds the available range contained therein
	 */
	private void handleHave(BTHave message) {
		addAvailablePiece(message.getPieceNum());
	}

	public boolean equals(Object o) {
		if (o instanceof BTConnection) {
			BTConnection other = (BTConnection) o;
			return other._endpoint.equals(_endpoint);
		}
		return false;
	}

	public String toString() {
		return _endpoint.toString();
	}
	
}
