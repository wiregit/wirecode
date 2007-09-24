package org.limewire.security;

import java.io.File;
import java.security.PublicKey;
import java.util.concurrent.ExecutorService;

import org.limewire.concurrent.ExecutorsHelper;

import com.google.inject.Singleton;

/** A class that verifies secure messages sequentially. */
@Singleton
public class SecureMessageVerifier {
    
    private final ExecutorService QUEUE;
    
    /** The public key. */
    private PublicKey pubKey;
    
    /** The File the public key is stored in. */
    private final File keyFile;
    
    /** The base32-encoded key */
    private final String keyBase32;
    
    public SecureMessageVerifier() {
        this(null, null, null);
    }
    
    public SecureMessageVerifier(String name) {
        this(null, null, name);
    }

    public SecureMessageVerifier(File keyFile) {
        this(keyFile, null, null);
    }
    
    public SecureMessageVerifier(String keyBase32, String name) {
        this(null, keyBase32, name);
    }

    public SecureMessageVerifier(File keyFile, String name) {
        this(keyFile, null, name);
    }
    
    private SecureMessageVerifier(File keyFile, String keyBase32, String name) {
        if (name == null) {
            QUEUE = ExecutorsHelper.newProcessingQueue("SecureMessageVerifier");
        } else {
            QUEUE = ExecutorsHelper.newProcessingQueue(name + "-SecureMessageVerifier");
        }
        
        if ((keyFile == null) == (keyBase32 == null))
            throw new IllegalArgumentException("must have only one source of key");
        
        this.keyFile = keyFile;
        this.keyBase32 = keyBase32;
    }
    
    /** Queues this SecureMessage for verification.  The callback will be notified of success or failure. */
    public void verify(SecureMessage sm, SecureMessageCallback smc) {
        QUEUE.execute(new VerifierImpl(pubKey, "SHA1withDSA", sm, smc));
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
        
        QUEUE.execute(new VerifierImpl(pubKey, algorithm, sm, smc));
    }
    
    /**
     * Enqueues a custom Verifier
     */
    public void verify(Verifier verifier) {
        QUEUE.execute(verifier);
    }
    
    /** Initializes the public key if one isn't set. */
    private void initializePublicKey() {
        if(pubKey == null) {
            pubKey = createPublicKey();
        }
    }
    
    /** Creates the public key. */
    protected PublicKey createPublicKey() {
        if(getKeyFile() == null && keyBase32 == null)
            throw new NullPointerException("no key source!!");
        
        // prefer string-based keys
        if (keyBase32 != null)
            return SignatureVerifier.readKey(keyBase32, "DSA");
        
        return SignatureVerifier.readKey(getKeyFile(), "DSA");
    }
    
    /** Gets the file holding the key. */
    protected File getKeyFile() {
        return keyFile;
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
