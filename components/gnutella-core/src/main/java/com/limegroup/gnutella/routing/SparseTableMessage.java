package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
import java.io.*;
import com.sun.java.util.collections.*;

/**
 * The ADD_SPARSE_BLOCK_VARIANT and REMOVE_SPARSE_BLOCK_VARIANT variants.  (The
 * variants are too similar to justify different classes.)  Each message
 * contains a sequence of blocks, where a "block" is just a fixed-width number,
 * e.g., a 2-byte unsigned integer.  
 */
public class SparseTableMessage extends RouteTableMessage {
    /** The offset in the payload of the bitmap (past the variant, TTTL, and 
     *  blockSize variable).  */
    private static final int BLOCKS_PAYLOAD_POSITION=3;

    /** INVARIANT: all blocks can fit in blockSize bytes */
    private int[] blocks;
    /** INVARIANT: 1<=blockSize<=4 */
    private byte blockSize;


    /////////////////////////////// Encoding //////////////////////////////
    
    /** 
     * Creates a new ADD_SPARSE_BLOCK_VARIANT of REMOVE_SPARSE_BLOCK_VARIANT
     * from scratch.  This method may be more convenient in some cases than the
     * corresponding constructor, as it figures out block size for you.
     * Unlike the constructor, blocks is not aliased.  The returned message's 
     * TTL is always 1.
     *     @requires all elements of blocks instance of Integer
     */
    public static SparseTableMessage create(
            byte tableTTL, boolean add,  List /* of Integer */ blocks) {
        int[] blocksA=new int[blocks.size()];
        byte blockSize=1;    //TODO2: chose block size based on largest i.
        for (int i=0; i<blocksA.length; i++) {
            int block=((Integer)blocks.get(i)).intValue();
            blockSize=(byte)Math.max(blockSize, bytes(block));
            blocksA[i]=block;
        }
        return new SparseTableMessage((byte)1,
                                      tableTTL, add, blocksA, blockSize);
    }

    /** Returns the number of bytes needed to represent n in binary.  For
     *  example, bytes(0)==1, bytes(1)==1, bytes(0xFFF)==2.
     *      @requires n>=0 */
    private static byte bytes(int n) {
        Assert.that(n>=0, "Precondition for bytes(int) violated.");
        if (n<=0xFF)
            return 1;
        else if (n<=0xFFFF)
            return 2;
        else if (n<=0xFFFFFF)
            return 3;
        else
            return 4;
    }

    /**
     * Creates a new ADD_SPARSE_BLOCK_VARIANT or REMOVE_SPARSE_BLOCK_VARIANT
     * message from scratch, depending on whether 'add' is true or false,
     * respectively.  For efficiency reasons, blocks is aliased internally;
     * hence callers must not modify the array after calling this.
     *
     * @requires blockSize<=4 && blockSize>=1, blocks not modified afterwards
     */
    public SparseTableMessage(byte ttl,
                              byte tableTTL,
                              boolean add,
                              int[] blocks,
                              byte blockSize) {
        //Payload length includes common arguments
        //TODO3: avoid two calls to blocks.  
        super((byte)ttl,
              BLOCKS_PAYLOAD_POSITION + blockSize*blocks.length,
              add ? RouteTableMessage.ADD_SPARSE_BLOCK_VARIANT
                  : RouteTableMessage.REMOVE_SPARSE_BLOCK_VARIANT, 
              tableTTL);    

        Assert.that(blockSize<=4 && blockSize>=1, "Bad block size: "+blockSize);
        this.blockSize=blockSize;
        this.blocks=blocks;         //Exposes rep!
    }


    protected void writePayloadData(OutputStream out) throws IOException {
        out.write(blockSize);
        //Because we restrict our block sizes to 4 bytes, we can simply use the
        //"int2leb" method and discard the high bytes accordingly.  This avoids
        //the need for the "int2short" method, etc.
        byte[] buf=new byte[4];
        for (int i=0; i<blocks.length; i++) {
            ByteOrder.int2leb(blocks[i], buf, 0);
            out.write(buf, 0, blockSize);
        }
    }        


