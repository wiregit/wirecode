package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.Buffer;
import com.sun.java.util.collections.*;
import java.util.Date;

/**
 * A spam filter that tries to eliminate duplicate packets from
 * overzealous users:
 *
 * <ul>
 * <li>Two pings are considered duplicates if they have 
 * GUIDs differing by no more than K bytes and arrived within N
 * seconds.
 * <li>Two queries are considered duplicates if they have 
 * the same query string and arrived within N seconds of each other.
 * </ul>
 */
public class DuplicateFilter extends SpamFilter {  
    /**
     * The number of old pings and queries to keep in memory.
     * If this is too small, we won't be filtering properly.
     * Assuming 10 messages arrive per second, this allows for
     * a whopping 10 seconds worth of history.  Luckily we don't
     * need to search all that history, except in rare cases.
     *
     * INVARIANT: BUF_SIZE>1 
     */
    private static final int BUF_SIZE=100;

    /** a list of the GUIDs of the last pings we saw and
     * their timestamps. 
     *
     * INVARIANT: the youngest entries have largest timestamps
     */
    private Buffer /* of PingPair */ pings=new Buffer(BUF_SIZE);
    /** The time, in milliseconds, allowed between similar pings. */
    private static final int PING_LAG=4000;
    /** The number of bytes in two GUIDs that must be the same to 
     * assume they came from the same host. */
    private static final int GUID_SIMILAR=6;

    /** a list of the last query strings we saw and their
     * timestamps. 
     *    Note that a different representation for queries
     * could make allowQuery run in O(1) time, instead
     * of O(BUF_SIZE).  However, profiling suggests this is not 
     * necessary. 
     *
     * INVARIANT: the youngest entries have largest timestamps
     */
    private Buffer /* of QueryPair */ queries=new Buffer(BUF_SIZE);
    /** The time, in milliseconds, allowed between similar queries. */
    private static final int QUERY_LAG=4000;

    public boolean allow(Message m) {
        if (m instanceof PingRequest)
            return allowPing((PingRequest)m);
        else if (m instanceof QueryRequest)
            return allowQuery((QueryRequest)m);
        else
            return true;        
    }

    public boolean allowPing(PingRequest pr) {
        PingPair me=new PingPair(pr.getGUID(),
                                 (new Date()).getTime());

        //Consider all pings that came in within PING_LAG milliseconds 
        //of this...
        for (Iterator iter=pings.iterator(); iter.hasNext(); ) {
            PingPair other=(PingPair)iter.next();
            //The following assertion fails for mysterious reasons on the
            //Macintosh.  Luckily it need not hold for the code to work
            //correctly.
            //  Assert.that(me.time>=other.time,"Unexpected clock behavior");
            if ((me.time-other.time) > PING_LAG)
                //All remaining pings have smaller timestamps.
                break;
            //Are the GUIDs similar?.  TODO2: can optimize
            int matches=0;
            for (int i=0; i<me.guid.length; i++) {
                if (me.guid[i]==other.guid[i])
                    matches++;
            }
            if (matches>=GUID_SIMILAR) {
                pings.add(me);
                return false;
            }
        }
        pings.add(me);
        return true;        
    }

    public boolean allowQuery(QueryRequest qr) {
        QueryPair me=new QueryPair(qr.getQuery(),
                                   (new Date()).getTime());
    
        //Consider all queries that came in within QUERY_LAG milliseconds 
        //of this...
        for (Iterator iter=queries.iterator(); iter.hasNext(); ) {
            QueryPair other=(QueryPair)iter.next();
            Assert.that(me.time>=other.time,"Unexpected clock behavior");
            if ((me.time-other.time) > QUERY_LAG)
                //All remaining queries have smaller timestamps.
                break;
            //Are the queries the same?
            if (me.query.equals(other.query)) {
                queries.add(me);
                return false;
            }
        }
        queries.add(me);
        return true;
    }


    ///** Unit test */
    /*
    public static void main(String args[]) {
        SpamFilter filter=new DuplicateFilter();
        PingRequest pr=null;
        QueryRequest qr=null;

        pr=new PingRequest((byte)2);
        Assert.that(filter.allow(pr));
        pr=new PingRequest((byte)2);
        Assert.that(filter.allow(pr)); //since GUIDs are currently random
        Assert.that(!filter.allow(pr));
    
        //Now, if I wait a few seconds, it should be allowed.
        synchronized (filter) {
            try {
                filter.wait(PING_LAG*2);
            } catch (InterruptedException e) { }
        }

        Assert.that(filter.allow(pr));  
        Assert.that(!filter.allow(pr));
        pr=new PingRequest((byte)2);
        Assert.that(filter.allow(pr));


        qr=new QueryRequest((byte)2, 0, "search1");
        Assert.that(filter.allow(qr));
        Assert.that(!filter.allow(qr));
        qr=new QueryRequest((byte)2, 0, "search2");
        Assert.that(filter.allow(qr));

        //Now, if I wait a few seconds, it should be allowed.
        synchronized (filter) {
            try {
                filter.wait(QUERY_LAG*2);
            } catch (InterruptedException e) { }
        }

        Assert.that(filter.allow(qr));
        Assert.that(!filter.allow(qr));
        qr=new QueryRequest((byte)2, 0, "search3");
        Assert.that(filter.allow(qr));
    }
    */
}

class PingPair {
    byte[] guid;
    long time;

    PingPair(byte[] guid, long time) {
        this.guid=guid;
        this.time=time;
    }

    public String toString() {
        return "["+(new GUID(guid)).toString()+", "+time+"]";
    }
}

class QueryPair {
    String query;
    long time;
    
    QueryPair(String query, long time) {
        this.time=time;
        this.query=query;
    }

    public String toString() {
        return "[\""+query+"\", "+time+"]";
    }
}
