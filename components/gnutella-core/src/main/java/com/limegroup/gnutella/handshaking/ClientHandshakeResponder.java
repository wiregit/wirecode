package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;
import java.util.Properties;
import java.io.IOException;

/**
 * A very simple responder to be used by client-nodes during the
 * connection handshake while accepting incoming connections
 */
public class ClientHandshakeResponder extends AuthenticationHandshakeResponder
{
    
    ConnectionManager _manager;
    
    MessageRouter _router;
    
    /**
     * Creates a new instance of ClientHandshakeResponder
     * @param manager Instance of connection manager, managing this
     * connection
     * @param router Instance of message router, to get correct local
     * address at runtime.
     * @param host The host with whom we are handshaking
     */
    public ClientHandshakeResponder(ConnectionManager manager, 
        MessageRouter router, String host)
    {
        super(manager, host);
        this._manager = manager;
        this._router = router;
    }
    
    //inherit doc comment
    protected HandshakeResponse respondUnauthenticated(
        HandshakeResponse response, boolean outgoing) throws IOException
    {
        if (outgoing)
        {
            //a) Outgoing: nothing more to say
            return new HandshakeResponse(new Properties());
        } else
        {
            Properties props=new ClientProperties(_router);
            addHostAddresses(props, _manager);
            
            if (_manager.hasClientSupernodeConnection())
            {
                //b) Incoming, with supernode connection: reject (redirect)
                return new HandshakeResponse(
                HandshakeResponse.SHIELDED,
                HandshakeResponse.SHIELDED_MESSAGE,
                props);
            } else
            {
                //c) Incoming, no supernode: accept...until I find one
                return new HandshakeResponse(props);
            }
        }
    }
    
}

