package org.limewire.lws.server;

import java.util.Map;

import org.limewire.lws.server.LWSMainDispatcher;

/**
 * Base class for local servers.
 */
public final class LocalServerImpl extends AbstractServer implements LocalServer {
    
    /** The port on which we'll connect this server. */
    public final static int PORT = 45100;
    
    private final LocalServerDelegate del;

    public LocalServerImpl(String host, int otherPort) {
        super(PORT, "Local Server");
        LWSMainDispatcher ssd = new LWSMainDispatcher(new SenderOfMessagesToServer() {
            public void sendMessageToServer(String msg, Map<String, String> args, StringCallback cb) {
                del.sendMessageToServer(msg, args, cb);
            }

        });
        setDispatcher(ssd);
        
        ssd.setDispatchee(new AbstractReceivesCommandsFromDispatcher() {

            @Override
            protected void connectionChanged(boolean isConnected) {
            }

            public String receiveCommand(String cmd, Map<String, String> args) {
                return null;
            }
            
        });
        this.del = new LocalServerDelegate(host, otherPort, new URLSocketOpenner());
    }
}
