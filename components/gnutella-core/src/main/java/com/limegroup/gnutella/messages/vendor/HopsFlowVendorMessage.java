package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/** In Vendor Message parlance, the "message type" of this VMP is "BEAR/4".
 *  Used to ask a host you connect to to not send queries above the specified
 *  hops value....
 */
public final class HopsFlowVendorMessage extends VendorMessage {

    public static final int VERSION = 1;

    /**
     * Constructs a new HopsFlowVendorMessage with data from the network.
     */
    HopsFlowVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                          byte[] payload)
        throws BadPacketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_HOPS_FLOW, version,
              payload);
        if (getVersion() > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");
        if (getPayload().length != 1)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPayload().length);
    }

    /**
     * Constructs a new HopsFlowVendorMessage to be sent out.
     *  @param hopVal represents the upper bound value for hops that you wish to
     *  see in queries from the neighbor you send this to.  Only queries whose 
     *  hops are STRICTLY lower than hopVal are expected to be received.  A 
     *  hopVal of 0 means that NO queries should be sent at all.  A hopVal of 1
     *  would mean that only queries from the immediate neighbor should be sent.
     */
    public HopsFlowVendorMessage(byte hopVal) {
        super(F_BEAR_VENDOR_ID, F_HOPS_FLOW, VERSION, derivePayload(hopVal));
    }

    /** @return a int representing the upper bound (exclusive) that the
     *  connection you received this on wants to see from you.
     */
    public int getHopValue() {
        return ByteOrder.ubyte2int(getPayload()[0]);
    }

    /**
     * Constructs the payload of the message, given the desired hops value.
     */
    private static byte[] derivePayload(byte hopVal) {
        byte[] payload = new byte[1];
        payload[0] = hopVal;
        return payload;
    }


    /** Overridden purely for stats handling.
     */
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        SentMessageStatHandler.TCP_HOPS_FLOW.addMessage(this);
    }

    /** Overridden purely for stats handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }
}
