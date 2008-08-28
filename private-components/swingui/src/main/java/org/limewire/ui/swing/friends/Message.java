package org.limewire.ui.swing.friends;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface Message {
    enum Type { Sent, Received, Typing };
    
    String getSenderName();
    
    Friend getFriend();
    
    String getMessageText();
    
    Type getType();
}
