package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.ByteOrder;

/**
 * An IP address.  More precisely, a set of IP addresses specified by a regular
 * expression (e.g., "18.239.*.*") or a submask (e.g., "18.239.0.0/255.255.0.0".
 * Immutable.  Used to implement IPFilter; the generic usefulness of this
 * class is questionable.
 *
 * This class is heavily optimized, as IP objects are constructed for every
 * PingReply, QueryReply, PushRequest, and internally or externally generated
 * connection.
 *
 * @author Gregorio Roper
 */
public class IP {
    private static final String MSG = "Could not parse: ";

    private final long addr;
    private final long mask;
    
    /**
     * Creates an IP object out of a four byte array of the IP in
     * BIG ENDIAN format (most significant byte first).
     */
    public IP(byte[] ip_bytes) throws IllegalArgumentException {
        if( ip_bytes.length != 4 )
            throw new IllegalArgumentException(MSG);
        this.addr = bytesToLong(ip_bytes, 0);
        this.mask = 0xFFFFFFFFL;
    }

    /**
     * Creates an IP object out of a String in the format
     * "0.0.0.0", "0.0.0.0/0.0.0.0" or "0.0.0.0/0"
     *
     * @param ip_str a String of the format "0.0.0.0", "0.0.0.0/0.0.0.0",
     *               or "0.0.0.0/0" as an argument.
     */
    public IP (final String ip_str) throws IllegalArgumentException {
        int slash = ip_str.indexOf("/");
        if ( slash == -1) { //assume a simple IP "0.0.*.*"
            this.addr = stringToLong(ip_str);
            this.mask = createNetmaskFromWildChars(ip_str);
        }
        // ensure that it isn't a malformed address like
        // 0.0.0.0/0/0
        else if ( ip_str.lastIndexOf("/") == slash ) {
            // maybe an IP of the form "0.0.0.0/0.0.0.0" or "0.0.0.0/0"
            this.addr = stringToLong(ip_str.substring(0, slash));
            this.mask = parseNetmask(ip_str.substring(slash+1));
        } else
            throw new IllegalArgumentException(MSG + ip_str);
    }

    /**
     * Convert String containing an netmask to long variable containing
     * a bitmask
     * @param mask String containing a netmask of the format "0.0.0.0" or
     *          containing an unsigned integer < 32 if this netmask uses the
     *          simplified bsd syntax.
     * @return long containing the subnetmask
     */
    private static long parseNetmask (final String mask)
        throws IllegalArgumentException {

        // assume simple syntax, an integer <= 32
        if (mask.indexOf(".") == -1) {
            try {
                // if the String contains the number k, we should return a
                // mask of k ones followed by (32 - k) zeroes
                short k = Short.parseShort(mask);
                if (k > 32 || k < 0)
                    throw new IllegalArgumentException (MSG + mask);
                else {
                    long netmask = 0;
                    for (int i = 0; i < k; i++) {
                        // move the bit first because we want
                        // the last 1 at position 0 of the long
                        // variable
                        netmask = netmask << 1;
                        netmask++;
                    }
                    netmask = netmask << (32 - k);
                    return netmask;
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException (MSG + mask);
            }
        } else
            return stringToLong(mask);
    }
    
    /**
     * Converts a four byte array into a long.
     */
    private static long bytesToLong(byte[] ip_bytes, int offset) {
        return ByteOrder.ubytes2long(ByteOrder.beb2int(ip_bytes, offset));
    }   

    /**
     * Convert String containing an ip_address or subnetmask to long
     * containing a bitmask.
     * @param String of the format "0.0.0..." presenting ip_address or
     * subnetmask.  A '*' will be converted to a '0'.
     * @return long containing a bit representation of the ip address
     */
    private static long stringToLong (final String ip_str)
        throws IllegalArgumentException {

        long ip = 0;
        int numOctets = 0;
        int length = ip_str.length();

        // loop over each octet
        for(int i = 0; i < length; i++, numOctets++) {
            short octet = 0;
            // loop over each character in the octet
            for(int j = 0; i < length; i++, j++) {
                char c = ip_str.charAt(i);
                
                // finished octet?
                if ( c == '.' ) {
                    if( j == 0 ) // cannot have 0..1.1
                        throw new IllegalArgumentException(MSG + ip_str);
                    else
                        break;
                }
                    
                // have we read more than 3 numeric characters?
                if ( j > 2 )
                    throw new IllegalArgumentException(MSG + ip_str);

                // convert wildcard.
                if( c == '*' ) {
                    // wildcard be the only entry in an octet
                    if( j != 0 )
                        throw new IllegalArgumentException(MSG + ip_str);
                    else // functionality equivilant to setting c to be '0' 
                        continue;
                } else if( c < '0' || c > '9' ) {
                    throw new IllegalArgumentException(MSG + ip_str);
                }

                octet = (short)(octet * 10 + c - '0');
                // faulty addr.
                if( octet > 255 || octet < 0 )
                    throw new IllegalArgumentException(MSG + ip_str);
            }
            ip = (ip << 8) + octet;
        }

        // if the address had less than 4 octets, push the ip over suitably..
        for(; numOctets < 4; numOctets++)
            ip <<= 8;

        // ensure that the ip address had 4 octets.
        if( numOctets != 4 )
            throw new IllegalArgumentException(MSG + ip_str);

         return ip;
    }

    /**
     * Create new subnet mask from IP-address of the format 0.*.*.0
     * @param String of the format W.X.Y.Z, W, X, Y and Z can be numbers or '*'
     * @return long with a subnet mask
     */
    private static long createNetmaskFromWildChars (final String s)
        throws IllegalArgumentException {

        long mask = 0;
        int numOctets = 0;
        int length = s.length();

        // loop over each octet
        for(int i = 0; i < length; i++, numOctets++) {
            short submask = 0;
            // loop over each character in the octet
            // if we encounter a single non '*', mask it off.
            for(int j = 0; i < length; i++, j++) {
                char c = s.charAt(i);
                if( c == '.' ) {
                    if( j == 0 )
                        throw new IllegalArgumentException(MSG + s);
                    else
                        break;
                }
                    
                // can't accept more than three characters.
                if( j > 2 )
                    throw new IllegalArgumentException(MSG + s);

                if( c != '*' )
                    submask = 0xff;
                else if ( j != 0 ) // canot accept wildcard past first char.
                    throw new IllegalArgumentException(MSG + s);
            }
            mask = (mask << 8) + submask;
        }

        // if the address had less than 4 octets, push the ip over suitably..
        for(; numOctets < 4; numOctets++)
            mask <<= 8;

        // ensure that the ip address had 4 octets.
        if( numOctets != 4 )
            throw new IllegalArgumentException(MSG + s);

        return mask;
    }

    /**
     * Returns if ip is contained in this.
     * @param ip a singleton IP set, e.g., one representing a single address
     */
    public boolean contains(IP ip) {
        //Example: let this=1.1.1.*=1.1.1.0/255.255.255.0
        //         let ip  =1.1.1.2=1.1.1.2/255.255.255.255
        //      => ip.addr&this.mask=1.1.1.0
        //      => this.addr&this.mask=1.1.1.0
        return ((ip.addr & this.mask) == (this.addr & this.mask));
    }

    /**
     * Returns true if other is an IP with the same address and mask.  Note that
     * "1.1.1.1/255.255.255.255" does not equal "2.2.2.2/255.255.255.255", even
     * though they denote the same sets of addresses.
     */
    public boolean equals(Object other) {
        if (other instanceof IP) {
            IP ip=(IP)other;
            return this.addr==ip.addr && this.mask==ip.mask;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return (int)(addr^mask);
    }
}
