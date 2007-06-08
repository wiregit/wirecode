package org.limewire.store.server;

import java.util.Map;

public interface SendsMessagesToServer {

    /**
     * Send a message to the server.
     * 
     * @param args
     */
    String sendMsgToRemoteServer(String msg, Map<String, String> args);    
}
