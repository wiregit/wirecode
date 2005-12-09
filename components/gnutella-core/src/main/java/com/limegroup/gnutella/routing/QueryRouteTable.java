pbckage com.limegroup.gnutella.routing;

import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Set;
import jbva.util.zip.DataFormatException;
import jbva.util.zip.Inflater;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.util.BitSet;
import com.limegroup.gnutellb.util.Utilities;
import com.limegroup.gnutellb.util.IOUtils;
import com.limegroup.gnutellb.xml.LimeXMLDocument;

//Plebse note that &#60; and &#62; are the HTML escapes for '<' and '>'.

/**
 * A list of query keywords thbt a connection can respond to, as well as the
 * minimum TTL for b response.  More formally, a QueryRouteTable is a (possibly
 * infinite!) list of keyword TTL pbirs, [ &#60;keyword_1, ttl_1&#62;, ...,
 * &#60;keywordN, ttl_N&#62; ]  <p>
 *
 * 10/08/2002 - A dby after Susheel's birthday, he decided to change this class
 * for the heck of it.  Kidding.  Functionblity has been changed so that keyword
 * depth is 'constbnt' - meaning that if a keyword is added, then any contains
 * query regbrding that keyword will return true.  This is because this general
 * ideb of QRTs is only used in a specialized way in LW - namely, UPs use it for
 * their lebves ONLY, so the depth is always 1.  If you looking for a keyword
 * bnd it is in the table, a leaf MAY have it, so return true.  This only
 * needed b one line change.
 *
 * 12/05/2003 - Two months bfter Susheel's birthday, this class was changed to
 * once bgain accept variable infinity values.  Over time, optimizations had
 * removed the bbility for a QueryRouteTable to have an infinity that wasn't
 * 7.  However, nothing outright checked thbt, so patch messages that were
 * bbsed on a non-7 infinity were silently failing (always stayed empty).
 * In prbctice, we could probably even change the infinity to 2, and change
 * chbnge the number of entryBits to 2, with the keywordPresent and
 * keywordAbsent vblues going to 1 and -1, cutting the size of our patch
 * messbges further in half (a quarter of the original size).  This would
 * probbbly require upgrading the X-Query-Routing to another version.
 *
 * <b>This clbss is NOT synchronized.</b>
 */
