package org.limewire.rest.oauth;

import static org.limewire.rest.oauth.OAuthRequest.AUTH_HEADER;
import static org.limewire.rest.oauth.OAuthRequest.AUTH_REALM;
import static org.limewire.rest.oauth.OAuthRequest.OAUTH_CONSUMER_KEY;
import static org.limewire.rest.oauth.OAuthRequest.OAUTH_NONCE;
import static org.limewire.rest.oauth.OAuthRequest.OAUTH_SIGNATURE;
import static org.limewire.rest.oauth.OAuthRequest.OAUTH_SIGNATURE_METHOD;
import static org.limewire.rest.oauth.OAuthRequest.OAUTH_TIMESTAMP;
import static org.limewire.rest.oauth.OAuthRequest.OAUTH_VERSION;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicHeader;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.rest.RestUtils;
import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for OAuthUtils.
 */
public class OAuthUtilsTest extends BaseTestCase {

    private Mockery context = new Mockery();

    /**
     * Constructs a test case for the specified method name.
     */
    public OAuthUtilsTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /** Tests method to create signature base string. */
    public void testCreateSignatureBaseString() {
        // Test values.
        final String METHOD = "GET";
        final String URI = "/remote/hello?type=test";
        final String REALM = "http://localhost/";
        final String CONSUMER_KEY = "restdemo";
        final String SIG_METHOD = "HMAC-SHA1";
        final String SIGNATURE = "C0aRkw2fACAgy5PyplnUur1eNak=";
        final String TIMESTAMP = "4421858300";
        final String NONCE = "2323977038244562009";
        final String VERSION = "1.0";
        
        // Create authorization header.
        StringBuilder buf = new StringBuilder();
        buf.append("OAuth");
        buf.append(" ").append(AUTH_REALM).append("=\"").append(REALM).append("\"");
        buf.append(", ").append(createHeaderElementString(OAUTH_CONSUMER_KEY, CONSUMER_KEY));
        buf.append(", ").append(createHeaderElementString(OAUTH_SIGNATURE_METHOD, SIG_METHOD));
        buf.append(", ").append(createHeaderElementString(OAUTH_SIGNATURE, SIGNATURE));
        buf.append(", ").append(createHeaderElementString(OAUTH_TIMESTAMP, TIMESTAMP));
        buf.append(", ").append(createHeaderElementString(OAUTH_NONCE, NONCE));
        buf.append(", ").append(createHeaderElementString(OAUTH_VERSION, VERSION));
        Header authHeader = new BasicHeader(AUTH_HEADER, buf.toString());
        
        // Create mock values.
        final RequestLine mockRequestLine = context.mock(RequestLine.class);
        final HttpRequest mockHttpRequest = context.mock(HttpRequest.class);
        final Header[] mockHeaders = new Header[] {authHeader}; 
        
        context.checking(new Expectations() {{
            allowing(mockRequestLine).getMethod();
            will(returnValue(METHOD));
            allowing(mockRequestLine).getUri();
            will(returnValue(URI));
            
            allowing(mockHttpRequest).getRequestLine();
            will(returnValue(mockRequestLine));
            allowing(mockHttpRequest).getHeaders(AUTH_HEADER);
            will(returnValue(mockHeaders));
        }});
        
        // Create OAuth request object.
        OAuthRequest mockRequest = new OAuthRequest(mockHttpRequest);
        String baseUrl = "http://localhost:45100";
        
        // Verify signature base string.
        String expectedReturn = "GET&http%3A%2F%2Flocalhost%3A45100%2Fremote%2Fhello&oauth_consumer_key%3Drestdemo%26oauth_nonce%3D2323977038244562009%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D4421858300%26oauth_version%3D1.0%26type%3Dtest";
        String sbs = OAuthUtils.createSignatureBaseString(mockRequest, baseUrl);
        assertEquals(expectedReturn, sbs);
    }
    
    /**
     * Creates a encoded header parameter string for the specified name and 
     * value.
     */
    private String createHeaderElementString(String name, String value) {
        StringBuilder buf = new StringBuilder();
        buf.append(name).append("=\"").append(RestUtils.percentEncode(value)).append("\"");
        return buf.toString();
    }
}
