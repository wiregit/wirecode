package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.Utilities;
import com.limegroup.gnutella.xml.*;
import com.sun.java.util.collections.*;
import java.util.BitSet;
import java.util.zip.*;
import java.io.*;

//Please note that &#60; and &#62; are the HTML escapes for '<' and '>'.

/**
 * A list of query keywords that a connection can respond to, as well as the
 * minimum TTL for a response.  More formally, a QueryRouteTable is a (possibly
 * infinite!) list of keyword TTL pairs, [ &#60;keyword_1, ttl_1&#62;, ...,
 * &#60;keywordN, ttl_N&#62; ]  <p>
 *
 * 10/08/2002 - A day after Susheel's birthday, he decided to change this class
 * for the heck of it.  Kidding.  Functionality has been changed so that keyword
 * depth is 'constant' - meaning that if a keyword is added, then any contains
 * query regarding that keyword will return true.  This is because this general
 * idea of QRTs is only used in a specialized way in LW - namely, UPs use it for
 * their leaves ONLY, so the depth is always 1.  If you looking for a keyword
 * and it is in the table, a leaft MAY have it, so return true.  This only
 * needed a one line change.
 *
 * <b>This class is NOT synchronized.</b>
 */
public class QueryRouteTable {
    /** The suggested default table size. */
    public static final int DEFAULT_TABLE_SIZE=1<<14;  //16KB
    /** The maximum size of patch messages, in bytes. */
    public static final int MAX_PATCH_SIZE=1024; //1 KB

    /** The *new* table implementation.  The table of keywords - each value in
     *  the BitSet is either 'true' or 'false' - 'true' signifies that a keyword
     *  match MAY be at a leaf 1 hop away, whereas 'false' signifies it isn't.
     *  QRP is really not used in full by the Gnutella Ultrapeer protocol, hence
     *  the easy optimization of only using BitSets.
     */
    private BitSet bitTable;

    /** The 'logical' length of the BitSet.  Needed because the BitSet accessor
     *  methods don't seem to offer what is needed.
     */
    private int bitTableLength;

