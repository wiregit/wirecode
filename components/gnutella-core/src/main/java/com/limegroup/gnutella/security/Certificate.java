package com.limegroup.gnutella.security;

import java.security.PublicKey;

/**
 * Encapusulates a certificate, not necessarily valid.
 */
public interface Certificate {

    byte[] getSignature();
    
    PublicKey getPublicKey();
    
    int getKeyVersion();
    
    byte[] getSignedPayload();
    
    String getCertificateString();
}
