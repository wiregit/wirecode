package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
import java.io.*;

/**
 *
 */
public class SetDenseTableMessage extends RouteTableMessage {
    /**
     * Creates a new SetDenseTableMessage from scratch.
     */
    public SetDenseTableMessage(byte ttl,
                                byte tableTTL,
                                byte tableAddressSize,
                                int startOffset,
                                int stopOffset,
                                BitSet bset) {
        //Paylod length is 3--not 0--because it includes common arguments
        super(ttl, (byte)3, RouteTableMessage.RESET_VARIANT,
              tableTTL, tableAddressSize);
    }

    /**
     * Creates a new SetDenseTableMessage with data read from the network.  The
     * payload argument is the complete payload of the message.  The first byte
     * is guaranteed to be RESET_VARIANT, and the second and third bytes are
     * tableTTL and tableAddressSize, respectively.  These values will be passed
     * to the superclass.
     * 
     * @exception BadPacketException the remaining values in payload are not
     *  well-formed, e.g., because it's the wrong length.
     */
    protected SetDenseTableMessage(byte[] guid, 
                                   byte ttl, 
                                   byte hops,
                                   byte tableTTL, 
                                   byte tableAddressSize,
                                   byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, payload.length, RouteTableMessage.RESET_VARIANT,
              tableTTL, tableAddressSize);

    }

    /*
     *
     */
    public BitSet getBits() {
        BitSet ret=new BitSet(..);
        copyTo(ret);
        return ret;
    }

    /**
     * Sets
     */
    public void copyTo(BitSet bset) {
        
    }

    protected void writePayloadData(OutputStream out) throws IOException {

    }
}
