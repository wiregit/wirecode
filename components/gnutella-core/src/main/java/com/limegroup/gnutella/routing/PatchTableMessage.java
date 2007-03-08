
// Commented for the Learning branch

package com.limegroup.gnutella.routing;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/**
 * A PatchTableMessage object represents a QRP patch table message that we've received or are going to send.
 * Gnutella programs use groups of patch messages to exchange QRP tables.
 * 
 * A QRP table is an array of 65536 bits.
 * To send a table, a Gnutella program first turns each bit into a byte.
 * The byte value 0 means no change, a positive number like 6 means change to block a search, and -6 lets a search through.
 * These values like 6 and -6 are defined by the infinity chosen in the reset message that came before groups of patch messages.
 * The receiving computer has a record of the QRP table, allowing 0 to be no change.
 * This creates an array of 65536 bytes, which takes up 64 KB of space.
 * 
 * Next, the array is halved.
 * Instead of having each value use a full byte, two values are put in each byte.
 * This takes the array of data down to 32 KB.
 * 
 * After that, the array is compressed using deflate compression.
 * Lastly, it's cut into 4 KB chunks, and each chunk is made the payload of a patch message.
 * Those patch messages make up a group, and are numbered like this:
 * 
 * patch message 1 of 3
 * patch message 2 of 3
 * patch message 3 of 3
 * 
 * Each message in the group tells the sequence size, like 3.
 * Each message has a sequence number, and the first message has number 1, not 0.
 * 
 * A QRP patch table message looks like this:
 * 
 * aaaaaaaa
 * aaaaaaaa
 * bcdeeeef
 * NSCBtttt
 * tttttttt
 * tttttttt
 * tt...
 * 
 * a is the 16 byte message GUID.
 * b is 0x30, the byte code for a QRP message.
 * c is the TTL, 1.
 * d is the hops, 0.
 * e is the length of the payload that follows, 5 for fNSCB plus the size of the patch data, which will be up to 4 KB.
 * 
 * f is the first byte of the payload, 0x01 identifes this as a patch message.
 * 
 * N is the sequence number, like 1, 2, or 3 that tells where this patch message is ordered in its group.
 * S is the sequence size, like 3.
 * C is the compressor, 0x00 if the patch data isn't compressed, 0x01 if it is compressed.
 * B is the entry bits, 4 if the patch values take up 4 bits in the patch data, 8 if they each take up a full byte.
 * 
 * t is the chunk of compressed patch data, up to 4 KB long.
 * 
 * The PATCH route table update message.  This class is as simple as possible.
 * For example, the getData() method returns the raw bytes of the message,
 * requiring the caller to call the getEntryBits() method to calculate the i'th
 * patch value.  (Note that this is trivial if getEntryBits() returns 8.)  This
 * is by intention, as patches are normally split into several
 * PatchTableMessages.
 */
public class PatchTableMessage extends RouteTableMessage {

    /*
     * For sequenceNumber and size, we really do need values of 0-255.
     * Java bytes are signed, of course, so we store shorts internally
     * and convert to bytes when writing.
     */

	/**
	 * The order this patch message appears in its group.
	 * The first patch message in a group is numbered 1, not 0.
	 * 
	 * In the Gnutella message payload that starts fNSCB, this value is at N.
	 */
    private short sequenceNumber;

	/**
	 * The number of patch messages that are in the group this patch message is a part of.
	 * All 3 patch messages in a group will have a sequenceSize of 3.
	 * 
	 * In the Gnutella message payload that starts fNSCB, this value is at S.
	 */
    private short sequenceSize;

	/**
	 * Whether or not the patch data in this message and the other messages in the group is compressed.
	 * 0x00 if it is not, 0x01 if it is.
	 * 
	 * In the Gnutella message payload that starts fNSCB, this value is at C.
	 */
    private byte compressor;

	/**
	 * The number of bits each value takes up, 4, a half byte, or 8, a full byte.
	 * 
	 * Patch table data only includes the values -6, 0, and 6, or -1, 0, and 1.
	 * QueryRouteTable.halve() and QueryRouteTable.unhalve() put these values in 4 bits instead of a whole byte.
	 * 
	 * In the Gnutella message payload that starts fNSCB, this value is at B.
	 */
    private byte entryBits;

    /*
     * TODO: I think storing payload here would be more efficient
     */

	/**
	 * The patch data this patch message holds.
	 * This is the data in the payload of this message that comes after fNSCB.
	 * 
	 * This is the next slice, up to 4 KB long, of probably compressed and halved QRP patch table data.
	 * QueryRouteTable.handlePatch() shows how to read and decode it.
	 */
    private byte[] data;

	/**
	 * 0x00, if the patch data in this group of patch messages isn't compressed, C at fNSCB will be 0x00.
	 */
    public static final byte COMPRESSOR_NONE = 0x0;

	/**
	 * 0x01, if the patch data in this group of patch messages is compressed, C at fNSCB will be 0x01.
	 */
    public static final byte COMPRESSOR_DEFLATE = 0x1;

