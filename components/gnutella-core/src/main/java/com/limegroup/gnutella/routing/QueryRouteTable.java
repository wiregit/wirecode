package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
//For 1.1.8 support, we need the collections framework.  Unfortunately we also
//need BitSet, which isn't in the collections package.  So do avoid conflicts,
//we don't use * imports here.
import java.util.BitSet;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.List;
import com.sun.java.util.collections.ArrayList;

/**
 * A list of query keywords that a connection can respond to, as well as the
 * minimum TTL for a response.  More formally, a QueryRouteTable is a (possibly
 * infinite!) list of keyword TTL pairs, [ <keyword_1, ttl_1>, ..., <keywordN,
 * ttl_N> ]<p>
 *
 * This also maintains timestamps for the purpose of throttling table
 * propogation.  
 */
public class QueryRouteTable {
    /** The suggested default table size. */
    public static final int DEFAULT_TABLE_SIZE=256;
    /** The suggested default max table TTL. */
    public static final int DEFAULT_TABLE_TTL=10;

    //TODO: formalize specifications.  Write rep invariant.  Specify add*
    //methods to allow adding of random keywords because of collisions.

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

    /** Placeholder for MessageRouter.  The time we can next send RouteTableMessage's
     *  along this connection. */
    private long nextUpdateTime=0l;


    /** 
     * Creates a new QueryRouteTable that has space for initialSize keywords
     * with TTL up to ttl.
     */
    public QueryRouteTable(int ttl, int initialSize) {
        this.tableLengths=new int[ttl];
        this.tables=new BitSet[ttl];
        initialize(initialSize);
    }

    /** Resets all TTL tables to be initialSize long and empty. */
    private void initialize(int initialSize) {
        for (int i=0; i<tables.length; i++) {
            this.tableLengths[i]=initialSize;
            this.tables[i]=new BitSet(initialSize);
        }
    }

    /**
     * Returns true if a response could be generated for qr.  Note that a return
     * value of true does not necessarily mean that a response will be
     * generated--just that it could.  It is assumed that qr's TTL has already
     * been decremented, and hence is potentially 0.
     */
    public boolean contains(QueryRequest qr) {
        //TODO: overload with contains(String query, int ttl)?
        //Check that all hashed keywords are in table[TTL].
        String[] keywords=HashFunction.keywords(qr.getQuery());
        BitSet table=tables[Math.min(qr.getTTL(), tables.length-1)];
        for (int i=0; i<keywords.length; i++) {
            if (! table.get(HashFunction.hash(keywords[i], length(i))))
                return false;
        }
        return true;
    }


    /**
     * For all keywords k in filename, adds <k, 0> to this.
     */
    public void add(String filename) {
        String[] keywords=HashFunction.keywords(filename);
        for (int i=0; i<keywords.length; i++) {
            //See contains(..) for a discussion on TTLs and decrementing.
            for (int ttl=0; ttl<tables.length; ttl++) {
                int hash=HashFunction.hash(keywords[i], length(ttl));
                tables[ttl].set(hash);
            }
        }
    }

    /**
     * For all <keyword_i, ttl_i> in m, adds <keyword_i, (ttl_i)+1> to this.
     * (This is useful for union lots of route tables for propoagation.)
     *
     * TODO: what if these have different TTLs.  Should probably expand this
     * as necessary, but that's pain.
     *
     *    @modifies this
     */
    public void addAll(QueryRouteTable qrt) {
        int maxTTL=Math.min(tables.length, qrt.tables.length)-1;
        for (int ttl=0; ttl<maxTTL; ttl++) {
            tables[ttl+1].or(qrt.tables[ttl]);
        }
    }

    /*
     * Adds or removes keywords according to m.
     *     @modifies this 
     */
    public void update(RouteTableMessage m) {
        switch (m.getVariant()) {
        case RouteTableMessage.RESET_VARIANT:
            handleTableMessage((ResetTableMessage)m);
            return;                       
        case RouteTableMessage.SET_DENSE_BLOCK_VARIANT:
            handleTableMessage((SetDenseTableMessage)m);
            return;
        default:
            Assert.that(false,
               "addAll not implemented for variant "+m.getVariant());
        }
    }

    private void handleTableMessage(ResetTableMessage m) {
        initialize(m.getTableSize());
    }

    private void handleTableMessage(SetDenseTableMessage m) {
        //For each bit i set in m, set the corresponding bit of
        //tables[m.getTTL...]
        for (int i=m.getStartOffset(); i<=m.getStopOffset(); i++) {
            for (int ttl=m.getTableTTL(); ttl<tables.length; ttl++) {
                try {
                    if (m.get(i))
                        tables[ttl].set(i);
                    else
                        tables[ttl].clear(i);  //TODO: check protocol spec
                } catch (IndexOutOfBoundsException e) {
                    //The RESET message preceding this lied about the table
                    //size.  Action could be take here to optimize.
                }
            }
        }
    }

