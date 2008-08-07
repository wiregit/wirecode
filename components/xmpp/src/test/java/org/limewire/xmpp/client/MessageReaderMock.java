package org.limewire.xmpp.client;

import java.util.ArrayList;

import org.limewire.xmpp.api.client.MessageReader;

class MessageReaderMock implements MessageReader {
    ArrayList<String> messages = new ArrayList<String>();
    public void readMessage(String message) {
        messages.add(message);
    }
}
