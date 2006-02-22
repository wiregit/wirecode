
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.util.Comparator;
import java.util.Random;

import com.limegroup.gnutella.util.NetworkUtils;

/**
 * A GUID is a globally unique identifier.
 * Separate computers can create lists of GUIDs and combine them later without worrying about both having made the same one.
 * 
 * LimeWire makes GUIDs by filling byte arrays with data from a java.util.Random object.
 * This is not a cryptographically-sound method of creating GUIDs, but works fine for Gnutella.
 * 
 * LimeWire hides additional data in the GUIDs it makes.
 * The tag(a, b) method takes two 2-byte values, and computes a third.
 * A LimeWire GUID contains a tag generated from two other locations in the same GUID.
 * This way, we can get a GUID and look for the tag.
 * If it's there, we know it's a LimeWire GUID.
 * 
 * By default, a LimeWire GUID has the tag at 9, generated from the pairs at 4 and 6.
 * The last byte is 0.
 * 
 * LimeWire can also store an IP address and port number in a GUID.
 * The IP address is right at the start, and the port number is 13 bytes in.
 * 
 * A 16-bit globally unique ID.  Immutable.<p>
 *
 * Let the bytes of a GUID G be labelled G[0]..G[15].  All bytes are unsigned.
 * Let a "short" be a 2 byte little-endian** unsigned number.  Let AB be the
 * short formed by concatenating bytes A and B, with B being the most
 * significant byte.  LimeWire GUID's have the following properties:
 *
 * <ol>
 * <li>G[15]=0x00.  This is reserved for future use.
 * <li>G[9][10]= tag(G[4][5], G[6][7]).  This is LimeWire's "secret" 
 *  proprietary marking. 
 * </ol>
 *
 * Here tag(A, B)=OxFFFF & ((A+2)*(B+3) >> 8).  In other words, the result is
 * obtained by first taking pair of two byte values and adding "secret"
 * constants.  These two byte values are then multiplied together to form a 4
 * byte product.  The middle two bytes of this product are the tag.  <b>Sign IS
 * considered during this process, since Java does that by default.</b><p>
 *
 * As of 9/2004, LimeWire GUIDs used to be marked as such:
 * <li>G[8]==0xFF.  This serves to identify "new GUIDs", e.g. from BearShare.
 * This marking was deprecated.
 *
 * In addition, LimeWire GUIDs may be marked as follows:
 * <ol>
 * <li>G[13][14]=tag(G[0]G[1], G[9][10]).  This was used by LimeWire 2.2.0-2.2.3
 * to mark automatic requeries.  Unfortunately these versions inadvertently sent
 * requeries when cancelling uploads or when sometimes encountering a group of
 * busy hosts. VERSION 0
 * </ol>
 * <li>G[13][14]=tag(G[0][1], G[2][3]).  This marks requeries from versions of
 *  LimeWire that have fixed the requery bug, e.g., 2.2.4 and all 2.3s.  VERSION
 * 1
 * </ol>
 * <li>G[13][14]=tag(G[0][1], G[11][12]).  This marks requeries from versions of
 * LimeWire that have much reduced the amount of requeries that can be sent by
 * an individual client.  a client can only send 32 requeries amongst ALL
 * requeries a day.  VERSION 2
 * </ol>
 *
 * Note that this still leaves 10-12 bytes for randomness.  That's plenty of
 * distinct GUIDs.  And there's only a 1 in 65000 chance of mistakenly
 * identifying a LimeWire.
 *
 * Furthermore, LimeWire GUIDs may be 'marked' by containing address info.  In
 * particular:
 * <ol>
 * <li>G[0][3] = 4-octet IP address.  G[13][14] = 2-byte port (little endian).
 * </ol>
 * Note that there is no way to tell from a guid if it has been marked in this
 * manner.  You need to have some indication external to the guid (i.e. for
 * queries the minSpeed field might have a bit set to indicate this).  Also,
 * this reduces the amount of guids per IP to 2^48 - plenty since IP and port
 * comboes are themselves unique.
 */
