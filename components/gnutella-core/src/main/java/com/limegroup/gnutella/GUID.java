package com.limegroup.gnutella;

import com.sun.java.util.collections.*;

/**
 * A 16-bit globally unique ID.  Immutable.<p>
 *
 * Let the bytes of a GUID G be labelled G[0]..G[15].  All bytes are unsigned.
 * Let a "short" be a 2 byte little-endian** unsigned number.  Let AB be the
 * short formed by concatenating bytes A and B, with B being the most
 * significant byte.  Then LimeWire GUID's have the following properties.
 *
 * <ol>
 * <li>G[8]==0xFF.  This serves to identify "new GUIDs", e.g. from BearShare.
 * <li>G[15]=0x00.  This is reserved for future use.
 * <li>G[9][10]= 0xFFFF & ((G[4]G[5]+2)*(G[6][7]+3) >> 8).  This is LimeWire's
 *   proprietary marking.  In other words, the result is obtained by first taking
 *   the two byte values before the 0xFF and adding "secret" constants.  These
 *   two byte values are then multiplied together to form a 4 byte product.  The
 *   middle two bytes of this product are then stored after the 0xFF.  <b>Sign
 *   IS considered during this process, since Java does that by default.</b>
 * </ol>
 *
 * Note that this still leaves 12 bytes for randomness.  That's plenty of
 * distinct GUID's.  And there's only a 1 in 65000 chance of mistakenly
 * identifying a LimeWire.
 *
 * ADDITION: added makeRequeryGuid() method.
 * A Requery GUID has the extra property that:
 * G[13][14]= 0xFFFF & ((G[0]G[1]+2)*(G[9][10]+3) >> 8).
 * So the chances of a false positive RequeryGuid are (1/65000) * (1/65000)
 * This still leaves 10 bytes for randomness, which is PLENTY of GUIDs. 
 */
