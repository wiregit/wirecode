
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.util.Set;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortSet;

/**
 * Send a Gnutella ping packet to a list of IP addresses using UDP.
 * Makes sure we don't bother the same computer twice.
 * 
 * There is only one HostCatcher object, and it only makes one UniqueHostPinger.
 * So, there is only one of these as the program runs.
 */
public class UniqueHostPinger extends UDPPinger {

    /**
     * The ExtendedEndpoint objects we sent a packet.
     * The sendSingleMessage(host, m) method adds host to _recent.
     * The resetData() method clears everything from _recent.
     * 
     * An IpPortSet is actually a TreeSet.
     * With _recent, we'll look at it through its Set interface.
     */
    private final Set _recent = new IpPortSet();

    /**
     * Make a new UniqueHostPinger object.
     * It will let us send a Gnutella ping packet to a list of IP addresses using UDP.
     */
    public UniqueHostPinger() {

        // Just use the UDPPinger constructor to set it up
        super();
    }

    /**
     * Send a Gnutella packet to an IP address and port number.
     * 
     * @param host The IP address and port number to send the message to
     * @param m    The Gnutella packet to send to that computer
     */
    protected void sendSingleMessage(IpPort host, Message m) {

        // If we just sent something to this computer, don't do anything now
        if (_recent.contains(host)) return;

        // Make a note that we sent a packet to this remote computer, and send it
        _recent.add(host);
        super.sendSingleMessage(host, m);
    }

    /**
     * Clears our record of IP addresses we sent pings to.
     * 
     * Uses the same ProcessingQueue that UDPPinger does.
     * There may already be SenderBundle objects in this queue.
     * The thread will send those pings out before reaching the run() method here.
     */
    void resetData() {

        // Use the same ProcessingQueue that UDPPinger does
        QUEUE.add(

            // Make a new unnamed object that implements the Runnable interface 
            new Runnable() {

                // All this class has is a run() method
                public void run() {

                    // Clear all the ExtendedEndpoint objects from our list
                    _recent.clear();
                }
            }
        );
    }
}
