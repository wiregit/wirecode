package com.limegroup.gnutella.connection;

import org.limewire.listener.DefaultEvent;

import com.limegroup.gnutella.GUID;

public class GnutellaConnectionEvent extends DefaultEvent<GnutellaConnection, GnutellaConnection.EventType> {
    private final GUID guid;

    public GnutellaConnectionEvent(GnutellaConnection source, GnutellaConnection.EventType event, GUID guid) {
        super(source, event);
        this.guid = guid;
    }

    public GUID getGuid() {
        return guid;
    }
}
