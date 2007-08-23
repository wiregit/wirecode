package com.limegroup.gnutella.browser;

import java.net.Socket;

import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.ConnectionDispatcher;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.statistics.HTTPStat;

/**
 * Listener for control requests that dispatches them through
 * ExternalControl.
 */
@Singleton
public class ControlRequestAcceptor implements ConnectionAcceptor {
    
    private final Provider<ExternalControl> externalControl;
    private final Provider<ConnectionDispatcher> connectionDispatcher;
    
    @Inject
	public ControlRequestAcceptor(Provider<ExternalControl> externalControl,
            Provider<ConnectionDispatcher> connectionDispatcher) {
        this.externalControl = externalControl;
        this.connectionDispatcher = connectionDispatcher;
    }

    public void register() {
		connectionDispatcher.get().addConnectionAcceptor(this,
				true,
				true,
				"MAGNET","TORRENT");
	}
	
	public void acceptConnection(String word, Socket sock) {
		if (word.equals("MAGNET"))
			HTTPStat.MAGNET_REQUESTS.incrementStat(); 
		externalControl.get().fireControlThread(sock, word.equals("MAGNET"));
	}
}
