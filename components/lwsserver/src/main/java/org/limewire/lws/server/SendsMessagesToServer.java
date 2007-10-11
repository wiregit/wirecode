package org.limewire.lws.server;

import java.util.Map;

/**
 * Something that communicates with a remote server.
 */
public interface SendsMessagesToServer {

    /**
     * Send a message to the server.
     * 
     * @param msg   the command upon which to act
     * @param args  name/value pair of arguments to send
     */
    String sendMsgToRemoteServer(String msg, Map<String, String> args);    
}
