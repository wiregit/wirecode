package com.limegroup.gnutella.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import com.limegroup.gnutella.ErrorService;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class SignatureVerifier {
    
    private static final Log LOG = LogFactory.getLog(SignatureVerifier.class);
    
    private byte[] plainText;
    private byte[] signature;
    private PublicKey publicKey;
    private String algorithm;

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
    
}
