package com.limegroup.bittorrent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.handshaking.BTHandshaker;
import com.limegroup.bittorrent.handshaking.OutgoingBTHandshaker;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.util.Sockets;

public class BTConnectionFetcher  {
	
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
	private static final byte[] EXTENSION_BYTES = new byte[8];

	/*
	 * Max concurrent connection attempts
	 */
	private static final int MAX_CONNECTORS = 5;
	
	/*
	 * the Set of concurrent connection fetchers
	 */
	final Set<BTHandshaker> fetchers = 
		Collections.synchronizedSet(new HashSet<BTHandshaker>());
	
	/**
	 * the torrent this fetcher belongs to
	 */
	final ManagedTorrent _torrent;

	/**
	 * The fixed outgoing handshake for this torrent.
	 */
	final ByteBuffer _handshake;
	
	/**
	 * Whether this fetcher is shutdown.
	 */
	private volatile boolean shutdown;
	
	
	BTConnectionFetcher(ManagedTorrent torrent) {
		_torrent = torrent;
		ByteBuffer handshake = ByteBuffer.allocate(68);
		handshake.put((byte) BITTORRENT_PROTOCOL.length()); // 19
		handshake.put(BITTORRENT_PROTOCOL_BYTES); // "BitTorrent protocol"
		handshake.put(EXTENSION_BYTES);
		handshake.put(_torrent.getInfoHash());
		handshake.put("LIME".getBytes());
		handshake.put(RouterService.getMyGUID());
		handshake.flip();
		
		_handshake = handshake.asReadOnlyBuffer(); 
	}
	
	public synchronized void fetch() {
		if (shutdown)
			return;
		
		while (_torrent.isActive() && 
				fetchers.size() < MAX_CONNECTORS &&
				_torrent.needsMoreConnections() && 
				_torrent.hasNonBusyLocations()) {
			fetchConnection();
			if (LOG.isDebugEnabled())
				LOG.debug("started connection fetcher: "
						+ fetchers.size());
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
		try {
			Socket s = Sockets.connect(ep.getAddress(),
					ep.getPort(), Constants.TIMEOUT, connector);
			connector.setSock(s);
		} catch (IOException impossible) {
			ErrorService.error(impossible);
		}
		fetchers.add(connector);
	}
	
	synchronized void shutdown() {
		if (shutdown)
			return;
		shutdown = true;
		synchronized(fetchers) {
			// copy because shutdown() removes
			List<Shutdownable> conns = new ArrayList<Shutdownable>(fetchers);
			for (Shutdownable connector : conns) 
				connector.shutdown();
		}
	}

	public ByteBuffer getOutgoingHandshake() {
		return _handshake.duplicate();
	}

	public void handshakerStarted(BTHandshaker shaker) {
		fetchers.add(shaker);
	}
	
	public void handshakerDone(BTHandshaker shaker) {
		fetchers.remove(shaker);
		fetch();
	}
}
