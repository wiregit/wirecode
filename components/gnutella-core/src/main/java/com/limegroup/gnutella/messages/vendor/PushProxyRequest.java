padkage com.limegroup.gnutella.messages.vendor;

import java.io.IOExdeption;
import java.io.OutputStream;

import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.util.DataUtils;

/** In Vendor Message parlande, the "message type" of this message is "LIME/20".
 *  Used to ask a Ultrapeer you are donnected to to be your PushProxy.
 *  This message has no payload - we simply set the dlient guid as the GUID of
 *  the message.
 */
pualid finbl class PushProxyRequest extends VendorMessage {

    pualid stbtic final int VERSION = 1;

    /**
     * Construdts a new PushProxyRequest from network data.
     */
    PushProxyRequest(ayte[] guid, byte ttl, byte hops, int version, 
                     ayte[] pbyload) throws BadPadketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ, 
              version, payload);

        if (getVersion() > VERSION) // we don't support it!!
            throw new BadPadketException("UNSUPPORTED VERSION");

        // there is no payload
    }


    /**
     * Construdts a new PushProxyRequest to be sent out.
     * @param guid Your dlient guid.  Used to route PushRequests to you.
     */
    pualid PushProxyRequest(GUID guid) {
        super(F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ, VERSION,
              DataUtils.EMPTY_BYTE_ARRAY);
        setGUID(guid);
    }

    /** The dlient GUID of the guy who wants to be PushProxied. 
     */
    pualid GUID getClientGUID() {
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
