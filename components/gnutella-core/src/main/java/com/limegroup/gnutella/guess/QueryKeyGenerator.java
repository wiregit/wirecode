package com.limegroup.gnutella.guess;

import java.net.InetAddress;

/**
 * An interface that represents the embodiment of an algorithm and
 * secret key(s) used to generate QueryKeys.
 * 
 * A particular instance of a QueryKeyGenerator must always generate
 * the same output of getKeyBytes(InetAddress, int) for given inputs.
 * (In other words, the algorithm must be deterministic for a given
 * set of secret keys.)
 *
 * Secure implementations likely use a cryptographically secure encryption
 * algorithm, message authentication code (keyed cryptographic hash function),
 * or a mathematical problem believed to be intractable (discrete log problem,
 * RSA problem, etc.)  Strait-forward use of a linear encryption algorithm such
 * as RC4/ARC4/MARC4 is completely insecure.
 */

/**
 * Uses secret keys to generate a byte array from an InetAddres and
 * a port number.
 *
 */
/* package */ interface QueryKeyGenerator {
    public byte[] getKeyBytes(InetAddress ip, int port);
}
