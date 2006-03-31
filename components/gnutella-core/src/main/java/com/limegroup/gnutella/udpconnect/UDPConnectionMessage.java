
// Commented for the Learning branch

package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;

/**
 * UDPConnectionMessage outlines the structure of Gnutella messages sent over UDP in LimeWire's UDP connection feature.
 * This class extends Message, gaining the basic structure and features of a Gnutella message.
 * 
 * The UDPConnectionMessage class is abstract to prevent the program from making a UDPConnectionMessage object.
 * Instead, make objects like AckMessage and DataMessage that represent specific kinds of UDP connection messages.
 * These classes extend UDPConnectionMessage.
 * 
 * The bytes of a UDP connection message are arranged like this:
 * 
 * aa bc dd dd ee ee ee ee
 * ee ee ee ee ee ee ee ee
 * ff gg hh ii ii ii ii jj
 * jj jj jj jj jj jj jj jj
 * jj...
 * 
 * The program reads the first 23 bytes, shown as a through i, as a Gnutella message header.
 * The first 16 bytes, shown as a through e, are where the GUID of a normal Gnutella message would be.
 * 
 * a is the connection ID, all the packets that make up a stream have the same connection ID.
 * b and c are 4-bit nibbles that fit together into 1 byte.
 * b is the UDP connection operation code, like 0x0 OP_SYN or 0x3 OP_DATA, that tells what kind of UDP connection packet this is.
 * c is the length of data hidden in the GUID, up to 12 bytes shown as e.
 * d is the sequence number, put the packets in this order to read them as a stream.
 * e is the first 12 bytes of data.
 * 
 * f is the Gnutella message type, like 0x80 Message.F_QUERY, here it's 0x41 Message.F_UDP_CONNECTION.
 * g and h are the TTL and hops count, which are 1 and 0, and not used.
 * i is the length of payload data that that follows, shown as j.
 * j is the payload data.
 * 
 * A UDP connection message keeps its data in both e and j.
 * e can hold 12 bytes, more data goes in j.
 * c holds e's length, and i holds j's length.
 */
public abstract class UDPConnectionMessage extends Message {

    /*
     * Inherited from Message:
     * public static final byte F_UDP_CONNECTION = (byte)0x41;
     * 
     * The AckMessage, DataMessage, FinMessage, KeepAliveMessage, and SynMessage packets are have 0x41 16 bytes into the packet.
     * These classes extend UDPConnectionMessage, which extends Message.
     */

    /*
     * The version number of the protocol to allow for future improvements
     */

    /**
     * 0, this is the first version of the firewall-to-firewall transfer protocol.
     * A SynMessage includes a 0 to indicate this.
     * In the future, we may improve the protocol in a way that changes it.
     * This number lets us do that.
     */
    public static final int PROTOCOL_VERSION_NUMBER = 0;

    /*
     * Opcode identifiers for the sub-message types
     */

    /** 0x0, the 4-bit code that identifies a SynMessage. */
    protected static final byte OP_SYN       = 0x0;
    /** 0x1, the 4-bit code that identifies a AckMessage. */
    protected static final byte OP_ACK       = 0x1;
    /** 0x2, the 4-bit code that identifies a KeepAliveMessage. */
    protected static final byte OP_KEEPALIVE = 0x2;
    /** 0x3, the 4-bit code that identifies a DataMessage. */
    protected static final byte OP_DATA      = 0x3;
    /** 0x4, the 4-bit code that identifies a FinMessage. */
    protected static final byte OP_FIN       = 0x4;

    /** 16, the GUID at the start of a Gnutella packet is 16 bytes. */
    protected static final int GUID_SIZE = 16;

    /** 12, we'll put 12 bytes of data into the space at the start where the GUID should be. */
    protected static final int MAX_GUID_DATA = 12;

    /** 4, we'll put 12 bytes of data 4 bytes into the 16-byte GUID at the start. */
    protected static final int GUID_DATA_START = GUID_SIZE - MAX_GUID_DATA;

	/** A byte array with length 0. */
	protected static byte[] emptyByteArray = new byte[0];

    /** The connection ID, all the packets that make up a stream have the same connection ID. */
    protected byte _connectionID;

    /** The number that identifies what kind of UDP connection message this is, like 0x1 AckMessage or 0x3 DataMessage. */
    protected byte _opcode;

    /**
     * Put the messages into order with this sequence number to read their data as a stream.
     * In the data of a message, the sequence number is 2 bytes.
     * The SequenceNumberExtender uses previous information to figure out the complete 8-byte number, and extendSequenceNumber(n) sets it.
     */
    protected long _sequenceNumber;

