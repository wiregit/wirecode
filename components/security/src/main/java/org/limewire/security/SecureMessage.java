package org.limewire.security;

import java.security.Signature;
import java.security.SignatureException;


/** Allows a message to be secured. */
public interface SecureMessage {
    
    public static final int INSECURE = 0;
    public static final int FAILED = 1;
    public static final int SECURE = 2;
    
    /** Sets whether or not the message is verified. */
    public void setSecureStatus(int secureStatus);
    
    /** Determines if the message was verified. */
    public int getSecureStatus();

    /** Returns the bytes of the signature from the secure GGEP block. */
    public byte[] getSecureSignature();
    
    /** Passes in the appropriate bytes of the payload to the signature. */
    public void updateSignatureWithSecuredBytes(Signature signature) throws SignatureException;
}
