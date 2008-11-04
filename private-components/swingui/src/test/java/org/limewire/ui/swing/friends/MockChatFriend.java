package org.limewire.ui.swing.friends;

import java.beans.PropertyChangeListener;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.Network;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.Presence.Mode;

class MockChatFriend implements ChatFriend {
    private String name, status;
    private Mode state;
    private boolean activeConversation;
    private boolean receivingUnviewedMessages;
    
    public MockChatFriend(String name, String status, Mode state) {
        this.name = name;
        this.state = state;
        this.status = status;
    }
    
    @Override
    public Friend getFriend() {
        return new Friend() {
            @Override
            public String getId() {
                return name;
            }
            @Override
            public String getName() {
                return name;
            }
            @Override
            public String getRenderName() {
                return name;
            }

            public void setName(String name) {
                MockChatFriend.this.name = name;
            }
            
            @Override
            public boolean isAnonymous() {
                return false;
            }

            public Network getNetwork() {
                return null;
            }
        };
    }

    @Override
    public String getID() {
        //Use name for now - add an id field if needed in the future
        return name;
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
        //no-op
    }

    @Override
    public void stopChat() {
        //no-op
    }

    @Override
    public boolean isActiveConversation() {
        return activeConversation;
    }

    @Override
    public void setActiveConversation(boolean active) {
        this.activeConversation = active;
    }

    @Override
    public boolean isSignedInToLimewire() {
        return false;
    }

    @Override
    public boolean isSignedIn() {
        return false;
    }

    @Override
    public boolean isReceivingUnviewedMessages() {
        return receivingUnviewedMessages;
    }

    @Override
    public void setReceivingUnviewedMessages(boolean hasMessages) {
        this.receivingUnviewedMessages = hasMessages;
    }

    public Presence getPresence() {
        return null;
    }

    @Override
    public void setMode(Mode mode) {
        this.state = mode;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public void releasePresence(Presence presence) {
        // TODO Auto-generated method stub
    }

    @Override
    public void updatePresence(Presence presence) {
        // TODO Auto-generated method stub
    }
}