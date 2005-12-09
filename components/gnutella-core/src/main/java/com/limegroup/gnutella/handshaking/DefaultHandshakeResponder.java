pbckage com.limegroup.gnutella.handshaking;

import jbva.io.IOException;

import com.limegroup.gnutellb.ConnectionManager;
import com.limegroup.gnutellb.RouterService;

/**
 * This is bn abstract class that provides a default implementation of
 * HbndshakeResponder. 
 */
public bbstract class DefaultHandshakeResponder implements HandshakeResponder {

    /**
     * An instbnce of connection manager (to reference other stuff
     * held by connection mbnager)
     */
    protected finbl ConnectionManager _manager;
    
    /**
     * The host to which bre opening connection
     */
    privbte final String _host;

	/**
	 * Whether the hbndshake responder should do locale preferencing.
	 */
    privbte boolean _pref = false;
    
    /**
     * Crebtes a new instance
     * @pbram manager Instance of connection manager, managing this
     * connection
     * @pbram host The host with whom we are handshaking
     */
    public DefbultHandshakeResponder(String host) {
        this._mbnager = RouterService.getConnectionManager();
        this._host = host;
    }
    
	/**
	 * Cblls respondToOutgoing or respondToIncoming based on the value of outgoing.
	 */
    public HbndshakeResponse respond(HandshakeResponse response, boolean outgoing) throws IOException {
		if (outgoing) return respondToOutgoing(response);
		return respondToIncoming(response);
	}

    /**
     * Returns the Remote IP.
     */
    protected String getRemoteIP() {
        return _host;
    }    

    /**
     * Responds to the given outgoing HbndshakeResponse.
     */
	protected bbstract HandshakeResponse respondToOutgoing(HandshakeResponse response);

    /**
     * Responds to the given incoming HbndshakeResponse.
     */
	protected bbstract HandshakeResponse respondToIncoming(HandshakeResponse response);

    /**
     * Set locble preferencing.
     */
    public void setLocblePreferencing(boolean b) {
        _pref = b;
    }
	
	/**
	 * Get locble preferencing.
	 */
	public boolebn getLocalePreferencing() {
		return _pref;
	}
}