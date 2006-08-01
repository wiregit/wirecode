package com.limegroup.bittorrent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.io.AbstractNBSocket;
import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.ThrottleReader;
import com.limegroup.bittorrent.statistics.BTMessageStat;
import com.limegroup.bittorrent.messages.*;
import com.limegroup.gnutella.uploader.UploadSlotListener;
import com.limegroup.gnutella.util.BitField;
import com.limegroup.gnutella.util.BitFieldSet;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.BitSet;

/**
 * Class wrapping a Bittorrent connection.
 */
public class BTConnection 
implements UploadSlotListener, BTMessageHandler, BTLink,
PieceSendListener, PieceReadListener {
	
	private static final Log LOG = LogFactory.getLog(BTConnection.class);

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
	 * the NBSocket we're using
	 */
	private AbstractNBSocket _socket;

	/*
	 * Reader for the messages
	 */
	private final ChannelReadObserver _reader;

	/*
	 * Writer for the messages
	 */
	private final BTChannelWriter _writer;

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
	final Set<BTInterval> _requested;

	/**
	 * the metaInfo of this torrent
	 */
	private final BTMetaInfo _info;

	/**
	 * the id of the remote client
	 */
	private final TorrentLocation _endpoint;

	/*
	 * our torrent
	 */
	private ManagedTorrent _torrent;

	/**
	 * whether we choke them: if we are choking, all requests from the remote
	 * host will be ignored
	 */
	boolean _isChoked;

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
	private long _startTime;
	
	/**
	 * The # of pieces the remote host is missing.
	 */
	private int numMissing;
	
	/**
	 * The # of the round this connection was unchoked last time.
	 */
	int unchokeRound;
	
	/** Whether this connection is currently using an upload slot */
	private volatile boolean usingSlot;
	
	/** Bandwidth trackers for the outgoing and incoming bandwidth */
	private SimpleBandwidthTracker up, downShort, downLong;
	
	/** Whether this connection is currently closing */
	private volatile boolean closing;
	
	/** Cached runnables for handling of slot-related events */
	private Runnable slotReleaser, slotNotifier;
	
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
	 */
	public BTConnection(BTMetaInfo info, TorrentLocation ep) {
		_endpoint = ep;
		_availableRanges = new BitSet(info.getNumBlocks());
		_available = new BitFieldSet(_availableRanges, info.getNumBlocks());
		_requesting = new HashSet<BTInterval>();
		_requested = new HashSet<BTInterval>();
		_info = info;

		// connections start choked and not interested
		_isChoked = true;
		_isChoking = true;
		_isInterested = false;
		_isInteresting = false;
		up = new SimpleBandwidthTracker();
		downShort = new SimpleBandwidthTracker(1000);
		downLong = new SimpleBandwidthTracker(5000);
		
		_writer = new BTMessageWriter(this, this);
		_reader = new BTMessageReader(this, this);

	}

	/**
	 * Initializes the connection 
	 */
	public void init(AbstractNBSocket socket, 
			ManagedTorrent torrent) {
		// if we were shutdown before initializing, return.
		if (closing)
			return;
		
		_socket = socket;
		_torrent = torrent;
		_startTime = System.currentTimeMillis();
		
		_writer.init(torrent.getScheduler());
		
		ThrottleReader readThrottle = new ThrottleReader(
				RouterService.getBandwidthManager().getThrottle(true));
		_reader.setReadChannel(readThrottle);
		readThrottle.interest(true);
		_socket.setReadObserver(_reader);
		_socket.setWriteObserver(_writer);
		
		// if we have downloaded anything send a bitfield
		if (_info.getDiskManager().getVerifiedBlockSize() > 0)
			sendBitfield();
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Chokable#isChoked()
	 */
	public boolean isChoked() {
		return _isChoked;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Chokable#isChoking()
	 */
	public boolean isChoking() {
		return _isChoking;
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Chokable#isInterested()
	 */
	public boolean isInterested() {
		return _isInterested;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Chokable#shouldBeInterested()
	 */
	public boolean shouldBeInterested() {
		return numMissing > 0;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Chokable#isInteresting()
	 */
	public boolean isInteresting() {
		return _isInteresting;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.BTLink#isWorthRetrying()
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
	private void close() {
		if (closing) 
			return;
		closing = true;
		
		// if not initialized just return
		if (_socket == null)
			return; 
		
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
		cancelSlotRequest();
		
		_torrent.connectionClosed(this);
	}
	
	private void cancelSlotRequest() {
		if (usingSlot) {
			if (LOG.isDebugEnabled())
				LOG.debug(this+" cancelling slot request");
			RouterService.getUploadSlotManager().cancelRequest(this);
		}
		usingSlot = false;
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Chokable#getMeasuredBandwidth(boolean, boolean)
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
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.BTMessageHandler#readBytes(int)
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
		shutdown();
	}
	
	public void shutdown() {
		close();
	}

	/**
	 * Chokes the connection
	 */
	public void choke() {
		_requested.clear();
		if (!_isChoked) {
			if (LOG.isDebugEnabled())
				LOG.debug(this+" choking");
			cancelSlotRequest();
			_writer.enqueue(BTChoke.createMessage());
			_isChoked = true;
		} 
	}

	
	/**
	 * Unchokes the connection
	 * @param now the unchoking round.
	 */
	public void unchoke(int now) {
		unchokeRound = now;
		if (_isChoked) {
			if (LOG.isDebugEnabled())
				LOG.debug(this +" unchoking, round "+now);
			_writer.enqueue(BTUnchoke.createMessage());
			_isChoked = false;
		}
	}
	
	/**
	 * @return the round during which the connection was last unchoked
	 */
	public int getUnchokeRound() {
		return unchokeRound;
	}
	
	/**
	 * sets the round during which the connection was choked
	 */
	public void clearUnchokeRound() {
		unchokeRound = -1;
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
		cancelAllRequests();
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
	public void sendHave(BTHave have) {
		int pieceNum = have.getPieceNum();
		
		// As a minor optimization we will not inform the remote host of any
		// pieces that it already has
		if (!_available.get(pieceNum)) {
			numMissing++;
			_writer.enqueue(have);
		}  

		// we should indicate that we are not interested anymore, so we are
		// not unchoked when we do not want to request anything.
		if (!_info.getDiskManager().containsAnyWeMiss(_available)) {
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
	private void cancelAllRequests() {
		for (BTInterval request : _requesting) 
			sendCancel(request);
		clearRequests();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.PieceSendListener#pieceSent()
	 */
	public void pieceSent() {
		if (LOG.isDebugEnabled())
			LOG.debug(this+" piece sent");
		usingSlot = false;
		RouterService.getUploadSlotManager().requestDone(this);
		readyForWriting();
	}
	
	/**
	 * notifies this, that the connection is ready to write the next chunk of
	 * the torrent
	 */
	private void readyForWriting() {
		if (_isChoked || _requested.isEmpty()) 
			return;
		
		usingSlot = true;
		int proceed = RouterService.getUploadSlotManager().requestSlot(
					this,
					!_torrent.isComplete());
		
		if (proceed == -1) { // denied, choke the connection
			usingSlot = false;
			choke();
		} else if (proceed == 0) 
			beginPieceSend();
		// else queued, will receive callback.
	}
	
	private void beginPieceSend() {
		if (_isChoked || _requested.isEmpty()) 
			return;
		
		// pick a request from them
		Iterator<BTInterval> iter = _requested.iterator();
		BTInterval in = iter.next();
		iter.remove();
		
		if (LOG.isDebugEnabled())
			LOG.debug(this+" requesting disk read for "+in);
		
		try {
			_info.getDiskManager().requestPieceRead(in, this);
		} catch (IOException bad) {
			close();
		}
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.PieceReadListener#pieceRead(com.limegroup.bittorrent.BTInterval, byte[])
	 */
	public void pieceRead(final BTInterval in, final byte [] data) {
		RouterService.getBandwidthManager().applyUploadRate();
		Runnable pieceSender = new Runnable() {
			public void run() {
				if (LOG.isDebugEnabled())
					LOG.debug("disk read done for "+in);
				_writer.enqueue(new BTPieceMessage(in, data));
			}
		};
		_torrent.getScheduler().invokeLater(pieceSender);
	}
	
	
	private void clearRequests() {
		for (BTInterval clear : _requesting)
			_info.getDiskManager().releaseInterval(clear);
		_requesting.clear();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.BTMessageHandler#processMessage(com.limegroup.bittorrent.messages.BTMessage)
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

		if (_info.getDiskManager().hasBlock(in.getId())) 
			_requested.add(in);
		
		if (!_requested.isEmpty() && !usingSlot)
			readyForWriting();
	}
	
	/**
	 * Notification that we are now receiving the specified piece
	 * @return true if the piece was requested.
	 */
	public boolean startReceivingPiece(BTInterval interval) {
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
	
	public void finishReceivingPiece() {
		request();
	}
	
	void request() {
		if (LOG.isDebugEnabled())
			LOG.debug("requesting ranges from " + this);
		
		// if we still have more than one outstanding request, wait for them
		if (_requesting.size() > 1) 
			return;
		
		// get new ranges to request if necessary
		while (!_torrent.isComplete() && _torrent.isActive()
				&& _requesting.size() < MAX_REQUESTS) {
			BTInterval in = _info.getDiskManager().leaseRandom(_available, _requesting);
			if (in == null)
				break;
			_requesting.add(in);
			_writer.enqueue(new BTRequest(in));
		}
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.BTMessageHandler#handlePiece(com.limegroup.bittorrent.BTPieceFactory)
	 */
	public void handlePiece(BTPieceFactory factory) {
		try {
			_info.getDiskManager().writeBlock(factory);
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
				if (!willBeInteresting && !_info.getDiskManager().hasBlock(i))
					willBeInteresting = true;
				_availableRanges.set(i);
			}
		}
		
		if (_available.cardinality() == numBits) {
			_availableRanges = null;
			_available = _info.getFullBitField();
			numMissing = 0;
		} else
			numMissing = _info.getDiskManager().getNumMissing(_available);
		
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
		
		TorrentDiskManager v = _info.getDiskManager();
		_availableRanges.set(pieceNum);

		
		// tell the remote host we are interested if we don't have that range
		if (v.hasBlock(pieceNum)) 
			numMissing--;
		else
			sendInterested();
		
		if (_available.cardinality() == _info.getNumBlocks()) {
			if (LOG.isDebugEnabled())
				LOG.debug(this+" now has everything");
			_availableRanges = null;
			_available = _info.getFullBitField();
			numMissing = 0;
			if (_torrent.isComplete()) // we're also seed - goodbye
				shutdown();
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
		StringBuilder b = new StringBuilder(_socket == null? "new" : "("+getHost());
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
		if (usingSlot)
			b.append(" U");
		b.append(")");
		return b.toString();
	}


	public String getHost() {
		return _socket.getInetAddress().getHostAddress();
	}
	
	
	public void releaseSlot() {
		_torrent.getScheduler().invokeLater(getSlotReleaser());
	}
	
	private Runnable getSlotReleaser() {
		if (slotReleaser == null) {
			slotReleaser = new Runnable() {
				public void run() {
					if (LOG.isDebugEnabled())
						LOG.debug(BTConnection.this +" releasing slot");
					usingSlot = false;
					choke();
				}
			};
		}
		return slotReleaser;
	}
	
	public void slotAvailable() {
		_torrent.getScheduler().invokeLater(getSlotNotifier());
	}

	private Runnable getSlotNotifier() {
		if (slotNotifier == null) {
			slotNotifier = new Runnable() {
				public void run() {
					if (LOG.isDebugEnabled())
						LOG.debug(BTConnection.this+" got available slot");
					beginPieceSend();
				}
			};
		}
		return slotNotifier;
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Chokable#getAverageBandwidth()
	 */
	public float getAverageBandwidth() {
		return up.getAverageBandwidth();
	}


	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Chokable#getMeasuredBandwidth()
	 */
	public float getMeasuredBandwidth() throws InsufficientDataException {
		return up.getMeasuredBandwidth();
	}


	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Chokable#measureBandwidth()
	 */
	public void measureBandwidth() {
		up.measureBandwidth();
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Chokable#isSeed()
	 */
	public boolean isSeed() {
		return _available.cardinality() == _info.getNumBlocks();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.BTLink#isBusy()
	 */
	public boolean isBusy() {
		return !isInteresting();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.BTLink#isUploading()
	 */
	public boolean isUploading() {
		return isInterested() && !isChoked();
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.BTLink#suspendTraffic()
	 */
	public void suspendTraffic() {
		sendNotInterested();
		choke();
	}
}
