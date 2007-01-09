package com.limegroup.gnutella.guess;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.limewire.io.IpPort;



/**
 * Simple host/port pair for endpoints used in GUESS.  The use of the
 * <tt>InetAddress</tt> class allows easy migration to IPv6.
 */
public final class GUESSEndpoint implements IpPort {

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
    

	/** Returns the address as a string. */
    public String getAddress() {
        return ADDRESS.getHostAddress();
    }

	/**
	 * Accessor for the <tt>InetAddress</tt> instance for this endpoint.
	 *
	 * @return the <tt>InetAddress</tt> instance for this endpoint
	 */
	public InetAddress getInetAddress() {
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

	public SocketAddress getSocketAddress() {
		return new InetSocketAddress(getInetAddress(), getPort());
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
        return "GUESSEndpoint: " + getInetAddress() + ":" + getPort();
    }

}