    /////////////////////////////// Decoding //////////////////////////////

    /**
     * Creates a new ADD_SPARSE_BLOCK or REMOVE_SPARSE_BLOCK message with data
     * read from the network.  The payload argument is the complete payload of
     * the message.  The first byte is guaranteed to be ADD_SPARSE_BLOCK if add
     * is true, or REMOVE_SPARSE_BLOCK otherwise.  The second byte is tableTTL.
     * These values will be passed to the superclass.
     * 
     * @exception BadPacketException the remaining values in payload are not
     *  well-formed, e.g., because it's the wrong length.  
     */
    protected SparseTableMessage(byte[] guid, 
                                 byte ttl, 
                                 byte hops,
                                 boolean add,
                                 byte tableTTL, 
                                 byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, payload.length,
              add ? RouteTableMessage.ADD_SPARSE_BLOCK_VARIANT
                  : RouteTableMessage.REMOVE_SPARSE_BLOCK_VARIANT,
              tableTTL);

        if (payload.length<3)
            //Note that we allow messages with zero blocks.  Why not?
            throw new BadPacketException("Payload too small: "+payload.length);
        blockSize=payload[BLOCKS_PAYLOAD_POSITION-1];
        if (blockSize>4 || this.blockSize<1)
            throw new BadPacketException("Bad block size: "+blockSize);        
        if (((payload.length-BLOCKS_PAYLOAD_POSITION) % blockSize) != 0)
            throw new BadPacketException("Remaining payload not multiple of block size: "
                                         +payload.length+" vs "+blockSize);

