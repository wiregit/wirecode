package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
import java.io.*;
import java.util.BitSet;

/**
 * The SET_DENSE_BLOCK route table update message.  Represents an array
 * of bits from a start offset to a stop offset.
 */
public class SetDenseTableMessage extends RouteTableMessage {
    /** The offset in the payload of the bitmap (past the variant, TTTL, and and
     *  startOffset variable).  */
    private static final int BITMAP_PAYLOAD_POSITION=6;

    /**
     * Variables included in the protocol.
     */
    private int startOffset;
    private BitSet bits;

    /** The size of bits.  Needed because BitSet.length doesn't exist in Java
     *  1.1.8.  This is not actually written to the network. */
    private int bitsLength;


    /////////////////////////////// Encoding //////////////////////////////

    /**
     * Creates a new SetDenseTableMessage from scratch.  This will
     * convey the bits in bset[startOffset] through bset[stopOffset],
     * inclusive.
     *
     * @requires startOffset<=stopOffset, and stopOffset<bset.length()
     */
    public SetDenseTableMessage(byte ttl,
                                byte tableTTL,
                                BitSet bset,
                                int startOffset,
                                int stopOffset) {
        //Payload length includes common arguments
        super(ttl,
              BITMAP_PAYLOAD_POSITION+bits2bytes(stopOffset-startOffset+1),
              RouteTableMessage.SET_DENSE_BLOCK_VARIANT, tableTTL);    
     
        //Copy bset to bits.  TODO2: we *could* alias bset, avoiding the copy.
        //This would be more efficient, at the loss of modularity.
        this.startOffset=startOffset;
        this.bitsLength=stopOffset-startOffset+1;
        bits=new BitSet(bitsLength);
        for (int i=0; i<bitsLength; i++) {
            if (bset.get(startOffset+i))
                bits.set(i);
        }
    }

    protected void writePayloadData(OutputStream out) throws IOException {
        //Write offset.
        byte[] buf=new byte[4];
        ByteOrder.int2leb(startOffset, buf, 0);
        out.write(buf);

        //Write bit vector.  You can probably speed this up marginally by
        //setting a whole byte at a time, instead of repeatedly OR'ing the same
        //byte, but that's more complex.
        buf=new byte[bits2bytes(bitsLength)];
        for (int i=0; i<bitsLength; i++) {
            if (bits.get(i)) {
                int byteOffset=i/8; 
                int remainder=i%8;   //bit position within buf[byteOffset]
                buf[byteOffset]=(byte)(buf[byteOffset] | 1<<remainder);
            }
        }
        out.write(buf);
    }

    /** Returns the number of bytes needed to store n bits, i.e.,
     *  ceiling(n/8). */
    private static int bits2bytes(int n) {
        boolean powerOf8 = ((n%8)==0);    //n%8 == n&0x7
        return n/8 + (powerOf8 ? 0 : 1);  //n/8 == n>>3
    }
        

    /////////////////////////////// Decoding //////////////////////////////


    /**
     * Creates a new SetDenseTableMessage with data read from the network.  The
     * payload argument is the complete payload of the message.  The first byte
     * is guaranteed to be RESET_VARIANT, and the second byte is tableTTL.
     * These values will be passed to the superclass.
     * 
     * @exception BadPacketException the remaining values in payload are not
     *  well-formed, e.g., because it's the wrong length.  */
    protected SetDenseTableMessage(byte[] guid, 
                                   byte ttl, 
                                   byte hops,
                                   byte tableTTL, 
                                   byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, payload.length,
              RouteTableMessage.SET_DENSE_BLOCK_VARIANT, tableTTL);

        //Verify length and parse startOffset variable.
        if (payload.length<=BITMAP_PAYLOAD_POSITION)
            throw new BadPacketException("Dense payload too small");
        this.startOffset=ByteOrder.leb2int(payload, 2);
        if (startOffset<0)
            throw new BadPacketException("Offset too big");

