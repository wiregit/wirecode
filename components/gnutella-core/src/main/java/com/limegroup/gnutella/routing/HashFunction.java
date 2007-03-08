
// Commented for the Learning branch

package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.util.I18NConvert;
import com.limegroup.gnutella.util.StringUtils;

/**
 * This HashFunction class contains methods that turn keywords into slot numbers to open in a QRP table.
 * 
 * Gnutella programs exchange QRP tables to shield each other from searches that couldn't possibly produce a hit.
 * A QRP table is an array of 65536 bits.
 * A bit set to 0 blocks a search, while a bit set to 1 lets a search through.
 * A Gnutella program sharing nothing will have a QRP table of 65536 0s, completely opaque, it will block everything.
 * Then, imagine that program shares a file, named "hello".
 * Code in this class hashes the keyword "hello", telling which bit in the array it maps to.
 * The program will open that bit in its QRP table, letting a search for "hello" through.
 * 
 * All of the methods in this class are static.
 * This means the program never makes a HashFunction object, it just calls the methods here to hash things.
 * Only code in QueryRouteTable calls these method.
 * 
 * To hash a word, call hash("hello", 16);
 * You'll get back a value that fits into 16 bits, it will be 0 through 65535.
 * Go to that place in the QRP table, and set the bit there to 1 to let a search for "hello" through.
 * 
 * The official platform-independent hashing function for query-routing.  The
 * key property is that it allows interpolation of hash tables of different
 * sizes.  More formally, with x&gt;=0, n&gt;=0, k&gt;=0, 0&lt;=r&lt;=n,<ul>
 * <li>2 ^ k * hash(x, n) &lt;= hash(x, n+k) &lt 2 ^ (k+1) * hash(x, n);</li>
 * <li>hash(x, n-r) = int(hash(x, n) / 2 ^ r).</li>
 * </ul>
 * 
 * This version should now work cross-platform, however it is not intended
 * to be secure, only very fast to compute.  See Chapter 12.3.2. of CLR
 * for details of multiplication-based algorithms.
 */
public class HashFunction {

    /**
     * Hash a word in into a value that identifies a slot in a QRP table.
     * QueryRouteTable.addBTInternal() gives this method a folder or file name.
     * QueryRouteTable.addIndivisible() gives it a keyword from XML metadata.
     * QueryRouteTable.contains() gives it the text of a hash URN, an XML schema URI, or keyword text that shouldn't be divided.
     * 
     * Returns the n-bit hash of x.toLowerCase(), where n=<tt>bits</tt>.
     * That is, the returned value value can fit in "<tt>bits</tt>" unsigned
     * bits, and is between 0 and <tt>(2 ^ bits) - 1</tt>.
     * 
     * @param x    The word to hash.
     * @param bits The number of bits the hash value should fit in.
     *             16 to keep the hash value in the range of 2^16, 65536 values, the size of a QRP table.
     * @return     The hash value.
     */
    public static int hash(String x, byte bits) {

    	// Call the next hash() function, giving it the whole String
        return hash(x, 0, x.length(), bits);
    }

