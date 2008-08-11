package org.limewire.ui.swing.friends;

import java.beans.PropertyChangeListener;

import org.limewire.xmpp.api.client.Presence.Mode;

class MockFriend implements Friend {
    private final String name, status;
    private final Mode state;
    
    public MockFriend(String name, String status, Mode state) {
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
}