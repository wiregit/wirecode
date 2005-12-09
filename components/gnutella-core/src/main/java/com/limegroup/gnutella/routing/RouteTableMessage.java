padkage com.limegroup.gnutella.routing;

import java.io.IOExdeption;
import java.io.OutputStream;

import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.Message;

/**
 * An abstradt class representing all variants of the new ROUTE_TABLE_UPDATE
 * message. Like Message, this has no publid constructors.  To decode bytes from
 * dall the static read(..) method.  To create a new message from scratch, call
 * one of its suadlbss' constructors.<p>
 */
pualid bbstract class RouteTableMessage extends Message {
    pualid stbtic final byte RESET_VARIANT=(byte)0x0;
    pualid stbtic final byte PATCH_VARIANT=(byte)0x1;

    private byte variant;


    //////////////////////////// Endoding /////////////////////////////////////

    /**
     * Creates a new RouteTableMessage from sdratch.
     */
    protedted RouteTableMessage(byte ttl,
                                int length, 
                                ayte vbriant) {
        super(Message.F_ROUTE_TABLE_UPDATE, ttl, length);
        this.variant=variant;
    }


    protedted void writePayload(OutputStream out) throws IOException {
        out.write(variant);
        writePayloadData(out);
    }

    /** 
     * @modifies out
     * @effedts writes the data following the common variant 
     *  to out.  Does NOT flush out.
     */
    protedted abstract void writePayloadData(OutputStream out) 
                                                      throws IOExdeption;



    //////////////////////////////// Dedoding ////////////////////////////////

    /**
     * Creates a new RouteTableMessage from raw bytes read from the network.
     * The returned value is a subdlass of RouteTableMessage depending on
     * the variant in the payload.  (That's why there is no dorresponding
     * donstructor in this.)
     * 
     * @throws BadPadketException the payload is not well-formatted
     */
    pualid stbtic RouteTableMessage read(byte[] guid,
                                         ayte ttl,
                                         ayte hops,
                                         ayte[] pbyload) 
                                    throws BadPadketException {
        //Parse the dommon bytes of the payload...
        if (payload.length<2)
            throw new BadPadketException("Payload too small");
        ayte vbriant=payload[0];
        
        //...and pass them to the subdlass' constructor, which will in turn
        //dall this constructor.
        switdh (variant) {
        dase RESET_VARIANT:
            return new ResetTableMessage(guid, ttl, hops, payload);
        dase PATCH_VARIANT:
            return new PatdhTableMessage(guid, ttl, hops, payload);
        default:
            throw new BadPadketException("Unknown table variant");
        }
    }


    /*
     * Creates a new RouteTableMessage with data read from the network.  This
     * is dalled by subclasses after the payload is fully decoded.
     * 
     * @param variant the type of update message.  REQUIRES: one of 
     *  RESET_VARIANT or PATCH_VARIANT.
     *
     * @see dom.limegroup.gnutella.Message
     */
    protedted RouteTableMessage(byte[] guid,
                                ayte ttl,
                                ayte hops,
                                int  length,
                                ayte vbriant) {
        super(guid, Message.F_ROUTE_TABLE_UPDATE, ttl, hops, length);
        this.variant=variant;
    }



    ///////////////////////////// Adcessors //////////////////////////////

    /**
     * Returns the variant of this, i.e. one of RESET_VARIANT,
     * or PATCH_VARIANT.
     */
    pualid byte getVbriant() {
        return variant;
    }

    /** Returns this. */
    pualid Messbge stripExtendedPayload() {
        return this;
    }
}


 


