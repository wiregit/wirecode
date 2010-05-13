package org.limewire.lws.server;

import java.util.Map;

import org.limewire.net.SocketsManager;

/**
 * Base class for local servers.
 */
public final class LocalServerImpl extends AbstractServer implements LocalServer {
    
    /** The port on which we'll connect this server. */
    public final static int PORT = 45100;
    
    private final String lwsPublickey;
    

    public LocalServerImpl(SocketsManager socketsManager, String host, String lwsPublickey) {
        super(PORT, "Local Server");
        this.lwsPublickey = lwsPublickey;
        LWSCommandValidator commandVerifier = new LWSCommandValidatorImpl(getLwsPublicKey(), new TestNetworkManagerImpl());
        LWSDispatcherImpl ssd = new LWSDispatcherImpl(commandVerifier);       
        setDispatcher(ssd);
        
        ssd.setCommandReceiver(new AbstractReceivesCommandsFromDispatcher() {
            public String receiveCommand(String cmd, Map<String, String> args) {
                return "ok";
            }
            
        });
    }
    
    /**
     * We do NOT want the IP of the incoming request to go to our handlers.
     */
    @Override
    protected final boolean sendIPToHandlers() {
        return false;
    }
    
    @Override
    public String getLwsPublicKey(){
        return lwsPublickey;
    }
    
    
}
