package com.limegroup.gnutella.privategroups;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * This class was originally used to generate RSA keys - it is not used anymore
 */
public class RSA {
    private final BigInteger one      = new BigInteger("1");
    private final SecureRandom random = new SecureRandom();

    private BigInteger privateKey;
    private BigInteger publicKey;
    private BigInteger modulus;

    // generate an N-bit (roughly) public and private key

    public RSA(int N) {
       BigInteger p = BigInteger.probablePrime(N/2, random);
       BigInteger q = BigInteger.probablePrime(N/2, random);
       BigInteger phi = (p.subtract(one)).multiply(q.subtract(one));

       this.modulus    = p.multiply(q);                                  
       this.publicKey  = BigInteger.probablePrime(10/2, random);     // common value in practice = 2^16 + 1
       this.privateKey = publicKey.modInverse(phi);
    }

    
    public BigInteger getModulus(){
        return modulus;
    }
    
    public BigInteger getPublicKey(){
        return publicKey;
    }
    
    public BigInteger getPrivateKey(){
        return privateKey;
    }
    
    public BigInteger encrypt(BigInteger message, BigInteger publicKey, BigInteger modulus) {
       return message.modPow(publicKey, modulus);
    }

    public BigInteger decrypt(BigInteger encrypted, BigInteger privateKey, BigInteger modulus) {
       return encrypted.modPow(privateKey, modulus);
    }

    public String toString() {
       String s = "";
       s += "public  = " + publicKey  + "\n";
       s += "private = " + privateKey + "\n";
       s += "modulus = " + modulus;
       return s;
    }
}