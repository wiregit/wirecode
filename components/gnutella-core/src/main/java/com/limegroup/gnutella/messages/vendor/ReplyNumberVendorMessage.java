
// Commented for the Learning branch

package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/**
 * Send a Reply Number vendor message to tell a searching computer how many hits you have, and if you can receive unsolicited UDP.
 * ReplyNumberVendorMessage represents the LIME 12 version 2 Reply Number vendor message.
 * 
 * This packet is used in LimeWire's out-of-band reply system.
 * There are 2 computers: the searching computer, and the computer that has a hit.
 * The searching computer sends a query packet to the hit computer.
 * The hit computer doesn't send back a query hit packet right away.
 * Instead, it sends back a Reply Number vendor message that tells the searching computer how many hits it has.
 * The searching computer sends a Lime Acknowledgement vendor message to the hit computer to say how many of those hits it wants.
 * The hit computer wraps the requested number of results into query hit packets and sends them to the searching computer.
 * The query is broadcast in the Gnutella network along TCP socket Gnutella connections.
 * The Reply Number, Lime Acknowledgement, and query hit packets are sent out-of-band in UDP packets.
 * All 4 packets have the same message GUID so computers can tell they are related.
 * 
 * A Reply Number vendor message looks like this:
 * 
 * gnutella packet header 23 bytes
 * vendor type 8 bytes
 * 
 *   LIME 12 2
 * 
 * rest of the payload
 * 
 *   nu
 * 
 * The rest of the payload is 2 bytes.
 * n is the number of hits the hit computer has.
 * The number is stored in 1 byte, and must be 1 through 255, 0x01 through 0xff.
 * u is 0 if the hit computer can't receive unsolicited UDP packets, or 1 if the hit computer is externally contactable for UDP.
 * (ask) But I thought the hit computer would reply with query hits in band if it was UDP firewalled?
 * 
 * This class can read version 1 and version 2 Reply Number vendor messages.
 * Version 1 messages don't have the u byte above.
 * The canReceiveUnsolicited() method always returns true instead of looking at the value in the u byte.
 * 
 * StandardMessageRouter.sendResponses() makes a new Reply Number vendor message for us to send.
 * It looks like this:
 * 
 * ae 91 f2 b2 d3 92 f9 fe  --------  aaaaaaaa
 * 87 df 6e a6 c9 b1 21 00  --n---!-  aaaaaaaa
 * 31 01 00 0a 00 00 00 4c  1------L  bcdeeeef
 * 49 4d 45 0c 00 02 00 05  IME-----  fffffffg
 * 01                       -         h
 * 
 * a, b, c, d, and e make up the 23 byte Gnutella packet header.
 * a is the message GUID.
 * b is 0x31, the byte that marks this as a vendor message.
 * c is the TTL, 1.
 * d is the hops, 0.
 * e is the length of the payload in little endian, 0x0a, 10 bytes, the size of the payload, f, g, and h.
 * 
 * f is the 8 bytes like LIMEssvv that indentify what kind of vendor message this is.
 * The vendor code is LIME, the type number is 0x0c, 12, and the version number is 2.
 * 
 * g is the number of hits we have, 5.
 * h is 1 because we can receive UDP packets.
 * 
 * This message contains a unsigned byte (1-255) that tells you how many
 * results the sending host has for the guid of a query (the guid of this
 * message is the same as the original query).  The recieving host can ACK
 * this message with a LimeACKVendorMessage to actually recieve the replies.
 * 
 * This message must maintain backwards compatibility between successive
 * versions.  This entails that any new features would grow the message
 * outward but shouldn't change the meaning of older fields.  This could lead
 * to some issues (i.e. abandoning fields does not allow for older fields to
 * be reused) but since we don't expect major changes this is probably OK.
 * 
 * Note that this behavior of maintaining backwards compatiblity is really
 * only necessary for UDP messages since in the UDP case there is probably no
 * MessagesSupportedVM exchange.
 */
public final class ReplyNumberVendorMessage extends VendorMessage {

