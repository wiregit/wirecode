package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
import java.io.*;

/**
 * An abstract class representing all variants of the new ROUTE_TABLE_UPDATE
 * message.  Like Message, it has no public constructors.  To decode bytes from 
 * call the static read(..) method.  To create a new message from scratch, call
 * one of the subclass constructors.
 */
public abstract class RouteTableMessage extends Message {
    public static final byte RESET_VARIANT=(byte)0x0;
    public static final byte SET_DENSE_BLOCK_VARIANT=(byte)0x1;
    public static final byte ADD_SPARSE_BLOCK_VARIANT=(byte)0x2;
    public static final byte REMOVE_SPARSE_BLOCK_VARIANT=(byte)0x3;
    
    /* 
     * Representation rationale: we never forward one of these packets without
     * decoding.  Hence we store the decoded packet, not the raw bytes.
     */

    private byte variant;
    private byte tableTTL;
    private byte tableAddressSize;

    /**
     * Creates a new RouteTableMessage from scratch.  The variant, tableTTL, and
     * tableAddressSize parameters are the same as in the other constructor.
     * The remaining parameters are the same as in Message's constructor.
     */
    protected RouteTableMessage(byte ttl,
                                byte length, 
                                byte variant,
                                byte tableTTL,
                                byte tableAddressSize) {
        super(Message.F_ROUTE_TABLE_UPDATE, ttl, length);
        this.variant=variant;
        this.tableTTL=tableTTL;
        this.tableAddressSize=tableAddressSize;
    }

    /*
     * Creates a new RouteTableMessage with data read from the network.
     * 
     * @param variant the type of update message.  REQUIRES: one of RESET_VARIANT,
     *  SET_DENSE_BLOCK_VARIANT, ADD_SPARSE_BLOCK_VARIANT, or
     *  REMOVE_SPARSE_BLOCK_VARIANT.
     * @param tableTTL routing tables for queries with hops of tableTTL
     *  and higher will be affected.  REQUIRES: tableTTL>0.
     * @param tableAddressSize the number of bytes needed to store the size
     *  of the routing table.  Typically 2 or 3, which allows for 2^(2*8) or
     *  2^(3*8) bits in the bitmap.  REQUIRES: tableAddressSize>0.
     *
     * All other parameters are the same as in Message.
     */
    protected RouteTableMessage(byte[] guid,
                                byte ttl,
                                byte hops,
                                int  length,
                                byte variant,
                                byte tableTTL,
                                byte tableAddressSize) {
        super(guid, Message.F_ROUTE_TABLE_UPDATE, ttl, hops, length);
        this.variant=variant;
        this.tableTTL=tableTTL;
        this.tableAddressSize=tableAddressSize;
    }


    /**
     * Creates a new RouteTableMessage from raw bytes read from the network.
     * The returned value is a subclass of RouteTableMessage depending on
     * the variant in the payload.  (That's why there is no corresponding
     * constructor in this.)
     * 
     * @throws BadPacketException the payload is not well-formatted
     */
    public static RouteTableMessage read(byte[] guid,
                                         byte ttl,
                                         byte hops,
                                         byte[] payload) 
                                    throws BadPacketException {
        //Parse the common bytes of the payload...
        if (payload.length<3)
            throw new BadPacketException("Payload too small");
        byte variant=payload[0];
        byte tableTTL=payload[1];
        byte tableAddressSize=payload[2];
        if (tableTTL<0 || tableAddressSize<0)
            throw new BadPacketException("Negative variant argument");
        
        //...and pass them to the subclass' constructor, which will in turn
        //call this constructor.
        switch (variant) {
        case RESET_VARIANT:
            return new ResetTableMessage(guid, ttl, hops,
                                         tableTTL, tableAddressSize,
                                         payload);
//          case ADD_SPARSE_BLOCK_VARIANT:
//              return new AddSparseTableMessage(guid, ttl, hops,
//                                               tableTTL, tableAddressSize,
//                                               payload);            
//          case REMOVE_SPARSE_BLOCK_VARIANT:
//              return new RemoveSparseTableMessage(guid, ttl, hops,
//                                                  tableTTL, tableAddressSize,
//                                                  payload);
        case SET_DENSE_BLOCK_VARIANT:
            return new SetDenseTableMessage(guid, ttl, hops,
                                            tableTTL, tableAddressSize,
                                            payload);
        default:
            throw new BadPacketException("Unknown table variant");
        }
    }

    protected void writePayload(OutputStream out) throws IOException {
        out.write(variant);
        out.write(tableTTL);
        out.write(tableAddressSize);
        writePayloadData(out);
    }

    /** 
     * @modifies out
     * @effects writes the data following the common variant to the header.  
     *  Does NOT flush out.
     */
    protected abstract void writePayloadData(OutputStream out) throws IOException;

    /**
     * Returns the variant of this, i.e. one of RESET_VARIANT,
     * SET_DENSE_BLOCK_VARIANT, ADD_SPARSE_BLOCK_VARIANT, or
     * REMOVE_SPARSE_BLOCK_VARIANT.
     */
    public byte getVariant() {
        return variant;
    }

    /**
     * Returns the TTL of the smallest table affected by this message.
     */
    public byte getTableTTL() {
        return tableTTL;
    }

    /** 
     * Returns the number of bytes needed to represent the size of the route
     * table.  Typically this value is quite small, e.g. 2 or 3. 
     */
    public byte getTableAddressSize() {
        return tableAddressSize;
    }

   
    public static void main(String[] args) {     
        //Read bytes with bad variant
        byte[] message=new byte[23+3];
        message[16]=Message.F_ROUTE_TABLE_UPDATE;            //function code
        message[19]=(byte)3;                                 //payload length
        message[23+0]=(byte)0xFF;                            //table address sz
        InputStream in=new ByteArrayInputStream(message);
        try {
            Message m=(ResetTableMessage)Message.read(in);
            Assert.that(false);
        } catch (BadPacketException e) {
        } catch (Exception e) {
            Assert.that(false);
        }

        //Read bytes with negative values
        message=new byte[23+3];
        message[16]=Message.F_ROUTE_TABLE_UPDATE;            //function code
        message[19]=(byte)3;                                 //payload length
        message[23+0]=(byte)RouteTableMessage.RESET_VARIANT; //reset variant
        message[23+1]=(byte)-2;                              //table TTL
        message[23+2]=(byte)2;                               //table address sz
        in=new ByteArrayInputStream(message);
        try {
            Message m=(ResetTableMessage)Message.read(in);
            Assert.that(false);
        } catch (BadPacketException e) {
        } catch (Exception e) {
            Assert.that(false);
        }
    }
}
