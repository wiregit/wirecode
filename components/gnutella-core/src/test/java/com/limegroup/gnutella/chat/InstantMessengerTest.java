package com.limegroup.gnutella.chat;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.stubs.ActivityCallbackStub;
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
        InstantMessenger im = new InstantMessenger("host", 1234, new ActivityCallbackStub(), new SocketsManager());
        assertTrue(im.isOutgoing());
        assertEquals("host", im.getHost());
        assertEquals(1234, im.getPort());
        
        im = new InstantMessenger(new StubSocket(), new ActivityCallbackStub());
        assertFalse(im.isOutgoing());
        assertEquals("1.2.3.4", im.getHost());
        assertEquals(1234, im.getPort());
    }

    private class StubSocket extends Socket {
        @Override
        public InetAddress getInetAddress() {
            try {
                return InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 });
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        
        @Override
        public int getPort() {
            return 1234;
        }
    }
    
}
