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
    String getName();
    
    String getStatus();

    Mode getMode();
    
    boolean isChatting();
    
    boolean isActiveConversation();
    
    void setActiveConversation(boolean active);
    
    void startChat();
    
    void stopChat();
    
    long getChatStartTime();
    
    MessageWriter createChat(MessageReader reader);

    void addPropertyChangeListener(PropertyChangeListener listener);
    
    void removePropertyChangeListener(PropertyChangeListener listener);
}
