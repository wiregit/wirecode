package com.limegroup.gnutella;

import com.sun.java.util.collections.*;

/**
 * A 16-bit globally unique ID.  Immutable.<p>
 *
 * Let the bytes of a GUID G be labelled G[0]..G[15].  All bytes are unsigned.
 * Let a "short" be a 2 byte little-endian** unsigned number.  Let AB be the
 * short formed by concatenating bytes A and B, with B being the most
 * significant byte.  LimeWire GUID's have the following properties:
 *
 * <ol>
 * <li>G[8]==0xFF.  This serves to identify "new GUIDs", e.g. from BearShare.
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
 * distinct GUID's.  And there's only a 1 in 65000 chance of mistakenly
 * identifying a LimeWire.
 */
public class GUID implements  com.sun.java.util.collections.Comparable {
    /** The size of a GUID. */
    private static final int SZ=16;
    /** Used to generated new GUID's. */
    private static Random rand=new Random();

    /** The contents of the GUID.  INVARIANT: bytes.length==SZ */
    private byte[] bytes;

    public GUID(byte[] bytes) {
        Assert.that(bytes.length==SZ);
        this.bytes=bytes;
    }

    /** See the class header description for more details.
     * @param first The first index of the first two bytes involved in the
     * marking.
     * @param second The first index of the second two bytes involved in the
     * marking.
     * @param markPoint The first index of the two bytes where to put the 
     * marking.
     */
    private static void tagGuid(byte[] guid,
                                int first,
                                int second,
                                int markPoint) {
        // You could probably avoid calls to ByteOrder as an optimization.
        short a=ByteOrder.leb2short(guid, first);
        short b=ByteOrder.leb2short(guid, second);
        short tag=tag(a, b);
        ByteOrder.short2leb(tag, guid, markPoint);        
    }

    /** Returns the bytes for a new GUID. */
    public static byte[] makeGuid() {
        //Start with random bytes.  You could avoid filling them all in,
        //but it's not worth it.
        byte[] ret=new byte[16];
        rand.nextBytes(ret);

        //Apply common tags.
        ret[8]=(byte)0xFF;    //Mark as "new" GUID.  (See isNewGUID().)
        ret[15]=(byte)0x00;   //Version number is 0.

        //Apply LimeWire's marking.
        tagGuid(ret, 4, 6, 9);
        return ret;
    }

    /** @return the bytes for a new GUID flagged to be a requery made by LW. 
     */
    public static byte[] makeGuidRequery() {
        byte[] ret = makeGuid();

        //Apply LimeWire's marking.
        tagGuid(ret, 0, 11, 13);

        return ret;
    }


    /** Returns LimeWire's secret tag described above. */
    static short tag(short a, short b) {
        int product=(a+2)*(b+3);
        //No need to actually do the AND since the downcast does that.
        short productMiddle=(short)(product >> 8);
        return productMiddle;
    }
    

    /** Same as isLimeGUID(this.bytes) */
    public boolean isLimeGUID() {    
        return isLimeGUID(this.bytes);
    }
    

    /** Same as isLimeRequeryGUID(this.bytes, version) */
    public boolean isLimeRequeryGUID(int version) {
        return isLimeRequeryGUID(this.bytes, version);
    }
    

    /** Same is isLimeRequeryGUID(this.bytes) */
    public boolean isLimeRequeryGUID() {
        return isLimeRequeryGUID(this.bytes);
    }

    private static boolean checkMatching(byte[] bytes, 
                                         int first,
                                         int second,
                                         int found) {
        short a = ByteOrder.leb2short(bytes, first);
        short b = ByteOrder.leb2short(bytes, second);
        short foundTag = ByteOrder.leb2short(bytes, found);
        short expectedTag = tag(a, b); 
        return foundTag == expectedTag;
    }


    /** Returns true if this is a specially marked LimeWire GUID.
     *  This does NOT mean that it's a new GUID as well; the caller
     *  will probably want to check that. */
    public static boolean isLimeGUID(byte[] bytes) {    
        return checkMatching(bytes, 4, 6, 9);
    }    

    /** Returns true if this is a specially marked Requery GUID from any version
     *  of LimeWire.  This does NOT mean that it's a new GUID as well; the
     *  caller will probably want to check that.
     */
    public static boolean isLimeRequeryGUID(byte[] bytes) {    
        return isLimeRequeryGUID(bytes, 0) || 
        isLimeRequeryGUID(bytes, 1) || isLimeRequeryGUID(bytes, 2);
    }    

    /** Returns true if this is a specially marked LimeWire Requery GUID.
     *  This does NOT mean that it's a new GUID as well; the caller
     *  will probably want to check that. 
     *
     *  @param version The version of RequeryGUID you want to test for.  0 for
     *  requeries up to 2.2.4, 1 for requeries between 2.2.4 and all 2.3s, and 2
     *  for current requeries....
     */
    public static boolean isLimeRequeryGUID(byte[] bytes, int version) {    
        if (version == 0)
            return checkMatching(bytes, 0, 9, 13);
        else if (version == 1)
            return checkMatching(bytes, 0, 2, 13);
        else
            return checkMatching(bytes, 0, 11, 13);
    }    
    
    
    /** Same as isWindowsGUID(this.bytes). */
    public final boolean isWindowsGUID() {    
        return isWindowsGUID(bytes);
    }