    /** The number of entries in this, used to make entries() run in O(1) time.
     *  INVARIANT: bitEntries=number of i s.t. bitTable[i]==true */
    private int bitEntries;

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
        this(DEFAULT_TABLE_SIZE);
    }

    /** 
     * Creates a new QueryRouteTable that has space for initialSize keywords
     * with hops up to but not including infinity.  The table is completely
     * empty, i.e., contains no keywords.  Hosts may want to send queries
     * down this table's connection until it is fully patched.
     */
    public QueryRouteTable(int initialSize) {
        initialize(initialSize);
    }

    private void initialize(int initialSize) {
        this.bitTable=new BitSet(initialSize);
        this.bitTableLength=initialSize;
        this.bitEntries=0;
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
        int ttl=qr.getTTL();
        byte bits=Utilities.log2(bitTableLength);

        //1. First we check that all the normal keywords of qr are in the route
        //   table.  Note that this is done with zero allocations!  Also note
        //   that HashFunction.hash() takes cares of the capitalization.
        String query=qr.getQuery();
        for (int i=0 ; ; ) {
            //Find next keyword...
            //    _ _ W O R D _ _ _ A B
            //    i   j       k
            int j=HashFunction.keywordStart(query, i);     
            if (j<0)
                break;
            int k=HashFunction.keywordEnd(query, j);

            //...and look up its hash.
            int hash=HashFunction.hash(query, j, k, bits);
            if (!contains(hash))
                return false;
            i=k+1;
        }        
        
        //2. Now we extract meta information in the query.  If there isn't any,
        //   declare success now.  Otherwise ensure that the URI is in the 
        //   table.  TODO: avoid allocations.
        //TODO2: avoid parsing if possible
        String richQuery = qr.getRichQuery();
        if (richQuery.equals(""))
            //Normal case for matching query with no metadata.
            return true;
        LimeXMLDocument doc = null;
        try {
            doc = new LimeXMLDocument(richQuery);
            String docSchemaURI = doc.getSchemaURI();
            int hash = HashFunction.hash(docSchemaURI, bits);
            if (!contains(hash))//don't know the URI? can't answer query
                return false;
        } catch(Exception e) {//malformed XML
            //TODO: avoid generic catch(Exception)
            return true;//coz leaf can answer based on normal query
        }
            
        //3. Finally check that "enough" of the metainformation keywords are in
        //   the table: 2/3 or 3, whichever is more.
        int wordCount=0;
        int matchCount=0;
        Iterator iter=doc.getKeyWords().iterator();
        while(iter.hasNext()) {
            //getKeyWords only returns all the fields, so we still need to split
            //the words.  The code is copied from part (1) above.  It could be
            //factored, but that's slightly tricky; the above code terminates if
            //a match fails--a nice optimization--while this code simply counts
            //the number of words and matches.
            String words = (String)iter.next();
            for (int i=0 ; ; ) {
                //Find next keyword...
                //    _ _ W O R D _ _ _ A B
                //    i   j       k
                int j=HashFunction.keywordStart(words, i);     
                if (j<0)
                    break;
                int k=HashFunction.keywordEnd(words, j);
                
                //...and look up its hash.
                int hash=HashFunction.hash(words, j, k, bits);
                if (contains(hash))
                    matchCount++;
                wordCount++;
                i=k+1;
            }
        }
        if (wordCount<3)
            //less than three word? 100% match required
            return wordCount==matchCount;
        else 
            //a 67% match will do...
            return ((float)matchCount/(float)wordCount) > 0.67;
    }
    
    // In the new version, we will not accept TTLs for methods.  Tables are only
    // 1 hop deep....
    private final boolean contains(int hash) {
        return bitTable.get(hash);
    }

    /**
     * For all keywords k in filename, adds <k> to this.
     */
    public void add(String filename) {
        addBTInternal(filename);
    }


    private void addBTInternal(String filename) {
        String[] words=HashFunction.keywords(filename);
        String[] keywords=HashFunction.getPrefixes(words);
        for (int i=0; i<keywords.length; i++) {
            int hash=HashFunction.hash(keywords[i], 
                                       Utilities.log2(bitTableLength));
            if (!bitTable.get(hash)) {
                bitEntries++; //added new entry
                bitTable.set(hash);
            }
        }
    }


    public void addIndivisible(String iString) {
        final int hash = HashFunction.hash(iString, 
                                           Utilities.log2(bitTableLength));
        if (!bitTable.get(hash)) {
            bitEntries++; //added new entry
            bitTable.set(hash);
        }
    }


    /**
     * For all <keyword_i> in qrt, adds <keyword_i> to this.
     * (This is useful for unioning lots of route tables for propoagation.)
     *
     *    @modifies this
     */
    public void addAll(QueryRouteTable qrt) {
        //This algorithm scales between tables of 2different lengths and TTLs.
        //Refer to the query routing paper for a full explanation.  If
        //performance is a problem, it's possible to special-case the algorithm
        //for when both tables have the same length and infinity:
        //
        //          for (int i=0; i<bitTableLength; i++)
        //              table[i]=(byte)Math.min(table[i], qrt.table[i]+1);
        //              //update entries accordingly

        int m=qrt.bitTableLength;
        int m2=this.bitTableLength;
        double scale=((double)m2)/((double)m);   //using float can cause round-off!
        for (int i=0; i<m; i++) {
            int low=(int)Math.floor(i*scale);
            int high=(int)Math.ceil((i+1)*scale);
            Assert.that(low>=0 && low<m2,
                        "Low value "+low+" for "+i+" incompatible with "+m2);
            Assert.that(high>=0 && high<=m2,
                        "High value "+high+" for "+i+" incompatible with "+m2);
            for (int i2=low; i2<high; i2++) {
                if (qrt.bitTable.get(i)!=bitTable.get(i2)) {
                    if (qrt.bitTable.get(i)) {
                        bitTable.set(i2);
                        bitEntries++;
                    }
                    // else bitTable[i] is already set..
                }
            }
        }
    }


    /** Returns the number of entries in this. */
    public int entries() {
        return bitEntries;
    }

    /** True if o is a QueryRouteTable with the same entries of this. */
    public boolean equals(Object o) {
        if (! (o instanceof QueryRouteTable))
            return false;

        //TODO: two qrt's can be equal even if they have different TTL ranges.
        QueryRouteTable other=(QueryRouteTable)o;
        if (this.bitTableLength!=other.bitTableLength)
            return false;

        if (!this.bitTable.equals(other.bitTable))
            return false;

        return true;
    }

    public String toString() {
        return bitTable.toString();
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
            initialize(reset.getTableSize()); 
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
        //0. Verify that m belongs in this sequence.  If we haven't just been
        //RESET, ensure that m's sequence size matches last message
        if (sequenceSize!=-1 && sequenceSize!=m.getSequenceSize())
            throw new BadPacketException("Inconsistent seq size: "
                                         +m.getSequenceSize()
                                         +" vs. "+sequenceSize);
        //If we were just reset, ensure that m's sequence number is one.
        //Otherwise it should be one greater than the last message received.
        if (sequenceNumber==-1 ? m.getSequenceNumber()!=1 //reset
                               : sequenceNumber+1!=m.getSequenceNumber())
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
                Assert.that(uncompressor!=null, 
                    "Null uncompressor.  Sequence: "+m.getSequenceNumber());
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
                boolean wasInfinity=(!bitTable.get(nextPatch));
                if (data[i] == -6)
                    bitTable.set(nextPatch);
                else if (data[i] == 6)
                    bitTable.clear(nextPatch);
                boolean isInfinity=(!bitTable.get(nextPatch));
                if (wasInfinity && !isInfinity)
                    bitEntries++;  //added entry
                else if (!wasInfinity && isInfinity)
                    bitEntries--;  //removed entry
            } catch (IndexOutOfBoundsException e) {
                throw new BadPacketException("Tried to patch "+nextPatch
                                             +" of an "+data.length
                                             +" element array.");
            }
            nextPatch++;
        }

        //4. Update sequence numbers.
        this.sequenceSize=m.getSequenceSize();
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
            buf.add(new ResetTableMessage(bitTableLength, (byte)7));
        else
            Assert.that(prev.bitTableLength==this.bitTableLength,
                        "TODO: can't deal with tables of different lengths");

        //1. Calculate patch array
        byte[] data=new byte[bitTableLength];
        //        byte[] data=new byte[bitTableLength];
        boolean needsPatch=false;
        boolean needsFullByte=false;
        for (int i=0; i<data.length; i++) {
            if (prev!=null) {
                if (this.bitTable.get(i) == prev.bitTable.get(i))
                    data[i] = 0;
                else if (this.bitTable.get(i))
                    data[i] = -6;
                else // prev.bitTable.get(i)
                    data[i] = 6;
            }
            else {
                if (this.bitTable.get(i))
                    data[i] = -6;
                else
                    data[i] = 0;
            }

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
    /* 
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
        Assert.that(dataCompressed.length<data.length);
        
        try {
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            for (int i=0; i<dataCompressed.length; ) {
                int length=Math.min(rand.nextInt(100), dataCompressed.length-i);
                byte[] chunk=new byte[length];
                System.arraycopy(dataCompressed, i, chunk, 0, length);
                byte[] chunkRead=dummy.uncompress(chunk);
                baos.write(chunkRead);
                i+=length;
            }
            baos.flush();
            Assert.that(Arrays.equals(data, baos.toByteArray()),
                        "Compress/uncompress loop failed");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //0.1. halve/unhalve
        Assert.that(extendNibble((byte)0x03)==0x03);
        Assert.that(extendNibble((byte)0x09)==(byte)0xF9);
        byte[] big={(byte)1, (byte)7, (byte)-1, (byte)-8};
        byte[] small={(byte)0x17, (byte)0xF8};
        Assert.that(Arrays.equals(halve(big), small));
        Assert.that(Arrays.equals(unhalve(small), big));

        QueryRouteTable qrt=new QueryRouteTable(1000);
        qrt.add("good book");
        Assert.that(qrt.entries()==2);
        qrt.add("bad");   //{good, book, bad}
        Assert.that(qrt.entries()==3);
        qrt.add("bad");   //{good, book, bad}
        Assert.that(qrt.entries()==3);

        //1. Simple keyword tests (add, contains)
        //we have moved to 1-bit entry per hash, so either absent or present....
        Assert.that(! qrt.contains(new QueryRequest((byte)4, 0, "garbage")));
        Assert.that(qrt.contains(new QueryRequest((byte)2, 0, "bad")));
        Assert.that(qrt.contains(new QueryRequest((byte)3, 0, "bad")));
        Assert.that(qrt.contains(new QueryRequest((byte)4, 0, "bad")));
        Assert.that(qrt.contains(new QueryRequest((byte)2, 0, "good bad")));
        Assert.that(! qrt.contains(new QueryRequest((byte)3, 0, "good bd")));
        Assert.that(qrt.contains(new QueryRequest((byte)3, 0, 
                                                  "good bad book")));
        Assert.that(! qrt.contains(new QueryRequest((byte)3, 0, 
                                                    "good bad bok")));

        //2. addAll tests
        QueryRouteTable qrt2=new QueryRouteTable(1000);
        Assert.that(qrt2.entries()==0);
        qrt2.add("new");
        qrt2.add("book");
        qrt2.addAll(qrt);     //{book, good, new, bad}
        QueryRouteTable qrt3=new QueryRouteTable(1000);
        Assert.that(qrt2.entries()==4);
        qrt3.add("book");
        qrt3.add("good");
        qrt3.add("new");
        qrt3.add("bad");
        Assert.that(qrt2.equals(qrt3));
        Assert.that(qrt3.equals(qrt2));

        //3. encode-decode test--with compression
        //qrt={good, book, bad}
        qrt2=new QueryRouteTable(1000);
        for (Iterator iter=qrt.encode(null); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            try { 
                qrt2.update(m); 
            } catch (BadPacketException e) {
            }
            if (m instanceof PatchTableMessage)
                Assert.that(((PatchTableMessage)m).getCompressor()
                    ==PatchTableMessage.COMPRESSOR_DEFLATE);
        }
        Assert.that(qrt2.equals(qrt), "Got \n    "+qrt2+"\nexpected\n    "+qrt);

        qrt.add("bad");
        qrt.add("other"); //qrt={good, book, bad, other}
        Assert.that(! qrt2.equals(qrt));
        for (Iterator iter=qrt.encode(qrt2); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            try { 
                qrt2.update(m); 
            } catch (BadPacketException e) {
            }
            if (m instanceof PatchTableMessage)
                Assert.that(((PatchTableMessage)m).getCompressor()
                    ==PatchTableMessage.COMPRESSOR_DEFLATE);
        }
        Assert.that(qrt2.equals(qrt));
        Assert.that(qrt.entries()==qrt2.entries());

        Iterator iter=qrt2.encode(qrt);
        Assert.that(! iter.hasNext());                     //test optimization

        iter=(new QueryRouteTable(1000).encode(null));  //blank table
        Assert.that(iter.next() instanceof ResetTableMessage);
        Assert.that(! iter.hasNext());
        
        //4. encode-decode test--without compression.  (We know compression
        //won't work because the table is very small and filled with random 
        //bytes.)
        qrt=new QueryRouteTable(10);
        rand=new Random();
        for (int i=0; i<qrt.bitTableLength; i++) 
            if (rand.nextBoolean())
                qrt.bitTable.set(i);
        qrt.bitTable.set(0);
        qrt2=new QueryRouteTable(10);
        Assert.that(! qrt2.equals(qrt));

        for (iter=qrt.encode(qrt2); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            try { 
                qrt2.update(m); 
            } catch (BadPacketException e) {
            }
            if (m instanceof PatchTableMessage)
                Assert.that(((PatchTableMessage)m).getCompressor()
                    ==PatchTableMessage.COMPRESSOR_NONE);
        }
        Assert.that(qrt2.equals(qrt));

        //4b. Encode/decode tests with multiple patched messages.
        qrt=new QueryRouteTable(5000);
        rand=new Random();
        for (int i=0; i<qrt.bitTableLength; i++)
            if (rand.nextBoolean())
                qrt.bitTable.set(i);
        qrt2=new QueryRouteTable(5000);
        Assert.that(! qrt2.equals(qrt));

        for (iter=qrt.encode(qrt2); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            try { 
                qrt2.update(m); 
            } catch (BadPacketException e) {
            }
        }
        Assert.that(qrt2.equals(qrt));

        //5. Interpolation/extrapolation glass-box tests.  Remember that +1 is
        //added to everything!
        qrt=new QueryRouteTable(4);  // 1 4 5 X ==> 2 6
        qrt2=new QueryRouteTable(2);
        qrt.bitTable.set(0);
        qrt.bitTable.set(1);
        qrt.bitTable.set(2);
        qrt.bitTable.clear(3);
        qrt2.addAll(qrt);
        Assert.that(qrt2.bitTable.get(0));
        Assert.that(qrt2.bitTable.get(1));

        //This also tests tables with different TTL problem.  (The 6 is qrt
        //is interepreted as infinity in qrt2, not a 7.)
        qrt=new QueryRouteTable(2);  // 1 X ==> 2 2 X X
        qrt2=new QueryRouteTable(4);
        qrt.bitTable.set(0);
        qrt.bitTable.clear(1);
        qrt2.addAll(qrt);
        Assert.that(qrt2.bitTable.get(0));
        Assert.that(qrt2.bitTable.get(1));
        Assert.that(!qrt2.bitTable.get(2));
        Assert.that(!qrt2.bitTable.get(3));

        qrt=new QueryRouteTable(4);  // 1 2 4 X ==> 2 3 5
        qrt2=new QueryRouteTable(3);
        qrt.bitTable.set(0);
        qrt.bitTable.set(1);
        qrt.bitTable.set(2);
        qrt.bitTable.clear(3);
        qrt2.addAll(qrt);
        Assert.that(qrt2.bitTable.get(0));
        Assert.that(qrt2.bitTable.get(1));
        Assert.that(qrt2.bitTable.get(2));
        Assert.that(qrt2.entries()==3);

        qrt=new QueryRouteTable(3);  // 1 4 X ==> 2 2 5 X
        qrt2=new QueryRouteTable(4);
        qrt.bitTable.set(0);
        qrt.bitTable.set(1);
        qrt.bitTable.clear(2);
        qrt2.addAll(qrt);
        Assert.that(qrt2.bitTable.get(0));
        Assert.that(qrt2.bitTable.get(1));
        Assert.that(qrt2.bitTable.get(2));
        Assert.that(!qrt2.bitTable.get(3));
        Assert.that(qrt2.entries()==3);

        //5b. Black-box test for addAll.
        qrt=new QueryRouteTable(128);
        qrt.add("good book");
        qrt.add("bad");   //{good/1, book/1, bad/3}
        qrt2=new QueryRouteTable(512);
        qrt2.addAll(qrt);
        Assert.that(qrt2.contains(new QueryRequest((byte)4, 0, "bad")));
        Assert.that(qrt2.contains(new QueryRequest((byte)4, 0, "good")));
        qrt2=new QueryRouteTable(32);
        qrt2.addAll(qrt);
        Assert.that(qrt2.contains(new QueryRequest((byte)4, 0, "bad")));
        Assert.that(qrt2.contains(new QueryRequest((byte)4, 0, "good")));

        //6. Test sequence numbers.
        qrt=new QueryRouteTable();   //a. wrong sequence after reset
        ResetTableMessage reset=null;
        PatchTableMessage patch=new PatchTableMessage((short)2, (short)2,
            PatchTableMessage.COMPRESSOR_DEFLATE, (byte)8, new byte[10], 0, 10);
        try {
            qrt.update(patch);
            Assert.that(false);
        } catch (BadPacketException e) { 
        }

        qrt=new QueryRouteTable();  //b. message sizes don't match
        try {
            reset=new ResetTableMessage(1024, (byte)2);
            qrt.update(reset);
            patch=new PatchTableMessage((short)1, (short)3,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
        } catch (BadPacketException e) {
            Assert.that(false);
        }
        try {
            patch=new PatchTableMessage((short)2, (short)4,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
            Assert.that(false);
        } catch (BadPacketException e) { 
        }

        qrt=new QueryRouteTable();  //c. message sequences don't match
        try {
            patch=new PatchTableMessage((short)1, (short)3,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
        } catch (BadPacketException e) {
            Assert.that(false);
        }
        try {
            patch=new PatchTableMessage((short)3, (short)3,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
            Assert.that(false);
        } catch (BadPacketException e) {
        }        

        qrt=new QueryRouteTable();  //d. sequence interrupted by reset
        try {
            patch=new PatchTableMessage((short)1, (short)3,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
            reset=new ResetTableMessage(1024, (byte)2);
            qrt.update(reset);
        } catch (BadPacketException e) {
            Assert.that(false);
        }
        try {
            patch=new PatchTableMessage((short)1, (short)6,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
        } catch (BadPacketException e) {
            Assert.that(false);
        }

        qrt=new QueryRouteTable();  //e. Sequence too big
        try {
            patch=new PatchTableMessage((short)1, (short)2,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
            patch=new PatchTableMessage((short)2, (short)2,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
        } catch (BadPacketException e) {
            Assert.that(false);
        }
        try {
            patch=new PatchTableMessage((short)3, (short)2,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
            Assert.that(false);
        } catch (BadPacketException e) {
        }
    }
    */
}
