package com.limegroup.gnutella.connection;


import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * Abstract class that supplies a general method for creating messages that
 * is used by both the blocking and non-blocking implementations.
 */
public abstract class AbstractMessageReader implements MessageReader {
    
    /**
     * Constant for the length of Gnutella headers.
     */
    protected static final int HEADER_SIZE = 23;
    
    /**
     * Constant for the length of the GUID.
     */
    protected static final int GUID_SIZE = 16;
    
    /**
     * Constant for the offset of the Gnutella operation code.
     */
    protected static final int OPCODE_OFFSET = 16;
    
    /**
     * Constant for the offset of the TTL
     */
    protected static final int TTL_OFFSET = 17;
    
    /**
     * Constant for the offset of the hops.
     */
    protected static final int HOPS_OFFSET = 18;
    
    /**
     * Constant for the offset of the length.
     */
    protected static final int LENGTH_OFFSET = 19;
    
    /**
     * Constant for whether or not to record stats.
     */
    protected static final boolean RECORD_STATS = !CommonUtils.isJava118();

    protected static final void checkFields(byte ttl, byte hops, byte softMax,
        byte func) throws BadPacketException {
        //4. Check values.   These are based on the recommendations from the
        //   GnutellaDev page.  This also catches those TTLs and hops whose
        //   high bit is set to 0.
        if (hops<0) {
            if( RECORD_STATS )
                ReceivedErrorStat.INVALID_HOPS.incrementStat();
            throw new BadPacketException("Negative (or very large) hops");
        } else if (ttl<0) {
            if( RECORD_STATS )
                ReceivedErrorStat.INVALID_TTL.incrementStat();
            throw new BadPacketException("Negative (or very large) TTL");
        } else if ((hops >= softMax) && 
                 (func != Message.F_QUERY_REPLY) &&
                 (func != Message.F_PING_REPLY)) {
            if( RECORD_STATS )
                ReceivedErrorStat.HOPS_EXCEED_SOFT_MAX.incrementStat();
            throw BadPacketException.HOPS_EXCEED_SOFT_MAX;
        } else if (ttl+hops > (byte)14) {
            if( RECORD_STATS )
                ReceivedErrorStat.HOPS_AND_TTL_OVER_HARD_MAX.incrementStat();
            throw new BadPacketException("TTL+hops over hard max - maybe spam");
        } else if ((ttl+hops > softMax) && 
                 (func != Message.F_QUERY_REPLY) &&
                 (func != Message.F_PING_REPLY)) {
            ttl = (byte)(softMax - hops);  //overzealous client;
                                         //readjust accordingly
            Assert.that(ttl>=0);     //should hold since hops<=softMax ==>
                                     //new ttl>=0
        }
    }
    
    /**
     * Generalized method for creating messages.
     * 
     * @param guid the message GUID
     * @param func the message op code - specifies whether it's a query, a hit
     *  a ping, a pong, etc
     * @param ttl the time to live for the message
     * @param hops the number of hops the message has travelled
     * @param length the length of the message payload
     * @param payload the payload itself
     * @param flag for the transport layer used, such as TCP, UPD, or multicast,
     *  even though technically multicast is still UDP
     * 
     * @return the new <tt>Message</tt>
     * 
     * @throws BadPacketException if the message was invalid for any reason
     */
    protected static Message createMessage(byte[] guid, byte func, byte ttl, 
        byte hops, int length, byte[] payload, int network) 
        throws BadPacketException  {
        switch (func) {
            //TODO: all the length checks should be encapsulated in the various
            //constructors; Message shouldn't know anything about the various
            //messages except for their function codes.  I've started this
           //refactoring with PushRequest and PingReply.
           case Message.F_PING:
              if (length>0) { //Big ping
                 return new PingRequest(guid,ttl,hops,payload);
              }
              return new PingRequest(guid,ttl,hops);

           case Message.F_PING_REPLY:
               return PingReply.createFromNetwork(guid, ttl, hops, payload);
           case Message.F_QUERY:
               if (length<3) break;
               return QueryRequest.createNetworkQuery(
                   guid, ttl, hops, payload, network);
           case Message.F_QUERY_REPLY:
               if (length<26) break;
               return new QueryReply(guid,ttl,hops,payload);
           case Message.F_PUSH:
               return new PushRequest(guid,ttl,hops,payload, network);
           case Message.F_ROUTE_TABLE_UPDATE:
               //The exact subclass of RouteTableMessage returned depends on
               //the variant stored within the payload.  So leave it to the
               //static read(..) method of RouteTableMessage to actually call
               //the right constructor.
               return RouteTableMessage.read(guid, ttl, hops, payload);
           case Message.F_VENDOR_MESSAGE:
               if ((ttl != 1) || (hops != 0))
                   throw new BadPacketException("VM with bad ttl/hops: " +
                                                ttl + "/" + hops);
               return VendorMessage.deriveVendorMessage(guid, ttl, hops, 
                                                        payload);
           case Message.F_VENDOR_MESSAGE_STABLE:
               if ((ttl != 1) || (hops != 0))
                   throw new BadPacketException("VM with bad ttl/hops: " +
                                                ttl + "/" + hops);
               return VendorMessage.deriveVendorMessage(guid, ttl, hops, 
                                                        payload);
        }
              
        throw new BadPacketException("Unrecognized function code: "+func);
    }

}
