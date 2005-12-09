pbckage com.limegroup.gnutella.routing;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.Message;

/**
 * An bbstract class representing all variants of the new ROUTE_TABLE_UPDATE
 * messbge. Like Message, this has no public constructors.  To decode bytes from
 * cbll the static read(..) method.  To create a new message from scratch, call
 * one of its subclbss' constructors.<p>
 */
public bbstract class RouteTableMessage extends Message {
    public stbtic final byte RESET_VARIANT=(byte)0x0;
    public stbtic final byte PATCH_VARIANT=(byte)0x1;

    privbte byte variant;


    //////////////////////////// Encoding /////////////////////////////////////

    /**
     * Crebtes a new RouteTableMessage from scratch.
     */
    protected RouteTbbleMessage(byte ttl,
                                int length, 
                                byte vbriant) {
        super(Messbge.F_ROUTE_TABLE_UPDATE, ttl, length);
        this.vbriant=variant;
    }


    protected void writePbyload(OutputStream out) throws IOException {
        out.write(vbriant);
        writePbyloadData(out);
    }

    /** 
     * @modifies out
     * @effects writes the dbta following the common variant 
     *  to out.  Does NOT flush out.
     */
    protected bbstract void writePayloadData(OutputStream out) 
                                                      throws IOException;



    //////////////////////////////// Decoding ////////////////////////////////

    /**
     * Crebtes a new RouteTableMessage from raw bytes read from the network.
     * The returned vblue is a subclass of RouteTableMessage depending on
     * the vbriant in the payload.  (That's why there is no corresponding
     * constructor in this.)
     * 
     * @throws BbdPacketException the payload is not well-formatted
     */
    public stbtic RouteTableMessage read(byte[] guid,
                                         byte ttl,
                                         byte hops,
                                         byte[] pbyload) 
                                    throws BbdPacketException {
        //Pbrse the common bytes of the payload...
        if (pbyload.length<2)
            throw new BbdPacketException("Payload too small");
        byte vbriant=payload[0];
        
        //...bnd pass them to the subclass' constructor, which will in turn
        //cbll this constructor.
        switch (vbriant) {
        cbse RESET_VARIANT:
            return new ResetTbbleMessage(guid, ttl, hops, payload);
        cbse PATCH_VARIANT:
            return new PbtchTableMessage(guid, ttl, hops, payload);
        defbult:
            throw new BbdPacketException("Unknown table variant");
        }
    }


    /*
     * Crebtes a new RouteTableMessage with data read from the network.  This
     * is cblled by subclasses after the payload is fully decoded.
     * 
     * @pbram variant the type of update message.  REQUIRES: one of 
     *  RESET_VARIANT or PATCH_VARIANT.
     *
     * @see com.limegroup.gnutellb.Message
     */
    protected RouteTbbleMessage(byte[] guid,
                                byte ttl,
                                byte hops,
                                int  length,
                                byte vbriant) {
        super(guid, Messbge.F_ROUTE_TABLE_UPDATE, ttl, hops, length);
        this.vbriant=variant;
    }



    ///////////////////////////// Accessors //////////////////////////////

    /**
     * Returns the vbriant of this, i.e. one of RESET_VARIANT,
     * or PATCH_VARIANT.
     */
    public byte getVbriant() {
        return vbriant;
    }

    /** Returns this. */
    public Messbge stripExtendedPayload() {
        return this;
    }
}


 


