package org.limewire.ui.swing.friends;

import org.jdesktop.beans.AbstractBean;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.Presence.Mode;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class FriendImpl extends AbstractBean implements Friend {
    private boolean chatting;
    private boolean activeConversation;
    private final User user;
    private final Presence presence;
    private String status;
    private Mode mode;
    private long chatStartTime;
    private boolean hasUnviewedMessages;
    
    FriendImpl(User user, Presence presence) {
        this.user = user;
        this.presence = presence;
        this.status = presence.getStatus();
        this.mode = presence.getMode();        
    }
    
    @Override
    public Mode getMode() {
        return mode;
    }
    
    void setMode(Mode mode) {
        Mode oldMode = getMode();
        this.mode = mode;
        firePropertyChange("mode", oldMode, mode);
    }

    @Override
    public String getName() {
        return safe(user.getName(), user.getId());
    }
    
    private String safe(String str, String str2) {
        return (str == null || "".equals(str)) ? str2 : str;
    }
    
    @Override
    public String getStatus() {
        return status;
    }
    
    void setStatus(String status) {
        String oldStatus = getStatus();
        this.status = status;
        firePropertyChange("status", oldStatus, status);
    }

    @Override
    public boolean isChatting() {
        return chatting;
    }

    void setChatting(boolean chatting) {
        boolean oldChatting = isChatting();
        this.chatting = chatting;
        firePropertyChange("chatting", oldChatting, chatting);
    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return presence.createChat(reader);
    }

    @Override
    public void startChat() {
        if (isChatting() == false) {
            chatStartTime = System.currentTimeMillis();
            setChatting(true);
        }
    }

    @Override
    public void stopChat() {
        setChatting(false);
        setActiveConversation(false);
    }

    @Override
    public long getChatStartTime() {
        return chatStartTime;
    }

    @Override
    public boolean isActiveConversation() {
        return activeConversation;
    }
    
    @Override
    public void setActiveConversation(boolean active) {
        boolean oldActiveConversation = activeConversation;
        activeConversation = active;
        firePropertyChange("activeConversation", oldActiveConversation, activeConversation);
    }

    @Override
    public boolean isSignedInToLimewire() {
        return presence instanceof LimePresence;
    }

    @Override
    public boolean isReceivingUnviewedMessages() {
        return hasUnviewedMessages;
    }

    @Override
    public void setReceivingUnviewedMessages(boolean hasMessages) {
        boolean oldHasUnviewedMessages = hasUnviewedMessages;
        hasUnviewedMessages = hasMessages;
        firePropertyChange("receivingUnviewedMessages", oldHasUnviewedMessages, hasMessages);
    }
}
