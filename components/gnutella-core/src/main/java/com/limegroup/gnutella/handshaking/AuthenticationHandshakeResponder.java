package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.util.StringUtils;
import java.util.Properties;
import java.io.IOException;
import com.sun.java.util.collections.*;

/**
 * An authentication-capable responder to be used during handshake.
 * This is an abstract class, and provides only authentication
 * capablities.
 * <p> Concrete subclasses should implement the respondUnAuthenticated()
 * method for the actual handshake (apart from authentication).
 * <p> The public respond(response, outgoing) method should not be
 * overwritten before taking this statement out.
 */
public abstract class AuthenticationHandshakeResponder 
    implements HandshakeResponder
{
    
    /**
     * Constant handle to the <tt>Cookies</tt> for authentication
     * purposes
     */
    private final Cookies COOKIES = Cookies.instance();
    
    /**
     * Constant handle to the <tt>SettingsManager</tt> for accessing
     * various properties.
     */
    private final SettingsManager SETTINGS = SettingsManager.instance();
    
    /**
     * An instance of connection manager (to reference other stuff
     * held by connection manager)
     */
    private ConnectionManager _manager;
    
    /**
     * The host to which are opening connection
     */
    private String _host = null;
    
    /**
     * Flag indicating whether we have used the cookie for authentication
     */
    private boolean _cookieUsed = false;
    
    /**
     * Flag indicating whether he user connecting to us, has authenticated
     */
    private boolean _authenticated = false;
    
    /**
     * The request received before we asked other node to authenticate
     */
    private HandshakeResponse _beforeAuthenticationRequest = null;
    
    /**
     * Creates a new instance
     * @param manager Instance of connection manager, managing this
     * connection
     * @param host The host with whom we are handshaking
     */
    public AuthenticationHandshakeResponder(ConnectionManager manager, String host)
    {
        this._manager = manager;
        this._host = host;
    }
    
    public HandshakeResponse respond(HandshakeResponse response, boolean outgoing) throws IOException
    {
        //save the first request
        if(_beforeAuthenticationRequest == null)
            _beforeAuthenticationRequest = response;
        //do stuff specific to connection direction
        if(outgoing)
        {
            return respondOutgoing(response);
        }else
        {
            return respondIncoming(response);
        }
    }
    
    /**
     * Returns the corresponding handshake to be written to the remote host
     * when responding to the connection handshake response just received,
     * for outgoing connection.
     * @param response The handshake response received from the remote end
     */
    private HandshakeResponse respondIncoming(HandshakeResponse response) 
        throws IOException
    {
        //if authentication is not required, return normal response
        //else authenticate
        if(_authenticated ||
        !SETTINGS.acceptAuthenticatedConnectionsOnly())
            return respondUnauthenticated(response, false);
        else
            return respondIncomingAuthenticate(response);
    }
    
    /**
     * Asks the connecting host for authentication, if authentication
     * headers not already received in the response from the connecting
     * host. If authenticated, lets the concrete class handle the
     * original request, and returns the response returned by
     * the concrete class, augmented with authentication information.
     * @param response The handshake response received from the remote end
     * @return response Our response
     */
    private HandshakeResponse respondIncomingAuthenticate(
        HandshakeResponse response) throws IOException
    {
        
        //get the domains, user has successfully authenticated
        Set domains = getDomainsAuthenticated(response.getHeaders());
        
        //if couldnt authenticate
        if(domains == null)
        {
            return new HandshakeResponse(
            HandshakeResponse.UNAUTHORIZED_CODE,
            HandshakeResponse.UNAUTHORIZED_MESSAGE, null);
        }else
        {
            _authenticated = true;
            //handle the original request
            HandshakeResponse ourResponse = respondUnauthenticated(
            _beforeAuthenticationRequest, false);
            //add the property in the response letting the
            //remote host know of the domains successfully authenticated
            ourResponse.getHeaders().put(
            ConnectionHandshakeHeaders.X_DOMAINS_AUTHENTICATED,
            StringUtils.getEntriesAsString(domains));
            //return our response
            return ourResponse;
        }
    }
    
    /**
     * Returns the domains to which the user has successfully
     * authenticated to
     * @param headersReceived The headers we received. These will be
     * used for authentication
     * @return the domains to which the user has successfully
     * authenticated to
     */
    private Set getDomainsAuthenticated(Properties headersReceived)
    {
        //pass the username and password to authenticator
        return _manager.getAuthenticator().authenticate(
        headersReceived.getProperty(
        ConnectionHandshakeHeaders.X_USERNAME),
        headersReceived.getProperty(
        ConnectionHandshakeHeaders.X_PASSWORD), null);
    }
    
    /**
     * Returns the corresponding handshake to be written to the remote host
     * when responding to the connection handshake response just received,
     * for outgoing connection.
     * @param response The handshake response received from the remote end
     */
    private HandshakeResponse respondOutgoing(HandshakeResponse response) 
        throws IOException
    {
        //check the code we received from the other side
        //if authentication is not needed, respond normally, else
        //authenticate
        if(response.getStatusCode() != HandshakeResponse.UNAUTHORIZED_CODE)
            return respondUnauthenticated(response, true);
        else
            return respondOutgoingWithAuthentication(response);
    }
    
    /**
     * Returns the corresponding handshake to be written to the remote host
     * on an outgoing connection, when authentication challenge received
     * @param response The handshake response received from the remote end
     * @return response Our response
     */
    private HandshakeResponse respondOutgoingWithAuthentication(
        HandshakeResponse response) throws IOException
    {
        int code = HandshakeResponse.OK;
        String message = HandshakeResponse.OK_MESSAGE;
        Properties ret = new Properties();
        
        //Authenticate
        //get user information
        User user = getUserInfo();
        
        //if user is unable to authenticate
        if((user == null) || user.getUsername().trim().equals(""))
        {
            //set the error codes in the response
            code = HandshakeResponse.DEFAULT_BAD_STATUS_CODE;
            message = HandshakeResponse.UNABLE_TO_AUTHENTICATE;
        }
        else
        {
            //set the user properties as well as response headers
            code = HandshakeResponse.OK;
            message = HandshakeResponse.AUTHENTICATING;
            //add user authentication headers
            ret.put(ConnectionHandshakeHeaders.X_USERNAME,
            user.getUsername());
            ret.put(ConnectionHandshakeHeaders.X_PASSWORD,
            user.getPassword());
            
            //also store the authentication information in a
            //cookie, for next-time use
            COOKIES.putCookie(_host, user);
        }
        return new HandshakeResponse(code, message, ret);
    }
    
    /**
     * Gets the user's authentication information for the host we are
     * handshaking with
     * @return User's authentication information for the host we are
     * handshaking with
     */
    private User getUserInfo()
    {
        User user = null;
        //first try the information stored in cookie
        if(!_cookieUsed)
        {
            _cookieUsed = true;
            if(_host != null)
                user = COOKIES.getUserInfo(_host);
        }
        
        //if we dont have cookie, or we have already used the
        //cookie, then get the information interactively from user
        if(user == null)
        {
            user = _manager.getCallback()
            .getUserAuthenticationInfo(_host);
        }
        
        //return the user information
        return user;
    }
    
    /**
     * Adds string representing addresses of other hosts that may
     * be connected thru gnutella, 
     * (to corresponding header keys) in the passed properties. 
     * Also includes the addresses of the
     * supernodes it is connected to
     * @param properties The properties instance to which to add host addresses
     * @param manager Reference to the connection manager from whom 
     * to retrieve the addressses
     * <p> Host address string added (to corresponding header keys)
     * is in the form:
     * <p> IP Address:Port [,IPAddress:Port]* 
     * <p> e.g. 123.4.5.67:6346,234.5.6.78:6347
     */
    protected void addHostAddresses(Properties properties, 
        ConnectionManager manager){
        StringBuffer hostString = new StringBuffer();
        boolean isFirstHost = true;
        //get the connected supernodes and pass them
        for(Iterator iter = manager.getBestHosts(10);iter.hasNext();){
            //get the next endpoint
            Endpoint endpoint =(Endpoint)iter.next();
            //if the first endpoint that we are adding
            if(!isFirstHost){
                //append separator to separate the entries
                hostString.append(Constants.ENTRY_SEPARATOR);
            }else{
                //unset the flag
                isFirstHost = false;
            }
            //append the host information
            hostString.append(endpoint.getHostname());
            hostString.append(":");
            hostString.append(endpoint.getPort());
        }
        //set the property
        properties.put(ConnectionHandshakeHeaders.X_TRY, 
            hostString.toString());

        //Also add neighbouring supernodes
        Set connectedSupernodeEndpoints 
            = manager.getSupernodeEndpoints();
        //if nothing to add, return
        if(connectedSupernodeEndpoints.size() < 0)
            return;
        
        //else add the supernodes
        hostString = new StringBuffer();
        isFirstHost = true;
        for(Iterator iter = connectedSupernodeEndpoints.iterator();
            iter.hasNext();){
            //get the next endpoint
            Endpoint endpoint =(Endpoint)iter.next();
            //if the first endpoint that we are adding
            if(!isFirstHost){
                //append separator to separate the entries
                hostString.append(Constants.ENTRY_SEPARATOR);
            }else{
                //unset the flag
                isFirstHost = false;
            }
            //append the host information
            hostString.append(endpoint.getHostname());
            hostString.append(":");
            hostString.append(endpoint.getPort());
        }
        //set the property
        properties.put(ConnectionHandshakeHeaders.X_TRY_SUPERNODES
            , hostString.toString());
    }
    
    /**
     * Returns the corresponding handshake to be sent
     * to the remote host when
     * responding to the connection handshake response received.
     * @param response The response received from the host on the
     * other side of the connection.
     * @param outgoing whether the connection to the remote host
     * is an outgoing connection.
     * @param includeProperties The properties that should be included
     * in the returned response
     * @return the response to be sent to the remote host
     */
    protected abstract HandshakeResponse respondUnauthenticated(
        HandshakeResponse response, boolean outgoing) throws IOException;
    
}

