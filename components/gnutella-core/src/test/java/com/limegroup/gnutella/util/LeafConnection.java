package com.limegroup.gnutella.util;

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

        for(int i=0; i<keywords.length; i++) {
            QRT.add(keywords[i]);
        }
        if(addStandardKeyword) {
            QRT.add(LEAF_KEYWORD);
        }
        DESCRIPTOR = descriptor;
    }
    
    public String toString()  {
        return DESCRIPTOR+": "+QRT;
    }


}
