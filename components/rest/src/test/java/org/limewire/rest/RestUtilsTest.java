package org.limewire.rest;

import java.util.Map;

import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for RestUtils.
 */
public class RestUtilsTest extends BaseTestCase {

    private Mockery context = new Mockery();

    /**
     * Constructs a test case for the specified method name.
     */
    public RestUtilsTest(String name) {
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

    /** Tests method to perform percent decoding. */
    public void testPercentDecode() {
        // Verify unreserved characters not decoded.
        String testStr = "first_name-last_name.bak~";
        assertEquals(testStr, RestUtils.percentDecode(testStr));
        
        // Verify URL string decoded.
        testStr = "http%3A%2F%2Fwww.limewire.com";
        String expected = "http://www.limewire.com";
        assertEquals(expected, RestUtils.percentDecode(testStr));
        
        // Verify parameter string decoded.
        testStr = "find%3Falpha%3D1%26beta%3D2%2B3";
        expected = "find?alpha=1&beta=2+3";
        assertEquals(expected, RestUtils.percentDecode(testStr));
    }

    /** Tests method to perform percent encoding. */
    public void testPercentEncode() {
        // Verify unreserved characters not encoded.
        String testStr = "first_name-last_name.bak~";
        assertEquals(testStr, RestUtils.percentEncode(testStr));
        
        // Verify URL string encoded.
        testStr = "http://www.limewire.com";
        String expected = "http%3A%2F%2Fwww.limewire.com";
        assertEquals(expected, RestUtils.percentEncode(testStr));
        
        // Verify parameter string encoded.
        testStr = "find?alpha=1&beta=2+3";
        expected = "find%3Falpha%3D1%26beta%3D2%2B3";
        assertEquals(expected, RestUtils.percentEncode(testStr));
    }

    /** Tests method to perform percent encoding on space characters. */
    public void testPercentEncodeSpace() {
        // Verify plus sign converted to encoded space.
        String testStr = "parm=hello world";
        String expected = "parm%3Dhello%20world";
        assertEquals(expected, RestUtils.percentEncode(testStr));
    }
    
    /** Tests method to get base URI string. */
    public void testGetBaseUri() throws Exception {
        String testUri = "http://localhost/remote/library/files?offset=1";
        String baseUri = "http://localhost/remote/library/files";
        
        assertEquals(baseUri, RestUtils.getBaseUri(testUri));
    }

    /** Tests method to get URI target. */
    public void testGetUriTarget() throws Exception {
        final String testUri = "http://localhost/remote/library/files?offset=1";
        final String testPrefix = "/library";
        
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine mockRequestLine = context.mock(RequestLine.class);
        context.checking(new Expectations() {{
            allowing(mockRequestLine).getUri();
            will(returnValue(testUri));
            allowing(mockRequest).getRequestLine();
            will(returnValue(mockRequestLine));
        }});
        
        assertEquals("/files", RestUtils.getUriTarget(mockRequest, testPrefix));
    }

    /** Tests method to get query parameters from HTTP request. */
    public void testGetQueryParamsFromRequest() throws Exception {
        final String testMethod = "GET";
        final String testUri = "http://localhost/remote/library/files?offset=1";
        
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine mockRequestLine = context.mock(RequestLine.class);
        context.checking(new Expectations() {{
            allowing(mockRequestLine).getMethod();
            will(returnValue(testMethod));
            allowing(mockRequestLine).getUri();
            will(returnValue(testUri));
            allowing(mockRequest).getRequestLine();
            will(returnValue(mockRequestLine));
        }});
        
        Map<String, String> queryParams = RestUtils.getQueryParams(mockRequest);
        assertEquals(1, queryParams.size());
        assertEquals("1", queryParams.get("offset"));
    }

    /** Tests method to get query parameters from URI string. */
    public void testGetQueryParamsFromString() throws Exception {
        String testUri = "http://localhost/remote/library/files?offset=1";
        
        Map<String, String> queryParams = RestUtils.getQueryParams(testUri);
        assertEquals(1, queryParams.size());
        assertEquals("1", queryParams.get("offset"));
    }
}