    /*
     * /////////////////////////////// Encoding //////////////////////////////
     */

    /**
     * Make a new PatchTableMessage object to represent a QRP patch table message we're going to send to a remote computer.
     * Only QueryRouteTable.encode() calls this method.
     * 
     * @param sequenceNumber The sequence number, like 1, 2, 3, 4, this PatchTableMessage is in
     * @param sequenceSize   The number of QRP patch table messages, like 4, that make up this sequence and will transfer the whole table
     * @param compressor     0x01 if the table is deflate compressed, 0x00 if it's not
     * @param entryBits      The number of bits each value takes up, 4, a half byte, or 8, a full byte
     * @param dataSrc        The array that contains the data of the QRP table
     * @param dataSrcStart   For this PatchTableMessage, clip out the portion of the array from this starting distance
     * @param dataStcStop    For this PatchTableMessage, clip out the portion of the array up to this ending distance
     */
    public PatchTableMessage(short sequenceNumber, short sequenceSize, byte compressor, byte entryBits, byte[] dataSrc, int dataSrcStart, int dataSrcStop) {

    	/*
    	 * Payload length INCLUDES variant
    	 */

    	// Call the Message and RouteTableMessage constructors to save data that goes in the header
        super(
        	(byte)1,                          // 1 TTL, this message will only travel once
            5 + (dataSrcStop - dataSrcStart), // Payload length, 5 bytes followed by the table patch
            RouteTableMessage.PATCH_VARIANT); // First payload byte 0x01, identifying this as a PatchTableMessage

        // Save the given information
        this.sequenceNumber = sequenceNumber; // The number this message is in order, like 1, 2, 3, 4
        this.sequenceSize = sequenceSize;     // The total number of messages it will take to send the whole table, like 4
        this.compressor = compressor;         // 0x01 if the table is deflate compressed, 0x00 if it's not
        this.entryBits = entryBits;           // The number of bits each value takes up, 4, a half byte, or 8, a full byte

        /*
         * Copy dataSrc[dataSrcStart...dataSrcStop - 1] to data
         */

        // Copy the specified portion of the QRP table into this new object
        data = new byte[dataSrcStop - dataSrcStart];
        System.arraycopy(dataSrc, dataSrcStart, data, 0, data.length);
    }

    /**
     * Write the payload of this PatchTableMessage after the type byte to send the message to a remote computer.
     * 
     * You've already written the 23-byte message header, and the first payload byte, 0x01, identifying this as a PatchTableMessage.
     * This method writes the rest:
     * 
     * NSCB
     * 
     * N is the sequence number, like 1, 2, or 3 that tells where this patch message is ordered in its group.
     * S is the sequence size, like 3.
     * C is the compressor, 0x00 if the patch data isn't compressed, 0x01 if it is compressed.
     * B is the entry bits, 4 if the patch values take up 4 bits in the patch data, 8 if they each take up a full byte.
     * 
     * After that, it writes the patch data chunk, which is up to 4 KB.
     * 
     * @param out An OutputStream object we can call write() on to send data to the remote computer
     */
    protected void writePayloadData(OutputStream out) throws IOException {

    	// Make a byte array large enough to hold the 4 NSCB bytes and the patch data chunk after them
    	byte[] buf = new byte[4 + data.length];

    	// Write the 4 NSCB bytes
    	buf[0] = (byte)sequenceNumber;
        buf[1] = (byte)sequenceSize;
        buf[2] = (byte)compressor;
        buf[3] = (byte)entryBits;

        // Copy the patch data after them
        System.arraycopy(data, 0, buf, 4, data.length);

        // Send the data we composed to the remote computer
        out.write(buf);

        // Use this moment to record that we send another QRP patch message
        SentMessageStatHandler.TCP_PATCH_ROUTE_TABLE_MESSAGES.addMessage(this);
    }

    /*
     * /////////////////////////////// Decoding ///////////////////////////////
     */

