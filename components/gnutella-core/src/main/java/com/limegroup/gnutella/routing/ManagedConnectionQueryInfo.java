package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;


//Please note that &#60; and &#62; are the HTML escapes for '<' and '>'.

/**
 * The &#60;timestamp, QueryRouteTable&#62; pair kept per connection for query
 * routing purposes.  The QueryRouteTable may be null.  In the future, this
 * might be expanded to tuples &#60;timestamp, QueryRouteTable,
 * QueryRouteTable_uncommitted&#62; to allow atomic query route table
 * updates.  <b>This class is not synchronized.</b><p>
 *
 * MessageRouter currently maintains the bijection between ManagedConnection and
 * ManagedConnectionQueryInfo's, but ManagedConnection could maintain the
 * reference itself.  
 */
public class ManagedConnectionQueryInfo {
    /** The time we can next send RouteTableMessage's along this connection. */
    private long nextUpdateTime=0l;
    /** The query words that this connection will respond to. */
    private QueryRouteTable qrt=null;
    //private QueryRouteTable uncommittedQrt;

    /** Creates a new ManagedConnectionQueryInfo that needs update but has
     *  no route table. */
    public void ManagedConnectionQueryInfo() { 
    }


    ///////////////////////// QueryRouteTable interface ////////////////////

    /** Returns qrt.contains(qr), or true if qrt is null. */
    public boolean contains(QueryRequest qr) {
        if (qrt==null)
            return true;  //TODO: should we return false?
        else
            return qrt.contains(qr);
    }

    /** Updates the state of this according to m.  This often involves
     *  delegating to QueryRouteTable.
     *      @modifies this */
    public void update(RouteTableMessage m) {
        if ((m instanceof ResetTableMessage)) {
            ResetTableMessage reset=(ResetTableMessage)m;
            //TODO!: this only works if TableTTL is 0.
            qrt=new QueryRouteTable(QueryRouteTable.DEFAULT_TABLE_TTL,
                                    reset.getTableSize());
        } else if (qrt!=null) {
            qrt.update(m);
        } else {
            //Do nothing.  TODO: eventually we will swap qrt and uncommittedQrt
            //if we get a COMMIT message.
        }
    }

    /** Returns this' current QueryRouteTable, or null if it hasn't been
     *  updated. */
    public QueryRouteTable getQueryRouteTable() {
        return qrt;
    }
}