        //Copy blocks from payload into array.
        blocks=new int[(payload.length-BLOCKS_PAYLOAD_POSITION) / blockSize];
        byte[] buf=new byte[4];
        int j=0;
        for (int i=BLOCKS_PAYLOAD_POSITION; i<payload.length; i+=blockSize) {
            //As with the writePayloadData, we take advantage of the fact that
            //blockSize<=4 and just use the "leb2int" method.  First we copy
            //payload[i..i+2] to buf, padding remaining bytes with zeroes.
            //(Yes, this results in a few more stores than necessary.)
            buf[0]=(byte)0;  buf[1]=(byte)0;  buf[2]=(byte)0;  buf[3]=(byte)0;
            System.arraycopy(payload, i, buf, 0, blockSize);
            blocks[j]=ByteOrder.leb2int(buf, 0);
            j++;
        }
    }


    /////////////////////////////// Accessors ///////////////////////////////

    /** Returns if this is an ADD_SPARSE_BLOCK_VARIANT, false if it's
     *  a REMOVE_SPARSE_BLOCK_VARIANT. */
    public boolean isAdd() {
        if (getVariant()==RouteTableMessage.ADD_SPARSE_BLOCK_VARIANT)
            return true;
        else if (getVariant()==RouteTableMessage.REMOVE_SPARSE_BLOCK_VARIANT) 
            return false;
        else {
            Assert.that(false, "Bad variant: "+getVariant());
            return false;   //Never executed
        }
    }

    /** Returns the number of blocks in this. */
    public int getSize() {
        return blocks.length;
    }

    /** Returns the i'th block in this.  Throws IndexOutOfBoundsException if 
     *  i<0 or i>=size(). */
    public int getBlock(int i) {
        return blocks[i];
    }


    public String toString() {
        StringBuffer buf=new StringBuffer();
        if (isAdd())
            buf.append("{ADD_SPARSE_BLOCK_VARIANT (");
        else
            buf.append("{REMOVE_SPARSE_BLOCK_VARIANT (");
        buf.append(getTableTTL());
        buf.append(", ");
        buf.append(blockSize);
        buf.append(") ");

        for (int i=0; i<blocks.length; i++) {
            buf.append(blocks[i]);
            buf.append(", ");
        }
        buf.append("}");
        return buf.toString();
    }

    /** Unit test */
    public static void main(String[] args) {
        //1. Encoding test
        SparseTableMessage m=new SparseTableMessage(
            (byte)2, (byte)5,
            true, new int[] {1, 513, 66051}, (byte)3);
        byte[] bytes=write(m);
        Assert.that(bytes.length==(23+BLOCKS_PAYLOAD_POSITION+3*3),
                    "Got: "+bytes.length);
        Assert.that(m.isAdd());
        Assert.that(bytes[23+0]==RouteTableMessage.ADD_SPARSE_BLOCK_VARIANT);
        Assert.that(bytes[23+1]==(byte)5);                      //table TTL
        Assert.that(bytes[23+2]==(byte)3);                      //block size
        Assert.that(bytes[23+BLOCKS_PAYLOAD_POSITION+0]==(byte)1); //block "1"
        Assert.that(bytes[23+BLOCKS_PAYLOAD_POSITION+1]==(byte)0);
        Assert.that(bytes[23+BLOCKS_PAYLOAD_POSITION+2]==(byte)0);
        Assert.that(bytes[23+BLOCKS_PAYLOAD_POSITION+3]==(byte)1); //block "513"
        Assert.that(bytes[23+BLOCKS_PAYLOAD_POSITION+4]==(byte)2);
        Assert.that(bytes[23+BLOCKS_PAYLOAD_POSITION+5]==(byte)0);
        Assert.that(bytes[23+BLOCKS_PAYLOAD_POSITION+6]==(byte)3); //blk "66051"
        Assert.that(bytes[23+BLOCKS_PAYLOAD_POSITION+7]==(byte)2);
        Assert.that(bytes[23+BLOCKS_PAYLOAD_POSITION+8]==(byte)1);

        List blocks=new ArrayList();
        blocks.add(new Integer(1));
        blocks.add(new Integer(66051));
        blocks.add(new Integer(513));
        m=SparseTableMessage.create((byte)5, true, blocks);
        Assert.that(m.getTTL()==1);
        Assert.that(m.getSize()==3);
        Assert.that(m.blockSize==3);

        //2. Decoding test.
        try {
            m=read(bytes);
        } catch (BadPacketException e) {
            Assert.that(false, "Bad packet");
        }
        Assert.that(m.isAdd());
        Assert.that(m.getVariant()==RouteTableMessage.ADD_SPARSE_BLOCK_VARIANT);
        Assert.that(m.getSize()==3);
        Assert.that(m.getBlock(0)==1);
        Assert.that(m.getBlock(1)==513);
        Assert.that(m.getBlock(2)==66051);

        //3. Remove variant
        bytes[23+0]=RouteTableMessage.REMOVE_SPARSE_BLOCK_VARIANT;  //from net
        try {
            m=read(bytes);
            Assert.that(! m.isAdd());
        } catch (BadPacketException e) {
            Assert.that(false, "Bad packet");
        }
        m=new SparseTableMessage(                                   //from scratch
            (byte)2, (byte)5,
            false, new int[] {1, 513, 66051}, (byte)3);
        Assert.that(! m.isAdd());
        

        //4. Error cases.
        bytes[23+2]=(byte)5;         //blocks not a multiple of block size
        try {
            m=read(bytes);
            Assert.that(false);
        } catch (BadPacketException e) {            
        }

        bytes[23+2]=(byte)9;         //block size too damn big
        try {
            m=read(bytes);
            Assert.that(false);
        } catch (BadPacketException e) {            
        }

        bytes[23+2]=(byte)0;         //block size too small
        try {
            m=read(bytes);
            Assert.that(false);
        } catch (BadPacketException e) {            
        }
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

    static SparseTableMessage read(byte[] bytes) throws BadPacketException {
        InputStream in=new ByteArrayInputStream(bytes);
        try {
            return (SparseTableMessage)Message.read(in);
        } catch (IOException e) {
            Assert.that(false, "Couldn't read: "+e);
            return null;  //never executed
        } 
    }
}
