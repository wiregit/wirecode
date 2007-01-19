package com.limegroup.bittorrent.handshaking;

import org.limewire.nio.AbstractNBSocket;

import com.limegroup.bittorrent.TorrentManager;

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
