package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.xml.*;
import com.sun.java.util.collections.*;
import java.util.zip.*;
import java.io.*;

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
    public static final int DEFAULT_TABLE_SIZE=1<<16;  //64KB
    /** The suggested default max table TTL. */
    public static final byte DEFAULT_INFINITY=(byte)7;
    /** The maximum size of patch messages, in bytes. */
    public static final int MAX_PATCH_SIZE=1024; //1 KB

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
    /** The number of entries in this, used to make entries() run in O(1) time.
     *  INVARIANT: entries=number of i s.t. table[i]<infinity */
    private int entries;

    /** The last message received of current sequence, or -1 if none. */
    private int sequenceNumber;
    /** The size of the current sequence, or -1 if none. */
    private int sequenceSize;

    /** The index of the next table entry to patch. */
    private int nextPatch;
    /** The uncompressor. This state must be maintained to implement chunked
     *  PATCH messages.  (You may need data from message N-1 to apply the patch
     *  in message N.) */
    private Inflater uncompressor;



    /////////////////////////////// Basic Methods ///////////////////////////


    /** Creates a QueryRouteTable with default sizes. */
    public QueryRouteTable() {
        this(DEFAULT_TABLE_SIZE, DEFAULT_INFINITY);
    }

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
        this.entries=0;
        this.sequenceNumber=-1;
        this.sequenceSize=-1;
        this.nextPatch=0;
    }

    /**
     * Returns true if a response could be generated for qr.  Note that a return
     * value of true does not necessarily mean that a response will be
     * generated--just that it could.  It is assumed that qr's TTL has already
     * been decremented, i.e., is the outbound not inbound TTL.  
     */
    public boolean contains(QueryRequest qr) {
        String richQuery = qr.getRichQuery();
        LimeXMLDocument doc = null;
        try{
            doc = new LimeXMLDocument(richQuery);
        }catch(Exception e){
            doc = null;
        }
        String richQueryWords="";
        if(doc!=null){            
            Iterator iter = doc.getKeyWords().iterator();
            while(iter.hasNext()){
                String str = (String)iter.next();
                richQueryWords=richQueryWords+" "+str;
            }
        }
        String allwords = richQueryWords+" "+qr.getQuery();
        //there are bound to be some blank spaces on either left side
        allwords = allwords.trim();
        //Check that all hashed keywords are reachable with given TTL.
        String[] keywords=HashFunction.keywords(allWords);
        int ttl=qr.getTTL();
        for (int i=0; i<keywords.length; i++) {
            String keyword=allWords[i];
            int hash=HashFunction.hash(keyword, log2(table.length));
            if (table[hash]>ttl || table[hash]>=infinity)
                return false;
        }
        return true;
    }
    
    /**
     * @requires num be a power of 2.
     */
    private static byte log2(int num)
    {
        return (byte)(Math.log(num)/Math.log(2));
    }
    
    /**
     * For all keywords k in filename, adds <k, 1> to this.
     */
    public void add(String filename) {
        add(filename, 1);
    }

    /**
     * For all keywords k in filename, adds <k, ttl> to this.
     * <b>For testing purposes only.</b>
     */
    public void add(String filename, int ttl) {
        String[] keywords=HashFunction.keywords(filename);
        for (int i=0; i<keywords.length; i++) {
            int hash=HashFunction.hash(keywords[i], log2(table.length));
            if (ttl<table[hash]) {
                if (table[hash]>=infinity)
                    entries++;  //added new entry
                table[hash]=(byte)ttl;
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
        //This algorithm scales between tables of different lengths and TTLs.
        //Refer to the query routing paper for a full explanation.  If
        //performance is a problem, it's possible to special-case the algorithm
        //for when both tables have the same length and infinity:
        //
        //          for (int i=0; i<table.length; i++)
        //              table[i]=(byte)Math.min(table[i], qrt.table[i]+1);
        //              //update entries accordingly

        int m=qrt.table.length;
        int m2=this.table.length;
        double scale=((double)m2)/((double)m);   //using float can cause round-off!
        for (int i=0; i<m; i++) {
            int low=(int)Math.floor(i*scale);
            int high=(int)Math.ceil((i+1)*scale);
            Assert.that(low>=0 && low<m2,
                        "Low value "+low+" for "+i+" incompatible with "+m2);
            Assert.that(high>=0 && high<=m2,
                        "High value "+high+" for "+i+" incompatible with "+m2);
            for (int i2=low; i2<high; i2++) {
                byte other;
                if (qrt.table[i]>=qrt.infinity)
                    other=this.infinity;
                else
                    other=(byte)(qrt.table[i]+1);
                if (other<table[i2]) {
                    if (table[i2]>=infinity)
                        entries++; //added new entry
                    table[i2]=other;
                }
            }
        }
    }


    /** Returns the number of entries in this. */
    public int entries() {
        return entries;
    }        

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


    ////////////////////// Core Encoding and Decoding //////////////////////

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

    //All encoding/decoding works in a pipelined manner, by continually
    //modifying a byte array called 'data'.  TODO2: we could avoid a lot of
    //allocations here if memory is at a premium.

    private void handlePatch(PatchTableMessage m) throws BadPacketException {
        //0. Verify that m belongs in this sequence.
        if (sequenceSize!=-1 && sequenceSize!=m.getSequenceSize())
            throw new BadPacketException("Inconsistent seq size: "
                                         +m.getSequenceSize()
                                         +" vs. "+sequenceSize);
        if (sequenceNumber!=-1 && sequenceNumber+1!=m.getSequenceNumber())
            throw new BadPacketException("Inconsistent seq number: "
                                         +m.getSequenceNumber()
                                         +" vs. "+sequenceNumber);

        byte[] data=m.getData();

        //1. Start pipelined uncompression.
        //TODO: check that compression is same as last message.
        if (m.getCompressor()==PatchTableMessage.COMPRESSOR_DEFLATE) {
            try {
                //a) If first message, create uncompressor (if needed).
                if (m.getSequenceNumber()==1)
                    uncompressor=new Inflater();
                Assert.that(uncompressor!=null, "Null uncompressor");
                data=uncompress(data);            
            } catch (IOException e) {
                throw new BadPacketException("Couldn't uncompress data: "+e);
            }
        } else if (m.getCompressor()!=PatchTableMessage.COMPRESSOR_NONE) {
            throw new BadPacketException("Unknown compressor");
        }
        
        //2. Expand nibbles if necessary.
        if (m.getEntryBits()==4) 
            data=unhalve(data);
        else if (m.getEntryBits()!=8)
            throw new BadPacketException("Unknown value for entry bits");

        //3. Add data[0...] to table[nextPatch...]            
        for (int i=0; i<data.length; i++) {
            try {
                boolean wasInfinity=(table[nextPatch]>=infinity);
                table[nextPatch]+=data[i];
                boolean isInfinity=(table[nextPatch]>=infinity);
                if (wasInfinity && !isInfinity)
                    entries++;  //added entry
                else if (!wasInfinity && isInfinity)
                    entries--;  //removed entry
            } catch (IndexOutOfBoundsException e) {
                throw new BadPacketException("Tried to patch "+nextPatch
                                             +" of an "+data.length
                                             +" element array.");
            }
            nextPatch++;
        }

        //4. Update sequence numbers.
        if (m.getSequenceNumber()!=m.getSequenceSize()) {            
            this.sequenceNumber=m.getSequenceNumber();
        } else {
            //Sequence complete.
            this.sequenceNumber=-1;
            this.sequenceSize=-1;
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

        //1. Calculate patch array
        byte[] data=new byte[table.length];
        boolean needsPatch=false;
        boolean needsFullByte=false;
        for (int i=0; i<data.length; i++) {
            if (prev!=null)
                data[i]=(byte)(this.table[i]-prev.table[i]);
            else
                data[i]=(byte)(this.table[i]-infinity);

            if (data[i]!=0)
                needsPatch=true;
            if (data[i]<-8 || data[i]>7) //can this fit in 4 signed bits?
                needsFullByte=true;
        }
        //Optimization: there's nothing to report.  If prev=null, send a single
        //RESET.  Otherwise send nothing.
        if (!needsPatch) {
            return buf.iterator();
        }


        //2. Try compression.
        byte bits=8;
        if (! needsFullByte) {
            data=halve(data);
            bits=4;
        }

        byte[] patchCompressed=compress(data);
        byte compression=PatchTableMessage.COMPRESSOR_NONE;
        if (patchCompressed.length<data.length) {
            //...Hooray!  Compression was efficient.
            data=patchCompressed;
            compression=PatchTableMessage.COMPRESSOR_DEFLATE;
        }
                   

        //3. Break into 1KB chunks and send.  TODO: break size limits if needed.
        final int chunks=(int)Math.ceil((float)data.length/(float)MAX_PATCH_SIZE);
        int chunk=1;
        for (int i=0; i<data.length; i+=MAX_PATCH_SIZE) {
            //Just past the last position of data to copy.
            //Note special case for last chunk.  
            int stop=Math.min(i+MAX_PATCH_SIZE, data.length);
            buf.add(new PatchTableMessage((short)chunk, (short)chunks,
                                          compression, bits,
                                          data, i, stop));
            chunk++;
        }        
        return buf.iterator();        
    }


    ///////////////// Helper Functions for Codec ////////////////////////

    /** Returns a GZIP'ed version of data. */
    private byte[] compress(byte[] data) {
        try {
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            DeflaterOutputStream dos=new DeflaterOutputStream(baos);
            dos.write(data, 0, data.length);
            dos.close();                      //flushes bytes
            return baos.toByteArray();
        } catch (IOException e) {
            //This should REALLY never happen because no devices are involved.
            //But could we propogate it up.
            Assert.that(false, "Couldn't write to byte stream");
            return null;
        }
    }

    /** Returns the uncompressed version of the given defalted bytes, using
     *  any dictionaries in uncompressor.  Throws IOException if the data is
     *  corrupt.
     *      @requires inflater initialized 
     *      @modifies inflater */
    private byte[] uncompress(byte[] data) throws IOException {
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        uncompressor.setInput(data);
        
        try {
            byte[] buf=new byte[1024];
            while (true) {
                int read=uncompressor.inflate(buf);
                //Needs input?
                if (read==0)
                    break;
                baos.write(buf, 0, read);                
            }
            baos.flush();
            return baos.toByteArray();
        } catch (DataFormatException e) {
            throw new IOException("Bad deflate format");
        }
    }

    
    /** Returns an array R of length array.length/2, where R[i] consists of the
     *  low nibble of array[2i] concatentated with the low nibble of array[2i+1].
     *  Note that unhalve(halve(array))=array if all elements of array fit can 
     *  fit in four signed bytes.
     *      @requires array.length is a multiple of two */
    private static byte[] halve(byte[] array) {
        byte[] ret=new byte[array.length/2];
        for (int i=0; i<ret.length; i++)
            ret[i]=(byte)((array[2*i]<<4) | (array[2*i+1]&0xF));
        return ret;
    }

    /** Returns an array of R of length array.length*2, where R[i] is the the
     *  sign-extended high nibble of floor(i/2) if i even, or the sign-extended
     *  low nibble of floor(i/2) if i odd. */        
    private static byte[] unhalve(byte[] array) {
        byte[] ret=new byte[array.length*2];
        for (int i=0; i<array.length; i++) {
            ret[2*i]=(byte)(array[i]>>4);     //sign extension
            ret[2*i+1]=extendNibble((byte)(array[i]&0xF));
        }
        return ret;
    }    
    
    /** Sign-extends the low nibble of b, i.e., 
     *  returns (from MSB to LSB) b[3]b[3]b[3]b[3]b[3]b[2]b[1]b[0]. */
    private static byte extendNibble(byte b) {
        if ((b&0x8)!=0)   //negative nibble; sign-extend.
            return (byte)(0xF0 | b);
        else
            return b;        
    }


    ////////////////////////////// Unit Tests ////////////////////////////////
    
    /** Unit test */
    public static void main(String args[]) {
        //TODO: test handle bad packets (sequences, etc)

        //0. compress/uncompress.  First we make a huge array with lots of
        //random bytes but also long strings of zeroes.  This means that
        //compression will work, but not too well.  Then we take the compressed
        //value and dice it up randomly.  It's critical to make sure that
        //decompress works incrementally without blocking.
        QueryRouteTable dummy=new QueryRouteTable();
        dummy.uncompressor=new Inflater();
        byte[] data=new byte[10000];
        Random rand=new Random();
        rand.nextBytes(data);
        for (int i=100; i<7000; i++) {
            data[i]=(byte)0;
        }
        byte[] dataCompressed=dummy.compress(data);
        //System.out.println("Compressed 10000 to "+dataCompressed.length);
        Assert.that(dataCompressed.length<data.length);
        
        try {
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            for (int i=0; i<dataCompressed.length; ) {
                int length=Math.min(rand.nextInt(100), dataCompressed.length-i);
                //System.out.print(length+"/");
                byte[] chunk=new byte[length];
                System.arraycopy(dataCompressed, i, chunk, 0, length);
                byte[] chunkRead=dummy.uncompress(chunk);
                //System.out.print(chunkRead.length+" ");
                baos.write(chunkRead);
                i+=length;
            }
            baos.flush();
            Assert.that(Arrays.equals(data, baos.toByteArray()),
                        "Compress/uncompress loop failed");
        } catch (IOException e) {
            System.out.println("Decompress failed: "+e);
            e.printStackTrace();
        }

        //0.1. halve/unhalve
        Assert.that(extendNibble((byte)0x03)==0x03);
        Assert.that(extendNibble((byte)0x09)==(byte)0xF9);
        byte[] big={(byte)1, (byte)7, (byte)-1, (byte)-8};
        byte[] small={(byte)0x17, (byte)0xF8};
        Assert.that(Arrays.equals(halve(big), small));
        Assert.that(Arrays.equals(unhalve(small), big));

        QueryRouteTable qrt=new QueryRouteTable(1000, (byte)7);
        qrt.add("good book");
        Assert.that(qrt.entries()==2);
        qrt.add("bad", 3);   //{good/1, book/1, bad/3}
        Assert.that(qrt.entries()==3);
        qrt.add("bad", 4);   //{good/1, book/1, bad/3}
        Assert.that(qrt.entries()==3);

        //1. Simple keyword tests (add, contains)
        Assert.that(! qrt.contains(new QueryRequest((byte)4, 0, "garbage")));
        Assert.that(! qrt.contains(new QueryRequest((byte)2, 0, "bad")));
        Assert.that(qrt.contains(new QueryRequest((byte)3, 0, "bad")));
        Assert.that(qrt.contains(new QueryRequest((byte)4, 0, "bad")));
        Assert.that(! qrt.contains(new QueryRequest((byte)2, 0, "good bad")));
        Assert.that(qrt.contains(new QueryRequest((byte)3, 0, "good bad")));

        //2. addAll tests
        QueryRouteTable qrt2=new QueryRouteTable(1000, (byte)7);
        Assert.that(qrt2.entries()==0);
        qrt2.add("new", 3);
        qrt2.add("book", 1);
        qrt2.addAll(qrt);     //{book/1, good/2, new/3, bad/4}
        QueryRouteTable qrt3=new QueryRouteTable(1000, (byte)7);
        Assert.that(qrt2.entries()==4);
        qrt3.add("book", 1);
        qrt3.add("good", 2);
        qrt3.add("new", 3);
        qrt3.add("bad", 4);
        Assert.that(qrt2.equals(qrt3));
        Assert.that(qrt3.equals(qrt2));

        //3. encode-decode test--with compression
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
            if (m instanceof PatchTableMessage)
                Assert.that(((PatchTableMessage)m).getCompressor()
                    ==PatchTableMessage.COMPRESSOR_DEFLATE);
        }
        Assert.that(qrt2.equals(qrt), "Got \n    "+qrt2+"\nexpected\n    "+qrt);

        System.out.println();
        qrt.add("bad", 2);
        qrt.add("other", 4); //qrt={good/1, book/1, bad/2, other/4}
        Assert.that(! qrt2.equals(qrt));
        for (Iterator iter=qrt.encode(qrt2); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            System.out.println("Got "+m);
            try { 
                qrt2.update(m); 
            } catch (BadPacketException e) {
                System.out.println("Got bad packet "+m);
            }
            if (m instanceof PatchTableMessage)
                Assert.that(((PatchTableMessage)m).getCompressor()
                    ==PatchTableMessage.COMPRESSOR_DEFLATE);
        }
        Assert.that(qrt2.equals(qrt));
        Assert.that(qrt.entries()==qrt2.entries());

        Iterator iter=qrt2.encode(qrt);
        Assert.that(! iter.hasNext());                     //test optimization

        iter=(new QueryRouteTable(1000, (byte)7)).encode(null);  //blank table
        Assert.that(iter.next() instanceof ResetTableMessage);
        Assert.that(! iter.hasNext());

        //4. encode-decode test--without compression.  (We know compression
        //won't work because the table is very small and filled with random bytes.)
        qrt=new QueryRouteTable(10, (byte)10);
        rand=new Random();
        for (int i=0; i<qrt.table.length; i++)
            qrt.table[i]=(byte)rand.nextInt(qrt.infinity+1);
        qrt.table[0]=(byte)1;
        qrt2=new QueryRouteTable(10, (byte)10);
        Assert.that(! qrt2.equals(qrt));

        System.out.println();
        for (iter=qrt.encode(qrt2); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            System.out.println("Got "+m);
            try { 
                qrt2.update(m); 
            } catch (BadPacketException e) {
                System.out.println("Got bad packet "+m);
            }
            if (m instanceof PatchTableMessage)
                Assert.that(((PatchTableMessage)m).getCompressor()
                    ==PatchTableMessage.COMPRESSOR_NONE);
        }
        Assert.that(qrt2.equals(qrt));

        //4b. Encode/decode tests with multiple patched messages.
        qrt=new QueryRouteTable(5000, (byte)10);
        rand=new Random();
        for (int i=0; i<qrt.table.length; i++)
            qrt.table[i]=(byte)rand.nextInt(qrt.infinity+1);
        qrt2=new QueryRouteTable(5000, (byte)10);
        Assert.that(! qrt2.equals(qrt));

        System.out.println();
        for (iter=qrt.encode(qrt2); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            System.out.println("Got "+m);
            try { 
                qrt2.update(m); 
            } catch (BadPacketException e) {
                System.out.println("Got bad packet "+e);
            }
        }
        Assert.that(qrt2.equals(qrt));

        //5. Interpolation/extrapolation glass-box tests.  Remember that +1 is
        //added to everything!
        qrt=new QueryRouteTable(4, (byte)7);  // 1 4 5 X ==> 2 6
        qrt2=new QueryRouteTable(2, (byte)7);
        qrt.table[0]=(byte)1;
        qrt.table[1]=(byte)4;
        qrt.table[2]=(byte)5;
        qrt.table[3]=qrt.infinity;
        qrt2.addAll(qrt);
        Assert.that(qrt2.table[0]==(byte)2, "Got: "+qrt2.table[0]);
        Assert.that(qrt2.table[1]==(byte)6, "Got: "+qrt2.table[1]);

        //This also tests tables with different TTL problem.  (The 6 is qrt
        //is interepreted as infinity in qrt2, not a 7.)
        qrt=new QueryRouteTable(2, (byte)6);  // 1 X ==> 2 2 X X
        qrt2=new QueryRouteTable(4, (byte)8);
        qrt.table[0]=(byte)1;
        qrt.table[1]=qrt.infinity;
        qrt2.addAll(qrt);
        Assert.that(qrt2.table[0]==(byte)2);
        Assert.that(qrt2.table[1]==(byte)2);
        Assert.that(qrt2.table[2]>=qrt.infinity);
        Assert.that(qrt2.table[3]>=qrt.infinity);

        qrt=new QueryRouteTable(4, (byte)8);  // 1 2 4 X ==> 2 3 5
        qrt2=new QueryRouteTable(3, (byte)7);
        qrt.table[0]=(byte)1;
        qrt.table[1]=(byte)2;
        qrt.table[2]=(byte)4;
        qrt.table[3]=qrt.infinity;
        qrt2.addAll(qrt);
        Assert.that(qrt2.table[0]==(byte)2);
        Assert.that(qrt2.table[1]==(byte)3);
        Assert.that(qrt2.table[2]==(byte)5);
        Assert.that(qrt2.entries()==3);

        qrt=new QueryRouteTable(3, (byte)7);  // 1 4 X ==> 2 2 5 X
        qrt2=new QueryRouteTable(4, (byte)7);
        qrt.table[0]=(byte)1;
        qrt.table[1]=(byte)4;
        qrt.table[2]=qrt.infinity;
        qrt2.addAll(qrt);
        Assert.that(qrt2.table[0]==(byte)2);
        Assert.that(qrt2.table[1]==(byte)2, "Got: "+qrt2.table[1]);
        Assert.that(qrt2.table[2]==(byte)5);
        Assert.that(qrt2.table[3]==qrt.infinity);
        Assert.that(qrt2.entries()==3);

        //5b. Black-box test for addAll.
        qrt=new QueryRouteTable(100, (byte)7);
        qrt.add("good book");
        qrt.add("bad", 3);   //{good/1, book/1, bad/3}
        qrt2=new QueryRouteTable(213, (byte)7);
        qrt2.addAll(qrt);
        Assert.that(qrt2.contains(new QueryRequest((byte)4, 0, "bad")));
        Assert.that(qrt2.contains(new QueryRequest((byte)4, 0, "good")));
        qrt2=new QueryRouteTable(59, (byte)7);
        qrt2.addAll(qrt);
        Assert.that(qrt2.contains(new QueryRequest((byte)4, 0, "bad")));
        Assert.that(qrt2.contains(new QueryRequest((byte)4, 0, "good")));

        //6. Test sequence numbers.
//          qrt=new QueryRouteTable();
//          PatchTableMessage patch=new PatchTableMessage((short)2, (short)2,
//              PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
//          try {
//              qrt.update(patch);
//              Assert.that(false);
//          } catch (BadPacketException e) { }
        
    }
}
