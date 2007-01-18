package com.limegroup.gnutella.messages;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.security.SHA1;
import com.limegroup.gnutella.security.SignatureVerifier;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.util.ProcessingQueue;

/** A class that verifies secure messages sequentially. */
public class SecureMessageVerifier {
    
    private static final Log LOG = LogFactory.getLog(SecureMessageVerifier.class);

    private final ProcessingQueue QUEUE = new ProcessingQueue("SecureMessageVerifier");
    
    /** The public key. */
    private PublicKey pubKey;
    
    /** Queues this SecureMessage for verification.  The callback will be notified of success or failure. */
    public void verify(SecureMessage sm, SecureMessageCallback smc) {
        QUEUE.add(new Verifier(sm, smc));
    }
    
    /** Initializes the public key if one isn't set. */
    private void initializePublicKey() {
        if(pubKey == null) 
            pubKey = createPublicKey();
    }
    
    /** Creates the public key. */
    protected PublicKey createPublicKey() {
        return SignatureVerifier.readKey(getKeyString(), "DSA");
    }
    
    /** Gets the file holding the key. */
    protected String getKeyString() {
        return "GCBADOBQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7" +
        "VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5" +
        "RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O" +
        "5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV" +
        "37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2B" +
        "BOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOE" +
        "EBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7Y" +
        "L7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76" +
        "Z5ESUA4BQUAAFAMBACDW4TNFXK772ZQN752VPKQSFXJWC6PPSIVTHKDNLRUIQ7UF" +
        "4J2NF6J2HC5LVC4FO4HYLWEWSB3DN767RXILP37KI5EDHMFAU6HIYVQTPM72WC7FW" +
        "SAES5K2KONXCW65VSREAPY7BF24MX72EEVCZHQOCWHW44N4RG5NPH2J4EELDPXMNR" +
        "WNYU22LLSAMBUBKW3KU4QCQXG7NNY";
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
        
        SHA1 sha1 = new SHA1();
        sha1.update(getKeyString().getBytes());
        if (!Arrays.equals(sha1.digest(), 
                Base32.decode(FilterSettings.MESSAGE_KEY_SHA1.getValue()))) {
            LOG.warn("Cannot verify message with invalid key");
            message.setSecureStatus(SecureMessage.INSECURE);
            callback.handleSecureMessage(message, false);
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
