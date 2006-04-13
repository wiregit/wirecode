package com.limegroup.bittorrent.handshaking;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.BTConnectionFetcher;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.gnutella.io.ConnectObserver;
import com.limegroup.gnutella.io.NIOSocket;

public class OutgoingBTHandshaker extends BTHandshaker 
implements ConnectObserver {
	private static final Log LOG = LogFactory.getLog(OutgoingBTHandshaker.class);
	
	private TorrentLocation loc;
	
	/**
	 * creates an outgoing handshaker to the given location for
	 * the given torrent.
	 */
	public OutgoingBTHandshaker(TorrentLocation loc, ManagedTorrent torrent) {
		this.loc = loc;
		this.torrent = torrent;
	}
	
	public void handleConnect(Socket socket) throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("established connection to "+socket.getInetAddress());
		
		sock = (NIOSocket)socket;
		
		// connection was established - init the buffers and interests.
		initOutgoingHandshake();
		initIncomingHandshake();
		setWriteInterest();
		setReadInterest();
	}
	
	protected void initIncomingHandshake() {
		incomingHandshake = new ByteBuffer[5];
		incomingHandshake[0] = ByteBuffer.allocate(1); // 19
		byte []tmp = new byte[19];
		incomingHandshake[1] = ByteBuffer.wrap(tmp); // protocol identifier
		extBytes = new byte[8];
		incomingHandshake[2] = ByteBuffer.wrap(extBytes); // extention bytes
		tmp = new byte[20];
		incomingHandshake[3] = ByteBuffer.wrap(tmp); // infoHash
		peerId = new byte[20];
		incomingHandshake[4] = ByteBuffer.wrap(peerId); // peerID
	}
	
	protected boolean verifyIncoming() {
		for(; 
		currentBufIndex < incomingHandshake.length &&
		!incomingHandshake[currentBufIndex].hasRemaining();
		currentBufIndex++) {
			ByteBuffer current = incomingHandshake[currentBufIndex];
			switch(currentBufIndex) {
			case 0 : // 0x19
				current.flip();
				if (current.get() != (byte)19) 
					return false;
				current.position(1); // mark it as full
				break;
			case 1 : // bittorrent protocol
				if (!Arrays.equals(current.array(), BTConnectionFetcher.BITTORRENT_PROTOCOL_BYTES))
					return false;
				break;
			case 2 : // extention bytes
				break;
			case 3 : // infoHash
				if (!Arrays.equals(current.array(), torrent.getInfoHash()))
					return false;
				break;
			case 4 : // peerId
				break;
			}
		}
		
		return true;
	}
	
	public void handleIOException(IOException iox) {
		if (LOG.isDebugEnabled())
			LOG.debug("Connection failed: " + loc);
		loc.strike();
		if (!loc.isOut())
			torrent.addEndpoint(loc);
		else
			torrent.addBadEndpoint(loc);
		super.handleIOException(iox);
	}
}
