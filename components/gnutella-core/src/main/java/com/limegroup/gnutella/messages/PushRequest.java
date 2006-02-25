
// Commented for the Learning branch

package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * A PushRequest object represents a Gnutella push packet.
 * 
 * Since it's creation, Gnutella has had a method for firewalled computers to share files.
 * The method invovles a push packet.
 * It only works when one computer is firewalled, and the other is externally contactable.
 * 
 * There are 2 computers: Want and Have.
 * Want wants a file that Have has.
 * Want released a query on the Gnutella network, and Have responded with a query hit.
 * 
 * If Have is externally contactable, Want will open a new TCP socket connection to Have with HTTP GET headers.
 * Have will accept this connection, and serve the requested file.
 * If Have is behind a firewall or Network Address Translation device, this won't work.
 * 
 * Both computers are connected to the Gnutella network, and can exchange Gnutella packets through it.
 * Gnutella packets have to be small, though, and may travel through several other computers.
 * Want and Have can't send a whole file through the Gnutella network.
 * 
 * Want prepares a push packet, and puts the following information in it:
 * Have's client ID GUID, allowing the Gnutella network to route the packet to Have.
 * Want's IP address, allowing Have to push open a new connection to Want.
 * The ID number that Have assigned to the file it's sharing that Want wants, telling Have which file to give.
 * 
 * Want releases the push packet onto the Gnutella network, which delivers it to Have.
 * Have opens a new TCP socket connection to Want with HTTP GIV headers, and sends the desired file.
 * 
 * If Want is also firewalled, this system doesn't help at all.
 * LimeWire developed firewall-to-firewall transfers to solve this problem.
 * 
 * A push packet has the following binary structure:
 * 
 * header            At  0, 23 bytes  The Gnutella packet header
 * [client ID GUID]  At 23, 16 bytes  The client ID GUID of the computer that will get this packet and push open a new connection
 * FILE              At 39,  4 bytes  The number the pushing computer has given the file the wanting computer wants
 * IPIPPP            At 43,  6 bytes  The IP address and port number of the wanting computer
 * 
 * At the start of the payload, the client ID GUID and the FILE id are two pieces of information chosen and known by Have.
 * Have included them in the query hit packet it sent to Want.
 * This is how Want got this information, and how it is able to include it in the push packet.
 * 
 * The Gnutella specification allows for a GGEP block to exist beyond the standard 26 byte payload.
 * LimeWire doesn't place a GGEP block there, or read a GGEP block that arrives.
 * But, it will save the data of a GGEP block at the end of the payload byte array.
 * Call stripExtendedPayload() to get a copy of this object without it.
 */
public class PushRequest extends Message implements Serializable {

    /** 26, a push packet has a 26 byte payload after the header. */
    private static final int STANDARD_PAYLOAD_SIZE = 26;

    /** 2147483645, the file id to use when this is a firewall-to-firewall transfer, not a traditional push request. (do) */
    public static final long FW_TRANS_INDEX = Integer.MAX_VALUE - 2;

    /**
     * The push packet payload data.
     * The packet maker constructor composes it.
     * The packet parser constructor saves it, and then parses it.
     */
    private byte[] payload;

    /**
     * Make a new PushRequest object to represent a push packet we read from the network.
     * This is the packet parser.
     * 
     * @param guid    The GUID that uniquely identifies this Gnutella message
     * @param ttl     The message TTL, the number of times it will travel across the Internet
     * @param hops    The number of times this Gnutella message has traveled across the Internet
     * @param payload The 26 byte payload
     * @param network The Internet protocol that brought us this message, like 1 N_TCP or 2 N_UDP
     * @return        A new PushRequest object with that information
     */
    public PushRequest(byte[] guid, byte ttl, byte hops, byte[] payload, int network) throws BadPacketException {

        // Call the Message constructor to save information from the packet header in the Message members this PushRequest inherits
        super(guid, Message.F_PUSH, ttl, hops, payload.length, network);

        // Make sure the payload is at least 26 bytes
        if (payload.length < STANDARD_PAYLOAD_SIZE) {
            ReceivedErrorStat.PUSH_INVALID_PAYLOAD.incrementStat();
            throw new BadPacketException("Payload too small: " + payload.length);
        }

        // Save the payload data
        this.payload = payload;

        // Make sure the port number isn't 0
		if (!NetworkUtils.isValidPort(getPort())) {
		    ReceivedErrorStat.PUSH_INVALID_PORT.incrementStat();
			throw new BadPacketException("invalid port");
		}

        // Make sure the IP address doesn't start 0 or 255
		String ip = NetworkUtils.ip2string(payload, 20);
		if (!NetworkUtils.isValidAddress(ip)) {
		    ReceivedErrorStat.PUSH_INVALID_ADDRESS.incrementStat();
		    throw new BadPacketException("invalid address: " + ip);
		}
    }

