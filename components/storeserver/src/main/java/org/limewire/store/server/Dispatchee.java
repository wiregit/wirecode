package org.limewire.store.server;

import java.util.Map;

/**
 * Recieves messages from the local server.
 */
public interface Dispatchee extends ConnectionListener.HasSome {

    /**
     * Responds to <tt>cmd</tt> with arguments <tt>args</tt>.
     * 
     * @param cmd the command to which we this responds
     * @param args the arguments to <tt>cmd</tt>
     * @return to <tt>cmd</tt> with arguments <tt>args</tt>
     */
    String dispatch(String cmd, Map<String, String> args);
    
    /**
     * Called when we're connected from the server.
     * 
     * @param isConnected whether we're connected when a connection changes
     */
    void setConnected(boolean isConnected);

}