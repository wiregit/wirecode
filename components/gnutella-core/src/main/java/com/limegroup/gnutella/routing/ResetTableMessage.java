package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
import java.io.*;


/**
 * The RESET route table update message.
 */
public class ResetTableMessage extends RouteTableMessage {
    private int tableSize;

    /////////////////////////////// Encoding //////////////////////////////

    /**
     * Creates a new ResetTableMessage from scratch.  The receiver
     * should reset its tables for TTL tableTTL and higher.
     *
     * @param tableSize the size of the table in <b>bits</b>
     *  REQUIRES: tableSize>1.
     * @see RouteTableMessage
     */
    public ResetTableMessage(byte ttl,
                             byte tableTTL,
                             int tableSize) {
        //Payload length includes common arguments
        super(ttl, 2+4, RouteTableMessage.RESET_VARIANT, tableTTL);
        this.tableSize=tableSize;
    }

    protected void writePayloadData(OutputStream out) throws IOException {
        byte[] buf=new byte[4];
        ByteOrder.int2leb(tableSize, buf, 0);
        out.write(buf);   
    }

    
    /////////////////////////////// Decoding ///////////////////////////////

    /**
     * Creates a new ResetTableMessage with data read from the network.  The
     * payload argument is the complete payload of the message.  The first byte
     * is guaranteed to be RESET_VARIANT, and the second byte is tableTTL.
     * These values will be passed to the superclass.
     * 
     * @exception BadPacketException the remaining values in payload are not
     *  well-formed, e.g., because it's the wrong length.  
     */
    protected ResetTableMessage(byte[] guid, 
                                byte ttl, 
                                byte hops,
                                byte tableTTL,
                                byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, payload.length, 
              RouteTableMessage.RESET_VARIANT, tableTTL);
        //TODO: maybe we shouldn't enforce this
        if (payload.length!=(2+4))
            throw new BadPacketException("Extra arguments in reset message.");
        tableSize=ByteOrder.leb2int(payload, 2);
        if (tableSize<0)
            throw new BadPacketException("Table too big.");                 
    }


    /////////////////////////////// Accessors ///////////////////////////////

    /** Returns the new size of the route table. */
    public int getTableSize() {
        return tableSize;
    }

    public String toString() {
        return "{RESET, tableSize: "+getTableSize()
              +", TTTL: "+getTableTTL()+"}";
    }


    /** Unit test */
    public static void main(String args[]) {
        //From scratch.  Check encode/decode.
        ResetTableMessage m=new ResetTableMessage((byte)3, (byte)7, 1024);
        Assert.that(m.getVariant()==ResetTableMessage.RESET_VARIANT);
        Assert.that(m.getTTL()==(byte)3);
        Assert.that(m.getTableTTL()==(byte)7);
        Assert.that(m.getTableSize()==1024);
        Assert.that(m.getLength()==6);
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
        Assert.that(m.getTableSize()==1024);
        Assert.that(m.getLength()==6);        

        //Read from bytes
        byte[] message=new byte[23+6];
        message[16]=Message.F_ROUTE_TABLE_UPDATE;            //function code
        message[17]=(byte)3;                                 //TTL
        message[19]=(byte)6;                                 //payload length
        message[23+0]=(byte)RouteTableMessage.RESET_VARIANT; //reset variant
        message[23+1]=(byte)7;                               //table TTL
        message[23+3]=(byte)1;                               //size==256
        m=read(message);
        Assert.that(m.getVariant()==RouteTableMessage.RESET_VARIANT);
        Assert.that(m.getTableTTL()==(byte)7);
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
