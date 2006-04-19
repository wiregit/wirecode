package com.limegroup.bittorrent;

import java.io.IOException;
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
 * Class wrapping a Bittorrent connection. This class is not thread-safe.
 */
public class BTConnection {
	private static final Log LOG = LogFactory.getLog(BTConnection.class);

	/*
	 * This is the max size of a block that we will ever request. Requesting
	 * larger ranges is not encouraged by the protocol.
	 */
	private static final int BLOCK_SIZE = 16384;

	/*
	 * This is the max size of a block that we will ever upload, requests larger
	 * than this are dropped.
	 */
	private static final int MAX_BLOCK_SIZE = 64 * 1024;

	/*
	 * the number of requests to send to any host without waiting for reply
	 */
	private static final int MAX_REQUESTS = 4;

	/*
	 * connections that die after less than a minute won't be retried
	 */
	private static final long MIN_RETRYABLE_LIFE_TIME = 60 * 1000;

	/*
	 * the NIOSocket
	 */
	private final NIOSocket _socket;

	/*
	 * this stores the incoming data in a buffer to be read later
	 */
	private BTMessageReader _reader;

	/*
	 * this stores the outgoing data for writing
	 */
	private BTMessageWriter _writer;

	/*
	 * The ranges the remote host offers, always lock on this before accessing
	 */
	private final BitSet _availableRanges;

	/*
	 * the List of LongInterval containing requests that we did not yet send but
	 * which we intend to send soon. Lock on this before accessing
	 */
	private final Set _toRequest;

	/*
	 * the LongInterval we requested but which was not yet satisfied.
	 */
	private final Set _requesting;

	/*
	 * the List of BTInterval containing the ranges requested by the remote
	 * host, we avoid queueing up all requested pieces in the MESSAGE_QUEUE to
	 * save memory
	 */
	private final Set _requested;

	/*
	 * the metaInfo of this torrent
	 */
	private final BTMetaInfo _info;

	/*
	 * the id of the remote client
	 */
	private final TorrentLocation _endpoint;

	/*
	 * whether or not this is an outgoing connection
	 */
	private final boolean _outgoing;

	/*
	 * our torrent
	 */
	private final ManagedTorrent _torrent;

	/*
	 * whether we choke them: if we are choking, all requests from the remote
	 * host will be ignored
	 */
	private boolean _isChoked;

	/*
	 * whether they choke us: only send requests if they are not choking us
	 */
	private volatile boolean _isChoking;

	/*
	 * Indicates whether the remote host is interested in one of the ranges we
	 * offer.
	 */
	private boolean _isInterested;

	/*
	 * Indicates whether or not the remote host offers ranges we want
	 */
	private volatile boolean _isInteresting;

	/*
	 * the time when this Connection was created
	 */
	private final long _startTime;
	
	/**
	 * The # of pieces the remote host is missing.
	 */
	private int numMissing;
	

	/**
	 * Constructs instance of this
	 * 
	 * @param sock
	 *            the Socket to the remote host. We assume that the Bittorrent
	 *            connection is already initialized and the headers were
	 *            exchanged successfully
	 * @param info
	 *            the BTMetaInfo holding all information for this torrent
	 * @param peerId
	 *            the array of byte containing the 20 byte peer id of the remote
	 *            host
	 */
	public BTConnection(NIOSocket sock, BTMetaInfo info, TorrentLocation ep,
			ManagedTorrent torrent) {
		this(sock, info, ep, torrent, false);
	}

	/**
	 * Constructs instance of this
	 * 
	 * @param sock
	 *            the Socket to the remote host. We assume that the Bittorrent
	 *            connection is already initialized and the headers were
	 *            exchanged successfully
	 * @param info
	 *            the BTMetaInfo holding all information for this torrent
	 * @param peerId
	 *            the array of byte containing the 20 byte peer id of the remote
	 *            host
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
		
		// we put the throttle on top, this puts the delayer on top
		// TODO: decide is this what we want?  maybe
		ThrottleWriter throttle = new ThrottleWriter(_torrent
				.getUploadThrottle());
		throttle.setWriteChannel(new DelayedBufferWriter(1400));
		_writer.setWriteChannel(throttle);
		socket.setReadObserver(_reader);
		socket.setWriteObserver(_writer);
		_info = info;

		// connections start choked and not interested
		_isChoked = true;
		_isChoking = true;
		_isInterested = false;
		_isInteresting = false;

		// always send this as the first message
		sendBitfield();
	}

	// PUBLIC INTERFACE

	public boolean isChoked() {
		return _isChoked;
	}
	
	/**
	 * Accessor, returning true if the remote host is choking us.
	 */
	public boolean isChoking() {
		return _isChoking;
	}

	/**
	 * Accessor for whether or not the remote host is interested in the content
	 * we offer.
	 * 
	 * @return true if the remote host is interested, false if not.
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
	 * Accessor for the _interesting attribute
	 * 
	 * @return true if the remote host may have ranges that want to download,
	 *         false if not
	 */
	public boolean isInteresting() {
		return _isInteresting;
	}
	
