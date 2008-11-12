package org.limewire.core.api.connection;

public interface ConnectionLifecycleListener {

    void handleEvent(ConnectionLifecycleEventType type);

}
