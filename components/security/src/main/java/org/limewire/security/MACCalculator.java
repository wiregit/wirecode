package org.limewire.security;

import org.limewire.security.SecurityToken.TokenData;

/**
 * An interface that represents the embodiment of an algorithm and
 * secret key(s) used to generate SecurityTokens.
 * 
 * Also, attackers have knowledge of the algorithms implemented here
 * and have the ability to query a host for getTokenBytes(TokenData data) for many
 * different values.  It must be computationally infeasable for
 * an attacker within the lifetime of a given MACCalculator instance
 * to guess a byte array that satisfies {@link SecurityToken#isFor(TokenData)} for
 * any TokenData value that the attacker does not control.  Otherwise, the Gnutella
 * network can be turned into a gigantic DDoS botnet.
 * 
 * Secure implementations likely use a cryptographically secure encryption
 * algorithm, message authentication code (keyed cryptographic message digest),
 * or a mathematical problem believed to be intractable (discrete log problem,
 * RSA problem, etc.)  Strait-forward use of a linear encryption algorithm such
 * as RC4/ARC4/MARC4 is completely insecure.
 */
public interface MACCalculator {
    /**
     * Uses secret keys to generate a byte array from an InetAddres and
     * a port number.
     */
    public byte[] getMACBytes(TokenData data);
}
