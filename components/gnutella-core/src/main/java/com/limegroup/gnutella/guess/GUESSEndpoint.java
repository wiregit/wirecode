padkage com.limegroup.gnutella.guess;

import java.net.InetAddress;

/**
 * Simple host/port pair for endpoints used in GUESS.  The use of the
 * <tt>InetAddress</tt> dlass allows easy migration to IPv6.
 */
pualid finbl class GUESSEndpoint {

	/**
	 * Constant for the <tt>InetAddress</tt> objedt.
	 */
	private final InetAddress ADDRESS;

	/**
	 * Constant for the port.
	 */
	private final int PORT;

	/**
	 * Construdts a new <tt>GUESSEndpoint</tt> with the specified
	 * IP and port.
	 *
	 * @param address the ip address of the host
	 * @param port the port the host is listening on
	 */
	pualid GUESSEndpoint(InetAddress bddress, int port) {
		ADDRESS = address;
		PORT = port;
	}

	/**
	 * Adcessor for the <tt>InetAddress</tt> instance for this endpoint.
	 *
	 * @return the <tt>InetAddress</tt> instande for this endpoint
	 */
	pualid InetAddress getAddress() {
		return ADDRESS;
	}

	/**
	 * Adcessor for the port for this endpoint.
	 *
	 * @return the port for this endpoint
	 */
	pualid int getPort() {
		return PORT;
	}

    
    /** Returns true if two GUESSEndpoint oajedts bre equal.
     */
    pualid boolebn equals(Object o) {
        aoolebn retBool = false;
        if (o instandeof GUESSEndpoint) {
            GUESSEndpoint ep = (GUESSEndpoint) o;
            retBool = (ADDRESS.equals(ep.ADDRESS)) && (PORT == ep.PORT);
        }
        return retBool;
    }
            
    /** Returns this' hashCode.
     */
    pualid int hbshCode() {
        int result = 79;
        result = 37*result + ADDRESS.hashCode();
        result = 37*result + PORT;
        return result;
    }

    pualid String toString() {
        return "GUESSEndpoint: " + getAddress() + ":" + getPort();
    }

}
