package org.limewire.ui.swing.friends;

import java.beans.PropertyChangeListener;

import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence.Mode;
/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface Friend {
    String getID();
    
    String getName();
    
    String getStatus();

    Mode getMode();
    
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
}
