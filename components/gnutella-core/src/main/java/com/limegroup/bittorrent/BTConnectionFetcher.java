package com.limegroup.bittorrent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.handshaking.BTHandshaker;
import com.limegroup.bittorrent.handshaking.IncomingBTHandshaker;
import com.limegroup.bittorrent.handshaking.OutgoingBTHandshaker;
import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.util.Sockets;
import com.limegroup.gnutella.util.StrictIpPortSet;

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
	
	/**
	 * the Set of outgoing connection fetchers
	 */
	final StrictIpPortSet<OutgoingBTHandshaker> outgoing =  
		new StrictIpPortSet<OutgoingBTHandshaker>();
	
	/**
	 * the Set of incoming connection hadnshakers
	 */
	final StrictIpPortSet<IncomingBTHandshaker> incoming = 
		new StrictIpPortSet<IncomingBTHandshaker>();
	
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
		// Note: with gathering writes everything but the info hash 
		// can be shared.
		_handshake = handshake.asReadOnlyBuffer(); 
	}
	
	public synchronized void fetch() {
		if (shutdown)
			return;
		
		while (_torrent.isActive() && 
				outgoing.size() < MAX_CONNECTORS &&
				_torrent.needsMoreConnections() && 
				_torrent.hasNonBusyLocations()) {
			fetchConnection();
			if (LOG.isDebugEnabled())
				LOG.debug("started connection fetcher: "
						+ outgoing.size());
		}
	}


	/**
	 * Starts a connector thread if possible
	 */
	private void fetchConnection() {

		// get a location to connect to that we know is not currently
		// trying to connect to us.
		TorrentLocation ep = null;
		do {
			ep = _torrent.getTorrentLocation();
			if (ep == null) {
				LOG.debug("no hosts to connect to");
				return;
			}
		} while (incoming.contains(ep) || outgoing.contains(ep));

		OutgoingBTHandshaker connector = new OutgoingBTHandshaker(ep, _torrent);
		try {
			Socket s = Sockets.connect(ep.getAddress(),
					ep.getPort(), Constants.TIMEOUT, connector);
			connector.setSock(s);
		} catch (IOException impossible) {
			ErrorService.error(impossible);
		}
		outgoing.add(connector);
	}
	
	void shutdown() {
		List<Shutdownable> conns; 
		synchronized(this) {
			if (shutdown)
				return;
			shutdown = true;
			// copy because shutdown() removes
			conns =	new ArrayList<Shutdownable>(outgoing.size()+incoming.size());
			conns.addAll(incoming);
			conns.addAll(outgoing);
		}
		for (Shutdownable connector : conns) 
			connector.shutdown();
	}

	public ByteBuffer getOutgoingHandshake() {
		return _handshake.duplicate();
	}

	public void handshakerStarted(IncomingBTHandshaker shaker) {
		if (LOG.isDebugEnabled())
			LOG.debug("incoming handshaker from "+shaker.getInetAddress()+":"+shaker.getPort());
		
		boolean reject;
		synchronized(this) {
			reject = shutdown || outgoing.contains(shaker);
			if (!reject)
				incoming.add(shaker);
		}
		if (reject) {
			LOG.debug("rejecting it");
			shaker.shutdown();
		}
	}
	
	public synchronized void handshakerDone(BTHandshaker shaker) {
		Assert.that(incoming.contains(shaker) != outgoing.contains(shaker));
		if (!incoming.remove(shaker)) {
			outgoing.remove(shaker);
			fetch();
		}
	}
}
