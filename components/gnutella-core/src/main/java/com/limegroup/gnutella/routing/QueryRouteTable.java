package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.Utilities;
import com.limegroup.gnutella.util.BitSet;
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
 * 10/08/2002 - A day after Susheel's birthday, he decided to change this class
 * for the heck of it.  Kidding.  Functionality has been changed so that keyword
 * depth is 'constant' - meaning that if a keyword is added, then any contains
 * query regarding that keyword will return true.  This is because this general
 * idea of QRTs is only used in a specialized way in LW - namely, UPs use it for
 * their leaves ONLY, so the depth is always 1.  If you looking for a keyword
 * and it is in the table, a leaf MAY have it, so return true.  This only
 * needed a one line change.
 *
 * <b>This class is NOT synchronized.</b>
 */
public class QueryRouteTable {
    /** The suggested default max table TTL. */
    public static final byte DEFAULT_INFINITY=(byte)7;
    /** What should come across the wire if a keyword is present. */
    public static final byte KEYWORD_PRESENT=(byte)(1 - DEFAULT_INFINITY);
    /** What should come across the wire if a keyword is absent. */
    public static final byte KEYWORD_ABSENT=(byte)(DEFAULT_INFINITY - 1);
    /** What should come across the wire if a keyword status is unchanged. */
    public static final byte KEYWORD_NO_CHANGE=(byte)0;
    /** The suggested default table size. */
    public static final int DEFAULT_TABLE_SIZE=1<<16;  //64KB
    /** The maximum size of patch messages, in bytes. */
    public static final int MAX_PATCH_SIZE=1<<12;      //4 KB

    /** The *new* table implementation.  The table of keywords - each value in
     *  the BitSet is either 'true' or 'false' - 'true' signifies that a keyword
     *  match MAY be at a leaf 1 hop away, whereas 'false' signifies it isn't.
     *  QRP is really not used in full by the Gnutella Ultrapeer protocol, hence
     *  the easy optimization of only using BitSets.
     */
    private BitSet bitTable;
    
    /**
     * The cached resized QRT.
     */
    private QueryRouteTable resizedQRT = null;

    /** The 'logical' length of the BitSet.  Needed because the BitSet accessor
     *  methods don't seem to offer what is needed.
     */
    private int bitTableLength;

    /** The number of entries in this, used to make entries() run in O(1) time.
     *  INVARIANT: bitEntries=number of i s.t. bitTable[i]==true */
//    private int bitEntries;

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
     * Creates a new <tt>QueryRouteTable</tt> instance with the specified
     * size.  This <tt>QueryRouteTable</tt> will be completely empty with
     * no keywords -- no queries will have hits in this route table until
     * patch messages are received.
     *
     * @param size the size of the query routing table
     */
    public QueryRouteTable(int size) {
        initialize(size);
    }

    /**
     * Initializes this <tt>QueryRouteTable</tt> to the specified size.
     * This table will be empty until patch messages are received.
     *
     * @param size the size of the query route table
     */
    private void initialize(int size) {
        this.bitTableLength = size;
        this.bitTable = new BitSet(bitTableLength);
        this.sequenceNumber = -1;
        this.sequenceSize = -1;
        this.nextPatch = 0;                
    }

    /**
     * Returns true if a response could be generated for qr.  Note that a return
     * value of true does not necessarily mean that a response will be
     * generated--just that it could.  It is assumed that qr's TTL has already
     * been decremented, i.e., is the outbound not inbound TTL.  
     */
    public boolean contains(QueryRequest qr) {
        byte bits=Utilities.log2(bitTableLength);

        //1. First we check that all the normal keywords of qr are in the route
        //   table.  Note that this is done with zero allocations!  Also note
        //   that HashFunction.hash() takes cares of the capitalization.
        String query = qr.getQuery();
        String richQuery = qr.getRichQuery();
		if(query.length() == 0 && 
		   richQuery.length() == 0 && 
		   !qr.hasQueryUrns()) {
			return false;
		}
		if(qr.hasQueryUrns()) {
			Set urns = qr.getQueryUrns();
			Iterator iter = urns.iterator();
			while(iter.hasNext()) {
				URN qurn = (URN)iter.next();
				int hash = HashFunction.hash(qurn.toString(), bits);
				if(contains(hash)) {
					// we note a match if any one of the hashes matches
					return true;
				}
			}
			return false;
		}
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
        //String richQuery = qr.getRichQuery();
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
		byte log2 = Utilities.log2(bitTableLength);
        for (int i=0; i<keywords.length; i++) {
            int hash=HashFunction.hash(keywords[i], log2);
            if (!bitTable.get(hash)) {
//                bitEntries++; //added new entry
                resizedQRT = null;
                bitTable.set(hash);
            }
        }
    }


