package org.limewire.swarm.http.listener;

import java.io.IOException;

import junit.framework.Test;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.Range;
import org.limewire.swarm.file.FileCoordinator;
import org.limewire.swarm.file.WriteJob;
import org.limewire.util.BaseTestCase;

public class FileContentListenerTest extends BaseTestCase {
    
    private Mockery mockery;
    private FileCoordinator fileCoordinator;
    private Range expectedRange;
    private FileContentListener fileContentListener;

    public FileContentListenerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FileContentListenerTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        mockery = new Mockery();
        fileCoordinator = mockery.mock(FileCoordinator.class);
        expectedRange = Range.createRange(5, 10);
        fileContentListener = new FileContentListener(fileCoordinator, expectedRange);
    }
    
    public void testRangeReleasesWhenFinished() throws Exception {
        mockery.checking(new Expectations() {{
            one(fileCoordinator).unlease(expectedRange);
        }});
        
        fileContentListener.finished();
        fileContentListener.finished();
        
        mockery.assertIsSatisfied();
    }
    
    public void testConsumePortionReleasesLess() throws Exception {
        final WriteJob writeJob = mockery.mock(WriteJob.class);
        final ContentDecoder decoder = mockery.mock(ContentDecoder.class);
        final IOControl ioctrl = mockery.mock(IOControl.class);
        
        mockery.checking(new Expectations() {{
            one(fileCoordinator).newWriteJob(5, ioctrl);
                will(returnValue(writeJob));
            one(writeJob).consumeContent(decoder);
                will(returnValue(2L));
            one(writeJob).consumeContent(decoder);
                will(returnValue(1L));
            one(fileCoordinator).unlease(Range.createRange(8, 10));
        }});
        
        fileContentListener.contentAvailable(decoder, ioctrl);
        fileContentListener.contentAvailable(decoder, ioctrl);
        fileContentListener.finished();
        
        mockery.assertIsSatisfied();
    }
    
    public void testConsumeAllReleasesNothing() throws Exception {
        final WriteJob writeJob = mockery.mock(WriteJob.class);
        final ContentDecoder decoder = mockery.mock(ContentDecoder.class);
        final IOControl ioctrl = mockery.mock(IOControl.class);
        
        mockery.checking(new Expectations() {{
            one(fileCoordinator).newWriteJob(5, ioctrl);
                will(returnValue(writeJob));
            one(writeJob).consumeContent(decoder);
                will(returnValue(6L));
        }});
        
        fileContentListener.contentAvailable(decoder, ioctrl);
        fileContentListener.finished();
        
        mockery.assertIsSatisfied();
    }
    
    public void testInitializeNeedsPositiveContentLength() {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, null));
        response.addHeader(new BasicHeader("Content-Length", "-1"));
        try {
            fileContentListener.initialize(response);
            fail();
        } catch(IOException expected) {
            assertEquals("Invalid content length: " + -1, expected.getMessage());
        }
    }
    
    public void testInitializeExplicitRange() throws Exception {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, null));
        response.addHeader(new BasicHeader("Content-Length", "6"));
        response.addHeader(new BasicHeader("Content-Range", "bytes 5-10/*"));
        
        mockery.checking(new Expectations() {{
            one(fileCoordinator).unlease(expectedRange);
        }});
        
        fileContentListener.initialize(response);
        fileContentListener.finished();
        
        mockery.assertIsSatisfied();
    }
    
    public void testInitializeInvalidRangeContentRange() throws Exception {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, null));
        response.addHeader(new BasicHeader("Content-Length", "6"));
        response.addHeader(new BasicHeader("Content-Range", "5-10/*"));
        
        try {
            fileContentListener.initialize(response);
            fail();
        } catch(IOException expected) {}
    }
    
    public void testInitializeShrinksRange() throws Exception {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, null));
        response.addHeader(new BasicHeader("Content-Length", "6"));
        response.addHeader(new BasicHeader("Content-Range", "bytes 6-8/*"));
        
        mockery.checking(new Expectations() {{
            one(fileCoordinator).unlease(Range.createRange(5));
            one(fileCoordinator).unlease(Range.createRange(9, 10));
            one(fileCoordinator).unlease(Range.createRange(6, 8));
        }});
        
        fileContentListener.initialize(response);
        fileContentListener.finished();
        
        mockery.assertIsSatisfied();
    }
    
    public void testInitializeDefaultsToContentLength() throws Exception {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, null));
        response.addHeader(new BasicHeader("Content-Length", "5"));
        
        fileContentListener = new FileContentListener(fileCoordinator, Range.createRange(0, 10));        
        mockery.checking(new Expectations() {{
            one(fileCoordinator).unlease(Range.createRange(5, 10));
            one(fileCoordinator).unlease(Range.createRange(0, 4));
        }});
        
        fileContentListener.initialize(response);
        fileContentListener.finished();
        
        mockery.assertIsSatisfied();
    }
    
    public void testInitializeWithoutAnyHeaderDefaultsToFileSize() throws Exception {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, null));
        
        fileContentListener = new FileContentListener(fileCoordinator, Range.createRange(0, 19));
        
        mockery.checking(new Expectations() {{
            one(fileCoordinator).getCompleteFileSize();
                will(returnValue(20L));
            one(fileCoordinator).unlease(Range.createRange(0, 19));
        }});
        
        fileContentListener.initialize(response);
        fileContentListener.finished();
        
        mockery.assertIsSatisfied();
    }
    
    public void testContentRangeRequiresContentLength() throws Exception {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, null));
        response.addHeader(new BasicHeader("Content-Range", "bytes 6-8/*"));
        
        try {
            fileContentListener.initialize(response);
            fail();
        } catch(IOException expected) {
            assertEquals("No content length, though content range existed.", expected.getMessage());
        }
    }
    
    public void testExpandedLowerRangeFails() throws Exception {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, null));
        response.addHeader(new BasicHeader("Content-Length", "3"));
        response.addHeader(new BasicHeader("Content-Range", "bytes 0-6/*"));
        
        try {
            fileContentListener.initialize(response);
            fail();
        } catch(IOException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith("Invalid actual range"));
        }
    }
    
    public void testExpandedHigherRangeFails() throws Exception {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, null));
        response.addHeader(new BasicHeader("Content-Length", "3"));
        response.addHeader(new BasicHeader("Content-Range", "bytes 5-11/*"));
        
        try {
            fileContentListener.initialize(response);
            fail();
        } catch(IOException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith("Invalid actual range"));
        }
    }
    
    public void testExpandedLowerAndHigherRangeFails() throws Exception {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, null));
        response.addHeader(new BasicHeader("Content-Length", "3"));
        response.addHeader(new BasicHeader("Content-Range", "bytes 0-10/*"));
        
        try {
            fileContentListener.initialize(response);
            fail();
        } catch(IOException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith("Invalid actual range"));
        }
    }
    
    public void testBadRangeFails() throws Exception {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, null));
        response.addHeader(new BasicHeader("Content-Length", "3"));
        response.addHeader(new BasicHeader("Content-Range", "bytes 6-5/*"));
        
        try {
            fileContentListener.initialize(response);
            fail();
        } catch(IOException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith("invalid range"));
        }
    }
}