public class GUID implements Comparable {

    /** 16, a GUID has a size of 16 bytes. */
    private static final int SZ = 16;
    
    /** The Java random number generator we'll use to make GUIDs. */
    private static Random rand = new Random();

    /**
     * The GUID.
     * This byte array has length SZ, which is 16.
     */
    private byte[] bytes;

    /**
     * Make a new 16 byte LimeWire GUID with the marking at 9 and the last byte 0.
     */
    public GUID() {

        // Make a new LimeWire GUID with the marking at 9 and the last byte 0, and pass it to the next constructor
        this(makeGuid());
    }

    /**
     * Make a new 16 byte GUID from the given byte array.
     * 
     * @param bytes An array of 16 bytes, the given GUID value
     */
    public GUID(byte[] bytes) {

        // Make sure the given byte array is 16 bytes big, and save it in this object
        Assert.that(bytes.length == SZ);
        this.bytes = bytes;
    }

    /**
     * Writes the LimeWire tag into a given GUID.
     * 
     * Takes the shorts at first and second, computes the mark, and writes it at markPoint.
     * Reads and writes the shorts with the least significant byte first in the GUID, little endian order.
     * 
     * @param guid      The GUID to edit in place
     * @param first     The index of the first 2 bytes involved in the marking
     * @param second    The index of the second 2 bytes involved in the marking
     * @param markPoint The index of the 2 bytes to mark
     */
    private static void tagGuid(byte[] guid, int first, int second, int markPoint) {

        /*
         * You could probably avoid calls to ByteOrder as an optimization.
         */

        // Read both pairs of bytes using little endian byte order
        short a = ByteOrder.leb2short(guid, first); // In guid, at first, read 2 bytes little endian, and return them as the short a
        short b = ByteOrder.leb2short(guid, second);

        // Compute the tag from the two pairs of bytes
        short tag = tag(a, b); // Compute the value (a + 2) * (b + 3) shifted to the right 8 bits

        // Write the tag into the GUID
        ByteOrder.short2leb(tag, guid, markPoint); // In guid, at markPoint, write the 2 bytes of the short tag in little endian order
    }

    /**
     * Generate the bytes of a new unique LimeWire GUID.
     * Makes an array of 16 bytes, fills it with random data, sets the last byte to 0, and applies LimeWire's marking at 9.
     * 
     * @return A byte array of 16 bytes holding a new LimeWire GUID value
     */
    public static byte[] makeGuid() {

        /*
         * Start with random bytes.
         * We could avoid filling them all in, but an optimization like that isn't worth it.
         */

        // Make a new byte array called ret that's 16 bytes big and filled with 0s
        byte[] ret = new byte[16];

        // Have the Java Random object fill the array with random data
        rand.nextBytes(ret);

        /*
         * Apply common tags.
         */

        // Set the version number to 0
        ret[15] = (byte)0x00; // Make the last byte 0

        // Mark this GUID as a LimeWire GUID
        tagGuid(ret, 4, 6, 9); // Use the byte pairs at 4 and 6 to write the tag at 9

        // Return the array of 16 bytes with the GUID inside
        return ret;
    }

    /**
     * Generate the bytes of a new unique LimeWire GUID for a requery.
     * Makes a GUID that has the LimeWire tag at 9, adds the requery tag at 13, and sets the last byte to 0.
     * 
     * @return A byte array of 16 bytes holding a new LimeWire requery GUID value
     */
    public static byte[] makeGuidRequery() {

        // Make a GUID with LimeWire's marking at 9 and the last byte set to 0
        byte[] ret = makeGuid();

        // Mark this GUID as a LimeWire requery
        tagGuid(ret, 0, 11, 13); // Use the byte pairs at 0 and 11 to write the tag at 13

        // Return the array of 16 bytes with the GUID inside
        return ret;
    }

