
// Commented for the Learning branch

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
 * A BTConnection object represents our connection to a remote computer running BitTorrent software and sharing a torrent we have.
 * 
 * Our ManagedTorrent object, _torrent, keeps a list of BTConnection objects.
 * There is one for each remote computer it's sharing this torrent with.
 * 
 * Only BTHandshaker.tryToFinishHandshakes() makes a new BTConnection object.
 * It's already done the handshake with a remote computer.
 * The code in this class deals with sending and receiving BitTorrent messages.
 * 
 * === Choking and Interest ===
 * 
 * The BTConnection object has 4 member booleans that keep track of our state with this remote computer.
 * _isChoked is true when we're choking this remote computer.
 * _isInteresting is true when we're interested in this remote computer's data.
 * _isChoking is true when this remote computer is choking us.
 * _isInterested is true when this remote computer is interested in our data.
 * Access these values with the methods isChoked(), isInteresting(), isChoking(), and isInterested().
 * 
 * _isChoked and _isInteresting are about us.
 * They're true when we've choked the remote computer, and when we're interested in its data.
 * _isChoking and _isInterested are are about the remote computer.
 * They're true when it has choked us, and when it is interested in our data.
 * 
 * Connections start out choked and not interested.
 * _isChoked and _isChoking are true, and _isInterested and _isInteresting are false.
 * The BTConnection constructor sets these defaults.
 * 
 * If this remote computer isn't sending us data fast enough, we'll choke it as punishment.
 * We'll send it a Choke message, and set _isChoked to true.
 * We won't send it any data, even if it requests some.
 * When we're done choking it, we'll send it an Unchoke message and set _isChoked to false.
 * To choke and unchoke this computer, call sendChoke() and sendUnchoke().
 * Those methods set _isChoked to true and false, and send Choke and Unchoke messages.
 * 
 * If we're not sending this remote computer data fast enough, it will choke us as punishment.
 * We'll receive a Choke message from it, and set _isChoking to true.
 * We won't ask it for any data.
 * When it's done choking us, it will send us an Unchoke message, and we'll set _isChoking to false.
 * When we receive a Choke message, handleMessage() sets _isChoking to true.
 * It sets it back to false when this remote computer sends us an Unchoke message.
 * 
 * If this remote computer has data that we need, it's interesting to us.
 * We'll set _isInteresting to true.
 * To tell this remote computer we're interested in its data or we're not, call sendInterested and sendNotInterested().
 * Those methods set _isInteresting to true and false, and set Interested and Not Interested messages.
 * 
 * If we have data this remote computer needs, it's interested in us.
 * We'll set _isInterested to true.
 * When we receive an Interested message, handleMessage() sets _isInterested to true.
 * When this remote computer sends us a Not Interested message, handleMessage() sets _isInterested to false.
 * 
 * === Request, Cancel, and Piece messages ===
 * 
 * This BTConnection object keeps 3 lists:
 * _toRequest
 * _requesting
 * _requested
 * 
 * All 3 are HashSet objects that hold BTInterval objects.
 * A HashSet is a list that has no duplicates.
 * Java calls BTIntervalHashSet.hashCode() to make the HashSet fast.
 * The BTConnection constructor makes 3 empty HashSet objects, and saves them.
 * 
 * _toRequest and _requesting are used to ask this remote computer for data.
 * _requested is used to keep track of what this remote computer has asked us.
 * 
 * _toRequest is the data ranges we're going to ask this remote computer.
 * When we send a Request message, we move the range it asks for from _toRequest to the _requesting list.
 * When this remote computer sends us a Piece message that satisifies the request, we remove it from _requesting.
 * 
 * sendRequest() adds a BTInterval to our _toRequest list.
 * enqueueRequests() moves BTIntervals to _requesting, and sends Request messages.
 * handlePiece() removes a BTInterval from _requesting.
 * 
 * _requested is the data ranges this remote computer has asked us to send.
 * When it gives us a Request message, we add the range to the list.
 * If it cancels that request with a Cancel message, we remove the range from the list.
 * When we satisfy the request with a Piece message, we remove the range from the list.
 * 
 * handleRequest() adds a BTInterval to _requested.
 * handleCancel() removes a BTInterval from _requested.
 * readyForWriting() removes a BTInterval from _requested, and sends a Piece message.
 */
public class BTConnection {

	/** A debugging log we can write lines of text to as the program runs. */
	private static final Log LOG = LogFactory.getLog(BTConnection.class);

	/**
	 * 16384 bytes, 16 KB.
	 * 
	 * At the most, we'll request 16 KB of data at a time.
	 * This is a 16 KB range of data within a single piece, which is much larger.
	 * 
	 * This is the largest range a BitTorrent program is supposed to request.
	 */
	private static final int BLOCK_SIZE = 16384;

	/**
	 * 65536 bytes, 64 KB.
	 * 
	 * If a remote computer gives us a Request message that asks for more than 64 KB of data, we won't do it.
	 * This describes a 64 KB range of data within a single piece, which is larger.
	 */
	private static final int MAX_BLOCK_SIZE = 64 * 1024;

	/** 4, we'll only ask for 4 pieces from this remote computer at a time. */
	private static final int MAX_REQUESTS = 4;

	/**
	 * 1 minute in milliseconds.
	 * isWorthRetrying() returns true if it's called more than a minute after this BTConnection object was made.
	 */
	private static final long MIN_RETRYABLE_LIFE_TIME = 60 * 1000;

	/** The NIOSocket object we use to send and receive data with the remote computer this BTConnection represents. */
	private final NIOSocket _socket;

	/**
	 * Slices data from this remote computer into BitTorrent messages, and parses them into objects that extend BTMessage.
	 * _reader joins a chain of readers in LimeWire's NIO design.
	 */
	private BTMessageReader _reader;

	/**
	 * To send a message, give its data to this BTMessageWriter object.
	 * _writer joins a chain of writers in LimeWire's NIO design.
	 */
	private BTMessageWriter _writer;

