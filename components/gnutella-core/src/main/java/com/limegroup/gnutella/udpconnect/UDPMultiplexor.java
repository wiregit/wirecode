
// Commented for the Learning branch

package com.limegroup.gnutella.udpconnect;

import java.lang.ref.WeakReference;
import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The program's single UDPMultiplexor object keeps a list of our up to 255 UDP connections, represented by UDPConnectionProcessor objects.
 * 
 * The UDPMultiplexor has an array named _connections.
 * Indices 1 through 255 in the array lead to UDPConnectionProcessor objects.
 * To find the UDPConnectionProcessor object register() assigned the connection ID 5, look in _connections[5].
 * 
 * When the program makes a new UDPConnectionProcessor object to initiate a new UDP connection to a remote computer, it calls register().
 * register() assigns it a connection ID number, 1 through 255.
 * 
 * When the program gets a UDP connection message, the UDPMultiplexor tells it which UDPConnectionProcessor object it belongs to.
 * routeMessage() looks at the connection ID in the message's first byte, and the IP address and port number it came from.
 */
public class UDPMultiplexor {

    /** A log we can write lines of text to as the program runs. */
    private static final Log LOG = LogFactory.getLog(UDPMultiplexor.class);

	/** The program makes a single UDPMultiplexor object to keep the list of our UDP connections. */
    private static UDPMultiplexor _instance = new UDPMultiplexor();

	/** 0, the UDPMultiplexor assigns new UDPConnectionProcessor objects IDs of 1 through 255, leaving 0 to mean not assigned. */
	public static final byte UNASSIGNED_SLOT = 0;

	/**
     * The list of our current UDP connections.
     * 
     * The _connections array has 256 references, indexed from 0 through 255.
     * We don't use the 0 spot, and only use 1 through 255.
     * The array index matches the connection ID number.
     * _connections[5] leads to the UDP connection we assigned the ID 5.
     * 
     * _connections is an array of references to Java WeakReference objects.
     * Each WeakReference object points to a UDPConnectionProcessor object that represents a UDP connection to a remote coputer.
     * Call weakReference.get() to get UDPConnectionProcessor object from the WeakReference object.
     * 
     * A weak reference to an object won't prevent the garbage collector from deleting it.
     * Suppose that the garbage collector determines that a UDPConnectionProcessor object is only reachable through its WeakReference object in our _connections array.
     * It will null the reference in the WeakReference object, and delete the UDPConnectionProcessor.
     * When we next call weakReferece.get(), it will return null.
     */
	private volatile WeakReference[] _connections;

	/**
     * The last connection ID number the register() method assigned a new connection.
     * 
     * The register() method loops from _lastConnectionID + 1 onwards, returning the first unused ID it finds.
     * _lastConnectionID is initialized to 0, making 1 the first connection ID register() will assign.
     * It assigns the connection IDs 1, 2, 3, 4, all the way up to 255, and then loops back to 1 again.
     * If it finds a UDPConnectionProcessor in _connections at a given ID, it tries the next one.
     * 
     * Keep track of the last assigned connection id so that we can use a
	 * circular assignment algorithm.  This should cut down on message
	 * collisions after the connection is shut down.
     */
	private int _lastConnectionID;

    /**
     * Access the program's single UDPMultiplexor object that keeps the list of our UDP connections.
     * 
     * @return The UDPMultiplexor object
     */
    public static UDPMultiplexor instance() {

        // Return the static reference to the object we made
		return _instance;
    }

    /**
     * Make the program's single UDPMultiplexor object.
     */
    private UDPMultiplexor() {

        // Make _connections, an array of 256 weak references
		_connections = new WeakReference[256];

        // Start _lastConnectionID at 0 so the first connection ID we'll assign will be 1
		_lastConnectionID = 0;
    }

    /**
     * Determine if the program has a UDP connection to the given IP address.
     * 
     * @param host An IP address as a Java InetAddress object
     * @return     True if we're connected to that remote coputer with a UDP connection, false if we're not
     */
    public boolean isConnectedTo(InetAddress host) {

        // Point array at _connections, the UDPMultiplexor's array of 256 weak references to UDPConnectionProcessor objects that represent our current UDP connections
        WeakReference[] array = _connections;

        // If we have never assigned a connection ID, we're not connected to anything yet, return no
        if (_lastConnectionID == 0) return false;

        // Loop through all the UDPConnectionProcessor objects weakly referenced by the 256 places in the array
        for (int i = 0; i < array.length; i++) {
            WeakReference conRef = array[i];                                       // Look up the WeakReference object at i in the _connections array
            if (conRef != null) {                                                  // If there is a WeakReference there, not null
                UDPConnectionProcessor con = (UDPConnectionProcessor)conRef.get(); // Get the object the WeakReference points to, if it still exists

                // If i leads to a UDPConnectionProcessor, and the remote computer it connects us to have the IP address we're looking for
                if (con != null && host.equals(con.getInetAddress())) {

                    // We do have a connection to the given IP address
                    return true;
                }
            }
        }

        // Not found
        return false;
    }

