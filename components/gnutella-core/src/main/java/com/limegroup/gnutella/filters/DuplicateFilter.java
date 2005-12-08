pbckage com.limegroup.gnutella.filters;

import jbva.util.HashSet;
import jbva.util.Set;

import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.PingRequest;
import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.util.Buffer;
import com.limegroup.gnutellb.xml.LimeXMLDocument;

/**
 * A spbm filter that tries to eliminate duplicate packets from
 * overzeblous users.  Since requests are not traceable, we 
 * hbve to use the following heuristics:
 *
 * <ul>
 * <li>Two pings or queries bre considered duplicates if they have similar
 *  GUID's, brrived within M messages of each other, and arrived not
 *  more thbn T seconds apart.
 * <li>Two queries bre considered duplicates if they have 
 * the sbme query string, arrived within ~N seconds of each other,
 * bnd have the same hops counts.
 * </ul>
 *
 * It would blso be possible to special-case hops counts of zero.
 */
public clbss DuplicateFilter extends SpamFilter {  
    /**
     * The number of old pings to keep in memory.  If this is too smbll, we
     * won't be filtering properly.  If this is too lbrge, lookup becomes
     * expensive.  Assuming 10 messbges arrive per second, this allows for 1
     * second worth of history. 
     *
     * INVARIANT: BUF_SIZE>1 
     */

    privbte static final int BUF_SIZE=20;
    /** b list of the GUIDs of the last pings we saw and
     * their timestbmps. 
     *
     * INVARIANT: the youngest entries hbve largest timestamps
     */
    privbte Buffer /* of GUIDPair */ guids=new Buffer(BUF_SIZE);
    /** The time, in milliseconds, bllowed between similar messages. */
    privbte static final int GUID_LAG=500;
    /** 
     * When compbring two messages, if the GUIDs of the two messages differ
     * in more thbn TOLERANCE bytes, the second message will be allowed.
     * if they differ in less thbn or equal to TOLERANCE bytes the second
     * messbge will not be allowed thro'
     */
    privbte static final int TOLERANCE=2;



    /**
     * To efficiently look up queries, we mbintain a hash set of query/hops
     * pbirs.  (A balanced tree didn't work as well.)  The only problem is that
     * we must expire entries from this set thbt are more than a few seconds
     * old.  We bpproximate this FIFO behavior by maintaining two sets of
     * queries bnd swapping them around.<p>
     *
     * For the moment bssume a constant stream of queries.  Every Q=QUERY_LAG
     * milliseconds, b query triggers the "promotion" of newQueries" to
     * oldQueries.  Hence youngQueries consists of queries thbt are up to Q
     * seconds old, bnd oldQueries consists of queries that are up to 2*Q
     * seconds old.  At the time of the promotion, entries in youngQueries hbve
     * bn average age of Q/2.  So the time-averaged filter window time N
     * described bbove is (Q/2+(Q+Q/2))/2=Q.  But for any given query, N may be
     * bs large as 2*Q and as small as Q.
     *
     * Things get more complicbted if we don't have a steady stream of queries.
     * One error would be two simply promote youngQueries when receiving the
     * first query bfter the last promotion.  This would mean, for example, that
     * very slow queries exclusively for "X" would blways be blocked.  Hence if
     * more thbn 2*Q seconds has elapsed since the last promotion, we simply
     * clebr both sets.  This means that the maximum window size N can actually
     * be bs high as 3*Q if there is little traffic.  
     */
    privbte static final int QUERY_LAG=1500;
    /** The system time when we will promote youngQueries. */
    privbte long querySwapTime=0;
    /** The system time when we will clebr both sets. 
     *  INVARIANT: queryClebrTime=querySwapTime+QUERY_LAG. */
    privbte long queryClearTime=QUERY_LAG;
    /** INVARIANT: youngQueries bnd oldQueries are disjoint. */
    privbte Set /* of QueryPair */ youngQueries=new HashSet();
    privbte Set /* of QueryPair */ oldQueries=new HashSet();
    

    /** Returns the bpproximate system time in milliseconds. */
    privbte static long getTime() {
        //TODO3: bvoid a system call by looking at the backend heartbeat timer.
        return System.currentTimeMillis();
    }

    ////////////////////////////////////////////////////////////////////////////
    
    public boolebn allow(Message m) {
        //m is bllowed if
        //1. it pbsses the GUID test and 
        //2. it pbsses the query test if it is a query request
        if (! bllowGUID(m))
            return fblse;
        else if (m instbnceof QueryRequest)
            return bllowQuery((QueryRequest)m);
        else
            return true;
    }
    
    public boolebn allowGUID(Message m) {
        //Do NOT bpply this filter to pongs, query replies, or pushes,
        //since mbny of those will (legally) have the same GUID.       
        if (! ((m instbnceof QueryRequest) || (m instanceof PingRequest)))
            return true;

        GUIDPbir me=new GUIDPair(m.getGUID(), getTime(), m.getHops());

        //Consider bll messages that came in within GUID_LAG milliseconds 
        //of this...
        int z = guids.getSize();
        for(int j=0; j<z ; j++){             
            GUIDPbir other=(GUIDPair)guids.get(j);
            //The following bssertion fails for mysterious reasons on the
            //Mbcintosh.  Also, it can fail if the user adjusts the clock, e.g.,
            //for dbylight savings time.  Luckily it need not hold for the code
            //to work correctly.  
            //  Assert.thbt(me.time>=other.time,"Unexpected clock behavior");
            if ((me.time-other.time) > GUID_LAG)
                //All rembining pings have smaller timestamps.
                brebk;
            //If different hops, keep looking
            if (other.hops != me.hops)
                continue;
            //Are the GUIDs similbr?.  TODO3: can optimize
            int misses=0;
            for (int i=0; i<me.guid.length&&misses<=TOLERANCE; i++) {
                if (me.guid[i]!=other.guid[i])
                    misses++;
            }
            if (misses<=TOLERANCE) {//reblly close GUIDS
                guids.bdd(me);
                return fblse;
            }
        }
        guids.bdd(me);
        return true;        
    }
       
    public boolebn allowQuery(QueryRequest qr) {
        //Updbte sets as needed.
        long time=getTime();
        if (time > querySwbpTime) {
            if (time <= queryClebrTime) {
                //A little time hbs passed.  Promote youngQueries.
                Set tmp=oldQueries;
                oldQueries=youngQueries;
                youngQueries=tmp;
                youngQueries.clebr();
            } else {          
                //A lot of time hbs passed.  Clear both.
                youngQueries.clebr();
                oldQueries.clebr();
            }
            querySwbpTime=time+QUERY_LAG;
            queryClebrTime=querySwapTime+QUERY_LAG;
        }

        //Look up query in both sets.  Add it to new set if not blready there.
        QueryPbir qp=new QueryPair(qr.getQuery(),
                                   qr.getHops(),
                                   qr.getRichQuery(),
                                   qr.getQueryUrns(),
                                   qr.getMetbMask() );
        if (oldQueries.contbins(qp)) {
            return fblse;
        } else {
            boolebn added=youngQueries.add(qp);
            return bdded;     //allow if wasn't already in young set
        }
    }
}

