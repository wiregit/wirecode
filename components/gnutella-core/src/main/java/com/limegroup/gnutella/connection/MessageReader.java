package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.routing.RouteTableMessage;

import java.nio.*;
import java.nio.channels.*;
import java.io.*;

/**
 * Reads Gnutella messages from a SocketChannel, instantiating subclasses of
 * Message as appropriate by opcode.  Used by Connnection to read messages in a
 * non-blocking manner (which requires state).  Contains code formerly in
 * Message.read(InputStream).  Not thread-safe.  
 */
public class MessageReader {
    final int READING_HEADER_STATE=0;
    final int READING_PAYLOAD_STATE=1;

    /** Where to get data */
    private SocketChannel channel;
    /** Are we working on the header or payload? */
    private int state;
    /** The header being read (partial or complete) */
    private ByteBuffer header;
    /** The payload being read, or possibly null if still reading header */
    private ByteBuffer payload;

    /** Reads messages from channel; nobody else may read from channel. */
    public MessageReader(SocketChannel channel) {
        this.channel=channel;
        this.state=READING_HEADER_STATE;
        header=ByteBuffer.allocate(Message.HEADER_SIZE);
        header.order(java.nio.ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Returns the next message available on the channel, or null if none.
     * Modifies channel.  Doesn't block if channel is non-blocking.  
     *
     * @return the next message or null
     * @exception BadPacketException got a bad packet but recovered
     * @exception IOException connection closed or unrecoverable error
     */
    public Message read() throws BadPacketException, IOException {
        if (state==READING_HEADER_STATE) {
            readHeader();
        } 
        if (state==READING_PAYLOAD_STATE) { //NOT "else if"!
            return readPayload();           //may be null, or BadPacketException
        }
        return null;
    }
    
    /** Reads as much of the header as possible.  If done, sets the state to
     *  READING_PAYLOAD_STATE and allocates payload. */
    private void readHeader() throws IOException {
        int n=channel.read(header);   
        if (n<0)
            throw new IOException("Connection closed");
        //System.out.println("Read "+n+" bytes of header ("+header.remaining()+")");
        if (! header.hasRemaining()) {
            //Done with header, transfer states to begin reading payload.
            int length=header.getInt(Message.LENGTH_OFFSET);  //little endian
            //If the length is hopelessly off (this includes lengths > than 2^31
            //bytes, throw an irrecoverable exception to cause this connection
            //to be closed.  Note we don't bother checking other fields here.
            if (length<0 || length>SettingsManager.instance().getMaxLength())
                throw new IOException("Unreasonable message length: "+length);
            //Allocate enough space for payload.  TODO2: avoid allocating
            //buffers each time.  Be careful not to use payload.hasRemaining()
            //and payload.capacity() below.
            payload=ByteBuffer.allocate(length);            
            state=READING_PAYLOAD_STATE;;
        }
    }

    /** Reads as much of the payload as possible.  If done, sets the state to
     *  READING_HEADER_STATE and clears header.  Returns the Message
     *  read. */
    private Message readPayload() throws BadPacketException, IOException {
        int n=channel.read(payload);
        if (n<0)
            throw new IOException("Connection closed");
        //System.out.println("Read "+n+" bytes of payload ("+payload.remaining()+")");
        if (! payload.hasRemaining()) {
            try {
                return createMessage();
            } finally {                
                //Be sure to execute even if bad packet.
                header.clear();
                state=READING_HEADER_STATE;
            }
        }
        return null;
    }

    /** Tries to create a Message from header/payload.  Assumes this is in
     *  READING_PAYLOAD_STATE with nothing left to read. */
    private Message createMessage() throws BadPacketException {
        //Unpack.
        header.flip();
        byte[] guid=new byte[Message.GUID_SIZE];
        header.get(guid);
        int length=payload.capacity();
        byte func=header.get(Message.OPCODE_OFFSET);
        byte ttl=header.get(Message.TTL_OFFSET);
        byte hops=header.get(Message.HOPS_OFFSET);

        //Check values.  These are based on the recommendations from the
        //GnutellaDev page.  This also catches those TTLs and hops whose high
        //bit is set to 0.  TODO: TTL/hops checks should be done later.
        byte softMax=SettingsManager.instance().getSoftMaxTTL();
        byte hardMax=SettingsManager.instance().getMaxTTL();
        if (hops<0)
            throw new BadPacketException("Negative (or very large) hops");
        else if (ttl<0)
            throw new BadPacketException("Negative (or very large) TTL");
        else if (hops>softMax)
            throw new BadPacketException("Hops already exceeds soft maximum");
        else if (ttl+hops > hardMax)
            throw new BadPacketException("TTL+hops exceeds hard max");
        else if (ttl+hops > softMax) {
            ttl=(byte)(softMax - hops);  //overzealous client;
                                         //readjust accordingly
            Assert.that(ttl>=0);     //should hold since hops<=softMax ==>
                                     //new ttl>=0
        }

        //Dispatch based on opcode.   
        payload.flip();     
        byte[] payloadBytes=new byte[payload.capacity()];
        payload.get(payloadBytes, 0, payloadBytes.length);
        switch (func) {
            //TODO: all the length checks should be encapsulated in the various
            //constructors; Message shouldn't know anything about the various
            //messages except for their function codes.  I've started this
            //refactoring with PushRequest and PingReply.
            case Message.F_PING:
                if (Message.PARSE_GROUP_PINGS && length>=15) {
				    // Build a GroupPingRequest
                    return new GroupPingRequest(guid,ttl,hops,payloadBytes);
				}
				else if (length>0) //Big ping
                    return new PingRequest(guid,ttl,hops,payloadBytes);
                return new PingRequest(guid,ttl,hops);

            case Message.F_PING_REPLY:
                return new PingReply(guid,ttl,hops,payloadBytes);
            case Message.F_QUERY:
                if (length<3) break;
                return new QueryRequest(guid,ttl,hops,payloadBytes);
            case Message.F_QUERY_REPLY:
                if (length<26) break;
                return new QueryReply(guid,ttl,hops,payloadBytes);
            case Message.F_PUSH:
                return new PushRequest(guid,ttl,hops,payloadBytes);
            case Message.F_ROUTE_TABLE_UPDATE:
                //The exact subclass of RouteTableMessage returned depends on
                //the variant stored within the payload.  So leave it to the
                //static read(..) method of RouteTableMessage to actually call
                //the right constructor.
                return RouteTableMessage.read(guid, ttl, hops, payloadBytes);
        }
        throw new BadPacketException("Unrecognized function code: "+func);
    }

}