    /**
     * Make a GUID with an IP address at the start, the LimeWire mark at 9, the port number at 13, and the last byte set to 0.
     * 
     * @param ip   An IP address as an array of 4 bytes
     * @param port A port number
     * @return     A LimeWire GUID with the IP address and port number written inside it
     */
    public static byte[] makeAddressEncodedGuid(byte[] ip, int port) throws IllegalArgumentException {

        return addressEncodeGuid(makeGuid(), ip, port);
    }

    /**
     * Write an IP address at the start and a port number at byte 13 over a given GUID value.
     * 
     * @param ret  A byte array of 16 bytes that holds a GUID value we'll edit
     * @param ip   The IP address to write into the start
     * @param port The port number to write at byte 13
     * @return     ret, the reference to the byte array we changed in place
     */
    public static byte[] addressEncodeGuid(byte[] ret, byte[] ip, int port) throws IllegalArgumentException {

        // Make sure the GUID is 16 bytes, the IP address doesn't start 0 or 255, and the port number isn't 0 or 65536 or higher
        if (ret.length != SZ)                 throw new IllegalArgumentException("Input byte array wrong length.");
        if (!NetworkUtils.isValidAddress(ip)) throw new IllegalArgumentException("IP is invalid!");
        if (!NetworkUtils.isValidPort(port))  throw new IllegalArgumentException("Port is invalid: " + port);

        // Write the IP address and port number into the GUID
        for (int i = 0; i < 4; i++) ret[i] = ip[i]; // Write the 4 bytes of the IP address into the start of the GUID
        ByteOrder.short2leb((short) port, ret, 13); // Write the 2 bytes of the port number 13 bytes into the GUID

        // Return a reference to the byte array that we edited in place
        return ret;
    }

    /**
     * Compute LimeWire's secret tag that we hide in GUIDs.
     *
     * @param a A signed 2-byte value from the GUID
     * @param b Another signed 2-byte value from the GUID
     * @return  The value of (a + 2) * (b + 3) shifted the right 8 bits
     */
    static short tag(short a, short b) {

        // Compute the product
        int product = (a + 2) * (b + 3);

        /*
         * No need to actually do the AND since the downcast does that.
         */

        // Shift the product to the right 8 bits and return it
        short productMiddle = (short)(product >> 8); // The significant byte of the 2 byte short product will be 0
        return productMiddle;
    }

    /**
     * Determine if this is a LimeWire GUID.
     * Computes the tag from the byte pairs at 4 and 6, and sees if it appears at 9.
     * 
     * Being a LimeWire GUID doesn't mean that it's a new GUID, check that seprately.
     * 
     * @return True if this GUID contains the LimeWire tag, false if it doesn't
     */
    public boolean isLimeGUID() {

        // Look for the LimeWire tag
        return isLimeGUID(this.bytes);
    }

    /**
     * Determine if this GUID is a LimeWire requery GUID.
     * 
     * The LimeWire tag appears 13 bytes into the GUID.
     * If it's a version 0 requery GUID, the tag will be generated from the byte pairs at 0 and 9.
     * For version 1, the byte pairs are at 0 and 2.
     * For version 2, the byte pairs are at 0 and 11.
     * 
     * Being a LimeWire requery GUID doesn't mean that it's a new GUID, check that seprately.
     * 
     * @param version The requery GUID version, 0, 1, or 2
     * @return        True if this GUID contains the tag that marks it as a LimeWire requery GUID, false if it doesn't
     */
    public boolean isLimeRequeryGUID(int version) {

        // Look for the LimeWire requery GUID tag
        return isLimeRequeryGUID(this.bytes, version);
    }

