package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.statistics.*;


/**
 * Reads Gnutella messages from a SocketChannel, instantiating subclasses of
 * Message as appropriate by opcode.  Used by Connnection to read messages in a
 * non-blocking manner (which requires state).  Contains code formerly in
 * Message.read(InputStream).  Thread-safe; in a true single-thread core, this
 * would not be necessary.<p>
 * 
 * NON-FINAL FOR TESTING
 */
public class NIOMessageReader extends AbstractMessageReader {
	 

    /**
	 * Constant for the <tt>ByteBuffer</tt> for reading headers.  Note that 
     * this is fixed size because all Gnutella headers have the same size.
	 */
     private final ByteBuffer HEADER = ByteBuffer.allocate(HEADER_SIZE);
        
    /**
     * Constant for the <tt>ByteBuffer</tt> for reading payloads.
     */
    private final ByteBuffer PAYLOAD = 
        ByteBuffer.allocate(MessageSettings.MAX_LENGTH.getValue());

    
    /**
     * Factory method for creating new <tt>MessageReader</tt>
     * instances for each connection.
     * 
     * @param conn the <tt>Connection</tt> this reader reads for
     * @param handshaker the <tt>Handshaker</tt> instance that can contain 
     *  extra, unprocessed data read in after all headers were read -- message
     *  data that should be processed right away
     * @return a new <tt>MessageReader</tt> instance
     */
    public static NIOMessageReader createReader(Connection conn, 
        Handshaker handshaker) throws IOException {
        return new NIOMessageReader(conn, handshaker);
    }
    
	/**
	 * Ensure that this class cannot be constructed by another class.
     *
     * @param conn the <tt>Connection</tt> this reader reads for
     * @param handshaker the <tt>Handshaker</tt> instance that can contain 
     *  extra, unprocessed data read in after all headers were read -- message
     *  data that should be processed right away
	 */
	protected NIOMessageReader(Connection conn, Handshaker handshaker) 
        throws IOException {
        super(conn);
        HEADER.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        ByteBuffer buffer = handshaker.getRemainingData();

        /*
        // read all data in the buffer
        while(buffer.hasRemaining()) {
            Message msg = createMessage(HEADER, PAYLOAD, conn, buffer);
            if(msg != null) { 
                System.out.println("NIOMessageReader::routing left over message!!");
                System.out.println(msg);
                routeMessage(msg);
            } else {
                Assert.that(!buffer.hasRemaining(), 
                    "should not be any data in the buffer");
            }
        }
        */
    }
	
    /**
     * Specialized method for creating a message from a pre-read 
     * <tt>ByteBuffer</tt>.  This is particularly used for handling message 
     * data that was read after a handshake but that was not processed.  This
     * often happens because we just read chunks of data in from the channel
     * during handshaking, and so can read well beyond the end of the 
     * handshaking headers and into the message data.
     * 
     * @param header the <tt>ByteBuffer</tt> that should be filled with message
     *  header data
     * @param payload the <tt>ByteBuffer</tt> that should be filled with 
     *  message payload data
     * @param conn the <tt>Connection</tt> that read this message
     * @param buffer the <tt>ByteBuffer</tt> containing the unprocessed data
     * @return a new <tt>Message</tt> instance from the specified payload data,
     *  or <tt>null</tt> if only an incomplete message was read
     * @throws IOException if there was an IO error reading the data
     * @throws BadPacketException if the data did not match the format of any
     *  known messages
     */
    private static Message createMessage(ByteBuffer header, ByteBuffer payload,
        Connection conn, ByteBuffer buffer) throws IOException {

        if(!readHeader(header, buffer)) {
            return null;
        }
        
        handlePayloadLimit(header, payload);

        return readPayload(header, payload, conn, buffer);
    }	
	
	/**
	 * Creates a new <tt>Message</tt> from incoming data over TCP.
	 * 
	 * @param key the <tt>SelectionKey</tt> instance containing access to the 
     *  channel and the <tt>Connection</tt> for the message
	 * @return a new <tt>Message</tt> instance read in from the network
	 * @throws IOException if there is an IO error reading the message, such as 
     *  the connection being broken
	 * @throws BadPacketException if the message has an unexpected form
	 */
	public Message createMessageFromTCP(SelectionKey key) 
		throws IOException, BadPacketException {
        return createMessage(key, Message.N_TCP);
	}
    
        

