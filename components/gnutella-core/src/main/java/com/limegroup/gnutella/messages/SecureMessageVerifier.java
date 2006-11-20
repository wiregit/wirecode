package com.limegroup.gnutella.messages;

import java.io.File;
import java.security.PublicKey;

import com.limegroup.gnutella.security.SignatureVerifier;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.ProcessingQueue;

/** A class that verifies secure messages sequentially. */
public class SecureMessageVerifier {
    
    private final ProcessingQueue QUEUE;
    
    /** The public key. */
    private PublicKey pubKey;
    
    public SecureMessageVerifier() {
        QUEUE = new ProcessingQueue("SecureMessageVerifier");
    }
    
    public SecureMessageVerifier(String name) {
        QUEUE = new ProcessingQueue(name + "-SecureMessageVerifier");
    }
    
    /** Queues this SecureMessage for verification. The callback will be notified of success or failure. */
    public void verify(SecureMessage sm, SecureMessageCallback smc) {
        QUEUE.add(new VerifierImpl(pubKey, "SHA1withDSA", sm, smc));
    }
    
    /** 
     * Queues this SecureMessage for verification. The callback will 
     * be notified of success or failure.
     */
    public void verify(PublicKey pubKey, String algorithm, 
            SecureMessage sm, SecureMessageCallback smc) {
        
        // To avoid ambiguous results interpret a null pubKey
        // as an error. A null pubKey would otherwise result
        // in loading core's secureMessage.key
        if (pubKey == null) {
            throw new IllegalArgumentException("PublicKey is null");
        }
        
        QUEUE.add(new VerifierImpl(pubKey, algorithm, sm, smc));
    }
    
    /**
     * Enqueues a custom Verifier
     */
    public void verify(Verifier verifier) {
        QUEUE.add(verifier);
    }
    
    /** Initializes the public key if one isn't set. */
    private void initializePublicKey() {
        if(pubKey == null) {
            pubKey = createPublicKey();
        }
    }
    
    /** Creates the public key. */
    protected PublicKey createPublicKey() {
        return SignatureVerifier.readKey(getKeyFile(), "DSA");
    }
    
    /** Gets the file holding the key. */
    protected File getKeyFile() {
        return new File(CommonUtils.getUserSettingsDir(), "secureMessage.key");
    }

    /** Simple runnable to insert into the ProcessingQueue. */
    private class VerifierImpl extends Verifier {
        
        private PublicKey pubKey;
        private String algorithm;
        
        VerifierImpl(PublicKey pubKey, String algorithm, 
                SecureMessage message, SecureMessageCallback callback) {
            super(message, callback);
            
            this.pubKey = pubKey;
            this.algorithm = algorithm;
        }
        
        @Override
        public String getAlgorithm() {
            return algorithm;
        }

        @Override
        public PublicKey getPublicKey() {
            if(pubKey == null) {
                initializePublicKey();
                pubKey = SecureMessageVerifier.this.pubKey;
            }
            
            return pubKey;
        }
    }
}