    /**
     * Add a new UDPConnectionProcessor object to the list of them the UDPMultiplexor keeps, and assign it an ID 1 through 255.
     * 
     * This registers it for receiving incoming events. (do)
     * 
     * register() returns 1, 2, 3, 4, ... 253, 254, 255, and then wraps around to 1, 2, 3.
     * It skips over numbers still in use.
     * If the program has 255 UDP connections taking up all the IDs 1 through 255, doesn't list it and returns null.
     * 
     * The UDPMultiplexor won't create a normal reference to the given UDPConnectionProcessor object.
     * If you loose all your references to it, the garbage collector will delete it, and it will safely remove itself from our list of connections.
     * 
     * @param con A new UDPConnectionProcessor object.
     * @return    The connection ID we chose for it.
     *            If we already have 255 UDP connections taking up the IDs 1 through 255, returns null.
     */
	public synchronized byte register(UDPConnectionProcessor con) {

        // The connection ID number from 1 through 255 that we'll choose for this new UDP connection, and return
		int connID;

        // Make a copy of our _connections array
		WeakReference[] copy = new WeakReference[_connections.length];           // Make a new array that can reference 256 WeakReference objects
		for (int i = 0; i < _connections.length; i++) copy[i] = _connections[i]; // Copy all the references from _connections to copy

        // Loop 256 times, with i going from 1 through 256
		for (int i = 1; i <= copy.length; i++) {

            /*
             * The first time this loop runs, connID will be 1 more than the connection ID register() assigned the last time.
             * The second time this loop runs, connID will be 1 more than that.
             * connID can't reach 256, it loops around like 253, 254, 255, 0, 1, 2, 3.
             * If we get 0, we loop again, making it wrap around like 253, 254, 255, 1, 2, 3.
             */

            // Try the next connection ID number, only choosing from values from 1 through 255
			connID = (_lastConnectionID + i) % 256;
			if (connID == 0) continue; // Never assign 0, loop again to try connID of 1 instead

			// If the slot is open, take it
			if (copy[connID] == null ||       // This spot in our array of 256 references to WeakReference objects is null instead of pointing to one, or
                copy[connID].get() == null) { // There is a WeakReference object here, but the UDPConnectionProcessor it pointed to is gone

                // Save the connection ID we're choosing as the last one we've produced
				_lastConnectionID = connID; // The next time register() runs, it will try looking from this point forward

                // Make a new WeakReference object that points to the given UDPConnectionProcessor, and store it in our array
				copy[connID] = new WeakReference(con); // Change the copy of the array we made, then
				_connections = copy;                   // Point _connections at the copy, this is faster than editing the array directly

                // Return the connection ID we assigned
				return (byte)connID;
			}
		}

        // All the slots 1 through 255 are full
		return UNASSIGNED_SLOT; // Return 0, we can't start a new connection
	}

    /**
     * Remove a given UDPConnectionProcessor object from the list of them the UDPMultiplexor keeps.
     * Unregisters the UDPConnectionProcessor for receiving incoming messages.
	 * Frees up the slot.
     * 
     * @param con A UDPConnectionProcessor object in our array
     */
	public synchronized void unregister(UDPConnectionProcessor con) {

        // Get the connection ID we assigned the connection we're going to unregister
		int connID = (int)con.getConnectionID() & 0xff; // Make sure it fits into 1 byte

        // Make a copy of the _connections array
		WeakReference[] copy = new WeakReference[_connections.length];           // Make a new array called copy that can hold 255 WeakReference objects
		for (int i = 0; i < _connections.length; i++) copy[i] = _connections[i]; // Copy all the references to WeakReference objects from _connections to copy

        // Look up the connection ID in the array
		if (copy[connID] != null &&      // There is a WeakReference object here, and
            copy[connID].get() == con) { // It weakly references a UDPConnectionProcessor object

            // Clear that spot in the array
		    copy[connID].clear(); // Have the WeakReference disconnect from the UDPConnectionProcessor object
		    copy[connID] = null;  // Remove the WeakReference object from the array
		}

        // Point _connections at the array we copied and edited
        _connections = copy;
	}

