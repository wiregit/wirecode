package com.limegroup.gnutella.downloader;

import java.io.IOException;

import junit.framework.Test;

import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.util.PrivilegedAccessor;

public class HTTPDownloaderTest extends com.limegroup.gnutella.util.BaseTestCase {

    public HTTPDownloaderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(HTTPDownloaderTest.class);
    }

	public void testLegacy() throws Throwable {
        assertEquals(1, parseContentRangeStart("Content-range: bytes 1-9/10"));
        assertEquals(1, parseContentRangeStart("Content-range:bytes=1-9/10"));
        assertEquals(0, parseContentRangeStart("Content-range:bytes */10"));
        assertEquals(0, parseContentRangeStart("Content-range:bytes */*"));
        assertEquals(1, parseContentRangeStart("Content-range:bytes 1-9/*"));
        assertEquals(1, parseContentRangeStart("Content-range:bytes 1-9/*"));
        assertEquals(0, parseContentRangeStart("Content-range:bytes 1-10/10"));
        
        try {
            parseContentRangeStart("Content-range:bytes 1 10 10");
            fail("Exception should be thrown with faulty content range");
        } catch (IOException ignored) {}           
        

        //readHeaders tests
		String str;
		HTTPDownloader down;
		boolean ok = true;

		str = "HTTP/1.1 200 OK\r\n";
		down = newHTTPDownloader(str);
		readHeaders(down);
        down.stop();
		
		
		str = "HTTP/1.1 301 Moved Permanently\r\n";
		down = newHTTPDownloader(str);
		try {
			readHeaders(down);
			down.stop();
			fail("exception should have been thrown");
		} catch (IOException e) {}

        str = "HTTP/1.1 300 Multiple Choices\r\n";
		down = newHTTPDownloader(str);
		try {
			readHeaders(down);
			down.stop();
			fail("exception should have been thrown");
		} catch (IOException e) {}

		str = "HTTP/1.1 404 File Not Found \r\n";
		down = newHTTPDownloader(str);
		try {
			readHeaders(down);
			down.stop();
			fail("exception should have been thrown");
		} catch (FileNotFoundException e) {}

		str = "HTTP/1.1 410 Not Sharing \r\n";
		down = newHTTPDownloader(str);
		try {
			readHeaders(down);
			down.stop();
			fail("exception should have been thrown");
		} catch (NotSharingException e) {}

		str = "HTTP/1.1 412 \r\n";
		down = newHTTPDownloader(str);
		try {
			readHeaders(down);
			down.stop();
			fail("exception should have been thrown");
		} catch (IOException e) {}

		str = "HTTP/1.1 503 \r\n";
		down = newHTTPDownloader(str);
		try {
			readHeaders(down);
			down.stop();
			fail("exception should have been thrown");
		} catch (TryAgainLaterException e) {}

		str = "HTTP/1.1 210 \r\n";
		down = newHTTPDownloader(str);
		readHeaders(down);
		down.stop();

		str = "HTTP/1.1 204 Partial Content\r\n";
		down = newHTTPDownloader(str);
        readHeaders(down);
        down.stop();


		str = "HTTP/1.1 200 OK\r\nUser-Agent: LimeWire\r\n\r\nx";
		down = newHTTPDownloader(str);
		readHeaders(down);
		assertEquals('x', 
		    ((ByteReader)PrivilegedAccessor.getValue(down,"_byteReader")).read());
		down.stop();
		
		str = "200 OK\r\n";
		down = newHTTPDownloader(str);
		try {
			readHeaders(down);
			down.stop();
			fail("exception should have been thrown.");
		} catch (NoHTTPOKException e) {}
	}
	
	private static int parseContentRangeStart(String s) throws Throwable {
	    try {
            return ((Integer)PrivilegedAccessor.invokeMethod(
                HTTPDownloader.class, "parseContentRangeStart", 
                new Object[] {s})).intValue();
        } catch(Exception e) {
            if ( e.getCause() != null ) 
                throw e.getCause();
            else throw e;
        }
    }
    
    private static HTTPDownloader newHTTPDownloader(String s) throws Throwable {
        return (HTTPDownloader)PrivilegedAccessor.invokeConstructor(
            HTTPDownloader.class, new Object[] {s});
    }
    
    private static void readHeaders(HTTPDownloader d) throws Throwable {
        try {
            PrivilegedAccessor.invokeMethod(d, "readHeaders", null);
        } catch(Exception e) {
            if ( e.getCause() != null ) 
                throw e.getCause();
            else throw e;
        }
    }
}
