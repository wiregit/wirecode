package org.limewire.rest.oauth;

import static org.limewire.rest.oauth.OAuthRequest.AUTH_REALM;
import static org.limewire.rest.oauth.OAuthRequest.OAUTH_CONSUMER_KEY;
import static org.limewire.rest.oauth.OAuthRequest.OAUTH_NONCE;
import static org.limewire.rest.oauth.OAuthRequest.OAUTH_SIGNATURE;
import static org.limewire.rest.oauth.OAuthRequest.OAUTH_SIGNATURE_METHOD;
import static org.limewire.rest.oauth.OAuthRequest.OAUTH_TIMESTAMP;
import static org.limewire.rest.oauth.OAuthRequest.OAUTH_VERSION;

import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;
import org.limewire.rest.RestUtils;
import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for LibraryRequestHandler.
 */
public class OAuthRequestTest extends BaseTestCase {
    // Test values.
    private static final String METHOD = "GET";
    private static final String URI = "/library/files?type=audio";
    private static final String REALM = "http://photos.example.net/";
    private static final String CONSUMER_KEY = "dpf43f3p2l4k3l03";
    private static final String SIG_METHOD = "HMAC-SHA1";
    private static final String SIGNATURE = "tR3+Ty81lMeYAr/Fid0kMTYa/WM=";
    private static final String TIMESTAMP = "1191242096";
    private static final String NONCE = "kllo9940pd9333jh";
    private static final String VERSION = "1.0";
    
    /** Instance of class being tested. */
    private OAuthRequest oauthRequest;

    /**
     * Constructs a test case for the specified method name.
     */
    public OAuthRequestTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Create HTTP request.
        HttpRequest httpRequest = new BasicHttpRequest(METHOD, URI, HttpVersion.HTTP_1_1);
        
        // Add authorization header.
        StringBuilder buf = new StringBuilder();
        buf.append("OAuth");
        buf.append(" ").append(AUTH_REALM).append("=\"").append(REALM).append("\"");
        buf.append(", ").append(createHeaderString(OAUTH_CONSUMER_KEY, CONSUMER_KEY));
        buf.append(", ").append(createHeaderString(OAUTH_SIGNATURE_METHOD, SIG_METHOD));
        buf.append(", ").append(createHeaderString(OAUTH_SIGNATURE, SIGNATURE));
        buf.append(", ").append(createHeaderString(OAUTH_TIMESTAMP, TIMESTAMP));
        buf.append(", ").append(createHeaderString(OAUTH_NONCE, NONCE));
        buf.append(", ").append(createHeaderString(OAUTH_VERSION, VERSION));
        httpRequest.setHeader("Authorization", buf.toString());
        
        // Create OAuth request object.
        oauthRequest = new OAuthRequest(httpRequest);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * Creates a encoded header parameter string for the specified name and 
     * value.
     */
    private String createHeaderString(String name, String value) {
        StringBuilder buf = new StringBuilder();
        buf.append(name).append("=\"").append(RestUtils.percentEncode(value)).append("\"");
        return buf.toString();
    }

    /** Tests method to retrieve request method. */
    public void testGetMethod() {
        assertEquals(METHOD, oauthRequest.getMethod());
    }

    /** Tests method to retrieve request uri. */
    public void testGetUri() {
        assertEquals("/library/files", oauthRequest.getUri());
    }
    
    /** Tests method to retrieve a single parameter. */
    public void testGetParameter() {
        // Verify header parameters were correctly parsed.  These include all
        // authorization header parameters and query parameters.
        assertEquals(REALM, oauthRequest.getParameter(AUTH_REALM));
        assertEquals(CONSUMER_KEY, oauthRequest.getParameter(OAUTH_CONSUMER_KEY));
        assertEquals(SIG_METHOD, oauthRequest.getParameter(OAUTH_SIGNATURE_METHOD));
        assertEquals(SIGNATURE, oauthRequest.getParameter(OAUTH_SIGNATURE));
        assertEquals(TIMESTAMP, oauthRequest.getParameter(OAUTH_TIMESTAMP));
        assertEquals(NONCE, oauthRequest.getParameter(OAUTH_NONCE));
        assertEquals(VERSION, oauthRequest.getParameter(OAUTH_VERSION));
        assertEquals("audio", oauthRequest.getParameter("type"));
    }
    
    /** Tests method to retrieve parameters. */
    public void testGetParameters() {
        assertEquals(8, oauthRequest.getParameters().size());
    }
}