    /**
     * Hash a word in into a value that identifies a slot in a QRP table.
     * QueryRouteTable.contains() gives this method search text.
     * 
     * Returns the same value as hash(x.substring(start, end), bits), but tries
     * to avoid allocations.<p>
     * 
     * Note that x is lower-cased when hashing, using a locale-neutral
     * character case conversion based on the UTF-16 representation of the
     * source string to hash.  So it is stable across all platforms and locales.
     * However this does not only convert ASCII characters but ALL Unicode
     * characters having a single lowercase mapping character.  No attempt is
     * made here to remove accents and diacritics.<p>
     * 
     * The string is supposed to be in NFC canonical form, but this is not
     * enforced here.  Conversion to lowercase of characters uses Unicode rules
     * built into the the java.lang.Character core class, excluding all special
     * case rules (N-to-1, 1-to-M, N-to-M, locale-sensitive and contextual).<p>
     * 
     * A better way to hash strings would be to use String conversion in the
     * Locale.US context (for stability across servents) after transformation
     * to NFKD and removal of all diacritics from hashed keywords.  If needed,
     * this should be done before splitting the query string into hashable
     * keywords.
     * 
     * @param x     A String that contains the word we'll hash.
     * @param start The index in the String of the word to hash.
     * @param end   The index in the String where the word to hash ends.
     *              This index points at the character beyond the end of the word, we won't hash that character.
     * @param bits  The number of bits the hash value should fit in.
     *              16 to keep the hash value in the range of 2^16, 65536 values, the size of a QRP table.
     * @return      The hash value.
     */
    public static int hash(String x, int start, int end, byte bits) {

    	/*
         * 1. First turn x[start...end-1] into a number by treating all 4-byte
         * chunks as a little-endian quadword, and XOR'ing the result together.
         * We pad x with zeroes as needed.
         *     To avoid having do deal with special cases, we do this by XOR'ing
         * a rolling value one byte at a time, taking advantage of the fact that
         * x XOR 0==x.
         * 
         * xor is the running total
         * j is the byte position in xor. INVARIANT: j==8*((i-start)%4)
    	 */

    	// Make variables for the loop
    	int xor = 0; // The running total of our hash value
        int j = 0;   // Our position in xor

        // Move i down each character in the given text that make up the word we're hashing
        for (int i = start; i < end; i++) {

        	/*
             * internationalization be damned? Not a problem here:
             * we just hash the lower 8 bits of the lowercase UTF-16 code-units
             * representing characters, ignoring only the high 8 bits that
             * indicate a Unicode page, and it is not very widely distributed
             * even though they could also have feeded the hash function.
        	 */

        	// Get the character at i, and make it lowercase, take just the low byte, shift it up j bits, and move them into xor
        	xor ^= (Character.toLowerCase(x.charAt(i)) & 0xFF) << j;

        	// Make j bigger for the next time
        	j = (j + 8) & 24;
        }

        /*
         * 2. Now map number to range 0 - (2^bits-1).
         */

        // The xor value we calculated may be bigger than 65535, move it into that range
        return hashFast(xor, bits);
    }

    /**
     * Map the value x into a value that is between 0 and n^bits - 1.
     * 
     * Only hash() above calls this method.
     * 
     * Returns the n-<b>bit</b> hash of x, where n="bits".  That is, the
     * returned value value can fit in "bits" unsigned bits, and is
     * between 0 and (2^bits)-1.
     */
    private static int hashFast(int x, byte bits) {

    	/*
    	 * Keep only the "bits" highest bits of the 32 *lowest* bits of the
    	 * product (ignore overflowing bits of the 64-bit product result).
    	 * The constant factor should distribute equally each byte of x in
    	 * the returned bits.
    	 */

    	// Map x into a lower value
    	return (int)(x * A_INT) >>> (32 - bits);
    }

    /** Only hashFast() above uses this variable. */
    private static final int A_INT = 0x4F1BBCDC;

    /**
     * Split a path and file name into words.
     * 
     * filePath    "C:\\My Documents\\Shared\\Some Folder\\Title - Name.ext"
     * keywords()  "c:", "my", "documents", "shared", "some", "folder", "title", "name", "ext"
     * 
     * QueryRouteTable.addBTInternal() gives this method the path to a shared file.
     * It uses it to get the words it will shorten, hash, and add to the QRP table.
     * 
     * Returns a list of canonicalized keywords in the given file name, suitable
     * for passing to hash(String,int).  The returned keywords are
     * lower-cased, though that is not strictly needed as hash ignores
     * case.<p>
     * 
     * This function is not consistent for case conversion: it uses a locale
     * dependant String conversion, which also considers special casing rules
     * (N-to-1, 1-to-M, N-to-N, locale-sensitive and contextual variants),
     * unlike the simplified case conversion done in
     * <tt>hash(String, int, int, byte)</tt>, which is locale-neutral.<p>
     * 
     * A better way to hash strings would be to use String conversion in the
     * Locale.US context (for stability across servents) after transformation
     * to NFKD and removal of all diacritics from hashed keywords.  If needed,
     * this should be done before splitting the file name string into hashable
     * keywords. Then we should remove the unneeded toLowerCase() call in
     * the <tt>hash(String, int, int, byte)</tt> function.
     * 
     * param fileName The name of the file to break up into keywords.  These
     * keywords will subsequently be hashed for inclusion in the bit vector.
     * 
     * @param filePath The complete path to a shared file
     * @return         An array of String objects of each word
     */
    public static String[] keywords(String filePath) {

    	/*
         * TODO1: this isn't a proper implementation.  It should really be
         * to tokenized by ALL non-alphanumeric characters.
    	 * 
    	 * TODO2: perhaps we should do an English-specific version that accounts
    	 * for plurals, common keywords, etc.  But that's only necessary for
    	 * our own files, since the assumption is that queries have already been
    	 * canonicalized.
    	 * 
    	 * TODO: a better canonicalForm(query) function here that
    	 * also removes accents by converting first to NFKD and keeping
    	 * only PRIMARY differences
    	 */

    	// Split the path around the delimeters " -._+/*()\,"
        return StringUtils.split(
            I18NConvert.instance().getNorm(filePath),
            FileManager.DELIMITERS);
    }

