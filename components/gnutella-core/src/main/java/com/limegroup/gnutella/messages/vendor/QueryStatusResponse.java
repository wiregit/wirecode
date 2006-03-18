
// Edited for the Learning branch

package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/**
 * 
 * 
 * 
 * Used by dynamic querying.
 * 
 * 
 * 
 * In Vendor Message parlance, the "message type" of this VMP is "BEAR/12".
 * This message contains 2 unsigned bytes that tells you how many
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
     * @param numResults The number of results (1-65535) that you have
     * for this query.  If you have more than 65535 just send 65535.
     * @param replyGUID The guid of the original query/reply that you want to
     * send reply info for.
     * 
     * 
     * @param replyGUID  The message GUID for this new message which will route it back home
     *                   Sets this new packet's message GUID.
     * @param numResults The result count to put in the payload
     *                   Puts this number in the payload.
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
     * @return an int (1-65535) representing the amount of results that a host
     * for a given query (as specified by the guid of this message).
     */
    public int getNumResults() {
        
        return ByteOrder.ushort2int(ByteOrder.leb2short(getPayload(), 0));
    }

    /** The query guid that this response refers to.
     */
    public GUID getQueryGUID() {
        
        return new GUID(getGUID());
    }

    /**
     * Constructs the payload from the desired number of results.
     */
    private static byte[] derivePayload(int numResults) {
        
        if ((numResults < 0) || (numResults > 65535)) throw new IllegalArgumentException("Number of results too big: " + numResults);
        byte[] payload = new byte[2];
        ByteOrder.short2leb((short) numResults, payload, 0);
        return payload;
    }

    //done

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
