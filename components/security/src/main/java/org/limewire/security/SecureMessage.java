package org.limewire.security;

import java.security.Signature;
import java.security.SignatureException;


/** 
 * Defines the interface to set a message's security state.
 */
public interface SecureMessage {
    
    //TODO: Make the following final ints into enums.
    /** A message that has not been verified.    */
    public static final int INSECURE = 0;
    /** A message that was attempted to be verified but failed verification.  */
    public static final int FAILED = 1;
    /** A message that was attempted to be verified and passed verification.  */
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