public class GUID implements java.lang.Comparable, 
                             com.sun.java.util.collections.Comparable {
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
        tagGuid(ret, 0, 9, 13);

        return ret;
    }


    /** Returns LimeWire's secret tag described above. */
    private static short tag(short a, short b) {
        int product=(a+2)*(b+3);
        //No need to actually do the AND since the downcast does that.
        short productMiddle=(short)(product >> 8);
        return productMiddle;
    }
    

    /** Same as isLimeGUID(this.bytes) */
    public boolean isLimeGUID() {    
        return isLimeGUID(this.bytes);
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


    /** Returns true if this is a specially marked LimeWire Requery GUID.
     *  This does NOT mean that it's a new GUID as well; the caller
     *  will probably want to check that. */
    public static boolean isLimeRequeryGUID(byte[] bytes) {    
        return checkMatching(bytes, 0, 9, 13);
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
    public static class GUIDComparator implements java.util.Comparator, 
            com.sun.java.util.collections.Comparator {
        public int compare(Object a, Object b) {
            return GUID.compare(((GUID)a).bytes, ((GUID)b).bytes);
        }
    }

    /** Compares 16-byte arrays (raw GUIDs) lexically. */
    public static class GUIDByteComparator implements java.util.Comparator, 
            com.sun.java.util.collections.Comparator {
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

    /*
    public static void main(String args[]) {
        byte[] b1=new byte[16];
        byte[] b2=new byte[16];
        for (int i=0; i<16; i++) {
            b1[i]=(byte)i;
            b2[i]=(byte)i;
        }
        GUID g1=new GUID(b1);
        GUID g2=new GUID(b1);
        Assert.that(g1.equals(g2));
        Assert.that(g2.equals(g1));
        Assert.that(g1.hashCode()==g2.hashCode());

        Hashtable t=new Hashtable();
        String out=null;
        t.put(g1,"test");
        Assert.that(t.containsKey(g1),"Contains 1");
        out=(String)t.get(g1);
        Assert.that(out!=null, "Null test 1");
        Assert.that(out.equals("test"), "Get test 1");

        Assert.that(t.containsKey(g2),"Contains 2");
        out=(String)t.get(g2);
        Assert.that(out!=null, "Null test 2");
        Assert.that(out.equals("test"), "Get test 2");

        String hexString="FF010A00000000000000000000000001";
        byte[] bytes=new byte[16];
        bytes[0]=(byte)255;
        bytes[1]=(byte)1;
        bytes[2]=(byte)10;
        bytes[15]=(byte)1;

        String s=(new GUID(bytes)).toHexString();
        Assert.that(s.equals(hexString));
        byte[] bytes2=GUID.fromHexString(s);
        Assert.that(Arrays.equals(bytes2,bytes));

        try {
            GUID.fromHexString("aa01");
            Assert.that(false);
        } catch (IllegalArgumentException e) {
            Assert.that(true);
        }

        try {
            GUID.fromHexString("ff010a0000000000000000000000000z");
            Assert.that(false);
        } catch (IllegalArgumentException e) {
            Assert.that(true);
        }

        //Try new GUID generator.
        for (int i=0; i<50; i++) {
            bytes=GUID.makeGuid();     g1=new GUID(bytes);
            Assert.that(bytes[8]==(byte)0xFF);
            Assert.that(bytes[15]==(byte)0x00);
            Assert.that(g1.isNewGUID());
            Assert.that(g1.isLimeGUID());
            Assert.that(! g1.isWindowsGUID());
            System.out.println(g1);
        }               
        
        //Try isWindowsGUID (again)
        bytes[8]=(byte)0xc0;   g1=new GUID(bytes);
        Assert.that(! g1.isWindowsGUID());
        bytes[8]=(byte)0x80;   g1=new GUID(bytes);
        Assert.that(g1.isWindowsGUID());
        
        //Try isNewGUID (again)
        bytes[8]=(byte)0xFE;   g1=new GUID(bytes);
        Assert.that(! g1.isNewGUID());
        bytes[8]=(byte)0xFF;  bytes[15]=(byte)0x00;   g1=new GUID(bytes);
        Assert.that(g1.isNewGUID());

        //Try isLimeGUID (again)
        bytes[9]=(byte)0xFF;   g1=new GUID(bytes);
        Assert.that(! g1.isLimeGUID());
        bytes[4]=(byte)0x02;
        bytes[5]=(byte)0x01;
        bytes[6]=(byte)0x00;        
        bytes[7]=(byte)0x05;
        //Note everything is LITTLE endian.
        //(0x0102+2)*(0x0500+3)=0x0104*0x0503=0x5170C ==> 0x0517
        bytes[9]=(byte)0x17;
        bytes[10]=(byte)0x05;
        g1=new GUID(bytes);
        short s1=ByteOrder.leb2short(bytes, 4);
        short s2=ByteOrder.leb2short(bytes, 6);
        short tag=tag(s1, s2);
        Assert.that(s1==(short)0x0102, Integer.toHexString(s1));
        Assert.that(s2==(short)0x0500, Integer.toHexString(s2));
        Assert.that(tag==(short)0x0517, Integer.toHexString(tag));
        Assert.that(g1.isLimeGUID());
        System.out.println(g1);

        // Test isLimeRequeryGUID
        bytes[0]=(byte)0x02;
        bytes[1]=(byte)0x01;
        // bytes[9]=(byte)0x17;
        // bytes[10]=(byte)0x05;
        //Note everything is LITTLE endian.
        //(0x0102+2)*(0x0517+3)=0x0104*0x051A=0x52E68 ==> 0x052E
        bytes[13]=(byte)0x2E;
        bytes[14]=(byte)0x05;
        g1 = new GUID(bytes);
        s1=ByteOrder.leb2short(bytes, 0);
        s2=ByteOrder.leb2short(bytes, 9);
        tag=tag(s1,s2);
        System.out.println(g1);
        Assert.that(s1==(short)0x0102, Integer.toHexString(s1));
        Assert.that(s2==(short)0x0517, Integer.toHexString(s2));
        Assert.that(tag==(short)0x052E, Integer.toHexString(tag));
        Assert.that(g1.isLimeRequeryGUID() && g1.isLimeGUID());


        // Test LimeRequeryGUID construction....
        bytes = makeGuidRequery();
        GUID gReq = new GUID(bytes);
        System.out.println(gReq);
        Assert.that(gReq.isLimeGUID() && gReq.isLimeRequeryGUID());

        //Test hashcode, compareTo for same
        java.util.Random r=new java.util.Random();       
        b1=new byte[16];
        r.nextBytes(b1);
        b2=new byte[16];
        System.arraycopy(b1,0,b2,0,16);
        g1=new GUID(b1);
        g2=new GUID(b2);
        Assert.that(g1.compareTo(g2)==0);
        Assert.that(g2.compareTo(g1)==0);
        Assert.that(g1.hashCode()==g2.hashCode());
        System.out.println("Hash: "+Integer.toHexString(g1.hashCode()));

        b2[7]+=1;
        g2=new GUID(b2);
        Assert.that(g1.compareTo(g2)<0);
        Assert.that((new GUID.GUIDComparator()).compare(g1, g2) < 0);
        Assert.that((new GUID.GUIDComparator()).compare(g2, g1) > 0);
        Assert.that((new GUID.GUIDByteComparator()).compare(b1, b2) < 0);
        Assert.that((new GUID.GUIDByteComparator()).compare(b2, b1) > 0);
        Assert.that(g2.compareTo(g1)>0);
        Assert.that(g1.hashCode()!=g2.hashCode());  //not strictly REQUIRED
        System.out.println("Hash: "+Integer.toHexString(g2.hashCode()));
    }
    */
}
