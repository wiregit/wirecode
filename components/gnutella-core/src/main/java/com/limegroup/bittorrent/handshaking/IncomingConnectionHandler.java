package com.limegroup.bittorrent.handshaking;

import org.limewire.nio.AbstractNBSocket;

import com.google.inject.Singleton;
import com.limegroup.bittorrent.TorrentManager;

@Singleton
public class IncomingConnectionHandler {
	
	public void handleIncoming(AbstractNBSocket s, TorrentManager t) {
		IncomingBTHandshaker shaker = 
			new IncomingBTHandshaker(s, t);
		shaker.startHandshaking();
	}
}