	/**
	 * Tells which pieces of the torrent this remote computer has, and we need.
	 * This is what we can get from it.
	 * 
	 * A BitSet object that has 1 bit for each piece of this torrent.
	 * If a bit it 0, that means this remote computer doesn't have that piece.
	 * 
	 * Tells which pieces this remote computer has, and we don't.
	 * sendHave() only sends this remote computer a Have message if it doesn't have that piece yet.
	 * If it does already have the piece, sendHave() changes _availableRanges to make it look like it doesn't.
	 * Then, if _availableRanges is all 0s, sendHave() sends a Not Interested message.
	 * 
	 * Lock on _availableRanges before using it.
	 */
	private final BitSet _availableRanges;

	/**
	 * A list of parts of pieces we're going to ask this remote computer for.
	 * We haven't send Request messages for these intervals yet, but will soon.
	 * 
	 * A HashSet of BTInterval objects.
	 * A HashSet is a list that doesn't allow duplicates, and calls BTInterval.hashCode() to be fast.
	 */
	private final Set _toRequest;

	/**
	 * A list of parts of pieces we've asked this remote computer for.
	 * We sent Request messages for these, and are waiting for the data.
	 * 
	 * A HashSet of BTInterval objects.
	 * A HashSet is a list that doesn't allow duplicates, and calls BTInterval.hashCode() to be fast.
	 */
	private final Set _requesting;

	/**
	 * A list of parts of pieces this remote computer has asked us to give it.
	 * 
	 * A HashSet of BTInterval objects.
	 * A HashSet is a list that doesn't allow duplicates, and calls BTInterval.hashCode() to be fast.
	 */
	private final Set _requested;

	/** The BTMetaInfo object made from the bencoded data in the .torrent file. */
	private final BTMetaInfo _info;

	/** This remote computer's IP address and port number, and BitTorrent peer ID and extension bytes. */
	private final TorrentLocation _endpoint;

	/**
	 * True if we initiated this outgoing connection to this remote computer.
	 * False if it connected to our listening socket.
	 */
	private final boolean _outgoing;

	/** The ManagedTorrent object that made this BTConnection to share its torrent with this remote computer. */
	private final ManagedTorrent _torrent;

	/** True while we're choking this remote computer. */
	private boolean _isChoked;

	/** True while we're interested in this remote computer's data. */
	private volatile boolean _isInteresting;

	/** True while this remote computer is choking us. */
	private volatile boolean _isChoking;

	/** True while this remote computer is interested in our data. */
	private boolean _isInterested;

	/** The time when this BTConnection object was made. */
	private final long _startTime;

	/**
	 * The number of pieces we have that this remote computer needs.
	 * This is the number of pieces it can get from us.
	 */
	private int numMissing;

	/** Not used. */
	public BTConnection(NIOSocket sock, BTMetaInfo info, TorrentLocation ep, ManagedTorrent torrent) {
		this(sock, info, ep, torrent, false);
	}

	/**
	 * Make a new BTConnection object to represent our TCP socket connection to a remote computer.
	 * The remote computer is running BitTorrent software, and we are sharing our torrent through this connection.
	 * The connection started with the BitTorrent handshake, and is now carrying BitTorrent messages.
	 * 
	 * Only BTHandshaker.tryToFinishHandshakes() calls this constructor.
	 * It's already done the handshake with this remote computer.
	 * 
	 * Saves the given objects, linking this new BTConnection to the ManagedTorrent that made it.
	 * Builds the chains of readers and writers according to LimeWire's NIO design.
	 * Starts us and this remote computer as choking and not interested.
	 * Sends the remote computer a Bitfield message to tell it which pieces of this torrent we have.
	 * 
	 * @param socket     The NIOSocket object that represents our TCP socket connection to this remote computer
	 * @param info       The BTMetaInfo object we made from the bencoded data inside the .torrent file
	 * @param ep         This remote computer's IP address and port number in a TorrentLocation object
	 * @param torrent    The ManagedTorrent object that is making this BTConnection object
	 * @param isOutgoing True if we connected to this remote computer, false if it connected to our listening socket
	 */
	public BTConnection(NIOSocket socket, BTMetaInfo info, TorrentLocation ep, ManagedTorrent torrent, boolean isOutgoing) {

		// Save the given NIOSocket object, we'll communicate with this remote computer through it
		_socket = socket;

		// Save the given TorrentLocation object, it has the IP address and port number of this remote computer
		_endpoint = ep;

		// Save the given ManagedTorrent object, it's the object that is making this new BTConnection
		_torrent = torrent;

		// Save the given boolean that tells which computer initiated this connection
		_outgoing = isOutgoing;

		// Make a new BitSet with one bit for each piece in this torrent, all the bits are 0
		_availableRanges = new BitSet(info.getNumBlocks()); // getNumBlocks() returns the number of pieces

		// Make 3 new HashSet objects to keep track of requests
		_requesting = new HashSet(); // The parts of the file we've sent Request messages to this remote computer for
		_toRequest  = new HashSet(); // The parts of the file we're going to send Request messages for next
		_requested  = new HashSet(); // The parts of the file this remote computer has asked us for with Request messages we've received

		// Record when this BTConnection object was made
		_startTime = System.currentTimeMillis(); // Set _startTime to now, the number of milliseconds since 1970

		// Make new BTMessageReader and BTMessageWriter objects
		_reader = new BTMessageReader(this); // Give them this so they can link back up to us
		_writer = new BTMessageWriter(this);

		/*
		 * we put the throttle on top, this puts the delayer on top
		 * decide is this what we want?  maybe
		 */

		/*
		 * The code below builds the chains of readers and writers.
		 * This is a part of LimeWire's NIO design.
		 * 
		 * _torrent.getUploadThrottle() gets a reference to the program's single NBThrottle object for BitTorrent.
		 * The line of code makes a new ThrottleWriter from it, which will control how fast we upload data to this remote computer.
		 * 
		 * The code also makes a DelayedBufferWriter for this connection.
		 * It will group 1400 bytes together before sending them all.
		 * 
		 * The chain of writers for this BTConnection looks like this:
		 * 
		 * BTMessageWriter -> DelayedBufferWriter -> ThrottleWriter
		 */

		// Make ThrottleWriter and DelayedBufferWriter objects for this new BTConnection
		ThrottleWriter throttle = new ThrottleWriter(_torrent.getUploadThrottle());
		DelayedBufferWriter delayer = new DelayedBufferWriter(1400);

		// Build this connection's chain of writers
		_writer.setWriteChannel(delayer);
		delayer.setWriteChannel(throttle);

		/*
		 * When Java gets data from this remote computer, it will tell the NIOSocket object.
		 * The code below tells the socket object which object to forward the call to.
		 * We want our BTMessageReader and BTMessageWriter objects to get the notifications from NIO.
		 */

		// Have our NIOSocket forward NIO notifications to our BTMessageReader and BTMessageWriter
		socket.setReadObserver(_reader);
		socket.setWriteObserver(_writer);

		// Save the given BTMetaInfo object, we'll read the contents of the .torrent file with it
		_info = info;

		// BitTorrent connections start out choked, and not interested
		_isChoked      = true;  // We're choking this remote computer
		_isChoking     = true;  // As far as we know, this remote computer is choking us
		_isInterested  = false; // As far as we know, this remote computer is not interested in our data
		_isInteresting = false; // We're not interested in this remote computer's data

		// Send this remote computer a Bitfield message, telling it which pieces we have
		sendBitfield(); // Right after the handshake, BitTorrent programs exchange Bitfield messages
	}

