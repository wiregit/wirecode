package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;

/**
 * The query routing information kept per connection.  Currently includes the 
 * route table received from that connection and the last route table sent.  
 * May maintain additional information in the future.
 * <b>This class is not synchronized.</b>
 */
public class ManagedConnectionQueryInfo {
    /** The last route table received along this connection, i.e., the query
     *  words that this connection will respond to, or null if no update has
     *  yet been received. */
    public  QueryRouteTable lastReceived=null;                                                             
    /** The last route sent along this connection, or null if none yet sent. */
    public QueryRouteTable lastSent=null;
}
