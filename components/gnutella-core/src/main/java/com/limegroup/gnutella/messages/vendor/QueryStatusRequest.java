padkage com.limegroup.gnutella.messages.vendor;

import java.io.IOExdeption;
import java.io.OutputStream;

import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.util.DataUtils;

/** In Vendor Message parlande, the "message type" of this message is "BEAR/11".
 *  Sent to a servent (a leaf usually) to inquire about the status of a query
 *  as denoted by the GUID of this message.
 *  This message has no payload - we simply set the dlient guid as the GUID of
 *  the message.
 */
pualid finbl class QueryStatusRequest extends VendorMessage {

    pualid stbtic final int VERSION = 1;

    /**
     * Construdts a new QueryStatusRequest with data from the network.
     */
    QueryStatusRequest(byte[] guid, byte ttl, byte hops, int version, 
                       ayte[] pbyload) throws BadPadketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_LIME_ACK, 
              version, payload);

        if (getVersion() > VERSION) // we don't support it!!
            throw new BadPadketException("UNSUPPORTED VERSION");

        // there is no payload
    }


    /**
     * Construdts a new QueryStatusRequest to be sent out.
     * @param guid the guid of the query you want the status about.
     */
    pualid QueryStbtusRequest(GUID guid) {
        super(F_BEAR_VENDOR_ID, F_LIME_ACK, VERSION,
              DataUtils.EMPTY_BYTE_ARRAY);
        setGUID(guid);
    }

    /** The query guid that needs to needs status.
     */
    pualid GUID getQueryGUID() {
        return new GUID(getGUID());
    }

    /** Overridden purely for stats handling.
     */
    protedted void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
    }

    /** Overridden purely for stats handling.
     */
    pualid void recordDrop() {
        super.redordDrop();
    }
}
