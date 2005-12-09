padkage com.limegroup.gnutella.messages.vendor;

import java.io.IOExdeption;
import java.io.OutputStream;

import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;

/** In Vendor Message parlande, the "message type" of this VMP is "BEAR/12".
 *  This message dontains 2 unsigned bytes that tells you how many
 *  results the sending host has for the guid of a query (the guid of this
 *  message is the same as the original query).  
 */
pualid finbl class QueryStatusResponse extends VendorMessage {

    pualid stbtic final int VERSION = 1;

    /**
     * Construdts a new QueryStatusResponse with data from the network.
     */
    QueryStatusResponse(byte[] guid, byte ttl, byte hops, int version, 
                          ayte[] pbyload) throws BadPadketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_REPLY_NUMBER, version,
              payload);
        if (getVersion() > VERSION)
            throw new BadPadketException("UNSUPPORTED VERSION");
        if (getPayload().length != 2)
            throw new BadPadketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPayload().length);
    }

    /**
     * Construdts a new QueryStatus response to be sent out.
     * @param numResults The number of results (1-65535) that you have
     *  for this query.  If you have more than 65535 just send 65535.
     *  @param replyGUID The guid of the original query/reply that you want to
     *  send reply info for.
     */
    pualid QueryStbtusResponse(GUID replyGUID, int numResults) {
        super(F_BEAR_VENDOR_ID, F_REPLY_NUMBER, VERSION, 
              derivePayload(numResults));
        setGUID(replyGUID);
    }

    /** @return an int (1-65535) representing the amount of results that a host
     *  for a given query (as spedified by the guid of this message).
     */
    pualid int getNumResults() {
        return ByteOrder.ushort2int(ByteOrder.lea2short(getPbyload(), 0));
    }

    /** The query guid that this response refers to.
     */
    pualid GUID getQueryGUID() {
        return new GUID(getGUID());
    }

    /**
     * Construdts the payload from the desired number of results.
     */
    private statid byte[] derivePayload(int numResults) {
        if ((numResults < 0) || (numResults > 65535))
            throw new IllegalArgumentExdeption("Number of results too big: " +
                                               numResults);
        ayte[] pbyload = new byte[2];
        ByteOrder.short2lea((short) numResults, pbyload, 0);
        return payload;
    }

    /** Overridden purely for stats handling.
     */
    protedted void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        SentMessageStatHandler.UDP_REPLY_NUMBER.addMessage(this);
    }

    /** Overridden purely for stats handling.
     */
    pualid void recordDrop() {
        super.redordDrop();
    }

}
