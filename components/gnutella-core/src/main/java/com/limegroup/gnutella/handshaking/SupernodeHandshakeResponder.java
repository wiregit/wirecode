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
    
    private ConnectionManager _manager;
    
    /**
     * Creates a new instance of ClientHandshakeResponder
     * @param manager Instance of connection manager, managing this
     * connection
     * @param router Instance of message router, to get correct local
     * address at runtime.
     * @param host The host with whom we are handshaking
     */
    public SupernodeHandshakeResponder(ConnectionManager manager, 
		String host)
    {
        super(manager, host);
        this._manager = manager;
    }
    
    //inherit doc comment
    protected HandshakeResponse respondUnauthenticated(
        HandshakeResponse response, boolean outgoing) throws IOException
    {
        
        if(!outgoing)
        {
            //Incoming connection....
            Properties ret=new SupernodeProperties(getRemoteIP());
            
            //guide the incoming connection to be a supernode/clientnode
            ret.put(ConnectionHandshakeHeaders.X_SUPERNODE_NEEDED,
            (new Boolean(_manager.supernodeNeeded())).toString());
            
            //give own IP address
            ret.put(ConnectionHandshakeHeaders.LISTEN_IP,
            _manager.getSelfAddress().getHostname() + ":"
            + _manager.getSelfAddress().getPort());
            
            //also add some host addresses in the response
            addHostAddresses(ret, _manager);

            //Decide whether to allow or reject.  Somewhat complicated because
            //of ultarpeer guidance.
            if (reject(outgoing, response.getHeaders())) {
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
            //already have enough old-fashioned connections, reject it.  We've
            //already given ultrapeer guidance at this point, so there's no
            //"second chance" like in the reject(..) method.
            Properties ret=new Properties();
            if (!_manager.allowConnection(outgoing,
                                          response.getHeaders().getProperty(
                                              ConnectionHandshakeHeaders.X_SUPERNODE),
                                          response.getHeaders().getProperty(
                                              ConnectionHandshakeHeaders.USER_AGENT))) {
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
     */
    private boolean reject(boolean outgoing, Properties headers) { 
        //Under some circumstances, we can decide to reject a connection during
        //handshaking because no slots are available.  You might think you could
        //reject the connection if !_manager.allowConnection(A, L), where A
        //is true if the connection is the connection is ultrapeer-aware and L
        //is true if the user is a leaf.  Unfortunately this fails when the
        //incoming connection is an ultrapeer (A&&!L) because of supernode
        //guidance; we don't know whether they'll become a leaf node or not.  So
        //we use the following conservative test, and depend on the
        //old-fashioned reject connection mechanism in ConnectionManager for the
        //other cases.
        
        String useragentHeader=headers.getProperty(
                                   ConnectionHandshakeHeaders.USER_AGENT);
        String ultrapeerHeader=headers.getProperty(
                                   ConnectionHandshakeHeaders.X_SUPERNODE);
        boolean isUltrapeer=ConnectionHandshakeHeaders.isTrue(ultrapeerHeader);

        boolean allowedNow=_manager.allowConnection(
            outgoing, ultrapeerHeader, useragentHeader);
        boolean allowedAsLeaf=_manager.allowConnection(
            outgoing, ConnectionHandshakeHeaders.FALSE, useragentHeader);

        //Reject if not allowed now and guidance not possible.
        return ! (allowedNow || (isUltrapeer && allowedAsLeaf));
    }    
}

