package com.limegroup.gnutella.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.settings.UltrapeerSettings;

/**
 * Helper class that supplies the list of connections for searching.
 */
@SuppressWarnings("unchecked")
public final class TestConnectionManager extends ConnectionManager {
    
    /**
     * The list of ultrapeer <tt>Connection</tt> instances
     */
    private final List CONNECTIONS = new LinkedList();
    
    /**
     * The list of leaf <tt>Connection</tt> instances
     */
    private final List LEAF_CONNECTIONS = new LinkedList();
    
    /**
     * Constant for whether or not this should be considered an
     * Ultrapeer.
     */
    private final boolean ULTRAPEER;

    /**
     * Constant for the number of Ultrapeer connections that the 
     * test connection manager should maintain.
     */
    private final int NUM_CONNECTIONS;

    /**
     * Constant for the number of leaf connections that the test 
     * connection manager should maintain.
     */
    private final int NUM_LEAF_CONNECTIONS;

    /**
     * Constant array for the keywords that I should have (this node).
     */
    private final String[] MY_KEYWORDS;

    /**
     * Constant array for the keywords that I should have (this node)
     * by default, unless the caller specifies otherwise.
     */
    private static final String[] DEFAULT_MY_KEYWORDS = {
        "me",
    };

    /**
     * Constant array for the keywords for Ultrapeers to use.
     */
    private static final String[] ULTRAPEER_KEYWORDS = {
        "qwe", "wer", "ert", "rty", "tyu", 
        "yui", "uio", "iop", "opa ", "pas", 
        "asd", "sdf", "dfg", "fgh", "ghj", 
        "hjk", "jkl", "klz", "lzx", "zxc", 
        "xcv", "cvb", "vbn", "bnm", "qwer",
        "wert", "erty", "rtyu", "tyui", "yuio",        
    };


    /**
     * Constant array for the keywords for leaves to use.
     */
    private static final String[] LEAF_KEYWORDS = {
        "this", "is", "a", "test", "for", 
        "query", "routing", "in", "all", "its", 
        "forms", "including", "both", "leaves", "and", 
        "Ultrapeers", "which", "should", "both", "work", 
        "like", "we", "expect", "them", "to", 
        "at", "least", "in", "theory", "and", 
        "hopefully", "in", "fact", "as", "well", 
        "but", "it's", "hard", "to", "know", 
    };
   

    /**
     * Array of keywords that should not match anything in the routing
     * tables.
     */
    private static final String[] UNMATCHING_KEYWORDS = {
        "miss", "NCWEPHCE", "IEYWHFDSNC", "UIYRIEH", "dfjaivuih",
    };

    /**
     * Factory method for generating a test manager with varied route tables from leaves.
     * 
     * @return a new <tt>TestConnectionManager</tt> with varied leaf route tables for
     *   use in tests that require varied tables
     */
    public static TestConnectionManager createManagerWithVariedLeaves() {
        return new TestConnectionManager(20, true, 20, 
            UltrapeerSettings.MAX_LEAVES.getValue(), DEFAULT_MY_KEYWORDS, true);
    }

    /**
     * Creates a standard manager.
     */
    public static TestConnectionManager createManager() {
        return new TestConnectionManager(20);
    }

    /**
     * Convenience constructor that creates a new 
     * <tt>TestConnectionManager</tt> with all of the default settings.
     */
    public TestConnectionManager() {
        this(20);
    }

    public TestConnectionManager(String[] myKeywords) {
        this(20, true, 20, UltrapeerSettings.MAX_LEAVES.getValue(), myKeywords, false);
    }
    
    /**
     * Creates a new <tt>ConnectionManager</tt> with a list of 
     * <tt>TestConnection</tt>s for testing.
     *
     * @param numNewConnections the number of new connections to 
     *  include in the set of connections
     */
    public TestConnectionManager(int numNewConnections) {
        this(numNewConnections, true);
    }

    /**
     * Creates a new <tt>ConnectionManager</tt> with a list of 
     * <tt>TestConnection</tt>s for testing.
     *
     * @param numNewConnections the number of new connections to 
     *  include in the set of connections
     * @param ultrapeer whether or not this should be considered
     *  an ultrapeer
     */
    public TestConnectionManager(int numNewConnections, boolean ultrapeer) {
        this(numNewConnections, ultrapeer, 20, UltrapeerSettings.MAX_LEAVES.getValue(), 
            DEFAULT_MY_KEYWORDS, false);
    }

