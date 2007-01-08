package org.limewire.security;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;


/** A class that verifies secure messages sequentially. */
public class SecureMessageVerifier {
    
    private static final Log LOG = LogFactory.getLog(SecureMessageVerifier.class);

    private final ExecutorService QUEUE = ExecutorsHelper.newProcessingQueue("SecureMessageVerifier");
    
    /** The public key. */
    private PublicKey pubKey;
    
    /** The File the public key is stored in. */
    private File keyFile;
    
    public SecureMessageVerifier() {}
    
    public SecureMessageVerifier(File keyFile) {
        this.keyFile = keyFile;
    }
    
    /** Queues this SecureMessage for verification.  The callback will be notified of success or failure. */
    public void verify(SecureMessage sm, SecureMessageCallback smc) {
        QUEUE.execute(new Verifier(sm, smc));
    }
    
    /** Initializes the public key if one isn't set. */
    private void initializePublicKey() {
        if(pubKey == null)
            pubKey = createPublicKey();
    }
    
    /** Creates the public key. */
    protected PublicKey createPublicKey() {
        if(getKeyFile() == null)
            throw new NullPointerException("no key file!");
        return SignatureVerifier.readKey(getKeyFile(), "DSA");
    }
    
    /** Gets the file holding the key. */
    protected File getKeyFile() {
        return keyFile;
    }
    
    /** Does the verification. */
    private void verifyMessage(SecureMessage message, SecureMessageCallback callback) {
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
            Signature verifier = Signature.getInstance("SHA1withDSA");
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
    
    /** Simple runnable to insert into the ProcessingQueue. */
    private class Verifier implements Runnable {
        private final SecureMessage message;
        private final SecureMessageCallback callback;
        
        Verifier(SecureMessage message, SecureMessageCallback callback) {
            this.message = message;
            this.callback = callback;
        }
        
        public void run() {
            initializePublicKey();
            verifyMessage(message, callback);
        }
    }

}
