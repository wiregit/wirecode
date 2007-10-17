package org.limewire.lws.server;

import org.limewire.lws.server.Dispatcher;
import org.limewire.lws.server.DispatcherSupport;

/**
 * This represents an instance of a local server.
 */
public interface LocalServer {
    
    /**
     * Returns the instance {@link DispatcherSupport}.
     * 
     * @return the instance {@link DispatcherSupport}
     */
    Dispatcher getDispatcher();

    /**
     * Returns the {@link Thread} that started this server, after starting it.
     * 
     * @return the {@link Thread} that started this server, after starting it
     */
    Thread start();

    /**
     * Shuts down the server, waiting for <code>millis</code> milliseconds.
     * 
     * @param millis milliseconds to wait before abruptly shutting everything
     *        down
     */
    void shutDown(long millis);

    /**
     * Reports a message.
     * 
     * @param msg message to report
     * @return reponse back to code after reporting <code>msg</code>
     */
    String report(String msg);    

}
