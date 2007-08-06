package com.limegroup.gnutella.util;

import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;

/**
 * Helper class that overrides getNumIntraUltrapeerConnections for
 * testing the horizon calculation and testing the new search
 * architecture.
 */
public abstract class TestConnection extends ManagedConnection {
    
    private boolean _queriesMustBeInRoutingTables;

    private final int CONNECTIONS;
    
    private int _numQueries = 0;
    
    private int _totalTTL = 0;
    
    private boolean _receivedQuery;
    
     /**
     * Constant for the <tt>QueryRouteTable</tt> for this
     * connection -- can be used by subclasses.
     */
    protected QueryRouteTable QRT;
    
    TestConnection(int connections) {
        super("60.76.5.3", 4444, ConnectType.PLAIN, ProviderHacks
                .getConnectionManager(), ProviderHacks.getNetworkManager(),
                ProviderHacks.getQueryRequestFactory(), ProviderHacks
                        .getHeadersFactory(), ProviderHacks
                        .getHandshakeResponderFactory(), ProviderHacks
                        .getQueryReplyFactory(), ProviderHacks
                        .getMessageDispatcher(), ProviderHacks
                        .getNetworkUpdateSanityChecker(), ProviderHacks
                        .getUdpService(), ProviderHacks.getMessageRouter(),
                ProviderHacks.getSearchResultHandler());
        CONNECTIONS = connections;
    }
    
    /**
     * @param connections2
     * @param b
     */
    public TestConnection(int connections, boolean b) {
        this(connections);
        _queriesMustBeInRoutingTables = b;
        // TODO Auto-generated constructor stub
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

    public void originateQuery(QueryRequest query) {
        send(query);
    }

    /**
     * Overridden to keep track of messages sent.
     */
    public void send(Message msg) {         
        if(msg instanceof ResetTableMessage) {
            QRT.reset((ResetTableMessage)msg);
        } else if(msg instanceof PatchTableMessage) {
            try {
                QRT.patch((PatchTableMessage)msg);
            } catch (BadPacketException e) {
                throw new IllegalArgumentException("should not have received a bad packet");
            }
        }

        if(!(msg instanceof QueryRequest)) return;
        
        _receivedQuery = true;
        _numQueries++;
        QueryRequest qr = (QueryRequest)msg;
        if(_queriesMustBeInRoutingTables && !shouldForwardQuery(qr))  {
            throw new IllegalArgumentException("received query that's not in table1: "+qr+" "+this);
        }
        int ttl = qr.getTTL();
        _totalTTL += ttl;
    }
    
    public int getNumQueries() {
        return _numQueries;
    }
    
    public int getTotalTTL() {
        return _totalTTL;
    }
    
    public boolean receivedQuery() {
        return _receivedQuery;
    }
}







