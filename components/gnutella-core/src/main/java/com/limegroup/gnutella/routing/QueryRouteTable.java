package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
//For 1.1.8 support, we need the collections framework.  Unfortunately we also
//need BitSet, which isn't in the collections package.  So do avoid conflicts,
//we don't use * imports here.
import java.util.BitSet;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.ArrayList;

/**
 * A list of query keywords that a connection can respond to, as well as the
 * minimum TTL for a response.  More formally, a QueryRouteTable is a list
 * of keyword TTL pairs, [ <keyword_1, ttl_1>, ..., <keywordN, ttl_N> ]
 */
public class QueryRouteTable {
    /** 
     * The bitmaps for each TTL.  We use a dense representation since space
     * isn't really an issue.  INVARIANT: for i<j, tables[i] is a subset of
     * tables[j].  (This means we only have to check one table for any TTL.)  
     * TODO: is tables[0] used?!
     */
    private BitSet[] tables;
    /**
     * tableLengths[i] is the size of tables[i].  This is needed because 
     * BitSet.length() doesn't exist in Java 1.1.8.  
     * INVARIANT: tablesLength.size==tables.size.
     */
    private int[] tableLengths;

    /** 
     * Creates a new QueryRouteTable that has space for initialSize keywords
     * with TTL up to ttl.
     */
    public QueryRouteTable(int ttl, int initialSize) {
        this.tableLengths=new int[ttl];
        this.tables=new BitSet[ttl];
        for (int i=0; i<ttl; i++) {
            this.tableLengths[i]=initialSize;
            this.tables[i]=new BitSet(initialSize);
        }
    }

    private int length(int ttl) {
        return tableLengths[ttl];
    }

    /**
     * Returns true if a response could be generated for qr.  Note that a return
     * value of true does not necessarily mean that a response will be
     * generated--just that it could. 
     */
    public boolean contains(QueryRequest qr) {
        //Check that all hashed keywords are in table[TTL].
        //TODO: has TTL already been decremented?
        String[] keywords=HashFunction.keywords(qr.getQuery());
        BitSet table=tables[Math.max(qr.getTTL(), tables.length-1)];
        for (int i=0; i<keywords.length; i++) {
            if (! table.get(HashFunction.hash(keywords[i], length(i))))
                return false;
        }
        return true;
    }

    /*
     * Adds or removes keywords according to m.  Returns true if this changed
     * state.
     *     @modifies this 
     */
    public boolean update(RouteTableMessage m) {
        //dispatch based on m's variant
        return false;
    }

    /**
     * For all <keyword_i, ttl_i> in m, adds <keyword_i, (ttl_i)-1> to this.
     * Returns true if this changed state.
     *    @modifies this
     */
    public boolean addAll(QueryRouteTable m) {
        //just union bitmaps.
        return false;
    }


    /**
     * For all keywords k in filename, adds <k, 1> to this.
     * Returns true if this changed state.
     */
    public boolean add(String filename) {
        //Should this be 1 or 0?
        int ttl=1;
        String[] keywords=HashFunction.keywords(filename);
        for (int i=0; i<keywords.length; i++) {
            tables[ttl].set(HashFunction.hash(keywords[i], length(ttl)));
        }
        return true;  //TODO: tweak
    }


    /**
     * Returns an iterator of RouteTableMessage that will convey the state
     * of this.  More formally, for any table m, the following holds
     *
     * <pre>
     * QueryRouteTable m2=new QueryRouteTable();
     * for (Iterator iter=m.encode(); iter.hasNext(); ) 
     *    m2.update((RouteTableUpdate)iter.next());
     * Assert.that(m2.equals(m)); 
     * </pre>
     */ 
    public Iterator /* of RouteTableMessage */ encode() {
        Iterator empty=(new ArrayList()).iterator();
        return empty;  //STUB!
    }

    /** Unit test */
    public static void main(String args[]) {
        
    }
}