	/**
	 * Determine if we're choking this remote computer.
	 * 
	 * @return True if we sent it a Choke message.
	 *         False if we sent it an Unchoke message.
	 */
	public boolean isChoked() {

		// Return the value sendChoke() or sendUnchoke() set
		return _isChoked;
	}

	/**
	 * Determine if this remote computer is choking us.
	 * 
	 * @return True if it sent us a Choke message.
	 *         False if it sent us an Unchoke message.
	 */
	public boolean isChoking() {

		// Return the value that handleMessage() set
		return _isChoking;
	}

	/**
	 * Determine if this remote computer is interested in our data.
	 * 
	 * @return True if it sent us an Interested message.
	 *         False if it sent us a Not Interested message.
	 */
	public boolean isInterested() {

		// Return the value that handleMessage() set
		return _isInterested;
	}

	/**
	 * Determine if we have pieces this remote computer needs.
	 * If so, this remote computer should be interested in our data.
	 * 
	 * @return True if we have pieces this remote computer needs
	 */
	public boolean shouldBeInterested() {

		// If numMissing is more than 0, return true
		return numMissing > 0;
	}

	/**
	 * Determine if we're interested in this remote computer's data.
	 * 
	 * @return True if we sent it an Interested message.
	 *         False if we sent it a Not Interested message.
	 */
	public boolean isInteresting() {

		// Return the value sendInterested() or sendNotInterested() set
		return _isInteresting;
	}

	/**
	 * Determine if this remote computer has asked us for anything we haven't given it right now.
	 * 
	 * @return True if this remote computer has made a request we haven't satisifed yet
	 */
	public boolean hasRequested() {

		// If the _requested list is empty, we've sent all the parts this remote computer has requested
		return !_requested.isEmpty();
	}

	/**
	 * Determine if we initiated this connection to this remote computer, or if it connected to us.
	 * 
	 * @return true if this is an outgoing connection we made
	 */
	public boolean isOutgoing() {

		// Return the value the constructor saved
		return _outgoing;
	}

	/**
	 * Returns true if this BTConnection object is more than a minute old.
	 * 
	 * Only ManagedTorrent.connectionClosed() calls this, right after disconnecting this connection.
	 * If we had it for more than a minute, it's worth retrying in the future.
	 * 
	 * @return True if this connection was open for more than a minute.
	 *         False if we disconnected sooner than that.
	 */
	public boolean isWorthRetrying() {

		/*
		 * don't retry connections that were aborted immediately after starting
		 * them, they were most likely terminated for a reason...
		 */

		// Return true if this BTConnection object is more than a minute old
		return System.currentTimeMillis() - _startTime > MIN_RETRYABLE_LIFE_TIME;
	}

	/**
	 * Get this remote computer's IP address and port number, and BitTorrent peer ID and extension bytes.
	 * 
	 * @return The address and BitTorrent handshake information in a TorrentLocation object
	 */
	public TorrentLocation getEndpoint() {

		// Return the TorrentLocation object we saved
		return _endpoint;
	}

	/**
	 * Close this connection.
	 * 
	 * Calls shutdownOutput() and shutdownInput() on our Java _socket object.
	 * Calls shutdown() on our BTMessageReader and BTMessageWriter, closing it with the chains of readers and writers.
	 * Has the NIODispatcher close our TCP socket connection to the remote computer.
	 * Tells the ManagedTorrent that made us that we're closed.
	 */
	public void close() {

		// Shut down the Java socket
		try { _socket.shutdownOutput(); } catch (IOException ioe1) {}
		try { _socket.shutdownInput();  } catch (IOException ioe2) {}

		// Shut down our chains of readers and writers
		_reader.shutdown(); // Just marks our BTMessageReader as shut down
		_writer.shutdown(); // Removes all the messages from its queue, and tells the object it writes to it has no data for it

		// Have the NIODispatcher close our TCP socket connection to the remote computer
		try { _socket.close(); } catch (IOException ioe) {}

		// Tell the ManagedTorrent that made us we're closed
		_torrent.connectionClosed(this);
	}

	/**
	 * Get the BTMessageReader object this BTConnection uses.
	 * It slices data from this remote computer into BitTorrent messages, and parses them into objects that extend BTMessage.
	 * 
	 * @return A reference to our BTMessageReader object
	 */
	public BTMessageReader getMessageReader() {

		// Return the object the constructor made
		return _reader;
	}

	/**
	 * Determine if our socket is connected to this remote computer.
	 * Calls isConnected on the Java socket inside this BTConnection's NIOSocket object.
	 * 
	 * @return True if we're still connected to this remote computer
	 */
	public boolean isConnected() {

		// Call NIOSocket.isConnected(), which leads to java.net.Socket.isConnected()
		return _socket.isConnected();
	}

	/**
	 * Record that we received a number of bytes of file data.
	 * Tells this torrent's BTDownloader.
	 * This method is named "readBytes", but should be spoken "redBytes", using the past tense of read.
	 * 
	 * @param read The number of bytes of file data we received from this remote computer
	 */
	public void readBytes(int read) {

		// Tell our torrent's BTDownloader
		_torrent.getDownloader().readBytes(read);
	}

