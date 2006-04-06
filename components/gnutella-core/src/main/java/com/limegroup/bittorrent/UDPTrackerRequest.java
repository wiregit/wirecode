package com.limegroup.bittorrent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.RouterService;

/**
 * handle UDP tracker requests
 * 
 * TODO this doesn't work yet
 */
public class UDPTrackerRequest extends TrackerRequester {
	private static final Log LOG = LogFactory.getLog(TrackerRequester.class);

	// 20 seconds
	private static final int UDP_TRACKER_TIMEOUT = 20 * 1000;

	// try tracker 3 times in a row (at most)
	private static final int UDP_TRACKER_RETRIES = 3;

	/*
	 * constant message identifiers
	 */
	private static final int ACTION_CONNECT = 0;

	private static final int ACTION_ANNOUNCE = 1;

	// we don't support scaping, it doesn't return any useful information, just
	// some general statistics about the tracker. Everything that may be even
	// remotely of use is already transmitted over the regular tracker request
	// private static final int ACTION_SCRAPE = 2;

	private static final int ACTION_ERROR = 3;

	// needed to distinguish between concurrent requests to the same tracker
	private final int TRANSACTION_ID;

	// the tracker address
	private final InetAddress ADDRESS;

	// tracker port
	private final int PORT;

	private final BTMetaInfo INFO;

	private final ManagedTorrent TORRENT;

	private final int EVENT;

	// lock to wait for tracker response
	private final Object _messageLock = new Object();

	/*
	 * integer telling the process() what to expect as the next packet, the
	 * first message we expect is the connect reply
	 */
	private int _status = ACTION_CONNECT;

	/*
	 * the connection id assigned to us by the tracker initialized to
	 * 0x41727101980L as expected by the protocol
	 */
	private long _connectionId = 0x41727101980L;

	/*
	 * the response that will be returned by connectUDP
	 */
	private TrackerResponse _response = null;

	public UDPTrackerRequest(URL url, BTMetaInfo info, ManagedTorrent torrent,
			int event) throws IOException {
		ADDRESS = InetAddress.getByName(url.getHost());
		PORT = url.getPort();

		INFO = info;
		TORRENT = torrent;
		EVENT = event;
		TRANSACTION_ID = (int) Math.random() * Integer.MAX_VALUE;
	}

	public TrackerResponse connectUDP() {
		// we try UDP_TRACKER_RETRIES times or until we get a response, -
		// this
		// response may be bad but we won't retry for a while after
		// receiving it
		for (int i = 0; i < UDP_TRACKER_RETRIES && _response == null; i++) {
			sendConnectRequest();
			// we expect to be notified once _response has been set
			try {
				_messageLock.wait(UDP_TRACKER_TIMEOUT);
			} catch (InterruptedException ie) {
				// ignore
			}
		}
		return _response;
	}

	private void sendConnectRequest() {
		// TODO
		// UDPService doesn't handle sending raw byte arrays, plus we need to
		// register some kind of message listener with UDPServie...
		// UDPService.instance().send(createConnectRequest(), ADDRESS, PORT);
	}

