package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
import java.io.*;

/**
 * An abstract class representing all variants of the new ROUTE_TABLE_UPDATE
 * message.  Contains the variant name and the TTTL argument shared among all
 * messages.<p>
 *
 * Like Message, this has no public constructors.  To decode bytes from call the
 * static read(..) method.  To create a new message from scratch, call one of
 * its subclass' constructors.<p>
 *
 * Subclasses of RouteTableMessage can handle tables of up to 2^31 bits.  Note
 * however, that the protocol allows for tables of up to 2^32 bits. 
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


    //////////////////////////// Encoding /////////////////////////////////////

    /**
     * Creates a new RouteTableMessage from scratch.  The variant, tableTTL, and
     * tableAddressSize parameters are the same as in the other constructor.
     * The remaining parameters are the same as in Message's constructor.
     */
    protected RouteTableMessage(byte ttl,
                                byte length, 
                                byte variant,
                                byte tableTTL) {
        super(Message.F_ROUTE_TABLE_UPDATE, ttl, length);
        this.variant=variant;
        this.tableTTL=tableTTL;
    }


    protected void writePayload(OutputStream out) throws IOException {
        out.write(variant);
        out.write(tableTTL);
        writePayloadData(out);
    }

    /** 
     * @modifies out
     * @effects writes the data following the common variant and table TTL 
     *  to out.  Does NOT flush out.
     */
    protected abstract void writePayloadData(OutputStream out) 
                                                      throws IOException;



    //////////////////////////////// Decoding ////////////////////////////////

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
        if (payload.length<2)
            throw new BadPacketException("Payload too small");
        byte variant=payload[0];
        //TODO: should this code be moved into the subclasses?
        byte tableTTL=payload[1];
        if (tableTTL<0)
            throw new BadPacketException("Negative tableTTL");
        
        //...and pass them to the subclass' constructor, which will in turn
        //call this constructor.
        switch (variant) {
        case RESET_VARIANT:
            return new ResetTableMessage(guid, ttl, hops, tableTTL, payload);
        case SET_DENSE_BLOCK_VARIANT:
            return new SetDenseTableMessage(guid, ttl, hops, tableTTL, payload);
        case ADD_SPARSE_BLOCK_VARIANT:
        case REMOVE_SPARSE_BLOCK_VARIANT:
        default:
            //TODO: this should really return a dummy message that can 
            //be forwarded without parsing.
            throw new BadPacketException("Unknown table variant");
        }
    }


    /*
     * Creates a new RouteTableMessage with data read from the network.  This
     * is called by subclasses after the payload is fully decoded.
     * 
     * @param variant the type of update message.  REQUIRES: one of RESET_VARIANT,
     *  SET_DENSE_BLOCK_VARIANT, ADD_SPARSE_BLOCK_VARIANT, or
     *  REMOVE_SPARSE_BLOCK_VARIANT.
     * @param tableTTL routing tables for queries with hops of tableTTL
     *  and higher will be affected.  REQUIRES: tableTTL>0.
     *
     * @see com.limegroup.gnutella.Message
     */
    protected RouteTableMessage(byte[] guid,
                                byte ttl,
                                byte hops,
                                int  length,
                                byte variant,
                                byte tableTTL) {
        super(guid, Message.F_ROUTE_TABLE_UPDATE, ttl, hops, length);
        this.variant=variant;
        this.tableTTL=tableTTL;
    }



    ///////////////////////////// Accessors //////////////////////////////

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


    /** Unit test */
    public static void main(String[] args) {                  
        //Read bytes with bad variant
        byte[] message=new byte[23+2];
        message[16]=Message.F_ROUTE_TABLE_UPDATE;            //function code
        message[17]=(byte)3;                                 //TTL
        message[19]=(byte)2;                                 //payload length
        message[23+0]=(byte)0xFF;                            //bogus variant
        InputStream in=new ByteArrayInputStream(message);
        try {
            Message m=(ResetTableMessage)Message.read(in);
            Assert.that(false);
        } catch (BadPacketException e) {
        } catch (Exception e) {
            Assert.that(false);
        }

        //Read bytes with negative values
        message=new byte[23+6];
        message[16]=Message.F_ROUTE_TABLE_UPDATE;            //function code
        message[17]=(byte)3;                                 //TTL
        message[19]=(byte)6;                                 //payload length
        message[23+0]=(byte)RouteTableMessage.RESET_VARIANT; //reset variant
        message[23+1]=(byte)-2;                              //table TTL
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


 
