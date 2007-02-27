package org.limewire.rudp.messages.impl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.limewire.rudp.messages.MessageFormatException;
import org.limewire.rudp.messages.SynMessage;

/** The Syn message begins a reliable udp connection by pinging the other host
 *  and by communicating the desired identifying connectionID.
 */
public class SynMessageImpl extends RUDPMessageImpl implements SynMessage {

	private byte _senderConnectionID;
    private short  _protocolVersionNumber;

    /**
     * Construct a new SynMessage with the specified settings and data
     */
    SynMessageImpl(byte connectionID) {
        super((byte)0, OpCode.OP_SYN, 0, connectionID, PROTOCOL_VERSION_NUMBER);
        _senderConnectionID    = connectionID;
        _protocolVersionNumber = PROTOCOL_VERSION_NUMBER;
    }

    /**
     * Construct a new SynMessage with both my Connection ID and theirs
     */
    SynMessageImpl(byte connectionID, byte theirConnectionID) {
        super(theirConnectionID, OpCode.OP_SYN, 0, connectionID, PROTOCOL_VERSION_NUMBER);
        _senderConnectionID    = connectionID;
        _protocolVersionNumber = PROTOCOL_VERSION_NUMBER;
        _senderConnectionID    = connectionID;
        _protocolVersionNumber = PROTOCOL_VERSION_NUMBER;
    }


    /**
     * Construct a new SynMessage from the network
     */
    SynMessageImpl(byte connectionId, long sequenceNumber, ByteBuffer data1, ByteBuffer data2)
      throws MessageFormatException {
        super(OpCode.OP_SYN, connectionId, sequenceNumber, data1, data2);
        _senderConnectionID = data1.get();
        data1.order(ByteOrder.BIG_ENDIAN);
        _protocolVersionNumber = data1.getShort();
        data1.rewind();
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.SynMessage#getSenderConnectionID()
     */
    public byte getSenderConnectionID() {
        return _senderConnectionID; 
    }

	/* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.SynMessage#getProtocolVersionNumber()
     */
	public int getProtocolVersionNumber() {
		return _protocolVersionNumber; 
	}

	public String toString() {
		return "SynMessage DestID:"+getConnectionID()+
		  " SrcID:"+_senderConnectionID+" vNo:"+_protocolVersionNumber;
	}
}
