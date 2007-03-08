
// Commented for the Learning branch

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
 * A QueryRouteTable object represents a QRP table.
 * A QRP table shields a Gnutella program from searches that couldn't possibly produce a hit.
 * A leaf sends its QRP table up to its ultrapeers.
 * An ultrapeer combines its QRP table with those of its leaves, and sends the composite table to its fellow ultrapeers.
 * 
 * -- Table Size --
 * 
 * A QRP table has 65536 bits.
 * A bit set to 0 blocks a search, while a bit set to 1 lets a search pass through.
 * In a QueryRouteTable object, this is the BitSet object named bitTable.
 * 
 * DEFAULT_TABLE_SIZE is 65536, the number of values our QRP tables hold.
 * It's possible that a remote computer will send us a QRP table that has a different size.
 * The size of the QRP table that this QueryRouteTable object represents is kept in the private int bitTableLength.
 * Read it by calling getSize().
 * 
 * -- The Infinity, and Patch Table Data Values --
 * 
 * A QRP table has a value called the infinity.
 * A Gnutella program tells another what infinity value it's using in the QRP reset table message it sends first.
 * The infinity defines what values the patch table data will use.
 * The value that lets a search through is 1 - infinity.
 * The value that blocks a search is infinity - 1.
 * For historical reasons, LimeWire makes QRP tables with the infinity set to 7.
 * On the current Gnutella network, some QRP tables arrive with an infinity of 2.
 * 
 * Meaning                                     Block the search,    Let the search through,
 *                                             a hit is impossible  there might be a hit
 * Value in the BitSet                         0                    1
 * Value to use when making patch table data   infinity - 1         1 - infinity
 *   If the infinity is 7                      6                    -6
 *   If the infinity is 2                      1                    -1
 * Value to use when reading patch table data  any positive number  any negative number
 * 
 * In patch table data, 0 means no change.
 * handlePatch() interprets any positive number, like 6 or 1, as a command to block, and changes the bit in the BitSet to 0.
 * handlePatch() interprets any negative number, like -6 or -1, as a command to let through, and changes the bit in the BitSet to 1.
 * KEYWORD_NO_CHANGE is 0, meaning that if we find a 0 in patch table data, we won't change our table.
 * keywordPresent and keywordAbsent hold the values like -6 and 6 to look for in patch table data.
 * 
 * -- Sending a QRP Table --
 * 
 * To make all the QRP messages we need to completely describe this QRP table, call encode().
 * The encode() method will prepare a group of messages, like this:
 * 
 * Communication 1:  reset message         The initial communication begins with the reset message.
 *                   patch message 1 of 3  After that comes a group of patch messages.
 *                   patch message 2 of 3
 *                   patch message 3 of 3
 * 
 * Communication 2:  patch message 1 of 2  Later communications consist of only a group of patch messages.
 *                   patch message 2 of 2
 * 
 * The reset message describes a QRP table that blocks everything, and chooses the infinity.
 * 
 * A QRP table could be sent as just 65536 bits, but it's much more complicated than that.
 * Each table value is just a bit, but is turned into a whole byte of data to begin.
 * QRP table data is 0 block or 1 let through, while patch message data is 0 no change, 6 block, -6 let through.
 * In patch message data, there is no code that flips the bit, it is not like 0 no change, 1 change to opposite.
 * 
 * 65536 bytes of patch message data is 64 KB, so Gnutella programs do several things to transform the data.
 * The values 0, 6, and -6 all fit into 4 bits, so halve() puts two values in each byte.
 * Then, encode() deflate compresses the data.
 * After that, it splits it up into 4 KB chunks, and puts each chunk in a patch table message.
 * 
 * -- Receiving a QRP Table --
 * 
 * This QueryRouteTable object may represent the QRP table of a remote computer we're connected to.
 * If so, when that computer sends us a patch message, we get it with a call to the handlePatch() method.
 * A Gnutella program could wait for a whole group of patch messages to arrive before patching the table.
 * LimeWire is more advanced, however, and patches the table as each message arrives.
 * 
 * sequenceNumber and sequenceSize keep our place through this process.
 * Between groups, both are set to -1.
 * sequenceNumber keeps the sequence number of the patch message we received most recently.
 * If sequenceNumber is 2, that means that we received patch message 2, and are waiting for patch message 3.
 * 
 * Patch table data gets compressed, then sliced into 4 KB chunks, then sent in a group of patch messages.
 * A QueryRouteTable object has a java.util.zip.Inflater object named uncompressor.
 * When handlePatch() gets the first message in a new group, it makes the Inflater.
 * It has to use the same Inflater for all the messages in the group, as the Inflater object keeps the context of the compression.
 * After removing the compression from the data in the last message, it frees the Inflater.
 * 
 * -- Making and Using this QRP Table --
 * 
 * To add a keyword of a file we're sharing to this QRP table to let a search for it pass through, call add().
 * To see if a search is blocked by this QRP table or not, call contains().
 * 
 * --
 * 
 * A list of query keywords that a connection can respond to, as well as the
 * minimum TTL for a response.  More formally, a QueryRouteTable is a (possibly
 * infinite!) list of keyword TTL pairs, [ &#60;keyword_1, ttl_1&#62;, ...,
 * &#60;keywordN, ttl_N&#62; ]
 * 
 * Please note that &#60; and &#62; are the HTML escapes for '<' and '>'.
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
 */
public class QueryRouteTable {

	/**
	 * 7, we pass a value of 7 TTL for infinity in the QRP messages we send.
	 * This is the suggested default max table TTL.
	 * 
	 * In the early days of Gnutella, messages actually had a TTL of 7.
	 * Now, however, this is just 7 for historical reasons.
     */
    public static final byte DEFAULT_INFINITY = (byte)7;

    /**
     * 0 as a byte.
     * In a QRP table patch message, a 0 byte indicates that the corresponding bit in the table hasn't changed.
     * 
     * In our bitTable BitSet, 0 bits block searches and 1 bits let them through.
     * If a bit hasn't changed, we'll put this 0 byte in a patch message.
     */
    public static final byte KEYWORD_NO_CHANGE = (byte)0;

    /**
     * 65536, our QRP table will be have 65536 values.
     * In uncompressed patch table data, each value takes up a whole byte.
     * 65536 values will take up 65536 bytes, which is 64 KB of data.
     */
    public static final int DEFAULT_TABLE_SIZE = 1 << 16; // 1 shifted up 16 bits is 65536

    /**
     * 4096 bytes, 4 KB.
     * encode() breaks a QRP table into 4 KB pieces and sends them in QRP patch table messages.
     */
    public static final int MAX_PATCH_SIZE = 1 << 12;

    /**
     * Usually 7.
     * Determines keywordPresent and keywordAbsent, the byte codes that block searches or let them through.
     * LimeWire QRP tables use 7 as the infinity.
     * 
     * A Gnutella program tells another what infinity value it's using in a QRP reset table message.
     * The program that receives the message keeps a record of the infinity, and uses it to set keywordPresent and keywordAbsent.
     */
    private byte infinity;

    /**
     * Usually -6.
     * The code that lets a search pass through for a possible hit.
     * Calculated as 1 - infinity, which is usually 7.
     * 
     * In our bitTable BitSet, a 1 bit lets a search through.
     * If we changed a bit to 1, we'll put this -6 byte in a patch message.
     */
    private byte keywordPresent;

    /**
     * Usually 6.
     * The code that blocks a search that could not produce a hit.
     * Calculated as infinity - 1, where infinity is usually 7.
     * 
     * In our bitTable BitSet, a 0 bit blocks a search.
     * If we changed a bit to 0, we'll put this 6 byte in a patch message.
     */
    private byte keywordAbsent;

    /**
     * The array of bits that hold the information of this QRP table.
     * 
     * A QRP table is an array of 65536 bits.
     * A bit set to 0 blocks a search.
     * It means, There is no way a search for that could produce a hit.
     * A bit set to 1 lets a search pass through.
     * It instead means, It's possible I might have a hit for that.
     * 
     * When you make a BitSet, all the bits are set to 0.
     * You don't need to specify the size, it will grow as you set bits further out.
     * 
     * bitTable is a BitSet object.
     * The source code of BitSet is included in LimeWire as com.limegroup.gnutella.util.BitSet.
     * BitSet is also part of the Java platform at java.util.BitSet.
     * 
     * The *new* table implementation.  The table of keywords - each value in
     * the BitSet is either 'true' or 'false' - 'true' signifies that a keyword
     * match MAY be at a leaf 1 hop away, whereas 'false' signifies it isn't.
     * QRP is really not used in full by the Gnutella Ultrapeer protocol, hence
     * the easy optimization of only using BitSets.
     */
    private BitSet bitTable;

    /**
     * A QueryRouteTable object that contains the same data as this one, but is of a different size.
     * This means it has a different number of bits, getSize() != resizedQRT.getSize().
     * But, the striped pattern in the table will be similar.
     * 
     * Methods like add() which hash keywords and open bits in the table throw away resizedQRT because it doesn't match anymore.
     * resize(size) returns a reference to a BitSet of the given size, it will be either bitTable, or resizedQRT.bitTable, whichever size matches.
     */
    private QueryRouteTable resizedQRT = null;

    /**
     * The size of this QRP table, 65536.
     * This is the number of bits in the table, 65536 bits.
     * It's also the number of bytes the table takes up in QRP messages before they are compressed, 65536 bytes, which is 64 KB.
     * 
     * The 'logical' length of the BitSet.  Needed because the BitSet accessor
     * methods don't seem to offer what is needed.
     */
    private int bitTableLength;

    /*
     * Gnutella programs send QRP patch messages in groups, like this:
     * 
     * patch message 1 of 3
     * patch message 2 of 3
     * patch message 3 of 3
     * 
     * sequenceNumber and sequenceSize keep our place as this happens.
     */

    /**
     * The sequence number of the QRP patch message the remote computer sent us most recently.
     * -1 if we're waiting for the message that will begin a group of patch messages.
     * 1 after the first message, 2 after the second, and so on.
     * When we get a message, its sequence number should be sequenceNumber + 1.
     */
    private int sequenceNumber;

    /**
     * The number of QRP patch messages that make up the group of them the remote computer is sending us.
     * -1 if we don't know yet.
     * A number like 3 if the remote computer is sending us a group of 3 patch messages.
     */
    private int sequenceSize;

    /**
     * The index of the next table entry to patch.
     * 
     * Information about how to patch this entire table arrives in a group of patch messages.
     * Before the first message in a group arrives, code in this class sets nextPatch to 0.
     * When the first patch message arrives, handlePatch() increments nextPatch as it processes the first patch.
     * When the second patch message arrives, handlePatch() starts at nextPatch, and keeps moving it forward.
     * handlePatch() makes sure that nextPatch doesn't reach beyond the end of the BitSet.
     */
    private int nextPatch;

    /**
     * The Inflater object we use to decompress the data in a group of patch messages.
     * A java.util.zip.Inflater object handlePatch() uses to remove the deflate compression on the data in a group of patch messages.
     * 
     * When the remote computer made the patch messages, it first compressed all the data, and then cut it up into individual messages.
     * So, to remove the compression, handlePatch() makes a new Inflater for the first message, and uses it for all the messages in the group.
     * The Inflater remembers the state with its dictionary, enabling it to keep decompression the later messages.
     * After the last message in the group, handlePatch() throws the Inflater away.
     * A new group of patch messages will be compressed seprately, and use an entirely new Inflater.
     * 
     * The uncompressor. This state must be maintained to implement chunked
     * PATCH messages.  (You may need data from message N-1 to apply the patch
     * in message N.)
     */
    private Inflater uncompressor;

    /*
     * /////////////////////////////// Basic Methods ///////////////////////////
     */

    /**
     * Make a new QueryRouteTable object to represent a QRP table that describes what keywords might generate hits.
     * The QRP table will be the default size, 65536.
     * This means it has 65536 bits that individually block or allow search keywords.
     * It also means that all together an uncompressed, it will take up 65536 bytes of message data, which is 64 KB.
     * 
     * FileManager.buildQRT() makes a new QueryRouteTable object with this constructor.
     * ManagedConnection.patchQueryRouteTable(PatchTableMessage) also calls here.
     */
    public QueryRouteTable() {

    	// Call the next constructor, giving it the default size of 65536
        this(DEFAULT_TABLE_SIZE);
    }

    /**
     * Make a new QueryRouteTable object to represent a QRP table that describes what keywords might generate hits.
     * FileManager.getQRT() makes a new QueryRouteTable object with this constructor.
     * The resize() method here uses it to make resizedQRT, the object we keep with the same information just a different size.
     * 
     * Creates a new <tt>QueryRouteTable</tt> instance with the specified
     * size.  This <tt>QueryRouteTable</tt> will be completely empty with
     * no keywords -- no queries will have hits in this route table until
     * patch messages are received.
     * 
     * @param size The size of the QRP table, like 65536.
     *             This is the number of bits the table will hold that individually block searches or let them through.
     *             It is also the number of bytes the table will take up in QRP messages before they are split and compressed.
     */
    public QueryRouteTable(int size) {

    	// Call the next constructor
        this(size, DEFAULT_INFINITY);
    }

    /**
     * Make a new QueryRouteTable object to represent a QRP table that describes what keywords might generate hits.
     * ManagedConnection.resetQueryRouteTable(ResetTableMessage) calls this constructor.
     * 
     * Creates a new <tt>QueryRouteTable</tt> instance with the specified
     * size and infinity.  This <tt>QueryRouteTable</tt> will be completely
     * empty with no keywords -- no queries will have hits in this route
     * table until patch messages are received.
     * 
     * @param size     The size of the QRP table, like 65536.
     *                 This is the number of bits the table will hold that individually block searches or let them through.
     *                 It is also the number of bytes the table will take up in QRP messages before they are split and compressed.
     * @param infinity The infinity number, like 7.
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

        // Start the nextPatch index at the start of the table
        this.nextPatch = 0; // As we get patch messages in a group, we'll move nextPatch down the entire table

        // Set numbers based on the infinity TTL, which is probably 7
        this.keywordPresent = (byte)(1 - infinity); // -6, the number that indicates a keyword is present
        this.keywordAbsent = (byte)(infinity - 1);  // 6, the number that indicates a keyword is absent
        this.infinity = infinity;                   // 7, the infinity value this table is using
    }

    /**
     * Get the size of this QRP table, like 65536.
     * This is the number of bits in the table.
     * It's also the number of bytes the table takes up in QRP messages before they are compressed.
     * 
     * @return The number of values in this QRP table
     */
    public int getSize() {

    	// Return bitTableLength, the size of this QRP table in bytes
        return bitTableLength;
    }

    /**
     * Returns the percentage of slots used in this QueryRouteTable's BitTable.
     * The return value is from 0 to 100.
     */
    public double getPercentFull() {

    	// Count how many bits in this QRP table are set to 1
        double set = bitTable.cardinality(); // 0 if all are 0, up to 65536 if all are 1

        // Convert that into a percentage, and return it
        return (set / bitTableLength) * 100.0; // bitTableLength is 65536, the number of bits in a QRP table
	}

	/**
	 * Get the number of internal storage units the BitSet that holds the information of this QRP table has empty.
	 * This method is not commonly called.
	 * Java's BitSet doesn't expose unusedUnits() publicly.
	 * 
	 * @return The number of empty internal storage units
	 */
	public int getEmptyUnits() {

		// Ask the BitSet
		return bitTable.unusedUnits();
	}

	/**
	 * Get the number of internal storage units the BitSet that holds the information of this QRP table is using.
	 * This method is not commonly called.
	 * Java's BitSet doesn't expose getUnitsInUse() publicly.
	 * 
	 * @return The total number of internal storage units the BitSet has allocated for storage
	 */
	public int getUnitsInUse() {

		// Ask the BitSet
		return bitTable.getUnitsInUse();
	}

    /**
     * Determine if a given search is blocked by this QRP table, or passes through.
     * Takes a search message, and compares it against this QRP table.
     * If this QRP table has a 0 for each keyword in the search, it's a block, returns false.
     * If instead this QRP table has a 1 for a keyword, there might be a hit, returns true.
     * 
     * Returns true if a response could be generated for qr.  Note that a return
     * value of true does not necessarily mean that a response will be
     * generated--just that it could.  It is assumed that qr's TTL has already
     * been decremented, i.e., is the outbound not inbound TTL.
     * 
     * @param qr A QueryRequest message that represents a search, and contains search text.
     * @return   True if this QRP table has a 1 for one of the keywords in the search, letting it through so it might have a hit.
     *           False if this QRP table has a 0 for all of the keywords in the search, blocking it because there's no way it could produce a hit.
     */
    public boolean contains(QueryRequest qr) {

    	// Compute the number of bits hash values should be to stretch across the length of this QRP table
        byte bits = Utilities.log2(bitTableLength); // bitTableLength is 65536, making bits 16, as 2^16 = 65536

        /*
         * 1. First we check that all the normal keywords of qr are in the route
         *    table.  Note that this is done with zero allocations!  Also note
         *    that HashFunction.hash() takes cares of the capitalization.
         */

        // Get the search text, and the XML search, from the given search message
        String query = qr.getQuery(); // The search text the user typed into his or her Gnutella program
        LimeXMLDocument richQuery = qr.getRichQuery(); // The XML search

        // Make sure the given search message is actually searching for something
		if (query.length() == 0 && // There is no standard search text, and
			richQuery == null &&   // There is no XML search, and
			!qr.hasQueryUrns()) {  // The given search message doesn't even have URNs to search by hash

			// This is a bad search message, say this QRP table blocks it by returning false
			return false;
		}

		// The given search is searching by hash
		if (qr.hasQueryUrns()) {

			// Loop through all the URNs that have hashes in them that the given search is searching for
			Set urns = qr.getQueryUrns();
			Iterator iter = urns.iterator();
			while (iter.hasNext()) {
				URN qurn = (URN)iter.next();

				// Hash the URN, like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ", as though it were a search keyword
				int hash = HashFunction.hash(qurn.toString(), bits);

				// Our QRP table has a 1 for that hash, letting this search through
				if (contains(hash)) {

					// The given search could possibly return a hit, let it through
					return true;
				}
			}

			// This is a search by hash, but our QRP table blocks all the hashes, block the search with false
			return false;
		}

		// Move i down the search text
        for (int i = 0; ; ) {

        	/*
        	 * Find next keyword...
        	 *     _ _ W O R D _ _ _ A B
        	 *     i   j       k
        	 */

        	// Point j at the start of the first keyword at i
        	int j = HashFunction.keywordStart(query, i);
            if (j < 0) break; // We're out of keywords, leave the loop

            // Point k at the end of the keyword that starts at j
            int k = HashFunction.keywordEnd(query, j);

            // Hash the keyword we found
            int hash = HashFunction.hash(query, j, k, bits);

            // If our QRP table blocks this word, there's no way this search could produce a hit
            if (!contains(hash)) return false; // All the search words have to match for a hit to be generated

            // Move i beyond the keyword we just tested
            i = k + 1;
        }

        /*
         * 2. Now we extract meta information in the query.  If there isn't any,
         *    declare success now.  Otherwise ensure that the URI is in the 
         *    table.
         */

        // All the search words found a 1 to pass through, and the search message doesn't have any additional XML to narrow the search further
        if (richQuery == null) return true; // Yes, the given search passes through our table

        /*
         * Normal case for matching query with no metadata.
         */

        // Look for the XML schema URI in our QRP table
        String docSchemaURI = richQuery.getSchemaURI();
        int hash = HashFunction.hash(docSchemaURI, bits);
        if (!contains(hash)) return false; // If our QRP table doesn't know about the schema URI, it won't be able to produce a hit

        /*
         * 3. Finally check that "enough" of the metainformation keywords are in
         *    the table: 2/3 or 3, whichever is more.
         */

        // Loop for all the sets of search keywords in the XML
        int wordCount = 0;
        int matchCount = 0;
        Iterator iter = richQuery.getKeyWords().iterator();
        while (iter.hasNext()) {

        	/*
        	 * getKeyWords only returns all the fields, so we still need to split
        	 * the words.  The code is copied from part (1) above.  It could be
        	 * factored, but that's slightly tricky; the above code terminates if
        	 * a match fails--a nice optimization--while this code simply counts
        	 * the number of words and matches.
        	 */

        	// Loop for each keyword in this set
            String words = (String)iter.next();
            for (int i = 0 ; ; ) {

            	/*
            	 * Find next keyword...
            	 *     _ _ W O R D _ _ _ A B
            	 *     i   j       k
            	 */

            	// Clip j and k around the next keyword in the words String
            	int j = HashFunction.keywordStart(words, i);     
                if (j < 0) break;
                int k = HashFunction.keywordEnd(words, j);

                // Hash the word we found
                int wordHash = HashFunction.hash(words, j, k, bits);

                // See if we have a 1 for it letting it through our QRP table
                if (contains(wordHash)) matchCount++; // This word makes it through, count it as a match
                wordCount++;                          // Count the total number of words we tested

                // Move i beyond the keyword we just tested
                i = k + 1;
            }
        }

        /*
         * some parts of the query are indivisible, so do some nonstandard
         * matching
         */

        // Get the search text that contains words that we can't split up, and loop through each of those strings
        iter = richQuery.getKeyWordsIndivisible().iterator();
        while (iter.hasNext()) {

        	// Has the entire indivisible string, without breaking it up into individual words
        	hash = HashFunction.hash((String)iter.next(), bits);

        	// See if we have a 1 for it, letting it through our QRP table
        	if (contains(hash)) matchCount++; // This word makes it through, count it as a match
            wordCount++;                      // Count the total number of words we tested
        }

        // The XML contained just 1 or 2 words for us to hash and look for
        if (wordCount < 3) {

        	// To report a possible hit, all 3 must have found a 1 in this QRP table
            return wordCount == matchCount;

        // The XML contained 3 or more words for us to hash and look for
        } else {

        	// If 2/3rds or more of the words found a 1, return true to let the search through
            return ((float)matchCount/(float)wordCount) > 0.67;
        }
    }

    /**
     * Determine if this QRP table has a 1 for a given hash.
     * This means that there might be a hit, and a search should pass through.
     * 
     * @param hash The hash value produced by hashing a keyword.
     * @return     True if this QRP table has a 1 for that hash, letting a search through to find a possible hit.
     *             False if this QRP table has a 0 for that hash, blocking a search that could not possibly produce a hit.
     */
    private final boolean contains(int hash) {

    	/*
    	 * In the new version, we will not accept TTLs for methods.  Tables are only
    	 * 1 hop deep....
    	 */

    	// Look up the hash in our BitSet that holds the data of this QRP table
    	return bitTable.get(hash);
    }

    /**
     * Split the path of a shared file into individual words, hash them, and set those bits to 1 in this QRP table.
     * 
     * This method is given the complete path to a file we're sharing.
     * It breaks it around puncutation and spaces into a list of words.
     * It makes this list even longer by chopping 1 and 2 characters off the end of long words, and adding them to the list.
     * Then, it hashes each word, goes to that position in the QRP table, and changes the 0 there to a 1.
     * 
     * For all keywords k in filename, adds <k> to this.
     * 
     * @param filePath The path to a shared file, like "C:\Documents\Shared Files\Folder\File Name.ext"
     */
    public void add(String filePath) {

    	// Call the next method
    	addBTInternal(filePath);
    }

    /**
     * Split the path of a shared file into individual words, hash them, and set those bits to 1 in this QRP table.
     * Only add() above calls this method.
     * 
     * @param filePath The path to a shared file, like "C:\Documents\Shared Files\Folder\File Name.ext"
     */
    private void addBTInternal(String filePath) {

    	/*
    	 * Tour Point
    	 * 
    	 * Here's where LimeWire opens bits in the QRP table.
    	 * This method is given the complete path to a file we're sharing.
    	 * It breaks it around puncutation and spaces into a list of words.
    	 * It makes this list even longer by chopping 1 and 2 characters off the end of long words, and adding them to the list.
    	 * Then, it hashes each word, goes to that position in the QRP table, and changes the 0 there to a 1.
    	 */

    	// Prepare the list of keywords that could match this file, and will be hashed
        String[] words = HashFunction.keywords(filePath); // Split the path and file name on all spaces and puncutation into individual words
        String[] keywords = HashFunction.getPrefixes(words); // Chop 1 and 2 characters from each long word, making the list of words even longer

        // Express the size of the QRP table as a power of 2
        byte log2 = Utilities.log2(bitTableLength); // bitTableLength is 65536, making log2 16, as 2^16 = 65536

        // Loop for each keyword, like "documents", "document", "documen", "shared", "share", "shar"
        for (int i = 0; i < keywords.length; i++) {

        	// Hash this keyword
            int hash = HashFunction.hash(
            	keywords[i], // The keyword we are on in this run of the loop
            	log2);       // 16, telling the hash function the whole table is 2^16 bits, so produce an answer in that range

            // Use the hash value as a distance into the table, go there, if the bit there isn't already opened
            if (!bitTable.get(hash)) {

            	// Delete our resized QRP table, which won't have matching data as soon as we change this one
            	resizedQRT = null;
            	
            	// Set the bit there to a 1 to let a search for this keyword through
                bitTable.set(hash);
            }
        }
    }

    /**
     * Hash a given string, go to that place in this QRP table, and set the bit there to 1 to let searches through.
     * add() and addBTInternal() will split the String you give them into words and then shorten and hash each word.
     * addIndivisible() doesn't do that, it treats the given String as indivisible and hashes it without breaking it up first.
     * 
     * @param iString The text to hash and open in this QRP table
     */
    public void addIndivisible(String iString) {

    	// Compute the QRP hash of the given String
        final int hash = HashFunction.hash(iString, Utilities.log2(bitTableLength)); // bitTableLength is 65536, making this 16, as 2^16 = 65536

        // Use the hash value as a distance into the table, go there, if the bit there isn't already opened
        if (!bitTable.get(hash)) {

        	// Delete our resized QRP table, which won't have matching data as soon as we change this one
        	resizedQRT = null;

        	// Set the bit there to a 1 to let a search for this keyword through
        	bitTable.set(hash);
        }
    }

    /**
     * Add the striped pattern of a given QRP table to this one.
     * If either table has a 1 in a spot, we will have a 1 there also.
     * This makes our table more clear, and lets more searches through.
     * 
     * FileManager.getQRT() uses addAll() to make a copy of a QRP table.
     * MessageRouter.addQueryRoutingEntriesForLeaves() uses addAll() to create a composite QRP table.
     * 
     * For all <keyword_i> in qrt, adds <keyword_i> to this.
     * (This is useful for unioning lots of route tables for propoagation.)
     * 
     * @param qrt Another QRP table to add to this one
     */
    public void addAll(QueryRouteTable qrt) {

    	/*
    	 * First, resize the given QRT table to match our size.
    	 * Then, meld all the 1s in it into our table.
    	 */

    	// Resize the given table to match our size, and then use or to make a 1 either place a 1 here
        this.bitTable.or(qrt.resize(this.bitTableLength));
    }

    /**
     * Get a BitSet that has the striped pattern of this QRP table, and is of the requested size.
     * If our bitTable BitSet is of size newSize, returns a reference to it.
     * Otherwise, returns a reference to the bitTable BitSet inside our cached resizedQRT QueryRouteTable object.
     * If that size didn't match, this method deleted resizedQRT and made a new one of the requested size.
     * 
     * Only addAll() above calls this method.
     * 
     * @param newSize The requested size, the number of bits that should make up the table
     * @return        A reference to a BitSet that has the striped pattern of this QRP table spread across the specified size
     */
    private BitSet resize(int newSize) {

    	// If our BitTable is the requested size, return a reference to it
        if (bitTableLength == newSize) return bitTable;

        // If we already have a cached resizedQRT and its BitTable is the correct size, use it
        if (resizedQRT != null && resizedQRT.bitTableLength == newSize) return resizedQRT.bitTable;

        /*
         * We have to make a new QueryRouteTable object that has a bitTable BitSet of the requested size
         * 
         * First, we'll make a new QueryRouteTable object, and save it as resizedQRT.
         * Then, we'll convert and resize the data in our QRP table into the new one.
         */

        // Make a new QueryRouteTable object just like this one, and save it under resizedQRT
        resizedQRT = new QueryRouteTable(newSize); // Give it the requesed size

        // Convert and resize the data in our QRP table into the new one
        /*
         * This algorithm scales between tables of different lengths.
         * Refer to the query routing paper for a full explanation.
         * (The below algorithm, contributed by Philippe Verdy,
         *  uses integer values instead of decimal values
         *  as both double & float can cause precision problems on machines
         *  with odd setups, causing the wrong values to be set in tables)
         */
        final int m = this.bitTableLength;
        final int m2 = resizedQRT.bitTableLength;
        for (int i = this.bitTable.nextSetBit(0); i >= 0; i = this.bitTable.nextSetBit(i + 1)) {
        	// floor(i*m2/m)
        	final int firstSet = (int)(((long)i * m2) / m);
        	i = this.bitTable.nextClearBit(i + 1);
        	// ceil(i*m2/m)
        	final int lastNotSet = (int)(((long)i * m2 - 1) / m + 1);
        	resizedQRT.bitTable.set(firstSet, lastNotSet);
        }

        // Return a reference to the BitSet in the QueryRouteTable we made as a resized copy of us
        return resizedQRT.bitTable;
    }

    /**
     * Determine if a given QueryRouteTable object holds exactly the same information as this one.
     * Compares the BitSet objects, returning true if they are the same length and have exactly the same bits set.
     * 
     * @return True if they are the same, false if different
     */
    public boolean equals(Object o) {

    	// If the given object is a reference to us, it's the same, return true
        if (this == o) return true;

        // Make sure the given object is a QueryRouteTable, not some other type of object
        if (!(o instanceof QueryRouteTable)) return false;
        QueryRouteTable other = (QueryRouteTable)o; // It is, cast it that way

        /*
         * TODO: two qrt's can be equal even if they have different TTL ranges.
         */

        // Compare the lengths, and then the contents of the BitSet objects
        if (this.bitTableLength != other.bitTableLength) return false;
        if (!this.bitTable.equals(other.bitTable)) return false;
        return true;
    }

    /**
     * Hash the data in this QueryRouteTable into a number.
     * 
     * @return An int based on the data in the bit table right now
     */
    public int hashCode() {

    	// Hash the BitSet, and multiply by a prime number
    	return bitTable.hashCode() * 17;
    }

    /**
     * Express this QueryRouteTable object as text.
     * Composes text like "QueryRouteTable : {0, 1, 5, 22}".
     * 
     * @return A String
     */
    public String toString() {

    	// Compose and return the text
    	return "QueryRouteTable : " + bitTable.toString();
    }

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

    /**
     * Given a QRP patch message, bring this QRP table up to date.
     * 
     * Adds the specified patch message to this query routing table.
     * param patch the <tt>PatchTableMessage</tt> containing the new data to add
     * throws <tt>BadPacketException</tt> if the sequence number or size is incorrect
     * 
     * @param m A QRP patch message from a remote computer
     */
    public void patch(PatchTableMessage patch) throws BadPacketException {

    	// Call the next method
        handlePatch(patch);
    }

    /*
     * All encoding/decoding works in a pipelined manner, by continually
     * modifying a byte array called 'data'.  TODO2: we could avoid a lot of
     * allocations here if memory is at a premium.
     */

    /**
     * Given a QRP patch message, bring this QRP table up to date.
     * 
     * Only patch() above calls this method.
     * A remote computer has sent us a QRP patch message.
     * This QueryRouteTable object repsresents our record of the remote computer's QRP table.
     * This handlePatch() method uses the received patch message to bring our record of the QRP table up to date.
     * 
     * @param m A QRP patch message from the remote computer
     */
    private void handlePatch(PatchTableMessage m) throws BadPacketException {

    	/*
    	 * 0. Verify that m belongs in this sequence.  If we haven't just been
    	 * RESET, ensure that m's sequence size matches last message
    	 */

    	// Check the message's sequence size number
    	if (sequenceSize != -1 &&                // We're in the middle of getting a group of patch messages, and
    		sequenceSize != m.getSequenceSize()) // This patch message is part of a differently-sized group than what we expect
    		throw new BadPacketException("Inconsistent seq size: " + m.getSequenceSize() + " vs. " + sequenceSize);

    	/*
    	 * If we were just reset, ensure that m's sequence number is one.
    	 * Otherwise it should be one greater than the last message received.
    	 */

    	// Check the message's sequence number
    	if (sequenceNumber == -1 ?                       // If we're waiting for the first patch message in a group,
    		m.getSequenceNumber() != 1 :                 // Make sure the message we got is numbered 1, otherwise
            sequenceNumber + 1 != m.getSequenceNumber()) // We're waiting for the next message, make sure this is it
            throw new BadPacketException("Inconsistent seq number: " + m.getSequenceNumber() + " vs. " + sequenceNumber);

    	// Get the data the patch message holds
        byte[] data = m.getData(); // This is the payload data of the patch table message

        /*
         * 1. Start pipelined uncompression.
         * TODO: check that compression is same as last message.
         */

        // The message says the data is compressed
        if (m.getCompressor() == PatchTableMessage.COMPRESSOR_DEFLATE) {

            try {

            	/*
            	 * a) If first message, create uncompressor (if needed).
            	 */

            	// When we get the first message in a new group, make the Inflater object that will uncompress all the messages in the group
                if (m.getSequenceNumber() == 1) uncompressor = new Inflater();
                Assert.that(uncompressor != null, "Null uncompressor.  Sequence: " + m.getSequenceNumber());

                // Remove the compression from the data in this patch message
                data = uncompress(data);

            // There was an error removing the compression
            } catch (IOException e) {
                throw new BadPacketException("Couldn't uncompress data: " + e);
            }

        // The message says the data is compressed using some other compression scheme
        } else if (m.getCompressor() != PatchTableMessage.COMPRESSOR_NONE) {

        	// We only support deflate compression
            throw new BadPacketException("Unknown compressor");
        }

        /*
         * 2. Expand nibbles if necessary.
         */

        // If the message says it was able to put 2 bytes of QRP table data in each byte, remove this kind of compression next
        if (m.getEntryBits() == 4) data = unhalve(data);
        else if (m.getEntryBits() != 8) throw new BadPacketException("Unknown value for entry bits"); // If not, make sure it used 8

        /*
         * 3. Add data[0...] to table[nextPatch...]
         */

        // Move i across all the bytes in data
        for (int i = 0; i < data.length; i++) {

        	// Make sure we haven't moved beyond the end of our table
            if (nextPatch >= bitTableLength) throw new BadPacketException("Tried to patch " + nextPatch + " on a bitTable of size " + bitTableLength);

            /*
             * All negative values indicate presence
             * All positive values indicate absence
             */

            // The patch data has a negative value, set the bit in our table to 1
            if (data[i] < 0) {

            	// Set the bit to 1
            	bitTable.set(nextPatch);
                resizedQRT = null; // Throw away our cached table of a different size, our table has changed so we'll have to remake it

            // The patch data has a positive value, set the bit in our table to 0
            } else if (data[i] > 0) {

            	// Set the bit to 0
            	bitTable.clear(nextPatch);
                resizedQRT = null; // Throw away our cached table of a different size, our table has changed so we'll have to remake it
            }

            /*
             * From this, it looks like patch messages don't contain changes in values.
             * Rather, they contain values.
             * 
             * For instance, it's not like it's 0 to leave the same and 1 to flip to the opposite.
             * Rather, it's like it's -whatever to set to 1, and +whatever to set to 0.
             * 0 produces no change, but no value will flip the bit.
             */

            // Increment nextPatch, which scans down the whole table across the patch messages in a group
            nextPatch++;
        }

        // Tell our BitSet we're done editing it
        bitTable.compact(); // If it has 0s at the end, it will switch to a smaller array

        /*
         * 4. Update sequence numbers.
         */

        // Save the number of patch messages in this group
        this.sequenceSize = m.getSequenceSize();

        // This isn't the last patch message in its group
        if (m.getSequenceNumber() != m.getSequenceSize()) {

        	// Save it's number, the next message we get should have sequenceNumber + 1
        	this.sequenceNumber = m.getSequenceNumber();

        // This is the last patch message in the group, it's finishing the group
        } else {

        	// Reset member variables to indicate we're between groups of patch messages
            this.sequenceNumber = -1;
            this.sequenceSize = -1;
            this.nextPatch = 0;

            // We made an Inflater object to decompress the data in this group of patch messages
            if (this.uncompressor != null) {

            	// Free it
                this.uncompressor.end();
                this.uncompressor = null;
            }
        }
    }

    /** Not used. */
    public List encode(QueryRouteTable prev) { // List of RouteTableMessage objects
        return encode(prev, true);
    }

    /**
     * Make all the QRP messages we need to completely describe the QRP table this QueryRouteTable object represents.
     * Makes a ResetTableMessage followed by any number of PatchTableMessage objects, and returns them in a List.
     * Only MessageRouter.forwardQueryRouteTables() calls this method.
     * 
     * If we've already send our QRP table to a remote computer, the caller gives this method the QueryRouteTable prev.
     * In that case, our messages only need to describe what's changed in the table, and can be simpler and compress better.
     * 
     * If this is the first time we're sending a remote computer this QRP table, prev will be null.
     * This method starts the list with a reset table message, followed by the patch table messages.
     * If the remote computer has this QRP table, prev will not be null.
     * This method composes and returns a list of patch table messages.
     * There is only a reset message the very first time one Gnutella program starts communicating QRP table data to another.
     * The message stream from one computer to another looks like this:
     * 
     * Communication 1:  reset message         The initial communication begins with the reset message.
     *                   patch message 1 of 3  After that come a group of patch messages.
     *                   patch message 2 of 3
     *                   patch message 3 of 3
     * 
     * Communication 2:  patch message 1 of 2  Later communications consist of only a group of patch messages.
     *                   patch message 2 of 2
     * 
     * The reset message describes a QRP table that blocks everything.
     * It also chooses the infinity, a number which chooses the values in patch data.
     * LimeWire chooses 7 as the infinity.
     * 
     * A group of patch messages make up a patch.
     * A patch describes how to change the current QRP table on file to bring it up to date.
     * The data of a patch looks like this:
     * 
     * Patch Data:  0  0 -6 -6  6  0  6  0  0  0
     * 
     * 0 means no change.
     * -6 means change to let a search through.
     * 6 means change to block a search.
     * 
     * The values -6 and 6 are based on the infinity of 7.
     * 
     * A QRP table could be sent as just a list of 65536 bits, but it's much more complicated than that.
     * Each table value is just a bit, but takes up a whole byte in the data of patch messages.
     * QRP table data is 0 block or 1 let through, while patch message data is 0 no change, 6 block, -6 let through.
     * 
     * 65536 bytes of patch message data is 64 KB, so Gnutella programs do several things to transform the data.
     * If the values like 0, 6, and -6 all fit into 4 bits, this method halves the patch data, putting two values in each byte.
     * Then, it deflate compresses the data.
     * After that, it splits it up into 4 KB chunks, and puts each chunk in a patch table message.
     * 
     * Returns an List of RouteTableMessage that will convey the state of
     * this.  If that is null, this will include a reset.  Otherwise it will
     * include only those messages needed to to convert that to this.  More
     * formally, for any non-null QueryRouteTable's m and that, the following
     * holds:
     * <pre>
     * for (Iterator iter = m.encode(); iter.hasNext();)
     *    prev.update((RouteTableUpdate)iter.next());
     * Assert.that(prev.equals(m));
     * </pre>
     * 
     * @param prev             Our QRP table as the remote computer knows it.
     * @param allowCompression True to let this method compress QRP messages, which it will do if it makes them smaller.
     * @return                 A LinkedList of ResetTableMessage and PatchTableMessage objects.
     *                         Both of these classes extend RouteTableMessage, so you can look at all the items in the list as though they were RouteTableMessage objects.
     *                         Send these messages in their order in the list to completely communicate this QRP table to a remote computer.
     */
    public List encode(QueryRouteTable prev, boolean allowCompression) {

    	// Make the list that we'll put the messages in
        List buf = new LinkedList();

        // The remote computer doesn't have a version of our QRP table at all
        if (prev == null) {

        	// Start the list of messages we're making with a QRP reset table message, which will announce we're starting from scratch
        	buf.add(new ResetTableMessage(bitTableLength, infinity));

        // The remote computer has an out of date version of or QRP table, passed here as prev
        } else {

        	// Make sure the QRP table length didn't change somehow
            Assert.that(prev.bitTableLength == this.bitTableLength, "TODO: can't deal with tables of different lengths");
        }

        /*
         * 1. Calculate patch array
         */

        // Make data an array of 65536 bytes, one byte for each bit in this QRP table that is set to 0 or 1
        byte[] data = new byte[bitTableLength];

        /*
         * Fill up data with KEYWORD_NO_CHANGE, since the majority
         * of elements will be that.
         * Because it is already filled, we do not need to iterate and
         * set it anywhere.
         */

        // Fill our data array of 65536 bytes with 0 bytes, indicating all 65536 bits that make up the table should be unchanged
        Utilities.fill(data, 0, bitTableLength, KEYWORD_NO_CHANGE);

        // If we determine that the remote computer's record of our QRP table, prev, is out of date, we'll set needsPatch to true
        boolean needsPatch = false;

        /*
         * 1a. If there was a previous table, determine if it was the same one.
         *     If so, we can prevent BitTableLength calls to BitSet.get(int).
         */

        // We have the QRP table we previously sent the remote computer
        if (prev != null) {

        	/*
        	 * 1a-I. If they are not equal, xOr the tables and loop
        	 *       through the different bits.  This avoids
        	 *       bitTableLength*2 calls to BitSet.get
        	 *       at the cost of the xOr'd table's cardinality
        	 *       calls to both BitSet.nextSetBit and BitSet.get.
        	 *       Generally it is worth it, as our BitTables don't
        	 *       change very rapidly.
        	 *       With the xOr'd table, we know that all 'clear'
        	 *       values have not changed.  Thus, we can use
        	 *       nextSetBit on the xOr'd table & this.bitTable.get
        	 *       to determine whether or not we should set
        	 *       data[x] to keywordPresent or keywordAbsent.
        	 *       Because this is an xOr, we know that if 
        	 *       this.bitTable.get is true, prev.bitTable.get
        	 *       is false, and vice versa.            
        	 */

        	// This QRP table has changed since we sent it to the remote computer
        	if (!this.bitTable.equals(prev.bitTable)) {

        		// Make a new BitSet named xOr that has 1s just where the bits in this QRP table have changed
                BitSet xOr = (BitSet)this.bitTable.clone();
                xOr.xor(prev.bitTable); // XOR makes the BitSet just show where this and prev are different

                // Loop the index i down each 1 in xOr, these are the bits that have changed in our QRP table
                for (int i = xOr.nextSetBit(0); i >= 0; i = xOr.nextSetBit(i + 1)) {

                	// Set bytes in the patch message to -6 we set a bit to 1 letting a search through, or 6 we cleared a bit to 0 blocking a search
                    data[i] =
                    	this.bitTable.get(i) ? // i is a changed bit, if it's currently set
                    	keywordPresent :       // Communicate -6 in a byte, the code that lets a search pass through for a possible hit, otherwise
                    	keywordAbsent;         // Communicate 6 in a byte, the code that blocks a search that could produce no hit
                    
                    /*
                     * Here's what data, the patch we'll send to a remote computer, looks like now:
                     * 
                     * data:  0  0 -6 -6  6  0  6  0  0  0
                     * 
                     * 0 means no change.
                     * -6 means change to let a search through.
                     * 6 means change to block a search.
                     */

                    // We found a changed bit, and will have to send a patch to update the remote copy of our table
                    needsPatch = true;
                }
            }

        	/*
        	 * Else the two tables are equal, and we don't need to do anything
        	 * because all elements already contain KEYWORD_NO_CHANGE.
        	 */

        // This is the first time we're sending this remote computer our QRP table
        } else {

        	/*
        	 * 1b. If there was no previous table, scan through the table using
        	 *     nextSetBit, avoiding bitTableLength calls to BitSet.get(int).
        	 */

        	// Loop the index i down each 1 in our BitTable, these are the bits that let searches through
            for (int i = bitTable.nextSetBit(0); i >= 0; i = bitTable.nextSetBit(i + 1)) {

            	// Change the corresponding byte to -6, the code that lets a search pass through for a possible hit
                data[i] = keywordPresent;

                /*
                 * Here's what data, the patch we'll send to a remote computer, looks like now:
                 * 
                 * data:  0  0 -6 -6  0  0  0  0  0  0
                 * 
                 * 0 means block a search.
                 * -6 means let a search through.
                 */

                // We found a changed bit, and will have to send a patch to update the remote copy of our table
                needsPatch = true;
            }
        }

        /*
         * Optimization: there's nothing to report.  If prev=null, send a single
         * RESET.  Otherwise send nothing.
         */

        // Nothing has changed, or no searches should be let through
        if (!needsPatch) {

        	// Return the list of messages we prepared, which is empty or just a reset
            return buf;
        }

        /*
         * 2. Try compression.
         * TODO: Should this not be done if compression isn't allowed?
         */

        // Set bits, the number of bits it takes to express a single value in our table, to 8, a full byte, to begin
        byte bits = 8;

        /*
         * Only halve if our values require 4 signed bits at most.
         * keywordPresent will always be negative and
         * keywordAbsent will always be positive.
         */

        // If the chosen infinity makes keywordPresent and keywordAbsent fit into 4 bits each
        if (keywordPresent >= -8 && keywordAbsent <= 7) {

        	// We can use a half byte for each table value instead of a full one
            bits = 4;           // Set bits to 4, each table value will now go into 4 bits
            data = halve(data); // Replace data with an array half its size, with two bytes of values squashed
        }

        // Make a note of what kind of compression we're using
        byte compression = PatchTableMessage.COMPRESSOR_NONE; // Unless we do compression, it's 0x00 for no compression

        /*
         * Optimization: If we are told it is safe to compress the message,
         * then attempt to compress it.  Reasons it is not safe include
         * the outgoing stream already being compressed.
         */

        // The caller has allowed us to compress the messages
        if (allowCompression) {

        	// Compress the possibly halved data of the patch table
            byte[] patchCompressed = IOUtils.deflate(data);

            // See if that actually made it smaller
            if (patchCompressed.length < data.length) {

            	// Save the compressed data instead of the original data, and record that its compressed
                data = patchCompressed;
                compression = PatchTableMessage.COMPRESSOR_DEFLATE;
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

        // Record which chunk we're on, the first one is numbered 1
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
            		bits,              // The number of bits each value takes up, 4, a half byte, or 8, a full byte
            		data,              // The array to find the data in
            		i,                 // Start looking this far into the array
            		stop));            // Stop this far into the array

            // Increment the chunk number so the second patch message will be numbered 2, and so on
            chunk++;
        }

        // Return the list of messages to send
        return buf;
    }

    /*
     * ///////////////// Helper Functions for Codec ////////////////////////
     */

    /**
     * Remove the deflate compression from the next chunk of compressed patch table data we found in a patch table message from the remote computer.
     * 
     * Returns the uncompressed version of the given defalted bytes, using
     * any dictionaries in uncompressor.  Throws IOException if the data is
     * corrupt.
     * requires inflater initialized
     * modifies inflater
     * 
     * @param data The payload of a QRP patch table message, a chunk of compressed QRP patch table data
     * @return     The data, uncompressed
     */
    private byte[] uncompress(byte[] data) throws IOException {

    	// Make a new ByteArrayOutputStream that will grow as we add uncompressed data to it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Point the Inflater we're using on this group of patch messages at the given compressed data from the next one of them
        uncompressor.setInput(data);

        try {

        	// Make a temporary 1 KB buffer to hold uncompressed data
            byte[] buf = new byte[1024];

            // Loop until the Inflater stops inflating
            while (true) {

            	// Tell the Inflater to inflate, giving it the 1 KB buffer to put uncompressed data in
                int read = uncompressor.inflate(buf); // Returns the number of bytes it wrote in buf

                // The Inflater didn't produce anything, probably because it is out of compressed data
                if (read == 0) break; // Leave the loop

                // Move the uncompressed data from buf to baos, leaving buf empty for the next loop
                baos.write(buf, 0, read); // Move read bytes from buf to baos
            }

            // Make sure our ByteArrayOutputStream takes all of the bytes we've written to it
            baos.flush();

            // Get the contents of the ByteArrayOutputStream as a byte array, and return it
            return baos.toByteArray();

        // Convert a DataFormatException into an IOException
        } catch (DataFormatException e) {
        	throw new IOException("Bad deflate format");
        }
    }

    /**
     * Given an array with low numbers in each byte, composes an array half as big with two of those values in each byte.
     * 
     * array:  [ a] [ b] [ c] [ d] [ e] [ f] [ g] [ h]  length 8 bytes
     * return: [ab] [cd] [ef] [gh]                      length 4 bytes
     * 
     * This only works if the values in the given source array all fit into the lowest 4 bits of each byte.
     * 
     * Returns an array R of length array.length/2, where R[i] consists of the
     * low nibble of array[2i] concatentated with the low nibble of array[2i+1].
     * Note that unhalve(halve(array))=array if all elements of array fit can
     * fit in four signed bits.
     * requires array.length is a multiple of two
     * 
     * @param array The given array to halve
     * @return      An array half as big with the information squashed inside
     */
    static byte[] halve(byte[] array) {

    	// Make the array we'll return, which is exactly half as big as the given array
        byte[] ret = new byte[array.length / 2];

        // Loop for each byte in the half-size return array
        for (int i = 0; i < ret.length; i++) {

        	// Set this byte from the next two bytes in the source array
        	ret[i] = (byte)
        	    ((array[2 * i] << 4) |     // Shift into the high 4 bits
        		(array[2 * i + 1] & 0xF)); // Or with 0x0f to keep just the low 4 bits
        }

        // Return the array we made
        return ret;
    }

    /**
     * Restore the original array that halve() made half as big.
     * 
     * Returns an array of R of length array.length*2, where R[i] is the the
     * sign-extended high nibble of floor(i/2) if i even, or the sign-extended
     * low nibble of floor(i/2) if i odd.
     */
    static byte[] unhalve(byte[] array) {

    	// Make the array we'll return, which is twice as long as the given array
        byte[] ret = new byte[array.length * 2];

        // Move i across each byte in the given array
        for (int i = 0; i < array.length; i++) {

        	// Fill the return array with the high and then low 4 bits from the byte in the given array
            ret[2 * i] = (byte)(array[i] >> 4);
            ret[2 * i + 1] = extendNibble((byte)(array[i] & 0xF)); // Read the low 4 bits as a value -7 to 7
        }

        // Return the array we made
        return ret;
    }

    /**
     * If the given nibble has a 1 at 0x08, set all 4 high bits to 1.
     * 
     * Sign-extends the low nibble of b, i.e.,
     * returns (from MSB to LSB) b[3]b[3]b[3]b[3]b[3]b[2]b[1]b[0].
     * 
     * @param b A byte that contains a halved value in the low 4 bits
     * @return  The byte value the way it was before the remote computer halved it
     */
    static byte extendNibble(byte b) {

    	/*
    	 * The given byte b is a byte, but this method only looks at the low 4 bits of it.
    	 * 
    	 * byte b: 0000 abbb
    	 * 
    	 * The bit at 0x08, a, tells us if the value is negative or positive.
    	 * bbb then holds the value, 0 through 7. (do)
    	 * So, a nibble can hold the values -7 through 7 using this scheme of holding values.
    	 * -7 is 1111, and 7 is 0111.
    	 */

    	// The given nibble has at 1 at the 0x08 place, it's negative
        if ((b & 0x8) != 0) {

        	// Add 1s in the entire high 4 bits, like 1111 abbb
        	return (byte)(0xF0 | b);

        // The given nibble has a 0 at the 0x08 place, it's positive
        } else {

        	// Return it unchanged
        	return b;
        }
    }
}
