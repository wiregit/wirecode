package org.limewire.net.address;

import java.io.IOException;

import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.net.SocketsManager;
import org.limewire.nio.observer.ConnectObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This class handles connecting to addresses of type {@link Connectable}.
 */
@Singleton
public class ConnectableConnector implements AddressConnector {

    private final SocketsManager socketsManager;

    @Inject
    public ConnectableConnector(SocketsManager socketsManager) {
        this.socketsManager = socketsManager;
    }
    
    @Override
    public boolean canConnect(Address address) {
        return address instanceof Connectable;
    }

    @Override
    public void connect(Address address, int timeout, ConnectObserver observer) {
        Connectable connectable = (Connectable)address;
        try {
            socketsManager.connect(connectable.getInetSocketAddress(), timeout, observer);
        } catch (IOException e) {
            observer.handleIOException(e);
        }
    }

}
