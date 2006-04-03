
// Commented for the Learning branch

package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/**
 * Both computers create a UDP connection by sending Syn messages to one another at the same time.
 * 
 * There are 2 connection IDs in a Syn message.
 * In the very first byte is the connection ID the receiving computer chose for this UDP connection.
 * In the first Syn messages a computer sends, it doesn't know what connection ID the remote computer will choose, and puts 0 in the first byte instead.
 * 4 bytes into the message is the connection ID the sending computer chose for this UDP connection.
 * It's at the start of the data in the GUID area.
 * 
 * A Syn message carries 3 bytes of payload data.
 * They all fit into the 12 bytes of space in the GUID area.
 * The first byte is the connection ID the sending computer is choosing for this new connection.
 * The next 2 byets are 0, the current version of the UDP connection protocol is 0.
 * 
 * A 23-byte Syn message we send to initiate a new UDP connection looks like this:
 * 
 * 00 03 00 00 05 00 00 00  aa bc dd dd ee ff ff gg
 * 00 00 00 00 00 00 00 00  gg gg gg gg gg gg gg gg
 * 41 01 00 00 00 00 00     hh ii jj kk kk kk kk
 * 
 * a is the connection ID the remote computer assigned this connection, it's 0 because this hasn't happened yet.
 * b and c are 4-bit nibbles that fit together into a single byte.
 * b is the operation code, 0 identifies this as a Syn message.
 * c is the length of data in the GUID area, we'll put 3 bytes there.
 * d is the sequence number, 0 because the sequence hasn't started yet.
 * 
 * e through g are the 12 bytes in the GUID area we can start to put data, our 3 bytes of data fit into this area.
 * e through f are the 3 bytes of data specific to this Syn message.
 * e is the ID the computer sending this Syn message has chosen for the UDP connection.
 * f is the protocol version number, 0.
 * g is the extra area in the GUID we don't need.
 * 
 * h is the Gnutella packet type, 0x41 identifies this as a UDP connection message.
 * i and j are the TTL and hops, 1 and 0, and not really useful or used.
 * k is the length of the payload beyond this 23-byte header, 0, there is no payload because all our data fits in the 12-byte GUID area e through g.
 */
public class SynMessage extends UDPConnectionMessage {

    /**
     * The connection ID the computer that sends this message has chosen for the UDP connection it's a part of.
     * The sender's connection ID is written 4 bytes into the message, at the start of the data in the GUID area.
     * 
     * This is not the same thing as the receiver's connection ID, which is the first byte of the message.
     */
	private byte _senderConnectionID;

    /** The version number of the UDP connection protocol, like 0. */
    private int  _protocolVersionNumber;

    /**
     * Make a new SynMessage to send with the connection ID we've chosen.
     * 
     * Use this constructor when we don't know what connection ID the remote comptuer has chosen for this connection with us yet.
     * In the packet, we leave the remote computer's connection ID at the start of the message as 0.
     * 
     * Only UDPConnectionProcessor.tryToConnect() calls this.
     * This is a packet maker.
     * 
     * @param connectionID The connection ID we've chosen for this UDP connection
     */
    public SynMessage(byte connectionID) {

        // Call the UDPConnectionMessage constructor to make the message
        super(
            (byte)0, // The connection ID the remote computer assigned to this connection, 0 because we don't know yet
            OP_SYN,  // 0x0, the operation code for a Syn message
            0,       // Sequence number 0, the packet stream hasn't started yet

            // 3 bytes of data, our connection ID in byte 1 and the protocol version 0 in the 2 bytes after that
            buildByteArray(connectionID, PROTOCOL_VERSION_NUMBER),

            3);      // Payload length 3, the payload will fit in the GUID area

        // Save the information we used in this object
		_senderConnectionID    = connectionID;            // The connection ID we chose for this UDP connection
        _protocolVersionNumber = PROTOCOL_VERSION_NUMBER; // 0, the current UDP connection number
    }

    /**
     * Make a new SynMessage to send with the connection IDs both we and the remote computer have chosen for this UDP connection between us.
     * 
     * Use this constructor when we got a Syn message from the remote computer that told us what connection ID it picked.
     * 
     * Only UDPConnectionProcessor.tryToConnect() calls this.
     * This is a packet maker.
     * 
     * @param connectionID      The connection ID we chose for this UDP connection
     * @param theirConnectionID The connection ID the remote computer choose for this UDP connection
     */
    public SynMessage(byte connectionID, byte theirConnectionID) {

        // Call the UDPConnectionMessage constructor to make the message
        super(
            theirConnectionID, // The connection ID the remote computer chose to identify this connection
            OP_SYN,            // 0x0, the 4-bit code that identifies this as a Syn message
            0,                 // Sequence number 0, the packet stream hasn't started yet

            // 3 bytes of data, the connection ID we chose in byte 1, and the protocol version 0 in the 2 bytes after that
            buildByteArray(connectionID, PROTOCOL_VERSION_NUMBER), 3); // Payload length 3, the payload will fit in the GUID area

        // Save the given information in this object also
        _senderConnectionID    = connectionID;            // The connection ID we're choosing for the UDP connection
        _protocolVersionNumber = PROTOCOL_VERSION_NUMBER; // 0, the current UDP connection number
    }

    /**
     * Make a new SynMessage from data a remote computer sent us.
     * This is the packet parser.
     * 
     * @param guid    The first 16 bytes of the Gnutella message header, where the GUID should be
     * @param ttl     The ttl we read
     * @param hops    The hops we read
     * @param payload The payload data after the 23-byte Gnutella message header
     */
    public SynMessage(byte[] guid, byte ttl, byte hops, byte[] payload) throws BadPacketException {

        // Call the UDPConnectionMessage and Message constructors to parse and save the information common to all UDP connection packets
        super(guid, ttl, hops, payload);

        /*
         * A SynMessage carries 3 bytes of data, like this:
         * 
         * abb
         * 
         * a is the connection ID the remote computer which is initiating this UDP connection chose for it
         * b is 2 bytes that hold the version number of the protocol, like 0.
         * 
         * The 3 bytes of data fit into the area where the GUID should be.
         */

        // Read the connection ID the remote computer chose for this UDP connection
        _senderConnectionID = guid[GUID_DATA_START]; // It's 4 bytes into the packet data, at the start of the data in the GUID area

        // Read the protocol version number from the 2 bytes after that
        _protocolVersionNumber = getShortInt(guid[GUID_DATA_START + 1], guid[GUID_DATA_START + 2]); // The version number, like 0
    }

    /**
     * The connection ID the computer that sent this Syn message chose for the UDP connection between us.
     * This ID number is stored 4 bytes into the packet data, at the start of the GUID area.
     * It's not the one at the start of the message which was chosen by the receiving computer.
     * 
     * @return The connection ID number in a byte
     */
    public byte getSenderConnectionID() {

        // Return the connection ID
        return _senderConnectionID;
    }

    /**
     * The version number of the UDP connection this SynMessage wants to establish.
     * 
     * @return The version number, like 0
     */
	public int getProtocolVersionNumber() {

        // Return the protocol version number
		return _protocolVersionNumber;
	}

    /**
     * Express this SynMessage as text.
     * Composes a String like "SynMessage DestID:5586 SrcID:2875 vNo:0".
     * 
     * @return A String
     */
	public String toString() {

        // Compose and return the text in a String
		return "SynMessage DestID:" + getConnectionID() + " SrcID:" + _senderConnectionID + " vNo:" + _protocolVersionNumber;
	}
}
