package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.Buffer;
import com.sun.java.util.collections.*;

/**
 * Stops queries that are bound to match too many files.
 *
 * Currently, queries that are blocked include "a.asf, d.mp3, etc." or
 * single-character searches.  Queries that are not stopped include "*.mp3" or
 * "mpg", since these can be useful.
 */
public class GreedyQueryFilter extends SpamFilter {
    public boolean allow(Message m) {
        if (! (m instanceof QueryRequest))
            return true;

        String query=((QueryRequest)m).getTextQuery();
        int n=query.length();
        if (n==1)
            return false;
        if ((n==5 || n==6)
               && query.charAt(1)=='.'
               && Character.isLetter(query.charAt(0)) )
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

        msg=new QueryRequest((byte)5, 0, "*.mpg");
        Assert.that(filter.allow(msg));

        msg=new QueryRequest((byte)5, 0, "1.mpg");
        Assert.that(filter.allow(msg));
    }
    */

}
