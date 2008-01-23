package org.limewire.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;

public class SocketBindingSettingsImplTest extends TestCase {
    public SocketBindingSettingsImplTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testGetAddressToBindTo() throws UnknownHostException {
        SocketBindingSettingsImpl settings = new SocketBindingSettingsImpl(InetAddress.getLocalHost(), 0);
        assertEquals(InetAddress.getLocalHost().getHostAddress(), settings.getAddressToBindTo());
    }
    
    public void testGetAddressToBindToNullAddress() throws UnknownHostException {
        SocketBindingSettingsImpl settings = new SocketBindingSettingsImpl(null, 0);
        assertNull(settings.getAddressToBindTo());
    }
    
    public void testGetPortToBindTo() throws UnknownHostException {
        SocketBindingSettingsImpl settings = new SocketBindingSettingsImpl(InetAddress.getLocalHost(), 12345);
        assertEquals(12345, settings.getPortToBindTo());
    }
    
    public void testGetPortToBindToZeroPort() throws UnknownHostException {
        SocketBindingSettingsImpl settings = new SocketBindingSettingsImpl(InetAddress.getLocalHost(), 0);
        assertEquals(0, settings.getPortToBindTo());
    }
    
    public void testGetPortToBindToNegativePort() throws UnknownHostException {
        SocketBindingSettingsImpl settings = new SocketBindingSettingsImpl(InetAddress.getLocalHost(), -1);
        assertEquals(0, settings.getPortToBindTo());
        settings = new SocketBindingSettingsImpl(InetAddress.getLocalHost(), -25);
        assertEquals(0, settings.getPortToBindTo());
    }
    
    public void testIsSocketBindingRequired() throws UnknownHostException {
        SocketBindingSettingsImpl settings = new SocketBindingSettingsImpl(InetAddress.getLocalHost(), 0);
        assertTrue(settings.isSocketBindingRequired());
        settings = new SocketBindingSettingsImpl(null, 5);
        assertTrue(settings.isSocketBindingRequired());
    }
    
    public void testIsSocketBindingRequiredNegative() throws UnknownHostException {
        SocketBindingSettingsImpl settings = new SocketBindingSettingsImpl(null, 0);
        assertFalse(settings.isSocketBindingRequired());
        settings = new SocketBindingSettingsImpl(null, -25);
        assertFalse(settings.isSocketBindingRequired());
    }
}
