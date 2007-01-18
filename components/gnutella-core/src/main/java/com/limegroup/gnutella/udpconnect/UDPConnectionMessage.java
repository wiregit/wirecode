package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.limewire.nio.BufferUtils;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;

/** Outline of message to allow a reliable udp protocol to be built on top of
 *  Gnutella messages.
 */
public abstract class UDPConnectionMessage extends Message {

    // Referenced from message
    // public static final byte F_UDP_CONNECTION = (byte)0x41;

    // The version number of the protocol to allow for future improvements
    public static final int PROTOCOL_VERSION_NUMBER = 0;
    
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
    protected long _sequenceNumber;
    
    /** The first piece of data in this message.
        This will hold any data stored in the GUID.         14 bytes */
    protected ByteBuffer _data1; 
    
    /** The second piece of data in this message.
        This will hold any data stored in the payload. 
        Up to 512 bytes by design. */
    protected ByteBuffer _data2;

    private static final BadPacketException NO_MATCH = 
      	new BadPacketException("No matching UDPConnectionMessage");

    public static UDPConnectionMessage createMessage(
      byte[] guid, byte ttl, byte hops, byte[] payload) 
      throws BadPacketException {
        byte opcode         = (byte)((guid[1] & 0xf0) >> 4); 
 
        // Create appropriate UDPConnectionMessage
        switch (opcode) {
    		case OP_SYN:
                return new SynMessage(guid,ttl,hops, payload);
    		case OP_ACK:
                return new AckMessage(guid,ttl,hops, payload);
    		case OP_KEEPALIVE:
                return new KeepAliveMessage(guid,ttl,hops, payload);
    		case OP_DATA:
                return new DataMessage(guid,ttl,hops, payload);
    		case OP_FIN:
                return new FinMessage(guid,ttl,hops, payload);
        } 
      	throw NO_MATCH;
	}


    /**
     * Construct a new UDPConnectionMessage with the specified 
	 * settings and data.
     */
    protected UDPConnectionMessage(
      byte connectionID, byte opcode, long sequenceNumber, byte[] data,
	  int  datalength ) {

        super(
          /*guid*/   buildGUID(connectionID, opcode, sequenceNumber, 
					   data, datalength), 
          /*func*/   F_UDP_CONNECTION, 
          /*ttl*/    (byte)1, 
          /*hops*/   (byte)0, 
          /*length*/ calcPayloadLength(datalength));

        _connectionID   = connectionID;
        _opcode         = opcode;
        _sequenceNumber = sequenceNumber;
        if(datalength > 0)
            _data1 = ByteBuffer.wrap(data, 0, 
                    (datalength >= MAX_GUID_DATA ? MAX_GUID_DATA : 
                        datalength));
        else
            _data1 = BufferUtils.getEmptyBuffer();
        
        if(datalength > MAX_GUID_DATA)
            _data2 = ByteBuffer.wrap(data, MAX_GUID_DATA, getLength());
        else
            _data2 = BufferUtils.getEmptyBuffer();
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
	public long getSequenceNumber() {
		return _sequenceNumber;
	}

    /**
     *  Extend the sequence number of incoming messages with the full 8 bytes
	 *  of state
     */
	public void extendSequenceNumber(long seqNo) {
		_sequenceNumber = seqNo;
	}

    /**
     * Allocate and fill in the data packed in the GUID.
     */
    private static byte[] buildGUID(byte connectionID, byte opcode, 
      long sequenceNumber, byte[] data, int datalength) {
        int guidDataLength = (datalength >= MAX_GUID_DATA ? MAX_GUID_DATA : 
          datalength);

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
    private static int calcPayloadLength(int datalength) {
        return(datalength <= MAX_GUID_DATA ? 0 : datalength - MAX_GUID_DATA);
    }

    /**
     * Given the guid and the payload, recreate the in memory data structures
     */
    private void unpackFromWire(byte[] guid, byte[] payload) 
      throws BadPacketException {
        _connectionID   = guid[0];
        _opcode         = (byte)((guid[1] & 0xf0) >> 4); 
        _sequenceNumber = 
          (((long) guid[2] & 0xff) << 8) | ((long) guid[3] & 0xff);

        _data1 = ByteBuffer.wrap(guid, GUID_DATA_START, (guid[1] & 0x0f));
        _data2 = ByteBuffer.wrap(payload);

        if ( _data1.remaining() > MAX_GUID_DATA )
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
     * Create a byte array from 1 byte and 1 short int
     */
    public static byte[] buildByteArray(byte val1, int val2) {
        byte[] darray = new byte[3];
        darray[0] = val1;
        darray[1] = (byte)((val2 & 0xff00) >> 8);
        darray[2] = (byte)((val2 & 0x00ff));

        return darray;
    } 

    /**
     * Create a byte array for data sending purposes.
     */
    public static byte[] buildByteArray(int val1, int val2) {
        byte[] darray = new byte[4];
        darray[0] = (byte)((val1 & 0xff00) >> 8);
        darray[1] = (byte)((val1 & 0x00ff));
        darray[2] = (byte)((val2 & 0xff00) >> 8);
        darray[3] = (byte)((val2 & 0x00ff));

        return darray;
    } 

    /**
     *  Return an int from 2 unsigned bytes
     */
    public static int getShortInt(byte b1, byte b2) {
          return ((b1 & 0xff) << 8) | (b2 & 0xff);
    }

    /** 
     *  Return the length of data stored in this message.
     */
    public int getDataLength() {
        return _data1.remaining() + getLength();
    }

    /** 
     *  Output additional data as payload.
     */
    protected void writePayload(OutputStream out) throws IOException {
        if ( _data2.remaining() > 0 )
            out.write(_data2.array(), _data2.position(), _data2.remaining());
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
