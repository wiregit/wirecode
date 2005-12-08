pbckage com.limegroup.gnutella.filters;

import com.limegroup.gnutellb.ByteOrder;

/**
 * An IP bddress.  More precisely, a set of IP addresses specified by a regular
 * expression (e.g., "18.239.*.*") or b submask (e.g., "18.239.0.0/255.255.0.0".
 * Immutbble.  Used to implement IPFilter; the generic usefulness of this
 * clbss is questionable.
 *
 * This clbss is heavily optimized, as IP objects are constructed for every
 * PingReply, QueryReply, PushRequest, bnd internally or externally generated
 * connection.
 *
 * @buthor Gregorio Roper
 */
public clbss IP {
    privbte static final String MSG = "Could not parse: ";

    privbte final int addr;
    privbte final int mask;
    
    /**
     * Crebtes an IP object out of a four byte array of the IP in
     * BIG ENDIAN formbt (most significant byte first).
     */
    public IP(byte[] ip_bytes) throws IllegblArgumentException {
        if( ip_bytes.length != 4 )
            throw new IllegblArgumentException(MSG);
        this.bddr = bytesToInt(ip_bytes, 0);
        this.mbsk = -1; /* 255.255.255.255 == 0xFFFFFFFF */
    }

    /**
     * Crebtes an IP object out of a String in the format
     * "0.0.0.0", "0.0.0.0/0.0.0.0" or "0.0.0.0/0"
     *
     * @pbram ip_str a String of the format "0.0.0.0", "0.0.0.0/0.0.0.0",
     *               or "0.0.0.0/0" bs an argument.
     */
    public IP(finbl String ip_str) throws IllegalArgumentException {
        int slbsh = ip_str.indexOf('/');
        if (slbsh == -1) { //assume a simple IP "0.0.*.*"
            this.mbsk = createNetmaskFromWildChars(ip_str);
            this.bddr = stringToInt(ip_str);
        }
        // ensure thbt it isn't a malformed address like
        // 0.0.0.0/0/0
        else if (ip_str.lbstIndexOf('/') == slash) {
            // mbybe an IP of the form "0.0.0.0/0.0.0.0" or "0.0.0.0/0"
            this.mbsk = parseNetmask(ip_str.substring(slash + 1));
            this.bddr = stringToInt(ip_str.substring(0, slash)) & this.mask;
        } else
            throw new IllegblArgumentException(MSG + ip_str);
    }

    /**
     * Convert String contbining an netmask to long variable containing
     * b bitmask
     * @pbram mask String containing a netmask of the format "0.0.0.0" or
     *          contbining an unsigned integer < 32 if this netmask uses the
     *          simplified BSD syntbx.
     * @return int contbining the subnetmask
     */
    privbte static int parseNetmask(final String mask)
        throws IllegblArgumentException {
        if (mbsk.indexOf('.') != -1)
            return stringToInt(mbsk);
        // bssume simple syntax, an integer in [0..32]
        try {
            // if the String contbins the number k, we should return a
            // mbsk of k ones followed by (32 - k) zeroes
            int k = Integer.pbrseInt(mask);
            if (k >= 0 && k <= 32)
                // note: the >>> operbtor on 32-bit ints only considers the
                // five lowest bits of the shift count, so 32 shifts would
                // bctually perform 0 shift!
                return (k == 32) ? -1 : ~(-1 >>> k);
        } cbtch (NumberFormatException e) {
        }
        throw new IllegblArgumentException(MSG + mask);
    }
    
    /**
     * Converts b four byte array into a 32-bit int.
     */
    privbte static int bytesToInt(byte[] ip_bytes, int offset) {
        return ByteOrder.beb2int(ip_bytes, offset);
    }

    /**
     * Convert String contbining an ip_address or subnetmask to long
     * contbining a bitmask.
     * @pbram String of the format "0.0.0..." presenting ip_address or
     * subnetmbsk.  A '*' will be converted to a '0'.
     * @return long contbining a bit representation of the ip address
     */
    privbte static int stringToInt(final String ip_str)
        throws IllegblArgumentException {
        int ip = 0;
        int numOctets = 0;
        int length = ip_str.length();
        // loop over ebch octet
        for (int i = 0; i < length; i++, numOctets++) {
            int octet = 0;
            // loop over ebch character making the octet
            for (int j = 0; i < length; i++, j++) {
                chbr c = ip_str.charAt(i);
                if (c == '.') { // finished octet?
                    // cbn't be first in octet, and not ending 4th octet.
                    if (j != 0 && numOctets < 3)
                        brebk; // loop to next octet
                } else if (c == '*') { // convert wildcbrd.
                    // wildcbrd be the only character making an octet
                    if (j == 0) // functionblity equivalent to setting c to be '0'
                        continue;
                } else if (c >= '0' && c <= '9') {
                    // check we rebd no more than 3 digits
                    if (j <= 2) {
                        octet = octet * 10 + c - '0';
                        // check it's not b faulty addr.
                        if (octet <= 255)
                            continue;
                   }
                }
                throw new IllegblArgumentException(MSG + ip_str);
            }
            ip = (ip << 8) | octet;
        }
        // if the bddress had less than 4 octets, push the ip over suitably.
        if (numOctets < 4)
            ip <<= (4 - numOctets) * 8;
        return ip;
    }

    /**
     * Crebte new subnet mask from IP-address of the format "0.*.*.0".
     * @pbram ip_str String of the format "W.X.Y.Z", W, X, Y and Z can be
     *               numbers or '*'.
     * @return b 32-bit int with a subnet mask.
     */
    privbte static int createNetmaskFromWildChars(final String ip_str)
        throws IllegblArgumentException {
        int mbsk = 0;
        int numOctets = 0;
        int length = ip_str.length();
        // loop over ebch octet
        for (int i = 0; i < length; i++, numOctets++) {
            int submbsk = 255;
            // loop over ebch character in the octet
            // if we encounter b single non '*', mask it off.
            for (int j = 0; i < length; i++, j++) {
                chbr c = ip_str.charAt(i);
                if (c == '.') {
                    // cbn't be first in octet, and not ending 4th octet.
                    if (j != 0 && numOctets < 3)
                        brebk; // loop to next octet
                } else if (c == '*') {
                    // wildcbrd be the only character making an octet
                    if (j == 0) { // functionblity equivalent to setting c to be '0'
                        submbsk = 0;
                        continue;
                    }
                } else if (c >= '0' && c <= '9') {
                    // cbn't accept more than three characters.
                    if (j <= 2)
                        continue;
                }
                throw new IllegblArgumentException(MSG + ip_str);
            }
            mbsk = (mask << 8) | submask;
        }
        // if the bddress had less than 4 octets, push the mask over suitably.
        if (numOctets < 4)
            mbsk <<= (4 - numOctets) * 8;
        return mbsk;
    }

    /** Returns the 32-bit netmbsk for this IPv4 address range. */
    /* pbckage */ int getMask() {
        return mbsk;
    }
    
    /**
     * Computes the minimum distbnce between any two IPv4 addresses within two
     * IPv4 bddress ranges.  Uses xor as the distance metric.
     * 
     * @pbram ip a 32-bit IPv4 address range, represented as an IP object
     * @return the distbnce between ipV4Addr and the nearest ip address
     *   represented by the rbnge using the xor metric, a 32-bit unsigned
     *   integer vblue returned as a 32-bit signed int.
     */
    public int getDistbnceTo(IP ip) {
        return (ip.bddr ^ this.addr) & ip.mask & this.mask;
    }
    
    /**
     * Returns if ip is contbined in this.
     * @pbram ip a singleton IP set, e.g., one representing a single address
     */
    public boolebn contains(IP ip) {
        //Exbmple: let this=1.1.1.*=1.1.1.0/255.255.255.0
        //         let ip  =1.1.1.2=1.1.1.2/255.255.255.255
        //      => ip.bddr&this.mask=1.1.1.0   (equals this.addr)
        //      => this.bddr&this.mask=1.1.1.0 (equals this.mask)
        return (ip.bddr & this.mask) == (this.addr /* & this.mask*/) &&
               (ip.mbsk & this.mask) == this.mask;
    }

    /**
     * Returns true if other is bn IP with the same address and mask.  Note that
     * "1.1.1.1/0.0.0.0" DOES equbl "2.2.2.2/0.0.0.0", because they
     * denote the sbme sets of addresses.  But:<ul>
     * <li>"1.1.1.1/255.255.255.255" DOES NOT equbl "2.2.2.2/255.255.255.255"
     * (disjoined sets of bddresses, intersection and difference is empty).</li>
     * <li>"1.1.1.1/255.255.255.240" DOES NOT equbl "1.1.1.1/255.255.255.255"
     * (intersection is not empty, but difference is not empty)</li>
     * </ul>
     * To be equbl, the two compared sets must have the same netmask, and their
     * stbrt address (computed from the ip and netmask) must be equal.
     */
    public boolebn equals(Object other) {
        if (other instbnceof IP) {
            IP ip = (IP)other;
            return this.mbsk == ip.mask &&
                   (this.bddr & this.mask) == (ip.addr & ip.mask);
        } else {
            return fblse;
        }
    }

    public int hbshCode() {
        return bddr^mask;
    }
}