    /** The start of the data this UDP connection message holds, up to 12 bytes that fit into the GUID area in the Gnutella packet header. */
    protected byte _data1[];

    /** Look at the contents of _data1 from _data1Offset onwards. */
    protected int _data1Offset;

    /** There are _data1Length bytes of data in _data1 at _data1Offset. */
    protected int _data1Length;

    /** The data this UDP connection message holds, after the 12 bytes we fit in the GUID area. */
    protected byte _data2[];

    /** Look at the contents of _data2 from _data2Offset onwards. */
    protected int _data2Offset;

    /** There are _data2Length of bytes of data in _data2 after _data2Offset. */
    protected int _data2Length;

    /** createMessage() throws the BadPacketException NO_MATCH when it gets a packet with a type code it doesn't recognize. */
    private static final BadPacketException NO_MATCH = new BadPacketException("No matching UDPConnectionMessage");

    /**
     * Parse data we've received into an object based on what type of UDP connection packet it is, like a SynMessage object or a DataMessage object.
     * Leads to the message parser.
     * 
     * Message.createMessage() calls this.
     * We've received data from a remote computer, read Gnutella a header and payload, and parsed the header.
     * The type byte 16 bytes into the GUID was 0x41, labeling this as a UDP connection message.
     * 
     * Calls a message type-specific constructor, like SynMessage().
     * That constructor will call super(), which jumps back to the UDPConnectionMessage() constructor in this class.
     * 
     * @param  guid    The first 16 bytes of the header data we read, where the GUID should be
     * @param  ttl     The TTL byte 17 bytes into the header
     * @param  hops    The hops count 18 bytes into the header
     * @param  payload The message payload data beyond the 23-byte header
     * @return         A new object specific to the type of packet we read, like a SynMessage object or a DataMessage object
     */
    public static UDPConnectionMessage createMessage(byte[] guid, byte ttl, byte hops, byte[] payload) throws BadPacketException {

        // Read the UDP connection message type number, which is the high 4 bits of the second byte in the header
        byte opcode = (byte)((int)(guid[1] & 0xf0) >> 4); // guid[1] gets the second byte, & 0xf0 masks out the high half, and >> 4 shifts those bits to the number

        // Create appropriate UDPConnectionMessage
        switch (opcode) {
        case OP_SYN:       return new SynMessage(guid, ttl, hops, payload);       // 0x0, a SYN message
        case OP_ACK:       return new AckMessage(guid, ttl, hops, payload);       // 0x1, a ACK message
        case OP_KEEPALIVE: return new KeepAliveMessage(guid, ttl, hops, payload); // 0x2, a Keep Alive message
        case OP_DATA:      return new DataMessage(guid, ttl, hops, payload);      // 0x3, a Data message
        case OP_FIN:       return new FinMessage(guid, ttl, hops, payload);       // 0x4, a FIN message
        }

        // The data contains a type code we don't recognize
        throw NO_MATCH;
    }

    /**
     * Make a new UDPConnectionMessage object with the given IDs and data.
     * This is the message maker.
     * 
     * The specific UDP connection message type constructors, like SynMessage, FinMessage, and DataMessage call this constructor.
     * It sets up the member variables UDPConnectionMessage holds, and inherits from Message.
     * The program never makes a UDPConnectionMessage object, and just makes objects like SynMessage that extend UDPConnectionMessage.
     * 
     * @param connectionID   A number that identifies the connection this packet is a part of
     * @param opcode         The code that tells what kind of UDP connection packet this is
     * @param sequenceNumber This packet's number, placing it in order with the others that are sent seprately
     * @param data           The data to put in this packet
     * @param datalength     The length of the data
     */
    protected UDPConnectionMessage(byte connectionID, byte opcode, long sequenceNumber, byte[] data, int datalength) {

        // Call the Message constructor to set member variables for information located in the 23-byte Gnutella message header
        super(
            buildGUID(connectionID, opcode, sequenceNumber, data, datalength), // Compose the first 16 bytes of the message, and save that as the GUID
            F_UDP_CONNECTION,                                                  // 0x41, this is a UDP connection message
            (byte)1,                                                           // 1 TTL and 0 hops, the default for a UDP connection message
            (byte)0,
            calcPayloadLength(datalength));                                    // Calculate the payload length, datalength - 12, not going below 0

        // Save the given information in this UDPConnectionMessage object
        _connectionID   = connectionID;
        _opcode         = opcode;
        _sequenceNumber = sequenceNumber;

        /*
         * Point _data1 and _data2 at the same given data byte array.
         * Set _data1Offset and _data1Length to clip out the first 12 bytes of data.
         * Set _data2Offset and _data2Length to clip out the data after that.
         * The lengths are measured from the start of the array, not from the offset.
         */

        // Make _data1, _data1Offset, and _data1Length clip out the up to 12 bytes of data we can put in the GUID area
        _data1       = data;                                                       // Point _data1 at the whole array of data
        _data1Offset = 0;                                                          // The data starts at the start of the array
        _data1Length = (datalength >= MAX_GUID_DATA ? MAX_GUID_DATA : datalength); // Look at up to 12 bytes there

        // Make _data2, _data2Offset, and _data2Length clip out the rest of the data that will go in the payload
        _data2       = data;          // Point _data2 at the same whole array of data
        _data2Offset = MAX_GUID_DATA; // The data starts 12 bytes into the array
        _data2Length = getLength();   // There is getLength() data there
    }

