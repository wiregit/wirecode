package org.limewire.xmpp.client;

public interface MessageWriter {
    void writeMessage(String message) throws XMPPException;    
}