	/**
	 * Creates a new <tt>Message</tt> instance from the specified 
     * <tt>SelectionKey</tt> and it's associated channel and connection.
	 * 
	 * @param key  the <tt>SelectionKey</tt> for the new data
	 * @param network the network interface this message arrived over, such as 
     *  TCP, UDP, multicast, etc.
	 * @return a new <tt>Message</tt> instance created from the specified data
	 * @throws IOException if there is an IO error reading the data or writing 
     *  it to the new message, which will subsequently close the socket
	 * @throws BadPacketException if the data received from the network is 
     *  invalid for any reason
	 */
	private Message createMessage(SelectionKey key, int network) 
		throws IOException, BadPacketException {

		SocketChannel channel = (SocketChannel)key.channel();
        
        // if there's more header to read, return to read it on the next pass
        if(!readHeader(channel)) {
            return null;
        }
        
        // Now read the payload.  This will either read the entire payload and
        // return the message, or it will not read all of the payload, in which
        // case it will return null, leaving us to keep reading the rest on the
        // next pass.
        return readPayload(key, network);
    }

    
    private static boolean readHeader(ByteBuffer header, ByteBuffer buffer) {
        if(buffer.remaining() < HEADER_SIZE) {
            header.put(buffer);
            return false;           
        }
        
        while(header.hasRemaining()) {
            header.put(buffer.get());
        }
        return true;
    }

    private static Message readPayload(ByteBuffer header, ByteBuffer payload, 
        Connection conn, ByteBuffer buffer) {
        // If the buffer does not contain the full payload, just copy
        // what it has and wait to read more data from the channel.
        if(buffer.remaining() < payload.limit()) {
            payload.put(buffer);
            return null;
        }
        
        // Otherwise, there's a complete message in the buffer, so
        // create it.
        while(payload.hasRemaining()) {
            payload.put(buffer.get());
        } 
           
        try {
            return createMessage(header, payload, conn, Message.N_TCP);
        } catch (BadPacketException e) {
            // Ignore this packet and hope to read valid packets on subsequent
            // reads.
            MessageReadErrorStat.BAD_PACKET_EXCEPTIONS.incrementStat();
            return null;
        } 
    }

    /**
     * Reads the message header into the HEADER ByteBuffer.
     * 
     * @param channel the channel to read from
     * @return <tt>true</tt> if the header was completely read, otherwise
     *  <tt>false</tt>
     * @throws IOException if the connection was closed or any other IO error
     *  occurred during reading
     */
    private boolean readHeader(SocketChannel channel) throws IOException {
        if(!HEADER.hasRemaining()) return true;
        
        if(channel.read(HEADER) < 0) {
            HEADER.clear();
            MessageReadErrorStat.CONNECTION_CLOSED_READING_HEADER.
                incrementStat();
            throw new IOException("end of stream reading header");
        } else if(HEADER.hasRemaining()) {
            // if we still haven't read all of the header, return
            // to get the rest on the next pass.
            return false;
        }
        
        // if we get here, the header has been completely read
        return true;
    }
    
    /**
     * Reads the payload of an incoming message.
     * 
     * @param key the <tt>SelectionKey</tt> for the incoming messsage
     * @param network the transport layer the message arrived on, such as 
     *  TCP or UDP
     * 
     * @return a new <tt>Message</tt> subclass
     *  
     * @throws IOException if there is an IO error reading the message
     * @throws BadPacketException if the message data cannot be parsed correctly
     */   
    private Message readPayload(SelectionKey key, int network) 
        throws IOException, BadPacketException {
        
        // If we've made it this far, the entire header has been
        // successfully read, so read the payload
        handlePayloadLimit(HEADER, PAYLOAD);
		
        SocketChannel channel = (SocketChannel)key.channel();
        // read in the payload
		if (channel.read(PAYLOAD) < 0) {
            // free memory as soon as possible
            HEADER.clear();
            PAYLOAD.clear();
            MessageReadErrorStat.CONNECTION_CLOSED_READING_PAYLOAD.
                incrementStat();
			throw new IOException("connection closed reading payload");
		}

        return handleReadPayload(HEADER, PAYLOAD, (Connection)key.attachment(), 
            network); 
    }
    