    /**
     * Determine if this is a LimeWire requery GUID.
     * 
     * The LimeWire tag appears 13 bytes into the GUID.
     * It's generated from the byte pair at 0, and a second pair somewhere else.
     * Sees if the tag can be generated from a second pair at 9, 2, or 11.
     * 
     * Being a LimeWire requery GUID doesn't mean that it's a new GUID, check that seprately.
     * 
     * @return True if this GUID is tagged as a version 0, 1, or 2 LimeWire requery GUID, false if it's none of those
     */
    public boolean isLimeRequeryGUID() {

        // Return true if this is a version 0, 1, or 2 LimeWire requery GUID
        return isLimeRequeryGUID(this.bytes);
    }

    /**
     * See if the given IP address and port number are written into this GUID.
     * Looks for the IP address at the start, and the port number 13 bytes in.
     * 
     * @param ip   An IP address as an array of 4 bytes
     * @param port A port number
     * @return     True if the IP address and port number are written into the GUID, false if they're not
     */
    public boolean addressesMatch(byte[] ip, int port) throws IllegalArgumentException {

        // Look for the IP address at the start, and the port number 13 bytes in
        return addressesMatch(this.bytes, ip, port);
    }

    /**
     * Read the first 4 bytes in this GUID as an IP address, and return it as a String like "1.2.3.4".
     * Query packets put the IP address and port number computers should send UDP out of band replies to in the GUID.
     * 
     * @return The first 4 bytes as IP address text like "1.2.3.4"
     */
    public String getIP() {

        // Read the first 4 bytes of this GUID
        return getIP(this.bytes);
    }

    /**
     * Determine if this GUID starts with a given IP address.
     * Query packets put the IP address and port number computers should send UDP out of band replies to in the GUID.
     * 
     * @param bytes An IP address in a 4-byte array
     * @return      True if the GUID starts with the IP address, false if a byte doesn't match
     */
    public boolean matchesIP(byte[] bytes) {

        // See if this GUID starts with the given 4 bytes
        return matchesIP(bytes, this.bytes);
    }

    /**
     * Reads the 2 bytes starting at 13 in this GUID as a port number.
     * Query packets put the IP address and port number computers should send UDP out of band replies to in the GUID.
     * 
     * @return The port number written in the GUID
     */
    public int getPort() {

        // Pull the 2 bytes at 13 from this GUID
        return getPort(this.bytes);
    }

    /**
     * Determine if a GUID contains a LimeWire tag.
     * 
     * @param bytes  The 16 bytes of a GUID to examine
     * @param first  The index of the first pair of bytes that may have been used to generate a tag here
     * @param second The index of the second pair of bytes for the tag
     * @param found  The index where the tag should be
     * @return       True if the GUID contains the tag, false if it's not there
     */
    private static boolean checkMatching(byte[] bytes, int first, int second, int found) {

        // Compute what tag should be there, and then see if it is there
        short a = ByteOrder.leb2short(bytes, first);
        short b = ByteOrder.leb2short(bytes, second);
        short foundTag = ByteOrder.leb2short(bytes, found);
        short expectedTag = tag(a, b); // The tag is the value of (a + 2) * (b + 3) shifted to the right 8 bits
        return foundTag == expectedTag;
    }

    /**
     * Determine if the given GUID is a LimeWire GUID.
     * Computes the tag from the byte pairs at 4 and 6, and sees if it appears at 9.
     * 
     * Being a LimeWire GUID doesn't mean that it's a new GUID, check that seprately.
     * 
     * @param bytes A GUID
     * @return      True if the GUID contains the LimeWire tag, false if it doesn't
     */
    public static boolean isLimeGUID(byte[] bytes) {

        // Return true if the tag from 4 and 6 appears at 9
        return checkMatching(bytes, 4, 6, 9);
    }

