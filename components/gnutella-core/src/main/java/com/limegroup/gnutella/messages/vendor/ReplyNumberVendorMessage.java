package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.messages.BadPacketException;
import java.io.*;

/** In Vendor Message parlance, the "message type" of this VMP is "LIME/12".
 *  This message contains a unsigned byte (1-255) that tells you how many
 *  results the sending host has for the guid of a query (the guid of this
 *  message is the same as the original query).  The recieving host can ACK
 *  this message with a LimeACKVendorMessage to actually recieve the replies.
 */
public final class ReplyNumberVendorMessage extends VendorMessage {

    public static final int VERSION = 1;

    ReplyNumberVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                          byte[] payload) 
        throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_REPLY_NUMBER, version,
              payload);
        if (getVersion() > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");
        if (getPayload().length != 1)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPayload().length);
    }

    /** @param numResults The number of results (1-255 inclusive) that you have
     *  for this query.  If you have more than 255 just send 255.
     */
    public ReplyNumberVendorMessage(int numResults) throws BadPacketException {
        super(F_LIME_VENDOR_ID, F_REPLY_NUMBER, VERSION, derivePayload(numResults));
    }

    /** @return an int (1-255) representing the amount of results that a host
     *  for a given query (as specified by the guid of this message).
     */
    public int getNumResults() {
        return ByteOrder.ubyte2int(getPayload()[0]);
    }

    private static byte[] derivePayload(int numResults) throws BadPacketException {
        if ((numResults < 1) || (numResults > 255))
            throw new BadPacketException("Number of results too big: " +
                                         numResults);
        byte[] payload = new byte[1];
        byte[] bytes = new byte[2];
        ByteOrder.short2leb((short) numResults, bytes, 0);
        payload[0] = bytes[0];
        return payload;
    }

}
