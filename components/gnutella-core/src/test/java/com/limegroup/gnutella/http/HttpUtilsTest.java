package com.limegroup.gnutella.http;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import com.limegroup.gnutella.util.BaseTestCase;

import junit.framework.*;

/**
 * Tests the HTTPUtils class.
 */
public final class HttpUtilsTest extends BaseTestCase {

	/**
	 * Constructs a new HttpUtilsTest.
	 */
	public HttpUtilsTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(HttpUtilsTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    /**
     * Tests the method for writing the date header to a <tt>Writer</tt> 
     * instance. 
     * 
     * @throws Exception if an error occurs
     */
    public void testWriteDateHeaderToWriter() throws Exception {
        StringWriter writer = new StringWriter();
        HTTPUtils.writeDate(writer);
        String header = writer.toString();
        assertTrue("should start with Date", header.startsWith("Date"));
        
        assertTrue("should end with GMT", header.endsWith("GMT\r\n"));
        
        // The date header should always be the same length.
        assertEquals("unexpected length", 36, header.length());
    }
    
    /**
     * Tests the method for writing the date header to a stream.
     * 
     * @throws Exception if an error occurs
     */
    public void testWriteDateHeaderToStream() throws Exception {
        OutputStream stream = new ByteArrayOutputStream();
        HTTPUtils.writeDate(stream);
        String header = stream.toString();
        assertTrue("should start with Date", header.startsWith("Date"));
        
        assertTrue("should end with GMT", header.endsWith("GMT\r\n"));
        
        // The date header should always be the same length.
        assertEquals("unexpected length", 36, header.length());
    }

    /**
     * Tests the method for writing an HTTP header with an integer value to a
     * <tt>Writer</tt>.
     * 
     * @throws Exception if an error occurs
     */
    public void testWriteHeaderWithIntValueToWriter() throws Exception {
        StringWriter writer = new StringWriter();
        HTTPUtils.writeHeader(HTTPHeaderName.CONTENT_LENGTH, 200, writer);
        assertEquals("unexpected header", "Content-Length: 200\r\n", 
            writer.toString());
    }
    
    /**
     * Tests the method for writing an HTTP header with an integer value to a
     * stream.
     * 
     * @throws Exception if an error occurs
     */
    public void testWriteHeaderWithIntValueToStream() throws Exception {
        OutputStream stream = new ByteArrayOutputStream();
        HTTPUtils.writeHeader(HTTPHeaderName.CONTENT_LENGTH, 200, stream);
        assertEquals("unexpected header", "Content-Length: 200\r\n", 
            stream.toString());
    }
    
	/**
	 * Tests the method to extract a header value from an HTTP header.
	 */
	public void testExtractHeaderValue() {
		String value = "value";
		String[] headers = {
			HTTPHeaderName.CONTENT_RANGE+":"   +value,
			HTTPHeaderName.CONTENT_RANGE+": "  +value,
			HTTPHeaderName.CONTENT_LENGTH+":  "+value,
			HTTPHeaderName.CONTENT_TYPE+":  "  +value
		};
		for(int i=0; i<headers.length; i++) {
			String curValue = HTTPUtils.extractHeaderValue(headers[i]);
			assertEquals("values should be equal", value, curValue);
		}
	}
}
