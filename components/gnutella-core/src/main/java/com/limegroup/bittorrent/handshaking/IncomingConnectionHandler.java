package com.limegroup.bittorrent.handshaking;

import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.io.AbstractNBSocket;

public class IncomingConnectionHandler {
	private static IncomingConnectionHandler instance;
	public static IncomingConnectionHandler instance() {
		if (instance == null)
			instance = new IncomingConnectionHandler();
		return instance;
	}
	
	protected IncomingConnectionHandler(){}
	
	public void handleIncoming(AbstractNBSocket s, TorrentManager t) {
		IncomingBTHandshaker shaker = 
			new IncomingBTHandshaker(s, t);
		shaker.startHandshaking();
	}
}
