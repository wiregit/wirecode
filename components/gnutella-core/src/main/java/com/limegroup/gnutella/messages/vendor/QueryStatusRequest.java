package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.statistics.*;
import java.io.*;

/** In Vendor Message parlance, the "message type" of this message is "BEAR/11".
 *  Sent to a servent (a leaf usually) to inquire about the status of a query
 *  as denoted by the GUID of this message.
 *  This message has no payload - we simply set the client guid as the GUID of
 *  the message.
 */
public final class QueryStatusRequest extends VendorMessage {

    public static final int VERSION = 1;

    /** The network constructor. */
    QueryStatusRequest(byte[] guid, byte ttl, byte hops, int version, 
                       byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_LIME_ACK, 
              version, payload);

        if (getVersion() > VERSION) // we don't support it!!
            throw new BadPacketException("UNSUPPORTED VERSION");

        // there is no payload
    }


    /** @param guid the guid of the query you want the status about.
     */
    public QueryStatusRequest(GUID guid) throws BadPacketException {
        super(F_BEAR_VENDOR_ID, F_LIME_ACK, VERSION, new byte[0]);
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
        if (RECORD_STATS)
            ;
    }

    /** Overridden purely for stats handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }
}
