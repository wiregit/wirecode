package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.DataUtils;

/** In Vendor Message parlance, the "message type" of this message is "BEAR/11".
 *  Sent to a servent (a leaf usually) to inquire about the status of a query
 *  as denoted by the GUID of this message.
 *  This message has no payload - we simply set the client guid as the GUID of
 *  the message.
 */
public final class QueryStatusRequest extends VendorMessage {

    public static final int VERSION = 1;

    /**
     * Constructs a new QueryStatusRequest with data from the network.
     */
    QueryStatusRequest(byte[] guid, byte ttl, byte hops, int version, 
                       byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_LIME_ACK, 
              version, payload);

        if (getVersion() > VERSION) // we don't support it!!
            throw new BadPacketException("UNSUPPORTED VERSION");

        // there is no payload
    }


    /**
     * Constructs a new QueryStatusRequest to be sent out.
     * @param guid the guid of the query you want the status about.
     */
    public QueryStatusRequest(GUID guid) {
        super(F_BEAR_VENDOR_ID, F_LIME_ACK, VERSION,
              DataUtils.EMPTY_BYTE_ARRAY);
        setGUID(guid);
    }

    /** The query guid that needs to needs status.
     */
    public GUID getQueryGUID() {
        return new GUID(getGUID());
    }

    /** Overridden purely for stats handling.
     */
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
    }

    /** Overridden purely for stats handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }
}
