package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;

import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.connection.ReadBufferChannel;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.http.SimpleReadHeaderState;

public class HTTPDownloaderTest extends com.limegroup.gnutella.util.LimeTestCase {

    public HTTPDownloaderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(HTTPDownloaderTest.class);
    }
    
    public void testParseContentRange() throws Throwable {
        int length = 1000;
        RemoteFileDesc rfd= new RemoteFileDesc("1.2.3.4", 1, 1, "file",
                                            length, new byte[16], 1,
                                            false, 2, false, null,
                                            null, false, false, "LIME",
                                            null, -1);
        File f = new File("sam");
        VerifyingFile vf = new VerifyingFile(length);
        vf.open(f);
        HTTPDownloader dl = new HTTPDownloader(rfd, vf, false);
        
        PrivilegedAccessor.setValue(dl, "_amountToRead", new Integer(rfd.getSize()));
        
        
        assertEquals(new Interval(1, 9), 
                    parseContentRange(dl, "Content-range: bytes 1-9/10"));
                        
        assertEquals(new Interval(1, 9),
                    parseContentRange(dl, "Content-range:bytes=1-9/10"));
                        
        // should this work?  the server says the size is 10, we think it's 
        // 1000.  throw IllegalArgumentException or ProblemReadingHeader?
        assertEquals(new Interval(0, 999),
                    parseContentRange(dl, "Content-range:bytes */10"));
                        
        assertEquals(new Interval(0, 999),
                    parseContentRange(dl, "Content-range:bytes */*"));
                    
        assertEquals(new Interval(1, 9),
                    parseContentRange(dl, "Content-range:bytes 1-9/*"));
                    
        // should this work?  the server says the size is 10, we think it's
        // 1000.  throw IllegalArgumentException or ProblemReadingHeader?
        // Putting aside the "should it work" question, this is the faulty
        // header from LimeWire 0.5, in which we subtract 1 from the
        // sizes (they send exclusive instead of inclusive values).
        assertEquals(new Interval(0, 9),
                    parseContentRange(dl, "Content-range:bytes 1-10/10"));
                    
        Interval iv = null;        
        try {
            iv = parseContentRange(dl, "Content-range:bytes 1 10 10");
            fail("Parsed invalid content range.  Got: " + iv);
        } catch (ProblemReadingHeaderException ignored) {}
        
        // low is less than high
        try {
            iv = parseContentRange(dl, "Content-range:bytes 10-9/*");
            fail("Parsed invalid content range.  Got: " + iv);
        } catch(ProblemReadingHeaderException ignored) {}
        
        // negative values.
        try {
            iv = parseContentRange(dl, "Content-range: bytes -10--5/*");
            fail("Parsed invalid content range.  Got: " + iv);
        } catch(ProblemReadingHeaderException ignored) {}
        
        // negative high.
        try {
            iv = parseContentRange(dl, "Content-range:bytes 0--10/*");
            fail("Parsed invalid content range.  Got: " + iv);
        } catch(ProblemReadingHeaderException ignored) {}
    }
    
    public void testLegacy() throws Throwable {
        //readHeaders tests
		String str;
		HTTPDownloader down;

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


		str = "HTTP/1.1 200 OK\r\nUser-Agent: LimeWire\r\n";
		down = newHTTPDownloader(str);
		readHeaders(down);
		down.stop();
		
		str = "200 OK\r\n";
		down = newHTTPDownloader(str);
		try {
			readHeaders(down);
			down.stop();
			fail("exception should have been thrown.");
		} catch (NoHTTPOKException e) {}
	}
	
	private static Interval parseContentRange(HTTPDownloader dl,
                                              String s) throws Throwable {
	    try {
            return (Interval)PrivilegedAccessor.invokeMethod(
                dl , "parseContentRange", new Object[] {s});
        } catch(Exception e) {
            if ( e.getCause() != null ) 
                throw e.getCause();
            else throw e;
        }
    }
    
    private static HTTPDownloader newHTTPDownloader(String s) throws Throwable {
        s += "\r\n";
        SimpleReadHeaderState reader = new SimpleReadHeaderState(null, 100, 2048);
        reader.process(new ReadBufferChannel(s.getBytes()), ByteBuffer.allocate(1024));
        RemoteFileDesc rfd = new RemoteFileDesc("", 1, 1, "file", 1, new byte[16], 1, 
                                                false, 1, false, null, Collections.EMPTY_SET,
                                                false, false, "", Collections.EMPTY_SET, 1);
        HTTPDownloader d = new HTTPDownloader(rfd, null, false);
        PrivilegedAccessor.setValue(d, "_headerReader", reader);
        return d;
    }
    
    private static void readHeaders(HTTPDownloader d) throws Throwable {
        try {
            PrivilegedAccessor.invokeMethod(d, "parseHeaders", null);
        } catch(Exception e) {
            if ( e.getCause() != null ) 
                throw e.getCause();
            else throw e;
        }
    }
}