    /**
     * Determine if the given GUID is a LimeWire requery GUID.
     * 
     * The LimeWire tag appears 13 bytes into the GUID.
     * It's generated from the byte pair at 0, and a second pair somewhere else.
     * Sees if the tag can be generated from a second pair at 9, 2, or 11.
     * 
     * Being a LimeWire requery GUID doesn't mean that it's a new GUID, check that seprately.
     * 
     * @param bytes A GUID
     * @return      True if the GUID is tagged as a version 0, 1, or 2 LimeWire requery GUID, false if it's none of those
     */
    public static boolean isLimeRequeryGUID(byte[] bytes) {

        // Return true if this is a version 0, 1, or 2 LimeWire requery GUID
        return isLimeRequeryGUID(bytes, 0) || isLimeRequeryGUID(bytes, 1) || isLimeRequeryGUID(bytes, 2);
    }

    /**
     * Determine if the given GUID is a LimeWire requery GUID.
     * 
     * The LimeWire tag appears 13 bytes into the GUID.
     * If it's a version 0 requery GUID, the tag will be generated from the byte pairs at 0 and 9.
     * For version 1, the byte pairs are at 0 and 2.
     * For version 2, the byte pairs are at 0 and 11.
     * 
     * Being a LimeWire requery GUID doesn't mean that it's a new GUID, check that seprately.
     * 
     * @param bytes   A GUID
     * @param version The requery GUID version, 0, 1, or 2
     * @return        True if the GUID contains the tag that marks it as a LimeWire requery GUID, false if it doesn't
     */
    public static boolean isLimeRequeryGUID(byte[] bytes, int version) {

        // Based on the given version number, look for the LimeWire tag having been generated from different parts of the GUID
        if      (version == 0) return checkMatching(bytes, 0,  9, 13); // Version 0, look for the tag at 13 from the byte pairs at 0 and 9
        else if (version == 1) return checkMatching(bytes, 0,  2, 13); // Version 1, look for the tag at 13 from the byte pairs at 0 and 2
        else                   return checkMatching(bytes, 0, 11, 13); // Version 2, look for the tag at 13 from the byte pairs at 0 and 11
    }

    /**
     * See if the given IP address and port number are written into the given GUID.
     * Looks for the IP address at the start, and the port number 13 bytes in.
     * 
     * @param guidBytes A GUID
     * @param ip        An IP address as an array of 4 bytes
     * @param port      A port number
     * @return          True if the IP address and port number are written into the GUID, false if they're not
     */
    public static boolean addressesMatch(byte[] guidBytes, byte[] ip, int port) throws IllegalArgumentException {

        // Make sure the IP address is 4 bytes, the port number is 1 through 65535, and the IP address doesn't start 0 or 255
        if (ip.length != 4) throw new IllegalArgumentException("IP address too big!");
        if (!NetworkUtils.isValidPort(port)) return false;
        if (!NetworkUtils.isValidAddress(ip)) return false;

        // Get the 2 bytes of the port number into the right order to look for them in the GUID
        byte[] portBytes = new byte[2];
        ByteOrder.short2leb((short)port, portBytes, 0);

        // Return true if the 4 bytes of the IP address are at the start, and the 2 bytes of the port number are at 13
        return ((guidBytes[0] == ip[0]) && // If anything doesn't match, return false
                (guidBytes[1] == ip[1]) &&
                (guidBytes[2] == ip[2]) &&
                (guidBytes[3] == ip[3]) &&
                (guidBytes[13] == portBytes[0]) &&
                (guidBytes[14] == portBytes[1]));
    }

    /**
     * Read the first 4 bytes in a GUID as an IP address, and return it as a String like "1.2.3.4".
     * 
     * @param guidBytes A GUID in a byte array of 16 bytes
     * @return          The first 4 bytes as IP address text like "1.2.3.4"
     */
    public static String getIP(byte[] guidBytes) {

        // Read the 4 bytes at the start, and compose them into text
        return NetworkUtils.ip2string(guidBytes);
    }

