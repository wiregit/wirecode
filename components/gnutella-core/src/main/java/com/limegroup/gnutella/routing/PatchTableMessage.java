package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.Arrays;
import java.io.*;


/**
 * The PATCH route table update message.
 */
public class PatchTableMessage extends RouteTableMessage {
    /** For sequenceNumber and size, we really do need values of 0-255.
     *  Java bytes are signed, of course, so we store shorts internally
     *  and convert to bytes when writing. */
    private short sequenceNumber;
    private short sequenceSize;
    private byte compressor;
    private byte entryBits;
    //TODO: I think storing payload here would be more efficient
    private byte[] data;

    public static final byte COMPRESSOR_NONE=0x0;
    public static final byte COMPRESSOR_GZIP=0x1;
    

    /////////////////////////////// Encoding //////////////////////////////

    /**
     * Creates a new PATCH variant from scratch, with TTL 1.  The patch data is
     * copied from dataSrc[datSrcStart...dataSrcStop-1], inclusive.  
     * 
     * @requires sequenceNumber and sequenceSize can fit in one unsigned byte
     *           compressor one of COMPRESSOR_NONE or COMPRESSOR_GZIP
     *           entryBits less than 1
     *           dataSrcStart>dataSrcStop
     *           dataSrcStart or dataSrcStop not valid indices fof dataSrc
     * @see RouteTableMessage 
     */
    public PatchTableMessage(short sequenceNumber,
                             short sequenceSize,
                             byte compressor,
                             byte entryBits,
                             byte[] dataSrc,
                             int dataSrcStart,
                             int dataSrcStop) {
        //Payload length INCLUDES variant
        super((byte)1,
              5+(dataSrcStop-dataSrcStart), 
              RouteTableMessage.PATCH_VARIANT);
        this.sequenceNumber=sequenceNumber;
        this.sequenceSize=sequenceSize;
        this.compressor=compressor;
        this.entryBits=entryBits;
        //Copy dataSrc[dataSrcStart...dataSrcStop-1] to data
        data=new byte[dataSrcStop-dataSrcStart];       //TODO3: avoid
        System.arraycopy(dataSrc, dataSrcStart, data, 0, data.length);
    }

    protected void writePayloadData(OutputStream out) throws IOException {
        //Does NOT include variant
        byte[] buf=new byte[4+data.length];
        buf[0]=(byte)sequenceNumber;
        buf[1]=(byte)sequenceSize;
        buf[2]=(byte)compressor;
        buf[3]=(byte)entryBits;
        System.arraycopy(data, 0, buf, 4, data.length); //TODO3: avoid
        out.write(buf);   
    }

    
    /////////////////////////////// Decoding ///////////////////////////////

