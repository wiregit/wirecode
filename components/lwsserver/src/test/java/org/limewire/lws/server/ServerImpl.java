package org.limewire.lws.server;

import org.limewire.lws.server.LWSServerDispatcher;

/**
 * Base class for local servers.
 */
public abstract class ServerImpl extends AbstractServer implements LocalServer {

    public final void start() {
        start(this);
    }

    public ServerImpl(final int port, final String name, final LWSServerDispatcher dis) {
        super(port, name, dis);
    }
    
    public ServerImpl(final int port, final String name) {
        super(port, name);
    }

 

}