    /**
     * Determine if a given GUID starts with a given IP address.
     * 
     * @param ipBytes   An IP address in a 4-byte array
     * @param guidBytes A GUID in a 16-byte array
     * @return          True if the GUID starts with the IP address, false if a byte doesn't match
     */
    public static boolean matchesIP(byte[] ipBytes, byte[] guidBytes) {

        // Make sure the IP address is 4 bytes long
        if (ipBytes.length != 4) throw new IllegalArgumentException("Bad byte[] length = " + ipBytes.length);

        // Match all 4 bytes in the IP address to bytes in the GUID
        for (int i = 0; i < ipBytes.length; i++) {

            // Return false on mismatch
            if (ipBytes[i] != guidBytes[i]) return false;
        }

        // The GUID starts with the IP address
        return true;
    }

    /**
     * Reads the 2 bytes starting at 13 in a GUID as a port number.
     * 
     * @param guidBytes A GUID
     * @return          The port number written in the GUID
     */
    public static int getPort(byte[] guidBytes) {

        // In the GUID, read the 2 bytes at 13 least significant first as a short, and then convert that into an int
        return ByteOrder.ushort2int(ByteOrder.leb2short(guidBytes, 13));
    }

    /**
     * Compares this GUID to a given object.
     * 
     * @param o An object to compare this GUID to.
     * @return  0 if they are the same.
     *          Negative if a this GUID should come before the given object.
     *          Positive if a this GUID should come after the given object.
     *          1 if the given object isn't a GUID, sort the given object first.
     */
    public int compareTo(Object o) {

        // If o is the same object as this GUID, report that they are the same
        if (this == o) return 0;

        // The given object is a GUID object like us
		else if (o instanceof GUID) {

            // Compare the 16 bytes of the GUIDs
		    return compare(this.bytes(), ((GUID)o).bytes());

        // The given object is not a GUID at all
        } else {

            // Sort it before this GUID
            return 1;
        }
    }

    /** Call GUID_COMPARATOR.compare(a, b) to sort two GUID objects. */
    public static final Comparator GUID_COMPARATOR = new GUIDComparator();

    /** Call GUID_BYTE_COMPARATOR.compare(a, b) to sort two byte arrays. */
    public static final Comparator GUID_BYTE_COMPARATOR = new GUIDByteComparator();

    /** A class with a compare(a, b) method that takes GUID objects. */
    public static class GUIDComparator implements Comparator {

        /**
         * Compare 2 GUID objects.
         * 
         * @param a A GUID.
         * @param b Another GUID.
         * @return  0 if they are the same.
         *          Negative if a the first should come before the second.
         *          Positive if a the first should come after the second.
         */
        public int compare(Object a, Object b) {

            // Cast a and b to GUID objects
            return GUID.compare(((GUID)a).bytes, ((GUID)b).bytes);
        }
    }

    /** A class with a compare(a, b) method that takes 2 byte arrays. */
    public static class GUIDByteComparator implements Comparator {

        /**
         * Compare 2 byte arrays.
         * 
         * @param a A GUID.
         * @param b Another GUID.
         * @return  0 if they are the same.
         *          Negative if a the first should come before the second.
         *          Positive if a the first should come after the second.
         */
        public int compare(Object a, Object b) {

            // Cast a and b to byte arrays
            return GUID.compare((byte[])a, (byte[])b);
        }
    }

    /**
     * Compare 2 GUIDS to see if they are the same, or which should come first.
     * 
     * @param guid  A GUID.
     * @param guid2 Another GUID.
     * @return      0 if they are the same.
     *              Negative if a the first should come before the second.
     *              Positive if a the first should come after the second.
     */
    private static final int compare(byte[] guid, byte[] guid2) {

        // Loop through the 16 bytes in a GUID
        for (int i = 0; i < SZ; i++) {

            // Compare the bytes
            int diff = guid[i] - guid2[i];
            if (diff != 0) return diff; // If the byte from the first GUID is bigger, return positive, it should come after the second
        }

        // All 16 bytes are exactly the same
        return 0;
    }

