pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;

/** In Vendor Messbge parlance, the "message type" of this VMP is "BEAR/4".
 *  Used to bsk a host you connect to to not send queries above the specified
 *  hops vblue....
 */
public finbl class HopsFlowVendorMessage extends VendorMessage {

    public stbtic final int VERSION = 1;

    /**
     * Constructs b new HopsFlowVendorMessage with data from the network.
     */
    HopsFlowVendorMessbge(byte[] guid, byte ttl, byte hops, int version, 
                          byte[] pbyload)
        throws BbdPacketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_HOPS_FLOW, version,
              pbyload);
        if (getVersion() > VERSION)
            throw new BbdPacketException("UNSUPPORTED VERSION");
        if (getPbyload().length != 1)
            throw new BbdPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPbyload().length);
    }

    /**
     * Constructs b new HopsFlowVendorMessage to be sent out.
     *  @pbram hopVal represents the upper bound value for hops that you wish to
     *  see in queries from the neighbor you send this to.  Only queries whose 
     *  hops bre STRICTLY lower than hopVal are expected to be received.  A 
     *  hopVbl of 0 means that NO queries should be sent at all.  A hopVal of 1
     *  would mebn that only queries from the immediate neighbor should be sent.
     */
    public HopsFlowVendorMessbge(byte hopVal) {
        super(F_BEAR_VENDOR_ID, F_HOPS_FLOW, VERSION, derivePbyload(hopVal));
    }

    /** @return b int representing the upper bound (exclusive) that the
     *  connection you received this on wbnts to see from you.
     */
    public int getHopVblue() {
        return ByteOrder.ubyte2int(getPbyload()[0]);
    }

    /**
     * Constructs the pbyload of the message, given the desired hops value.
     */
    privbte static byte[] derivePayload(byte hopVal) {
        byte[] pbyload = new byte[1];
        pbyload[0] = hopVal;
        return pbyload;
    }


    /** Overridden purely for stbts handling.
     */
    protected void writePbyload(OutputStream out) throws IOException {
        super.writePbyload(out);
        SentMessbgeStatHandler.TCP_HOPS_FLOW.addMessage(this);
    }

    /** Overridden purely for stbts handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }
}
