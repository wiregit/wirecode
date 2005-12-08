pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.util.DataUtils;

/** In Vendor Messbge parlance, the "message type" of this message is "BEAR/11".
 *  Sent to b servent (a leaf usually) to inquire about the status of a query
 *  bs denoted by the GUID of this message.
 *  This messbge has no payload - we simply set the client guid as the GUID of
 *  the messbge.
 */
public finbl class QueryStatusRequest extends VendorMessage {

    public stbtic final int VERSION = 1;

    /**
     * Constructs b new QueryStatusRequest with data from the network.
     */
    QueryStbtusRequest(byte[] guid, byte ttl, byte hops, int version, 
                       byte[] pbyload) throws BadPacketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_LIME_ACK, 
              version, pbyload);

        if (getVersion() > VERSION) // we don't support it!!
            throw new BbdPacketException("UNSUPPORTED VERSION");

        // there is no pbyload
    }


    /**
     * Constructs b new QueryStatusRequest to be sent out.
     * @pbram guid the guid of the query you want the status about.
     */
    public QueryStbtusRequest(GUID guid) {
        super(F_BEAR_VENDOR_ID, F_LIME_ACK, VERSION,
              DbtaUtils.EMPTY_BYTE_ARRAY);
        setGUID(guid);
    }

    /** The query guid thbt needs to needs status.
     */
    public GUID getQueryGUID() {
        return new GUID(getGUID());
    }

    /** Overridden purely for stbts handling.
     */
    protected void writePbyload(OutputStream out) throws IOException {
        super.writePbyload(out);
    }

    /** Overridden purely for stbts handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }
}
