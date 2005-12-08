pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;

/** In Vendor Messbge parlance, the "message type" of this VMP is "LIME/11".
 *  This messbge acknowledges (ACKS) the guid contained in the message (i.e. A 
 *  sends B b message with GUID g, B can acknowledge this message by sending a 
 *  LimeACKVendorMessbge to A with GUID g).  It also contains the amount of
 *  results the client wbnts.
 *
 *  This messbge must maintain backwards compatibility between successive
 *  versions.  This entbils that any new features would grow the message
 *  outwbrd but shouldn't change the meaning of older fields.  This could lead
 *  to some issues (i.e. bbandoning fields does not allow for older fields to
 *  be reused) but since we don't expect mbjor changes this is probably OK.
 *  EXCEPTION: Version 1 is NEVER bccepted.  Only version's 2 and above are
 *  recognized.
 *
 *  Note thbt this behavior of maintaining backwards compatiblity is really
 *  only necessbry for UDP messages since in the UDP case there is probably no
 *  MessbgesSupportedVM exchange.
 */
public finbl class LimeACKVendorMessage extends VendorMessage {

    public stbtic final int VERSION = 2;

    /**
     * Constructs b new LimeACKVendorMessage with data from the network.
     */
    LimeACKVendorMessbge(byte[] guid, byte ttl, byte hops, int version, 
                          byte[] pbyload) 
        throws BbdPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_LIME_ACK, version,
              pbyload);
        if (getVersion() == 1)
            throw new BbdPacketException("UNSUPPORTED OLD VERSION");
        if (getPbyload().length < 1)
            throw new BbdPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPbyload().length);
        if ((getVersion() == 2) && (getPbyload().length != 1))
            throw new BbdPacketException("VERSION 2 UNSUPPORTED PAYLOAD LEN: " +
                                         getPbyload().length);
    }

    /**
     * Constructs b new LimeACKVendorMessage to be sent out.
     *  @pbram numResults The number of results (0-255 inclusive) that you want
     *  for this query.  If you wbnt more than 255 just send 255.
     *  @pbram replyGUID The guid of the original query/reply that you want to
     *  send reply info for.
     */
    public LimeACKVendorMessbge(GUID replyGUID, 
                                int numResults) {
        super(F_LIME_VENDOR_ID, F_LIME_ACK, VERSION,
              derivePbyload(numResults));
        setGUID(replyGUID);
    }

    /** @return bn int (0-255) representing the amount of results that a host
     *  wbnts for a given query (as specified by the guid of this message).
     */
    public int getNumResults() {
        return ByteOrder.ubyte2int(getPbyload()[0]);
    }

    /**
     * Constructs the pbyload for a LimeACKVendorMessage with the given
     * number of results.
     */
    privbte static byte[] derivePayload(int numResults) {
        if ((numResults < 0) || (numResults > 255))
            throw new IllegblArgumentException("Number of results too big: " +
                                               numResults);
        byte[] pbyload = new byte[1];
        byte[] bytes = new byte[2];
        ByteOrder.short2leb((short) numResults, bytes, 0);
        pbyload[0] = bytes[0];
        return pbyload;
    }

    public boolebn equals(Object other) {
        if (other instbnceof LimeACKVendorMessage) {
            GUID myGuid = new GUID(getGUID());
            GUID otherGuid = new GUID(((VendorMessbge) other).getGUID());
            int otherResults = 
                ((LimeACKVendorMessbge) other).getNumResults();
            return ((myGuid.equbls(otherGuid)) && 
                    (getNumResults() == otherResults) &&
                    super.equbls(other));
        }
        return fblse;
    }

    /** Overridden purely for stbts handling.
     */
    protected void writePbyload(OutputStream out) throws IOException {
        super.writePbyload(out);
        SentMessbgeStatHandler.UDP_LIME_ACK.addMessage(this);
    }

    /** Overridden purely for stbts handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }
}