    /**
     * Make a new PushRequest object for us to send with the given information.
     * Calls the packet maker.
     * 
     * @param guid       For the header, the message GUID that will mark this packet unique
     * @param ttl        For the header, the TTL
     * @param clientGUID For the payload, the client ID GUID of the firewalled computer that has the file and will push open the connection
     * @param index      For the payload, the ID number the firewalled computer has assigned the file the downloading computer wants
     * @param ip         For the payload, the downloading computer's IP address the firewalled computer will push open a connection to
     * @param port       For the payload, the downloading computer's port number the firewalled computer will push open a connection to
     * @return           A new PushRequest object with that information
     */
    public PushRequest(byte[] guid, byte ttl, byte[] clientGUID, long index, byte[] ip, int port) {

        // Call the next constructor
    	this(guid, ttl, clientGUID, index, ip, port, Message.N_UNKNOWN); // We don't know if we'll send this push packet over TCP or UDP yet
    }

    /**
     * Make a new PushRequest object for us to send with the given information.
     * This is the packet maker.
     * 
     * @param guid       For the header, the message GUID that will mark this packet unique
     * @param ttl        For the header, the TTL
     * @param clientGUID For the payload, the client ID GUID of the firewalled computer that has the file and will push open the connection
     * @param index      For the payload, the ID number the firewalled computer has assigned the file the downloading computer wants
     * @param ip         For the payload, the downloading computer's IP address the firewalled computer will push open a connection to
     * @param port       For the payload, the downloading computer's port number the firewalled computer will push open a connection to
     * @param network    The Internet protocol this push packet will travel on, like 1 N_TCP or 2 N_UDP, this is not a part of the packet that's sent
     * @return           A new PushRequest object with that information
     */
    public PushRequest(byte[] guid, byte ttl, byte[] clientGUID, long index, byte[] ip, int port, int network) {

        // Call the Message constructor to save packet header information
    	super(
            guid,                  // The message GUID
            Message.F_PUSH,        // 0x40, this is a push message
            ttl,                   // The number of times this push will be able to travel between ultrapeers
            (byte)0,               // This push message hasn't traveled any hops yet
            STANDARD_PAYLOAD_SIZE, // The payload length will be 26 bytes
            network);              // The Internet protocol we'll use to send, the Message object keeps this information but it is not part of the packet that's sent

        // Make sure the GUID is 16 bytes, the file id fits in 4 bytes, the IP is 4 bytes, the IP address doesn't start 0 or 255, and the port number fits in 2 bytes
    	if      (clientGUID.length != 16)            throw new IllegalArgumentException("invalid guid length: " + clientGUID.length);
        else if ((index & 0xFFFFFFFF00000000l) != 0) throw new IllegalArgumentException("invalid index: "       + index);
        else if (ip.length != 4)                     throw new IllegalArgumentException("invalid ip length: "   + ip.length);
        else if (!NetworkUtils.isValidAddress(ip))   throw new IllegalArgumentException("invalid ip "           + NetworkUtils.ip2string(ip));
        else if (!NetworkUtils.isValidPort(port))    throw new IllegalArgumentException("invalid port: "        + port);

        /*
         * The 26 byte pong payload has this structure:
         * 
         * [client ID GUID]  At  0, 16 bytes  The client ID GUID of the computer that will get this packet and push open a new connection
         * FILE              At 16,  4 bytes  The number the pushing computer has given the file the wanting computer wants
         * IPIPPP            At 20,  6 bytes  The IP address and port number of the wanting computer
         */

        // Compose the 26 byte push payload
        payload = new byte[STANDARD_PAYLOAD_SIZE];       // Make a byte array that can hold 26 bytes
        System.arraycopy(clientGUID, 0, payload, 0, 16); // Copy the client ID GUID into the first 16
        ByteOrder.int2leb((int)index, payload, 16);      // 16 bytes into the array, copy the 4 byte file id
        payload[20] = ip[0];                             // 20 bytes into the array, copy the 4 bytes of the IP address
        payload[21] = ip[1];
        payload[22] = ip[2];
        payload[23] = ip[3];
        ByteOrder.short2leb((short)port, payload, 24);   // 24 bytes into the array, place the 2 byte port number
    }

    /**
     * Have this QueryRequest object serialize the data of its payload for sending the packet.
     * 
     * @param out An OutputStream object we can call out.write(byteArray) on to send the byte array
     */
    protected void writePayload(OutputStream out) throws IOException {

        // Write the payload data we saved or composed
		out.write(payload);

        // Record that we sent this push packet in the program statistics
		SentMessageStatHandler.TCP_PUSH_REQUESTS.addMessage(this);
    }

