package org.limewire.http;

import java.io.IOException;

import junit.framework.TestCase;

public class AbstractHttpNIOEntityTest extends TestCase {

    public void testGetContent() throws Exception {
        MockHttpNIOEntity entity = new MockHttpNIOEntity();
        try {
            entity.getContent();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            entity.isOpen();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            entity.isRepeatable();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            entity.isStreaming();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            entity.shutdown();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        try {
            entity.writeTo(null);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    public void testGetContentType() {
        MockHttpNIOEntity entity = new MockHttpNIOEntity();
        entity.setContentType("type");
        assertEquals("type", entity.getContentType().getValue());
    }

    public void testIsChunked() {
        MockHttpNIOEntity entity = new MockHttpNIOEntity();
        assertFalse(entity.isChunked());
    }

    private class MockHttpNIOEntity extends AbstractHttpNIOEntity {

        @Override
        public void finished() {
        }

        @Override
        public long getContentLength() {
            return 0;
        }

        @Override
        public boolean handleWrite() throws IOException {
            return false;
        }

        @Override
        public void initialize() throws IOException {
        }

        @Override
        public void timeout() {
        }
        
    }
    
}
