package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.FileMetaData;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface Message {
    enum Type { Sent, Received, FileOffer };
    
    String getSenderName();
    
    String getFriendName();
    
    String getFriendID();
    
    String getMessageText();
    
    Type getType();
    
    long getMessageTimeMillis();
    
    /**
     * 
     * @return null for message types other than FileOffer
     */
    FileMetaData getFileOffer();
}
