package com.limegroup.gnutella.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.io.File;

import com.limegroup.gnutella.util.IOUtils;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class SignatureVerifier {
    
    private static final Log LOG = LogFactory.getLog(SignatureVerifier.class);
    
    private final byte[] plainText;
    private final byte[] signature;
    private final PublicKey publicKey;
    private final String algorithm;

    public SignatureVerifier(byte[] pText, byte[] sigBytes, PublicKey key, 
                                             String algorithm) {
        this.plainText = pText;
        this.signature = sigBytes;
        this.publicKey = key;
        this.algorithm = algorithm;
    }
    
    public boolean verifySignature() {
        try {
            Signature verifier = Signature.getInstance(algorithm);
            verifier.initVerify(publicKey);
            verifier.update(plainText,0, plainText.length);
            return verifier.verify(signature);            
        } catch (NoSuchAlgorithmException nsax) {
            LOG.error("No alg", nsax);
            return false;
        } catch (InvalidKeyException ikx) {
            LOG.error("Invalid key", ikx);
            return false;
        } catch (SignatureException sx) {
            LOG.error("Bad sig", sx);
            return false;
        } catch (ClassCastException ccx) {
            LOG.error("bad cast", ccx);
            return false;
        }       
    }
    

    /**
     * Retrieves the data from a byte[] containing both the signature & content,
     * returning the data only if it is verified.
     */
    public static String getVerifiedData(byte[] data, File keyFile, String alg) {
        PublicKey key = readKey(keyFile);
        byte[][] info = parseData(data);
        return verify(key, info, alg);
    }
    
    /**
     * Retrieves the data from a file, returning the data only if it is verified.
     */
    public static String getVerifiedData(File source, File keyFile, String alg) {
        PublicKey key = readKey(keyFile);
        byte[][] info = readDataFile(source);
        return verify(key, info, alg);
     }
    
    /**
     * Verified the key, info, using the algorithm.
     */
    private static String verify(PublicKey key, byte[][] info, String alg) {
        if(key == null || info == null)
            return null;
            
        SignatureVerifier sv = new SignatureVerifier(info[0], info[1], key, alg);
        if(sv.verifySignature()) {
            try {
                return new String(info[1], "UTF-8");
            } catch(UnsupportedEncodingException uee) {
                return new String(info[1]);
            }
        } else {
            return null;
        }   
    }    
    
    /**
     * Reads a public key from disk.
     */
    private static PublicKey readKey(File keyFile) {
        ObjectInputStream ois = null;
        PublicKey key = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(keyFile));
            key = (PublicKey)ois.readObject();
        } catch(Throwable t) {
            LOG.warn("Unable to read public key: " + keyFile, t);
        } finally {
            IOUtils.close(ois);
        }
        
        return key;
    }
    
    /**
     * Reads a data file and returns the data & signature.
     */
    private static byte[][] readDataFile(File source) {
        RandomAccessFile raf = null;
        int length = (int)source.length();
        if(length <= 0)
            return null;
        byte[] data = new byte[length];
        try {
            raf = new RandomAccessFile(source, "r");
            raf.readFully(data);
        } catch(IOException ioe) {
            LOG.warn("Unable to read file: " + source, ioe);
            return null;
        } finally {
            IOUtils.close(raf);
        }
        
        return parseData(data);
    }
    
    /**
     * Parses data, returning the signature & content.
     */
    private static byte[][] parseData(byte[] data) {
        // look for the separator between sig & data.
        int i = findPipes(data);
        if(i == -1 || i >= data.length - 3)
            return null;
            
        byte[] sig = new byte[i];
        byte[] content = new byte[data.length - i - 2];
        System.arraycopy(data, 0, sig, 0, sig.length);
        System.arraycopy(data, i+2, content, 0, content.length);
        return new byte[][] { sig, content };
    }
    
    /**
     * @return the index of "|" starting from startIndex, -1 if none found in
     * this.data
     */
    private static int findPipes(byte[] data) {
        for(int i = 0 ; i < data.length-1; i++) {
            if(data[i] == (byte)124 && data[i+1] == (byte)124)
                return i;
        }
        
        return -1;
    }    
}
