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
            Properties ret=new SupernodeProperties(_router, getRemoteIP());
            
            //guide the incoming connection to be a supernode/clientnode
            ret.put(ConnectionHandshakeHeaders.X_SUPERNODE_NEEDED,
            (new Boolean(_manager.supernodeNeeded())).toString());
            
            //give own IP address
            ret.put(ConnectionHandshakeHeaders.X_MY_ADDRESS,
            _manager.getSelfAddress().getHostname() + ":"
            + _manager.getSelfAddress().getPort());
            
            //also add some host addresses in the response
            addHostAddresses(ret, _manager);

            //Decide whether to allow or reject.
            Properties props=response.getHeaders();
            String isUltrapeer=
                props.getProperty(ConnectionHandshakeHeaders.X_SUPERNODE);
            if (reject(isUltrapeer)) {
                return new HandshakeResponse(
                    HandshakeResponse.SLOTS_FULL,
                    HandshakeResponse.SLOTS_FULL_MESSAGE,
                    ret);
            } else {
                return new HandshakeResponse(ret);
            }
        } else
        {
            //Outgoing connection.  If the other guy is ultrapeer unaware and I
            //already have enough old-fashioned connections, reject it.
            Properties ret=new Properties();
            if (response.getHeaders().getProperty(
                        ConnectionHandshakeHeaders.X_SUPERNODE)==null
                    && !_manager.hasAvailableIncoming(false, false)) {
                return new HandshakeResponse(
                    HandshakeResponse.SLOTS_FULL,
                    HandshakeResponse.SLOTS_FULL_MESSAGE,
                    ret);
            }
            //Did the server request we become a leaf?
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
    
    /** 
     * Returns true if this incoming connections should be rejected with a 503. 
     * @param isUltrapeer thevalue of the X_SUPERNODE header, or possibly null. 
     */
    private boolean reject(String isUltrapeer) { 
        //Under some circumstances, we can decide to reject a connection during
        //handshaking because no slots are available.  You might think you could
        //reject the connection if !_manager.hasAvailableIncoming(A, L), where A
        //is true if the connection is the connection is ultrapeer-aware and L
        //is true if the user is a leaf.  Unfortunately this fails when the
        //incoming connection is an ultrapeer (A&&!L) because of supernode
        //guidance; we don't know whether they'll become a leaf node or not.  So
        //we use the following conservative test, and depend on the
        //old-fashioned reject connection mechanism in ConnectionManager for the
        //other cases.
        return //it's an unrouted old-style connection, and no slot
               (isUltrapeer==null 
                   && !_manager.hasAvailableIncoming(false, false))
               //OR leaf, and no space
               || (ConnectionHandshakeHeaders.isFalse(isUltrapeer)
                   && !_manager.hasAvailableIncoming(true, true))
               //OR no space for leaves or ultrapeers [sic]
               || (!_manager.hasAvailableIncoming(true, true)
                   && !_manager.hasAvailableIncoming(true, false));
    }    
}