    /** 2, LimeWire understands the updated version of the Reply Number vendor message. */
    public static final int VERSION = 2;

    /** 0x01, Use this byte to indicate the computer can receive unsolicited UDP packets. */
    private static final byte UNSOLICITED = 0x1;

    /**
     * Make a new ReplyNumberVendorMessage with data we read from a remote computer.
     * This is the message parser.
     * 
     * Only VendorMessage.deriveVendorMessage() calls this.
     * It parses network data into a ReplyNumberVendorMessage object that represents the packet a remote computer sent us.
     * 
     * @param guid    The message GUID we read from the Gnutella packet header data
     * @param ttl     The TTL number we read from the Gnutella packet header data
     * @param hops    The hops count we read from the Gnutella packet header data
     * @param version The vendor message type version number we read from the payload data
     * @param payload The data beyond the Gnutella packet header and the start of the payload like LIMEssvv
     */
    ReplyNumberVendorMessage(byte[] guid, byte ttl, byte hops, int version, byte[] payload) throws BadPacketException {

        // Call the VendorMessage constructor
        super(
            guid,             // From the Gnutella packet header data, the message GUID
            ttl,              // From the Gnutella packet header data, the message TTL
            hops,             // From the Gnutella packet header data, the hops count
            F_LIME_VENDOR_ID, // Vendor message LIME 12, Reply Number
            F_REPLY_NUMBER,
            version,          // From the payload, the version number
            payload);         // The payload we read specific to this kind of vendor message

        // Make sure the payload is long enough
        if (getPayload().length < 1) throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " + getPayload().length);
        if ((getVersion() == 1) && (getPayload().length != 1)) throw new BadPacketException("VERSION 1 UNSUPPORTED PAYLOAD LEN: " + getPayload().length);
        if ((getVersion() == 2) && (getPayload().length != 2)) throw new BadPacketException("VERSION 2 UNSUPPORTED PAYLOAD LEN: " + getPayload().length);
    }

    /**
     * Make a new ReplyNumberVendorMessage for us to send.
     * We send a Reply Number vendor message when we're a hit computer, telling a searching computer how many hits we have.
     * This is the message maker.
     * 
     * Only StandardMessageRouter.sendResponses() calls this.
     * It's buffered a response to send later, and tells the searching computer how many hits we have.
     * 
     * @param replyGUID  The GUID that identifies the search, matches the message GUID of all the packets, and can route packets back to the searching computer.
     *                   Sets this new packet's message GUID.
     * @param numResults The number of hits we have.
     *                   Puts this number in the payload.
     *                   Must be 1 through 255.
     *                   If you have more than 255 results, just say you have 255.
     */
    public ReplyNumberVendorMessage(GUID replyGUID, int numResults) {

        // Call the VendorMessage constructor
        super(
            F_LIME_VENDOR_ID,           // LIME 12 2, the information that identifies a Reply Number vendor message
            F_REPLY_NUMBER,
            VERSION,
            derivePayload(numResults)); // Compose the payload from the given results count, and save it in VendorMessage._payload

        // Set this new packet's message GUID
        setGUID(replyGUID);
    }

    /**
     * Get the results count this Reply Number vendor message is carrying in its payload.
     * This is the number of hits the sharing computer that sent the message has.
     * 
     * Only MessageRouter.handleReplyNumberMessage() calls this. (do)
     * 
     * @return The results count, a number 1 through 255
     */
    public int getNumResults() {

        // Get the results count from the start of the payload beyond the LIMEssvv data
        return ByteOrder.ubyte2int(getPayload()[0]);
    }

