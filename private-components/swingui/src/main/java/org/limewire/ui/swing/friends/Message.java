package org.limewire.ui.swing.friends;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface Message {
    enum Type { Sent, Received };
    
    String getSenderName();
    
    String getMessageText();
    
    Type getType();
}
