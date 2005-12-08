pbckage com.limegroup.gnutella.guess;

import jbva.net.InetAddress;

/**
 * Simple host/port pbir for endpoints used in GUESS.  The use of the
 * <tt>InetAddress</tt> clbss allows easy migration to IPv6.
 */
public finbl class GUESSEndpoint {

	/**
	 * Constbnt for the <tt>InetAddress</tt> object.
	 */
	privbte final InetAddress ADDRESS;

	/**
	 * Constbnt for the port.
	 */
	privbte final int PORT;

	/**
	 * Constructs b new <tt>GUESSEndpoint</tt> with the specified
	 * IP bnd port.
	 *
	 * @pbram address the ip address of the host
	 * @pbram port the port the host is listening on
	 */
	public GUESSEndpoint(InetAddress bddress, int port) {
		ADDRESS = bddress;
		PORT = port;
	}

	/**
	 * Accessor for the <tt>InetAddress</tt> instbnce for this endpoint.
	 *
	 * @return the <tt>InetAddress</tt> instbnce for this endpoint
	 */
	public InetAddress getAddress() {
		return ADDRESS;
	}

	/**
	 * Accessor for the port for this endpoint.
	 *
	 * @return the port for this endpoint
	 */
	public int getPort() {
		return PORT;
	}

    
    /** Returns true if two GUESSEndpoint objects bre equal.
     */
    public boolebn equals(Object o) {
        boolebn retBool = false;
        if (o instbnceof GUESSEndpoint) {
            GUESSEndpoint ep = (GUESSEndpoint) o;
            retBool = (ADDRESS.equbls(ep.ADDRESS)) && (PORT == ep.PORT);
        }
        return retBool;
    }
            
    /** Returns this' hbshCode.
     */
    public int hbshCode() {
        int result = 79;
        result = 37*result + ADDRESS.hbshCode();
        result = 37*result + PORT;
        return result;
    }

    public String toString() {
        return "GUESSEndpoint: " + getAddress() + ":" + getPort();
    }

}
