package org.limewire.http;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.limewire.util.BaseTestCase;

public class BasicAuthenticationRequestInterceptorTest extends BaseTestCase {

    public BasicAuthenticationRequestInterceptorTest(String name) {
        super(name);
    }

    public void testProcessNoCredentials() throws Exception {
        BasicHttpContext context = new BasicHttpContext();
        try {
            new BasicAuthenticationRequestInterceptor("mellow", "world").process(new BasicHttpRequest("GET", "/"), context);
            fail("expected exception");
        } catch (HttpException e) {
        }
    }
    
    public void testProcessWrongUsername() throws IOException {
        BasicHttpContext context = new BasicHttpContext();
        try {
            new BasicAuthenticationRequestInterceptor("mellow", "world").process(createRequest("hello", "world"), context);
            fail("expected exception");
        } catch (HttpException e) {
        }
    }
    
    public void testProcessWrongPassword() throws IOException {
        BasicHttpContext context = new BasicHttpContext();
        try {
            new BasicAuthenticationRequestInterceptor("hello", "morld").process(createRequest("hello", "world"), context);
            fail("expected exception");
        } catch (HttpException e) {
        }
    }
    
    public void testProcessValidCredentials() throws IOException {
        BasicHttpContext context = new BasicHttpContext();
        try {
            new BasicAuthenticationRequestInterceptor("hello", "world").process(createRequest("hello", "world"), context);
        } catch (HttpException e) {
        }
    }

    public void testParseCredentialsGeneratedByBasicScheme() throws Exception {
        Header header = createAuthorizationHeader("hello", "world\u30d5");
        BasicAuthenticationRequestInterceptor acceptingInterceptor = new BasicAuthenticationRequestInterceptor("hello", "world\u30d5");
        UsernamePasswordCredentials parsedCredentials = acceptingInterceptor.parseCredentials(header);
        assertEquals("hello", parsedCredentials.getUserName());
        assertEquals("world\u30d5", parsedCredentials.getPassword());
    }
    
    private Header createAuthorizationHeader(String username, String password) {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        return BasicScheme.authenticate(credentials, "UTF-8", false);
    }
    
    private HttpRequest createRequest(String username, String password) {
        BasicHttpRequest request = new BasicHttpRequest("GET", "/");
        request.addHeader(createAuthorizationHeader(username, password));
        return request;
    }

}
