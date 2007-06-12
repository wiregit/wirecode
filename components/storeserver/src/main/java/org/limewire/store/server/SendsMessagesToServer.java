package org.limewire.store.server;

import java.util.Map;

/**
 * Something that communicates with a remote server.
 */
public interface SendsMessagesToServer {

    /**
     * Send a message to the server.
     * 
     * @param args
     */
    String sendMsgToRemoteServer(String msg, Map<String, String> args);    
}
