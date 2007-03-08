
// Commented for the Learning branch

package com.limegroup.gnutella.routing;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/**
 * A ResetTableMessage object represents a QRP reset table message that we've received or are going to send.
 * A Gnutella program sends a QRP reset table message one time, after the connection is made and before groups of patch messages.
 * The reset message states the table size and the infinity value.
 * 
 * A QRP reset table message is 29 bytes:
 * 
 * aaaaaaaa
 * aaaaaaaa
 * bcdeeeef
 * tttti
 * 
 * a is the 16 byte message GUID.
 * b is 0x30, the byte code for a QRP message.
 * c is the TTL, 1.
 * d is the hops, 0.
 * e is the length of the payload that follows, 6.
 * 
 * f is the first byte of the payload, 0x00 identifes this as a reset message.
 * t is the table size, like 65536, an int written in little endian order.
 * i is the infinity, like 7.
 * 
 * LimeWire uses a table size of 65536 bytes, which is 64 KB.
 * The infinity TTL is 7 for historical reasons.
 * The payload size is always 6.
 * QRP messages aren't relayed on the network, and start out with a TTL of 1 and a hops of 0.
 */
public class ResetTableMessage extends RouteTableMessage {

	/**
	 * The number of values the QRP table can hold.
	 * LimeWire and current Gnutella programs make QRP tables that hold 65536 bits of information.
	 * 
	 * This means that a BitSet used to describe the QRP table will have 65536 bits.
	 * Uncompressed table data uses a full byte to describe each table entry.
	 * This means the whole table will take up 65536 bytes, which is 64 KB.
	 */
    private int tableSize;

    /**
     * The infinity TTL.
     * LimeWire sends QRP tables with the infinity set to 7.
     * Most modern Gnutella programs use 7, while others use 2.
     * 
     * A QRP table's infinity defines what values in the table will mean.
     * The value that lets a search through will be 1 - infinity.
     * The value that blocks a search will be infinity - 1.
     * For instance, if the infinity is 7, -6 will let a search through and 6 will block it.
     * For an infinity of 2, -1 will let a search through and 1 will block it.
     * 
     * The value 0 always means no change.
     */
    private byte infinity;

    /*
     * /////////////////////////////// Encoding //////////////////////////////
     */

    /**
     * Make a new ResetTableMessage for us to send.
     * 
     * Creates a new ResetTableMessage from scratch, with TTL 1. The
     * receiver should initialize its routing table to a tableSize array
     * with values of infinity.  Throws IllegalArgumentException if
     * either value is less than 1.
     * infinity is the smallest value in the route table for infinity,
     * i.e., one more than the max TTL
     * 
     * @param tableSize The size of the QRP table in bytes, 63356 bytes, which is 64 KB
     * @param inifinity A TTL of 7 for historical reasons
     */
    public ResetTableMessage(int tableSize, byte infinity) {

    	/*
    	 * Payload length includes variant
    	 */

    	// Call the Message and RouteTableMessage constructors to save data that goes in the header
    	super(
        	(byte)1,                          // TTL 1, we'll only send this message 1 hop
        	1 + 4 + 1,                        // Payload length 6, FTTTTI takes up 6 bytes
        	RouteTableMessage.RESET_VARIANT); // First payload byte 0x00, identifying this as a ResetTableMessage

        // Save the table size and infinity
        if (tableSize < 1 || infinity < 1) throw new IllegalArgumentException("Argument too small: " + tableSize + ", " + infinity);
        this.tableSize = tableSize;
        this.infinity = infinity;
    }

