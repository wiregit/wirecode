package com.limegroup.gnutella.security;

import java.security.SignatureException;

import org.limewire.io.IpPort;
import org.limewire.security.SignatureVerifier;

public class CertifiedMessageVerifierImpl implements CertifiedMessageVerifier {

    private final CertificateProvider certificateProvider;
    private final CertificateVerifier certificateVerifier;

    public CertifiedMessageVerifierImpl(CertificateProvider certificateProvider,
            CertificateVerifier certificateVerifier) {
        this.certificateProvider = certificateProvider;
        this.certificateVerifier = certificateVerifier;
    }
    
    @Override
    public void verify(CertifiedMessage message, IpPort messageSource) throws SignatureException {
        Certificate certificate = message.getCertificate();
        if (certificate != null) {
            certificate = certificateVerifier.verify(certificate);
            // update provider with new certificate after it verified
            certificateProvider.set(certificate);
        } else {
            certificate = certificateProvider.get();
        }
        if (message.getKeyVersion() < certificate.getKeyVersion()) {
            throw new SignatureException("message version less than certificate version");
        } else if (message.getKeyVersion() > certificate.getKeyVersion()) {
            certificate = certificateProvider.getFromHttp(messageSource);
            if (message.getKeyVersion() > certificate.getKeyVersion()) {
                throw new SignatureException("key version greater than certificate version");
            }
        }
        SignatureVerifier signatureVerifier = new SignatureVerifier(message.getSignedPayload(), message.getSignature(), certificate.getPublicKey(), "DSA");
        if (!signatureVerifier.verifySignature()) {
            throw new SignatureException("Invalid signature for: " + message);
        }
    }

}
