package com.limegroup.gnutella.messages;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.security.SignatureVerifier;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.ProcessingQueue;

/** A class that verifies secure messages sequentially. */
public class SecureMessageVerifier {
    
    private static final Log LOG = LogFactory.getLog(SecureMessageVerifier.class);

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
        QUEUE.add(new Verifier(pubKey, "SHA1withDSA", sm, smc));
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
        
        QUEUE.add(new Verifier(pubKey, algorithm, sm, smc));
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
    private class Verifier implements Runnable {
        
        private PublicKey pubKey;
        private String algorithm;
        
        private SecureMessage message;
        private SecureMessageCallback callback;
        
        Verifier(PublicKey pubKey, String algorithm, 
                SecureMessage message, SecureMessageCallback callback) {
            
            this.pubKey = pubKey;
            this.algorithm = algorithm;
            this.message = message;
            this.callback = callback;
        }
        
        /** Does the verification. */
        public void run() {
            
            // See verify(SecureMessage, SecureMessageCallback)
            if(pubKey == null) {
                initializePublicKey();
                pubKey = SecureMessageVerifier.this.pubKey;
            }
            
            if(pubKey == null) {
                LOG.warn("Cannot verify message without a public key.");
                message.setSecureStatus(SecureMessage.INSECURE);
                callback.handleSecureMessage(message, false);
                return;
            }
            
            byte[] signature = message.getSecureSignature();
            if(signature == null) {
                LOG.warn("Cannot verify message without a signature.");
                message.setSecureStatus(SecureMessage.INSECURE);
                callback.handleSecureMessage(message, false);
                return;
            }
            
            try {
                Signature verifier = Signature.getInstance(algorithm);
                verifier.initVerify(pubKey);
                message.updateSignatureWithSecuredBytes(verifier);
                if(verifier.verify(signature)) {
                    message.setSecureStatus(SecureMessage.SECURE);
                    callback.handleSecureMessage(message, true);
                    return;
                }
                // fallthrough on not secure & failures to set failed.
            } catch (NoSuchAlgorithmException nsax) {
                LOG.error("No alg.", nsax);
            } catch (InvalidKeyException ikx) {
                LOG.error("Invalid key", ikx);
            } catch (SignatureException sx) {
                LOG.error("Bad sig", sx);
            } catch (ClassCastException ccx) {
                LOG.error("bad cast", ccx);
            }
            
            message.setSecureStatus(SecureMessage.FAILED);
            callback.handleSecureMessage(message, false);
        }
    }
}
