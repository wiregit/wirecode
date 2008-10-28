package org.limewire.core.impl.connection;

import org.limewire.core.api.connection.ConnectionLifeCycleListener;
import org.limewire.core.api.connection.ConnectionManager;

import com.google.inject.Singleton;

@Singleton
public class MockConnectionManagerImpl implements ConnectionManager {

    @Override
    public void addEventListener(ConnectionLifeCycleListener listener) {

    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isFullyConnected() {
        return true;
    }

    @Override
    public void removeEventListener(ConnectionLifeCycleListener listener) {

    }

}
