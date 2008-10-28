package org.limewire.core.api.connection;

public interface ConnectionManager {

    public void addEventListener(ConnectionLifeCycleListener listener);

    public boolean isConnected();

    public boolean isFullyConnected();

    public void removeEventListener(ConnectionLifeCycleListener listener);

}
