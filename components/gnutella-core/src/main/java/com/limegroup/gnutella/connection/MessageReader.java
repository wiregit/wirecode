package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.statistics.MessageReadErrorStat;

/**
 * Reads Gnutella messages from a SocketChannel, instantiating subclasses of
 * Message as appropriate by opcode.  Used by Connnection to read messages in a
 * non-blocking manner (which requires state).  Contains code formerly in
 * Message.read(InputStream).  Thread-safe; in a true single-thread core, this
 * would not be necessary.
 */
public final class MessageReader {
 
 	private static final byte F_PING                  = (byte)0x0;
	private static final byte F_PING_REPLY            = (byte)0x1;
	private static final byte F_PUSH                  = (byte)0x40;
	private static final byte F_QUERY                 = (byte)0x80;
	private static final byte F_QUERY_REPLY           = (byte)0x81;
	private static final byte F_ROUTE_TABLE_UPDATE    = (byte)0x30;
	private static final byte F_VENDOR_MESSAGE        = (byte)0x31;
	private static final byte F_VENDOR_MESSAGE_STABLE = (byte)0x32;
	
	/**
	  * Constant for the length of Gnutella headers.
	  */
	 private static final int HEADER_SIZE = 23;
	
	 /**
	  * Constant for the length of the GUID.
	  */
	 private static final int GUID_SIZE = 16;
	
	 /**
	  * Constant for the offset of the Gnutella operation code.
	  */
	 private static final int OPCODE_OFFSET = 16;
	
	 /**
	  * Constant for the offset of the TTL
	  */
	 private static final int TTL_OFFSET = 17;
	
	 /**
	  * Constant for the offset of the hops.
	  */
	 private static final int HOPS_OFFSET = 18;
	
	 /**
	  * Constant for the offset of the length.
	  */
	 private static final int LENGTH_OFFSET = 19;
	 
	
	/**
	 * Constant for the <tt>ByteBuffer</tt> for reading headers.
	 */
     private final ByteBuffer HEADER = 
    	ByteBuffer.allocate(HEADER_SIZE);
        
    /**
     * Constant for the <tt>ByteBuffer</tt> for reading payloads.
     */
     private final ByteBuffer PAYLOAD = 
        ByteBuffer.allocate(MessageSettings.MAX_LENGTH.getValue());
       

	/**
	 * Constant for the hard max TTL for incoming messages.  If the TTL+hops of 
	 * incoming messages exceeds this value, the message is dropped.
	 */
	private static final byte HARD_MAX = (byte)14;
    
	
    /**
     * Factory method for creating new <tt>MessageReader</tt>
     * instances for each connection.
     * 
     * @return a new <tt>MessageReader</tt> instance
     */
    public static MessageReader createReader() {
        return new MessageReader();
    }
    
