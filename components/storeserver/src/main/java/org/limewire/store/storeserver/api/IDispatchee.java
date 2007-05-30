package org.limewire.store.storeserver.api;

import java.util.Map;

/**
 * Recieves messages from the local server.
 * 
 * @author jpalm
 */
public interface IDispatchee extends IConnectionListener.HasSome {

    /**
     * Returns the local server from which this receives commands.
     * 
     * @return the local server from which this receives commands
     */
    IServer getServer();

    /**
     * Responds to <tt>cmd</tt> with arguments <tt>args</tt>.
     * 
     * @param cmd the command to which we this responds
     * @param args the arguments to <tt>cmd</tt>
     * @return to <tt>cmd</tt> with arguments <tt>args</tt>
     */
    String dispatch(String cmd, Map<String, String> args);

    /**
     * Sent from the local server to indicate whether we are connected or not.
     * 
     * @param isConnected <tt>true</tt> if we're connected, <tt>false</tt>
     *        otherwise
     */
    void setConnected(boolean isConnected);

}