package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
//For 1.1.8 support, we need the collections framework.  Unfortunately we also
//need BitSet, which isn't in the collections package.  So do avoid conflicts,
//we don't use * imports here.
import java.util.BitSet;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.List;
import com.sun.java.util.collections.ArrayList;

//Please note that &#60; and &#62; are the HTML escapes for '<' and '>'.

/**
 * A list of query keywords that a connection can respond to, as well as the
 * minimum TTL for a response.  More formally, a QueryRouteTable is a (possibly
 * infinite!) list of keyword TTL pairs, [ &#60;keyword_1, ttl_1&#62;, ...,
 * &#60;keywordN, ttl_N&#62; ]  Tables cannot be resized.
 */
public class QueryRouteTable {
    /** The suggested default table size. */
    public static final int DEFAULT_TABLE_SIZE=8192;
    /** The suggested default max table TTL. */
    public static final int DEFAULT_TABLE_TTL=5;

    //TODO: formalize specifications.  Write rep invariant.  Specify add*
    //methods to allow adding of random keywords because of collisions.

    //TODO: having an array of numbers would probably be better representation
    //It would require slightly more space but would make implementation easier.
    //But does this suggest a new message?

    /** 
     * The bitmaps for each TTL.  We use a dense representation since space
     * isn't really an issue.  INVARIANT: for i<j, tables[i] is a subset of
     * tables[j].  (This means we only have to check one table for any TTL.)  
     */
    private BitSet[] tables;
    /**
     * The length of each table.  This is needed because BitSet.length() doesn't
     * exist in Java 1.1.8.
     * INVARIANT: for all i, tables[i].length()==tableLength (Java 1.3)
     *                       tables[i].size()>=tableLength (Java 1.1.8+) 
     */
    private int tableLength;


    /** 
     * Creates a new QueryRouteTable that has space for initialSize keywords
     * with TTL up to ttl.   
     */
    public QueryRouteTable(int ttl, int initialSize) {
        this.tables=new BitSet[ttl];
        this.tableLength=initialSize;
        for (int i=0; i<tables.length; i++) {
            this.tables[i]=new BitSet(initialSize);
        }
        repOk();
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
        int ttl=Math.min(qr.getTTL(), tables.length-1);        
        BitSet table=tables[ttl];
        for (int i=0; i<keywords.length; i++) {
            String keyword=keywords[i];
            int hash=HashFunction.hash(keyword, tableLength);
            if (! table.get(hash))
                return false;
        }
        return true;
    }
    
    /**
     * For all keywords k in filename, adds <k, 0> to this.
     */
    public void add(String filename) {
        add(filename, 0);
        repOk();
    }

    /**
     * For all keywords k in filename, adds <k, ttl> to this.
     * This method is public mainly for testing reasons.
     */
    public void add(String filename, int ttl) {
        String[] keywords=HashFunction.keywords(filename);
        for (int i=0; i<keywords.length; i++) {
            //See contains(..) for a discussion on TTLs and decrementing.
            for (int t=ttl; t<tables.length; t++) {
                String keyword=keywords[i];
                int hash=HashFunction.hash(keyword, tableLength);
                tables[t].set(hash);
            }
        }
        repOk();
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
        //qrt     1   2   3   4  
        //this    0   1   2   3   4   4
        for (int ttl=1; ttl<tables.length; ttl++) {
            tables[ttl].or(qrt.tables[Math.min(ttl-1, qrt.tables.length-1)]);
        }
        repOk();
    }

    /*
     * Adds or removes keywords according to m.  Does not resize this.
     *     @modifies this 
     */
    public void update(RouteTableMessage m) {
        switch (m.getVariant()) {                      
        case RouteTableMessage.SET_DENSE_BLOCK_VARIANT:
            handleTableMessage((SetDenseTableMessage)m);
            return;
        case RouteTableMessage.ADD_SPARSE_BLOCK_VARIANT:
            handleTableMessage((SparseTableMessage)m);
        case RouteTableMessage.REMOVE_SPARSE_BLOCK_VARIANT:
            handleTableMessage((SparseTableMessage)m);
        default:
            //Ignore.
            return;
        }
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
        repOk();
    }

