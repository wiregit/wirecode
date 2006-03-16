/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.security;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class CryptoHelper {
    
    private static final String KEY_STORE = "JKS";
    
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
            if (!verify(pubKey, encodedKey, signature)) {
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
    
    public static Signature createSignature(KeyPair keyPair) {
        return createSignature(keyPair.getPrivate(), keyPair.getPublic());
    }
    
    public static Signature createSignature(PrivateKey privateKey) {
        return createSignature(privateKey, null);
    }
    
    public static Signature createSignature(PublicKey publicKey) {
        return createSignature(null, publicKey);
    }
    
    public static Signature createSignature(PrivateKey privateKey, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            if (privateKey != null) {
                signature.initSign(privateKey);
            }
            
            if (publicKey != null) {
                signature.initVerify(publicKey);
            }
            return signature;
        } catch (InvalidKeyException err) {
            throw new RuntimeException(err);
        } catch (NoSuchAlgorithmException err) {
            throw new RuntimeException(err);
        }
    }
    
    public static byte[] sign(KeyPair keyPair, byte[] data) 
            throws SignatureException, InvalidKeyException {
        return sign(keyPair.getPrivate(), data);
    }
    
    public static synchronized byte[] sign(PrivateKey privateKey, byte[] data) 
            throws SignatureException, InvalidKeyException {
        
        try {
            if (SIGNATURE == null) {
                SIGNATURE = Signature.getInstance(SIGNATURE_ALGORITHM);
            }
            
            SIGNATURE.initSign(privateKey);
            SIGNATURE.update(data, 0, data.length);
            return SIGNATURE.sign();
        } catch (NoSuchAlgorithmException err) {
            throw new RuntimeException(err);
        }
    }
    
    public static boolean verify(KeyPair keyPair, byte[] data, byte[] signature) 
            throws SignatureException, InvalidKeyException {
        return verify(keyPair.getPublic(), data, signature);
    }

    public static synchronized boolean verify(PublicKey publicKey, byte[] data, byte[] signature) 
            throws SignatureException, InvalidKeyException {
        
        if (signature == null) {
            return false;
        }
        
        try {
            if (SIGNATURE == null) {
                SIGNATURE = Signature.getInstance(SIGNATURE_ALGORITHM);
            }
            
            SIGNATURE.initVerify(publicKey);
            SIGNATURE.update(data, 0, data.length);
            return SIGNATURE.verify(signature);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static KeyPair load(File file, String alias, char[] password) 
            throws IOException, KeyStoreException, CertificateException, 
                NoSuchAlgorithmException, UnrecoverableKeyException {
        
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE);
            keyStore.load(in, password);
            
            PrivateKey privKey = (PrivateKey)keyStore.getKey(alias, password);
            Certificate cert = keyStore.getCertificate(alias);
            PublicKey pubKey = cert.getPublicKey();
            
            return new KeyPair(pubKey, privKey);
        } finally {
            if (in != null) { in.close(); }
        }
    }
    
    public static void main(String[] args) throws Exception {
        
        KeyPair keyPair = load(new File(args[0]), args[1], args[2].toCharArray());
        
        PublicKey pubKey = keyPair.getPublic();
        byte[] encodedKey = pubKey.getEncoded();
        byte[] signature = sign(keyPair, encodedKey);
        
        FileOutputStream fos = new FileOutputStream(new File("public.key"));
        GZIPOutputStream gz = new GZIPOutputStream(fos);
        DataOutputStream out = new DataOutputStream(gz);
        
        out.writeInt(signature.length);
        out.write(signature);
        
        out.writeInt(encodedKey.length);
        out.write(encodedKey);
        
        out.close();
        
        System.out.println(pubKey.equals(loadMasterKey(new File("public.key"))));
    }
}
