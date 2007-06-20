package org.limewire.store.server;

/**
 * Base class for local servers.
 */
public abstract class ServerImpl extends AbstractServer implements Server {

    /** Whether we're using the watcher of not. */
    private final static boolean USING_WATCHER = true;

    public final void start() {
        start(this);
    }

    public ServerImpl(final int port, final String name, final StoreServerDispatcher dis) {
        super(port, name, dis);
    }
    
    public ServerImpl(final int port, final String name) {
        super(port, name);
    }

 

}
