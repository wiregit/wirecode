package com.limegroup.gnutella.util;

import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.messages.*;

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
     * Constant for the <tt>QueryRouteTable</tt> for this
     * connection -- can be used by subclasses.
     */
    protected final QueryRouteTable QRT;

    protected NewConnection(int connections) {
        this(connections, new QueryRouteTable());
    }

    protected NewConnection(int connections, 
                            QueryRouteTable qrt) {
        super(connections);
        QRT = qrt;
    }

    /**
     * Creates a generic <tt>NewConnection</tt> for testing with 
     * all of the default values and the specified number of 
     * connections.
     */
    public static NewConnection createConnection(int connections) {
        return new NewConnection(connections, new QueryRouteTable());
    }

    /**
     * Creates a generic <tt>NewConnection</tt> for testing with 
     * all of the default values.
     */
    public static NewConnection createConnection() {
        return new NewConnection(10, new QueryRouteTable());
    }

    /**
     * Creates a utility connection that has a hit in its 
     * query route tables for every query.
     */
    public static NewConnection createHitConnection() {
        return new NewConnection(10, new HitQueryRouteTable());        
    }

    public boolean isGoodUltrapeer() {
        return true;
    }
    
    public boolean isUltrapeerQueryRoutingConnection() {
        return true;
    }
    
    public QueryRouteTable getQueryRouteTableReceived() {
        return QRT;
    }

    public boolean hitsQueryRouteTable(QueryRequest query) {
        return QRT.contains(query);
    }


    public String toString() {
        return "NEW TEST CONNECTION";
    }

    private static final class HitQueryRouteTable extends QueryRouteTable {
        public boolean contains(QueryRequest qr) {
            return true;
        }
    }
}