    /**
     * Creates a new PATCH variant with data read from the network.  
     * The first byte is guaranteed to be PATCH_VARIANT.
     * 
     * @exception BadPacketException the remaining values in payload are not
     *  well-formed, e.g., because it's the wrong length.  
     */
    protected PatchTableMessage(byte[] guid, 
                                byte ttl, 
                                byte hops,
                                byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, payload.length, 
              RouteTableMessage.PATCH_VARIANT);
        //TODO: maybe we shouldn't enforce this
        //if (payload.length<5)
        //    throw new BadPacketException("Extra arguments in reset message.");
        Assert.that(payload[0]==PATCH_VARIANT);
        this.sequenceNumber=(short)ByteOrder.ubyte2int(payload[1]);
        this.sequenceSize=(short)ByteOrder.ubyte2int(payload[2]);
        this.compressor=payload[3];
        if (! (compressor==COMPRESSOR_NONE || compressor==COMPRESSOR_GZIP))
            throw new BadPacketException("Bad compressor: "+compressor);
        this.entryBits=payload[4];
        if (entryBits<0)
            throw new BadPacketException("Negative entryBits: "+entryBits);
        this.data=new byte[payload.length-5];        
        System.arraycopy(payload, 5, data, 0, data.length);  //TODO3: avoid
    }


    /////////////////////////////// Accessors ///////////////////////////////
    
    public short getSequenceNumber() {
        return sequenceNumber;
    }

    public short getSequenceSize() {
        return sequenceSize;
    }
        
    public byte getCompressor() {
        return compressor;
    }

    public byte getEntryBits() {
        return entryBits;
    }

    public byte[] getData() {
        return data;
    }

    public String toString() {
        StringBuffer buf=new StringBuffer();
        buf.append("{PATCH, Sequence: "+getSequenceNumber()+"/"+getSequenceSize()
              +", Compr: "+getCompressor()+", [");
        for (int i=0; i<data.length; i++) {
            if (data[i]!=0)
                buf.append(i+"/"+data[i]+", ");
        }
        buf.append("]");
        return buf.toString();
    }


    /** Unit test */
    public static void main(String args[]) {
        //From scratch.  Check encode.
        PatchTableMessage m=new PatchTableMessage(
            (short)3, (short)255, COMPRESSOR_NONE, (byte)2,
            new byte[] {(byte)0, (byte)0xAB, (byte)0xCD, (byte)0},
            1, 3);
        Assert.that(m.getVariant()==ResetTableMessage.PATCH_VARIANT);
        Assert.that(m.getTTL()==(byte)1);
        Assert.that(m.getSequenceSize()==255);
        Assert.that(m.getSequenceNumber()==3);
        Assert.that(m.getCompressor()==COMPRESSOR_NONE);
        Assert.that(m.getEntryBits()==2);
        Assert.that(Arrays.equals(m.getData(),
                                  new byte[] {(byte)0xAB, (byte)0xCD }));
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try {
            m.write(out);
            out.flush();
        } catch (IOException e) {
            Assert.that(false);
        }
        Assert.that(m.getVariant()==ResetTableMessage.PATCH_VARIANT);
        Assert.that(m.getTTL()==(byte)1);
        Assert.that(m.getSequenceSize()==255);
        Assert.that(m.getSequenceNumber()==3);
        Assert.that(m.getCompressor()==COMPRESSOR_NONE);
        Assert.that(m.getEntryBits()==2);
        Assert.that(Arrays.equals(m.getData(),
                                  new byte[] {(byte)0xAB, (byte)0xCD }));

        //Read from bytes
        byte[] message=new byte[23+5+2];
        message[16]=Message.F_ROUTE_TABLE_UPDATE;            //function code
        message[17]=(byte)1;                                 //TTL
        message[19]=(byte)7;                                 //payload length
        message[23+0]=(byte)RouteTableMessage.PATCH_VARIANT; //patch variant
        message[23+1]=(byte)1;                               //sequence 1...
        message[23+2]=(byte)0xFF;                            //...of 255
        message[23+3]=COMPRESSOR_GZIP;                       //comrpessor
        message[23+4]=(byte)2;                               //entry bits
        message[23+5]=(byte)0xAB;                            //data
        message[23+6]=(byte)0xCD;
        m=read(message);
        Assert.that(m.getVariant()==RouteTableMessage.PATCH_VARIANT);
        Assert.that(m.getTTL()==(byte)1);
        Assert.that(m.getSequenceNumber()==1);
        Assert.that(m.getSequenceSize()==255, "Got: "+m.getSequenceSize());
        Assert.that(m.getCompressor()==COMPRESSOR_GZIP);
        Assert.that(m.getEntryBits()==2);
        Assert.that(m.getData().length==2);
        Assert.that(m.getData()[0]==(byte)0xAB);
        Assert.that(m.getData()[1]==(byte)0xCD);
    }

    static PatchTableMessage read(byte[] bytes) {
        InputStream in=new ByteArrayInputStream(bytes);
        try {
            return (PatchTableMessage)Message.read(in);
        } catch (Exception e) {
            Assert.that(false, "Bad message: "+e);
            return null;  //never executed
        } 
    }
}
