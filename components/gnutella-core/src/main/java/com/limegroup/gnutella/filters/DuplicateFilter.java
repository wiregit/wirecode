package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.Buffer;
import com.sun.java.util.collections.*;

/**
 * A spam filter that tries to eliminate duplicate packets from
 * overzealous users.  Since requests are not traceable, we 
 * have to use the following heuristics:
 *
 * <ul>
 * <li>Two pings or queries are considered duplicates if they have similar
 *  GUID's, arrived within M messages of each other, and arrived not
 *  more than T seconds apart.
 * <li>Two queries are considered duplicates if they have 
 * the same query string, arrived within ~N seconds of each other,
 * and have the same hops counts.
 * </ul>
 *
 * It would also be possible to special-case hops counts of zero.
 */
public class DuplicateFilter extends SpamFilter {  
    /**
     * The number of old pings to keep in memory.  If this is too small, we
     * won't be filtering properly.  If this is too large, lookup becomes
     * expensive.  Assuming 10 messages arrive per second, this allows for 1
     * second worth of history. 
     *
     * INVARIANT: BUF_SIZE>1 
     */

    private static final int BUF_SIZE=20;
    /** a list of the GUIDs of the last pings we saw and
     * their timestamps. 
     *
     * INVARIANT: the youngest entries have largest timestamps
     */
    private Buffer /* of GUIDPair */ guids=new Buffer(BUF_SIZE);
    /** The time, in milliseconds, allowed between similar messages. */
    private static final int GUID_LAG=500;
    /** 
     * When comparing two messages, if the GUIDs of the two messages differ
     * in more than TOLERANCE bytes, the second message will be allowed.
     * if they differ in less than or equal to TOLERANCE bytes the second
     * message will not be allowed thro'
     */
    private static final int TOLERANCE=2;



    /**
     * To efficiently look up queries, we maintain a hash set of query/hops
     * pairs.  (A balanced tree didn't work as well.)  The only problem is that
     * we must expire entries from this set that are more than a few seconds
     * old.  We approximate this FIFO behavior by maintaining two sets of
     * queries and swapping them around.<p>
     *
     * For the moment assume a constant stream of queries.  Every Q=QUERY_LAG
     * milliseconds, a query triggers the "promotion" of newQueries" to
     * oldQueries.  Hence youngQueries consists of queries that are up to Q
     * seconds old, and oldQueries consists of queries that are up to 2*Q
     * seconds old.  At the time of the promotion, entries in youngQueries have
     * an average age of Q/2.  So the time-averaged filter window time N
     * described above is (Q/2+(Q+Q/2))/2=Q.  But for any given query, N may be
     * as large as 2*Q and as small as Q.
     *
     * Things get more complicated if we don't have a steady stream of queries.
     * One error would be two simply promote youngQueries when receiving the
     * first query after the last promotion.  This would mean, for example, that
     * very slow queries exclusively for "X" would always be blocked.  Hence if
     * more than 2*Q seconds has elapsed since the last promotion, we simply
     * clear both sets.  This means that the maximum window size N can actually
     * be as high as 3*Q if there is little traffic.  
     */
    private static final int QUERY_LAG=1500;
    /** The system time when we will promote youngQueries. */
    private long querySwapTime=0;
    /** The system time when we will clear both sets. 
     *  INVARIANT: queryClearTime=querySwapTime+QUERY_LAG. */
    private long queryClearTime=QUERY_LAG;
    /** INVARIANT: youngQueries and oldQueries are disjoint. */
    private Set /* of QueryPair */ youngQueries=new HashSet();
    private Set /* of QueryPair */ oldQueries=new HashSet();
    

    /** Returns the approximate system time in milliseconds. */
    private static long getTime() {
        //TODO3: avoid a system call by looking at the backend heartbeat timer.
        return System.currentTimeMillis();
    }

    ////////////////////////////////////////////////////////////////////////////
    
    public boolean allow(Message m) {
        //m is allowed if
        //1. it passes the GUID test and 
        //2. it passes the query test if it is a query request
        if (! allowGUID(m))
            return false;
        else if (m instanceof QueryRequest)
            return allowQuery((QueryRequest)m);
        else
            return true;
    }
    