    /**
     * Returns an iterator of RouteTableMessage that will convey the state of
     * this.  prev may be be null.  If prev!=null, only those messages needed to
     * to convert prev to this need be returned.  More formally, for any
     * non-null QueryRouteTable's m and prev, the following holds:
     *
     * <pre>
     * for (Iterator iter=m.encode(); iter.hasNext(); ) 
     *    prev.update((RouteTableUpdate)iter.next());
     * Assert.that(prev.equals(m)); 
     * </pre> 
     */
    public Iterator /* of RouteTableMessage */ encode(RouteTableMessage prev) {
        //TODO1: this is a hopelessly inefficient encoding.  Optimize.
        List buf=new ArrayList(tables.length);
        for (int i=0; i<tables.length; i++) {
            buf.add(new SetDenseTableMessage((byte)1, (byte)i, 
                                             tables[i], 0, length(i)));
        }
        return buf.iterator();
    }


    private int length(int ttl) {
        return tableLengths[ttl];
    }

    /** Returns true if we can send a route table update along this
     *  connection. */
    public boolean needsUpdate() {
        return System.currentTimeMillis()>nextUpdateTime;        
    }

    /** Sets this to allow an update in 'time' milliseconds. */
    public void resetUpdateTime(long time) {
        nextUpdateTime=System.currentTimeMillis()+time;
    }

    /** True if o is a QueryRouteTable with the same entries of this.
     *  TODO: document more. */
    public boolean equals(Object o) {
        if (! (o instanceof QueryRouteTable))
            return false;

        QueryRouteTable other=(QueryRouteTable)o;
        if (this.tables.length!=other.tables.length)
            return false;

        for (int i=0; i<this.tables.length; i++) {
            if (! this.tables[i].equals(other.tables[i]))
                return false;
        }
        return true;
    }

    public String toString() {
        StringBuffer buf=new StringBuffer();
        buf.append("{");
        for (int ttl=0; ttl<tables.length; ttl++) {
            for (int i=0; i<length(ttl); i++) {
                if (tables[ttl].get(i)) {
                    if (ttl==0 || !tables[ttl-1].get(i))
                        buf.append("unhash("+i+")/"+ttl+", ");
                }
            }
        }
        buf.append("}");
        return buf.toString();
    }

    /** Unit test */
    public static void main(String args[]) {
        //1. Keyword tests (add, contains)
        QueryRouteTable qrt=new QueryRouteTable(7, 1000);
        QueryRequest qrA=new QueryRequest((byte)1, 0, "good");
        QueryRequest qrB=new QueryRequest((byte)1, 0, "book good");
        QueryRequest qrC=new QueryRequest((byte)1, 0, "book bad");
        Assert.that(! qrt.contains(qrA));
        Assert.that(! qrt.contains(qrB));
        Assert.that(! qrt.contains(qrC));
        qrt.add("good book file");
        Assert.that(qrt.contains(qrA));
        Assert.that(qrt.contains(qrB));
        Assert.that(! qrt.contains(qrC));

        //2. TTL tests. (update)
        int size=100;
        //    clear
        ResetTableMessage reset=new ResetTableMessage((byte)1, (byte)5, size);
        qrt.update(reset);
        Assert.that(! qrt.contains(qrA));
        //    add one entry
        BitSet dummy=new BitSet(size);
        dummy.set(HashFunction.hash("good", size));
        dummy.set(HashFunction.hash("book", size));
        SetDenseTableMessage update=
            new SetDenseTableMessage((byte)1, (byte)3, dummy, 0, 99);
        qrt.update(update);                 //{good/3, book/3}
        Assert.that(! qrt.tables[2].equals(dummy));   //looking at rep
        Assert.that(qrt.tables[3].equals(dummy));
        Assert.that(qrt.tables[4].equals(dummy));
        //    test contains
        QueryRequest qrD0=new QueryRequest((byte)2, 0, "good");
        QueryRequest qrD=new QueryRequest((byte)3, 0, "good");
        QueryRequest qrE=new QueryRequest((byte)4, 0, "good book");
        Assert.that(! qrt.contains(qrA));
        Assert.that(! qrt.contains(qrB));
        Assert.that(! qrt.contains(qrD0));
        Assert.that(qrt.contains(qrD));
        Assert.that(qrt.contains(qrE));        

        //3. addAll tests
        QueryRequest qrF=new QueryRequest((byte)4, 0, "bad");
        QueryRouteTable qrt2=new QueryRouteTable(7, size);
        Assert.that(! qrt2.contains(qrF));
        qrt2.add("bad");                   //{bad/0}
        Assert.that(qrt2.contains(qrF));
        Assert.that(! qrt2.contains(qrD));
        qrt2.addAll(qrt);                  //{bad/0, good/4, book/4}
        System.out.println(qrt2.toString());
        Assert.that(qrt2.contains(qrF));
        Assert.that(! qrt2.contains(qrD0));
        Assert.that(! qrt2.contains(qrD));
        Assert.that(qrt2.contains(qrE));   

        //4. encode tests.  (Expand when encode is fancier.)
        QueryRouteTable qrt3=new QueryRouteTable(7, size);
        Assert.that(! qrt3.equals(qrt2));
        for (Iterator iter=qrt2.encode(null); iter.hasNext(); ) {
            qrt3.update((RouteTableMessage)iter.next());
        }        
        Assert.that(qrt3.equals(qrt2));
    }
}
