
// Commented for the Learning branch

package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/**
 * Send a Lime Acknowledgement vendor message to tell a hit computer how many hits you want.
 * LimeACKVendorMessage represents the LIME 11 version 2 Lime Acknowledgement vendor message.
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
 * A Lime Acknowledgement vendor message looks like this:
 * 
 * gnutella packet header 23 bytes
 * vendor type 8 bytes
 * 
 *   LIME 11 2
 * 
 * rest of the payload
 * 
 *   n
 * 
 * The rest of the payload is 1 byte.
 * n is the number of hits the searching computer wants.
 * The number is stored in 1 byte, and must be 0 through 255, 0x00 through 0xff.
 * (ask) Why can n be 0? Do we really send a Lime Acknowledgement vendor message to request 0 hits?
 * 
 * MessageRouter.handleReplyNumberMessage() makes a new Lime Acknowledgement vendor message for us to send.
 * It looks like this:
 * 
 * ae 91 f2 b2 d3 92 f9 fe  --------  aaaaaaaa
 * 87 df 6e a6 c9 b1 21 00  --n---!-  aaaaaaaa
 * 31 01 00 09 00 00 00 4c  1------L  bcdeeeef
 * 49 4d 45 0b 00 02 00 03  IME-----  fffffffg
 * 
 * a, b, c, d, and e make up the 23 byte Gnutella packet header.
 * a is the message GUID.
 * b is 0x31, the byte that marks this as a vendor message.
 * c is the TTL, 1.
 * d is the hops, 0.
 * e is the length of the payload in little endian, 0x09, 9 bytes, the size of the payload, f and g.
 * 
 * f is the 8 bytes like LIMEssvv that indentify what kind of vendor message this is.
 * The vendor code is LIME, the type number is 0x0b, 11, and the version number is 2.
 * 
 * g is the number of hits we want, 3.
 * 
 * This message acknowledges (ACKS) the guid contained in the message (i.e. A 
 * sends B a message with GUID g, B can acknowledge this message by sending a 
 * LimeACKVendorMessage to A with GUID g).  It also contains the amount of
 * results the client wants.
 * 
 * This message must maintain backwards compatibility between successive
 * versions.  This entails that any new features would grow the message
 * outward but shouldn't change the meaning of older fields.  This could lead
 * to some issues (i.e. abandoning fields does not allow for older fields to
 * be reused) but since we don't expect major changes this is probably OK.
 * EXCEPTION: Version 1 is NEVER accepted.  Only version's 2 and above are
 * recognized.
 * 
 * Note that this behavior of maintaining backwards compatiblity is really
 * only necessary for UDP messages since in the UDP case there is probably no
 * MessagesSupportedVM exchange.
 */
public final class LimeACKVendorMessage extends VendorMessage {

    /** 2, LimeWire understands the updated version of the Lime Acknowledgement vendor message. */
    public static final int VERSION = 2;

    /**
     * Make a new LimeACKVendorMessage with data we read from a remote computer.
     * This is the message parser.
     * 
     * Only VendorMessage.deriveVendorMessage() calls this.
     * It parses network data into a LimeACKVendorMessage object that represents the packet a remote computer sent us.
     * 
     * @param guid    The message GUID we read from the Gnutella packet header data
     * @param ttl     The TTL number we read from the Gnutella packet header data
     * @param hops    The hops count we read from the Gnutella packet header data
     * @param version The vendor message type version number we read from the payload data
     * @param payload The data beyond the Gnutella packet header and the start of the payload like LIMEssvv
     */
    LimeACKVendorMessage(byte[] guid, byte ttl, byte hops, int version, byte[] payload) throws BadPacketException {

        // Call the VendorMessage constructor
        super(
            guid,             // From the Gnutella packet header data, the message GUID
            ttl,              // From the Gnutella packet header data, the message TTL
            hops,             // From the Gnutella packet header data, the hops count
            F_LIME_VENDOR_ID, // Vendor message LIME 11, Lime Acknowledgement
            F_LIME_ACK,
            version,          // From the payload, the version number
            payload);         // The payload we read specific to this kind of vendor message

        // Make sure the version number is 2 and the payload data is long enough
        if (getVersion() == 1) throw new BadPacketException("UNSUPPORTED OLD VERSION");
        if (getPayload().length < 1) throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " + getPayload().length);
        if ((getVersion() == 2) && (getPayload().length != 1)) throw new BadPacketException("VERSION 2 UNSUPPORTED PAYLOAD LEN: " + getPayload().length);
    }

