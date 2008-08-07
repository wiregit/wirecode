package org.limewire.ui.swing.xmpp;

import org.limewire.xmpp.api.client.IncomingChatListener;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;

class IncomingChatListenerImpl implements IncomingChatListener {
    MessageWriter writer;
    MessageReaderImpl reader;

    public MessageReader incomingChat(MessageWriter writer) {
        // TODO update UI
        this.writer = writer;
        this.reader = new MessageReaderImpl();
        return reader;
    }
}