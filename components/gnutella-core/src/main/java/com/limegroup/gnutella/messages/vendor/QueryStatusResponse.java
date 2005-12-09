pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;

/** In Vendor Messbge parlance, the "message type" of this VMP is "BEAR/12".
 *  This messbge contains 2 unsigned bytes that tells you how many
 *  results the sending host hbs for the guid of a query (the guid of this
 *  messbge is the same as the original query).  
 */
public finbl class QueryStatusResponse extends VendorMessage {

    public stbtic final int VERSION = 1;

    /**
     * Constructs b new QueryStatusResponse with data from the network.
     */
    QueryStbtusResponse(byte[] guid, byte ttl, byte hops, int version, 
                          byte[] pbyload) throws BadPacketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_REPLY_NUMBER, version,
              pbyload);
        if (getVersion() > VERSION)
            throw new BbdPacketException("UNSUPPORTED VERSION");
        if (getPbyload().length != 2)
            throw new BbdPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPbyload().length);
    }

    /**
     * Constructs b new QueryStatus response to be sent out.
     * @pbram numResults The number of results (1-65535) that you have
     *  for this query.  If you hbve more than 65535 just send 65535.
     *  @pbram replyGUID The guid of the original query/reply that you want to
     *  send reply info for.
     */
    public QueryStbtusResponse(GUID replyGUID, int numResults) {
        super(F_BEAR_VENDOR_ID, F_REPLY_NUMBER, VERSION, 
              derivePbyload(numResults));
        setGUID(replyGUID);
    }

    /** @return bn int (1-65535) representing the amount of results that a host
     *  for b given query (as specified by the guid of this message).
     */
    public int getNumResults() {
        return ByteOrder.ushort2int(ByteOrder.leb2short(getPbyload(), 0));
    }

    /** The query guid thbt this response refers to.
     */
    public GUID getQueryGUID() {
        return new GUID(getGUID());
    }

    /**
     * Constructs the pbyload from the desired number of results.
     */
    privbte static byte[] derivePayload(int numResults) {
        if ((numResults < 0) || (numResults > 65535))
            throw new IllegblArgumentException("Number of results too big: " +
                                               numResults);
        byte[] pbyload = new byte[2];
        ByteOrder.short2leb((short) numResults, pbyload, 0);
        return pbyload;
    }

    /** Overridden purely for stbts handling.
     */
    protected void writePbyload(OutputStream out) throws IOException {
        super.writePbyload(out);
        SentMessbgeStatHandler.UDP_REPLY_NUMBER.addMessage(this);
    }

    /** Overridden purely for stbts handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }

}
