package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
import java.io.*;


/**
 * The RESET route table update message.
 */
public class ResetTableMessage extends RouteTableMessage {
    private int tableSize;
    private byte infinity;

    /////////////////////////////// Encoding //////////////////////////////

    /**
     * Creates a new ResetTableMessage from scratch, with TTL 1. The
     * receiver should initialize its routing table to a tableSize array
     * with values of infinity.  Throws IllegalArgumentException if
     * either value is less than 1.
     *
     * @param tableSize the size of the table
     * @param infinity the smallest value in the route table for infinity,
     *  i.e., one more than the max TTL
     * @see RouteTableMessage
     */
    public ResetTableMessage(int tableSize,
                             byte infinity) {
        //Payload length includes variant
        super((byte)1, 1+4+1, RouteTableMessage.RESET_VARIANT);
        if (tableSize<1 || infinity<1)
            throw new IllegalArgumentException("Argument too small: "
                                               +tableSize+", "+infinity);
        this.tableSize=tableSize;
        this.infinity=infinity;
    }

    protected void writePayloadData(OutputStream out) throws IOException {
        byte[] buf=new byte[5];
        ByteOrder.int2leb(tableSize, buf, 0);
        buf[4]=infinity;
        out.write(buf);   
    }

    
    /////////////////////////////// Decoding ///////////////////////////////

    /**
     * Creates a new ResetTableMessage with data read from the network.  The
     * payload argument is the complete payload of the message.  The first byte
     * is guaranteed to be RESET_VARIANT.
     * 
     * @exception BadPacketException the remaining values in payload are not
     *  well-formed, e.g., because it's the wrong length.  
     */
    protected ResetTableMessage(byte[] guid, 
                                byte ttl, 
                                byte hops,
                                byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, payload.length, 
              RouteTableMessage.RESET_VARIANT);
        //TODO: maybe we shouldn't enforce this
        //if (payload.length!=(2+4))
        //    throw new BadPacketException("Extra arguments in reset message.");
        tableSize=ByteOrder.leb2int(payload, 1);
        infinity=payload[5];
    }


    /////////////////////////////// Accessors ///////////////////////////////
    
    /** Returns the smallest value in the route table for infinity,
     *  i.e., one more than the max TTL. */
    public byte getInfinity() {
        return infinity;
    }

    /** Returns the new size of the route table. */
    public int getTableSize() {
        return tableSize;
    }

    public String toString() {
        return "{RESET, tableSize: "+getTableSize()
              +", Infinity: "+getInfinity()+"}";
    }


    /** Unit test */
    public static void main(String args[]) {
        //From scratch.  Check encode/decode.
        ResetTableMessage m=new ResetTableMessage(1024, (byte)10);
        Assert.that(m.getVariant()==ResetTableMessage.RESET_VARIANT);
        Assert.that(m.getTTL()==(byte)1);
        Assert.that(m.getTableSize()==1024);
        Assert.that(m.getInfinity()==10);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try {
            m.write(out);
            out.flush();
        } catch (IOException e) {
            Assert.that(false);
        }
        m=read(out.toByteArray());
        Assert.that(m.getVariant()==ResetTableMessage.RESET_VARIANT);
        Assert.that(m.getTTL()==(byte)1);
        Assert.that(m.getTableSize()==1024);
        Assert.that(m.getInfinity()==10);

        //Read from bytes
        byte[] message=new byte[23+6];
        message[16]=Message.F_ROUTE_TABLE_UPDATE;            //function code
        message[17]=(byte)3;                                 //TTL
        message[19]=(byte)6;                                 //payload length
        message[23+0]=(byte)RouteTableMessage.RESET_VARIANT; //reset variant
        message[23+2]=(byte)1;                               //size==256
        message[23+5]=(byte)10;                              //infinity
        m=read(message);
        Assert.that(m.getVariant()==RouteTableMessage.RESET_VARIANT);
        Assert.that(m.getInfinity()==(byte)10);
        Assert.that(m.getTableSize()==256);
    }

    static ResetTableMessage read(byte[] bytes) {
        InputStream in=new ByteArrayInputStream(bytes);
        try {
            return (ResetTableMessage)Message.read(in);
        } catch (Exception e) {
            Assert.that(false, "Bad message: "+e);
            return null;  //never executed
        } 
    }
}
