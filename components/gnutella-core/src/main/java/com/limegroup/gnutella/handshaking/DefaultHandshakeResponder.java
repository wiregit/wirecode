
// Commented for the Learning branch

package com.limegroup.gnutella.handshaking;

import java.io.IOException;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.RouterService;

/** An abstract class that provides a default implementation of HandshakeResponder */
public abstract class DefaultHandshakeResponder implements HandshakeResponder {

    /** An instance of connection manager we can use to reference the things connection manager holds */
    protected final ConnectionManager _manager;
    
    /** The IP address of the remote computer */
    private final String _host;

	/**
     * A Gnutella program can send a header like "X-Locale-Pref: en" to say it is configured for the English language
     * Call setLocalePreferencing(true) to make this handshake responder refuse connections from foreign language remote computers
     * The boolean _pref holds this value
     */
    private boolean _pref = false; // By default, don't refuse foreign language computers

    /**
     * Make a new DefaultHandshakeResponder object with _manager and _host filled out.
     * 
     * @param host The IP address of the remote computer
     */
    public DefaultHandshakeResponder(String host) {
    	
    	// Get a reference to the connection manager and store it in this object
        this._manager = RouterService.getConnectionManager();
        
        // Record the IP address of the remote computer
        this._host = host;
    }
    
	/**
	 * This is the method we have to write to implement the HandshakeResponder interface.
	 * Reads a group of handshake headers from a remote computer, and composes our response.
	 * Calls respondToOutgoing or respondToIncoming based on who initiated the connection.
	 * 
	 * @param response The group of handshake headers the remote computer sent us
	 * @param outgoing True if we connected to the remote computer, false if it connected to us
	 * @return Our group of handshake headers we should send the remote computer in response
	 */
    public HandshakeResponse respond(HandshakeResponse response, boolean outgoing) throws IOException {

    	// Just call respondToOutgoing or respondToIncoming based on the value of outgoing
    	if (outgoing) return respondToOutgoing(response); // We connected to the remote computer
    	else          return respondToIncoming(response); // The remote computer connected to us
	}

    /** Returns the IP address of the remote computer we're connected to and shaking hands with */
    protected String getRemoteIP() {
    	
    	// The constructor got this value, and we kept it in the string named _host
        return _host;
    }    

    /**
     * Outgoing means we connected to the remote computer.
     * We connected and sent the stage 1 headers.
     * The remote computer replied with stage 2 headers, passed here as response.
     * Now, this method will compose stage 3, our group of headers that will finish the handshake.
     * 
     * @param response The stage 2 handshake headers the remote computer sent us
     * @return Our stage 3 headers we'll send in response to finish the handshake
     */
	protected abstract HandshakeResponse respondToOutgoing(HandshakeResponse response);

    /**
     * Incoming means the remote computer connected to us on the socket we're listening on.
     * It opened the connection and sent us stage 1 headers, passed here as response.
     * Now, this method will compose stage 2, our response.
     * 
     * @param response The stage 1 headers the remote computer began the connection with
     * @return Our stage 2 headers we'll send in response
     */
	protected abstract HandshakeResponse respondToIncoming(HandshakeResponse response);

    /**
     * A Gnutella program can send a header like "X-Locale-Pref: en" to say it is configured for the English language
     * Call setLocalePreferencing(true) to make this handshake responder refuse connections from foreign language remote computers
     * The default is false, when you make a new handshake responder, it won't start doing this until you tell it to
     *
     * @param b True to refuse foreign language connections
     */
    public void setLocalePreferencing(boolean b) {
    	
    	// Store this value in _pref
        _pref = b;
    }
	
	/**
     * A Gnutella program can send a header like "X-Locale-Pref: en" to say it is configured for the English language
	 * This handshake responder may have been configured to refuse connections from foreign language remote computers
	 * Call this method to find out if it has been configured that way
	 * 
	 * @return True if this handshake responder will refuse foreign language connections
	 */
	public boolean getLocalePreferencing() {
		
		// We stored this value in _pref
		return _pref;
	}
}
