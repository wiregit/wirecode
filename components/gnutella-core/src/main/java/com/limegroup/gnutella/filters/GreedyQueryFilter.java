package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.Buffer;
import com.sun.java.util.collections.*;

/** 
 * Stops queries that are bound to match too many files.  
 *
 * Currently, queries that are blocked include "a.asf, d.mp3, etc." or
 * single-character searches.  Additionally, queries such as "*.mp3" or 
 * "mpg" or "*.*" are to be blocked, are at least set to travel less than
 * other queries.
 */
public class GreedyQueryFilter extends SpamFilter {
    private static final int GREEDY_QUERY_MAX = 3;

    public boolean allow(Message m) {
        if (! (m instanceof QueryRequest))
            return true;

        String query=((QueryRequest)m).getQuery();
        int n=query.length();
        if (n==1)
            return false;
        if ((n==5 || n==6) 
               && query.charAt(1)=='.' 
               && Character.isLetter(query.charAt(0)) )
            return false; 
        if (this.isVeryGeneralSearch(query) ||
            this.isObfuscatedGeneralSearch(query)) {
            int hops = (int)m.getHops();
            int ttl = (int)m.getTTL();
            if (hops >= this.GREEDY_QUERY_MAX)
                return false;
            if ( (hops + ttl) > this.GREEDY_QUERY_MAX) 
                m.setTTL((byte)(this.GREEDY_QUERY_MAX - hops));
        }

        return true;
    }

    /**
     * Search through a query string and see if matches a very general search
     * such as "*.*", "*.mp3", or "*.mpg" and check for uppercase also
     */
    private boolean isVeryGeneralSearch(String queryString) {
        int length = queryString.length();

        if ((length == 3) && 
            ( (queryString.charAt(1) == '.') ||
              (queryString.equalsIgnoreCase("mp3")) ||
              (queryString.equalsIgnoreCase("mpg")) ) ) 
            return true;

        if (length == 5) { //could by of type "*.mp3" or "*.mpg"
            String fileFormat = queryString.substring(2,5);
            if ((queryString.charAt(1) == '.') &&
                ( (fileFormat.equalsIgnoreCase("mp3")) ||
                  (fileFormat.equalsIgnoreCase("mpg")) ) )
                return true;
        }
        
        return false; //not a general search
    }


    /** To combat system-wide gnutella overflow, this method checks for
     *  permutations of "*.*"
     */
    private boolean isObfuscatedGeneralSearch(final String queryString) {
        final String unacceptable = "*.- ";
        for (int i = 0; i < queryString.length(); i++) 
            // if a character is not one of the unacceptable strings, the query
            // is ok.
            if (unacceptable.indexOf(queryString.charAt(i)) == -1)
                return false;

        return true;
    }

    /*
    public static void main(String args[]) {
        Message msg;
        GreedyQueryFilter filter=new GreedyQueryFilter();

        msg=new PingRequest((byte)5);
        Assert.that(filter.allow(msg));

        msg=new QueryRequest((byte)5, 0, "a");
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest((byte)5, 0, "*");
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest((byte)5, 0, "a.asf");
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest((byte)5, 0, "z.mpg");
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest((byte)5, 0, "z.mp");
        Assert.that(filter.allow(msg));

        msg=new QueryRequest((byte)5, 0, "z mpg");
        Assert.that(filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)1, 
                             (byte)4, "*.mpg".getBytes());
        Assert.that(!filter.allow(msg));

        msg=new QueryRequest((byte)5, 0, "1.mpg");
        Assert.that(filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)1, 
                             (byte)4, "*.mp3".getBytes());
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)1, 
                             (byte)4, "*.*".getBytes());
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)1, 
                             (byte)4, "*.MP3".getBytes());
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)1, 
                             (byte)4, "*.MPG".getBytes());
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)1, 
                             (byte)4, "mp3".getBytes());
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)1, 
                             (byte)4, "mpg".getBytes());
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)1, 
                             (byte)4, "MP3".getBytes());
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)1, 
                             (byte)4, "MPG".getBytes());
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)1, 
                             (byte)4, "a.b".getBytes());
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)1, 
                             (byte)4, "*.*-".getBytes());
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)3, 
                             (byte)2, "--**.*-".getBytes());
        Assert.that(filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)1, 
                             (byte)4, "*****".getBytes());
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)2,
                             (byte)3, "britney*.*".getBytes());
        Assert.that(filter.allow(msg)); 
    
        msg=new QueryRequest(GUID.makeGuid(), (byte)2,
                             (byte)3, "*.*.".getBytes());
        Assert.that(! filter.allow(msg));

        msg=new QueryRequest(GUID.makeGuid(), (byte)1,
                             (byte)6, "new order*".getBytes());
        Assert.that(filter.allow(msg)); 
    
    }*/

}