    private static void handlePayloadLimit(ByteBuffer header, 
        ByteBuffer payload) throws IOException {
        // If we've made it this far, the entire header has been
        // successfully read, so read the payload
    
        int length = header.getInt(LENGTH_OFFSET);  //little endian
    
   
        // If the length is hopelessly off (this includes lengths > than 2^31
        // bytes, throw an irrecoverable exception to cause this connection
        // to be closed.  Note we don't bother checking other fields here.
        if (length<0 || 
            length>MessageSettings.MAX_LENGTH.getValue())  {
            header.clear();

            MessageReadErrorStat.BAD_MESSAGE_LENGTH.incrementStat();
            throw new IOException("unreasonable message length");
        }
    
        // now set the read limit on the payload buffer so we'll only
        // read to the end of the message
        payload.limit(length);             
    }
    
    private static Message handleReadPayload(ByteBuffer header, 
        ByteBuffer payload, Connection conn, int network) throws IOException, 
        BadPacketException {
        // if there's still more to read, return to read it on the next pass
        if(payload.hasRemaining()) {
            return null;
        }
            
        try {
            return createMessage(header, payload, conn, network);
        } finally {
            // either the message was returned, or it was corrupt in some way,
            // so clear our buffers for new messages
            header.clear();
            payload.clear();
        }        
    }
    /**
     * Creates a new <tt>Message</tt> instance from the specified header,
     * payload, connection, and network.
     * 
     * @param header the <tt>ByteBuffer</tt> containing the read Gnutella header
     *  for the message
     * @param payloadBuffer the <tt>ByteBuffer</tt> containing the read message
     *  payload
     * @param conn the <tt>Connection</tt> the message was read over
     * @param network the "network" transport the message was received over,
     *  such as UDP, TCP, or multicast (UDP)
     * @return a new <tt>Message</tt> instance from the specified data
     * @throws BadPacketException if the data does not match the expected 
     *  message format
     */  
    private static Message createMessage(ByteBuffer header, 
        ByteBuffer payloadBuffer, Connection conn, int network) 
        throws BadPacketException {
        
        // make sure the limit on the payload buffer is set correctly    
        Assert.that(payloadBuffer.limit() == payloadBuffer.position(),
            "something wrong with buffer");
		header.flip();
		byte[] guid = new byte[GUID_SIZE];
		header.get(guid);
		int length = payloadBuffer.limit();
		byte func  = header.get(OPCODE_OFFSET);
		byte ttl   = header.get(TTL_OFFSET);
		byte hops  = header.get(HOPS_OFFSET);
		
		//  Check values. 
		byte softMax = conn.getSoftMax();
        
        ttl = checkFields(ttl, hops, softMax, func);

		// dispatch based on opcode.
		payloadBuffer.flip();     
		byte[] payload = new byte[length];
		payloadBuffer.get(payload, 0, length);
		return createMessage(guid, func, ttl, hops, length, payload, network);
	}

    /**
     * Implements <tt>MessageReader</tt> interface.  Since we're in non-blocking
     * mode and reading messages from channels and not streams, this always
     * throws <tt>IllegalStateException</tt>.
     * 
     * @throws IllegalStateException whenever this method is called, as 
     *  we shouldn't be reading from streams while in non-blocking mode
     */
    public Message createMessageFromTCP(InputStream is) 
        throws BadPacketException, IOException {
        throw new IllegalStateException("attempting blocking read in NIO mode");
    }

    /**
     * Does nothing because in the case of NIO, the Connection is added to 
     * the NIODispatcher during it's initialization.
     */
    public void startReading() throws IOException {}

    /**
     * Used only for testing.  
     */
    public Message read() throws IOException, BadPacketException {
        // silly code to make everything compile
        if(true) {
            throw new IllegalStateException("this method should not be called");
        }    
        return null;
    }

    /**
     * Used only for testing.
     */
    public Message read(int i) throws IOException, BadPacketException, 
        InterruptedIOException {
        // silly code to make everything compile
        if(true) {
            throw new IllegalStateException("this method should not be called");
        }    
        return null;
    }
}