    /**
     * Creates a new <tt>ConnectionManager</tt> with a list of 
     * <tt>TestConnection</tt>s for testing.
     *
     * @param numNewConnections the number of new connections to 
     *  include in the set of connections
     * @param ultrapeer whether or not this should be considered
     *  an ultrapeer
     * @param useVaried boolean specifying whether or not leaves should
     *   have variable routing tables
     */
    public TestConnectionManager(int numNewConnections, boolean ultrapeer,
                                 int numConnections, int numLeafConnections,
                                 String[] myKeywords, boolean useVaried) {
        super(ProviderHacks.getNetworkManager());
        NUM_CONNECTIONS = numConnections;
        NUM_LEAF_CONNECTIONS = numLeafConnections;
        MY_KEYWORDS = myKeywords;
        for(int i=0; i<NUM_CONNECTIONS; i++) {
            Connection curConn = null;
            if(i < numNewConnections) {
                curConn = 
                    new UltrapeerConnection(new String[]{ULTRAPEER_KEYWORDS[i]});
            } else {
                curConn = new OldConnection(15); 
            }
            CONNECTIONS.add(curConn);            
        }

        // now, give ourselves the desired number of leaves
        for(int i=0; i<NUM_LEAF_CONNECTIONS; i++) {
            Connection conn;
            if(useVaried && i >= (NUM_LEAF_CONNECTIONS/2)) {
                conn = LeafConnection.createAltLeafConnection();
            } else {
                conn = LeafConnection.createWithKeywords(new String[]{LEAF_KEYWORDS[i]});
            }
            LEAF_CONNECTIONS.add(conn);
        }
        ULTRAPEER = ultrapeer;
    }

    /**
     * Test to make sure that the given <tt>QueryRouteTable</tt> has matches
     * for all of the expected keywords and that it doesn't have matches
     * for any of the unexpected keywords.
     *
     * @param qrt the <tt>QueryRouteTable</tt> instance to test
     */
    public boolean runQRPMatch(QueryRouteTable qrt) {
        for(int i=0; i<MY_KEYWORDS.length; i++) {
            QueryRequest qr = ProviderHacks.getQueryRequestFactory().createQuery(MY_KEYWORDS[i]);
            if(!qrt.contains(qr)) return false;
        }

        for(int i=0; i<NUM_LEAF_CONNECTIONS; i++) {
            QueryRequest qr = ProviderHacks.getQueryRequestFactory().createQuery(LEAF_KEYWORDS[i]);
            if(!qrt.contains(qr)) return false;
        }

        for(int i=0; i<UNMATCHING_KEYWORDS.length; i++) {
            QueryRequest qr = 
                ProviderHacks.getQueryRequestFactory().createQuery(UNMATCHING_KEYWORDS[i]);
            if(qrt.contains(qr)) return false;
        }
        return true;
    }

    /**
     * Accessor for the custom list of connections.
     */
    public List getInitializedConnections() {
        return CONNECTIONS;
    }

    public List getInitializedClientConnections() {
        return LEAF_CONNECTIONS;
    }

    public boolean isSupernode() {
        return ULTRAPEER;
    }

    /**
     * Returns the total number of queries received over all Ultrapeer connections.
     */
    public int getNumUltrapeerQueries() {
        return getNumQueries(CONNECTIONS);
    }

    /**
     * Returns the total number of queries received over all leaf connections.
     */
    public int getNumLeafQueries() {
        return getNumQueries(LEAF_CONNECTIONS);
    }

    /**
     * Returns the total number of queries received over all leaf connections.
     */
    private static int getNumQueries(Collection connections) {
        int numQueries = 0;
        Iterator iter = connections.iterator();
        while(iter.hasNext()) {
            TestConnection tc = (TestConnection)iter.next();
            numQueries += tc.getNumQueries();
        }
        return numQueries;
    }

    /**
     * Returns the total number of queries received over all old connections.
     */
    public int getNumOldConnectionQueries() {
        int numQueries = 0;
        Iterator iter = CONNECTIONS.iterator();
        while(iter.hasNext()) {
            TestConnection tc = (TestConnection)iter.next();
            if(tc instanceof OldConnection) {
                numQueries += tc.getNumQueries();
            }
        }
        return numQueries;
    }

    /**
     * Returns the total number of queries received over all old connections.
     */
    public int getNumNewConnectionQueries() {
        int numQueries = 0;
        Iterator iter = CONNECTIONS.iterator();
        while(iter.hasNext()) {
            TestConnection tc = (TestConnection)iter.next();
            if(tc instanceof NewConnection) {
                numQueries += tc.getNumQueries();
            }
        }
        return numQueries;
    }    

    public boolean isClientSupernodeConnection() {
        return false;
    }
}


