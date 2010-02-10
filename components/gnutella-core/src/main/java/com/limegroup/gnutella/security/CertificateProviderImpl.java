package com.limegroup.gnutella.security;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.SignatureException;

import org.limewire.io.IpPort;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

/**
 * Reads valid certificates from file, http and stores them to the same file.
 */
public class CertificateProviderImpl implements CertificateProvider {

    private static final Log LOG = LogFactory.getLog(CertificateProviderImpl.class);
    
    private final FileCertificateReader fileCertificateReader;
    private final HttpCertificateReader httpCertificateReader;
    private final CertificateVerifier certificateVerifier;
    
    private volatile Certificate validCertificate;

    private final File file;

    private final URI uri;
    
    /**
     * @param fileCertificateReader the file certificate reader used for reading
     * certificates from disk and for storing them to disk.
     * @param httpCertificateReader the http certificate reader used for 
     * retrieving certificates from a trusted http server
     * @param certificateVerifier verifier to verify all read and set certificates
     * @param file the file to read certificates from and write them to
     * @param uri the uri certificates are downloaded from over http
     */
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
            if (localCopy == null || certificate.getKeyVersion() > localCopy.getKeyVersion()) {
                validCertificate = certificateVerifier.verify(certificate);
                fileCertificateReader.write(certificate, file);
            }
        } catch (SignatureException se) {
            LOG.debugf(se, "certificate invalid {0} ", certificate);
        }
    }

    /**
     * Potentially blocking call, accessing the disk and making network connections.
     * <p>
     * If a valid certificate is loaded, it will return the valid certificate.
     * Otherwise it will try to read a certificate from disk. If this fails it
     * will resort to http.
     * 
     * @returns {@link NullCertificate} if no valid certificate could be retrieved
     * from any of the sources
     */
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
            set(certificate);
        } catch (IOException ie) {
            LOG.debugf(ie, "certificate from invalid url: {0}", uri);
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
