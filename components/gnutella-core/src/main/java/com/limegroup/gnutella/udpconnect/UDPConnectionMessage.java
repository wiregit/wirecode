package com.limegroup.gnutella.udpconnect;

import java.io.*;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.BadPacketException;

/** Outline of message to allow a reliable udp protocol to be built on top of
 *  Gnutella messages.
 */
public abstract class UDPConnectionMessage extends Message {

    // Referenced from message
    // public static final byte F_UDP_CONNECTION = (byte)0x41;
    
    // Opcode identifiers for the sub-message types
    protected static final byte OP_SYN        = 0x0;
    protected static final byte OP_ACK        = 0x1;
    protected static final byte OP_KEEPALIVE  = 0x2;
    protected static final byte OP_DATA       = 0x3;
    protected static final byte OP_FIN        = 0x4;

    /** The number of bytes in a GUID   */
    protected static final int GUID_SIZE = 16;

    /** The maximum amount of data that can go into the GUID   */
    protected static final int MAX_GUID_DATA = 12;


    /** The start location of data embedded in the GUID   */
    protected static final int GUID_DATA_START = GUID_SIZE - MAX_GUID_DATA; 

	/** An empty byte array for internal use */
	protected static byte[] emptyByteArray = new byte[0];

    /** This is an identifier for a stream from a given IP. 1 byte   */
    protected byte _connectionID;

    /** This is the opcode for the sub-message type.        1 nibble */
    protected byte _opcode;

    /** The communications message sequence number.         2 bytes  */
    protected int _sequenceNumber;
    
    /** The first piece of data in this message.
        This will hold any data stored in the GUID.         14 bytes */
    protected byte _data1[];
    /** The offset of the data in data1.  4 in GUID data. 0 in raw data */
    protected int  _data1Offset;
    /** The amount of data used in the GUID.  Up to 12 bytes by design. */
    protected int  _data1Length;                           // 1 nibble 
    
    /** The second piece of data in this message.
        This will hold any data stored in the payload. 
        Up to 512 bytes by design. */
    protected byte _data2[];
    /** The offset of the data in data1.  
        0 in network payload.  12 in raw data. */
    protected int  _data2Offset;
    /** The usable length of data2.  Up to 500 bytes by design. */
    protected int  _data2Length;


    /**
     * Construct a new UDPConnectionMessage with the specified 
	 * settings and data.
     */
    protected UDPConnectionMessage(
      byte connectionID, byte opcode, int sequenceNumber, byte[] data ) 
      throws BadPacketException {

        super(
          /*guid*/   buildGUID(connectionID, opcode, sequenceNumber, data), 
          /*func*/   F_UDP_CONNECTION, 
          /*ttl*/    (byte)1, 
          /*hops*/   (byte)0, 
          /*length*/ calcPayloadLength(data));

        _connectionID   = connectionID;
        _opcode         = opcode;
        _sequenceNumber = sequenceNumber;
        _data1          = data;
        _data1Offset    = 0;
        _data1Length    = (data.length >= MAX_GUID_DATA ? MAX_GUID_DATA : 
                           data.length);
        _data2          = data;
        _data2Offset    = MAX_GUID_DATA;
        _data2Length    = getLength();
    }

    /**
     * Construct a new UDPConnectionMessage from the network.
     */
    protected UDPConnectionMessage(
      byte[] guid, byte ttl, byte hops, byte[] payload) 
      throws BadPacketException {

        super(
          /*guid*/   guid,
          /*func*/   F_UDP_CONNECTION, 
          /*ttl*/    ttl, 
          /*hops*/   hops, 
          /*length*/ payload.length);

        unpackFromWire(guid, payload);
    }

    /**
     *  Return the messages connectionID identifier.
     */
    public byte getConnectionID() {
        return _connectionID;
    }

    /**
     *  Return the messages sequence number
     */
	public int getSequenceNumber() {
		return _sequenceNumber;
	}

    /**
     * Allocate and fill in the data packed in the GUID.
     */
    private static byte[] buildGUID(byte connectionID, byte opcode, 
      int sequenceNumber, byte[] data) {
        int guidDataLength = (data.length >= MAX_GUID_DATA ? MAX_GUID_DATA : 
          data.length);

        // Fill up the GUID
        byte guid[] = new byte[GUID_SIZE];
        guid[0] = connectionID;
        guid[1] = (byte) 
          ((((opcode & 0x0f) << 4) | (((byte)guidDataLength) & 0x0f)));
        guid[2] = (byte)((sequenceNumber & 0xff00) >> 8);
        guid[3] = (byte)((sequenceNumber & 0x00ff));
        int end = GUID_DATA_START + guidDataLength;
        for ( int i = GUID_DATA_START; i < end; i++ ) {
            guid[i] = data[i - GUID_DATA_START];
        }
        return guid;
    } 

    /**
     * Calculate the payload length when the data is available in one array.
     */
    private static int calcPayloadLength(byte[] data) {
        return(data.length <= MAX_GUID_DATA ? 0 : data.length - MAX_GUID_DATA);
    }

    /**
     * Given the guid and the payload, recreate the in memory data structures
     */
    private void unpackFromWire(byte[] guid, byte[] payload) 
      throws BadPacketException {
        _connectionID   = guid[0];
        _opcode         = (byte)((int)(guid[1] & 0xf0) >> 4); 
        _sequenceNumber = 
          (((int) guid[2] & 0xff) << 8) | ((int) guid[3] & 0xff);

        _data1          = guid;
        _data1Offset    = GUID_DATA_START;
        _data1Length    = ((int) guid[1] & 0x0f); 
        _data2          = payload;
        _data2Offset    = 0;
        _data2Length    = payload.length;

        if ( _data1Length > MAX_GUID_DATA )
            throw new BadPacketException("GUID data too big");
    }

    /**
     * Create a byte array for data sending purposes.
     */
    public static byte[] buildByteArray(byte data) {
        byte[] darray = new byte[1];
        darray[0]     = data;

        return darray;
    } 


    /** 
     *  Return the length of data stored in this message.
     */
    protected int getDataLength() {
        return _data1Length + getLength();
    }

    /** 
     *  Output additional data as payload.
     */
    protected void writePayload(OutputStream out) throws IOException {
        if ( _data2Length > 0 )
            out.write(_data2, _data2Offset, _data2Length);
    }

    /** 
     *  There is no extended information.
     */
    public Message stripExtendedPayload() {
        return this;
    }

    /** 
     *  Drop nothing.
     */
    public void recordDrop() {
    }
}
