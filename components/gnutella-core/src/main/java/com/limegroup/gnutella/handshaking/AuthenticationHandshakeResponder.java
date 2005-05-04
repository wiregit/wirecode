package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.security.Cookies;
import com.limegroup.gnutella.security.User;
import com.limegroup.gnutella.settings.SecuritySettings;
import com.limegroup.gnutella.util.StringUtils;

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
    //private final SettingsManager SETTINGS = SettingsManager.instance();
    
    /**
     * An instance of connection manager (to reference other stuff
     * held by connection manager)
     */
    protected final ConnectionManager _manager;
    
    /**
     * The host to which are opening connection
     */
    private final String _host;
    
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
    
    public HandshakeResponse respond(HandshakeResponse response, boolean outgoing) 
        throws IOException
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
     * Returns the Remote IP
     */
    protected String getRemoteIP()
    {
        return _host;
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
        !SecuritySettings.ACCEPT_AUTHENTICATED_CONNECTIONS_ONLY.getValue())
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
        Set domains = getDomainsAuthenticated(response.props());
        
        //if couldnt authenticate
        if(domains == null) {
            return new HandshakeResponse(HandshakeResponse.UNAUTHORIZED_CODE,
                                         HandshakeResponse.UNAUTHORIZED_MESSAGE, 
                                         null);
        } else {
            _authenticated = true;
            //handle the original request
            HandshakeResponse ourResponse = 
                respondUnauthenticated(_beforeAuthenticationRequest, false);

            //add the property in the response letting the
            //remote host know of the domains successfully authenticated
            ourResponse.props().put(HeaderNames.X_DOMAINS_AUTHENTICATED,
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
        return 
            _manager.getAuthenticator().authenticate(
                headersReceived.getProperty(HeaderNames.X_USERNAME),
                headersReceived.getProperty(HeaderNames.X_PASSWORD), null);
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
            return respondOutgoingWithAuthentication();
    }
    
    /**
     * Returns the corresponding handshake to be written to the remote host
     * on an outgoing connection, when authentication challenge received
     * @param response The handshake response received from the remote end
     * @return response Our response
     */
    private HandshakeResponse respondOutgoingWithAuthentication()
        throws IOException
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
            ret.put(HeaderNames.X_USERNAME,
            user.getUsername());
            ret.put(HeaderNames.X_PASSWORD,
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
        
        //return the user information
        return user;
    }

    /**
     * optional method empty impl.
     * Should override if preferencing is wanted.
     */
    public void setLocalePreferencing(boolean b) {
        //should this throw UnsupportedOperationException?
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

