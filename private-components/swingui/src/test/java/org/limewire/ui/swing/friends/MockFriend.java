package org.limewire.ui.swing.friends;

import java.beans.PropertyChangeListener;

import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence.Mode;

class MockFriend implements Friend {
    private final String id, name, status;
    private final Mode state;
    
    public MockFriend(String id, String name, String status, Mode state) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.status = status;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Mode getMode() {
        return state;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return null;
    }
}