package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
import java.io.*;

public class ResetTableMessage extends RouteTableMessage {
    /**
     * Creates a new ResetTableMessage from scratch.
     */
    public ResetTableMessage(byte ttl, byte tableTTL, byte tableAddressSize) {
        //Paylod length is 3--not 0--because it includes common arguments
        super(ttl, (byte)3, RouteTableMessage.RESET_VARIANT,
              tableTTL, tableAddressSize);
    }

    /**
     * Creates a new ResetTableMessage with data read from the network.  The
     * payload argument is the complete payload of the message.  The first byte
     * is guaranteed to be RESET_VARIANT, and the second and third bytes are
     * tableTTL and tableAddressSize, respectively.  These values will be passed
     * to the superclass.
     * 
     * @exception BadPacketException the remaining values in payload are not
     *  well-formed, e.g., because it's the wrong length.
     */
    protected ResetTableMessage(byte[] guid, 
                                byte ttl, 
                                byte hops,
                                byte tableTTL, 
                                byte tableAddressSize,
                                byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, payload.length, RouteTableMessage.RESET_VARIANT,
              tableTTL, tableAddressSize);
        //TODO: maybe we shouldn't enforce this.
        if (payload.length!=3)
            throw new BadPacketException("Extra arguments in reset message.");
    }

    protected void writePayloadData(OutputStream out) throws IOException {
        //Does nothing; there's no data in this message.
    }

    /** Unit test */
    public static void main(String args[]) {
        //From scratch.  Check encode/decode.
        ResetTableMessage m=new ResetTableMessage((byte)3, (byte)7,
                                                  (byte)2);
        Assert.that(m.getVariant()==ResetTableMessage.RESET_VARIANT);
        Assert.that(m.getTTL()==(byte)3);
        Assert.that(m.getTableTTL()==(byte)7);
        Assert.that(m.getTableAddressSize()==(byte)2);
        Assert.that(m.getLength()==3);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try {
            m.write(out);
            out.flush();
        } catch (IOException e) {
            Assert.that(false);
        }
        m=read(out.toByteArray());
        Assert.that(m.getVariant()==ResetTableMessage.RESET_VARIANT);
        Assert.that(m.getTTL()==(byte)3);
        Assert.that(m.getTableTTL()==(byte)7);
        Assert.that(m.getTableAddressSize()==(byte)2);
        Assert.that(m.getLength()==3);        

        //Read from bytes
        byte[] message=new byte[23+3];
        message[16]=Message.F_ROUTE_TABLE_UPDATE;            //function code
        message[19]=(byte)3;                                 //payload length
        message[23+0]=(byte)RouteTableMessage.RESET_VARIANT; //reset variant
        message[23+1]=(byte)7;                               //table TTL
        message[23+2]=(byte)2;                               //table address sz
        m=read(message);
        Assert.that(m.getVariant()==RouteTableMessage.RESET_VARIANT);
        Assert.that(m.getTableTTL()==(byte)7);
        Assert.that(m.getTableAddressSize()==(byte)2);
    }

    static ResetTableMessage read(byte[] bytes) {
        InputStream in=new ByteArrayInputStream(bytes);
        try {
            return (ResetTableMessage)Message.read(in);
        } catch (Exception e) {
            Assert.that(false, "Bad message");
            return null;  //never executed
        } 
    }
}
