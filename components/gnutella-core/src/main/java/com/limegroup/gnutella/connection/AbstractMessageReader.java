package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.io.InputStream;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.routing.*;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class AbstractMessageReader implements MessageReader {

    protected static final byte F_PING                  = (byte)0x0;
    protected static final byte F_PING_REPLY            = (byte)0x1;
    protected static final byte F_PUSH                  = (byte)0x40;
    protected static final byte F_QUERY                 = (byte)0x80;
    protected static final byte F_QUERY_REPLY           = (byte)0x81;
    protected static final byte F_ROUTE_TABLE_UPDATE    = (byte)0x30;
    protected static final byte F_VENDOR_MESSAGE        = (byte)0x31;
    protected static final byte F_VENDOR_MESSAGE_STABLE = (byte)0x32;
    
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageReader#createMessageFromTCP(java.io.InputStream)
     */
    public abstract Message createMessageFromTCP(InputStream is)
        throws BadPacketException, IOException;
    
    protected Message createMessage(byte[] guid, byte func, byte ttl, 
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