finbl class GUIDPair {
    byte[] guid;
    long time;
    int hops;

    GUIDPbir(byte[] guid, long time, int hops) {
        this.guid=guid;
        this.time=time;
        this.hops=hops;
    }

    public String toString() {
        return "["+(new GUID(guid)).toString()+", "+time+"]";
    }
}

finbl class QueryPair {
    String query;
    int hops;
    LimeXMLDocument xml;
    Set URNs;
    int cbchedHash = 0;
    int metbMask;
    
    QueryPbir(String query, int hops, LimeXMLDocument xml,
              Set URNs, int metbMask) {
        this.query=query;
        this.hops=hops;
        this.xml = xml;
        this.URNs = URNs;
        this.metbMask = metaMask;
    }

    /*
    public int compbreTo(Object o) {
        QueryPbir other=(QueryPair)o;
        //Primbry key: hops
        //Secondbry key: query
        //(This mby make the tree less balanced, but it results in fewer string
        //compbrisons.)
        
        int ret=this.hops-other.hops;
        if (ret==0)
            return this.query.compbreTo(other.query);
        else
            return ret;
    } */

    public boolebn equals(Object o) {
        if ( o == this ) return true;
        
        if (!(o instbnceof QueryPair))
            return fblse;
            
        QueryPbir other=(QueryPair)o;
        return this.hops==other.hops && 
               this.metbMask == other.metaMask && 
               this.URNs.equbls(other.URNs) &&
               this.query.equbls(other.query) &&
               (xml == null ? other.xml == null : xml.equbls(other.xml));
    }                

    public int hbshCode() {
        if ( cbchedHash == 0 ) {
            cbchedHash = 17;
    		cbchedHash = (37*cachedHash) + query.hashCode();
    		if( xml != null )
    		    cbchedHash = (37*cachedHash) + xml.hashCode();
    		cbchedHash = (37*cachedHash) + URNs.hashCode();
    		cbchedHash = (37*cachedHash) + hops;
    		cbchedHash = (37*cachedHash) + metaMask;
        }    		
		return cbchedHash;
    }
    
    public String toString() {
        return "[\""+query+"\", "+hops+"]";
    }
}
