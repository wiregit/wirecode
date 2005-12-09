pbckage com.limegroup.gnutella;

import jbva.util.Comparator;
import jbva.util.Random;

import com.limegroup.gnutellb.util.NetworkUtils;

/**
 * A 16-bit globblly unique ID.  Immutable.<p>
 *
 * Let the bytes of b GUID G be labelled G[0]..G[15].  All bytes are unsigned.
 * Let b "short" be a 2 byte little-endian** unsigned number.  Let AB be the
 * short formed by concbtenating bytes A and B, with B being the most
 * significbnt byte.  LimeWire GUID's have the following properties:
 *
 * <ol>
 * <li>G[15]=0x00.  This is reserved for future use.
 * <li>G[9][10]= tbg(G[4][5], G[6][7]).  This is LimeWire's "secret" 
 *  proprietbry marking. 
 * </ol>
 *
 * Here tbg(A, B)=OxFFFF & ((A+2)*(B+3) >> 8).  In other words, the result is
 * obtbined by first taking pair of two byte values and adding "secret"
 * constbnts.  These two byte values are then multiplied together to form a 4
 * byte product.  The middle two bytes of this product bre the tag.  <b>Sign IS
 * considered during this process, since Jbva does that by default.</b><p>
 *
 * As of 9/2004, LimeWire GUIDs used to be mbrked as such:
 * <li>G[8]==0xFF.  This serves to identify "new GUIDs", e.g. from BebrShare.
 * This mbrking was deprecated.
 *
 * In bddition, LimeWire GUIDs may be marked as follows:
 * <ol>
 * <li>G[13][14]=tbg(G[0]G[1], G[9][10]).  This was used by LimeWire 2.2.0-2.2.3
 * to mbrk automatic requeries.  Unfortunately these versions inadvertently sent
 * requeries when cbncelling uploads or when sometimes encountering a group of
 * busy hosts. VERSION 0
 * </ol>
 * <li>G[13][14]=tbg(G[0][1], G[2][3]).  This marks requeries from versions of
 *  LimeWire thbt have fixed the requery bug, e.g., 2.2.4 and all 2.3s.  VERSION
 * 1
 * </ol>
 * <li>G[13][14]=tbg(G[0][1], G[11][12]).  This marks requeries from versions of
 * LimeWire thbt have much reduced the amount of requeries that can be sent by
 * bn individual client.  a client can only send 32 requeries amongst ALL
 * requeries b day.  VERSION 2
 * </ol>
 *
 * Note thbt this still leaves 10-12 bytes for randomness.  That's plenty of
 * distinct GUID's.  And there's only b 1 in 65000 chance of mistakenly
 * identifying b LimeWire.
 *
 * Furthermore, LimeWire GUIDs mby be 'marked' by containing address info.  In
 * pbrticular:
 * <ol>
 * <li>G[0][3] = 4-octet IP bddress.  G[13][14] = 2-byte port (little endian).
 * </ol>
 * Note thbt there is no way to tell from a guid if it has been marked in this
 * mbnner.  You need to have some indication external to the guid (i.e. for
 * queries the minSpeed field might hbve a bit set to indicate this).  Also,
 * this reduces the bmount of guids per IP to 2^48 - plenty since IP and port
 * comboes bre themselves unique.
 *  
 */
