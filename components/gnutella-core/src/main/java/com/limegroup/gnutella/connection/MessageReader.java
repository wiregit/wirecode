package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.MessageSettings;

import java.nio.*;
import java.nio.channels.*;
import java.io.IOException;

/**
 * Reads Gnutella messages from a SocketChannel, instantiating subclasses of
 * Message as appropriate by opcode.  Used by Connnection to read messages in a
 * non-blocking manner (which requires state).  Contains code formerly in
 * Message.read(InputStream).  Thread-safe; in a true single-thread core, this
 * would not be necessary.
 */
public final class MessageReader {
 
 	private static final byte F_PING                                              = (byte)0x0;
	private static final byte F_PING_REPLY                              = (byte)0x1;
	private static final byte F_PUSH                                            = (byte)0x40;
	private static final byte F_QUERY                                          = (byte)0x80;
	private static final byte F_QUERY_REPLY                           = (byte)0x81;
	private static final byte F_ROUTE_TABLE_UPDATE         = (byte)0x30;
	private static final byte F_VENDOR_MESSAGE                  = (byte)0x31;
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
     //private static final ByteBuffer HEADER = 
    	//ByteBuffer.allocate(HEADER_SIZE);



	//private static MessageReader INSTANCE = 
		//new MessageReader();
		
	private static final byte HARD_MAX = (byte)14;
	
	/**
	 * Ensure that this class cannot be constructed.
	 */
	private MessageReader() {}
		
	static  {
		//HEADER.order(java.nio.ByteOrder.LITTLE_ENDIAN);
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
	public static Message createMessageFromTCP(SelectionKey key) 
		throws IOException, BadPacketException {
		return createMessage(key, Message.N_TCP);
	}

	/**
	 * Creates a new <tt>Message</tt> instance from the specified <tt>SelectionKey</tt>
	 * and it's associated channel and connection.
	 * 
	 * @param key  the <tt>SelectionKey</tt> for the new data
	 * @param network the network interface this message arrived over, such as TCP,
	 *     UDP, multicast, etc.
	 * @return a new <tt>Message</tt> instance created from the specified data
	 * @throws IOException if there is an IO error reading the data or writing it to the
	 * 	new message
	 * @throws BadPacketException if the data received from the network is invalid for 
	 *    any reason
	 */
	private static Message createMessage(SelectionKey key, int network) 
		throws IOException, BadPacketException {
		
		ByteBuffer HEADER = 
			ByteBuffer.allocate(HEADER_SIZE);
		HEADER.order(java.nio.ByteOrder.LITTLE_ENDIAN);
		SocketChannel channel = (SocketChannel)key.channel();
		int n = channel.read(HEADER);
		if(n < 0) {
	        throw new IOException("connection closed reading header");    
		}
		if(HEADER.hasRemaining()) {
			throw new IOException("invalid header");
		}
		
		int length = HEADER.getInt(LENGTH_OFFSET);  //little endian
		//If the length is hopelessly off (this includes lengths > than 2^31
		//bytes, throw an irrecoverable exception to cause this connection
		//to be closed.  Note we don't bother checking other fields here.
		if (length<0 || 
			length > MessageSettings.MAX_LENGTH.getValue())  {
			throw new IOException("Unreasonable message length: "+length);
		}
		
		//Allocate enough space for payload.  TODO2: avoid allocating
		//buffers each time.  Be careful not to use payload.hasRemaining()
		//and payload.capacity() below.
		ByteBuffer payload = ByteBuffer.allocate(length);            
		n = channel.read(payload);
		if (n<0) {
			throw new IOException("connection closed reading payload");
		}

		if(payload.hasRemaining()) {
			throw new IOException("invalid payload");
		}
		
		try {
			return createMessage(HEADER, payload, (Connection)key.attachment(), network);
		}  finally {                
			// be sure to execute even if bad packet.
			HEADER.clear();
		}
   }
   
   private static Message 
   		createMessage(ByteBuffer header, ByteBuffer payloadBuffer, Connection conn, 
   									 int network) 
        throws IOException, BadPacketException {
        	System.out.println("MessageReader::createMessage");
			header.flip();
			byte[] guid = new byte[GUID_SIZE];
			header.get(guid);
			int length = payloadBuffer.capacity();
			byte func  = header.get(OPCODE_OFFSET);
			byte ttl       = header.get(TTL_OFFSET);
			byte hops = header.get(HOPS_OFFSET);
			
			//  Check values. 
			byte softMax = conn.getSoftMax();
			if (hops<0) {
				throw new BadPacketException("Negative (or very large) hops");
			} else if (ttl<0) {
				throw new BadPacketException("Negative (or very large) TTL");
			} else if ((hops >= softMax) && 
					 (func != F_QUERY_REPLY) &&
					 (func != F_PING_REPLY)) {
				throw BadPacketException.HOPS_EXCEED_SOFT_MAX;
			}
			else if (ttl+hops > HARD_MAX)
				throw new BadPacketException("TTL+hops exceeds hard max; probably spam");
			else if ((ttl+hops > softMax) && 
					 (func != F_QUERY_REPLY) &&
					 (func != F_PING_REPLY)) {
				ttl=(byte)(softMax - hops);  //overzealous client;
											 //readjust accordingly
				Assert.that(ttl>=0);     //should hold since hops<=softMax ==>
										 //new ttl>=0
			}
			
			//Dispatch based on opcode.
			payloadBuffer.flip();     
			byte[] payload = new byte[payloadBuffer.capacity()];
			payloadBuffer.get(payload, 0, payload.length);
			Message msg = createMessage(guid, func, ttl, hops, length, payload, network);
			System.out.println("MessageReader::createMessage:::::CREATED MESSAGE!!!!!!!!!");
			return msg;
			//return createMessage(guid, func, ttl, hops, length, payload, network);
        }
        
        private static Message createMessage(byte[] guid, byte func, byte ttl, 
        																		 byte hops, int length, byte[] payload,
        																		 int network) throws BadPacketException  {
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