	/**
	 * Record that we sent a number of bytes of file data.
	 * Tells this torrent's BTUploader.
	 * 
	 * @param written The number of bytes of file data we sent to this remote computer
	 */
	public void wroteBytes(int written) {

		// Tell our torrent's BTUploader
		_torrent.getUploader().wroteBytes(written);
	}

	/**
	 * Close this connection because we got an IOException.
	 * 
	 * This method handles IOExceptions for this connection.
	 * If NIO gets an exception while transferring data for our socket, it will give the exception to this method.
	 * 
	 * @param iox The exception we got
	 */
	public void handleIOException(IOException iox) {

		// If this exception is about a bad BitTorrent message, count that we got another one
		if (iox instanceof BadBTMessageException) BTMessageStat.INCOMING_BAD.incrementStat();

		// Make a note about this exception in the debugging log
		if (LOG.isDebugEnabled()) LOG.debug(iox);

		// Close our connection to this remote computer
		close();
	}

	/**
	 * Read and respond to a BitTorent message this remote computer has sent us.
	 * 
	 * Only BTMessageReader.handleRead() calls this method.
	 * Just calls handleMessage(message).
	 * 
	 * @param message The BitTorrent message from this remote computer
	 */
	public void processMessage(final BTMessage message) {

		// Make a note in the debugging log, and pass the message to handleMessage()
		if (LOG.isDebugEnabled()) LOG.debug("received message: " + message + " from " + _endpoint);
		handleMessage(message);
	}

	/**
	 * Choke this remote computer.
	 * Sends it a Choke message, and sets _isChoked to true.
	 */
	void sendChoke() {

		// Forget about all the Request messages this remote computer has sent us
		_requested.clear();

		// Only do something if we're not choking this remote computer already
		if (!_isChoked) {

			// Send a Choke message to this remote computer, and record that we're choking it
			_writer.enqueue(BTChoke.createMessage());
			_isChoked = true;
		}
	}

	/**
	 * Unchoke this remote computer.
	 * Sends it an Unchoke message, and sets _isChoked to false.
	 */
	void sendUnchoke() {

		// Only do something if we're choking this remote computer right now
		if (_isChoked) {

			// Send an Unchoke message to this remote computer, and record that we're not choking it
			_writer.enqueue(BTUnchoke.createMessage());
			_isChoked = false;
		}
	}

	/**
	 * Tell this remote computer we're interested in its data.
	 * Sends it an Interested message, and sets _isInteresting to true.
	 */
	void sendInterested() {

		// Only do something if this remote computer thinks we're not interested in its data
		if (!_isInteresting) {

			// Send it an Interested message, and record that we told it
			LOG.debug("sending interested message");
			_writer.enqueue(BTInterested.createMessage());
			_isInteresting = true;
		}
	}

	/**
	 * Tell this remote computer we're not interested in its data.
	 * Sends it a Not Interested message, and sets _isInteresting to false.
	 */
	void sendNotInterested() {

		// Only do something if this remote computer thanks we're interested in its data
		if (_isInteresting) {

			// Send it a Not Interested message, and record that we told it
			_writer.enqueue(BTNotInterested.createMessage());
			_isInteresting = false;
		}
	}

	/**
	 * Send a Have message to this remote computer, telling it we have a piece.
	 * 
	 * Only ManagedTorrent.notifyOfComplete() calls this method.
	 * We've just received a complete piece, and checked that its hash is correct.
	 * notifyOfComplete() loops for each of the ManagedTorrent's connections.
	 * It calls sendHave() to send a Have message to this remote computer.
	 * The Have message tells this remote computer that we have this piece.
	 * 
	 * Make sure to call this method only for complete pieces.
	 * 
	 * @param have The Have message which tells that we have a complete piece
	 */
	void sendHave(BTHave have) {

		// Read the piece number from the given Have message
		int pieceNum = have.getPieceNum(); // This is the piece we have

		/*
		 * As a minor optimization we will not inform the remote host of any
		 * pieces that it already has
		 * Else, simply remove the chunk from the available ranges, to optimize
		 * requesting ranges...
		 */

		// If this remote computer doesn't have the piece we just got
		if (!_availableRanges.get(pieceNum)) {

			// Record that there's one more piece this remote computer needs
			numMissing++;

			// Send the Have message to this remote computer to tell it that it can get the piece from us
			_writer.enqueue(have);

		// This remote computer already has the piece we just got
		} else {

			// Change our record of which pieces this remote computer has to make it look like the remote computer does not have this piece
			_availableRanges.clear(pieceNum); // This makes our code of requesting ranges a little faster
		}

		/*
		 * we should indicate that we are not interested anymore, so we are
		 * not unchoked when we do not want to request anything.
		 */

		// If this remote computer doesn't have anything we need, send it a Not Interested message
		if (_availableRanges.isEmpty()) sendNotInterested();

		// If we removed a BTInterval from _requesting or _toRequest, the loops below will set modified to true
		boolean modified = false;

		// Loop for each BTInterval in _requesting, we sent this remote computer Request messages to ask for these pieces
		for (Iterator iter = _requesting.iterator(); iter.hasNext(); ) {
			BTInterval req = (BTInterval)iter.next();

			// If this request was for data within the piece we now have
			if (req.getId() == pieceNum) {

				// Remove it from the list
				iter.remove();

				// Send the remote computer a Cancel message, we don't need it anymore
				sendCancel(req);

				// Yes, we modified the _requesting or _toRequest lists
				modified = true;
			}
		}

		// Loop for each BTInterval in _toRequest, the intervals we're going to request from this remote computer
		for (Iterator iter = _toRequest.iterator(); iter.hasNext(); ) {
			BTInterval req = (BTInterval)iter.next();

			// If this pending request was for data within the piece we now have
			if (req.getId() == pieceNum) {

				// Remove it from the list
				iter.remove();

				// Yes, we modified the _requesting or _toRequest lists
				modified = true;
			}
		}

		/*
		 * if we removed any ranges, choose some more rages to request...
		 */

		// If we need data and _toRequest is empty, have the ManagedTorrent send a Request message
		if (!_torrent.isComplete() && // If we still need parts of this torrent, and
			modified &&               // The piece we just got cancelled some requests we made this remote computer, or were going to make, and
			_isInteresting &&         // This remote computer still has data we need, and
			_toRequest.isEmpty()) {   // Our _toRequest list of things to ask for is empty

			// Have the ManagedTorrent object pick a range to ask for, and send a Request message
			_torrent.request(this);
		}
	}