    /**
     * Determine if this GUID is the same as a given object.
     * 
     * @param o The object that might be the same
     * @return  True if o is a GUID with the same value, false if different value or different type of object
     */
    public boolean equals(Object o) {

        /*
         * The following assertions are to try to track down bug X96.
         */

        // If the given object isn't even a GUID, they're not the same
        if (!(o instanceof GUID)) return false;

        // Point bytes2 at the byte array inside the given GUID object, and make sure both references are good
        Assert.that(o != null, "Null o in GUID.equals");
        byte[] bytes2 = ((GUID)o).bytes();
        Assert.that(bytes  != null, "Null bytes in GUID.equals");
        Assert.that(bytes2 != null, "Null bytes2 in GUID.equals");

        // Loop down the 16 bytes in the GUIDs
        for (int i = 0; i < SZ; i++) {

            // If this pair doesn't match, return false, different
            if (bytes[i] != bytes2[i]) return false;
        }

        // The GUIDs are the same
        return true;
    }

    /**
     * Compute the hash code of this GUID.
     * 
     * @return An int hash code
     */
    public int hashCode() {

        /*
         * Glum bytes 0..3, 4..7, etc. together into 32-bit numbers.
         */

        // Point ba at the byte array that holds the 16 bytes of this GUID
        byte[] ba = bytes;

        // Set up 3 masks
        final int M1 = 0x000000FF;
        final int M2 = 0x0000FF00;
        final int M3 = 0x00FF0000;

        // Mask, shift, and combine to generate 4 numbers
        int a = (M1 & ba[0])  | (M2 & ba[1]  << 8) | (M3 & ba[2]  << 16) | (ba[3]  << 24);
        int b = (M1 & ba[4])  | (M2 & ba[5]  << 8) | (M3 & ba[6]  << 16) | (ba[7]  << 24);
        int c = (M1 & ba[8])  | (M2 & ba[9]  << 8) | (M3 & ba[10] << 16) | (ba[11] << 24);
        int d = (M1 & ba[12]) | (M2 & ba[13] << 8) | (M3 & ba[14] << 16) | (ba[15] << 24);

        /*
         * XOR together to yield new 32-bit number.
         */

        // Combine the 4 numbers and return the result
        return a ^ b ^ c ^ d;
    }

    /**
     * Get access to the byte array this GUID object keeps its GUID in.
     * 
     * This exposes the representation.
     * Don't modify the returned value.
     * 
     * @return A reference to this GUID object's internal byte array
     */
    public byte[] bytes() {

        // Return a reference to the internal byte array
        return bytes;
    }

    /**
     * Write out the GUID in base 16 for display and storage.
     * Each byte in the GUID becomes the text "00" through "FF".
     * Letters are uppercase.
     * 
     * Note that the client guid should be read in with the
     * Integer.parseByte(String s, int radix) call like this in reverse
     * 
     * @return The data of this GUID in hexadecimal characters
     */
    public String toString() {
        
        // Call the next method
        return toHexString();
    }

    /**
     * Write out the GUID in base 16 for display and storage.
     * Each byte in the GUID becomes the text "00" through "FF".
     * Letters are uppercase.
     * 
     * Note that the client guid should be read in with the
     * Integer.parseByte(String s, int radix) call like this in reverse
     * 
     * @return The data of this GUID in hexadecimal characters
     */
    public String toHexString() {

        // Set up what we'll need in the loop
        StringBuffer buf = new StringBuffer(); // A StringBuffer object that we can edit quickly
        String str; // The base 16 characters of one byte, like "00"
        int val; // The value of one byte in an int

        // Loop through the 16 bytes in the GUID
        for (int i = 0; i < SZ; i++) {

            /*
             * Treating each byte as an unsigned value ensures
             * that we don't str doesn't equal things like 0xFFFF...
             */

            // Read the byte and convert it to text characters in base 16
            val = ByteOrder.ubyte2int(bytes[i]);
            str = Integer.toHexString(val);
            while (str.length() < 2) str = "0" + str; // Add a leading 0 if necessary

            // Add the 2 characters to the end of the string
            buf.append(str);
        }

        // Make a String from the StringBuilder text, and make it uppercase
        return buf.toString().toUpperCase();
    }

