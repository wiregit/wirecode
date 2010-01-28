package com.limegroup.gnutella.security;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.SignatureException;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;
import org.limewire.util.URIUtils;

public class CertificateProviderImplTest extends BaseTestCase {

    public static Test suite() {
        return buildTestSuite(CertificateProviderImplTest.class);
    }

    private Mockery context;
    private FileCertificateReader fileCertificateReader;
    private HttpCertificateReader httpCertificateReader;
    private CertificateVerifier certificateVerifier;
    private URI uri;
    private CertificateProviderImpl certificateProviderImpl;
    private Certificate certificate;
    private File file;
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        fileCertificateReader = context.mock(FileCertificateReader.class);
        httpCertificateReader = context.mock(HttpCertificateReader.class);
        certificateVerifier = context.mock(CertificateVerifier.class);
        certificate = context.mock(Certificate.class);
        uri = URIUtils.toSafeUri("http://cert.cert/cert/cert");
        file = new File("file");
        certificateProviderImpl = new CertificateProviderImpl(fileCertificateReader, httpCertificateReader, certificateVerifier, file, uri);
    }
    
    public void testVerifiedGetFromFileWillNotCauseHttpGet() throws Exception {
        context.checking(new Expectations() {{
            one(fileCertificateReader).read(file);
            will(returnValue(certificate));
            
            one(certificateVerifier).verify(certificate);
            will(returnValue(certificate));
            
            never(httpCertificateReader);
        }});
        
        assertSame(certificate, certificateProviderImpl.get());
        
        context.assertIsSatisfied();
    }
    
    public void testUnverifiedGetFromFileWillCauseHttpGet() throws Exception {
        context.checking(new Expectations() {{
            one(fileCertificateReader).read(file);
            will(returnValue(certificate));
            
            one(certificateVerifier).verify(certificate);
            will(throwException(new SignatureException()));
            
            one(httpCertificateReader).read(uri, null);
            will(throwException(new IOException()));
        }});
        
        assertInstanceof(NullCertificate.class, certificateProviderImpl.get());
        
        context.assertIsSatisfied();
    }
    
    public void testVerifiedGetFromHttp() throws Exception {
        context.checking(new SequencedExpectations(context) {{
            one(fileCertificateReader).read(file);
            will(returnValue(certificate));
            
            one(certificateVerifier).verify(certificate);
            will(throwException(new SignatureException()));
            
            one(httpCertificateReader).read(uri, null);
            will(returnValue(certificate));
            
            exactly(2).of(certificateVerifier).verify(certificate);
            will(returnValue(certificate));
            
            one(fileCertificateReader).write(certificate, file);
            will(returnValue(true));
        }});
        
        assertSame(certificate, certificateProviderImpl.get());
        
        context.assertIsSatisfied();
    }
}
