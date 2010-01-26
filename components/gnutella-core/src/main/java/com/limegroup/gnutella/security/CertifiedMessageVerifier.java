package com.limegroup.gnutella.security;

import java.security.SignatureException;

import org.limewire.io.IpPort;

public interface CertifiedMessageVerifier {

    void verify(CertifiedMessage message, IpPort messageSource) throws SignatureException;
    
    public interface CertifiedMessage {
        int getKeyVersion();
        byte[] getSignature();
        byte[] getSignedPayload();
        Certificate getCertificate();
    }
}
