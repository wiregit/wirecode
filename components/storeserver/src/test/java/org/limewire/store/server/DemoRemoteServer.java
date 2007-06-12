package org.limewire.store.server;

import java.util.Map;

import org.limewire.store.server.AbstractServer;
import org.limewire.store.server.DispatcherSupport;
import org.limewire.store.server.DemoLocalServer;
import org.limewire.store.server.LocalServerDelegate;
import org.limewire.store.server.AbstractRemoteServer;
import org.limewire.store.server.URLSocketOpenner;

/**
 * A simple remote server.
 * 
 * @author jpalm
 */
public class DemoRemoteServer extends AbstractRemoteServer implements
        RemoteServer {

    public final static int PORT = 8091;

    private final LocalServerDelegate del;

    public DemoRemoteServer(final int otherPort,
            final DispatcherSupport.OpensSocket openner) {
        super(PORT, null);
        Dispatcher d = new DispatcherImpl() {
            @Override
            public String sendMsgToRemoteServer(String msg,
                    Map<String, String> args) {
                return del.sendMsgToRemoteServer(msg, args);
            }
        };
        setDispatcher(d);
        this.del = new LocalServerDelegate("localhost", otherPort, openner);
    }

    public DemoRemoteServer(final int otherPort) {
        this(otherPort, new URLSocketOpenner());
    }

    public String toString() {
        return "Remote Server";
    }

    // ---------------------------------------------------------------
    // RemoteServer
    // ---------------------------------------------------------------
    
    private final RemoteServer remoteServer = new DefaultRemoteServer();

    @Override
    public final String lookUpPrivateKey(String publicKey, String ip) {
        return remoteServer.lookUpPrivateKey(publicKey, ip);
    }

    @Override
    public final boolean storeKey(String publicKey, String privateKey, String ip) {
        return remoteServer.storeKey(publicKey, privateKey, ip);
    }
        
    // ---------------------------------------------------------------
    // Testing
    // ---------------------------------------------------------------
    
    public static void main(String[] args) {
        AbstractServer.start(new DemoRemoteServer(DemoLocalServer.PORT));
    }


}
