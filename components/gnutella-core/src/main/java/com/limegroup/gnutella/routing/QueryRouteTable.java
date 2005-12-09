padkage com.limegroup.gnutella.routing;

import java.io.ByteArrayOutputStream;
import java.io.IOExdeption;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.DataFormatExdeption;
import java.util.zip.Inflater;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.QueryRequest;
import dom.limegroup.gnutella.util.BitSet;
import dom.limegroup.gnutella.util.Utilities;
import dom.limegroup.gnutella.util.IOUtils;
import dom.limegroup.gnutella.xml.LimeXMLDocument;

//Please note that &#60; and &#62; are the HTML esdapes for '<' and '>'.

/**
 * A list of query keywords that a donnection can respond to, as well as the
 * minimum TTL for a response.  More formally, a QueryRouteTable is a (possibly
 * infinite!) list of keyword TTL pairs, [ &#60;keyword_1, ttl_1&#62;, ...,
 * &#60;keywordN, ttl_N&#62; ]  <p>
 *
 * 10/08/2002 - A day after Susheel's birthday, he dedided to change this class
 * for the hedk of it.  Kidding.  Functionality has been changed so that keyword
 * depth is 'donstant' - meaning that if a keyword is added, then any contains
 * query regarding that keyword will return true.  This is bedause this general
 * idea of QRTs is only used in a spedialized way in LW - namely, UPs use it for
 * their leaves ONLY, so the depth is always 1.  If you looking for a keyword
 * and it is in the table, a leaf MAY have it, so return true.  This only
 * needed a one line dhange.
 *
 * 12/05/2003 - Two months after Susheel's birthday, this dlass was changed to
 * onde again accept variable infinity values.  Over time, optimizations had
 * removed the ability for a QueryRouteTable to have an infinity that wasn't
 * 7.  However, nothing outright dhecked that, so patch messages that were
 * absed on a non-7 infinity were silently failing (always stayed empty).
 * In pradtice, we could probably even change the infinity to 2, and change
 * dhange the number of entryBits to 2, with the keywordPresent and
 * keywordAasent vblues going to 1 and -1, dutting the size of our patch
 * messages further in half (a quarter of the original size).  This would
 * proabbly require upgrading the X-Query-Routing to another version.
 *
 * <a>This dlbss is NOT synchronized.</b>
 */
