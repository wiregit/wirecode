package com.limegroup.gnutella.util;

import com.limegroup.gnutella.routing.*;

/**
 * Helper class that overrides getNumIntraUltrapeerConnections for
 * testing the horizon calculation and testing the new search
 * architecture.
 */
public class NewConnection extends TestConnection {    
    
    /**
     * Constant route table data for testing.
     */
    private final ManagedConnectionQueryInfo QUERY_INFO =
        new ManagedConnectionQueryInfo();
    
    /**
     * Constant query route table for testing.
     */
    protected final QueryRouteTable QRT = new QueryRouteTable();
    
    public static NewConnection createConnection(int connections) {
        return new NewConnection(connections);
    }

    protected NewConnection(int connections) {
        super(connections);
        QUERY_INFO.lastReceived = QRT;
    }

    public static NewConnection createConnection() {
        return new NewConnection(10);
    }

    //public NewConnection createHitConnection() {
        
    //}

    public boolean isGoodUltrapeer() {
        return true;
    }
    
    public boolean isUltrapeerQueryRoutingConnection() {
        return true;
    }
    
    public ManagedConnectionQueryInfo getQueryRouteState() {
        return QUERY_INFO;
    }

    public String toString() {
        return "NEW TEST CONNECTION";
    }
}