	public boolean hasRequested() {
		return !_requested.isEmpty();
	}

	/**
	 * @return true if we initiated this connection
	 */
	public boolean isOutgoing() {
		return _outgoing;
	}

	public boolean isWorthRetrying() {
		// don't retry connections that were aborted immediately after starting
		// them, they were most likely terminated for a reason...
		return System.currentTimeMillis() - _startTime > MIN_RETRYABLE_LIFE_TIME;
	}

	/**
	 * Accessor for the <tt>Endpoint</tt> we are connected to
	 * 
	 * @return <tt>TorrentLocation</tt>
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
	 * @return the BTMessageReader in use
	 */
	public BTMessageReader getMessageReader() {
		return _reader;
	}

	/**
	 * @return true if the socket is still connected.
	 */
	public boolean isConnected() {
		return _socket.isConnected();
	}

	public void readBytes(int read) {
		_torrent.getDownloader().readBytes(read);
	}

	public void wroteBytes(int written) {
		_torrent.getUploader().wroteBytes(written);
	}

	/**
	 * Handles IOExceptions for this connection
	 * 
	 * @param iox
	 *            the exception to handle
	 */
	public void handleIOException(IOException iox) {
		if (iox instanceof BadBTMessageException)
			BTMessageStat.INCOMING_BAD.incrementStat();
		if (LOG.isDebugEnabled())
			LOG.debug(iox);
		close();
	}

