package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.statistics.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;
import java.io.*;

/**
 * An abstract class representing all variants of the new ROUTE_TABLE_UPDATE
 * message. Like Message, this has no public constructors.  To decode bytes from
 * call the static read(..) method.  To create a new message from scratch, call
 * one of its subclass' constructors.<p>
 */
public abstract class RouteTableMessage extends Message {
    public static final byte RESET_VARIANT=(byte)0x0;
    public static final byte PATCH_VARIANT=(byte)0x1;

    private byte variant;


    //////////////////////////// Encoding /////////////////////////////////////

    /**
     * Creates a new RouteTableMessage from scratch.
     */
    protected RouteTableMessage(byte ttl,
                                int length, 
                                byte variant) {
        super(Message.F_ROUTE_TABLE_UPDATE, ttl, length);
        this.variant=variant;
    }


    protected void writePayload(OutputStream out) throws IOException {
        out.write(variant);
        writePayloadData(out);
		if(RECORD_STATS) {
			SentMessageStatHandler.TCP_ROUTE_TABLE_MESSAGES.addMessage(this);
		}
    }

    /** 
     * @modifies out
     * @effects writes the data following the common variant 
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
        
        //...and pass them to the subclass' constructor, which will in turn
        //call this constructor.
        switch (variant) {
        case RESET_VARIANT:
            return new ResetTableMessage(guid, ttl, hops, payload);
        case PATCH_VARIANT:
            return new PatchTableMessage(guid, ttl, hops, payload);
        default:
            throw new BadPacketException("Unknown table variant");
        }
    }


    /*
     * Creates a new RouteTableMessage with data read from the network.  This
     * is called by subclasses after the payload is fully decoded.
     * 
     * @param variant the type of update message.  REQUIRES: one of 
     *  RESET_VARIANT or PATCH_VARIANT.
     *
     * @see com.limegroup.gnutella.Message
     */
    protected RouteTableMessage(byte[] guid,
                                byte ttl,
                                byte hops,
                                int  length,
                                byte variant) {
        super(guid, Message.F_ROUTE_TABLE_UPDATE, ttl, hops, length);
        this.variant=variant;
    }



    ///////////////////////////// Accessors //////////////////////////////

    /**
     * Returns the variant of this, i.e. one of RESET_VARIANT,
     * or PATCH_VARIANT.
     */
    public byte getVariant() {
        return variant;
    }

    /** Returns this. */
    public Message stripExtendedPayload() {
        return this;
    }

	// inherit doc comment
	public void recordDrop() {
		if(RECORD_STATS) {
			DroppedSentMessageStatHandler.TCP_ROUTE_TABLE_MESSAGES.addMessage(this);	   
		}
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
    }
}


 
