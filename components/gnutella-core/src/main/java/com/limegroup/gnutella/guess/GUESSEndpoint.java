
// Edited for the Learning branch

package com.limegroup.gnutella.guess;

import java.net.InetAddress;

/**
 * GUESSEndpoint is part of GUESS, which LimeWire doesn't use anymore.
 * 
 * Simple host/port pair for endpoints used in GUESS.  The use of the
 * <tt>InetAddress</tt> class allows easy migration to IPv6.
 */
public final class GUESSEndpoint {

	/**
	 * Constant for the <tt>InetAddress</tt> object.
	 */
	private final InetAddress ADDRESS;

	/**
	 * Constant for the port.
	 */
	private final int PORT;

	/**
	 * Constructs a new <tt>GUESSEndpoint</tt> with the specified
	 * IP and port.
	 *
	 * @param address the ip address of the host
	 * @param port the port the host is listening on
	 */
	public GUESSEndpoint(InetAddress address, int port) {
		ADDRESS = address;
		PORT = port;
	}

	/**
	 * Accessor for the <tt>InetAddress</tt> instance for this endpoint.
	 *
	 * @return the <tt>InetAddress</tt> instance for this endpoint
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

    
    /** Returns true if two GUESSEndpoint objects are equal.
     */
    public boolean equals(Object o) {
        boolean retBool = false;
        if (o instanceof GUESSEndpoint) {
            GUESSEndpoint ep = (GUESSEndpoint) o;
            retBool = (ADDRESS.equals(ep.ADDRESS)) && (PORT == ep.PORT);
        }
        return retBool;
    }
            
    /** Returns this' hashCode.
     */
    public int hashCode() {
        int result = 79;
        result = 37*result + ADDRESS.hashCode();
        result = 37*result + PORT;
        return result;
    }

    public String toString() {
        return "GUESSEndpoint: " + getAddress() + ":" + getPort();
    }

}
