package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.*;
import java.io.*;

/** In Vendor Message parlance, the "message type" of this VMP is "LIME/11".
 *  This message acknowledges (ACKS) the guid contained in the message (i.e. A 
 *  sends B a message with GUID g, B can acknowledge this message by sending a 
 *  LimeACKVendorMessage to A with GUID g).  It also contains the amount of
 *  results the client wants.
 */
public final class LimeACKVendorMessage extends VendorMessage {

    public static final int VERSION = 2;

    LimeACKVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                          byte[] payload) 
        throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_LIME_ACK, version,
              payload);
        if (getVersion() == 1)
            throw new BadPacketException("UNSUPPORTED OLD VERSION");
        if (getVersion() > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");
        if (getPayload().length != 1)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPayload().length);
    }

    /** @param numResults The number of results (0-255 inclusive) that you want
     *  for this query.  If you want more than 255 just send 255.
     *  @param replyGUID The guid of the original query/reply that you want to
     *  send reply info for.
     */
    public LimeACKVendorMessage(GUID replyGUID, 
                                int numResults) throws BadPacketException {
        super(F_LIME_VENDOR_ID, F_LIME_ACK, VERSION,
              derivePayload(numResults));
        setGUID(replyGUID);
    }

    /** @return an int (0-255) representing the amount of results that a host
     *  wants for a given query (as specified by the guid of this message).
     */
    public int getNumResults() {
        return ByteOrder.ubyte2int(getPayload()[0]);
    }

    private static byte[] derivePayload(int numResults) 
        throws BadPacketException {
        if ((numResults < 0) || (numResults > 255))
            throw new BadPacketException("Number of results too big: " +
                                         numResults);
        byte[] payload = new byte[1];
        byte[] bytes = new byte[2];
        ByteOrder.short2leb((short) numResults, bytes, 0);
        payload[0] = bytes[0];
        return payload;
    }

    public boolean equals(Object other) {
        if (other instanceof LimeACKVendorMessage) {
            GUID myGuid = new GUID(getGUID());
            GUID otherGuid = new GUID(((VendorMessage) other).getGUID());
            int otherResults = 
                ((LimeACKVendorMessage) other).getNumResults();
            return ((myGuid.equals(otherGuid)) && 
                    (getNumResults() == otherResults) &&
                    super.equals(other));
        }
        return false;
    }

    /** Overridden purely for stats handling.
     */
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        if (RECORD_STATS)
            SentMessageStatHandler.UDP_LIME_ACK.addMessage(this);
    }

    /** Overridden purely for stats handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }
}