    /**
     * Make a new LimeACKVendorMessage for us to send.
     * We send a Lime Acknowledgement vendor message when we're a searching computer, requesting a number of hits from a hit computer.
     * This is the message maker.
     * 
     * Only MessageRouter.handleReplyNumberMessage() calls this. (do)
     * 
     * @param replyGUID  The GUID that identifies the search, matches the message GUID of all the packets, and can route packets back to the searching computer.
     *                   Sets this new packet's message GUID.
     * @param numResults The number of hits we want.
     *                   Puts this number in the payload.
     *                   Must be 0 through 255.
     */
    public LimeACKVendorMessage(GUID replyGUID, int numResults) {

        // Call the VendorMessage constructor
        super(
            F_LIME_VENDOR_ID,           // LIME 11 2, the information that identifies a Lime Acknowledgement vendor message
            F_LIME_ACK,
            VERSION,
            derivePayload(numResults)); // Compose the payload from the given results count, and save it in VendorMessage._payload

        // Set this new packet's message GUID
        setGUID(replyGUID);
    }

    /**
     * Get the results number this Lime Acknowledgement vendor message is carrying in its payload.
     * This is the number of hits the searching computer that sent the message wants.
     * 
     * Only MessageRouter.handleLimeACKMessage() calls this. (do)
     * 
     * @return The results count, a number 0 through 255
     */
    public int getNumResults() {

        // Get the results count from the start of the payload beyond the LIMEssvv data
        return ByteOrder.ubyte2int(getPayload()[0]);
    }

    /**
     * Compose the payload from a given number of results.
     * The payload data beyond the LIMEssvv data looks like this:
     * 
     * n
     * 
     * It's 1 byte.
     * n is the number of hits the searching computer wants.
     * The number is stored in 1 byte, and must be 1 through 255, 0x01 through 0xff.
     * 
     * @param numResults The result number to include in the payload, 0 through 255
     * @return           A byte array with the given number in 1 byte
     */
    private static byte[] derivePayload(int numResults) {

        // Make sure the number of results is 1 through 255
        if ((numResults < 0) || (numResults > 255)) throw new IllegalArgumentException("Number of results too big: " + numResults);

        // Make a 1-byte array to compose the payload in
        byte[] payload = new byte[1];

        // Write the given number into a byte
        byte[] bytes = new byte[2]; // The number will fit into the first byte
        ByteOrder.short2leb((short) numResults, bytes, 0);

        // Put the given number in the first payload byte
        payload[0] = bytes[0];

        // Return the 1-byte payload we composed
        return payload;
    }

    /**
     * Determine if this LimeACKVendorMessage is the same as another.
     * Compares the message GUIDs and results numbers.
     * 
     * @param other The object to compare this one to
     * @return      True if they contain the same information, false if they are different
     */
    public boolean equals(Object other) {

        // Make sure the given object is a LimeACKVendorMessage
        if (other instanceof LimeACKVendorMessage) {

            // Copy the message GUIDs from both packets
            GUID myGuid = new GUID(getGUID());
            GUID otherGuid = new GUID(((VendorMessage) other).getGUID());

            // Get the results number the other message is carrying
            int otherResults = ((LimeACKVendorMessage) other).getNumResults();

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
        SentMessageStatHandler.UDP_LIME_ACK.addMessage(this);
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
