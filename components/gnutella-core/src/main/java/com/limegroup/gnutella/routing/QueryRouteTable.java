package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;

//Please note that &#60; and &#62; are the HTML escapes for '<' and '>'.

/**
 * A list of query keywords that a connection can respond to, as well as the
 * minimum TTL for a response.  More formally, a QueryRouteTable is a (possibly
 * infinite!) list of keyword TTL pairs, [ &#60;keyword_1, ttl_1&#62;, ...,
 * &#60;keywordN, ttl_N&#62; ]  <p>
 *
 * <b>This class is NOT synchronized.</b>
 */
public class QueryRouteTable {
    /** The suggested default table size. */
    public static final int DEFAULT_TABLE_SIZE=8192;
    /** The suggested default max table TTL. */
    public static final byte DEFAULT_INFINITY=(byte)10;

    /** 
     * The table of keywords and their associated TTLs.  Each value table[i] is
     * the minimum number of hops <b>minus one</b> to a file that matches a
     * keyword with hash i.  (Note that this corresponds to the minimum TTL for
     * a matching query.)  If table[i]>=infinity, no file in the horizon matches
     * a keyword with hash i.
     *
     * In other words, [0, 0, ... ] represents a completely full table, while
     * [infinity, infinity, ...] represents an empty table.  
     */
    private byte[] table;
    /** The max distance (measured in hops) of files tracked by this, i.e., the 
     *  value used in table for infinity. */
    private byte infinity;

    /** The last message received of current sequence, or -1 if none. */
    private int sequenceNumber;
    /** The size of the current sequence, or -1 if none. */
    private int sequenceSize;

    /** The index of the next table entry to patch. */
    private int nextPatch;
    /** True if this has been fully patched following a reset. */
    private boolean isPatched;

    /** 
     * Creates a new QueryRouteTable that has space for initialSize keywords
     * with hops up to but not including infinity.  The table is completely
     * empty, i.e., contains no keywords.  Hosts may want to send queries
     * down this table's connection until it is fully patched.
     */
    public QueryRouteTable(int initialSize, byte infinity) {
        initialize(initialSize, infinity);
    }

    private void initialize(int initialSize, byte infinity) {
        this.table=new byte[initialSize];
        Arrays.fill(table, infinity);
        this.infinity=infinity;
        this.sequenceNumber=-1;
        this.sequenceSize=-1;
        this.nextPatch=0;
        this.isPatched=false;
    }

    /**
     * Returns true if a response could be generated for qr.  Note that a return
     * value of true does not necessarily mean that a response will be
     * generated--just that it could.  It is assumed that qr's TTL has already
     * been decremented, i.e., is the outbound not inbound TTL.  
     */
    public boolean contains(QueryRequest qr) {
        //Check that all hashed keywords are reachable with given TTL.
        String[] keywords=HashFunction.keywords(qr.getQuery());
        int ttl=qr.getTTL();
        for (int i=0; i<keywords.length; i++) {
            String keyword=keywords[i];
            int hash=HashFunction.hash(keyword, table.length);
            if (table[hash]>ttl || table[hash]>=infinity)
                return false;
        }
        return true;
    }
    
    /**
     * For all keywords k in filename, adds <k, 1> to this.
     */
    public void add(String filename) {
        add(filename, 1);
    }