	/**
	 * Send a Bitfield message to this remote computer, showing it which pieces of the torrent we have and which we need.
	 * BitTorrent programs exchange Bitfield messages right after the BitTorrent handshake.
	 * 
	 * Only the BTConnection constructor calls this method.
	 * The constructor runs right after we've finished the BitTorrent handshake with this remote computer.
	 */
	void sendBitfield() {

		// Make a Bitfield message, and send it to this remote computer
		_writer.enqueue(BTBitField.createMessage(_info)); // _info links to the torrent's VerifyingFolder, which knows which pieces we have
	}

	/**
	 * Add a given piece number to those we'll request, and send this remote computer a Request message.
	 * Only ManagedTorrent.request() calls this method.
	 * 
	 * @param in A BTInterval object with the piece number and data range within that piece we want to ask this remote computer for
	 */
	void sendRequest(BTInterval in) {

		/*
		 * we do not request any pieces larger than BLOCK_SIZE!
		 */

		// Loop, moving i forward 16 KB within this piece
		for (long i = in.low;  // Start i at the distance into the piece the range we want starts
			i < in.high;       // If i reaches high, the last byte in the range, don't run the loop this time
			i += BLOCK_SIZE) { // Move i forward 16 KB within this piece

			/*
			 * watch out, all Intervals are inclusive on both ends...
			 * safe cast, length is always <= BLOCK_SIZE
			 */

			// Calculate length, the number of bytes we'll ask for
			int length = (int)Math.min( // Make sure it isn't bigger than the size of a piece
				in.high - i + 1,        // The length of the data from i to the end of the interval
				BLOCK_SIZE);            // The maximum size of our request, 16 KB

			// Make a new BTInterval object that has the range within a single piece
			BTInterval toReq = new BTInterval(
				i,              // The data we want starts at i
				i + length - 1, // The last byte we want
				in.getId());    // The given piece number

			// If we haven't already asked this remote computer for this range, add it to the _toRequest list
			if (!_requesting.contains(toReq)) _toRequest.add(toReq);
		}

		// Send this remote computer 4 Request messages from the BTIntervals in the _toRequest list
		enqueueRequests();
	}

	/**
	 * Send this remote computer a Cancel message.
	 * 
	 * We previously sent it a Request message.
	 * Our Cancel message will have the same piece number and range information.
	 * This is how the remote computer will know which of our requests to cancel.
	 * 
	 * @param in The BTInterval with the piece number and range within that piece that we don't want anymore.
	 */
	private void sendCancel(BTInterval in) {

		// Make a new Cancel message from the given BTInterval, and send it to this remote computer
		_writer.enqueue(new BTCancel(in));
	}

	/**
	 * Send a Cancel message for each Request message we've sent this remote computer that it hasn't fulfilled yet.
	 * 
	 * Only MangedTorrent.saveCompleteFiles() calls this method.
	 * We've finished downloading the entire torrent, and will move it from the "Incomplete" folder to the "Shared" folder.
	 */
	void cancelAllRequests() {

		// Loop for each BTInterval object in our _requesting list, these are the Request messages we've sent this remote computer
		Iterator iter = _requesting.iterator();
		while (iter.hasNext()) {
			BTInterval request = (BTInterval)iter.next();

			// Send a Cancel message with the same piece number and data range information
			sendCancel(request);
		}

		// Clear the _toRequest and _requesting lists, and tells the piece numbers to the VerifyingFolder
		clearRequests();
	}

	/**
	 * Get a request from our _requested list, and send this remote computer a Piece message with the part of this torrent it asked for.
	 * This method removes a BTInterval from the _requested list.
	 * 
	 * BTMessageWriter.sendNextMessage() and BTConnection.handleRequest() call this method.
	 * 
	 * This method is called readyForWriting() because the program can call it when the program is ready to write data to this remote computer.
	 * The methd writes data to the remote computer, sending it a Piece message with the file data it previously asked for.
	 */
	void readyForWriting() {

		// If we're choking this remote computer, or it hasn't sent us any Request messages we haven't satisified yet
		if (_isChoked || _requested.size() <= 0) {

			// We can't send this remote computer data, make a note and leave without doing anything
			if (LOG.isDebugEnabled()) LOG.debug("cannot write while choked, requested size is " + _requested);
			return;
		}

		/*
		 * If control makes it here, this remote computer has sent us Request messages, and we can send Piece messages in response.
		 */

		// Get a single BTInterval from the _requested list
		Iterator iter = _requested.iterator();
		BTInterval in = (BTInterval)iter.next(); // The BTInterval has the piece number and range the remote computer wants from us
		iter.remove(); // Remove it from the _requested list
		if (LOG.isDebugEnabled()) LOG.debug("requesting disk read for " + in);

		try {

			// Have the torrent's VerifyingFolder object send this remote computer the data it wants in a Piece message
			_info.getVerifyingFolder().sendPiece(in, this);

		// There was an error with the disk
		} catch (IOException bad) {

			// Close this connection
			close();
		}
	}

	/**
	 * Make a new Piece message, and send it to this remote computer.
	 * Only VerifyingFolder.SendJob.run() calls this method.
	 * 
	 * @param in   The piece number and range within that piece of the data to send
	 * @param data The data to send
	 */
	void pieceRead(final BTInterval in, final byte[] data) {

		// Have the "NIODispatch" thread call this run() method
		Runnable pieceSender = new Runnable() {
			public void run() {

				// Make a new Piece message, and send it to this remote computer
				if (LOG.isDebugEnabled()) LOG.debug("disk read done for " + in);
				_writer.enqueue(new BTPiece(in, data));
			}
		};
		NIODispatcher.instance().invokeLater(pieceSender);
	}

