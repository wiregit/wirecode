package com.limegroup.gnutella.security;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.SignatureException;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.util.BaseTestCase;

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
        uri = URI.create("http://cert.cert/cert/cert");
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
        // test again to ensure the same certificate is returned henceforth
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
        // test again to ensure the same certificate is returned henceforth
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
            
            one(certificateVerifier).verify(certificate);
            will(returnValue(certificate));
            
            one(fileCertificateReader).write(certificate, file);
            will(returnValue(true));
        }});
        
        assertSame(certificate, certificateProviderImpl.get());
        // test again to ensure the same certificate is returned henceforth
        assertSame(certificate, certificateProviderImpl.get());
        
        context.assertIsSatisfied();
    }
    
    public void testSuccessfulHttpGetReplacesNullCertificate() throws Exception {
        context.checking(new SequencedExpectations(context) {{
            // fail
            one(fileCertificateReader).read(file);
            will(throwException(new IOException()));
            
            // fail
            one(httpCertificateReader).read(uri, null);
            will(throwException(new IOException()));

            // successful http get
            one(httpCertificateReader).read(uri, null);
            will(returnValue(certificate));
            
            one(certificate).getKeyVersion();
            will(returnValue(1));

            // and another verification in set()
            one(certificateVerifier).verify(certificate);
            will(returnValue(certificate));
            
            // write new certificate
            one(fileCertificateReader).write(certificate, file);
            will(returnValue(true));
        }});
        
        assertInstanceof(NullCertificate.class, certificateProviderImpl.get());
        assertSame(certificate, certificateProviderImpl.getFromHttp(null));
        // ensure null certificate was replaced
        assertSame(certificate, certificateProviderImpl.get());
        
        context.assertIsSatisfied();
    }
    
    public void testFailedGetFromHttpBeforeGetReturnsNullCertificate() throws Exception {
        context.checking(new SequencedExpectations(context) {{
            // failed http get
            one(httpCertificateReader).read(uri, null);
            will(throwException(new IOException()));
        }});
        
        assertInstanceof(NullCertificate.class, certificateProviderImpl.getFromHttp(null));
        assertInstanceof(NullCertificate.class, certificateProviderImpl.get());
        context.assertIsSatisfied();
    }
    
    public void testSuccessfulGetFromHttpBeforeGet() throws Exception {
        context.checking(new SequencedExpectations(context) {{
            // successful http get
            one(httpCertificateReader).read(uri, null);
            will(returnValue(certificate));

            one(certificateVerifier).verify(certificate);
            will(returnValue(certificate));
            
            one(fileCertificateReader).write(certificate, file);
            will(returnValue(true));
        }});
        
        assertSame(certificate, certificateProviderImpl.getFromHttp(null));
        assertSame(certificate, certificateProviderImpl.get());
        
        context.assertIsSatisfied();
    }
    
    public void testMessageSourceIsReportedToHttpCertificateReader() throws Exception {
        final IpPort messageSource = new IpPortImpl("192.168.0.1:4045");
        context.checking(new SequencedExpectations(context) {{
            one(httpCertificateReader).read(uri, messageSource);
            will(throwException(new IOException()));
        }});
        
        assertInstanceof(NullCertificate.class, certificateProviderImpl.getFromHttp(messageSource));
        
        context.assertIsSatisfied();
    }
    
    public void testSetBeforeGet() throws Exception {
        context.checking(new Expectations() {{
            one(certificateVerifier).verify(certificate);
            will(returnValue(certificate));
            
            one(fileCertificateReader).write(certificate, file);
            will(returnValue(true));
        }});
        
        certificateProviderImpl.set(certificate);
        assertSame(certificate, certificateProviderImpl.get());
        context.assertIsSatisfied();
    }
    
    public void testSetInvalidCertifcateFails() throws Exception {
        context.checking(new Expectations() {{
            one(certificateVerifier).verify(certificate);
            will(throwException(new SignatureException()));
            
            one(fileCertificateReader).read(file);
            will(throwException(new IOException()));
            
            // fail
            one(httpCertificateReader).read(uri, null);
            will(throwException(new IOException()));
        }});
        
        certificateProviderImpl.set(certificate);
        assertInstanceof(NullCertificate.class, certificateProviderImpl.get());
        
        context.assertIsSatisfied();
    }
    
    public void testSetOlderCertificateDoesNotOverrideNewerOne() throws Exception {
        final Certificate olderCertificate = context.mock(Certificate.class);  
        context.checking(new SequencedExpectations(context) {{
            allowing(certificate).getKeyVersion();
            will(returnValue(2));
            allowing(olderCertificate).getKeyVersion();
            will(returnValue(1));
            
            allowing(certificateVerifier).verify(certificate);
            will(returnValue(certificate));
            
            allowing(certificateVerifier).verify(olderCertificate);
            will(returnValue(olderCertificate));
            
            allowing(fileCertificateReader).write(certificate, file);
            will(returnValue(true));
        }});
        
        certificateProviderImpl.set(certificate);
        assertSame(certificate, certificateProviderImpl.get());
        
        // try to set older certificate
        certificateProviderImpl.set(olderCertificate);
        // still the same certificate
        assertSame(certificate, certificateProviderImpl.get());
        
        context.assertIsSatisfied();
    }
}
