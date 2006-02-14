
// Commented for the Learning branch

package com.limegroup.gnutella.util;

import java.net.InetAddress;
import java.util.Comparator;

/**
 * LimeWire has several classes that hold an IP address and port number among their other information.
 * This interface was created to provide a uniform way for these classes to express their IP and port.
 * If you have a class that holds an IP and port, implement this class so that the IP and port are reachable in a uniform way.
 * 
 * Some of the important classes in LimeWire that implement IpPort are:
 * Connection and ManagedConnection - The object that represents a remote computer running Gnutella software that we're connected to.
 * Endpoint and ExtendedEndpoint - A listing in the gnutella.net file of an IP address to try to connect to.
 * QueryReply.IPPortCombo - An IP address and port number with checking specific to QueryReply packets.
 * IpPortImpl - An IP address and port number without additional features.
 */
public interface IpPort {

    /**
     * Get the IP address stored in this object.
     * The 4 byte IP address is returned as an InetAddress object.
     * This is like 216.27.158.74, and doesn't include a port number.
     * When your class implements this method, have it return the single IP address stored in the object.
     * 
     * @return The IP address stored in the object
     */
    InetAddress getInetAddress();

    /**
     * Get the port number stored in this object that goes with this IP address.
     * This may be the port number the host at the corresponding IP address is currently listening for new connections on.
     * 
     * @return The port number stored in this object that goes with the IP address from getInetAddress
     */
    int getPort();

    /**
     * Get the IP address and port number as a string, like "216.27.178.74"
     * 
     * @return The IP address and port number written out in a string
     */
    String getAddress();

    /**
     * A Comparator is an object that has a compare(a, b) method.
     * It takes two objects of the same type, and determines which should come first or if they are equal.
     * Comparators are used to see if two objects are the same, and to sort lists of objects into order.
     * A class which implements this interface must provide a comparator.
     */
    public static final Comparator COMPARATOR = new IpPortComparator();
    
    /**
     * A comparator to compare IpPort objects.
     * This is useful for when a variety of objects that implement IpPort want to be placed in a Set.
     * Since it is difficult to enforce that they all maintain a valid contract with regards to hashCode and equals,
     * the only valid way to enforce Set equality is to use a Comparator that is based on the IpPort-ness.
     */
    public static class IpPortComparator implements Comparator {

    	/**
    	 * The compare method which takes 2 objects and determines if they are the same or which comes first.
    	 * This compare method compares IpPort objects.
    	 * It sorts them by port number first, then the 4 numbers of the IP address start to finish.
    	 * 
    	 * @param a One object to compare
    	 * @param b The other object to compare
    	 * @return  Returns 0 if a == b, negative if a comes first, positive if b comes first
    	 */
    	public int compare(Object a, Object b) {

    		// If the a and b references point to the same object, the objects are the same
    		if (a == b) return 0;

    		// Cast both objects to what they really are, IpPort objects
    		IpPort ipa = (IpPort)a;
    		IpPort ipb = (IpPort)b;

    		// Subtract their port numbers
    		int diff = ipa.getPort() - ipb.getPort(); // If object a has a bigger port number, diff will be positive, b should come first

    		// They both have the same port number
    		if (diff == 0) {

    			// Extract the IP addresses as byte arrays
    			byte[] neta = ipa.getInetAddress().getAddress(); // An IP address is 4 bytes
    			byte[] netb = ipb.getInetAddress().getAddress();
    			
    			// Go through the 4 bytes, comparing the IP addresses based on the first that aren't the same
    			if (neta[0] == netb[0]) {                 // Byte 1, like 1.0.0.0, is the same
    				if (neta[1] == netb[1]) {             // Byte 2, like 0.1.0.0, is the same
    					if (neta[2] == netb[2]) {         // Byte 3, like 0.0.1.0, is the same
    						if (neta[3] == netb[3]) {     // Byte 4, like 0.0.0.1, is the same
    							return 0;                 // The two objects are identical
    						} else {                      // Byte 4 is different
    							return neta[3] - netb[3]; // Sort based on byte 4
    						}
    					} else {                          // Byte 3 is different
    						return neta[2] - netb[2];     // Sort based on byte 3
    					}
    				} else {                              // Byte 2 is different
    					return neta[1] - netb[1];         // Sort based on byte 2
    				}
    			} else {                                  // Byte 1 is different
    				return neta[0] - netb[0];             // Sort based on byte 1
    			}

    		// The port numbers are not the same
    		} else {

    			// Return the difference, positive if a is bigger and b should come first
    			return diff;
    		}
    	}
    }    
}
