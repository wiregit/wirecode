package org.limewire.security;

import java.security.Signature;
import java.security.SignatureException;


/** Allows a message to be secured. */
public interface SecureMessage {
    
    public static final int INSECURE = 0;
    public static final int FAILED = 1;
    public static final int SECURE = 2;
    
    public void setSecureStatus(int secureStatus);
    
    public int getSecureStatus();

    public byte[] getSecureSignature();
    
    public void updateSignatureWithSecuredBytes(Signature signature) throws SignatureException;
}