    /**
     * Give a UDP connection message we've just received to the UDPConnectionProcessor that represents the UDP connection the message is a part of.
     * 
     * The message we just received is part of a UDP connection, represented by a UDPConnectionProcessor object.
     * It's routeMessage()'s job to give the message to the right connection.
     * 
     * We use 2 pieces of information about the UDP connection message we just received to find the right UDPConnectionProcessor:
     * The connection ID, like 0, or 1 through 255, written into its first byte.
     * The IP address and port number we got the message from.
     * 
     * The UDPMultiplexor keeps an array called _connections of UDPConnectionProcessor objects, indexed 1 through 255.
     * If the message we just got has a connection ID of 5, we look up _connections[5], and give the message to that UDPConnectionProcessor.
     * We make sure the UDPConnectionProcessor is connected to the same IP address and port number the message came from, also.
     * 
     * When the connection is just starting, we'll get Syn messages with connection IDs of 0.
     * In this case, we find the UDPConnectionProcessor by IP address and port number alone.
     * routeMessage() looks through the _connections array until it finds a UDPConnectionProcessor with an IP address and port number that match the message's.
     * Then, it gives the message to that UDPConnectionProcessor.
     * 
     * @param msg        A UDP connection message we just received
     * @param senderIP   The IP address it came from
     * @param senderPort The port number it came from
     */
	public void routeMessage(UDPConnectionMessage msg, InetAddress senderIP, int senderPort) {

        // When we find the UDPConnectionProcessor that represents the UDP connection the message we just received is a part of, we'll point con at it
		UDPConnectionProcessor con;

        // Point array at our _connections array of 255 references to WeakReference objects
		WeakReference[] array = _connections;

        // Read the connection ID from the first byte of the message
		int connID = (int)msg.getConnectionID() & 0xff; // Mask with 0xff to make sure connID isn't bigger than 255

        /*
         * If connID equals 0 and SynMessage then associate with a connection
         * that appears to want it (connecting and with knowledge of it).
         */

        // The connection ID in the first byte of the message is 0, and it's a Syn message
        if (connID == 0 && msg instanceof SynMessage) {

            // Make a note we've received a Syn message with a connection ID of 0
            if (LOG.isDebugEnabled()) LOG.debug("Receiving SynMessage :" + msg);

            // Loop i from 1 through 255, covering every connection ID we assign
            for (int i = 1; i < array.length; i++) {

                // Look up the UDPConnectionProcessor at the index i that we assigned connection ID i in the array
				if (array[i] == null) con = null;                                   // No WeakReference object here
				else                  con = (UDPConnectionProcessor)array[i].get(); // Get the UDPConnectionProcessor the WeakReference object references

                // See if our UDPConnectionProcessor object at i is trying to connect to the same IP address that sent us this packet
				if (con != null        &&                     // Our array has a UDPConnectionProcessor object for i, and
					con.isConnecting() &&                     // It hasn't connected yet, and
					con.matchAddress(senderIP, senderPort)) { // It's trying to connect to the same computer that just sent us the Syn message with an ID of 0

                    // We've found the UDPConnectionProcessor to give the message to
                    if (LOG.isDebugEnabled()) LOG.debug("routeMessage to conn:" + i + " Syn:" + msg);
					con.handleMessage(msg);
                    break;
				}
			}

            /*
             * Note: eventually these messages should find a match
             * so it is safe to throw away premature ones
             */

        // The message has a connection ID of 1 through 255 in the first byte of the message
		} else {

            /*
             * If valid connID then send on to connection
             */

            // Look up the UDPConnectionProcessor object at the connection ID's place in the array
            if (array[connID] == null) con = null;                                        // No WeakReference object
			else                       con = (UDPConnectionProcessor)array[connID].get(); // Get the UDPConnectionProcessor from the WeakReference object

            // If we found a UDPConnectionProcessor object with the message's ID, and this packet came from the same IP address and port number it's connected to
			if (con != null && con.matchAddress(senderIP, senderPort)) {

                // Give the message we just received to the UDPConnectionProcessor object
				con.handleMessage(msg);
			}
		}
	}
}