    /**
     * For all keywords k in filename, adds <k, ttl> to this.
     * Useful for testing.
     */
    private void add(String filename, int ttl) {
        String[] keywords=HashFunction.keywords(filename);
        for (int i=0; i<keywords.length; i++) {
            int hash=HashFunction.hash(keywords[i], table.length);
            table[hash]=(byte)Math.min(ttl, table[hash]);
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
    public void update(RouteTableMessage m) throws BadPacketException {
        switch (m.getVariant()) {                      
        case RouteTableMessage.RESET_VARIANT:
            ResetTableMessage reset=(ResetTableMessage)m;
            initialize(reset.getTableSize(), reset.getInfinity()); 
            return;
        case RouteTableMessage.PATCH_VARIANT:
            PatchTableMessage patch=(PatchTableMessage)m;
            handlePatch(patch);
            return;
        default:
            //Ignore.
            return;
        }
    }

    private void handlePatch(PatchTableMessage m) throws BadPacketException {
        //1. Verify that m belongs in this sequence.
        if (sequenceSize!=-1 && sequenceSize!=m.getSequenceSize())
            throw new BadPacketException("Inconsistent seq size: "
                                         +m.getSequenceSize()
                                         +" vs. "+sequenceSize);
        if (sequenceNumber!=-1 && sequenceNumber+1!=m.getSequenceNumber())
            throw new BadPacketException("Inconsistent seq number: "
                                         +m.getSequenceNumber()
                                         +" vs. "+sequenceNumber);
        Assert.that(m.getCompressor()==PatchTableMessage.COMPRESSOR_NONE,
                    "TODO: uncompress");
        Assert.that(m.getEntryBits()==8, "TODO: entry bits not implemented");

        //2. Add data[0...] to table[nextPatch...]
        byte[] data=m.getData();
        for (int i=0; i<data.length; i++) {
            try {
                table[nextPatch]+=data[i];
            } catch (IndexOutOfBoundsException e) {
                throw new BadPacketException("Tried to patch "+nextPatch
                                             +" of an "+data.length
                                             +" element array.");
            }
            nextPatch++;
        }

        //3. Update sequence numbers.
        if (m.getSequenceNumber()!=m.getSequenceSize()) {            
            this.sequenceNumber=m.getSequenceNumber();
        } else {
            //Sequence complete.
            this.sequenceNumber=-1;
            this.sequenceSize=-1;
            this.isPatched=true;
            this.nextPatch=0; //TODO: is this right?
        }   
    }
    

    /**
     * Returns an iterator of RouteTableMessage that will convey the state of
     * this.  If prev is null, this will include a reset.  Otherwise it will
     * include only those messages needed to to convert prev to this.  More
     * formally, for any non-null QueryRouteTable's m and prev, the following 
     * holds:
     *
     * <pre>
     * for (Iterator iter=m.encode(); iter.hasNext(); ) 
     *    prev.update((RouteTableUpdate)iter.next());
     * Assert.that(prev.equals(m)); 
     * </pre> 
     */
    public Iterator /* of RouteTableMessage */ encode(QueryRouteTable prev) {
        List /* of RouteTableMessage */ buf=new LinkedList();
        if (prev==null)
            buf.add(new ResetTableMessage(table.length, infinity));
        else
            Assert.that(prev.table.length==this.table.length,
                        "TODO: can't deal with tables of different lengths");

        //TODO: different values of entryBits, split messages, compression,
        //avoid updates if nothings changed
        boolean gotDifference=false;
        byte[] patch=new byte[table.length];
        for (int i=0; i<patch.length; i++) {
            if (prev!=null)
                patch[i]=(byte)(this.table[i]-prev.table[i]);
            else
                patch[i]=(byte)(this.table[i]-infinity);

            if (patch[i]!=0)
                gotDifference=true;
        }
        //As an optimization, we don't send message if no changes.
        if (! gotDifference) {
            buf.clear();
            return buf.iterator();
        }

        buf.add(new PatchTableMessage((short)1, (short)1,
                                      PatchTableMessage.COMPRESSOR_NONE,
                                      (byte)8, patch, 0, patch.length));
        
        return buf.iterator();        
    }

    /** Returns true if this has been fully patched following a reset. */
    public boolean isPatched() {
        return isPatched;
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
            if (table[i]<infinity)
                buf.append(i+"/"+table[i]+", ");
        }
        buf.append("}");
        return buf.toString();
    }


    /** Unit test */
    public static void main(String args[]) {
        QueryRouteTable qrt=new QueryRouteTable(1000, (byte)7);
        qrt.add("good book");
        qrt.add("bad", 3);   //{good/1, book/1, bad/3}

        //1. Simple keyword tests (add, contains)
        Assert.that(! qrt.contains(new QueryRequest((byte)4, 0, "garbage")));
        Assert.that(! qrt.contains(new QueryRequest((byte)2, 0, "bad")));
        Assert.that(qrt.contains(new QueryRequest((byte)3, 0, "bad")));
        Assert.that(qrt.contains(new QueryRequest((byte)4, 0, "bad")));
        Assert.that(! qrt.contains(new QueryRequest((byte)2, 0, "good bad")));
        Assert.that(qrt.contains(new QueryRequest((byte)3, 0, "good bad")));

        //2. addAll tests
        QueryRouteTable qrt2=new QueryRouteTable(1000, (byte)7);
        qrt2.add("new", 3);
        qrt2.add("book", 1);
        qrt2.addAll(qrt);     //{book/1, good/2, new/3, bad/4}
        QueryRouteTable qrt3=new QueryRouteTable(1000, (byte)7);
        qrt3.add("book", 1);
        qrt3.add("good", 2);
        qrt3.add("new", 3);
        qrt3.add("bad", 4);
        Assert.that(qrt2.equals(qrt3));
        Assert.that(qrt3.equals(qrt2));

        //3. encode-decode test.
        //qrt={good/1, book/1, bad/3}
        qrt2=new QueryRouteTable(1000, (byte)7);
        for (Iterator iter=qrt.encode(null); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            System.out.println("Got "+m);
            try { 
                qrt2.update(m); 
            } catch (BadPacketException e) {
                System.out.println("Got bad packet "+m);
            }
        }
        Assert.that(qrt2.equals(qrt), "Got \n    "+qrt2+"\nexpected\n    "+qrt);
        System.out.println("");

        qrt.add("bad", 2);
        qrt.add("other", 4); //{good/1, book/1, bad/2, other/4}
        Assert.that(! qrt2.equals(qrt));
        for (Iterator iter=qrt.encode(qrt2); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            System.out.println("Got "+m);
            try { 
                qrt2.update(m); 
            } catch (BadPacketException e) {
                System.out.println("Got bad packet "+m);
            }
        }
        Assert.that(qrt2.equals(qrt));

        Iterator iter=qrt2.encode(qrt);
        Assert.that(! iter.hasNext());
        iter=(new QueryRouteTable(1000, (byte)7)).encode(null);  //blank table
        Assert.that(! iter.hasNext());
    }
}
