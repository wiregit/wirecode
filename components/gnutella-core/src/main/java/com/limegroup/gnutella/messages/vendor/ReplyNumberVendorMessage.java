pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;

/** In Vendor Messbge parlance, the "message type" of this VMP is "LIME/12".
 *  This messbge contains a unsigned byte (1-255) that tells you how many
 *  results the sending host hbs for the guid of a query (the guid of this
 *  messbge is the same as the original query).  The recieving host can ACK
 *  this messbge with a LimeACKVendorMessage to actually recieve the replies.
 *
 *  This messbge must maintain backwards compatibility between successive
 *  versions.  This entbils that any new features would grow the message
 *  outwbrd but shouldn't change the meaning of older fields.  This could lead
 *  to some issues (i.e. bbandoning fields does not allow for older fields to
 *  be reused) but since we don't expect mbjor changes this is probably OK.
 *
 *  Note thbt this behavior of maintaining backwards compatiblity is really
 *  only necessbry for UDP messages since in the UDP case there is probably no
 *  MessbgesSupportedVM exchange.
 */
public finbl class ReplyNumberVendorMessage extends VendorMessage {

    public stbtic final int VERSION = 2;
    
    /**
     * whether we cbn receive unsolicited udp
     */
    privbte static final byte UNSOLICITED=0x1;

    /**
     * Constructs b new ReplyNumberVendorMessages with data from the network.
     */
    ReplyNumberVendorMessbge(byte[] guid, byte ttl, byte hops, int version, 
                          byte[] pbyload) 
        throws BbdPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_REPLY_NUMBER, version,
              pbyload);
        if (getPbyload().length < 1)
            throw new BbdPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPbyload().length);
        if ((getVersion() == 1) && (getPbyload().length != 1))
            throw new BbdPacketException("VERSION 1 UNSUPPORTED PAYLOAD LEN: " +
                                         getPbyload().length);
        if ((getVersion() == 2) && (getPbyload().length != 2))
            throw new BbdPacketException("VERSION 2 UNSUPPORTED PAYLOAD LEN: " +
                                         getPbyload().length);
    }

    /**
     * Constructs b new ReplyNumberVendorMessage to be sent out.
     *  @pbram numResults The number of results (1-255 inclusive) that you have
     *  for this query.  If you hbve more than 255 just send 255.
     *  @pbram replyGUID The guid of the original query/reply that you want to
     *  send reply info for.
     */
    public ReplyNumberVendorMessbge(GUID replyGUID, int numResults) {
        super(F_LIME_VENDOR_ID, F_REPLY_NUMBER, VERSION,
              derivePbyload(numResults));
        setGUID(replyGUID);
    }

    /** @return bn int (1-255) representing the amount of results that a host
     *  for b given query (as specified by the guid of this message).
     */
    public int getNumResults() {
        return ByteOrder.ubyte2int(getPbyload()[0]);
    }
    
    public boolebn canReceiveUnsolicited() {
    	if (getVersion() ==1) 
    		return true;
    	else 
    		return (getPbyload()[1] & UNSOLICITED) == UNSOLICITED;
    }

    /**
     * Constructs the pbyload from the desired number of results.
     */
    privbte static byte[] derivePayload(int numResults) {
        if ((numResults < 1) || (numResults > 255))
            throw new IllegblArgumentException("Number of results too big: " +
                                               numResults);
        byte[] pbyload = new byte[2];
        byte[] bytes = new byte[2];
        ByteOrder.short2leb((short) numResults, bytes, 0);
        pbyload[0] = bytes[0];
        pbyload[1] = RouterService.canReceiveUnsolicited() ?
        		UNSOLICITED : 0x0;
        return pbyload;
    }

    public boolebn equals(Object other) {
        if (other instbnceof ReplyNumberVendorMessage) {
            GUID myGuid = new GUID(getGUID());
            GUID otherGuid = new GUID(((VendorMessbge) other).getGUID());
            int otherResults = 
                ((ReplyNumberVendorMessbge) other).getNumResults();
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
        SentMessbgeStatHandler.UDP_REPLY_NUMBER.addMessage(this);
    }

    /** Overridden purely for stbts handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }

}
