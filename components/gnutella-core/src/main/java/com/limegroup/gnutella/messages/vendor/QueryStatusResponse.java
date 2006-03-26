
// Commented for the Learning branch

package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/**
 * As a leaf, send a Query Status Response vendor message to tell the ultrapeers searching for you how many hits you have.
 * QueryStatusResponse represents the BEAR 12 version 1 Query Status Response vendor message.
 * 
 * This packet is used in LimeWire's dynamic querying system.
 * In dynamic querying, a leaf has its ultrapeers perform a search on its behalf.
 * 
 * The leaf needs to tell the ultrapeer how many hits it's gotten and kept.
 * This is necessary in a variety of situations:
 * The leaf may be getting out of band replies directly in UDP packets, so the ultrapeer doesn't know how effective its search has been.
 * The leaf may have an agressive filter, causing it to throw out many of the results the ultrapeer is sending it.
 * The leaf has several of its ultrapeers searching for it, and one may be doing so well all can stop their search.
 * The leaf may have done really well searching its LAN on multicast, and not need any more hits from its ultrapeers.
 * 
 * The SearchResultHandler class makes QueryStatusResponse objects. (do)
 * 
 * A Query Status Response vendor message looks like this:
 * 
 * gnutella packet header 23 bytes
 * vendor type 8 bytes
 * 
 *   BEAR 12 1
 * 
 * rest of the payload
 * 
 *   nn
 * 
 * The rest of the payload is 2 bytes.
 * nn is the number of hits the computer has.
 * The number is stored in 2 bytes, and must be 1 through 65535, 0x0001 through 0xffff.
 * 
 * The hit number in a Query Status Response vendor message isn't the total number of hits the leaf has.
 * Rather, it's the number of hits the leaf has up to 150, divided by 4.
 * A leaf divides by 4 assuming it has connections up to 4 ultrapeers.
 * In the packet, nn is the number of hits the ultrapeer that receives the message has gotten the leaf that sent it.
 * 
 * This message contains 2 unsigned bytes that tell you how many
 * results the sending host has for the guid of a query (the guid of this
 * message is the same as the original query).
 */
public final class QueryStatusResponse extends VendorMessage {

    /** 1, LimeWire understands the initial version of the Query Status Request vendor message. */
    public static final int VERSION = 1;

    /**
     * Make a new QueryStatusResponse with data we read from a remote computer.
     * This is the message parser.
     * 
     * @param guid    The message GUID we read from the Gnutella packet header data
     * @param ttl     The TTL number we read from the Gnutella packet header data
     * @param hops    The hops count we read from the Gnutella packet header data
     * @param version The vendor message type version number we read from the payload data
     * @param payload The data beyond the Gnutella packet header and the start of the payload like LIMEssvv
     */
    QueryStatusResponse(byte[] guid, byte ttl, byte hops, int version, byte[] payload) throws BadPacketException {

        // Call the VendorMessage constructor
        super(
            guid,             // From the Gnutella packet header data, the message GUID
            ttl,              // From the Gnutella packet header data, the message TTL
            hops,             // From the Gnutella packet header data, the hops count
            F_BEAR_VENDOR_ID, // Vendor message BEAR 12, Query Status Response
            F_REPLY_NUMBER,
            version,          // From the payload, the version number
            payload);         // The payload we read specific to this kind of vendor message

        // Make sure this isn't a packet from the future and make sure the payload is exactly the right length
        if (getVersion() > VERSION) throw new BadPacketException("UNSUPPORTED VERSION");
        if (getPayload().length != 2) throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " + getPayload().length);
    }

    /**
     * Make a new QueryStatusResponse for us to send.
     * This is the message maker.
     * 
     * The following 3 methods use this constructor to make Query Status Response vendor messages for us to send:
     * SearchResultHandler.accountAndUpdateDynamicQueriers(QueryReply, int) makes a message to tell our ultrapeers how many hits their searching has gotten us.
     * SearchResultHandler.removeQuery(GUID) makes a message with 0xffff 65535 hits to tell the ultrapeers to stop.
     * ManagedConnection.morphToStopQuery(QueryStatusResponse) (do)
     * 
     * @param replyGUID  The GUID that identifies the search, matches the message GUID of all the packets, and can route packets back to the searching computer.
     *                   Sets this new packet's message GUID.
     * @param numResults The number of hits we have up to 150, divided by 4.
     *                   This is the number of hits we'd like each of our ultrapeers to get for us.
     *                   Puts this number in the payload.
     *                   Must be 1 through 65535.
     *                   If you have more than 65535 results, just say you have 65535.
     */
    public QueryStatusResponse(GUID replyGUID, int numResults) {

        // Call the VendorMessage constructor
        super(
            F_BEAR_VENDOR_ID,           // BEAR 12 1, the information that identifies a Query Status Response vendor message
            F_REPLY_NUMBER,
            VERSION,
            derivePayload(numResults)); // Compose the payload from the given results count, and save it in VendorMessage._payload

        // Set this new packet's message GUID
        setGUID(replyGUID);
    }

    /**
     * Get the results number that this Query Status Response vendor message is carrying.
     * This number is stored in the 2-byte payload beyond the LIMEssvv type information.
     * 
     * @return The number in the payload
     */
    public int getNumResults() {

        // Read the 2 byte payload in little endian order, and return the number stored there
        return ByteOrder.ushort2int(ByteOrder.leb2short(getPayload(), 0));
    }

    /**
     * Get the message GUID of this Query Status Response vendor message.
     * This is the GUID that identifies the search this Query Status Response is a part of.
     * All the packets involved in this search have this as their message GUID.
     * You can use this message GUID to route a packet back to the computer that sent the original request.
     * 
     * @return This packet's message GUID that identifies the search
     */
    public GUID getQueryGUID() {

        // Copy this packet's message GUID and return it
        return new GUID(getGUID());
    }

    /**
     * Write the given number in 2 bytes in little endian order.
     * This is the payload of the Query Status Response vendor message beyond the LIMEssvv type information.
     * 
     * @param numResults A number, like 5
     * @return           A 2-byte array with the number in little endian order, like {0x05, 0x00}
     */
    private static byte[] derivePayload(int numResults) {

        // Make sure the given results number will fit in 2 bytes
        if ((numResults < 0) || (numResults > 65535)) throw new IllegalArgumentException("Number of results too big: " + numResults);

        // Compose 2 bytes with the number in little endian order, and return it as the payload
        byte[] payload = new byte[2];
        ByteOrder.short2leb((short)numResults, payload, 0);
        return payload;
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
