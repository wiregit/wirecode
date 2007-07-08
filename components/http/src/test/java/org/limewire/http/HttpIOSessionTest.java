package org.limewire.http;

import junit.framework.TestCase;

import org.apache.http.nio.reactor.EventMask;
import org.apache.http.nio.reactor.IOEventDispatch;

public class HttpIOSessionTest extends TestCase {

    public void testEventMask() {
        StubSocket socket = new StubSocket();
        HttpIOSession session = new HttpIOSession(socket);
        HttpChannel channel = new StubHttpChannel(session, new MockIOEventDispatch());
        session.setHttpChannel(channel);
        
        assertEquals(0, session.getEventMask());
        session.setEvent(EventMask.WRITE);
        assertEquals(EventMask.WRITE, session.getEventMask());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());

        session.clearEvent(EventMask.READ);
        assertEquals(EventMask.WRITE, session.getEventMask());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());

        session.setEvent(EventMask.READ);
        assertEquals(EventMask.READ_WRITE, session.getEventMask());
        assertTrue(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());

        session.setEventMask(EventMask.WRITE);
        assertEquals(EventMask.WRITE, session.getEventMask());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());

        session.setEvent(EventMask.WRITE);
        assertEquals(EventMask.WRITE, session.getEventMask());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());

        session.clearEvent(EventMask.WRITE);
        assertEquals(0, session.getEventMask());
        assertFalse(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        session.setEventMask(EventMask.READ);
        assertEquals(EventMask.READ, session.getEventMask());
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        session.clearEvent(EventMask.READ);
        assertEquals(0, session.getEventMask());
        assertFalse(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        session.setEventMask(EventMask.READ);
        assertEquals(EventMask.READ, session.getEventMask());
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        session.clearEvent(EventMask.WRITE);
        assertEquals(EventMask.READ, session.getEventMask());
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        session.setEventMask(EventMask.READ_WRITE);
        session.clearEvent(EventMask.WRITE);
        assertEquals(EventMask.READ, session.getEventMask());
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        session.setEventMask(EventMask.READ_WRITE);
        session.clearEvent(EventMask.READ);
        assertEquals(EventMask.WRITE, session.getEventMask());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
    }

    private static class StubHttpChannel extends HttpChannel {

        public StubHttpChannel(HttpIOSession session,
                IOEventDispatch eventDispatch) {
            super(session, eventDispatch);
        }
        
    }

}
