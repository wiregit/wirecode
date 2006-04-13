package com.limegroup.bittorrent;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.handshaking.BTHandshaker;
import com.limegroup.bittorrent.handshaking.OutgoingBTHandshaker;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.io.NIOSocket;
import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.Sockets;

public class BTConnectionFetcher implements Runnable {
	
	private static final Log LOG = LogFactory.getLog(BTConnectionFetcher.class);
	
	/*
	 * final String we send as a header for bittorrent connections
	 */
	private static final String BITTORRENT_PROTOCOL = "BitTorrent protocol";

	/*
	 * same as above as byte array
	 */
	public static byte[] BITTORRENT_PROTOCOL_BYTES;
	static {
		try {
			BITTORRENT_PROTOCOL_BYTES = BITTORRENT_PROTOCOL
					.getBytes(Constants.ASCII_ENCODING);
		} catch (UnsupportedEncodingException e) {
			ErrorService.error(e);
		}
	}
	
	/*
	 * extension bytes
	 */
	static final byte[] EXTENSION_BYTES = new byte[] { 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x02 };

	/*
	 * Max concurrent connection threads
	 */
	private static final int MAX_CONNECTORS = 5;
	
	/*
	 * time in milliseconds between connection attempts
	 */
	private static final int TIME_BETWEEN_CONNECTIONS = 20 * 1000;
	
	/*
	 * the List of concurrent connector threads
	 */
	final Set fetchers = 
		Collections.synchronizedSet(new HashSet());
	
	private volatile boolean _isScheduled;
	
	final ManagedTorrent _torrent;

	/**
	 * The fixed outgoing handshake for this torrent.
	 */
	final ByteBuffer _handshake;
	
	private boolean shutdown;
	
	BTConnectionFetcher(ManagedTorrent torrent, byte[]peerId) {
		_isScheduled = false;
		_torrent = torrent;
		ByteBuffer handshake = ByteBuffer.allocate(68);
		handshake.put((byte) BITTORRENT_PROTOCOL.length());
		handshake.put(BITTORRENT_PROTOCOL_BYTES);
		handshake.put(EXTENSION_BYTES);
		handshake.put(_torrent.getInfoHash());
		handshake.put(peerId);
		handshake.flip();
		
		_handshake = handshake.asReadOnlyBuffer(); // this actually does nothing :(
	}

	/**
	 * schedule a new connection attempt if non is scheduled so far
	 * 
	 * @param waitTime
	 *            the number of milliseconds to wait before starting the
	 *            BTConnectionFetcher
	 */
	public void schedule(long waitTime) {
		if (!_isScheduled) {
			_isScheduled = true;
			if (_torrent.hasStopped()) 
				return;
			if (LOG.isDebugEnabled())
				LOG.debug("rescheduling connection fetcher in " + waitTime);
			RouterService.schedule(this, waitTime, 0);
		}
	}

	/**
	 * main method
	 */
	public synchronized void run() {
		if(shutdown)
			return;
		
		_isScheduled = false;
		
		if (_torrent.shouldStop()) {
			_torrent.stop();
			return;
		}

		while (!_torrent.hasStopped() && 
				fetchers.size() < MAX_CONNECTORS &&
				_torrent.needsMoreConnections() && 
				_torrent.hasNonBusyLocations()) {
			fetchConnection();
			if (LOG.isDebugEnabled())
				LOG.debug("started connection fetcher: "
						+ fetchers.size());
		}
	}


	void reschedule() {
		synchronized(this) {
			if(shutdown)
				return;
		}
		
		if (_torrent.needsMoreConnections()) {
			long waitTime = 
				Math.max(_torrent.calculateWaitTime(), TIME_BETWEEN_CONNECTIONS);
			
			schedule(waitTime);
		}
	}

	/**
	 * Starts a connector thread if possible
	 */
	private void fetchConnection() {

		TorrentLocation ep = _torrent.getTorrentLocation();

		if (ep == null) {
			if (LOG.isDebugEnabled())
				LOG.debug("no hosts to connect to");
			return;
		}
		
		OutgoingBTHandshaker connector = new OutgoingBTHandshaker(ep, _torrent);
		fetchers.add(connector);
		try {
			Sockets.connect(ep.getAddress(),
					ep.getPort(), Constants.TIMEOUT, connector);
		} catch (IOException impossible) {
			ErrorService.error(impossible);
		}
	}
	
	synchronized void shutdown() {
			if (shutdown)
				return;
			shutdown = true;
		synchronized(fetchers) {
			// copy because shutdown() removes
			List conns = new ArrayList(fetchers);
			for (Iterator iter = conns.iterator(); iter.hasNext();) {
				Shutdownable connector = (Shutdownable) iter.next();
				connector.shutdown();
			}
		}
	}

	/**
	 * Accept a bittorrent connection
	 * 
	 * @param sock
	 *            the <tt>Socket</tt> for which to accept the connection
	 */
	public void acceptConnection(Socket sock, byte[] extensionBytes) {
		TorrentLocation ep;

		LOG.debug("got incoming connection "
				+ sock.getInetAddress().getHostAddress());

		try {
			InputStream in = sock.getInputStream();
			// read peer ID, everything else has already been consumed
			byte[] peerId = new byte[20];
			for (int i = 0; i < peerId.length; i += in.read(peerId))
				;

			ep = new TorrentLocation(sock.getInetAddress(), sock.getPort(),
					new String(peerId, Constants.ASCII_ENCODING),
					extensionBytes);
		} catch (IOException ioe) {
			IOUtils.close(sock);
			return;
		}

		if (!_torrent.allowIncomingConnection(ep)) {
			LOG.debug("no more connection slots");
			IOUtils.close(sock);
			return;
		}

		// send our part of the handshake
		try {
			ByteBuffer handshake = _handshake.duplicate();
			sock.getChannel().write(handshake);

			BTConnection btc = new BTConnection((NIOSocket) sock, 
					_torrent.getMetaInfo(), 
					ep,
					_torrent);

			// now add the connection to the Choker and to this:
			_torrent.addConnection(btc);
			if (LOG.isDebugEnabled())
				LOG.debug("added incoming connection "
						+ sock.getInetAddress().getHostAddress());
		} catch (IOException ioe) {
			IOUtils.close(sock);
		}
	}
	
	public ByteBuffer getOutgoingHandshake() {
		return _handshake.duplicate();
	}

	public void handshakerStarted(BTHandshaker shaker) {
		fetchers.add(shaker);
	}
	
	public void handshakerDone(BTHandshaker shaker) {
		if (fetchers.remove(shaker))
			reschedule();
	}
}
