package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;

/**
 * This class handles access to query routing tables for a given connection.
 * Any methods that modify the underlying query routing table are synchronized
 * on <tt>this</tt>.  Therefore, if you need to iterate over the underlying
 * table, you can also synchronize on <tt>this</tt> instance, accessible from
 * the associated <tt>Connection</tt>.
 */
public final class QRPHandler {


    /**
     * Variable for the <tt>QueryRouteTable</tt> received for this 
     * connection.
     */
    private QueryRouteTable _lastQRPTableReceived;
    
    /**
     * Factory constructor for creating new <tt>QRPHandler</tt> instances.
     * 
     * @return a new <tt>QRPHandler</tt> instance
     */
    public static QRPHandler createHandler() {
        return new QRPHandler();    
    }
    
    /**
     * Creates a new <tt>QRPHandler</tt> instance to handle all query routing
     * management.
     */
    private QRPHandler() {}
        
    /**
     * Resets the query route table for this connection.  The new table
     * will be of the size specified in <tt>rtm</tt> and will contain
     * no data.  If there is no <tt>QueryRouteTable</tt> yet created for
     * this connection, this method will create one.
     *
     * @param rtm the <tt>ResetTableMessage</tt> 
     * @throws NullPointerException if <tt>rtm</tt> is <tt>null</tt>
     */
    public synchronized void resetQueryRouteTable(ResetTableMessage rtm) {
        if(rtm == null) {
            throw new NullPointerException("cannot accept null table");
        }
        if(_lastQRPTableReceived == null) {
            _lastQRPTableReceived = new QueryRouteTable(rtm.getTableSize());
        } else {
            _lastQRPTableReceived.reset(rtm);
        }
    }

    /**
     * Patches the <tt>QueryRouteTable</tt> for this connection.
     *
     * @param ptm the patch with the data to update
     * @throws NullPointerException if <tt>ptm</tt> is <tt>null</tt>
     */
    public synchronized void patchQueryRouteTable(PatchTableMessage ptm) {
        if(ptm == null) {
            throw new NullPointerException("cannot accept null table");
        }
        // we should always get a reset before a patch, but 
        // allocate a table in case we don't
        if(_lastQRPTableReceived == null) {
            _lastQRPTableReceived = new QueryRouteTable();
        }
        try {
            _lastQRPTableReceived.patch(ptm);
        } catch(BadPacketException e) {
            // TODO: at least record a stat/log
            // not sure what to do here!!
        }                    
    }


    /**
     * Determines whether or not the specified <tt>QueryRequest</tt>
     * instance has a hit in the query routing tables.  If this 
     * connection has not yet sent a query route table, this returns
     * <tt>false</tt>.
     *
     * @param query the <tt>QueryRequest</tt> to check against
     *  the tables
     * @return <tt>true</tt> if the <tt>QueryRequest</tt> has a hit
     *  in the tables, otherwise <tt>false</tt>
     */
    public boolean hitsQueryRouteTable(QueryRequest query) {
        if(_lastQRPTableReceived == null) return false;
        return _lastQRPTableReceived.contains(query);
    }

    /**
     * Accessor for the <tt>QueryRouteTable</tt> received along this 
     * connection.  Can be <tt>null</tt> if no query routing table has been 
     * received yet.
     *
     * @return the last <tt>QueryRouteTable</tt> received along this
     *  connection
     */
    public QueryRouteTable getQueryRouteTableReceived() {
        return _lastQRPTableReceived;
    }
    
    /**
     * Accessor for the last QueryRouteTable's percent full.
     */
    public double getQueryRouteTablePercentFull() {
        return _lastQRPTableReceived == null ?
            0 : _lastQRPTableReceived.getPercentFull();
    }

}