    /**
     * Parse data we've received into an object based on what type of UDP connection packet it is, like a SynMessage object or a DataMessage object.
     * This is the message parser.
     * 
     * The message type-specific constructors like SynMessage() and AckMessage() call this.
     * We've received data from a remote computer, read Gnutella a header and payload, and parsed the header.
     * The type byte 16 bytes into the GUID was 0x41, labeling this as a UDP connection message.
     * 
     * @param guid    The first 16 bytes we read
     * @param ttl     The TTL byte we read
     * @param hops    The hops byte we read
     * @param payload The payload data beyond the 23-byte Gnutella message header
     */
    protected UDPConnectionMessage(byte[] guid, byte ttl, byte hops, byte[] payload) throws BadPacketException {

        // Call the Message constructor to set member variables for information located in the 23-byte Gnutella message header
        super(
            guid,             // The first 16 bytes of the message header we read
            F_UDP_CONNECTION, // 0x41, this is a UDP connection message
            ttl,              // The TTL we read
            hops,             // The hops count we read
            payload.length);  // The payload length we read

        // Look in the first 16 bytes we read and in the payload beyond the 23-byte header to set the member variables for this new UDPConnectionMessage object
        unpackFromWire(guid, payload);
    }

    /**
     * The connection ID the receiving computer chose to identify the sending computer and their connection.
     * All the UDP packets that make up a stream of data have the same ID.
     * This lets the receiving computer keep them separate from other UDP packets.
     * The connection ID is the first byte in the header.
     * 
     * @return The connection ID this UDP connection message is a part of
     */
    public byte getConnectionID() {

        // Return the byte we saved or parsed
        return _connectionID;
    }

    /**
     * This UDP connection message's sequence number.
     * The sequence number lets the receiving computer put the messages back in order to read their data as a stream again.
     * In the data, the sequence number is located 2 bytes into the packet, and takes up 2 bytes.
     * The extendSequenceNumber() method sets a new sequence number, which is an 8 byte long.
     * 
     * @return The sequence number of this UDP connection message
     */
	public long getSequenceNumber() {

        // Return the sequence number we saved or parsed
		return _sequenceNumber;
	}

    /**
     * Set this UDP connection message's sequence number from the 2 byte number we read to the given full 8 byte number.
     * 
     * UDPConnectionProcessor.handleMessage() calls this.
     * The SequenceNumberExtender keeps track of previous information to turn the 2 byte part of the number in the packet into the full 8 byte number.
     * 
     * @param seqNo The full sequence number
     */
	public void extendSequenceNumber(long seqNo) {

        // Save the given sequence number
		_sequenceNumber = seqNo;
	}

