package com.limegroup.gnutella.browser;

import java.net.Socket;

import com.limegroup.gnutella.ConnectionAcceptor;
import com.limegroup.gnutella.ConnectionDispatcher;
import com.limegroup.gnutella.statistics.HTTPStat;

/**
 * Listener for control requests that dispatches them through
 * ExternalControl.
 */
public class ControlRequestAcceptor implements ConnectionAcceptor {
	
	public void register(ConnectionDispatcher dispatcher) {
		dispatcher.addConnectionAcceptor(this,
				new String[]{"MAGNET","TORRENT"},
				true,
				true);
	}
	
	public void acceptConnection(String word, Socket sock) {
		if (word.equals("MAGNET"))
			HTTPStat.MAGNET_REQUESTS.incrementStat(); 
		ExternalControl.fireControlThread(sock, word.equals("MAGNET"));
	}
}
