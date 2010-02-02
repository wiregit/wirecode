package com.limegroup.gnutella.security;

import java.security.SignatureException;

import org.limewire.io.IpPort;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.security.SignatureVerifier;

public class CertifiedMessageVerifierImpl implements CertifiedMessageVerifier {

    private static final Log LOG = LogFactory.getLog(CertifiedMessageVerifierImpl.class);
    
    private final CertificateProvider certificateProvider;

    public CertifiedMessageVerifierImpl(CertificateProvider certificateProvider) {
        this.certificateProvider = certificateProvider;
    }
    
    @Override
    public void verify(CertifiedMessage message, IpPort messageSource) throws SignatureException {
        LOG.debugf("verifying message {0} from {1}", message, messageSource);
        Certificate certificate = message.getCertificate();
        if (certificate != null) {
            LOG.debugf("message comes with new certificate: {0}", certificate);
            // try to set certificate, if it is older or invalid this will fail
            certificateProvider.set(certificate);
        } 
        certificate = certificateProvider.get();
        if (message.getKeyVersion() < certificate.getKeyVersion()) {
            throw new SignatureException("message version less than certificate version");
        } else if (message.getKeyVersion() > certificate.getKeyVersion()) {
            LOG.debug("message key version greater than stored key version");
            certificate = certificateProvider.getFromHttp(messageSource);
            if (message.getKeyVersion() != certificate.getKeyVersion()) {
                throw new SignatureException("key version greater than certificate version");
            }
        }
        SignatureVerifier signatureVerifier = new SignatureVerifier(message.getSignedPayload(), message.getSignature(), certificate.getPublicKey(), "DSA");
        if (!signatureVerifier.verifySignature()) {
            throw new SignatureException("Invalid signature for: " + message);
        }
    }

}
