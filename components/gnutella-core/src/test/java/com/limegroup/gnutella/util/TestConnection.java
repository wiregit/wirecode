package com.limegroup.gnutella.util;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.*;
import java.io.*;

/**
 * Helper class that overrides getNumIntraUltrapeerConnections for
 * testing the horizon calculation and testing the new search
 * architecture.
 */
public abstract class TestConnection extends ManagedConnection {
    
    private final int CONNECTIONS;
    
    private int _numQueries = 0;
    
    private int _totalTTL = 0;
    
    private boolean _receivedQuery;
    
    /**
     * Constant for the query route table for this connection.
     */
    private final QueryRouteTable QRT = new QueryRouteTable();
    
    TestConnection(int connections) {
        super("60.76.5.3", 4444);
        CONNECTIONS = connections;
    }
    
    public int getNumIntraUltrapeerConnections() {
        return CONNECTIONS;
    }
    
    public boolean isUltrapeerQueryRoutingConnection() {
        return false;
    }
    
    /**
     * Override the stability check method -- assume we're always stable.
     */
    public boolean isStable(long time) {
        return true;
    }
    
    /**
     * Accessor for the <tt>QueryRouteTable</tt> instance.
     */
    public QueryRouteTable getQueryRouteTable() {
        return QRT;
    }

    /**
     * Overridden to keep track of messages sent.
     */
    public void send(Message msg) {            
        if(msg instanceof RouteTableMessage) {
            try {
                QRT.update((RouteTableMessage)msg);
            } catch (BadPacketException e) {
                throw new IllegalArgumentException("should not have received a bad packet");
            }
        }
        if(!(msg instanceof QueryRequest)) return;
        
        _receivedQuery = true;
        _numQueries++;
        QueryRequest qr = (QueryRequest)msg;
        int ttl = qr.getTTL();
        _totalTTL += ttl;
    }
    
    int getNumQueries() {
        return _numQueries;
    }
    
    int getTotalTTL() {
        return _totalTTL;
    }
    
    boolean receivedQuery() {
        return _receivedQuery;
    }
}







