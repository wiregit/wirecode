package com.limegroup.gnutella.security;

import java.io.IOException;
import java.net.URI;

import junit.framework.Test;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.http.httpclient.DefaultHttpClientInstanceUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.inject.Providers;
import org.limewire.io.IpPortImpl;
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
        httpCertificateReaderImpl = new HttpCertificateReaderImpl(Providers.of(limeHttpClient), certificateParser, new DefaultHttpClientInstanceUtils());
        
        context.checking(new Expectations() {{
            one(limeHttpClient).releaseConnection(with(any(BasicHttpResponse.class)));
        }});
    }
    
    public void testAddsMessageSourceHeader() throws Exception {
        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(new BaseMatcher<HttpUriRequest>() {
                @Override
                public boolean matches(Object item) {
                    assertInstanceof(HttpUriRequest.class, item);
                    HttpUriRequest uriRequest = (HttpUriRequest)item;
                    assertEquals("http://limewire.com/", uriRequest.getRequestLine().getUri());
                    Header header = uriRequest.getFirstHeader("X-Message-Source");
                    assertNotNull(header);
                    assertEquals("192.168.0.1:5555", header.getValue());
                    return true;
                }
                @Override
                public void describeTo(Description description) {
                }
            }));
            will(throwException(new IOException()));
        }});
        
        try {
            httpCertificateReaderImpl.read(URI.create("http://limewire.com/"), new IpPortImpl("192.168.0.1:5555"));
        } catch (IOException ie) {
        }
        
        context.assertIsSatisfied();
    }
    
    public void testReadFailsOnNonOkStatusResponse() throws Exception {
        final HttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 404, "not found");
        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(any(HttpUriRequest.class)));
            will(returnValue(httpResponse));
        }});
        
        try {
            httpCertificateReaderImpl.read(URI.create("http://limewire.com/"), new IpPortImpl("192.168.0.1:5555"));
            fail("exception expected");
        } catch (IOException ie) {
        }
        
        context.assertIsSatisfied();
    }
    
    public void testReadDecodesEntityUsingContentEncoding() throws Exception {
        final HttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        final String content = "\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7.\u30b9\u30c6\u30e0";
        StringEntity entity = new StringEntity(content, "UTF-16");
        httpResponse.setEntity(entity);
        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(any(HttpUriRequest.class)));
            will(returnValue(httpResponse));
            one(certificateParser).parseCertificate(content);
            will(returnValue(new NullCertificate()));
        }});
        
        Certificate certificate = httpCertificateReaderImpl.read(URI.create("http://limewire.com/"), null);
        assertInstanceof(NullCertificate.class, certificate);
        
        context.assertIsSatisfied();
    }
}
