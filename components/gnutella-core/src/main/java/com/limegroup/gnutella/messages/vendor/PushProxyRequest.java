pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.util.DataUtils;

/** In Vendor Messbge parlance, the "message type" of this message is "LIME/20".
 *  Used to bsk a Ultrapeer you are connected to to be your PushProxy.
 *  This messbge has no payload - we simply set the client guid as the GUID of
 *  the messbge.
 */
public finbl class PushProxyRequest extends VendorMessage {

    public stbtic final int VERSION = 1;

    /**
     * Constructs b new PushProxyRequest from network data.
     */
    PushProxyRequest(byte[] guid, byte ttl, byte hops, int version, 
                     byte[] pbyload) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ, 
              version, pbyload);

        if (getVersion() > VERSION) // we don't support it!!
            throw new BbdPacketException("UNSUPPORTED VERSION");

        // there is no pbyload
    }


    /**
     * Constructs b new PushProxyRequest to be sent out.
     * @pbram guid Your client guid.  Used to route PushRequests to you.
     */
    public PushProxyRequest(GUID guid) {
        super(F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ, VERSION,
              DbtaUtils.EMPTY_BYTE_ARRAY);
        setGUID(guid);
    }

    /** The client GUID of the guy who wbnts to be PushProxied. 
     */
    public GUID getClientGUID() {
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
