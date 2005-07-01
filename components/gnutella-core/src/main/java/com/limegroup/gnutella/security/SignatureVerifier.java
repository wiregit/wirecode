package com.limegroup.gnutella.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.KeyFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.io.UnsupportedEncodingException;
import java.io.File;

import com.limegroup.gnutella.util.FileUtils;
import com.bitzi.util.Base32;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class SignatureVerifier {
    
    private static final Log LOG = LogFactory.getLog(SignatureVerifier.class);
    
    private final byte[] plainText;
    private final byte[] signature;
    private final PublicKey publicKey;
    private final String algorithm;
    private final String digAlg;

    public SignatureVerifier(byte[] pText, byte[] sigBytes, PublicKey key, 
                             String algorithm) {
        this(pText, sigBytes, key, algorithm, null);
    }

    public SignatureVerifier(byte[] pText, byte[] sigBytes, PublicKey key, 
                             String algorithm, String digAlg) {
        this.plainText = pText;
        this.signature = sigBytes;
        this.publicKey = key;
        this.algorithm = algorithm;
        this.digAlg = digAlg;
    }
    
    public String toString() {
        //String alg = digAlg == null ? algorithm : digAlg + "with" + algorithm;
        return "text: " + new String(plainText) + ", sig: " + new String(signature) + 
               ", key: " + publicKey + ", alg: " + algorithm + ", digAlg: " + digAlg;
    }
    
    public boolean verifySignature() {
        String alg = digAlg == null ? algorithm : digAlg + "with" + algorithm;
        try {
            Signature verifier = Signature.getInstance(alg);
            verifier.initVerify(publicKey);
            verifier.update(plainText,0, plainText.length);
            return verifier.verify(signature);            
        } catch (NoSuchAlgorithmException nsax) {
            LOG.error("No alg." + this, nsax);
            return false;
        } catch (InvalidKeyException ikx) {
            LOG.error("Invalid key. " + this, ikx);
            return false;
        } catch (SignatureException sx) {
            LOG.error("Bad sig." + this, sx);
            return false;
        } catch (ClassCastException ccx) {
            LOG.error("bad cast." + this, ccx);
            return false;
        }       
    }

    /**
     * Retrieves the data from a byte[] containing both the signature & content,
     * returning the data only if it is verified.
     */
    public static String getVerifiedData(byte[] data, File keyFile, String alg, String dig) {
        PublicKey key = readKey(keyFile, alg);
        byte[][] info = parseData(data);
        return verify(key, info, alg, dig);
    }
    
    /**
     * Retrieves the data from a file, returning the data only if it is verified.
     */
    public static String getVerifiedData(File source, File keyFile, String alg, String dig) {
        PublicKey key = readKey(keyFile, alg);
        byte[][] info = parseData(FileUtils.readFileFully(source));
        return verify(key, info, alg, dig);
     }
    
    /**
     * Verified the key, info, using the algorithm & digest algorithm.
     */
    private static String verify(PublicKey key, byte[][] info, String alg, String dig) {
        if(key == null || info == null) {
            LOG.warn("No key or data to verify.");
            return null;
        }
            
        SignatureVerifier sv = new SignatureVerifier(info[1], info[0], key, alg, dig);
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
    private static PublicKey readKey(File keyFile, String alg) {
        byte[] fileData = FileUtils.readFileFully(keyFile);
        if(fileData == null)
            return null;
            
        try {
            EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Base32.decode(new String(fileData)));
            KeyFactory kf = KeyFactory.getInstance(alg);
            PublicKey key = kf.generatePublic(pubKeySpec);
            return key;
        } catch(NoSuchAlgorithmException nsae) {
            LOG.error("Invalid algorithm: " + alg, nsae);
            return null;
        } catch(InvalidKeySpecException ikse) {
            LOG.error("Invalid keyspec: " + keyFile, ikse);
            return null;
        }
    }
    
    /**
     * Parses data, returning the signature & content.
     */
    private static byte[][] parseData(byte[] data) {
        if(data == null) {
            LOG.warn("No data to parse.");
            return null;
        }
        
        // look for the separator between sig & data.
        int i = findPipes(data);
        if(i == -1 || i >= data.length - 3) {
            LOG.warn("Couldn't find pipes.");
            return null;
        }
            
        byte[] sig = new byte[i];
        byte[] content = new byte[data.length - i - 2];
        System.arraycopy(data, 0, sig, 0, sig.length);
        System.arraycopy(data, i+2, content, 0, content.length);
        return new byte[][] { Base32.decode(new String(sig)), content };
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
