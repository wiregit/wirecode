package com.limegroup.gnutella.security;

import junit.framework.Test;

import org.apache.http.message.BasicHttpResponse;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.inject.Providers;
import org.limewire.util.BaseTestCase;

public class HttpCertificateReaderImplTest extends BaseTestCase {
    
    public static Test suite() {
        return buildTestSuite(HttpCertificateReaderImplTest.class);
    }

    private Mockery context;
    private LimeHttpClient limeHttpClient;
    private CertificateParser certificateParser;
    private HttpCertificateReaderImpl httpCertificateReaderImpl;
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        limeHttpClient = context.mock(LimeHttpClient.class);
        certificateParser = context.mock(CertificateParser.class);
        httpCertificateReaderImpl = new HttpCertificateReaderImpl(Providers.of(limeHttpClient), certificateParser);
        
        context.checking(new Expectations() {{
            one(limeHttpClient).releaseConnection(with(any(BasicHttpResponse.class)));
        }});
    }
    
    
}
