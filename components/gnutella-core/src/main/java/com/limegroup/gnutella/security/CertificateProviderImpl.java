package com.limegroup.gnutella.security;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.PublicKey;
import java.security.SignatureException;

import org.limewire.io.IpPort;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.limegroup.gnutella.util.DataUtils;

public class CertificateProviderImpl implements CertificateProvider {

    private static final Log LOG = LogFactory.getLog(CertificateProviderImpl.class);
    
    private final FileCertificateReaderImpl fileCertificateReader;
    private final HttpCertificateReaderImpl httpCertificateReader;
    private final CertificateVerifier certificateVerifier;
    
    private volatile Certificate validCertificate;

    private final File file;

    private final URI uri;
    
    public CertificateProviderImpl(FileCertificateReaderImpl fileCertificateReader,
            HttpCertificateReaderImpl httpCertificateReader, 
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
            validCertificate = certificateVerifier.verify(certificate);
            // TODO fberger write to file
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
            validCertificate = certificateVerifier.verify(certificate);
        } catch (IOException ie) {
            LOG.debugf(ie, "certificate from invalid url: {0}", uri);
        } catch (SignatureException e) {
            LOG.debugf(e, "certificate from url {0} invalid {1} ", uri, certificate);
        }
        return validCertificate;
    }

    private static class NullCertificate implements Certificate {
        @Override
        public String getCertificateString() {
            return null;
        }
        @Override
        public int getKeyVersion() {
            return -1;
        }
        @Override
        public PublicKey getPublicKey() {
            return new PublicKey() {
                @Override
                public String getFormat() {
                    return "";
                }
                @Override
                public byte[] getEncoded() {
                    return DataUtils.EMPTY_BYTE_ARRAY;
                }
                @Override
                public String getAlgorithm() {
                    return "DSA";
                }
            };
        }
        @Override
        public byte[] getSignature() {
            return DataUtils.EMPTY_BYTE_ARRAY;
        }
        @Override
        public byte[] getSignedPayload() {
            return DataUtils.EMPTY_BYTE_ARRAY;
        }
    }
    

}
