package com.limegroup.gnutella.routing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.BitSet;
import com.limegroup.gnutella.util.Utilities;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * 
 * 
 * 
 * 
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
 * 12/05/2003 - Two months after Susheel's birthday, this class was changed to
 * once again accept variable infinity values.  Over time, optimizations had
 * removed the ability for a QueryRouteTable to have an infinity that wasn't
 * 7.  However, nothing outright checked that, so patch messages that were
 * based on a non-7 infinity were silently failing (always stayed empty).
 * In practice, we could probably even change the infinity to 2, and
 * change the number of entryBits to 2, with the keywordPresent and
 * keywordAbsent values going to 1 and -1, cutting the size of our patch
 * messages further in half (a quarter of the original size).  This would
 * probably require upgrading the X-Query-Routing to another version.
 *
 * This class is NOT synchronized.
 * 
 * Please note that &#60; and &#62; are the HTML escapes for '<' and '>'.
 */
public class QueryRouteTable {

	//done

	/**
	 * 7, we pass a value of 7 TTL for infinity in the QRP messages we send.
	 * This is the suggested default max table TTL.
	 * 
	 * In the early days of Gnutella, messages actually had a TTL of 7.
	 * Now, however, this is just 7 for historical reasons.
     */
    public static final byte DEFAULT_INFINITY = (byte)7;

    //do

    /** What should come across the wire if a keyword status is unchanged. */
    public static final byte KEYWORD_NO_CHANGE = (byte)0;

    //done

    /** 65536, our QRP table will be 65536 bytes big, which is 64 KB of data. */
    public static final int DEFAULT_TABLE_SIZE = 1 << 16; // 1 shifted up 16 bits is 65536

    /**
     * 4096 bytes, 4 KB.
     * encode() breaks a QRP table into 4 KB pieces and sends them in QRP patch table messages.
     */
    public static final int MAX_PATCH_SIZE = 1 << 12;

    //do

    /**
     * 7
     * 
     * The current infinity this table is using. Necessary for creating
     * ResetTableMessages with the correct infinity.
     */
    private byte infinity;

    /**
     * -6
     * 1 - infinity
     * 
     * What should come across the wire if a keyword is present.
     * The nature of this value is dependent on the infinity of the
     * ResetTableMessage.
     */
    private byte keywordPresent;

    /**
     * 6
     * infinity - 1
     * 
     * What should come across the wire if a keyword is absent.
     * The nature of thsi value is dependent on the infinity of the
     * ResetTableMessage.
     */
    private byte keywordAbsent;

    /**
     * The *new* table implementation.  The table of keywords - each value in
     * the BitSet is either 'true' or 'false' - 'true' signifies that a keyword
     * match MAY be at a leaf 1 hop away, whereas 'false' signifies it isn't.
     * QRP is really not used in full by the Gnutella Ultrapeer protocol, hence
     * the easy optimization of only using BitSets.
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

    /*
     * /////////////////////////////// Basic Methods ///////////////////////////
     */

    /**
     * 
     * 
     * Creates a QueryRouteTable with default sizes.
     * 
     * 
     * FileManager.buildQRT() makes a new QueryRouteTable object with this constructor.
     * ManagedConnection.patchQueryRouteTable(PatchTableMessage) also calls here.
     * 
     * 
     */
    public QueryRouteTable() {

        this(DEFAULT_TABLE_SIZE);
    }

    /**
     * 
     * FileManager.getQRT() makes a new QueryRouteTable object with this constructor.
     * 
     * 
     * 
     * Creates a new <tt>QueryRouteTable</tt> instance with the specified
     * size.  This <tt>QueryRouteTable</tt> will be completely empty with
     * no keywords -- no queries will have hits in this route table until
     * patch messages are received.
     * 
     * @param size the size of the query routing table
     */
    public QueryRouteTable(int size) {

        this(size, DEFAULT_INFINITY);
    }

    //done

    /**
     * Make a new QueryRouteTable object to represent a QRP table that describes what keywords might generate hits.
     * ManagedConnection.resetQueryRouteTable(ResetTableMessage) calls this constructor.
     * 
     * Creates a new <tt>QueryRouteTable</tt> instance with the specified
     * size and infinity.  This <tt>QueryRouteTable</tt> will be completely
     * empty with no keywords -- no queries will have hits in this route
     * table until patch messages are received.
     * 
     * @param size     The size of the QRP table, like 65536 bytes
     * @param infinity The infinity number, like 7
     */
    public QueryRouteTable(int size, byte infinity) {

    	// Clear this QueryRouteTable, making it block everything, and set its size and infinity value
        initialize(size, infinity);
    }

