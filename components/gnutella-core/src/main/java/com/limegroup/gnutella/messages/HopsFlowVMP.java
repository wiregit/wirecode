package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.ByteOrder;
import java.io.*;

/** In Vendor Message parlance, the "message type" of this VMP is "BEAR/7".
 *  Used to ask a host you connect to do a TCP ConnectBack.
 */
public final class HopsFlowVMP extends VendorMessagePayload {

    public static final int VERSION = 1;

    private byte[] _payload = null;

    HopsFlowVMP(int version, byte[] payload) throws BadPacketException {
        super(F_BEAR_VENDOR_ID, F_HOPS_FLOW, version);
        if (version > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");
        if (payload.length != 1)
            throw new BadPacketException();
        // get the port from the payload....
        _payload = payload;
    }

    /** @param hopVal represents the upper bound value for hops that you wish to
     *  see in queries from the neighbor you send this to.  Only queries whose 
     *  hops are STRICTLY lower than hopVal are expected to be received.  A 
     *  hopVal of 0 means that NO queries should be sent at all.  A hopVal of 1
     *  would mean that only queries from the immediate neighbor should be sent.
     */
    public HopsFlowVMP(byte hopVal) {
        super(F_BEAR_VENDOR_ID, F_HOPS_FLOW, VERSION);
        _payload = new byte[1];
        _payload[0] = hopVal;
    }

    /** @return a byte reprsenting the upper bound (exclusive) that the
     *  connection you received this on wants to see from you.
     */
    public byte getHopValue() {
        return _payload[0];
    }

    protected byte[] getPayload() {
        return _payload;
    }

}
