package com.limegroup.gnutella.security;

import java.security.PublicKey;

/**
 * Encapusulates a certificate, not necessarily valid.
 */
public interface Certificate {

    public static final int IGNORE_ID = Integer.MAX_VALUE;
    
    byte[] getSignature();
    
    PublicKey getPublicKey();
    
    int getKeyVersion();
    
    byte[] getSignedPayload();
    
    String getCertificateString();
}