    /**
     * Get the client ID GUID of the computer that will do the push.
     * This is the client ID GUID of the computer that has a file behind its firewall.
     * This push packet will make it to the computer, which will push open a connection to deliver the file.
     * The payload starts with this GUID.
     * 
     * @return The client ID GUID
     */
    public byte[] getClientGUID() {

        // Copy the 16 byte GUID from the start of the payload to a new byte array, and return it
        byte[] ret = new byte[16];
        System.arraycopy(payload, 0, ret, 0, 16);
        return ret;
    }

    /**
     * Get the file ID index of the file the firewalled computer is sharing, and will push out.
     * The firewalled computer returned a query hit with this file ID.
     * The downloading computer writes this ID in this push packet to tell the firewalled computer which one to push out.
     * 
     * @return The shared file ID
     */
    public long getIndex() {

        // Return the 4 byte file id 16 bytes into the payload
        return ByteOrder.uint2long(ByteOrder.leb2int(payload, 16));
    }

    /**
     * Determine if the file ID in this push packet marks it as special for a firewall-to-firewall transfer. (do)
     * 
     * @return True if the file ID is 2147483645, Integer.MAX_VALUE - 2
     */
    public boolean isFirewallTransferPush() {

        // Get the file ID, and see if it matches the special value
        return (getIndex() == FW_TRANS_INDEX);
    }

    /**
     * Get the IP address this push packet tells the firewalled computer to push open a connection to.
     * This is the IP address of the computer that wants the file and sent this packet.
     * 
     * @return The downloading computer's IP address as a 4 byte array
     */
    public byte[] getIP() {

        // Copy the 4 byte IP address from the payload to a new byte array, and return it
        byte[] ret = new byte[4];
        ret[0] = payload[20];
        ret[1] = payload[21];
        ret[2] = payload[22];
        ret[3] = payload[23];
        return ret;
    }

    /**
     * Get the port number this push packet tells the firewalled computer to push open a connection to.
     * This is the port number of the computer that wants the file and sent this packet.
     * 
     * @return The downloading computer's port number
     */
    public int getPort() {

        // Read the port number from the 2 bytes after the IP address
        return ByteOrder.ushort2int(ByteOrder.leb2short(payload, 24));
    }

    /**
     * Remove a GGEP block that might be attached to the end of the standard payload of this push packet.
     * 
     * A push packet looks like this:
     * 
     * header 23 bytes
     * payload 26 bytes
     * ggep block
     * 
     * The Gnutella specification allows for the GGEP block, but LimeWire doesn't use it.
     * There isn't any code here to insert a GGEP block in the push packets we send.
     * There also isn't any code to read a GGEP block that we get.
     * But, if a push packet arrives that has a GGEP block, the parsing constructor will save it here.
     * This method removes it.
     * 
     * @return A copy of this PushRequest object without a GGEP block we may have read from the network
     */
    public Message stripExtendedPayload() {

        /*
         * TODO: if this is too slow, we can alias parts of this, as as the
         * payload.  In fact we could even return a subclass of PushRequest that
         * simply delegates to this.
         */

        // Make a new 26 byte buffer that can hold just the standard push payload, and copy just that part in
        byte[] newPayload = new byte[STANDARD_PAYLOAD_SIZE];
        System.arraycopy(payload, 0, newPayload, 0, STANDARD_PAYLOAD_SIZE); // Leaves behind data beyond 26 bytes, the GGEP block

        // Make a new PushRequest object with the header information of this one, and the clipped payload
        try {
            return new PushRequest(this.getGUID(), this.getTTL(), this.getHops(), newPayload, this.getNetwork());
        } catch (BadPacketException e) {
            Assert.that(false, "Standard packet length not allowed!");
            return null;
        }
    }

    /**
     * Make a record that we've dropped this push packet.
     */
	public void recordDrop() {

        // Give this PushRequest object to the DroppedSentMessageHandler for TCP push packets
		DroppedSentMessageStatHandler.TCP_PUSH_REQUESTS.addMessage(this);
	}

    /**
     * Express this PushRequest object as text.
     * 
     * @return A String like "PushRequest({guid=00FF00FF00FF00FF00FF00FF00FF00FF, ttl=1, hops=1, priority=1} 1.2.3.4:5)"
     */
    public String toString() {

        // Compose and return the text
        return "PushRequest(" + super.toString() + " " + NetworkUtils.ip2string(getIP()) + ":" + getPort() + ")";
    }
}