    /**
     * Make a new PatchTableMessage object by parsing data a remote computer sent us.
     * 
     * Creates a new PATCH variant with data read from the network.
     * The first byte is guaranteed to be PATCH_VARIANT.
     * 
     * Throws BadPacketException the remaining values in payload are not
     * well-formed, e.g., because it's the wrong length, the sequence size
     * is less than the sequence number, etc.
     * 
     * @param guid    The message GUID we read from the header
     * @param ttl     The TTL we read from the message header
     * @param hops    The hops count we read from the message header
     * @param payload The payload of the message after that, like fNSCBpatchdatapatchdatapatchdata
     */
    protected PatchTableMessage(byte[] guid, byte ttl, byte hops, byte[] payload) throws BadPacketException {

    	// Save the GUID, TTL, hops and length in the Message and RouteTableMessage cores of this new PatchTableMessage
        super(guid, ttl, hops, payload.length, RouteTableMessage.PATCH_VARIANT); // 0x01 identifies this as a PatchTableMessage

        /*
         * TODO: maybe we shouldn't enforce this
         * if (payload.length < 5) throw new BadPacketException("Extra arguments in reset message.");
         */

        /*
         * The payload of a patch message looks like this:
         * 
         * fNSCBpatchdatapatchdata...
         */

        // Make sure the first payload byte, f, is 0x01, identifying this as a patch message
        Assert.that(payload[0] == PATCH_VARIANT);

        // Read the sequence number and sequence size, N and S, a distance of 1 and 2 bytes into the payload data
        this.sequenceNumber = (short)ByteOrder.ubyte2int(payload[1]);
        this.sequenceSize = (short)ByteOrder.ubyte2int(payload[2]);
        if (sequenceNumber < 1 || sequenceSize < 1 || sequenceNumber > sequenceSize) throw new BadPacketException("Bad sequence/size: " + sequenceNumber + "/" + sequenceSize);

        // Find out if the patch data is compressed by reading C 3 bytes into the payload data
        this.compressor = payload[3];
        if (!(compressor == COMPRESSOR_NONE || compressor == COMPRESSOR_DEFLATE)) throw new BadPacketException("Bad compressor: " + compressor);

        // Find out if the patch data was halved or not by reading B 4 bytes into the payload data
        this.entryBits = payload[4];
        if (entryBits < 0) throw new BadPacketException("Negative entryBits: " + entryBits);

        // Clip out the chunk of probably compressed and halved patch data this patch message carries after that
        this.data = new byte[payload.length - 5];           // It's the whole paylaod not including the first 4 fNSCB bytes we read
        System.arraycopy(payload, 5, data, 0, data.length); // Copy it from 5 bytes in to the end
    }

    /*
     * /////////////////////////////// Accessors ///////////////////////////////
     */

	/**
	 * Get the sequence number that places this patch message in its group.
	 * The first patch message in a group is numbered 1, not 0.
	 * In the Gnutella message payload that starts fNSCB, this value is at N.
	 * 
	 * @return The sequence number
	 */
    public short getSequenceNumber() {

    	// Return the value we saved or parsed
    	return sequenceNumber;
    }

	/**
	 * Get the number of patch messages that are in the group this patch message is a part of.
	 * All 3 patch messages in a group will have a sequenceSize of 3.
	 * In the Gnutella message payload that starts fNSCB, this value is at S.
	 * 
	 * @return The sequence size
	 */
    public short getSequenceSize() {

    	// Return the value we saved or parsed
        return sequenceSize;
    }

	/**
	 * Get the byte that tells whether or not the patch data in this message and the other messages in the group is compressed.
	 * 0x00 if it is not, 0x01 if it is.
	 * In the Gnutella message payload that starts fNSCB, this value is at C.
	 * 
	 * @return The compressor byte
	 */
    public byte getCompressor() {

    	// Return the value we saved or parsed
        return compressor;
    }

	/**
	 * Get the byte that tells how many bits each value takes up in the patch data once it's uncompressed.
	 * The value will be 4, a half byte, or 8, a full byte.
	 * 
	 * Patch table data only includes the values -6, 0, and 6, or -1, 0, and 1.
	 * QueryRouteTable.halve() and QueryRouteTable.unhalve() put these values in 4 bits instead of a whole byte.
	 * 
	 * In the Gnutella message payload that starts fNSCB, this value is at B.
	 * 
	 * @return The byte that tells how many bits each patch value takes up
	 */
    public byte getEntryBits() {

    	// Return the value we saved or parsed
        return entryBits;
    }

	/**
	 * Get the patch data this patch message holds.
	 * This is the data in the payload of this message that comes after fNSCB.
	 * 
	 * This is the next slice, up to 4 KB long, of probably compressed and halved QRP patch table data.
	 * QueryRouteTable.handlePatch() shows how to read and decode it.
	 * 
	 * @return The chunk of patch table data this message carries
	 */
    public byte[] getData() {

    	// Return the value we saved or parsed
        return data;
    }

    /**
     * Call recordDrop() to record that we're dropping this PatchTableMessage in the program's statistics.
     */
	public void recordDrop() {

  		// Give this PatchTableMessage object to the statistic handler for PatchTableMessage messages
		DroppedSentMessageStatHandler.TCP_PATCH_ROUTE_TABLE_MESSAGES.addMessage(this);
	}

  	/**
  	 * Express this PatchTableMessage as text.
  	 * Composes a string like:
  	 * 
  	 * "{PATCH, Sequence: 1/3, Bits: 4, Compr: 1, [<4096 bytes>]"
  	 * 
  	 * @return A String
  	 */
    public String toString() {

    	// Compose and return the text
        StringBuffer buf = new StringBuffer();
        buf.append("{PATCH, Sequence: " + getSequenceNumber() + "/" + getSequenceSize() + ", Bits: " + entryBits + ", Compr: " + getCompressor() + ", [");
        buf.append("<"+data.length+" bytes>");
        buf.append("]");
        return buf.toString();
    }
}
