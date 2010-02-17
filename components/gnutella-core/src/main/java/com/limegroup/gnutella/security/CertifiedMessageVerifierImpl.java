package com.limegroup.gnutella.security;

import java.security.SignatureException;

import org.limewire.io.IpPort;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.security.SignatureVerifier;

public class CertifiedMessageVerifierImpl implements CertifiedMessageVerifier {

    private static final Log LOG = LogFactory.getLog(CertifiedMessageVerifierImpl.class);
    
    private final CertificateProvider certificateProvider;

    private final CertificateVerifier certificateVerifier;

    public CertifiedMessageVerifierImpl(CertificateProvider certificateProvider, CertificateVerifier certificateVerifier) {
        this.certificateProvider = certificateProvider;
        this.certificateVerifier = certificateVerifier;
    }
    
    @Override
    public Certificate verify(CertifiedMessage message, IpPort messageSource) throws SignatureException {
        LOG.debugf("verifying message {0} from {1}", message, messageSource);
        Certificate certificate = message.getCertificate();
        if (certificate != null) {
            if (certificate.getKeyVersion() != message.getKeyVersion()) {
                // no need to even verify
                throw new SignatureException("certificate key version and message key version don't match");
            }
            // verify sent certificate
            certificate = certificateVerifier.verify(certificate);
        } else {
            certificate = certificateProvider.get();
            // we might have to fetch a newer certificate from http
            if (message.getKeyVersion() > certificate.getKeyVersion()) {
                LOG.debug("message key version greater than stored key version");
                certificate = certificateProvider.getFromHttp(messageSource);
                if (message.getKeyVersion() != certificate.getKeyVersion()) {
                    throw new SignatureException("key version not equal to certificate version");
                }
            }
        }
        if (message.getKeyVersion() != certificate.getKeyVersion()) {
            throw new SignatureException("message version less than certificate version");
        }
        SignatureVerifier signatureVerifier = new SignatureVerifier(message.getSignedPayload(), message.getSignature(), certificate.getPublicKey(), "DSA");
        if (!signatureVerifier.verifySignature()) {
            throw new SignatureException("Invalid signature for: " + message);
        }
        return certificate;
    }

}
