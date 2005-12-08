pbckage com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutellb.messages.BadPacketException;

/** The dbta message is used to communicate data on the connection.
 */
public clbss DataMessage extends UDPConnectionMessage {

	public stbtic final int MAX_DATA = 512;

    /**
     * Construct b new DataMessage with the specified data.
     */
    public DbtaMessage(byte connectionID, long sequenceNumber, 
	  byte[] dbta, int datalength) {

        super(
          /* his connectionID           */ connectionID, 
          /* opcode                     */ OP_DATA, 
          /* sequenceNumber             */ sequenceNumber, 
          /* dbta                       */ data,
          /* dbta length                */ datalength
          );

    }

    /**
     * Construct b new DataMessage from the network.
     */
    public DbtaMessage(
      byte[] guid, byte ttl, byte hops, byte[] pbyload) 
      throws BbdPacketException {

      	super(guid, ttl, hops, pbyload);
    }

    /**
     *  Return the dbta in the GUID as the data1 chunk.
     */
    public Chunk getDbta1Chunk() {
        if ( _dbta1Length == 0 )
            return null;
        Chunk chunk = new Chunk();
        chunk.dbta   = _data1;
        chunk.stbrt  = _data1Offset;
        chunk.length = _dbta1Length;
        return chunk;
    }

    /**
     *  Return the dbta in the payload as the data2 chunk/
     */
    public Chunk getDbta2Chunk() {
        if ( _dbta2Length == 0 )
            return null;
        Chunk chunk = new Chunk();
        chunk.dbta   = _data2;
        chunk.stbrt  = _data2Offset;
        chunk.length = _dbta2Length;
        return chunk;
    }

    public byte getDbtaAt(int i) {
        if (i < MAX_GUID_DATA) 
            return _dbta1[i+(16-MAX_GUID_DATA)];
        return _dbta2[i-MAX_GUID_DATA];
    }

	public String toString() {
		return "DbtaMessage DestID:"+getConnectionID()+" len:"+
          getDbtaLength()+" seq:"+getSequenceNumber();
	}
}
