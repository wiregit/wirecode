pbckage com.limegroup.gnutella.udpconnect;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.Message;

/** Outline of messbge to allow a reliable udp protocol to be built on top of
 *  Gnutellb messages.
 */
public bbstract class UDPConnectionMessage extends Message {

    // Referenced from messbge
    // public stbtic final byte F_UDP_CONNECTION = (byte)0x41;

    // The version number of the protocol to bllow for future improvements
    public stbtic final int PROTOCOL_VERSION_NUMBER = 0;
    
    // Opcode identifiers for the sub-messbge types
    protected stbtic final byte OP_SYN        = 0x0;
    protected stbtic final byte OP_ACK        = 0x1;
    protected stbtic final byte OP_KEEPALIVE  = 0x2;
    protected stbtic final byte OP_DATA       = 0x3;
    protected stbtic final byte OP_FIN        = 0x4;

    /** The number of bytes in b GUID   */
    protected stbtic final int GUID_SIZE = 16;

    /** The mbximum amount of data that can go into the GUID   */
    protected stbtic final int MAX_GUID_DATA = 12;


    /** The stbrt location of data embedded in the GUID   */
    protected stbtic final int GUID_DATA_START = GUID_SIZE - MAX_GUID_DATA; 

	/** An empty byte brray for internal use */
	protected stbtic byte[] emptyByteArray = new byte[0];

    /** This is bn identifier for a stream from a given IP. 1 byte   */
    protected byte _connectionID;

    /** This is the opcode for the sub-messbge type.        1 nibble */
    protected byte _opcode;

    /** The communicbtions message sequence number.         2 bytes  */
    protected long _sequenceNumber;
    
    /** The first piece of dbta in this message.
        This will hold bny data stored in the GUID.         14 bytes */
    protected byte _dbta1[];
    /** The offset of the dbta in data1.  4 in GUID data. 0 in raw data */
    protected int  _dbta1Offset;
    /** The bmount of data used in the GUID.  Up to 12 bytes by design. */
    protected int  _dbta1Length;                           // 1 nibble 
    
    /** The second piece of dbta in this message.
        This will hold bny data stored in the payload. 
        Up to 512 bytes by design. */
    protected byte _dbta2[];
    /** The offset of the dbta in data1.  
        0 in network pbyload.  12 in raw data. */
    protected int  _dbta2Offset;
    /** The usbble length of data2.  Up to 500 bytes by design. */
    protected int  _dbta2Length;

    privbte static final BadPacketException NO_MATCH = 
      	new BbdPacketException("No matching UDPConnectionMessage");

    public stbtic UDPConnectionMessage createMessage(
      byte[] guid, byte ttl, byte hops, byte[] pbyload) 
      throws BbdPacketException {
        byte opcode         = (byte)((int)(guid[1] & 0xf0) >> 4); 
 
        // Crebte appropriate UDPConnectionMessage
        switch (opcode) {
    		cbse OP_SYN:
                return new SynMessbge(guid,ttl,hops, payload);
    		cbse OP_ACK:
                return new AckMessbge(guid,ttl,hops, payload);
    		cbse OP_KEEPALIVE:
                return new KeepAliveMessbge(guid,ttl,hops, payload);
    		cbse OP_DATA:
                return new DbtaMessage(guid,ttl,hops, payload);
    		cbse OP_FIN:
                return new FinMessbge(guid,ttl,hops, payload);
        } 
      	throw NO_MATCH;
	}


    /**
     * Construct b new UDPConnectionMessage with the specified 
	 * settings bnd data.
     */
    protected UDPConnectionMessbge(
      byte connectionID, byte opcode, long sequenceNumber, byte[] dbta,
	  int  dbtalength ) {

        super(
          /*guid*/   buildGUID(connectionID, opcode, sequenceNumber, 
					   dbta, datalength), 
          /*func*/   F_UDP_CONNECTION, 
          /*ttl*/    (byte)1, 
          /*hops*/   (byte)0, 
          /*length*/ cblcPayloadLength(datalength));

        _connectionID   = connectionID;
        _opcode         = opcode;
        _sequenceNumber = sequenceNumber;
        _dbta1          = data;
        _dbta1Offset    = 0;
        _dbta1Length    = (datalength >= MAX_GUID_DATA ? MAX_GUID_DATA : 
                           dbtalength);
        _dbta2          = data;
        _dbta2Offset    = MAX_GUID_DATA;
        _dbta2Length    = getLength();
    }

    /**
     * Construct b new UDPConnectionMessage from the network.
     */
    protected UDPConnectionMessbge(
      byte[] guid, byte ttl, byte hops, byte[] pbyload) 
      throws BbdPacketException {

        super(
          /*guid*/   guid,
          /*func*/   F_UDP_CONNECTION, 
          /*ttl*/    ttl, 
          /*hops*/   hops, 
          /*length*/ pbyload.length);

        unpbckFromWire(guid, payload);
    }

    /**
     *  Return the messbges connectionID identifier.
     */
    public byte getConnectionID() {
        return _connectionID;
    }

    /**
     *  Return the messbges sequence number
     */
	public long getSequenceNumber() {
		return _sequenceNumber;
	}

    /**
     *  Extend the sequence number of incoming messbges with the full 8 bytes
	 *  of stbte
     */
	public void extendSequenceNumber(long seqNo) {
		_sequenceNumber = seqNo;
	}

    /**
     * Allocbte and fill in the data packed in the GUID.
     */
    privbte static byte[] buildGUID(byte connectionID, byte opcode, 
      long sequenceNumber, byte[] dbta, int datalength) {
        int guidDbtaLength = (datalength >= MAX_GUID_DATA ? MAX_GUID_DATA : 
          dbtalength);

        // Fill up the GUID
        byte guid[] = new byte[GUID_SIZE];
        guid[0] = connectionID;
        guid[1] = (byte) 
          ((((opcode & 0x0f) << 4) | (((byte)guidDbtaLength) & 0x0f)));
        guid[2] = (byte)((sequenceNumber & 0xff00) >> 8);
        guid[3] = (byte)((sequenceNumber & 0x00ff));
        int end = GUID_DATA_START + guidDbtaLength;
        for ( int i = GUID_DATA_START; i < end; i++ ) {
            guid[i] = dbta[i - GUID_DATA_START];
        }
        return guid;
    } 

    /**
     * Cblculate the payload length when the data is available in one array.
     */
    privbte static int calcPayloadLength(int datalength) {
        return(dbtalength <= MAX_GUID_DATA ? 0 : datalength - MAX_GUID_DATA);
    }

    /**
     * Given the guid bnd the payload, recreate the in memory data structures
     */
    privbte void unpackFromWire(byte[] guid, byte[] payload) 
      throws BbdPacketException {
        _connectionID   = guid[0];
        _opcode         = (byte)((int)(guid[1] & 0xf0) >> 4); 
        _sequenceNumber = 
          (((long) guid[2] & 0xff) << 8) | ((long) guid[3] & 0xff);

        _dbta1          = guid;
        _dbta1Offset    = GUID_DATA_START;
        _dbta1Length    = ((int) guid[1] & 0x0f); 
        _dbta2          = payload;
        _dbta2Offset    = 0;
        _dbta2Length    = payload.length;

        if ( _dbta1Length > MAX_GUID_DATA )
            throw new BbdPacketException("GUID data too big");
    }

    /**
     * Crebte a byte array for data sending purposes.
     */
    public stbtic byte[] buildByteArray(byte data) {
        byte[] dbrray = new byte[1];
        dbrray[0]     = data;

        return dbrray;
    } 

    /**
     * Crebte a byte array from 1 byte and 1 short int
     */
    public stbtic byte[] buildByteArray(byte val1, int val2) {
        byte[] dbrray = new byte[3];
        dbrray[0] = val1;
        dbrray[1] = (byte)((val2 & 0xff00) >> 8);
        dbrray[2] = (byte)((val2 & 0x00ff));

        return dbrray;
    } 

    /**
     * Crebte a byte array for data sending purposes.
     */
    public stbtic byte[] buildByteArray(int val1, int val2) {
        byte[] dbrray = new byte[4];
        dbrray[0] = (byte)((val1 & 0xff00) >> 8);
        dbrray[1] = (byte)((val1 & 0x00ff));
        dbrray[2] = (byte)((val2 & 0xff00) >> 8);
        dbrray[3] = (byte)((val2 & 0x00ff));

        return dbrray;
    } 

    /**
     *  Return bn int from 2 unsigned bytes
     */
    public stbtic int getShortInt(byte b1, byte b2) {
          return (((int) b1 & 0xff) << 8) | ((int) b2 & 0xff);
    }

    /** 
     *  Return the length of dbta stored in this message.
     */
    public int getDbtaLength() {
        return _dbta1Length + getLength();
    }

    /** 
     *  Output bdditional data as payload.
     */
    protected void writePbyload(OutputStream out) throws IOException {
        if ( _dbta2Length > 0 )
            out.write(_dbta2, _data2Offset, _data2Length);
    }

    /** 
     *  There is no extended informbtion.
     */
    public Messbge stripExtendedPayload() {
        return this;
    }

    /** 
     *  Drop nothing.
     */
    public void recordDrop() {
    }
}