    /**
     * Find the start of the next word in search text.
     * Given i that points to a space between words, returns the index to the start of the next word.
     * 
	 * query             "red green blue"
	 * i               3  -->
	 * keywordStart()  4  --->
     * 
     * Returns the index of the keyword starting at or after the i'th position
     * of query, or -1 if no such luck.
     * 
     * @param query Search text that contains words separated by spaces and other characters.
     * @param i     The index in that text to start looking from.
     * @return      The index to the start of the first word at or beyond i.
     *              -1 if no word found.
     */
    public static int keywordStart(String query, int i) {

    	// Find the first character at or beyond i that is not a character that separates words
    	final String DELIMITERS = FileManager.DELIMITERS; // The characters to avoid, " -._+/*()\,"
        for (; i < query.length(); i++) {                 // Loop from i for each remaining character in the text
            char c = query.charAt(i);
            if (DELIMITERS.indexOf(c) < 0) return i;      // Return the index where we found the word character
        }

        // Not found
        return -1;
    }

    /**
     * Find the end of the next word in search text.
     * Given i that points to a word, returns the index to the end of that word.
     * 
	 * query           "red green blue"
	 * i             4  --->
	 * keywordEnd()  9  -------->
     * 
     * Returns the index just past the end of the keyword starting at the i'th
     * position of query, or query.length() if no such index.
     * 
     * @param query Search text that contains words separated by spaces and other characters.
     * @param i     The index in that text to start looking from.
     * @return      The index to the start of the first word at or beyond i.
     *              query.length() if no word found.
     */
    public static int keywordEnd(String query, int i) {

    	// Find the first character at or beyond i that is a character that separates words
    	final String DELIMITERS = FileManager.DELIMITERS; // The character to find, " -._+/*()\,"
        for (; i < query.length(); i++) {                 // Loop from i for each remaining character in the text
            char c = query.charAt(i);
            if (DELIMITERS.indexOf(c) >= 0) return i;     // Return the index where we found the separator character
        }

        // Not found
        return query.length();
    }

    /**
     * Given a list of words, remove 1 and 2 characters from each word 5 characters or more, making the list of words even longer.
     * 
     * words          "reds and greens"
     * getPrefixes()  "reds", "and", "greens", "green", "gree"
     * 
     * Only the word "greens" is affected because only it is 5 characters or longer.
     * It gets 1 and 2 letters chopped off, creating "green" and "gree".
     * 
     * QueryRouteTable.addBTInternal() calls this method right after calling keywords() above.
     * This is designed to make searching for "things" also match "thing", removing the "s" and "es" plural suffixes from English words.
     * 
     * @param words A list of words
     * @return      A longer list, with those words and them 1 and 2 characters shorter
     */
    public static String[] getPrefixes(String[] words) {

    	// Count how many words in the given list are 5 characters long or longer
    	int prefixable = 0;
        for (int i = 0; i < words.length; i++) {
            if (words[i].length() > 4) prefixable++;
        }

        // All the words we were given are short, return the list as it was given to us
        if (prefixable == 0) return words;

        // Make the array we'll return with enough room for all the given words, and 2 shortened versions of each long word
        final String[] retArray = new String[words.length + prefixable * 2];

        // Loop for each given word
        int j = 0;
        for (int i = 0; i < words.length; i++) {
            final String word = words[i];

            // Copy this word into the array we'll return
            retArray[j++] = word;

            // This word is long enough to shorten
            final int len = word.length();
            if (len > 4) {

            	// Chop 1 and then 2 characters off the end, adding the shortened words to the return array also
                retArray[j++] = word.substring(0, len - 1);
                retArray[j++] = word.substring(0, len - 2);
            }
        }

        // Return the array we prepared
        return retArray;
    }
}
