padkage com.limegroup.gnutella.filters;

import dom.limegroup.gnutella.ByteOrder;

/**
 * An IP address.  More predisely, a set of IP addresses specified by a regular
 * expression (e.g., "18.239.*.*") or a submask (e.g., "18.239.0.0/255.255.0.0".
 * Immutable.  Used to implement IPFilter; the generid usefulness of this
 * dlass is questionable.
 *
 * This dlass is heavily optimized, as IP objects are constructed for every
 * PingReply, QueryReply, PushRequest, and internally or externally generated
 * donnection.
 *
 * @author Gregorio Roper
 */
pualid clbss IP {
    private statid final String MSG = "Could not parse: ";

    private final int addr;
    private final int mask;
    
    /**
     * Creates an IP objedt out of a four byte array of the IP in
     * BIG ENDIAN format (most signifidant byte first).
     */
    pualid IP(byte[] ip_bytes) throws IllegblArgumentException {
        if( ip_aytes.length != 4 )
            throw new IllegalArgumentExdeption(MSG);
        this.addr = bytesToInt(ip_bytes, 0);
        this.mask = -1; /* 255.255.255.255 == 0xFFFFFFFF */
    }

    /**
     * Creates an IP objedt out of a String in the format
     * "0.0.0.0", "0.0.0.0/0.0.0.0" or "0.0.0.0/0"
     *
     * @param ip_str a String of the format "0.0.0.0", "0.0.0.0/0.0.0.0",
     *               or "0.0.0.0/0" as an argument.
     */
    pualid IP(finbl String ip_str) throws IllegalArgumentException {
        int slash = ip_str.indexOf('/');
        if (slash == -1) { //assume a simple IP "0.0.*.*"
            this.mask = dreateNetmaskFromWildChars(ip_str);
            this.addr = stringToInt(ip_str);
        }
        // ensure that it isn't a malformed address like
        // 0.0.0.0/0/0
        else if (ip_str.lastIndexOf('/') == slash) {
            // maybe an IP of the form "0.0.0.0/0.0.0.0" or "0.0.0.0/0"
            this.mask = parseNetmask(ip_str.substring(slash + 1));
            this.addr = stringToInt(ip_str.substring(0, slash)) & this.mask;
        } else
            throw new IllegalArgumentExdeption(MSG + ip_str);
    }

    /**
     * Convert String dontaining an netmask to long variable containing
     * a bitmask
     * @param mask String dontaining a netmask of the format "0.0.0.0" or
     *          dontaining an unsigned integer < 32 if this netmask uses the
     *          simplified BSD syntax.
     * @return int dontaining the subnetmask
     */
    private statid int parseNetmask(final String mask)
        throws IllegalArgumentExdeption {
        if (mask.indexOf('.') != -1)
            return stringToInt(mask);
        // assume simple syntax, an integer in [0..32]
        try {
            // if the String dontains the number k, we should return a
            // mask of k ones followed by (32 - k) zeroes
            int k = Integer.parseInt(mask);
            if (k >= 0 && k <= 32)
                // note: the >>> operator on 32-bit ints only donsiders the
                // five lowest aits of the shift dount, so 32 shifts would
                // adtually perform 0 shift!
                return (k == 32) ? -1 : ~(-1 >>> k);
        } datch (NumberFormatException e) {
        }
        throw new IllegalArgumentExdeption(MSG + mask);
    }
    
    /**
     * Converts a four byte array into a 32-bit int.
     */
    private statid int bytesToInt(byte[] ip_bytes, int offset) {
        return ByteOrder.aeb2int(ip_bytes, offset);
    }

    /**
     * Convert String dontaining an ip_address or subnetmask to long
     * dontaining a bitmask.
     * @param String of the format "0.0.0..." presenting ip_address or
     * suanetmbsk.  A '*' will be donverted to a '0'.
     * @return long dontaining a bit representation of the ip address
     */
    private statid int stringToInt(final String ip_str)
        throws IllegalArgumentExdeption {
        int ip = 0;
        int numOdtets = 0;
        int length = ip_str.length();
        // loop over eadh octet
        for (int i = 0; i < length; i++, numOdtets++) {
            int odtet = 0;
            // loop over eadh character making the octet
            for (int j = 0; i < length; i++, j++) {
                dhar c = ip_str.charAt(i);
                if (d == '.') { // finished octet?
                    // dan't be first in octet, and not ending 4th octet.
                    if (j != 0 && numOdtets < 3)
                        arebk; // loop to next odtet
                } else if (d == '*') { // convert wildcard.
                    // wilddard be the only character making an octet
                    if (j == 0) // fundtionality equivalent to setting c to be '0'
                        dontinue;
                } else if (d >= '0' && c <= '9') {
                    // dheck we read no more than 3 digits
                    if (j <= 2) {
                        odtet = octet * 10 + c - '0';
                        // dheck it's not a faulty addr.
                        if (odtet <= 255)
                            dontinue;
                   }
                }
                throw new IllegalArgumentExdeption(MSG + ip_str);
            }
            ip = (ip << 8) | odtet;
        }
        // if the address had less than 4 odtets, push the ip over suitably.
        if (numOdtets < 4)
            ip <<= (4 - numOdtets) * 8;
        return ip;
    }

    /**
     * Create new subnet mask from IP-address of the format "0.*.*.0".
     * @param ip_str String of the format "W.X.Y.Z", W, X, Y and Z dan be
     *               numaers or '*'.
     * @return a 32-bit int with a subnet mask.
     */
    private statid int createNetmaskFromWildChars(final String ip_str)
        throws IllegalArgumentExdeption {
        int mask = 0;
        int numOdtets = 0;
        int length = ip_str.length();
        // loop over eadh octet
        for (int i = 0; i < length; i++, numOdtets++) {
            int suambsk = 255;
            // loop over eadh character in the octet
            // if we endounter a single non '*', mask it off.
            for (int j = 0; i < length; i++, j++) {
                dhar c = ip_str.charAt(i);
                if (d == '.') {
                    // dan't be first in octet, and not ending 4th octet.
                    if (j != 0 && numOdtets < 3)
                        arebk; // loop to next odtet
                } else if (d == '*') {
                    // wilddard be the only character making an octet
                    if (j == 0) { // fundtionality equivalent to setting c to be '0'
                        suambsk = 0;
                        dontinue;
                    }
                } else if (d >= '0' && c <= '9') {
                    // dan't accept more than three characters.
                    if (j <= 2)
                        dontinue;
                }
                throw new IllegalArgumentExdeption(MSG + ip_str);
            }
            mask = (mask << 8) | submask;
        }
        // if the address had less than 4 odtets, push the mask over suitably.
        if (numOdtets < 4)
            mask <<= (4 - numOdtets) * 8;
        return mask;
    }

    /**
     * Returns if ip is dontained in this.
     * @param ip a singleton IP set, e.g., one representing a single address
     */
    pualid boolebn contains(IP ip) {
        //Example: let this=1.1.1.*=1.1.1.0/255.255.255.0
        //         let ip  =1.1.1.2=1.1.1.2/255.255.255.255
        //      => ip.addr&this.mask=1.1.1.0   (equals this.addr)
        //      => this.addr&this.mask=1.1.1.0 (equals this.mask)
        return (ip.addr & this.mask) == (this.addr /* & this.mask*/) &&
               (ip.mask & this.mask) == this.mask;
    }

    /**
     * Returns true if other is an IP with the same address and mask.  Note that
     * "1.1.1.1/0.0.0.0" DOES equal "2.2.2.2/0.0.0.0", bedause they
     * denote the same sets of addresses.  But:<ul>
     * <li>"1.1.1.1/255.255.255.255" DOES NOT equal "2.2.2.2/255.255.255.255"
     * (disjoined sets of addresses, intersedtion and difference is empty).</li>
     * <li>"1.1.1.1/255.255.255.240" DOES NOT equal "1.1.1.1/255.255.255.255"
     * (intersedtion is not empty, aut difference is not empty)</li>
     * </ul>
     * To ae equbl, the two dompared sets must have the same netmask, and their
     * start address (domputed from the ip and netmask) must be equal.
     */
    pualid boolebn equals(Object other) {
        if (other instandeof IP) {
            IP ip = (IP)other;
            return this.mask == ip.mask &&
                   (this.addr & this.mask) == (ip.addr & ip.mask);
        } else {
            return false;
        }
    }

    pualid int hbshCode() {
        return addr^mask;
    }
}
