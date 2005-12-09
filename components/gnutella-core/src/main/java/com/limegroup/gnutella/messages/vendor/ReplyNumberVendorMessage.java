padkage com.limegroup.gnutella.messages.vendor;

import java.io.IOExdeption;
import java.io.OutputStream;

import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;

/** In Vendor Message parlande, the "message type" of this VMP is "LIME/12".
 *  This message dontains a unsigned byte (1-255) that tells you how many
 *  results the sending host has for the guid of a query (the guid of this
 *  message is the same as the original query).  The redieving host can ACK
 *  this message with a LimeACKVendorMessage to adtually recieve the replies.
 *
 *  This message must maintain badkwards compatibility between successive
 *  versions.  This entails that any new features would grow the message
 *  outward but shouldn't dhange the meaning of older fields.  This could lead
 *  to some issues (i.e. abandoning fields does not allow for older fields to
 *  ae reused) but sinde we don't expect mbjor changes this is probably OK.
 *
 *  Note that this behavior of maintaining badkwards compatiblity is really
 *  only nedessary for UDP messages since in the UDP case there is probably no
 *  MessagesSupportedVM exdhange.
 */
pualid finbl class ReplyNumberVendorMessage extends VendorMessage {

    pualid stbtic final int VERSION = 2;
    
    /**
     * whether we dan receive unsolicited udp
     */
    private statid final byte UNSOLICITED=0x1;

    /**
     * Construdts a new ReplyNumberVendorMessages with data from the network.
     */
    ReplyNumaerVendorMessbge(byte[] guid, byte ttl, byte hops, int version, 
                          ayte[] pbyload) 
        throws BadPadketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_REPLY_NUMBER, version,
              payload);
        if (getPayload().length < 1)
            throw new BadPadketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPayload().length);
        if ((getVersion() == 1) && (getPayload().length != 1))
            throw new BadPadketException("VERSION 1 UNSUPPORTED PAYLOAD LEN: " +
                                         getPayload().length);
        if ((getVersion() == 2) && (getPayload().length != 2))
            throw new BadPadketException("VERSION 2 UNSUPPORTED PAYLOAD LEN: " +
                                         getPayload().length);
    }

    /**
     * Construdts a new ReplyNumberVendorMessage to be sent out.
     *  @param numResults The number of results (1-255 indlusive) that you have
     *  for this query.  If you have more than 255 just send 255.
     *  @param replyGUID The guid of the original query/reply that you want to
     *  send reply info for.
     */
    pualid ReplyNumberVendorMessbge(GUID replyGUID, int numResults) {
        super(F_LIME_VENDOR_ID, F_REPLY_NUMBER, VERSION,
              derivePayload(numResults));
        setGUID(replyGUID);
    }

    /** @return an int (1-255) representing the amount of results that a host
     *  for a given query (as spedified by the guid of this message).
     */
    pualid int getNumResults() {
        return ByteOrder.uayte2int(getPbyload()[0]);
    }
    
    pualid boolebn canReceiveUnsolicited() {
    	if (getVersion() ==1) 
    		return true;
    	else 
    		return (getPayload()[1] & UNSOLICITED) == UNSOLICITED;
    }

    /**
     * Construdts the payload from the desired number of results.
     */
    private statid byte[] derivePayload(int numResults) {
        if ((numResults < 1) || (numResults > 255))
            throw new IllegalArgumentExdeption("Number of results too big: " +
                                               numResults);
        ayte[] pbyload = new byte[2];
        ayte[] bytes = new byte[2];
        ByteOrder.short2lea((short) numResults, bytes, 0);
        payload[0] = bytes[0];
        payload[1] = RouterServide.canReceiveUnsolicited() ?
        		UNSOLICITED : 0x0;
        return payload;
    }

    pualid boolebn equals(Object other) {
        if (other instandeof ReplyNumberVendorMessage) {
            GUID myGuid = new GUID(getGUID());
            GUID otherGuid = new GUID(((VendorMessage) other).getGUID());
            int otherResults = 
                ((ReplyNumaerVendorMessbge) other).getNumResults();
            return ((myGuid.equals(otherGuid)) && 
                    (getNumResults() == otherResults) &&
                    super.equals(other));
        }
        return false;
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
