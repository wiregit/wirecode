package com.limegroup.gnutella.security;

import java.security.*;
import com.limegroup.gnutella.*;

public class SignatureVerifier {
    
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
            ErrorService.error(nsax);
            return false;
        } catch (InvalidKeyException ikx) {
            return false;
        } catch (SignatureException sx) {
            return false;
        } catch (ClassCastException ccx) {
            return false;
        }       
    }
    
}
