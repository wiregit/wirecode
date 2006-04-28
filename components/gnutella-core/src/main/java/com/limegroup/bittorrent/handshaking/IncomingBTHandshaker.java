package com.limegroup.bittorrent.handshaking;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.io.NIOSocket;

public class IncomingBTHandshaker extends BTHandshaker {

	private static final Log LOG = LogFactory.getLog(IncomingBTHandshaker.class);
	
	private static final byte[] PROTOCOL = new byte[] { 'p', 'r', 'o', 't',
		'o', 'c', 'o', 'l' };
	
	private final TorrentManager manager;
	
	public IncomingBTHandshaker(NIOSocket sock, TorrentManager manager) {
		super(new TorrentLocation(sock.getInetAddress(), 
				sock.getPort(),
				new byte[20],
				new byte[8]));
		this.sock = sock;
		this.manager = manager;
	}
	
	public void startHandshaking() {
		initIncomingHandshake();
		setReadInterest();
	}
	
	protected boolean verifyIncoming() {
		for(; 
		currentBufIndex < incomingHandshake.length &&
		!incomingHandshake[currentBufIndex].hasRemaining();
		currentBufIndex++) {
			ByteBuffer current = incomingHandshake[currentBufIndex];
			switch(currentBufIndex) {
			case 0 : // 'protocol'
				if (!Arrays.equals(current.array(), PROTOCOL))
					return false;
				break;
			case 1 : // extention bytes
				break;
			case 2 : // infoHash
				torrent = 
					manager.getTorrentForHash(current.array());
				if (torrent == null) {
					if (LOG.isDebugEnabled())
						LOG.debug("incoming connection for unknown torrent");
					return false;
				}
				else {
					initOutgoingHandshake();
					setWriteInterest();
					torrent.getFetcher().handshakerStarted(this);
				}
				break;
			case 3 : // peerId
				break;
			}
		}
		
		return true;
	}

	protected void initIncomingHandshake() {
		incomingHandshake = new ByteBuffer[4];
		incomingHandshake[0] = ByteBuffer.allocate(8); // protocol bytes
		incomingHandshake[1] = ByteBuffer.wrap(loc.getExtBytes());
		incomingHandshake[2] = ByteBuffer.allocate(20); // infohash
		incomingHandshake[3] = ByteBuffer.wrap(loc.getPeerID());
	}

}
