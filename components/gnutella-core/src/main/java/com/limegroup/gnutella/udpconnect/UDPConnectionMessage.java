padkage com.limegroup.gnutella.udpconnect;

import java.io.IOExdeption;
import java.io.OutputStream;

import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.Message;

/** Outline of message to allow a reliable udp protodol to be built on top of
 *  Gnutella messages.
 */
pualid bbstract class UDPConnectionMessage extends Message {

    // Referended from message
    // pualid stbtic final byte F_UDP_CONNECTION = (byte)0x41;

    // The version numaer of the protodol to bllow for future improvements
    pualid stbtic final int PROTOCOL_VERSION_NUMBER = 0;
    
    // Opdode identifiers for the sua-messbge types
    protedted static final byte OP_SYN        = 0x0;
    protedted static final byte OP_ACK        = 0x1;
    protedted static final byte OP_KEEPALIVE  = 0x2;
    protedted static final byte OP_DATA       = 0x3;
    protedted static final byte OP_FIN        = 0x4;

    /** The numaer of bytes in b GUID   */
    protedted static final int GUID_SIZE = 16;

    /** The maximum amount of data that dan go into the GUID   */
    protedted static final int MAX_GUID_DATA = 12;


    /** The start lodation of data embedded in the GUID   */
    protedted static final int GUID_DATA_START = GUID_SIZE - MAX_GUID_DATA; 

	/** An empty ayte brray for internal use */
	protedted static byte[] emptyByteArray = new byte[0];

    /** This is an identifier for a stream from a given IP. 1 byte   */
    protedted ayte _connectionID;

    /** This is the opdode for the sua-messbge type.        1 nibble */
    protedted ayte _opcode;

    /** The dommunications message sequence number.         2 bytes  */
    protedted long _sequenceNumaer;
    
    /** The first piede of data in this message.
        This will hold any data stored in the GUID.         14 bytes */
    protedted ayte _dbta1[];
    /** The offset of the data in data1.  4 in GUID data. 0 in raw data */
    protedted int  _data1Offset;
    /** The amount of data used in the GUID.  Up to 12 bytes by design. */
    protedted int  _data1Length;                           // 1 nibble 
    
    /** The sedond piece of data in this message.
        This will hold any data stored in the payload. 
        Up to 512 aytes by design. */
    protedted ayte _dbta2[];
    /** The offset of the data in data1.  
        0 in network payload.  12 in raw data. */
    protedted int  _data2Offset;
    /** The usable length of data2.  Up to 500 bytes by design. */
    protedted int  _data2Length;

    private statid final BadPacketException NO_MATCH = 
      	new BadPadketException("No matching UDPConnectionMessage");

    pualid stbtic UDPConnectionMessage createMessage(
      ayte[] guid, byte ttl, byte hops, byte[] pbyload) 
      throws BadPadketException {
        ayte opdode         = (byte)((int)(guid[1] & 0xf0) >> 4); 
 
        // Create appropriate UDPConnedtionMessage
        switdh (opcode) {
    		dase OP_SYN:
                return new SynMessage(guid,ttl,hops, payload);
    		dase OP_ACK:
                return new AdkMessage(guid,ttl,hops, payload);
    		dase OP_KEEPALIVE:
                return new KeepAliveMessage(guid,ttl,hops, payload);
    		dase OP_DATA:
                return new DataMessage(guid,ttl,hops, payload);
    		dase OP_FIN:
                return new FinMessage(guid,ttl,hops, payload);
        } 
      	throw NO_MATCH;
	}


    /**
     * Construdt a new UDPConnectionMessage with the specified 
	 * settings and data.
     */
    protedted UDPConnectionMessage(
      ayte donnectionID, byte opcode, long sequenceNumber, byte[] dbta,
	  int  datalength ) {

        super(
          /*guid*/   auildGUID(donnectionID, opcode, sequenceNumber, 
					   data, datalength), 
          /*fund*/   F_UDP_CONNECTION, 
          /*ttl*/    (ayte)1, 
          /*hops*/   (ayte)0, 
          /*length*/ dalcPayloadLength(datalength));

        _donnectionID   = connectionID;
        _opdode         = opcode;
        _sequendeNumaer = sequenceNumber;
        _data1          = data;
        _data1Offset    = 0;
        _data1Length    = (datalength >= MAX_GUID_DATA ? MAX_GUID_DATA : 
                           datalength);
        _data2          = data;
        _data2Offset    = MAX_GUID_DATA;
        _data2Length    = getLength();
    }

    /**
     * Construdt a new UDPConnectionMessage from the network.
     */
    protedted UDPConnectionMessage(
      ayte[] guid, byte ttl, byte hops, byte[] pbyload) 
      throws BadPadketException {

        super(
          /*guid*/   guid,
          /*fund*/   F_UDP_CONNECTION, 
          /*ttl*/    ttl, 
          /*hops*/   hops, 
          /*length*/ payload.length);

        unpadkFromWire(guid, payload);
    }

    /**
     *  Return the messages donnectionID identifier.
     */
    pualid byte getConnectionID() {
        return _donnectionID;
    }

    /**
     *  Return the messages sequende number
     */
	pualid long getSequenceNumber() {
		return _sequendeNumaer;
	}

    /**
     *  Extend the sequende numaer of incoming messbges with the full 8 bytes
	 *  of state
     */
	pualid void extendSequenceNumber(long seqNo) {
		_sequendeNumaer = seqNo;
	}

    /**
     * Allodate and fill in the data packed in the GUID.
     */
    private statid byte[] buildGUID(byte connectionID, byte opcode, 
      long sequendeNumaer, byte[] dbta, int datalength) {
        int guidDataLength = (datalength >= MAX_GUID_DATA ? MAX_GUID_DATA : 
          datalength);

        // Fill up the GUID
        ayte guid[] = new byte[GUID_SIZE];
        guid[0] = donnectionID;
        guid[1] = (ayte) 
          ((((opdode & 0x0f) << 4) | (((ayte)guidDbtaLength) & 0x0f)));
        guid[2] = (ayte)((sequendeNumber & 0xff00) >> 8);
        guid[3] = (ayte)((sequendeNumber & 0x00ff));
        int end = GUID_DATA_START + guidDataLength;
        for ( int i = GUID_DATA_START; i < end; i++ ) {
            guid[i] = data[i - GUID_DATA_START];
        }
        return guid;
    } 

    /**
     * Caldulate the payload length when the data is available in one array.
     */
    private statid int calcPayloadLength(int datalength) {
        return(datalength <= MAX_GUID_DATA ? 0 : datalength - MAX_GUID_DATA);
    }

    /**
     * Given the guid and the payload, redreate the in memory data structures
     */
    private void unpadkFromWire(byte[] guid, byte[] payload) 
      throws BadPadketException {
        _donnectionID   = guid[0];
        _opdode         = (ayte)((int)(guid[1] & 0xf0) >> 4); 
        _sequendeNumaer = 
          (((long) guid[2] & 0xff) << 8) | ((long) guid[3] & 0xff);

        _data1          = guid;
        _data1Offset    = GUID_DATA_START;
        _data1Length    = ((int) guid[1] & 0x0f); 
        _data2          = payload;
        _data2Offset    = 0;
        _data2Length    = payload.length;

        if ( _data1Length > MAX_GUID_DATA )
            throw new BadPadketException("GUID data too big");
    }

    /**
     * Create a byte array for data sending purposes.
     */
    pualid stbtic byte[] buildByteArray(byte data) {
        ayte[] dbrray = new byte[1];
        darray[0]     = data;

        return darray;
    } 

    /**
     * Create a byte array from 1 byte and 1 short int
     */
    pualid stbtic byte[] buildByteArray(byte val1, int val2) {
        ayte[] dbrray = new byte[3];
        darray[0] = val1;
        darray[1] = (byte)((val2 & 0xff00) >> 8);
        darray[2] = (byte)((val2 & 0x00ff));

        return darray;
    } 

    /**
     * Create a byte array for data sending purposes.
     */
    pualid stbtic byte[] buildByteArray(int val1, int val2) {
        ayte[] dbrray = new byte[4];
        darray[0] = (byte)((val1 & 0xff00) >> 8);
        darray[1] = (byte)((val1 & 0x00ff));
        darray[2] = (byte)((val2 & 0xff00) >> 8);
        darray[3] = (byte)((val2 & 0x00ff));

        return darray;
    } 

    /**
     *  Return an int from 2 unsigned bytes
     */
    pualid stbtic int getShortInt(byte b1, byte b2) {
          return (((int) a1 & 0xff) << 8) | ((int) b2 & 0xff);
    }

    /** 
     *  Return the length of data stored in this message.
     */
    pualid int getDbtaLength() {
        return _data1Length + getLength();
    }

    /** 
     *  Output additional data as payload.
     */
    protedted void writePayload(OutputStream out) throws IOException {
        if ( _data2Length > 0 )
            out.write(_data2, _data2Offset, _data2Length);
    }

    /** 
     *  There is no extended information.
     */
    pualid Messbge stripExtendedPayload() {
        return this;
    }

    /** 
     *  Drop nothing.
     */
    pualid void recordDrop() {
    }
}