    /**
     * Determine if the sharing computer that sent this Reply Number vendor message can receive unsolicited UDP packets.
     * This is the flag in the second payload byte.
     * When we receive a Reply Number vendor message, we make sure it has this flag set before doing anything with it.
     * 
     * Only MessageRouter.handleReplyNumberMessage() calls this.
     * A remote computer has sent us a Reply Number vendor message in a UDP packet.
     * It has hits for us, and is telling us how many it has.
     * handleReplyNumberMessage() calls canReceiveUnsolicited() to make sure the hit computer is externally contactable for UDP.
     * If it's not, we can't communicate with it, so it's results are meaningless.
     * 
     * @return True if the sharing computer is externally contactable for UDP, false if it's firewalled
     */
    public boolean canReceiveUnsolicited() {

        // If we read a version 1 Reply Number vendor message, it won't have a second byte, return true to assume it can get UDP
    	if (getVersion() == 1) return true;
    	else                   return (getPayload()[1] & UNSOLICITED) == UNSOLICITED; // For version 2, read the second byte
    }

    /**
     * Compose the payload from a given number of results.
     * The payload data beyond the LIMEssvv bytes looks like this:
     * 
     * nu
     * 
     * It's 2 bytes.
     * n is the number of hits the hit computer has.
     * The number is stored in 1 byte, and must be 1 through 255, 0x01 through 0xff.
     * u is 0 if the hit computer can't receive unsolicited UDP packets, or 1 if the hit computer is externally contactable for UDP.
     * 
     * This method returns data about us.
     * It should only be used to make a packet we send.
     * 
     * @param numResults The result number to include in the payload, 1 through 255
     * @return           A byte array like nu, the given number in the first byte, and our UDP firewalled status in the second
     */
    private static byte[] derivePayload(int numResults) {

        // Make sure the number of results is 1 through 255
        if ((numResults < 1) || (numResults > 255)) throw new IllegalArgumentException("Number of results too big: " + numResults);

        // Make a 2-byte array to compose the payload in
        byte[] payload = new byte[2];

        // Write the given number into a byte
        byte[] bytes = new byte[2]; // The number will fit into the first byte
        ByteOrder.short2leb((short) numResults, bytes, 0);

        // Put the given number in the first payload byte
        payload[0] = bytes[0];

        // If we've received a UDP packet, make the second byte 0x01, otherwise make it 0x00
        payload[1] = RouterService.canReceiveUnsolicited() ? UNSOLICITED : 0x0;

        // Return the 2-byte payload we composed
        return payload;
    }

    /**
     * Determine if this ReplyNumberVendorMessage is the same as another.
     * Compares the message GUIDs and results numbers.
     * Doesn't compare the UDP firewalled status.
     * 
     * @param other The object to compare this one to
     * @return      True if they contain the same information, false if they are different
     */
    public boolean equals(Object other) {

        // Make sure the given object is a ReplyNumberVendorMessage
        if (other instanceof ReplyNumberVendorMessage) {

            // Copy the message GUIDs from both packets
            GUID myGuid = new GUID(getGUID());
            GUID otherGuid = new GUID(((VendorMessage)other).getGUID());

            // Get the results number the given message is carrying
            int otherResults = ((ReplyNumberVendorMessage)other).getNumResults();

            // Return true if the GUIDs and results numbers match, false if either are different
            return ((myGuid.equals(otherGuid)) && (getNumResults() == otherResults) && super.equals(other));
        }

        // Different
        return false;
    }

    /**
     * Write the payload of this vendor message to the given OutputStream.
     * Writes the 8-byte LIMEssvv type identifer that begins the payload, and the type specific payload data after that.
     * This class overrides this method from VendorMessage to count the statistic that we're sending this message.
     * 
     * @param out An OutputStream we can call write() on to give it data
     */
    protected void writePayload(OutputStream out) throws IOException {

        // Call VendorMessage.writePayload() to write the 8 type bytes like LIMEssvv, and the payload data after that
        super.writePayload(out);

        // We only write the payload when we're sending the message, count the statistic
        SentMessageStatHandler.UDP_REPLY_NUMBER.addMessage(this);
    }

    /**
     * Does nothing.
     * Call recordDrop() to record that we're dropping this Gnutella packet in the program's statistics.
     */
    public void recordDrop() {

        // Does nothing
        super.recordDrop();
    }
}
