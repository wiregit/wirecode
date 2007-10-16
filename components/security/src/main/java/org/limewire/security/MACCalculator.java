package org.limewire.security;

import org.limewire.security.SecurityToken.TokenData;

/**<p>
 * Defines the interface that represents the embodiment of an algorithm and
 * secret keys used to generate {@link SecurityToken SecurityTokens}.
 * </p><p>
 * Attackers have knowledge of the algorithms implemented here and have the
 * ability to query a host for <code>getMACBytes(TokenData)</code> for many
 * different values. Therefore, it must be computationally infeasible for an
 * attacker, within the lifetime of a given <code>MACCalculator</code>
 * instance, to guess a byte array that satisfies
 * {@link SecurityToken#isFor(TokenData)} for any <code>TokenData</code> value
 * the attacker does not control. Otherwise, the Gnutella network can be turned
 * into a gigantic Distributed Denial-of-Service (DDoS) botnet.
 * </p>
 * Secure implementations likely use a cryptographically secure encryption
 * algorithm, message authentication code (keyed cryptographic message digest),
 * or a mathematical problem believed to be intractable (discrete log problem,
 * RSA problem, etc.) Straight-forward use of a linear encryption algorithm such
 * as RC4/ARC4/MARC4 is completely insecure.
 */
public interface MACCalculator {
    /**
     * Uses secret keys to generate a byte array from an InetAddres and
     * a port number.
     */
    public byte[] getMACBytes(TokenData data);
}
