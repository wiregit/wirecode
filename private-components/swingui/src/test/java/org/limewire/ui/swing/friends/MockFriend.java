package org.limewire.ui.swing.friends;

import java.beans.PropertyChangeListener;

import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
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

    MessageReader reader;
    MessageWriter writer;
    @Override
    public MessageWriter createChat(MessageReader reader) {
        this.reader = reader;
        return writer;
    }

    long chatStartTime;
    @Override
    public long getChatStartTime() {
        return chatStartTime;
    }

    boolean chatting;
    @Override
    public boolean isChatting() {
        return chatting || chatStartTime > 0l;
    }

    @Override
    public void startChat() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stopChat() {
        throw new UnsupportedOperationException();
    }
}