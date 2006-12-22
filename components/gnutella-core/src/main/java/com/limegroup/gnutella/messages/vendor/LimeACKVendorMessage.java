package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.io.ByteOrder;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/** In Vendor Message parlance, the "message type" of this VMP is "LIME/11".
 *  This message acknowledges (ACKS) the guid contained in the message (i.e. A 
 *  sends B a message with GUID g, B can acknowledge this message by sending a 
 *  LimeACKVendorMessage to A with GUID g).  It also contains the amount of
 *  results the client wants.
 *
 *  This message must maintain backwards compatibility between successive
 *  versions.  This entails that any new features would grow the message
 *  outward but shouldn't change the meaning of older fields.  This could lead
 *  to some issues (i.e. abandoning fields does not allow for older fields to
 *  be reused) but since we don't expect major changes this is probably OK.
 *  EXCEPTION: Version 1 is NEVER accepted.  Only version's 2 and above are
 *  recognized.
 *
 *  Note that this behavior of maintaining backwards compatiblity is really
 *  only necessary for UDP messages since in the UDP case there is probably no
 *  MessagesSupportedVM exchange.
 */
public final class LimeACKVendorMessage extends VendorMessage {

    public static final int VERSION = 2;

    /**
     * Constructs a new LimeACKVendorMessage with data from the network.
     */
    LimeACKVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                          byte[] payload) 
        throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_LIME_ACK, version,
              payload);
        if (getVersion() == 1)
            throw new BadPacketException("UNSUPPORTED OLD VERSION");
        if (getPayload().length < 1)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPayload().length);
        if ((getVersion() == 2) && (getPayload().length != 1))
            throw new BadPacketException("VERSION 2 UNSUPPORTED PAYLOAD LEN: " +
                                         getPayload().length);
    }

    /**
     * Constructs a new LimeACKVendorMessage to be sent out.
     *  @param numResults The number of results (0-255 inclusive) that you want
     *  for this query.  If you want more than 255 just send 255.
     *  @param replyGUID The guid of the original query/reply that you want to
     *  send reply info for.
     */
    public LimeACKVendorMessage(GUID replyGUID, 
                                int numResults) {
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

    /**
     * Constructs the payload for a LimeACKVendorMessage with the given
     * number of results.
     */
    private static byte[] derivePayload(int numResults) {
        if ((numResults < 0) || (numResults > 255))
            throw new IllegalArgumentException("Number of results too big: " +
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
        SentMessageStatHandler.UDP_LIME_ACK.addMessage(this);
    }

    /** Overridden purely for stats handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }
}