    /**
     * Clear this QueryRouteTable, making it block everything, and set its size and infinity value.
     * 
     * Initializes this <tt>QueryRouteTable</tt> to the specified size.
     * This table will be empty until patch messages are received.
     * 
     * @param The size of the QRT table, like 65536 bytes
     * @param The infinity TTL, like 7
     */
    private void initialize(int size, byte infinity) {

    	// Save the given table size, the number of bytes it will take up
        this.bitTableLength = size;

        // Make a new BitSet, an array of bits that can grow as we set them
        this.bitTable = new BitSet();

        // We don't know the sequence number or sequence size yet
        this.sequenceNumber = -1;
        this.sequenceSize = -1;

        // The next patch message we expect will have sequence number 0
        this.nextPatch = 0;

        // Set numbers based on the infinity TTL, which is probably 7
        this.keywordPresent = (byte)(1 - infinity); // -6, the number that indicates a keyword is present
        this.keywordAbsent = (byte)(infinity - 1);  // 6, the number that indicates a keyword is absent
        this.infinity = infinity;                   // 7, the infinity value this table is using
    }

    //do

    /**
     * Returns the size of this QueryRouteTable.
     */
    public int getSize() {

        return bitTableLength;
    }    
    
    /**
     * Returns the percentage of slots used in this QueryRouteTable's BitTable.
     * The return value is from 0 to 100.
     */
    public double getPercentFull() {
        double set = bitTable.cardinality();
        return ( set / bitTableLength ) * 100.0;
	}
	
	/**
	 * Returns the number of empty elements in the table.
	 */
	public int getEmptyUnits() {
	    return bitTable.unusedUnits();
	}
	
	/**
	 * Returns the total number of units allocated for storage.
	 */
	public int getUnitsInUse() {
	    return bitTable.getUnitsInUse();
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
        LimeXMLDocument richQuery = qr.getRichQuery();
		if(query.length() == 0 && 
		   richQuery == null && 
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
        //   table.
        if (richQuery == null) //Normal case for matching query with no metadata.
            return true;
        String docSchemaURI = richQuery.getSchemaURI();
        int hash = HashFunction.hash(docSchemaURI, bits);
        if (!contains(hash))//don't know the URI? can't answer query
            return false;
            
        //3. Finally check that "enough" of the metainformation keywords are in
        //   the table: 2/3 or 3, whichever is more.
        int wordCount=0;
        int matchCount=0;
        Iterator iter=richQuery.getKeyWords().iterator();
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
                int wordHash = HashFunction.hash(words, j, k, bits);
                if (contains(wordHash))
                    matchCount++;
                wordCount++;
                i=k+1;
            }
        }