    /**
     * Compose the first 16 bytes of a UDP Connection message, where the GUID in a Gnutella packet should be.
     * 
     * All Gnutella messages begin with a 16-byte GUID.
     * UDP Connect messages don't need to have a GUID, so they use the space for the GUID to IDs, lengths, and the first 12 byts of the data.
     * The first 16 bytes of a UDP Connect message look like this:
     * 
     * aa bc dd dd ee ee ee ee
     * ee ee ee ee ee ee ee ee
     * 
     * a is the connection ID, all the packets from a sending computer have the same connection ID so the receiving computer can keep them separate from other computers.
     * b and c are 4 bits each, and fit into 1 byte together.
     * b is the operation code, the number that tells what kind of UDP Connect message this is.
     * c is the length of data we're hiding in the GUID, in the area e, up to 12 bytes.
     * d is the sequence number, the receiving computer puts packets with the same connection ID in order with this number, recreating the stream.
     * e is the data, we put the first 12 bytes of data in this part of where the GUID should be.
     * 
     * buildGUID() composes and returns 16 bytes like this.
     * 
     * @param connectionID   A 1 byte number that identifies the connection this packet is a part of
     * @param opcode         A half byte code that tells what kind of packet this is
     * @param sequenceNumber A 2 byte number that tells what order this packet should go in
     * @param data           The packet payload data, we'll take the first 12 bytes of it
     * @param datalength     The length of the payload data
     * @return               A 16-byte array with that information written inside
     */
    private static byte[] buildGUID(byte connectionID, byte opcode, long sequenceNumber, byte[] data, int datalength) {

        // guidDataLength is the number of bytes of payload data we'll put in the space where the GUID should be
        int guidDataLength = (datalength >= MAX_GUID_DATA ? MAX_GUID_DATA : datalength); // Make it 12 or less

        // Compose the 16 bytes for the start of the message
        byte guid[] = new byte[GUID_SIZE];                                            // Make a new 16-byte array
        guid[0] = connectionID;                                                       // The first byte is the connection ID
        guid[1] = (byte)((((opcode & 0x0f) << 4) | (((byte)guidDataLength) & 0x0f))); // The operation code and data length are both in the second byte
        guid[2] = (byte)((sequenceNumber & 0xff00) >> 8);                             // The sequence number is in the next 2 bytes
        guid[3] = (byte)((sequenceNumber & 0x00ff));

        // Loop for each of the remaining 12 bytes
        int end = GUID_DATA_START + guidDataLength;   // end points from the start of the 16 byte array to the end of the data we'll put in it
        for (int i = GUID_DATA_START; i < end; i++) { // Loop i from 4 to this end, i is an index in the 16-byte array we're composing

            // Copy the first 12 bytes of data into our 16-byte array
            guid[i] = data[i - GUID_DATA_START];
        }

        // Return the 16-byte array we composed, put it in the place for a GUID at the start of the Gnutella message
        return guid;
    }

    /**
     * Calculate how much of the data in a UDP connection message spills over the 12 bytes we can put in the GUID area at the start.
     * This is the amount that will end up in the Gnutella message payload, and be sized by the length in the Gnutella message header.
     * 
     * If the given length is 12 or less, returns 0.
     * If the given length is more than 12, returns how much more.
     * 
     * @param datalength The number of bytes of data in a UDP connection message
     * @return           The number of bytes in the message payload beyond the first 12 in the GUID area
     */
    private static int calcPayloadLength(int datalength) {

        // If the given number is bigger than 12, return it minus 12
        return (datalength <= MAX_GUID_DATA ? 0 : datalength - MAX_GUID_DATA);
    }

    /**
     * Given the first 16 bytes of a Gnutella message we read and the payload beyond 23 bytes, parse and read this UDP connection message.
     * The bytes of a UDP Connection message are arranged like this:
     * 
     * aa bc dd dd ee ee ee ee
     * ee ee ee ee ee ee ee ee
     * ff gg hh ii ii ii ii jj
     * jj jj jj jj jj jj jj jj
     * jj...
     * 
     * The program reads the first 23 bytes, shown as a through i, as a Gnutella message header.
     * The first 16 bytes, shown as a through e, are where the GUID of a normal Gnutella message would be.
     * 
     * a is the connection ID, all the packets that make up a stream have the same connection ID.
     * b and c are 4-bit nibbles that fit together into 1 byte.
     * b is the UDP connection operation code, like 0x0 OP_SYN or 0x3 OP_DATA, that tells what kind of UDP connection packet this is.
     * c is the length of data hidden in the GUID, up to 12 bytes shown as e.
     * d is the sequence number, put the packets in this order to read them as a stream.
     * e is the first 12 bytes of data.
     * 
     * f is the Gnutella message type, like 0x80 Message.F_QUERY, here it's 0x41 Message.F_UDP_CONNECTION.
     * g and h are the TTL and hops count, which are 1 and 0, and not used.
     * i is the length of payload data that that follows, shown as j.
     * j is the payload data.
     * 
     * Both e and j are data.
     * e can hold 12 bytes, more data goes in j.
     * c holds e's length, and i holds j's length.
     * 
     * @param guid    The first 16 bytes of the Gnutella message header data, where the GUID would be in a traditional Gnutella message
     * @param payload The bytes of the Gnutella message payload after that
     */
    private void unpackFromWire(byte[] guid, byte[] payload) throws BadPacketException {

        // Set member variables for this new UDPConnectionMessage object
        _connectionID   = guid[0];                                                  // Save the connection ID, a, 1 byte
        _opcode         = (byte)((int)(guid[1] & 0xf0) >> 4);                       // Save the operation code, b, a half a byte
        _sequenceNumber = (((long) guid[2] & 0xff) << 8) | ((long) guid[3] & 0xff); // Save the sequence number, d, 2 bytes

        // Set _data1, _data1Offset, and _data1Length to clip out the up to 12 bytes of data in the GUID area
        _data1       = guid;                  // Point _data1 at the given 16 byte array
        _data1Offset = GUID_DATA_START;       // The up to 12 bytes of data start 4 bytes into the 16 byte array
        _data1Length = ((int)guid[1] & 0x0f); // The lower half of the second byte numbers how many of these bytes contain data

        // Set _data2, _data2Offset, and _data2Length to clip out the data this packet carries beyond the first 12 bytes
        _data2       = payload;        // Point _data2 at payload, the data we read beyond the 23-byte header
        _data2Offset = 0;              // Set the offset and length to clip out the whole byte array
        _data2Length = payload.length;

        // Make sure the length nibble doesn't contain a number bigger than 12
        if (_data1Length > MAX_GUID_DATA) throw new BadPacketException("GUID data too big");
    }