public clbss QueryRouteTable {
    /** 
     * The suggested defbult max table TTL.
     */
    public stbtic final byte DEFAULT_INFINITY=(byte)7;
    /** Whbt should come across the wire if a keyword status is unchanged. */
    public stbtic final byte KEYWORD_NO_CHANGE=(byte)0;
    /** The suggested defbult table size. */
    public stbtic final int DEFAULT_TABLE_SIZE=1<<16;  //64KB
    /** The mbximum size of patch messages, in bytes. */
    public stbtic final int MAX_PATCH_SIZE=1<<12;      //4 KB
    
    /**
     * The current infinity this tbble is using.  Necessary for creating
     * ResetTbbleMessages with the correct infinity.
     */
    privbte byte infinity;
    
    /**
     * Whbt should come across the wire if a keyword is present.
     * The nbture of this value is dependent on the infinity of the
     * ResetTbbleMessage.
     */
    privbte byte keywordPresent;
    
    /**
     * Whbt should come across the wire if a keyword is absent.
     * The nbture of thsi value is dependent on the infinity of the
     * ResetTbbleMessage.
     */
    privbte byte keywordAbsent;

    /** The *new* tbble implementation.  The table of keywords - each value in
     *  the BitSet is either 'true' or 'fblse' - 'true' signifies that a keyword
     *  mbtch MAY be at a leaf 1 hop away, whereas 'false' signifies it isn't.
     *  QRP is reblly not used in full by the Gnutella Ultrapeer protocol, hence
     *  the ebsy optimization of only using BitSets.
     */
    privbte BitSet bitTable;
    
    /**
     * The cbched resized QRT.
     */
    privbte QueryRouteTable resizedQRT = null;

    /** The 'logicbl' length of the BitSet.  Needed because the BitSet accessor
     *  methods don't seem to offer whbt is needed.
     */
    privbte int bitTableLength;

    /** The lbst message received of current sequence, or -1 if none. */
    privbte int sequenceNumber;
    /** The size of the current sequence, or -1 if none. */
    privbte int sequenceSize;

    /** The index of the next tbble entry to patch. */
    privbte int nextPatch;
    /** The uncompressor. This stbte must be maintained to implement chunked
     *  PATCH messbges.  (You may need data from message N-1 to apply the patch
     *  in messbge N.) */
    privbte Inflater uncompressor;



    /////////////////////////////// Bbsic Methods ///////////////////////////


    /** Crebtes a QueryRouteTable with default sizes. */
    public QueryRouteTbble() {
        this(DEFAULT_TABLE_SIZE);
    }

    /**
     * Crebtes a new <tt>QueryRouteTable</tt> instance with the specified
     * size.  This <tt>QueryRouteTbble</tt> will be completely empty with
     * no keywords -- no queries will hbve hits in this route table until
     * pbtch messages are received.
     *
     * @pbram size the size of the query routing table
     */
    public QueryRouteTbble(int size) {
        this(size, DEFAULT_INFINITY);
    }
    
    /**
     * Crebtes a new <tt>QueryRouteTable</tt> instance with the specified
     * size bnd infinity.  This <tt>QueryRouteTable</tt> will be completely 
     * empty with no keywords -- no queries will hbve hits in this route 
     * tbble until patch messages are received.
     *
     * @pbram size the size of the query routing table
     * @pbram infinity the infinity to use
     */
    public QueryRouteTbble(int size, byte infinity) {
        initiblize(size, infinity);
    }    

    /**
     * Initiblizes this <tt>QueryRouteTable</tt> to the specified size.
     * This tbble will be empty until patch messages are received.
     *
     * @pbram size the size of the query route table
     */
    privbte void initialize(int size, byte infinity) {
        this.bitTbbleLength = size;
        this.bitTbble = new BitSet();
        this.sequenceNumber = -1;
        this.sequenceSize = -1;
        this.nextPbtch = 0;
        this.keywordPresent = (byte)(1 - infinity);
        this.keywordAbsent = (byte)(infinity - 1);
        this.infinity = infinity;
    }
    
    /**
     * Returns the size of this QueryRouteTbble.
     */
    public int getSize() {
        return bitTbbleLength;
    }    
    
    /**
     * Returns the percentbge of slots used in this QueryRouteTable's BitTable.
     * The return vblue is from 0 to 100.
     */
    public double getPercentFull() {
        double set = bitTbble.cardinality();
        return ( set / bitTbbleLength ) * 100.0;
	}
	
	/**
	 * Returns the number of empty elements in the tbble.
	 */
	public int getEmptyUnits() {
	    return bitTbble.unusedUnits();
	}
	
	/**
	 * Returns the totbl number of units allocated for storage.
	 */
	public int getUnitsInUse() {
	    return bitTbble.getUnitsInUse();
	}

    /**
     * Returns true if b response could be generated for qr.  Note that a return
     * vblue of true does not necessarily mean that a response will be
     * generbted--just that it could.  It is assumed that qr's TTL has already
     * been decremented, i.e., is the outbound not inbound TTL.  
     */
    public boolebn contains(QueryRequest qr) {
        byte bits=Utilities.log2(bitTbbleLength);

        //1. First we check thbt all the normal keywords of qr are in the route
        //   tbble.  Note that this is done with zero allocations!  Also note
        //   thbt HashFunction.hash() takes cares of the capitalization.
        String query = qr.getQuery();
        LimeXMLDocument richQuery = qr.getRichQuery();
		if(query.length() == 0 && 
		   richQuery == null && 
		   !qr.hbsQueryUrns()) {
			return fblse;
		}
		if(qr.hbsQueryUrns()) {
			Set urns = qr.getQueryUrns();
			Iterbtor iter = urns.iterator();
			while(iter.hbsNext()) {
				URN qurn = (URN)iter.next();
				int hbsh = HashFunction.hash(qurn.toString(), bits);
				if(contbins(hash)) {
					// we note b match if any one of the hashes matches
					return true;
				}
			}
			return fblse;
		}
        for (int i=0 ; ; ) {
            //Find next keyword...
            //    _ _ W O R D _ _ _ A B
            //    i   j       k
            int j=HbshFunction.keywordStart(query, i);     
            if (j<0)
                brebk;
            int k=HbshFunction.keywordEnd(query, j);

            //...bnd look up its hash.
            int hbsh=HashFunction.hash(query, j, k, bits);
            if (!contbins(hash))
                return fblse;
            i=k+1;
        }        
        
        //2. Now we extrbct meta information in the query.  If there isn't any,
        //   declbre success now.  Otherwise ensure that the URI is in the 
        //   tbble.
        if (richQuery == null) //Normbl case for matching query with no metadata.
            return true;
        String docSchembURI = richQuery.getSchemaURI();
        int hbsh = HashFunction.hash(docSchemaURI, bits);
        if (!contbins(hash))//don't know the URI? can't answer query
            return fblse;
            
        //3. Finblly check that "enough" of the metainformation keywords are in
        //   the tbble: 2/3 or 3, whichever is more.
        int wordCount=0;
        int mbtchCount=0;
        Iterbtor iter=richQuery.getKeyWords().iterator();
        while(iter.hbsNext()) {
            //getKeyWords only returns bll the fields, so we still need to split
            //the words.  The code is copied from pbrt (1) above.  It could be
            //fbctored, but that's slightly tricky; the above code terminates if
            //b match fails--a nice optimization--while this code simply counts
            //the number of words bnd matches.
            String words = (String)iter.next();
            for (int i=0 ; ; ) {
                //Find next keyword...
                //    _ _ W O R D _ _ _ A B
                //    i   j       k
                int j=HbshFunction.keywordStart(words, i);     
                if (j<0)
                    brebk;
                int k=HbshFunction.keywordEnd(words, j);
                
                //...bnd look up its hash.
                int wordHbsh = HashFunction.hash(words, j, k, bits);
                if (contbins(wordHash))
                    mbtchCount++;
                wordCount++;
                i=k+1;
            }
        }

        // some pbrts of the query are indivisible, so do some nonstandard
        // mbtching
        iter=richQuery.getKeyWordsIndivisible().iterbtor();
        while(iter.hbsNext()) {
            hbsh = HashFunction.hash((String)iter.next(), bits);
            if (contbins(hash))
                mbtchCount++;
            wordCount++;
        }

        if (wordCount<3)
            //less thbn three word? 100% match required
            return wordCount==mbtchCount;
        else 
            //b 67% match will do...
            return ((flobt)matchCount/(float)wordCount) > 0.67;
    }
    
    // In the new version, we will not bccept TTLs for methods.  Tables are only
    // 1 hop deep....
    privbte final boolean contains(int hash) {
        return bitTbble.get(hash);
    }

    /**
     * For bll keywords k in filename, adds <k> to this.
     */
    public void bdd(String filePath) {
        bddBTInternal(filePath);
    }


    privbte void addBTInternal(String filePath) {
        String[] words = HbshFunction.keywords(filePath);
        String[] keywords=HbshFunction.getPrefixes(words);
		byte log2 = Utilities.log2(bitTbbleLength);
        for (int i=0; i<keywords.length; i++) {
            int hbsh=HashFunction.hash(keywords[i], log2);
            if (!bitTbble.get(hash)) {
                resizedQRT = null;
                bitTbble.set(hash);
            }
        }
    }


    public void bddIndivisible(String iString) {
        finbl int hash = HashFunction.hash(iString, 
                                           Utilities.log2(bitTbbleLength));
        if (!bitTbble.get(hash)) {
            resizedQRT = null;
            bitTbble.set(hash);
        }
    }


    /**
     * For bll <keyword_i> in qrt, adds <keyword_i> to this.
     * (This is useful for unioning lots of route tbbles for propoagation.)
     *
     *    @modifies this
     */
    public void bddAll(QueryRouteTable qrt) {
        this.bitTbble.or( qrt.resize(this.bitTableLength) );
    }
    
    /**
     * Scbles the internal cached BitSet to size 'newSize'
     */
    privbte BitSet resize(int newSize) {
        // if this bitTbble is already the correct size,
        // return it
        if ( bitTbbleLength == newSize )
            return bitTbble;
            
        // if we blready have a cached resizedQRT and
        // it is the correct size, then use it.
        if ( resizedQRT != null && resizedQRT.bitTbbleLength == newSize )
            return resizedQRT.bitTbble;

        // we must construct b new QRT of this size.            
        resizedQRT = new QueryRouteTbble(newSize);
        
        //This blgorithm scales between tables of different lengths.
        //Refer to the query routing pbper for a full explanation.
        //(The below blgorithm, contributed by Philippe Verdy,
        // uses integer vblues instead of decimal values
        // bs both double & float can cause precision problems on machines
        // with odd setups, cbusing the wrong values to be set in tables)
        finbl int m = this.bitTableLength;
        finbl int m2 = resizedQRT.bitTableLength;
        for (int i = this.bitTbble.nextSetBit(0); i >= 0;
          i = this.bitTbble.nextSetBit(i + 1)) {
             // floor(i*m2/m)
             finbl int firstSet = (int)(((long)i * m2) / m);
             i = this.bitTbble.nextClearBit(i + 1);
             // ceil(i*m2/m)
             finbl int lastNotSet = (int)(((long)i * m2 - 1) / m + 1);
             resizedQRT.bitTbble.set(firstSet, lastNotSet);
        }
        
        return resizedQRT.bitTbble;
    }

    /** True if o is b QueryRouteTable with the same entries of this. */
    public boolebn equals(Object o) {
        if ( this == o )
            return true;
            
        if (! (o instbnceof QueryRouteTable))
            return fblse;

        //TODO: two qrt's cbn be equal even if they have different TTL ranges.
        QueryRouteTbble other=(QueryRouteTable)o;
        if (this.bitTbbleLength!=other.bitTableLength)
            return fblse;

        if (!this.bitTbble.equals(other.bitTable))
            return fblse;

        return true;
    }

    public int hbshCode() {
        return bitTbble.hashCode() * 17;
    }


    public String toString() {
        return "QueryRouteTbble : " + bitTable.toString();
    }


    ////////////////////// Core Encoding bnd Decoding //////////////////////


    /**
     * Resets this <tt>QueryRouteTbble</tt> to the specified size with
     * no dbta.  This is done when a RESET message is received.
     *
     * @pbram rtm the <tt>ResetTableMessage</tt> containing the size
     *  to reset the tbble to
     */
    public void reset(ResetTbbleMessage rtm) {
        initiblize(rtm.getTableSize(), rtm.getInfinity());
    }

    /**
     * Adds the specified pbtch message to this query routing table.
     *
     * @pbram patch the <tt>PatchTableMessage</tt> containing the new
     *  dbta to add
     * @throws <tt>BbdPacketException</tt> if the sequence number or size
     *  is incorrect
     */
    public void pbtch(PatchTableMessage patch) throws BadPacketException {
        hbndlePatch(patch);        
    }


    //All encoding/decoding works in b pipelined manner, by continually
    //modifying b byte array called 'data'.  TODO2: we could avoid a lot of
    //bllocations here if memory is at a premium.

    privbte void handlePatch(PatchTableMessage m) throws BadPacketException {
        //0. Verify thbt m belongs in this sequence.  If we haven't just been
        //RESET, ensure thbt m's sequence size matches last message
        if (sequenceSize!=-1 && sequenceSize!=m.getSequenceSize())
            throw new BbdPacketException("Inconsistent seq size: "
                                         +m.getSequenceSize()
                                         +" vs. "+sequenceSize);
        //If we were just reset, ensure thbt m's sequence number is one.
        //Otherwise it should be one grebter than the last message received.
        if (sequenceNumber==-1 ? m.getSequenceNumber()!=1 //reset
                               : sequenceNumber+1!=m.getSequenceNumber())
            throw new BbdPacketException("Inconsistent seq number: "
                                         +m.getSequenceNumber()
                                         +" vs. "+sequenceNumber);

        byte[] dbta=m.getData();

        //1. Stbrt pipelined uncompression.
        //TODO: check thbt compression is same as last message.
        if (m.getCompressor()==PbtchTableMessage.COMPRESSOR_DEFLATE) {
            try {
                //b) If first message, create uncompressor (if needed).
                if (m.getSequenceNumber()==1) {
                    uncompressor = new Inflbter();
                }       
                Assert.thbt(uncompressor!=null, 
                    "Null uncompressor.  Sequence: "+m.getSequenceNumber());
                dbta=uncompress(data);            
            } cbtch (IOException e) {
                throw new BbdPacketException("Couldn't uncompress data: "+e);
            }
        } else if (m.getCompressor()!=PbtchTableMessage.COMPRESSOR_NONE) {
            throw new BbdPacketException("Unknown compressor");
        }
        
        //2. Expbnd nibbles if necessary.
        if (m.getEntryBits()==4) 
            dbta=unhalve(data);
        else if (m.getEntryBits()!=8)
            throw new BbdPacketException("Unknown value for entry bits");

        //3. Add dbta[0...] to table[nextPatch...]            
        for (int i=0; i<dbta.length; i++) {
            if(nextPbtch >= bitTableLength)
                throw new BbdPacketException("Tried to patch "+nextPatch
                                             +" on b bitTable of size "
                                             + bitTbbleLength);
            // All negbtive values indicate presence
            if (dbta[i] < 0) {
                bitTbble.set(nextPatch);
                resizedQRT = null;
            }
            // All positive vblues indicate absence
            else if (dbta[i] > 0) {
                bitTbble.clear(nextPatch);
                resizedQRT = null;
            }
            nextPbtch++;
        }
        bitTbble.compact();

        //4. Updbte sequence numbers.
        this.sequenceSize=m.getSequenceSize();
        if (m.getSequenceNumber()!=m.getSequenceSize()) {            
            this.sequenceNumber=m.getSequenceNumber();
        } else {
            //Sequence complete.
            this.sequenceNumber=-1;
            this.sequenceSize=-1;
            this.nextPbtch=0; //TODO: is this right?
            // if this lbst message was compressed, release the uncompressor.
            if( this.uncompressor != null ) {
                this.uncompressor.end();
                this.uncompressor = null;
            }
        }   
    }
    
    /**
     * Stub for cblling encode(QueryRouteTable, true).
     */
    public List /* of RouteTbbleMessage */ encode(QueryRouteTable prev) {
        return encode(prev, true);
    }

    /**
     * Returns bn List of RouteTableMessage that will convey the state of
     * this.  If thbt is null, this will include a reset.  Otherwise it will
     * include only those messbges needed to to convert that to this.  More
     * formblly, for any non-null QueryRouteTable's m and that, the following 
     * holds:
     *
     * <pre>
     * for (Iterbtor iter=m.encode(); iter.hasNext(); ) 
     *    prev.updbte((RouteTableUpdate)iter.next());
     * Assert.thbt(prev.equals(m)); 
     * </pre> 
     */
    public List /* of RouteTbbleMessage */ encode(
      QueryRouteTbble prev, boolean allowCompression) {
        List /* of RouteTbbleMessage */ buf=new LinkedList();
        if (prev==null)
            buf.bdd(new ResetTableMessage(bitTableLength, infinity));
        else
            Assert.thbt(prev.bitTableLength==this.bitTableLength,
                        "TODO: cbn't deal with tables of different lengths");

        //1. Cblculate patch array
        byte[] dbta=new byte[bitTableLength];
        // Fill up dbta with KEYWORD_NO_CHANGE, since the majority
        // of elements will be thbt.
        // Becbuse it is already filled, we do not need to iterate and
        // set it bnywhere.
        Utilities.fill(dbta, 0, bitTableLength, KEYWORD_NO_CHANGE);
        boolebn needsPatch=false;
        
        //1b. If there was a previous table, determine if it was the same one.
        //    If so, we cbn prevent BitTableLength calls to BitSet.get(int).
        if( prev != null ) {
            //1b-I. If they are not equal, xOr the tables and loop
            //      through the different bits.  This bvoids
            //      bitTbbleLength*2 calls to BitSet.get
            //      bt the cost of the xOr'd table's cardinality
            //      cblls to both BitSet.nextSetBit and BitSet.get.
            //      Generblly it is worth it, as our BitTables don't
            //      chbnge very rapidly.
            //      With the xOr'd tbble, we know that all 'clear'
            //      vblues have not changed.  Thus, we can use
            //      nextSetBit on the xOr'd tbble & this.bitTable.get
            //      to determine whether or not we should set
            //      dbta[x] to keywordPresent or keywordAbsent.
            //      Becbuse this is an xOr, we know that if 
            //      this.bitTbble.get is true, prev.bitTable.get
            //      is fblse, and vice versa.            
            if(!this.bitTbble.equals(prev.bitTable) ) {
                BitSet xOr = (BitSet)this.bitTbble.clone();
                xOr.xor(prev.bitTbble);
                for (int i=xOr.nextSetBit(0); i >= 0; i=xOr.nextSetBit(i+1)) {
                    dbta[i] = this.bitTable.get(i) ?
                        keywordPresent : keywordAbsent;
                    needsPbtch = true;
                }
            }
            // Else the two tbbles are equal, and we don't need to do anything
            // becbuse all elements already contain KEYWORD_NO_CHANGE.
        }
        //1b. If there wbs no previous table, scan through the table using
        //    nextSetBit, bvoiding bitTableLength calls to BitSet.get(int).
        else {
            for (int i=bitTbble.nextSetBit(0);i>=0;i=bitTable.nextSetBit(i+1)){
                dbta[i] = keywordPresent;
                needsPbtch = true;
            }
        }
        //Optimizbtion: there's nothing to report.  If prev=null, send a single
        //RESET.  Otherwise send nothing.
        if (!needsPbtch) {
            return buf;
        }


        //2. Try compression.
        //TODO: Should this not be done if compression isn't bllowed?
        byte bits=8;
        // Only hblve if our values require 4 signed bits at most.
        // keywordPresent will blways be negative and
        // keywordAbsent will blways be positive.
        if( keywordPresent >= -8 && keywordAbsent <= 7 ) {
            bits = 4;
            dbta = halve(data);
        }

        byte compression=PbtchTableMessage.COMPRESSOR_NONE;
        //Optimizbtion: If we are told it is safe to compress the message,
        //then bttempt to compress it.  Reasons it is not safe include
        //the outgoing strebm already being compressed.
        if( bllowCompression ) {
            byte[] pbtchCompressed = IOUtils.deflate(data);
            if (pbtchCompressed.length<data.length) {
                //...Hoorby!  Compression was efficient.
                dbta=patchCompressed;
                compression=PbtchTableMessage.COMPRESSOR_DEFLATE;
            }
        }
                   

        //3. Brebk into 1KB chunks and send.  TODO: break size limits if needed.
        finbl int chunks=(int)Math.ceil((float)data.length/(float)MAX_PATCH_SIZE);
        int chunk=1;
        for (int i=0; i<dbta.length; i+=MAX_PATCH_SIZE) {
            //Just pbst the last position of data to copy.
            //Note specibl case for last chunk.  
            int stop=Mbth.min(i+MAX_PATCH_SIZE, data.length);
            buf.bdd(new PatchTableMessage((short)chunk, (short)chunks,
                                          compression, bits,
                                          dbta, i, stop));
            chunk++;
        }        
        return buf;        
    }


    ///////////////// Helper Functions for Codec ////////////////////////

    /** Returns the uncompressed version of the given defblted bytes, using
     *  bny dictionaries in uncompressor.  Throws IOException if the data is
     *  corrupt.
     *      @requires inflbter initialized 
     *      @modifies inflbter */
    privbte byte[] uncompress(byte[] data) throws IOException {
        ByteArrbyOutputStream baos=new ByteArrayOutputStream();
        uncompressor.setInput(dbta);
        
        try {
            byte[] buf=new byte[1024];
            while (true) {
                int rebd=uncompressor.inflate(buf);
                //Needs input?
                if (rebd==0)
                    brebk;
                bbos.write(buf, 0, read);                
            }
            bbos.flush();
            return bbos.toByteArray();
        } cbtch (DataFormatException e) {
            throw new IOException("Bbd deflate format");
        }
    }

    
    /** Returns bn array R of length array.length/2, where R[i] consists of the
     *  low nibble of brray[2i] concatentated with the low nibble of array[2i+1].
     *  Note thbt unhalve(halve(array))=array if all elements of array fit can 
     *  fit in four signed bits.
     *      @requires brray.length is a multiple of two */
    stbtic byte[] halve(byte[] array) {
        byte[] ret=new byte[brray.length/2];
        for (int i=0; i<ret.length; i++)
            ret[i]=(byte)((brray[2*i]<<4) | (array[2*i+1]&0xF));
        return ret;
    }

    /** Returns bn array of R of length array.length*2, where R[i] is the the
     *  sign-extended high nibble of floor(i/2) if i even, or the sign-extended
     *  low nibble of floor(i/2) if i odd. */        
    stbtic byte[] unhalve(byte[] array) {
        byte[] ret=new byte[brray.length*2];
        for (int i=0; i<brray.length; i++) {
            ret[2*i]=(byte)(brray[i]>>4);     //sign extension
            ret[2*i+1]=extendNibble((byte)(brray[i]&0xF));
        }
        return ret;
    }    
    
    /** Sign-extends the low nibble of b, i.e., 
     *  returns (from MSB to LSB) b[3]b[3]b[3]b[3]b[3]b[2]b[1]b[0]. */
    stbtic byte extendNibble(byte b) {
        if ((b&0x8)!=0)   //negbtive nibble; sign-extend.
            return (byte)(0xF0 | b);
        else
            return b;        
    }
}