        // some parts of the query are indivisible, so do some nonstandard
        // matching
        iter=richQuery.getKeyWordsIndivisible().iterator();
        while(iter.hasNext()) {
            hash = HashFunction.hash((String)iter.next(), bits);
            if (contains(hash))
                matchCount++;
            wordCount++;
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
    public void add(String filePath) {
        addBTInternal(filePath);
    }


    private void addBTInternal(String filePath) {
        String[] words = HashFunction.keywords(filePath);
        String[] keywords=HashFunction.getPrefixes(words);
		byte log2 = Utilities.log2(bitTableLength);
        for (int i=0; i<keywords.length; i++) {
            int hash=HashFunction.hash(keywords[i], log2);
            if (!bitTable.get(hash)) {
                resizedQRT = null;
                bitTable.set(hash);
            }
        }
    }


    public void addIndivisible(String iString) {
        final int hash = HashFunction.hash(iString, 
                                           Utilities.log2(bitTableLength));
        if (!bitTable.get(hash)) {
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
    	
        this.bitTable.or(qrt.resize(this.bitTableLength));
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
        
        //This algorithm scales between tables of different lengths.
        //Refer to the query routing paper for a full explanation.
        //(The below algorithm, contributed by Philippe Verdy,
        // uses integer values instead of decimal values
        // as both double & float can cause precision problems on machines
        // with odd setups, causing the wrong values to be set in tables)
        final int m = this.bitTableLength;
        final int m2 = resizedQRT.bitTableLength;
        for (int i = this.bitTable.nextSetBit(0); i >= 0;
          i = this.bitTable.nextSetBit(i + 1)) {
             // floor(i*m2/m)
             final int firstSet = (int)(((long)i * m2) / m);
             i = this.bitTable.nextClearBit(i + 1);
             // ceil(i*m2/m)
             final int lastNotSet = (int)(((long)i * m2 - 1) / m + 1);
             resizedQRT.bitTable.set(firstSet, lastNotSet);
        }
        
        return resizedQRT.bitTable;
    }

    /** True if o is a QueryRouteTable with the same entries of this. */
    public boolean equals(Object o) {
        if ( this == o )
            return true;
            
        if (! (o instanceof QueryRouteTable))
            return false;

        //TODO: two qrt's can be equal even if they have different TTL ranges.
        QueryRouteTable other = (QueryRouteTable)o;
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

    //done

    /*
     * ////////////////////// Core Encoding and Decoding //////////////////////
     */

    /**
     * Reset this QueryRouteTable to the given size, making it block everything.
     * We do this when a remote computer sends a QRP reset table message.
     * 
     * @param rtm The ResetTableMessage a remote computer sent us
     */
    public void reset(ResetTableMessage rtm) {

        // Clear this QueryRouteTable, making it block everything, and set its size and infinity value
        initialize(rtm.getTableSize(), rtm.getInfinity());
    }

    //do

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
                if (m.getSequenceNumber()==1) {
                    uncompressor = new Inflater();
                }       
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
            if(nextPatch >= bitTableLength)
                throw new BadPacketException("Tried to patch "+nextPatch
                                             +" on a bitTable of size "
                                             + bitTableLength);
            // All negative values indicate presence
            if (data[i] < 0) {
                bitTable.set(nextPatch);
                resizedQRT = null;
            }
            // All positive values indicate absence
            else if (data[i] > 0) {
                bitTable.clear(nextPatch);
                resizedQRT = null;
            }
            nextPatch++;
        }
        bitTable.compact();

        //4. Update sequence numbers.
        this.sequenceSize=m.getSequenceSize();
        if (m.getSequenceNumber()!=m.getSequenceSize()) {            
            this.sequenceNumber=m.getSequenceNumber();
        } else {
            //Sequence complete.
            this.sequenceNumber=-1;
            this.sequenceSize=-1;
            this.nextPatch=0; //TODO: is this right?
            // if this last message was compressed, release the uncompressor.
            if( this.uncompressor != null ) {
                this.uncompressor.end();
                this.uncompressor = null;
            }
        }   
    }

    //done

    /** Not used. */
    public List encode(QueryRouteTable prev) { // List of RouteTableMessage objects
        return encode(prev, true);
    }

    //do

    /**
     * Make all the QRP messages we need to completely describe the QRP table this QueryRouteTable object represents.
     * Makes a ResetTableMessage followed by any number of PatchTableMessage objects, and returns them in a List.
     * 
     * 
     * Only MessageRouter.forwardQueryRouteTables() calls this method.
     * 
     * 
     * 
     * Returns an List of RouteTableMessage that will convey the state of
     * this.  If that is null, this will include a reset.  Otherwise it will
     * include only those messages needed to to convert that to this.  More
     * formally, for any non-null QueryRouteTable's m and that, the following
     * holds:
     * 
     * <pre>
     * for (Iterator iter = m.encode(); iter.hasNext();)
     *    prev.update((RouteTableUpdate)iter.next());
     * Assert.that(prev.equals(m));
     * </pre>
     * 
     * @param prev
     * @param allowCompression
     * @return                 A LinkedList of ResetTableMessage and PatchTableMessage objects.
     *                         Both of these classes extend RouteTableMessage, so you can look at all the items in the list as though they were RouteTableMessage objects.
     *                         Send these messages in their order in the list to completely communicate this QRP table to a remote computer.
     */
    public List encode(QueryRouteTable prev, boolean allowCompression) {

        List buf = new LinkedList();

        if (prev == null) {

        	buf.add(new ResetTableMessage(bitTableLength, infinity));

        } else {

            Assert.that(prev.bitTableLength == this.bitTableLength, "TODO: can't deal with tables of different lengths");
        }

        //1. Calculate patch array
        byte[] data = new byte[bitTableLength];

        // Fill up data with KEYWORD_NO_CHANGE, since the majority
        // of elements will be that.
        // Because it is already filled, we do not need to iterate and
        // set it anywhere.
        Utilities.fill(data, 0, bitTableLength, KEYWORD_NO_CHANGE);
        boolean needsPatch=false;
        
        //1a. If there was a previous table, determine if it was the same one.
        //    If so, we can prevent BitTableLength calls to BitSet.get(int).
        if( prev != null ) {
            //1a-I. If they are not equal, xOr the tables and loop
            //      through the different bits.  This avoids
            //      bitTableLength*2 calls to BitSet.get
            //      at the cost of the xOr'd table's cardinality
            //      calls to both BitSet.nextSetBit and BitSet.get.
            //      Generally it is worth it, as our BitTables don't
            //      change very rapidly.
            //      With the xOr'd table, we know that all 'clear'
            //      values have not changed.  Thus, we can use
            //      nextSetBit on the xOr'd table & this.bitTable.get
            //      to determine whether or not we should set
            //      data[x] to keywordPresent or keywordAbsent.
            //      Because this is an xOr, we know that if 
            //      this.bitTable.get is true, prev.bitTable.get
            //      is false, and vice versa.            
            if(!this.bitTable.equals(prev.bitTable) ) {
                BitSet xOr = (BitSet)this.bitTable.clone();
                xOr.xor(prev.bitTable);
                for (int i=xOr.nextSetBit(0); i >= 0; i=xOr.nextSetBit(i+1)) {
                    data[i] = this.bitTable.get(i) ?
                        keywordPresent : keywordAbsent;
                    needsPatch = true;
                }
            }
            // Else the two tables are equal, and we don't need to do anything
            // because all elements already contain KEYWORD_NO_CHANGE.
        }
        //1b. If there was no previous table, scan through the table using
        //    nextSetBit, avoiding bitTableLength calls to BitSet.get(int).
        else {
            for (int i=bitTable.nextSetBit(0);i>=0;i=bitTable.nextSetBit(i+1)){
                data[i] = keywordPresent;
                needsPatch = true;
            }
        }
        //Optimization: there's nothing to report.  If prev=null, send a single
        //RESET.  Otherwise send nothing.
        if (!needsPatch) {
            return buf;
        }


        //2. Try compression.
        //TODO: Should this not be done if compression isn't allowed?
        byte bits=8;
        // Only halve if our values require 4 signed bits at most.
        // keywordPresent will always be negative and
        // keywordAbsent will always be positive.
        if( keywordPresent >= -8 && keywordAbsent <= 7 ) {
            bits = 4;
            data = halve(data);
        }

        byte compression=PatchTableMessage.COMPRESSOR_NONE;
        //Optimization: If we are told it is safe to compress the message,
        //then attempt to compress it.  Reasons it is not safe include
        //the outgoing stream already being compressed.
        if( allowCompression ) {
            byte[] patchCompressed = IOUtils.deflate(data);
            if (patchCompressed.length<data.length) {
                //...Hooray!  Compression was efficient.
                data=patchCompressed;
                compression=PatchTableMessage.COMPRESSOR_DEFLATE;
            }
        }

        /*
         * 3. Break into 1KB chunks and send.  TODO: break size limits if needed.
         * 
         * Actually, it looks like this breaks it into 4 KB chunks.
         */

        // Calculate how many 4 KB chunks it will take to hold the whole QRP table
        final int chunks = (int)Math.ceil( // Match ceiling, round up to the next number
        	(float)data.length /           // The number of bytes in the QRP table
        	(float)MAX_PATCH_SIZE);        // 4 KB in bytes

        int chunk = 1;

        // Move i to the start of each chunk, like 0, 4096, 8192, through the location of the last chunk
        for (int i = 0; i < data.length; i += MAX_PATCH_SIZE) {

        	/*
        	 * Just past the last position of data to copy.
        	 * Note special case for last chunk.
        	 */

        	// Compute the distance in bytes from the start of the table to where this chunk ends
        	int stop = Math.min(    // Choose whichever is smaller
        		i + MAX_PATCH_SIZE, // The index to the end of the chunk if this chunk is full
        		data.length);       // The index of the very end of the table

        	// Make a new PatchTableMessage for this 4 KB chunk of table, and add it to our buffer of them to send
            buf.add(                   // Add it to our buffer of them to send
            	new PatchTableMessage( // Make a new PatchTableMessage
            		(short)chunk,      // The sequence number to use, like 1, 2, 3, and so on
            		(short)chunks,     // The number of QRP patch table messages we're going to send to send the whole table
            		compression,       // 0x01 if the table is deflate compressed, 0x00 if it's not
            		bits,              // (do)
            		data,              // The array to find the data in
            		i,                 // Start looking this far into the array
            		stop));            // Stop when reaching this point in the array

            chunk++;
        }

        return buf;
    }

    ///////////////// Helper Functions for Codec ////////////////////////

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
     *  fit in four signed bits.
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
