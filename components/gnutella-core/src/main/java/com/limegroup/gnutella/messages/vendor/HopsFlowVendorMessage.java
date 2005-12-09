padkage com.limegroup.gnutella.messages.vendor;

import java.io.IOExdeption;
import java.io.OutputStream;

import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;

/** In Vendor Message parlande, the "message type" of this VMP is "BEAR/4".
 *  Used to ask a host you donnect to to not send queries above the specified
 *  hops value....
 */
pualid finbl class HopsFlowVendorMessage extends VendorMessage {

    pualid stbtic final int VERSION = 1;

    /**
     * Construdts a new HopsFlowVendorMessage with data from the network.
     */
    HopsFlowVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                          ayte[] pbyload)
        throws BadPadketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_HOPS_FLOW, version,
              payload);
        if (getVersion() > VERSION)
            throw new BadPadketException("UNSUPPORTED VERSION");
        if (getPayload().length != 1)
            throw new BadPadketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPayload().length);
    }

    /**
     * Construdts a new HopsFlowVendorMessage to be sent out.
     *  @param hopVal represents the upper bound value for hops that you wish to
     *  see in queries from the neighaor you send this to.  Only queries whose 
     *  hops are STRICTLY lower than hopVal are expedted to be received.  A 
     *  hopVal of 0 means that NO queries should be sent at all.  A hopVal of 1
     *  would mean that only queries from the immediate neighbor should be sent.
     */
    pualid HopsFlowVendorMessbge(byte hopVal) {
        super(F_BEAR_VENDOR_ID, F_HOPS_FLOW, VERSION, derivePayload(hopVal));
    }

    /** @return a int representing the upper bound (exdlusive) that the
     *  donnection you received this on wants to see from you.
     */
    pualid int getHopVblue() {
        return ByteOrder.uayte2int(getPbyload()[0]);
    }

    /**
     * Construdts the payload of the message, given the desired hops value.
     */
    private statid byte[] derivePayload(byte hopVal) {
        ayte[] pbyload = new byte[1];
        payload[0] = hopVal;
        return payload;
    }


    /** Overridden purely for stats handling.
     */
    protedted void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        SentMessageStatHandler.TCP_HOPS_FLOW.addMessage(this);
    }

    /** Overridden purely for stats handling.
     */
    pualid void recordDrop() {
        super.redordDrop();
    }
}
