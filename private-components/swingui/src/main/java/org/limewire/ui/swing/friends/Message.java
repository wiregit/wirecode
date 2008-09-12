package org.limewire.ui.swing.friends;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface Message {
    enum Type { Sent, Received };
    
    String getSenderName();
    
    String getFriendName();
    
    String getFriendID();
    
    String getMessageText();
    
    Type getType();
    
    long getMessageTimeMillis();
}
