package org.limewire.security;

import java.net.InetAddress;

/**
 * An interface that represents the embodiment of an algorithm and
 * secret key(s) used to generate QueryKeys.
 * 
 * A particular instance of a QueryKeyGenerator must obey the relation
 * checkKeyBytes(getKeyBytes(x, y), x, y) == true for all legal 
 * InetAddresses x and IP port numbers y.
 * 
 * Also, attackers have knowledge of the algorithms implemented here
 * and have the ability to query a host for getKeyBytes(x,y) for many
 * different (x,y) values.  It must be computationally infeasable for
 * an attacker within the lifetime of a given QueryKeyGenerator instance
 * to guess a byte array that satisfies checkKeyBytes(keyBytes, a,b) for
 * any (a,b) that the attacker does not control.  Otherwise, the Gnutella
 * network can be turned into a gigantic DDoS botnet.
 * 
 * Secure implementations likely use a cryptographically secure encryption
 * algorithm, message authentication code (keyed cryptographic message digest),
 * or a mathematical problem believed to be intractable (discrete log problem,
 * RSA problem, etc.)  Strait-forward use of a linear encryption algorithm such
 * as RC4/ARC4/MARC4 is completely insecure.
 */

public interface QueryKeyGenerator {
    /**
     * Uses secret keys to generate a byte array from an InetAddres and
     * a port number.
     */
    public byte[] getKeyBytes(InetAddress ip, int port);
    
    /**
     * Returns true if the algorithm and secret keys of this instance were
     * used to generate keyBytes from ip and port.
     * 
     */
    public boolean checkKeyBytes(byte[] keyBytes, InetAddress ip, int port);
}
