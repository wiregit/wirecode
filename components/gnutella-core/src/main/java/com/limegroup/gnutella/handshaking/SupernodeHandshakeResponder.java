package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;
import java.util.Properties;
import java.io.IOException;

/**
 * A very simple responder to be used by supernodes during the
 * connection handshake while accepting incoming connections
 */
public class SupernodeHandshakeResponder 
    extends AuthenticationHandshakeResponder
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
    public SupernodeHandshakeResponder(ConnectionManager manager, 
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
        
        if(!outgoing)
        {
            //Incoming connection....
            Properties ret=new SupernodeProperties(_router);
            
            //guide the incoming connection to be a supernode/clientnode
            ret.put(ConnectionHandshakeHeaders.X_SUPERNODE_NEEDED,
            (new Boolean(_manager.supernodeNeeded())).toString());
            
            //give own IP address
            ret.put(ConnectionHandshakeHeaders.X_MY_ADDRESS,
            _manager.getSelfAddress().getHostname() + ":"
            + _manager.getSelfAddress().getPort());
            
            //also add some host addresses in the response
            addHostAddresses(ret, _manager);
            
            //Under some circumstances, we can decide to reject a connection
            //during handshaking because no slots are available.  You might
            //think you could reject the connection if
            //_manager.hasAvailableIncoming(S) is false, where S is the
            //value of Supernode property written by the remote host.
            //Unfortunately this fails when S==true because of supernode
            //guidance; we don't know whether they'll become a leaf node or
            //not. So we use the following conservative test, and depend on
            //the old-fashioned reject connection mechanism in
            //ConnectionManager for the other cases.
            if (!_manager.hasAnyAvailableIncoming())
                return new HandshakeResponse(
                HandshakeResponse.SLOTS_FULL,
                HandshakeResponse.SLOTS_FULL_MESSAGE,
                ret);
            else
                return new HandshakeResponse(ret);
        } else
        {
            //Outgoing connection.  Did the server request we become a leaf?
            Properties ret=new Properties();
            String neededS=response.getHeaders().
            getProperty(ConnectionHandshakeHeaders.X_SUPERNODE_NEEDED);
            if (neededS!=null
            && !Boolean.valueOf(neededS).booleanValue()
            && _manager.allowClientMode())
            {
                //Fine, we'll become a leaf.
                ret.put(ConnectionHandshakeHeaders.X_SUPERNODE,
                "False");
            }
            return new HandshakeResponse(ret);
        }
    }
    
}