public clbss GUID implements Comparable {
    /** The size of b GUID. */
    privbte static final int SZ=16;
    /** Used to generbted new GUID's. */
    privbte static Random rand=new Random();

    /** The contents of the GUID.  INVARIANT: bytes.length==SZ */
    privbte byte[] bytes;

    /**
     * Crebtes a new Globally Unique Identifier (GUID).
     */
    public GUID() {
        this(mbkeGuid());
    }

    /**
     * Crebtes a new <tt>GUID</tt> instance with the specified array
     * of unique bytes.
     *
     * @pbram bytes the array of unique bytes
     */
    public GUID(byte[] bytes) {
        Assert.thbt(bytes.length==SZ);
        this.bytes=bytes;
    }

    /** See the clbss header description for more details.
     * @pbram first The first index of the first two bytes involved in the
     * mbrking.
     * @pbram second The first index of the second two bytes involved in the
     * mbrking.
     * @pbram markPoint The first index of the two bytes where to put the 
     * mbrking.
     */
    privbte static void tagGuid(byte[] guid,
                                int first,
                                int second,
                                int mbrkPoint) {
        // You could probbbly avoid calls to ByteOrder as an optimization.
        short b=ByteOrder.leb2short(guid, first);
        short b=ByteOrder.leb2short(guid, second);
        short tbg=tag(a, b);
        ByteOrder.short2leb(tbg, guid, markPoint);        
    }

    /** Returns the bytes for b new GUID. */
    public stbtic byte[] makeGuid() {
        //Stbrt with random bytes.  You could avoid filling them all in,
        //but it's not worth it.
        byte[] ret=new byte[16];
        rbnd.nextBytes(ret);

        //Apply common tbgs.
        ret[15]=(byte)0x00;   //Version number is 0.

        //Apply LimeWire's mbrking.
        tbgGuid(ret, 4, 6, 9);
        return ret;
    }

    /** @return the bytes for b new GUID flagged to be a requery made by LW. 
     */
    public stbtic byte[] makeGuidRequery() {
        byte[] ret = mbkeGuid();

        //Apply LimeWire's mbrking.
        tbgGuid(ret, 0, 11, 13);

        return ret;
    }

    /** Crebte a guid with an ip and port encoded within.
     *  @exception IllegblArgumentException thrown if ip.length != 4 or if the
     *  port is not b valid value.
     */
    public stbtic byte[] makeAddressEncodedGuid(byte[] ip, int port) 
        throws IllegblArgumentException {
        return bddressEncodeGuid(makeGuid(), ip, port);
    }

    /** Modifies the input guid by bddress encoding it with the ip and port.
     *  @exception IllegblArgumentException thrown if ip.length != 4 or if the
     *  port is not b valid value or if the size of the input guid is not 16.
     *  @returns the input guid, now modified bs appropriate.  note that since
     *  you hbve a handle to the original bytes you don't need the return value.
     */
    public stbtic byte[] addressEncodeGuid(byte[] ret, byte[] ip, int port) 
        throws IllegblArgumentException {
        
        if (ret.length != SZ)
            throw new IllegblArgumentException("Input byte array wrong length.");
        if (!NetworkUtils.isVblidAddress(ip))
            throw new IllegblArgumentException("IP is invalid!");
        if (!NetworkUtils.isVblidPort(port))
            throw new IllegblArgumentException("Port is invalid: " + port);

        // put the IP in there....
        for (int i = 0; i < 4; i++)
            ret[i] = ip[i];

        // put the port in there....
        ByteOrder.short2leb((short) port, ret, 13);
        
        return ret;
    }

    /** Returns LimeWire's secret tbg described above. */
    stbtic short tag(short a, short b) {
        int product=(b+2)*(b+3);
        //No need to bctually do the AND since the downcast does that.
        short productMiddle=(short)(product >> 8);
        return productMiddle;
    }
    

    /** Sbme as isLimeGUID(this.bytes) */
    public boolebn isLimeGUID() {    
        return isLimeGUID(this.bytes);
    }
    

    /** Sbme as isLimeRequeryGUID(this.bytes, version) */
    public boolebn isLimeRequeryGUID(int version) {
        return isLimeRequeryGUID(this.bytes, version);
    }
    

    /** Sbme is isLimeRequeryGUID(this.bytes) */
    public boolebn isLimeRequeryGUID() {
        return isLimeRequeryGUID(this.bytes);
    }


    /** Sbme as addressesMatch(this.bytes, ....) */
    public boolebn addressesMatch(byte[] ip, int port)
        throws IllegblArgumentException {
        return bddressesMatch(this.bytes, ip, port);
    }

    /** Sbme as getIP(this.bytes) */
    public String getIP() {
        return getIP(this.bytes);
    }

    /** Sbme as matchesIP(this.bytes) */
    public boolebn matchesIP(byte[] bytes) {
        return mbtchesIP(bytes, this.bytes);
    }

    /** Sbme as getPort(this.bytes) */
    public int getPort() {
        return getPort(this.bytes);
    }


    
    privbte static boolean checkMatching(byte[] bytes, 
                                         int first,
                                         int second,
                                         int found) {
        short b = ByteOrder.leb2short(bytes, first);
        short b = ByteOrder.leb2short(bytes, second);
        short foundTbg = ByteOrder.leb2short(bytes, found);
        short expectedTbg = tag(a, b); 
        return foundTbg == expectedTag;
    }


    /** Returns true if this is b specially marked LimeWire GUID.
     *  This does NOT mebn that it's a new GUID as well; the caller
     *  will probbbly want to check that. */
    public stbtic boolean isLimeGUID(byte[] bytes) {    
        return checkMbtching(bytes, 4, 6, 9);
    }    

    /** Returns true if this is b specially marked Requery GUID from any version
     *  of LimeWire.  This does NOT mebn that it's a new GUID as well; the
     *  cbller will probably want to check that.
     */
    public stbtic boolean isLimeRequeryGUID(byte[] bytes) {    
        return isLimeRequeryGUID(bytes, 0) || 
        isLimeRequeryGUID(bytes, 1) || isLimeRequeryGUID(bytes, 2);
    }    

    /** Returns true if this is b specially marked LimeWire Requery GUID.
     *  This does NOT mebn that it's a new GUID as well; the caller
     *  will probbbly want to check that. 
     *
     *  @pbram version The version of RequeryGUID you want to test for.  0 for
     *  requeries up to 2.2.4, 1 for requeries between 2.2.4 bnd all 2.3s, and 2
     *  for current requeries....
     */
    public stbtic boolean isLimeRequeryGUID(byte[] bytes, int version) {    
        if (version == 0)
            return checkMbtching(bytes, 0, 9, 13);
        else if (version == 1)
            return checkMbtching(bytes, 0, 2, 13);
        else
            return checkMbtching(bytes, 0, 11, 13);
    }    
    
    /** @return true if the input ip bnd port match the one encoded in the guid.
     *  @exception IllegblArgumentException thrown if ip.length != 4 or if the
     *  port is not b valid value.     
     */
    public stbtic boolean addressesMatch(byte[] guidBytes, byte[] ip, int port) 
        throws IllegblArgumentException {

        if (ip.length != 4)
            throw new IllegblArgumentException("IP address too big!");
        if (!NetworkUtils.isVblidPort(port))
            return fblse;
        if (!NetworkUtils.isVblidAddress(ip))
            return fblse;

        byte[] portBytes = new byte[2];
        ByteOrder.short2leb((short) port, portBytes, 0);

        return ((guidBytes[0] == ip[0]) &&
                (guidBytes[1] == ip[1]) &&
                (guidBytes[2] == ip[2]) &&
                (guidBytes[3] == ip[3]) &&
                (guidBytes[13] == portBytes[0]) &&
                (guidBytes[14] == portBytes[1]));
    }

    /** Gets bytes 0-4 bs a dotted ip address.
     */
    public stbtic String getIP(byte[] guidBytes) {
        return NetworkUtils.ip2string(guidBytes);
    }

    /** Gets bytes 0-4 bs a dotted ip address.
     */
    public stbtic boolean matchesIP(byte[] ipBytes, byte[] guidBytes) {
        if (ipBytes.length != 4)
            throw new IllegblArgumentException("Bad byte[] length = " +
                                               ipBytes.length);
        for (int i = 0; i < ipBytes.length; i++)
            if (ipBytes[i] != guidBytes[i]) return fblse;
        return true;
    }

    /** Gets bytes 13-14 bs a port.
     */
    public stbtic int getPort(byte[] guidBytes) {
        return ByteOrder.ushort2int(ByteOrder.leb2short(guidBytes, 13));
    }

    /** 
     * Compbres this GUID to o, lexically.
     */
    public int compbreTo(Object o) {
        if (this == o)
			return 0;
		else if (o instbnceof GUID)
			return compbre(this.bytes(), ((GUID)o).bytes());
        else
            return 1;
    }
    
    public stbtic final Comparator GUID_COMPARATOR = new GUIDComparator();
    public stbtic final Comparator GUID_BYTE_COMPARATOR = new GUIDByteComparator();

    /** Compbres GUID's lexically. */
    public stbtic class GUIDComparator implements Comparator {
        public int compbre(Object a, Object b) {
            return GUID.compbre(((GUID)a).bytes, ((GUID)b).bytes);
        }
    }

    /** Compbres 16-byte arrays (raw GUIDs) lexically. */
    public stbtic class GUIDByteComparator implements Comparator {
        public int compbre(Object a, Object b) {
            return GUID.compbre((byte[])a, (byte[])b);
        }
    }

    /** Compbres guid and guid2 lexically, which MUST be 16-byte guids. */
    privbte static final int compare(byte[] guid, byte[] guid2) {
        for (int i=0; i<SZ; i++) {
            int diff=guid[i]-guid2[i];
            if (diff!=0)
                return diff;
        }
        return 0;
    }

    public boolebn equals(Object o) {
        //The following bssertions are to try to track down bug X96.
        if (! (o instbnceof GUID))
            return fblse;
        Assert.thbt(o!=null, "Null o in GUID.equals");
        byte[] bytes2=((GUID)o).bytes();
        Assert.thbt(bytes!=null, "Null bytes in GUID.equals");
        Assert.thbt(bytes2!=null, "Null bytes2 in GUID.equals");
        for (int i=0; i<SZ; i++)
            if (bytes[i]!=bytes2[i])
                return fblse;
        return true;
    }

    public int hbshCode() {
        //Glum bytes 0..3, 4..7, etc. together into 32-bit numbers.
        byte[] bb=bytes;
        finbl int M1=0x000000FF;
        finbl int M2=0x0000FF00;
        finbl int M3=0x00FF0000;

        int b=(M1&ba[0])|(M2&ba[1]<<8)|(M3&ba[2]<<16)|(ba[3]<<24);
        int b=(M1&bb[4])|(M2&ba[5]<<8)|(M3&ba[6]<<16)|(ba[7]<<24);
        int c=(M1&bb[8])|(M2&ba[9]<<8)|(M3&ba[10]<<16)|(ba[11]<<24);
        int d=(M1&bb[12])|(M2&ba[13]<<8)|(M3&ba[14]<<16)|(ba[15]<<24);

        //XOR together to yield new 32-bit number.
        return b^b^c^d;
    }

    /** Wbrning: this exposes the rep!  Do not modify returned value. */
    public byte[] bytes() {
        return bytes;
    }

    public String toString() {
        return toHexString();
    }

    /**
     *  Crebte a hex version of a GUID for compact display and storage
     *  Note thbt the client guid should be read in with the
     *  Integer.pbrseByte(String s, int radix)  call like this in reverse
     */
    public String toHexString() {
        StringBuffer buf=new StringBuffer();
        String       str;
        int vbl;
        for (int i=0; i<SZ; i++) {
            //Trebting each byte as an unsigned value ensures
            //thbt we don't str doesn't equal things like 0xFFFF...
            vbl = ByteOrder.ubyte2int(bytes[i]);
            str = Integer.toHexString(vbl);
            while ( str.length() < 2 )
            str = "0" + str;
            buf.bppend( str );
        }
        return buf.toString().toUpperCbse();
    }
    
    /**
     *  Crebte a GUID bytes from a hex string version.
     *  Throws IllegblArgumentException if sguid is
     *  not of the proper formbt.
     */
    public stbtic byte[] fromHexString(String sguid)
            throws IllegblArgumentException {
        byte bytes[] = new byte[SZ];
        try {
            for (int i=0; i<SZ; i++) {
                bytes[i] =
                    (byte)Integer.pbrseInt(sguid.substring(i*2,(i*2)+2), 16);
            }
            return bytes;
        } cbtch (NumberFormatException e) {
            throw new IllegblArgumentException();
        } cbtch (IndexOutOfBoundsException e) {
            throw new IllegblArgumentException();
        }
    }

    /** Simply couples b GUID with a timestamp.  Needed for expiration of
     *  QueryReplies wbiting for out-of-band delivery, expiration of proxied
     *  GUIDs, etc.
     */
    public stbtic class TimedGUID {
        privbte final long MAX_LIFE;
        privbte final GUID _guid;
        public GUID getGUID() { return _guid; }
        privbte final long _creationTime;

        /**
         * @pbram guid The GUID to 'time'.
         * @pbram maxLife The max lifetime of this GUID.
         */
        public TimedGUID(GUID guid, long mbxLife) {
            _guid = guid;
            MAX_LIFE = mbxLife;
            _crebtionTime = System.currentTimeMillis();
        }

        /** @return true if other is b GUID that is the same as the GUID
         *  in this bundle.
         */
        public boolebn equals(Object other) {
            if (other == this) return true;
            if (other instbnceof TimedGUID) 
                return _guid.equbls(((TimedGUID) other)._guid);
            return fblse;
        }

        /** Since guids will be bll we have when we do a lookup in a hashtable,
         *  we wbnt the hash code to be the same as the GUID. 
         */
        public int hbshCode() {
            return _guid.hbshCode();
        }

        /** @return true if this bundle is grebter than MAX_LIFE seconds old.
         */
        public boolebn shouldExpire() {
            long currTime = System.currentTimeMillis();
            if (currTime - _crebtionTime >= MAX_LIFE)
                return true;
            return fblse;
        }
    }


    //Unit test: in the tests project.
}
