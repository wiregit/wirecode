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

    private final ProcessingQueue QUEUE = new ProcessingQueue("SecureMessageVerifier");
    
    /** The public key. */
    private PublicKey pubKey;
    
    
    public void verify(SecureMessage sm, SecureMessageCallback smc) {
        QUEUE.add(new Verifier(sm, smc));
    }
    
    private void initializePublicKey() {
        if(pubKey == null)
            pubKey = SignatureVerifier.readKey(getKeyFile(), "DSA");
    }
    
    private File getKeyFile() {
        return new File(CommonUtils.getUserSettingsDir(), "secureMessage.key");
    }
    
    private void verifyMessage(SecureMessage message, SecureMessageCallback callback) {
        if(pubKey == null) {
            LOG.warn("Cannot verify message without a public key.");
            callback.handleSecureMessage(message, false);
            return;
        }
        
        byte[] signature = message.getSecureSignature();
        if(signature == null) {
            LOG.warn("Cannot verify message without a signature.");
            callback.handleSecureMessage(message, false);
            return;
        }
        
        try {
            Signature verifier = Signature.getInstance("DSAwithSHA1");
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
