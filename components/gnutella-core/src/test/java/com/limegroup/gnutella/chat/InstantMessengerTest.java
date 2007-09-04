package com.limegroup.gnutella.chat;


import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.StubSocket;
import com.limegroup.gnutella.util.SocketsManager;

public class InstantMessengerTest extends BaseTestCase {

    public InstantMessengerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(InstantMessengerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testIsOutgoing() {
        InstantMessenger im = new InstantMessengerImpl("host", 1234, new ActivityCallbackStub(), new SocketsManager());
        assertTrue(im.isOutgoing());
        assertEquals("host", im.getHost());
        assertEquals(1234, im.getPort());
        
        im = new InstantMessengerImpl(new StubSocket(), new ActivityCallbackStub());
        assertFalse(im.isOutgoing());
        assertEquals("1.2.3.4", im.getHost());
        assertEquals(1234, im.getPort());
    }
    
}
