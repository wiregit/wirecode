package org.limewire.lws.server;

import java.io.IOException;
import java.util.Map;

/**
 * Defines an interface to communicate with a remote server.
 */
public interface SenderOfMessagesToServer {

    /**
     * Returns the response after sending a message <code>msg</code> and
     * name/value pair arguments <code>args</code>.
     * 
     * @param msg the command upon which to act
     * @param args name/value pair of arguments to send
     * @return the response after sending a message <code>msg</code> and
     *         name/value pair arguments <code>args</code>.
     * @throws IOException when something happens to the connection or another
     *         IO program occurs
     */
    String semdMessageToServer(String msg, Map<String, String> args) throws IOException;    
}