	/**
	 * processes message off-thread.
	 * 
	 * @param message
	 *            the incoming BTMessage to handle
	 */
	public void processMessage(final BTMessage message) {
		if (LOG.isDebugEnabled())
			LOG.debug("received message: " + message + " from " + _endpoint);
		handleMessage(message);

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
	void sendUnchoke() {
		if (_isChoked) {
			_writer.enqueue(BTUnchoke.createMessage());
			_isChoked = false;
		}
	}

	/**
	 * Indicates that we are interested in downloading pieces. Sets
	 * _isInteresting to true
	 */
	void sendInterested() {
		if (!_isInteresting) {
			LOG.debug("sending interested message");
			_writer.enqueue(BTInterested.createMessage());
			_isInteresting = true;
		} 
	}

	/**
	 * Indicates that the remote host does not have any pieces we would want to
	 * download. Sets _isInteresting to false.
	 */
	void sendNotInterested() {
		if (_isInteresting) {
			_writer.enqueue(BTNotInterested.createMessage());
			_isInteresting = false;
		}
	}

	/**
	 * Tells the remote host, that we have a new piece. IMPORTANT: make sure to
	 * call this method only for complete pieces.
	 * 
	 * @param have
	 *            the <tt>BTHave</tt> message representing a complete piece.
	 */
	void sendHave(BTHave have) {
		int pieceNum = have.getPieceNum();
		// As a minor optimization we will not inform the remote host of any
		// pieces that it already has
		// Else, simply remove the chunk from the available ranges, to optimize
		// requesting ranges...
		if (!_availableRanges.get(pieceNum)) {
			numMissing++;
			_writer.enqueue(have);
		} else
			_availableRanges.clear(pieceNum);

		// we should indicate that we are not interested anymore, so we are
		// not unchoked when we do not want to request anything.
		if (_availableRanges.isEmpty())
			sendNotInterested();

		// whether we canceled some requested ranges
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
		if (!_torrent.isComplete() && modified && _isInteresting
				&& _toRequest.isEmpty())
			_torrent.request(this);
	}

	/**
	 * Sends a bitfield message to the remote host, this will be done only once
	 * after the connection was created.
	 */
	void sendBitfield() {
		_writer.enqueue(BTBitField.createMessage(_info));
	}

	/**
	 * Requests a piece from the remote host
	 * 
	 * @param in
	 *            an <tt>LongInterval</tt> specifying the ranges we want to
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
		if (_isChoked || _requested.size() <= 0) {
			if (LOG.isDebugEnabled())
				LOG.debug("cannot write while choked, requested size is "
						+ _requested);
			return;
		}
		
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
	 * Accessor for the LongIntervalSet of available ranges
	 * 
	 * @return LongIntervalSet containing all ranges the remote host offers
	 */
	BitSet getAvailableRanges() {
		return _availableRanges;
	}

	
	
	/**
	 * Adds a range to the list of available ranges and resets _isInteresting to
	 * true if we do not have this range ourselves
	 * 
	 * @param pieceNum
	 *            the piece number that is available
	 */
	private void addAvailablePiece(int pieceNum) {
		VerifyingFolder v = _info.getVerifyingFolder();
		_availableRanges.set(pieceNum);

		
		// tell the remote host we are interested if we don't have that range!
		if (v.hasBlock(pieceNum)) 
			numMissing--;
		else
			sendInterested();
	}

	/**
	 * private utility method clearing all requested ranges and informing the
	 * VerifyingFolder that these ranges can now be requested again.
	 */
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
	 * @param in
	 *            an <tt>LongInterval</tt> representing the range to clear.
	 */
	private void clearRequest(BTInterval in) {
		_info.getVerifyingFolder().releaseChunk(in.getId());
	}

	/**
	 * private helper trying to ensure there are exactly two open requests sent
	 * to the remote host all the time. Gets more ranges to request from the
	 * ManagedTorrent if necessary
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
	 * @param message the incoming message
	 */
	private void handleMessage(BTMessage message) {
		switch (message.getType()) {
		case BTMessage.CHOKE:
			// we queue all requests up again, maybe we get unchoked
			// again, - if not this will be resolved in endgame mode
			for (Iterator iter = _requesting.iterator(); iter.hasNext();) {
				_toRequest.add(iter.next());
				iter.remove();
			}
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
			// connections that aren't interested any more are choked
			// instantly
			sendChoke();
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
	 * This method handles a cancel message
	 * 
	 * @param payload
	 *            the array of byte containing the payload of the message
	 */
	private void handleCancel(BTCancel message) {
		BTInterval in = message.getInterval();
		
		// removes the range from the list of requests. If we are already
		// sending the piece, there is nothing we can do about it
		for (Iterator iter = _requested.iterator(); iter.hasNext();) {
			BTInterval current = (BTInterval) iter.next();
			if (in.getId() == current.getId() &&
					(in.low <= current.high && current.low <= in.high))
				iter.remove();
		}
	}

	/**
	 * Parses and handles a request message
	 * 
	 * @param payload
	 *            the array of byte containing the payload of the message
	 * @throws IOException
	 */
	private void handleRequest(BTRequest message) {
		// we do not accept request from choked connections, if we just choked
		// a connection, we may still receive some requests, though
		if (_isChoked) {
			if (LOG.isDebugEnabled())
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
		// we skip all requests for ranges larger than MAX_BLOCK_SIZED as
		// proposed by the BitTorrent spec.
		if (in.high - in.low + 1 > MAX_BLOCK_SIZE) {
			if (LOG.isDebugEnabled())
				LOG.debug("got long request");
			return;
		}

		
		// skip the message if we don't have that range
		if (_info.getVerifyingFolder().hasBlock(in.getId())) 
			_requested.add(in);
		
		if (!_requested.isEmpty() && _writer.isIdle())
			readyForWriting();
	}

	/**
	 * handles a piece message and writes its payload to disk
	 * 
	 * @param payload
	 *            the byte array containing the payload of the message
	 * @throws IOException
	 *             if there was a problem writing to disk
	 */
	private void handlePiece(BTPiece message) {
		final BTInterval in = message.getInterval();
		final byte[] data = message.getData();
		
		readBytes(data.length);
		
		if (!_requesting.remove(in) && !_toRequest.remove(in)) {
			if (LOG.isDebugEnabled())
				LOG.debug("received unexpected range " + in + " from "
						+ _socket.getInetAddress() + " expected "
						+ _requesting + " " + _toRequest);
			return;
		}
		try {
			VerifyingFolder v = _info.getVerifyingFolder();
			if (v.hasBlock(in.getId()))
				return;
			
			_info.getVerifyingFolder().writeBlock(
					in,
					data);
		} catch (IOException ioe) {
			// inform the user and stop the download
			IOUtils.handleException(ioe, null);
			_torrent.stop();
		}
		
		// get new ranges to request if necessary
		if (!_torrent.isComplete()
				&& _toRequest.size() + _requesting.size() < MAX_REQUESTS)
			_torrent.request(this);
		
		// send next request upon receiving piece.
		enqueueRequests();
	}

	/**
	 * handles a bitfield and reads in the available ranges contained therein
	 * 
	 * @param field
	 *            the byte array containing the payload of the bitfield message
	 */
	private void handleBitField(BTBitField message) {
		byte[] field = message.getBitField();

		// the number of pieces
		int numBits = _info.getNumBlocks();

		int bitFieldLength = (numBits + 7) / 8;

		if (field.length != bitFieldLength)
			handleIOException(new BadBTMessageException(
					"bad bitfield received! " + _endpoint.toString()));

		for (int i = 0; i < numBits; i++) {
			byte mask = (byte) (0x80 >>> (i % 8));
			if ((mask & field[i / 8]) == mask) {
				//TODO: do not send interested until all are added.
				addAvailablePiece(i);
			}
		}
		
		numMissing = _info.getVerifyingFolder().getNumMissing(_availableRanges);
	}

	/**
	 * handles a have message and adds the available range contained therein
	 * 
	 * @param payload
	 *            the byte array carrying the payload of the message
	 */
	private void handleHave(BTHave message) {
		int pieceNum = message.getPieceNum();
		addAvailablePiece(pieceNum);
	}

	// overriding Object
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
