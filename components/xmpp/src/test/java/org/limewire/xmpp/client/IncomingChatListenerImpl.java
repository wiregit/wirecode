package org.limewire.xmpp.client;

import org.limewire.xmpp.client.service.IncomingChatListener;
import org.limewire.xmpp.client.service.MessageWriter;
import org.limewire.xmpp.client.service.MessageReader;

public class IncomingChatListenerImpl implements IncomingChatListener {
    MessageWriter writer;
    MessageReaderImpl reader;
    
    public MessageReader incomingChat(MessageWriter writer) {
        System.out.println("new chat");
        this.writer = writer;
        this.reader = new MessageReaderImpl();
        return reader;
    }
}
