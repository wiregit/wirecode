package org.limewire.rest.oauth;

import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;

import junit.framework.TestCase;

/**
 * JUnit test case for OAuthValidatorImpl.
 */
public class OAuthValidatorImplTest extends TestCase {
    private static final String BASE_URL = "http://localhost";
    private static final int PORT = 45100;
    private static final String SECRET = "abcde";
    
    /** Instance of class being tested. */
    private OAuthValidator validator;

    /**
     * Constructs a test case for the specified method name.
     */
    public OAuthValidatorImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        validator = new OAuthValidatorImpl(BASE_URL, PORT, SECRET);
    }

    @Override
    protected void tearDown() throws Exception {
        validator = null;
        super.tearDown();
    }

    /** Tests method to validate request using OAuth. */
    public void testValidateRequest() {
        // Create HTTP request.
        HttpRequest httpRequest = new BasicHttpRequest("GET", "/remote/hello?type=test", HttpVersion.HTTP_1_1);
        
        // Add valid authorization header.
        String value = "OAuth oauth_consumer_key=\"restdemo\", oauth_version=\"1.0\", oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"1268258300\", oauth_nonce=\"2323977038244562009\", oauth_signature=\"zk%2BJ6xCnVU1AjTc6gNqMU5dkxkM%3D\"";
        httpRequest.addHeader(OAuthRequest.AUTH_HEADER, value);
        
        // Verify success.
        try {
            validator.validateRequest(new OAuthRequest(httpRequest));
        } catch (OAuthException ex) {
            fail("Authorized request failed");
        }
    }

    /** Tests method to validate request with bad signature. */
    public void testValidateRequestBadSignature() {
        // Create HTTP request.
        HttpRequest httpRequest = new BasicHttpRequest("GET", "/remote/hello?type=test", HttpVersion.HTTP_1_1);
        
        // Add authorization header with bad signature.
        String value = "OAuth oauth_consumer_key=\"restdemo\", oauth_version=\"1.0\", oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"1268258300\", oauth_nonce=\"2323977038244562009\", oauth_signature=\"zk%2BJ6xCnVU1AjTc6gNqMU5dkxkM\"";
        httpRequest.addHeader(OAuthRequest.AUTH_HEADER, value);
        
        // Verify exception thrown.
        try {
            validator.validateRequest(new OAuthRequest(httpRequest));
            fail("Bad signature did not fail");
        } catch (OAuthException ex) {
            assertNotNull(ex);
        }
    }

    /** Tests method to validate request with missing parameter. */
    public void testValidateRequestMissingParameter() {
        // Create HTTP request.
        HttpRequest httpRequest = new BasicHttpRequest("GET", "/remote/hello?type=test", HttpVersion.HTTP_1_1);
        
        // Add authorization header missing required parameter.
        String value = "OAuth oauth_consumer_key=\"restdemo\", oauth_version=\"1.0\", oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"1268258300\", oauth_nonce=\"2323977038244562009\"";
        httpRequest.addHeader(OAuthRequest.AUTH_HEADER, value);
        
        // Verify exception thrown.
        try {
            validator.validateRequest(new OAuthRequest(httpRequest));
            fail("Invalid request did not fail");
        } catch (OAuthException ex) {
            assertNotNull(ex);
        }
    }

    /** Tests method to validate unauthorized request. */
    public void testValidateRequestUnauthorized() {
        // Create HTTP request without authorization.
        HttpRequest httpRequest = new BasicHttpRequest("GET", "/remote/hello?type=test", HttpVersion.HTTP_1_1);
        
        // Verify exception thrown.
        try {
            validator.validateRequest(new OAuthRequest(httpRequest));
            fail("Unauthorized request did not fail");
        } catch (OAuthException ex) {
            assertNotNull(ex);
        }
    }

    /** Tests method to validate request with unsupported method. */
    public void testValidateRequestUnsupportedMethod() {
        // Create HTTP request.
        HttpRequest httpRequest = new BasicHttpRequest("GET", "/remote/hello?type=test", HttpVersion.HTTP_1_1);
        
        // Add authorization header with unsupported method.
        String value = "OAuth oauth_consumer_key=\"restdemo\", oauth_version=\"1.0\", oauth_signature_method=\"RSA-SHA1\", oauth_timestamp=\"1268258300\", oauth_nonce=\"2323977038244562009\", oauth_signature=\"zk%2BJ6xCnVU1AjTc6gNqMU5dkxkM%3D\"";
        httpRequest.addHeader(OAuthRequest.AUTH_HEADER, value);
        
        // Verify exception thrown.
        try {
            validator.validateRequest(new OAuthRequest(httpRequest));
            fail("Unsupported method did not fail");
        } catch (OAuthException ex) {
            assertNotNull(ex);
        }
    }
}
