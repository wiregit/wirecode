package com.limegroup.gnutella.encryption;

import java.math.*;
import java.util.Random;
import com.limegroup.gnutella.*;

/**
 * Generates a Random number of the number of 128 bits -- the strenght of 
 * encryption we want to support, and generates the a key to send.
 * <p>
 * Has a method to take as input the key from the other end.
 * <p>
 * Generates the symetric key after it has both the keys.
 *
 * @author
 */
public class DiffieHellmanKeyNegotiator {

    //implementation
    private int _keyStrengthInBits;
    private BigInteger _myRandomNumber;
    static final BigInteger N = new BigInteger("DE9B707D4C5A4633C0290C95FF30A605AEB7AE864FF48370F13CF01D49ADB9F23D19A439F753EE7703CF342D87F431105C843C78CA4DF639931F3458FAE8A94D1687E99A76ED99D0BA87189F42FD31AD8262C54A8CF5914AE6C28C540D714A5F6087A171FB74F4814C6F968D72386EF356A05180C3BEC7DDD5EF6FE76B0531C3",16);
    private static final BigInteger G = new BigInteger("2");

    /**
     * @param keyStrengthInBits strength of the encryption
     */
    public DiffieHellmanKeyNegotiator(int keyStrengthInBits) {
        _keyStrengthInBits= keyStrengthInBits;
    }

    /**
     * Generates a random number x and calculates g^x % N
     */
    public byte[] keyForOtherSide() {
        int randomBytesRequired = _keyStrengthInBits / 8;
        Random gen = new Random(System.currentTimeMillis());//use time as seed
        byte[] randomBytes = new byte[randomBytesRequired];
        gen.nextBytes(randomBytes);
        _myRandomNumber = new BigInteger(randomBytes);
        return G.modPow(_myRandomNumber,N).toByteArray();
    }

    /**
     * Takes Y from the other end and generates the symmetric key
     */
    public byte[] generateSymmetricKey(byte[] otherKey) {
        BigInteger other = new BigInteger(otherKey);
        return other.modPow(_myRandomNumber,N).toByteArray();
    }
	//validation
}