    private void handleTableMessage(SparseTableMessage m) {
        //For each block set in m, set the corresponding bit of
        //tables[m.getTTL...]
        for (int i=0; i<m.getSize(); i++) {
            int block=m.getBlock(i);
            for (int ttl=m.getTableTTL(); ttl<tables.length; ttl++) {
                try {
                    if (m.isAdd())
                        tables[ttl].set(block);
                    else
                        tables[ttl].clear(block);
                } catch (IndexOutOfBoundsException e) {
                    //The RESET message preceding this lied about the table
                    //size.  Action could be take here to optimize.
                }
            }
        }
        repOk();
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
    public Iterator /* of RouteTableMessage */ encode(QueryRouteTable prev) {
        //TODO1: this is not an optimal encoding.  Optimize.
        //TODO: this may get complicated.  Should we put it in another class?
        List messages=new ArrayList(tables.length);
        if (prev==null) 
            messages.add(new ResetTableMessage((byte)1,
                                               (byte)0, tableLength));
        
//          //Dense encoding
//          for (int i=0; i<tables.length; i++) {
//              buf.add(new SetDenseTableMessage((byte)1, (byte)i, 
//                                               tables[i], 0, tableLength-1));
//          }

        //"Differential" sparse encoding: for each TTL...
        for (int ttl=0; ttl<tables.length; ttl++) {
            //Find indices that need adding and removing for this TTL.
            List /* of Integer */ addBlocks=new ArrayList();
            List /* of Integer */ removeBlocks=new ArrayList();
            //For each bit i of the table...
            for (int i=0; i<tableLength; i++) {
                //If bit is set, wasn't set in previous message, and wasn't set
                //with a lower TTL, send ADD message.  (TODO: document with
                //picture from notes file.)
                if (tables[ttl].get(i)) {
                    if ((ttl==0 || !tables[ttl-1].get(i))
                            && (prev==null || !prev.tables[ttl].get(i)))
                        addBlocks.add(new Integer(i));
                    else if ((ttl==0 || !tables[ttl-1].get(i))
                            && (prev==null || ttl==0 || prev.tables[ttl-1].get(i)))
                        addBlocks.add(new Integer(i));
                }
                //If bit isn't set but was set in previous tables, send REMOVE
                //message.
                else {
                    if (prev!=null && prev.tables[ttl].get(i))
                        if (ttl==0 || !prev.tables[ttl-1].get(i))  
                            removeBlocks.add(new Integer(i));
                }                
            }

            //Add ADD/REMOVE_SPARSE_BLOCK_VARIANT messages, as needed
            if (addBlocks.size()>0)
                messages.add(SparseTableMessage.create((byte)ttl, //Table TTL
                                                       true,
                                                       addBlocks));
            if (removeBlocks.size()>0)
                messages.add(SparseTableMessage.create((byte)ttl, //Table TTL
                                                       false,
                                                       removeBlocks));
        }                                            

        return messages.iterator();
    }


    


    ////////////////////////////////////////////////////////////////////////////

    /** True if o is a QueryRouteTable with the same entries of this. */
    public boolean equals(Object o) {
        //TODO: two qrt's can be equal even if they have different TTL ranges.
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
            for (int i=0; i<tableLength; i++) {
                if (tables[ttl].get(i)) {
                    if (ttl==0 || !tables[ttl-1].get(i))
                        buf.append(i+"/"+ttl+", ");
                }
            }
        }
        buf.append("}");
        return buf.toString();
    }

    /** Checks internal consistency. */
    private void repOk() {
        for (int ttl=1; ttl<tables.length; ttl++) {
            BitSet smaller=tables[ttl-1];
            BitSet larger=tables[ttl];
            for (int i=0; i<tableLength; i++) {
                if (smaller.get(i))
                    Assert.that(larger.get(i),
                        "Bit "+i+" is set in table of TTL "+(ttl-1)
                       +" but not in "+ttl+", breaking superset invariant");
            }
        }
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
        qrt=new QueryRouteTable((byte)5, size);
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
        Assert.that(qrt2.contains(qrF));
        Assert.that(! qrt2.contains(qrD0));
        Assert.that(! qrt2.contains(qrD));
        Assert.that(qrt2.contains(qrE));   

        //4. Simple encode test.
        QueryRouteTable qrt3=new QueryRouteTable(7, size);
        Assert.that(! qrt3.equals(qrt2));
        for (Iterator iter=qrt2.encode(null); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            qrt3.update(m);
        }        
        Assert.that(qrt3.equals(qrt2), "Got "+qrt3+" not "+qrt2);

        //5. Glass-box encode test.
        //   qrt:  {a/0, x/1, c/3, d/4}
        //   qrt2: {a/0, b/1, c/2, d/5}
        //   ===>  ADD(b, 1), REMOVE(x, 1), ADD(c, 2), REMOVE(d, 4), ADD(d, 5)
        qrt=new QueryRouteTable(7, 1024);
        qrt.add("a", 0);
        qrt.add("x", 1);
        qrt.add("c", 3);
        qrt.add("d", 4);
        qrt2=new QueryRouteTable(7, 1024);
        qrt2.add("a", 0);
        qrt2.add("b", 1);
        qrt2.add("c", 2);
        qrt2.add("d", 5);
        Iterator iter=qrt2.encode(qrt);
        checkNext(iter, true,  1, "b");
        checkNext(iter, false, 1, "x");
        checkNext(iter, true,  2, "c");
        checkNext(iter, false, 4, "d");
        checkNext(iter, true,  5, "d");
    }

    private static void checkNext(Iterator iter,
                           boolean isAdd, int tableTTL, String value) {
        SparseTableMessage s=(SparseTableMessage)iter.next();
        System.out.println("Got "+s);
        Assert.that(s.getTableTTL()==tableTTL,
                    "Unexpected table TTL: "+s.getTableTTL()); 
        Assert.that(s.getSize()==1,
                    "Unexpected table size: "+s.getSize());
        Assert.that(s.isAdd()==isAdd,
                    "Unexpected variant: "+s.isAdd());
        Assert.that(s.getBlock(0)==HashFunction.hash(value, 1024),
                    "Unexpected block: "+s.getBlock(0));
        
    }
}