        //Parse bitmap and store in BitSet. More efficient algorithms exist.
        //TODO: a far better performance improvement might come by using raw
        //bytes for the underlying representation, instead of a BitSet.  This
        //avoids the copying.
        this.bitsLength=8*(getLength()-BITMAP_PAYLOAD_POSITION);
        this.bits=new BitSet(bitsLength);
        for (int i=0; i<bitsLength/8; i++) {  //for each i'th byte
            byte b=payload[BITMAP_PAYLOAD_POSITION+i];
            for (int j=0; j<8; j++) {         //for each j'th bit within the byte
                int mask=(1<<j);
                if ((b&mask)!=0) 
                    bits.set(8*i+j);
            }
        }
    }


    /////////////////////////////// Accessors ///////////////////////////////

    /** Returns the lowest legal index to get(int). */
    public int getStartOffset() {
        return startOffset;
    }

    /** Returns the highest legal index to get(int). */
    public int getStopOffset() {
        return startOffset+bitsLength-1;
    }

    /** Returns the i'th bit of this' data block.  Throws
     * IndexOutOfBoundsException if i<getStartOffset or i>getStopOffset.  TODO2:
     * by exposing bits or providing, a copyTo/orWith method, we could make this
     * more efficient for typical callers. */
    public boolean get(int i) throws IndexOutOfBoundsException {
        return bits.get(i-startOffset);
    }


    public String toString() {
        StringBuffer buf=new StringBuffer();
        int x=0;
        for (int i=0; i<bitsLength; i++) {
            if (bits.get(i)) 
                x++;
        }
        return "{SET_DENSE, TTTL: "+getTableTTL()+", entries: "+x+"}";
    }

    /** Unit test */
    public static void main(String[] args) {
        Assert.that(bits2bytes(2)==1);
        Assert.that(bits2bytes(8)==1);
        Assert.that(bits2bytes(9)==2);
        Assert.that(bits2bytes(15)==2);
        Assert.that(bits2bytes(16)==2);
        Assert.that(bits2bytes(17)==3);

        //1. Encoding test.   
        //   Index:            0 1 2 3 4 5 6 7 8 9 10
        //   Values of bits:   1 0 1 0 0 0 0 1 0 1 1
        //   Values of m:      0 1 0 0 0 0 1 0 1       (shift left, remove last)
        BitSet bits=new BitSet(11);
        bits.set(0);
        bits.set(2);
        bits.set(7);
        bits.set(9);
        bits.set(10);
        SetDenseTableMessage m=new SetDenseTableMessage((byte)3, (byte)7,
                                                        bits, 1, 9);
        Assert.that(m.getLength()==8);
        byte[] bytes=write(m);
        Assert.that(bytes.length==(23+6+2), "Length: "+bytes.length);
        Assert.that(bytes[23+0]==RouteTableMessage.SET_DENSE_BLOCK_VARIANT);
        Assert.that(bytes[23+1]==(byte)7);    //TTTL
        Assert.that(bytes[23+2]==(byte)1);    //offset
        Assert.that(bytes[23+3]==(byte)0); 
        Assert.that(bytes[23+4]==(byte)0);
        Assert.that(bytes[23+5]==(byte)0);
        Assert.that(bytes[23+6+0]==(byte)66);  //bitmap
        Assert.that(bytes[23+6+1]==(byte)1);

        //2. Decoding test
        m=read(bytes);
        Assert.that(m.getTableTTL()==(byte)7);
        Assert.that(m.getStartOffset()==(byte)1);
        Assert.that(! m.get(1));
        Assert.that(  m.get(2));
        Assert.that(! m.get(3));
        Assert.that(! m.get(4));
        Assert.that(! m.get(5));
        Assert.that(! m.get(6));
        Assert.that(  m.get(7));
        Assert.that(! m.get(8));
        Assert.that(  m.get(9));
        try {
            Assert.that(! m.get(0));
            Assert.that(false);
        } catch (IndexOutOfBoundsException e) { }                    
    }

    static byte[] write(Message m) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try {
            m.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            Assert.that(false);
            return null;  //never executed
        } 
    }

    static SetDenseTableMessage read(byte[] bytes) {
        InputStream in=new ByteArrayInputStream(bytes);
        try {
            return (SetDenseTableMessage)Message.read(in);
        } catch (Exception e) {
            Assert.that(false, "Bad message: "+e);
            return null;  //never executed
        } 
    }
}