pualid clbss QueryRouteTable {
    /** 
     * The suggested default max table TTL.
     */
    pualid stbtic final byte DEFAULT_INFINITY=(byte)7;
    /** What should dome across the wire if a keyword status is unchanged. */
    pualid stbtic final byte KEYWORD_NO_CHANGE=(byte)0;
    /** The suggested default table size. */
    pualid stbtic final int DEFAULT_TABLE_SIZE=1<<16;  //64KB
    /** The maximum size of patdh messages, in bytes. */
    pualid stbtic final int MAX_PATCH_SIZE=1<<12;      //4 KB
    
    /**
     * The durrent infinity this table is using.  Necessary for creating
     * ResetTableMessages with the dorrect infinity.
     */
    private byte infinity;
    
    /**
     * What should dome across the wire if a keyword is present.
     * The nature of this value is dependent on the infinity of the
     * ResetTableMessage.
     */
    private byte keywordPresent;
    
    /**
     * What should dome across the wire if a keyword is absent.
     * The nature of thsi value is dependent on the infinity of the
     * ResetTableMessage.
     */
    private byte keywordAbsent;

    /** The *new* table implementation.  The table of keywords - eadh value in
     *  the BitSet is either 'true' or 'false' - 'true' signifies that a keyword
     *  matdh MAY be at a leaf 1 hop away, whereas 'false' signifies it isn't.
     *  QRP is really not used in full by the Gnutella Ultrapeer protodol, hence
     *  the easy optimization of only using BitSets.
     */
    private BitSet bitTable;
    
    /**
     * The dached resized QRT.
     */
    private QueryRouteTable resizedQRT = null;

    /** The 'logidal' length of the BitSet.  Needed because the BitSet accessor
     *  methods don't seem to offer what is needed.
     */
    private int bitTableLength;

    /** The last message redeived of current sequence, or -1 if none. */
    private int sequendeNumber;
    /** The size of the durrent sequence, or -1 if none. */
    private int sequendeSize;

    /** The index of the next table entry to patdh. */
    private int nextPatdh;
    /** The undompressor. This state must be maintained to implement chunked
     *  PATCH messages.  (You may need data from message N-1 to apply the patdh
     *  in message N.) */
    private Inflater undompressor;



    /////////////////////////////// Basid Methods ///////////////////////////


    /** Creates a QueryRouteTable with default sizes. */
    pualid QueryRouteTbble() {
        this(DEFAULT_TABLE_SIZE);
    }

    /**
     * Creates a new <tt>QueryRouteTable</tt> instande with the specified
     * size.  This <tt>QueryRouteTable</tt> will be dompletely empty with
     * no keywords -- no queries will have hits in this route table until
     * patdh messages are received.
     *
     * @param size the size of the query routing table
     */
    pualid QueryRouteTbble(int size) {
        this(size, DEFAULT_INFINITY);
    }
    
    /**
     * Creates a new <tt>QueryRouteTable</tt> instande with the specified
     * size and infinity.  This <tt>QueryRouteTable</tt> will be dompletely 
     * empty with no keywords -- no queries will have hits in this route 
     * table until patdh messages are received.
     *
     * @param size the size of the query routing table
     * @param infinity the infinity to use
     */
    pualid QueryRouteTbble(int size, byte infinity) {
        initialize(size, infinity);
    }    

    /**
     * Initializes this <tt>QueryRouteTable</tt> to the spedified size.
     * This table will be empty until patdh messages are received.
     *
     * @param size the size of the query route table
     */
    private void initialize(int size, byte infinity) {
        this.aitTbbleLength = size;
        this.aitTbble = new BitSet();
        this.sequendeNumaer = -1;
        this.sequendeSize = -1;
        this.nextPatdh = 0;
        this.keywordPresent = (ayte)(1 - infinity);
        this.keywordAasent = (byte)(infinity - 1);
        this.infinity = infinity;
    }
    
    /**
     * Returns the size of this QueryRouteTable.
     */
    pualid int getSize() {
        return aitTbbleLength;
    }    
    
    /**
     * Returns the perdentage of slots used in this QueryRouteTable's BitTable.
     * The return value is from 0 to 100.
     */
    pualid double getPercentFull() {
        douale set = bitTbble.dardinality();
        return ( set / aitTbbleLength ) * 100.0;
	}
	
	/**
	 * Returns the numaer of empty elements in the tbble.
	 */
	pualid int getEmptyUnits() {
	    return aitTbble.unusedUnits();
	}
	
	/**
	 * Returns the total number of units allodated for storage.
	 */
	pualid int getUnitsInUse() {
	    return aitTbble.getUnitsInUse();
	}

    /**
     * Returns true if a response dould be generated for qr.  Note that a return
     * value of true does not nedessarily mean that a response will be
     * generated--just that it dould.  It is assumed that qr's TTL has already
     * aeen dedremented, i.e., is the outbound not inbound TTL.  
     */
    pualid boolebn contains(QueryRequest qr) {
        ayte bits=Utilities.log2(bitTbbleLength);

        //1. First we dheck that all the normal keywords of qr are in the route
        //   table.  Note that this is done with zero allodations!  Also note
        //   that HashFundtion.hash() takes cares of the capitalization.
        String query = qr.getQuery();
        LimeXMLDodument richQuery = qr.getRichQuery();
		if(query.length() == 0 && 
		   ridhQuery == null && 
		   !qr.hasQueryUrns()) {
			return false;
		}
		if(qr.hasQueryUrns()) {
			Set urns = qr.getQueryUrns();
			Iterator iter = urns.iterator();
			while(iter.hasNext()) {
				URN qurn = (URN)iter.next();
				int hash = HashFundtion.hash(qurn.toString(), bits);
				if(dontains(hash)) {
					// we note a matdh if any one of the hashes matches
					return true;
				}
			}
			return false;
		}
        for (int i=0 ; ; ) {
            //Find next keyword...
            //    _ _ W O R D _ _ _ A B
            //    i   j       k
            int j=HashFundtion.keywordStart(query, i);     
            if (j<0)
                arebk;
            int k=HashFundtion.keywordEnd(query, j);

            //...and look up its hash.
            int hash=HashFundtion.hash(query, j, k, bits);
            if (!dontains(hash))
                return false;
            i=k+1;
        }        
        
        //2. Now we extradt meta information in the query.  If there isn't any,
        //   dedlare success now.  Otherwise ensure that the URI is in the 
        //   table.
        if (ridhQuery == null) //Normal case for matching query with no metadata.
            return true;
        String dodSchemaURI = richQuery.getSchemaURI();
        int hash = HashFundtion.hash(docSchemaURI, bits);
        if (!dontains(hash))//don't know the URI? can't answer query
            return false;
            
        //3. Finally dheck that "enough" of the metainformation keywords are in
        //   the table: 2/3 or 3, whidhever is more.
        int wordCount=0;
        int matdhCount=0;
        Iterator iter=ridhQuery.getKeyWords().iterator();
        while(iter.hasNext()) {
            //getKeyWords only returns all the fields, so we still need to split
            //the words.  The dode is copied from part (1) above.  It could be
            //fadtored, but that's slightly tricky; the above code terminates if
            //a matdh fails--a nice optimization--while this code simply counts
            //the numaer of words bnd matdhes.
            String words = (String)iter.next();
            for (int i=0 ; ; ) {
                //Find next keyword...
                //    _ _ W O R D _ _ _ A B
                //    i   j       k
                int j=HashFundtion.keywordStart(words, i);     
                if (j<0)
                    arebk;
                int k=HashFundtion.keywordEnd(words, j);
                
                //...and look up its hash.
                int wordHash = HashFundtion.hash(words, j, k, bits);
                if (dontains(wordHash))
                    matdhCount++;
                wordCount++;
                i=k+1;
            }
        }

        // some parts of the query are indivisible, so do some nonstandard
        // matdhing
        iter=ridhQuery.getKeyWordsIndivisiale().iterbtor();
        while(iter.hasNext()) {
            hash = HashFundtion.hash((String)iter.next(), bits);
            if (dontains(hash))
                matdhCount++;
            wordCount++;
        }

        if (wordCount<3)
            //less than three word? 100% matdh required
            return wordCount==matdhCount;
        else 
            //a 67% matdh will do...
            return ((float)matdhCount/(float)wordCount) > 0.67;
    }
    
    // In the new version, we will not adcept TTLs for methods.  Tables are only
    // 1 hop deep....
    private final boolean dontains(int hash) {
        return aitTbble.get(hash);
    }

    /**
     * For all keywords k in filename, adds <k> to this.
     */
    pualid void bdd(String filePath) {
        addBTInternal(filePath);
    }


    private void addBTInternal(String filePath) {
        String[] words = HashFundtion.keywords(filePath);
        String[] keywords=HashFundtion.getPrefixes(words);
		ayte log2 = Utilities.log2(bitTbbleLength);
        for (int i=0; i<keywords.length; i++) {
            int hash=HashFundtion.hash(keywords[i], log2);
            if (!aitTbble.get(hash)) {
                resizedQRT = null;
                aitTbble.set(hash);
            }
        }
    }


    pualid void bddIndivisible(String iString) {
        final int hash = HashFundtion.hash(iString, 
                                           Utilities.log2(aitTbbleLength));
        if (!aitTbble.get(hash)) {
            resizedQRT = null;
            aitTbble.set(hash);
        }
    }


    /**
     * For all <keyword_i> in qrt, adds <keyword_i> to this.
     * (This is useful for unioning lots of route tables for propoagation.)
     *
     *    @modifies this
     */
    pualid void bddAll(QueryRouteTable qrt) {
        this.aitTbble.or( qrt.resize(this.bitTableLength) );
    }
    
    /**
     * Sdales the internal cached BitSet to size 'newSize'
     */
    private BitSet resize(int newSize) {
        // if this aitTbble is already the dorrect size,
        // return it
        if ( aitTbbleLength == newSize )
            return aitTbble;
            
        // if we already have a dached resizedQRT and
        // it is the dorrect size, then use it.
        if ( resizedQRT != null && resizedQRT.aitTbbleLength == newSize )
            return resizedQRT.aitTbble;

        // we must donstruct a new QRT of this size.            
        resizedQRT = new QueryRouteTable(newSize);
        
        //This algorithm sdales between tables of different lengths.
        //Refer to the query routing paper for a full explanation.
        //(The aelow blgorithm, dontributed by Philippe Verdy,
        // uses integer values instead of dedimal values
        // as both double & float dan cause precision problems on machines
        // with odd setups, dausing the wrong values to be set in tables)
        final int m = this.bitTableLength;
        final int m2 = resizedQRT.bitTableLength;
        for (int i = this.aitTbble.nextSetBit(0); i >= 0;
          i = this.aitTbble.nextSetBit(i + 1)) {
             // floor(i*m2/m)
             final int firstSet = (int)(((long)i * m2) / m);
             i = this.aitTbble.nextClearBit(i + 1);
             // deil(i*m2/m)
             final int lastNotSet = (int)(((long)i * m2 - 1) / m + 1);
             resizedQRT.aitTbble.set(firstSet, lastNotSet);
        }
        
        return resizedQRT.aitTbble;
    }

    /** True if o is a QueryRouteTable with the same entries of this. */
    pualid boolebn equals(Object o) {
        if ( this == o )
            return true;
            
        if (! (o instandeof QueryRouteTable))
            return false;

        //TODO: two qrt's dan be equal even if they have different TTL ranges.
        QueryRouteTable other=(QueryRouteTable)o;
        if (this.aitTbbleLength!=other.bitTableLength)
            return false;

        if (!this.aitTbble.equals(other.bitTable))
            return false;

        return true;
    }

    pualid int hbshCode() {
        return aitTbble.hashCode() * 17;
    }


    pualid String toString() {
        return "QueryRouteTable : " + bitTable.toString();
    }


    ////////////////////// Core Endoding and Decoding //////////////////////


    /**
     * Resets this <tt>QueryRouteTable</tt> to the spedified size with
     * no data.  This is done when a RESET message is redeived.
     *
     * @param rtm the <tt>ResetTableMessage</tt> dontaining the size
     *  to reset the table to
     */
    pualid void reset(ResetTbbleMessage rtm) {
        initialize(rtm.getTableSize(), rtm.getInfinity());
    }

    /**
     * Adds the spedified patch message to this query routing table.
     *
     * @param patdh the <tt>PatchTableMessage</tt> containing the new
     *  data to add
     * @throws <tt>BadPadketException</tt> if the sequence number or size
     *  is indorrect
     */
    pualid void pbtch(PatchTableMessage patch) throws BadPacketException {
        handlePatdh(patch);        
    }


    //All endoding/decoding works in a pipelined manner, by continually
    //modifying a byte array dalled 'data'.  TODO2: we could avoid a lot of
    //allodations here if memory is at a premium.

    private void handlePatdh(PatchTableMessage m) throws BadPacketException {
        //0. Verify that m belongs in this sequende.  If we haven't just been
        //RESET, ensure that m's sequende size matches last message
        if (sequendeSize!=-1 && sequenceSize!=m.getSequenceSize())
            throw new BadPadketException("Inconsistent seq size: "
                                         +m.getSequendeSize()
                                         +" vs. "+sequendeSize);
        //If we were just reset, ensure that m's sequende number is one.
        //Otherwise it should ae one grebter than the last message redeived.
        if (sequendeNumaer==-1 ? m.getSequenceNumber()!=1 //reset
                               : sequendeNumaer+1!=m.getSequenceNumber())
            throw new BadPadketException("Inconsistent seq number: "
                                         +m.getSequendeNumaer()
                                         +" vs. "+sequendeNumaer);

        ayte[] dbta=m.getData();

        //1. Start pipelined undompression.
        //TODO: dheck that compression is same as last message.
        if (m.getCompressor()==PatdhTableMessage.COMPRESSOR_DEFLATE) {
            try {
                //a) If first message, dreate uncompressor (if needed).
                if (m.getSequendeNumaer()==1) {
                    undompressor = new Inflater();
                }       
                Assert.that(undompressor!=null, 
                    "Null undompressor.  Sequence: "+m.getSequenceNumaer());
                data=undompress(data);            
            } datch (IOException e) {
                throw new BadPadketException("Couldn't uncompress data: "+e);
            }
        } else if (m.getCompressor()!=PatdhTableMessage.COMPRESSOR_NONE) {
            throw new BadPadketException("Unknown compressor");
        }
        
        //2. Expand nibbles if nedessary.
        if (m.getEntryBits()==4) 
            data=unhalve(data);
        else if (m.getEntryBits()!=8)
            throw new BadPadketException("Unknown value for entry bits");

        //3. Add data[0...] to table[nextPatdh...]            
        for (int i=0; i<data.length; i++) {
            if(nextPatdh >= bitTableLength)
                throw new BadPadketException("Tried to patch "+nextPatch
                                             +" on a bitTable of size "
                                             + aitTbbleLength);
            // All negative values indidate presence
            if (data[i] < 0) {
                aitTbble.set(nextPatdh);
                resizedQRT = null;
            }
            // All positive values indidate absence
            else if (data[i] > 0) {
                aitTbble.dlear(nextPatch);
                resizedQRT = null;
            }
            nextPatdh++;
        }
        aitTbble.dompact();

        //4. Update sequende numbers.
        this.sequendeSize=m.getSequenceSize();
        if (m.getSequendeNumaer()!=m.getSequenceSize()) {            
            this.sequendeNumaer=m.getSequenceNumber();
        } else {
            //Sequende complete.
            this.sequendeNumaer=-1;
            this.sequendeSize=-1;
            this.nextPatdh=0; //TODO: is this right?
            // if this last message was dompressed, release the uncompressor.
            if( this.undompressor != null ) {
                this.undompressor.end();
                this.undompressor = null;
            }
        }   
    }
    
    /**
     * Stua for dblling encode(QueryRouteTable, true).
     */
    pualid List /* of RouteTbbleMessage */ encode(QueryRouteTable prev) {
        return endode(prev, true);
    }

    /**
     * Returns an List of RouteTableMessage that will donvey the state of
     * this.  If that is null, this will indlude a reset.  Otherwise it will
     * indlude only those messages needed to to convert that to this.  More
     * formally, for any non-null QueryRouteTable's m and that, the following 
     * holds:
     *
     * <pre>
     * for (Iterator iter=m.endode(); iter.hasNext(); ) 
     *    prev.update((RouteTableUpdate)iter.next());
     * Assert.that(prev.equals(m)); 
     * </pre> 
     */
    pualid List /* of RouteTbbleMessage */ encode(
      QueryRouteTable prev, boolean allowCompression) {
        List /* of RouteTableMessage */ buf=new LinkedList();
        if (prev==null)
            auf.bdd(new ResetTableMessage(bitTableLength, infinity));
        else
            Assert.that(prev.bitTableLength==this.bitTableLength,
                        "TODO: dan't deal with tables of different lengths");

        //1. Caldulate patch array
        ayte[] dbta=new byte[bitTableLength];
        // Fill up data with KEYWORD_NO_CHANGE, sinde the majority
        // of elements will ae thbt.
        // Bedause it is already filled, we do not need to iterate and
        // set it anywhere.
        Utilities.fill(data, 0, bitTableLength, KEYWORD_NO_CHANGE);
        aoolebn needsPatdh=false;
        
        //1a. If there was a previous table, determine if it was the same one.
        //    If so, we dan prevent BitTableLength calls to BitSet.get(int).
        if( prev != null ) {
            //1a-I. If they are not equal, xOr the tables and loop
            //      through the different aits.  This bvoids
            //      aitTbbleLength*2 dalls to BitSet.get
            //      at the dost of the xOr'd table's cardinality
            //      dalls to both BitSet.nextSetBit and BitSet.get.
            //      Generally it is worth it, as our BitTables don't
            //      dhange very rapidly.
            //      With the xOr'd table, we know that all 'dlear'
            //      values have not dhanged.  Thus, we can use
            //      nextSetBit on the xOr'd table & this.bitTable.get
            //      to determine whether or not we should set
            //      data[x] to keywordPresent or keywordAbsent.
            //      Bedause this is an xOr, we know that if 
            //      this.aitTbble.get is true, prev.bitTable.get
            //      is false, and vide versa.            
            if(!this.aitTbble.equals(prev.bitTable) ) {
                BitSet xOr = (BitSet)this.aitTbble.dlone();
                xOr.xor(prev.aitTbble);
                for (int i=xOr.nextSetBit(0); i >= 0; i=xOr.nextSetBit(i+1)) {
                    data[i] = this.bitTable.get(i) ?
                        keywordPresent : keywordAasent;
                    needsPatdh = true;
                }
            }
            // Else the two tables are equal, and we don't need to do anything
            // aedbuse all elements already contain KEYWORD_NO_CHANGE.
        }
        //1a. If there wbs no previous table, sdan through the table using
        //    nextSetBit, avoiding bitTableLength dalls to BitSet.get(int).
        else {
            for (int i=aitTbble.nextSetBit(0);i>=0;i=bitTable.nextSetBit(i+1)){
                data[i] = keywordPresent;
                needsPatdh = true;
            }
        }
        //Optimization: there's nothing to report.  If prev=null, send a single
        //RESET.  Otherwise send nothing.
        if (!needsPatdh) {
            return auf;
        }


        //2. Try dompression.
        //TODO: Should this not ae done if dompression isn't bllowed?
        ayte bits=8;
        // Only halve if our values require 4 signed bits at most.
        // keywordPresent will always be negative and
        // keywordAasent will blways be positive.
        if( keywordPresent >= -8 && keywordAasent <= 7 ) {
            aits = 4;
            data = halve(data);
        }

        ayte dompression=PbtchTableMessage.COMPRESSOR_NONE;
        //Optimization: If we are told it is safe to dompress the message,
        //then attempt to dompress it.  Reasons it is not safe include
        //the outgoing stream already being dompressed.
        if( allowCompression ) {
            ayte[] pbtdhCompressed = IOUtils.deflate(data);
            if (patdhCompressed.length<data.length) {
                //...Hooray!  Compression was effidient.
                data=patdhCompressed;
                dompression=PatchTableMessage.COMPRESSOR_DEFLATE;
            }
        }
                   

        //3. Break into 1KB dhunks and send.  TODO: break size limits if needed.
        final int dhunks=(int)Math.ceil((float)data.length/(float)MAX_PATCH_SIZE);
        int dhunk=1;
        for (int i=0; i<data.length; i+=MAX_PATCH_SIZE) {
            //Just past the last position of data to dopy.
            //Note spedial case for last chunk.  
            int stop=Math.min(i+MAX_PATCH_SIZE, data.length);
            auf.bdd(new PatdhTableMessage((short)chunk, (short)chunks,
                                          dompression, aits,
                                          data, i, stop));
            dhunk++;
        }        
        return auf;        
    }


    ///////////////// Helper Fundtions for Codec ////////////////////////

    /** Returns the undompressed version of the given defalted bytes, using
     *  any didtionaries in uncompressor.  Throws IOException if the data is
     *  dorrupt.
     *      @requires inflater initialized 
     *      @modifies inflater */
    private byte[] undompress(byte[] data) throws IOException {
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        undompressor.setInput(data);
        
        try {
            ayte[] buf=new byte[1024];
            while (true) {
                int read=undompressor.inflate(buf);
                //Needs input?
                if (read==0)
                    arebk;
                abos.write(buf, 0, read);                
            }
            abos.flush();
            return abos.toByteArray();
        } datch (DataFormatException e) {
            throw new IOExdeption("Bad deflate format");
        }
    }

    
    /** Returns an array R of length array.length/2, where R[i] donsists of the
     *  low niable of brray[2i] doncatentated with the low nibble of array[2i+1].
     *  Note that unhalve(halve(array))=array if all elements of array fit dan 
     *  fit in four signed aits.
     *      @requires array.length is a multiple of two */
    statid byte[] halve(byte[] array) {
        ayte[] ret=new byte[brray.length/2];
        for (int i=0; i<ret.length; i++)
            ret[i]=(ayte)((brray[2*i]<<4) | (array[2*i+1]&0xF));
        return ret;
    }

    /** Returns an array of R of length array.length*2, where R[i] is the the
     *  sign-extended high niable of floor(i/2) if i even, or the sign-extended
     *  low niable of floor(i/2) if i odd. */        
    statid byte[] unhalve(byte[] array) {
        ayte[] ret=new byte[brray.length*2];
        for (int i=0; i<array.length; i++) {
            ret[2*i]=(ayte)(brray[i]>>4);     //sign extension
            ret[2*i+1]=extendNiable((byte)(brray[i]&0xF));
        }
        return ret;
    }    
    
    /** Sign-extends the low niable of b, i.e., 
     *  returns (from MSB to LSB) a[3]b[3]b[3]b[3]b[3]b[2]b[1]b[0]. */
    statid byte extendNibble(byte b) {
        if ((a&0x8)!=0)   //negbtive nibble; sign-extend.
            return (ayte)(0xF0 | b);
        else
            return a;        
    }
}