	/**
	 * Get a BitSet that tells which pieces of the torent this remote computer has, and we need.
	 * This is what we can get from it.
	 * 
	 * A BitSet object that has 1 bit for each piece of this torrent.
	 * If a bit it 0, that means this remote computer doesn't have that piece.
	 * 
	 * @return A BitSet with bits representing pieces
	 */
	BitSet getAvailableRanges() {

		// Return the BitSet that tells which pieces we can get from this remote computer
		return _availableRanges;
	}

	/**
	 * Call addAvailablePiece(pieceNumber) when we find out this remote computer has a piece.
	 * Tells the torrent's VerifyingFolder we have a connection to a computer that has the given piece number.
	 * If we need this piece, sends this remote computer an Intersted message.
	 * 
	 * handleHave() and handleBitField() call this method.
	 * 
	 * @param pieceNum The piece number this remote computer says it has
	 */
	private void addAvailablePiece(int pieceNum) {

		// Tell the torrent's VerifyingFolder that a remote computer has that piece
		VerifyingFolder v = _info.getVerifyingFolder();
		_availableRanges.set(pieceNum); // But, it won't know to ask this remote computer for it

		/*
		 * tell the remote host we are interested if we don't have that range!
		 */

		// We and this remote computer both have the piece
		if (v.hasBlock(pieceNum)) {

			// Count one fewer piece we have that this remote computer needs
			numMissing--;

		// We need the piece, and can get it from this remote computer
		} else {

			// If we haven't already done so, send this remote computer an Interested message
			sendInterested();
		}
	}

	/**
	 * Clear the _toRequest and _requesting lists, and tells the piece numbers to the VerifyingFolder.
	 * 
	 * Loops through all the BTInterval objects in our _toRequest and _requesting lists.
	 * These are the intervals we were going to request from this remote computer, and have requests.
	 * Gets the piece number each is in, adn tells our torrent's VerifyingFolder to release those pieces.
	 * This tells the VerifyingFolder that those pices can be requested again.
	 * Clears the _toRequest and _requesting lists.
	 */
	private void clearRequests() {

		// Loop for each BTInterval in the _toRequest list
		for (Iterator iter = _toRequest.iterator(); iter.hasNext(); ) {

			// Tell our torrent's VerifyingFolder to release the piece number this interval is in
			clearRequest((BTInterval)iter.next());
		}

		// Loop for each BTInterval in the _requesting list
		for (Iterator iter = _requesting.iterator(); iter.hasNext(); ) {

			// Tell our torrent's VerifyingFolder to release the piece number this interval is in
			clearRequest((BTInterval)iter.next());
		}

		// Make both lists empty
		_toRequest.clear();
		_requesting.clear();
	}

	/**
	 * Tell our torrent's VerifyingFolder that we're not requesting a piece number any longer.
	 * 
	 * @param in A BTInterval that describes a range of data within a piece.
	 *           This method just uses the piece number, in.getId().
	 */
	private void clearRequest(BTInterval in) {

		// Tell our torrent's VerifyingFolder that we're not requesting the given piece number anymore
		_info.getVerifyingFolder().releaseChunk(in.getId());
	}

	/**
	 * Send this remote computer 4 Request messages from the BTIntervals in the _toRequest list.
	 * 
	 * Randomly picks 4 BTInterval ranges from the _toRequest list.
	 * Composes BitTorrent Request messages from them, and sends them to this remote computer.
	 * Moves the BTInterval objects from the _toRequest list to the _requesting list.
	 * 
	 * Stops if we have 4 requests pending with this remote computer, or if we have 10 messages waiting to go out to it.
	 */
	private void enqueueRequests() {

		/*
		 * the reason we randomize the list of requests to be sent is that we
		 * are receiving far too many ranges multiple times when the download
		 * is about to finish.
		 */

		// Copy all the BTInterval ranges we're going to ask this remote computer for into a new list, and shuffle it
		List random = new ArrayList();
		random.addAll(_toRequest);
		Collections.shuffle(random);

		// Loop for each BTInterval object in the _toRequest list, moving through them in random order
		for (Iterator iter = random.iterator();

			// Only run this loop if all of the following things are true
			_requesting.size() < MAX_REQUESTS && // We haven't asked this computer for 4 pieces yet, and
			iter.hasNext() &&                    // We're not through the whole _toRequest list yet, and
			!_isChoking; ) {                     // This remote computer isn't choking us right now

			// Get the BTInterval for this run of the loop
			BTInterval toReq = (BTInterval)iter.next();

			// Send this remote computer a Request message that will ask for the toReq range
			if (!_writer.enqueue(new BTRequest(toReq))) return; // enqueue() returns false if _writer already has 10 messages

			// The message will be sent, move toReq from _toRequest to _requesting
			_toRequest.remove(toReq);
			_requesting.add(toReq);
		}
	}

