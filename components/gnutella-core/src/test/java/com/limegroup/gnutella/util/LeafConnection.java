package com.limegroup.gnutella.util;

import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.List;

/**
 * Specialized class that uses special keywords for leaf routing
 * tables.
 */
public final class LeafConnection extends NewConnection {

    private final String DESCRIPTOR;

    /**
      * Constant keywork that is in ever standard leaf's QRP table.
      */
    public static final String LEAF_KEYWORD = "LEAFKEYWORD";

    /**
     * Constant alternate keyword for use in testing.
     */
    public static final String ALT_LEAF_KEYWORD = "ALTLEAFKEYWORD";
    
    public static LeafConnection createAltLeafConnection()  {
        return new LeafConnection(new String[] {ALT_LEAF_KEYWORD},
            15, "ALT LEAF CONNECTION", false, true);
    }

    public static LeafConnection createWithKeywords(String[] keywords) {
        return new LeafConnection(keywords, 20, "LEAF CONNECTION", true, true);
    }

    
    public static LeafConnection createLeafConnection(boolean b) {
        return new LeafConnection(new String[0], 15, "LEAF_CONNECTION", false, b);
    }


    /**
     * Creates a new LeafConnection with the specified list of keywords, etc.
     */
    private LeafConnection(String[] keywords, int connections, 
        String descriptor, boolean addStandardKeyword, boolean requireMatches) {
        super(connections, requireMatches);
        
        QueryRouteTable qrt = new QueryRouteTable();
        
        for(int i=0; i<keywords.length; i++) {
            qrt.add(keywords[i]);
        }
        if(addStandardKeyword) {
            qrt.add(LEAF_KEYWORD);
        }
        
        List qrts = qrt.encode(null);
        
        Iterator iter = qrts.iterator();
        while(iter.hasNext()) {
            RouteTableMessage rtm = (RouteTableMessage)iter.next();
            if(rtm instanceof ResetTableMessage) {
                qrp().resetQueryRouteTable((ResetTableMessage)rtm);
            } else {
                qrp().patchQueryRouteTable((PatchTableMessage)rtm);
            }
        }
        DESCRIPTOR = descriptor;
    }
    
    public String toString()  {
        return DESCRIPTOR+": "+QRT;
    }


}