    /**
     * Read base 16 text as a GUID value.
     * 
     * @param sguid A GUID expressed as text using base 16 characters
     * @return      A byte array with the 16 bytes of the GUID
     */
    public static byte[] fromHexString(String sguid) throws IllegalArgumentException {

        // Make a byte array that can hold 16 bytes
        byte bytes[] = new byte[SZ];

        try {

            // Loop for each byte in the array
            for (int i = 0; i < SZ; i++) {

                // Read the next pair of characters like "0A", parse them as an integer, and store that number as a byte in the byte array
                bytes[i] = (byte)Integer.parseInt(sguid.substring(i * 2, (i * 2) + 2), 16); // (do) what's the third argument?
            }

            // Return the byte array we filled with GUID data
            return bytes;

        // Catch exceptions and throw IllegalArgumentException instead
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * A TimedGUID is a GUID that expires after a certain amount of time.
     * 
     * When you make a TimedGUID, you give the constructor a GUID and a lifetime in milliseconds.
     * It records the time it was created.
     * Call shouldExpire() on it to see if its time has run out yet.
     * 
     * Simply couples a GUID with a timestamp.  Needed for expiration of
     * QueryReplies waiting for out-of-band delivery, expiration of proxied
     * GUIDs, etc.
     */
    public static class TimedGUID {

        /** The number of milliseconds this TimedGUID will live. */
        private final long MAX_LIFE;
        
        /** The GUID value this TimedGUID object holds. */
        private final GUID _guid;

        /**
         * Get the GUID object this TimedGUID holds.
         * 
         * @return The GUID
         */
        public GUID getGUID() {

            // Return the object
            return _guid;
        }

        /** The time when this object was made. */
        private final long _creationTime;

        /**
         * Make a new TimedGUID object that will hold a GUID and an expiration time.
         * 
         * @param guid    The GUID to store in this new TimedGUID object
         * @param maxLife The number of milliseconds from now when this new TimedGUID should expire
         */
        public TimedGUID(GUID guid, long maxLife) {

            // Save the given GUID and expiration date
            _guid = guid;
            MAX_LIFE = maxLife;

            // Record this new TimedGUID object was made now
            _creationTime = System.currentTimeMillis();
        }

        /**
         * Determine if this TimedGUID object is the same as a given one.
         * Compares the 16 bytes of the GUIDs, not the creation or expiration times.
         * 
         * If the given object isn't a TimedGUID, this will return false.
         * If you compare a TimedGUID to a GUID and the GUID values are the same, this will still return false.
         * 
         * @return True if the GUIDs are the same, false if they are different.
         */
        public boolean equals(Object other) {
            
            // The caller passed us a reference to ourselves
            if (other == this) return true; // Report same

            // Compare the bytes of the GUID
            if (other instanceof TimedGUID) return _guid.equals(((TimedGUID) other)._guid);
            return false; // The given object isn't a TimedGUID, different
        }

        /**
         * Compute the hash code of the GUID inside this TimedGUID object.
         * 
         * Since guids will be all we have when we do a lookup in a hashtable,
         * we want the hash code to be the same as the GUID.
         * 
         * @return An int hash code
         */
        public int hashCode() {

            // Compute the hash code of the GUID
            return _guid.hashCode();
        }

        /**
         * Determine if this TimedGUID has expired.
         * 
         * When you made this TimedGUID, you gave it a lifetime, and it recorded when it was made.
         * This method checks the current time, if the lifetime has run out, this TimedGUID has expired.
         * 
         * @return True if this TimedGUID has expired, false if it still has some time left.
         */
        public boolean shouldExpire() {

            // See if it's been more than MAX_LIFE milliseconds since we created this TimedGUID
            long currTime = System.currentTimeMillis();
            if (currTime - _creationTime >= MAX_LIFE) return true;
            return false;
        }
    }
}