	/**
	 * Read and respond to a BitTorent message this remote computer has sent us.
	 * 
	 * Only BTConnection.processMessage() calls this method.
	 * Here's what's happened so far:
	 * BTMessageReader.handleRead() sliced a BitTorrent message from the data this remote computer sent us.
	 * It parsed it into a type-specific object that extends BTMessage, and passed it to the next method.
	 * BTConnection.processMessage() just calls this method.
	 * 
	 * @param message The BitTorrent message from this remote computer
	 */
	private void handleMessage(BTMessage message) {

		// Sort by the type of message this remote computer sent us
		switch (message.getType()) {

		// This remote computer sent us a Choke message, it's not going to send us any data
		case BTMessage.CHOKE:

			/*
			 * we queue all requests up again, maybe we get unchoked
			 * again, - if not this will be resolved in endgame mode
			 */

			// Now that we're choked, move all the intervals we requested back from _requesting to _toRequest
			for (Iterator iter = _requesting.iterator(); iter.hasNext(); ) {
				_toRequest.add(iter.next());
				iter.remove();
			}

			// Record that this remote computer is choking us
			_isChoking = true;
			break;

		// This remote computer sent us an Unchoke message, it is willing to send us data
		case BTMessage.UNCHOKE:

			// Record that this remote computer isn't choking us any longer
			_isChoking = false;

			// If we're interested in this remote computer's data right now
			if (_isInteresting) { // If it has parts of this torrent's file that we need

				// Randomly pick 4 ranges to request from this remote computer
				enqueueRequests(); // We'll send the Request messages as soon as we are unchoked

				// If we still need parts of this torrent, and our list of intervals to ask for is empty
				if (!_torrent.isComplete() && // If we don't have this whole torrent yet, and
					_toRequest.isEmpty()) {   // Our list of pieces to ask for is empty

					// Have the ManagedTorrent object pick a range to ask for, and send a Request message
					_torrent.request(this);
				}
			}
			break;

		// This remote computer sent us an Interested message, it is interested in our data
		case BTMessage.INTERESTED:

			// This remote computer is interested in our data
			_isInterested = true;

			// If we're not choking this remote computer, send each of this torrent's connections a Choke or Unchoke message now
			if (!_isChoked) _torrent.rechoke();
			break;

		// This remote computer sent us a Not Interested message, it doesn't need any of the data we have
		case BTMessage.NOT_INTERESTED:

			// This remote computer doesn't need any of the data we have for this torrent
			_isInterested = false;

			// If we're not choking this remote computer, send each of this torrent's connections a Choke or Unchoke message now
			if (!_isChoked) _torrent.rechoke();

			/*
			 * connections that aren't interested any more are choked instantly
			 */

			// In response to this remote computer's Not Interested message, send it a Choke message to choke it
			sendChoke();

			/*
			 * if we have all pieces and the remote is not interested,
			 * disconnect, - they have obviously completed their download, too
			 */

			// If we have the whole file and this remote computer doesn't need any data, close our connection to it
			if (_torrent.isComplete()) close();
			break;

		// This remote computer sent us a Bitfield message, it's showing us which pieces it has and which pieces it needs
		case BTMessage.BITFIELD:

			// Tell the torrent's VerifyingFolder which pieces this remote computer has, and figure out what we can get from it
			handleBitField((BTBitField)message);
			break;

		// This remote computer sent us a Have message, it just received and validated a numbered piece
		case BTMessage.HAVE:

			// Tell the torrent's VerifyingFolder we have a connection to a remote computer that has this piece
			handleHave((BTHave)message);
			break;

		// This remote computer sent us a Piece message, it's sending us a range of file data within a numbered piece
		case BTMessage.PIECE:

			// Save the file data inside a Piece message to disk, and send more Request messages to ask for more
			handlePiece((BTPiece)message);
			break;

		// This remote computer sent us a Request message, it wants us to send some file data in a Piece message
		case BTMessage.REQUEST:

			// Respond to a Request message from this remote computer by sending a Piece message with the file data it asked for
			handleRequest((BTRequest)message);
			break;

		// This remote computer sent us a Cancel message, it's cancelling a previous request it made in a Request message
		case BTMessage.CANCEL:

			// Remove a request this remote computer made from our list of them
			handleCancel((BTCancel)message);
			break;
		}
	}

	/**
	 * Remove a request this remote computer made from our list of them, if we find one that matches a given Cancel message this remote computer sent us.
	 * 
	 * Only BTConnection.handleMessage() calls this method.
	 * This remote computer has sent us a Cancel message, cancelling a request for data made by a Request message it previously sent us.
	 * 
	 * @param message A Cancel message from this remote computer
	 */
	private void handleCancel(BTCancel message) {

		// Read the given Cancel message, getting the piece number and range within that piece that it's cancelling a previous request for
		BTInterval in = message.getInterval();

		/*
		 * removes the range from the list of requests. If we are already
		 * sending the piece, there is nothing we can do about it
		 */

		// Loop through the BTInterval objects in our _requested list, these are the requests this remote computer has asked for
		for (Iterator iter = _requested.iterator(); iter.hasNext(); ) {
			BTInterval current = (BTInterval) iter.next();

			// If the Cancel message matches or exceeds this request in the _requested list
			if (in.getId() == current.getId() && // If the piece numbers are the same, and
				(in.low <= current.high && current.low <= in.high)) { // The range the Cancel message clips out is the same or bigger than the range of the request

				// Remove the BTInterval from the _requested list
				iter.remove();
			}
		}
	}

	/**
	 * Respond to a Request message from this remote computer by sending a Piece message with the file data it asked for.
	 * 
	 * Only BTConnection.handleMessage() calls this method.
	 * This remote computer has sent us a Request message, asking for a part of the torrent we're sharing.
	 * 
	 * @param message The Request message from this remote computer
	 */
	private void handleRequest(BTRequest message) {

		/*
		 * we do not accept request from choked connections, if we just choked
		 * a connection, we may still receive some requests, though
		 */

		// If we're choking this remote computer
		if (_isChoked) {

			// We won't send this remote computer data while we're choking it, make a note and leave
			if (LOG.isDebugEnabled()) LOG.debug("got request while choked");
			return;
		}

		// Read the Request message to find out what piece number and data range within that piece this remote computer is asking us for
		BTInterval in = message.getInterval();
		if (LOG.isDebugEnabled()) LOG.debug("got request for " + in);

		/*
		 * ignore, that's a buggy client sending this request (didn't manage to
		 * find out which one) - we could also throw an exception causing us to
		 * disconnect...
		 */

		// If the Request message is asking for a piece number beyond the end of the file
		if (in.getId() > _info.getNumBlocks()) {

			// Make a note about a bad request and leave without doing anything
			if (LOG.isDebugEnabled()) LOG.debug("got bad request " + message);
			return;
		}

		/*
		 * we skip all requests for ranges larger than MAX_BLOCK_SIZED as
		 * proposed by the BitTorrent spec.
		 */

		// Make sure the data range isn't bigger than our limit for how much remote computers can ask us for
		if (in.high - in.low + 1 > MAX_BLOCK_SIZE) {

			// Requests can only clip out data in a single piece, make a note and leave without doing anything
			if (LOG.isDebugEnabled()) LOG.debug("got long request");
			return;
		}

		/*
		 * skip the message if we don't have that range
		 */

		// If our VerifyingFolder says we have that data
		if (_info.getVerifyingFolder().hasBlock(in.getId())) {

			// Add the range to our _requested list of ranges this remote computer has asked us for
			_requested.add(in);
		}

		// If we have a pending request from this remote computer, and no other messages to send it, send it a Piece message
		if (!_requested.isEmpty() && // If we have requests from this remote computer waiting, either from right now, or from before, and
			_writer.isIdle()) {      // We don't have any messages waiting to send this remote computer right now

			// Send this remote computer a Piece message with one of the ranges in the _requested list
			readyForWriting();
		}
	}

