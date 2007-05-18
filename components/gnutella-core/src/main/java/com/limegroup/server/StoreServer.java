package com.limegroup.server;

import java.util.Map;

import com.limewire.store.server.demo.DemoLocalServer;
import com.limewire.store.server.demo.DemoRemoteServer;
import com.limewire.store.server.main.Dispatchee;
import com.limewire.store.server.main.LocalServer;
import com.limewire.store.server.main.Server;

/**
 * Encapsulates a local server and dispatchee.
 * 
 * @author jpalm
 */
public class StoreServer {
    
    // -----------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------
    
    private static StoreServer instance;
    
    public static StoreServer instance() {
        if (instance == null) instance = newDemoInstance();
        return instance;
    }
    
    private static StoreServer newDemoInstance() {
        final LocalServer s = new DemoLocalServer(DemoRemoteServer.PORT, false);
        s.setDebug(true);
        final Dispatchee d = new DispatcheeImpl(s);
        return instance = new StoreServer(s, d);
    }
    
    // -----------------------------------------------------------------
    // Instance
    // -----------------------------------------------------------------

    private final LocalServer localServer;
    
    public StoreServer(LocalServer localServer, Dispatchee dispatchee) {
        this.localServer = localServer;
        this.localServer.setDispatchee(dispatchee);
    }
    
    /**
     * Returns the local server.
     * 
     * @return the local server
     */
    public final LocalServer getLocalServer() {
        return this.localServer;
    }
    
    /**
     * Starts this service.
     */
    public void start() {
        Server.start(this.localServer);
    }
}
