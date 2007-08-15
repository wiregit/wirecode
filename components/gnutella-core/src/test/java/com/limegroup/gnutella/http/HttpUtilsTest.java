package com.limegroup.gnutella.http;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Tests the HTTPUtils class.
 */
public final class HttpUtilsTest extends LimeTestCase {

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
     * Test to make sure the method for writing headers to a stream with 
     * string values is working properly.
     * 
     * @throws Exception if an error occurs
     */
    public void testWriteHeaderWithStringToStream() throws Exception {
        try {
            HTTPUtils.writeHeader(null, 
                "test",
                new ByteArrayOutputStream());
            fail("should have throws null pointer");
        } catch(NullPointerException e) {
        }        
        try {
            HTTPUtils.writeHeader(HTTPHeaderName.ACCEPT_ENCODING, 
                (String)null, new ByteArrayOutputStream());
            fail("should have throws null pointer");
        } catch(NullPointerException e) {
        }        
        try {
            HTTPUtils.writeHeader(HTTPHeaderName.ACCEPT_ENCODING, 
                "test", (OutputStream)null);
            fail("should have throws null pointer");
        } catch(NullPointerException e) {
        }  
        
        OutputStream os = new ByteArrayOutputStream();
        HTTPUtils.writeHeader(HTTPHeaderName.CONTENT_LENGTH, 
            "10", os);
        
        assertEquals("unexpected header", 
            "Content-Length: 10\r\n", os.toString());
    }
    
    /**
     * Test to make sure the method for writing headers to a Writer with 
     * string values is working properly.
     * 
     * @throws Exception if an error occurs
     */
    public void testWriteHeaderWithStringToWriter() throws Exception {
        try {
            HTTPUtils.writeHeader(null, 
                "test",
                new FileWriter("test"));
            fail("should have throws null pointer");
        } catch(NullPointerException e) {
        }    
        try {
            HTTPUtils.writeHeader(HTTPHeaderName.ACCEPT_ENCODING, 
                (String)null,
                new FileWriter("test"));
            fail("should have throws null pointer");
        } catch(NullPointerException e) {
        } 
        try {
            HTTPUtils.writeHeader(HTTPHeaderName.ACCEPT_ENCODING, 
                "test",
                (Writer)null);
            fail("should have throws null pointer");
        } catch(NullPointerException e) {
        }   
        
        Writer fw = new StringWriter();
        HTTPUtils.writeHeader(HTTPHeaderName.CONNECTION, 
            "close", fw);
        
        assertEquals("unexpected header", 
            "Connection: close\r\n", fw.toString());
    }
    
    /**
     * Test to make sure the method for writing headers to a stream with 
     * HTTP values values is working properly.
     * 
     * @throws Exception if an error occurs
     */
    public void testWriteHeaderWithSHttpValueToStream() throws Exception {
        try {
            HTTPUtils.writeHeader(null, 
                ConstantHTTPHeaderValue.CLOSE_VALUE,
                new ByteArrayOutputStream());
            fail("should have throws null pointer");
        } catch(NullPointerException e) {
        }    
        try {
            HTTPUtils.writeHeader(HTTPHeaderName.ACCEPT_ENCODING, 
                (HTTPHeaderValue)null,
                new ByteArrayOutputStream());
            fail("should have throws null pointer");
        } catch(NullPointerException e) {
        } 
        try {
            HTTPUtils.writeHeader(HTTPHeaderName.ACCEPT_ENCODING, 
                ConstantHTTPHeaderValue.CLOSE_VALUE,
                (OutputStream)null);
            fail("should have throws null pointer");
        } catch(NullPointerException e) {
        }    
        
        OutputStream os = new ByteArrayOutputStream();
        HTTPUtils.writeHeader(HTTPHeaderName.CONNECTION, 
            ConstantHTTPHeaderValue.CLOSE_VALUE, os);
        
        assertEquals("unexpected header", 
            "Connection: close\r\n", os.toString());
    }
    
    /**
     * Test to make sure the method for writing headers to a Writer with 
     * HTTP values values is working properly.
     * 
     * @throws Exception if an error occurs
     */
    public void testWriteHeaderWithSHttpValueToWriter() throws Exception {
        try {
            HTTPUtils.writeHeader(null, 
                ConstantHTTPHeaderValue.CLOSE_VALUE,
                new FileWriter("test"));
            fail("should have throws null pointer");
        } catch(NullPointerException e) {
        }    
        try {
            HTTPUtils.writeHeader(HTTPHeaderName.ACCEPT_ENCODING, 
                (HTTPHeaderValue)null,
                new FileWriter("test"));
            fail("should have throws null pointer");
        } catch(NullPointerException e) {
        } 
        try {
            HTTPUtils.writeHeader(HTTPHeaderName.ACCEPT_ENCODING, 
                ConstantHTTPHeaderValue.CLOSE_VALUE,
                (Writer)null);
            fail("should have throws null pointer");
        } catch(NullPointerException e) {
        }    
        
        Writer fw = new StringWriter();
        HTTPUtils.writeHeader(HTTPHeaderName.CONNECTION, 
            ConstantHTTPHeaderValue.CLOSE_VALUE, fw);
        
        assertEquals("unexpected header", 
            "Connection: close\r\n", fw.toString());
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
        assertGreaterThan("unexpected length of header: "+header, 35, 
            header.length());
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
        assertGreaterThan("unexpected length of header: "+header, 35, 
            header.length());
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
    
    public void testParseValue() throws Exception {
        assertEquals("value", HTTPUtils.parseValue("key=value"));
        assertEquals("value", HTTPUtils.parseValue("key= value"));
        assertEquals("value", HTTPUtils.parseValue("key= value "));
        assertEquals("VAlue", HTTPUtils.parseValue("key = VAlue"));
        
        try {
            fail("got: " + HTTPUtils.parseValue("key="));
        } catch(IOException expected) {}
        
        try {
            fail("got: " + HTTPUtils.parseValue("key"));
        } catch(IOException expected) {}
        
        try {
            fail("got: " + HTTPUtils.parseValue(""));
        } catch(IOException expected) {}
    }
}
