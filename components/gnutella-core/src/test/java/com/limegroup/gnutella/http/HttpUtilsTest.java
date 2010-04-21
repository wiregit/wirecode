package com.limegroup.gnutella.http;

import java.io.IOException;

import junit.framework.Test;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.limewire.gnutella.tests.LimeTestCase;


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
			String curValue = HttpTestUtils.extractHeaderValue(headers[i]);
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
    
    public void testGetStartPoint()throws Exception {        
//      Content-Range = "Content-Range" ":" content-range-spec
//                  content-range-spec      = byte-content-range-spec
//                  byte-content-range-spec = bytes-unit SP
//                                            byte-range-resp-spec "/"
//                                            ( instance-length | "*" )
//                  byte-range-resp-spec = (first-byte-pos "-" last-byte-pos)
//                                                 | "*"
//                  instance-length           = 1*DIGIT
        
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        
        assertEquals(0, HTTPUtils.getStartPoint(response)); // no header, start point: 0
        
        TestHeader range = new TestHeader("Content-Range");
        response.addHeader(range);
        
        try {
            HTTPUtils.getStartPoint(response);
            fail("expected IOX");
        } catch(IOException expected) {}
        
        range.setValue("foo");
        try {
            HTTPUtils.getStartPoint(response);
            fail("expected IOX");
        } catch(IOException expected) {}
        
        range.setValue("bytes"); // requires a space after 'bytes', as in 'bytes '
        try {
            HTTPUtils.getStartPoint(response);
            fail("expected IOX");
        } catch(IOException expected) {}
        
        range.setValue("bytes "); // but, a space but itself won't do it.
        try {
            HTTPUtils.getStartPoint(response);
            fail("expected IOX");
        } catch(IOException expected) {}
        
        range.setValue("bytes *");
        assertEquals(0, HTTPUtils.getStartPoint(response));
        range.setValue("bytes */*");
        assertEquals(0, HTTPUtils.getStartPoint(response));
        range.setValue("bytes */50");
        assertEquals(0, HTTPUtils.getStartPoint(response));
        
        range.setValue("bytes 50");
        try {
            HTTPUtils.getStartPoint(response);
            fail("expected IOX");
        } catch(IOException expected) {}
        
        range.setValue("bytes 50/100");
        try {
            HTTPUtils.getStartPoint(response);
            fail("expected IOX");
        } catch(IOException expected) {}
        
        range.setValue("bytes 50/*");
        try {
            HTTPUtils.getStartPoint(response);
            fail("expected IOX");
        } catch(IOException expected) {}
        
        range.setValue("bytes 0-1/5");
        assertEquals(0, HTTPUtils.getStartPoint(response));
        range.setValue("bytes 0-1/*");
        assertEquals(0, HTTPUtils.getStartPoint(response));
        range.setValue("bytes 3-5/7");
        assertEquals(3, HTTPUtils.getStartPoint(response));
        range.setValue("bytes 3-5/*");
        assertEquals(3, HTTPUtils.getStartPoint(response));
        range.setValue("bytes 8 - 15 / 32");
        assertEquals(8, HTTPUtils.getStartPoint(response));
        range.setValue("bytes 8 - 15 / *");
        assertEquals(8, HTTPUtils.getStartPoint(response));
    }
    
    private static class TestHeader implements Header {
        private final String name;
        private String value = "";
        
        public TestHeader(String name) {
            this.name = name;
        }
        
        void setValue(String value) {
            this.value = value;
        }

        @Override
        public HeaderElement[] getElements() throws ParseException {
            return new BasicHeader(name, value).getElements();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return new BasicHeader(name, value).toString();
        }

    }
}