	/**
	 * Save the file data inside a Piece message to disk, and send more Request messages to ask for more.
	 * 
	 * Only handleMessage() above calls this method.
	 * This remote computer has sent us a Piece message with file data inside.
	 * 
	 * @param message The Piece message this remote computer sent us
	 */
	private void handlePiece(BTPiece message) {

		// Get where the data goes, and the data, from the Piece message this remote computer sent us
		final BTInterval in = message.getInterval(); // Tells the piece number, and the range within that piece
		final byte[] data = message.getData();       // A byte array with the file data

		// Record that we received this much more file data for this torrent
		readBytes(data.length);

		// Remove the range of data we got from our lists of what to ask this remote computer
		if (!_requesting.remove(in) && // Remove the interval we got from those we're going to ask for, if it wasn't in that list
			!_toRequest.remove(in)) {  // Try removing it from the list of intervals we did ask for, if it wasn't there either

			// Make a note that this remote computer sent us something we didn't ask for, and leave
			if (LOG.isDebugEnabled()) LOG.debug("received unexpected range " + in + " from " + _socket.getInetAddress() + " expected " + _requesting + " " + _toRequest);
			return;
		}

		try {

			// Get the torrent's VerifyingFolder object, which can save data to disk
			VerifyingFolder v = _info.getVerifyingFolder();

			// If we already have this piece, leave
			if (v.hasBlock(in.getId())) return;

			// Give the file data to the VerifyingFolder, which will save it to disk
			_info.getVerifyingFolder().writeBlock(in, data);

		// writeBlock() threw an exception, there was a problem writing the data to disk
		} catch (IOException ioe) {

			// Inform the user and stop the torrent
			IOUtils.handleException(ioe, null);
			_torrent.stop();
		}

		// Request more ranges if we don't have enough requests right now
		if (!_torrent.isComplete() &&                                // If we haven't downloaded this whole torrent yet, and
			_toRequest.size() + _requesting.size() < MAX_REQUESTS) { // We don't have 4 requests to make right now

			// Have the ManagedTorrent object pick a range to ask for, and send a Request message
			_torrent.request(this);
		}

		// Send this remote computer 4 Request messages from the BTIntervals in the _toRequest list
		enqueueRequests();
	}

	/**
	 * Tell the torrent's VerifyingFolder which pieces this remote computer has, and figure out what we can get from it.
	 * 
	 * Only BTConnection.handleMessage() calls this method.
	 * This remote computer has sent us a Bitfield message.
	 * BitTorrent programs exchange Bitfield messages right after the BitTorrent handshake.
	 * 
	 * Gives each piece number this remote computer has to the torrent's VerifyingFolder.
	 * The VerifyingFolder knows that we have a connection to a computer that has the piece, but doesn't know which computer has it.
	 * This lets the VerifyingFolder determine which pieces are rare and which are common.
	 * 
	 * Puts together _availableRanges, the BitSet which tells which pieces we need and this remote computer has.
	 * This tells what we can get from it.
	 * Sets numMissing, the number of pieces we have that this remote computer needs.
	 * This tells what it can get from us.
	 * 
	 * @param message The Bitfield message from this remote computer
	 */
	private void handleBitField(BTBitField message) {

		// Get the bit field, a byte array with a bit representing each piece this remote computer has or needs
		byte[] field = message.getBitField();

		// Find out how many pieces this torrent has
		int numBits = _info.getNumBlocks();

		// Calculate the number of bytes the bit field for that many pieces should be
		int bitFieldLength = (numBits + 7) / 8;

		// Make sure the bit field the remote computer gave us is the right length
		if (field.length != bitFieldLength) {

			// It's not, throw an IOException as NIO would
			handleIOException(new BadBTMessageException("bad bitfield received! " + _endpoint.toString()));
		}

		// Loop for each bit in the bit field
		for (int i = 0; i < numBits; i++) {
			byte mask = (byte)(0x80 >>> (i % 8));
			if ((mask & field[i / 8]) == mask) {

				/*
				 * now, addAvailablePiece() sends this remote computer an Interested message as soon as i is something we need.
				 * change this, do not send interested until all are added.
				 */

				// Tell the torrent's VerifyingFolder we have a connection to a remote compuer that has this piece
				addAvailablePiece(i);
			}
		}

		// Set numMissing to the number of pieces we have that this remote computer needs
		numMissing = _info.getVerifyingFolder().getNumMissing(_availableRanges);
	}

	/**
	 * Tell the torrent's VerifyingFolder we have a connection to a remote computer that has this piece.
	 * 
	 * @param message the Have message from this remote computer
	 */
	private void handleHave(BTHave message) {

		// Tell the torrent's VerifyingFolder we have a connection to a remote computer that has this piece
		int pieceNum = message.getPieceNum();
		addAvailablePiece(pieceNum);
	}

	/**
	 * Determine if this BTConnection object is the same as a given one.
	 * Compares their IP addresses and BitTorrent peer IDs.
	 * 
	 * @return true if they are the same, false if different
	 */
	public boolean equals(Object o) {

		// Make sure the given object is also a BTConnection
		if (o instanceof BTConnection) {
			BTConnection other = (BTConnection)o;

			// Compare their TorrentLocation _endpoint members
			return other._endpoint.equals(_endpoint); // Matches the IP addresses and peer IDs only
		}

		// Different
		return false;
	}

	/**
	 * Express this BTConnection as text.
	 * Composes a String like:
	 * 
	 * 1.2.3.4:5:LIMEguidguidguidguid:base32extensionbytes
	 * 
	 * The parts are separated by ":"
	 * First are the IP address and port number.
	 * After that is the 20-byte peer ID, converted into a String.
	 * The vendor code at the start will be readable, while the guid may not be.
	 * Last are the extension bytes, written in base 32 encoding.
	 * 
	 * @return A String
	 */
	public String toString() {

		// Turn the TorrentLocation that has the address of this remote computer into text
		return _endpoint.toString();
	}
}
