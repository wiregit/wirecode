package org.limewire.lws.server;

import java.util.Map;

import org.limewire.lws.server.AbstractRemoteServer;
import org.limewire.lws.server.AbstractServer;
import org.limewire.lws.server.LocalServerForTesting;
import org.limewire.lws.server.Dispatcher;
import org.limewire.lws.server.DispatcherSupport;
import org.limewire.lws.server.LocalServerDelegate;

/**
 * A simple remote server.
 */
public class RemoteServerForTesting extends AbstractRemoteServer implements
        RemoteServer {

    public final static int PORT = 8091;

    private final LocalServerDelegate del;

    public RemoteServerForTesting(final int otherPort,
            final DispatcherSupport.OpensSocket openner) {
        super(PORT, null);
        Dispatcher d = new DispatcherImpl() {
            @Override
            public String semdMessageToServer(String msg,
                    Map<String, String> args) {
                return del.semdMessageToServer(msg, args);
            }
        };
        setDispatcher(d);
        this.del = new LocalServerDelegate("localhost", otherPort, openner);
    }

    public RemoteServerForTesting(final int otherPort) {
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
        AbstractServer.start(new RemoteServerForTesting(LocalServerForTesting.PORT));
    }


}