    /**
     * Write the payload of this ResetTableMessage after the type byte to send the message to a remote computer.
     * 
     * You've already written the 23-byte message header, and the first payload byte, 0x00, identifying this as a ResetTableMessage.
     * This method writes the rest:
     * 
     * TTTTI
     * 
     * T is the table size, 65536, an int written in little endian order.
     * I is the infinity value, 7.
     * 
     * @param out An OutputStream object we can call write() on to send data to the remote computer
     */
    protected void writePayloadData(OutputStream out) throws IOException {

    	// Make a 5-byte buffer, and write TTTTI in it
        byte[] buf = new byte[5];
        ByteOrder.int2leb(tableSize, buf, 0); // TTTT, the table size, 65536 bytes, written in 4 bytes in little endian order
        buf[4] = infinity;                    // I, the infinity TTL, 7 for historical reasons

        // Write the last 5 bytes to the given output stream, now the whole message is in there
        out.write(buf);

        // Record that we sent another RouteTableMessage in statistics
        SentMessageStatHandler.TCP_RESET_ROUTE_TABLE_MESSAGES.addMessage(this);
    }

    /*
     * /////////////////////////////// Decoding ///////////////////////////////
     */

    /**
     * Make a new ResetTableMessage object by parsing data a remote computer sent us.
     * 
     * Creates a new ResetTableMessage with data read from the network. The
     * payload argument is the complete payload of the message. The first byte
     * is guaranteed to be RESET_VARIANT.
     * 
     * @param guid    The message GUID we read from the header
     * @param ttl     The TTL we read from the message header
     * @param hops    The hops count we read from the message header
     * @param payload The 6-byte payload of the message, like FTTTTI, with the type F, length T, and infinity I
     */
    protected ResetTableMessage(byte[] guid, byte ttl, byte hops, byte[] payload) throws BadPacketException {

    	// Save the GUID, TTL, hops and length in the Message and RouteTableMessage cores of this new ResetTableMessage
        super(guid, ttl, hops, payload.length, RouteTableMessage.RESET_VARIANT); // 0x00 identifies this as a ResetTableMessage

        /*
         * TODO: maybe we shouldn't enforce this
         * if (payload.length != (2 + 4)) throw new BadPacketException("Extra arguments in reset message.");
         */

        // Read the table size T from the middle of FTTTTI, probably 65536
        tableSize = ByteOrder.leb2int(payload, 1);

        // Read the infinity TTL from the end of FTTTTI, probably 7, you've also seen a program send 2
        infinity = payload[5];
    }

    /*
     * /////////////////////////////// Accessors ///////////////////////////////
     */

    /**
     * Get the infinity TTL of the QRP table this ResetTableMessage is resetting.
     * If we made this ResetTableMessage, it will be 7.
     * Most Gnutella programs on the network send 7, although you have also seen 2.
     * 
     * This is the smallest value in the route table for infinity, i.e., one more than the max TTL.
     * 
     * @return The infinity TTL the QRP table will use, probably 7
     */
    public byte getInfinity() {

    	// Return the infinity TTL we set or parsed
        return infinity;
    }

    /**
     * The number of bits of information the QRP table will have.
     * 
     * Before sending a QRP table, a Gnutella program sends a reset table message.
     * This reset message tells the size of the QRP table that will follow.
     * 
     * If we made this ResetTableMessage, the size will be 65536.
     * 
     * Internally, LimeWire keeps QRP tables as BitSet objects, meaning this size is 65536 bits.
     * These bits are not sent over the wire, rather, they are sent in the data of patch table messages.
     * 
     * @return The number of bits of information the QRP table will have, like 65536
     */
    public int getTableSize() {

    	// Return the size we set or parsed
    	return tableSize;
    }

    /**
     * Call recordDrop() to record that we're dropping this ResetTableMessage in the program's statistics.
     */
  	public void recordDrop() {

  		// Give this ResetTableMessage object to the statistic handler for ResetTableMessage messages
  		DroppedSentMessageStatHandler.TCP_RESET_ROUTE_TABLE_MESSAGES.addMessage(this);
  	}

  	/**
  	 * Express this ResetTableMessage as text.
  	 * Composes a string like:
  	 * 
  	 * "{RESET, tableSize: 65536, Infinity: 7}"
  	 * 
  	 * @return A String
  	 */
    public String toString() {

    	// Compose and return the text
        return "{RESET, tableSize: " + getTableSize() + ", Infinity: " + getInfinity() + "}";
    }
}
