package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;

/**
 * The query routing information kept per connection.  Currently includes the 
 * route table received from that connection and the last route table sent.  
 * May maintain additional information in the future.
 * <b>This class is not synchronized.</b><p>
 *
 * MessageRouter currently maintains the bijection between ManagedConnection and
 * ManagedConnectionQueryInfo's, but ManagedConnection could maintain the
 * reference itself.  
 */
public class ManagedConnectionQueryInfo {
    /** The last route table received along this connection, i.e., the query
     *  words that this connection will respond to, e.g., */
    public final QueryRouteTable lastReceived=new QueryRouteTable(
         QueryRouteTable.DEFAULT_TABLE_SIZE, QueryRouteTable.DEFAULT_INFINITY);
                                                             
    /** The last route sent along this connection, or null if none yet sent. */
    public QueryRouteTable lastSent=null;
}
