package org.limewire.core.impl.connection;

import java.beans.PropertyChangeListener;

import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.core.api.connection.ConnectionStrength;

import com.google.inject.Singleton;

@Singleton
public class MockConnectionManagerImpl implements GnutellaConnectionManager {

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub

    }

    public boolean isUltrapeer() {
        return false;
    }

    @Override
    public void restart() {

    }

    @Override
    public ConnectionStrength getConnectionStrength() {
        return ConnectionStrength.FULL;
    }

}