	private void sendAnnounceRequest() {
		// TODO
		// UDPService doesn't handle sending raw byte arrays, plus we need to
		// register some kind of message listener with UDPServie...
		// UDPService.instance().send(createAnnounceRequest(), ADDRESS, PORT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.limegroup.gnutella.UDPMessageHandler#process(java.net.DatagramPacket)
	 */
	public void process(DatagramPacket datagram) {
		// message validation, primarily checks if the message was for us. If it
		// scceeds we have reason enough to believe that this is supposed to be
		// a tracker response and that it is intended for us
		if (!checkMessage(datagram))
			return;

		// here we do the real message handling, depending on what state the
		// request is in
		if (_status == ACTION_CONNECT) {
			try {
				_connectionId = parseConnectResponse(datagram.getData());
				sendAnnounceRequest();
				_status = ACTION_ANNOUNCE;
			} catch (BadTrackerResponseException btre) {
				// zero tolerance for bad tracker responses
				_response = new TrackerResponse(new ArrayList(), 0, 0, 0, btre
						.getMessage());
				_messageLock.notify();
			}
		} else if (_status == ACTION_ANNOUNCE) {
			try {
				_response = parseAnnounceResponse(datagram.getData());
				_messageLock.notify();
			} catch (BadTrackerResponseException btre) {
				// zero tolerance again;
				_response = new TrackerResponse(new ArrayList(), 0, 0, 0, btre
						.getMessage());
				_messageLock.notify();
			}
		}
	}

	private boolean checkMessage(DatagramPacket datagram) {
		if (!datagram.getAddress().equals(ADDRESS)
				|| datagram.getPort() != PORT)
			// message is not for us...
			return false;

		// the transaction id of every message identifying the session are
		// always bytes 4-7 of any UDP tracker response
		if (datagram.getData().length < 8
				|| TRANSACTION_ID != ByteOrder.beb2int(datagram.getData(), 4))
			// if the transaction id does not match the message is not for us
			// either
			return false;

		return true;
	}

	/**
	 * creates a connection message, - always send this message before sending
	 * anything else.
	 * 
	 * @param transactionId
	 *            the ID for this tracker session, needed for concurrent tracker
	 *            requests
	 * @return byte array containing the message body
	 */
	private byte[] createConnectRequest() {
		byte[] message = new byte[16];
		ByteOrder.long2beb(_connectionId, message, 0);
		ByteOrder.int2beb(ACTION_CONNECT, message, 8);
		ByteOrder.int2beb(TRANSACTION_ID, message, 12);
		return message;
	}

	/**
	 * creates a new announce request message
	 * 
	 * @param connectionId
	 *            the session id created by the server and sent to us via
	 *            connect response
	 * @param transactionId
	 *            the session id we generated
	 * @param event
	 *            the event we should send to the tracker
	 * @param info
	 *            the BTMetaInfo for this download
	 * @param torrent
	 *            the ManagedTorrent for this download
	 * @return a byte array containing the message body
	 */
	private byte[] createAnnounceRequest() {
		byte[] message = new byte[98];
		// first the session id from the server
		ByteOrder.long2beb(_connectionId, message, 0);
		// the action, ACTION_ANNOUNCE
		ByteOrder.int2beb(ACTION_ANNOUNCE, message, 8);
		// the session id that we created, - don't ask me why the redundancy...
		ByteOrder.int2beb(TRANSACTION_ID, message, 12);
		// the info hash
		System.arraycopy(INFO.getInfoHash(), 0, message, 16, 20);
		// our peer id
		System.arraycopy(RouterService.getTorrentManager().getPeerId(), 0,
				message, 36, 20);
		// the amound downloaded this session
		ByteOrder
				.long2beb(TORRENT.getDownloader().getAmountRead(), message, 56);
		// the amount left to download
		ByteOrder.long2beb(INFO.getTotalSize()
				- INFO.getVerifyingFolder().getBlockSize(), message, 72);
		// the amount uploaded this session
		ByteOrder.long2beb(TORRENT.getUploader().getTotalAmountUploaded(),
				message, 72);
		// the event like in the regular http tracker request
		ByteOrder.int2beb(EVENT, message, 80);
		// originally our IP address but 0 since we want the remote host to use
		// the ip from the UDP message
		ByteOrder.int2beb(0, message, 84);
		// 4 byte unique key for the session, - doesn't make sense at all, use
		// part of the peerId. Same as 'key' parameter in HTTP tracker requests
		System.arraycopy(RouterService.getTorrentManager().getPeerId(), 16,
				message, 88, 4);
		// our listening port
		ByteOrder.int2beb(RouterService.getPort(), message, 92);
		// there seem to be some disagreements about the bytes 94-95, according
		// to some specs there is an extension here, - but Azureus doesn't
		// support it and neither do we
		return message;
	}

	/**
	 * create authentication message
	 * 
	 * @param connectId
	 *            the long returned by a previous connect response
	 * @return
	 * @throws BadTrackerResponseException
	 */

	private static long parseConnectResponse(byte[] message)
			throws BadTrackerResponseException {
		int code = ByteOrder.beb2int(message, 0, 4);
		if (code == ACTION_CONNECT) {
			if (message.length != 16)
				throw new BadTrackerResponseException(
						"bad connect message length");
			return ByteOrder.beb2long(message, 8, 8);
		} else if (code == ACTION_ERROR)
			throw new BadTrackerResponseException(parseErrorResponse(message));
		else
			throw new BadTrackerResponseException("unknown tracker message "
					+ code);
	}

	/**
	 * @param message
	 *            the message to parse
	 * @return a new TrackerResponse
	 * @throws BadTrackerResponseException
	 */
	private static TrackerResponse parseAnnounceResponse(byte[] message)
			throws BadTrackerResponseException {

		// parse the code, identifying the message
		int code = ByteOrder.beb2int(message, 0, 4);
		if (code == ACTION_ERROR)
			throw new BadTrackerResponseException(parseErrorResponse(message));
		if (code != ACTION_ANNOUNCE)
			throw new BadTrackerResponseException("unknown tracker message "
					+ code);

		if (message.length < 20)
			throw new BadTrackerResponseException(
					"short announce tracker message");

		int interval = ByteOrder.beb2int(message, 8);
		int leechers = ByteOrder.beb2int(message, 12);
		int seeders = ByteOrder.beb2int(message, 16);

		// get the interesting part, the peer addresses
		byte[] peerBytes = new byte[message.length - 20];
		System.arraycopy(message, 20, peerBytes, 0, peerBytes.length);

		List peers = new ArrayList();

		try {
			peers = TrackerResponse.parsePeers(peerBytes);
		} catch (ValueException ve) {
			LOG.debug(ve);
		}
		return new TrackerResponse(peers, interval, leechers + seeders,
				seeders, null);
	}

	/**
	 * @param message
	 *            the message to parse
	 * @return human readable error String from the message
	 * @throws BadTrackerResponseException
	 */
	private static String parseErrorResponse(byte[] message)
			throws BadTrackerResponseException {
		if (message.length < 8)
			throw new BadTrackerResponseException("bad error message length");
		byte[] error = new byte[message.length - 8];
		System.arraycopy(message, 8, error, 0, error.length);

		try {
			return new String(error, Constants.ASCII_ENCODING);
		} catch (UnsupportedEncodingException uee) {
			return null;
		}
	}
}
