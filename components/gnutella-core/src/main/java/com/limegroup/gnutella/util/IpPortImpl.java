
// Commented for the Learning branch

package com.limegroup.gnutella.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * An IpPortImpl object holds the data of an IP address and port number together.
 * This is the address of a computer on the Internet.
 * 
 * IpPortImpl implements the IpPort interface without adding additional data or features.
 * 
 * LimeWire has many classes that hold IP address and port number data:
 * IpPortImpl                    - This class, holds an IP address and port number without additional features.
 * Endpoint and ExtendedEndpoint - A listing in the gnutella.net file of an IP address and port number we can try to connect to.
 * QueryReply.IPPortCombo        - Has checking and throws exceptions specific to QueryReply packets.
 * 
 * These classes implement the IpPort interface, letting them be in an IpPortSet list.
 * IpPort    - An interface that requires methods like getAddress() and getPort().
 * IpPortSet - A list that keeps objects that implement the IpPort interface in sorted order with no duplicates.
 */
public class IpPortImpl implements IpPort {

    /** The IP address as a Java InetAddress object. */
    private final InetAddress addr;

    /** The IP address as text like "24.3.65.216". */
    private final String addrString;

    /** The port number. */
    private final int port;

    /**
     * Make a new IpPortImpl object with the given IP address and port number.
     * Only the next constructor here calls this method.
     * 
     * @param addr The IP address in a Java InetAddress object
     * @param host The text like "24.3.65.216" the next constructor used to make the InetAddress object
     * @param port The port number
     */
    public IpPortImpl(InetAddress addr, String host, int port) {

        // Save the given information in this object
        this.addr       = addr; // The IP address in a Java InetAddress object
        this.addrString = host; // The text we used to make that InetAddress object, like "24.3.65.216"
        this.port       = port; // The port number
    }

    /**
     * Make a new IpPortImpl object with the given IP address and port number.
     * 
     * @param host The IP address, like "24.3.65.216"
     * @param port The port number
     */
    public IpPortImpl(String host, int port) throws UnknownHostException {

        // Make a Java InetAddress object from the text, and pass it and the port number to the constructor above
        this(InetAddress.getByName(host), host, port);
    }

    /**
     * Get the IP address as an InetAddress object.
     * The IpPort interface requires this method.
     * 
     * @return The IP address as a Java InetAddress object
     */
    public InetAddress getInetAddress() {

        // Return the InetAddress object we made from the addrString text
        return addr;
    }

    /**
     * Get the IP address as text.
     * The IpPort interface requires this method.
     * 
     * @return The IP address as a String like "24.3.65.216"
     */
    public String getAddress() {

        // Return the String the constructor saved
        return addrString;
    }

    /**
     * The port number this IpPortImpl object is storing.
     * The IpPort interface requires this method.
     * 
     * @return The port number
     */
    public int getPort() {

        // Return the port number the constructor saved
        return port;
    }

    /**
     * Express this IpPortImpl object as text.
     * 
     * @return A String like "host: 24.3.65.216, port: 6346"
     */
    public String toString() {

        // Compose the text
        return "host: " + getAddress() + ", port: " + getPort();
    }
}
