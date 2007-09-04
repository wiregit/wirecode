package com.limegroup.gnutella.browser;

import java.net.Socket;

import org.limewire.net.ConnectionAcceptor;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.statistics.HTTPStat;

/**
 * Listener for control requests that dispatches them through ExternalControl.
 */
@Singleton
public class ControlRequestAcceptor implements ConnectionAcceptor {

    private final Provider<ExternalControl> externalControl;

    @Inject
    public ControlRequestAcceptor(Provider<ExternalControl> externalControl) {
        this.externalControl = externalControl;
    }

    public boolean isBlocking() {
        return true;
    }

    public void acceptConnection(String word, Socket sock) {
        if (word.equals("MAGNET"))
            HTTPStat.MAGNET_REQUESTS.incrementStat();
        externalControl.get().fireControlThread(sock, word.equals("MAGNET"));
    }
}
