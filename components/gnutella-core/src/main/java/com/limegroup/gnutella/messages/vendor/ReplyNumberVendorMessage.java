package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/** In Vendor Message parlance, the "message type" of this VMP is "LIME/12".
 *  This message contains a unsigned byte (1-255) that tells you how many
 *  results the sending host has for the guid of a query (the guid of this
 *  message is the same as the original query).  The recieving host can ACK
 *  this message with a LimeACKVendorMessage to actually recieve the replies.
 *
 *  This message must maintain backwards compatibility between successive
 *  versions.  This entails that any new features would grow the message
 *  outward but shouldn't change the meaning of older fields.  This could lead
 *  to some issues (i.e. abandoning fields does not allow for older fields to
 *  be reused) but since we don't expect major changes this is probably OK.
 *
 *  Note that this behavior of maintaining backwards compatiblity is really
 *  only necessary for UDP messages since in the UDP case there is probably no
 *  MessagesSupportedVM exchange.
 */
public final class ReplyNumberVendorMessage extends VendorMessage {


    public static final int OLD_VERSION = 2;
    public static final int VERSION = 3;
    
    /**
     * whether we can receive unsolicited udp
     */
    private static final byte UNSOLICITED=0x1;

    /**
     * Constructs a new ReplyNumberVendorMessages with data from the network.
     */
    ReplyNumberVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                          byte[] payload) 
        throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_REPLY_NUMBER, version,
              payload);
        
        if (version < OLD_VERSION)
            throw new BadPacketException("ancient version");
        
        // only allow current version to come from network
        // unless OOBv2 is allowed
        if (version < VERSION && SearchSettings.DISABLE_OOB_V2.getValue())
            throw new BadPacketException("OOB v2 not allowed");
        
        // loosen the condition on the message size to allow this message version
        // to have a GGEP in the future
        if (getPayload().length < 2)
            throw new BadPacketException("VERSION " + version+" UNSUPPORTED PAYLOAD LEN: " +
                    getPayload().length);
    }

    /**
     * Constructs a new ReplyNumberVendorMessage to be sent out.
     *  @param numResults The number of results (1-255 inclusive) that you have
     *  for this query.  If you have more than 255 just send 255.
     *  @param replyGUID The guid of the original query/reply that you want to
     *  send reply info for.
     */
    ReplyNumberVendorMessage(GUID replyGUID, int version, int numResults) {
        super(F_LIME_VENDOR_ID, F_REPLY_NUMBER, version,
              derivePayload(numResults));
        setGUID(replyGUID);
    }
    
    /**
     * Constructs a new ReplyNumberVendorMessage with the current version
     * number.
     */
    public ReplyNumberVendorMessage(GUID replyGUID, int numResults) {
        this(replyGUID, VERSION, numResults);
    }
    
    public static ReplyNumberVendorMessage createV2ReplyNumberVendorMessage(GUID replyGUID, int numResults) {
        return new ReplyNumberVendorMessage(replyGUID, OLD_VERSION, numResults);
    }
    
    public static ReplyNumberVendorMessage  createV3ReplyNumberVendorMessage(GUID replyGUID, int numResults) {
        return new ReplyNumberVendorMessage(replyGUID, VERSION, numResults);
    }

    /** @return an int (1-255) representing the amount of results that a host
     *  for a given query (as specified by the guid of this message).
     */
    public int getNumResults() {
        return ByteOrder.ubyte2int(getPayload()[0]);
    }
    
    public boolean canReceiveUnsolicited() {
        if (getVersion() ==1) 
            return true;
        else 
            return (getPayload()[1] & UNSOLICITED) == UNSOLICITED;
    }

    /**
     * Constructs the payload from the desired number of results.
     */
    private static byte[] derivePayload(int numResults) {
        if ((numResults < 1) || (numResults > 255))
            throw new IllegalArgumentException("Number of results too big: " +
                                               numResults);
        byte[] bytes = new byte[2];
        ByteOrder.short2leb((short) numResults, bytes, 0);
        bytes[1] = RouterService.canReceiveUnsolicited() ? UNSOLICITED : 0x0;
        
        return bytes;
    }

    public boolean equals(Object other) {
        if (other instanceof ReplyNumberVendorMessage) {
            GUID myGuid = new GUID(getGUID());
            GUID otherGuid = new GUID(((VendorMessage) other).getGUID());
            int otherResults = 
                ((ReplyNumberVendorMessage) other).getNumResults();
            return ((myGuid.equals(otherGuid)) && 
                    (getNumResults() == otherResults) &&
                    super.equals(other));
        }
        return false;
    }

    /** Overridden purely for stats handling.
     */
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        SentMessageStatHandler.UDP_REPLY_NUMBER.addMessage(this);
    }

    /** Overridden purely for stats handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }
    
    public boolean isOOBv3() {
        return getVersion() == VERSION;
    }

}