    public void addIndivisible(String iString) {
        final int hash = HashFunction.hash(iString, 
                                           Utilities.log2(bitTableLength));
        if (!bitTable.get(hash)) {
//            bitEntries++; //added new entry
            resizedQRT = null;
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
        this.bitTable.or( qrt.resize(this.bitTableLength) );
    }
    
    /**
     * Scales the internal cached BitSet to size 'newSize'
     */
    private BitSet resize(int newSize) {
        // if this bitTable is already the correct size,
        // return it
        if ( bitTableLength == newSize )
            return bitTable;
            
        // if we already have a cached resizedQRT and
        // it is the correct size, then use it.
        if ( resizedQRT != null && resizedQRT.bitTableLength == newSize )
            return resizedQRT.bitTable;

        // we must construct a new QRT of this size.            
        resizedQRT = new QueryRouteTable(newSize);
        
        //This algorithm scales between tables of 2different lengths and TTLs.
        //Refer to the query routing paper for a full explanation.
        int m = this.bitTableLength;
        int m2 = resizedQRT.bitTableLength;
        double scale=((double)m2)/((double)m);   //using float can cause round-off!
    
        for (int i = this.bitTable.nextSetBit(0); i >= 0;
          i = this.bitTable.nextSetBit(i+1)) {
            int low=(int)Math.floor(i*scale);
            int high=(int)Math.ceil((i+1)*scale);
            resizedQRT.bitTable.set(low, high);
        }
        return resizedQRT.bitTable;
    }


    /** Returns the number of entries in this. */
//    public int entries() {
//        return bitEntries;
//    }

    /** True if o is a QueryRouteTable with the same entries of this. */
    public boolean equals(Object o) {
        if ( this == o )
            return true;
            
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

    public int hashCode() {
        return bitTable.hashCode() * 17;
    }


    public String toString() {
        return "QueryRouteTable : " + bitTable.toString();
    }


    ////////////////////// Core Encoding and Decoding //////////////////////


    /**
     * Resets this <tt>QueryRouteTable</tt> to the specified size with
     * no data.  This is done when a RESET message is received.
     *
     * @param rtm the <tt>ResetTableMessage</tt> containing the size
     *  to reset the table to
     */
    public void reset(ResetTableMessage rtm) {
        initialize(rtm.getTableSize());
    }

    /**
     * Adds the specified patch message to this query routing table.
     *
     * @param patch the <tt>PatchTableMessage</tt> containing the new
     *  data to add
     * @throws <tt>BadPacketException</tt> if the sequence number or size
     *  is incorrect
     */
    public void patch(PatchTableMessage patch) throws BadPacketException {
        handlePatch(patch);        
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
                boolean wasSet = bitTable.get(nextPatch);
                boolean isSet = wasSet;
                if (data[i] == KEYWORD_PRESENT) {
                    bitTable.set(nextPatch);
                    isSet = true;
                }
                else if (data[i] == KEYWORD_ABSENT) {
                    bitTable.clear(nextPatch);
                    isSet = false;
                }                
                if (wasSet != isSet) {
                    resizedQRT = null;
                }
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
     * Stub for calling encode(QueryRouteTable, true).
     */
    public Iterator /* of RouteTableMessage */ encode(QueryRouteTable prev) {
        return encode(prev, true);
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
    public Iterator /* of RouteTableMessage */ encode(
      QueryRouteTable prev, boolean allowCompression) {
        List /* of RouteTableMessage */ buf=new LinkedList();
        if (prev==null)
            buf.add(new ResetTableMessage(bitTableLength, DEFAULT_INFINITY));
        else
            Assert.that(prev.bitTableLength==this.bitTableLength,
                        "TODO: can't deal with tables of different lengths");

        //1. Calculate patch array
        byte[] data=new byte[bitTableLength];
        //        byte[] data=new byte[bitTableLength];
        boolean needsPatch=false;
        boolean needsFullByte=false;
        for (int i=0; i<data.length; i++) {
            boolean thisGet = this.bitTable.get(i);
            if (prev!=null) {
                if (thisGet == prev.bitTable.get(i))
                    data[i] = KEYWORD_NO_CHANGE;
                else if (thisGet)
                    data[i] = KEYWORD_PRESENT;
                else // prev.bitTable.get(i)
                    data[i] = KEYWORD_ABSENT;
            }
            else {
                if (thisGet)
                    data[i] = KEYWORD_PRESENT;
                else
                    data[i] = KEYWORD_NO_CHANGE;
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
        //TODO: Should this not be done if compression isn't allowed?
        byte bits=8;
        if (! needsFullByte) {
            data=halve(data);
            bits=4;
        }

        byte compression=PatchTableMessage.COMPRESSOR_NONE;
        //Optimization: If we are told it is safe to compress the message,
        //then attempt to compress it.  Reasons it is not safe include
        //the outgoing stream already being compressed.
        if( allowCompression ) {
            byte[] patchCompressed=compress(data);
            if (patchCompressed.length<data.length) {
                //...Hooray!  Compression was efficient.
                data=patchCompressed;
                compression=PatchTableMessage.COMPRESSOR_DEFLATE;
            }
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
    static byte[] halve(byte[] array) {
        byte[] ret=new byte[array.length/2];
        for (int i=0; i<ret.length; i++)
            ret[i]=(byte)((array[2*i]<<4) | (array[2*i+1]&0xF));
        return ret;
    }

    /** Returns an array of R of length array.length*2, where R[i] is the the
     *  sign-extended high nibble of floor(i/2) if i even, or the sign-extended
     *  low nibble of floor(i/2) if i odd. */        
    static byte[] unhalve(byte[] array) {
        byte[] ret=new byte[array.length*2];
        for (int i=0; i<array.length; i++) {
            ret[2*i]=(byte)(array[i]>>4);     //sign extension
            ret[2*i+1]=extendNibble((byte)(array[i]&0xF));
        }
        return ret;
    }    
    
    /** Sign-extends the low nibble of b, i.e., 
     *  returns (from MSB to LSB) b[3]b[3]b[3]b[3]b[3]b[2]b[1]b[0]. */
    static byte extendNibble(byte b) {
        if ((b&0x8)!=0)   //negative nibble; sign-extend.
            return (byte)(0xF0 | b);
        else
            return b;        
    }


}
