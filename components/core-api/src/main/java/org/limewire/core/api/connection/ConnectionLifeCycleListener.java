package org.limewire.core.api.connection;

public interface ConnectionLifeCycleListener {

    void handleEvent(ConnectionLifeCycleEventType type);

}