	/**
	 * Ensure that this class cannot be constructed.
	 */
	private MessageReader() {
        HEADER.order(java.nio.ByteOrder.LITTLE_ENDIAN);
    }
		
	
	/**
	 * Creates a new <tt>Message</tt> that came in over TCP.
	 * 
	 * @param key the <tt>SelectionKey</tt> instance containing access to the channel
	 *    and the <tt>Connection</tt> for the message
	 * @return a new <tt>Message</tt> instance read in from the network
	 * @throws IOException if there is an IO error reading the message, such as the
	 *    connection being broken
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
            System.out.println("MessageReader::createMessage::read only part");
            return null;
        }
        
        // Now read the payload.  This will either read the entire payload and
        // return the message, or it will not read all of the payload, in which
        // case it will return null, leaving us to keep reading the rest on the
        // next pass.
        Message msg = readPayload(key, network);
        if(msg == null) {
            System.out.println("MessageReader::createMessage::read only part of payload");
        }
        return msg;
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
        System.out.println("MessageReader::readHeader");
        if(!HEADER.hasRemaining()) return true;
        
        if(channel.read(HEADER) < 0) {
            HEADER.clear();
            //PAYLOAD.clear();
            MessageReadErrorStat.CONNECTION_CLOSED_READING_HEADER.
                incrementStat();
            //Thread.dumpStack();
            throw new IOException("end of stream reading header");
        } else if(HEADER.hasRemaining()) {
            // if we still haven't read all of the header, return
            // to get the rest on the next pass.
            return false;
        }
        
        // if we get here, the header has been completely read
        
        // flip it for reading
        //HEADER.flip();
        return true;
    }
       
    private Message readPayload(SelectionKey key, int network) 
        throws IOException, BadPacketException {
        System.out.println("MessageReader::readPayload");
        SocketChannel channel = (SocketChannel)key.channel();
        // if we've made it this far, the entire header has been
        // successfully read, so read the payload
		
		int length = HEADER.getInt(LENGTH_OFFSET);  //little endian
        
       
		// If the length is hopelessly off (this includes lengths > than 2^31
		// bytes, throw an irrecoverable exception to cause this connection
		// to be closed.  Note we don't bother checking other fields here.
		if (length<0 || 
			length>MessageSettings.MAX_LENGTH.getValue())  {
            HEADER.clear();
            //PAYLOAD.clear();
            MessageReadErrorStat.BAD_MESSAGE_LENGTH.incrementStat();
			throw new IOException("unreasonable message length");
		}
        
        // now set the read limit on the payload buffer so we'll only
        // read to the end of the message
        PAYLOAD.limit(length); 
		
        // read in the payload
		if (channel.read(PAYLOAD) < 0) {
            // free memory as soon as possible
            HEADER.clear();
            PAYLOAD.clear();
            MessageReadErrorStat.CONNECTION_CLOSED_READING_PAYLOAD.
                incrementStat();
			throw new IOException("connection closed reading payload");
		}

        // if there's still more to read, return to read it on the next pass
		if(PAYLOAD.hasRemaining()) {
            //MessageReadErrorStat.INVALID_PAYLOAD.incrementStat();
			return null;
		}
			
		try {
        
            return createMessage(HEADER, PAYLOAD, 
                (Connection)key.attachment(), network);
        } finally {
            // either the message was returned, or it was corrupt in some way,
            // so clear our buffers for new messages
            HEADER.clear();
            PAYLOAD.clear();
        }
    }
    

    
    /**
     * Creates a new <tt>Message</tt> instance from the specified header,
     * payload, connection, and network.
     * 
     * @param header
     * @param payloadBuffer
     * @param conn
     * @param network
     * @return
     * @throws BadPacketException
     */  
    private Message 
   		createMessage(ByteBuffer header, ByteBuffer payloadBuffer, 
            Connection conn, int network) 
        throws BadPacketException {
            
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
		if (hops<0) {
            MessageReadErrorStat.NEGATIVE_HOPS.incrementStat();
			throw new BadPacketException("negative hops");
		} else if (ttl<0) {
            MessageReadErrorStat.NEGATIVE_TTL.incrementStat();
			return null;
		} else if ((hops >= softMax) && 
				 (func != F_QUERY_REPLY) &&
				 (func != F_PING_REPLY)) {
			
            MessageReadErrorStat.HOPS_OVER_SOFT_MAX.incrementStat();
            throw new BadPacketException("hops over soft max");
		}
		else if (ttl+hops > HARD_MAX) {
            
            MessageReadErrorStat.HOPS_PLUS_TTL_OVER_HARD_MAX.incrementStat();
            throw new BadPacketException("ttl+hops over hard max");
		} else if ((ttl+hops > softMax) && 
				 (func != F_QUERY_REPLY) &&
				 (func != F_PING_REPLY)) {
                     
            // overzealous client, bump down the TTL           
			ttl = (byte)(softMax - hops);
			Assert.that(ttl>=0);     //should hold since hops<=softMax ==>
									 //new ttl>=0
		}
		
		// dispatch based on opcode.
		payloadBuffer.flip();     
		byte[] payload = new byte[length];
		payloadBuffer.get(payload, 0, length);
		return createMessage(guid, func, ttl, hops, length, payload, network);
	}
        
    private Message createMessage(byte[] guid, byte func, byte ttl, 
        byte hops, int length, byte[] payload, int network) 
        throws BadPacketException  {
		switch (func) {
		    //TODO: all the length checks should be encapsulated in the various
		    //constructors; Message shouldn't know anything about the various
		    //messages except for their function codes.  I've started this
		   //refactoring with PushRequest and PingReply.
	       case F_PING:
		      if (length>0) { //Big ping
			     return new PingRequest(guid,ttl,hops,payload);
			  }
			  return new PingRequest(guid,ttl,hops);

		   case F_PING_REPLY:
			   return PingReply.createFromNetwork(guid, ttl, hops, payload);
		   case F_QUERY:
			   if (length<3) break;
			   return QueryRequest.createNetworkQuery(
				   guid, ttl, hops, payload, network);
		   case F_QUERY_REPLY:
			   if (length<26) break;
			   return new QueryReply(guid,ttl,hops,payload);
		   case F_PUSH:
			   return new PushRequest(guid,ttl,hops,payload, network);
		   case F_ROUTE_TABLE_UPDATE:
			   //The exact subclass of RouteTableMessage returned depends on
			   //the variant stored within the payload.  So leave it to the
			   //static read(..) method of RouteTableMessage to actually call
			   //the right constructor.
			   return RouteTableMessage.read(guid, ttl, hops, payload);
		   case F_VENDOR_MESSAGE:
			   if ((ttl != 1) || (hops != 0))
				   throw new BadPacketException("VM with bad ttl/hops: " +
												ttl + "/" + hops);
			   return VendorMessage.deriveVendorMessage(guid, ttl, hops, 
														payload);
		   case F_VENDOR_MESSAGE_STABLE:
			   if ((ttl != 1) || (hops != 0))
				   throw new BadPacketException("VM with bad ttl/hops: " +
												ttl + "/" + hops);
			   return VendorMessage.deriveVendorMessage(guid, ttl, hops, 
														payload);
	   }
			  
		 throw new BadPacketException("Unrecognized function code: "+func);
	}
}
