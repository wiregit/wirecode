package com.limegroup.gnutella.util;

import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.messages.*;

/**
 * Helper class that overrides getNumIntraUltrapeerConnections for
 * testing the horizon calculation and testing the new search
 * architecture.
 */
public class NewConnection extends TestConnection {    

    protected NewConnection(int connections, boolean b) {
        this(connections, new QueryRouteTable(), b);
    }

    protected NewConnection(int connections, 
                            QueryRouteTable qrt, boolean b) {
        super(connections, b);
        QRT = qrt;
        try {
            PrivilegedAccessor.setValue(qrp(), "_lastQRPTableReceived", QRT);
        } catch (IllegalAccessException e) {
            // should not happen
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            // should not happen
            e.printStackTrace();
        }
    }

    protected NewConnection(int connections, 
                            QueryRouteTable qrt) {
        this(connections, qrt, false);
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

    public String toString() {
        return "NEW TEST CONNECTION";
    }

    private static final class HitQueryRouteTable extends QueryRouteTable {
        public boolean contains(QueryRequest qr) {
            return true;
        }
    }
}


