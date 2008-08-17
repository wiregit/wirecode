package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.MessageWriter;


/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface ConversationPaneFactory {
    ConversationPane create(MessageWriter writer, String conversationName);
}
