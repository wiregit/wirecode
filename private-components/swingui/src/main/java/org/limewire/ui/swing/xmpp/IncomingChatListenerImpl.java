package org.limewire.ui.swing.xmpp;

import org.limewire.xmpp.client.service.IncomingChatListener;
import org.limewire.xmpp.client.service.MessageWriter;
import org.limewire.xmpp.client.service.MessageReader;

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