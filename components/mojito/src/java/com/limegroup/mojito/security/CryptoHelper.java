/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package com.limegroup.mojito.security;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.zip.GZIPInputStream;

/**
 * 
 */
public final class CryptoHelper {
    
    public static final String KEY_ALGORITHM = "DSA";
    public static final int KEY_SIZE = 512;
    
    public static final String SIGNATURE_ALGORITHM = "SHA1withDSA";
    
    private static Signature SIGNATURE;
    
    private CryptoHelper() {}
    
    public static PublicKey loadMasterKey(File file) 
            throws IOException, SignatureException, InvalidKeyException {
        
        FileInputStream fis = null;
        GZIPInputStream gz = null;
        DataInputStream in = null;
        try {
            fis = new FileInputStream(file);
            gz = new GZIPInputStream(fis);
            in = new DataInputStream(gz);
            
            byte[] signature = new byte[in.readInt()];
            in.readFully(signature);
            
            byte[] encodedKey = new byte[in.readInt()];
            in.readFully(encodedKey);
            
            PublicKey pubKey = createPublicKey(encodedKey);
            if (!verify(pubKey, signature, encodedKey)) {
                throw new SignatureVerificationException();
            }
            return pubKey;
        } finally {
            if (in != null) { in.close(); }
        }
    }
    
    public static KeyPair createKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            kpg.initialize(KEY_SIZE);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException err) {
            throw new RuntimeException(err);
        }
    }
    
    public static PublicKey createPublicKey(byte[] encodedKey) {
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
            KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
            return factory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException err) {
            throw new RuntimeException(err);
        } catch (InvalidKeySpecException err) {
            throw new RuntimeException(err);
        }
    }
    
    public static Signature createSignSignature(PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            return signature;
        } catch (InvalidKeyException err) {
            throw new RuntimeException(err);
        } catch (NoSuchAlgorithmException err) {
            throw new RuntimeException(err);
        }
    }
    
    public static Signature createVerifySignature(PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            return signature;
        } catch (InvalidKeyException err) {
            throw new RuntimeException(err);
        } catch (NoSuchAlgorithmException err) {
            throw new RuntimeException(err);
        }
    }
    
    public static synchronized byte[] sign(PrivateKey privateKey, byte[]... data)
            throws SignatureException, InvalidKeyException {
        
        try {
            if (SIGNATURE == null) {
                SIGNATURE = Signature.getInstance(SIGNATURE_ALGORITHM);
            }
            
            SIGNATURE.initSign(privateKey);
            
            for(byte[] d : data) {
                SIGNATURE.update(d, 0, d.length);
            }
            
            return SIGNATURE.sign();
        } catch (NoSuchAlgorithmException err) {
            throw new RuntimeException(err);
        }
    }

    public static synchronized boolean verify(PublicKey publicKey, byte[] signature, byte[]... data) 
            throws SignatureException, InvalidKeyException {
        
        if (signature == null) {
            return false;
        }
        
        try {
            if (SIGNATURE == null) {
                SIGNATURE = Signature.getInstance(SIGNATURE_ALGORITHM);
            }
            
            SIGNATURE.initVerify(publicKey);
            
            for(byte[] d : data) {
                SIGNATURE.update(d, 0, d.length);
            }
            
            return SIGNATURE.verify(signature);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
