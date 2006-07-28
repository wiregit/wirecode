
// Edited for the Learning branch

package com.limegroup.gnutella.routing;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/**
 * 
 * 
 * The PATCH route table update message.  This class is as simple as possible.
 * For example, the getData() method returns the raw bytes of the message,
 * requiring the caller to call the getEntryBits() method to calculate the i'th
 * patch value.  (Note that this is trivial if getEntryBits() returns 8.)  This
 * is by intention, as patches are normally split into several
 * PatchTableMessages.
 * 
 * 
 */
public class PatchTableMessage extends RouteTableMessage {

    /*
     * For sequenceNumber and size, we really do need values of 0-255.
     * Java bytes are signed, of course, so we store shorts internally
     * and convert to bytes when writing.
     */

	/**  */
    private short sequenceNumber;

	/**  */
    private short sequenceSize;

	/**  */
    private byte compressor;

	/**  */
    private byte entryBits;

    /*
     * TODO: I think storing payload here would be more efficient
     */

	/**  */
    private byte[] data;

	/** 0x00 */
    public static final byte COMPRESSOR_NONE = 0x0;

	/** 0x01 */
    public static final byte COMPRESSOR_DEFLATE = 0x1;

    //done

    /*
     * /////////////////////////////// Encoding //////////////////////////////
     */

    /**
     * Make a new PatchTableMessage object to represent a QRP patch table message we're going to send to a remote computer.
     * Only QueryRouteTable.encode() calls this method.
     * 
     * A QRP table is 16 KB, and a patch table message holds a 4 KB chunk of it.
     * So, a QRP table will be sent in 4 patch table messages, numbered 1, 2, 3, 4.
     * The sequence numbers are 1, 2, 3, and 4, and the sequence size is 4 in each message.
     * If the table is compressed, it will be smaller and fit into fewer than 4 messages.
     * 
     * @param sequenceNumber The sequence number, like 1, 2, 3, 4, this PatchTableMessage is in
     * @param sequenceSize   The number of QRP patch table messages, like 4, that make up this sequence and will transfer the whole table
     * @param compressor     0x01 if the table is deflate compressed, 0x00 if it's not
     * @param entryBits      (do)
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
            RouteTableMessage.PATCH_VARIANT); // First payload byte 0x00, identifying this as a ResetTableMessage

        // Save the given information
        this.sequenceNumber = sequenceNumber; // The number this message is in order, like 1, 2, 3, 4
        this.sequenceSize = sequenceSize;     // The total number of messages it will take to send the whole table, like 4
        this.compressor = compressor;         // 0x01 if the table is deflate compressed, 0x00 if it's not
        this.entryBits = entryBits;           // (do)

        /*
         * Copy dataSrc[dataSrcStart...dataSrcStop - 1] to data
         */

        // Copy the specified portion of the QRP table into this new object
        data = new byte[dataSrcStop - dataSrcStart];
        System.arraycopy(dataSrc, dataSrcStart, data, 0, data.length);
    }

    //do

    /**
     * Write the payload of this PatchTableMessage after the type byte to send the message to a remote computer.
     * 
     * You've already written the 23-byte message header, and the first payload byte, 0x01, identifying this as a PatchTableMessage.
     * This method writes the rest:
     * 
     * NSCB
     * 
     * N is the sequence number
     * S is the sequence size
     * C is the compressor
     * B is the entry bits
     * 
     * 
     * 
     * Followed by the table data.
     * 
     * 
     * 
     * 
     * 
     * @param out An OutputStream object we can call write() on to send data to the remote computer
     */
    protected void writePayloadData(OutputStream out) throws IOException {

    	// Make a byte array large enough to hold the 4 NSCB bytes and the QRP table after them
    	byte[] buf = new byte[4 + data.length];

    	// Write the 4 NSCB bytes
    	buf[0] = (byte)sequenceNumber;
        buf[1] = (byte)sequenceSize;
        buf[2] = (byte)compressor;
        buf[3] = (byte)entryBits;

        // Copy the QRP table after them
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
     * Creates a new PATCH variant with data read from the network.
     * The first byte is guaranteed to be PATCH_VARIANT.
     * 
     * @exception BadPacketException the remaining values in payload are not
     *  well-formed, e.g., because it's the wrong length, the sequence size
     *  is less than the sequence number, etc.
     */
    protected PatchTableMessage(byte[] guid, byte ttl, byte hops, byte[] payload) throws BadPacketException {

        super(guid, ttl, hops, payload.length, RouteTableMessage.PATCH_VARIANT);

        /*
         * TODO: maybe we shouldn't enforce this
         * if (payload.length < 5) throw new BadPacketException("Extra arguments in reset message.");
         */

        Assert.that(payload[0] == PATCH_VARIANT);

        this.sequenceNumber = (short)ByteOrder.ubyte2int(payload[1]);
        this.sequenceSize = (short)ByteOrder.ubyte2int(payload[2]);
        if (sequenceNumber < 1 || sequenceSize < 1 || sequenceNumber>sequenceSize) throw new BadPacketException("Bad sequence/size: " + sequenceNumber + "/" + sequenceSize);

        this.compressor = payload[3];
        if (!(compressor == COMPRESSOR_NONE || compressor == COMPRESSOR_DEFLATE)) throw new BadPacketException("Bad compressor: " + compressor);

        this.entryBits = payload[4];
        if (entryBits < 0) throw new BadPacketException("Negative entryBits: " + entryBits);

        this.data = new byte[payload.length - 5];

        System.arraycopy(payload, 5, data, 0, data.length);
    }

    /*
     * /////////////////////////////// Accessors ///////////////////////////////
     */

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

	// inherit doc comment
	public void recordDrop() {

		DroppedSentMessageStatHandler.TCP_PATCH_ROUTE_TABLE_MESSAGES.addMessage(this);
	}

    public String toString() {

        StringBuffer buf = new StringBuffer();

        buf.append("{PATCH, Sequence: " + getSequenceNumber() + "/" + getSequenceSize() + ", Bits: " + entryBits + ", Compr: " + getCompressor() + ", [");
        buf.append("<"+data.length+" bytes>");
        buf.append("]");
        return buf.toString();
    }
}