    public boolean allowGUID(Message m) {
        //Do NOT apply this filter to pongs, query replies, or pushes,
        //since many of those will (legally) have the same GUID.       
        if (! ((m instanceof QueryRequest) || (m instanceof PingRequest)))
            return true;

        GUIDPair me=new GUIDPair(m.getGUID(), getTime(), m.getHops());

        //Consider all messages that came in within GUID_LAG milliseconds 
        //of this...
        int z = guids.getSize();
        for(int j=0; j<z ; j++){             
            GUIDPair other=(GUIDPair)guids.get(j);
            //The following assertion fails for mysterious reasons on the
            //Macintosh.  Also, it can fail if the user adjusts the clock, e.g.,
            //for daylight savings time.  Luckily it need not hold for the code
            //to work correctly.  
            //  Assert.that(me.time>=other.time,"Unexpected clock behavior");
            if ((me.time-other.time) > GUID_LAG)
                //All remaining pings have smaller timestamps.
                break;
            //If different hops, keep looking
            if (other.hops != me.hops)
                continue;
            //Are the GUIDs similar?.  TODO3: can optimize
            int misses=0;
            for (int i=0; i<me.guid.length&&misses<=TOLERANCE; i++) {
                if (me.guid[i]!=other.guid[i])
                    misses++;
            }
            if (misses<=TOLERANCE) {//really close GUIDS
                guids.add(me);
                return false;
            }
        }
        guids.add(me);
        return true;        
    }
       
    public boolean allowQuery(QueryRequest qr) {
        //Update sets as needed.
        long time=getTime();
        if (time > querySwapTime) {
            if (time <= queryClearTime) {
                //A little time has passed.  Promote youngQueries.
                Set tmp=oldQueries;
                oldQueries=youngQueries;
                youngQueries=tmp;
                youngQueries.clear();
            } else {          
                //A lot of time has passed.  Clear both.
                youngQueries.clear();
                oldQueries.clear();
            }
            querySwapTime=time+QUERY_LAG;
            queryClearTime=querySwapTime+QUERY_LAG;
        }

        //Look up query in both sets.  Add it to new set if not already there.
        QueryPair qp=new QueryPair(qr.getQuery(), qr.getHops());
        if (oldQueries.contains(qp)) {
            return false;
        } else {
            boolean added=youngQueries.add(qp);
            return added;     //allow if wasn't already in young set
        }
    }


    ///** Unit test */
    /*
    public static void main(String args[]) {        
        SpamFilter filter=new DuplicateFilter();
        PingRequest pr=null;
        QueryRequest qr=null;
        
        pr=new PingRequest((byte)2);
        byte[] guid=pr.getGUID();
        guid[9]++;
        qr=new QueryRequest(guid, pr.getTTL(), pr.getHops(), new byte[3]);
        Assert.that(filter.allow(pr));
        Assert.that(!filter.allow(qr));
        pr=new PingRequest((byte)2);
        Assert.that(filter.allow(pr)); //since GUIDs are currently random
        Assert.that(!filter.allow(pr));
        
        //Now, if I wait a few seconds, it should be allowed.
        synchronized (filter) {
            try {
                filter.wait(GUID_LAG*2);
            } catch (InterruptedException e) { }
        }
        
        Assert.that(filter.allow(pr));  
        Assert.that(!filter.allow(pr));
        pr=new PingRequest((byte)2);
        Assert.that(filter.allow(pr));
        pr.hop(); //hack to get different hops count
        Assert.that(filter.allow(pr));
        
        
        qr=new QueryRequest((byte)2, 0, "search1");
        Assert.that(filter.allow(qr));
        Assert.that(!filter.allow(qr));
        qr=new QueryRequest((byte)2, 0, "search2");
        Assert.that(filter.allow(qr));

        //Now, if I wait a few seconds, it should be allowed.
        synchronized (filter) {
            try {
                filter.wait(QUERY_LAG*4);
            } catch (InterruptedException e) { }
        }

        Assert.that(filter.allow(qr));
        Assert.that(!filter.allow(qr));
        qr=new QueryRequest((byte)2, 0, "search3");
        Assert.that(filter.allow(qr));
        qr.hop(); //hack to get different hops count
        Assert.that(filter.allow(qr));
    }
    */
}

class GUIDPair {
    byte[] guid;
    long time;
    int hops;

    GUIDPair(byte[] guid, long time, int hops) {
        this.guid=guid;
        this.time=time;
        this.hops=hops;
    }

    public String toString() {
        return "["+(new GUID(guid)).toString()+", "+time+"]";
    }
}

class QueryPair implements com.sun.java.util.collections.Comparable {
    String query;
    int hops;
    
    QueryPair(String query, int hops) {
        this.query=query;
        this.hops=hops;
    }

    public int compareTo(Object o) {
        QueryPair other=(QueryPair)o;
        //Primary key: hops
        //Secondary key: query
        //(This may make the tree less balanced, but it results in fewer string
        //comparisons.)
        
        int ret=this.hops-other.hops;
        if (ret==0)
            return this.query.compareTo(other.query);
        else
            return ret;
    }

    public boolean equals(Object o) {
        if (! (o instanceof QueryPair))
            return false;
        QueryPair other=(QueryPair)o;
        return (this.hops==other.hops) && this.query.equals(other.query);
    }                

    public int hashCode() {
        return query.hashCode()+hops;
    }
    
    public String toString() {
        return "[\""+query+"\", "+hops+"]";
    }
}
