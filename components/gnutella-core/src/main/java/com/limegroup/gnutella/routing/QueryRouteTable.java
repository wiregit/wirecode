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

    /** 
     * The table of keywords and their associated TTLs.  Each value table[i] is
     * the minimum number of hops <i>minus one</i> to a file that matches a
     * keyword with hash i.  (Note that this corresponds to the minimum TTL for
     * a matching query.)  If table[i]>ttl, no file in the horizon matches a
     * keyword with hash i.  
     *
     * In other words, [0, 0, ... ] represents a completely full table, while
     * [maxTTL+1, maxTTL+1, ...] represents an empty table.
     */
    private byte[] table;
    /**
     * The range (measured in hops) of files tracked by this.
     */
    private byte maxTTL;


    /** 
     * If ttl>=127, throws IllegalArgumentException.  Otherwise creates a new
     * QueryRouteTable that has space for initialSize keywords with TTL up to
     * ttl, inclusive.
     */
    public QueryRouteTable(byte ttl, int initialSize) {
        if (ttl>=127)
            throw new IllegalArgumentException();
        table=new byte[initialSize];
        for (int i=0; i<table.length; i++)
            table[i]=(byte)(ttl+1);
        maxTTL=ttl;
    }

    /**
     * Returns true if a response could be generated for qr.  Note that a return
     * value of true does not necessarily mean that a response will be
     * generated--just that it could.  It is assumed that qr's TTL has already
     * been decremented, and hence is potentially 0.
     */
    public boolean contains(QueryRequest qr) {
        //Check that all hashed keywords are reachable with given TTL.
        String[] keywords=HashFunction.keywords(qr.getQuery());
        int ttl=qr.getTTL();
        for (int i=0; i<keywords.length; i++) {
            String keyword=keywords[i];
            int hash=HashFunction.hash(keyword, table.length);
            if (table[hash]>ttl)
                return false;
        }
        return true;
    }
    
    /**
     * For all keywords k in filename, adds <k, 0> to this.
     */
    public void add(String filename) {
        add(filename, 0);
    }

    /**
     * For all keywords k in filename, adds <k, ttl> to this.
     * This method is public mainly for testing reasons.
     */
    public void add(String filename, int ttl) {
        String[] keywords=HashFunction.keywords(filename);
        for (int i=0; i<keywords.length; i++) {
            //See contains(..) for a discussion on TTLs and decrementing.
            for (int t=ttl; t<table.length; t++) {
                String keyword=keywords[i];
                int hash=HashFunction.hash(keyword, table.length);
                table[hash]=(byte)Math.min(ttl, table[hash]);
            }
        }
    }

    /**
     * For all <keyword_i, ttl_i> in m, adds <keyword_i, (ttl_i)+1> to this.
     * (This is useful for unioning lots of route tables for propoagation.)
     *
     *    @modifies this
     */
    public void addAll(QueryRouteTable qrt) {
        Assert.that(this.table.length==qrt.table.length,
                    "TODO2: table scaling not implemented.");
        for (int i=0; i<table.length; i++) 
            table[i]=(byte)Math.min(table[i], qrt.table[i]+1);
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
        for (int i=m.getStartOffset(); i<=m.getStopOffset(); i++) {
            try {
                table[i]=m.getTableTTL();
            } catch (IndexOutOfBoundsException e) {
                //The RESET message preceding this lied about the table
                //size.  Action could be take here to optimize.
            }
        }
    }

    private void handleTableMessage(SparseTableMessage m) {
        for (int j=0; j<m.getSize(); j++) {
            int i=m.getBlock(j);
            int ttl=m.getTableTTL();
            try {
                if (m.isAdd())
                    table[i]=(byte)ttl;
                else
                    table[i]=(byte)(ttl+1); //TODO: not maxTTL, right?
            } catch (IndexOutOfBoundsException e) {
                //The RESET message preceding this lied about the table
                //size.  Action could be take here to optimize.
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
    public Iterator /* of RouteTableMessage */ encode(QueryRouteTable prev) {
        //TODO1: this is not an optimal encoding.  Optimize.
        //TODO: this may get complicated.  Should we put it in another class?
        List messages=new ArrayList(maxTTL);
        if (prev==null) 
            messages.add(new ResetTableMessage((byte)1,
                                               (byte)0, table.length));
        
        //Dense encoding: reeeally stupid!
        for (int ttl=0; ttl<maxTTL; ttl++) {  //Off by 1?
            BitSet bits=new BitSet(table.length);            
            for (int i=0; i<table.length; i++) {
                if (table[i]<=ttl)            //Off by 1?
                    bits.set(i);
            }
            messages.add(new SetDenseTableMessage((byte)1, (byte)ttl, 
                                                  bits, 0, table.length));
        }

//          //"Differential" sparse encoding: for each TTL...
//          for (int ttl=0; ttl<tables.length; ttl++) {
//              //Find indices that need adding and removing for this TTL.
//              List /* of Integer */ addBlocks=new ArrayList();
//              List /* of Integer */ removeBlocks=new ArrayList();
//              //For each bit i of the table...
//              for (int i=0; i<tableLength; i++) {
//                  //If bit is set, wasn't set in previous message, and wasn't set
//                  //with a lower TTL, send ADD message.  (TODO: document with
//                  //picture from notes file.)
//                  if (tables[ttl].get(i)) {
//                      if ((ttl==0 || !tables[ttl-1].get(i))
//                              && (prev==null || !prev.tables[ttl].get(i)))
//                          addBlocks.add(new Integer(i));
//                      else if ((ttl==0 || !tables[ttl-1].get(i))
//                              && (prev==null || ttl==0 || prev.tables[ttl-1].get(i)))
//                          addBlocks.add(new Integer(i));
//                  }
//                  //If bit isn't set but was set in previous tables, send REMOVE
//                  //message.
//                  else {
//                      if (prev!=null && prev.tables[ttl].get(i))
//                          if (ttl==0 || !prev.tables[ttl-1].get(i))  
//                              removeBlocks.add(new Integer(i));
//                  }                
//              }

//              //Add ADD/REMOVE_SPARSE_BLOCK_VARIANT messages, as needed
//              if (addBlocks.size()>0)
//                  messages.add(SparseTableMessage.create((byte)ttl, //Table TTL
//                                                         true,
//                                                         addBlocks));
//              if (removeBlocks.size()>0)
//                  messages.add(SparseTableMessage.create((byte)ttl, //Table TTL
//                                                         false,
//                                                         removeBlocks));
//          }                                            

        return messages.iterator();
    }

    ////////////////////////////////////////////////////////////////////////////

    /** True if o is a QueryRouteTable with the same entries of this. */
    public boolean equals(Object o) {
        if (! (o instanceof QueryRouteTable))
            return false;

        //TODO: two qrt's can be equal even if they have different TTL ranges.
        QueryRouteTable other=(QueryRouteTable)o;
        if (this.table.length!=other.table.length)
            return false;

        for (int i=0; i<this.table.length; i++) {
            if (this.table[i]!=other.table[i])
                return false;
        }
        return true;
    }

    public String toString() {
        StringBuffer buf=new StringBuffer();
        buf.append("{");
        for (int i=0; i<table.length; i++) {
            if (table[i]<=maxTTL)
                buf.append(i+"/"+table[i]+", ");
        }
        buf.append("}");
        return buf.toString();
    }

    private static final byte min(byte a, byte b) {
        return a<=b ? a : b;
    }


    /** Unit test */
    public static void main(String args[]) {
        //1. Keyword tests (add, contains)
        QueryRouteTable qrt=new QueryRouteTable((byte)7, 1000);
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
//          Assert.that(! qrt.tables[2].equals(dummy));   //looking at rep
//          Assert.that(qrt.tables[3].equals(dummy));
//          Assert.that(qrt.tables[4].equals(dummy));
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
        QueryRouteTable qrt2=new QueryRouteTable((byte)7, size);
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
        QueryRouteTable qrt3=new QueryRouteTable((byte)7, size);
        Assert.that(! qrt3.equals(qrt2));
        for (Iterator iter=qrt2.encode(null); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            qrt3.update(m);
        }        
        Assert.that(qrt3.equals(qrt2), "Got "+qrt3+" not "+qrt2);

//          //5. Glass-box encode test.
//          //   qrt:  {a/0, x/1, c/3, d/4}
//          //   qrt2: {a/0, b/1, c/2, d/5}
//          //   ===>  ADD(b, 1), REMOVE(x, 1), ADD(c, 2), REMOVE(d, 4), ADD(d, 5)
//          qrt=new QueryRouteTable((byte)7, 1024);
//          qrt.add("a", 0);
//          qrt.add("x", 1);
//          qrt.add("c", 3);
//          qrt.add("d", 4);
//          qrt2=new QueryRouteTable((byte)7, 1024);
//          qrt2.add("a", 0);
//          qrt2.add("b", 1);
//          qrt2.add("c", 2);
//          qrt2.add("d", 5);
//          Iterator iter=qrt2.encode(qrt);
//          checkNext(iter, true,  1, "b");
//          checkNext(iter, false, 1, "x");
//          checkNext(iter, true,  2, "c");
//          checkNext(iter, false, 4, "d");
//          checkNext(iter, true,  5, "d");
    }

//      private static void checkNext(Iterator iter,
//                             boolean isAdd, int tableTTL, String value) {
//          SparseTableMessage s=(SparseTableMessage)iter.next();
//          System.out.println("Got "+s);
//          Assert.that(s.getTableTTL()==tableTTL,
//                      "Unexpected table TTL: "+s.getTableTTL()); 
//          Assert.that(s.getSize()==1,
//                      "Unexpected table size: "+s.getSize());
//          Assert.that(s.isAdd()==isAdd,
//                      "Unexpected variant: "+s.isAdd());
//          Assert.that(s.getBlock(0)==HashFunction.hash(value, 1024),
//                      "Unexpected block: "+s.getBlock(0));
        
//      }
}
