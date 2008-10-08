package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.FileMetaData;

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

    boolean hasFileOffer();

    FileMetaData getFileOffer();
}
