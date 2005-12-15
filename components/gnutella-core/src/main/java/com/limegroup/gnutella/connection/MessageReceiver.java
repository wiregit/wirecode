
// Commented for the Learning branch

package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.Message;
import java.io.IOException;

/**
 * Implement the MessageReceiver interface so another object can give you Gnutella packets.
 * ManagedConnection implements MessageReceiver so MessageReader can give it packets.
 * 
 * MessageReader is the object that slices decompressed data from the remote computer into Gnutella packets.
 * MessageReader takes a MessageReceiver, and gets the ManagedConnection object.
 * When MessageReader calls receiver.getNetwork(), it's ManagedConnection.getNetwork() that is returning 1 for TCP.
 */
public interface MessageReceiver {

    /**
     * MessageReader calls ManagedConnection.processReadMessage(m) to give it a freshly sliced Gnutella packet.
     * 
     * MessageReader.handleRead() slices off a new Gnutella packet and then calls ManagedConnection.processReadMessage(m).
     * MessageReader gives the packet to ManagedConnection with this call.
     * ManagedConnection.processReadMessage(m) takes the packet, and processes it.
     * 
     * @param m The Gnutella packet that MessageReader read and is giving to ManagedConnection
     */
    public void processReadMessage(Message m) throws IOException;

    /**
     * MessageReader calls ManagedConnection.getSoftMax() to get the hops + TTL limit we've set for the remote computer.
     * 
     * MessageReader.handleRead() calls Connection.getSoftMax().
     * Connection.getSoftMax() returns the hops + TTL limit we've set for the remote computer these messages are from.
     * MessageReader.handleRead() puts this number in the new Message object it's making to hold the Gnutella packet.
     * 
     * @return The hops + TTL limit ManagedConnection has set for this remote computer
     */
    public byte getSoftMax();

    /**
     * MessageReader calls ManagedConnection.getNetwork() to find out ManagedConnection uses TCP and not UDP.
     * 
     * MessageReader.handleRead() calls ManagedConnection.getNetwork().
     * ManagedConnection.getNetwork() returns 1 Message.N_TCP, telling MessageReader the packet came in from TCP and not UDP.
     * MessageReader.handleRead() puts this number in the new Message object it's making to hold the Gnutella packet.
     * 
     * @return 1 for Message.N_TCP because the ManagedConnection object is using TCP, not UDP
     */
    public int getNetwork();

    /**
     * MessageReader calls ManagedConnection.messagingClosed() to tell the ManagedConnection object there will be no more packets.
     * 
     * MessageReader.shutdown() calls ManagedConnection.messagingClosed().
     * ManagedConnection.messagingClosed() has the ConnectionManager remove the ManagedConnection from its list.
     * So, shutting the MessageReader down will cause the ConnectionManager to remove and delete the ManagedConnection object.
     */
    public void messagingClosed();
}