    /** Returns true if this is a GUID created by the Windows CoCreateGUID()
     *  routine.  Note that the converse does not hold; this method may
     *  also return true for other randomly generated GUID's. 
     *     @requires bytes.length==16
     */      
    public static boolean isWindowsGUID(byte[] bytes) {    
        //Windows GUID's are of the form 10------, where "1" is the most
        //significant bit and "-" means "don't care".  See the internet
        //draft by Salz and Leach:
        //       http://www.ics.uci.edu/~ejw/authoring/uuid-guid/
        //                              draft-leach-uuids-guids-01.txt    
        return (bytes[8]&0xc0)==0x80;
    }

    /** Same as isNewGUID(this.bytes). */
    public final boolean isNewGUID() {
        return isNewGUID(this.bytes);
    }
    
    /** Returns true if this is a GUID from newer Gnutella clients, e.g.,
     *  LimeWire and BearShare. */
    public static boolean isNewGUID(byte[] bytes) {
        //Is byte 8 all 1's?  Note that we downcast 0xFF first so both sides of
        //the equality are automatically widened, with the same sign extension.
        return bytes[8]==(byte)0xFF;
    }

    /** 
     * Compares this GUID to o, lexically.  Throws ClassCastException if o not
     *  a GUID. 
     */
    public int compareTo(Object o) {
        return compare(this.bytes, ((GUID)o).bytes);
    }

    /** Compares GUID's lexically. */
    public static class GUIDComparator implements com.sun.java.util.collections.Comparator {
        public int compare(Object a, Object b) {
            return GUID.compare(((GUID)a).bytes, ((GUID)b).bytes);
        }
    }

    /** Compares 16-byte arrays (raw GUIDs) lexically. */
    public static class GUIDByteComparator implements com.sun.java.util.collections.Comparator {
        public int compare(Object a, Object b) {
            return GUID.compare((byte[])a, (byte[])b);
        }
    }

    /** Compares guid and guid2 lexically, which MUST be 16-byte guids. */
    private static final int compare(byte[] guid, byte[] guid2) {
        for (int i=0; i<SZ; i++) {
            int diff=guid[i]-guid2[i];
            if (diff!=0)
                return diff;
        }
        return 0;
    }

    public boolean equals(Object o) {
        //The following assertions are to try to track down bug X96.
        if (! (o instanceof GUID))
            return false;
        Assert.that(o!=null, "Null o in GUID.equals");
        byte[] bytes2=((GUID)o).bytes();
        Assert.that(bytes!=null, "Null bytes in GUID.equals");
        Assert.that(bytes2!=null, "Null bytes2 in GUID.equals");
        for (int i=0; i<SZ; i++)
            if (bytes[i]!=bytes2[i])
                return false;
        return true;
    }

    public int hashCode() {
        //Glum bytes 0..3, 4..7, etc. together into 32-bit numbers.
        byte[] ba=bytes;
        final int M1=0x000000FF;
        final int M2=0x0000FF00;
        final int M3=0x00FF0000;

        int a=(M1&ba[0])|(M2&ba[1]<<8)|(M3&ba[2]<<16)|(ba[3]<<24);
        int b=(M1&ba[4])|(M2&ba[5]<<8)|(M3&ba[6]<<16)|(ba[7]<<24);
        int c=(M1&ba[8])|(M2&ba[9]<<8)|(M3&ba[10]<<16)|(ba[11]<<24);
        int d=(M1&ba[12])|(M2&ba[13]<<8)|(M3&ba[14]<<16)|(ba[15]<<24);

        //XOR together to yield new 32-bit number.
        return a^b^c^d;
    }

    /** Warning: this exposes the rep!  Do not modify returned value. */
    public byte[] bytes() {
        return bytes;
    }

    public String toString() {
        return toHexString();
//      StringBuffer buf=new StringBuffer();
//      for (int i=0; i<2; i++)
//          buf.append(bytes[i]+".");
//      buf.append("..");
//      for (int i=14; i<SZ-1; i++)
//          buf.append(bytes[0]+".");
//      buf.append(bytes[SZ-1]+"");
//      return buf.toString();
    }

    /**
     *  Create a hex version of a GUID for compact display and storage
     *  Note that the client guid should be read in with the
     *  Integer.parseByte(String s, int radix)  call like this in reverse
     */
    public String toHexString() {
        StringBuffer buf=new StringBuffer();
        String       str;
        int val;
        for (int i=0; i<SZ; i++) {
            //Treating each byte as an unsigned value ensures
            //that we don't str doesn't equal things like 0xFFFF...
            val = ByteOrder.ubyte2int(bytes[i]);
            str = Integer.toHexString(val);
            while ( str.length() < 2 )
            str = "0" + str;
            buf.append( str );
        }
        return buf.toString().toUpperCase();
    }
    
    /**
     *  Create a GUID bytes from a hex string version.
     *  Throws IllegalArgumentException if sguid is
     *  not of the proper format.
     */
    public static byte[] fromHexString(String sguid)
            throws IllegalArgumentException {
        byte bytes[] = new byte[SZ];
        try {
            for (int i=0; i<SZ; i++) {
                bytes[i] =
                    (byte)Integer.parseInt(sguid.substring(i*2,(i*2)+2), 16);
            }
            return bytes;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException();
        }
    }

    //Unit test: in the tests project.
}
