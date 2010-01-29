package com.limegroup.gnutella.security;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.SignatureException;

import org.limewire.io.IpPort;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;


public class CertificateProviderImpl implements CertificateProvider {

    private static final Log LOG = LogFactory.getLog(CertificateProviderImpl.class);
    
    private final FileCertificateReader fileCertificateReader;
    private final HttpCertificateReader httpCertificateReader;
    private final CertificateVerifier certificateVerifier;
    
    private volatile Certificate validCertificate;

    private final File file;

    private final URI uri;
    
    public CertificateProviderImpl(FileCertificateReader fileCertificateReader,
            HttpCertificateReader httpCertificateReader, 
            CertificateVerifier certificateVerifier,
            File file, URI uri) {
        this.fileCertificateReader = fileCertificateReader;
        this.httpCertificateReader = httpCertificateReader;
        this.certificateVerifier = certificateVerifier;
        this.file = file;
        this.uri = uri;
    }
    
    public Certificate getFromFile() {
        Certificate certificate = null; 
        try {
            certificate = fileCertificateReader.read(file);
            validCertificate = certificateVerifier.verify(certificate);
        } catch (IOException e) {
            LOG.debugf(e, "certificate from invalid file: {0}", file);
        } catch (SignatureException e) {
            LOG.debugf(e, "certificate from file {0} invalid {1} ", file, certificate);
        }
        return validCertificate;
    }

    @Override
    public void set(Certificate certificate) {
        try { 
            Certificate localCopy = validCertificate;
            if (localCopy == null || (!localCopy.equals(certificate) && certificate.getKeyVersion() > localCopy.getKeyVersion())) {
                validCertificate = certificateVerifier.verify(certificate);
                fileCertificateReader.write(certificate, file);
            }
        } catch (SignatureException se) {
            LOG.debugf(se, "certificate invalid {0} ", certificate);
        }
    }

    @Override
    public Certificate get() {
        Certificate copy = validCertificate;
        if (copy != null) {
            return copy;
        }
        getFromFile();
        copy = validCertificate;
        if (copy != null) {
            return copy;
        }
        getFromHttp(null);
        copy = validCertificate;
        if (copy != null) {
            return copy;
        }
        copy = new NullCertificate();
        validCertificate = copy;
        return copy;
    }

    @Override
    public Certificate getFromHttp(IpPort messageSource) {
        Certificate certificate = null;
        try {
            certificate = httpCertificateReader.read(uri, messageSource);
            set(certificateVerifier.verify(certificate));
            return certificate;
        } catch (IOException ie) {
            LOG.debugf(ie, "certificate from invalid url: {0}", uri);
        } catch (SignatureException e) {
            LOG.debugf(e, "certificate from url {0} invalid {1} ", uri, certificate);
        }
        certificate = validCertificate;
        if (certificate != null) {
            return certificate;
        }
        certificate = new NullCertificate();
        validCertificate = certificate;
        return certificate;
    }
    

}
