package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.io.ByteOrder;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/** In Vendor Message parlance, the "message type" of this VMP is "BEAR/12".
 *  This message contains 2 unsigned bytes that tells you how many
 *  results the sending host has for the guid of a query (the guid of this
 *  message is the same as the original query).  
 */
public final class QueryStatusResponse extends VendorMessage {

    public static final int VERSION = 1;

    /**
     * Constructs a new QueryStatusResponse with data from the network.
     */
    QueryStatusResponse(byte[] guid, byte ttl, byte hops, int version, 
                          byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_REPLY_NUMBER, version,
              payload);
        if (getVersion() > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");
        if (getPayload().length != 2)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPayload().length);
    }

    /**
     * Constructs a new QueryStatus response to be sent out.
     * @param numResults The number of results (1-65535) that you have
     *  for this query.  If you have more than 65535 just send 65535.
     *  @param replyGUID The guid of the original query/reply that you want to
     *  send reply info for.
     */
    public QueryStatusResponse(GUID replyGUID, int numResults) {
        super(F_BEAR_VENDOR_ID, F_REPLY_NUMBER, VERSION, 
              derivePayload(numResults));
        setGUID(replyGUID);
    }

    /** @return an int (1-65535) representing the amount of results that a host
     *  for a given query (as specified by the guid of this message).
     */
    public int getNumResults() {
        return ByteOrder.ushort2int(ByteOrder.leb2short(getPayload(), 0));
    }

    /**
     * The query guid that this response refers to.
     */
    public GUID getQueryGUID() {
        return new GUID(getGUID());
    }

    /**
     * Constructs the payload from the desired number of results.
     */
    private static byte[] derivePayload(int numResults) {
        if ((numResults < 0) || (numResults > 65535))
            throw new IllegalArgumentException("Number of results too big: " +
                                               numResults);
        byte[] payload = new byte[2];
        ByteOrder.short2leb((short) numResults, payload, 0);
        return payload;
    }

    /** Overridden purely for stats handling.
     */
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        SentMessageStatHandler.UDP_REPLY_NUMBER.addMessage(this);
    }

    /** Overridden purely for stats handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }

}
