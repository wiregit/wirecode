package org.limewire.ui.swing.friends;

import java.beans.PropertyChangeListener;

import org.limewire.core.api.friend.Friend;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.Presence.Mode;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface ChatFriend {
    Friend getFriend();
    
    String getID();
    
    String getName();
    
    void setStatus(String status);
    
    String getStatus();

    void setMode(Mode mode);
    
    Mode getMode();
    
    boolean jidBelongsTo(String jid);
    
    boolean isChatting();
    
    boolean isActiveConversation();
    
    void setActiveConversation(boolean active);
    
    boolean isSignedInToLimewire();
    
    void startChat();
    
    void stopChat();
    
    long getChatStartTime();
    
    boolean isReceivingUnviewedMessages();
    
    void setReceivingUnviewedMessages(boolean hasMessages);
    
    MessageWriter createChat(MessageReader reader);

    void addPropertyChangeListener(PropertyChangeListener listener);
    
    void removePropertyChangeListener(PropertyChangeListener listener);

    Presence getPresence();
    
    void releasePresence(Presence presence);

    void updatePresence(Presence presence);
}