    /**
     * Put a byte into a 1-byte array.
     * 
     * @param data A byte of data
     * @return     The data byte in a 1-byte array
     */
    public static byte[] buildByteArray(byte data) {

        // Make a 1-byte array, put the given byte in it, and return it
        byte[] darray = new byte[1];
        darray[0] = data;
        return darray;
    }

    /**
     * Put a byte and an int into a 3-byte array.
     * 
     * @param val1 A byte
     * @param val2 An int
     * @return     A 3-byte array with the byte in the first byte, and the int in the next 2 bytes
     */
    public static byte[] buildByteArray(byte val1, int val2) {

        // Make a 3-byte array
        byte[] darray = new byte[3];

        // Put the given byte at the start
        darray[0] = val1;

        // Put the value of the given int in the next 2 bytes
        darray[1] = (byte)((val2 & 0xff00) >> 8);
        darray[2] = (byte)((val2 & 0x00ff));

        // Return the array we made and filled
        return darray;
    }

    /**
     * Put two ints into a 4-byte array.
     * 
     * @param val1 An int
     * @param val2 A second int
     * @return     A 4-byte array with the two int values in 2 bytes each
     */
    public static byte[] buildByteArray(int val1, int val2) {

        // Make a 4-byte array
        byte[] darray = new byte[4];

        // Put the val1 int in the first 2 bytes
        darray[0] = (byte)((val1 & 0xff00) >> 8);
        darray[1] = (byte)((val1 & 0x00ff));

        // Put the value of the val2 int in the last 2 bytes
        darray[2] = (byte)((val2 & 0xff00) >> 8);
        darray[3] = (byte)((val2 & 0x00ff));

        // Return the array we made and filled
        return darray;
    }

    /**
     * Read 2 unsigned bytes as an int.
     * 
     * @param b1 A byte with the high 8 bits of a number
     * @param b2 A byte with the low 8 bits of a number
     * @return   The number
     */
    public static int getShortInt(byte b1, byte b2) {

        // Mask, shift, and combine the bytes into an int value, and return it
        return (((int)b1 & 0xff) << 8) | ((int)b2 & 0xff);
    }

    /**
     * The number of bytes of data this UDP connection message holds.
     * A UDP connection message can hold 12 bytes in the GUID area, and more in the payload.
     * 
     * @return The total number of bytes of data this message is carrying
     */
    public int getDataLength() {

        // Add the number of bytes of data we stored in the GUID to the message payload length
        return _data1Length + getLength();
    }

    /**
     * Write the payload of this UDP connection Gnutella message to the given output stream.
     * 
     * @param out The OutputStream object we can call out.write(data) on to give it data
     */
    protected void writePayload(OutputStream out) throws IOException {

        // If this UDP connection packet holds more than 12 bytes of data
        if (_data2Length > 0) {

            // Write _data2, the part of the data after the 12 bytes we put in the GUID area that we'll put in the payload
            out.write(_data2, _data2Offset, _data2Length);
        }
    }

    /**
     * A UDP connection message doesn't have any extended information to remove.
     * 
     * @return A reference to this same object
     */
    public Message stripExtendedPayload() {

        // Return a reference to this same object, unchanged
        return this;
    }

    /** Does nothing. */
    public void recordDrop() {}
}
