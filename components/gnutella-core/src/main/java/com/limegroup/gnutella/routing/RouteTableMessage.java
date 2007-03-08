
// Commented for the Learning branch

package com.limegroup.gnutella.routing;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;

/**
 * RouteTableMessage is the base class for objects that represent the two different kinds of QRP messages.
 * 
 * There are two kinds of messages used with QRP in Gnutella: ResetTableMessage and PatchTableMessage.
 * The class heirarchy of what extends what looks like this:
 * 
 *   Message
 *     RouteTableMessage
 *       PatchTableMessage
 *       ResetTableMessage
 * 
 * The base class Message represents the 23-byte Gnutella message header, with the GUID, TTL and hops.
 * This RouteTableMessage class adds the first byte of the payload, which is 0x00 for a ResetTableMessage and 0x01 for a PatchTableMessage.
 * 
 * An abstract class representing all variants of the new ROUTE_TABLE_UPDATE
 * message. Like Message, this has no public constructors.  To decode bytes from
 * call the static read(..) method.  To create a new message from scratch, call
 * one of its subclass' constructors.
 */
public abstract class RouteTableMessage extends Message {

	/** 0x00, a ResetTableMessage is marked with 0x00 as the first payload byte. */
    public static final byte RESET_VARIANT = (byte)0x0;

    /** 0x01, a PatchTableMessage is marked with 0x01 as the first payload byte. */
    public static final byte PATCH_VARIANT = (byte)0x1;

    /** The first byte of the payload, which will be 0x00 for a ResetTableMessage or 0x01 for a PatchTableMessage. */
    private byte variant;

    /*
     * //////////////////////////// Encoding /////////////////////////////////////
     */

    /**
     * Make a new RouteTableMessage from the given information to represent a QRP message we'll send.
     * The PatchTableMessage and ResetTableMessage constructors call this one.
     * Sets up the Message and RouteTableMessage cores of the new object.
     * 
     * @param ttl     The TTL to put in the message header
     * @param length  The payload length
     * @param variant A byte, 0x00 to make a new ResetTableMessage or 0x01 to make a new PatchTableMessage
     */
    protected RouteTableMessage(byte ttl, int length, byte variant) {

    	// Call the Message constructor to save information in the message header
        super(
        	Message.F_ROUTE_TABLE_UPDATE, // 0x30, the type code for a QRP message
        	ttl,
        	length);

        // Save the first byte of the payload that tells what kind of QRP message this will be
        this.variant = variant;
    }

    /**
     * Write the payload of this QRP message.
     * You've already written the 23-byte Gnutella message header.
     * Call writePayload() to write the first byte that tells what kind of QRP message this is, followed by the payload specific to that kind of message.
     * 
     * @param out An OutputStream object we can call write() on to send data to a remote computer
     */
    protected void writePayload(OutputStream out) throws IOException {

    	// Write the first byte of the payload, 0x00 for a ResetTableMessage or 0x01 for a PatchTableMessage
        out.write(variant);

        // Call ResetTableMessage.writePayloadData() or PatchTableMessage.writePayloadData() to write the rest of the payload, specific to the kind of message this is
        writePayloadData(out);
    }

    /**
     * Write the payload of this QRP message after the 23-byte Gnutella message header and the first byte that tells what kind of QRP message it is.
     * This method is abstract, ResetTableMessage and PatchTableMessage have writePayloadData methods that implement it.
     * 
     * @param out An OutputStream object we can call write() on to send data to a remote computer.
     *            Does not flush the out OutputStream.
     */
    protected abstract void writePayloadData(OutputStream out) throws IOException;

    /*
     * //////////////////////////////// Decoding ////////////////////////////////
     */

    /**
     * Parse data from a remote computer into a ResetTableMessage or PatchTableMessage object.
     * 
     * Creates a new RouteTableMessage from raw bytes read from the network.
     * The returned value is a subclass of RouteTableMessage depending on
     * the variant in the payload.  (That's why there is no corresponding
     * constructor in this.)
     * 
     * @param  guid               The message GUID read from the header
     * @param  ttl                The TTL read from the header
     * @param  hops               The hops count read from the header
     * @param  payload            The bytes of the message payload read beyond the 23-byte message header
     * @return                    A ResetTableMessage or PatchTableMessage object to represent the message we read and parsed
     * @throws BadPacketException The payload contained something we couldn't understand
     */
    public static RouteTableMessage read(byte[] guid, byte ttl, byte hops, byte[] payload) throws BadPacketException {

    	// Read the first byte of the payload, which will tell us if this is a ResetTableMessage or a PatchTableMessage
        if (payload.length < 2) throw new BadPacketException("Payload too small");
        byte variant = payload[0];

        /*
         * ...and pass them to the subclass' constructor, which will in turn
         * call this constructor.
         */

        // Sort by the first payload byte
        switch (variant) {

        // The first payload byte is 0x00, this is a ResetTableMessage
        case RESET_VARIANT:

        	// Make and return a new ResetTableMessage object to represent it
            return new ResetTableMessage(guid, ttl, hops, payload);

        // The first payload byte is 0x01, this is a PatchTableMessage
        case PATCH_VARIANT:

        	// Make and return a new ResetTableMessage object to represent it
        	return new PatchTableMessage(guid, ttl, hops, payload);

        // The first payload byte isn't 0x00 or 0x01, we don't know how to deal with this kind of message
        default:

        	// Throw a BadPacketException to move on to the next message
        	throw new BadPacketException("Unknown table variant");
        }
    }

    /**
     * Make a new RouteTableMessage object to represent a message a remote computer sent us.
     * The PatchTableMessage and ResetTableMessage constructors call this to setup their RouteTableMessage cores.
     * 
     * @param guid    The message GUID we read from the header
     * @param ttl     The TTL we read from the header
     * @param hops    The hops count we read from the header
     * @param length  The length of the payload
     * @param variant The first byte of the payload, 0x00 for ResetTableMessage or 0x01 for PatchTableMessage
     */
    protected RouteTableMessage(byte[] guid, byte ttl, byte hops, int length, byte variant) {

    	// Save the given information in the Message core of this RouteTableMessage object 
        super(
        	guid,
        	Message.F_ROUTE_TABLE_UPDATE, // 0x30, the byte that identifes a QRP message
        	ttl,
        	hops,
        	length);

        // Save the first byte of the payload, 0x00 for ResetTableMessage or 0x01 for PatchTableMessage
        this.variant = variant;
    }

    /*
     * ///////////////////////////// Accessors //////////////////////////////
     */

    /**
     * Get the first byte of the payload of this QRP message, which tells what kind of QRP message it is.
     * 
     * @return 0x00 if this is a ResetTableMessage.
     *         0x01 if this is a PatchTableMessage.
     */
    public byte getVariant() {

    	// Return the byte we saved or parsed
    	return variant;
    }

    /**
     * stripExtendedPayload() is supposed to remove a GGEP block from this message if it has one, and return it.
     * Since QRP messages don't have GGEP blocks, this method returns a reference to this object unchanged.
     * 
     * @return A reference to this same object, without changing it.
     */
    public Message stripExtendedPayload() {

    	// There is no GGEP block to remove, return a reference to this object
    	return this;
    }
}
