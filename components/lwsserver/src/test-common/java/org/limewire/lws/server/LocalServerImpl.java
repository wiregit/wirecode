package org.limewire.lws.server;

import java.security.KeyPair;
import java.util.Map;

import org.limewire.net.SocketsManager;
import org.limewire.util.Base32;

/**
 * Base class for local servers.
 */
public final class LocalServerImpl extends AbstractServer implements LocalServer {
    
    /** The port on which we'll connect this server. */
    public final static int PORT = 45100;
    
    private final String lwsPublickey;
    

    public LocalServerImpl(SocketsManager socketsManager, String host, int otherPort, KeyPair keyPair) {
        super(PORT, "Local Server", keyPair);
        lwsPublickey = Base32.encode(keyPair.getPublic().getEncoded());
        LWSDispatcherImpl ssd = new LWSDispatcherImpl();       
        setDispatcher(ssd);
        
        ssd.setCommandReceiver(new AbstractReceivesCommandsFromDispatcher() {
            public String receiveCommand(String cmd, Map<String, String> args) {
                return "ok";
            }
            
        });
        
        ssd.setCommandVerifier(new LWSCommandValidatorImpl(getLwsPublicKey(), new TestNetworkManagerImpl()) );
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
