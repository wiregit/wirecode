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
        PerCallSocketBindingSettingsImpl settings = new PerCallSocketBindingSettingsImpl(InetAddress.getLocalHost(), 0);
        assertEquals(InetAddress.getLocalHost().getHostAddress(), settings.getAddressToBindTo());
    }
    
    public void testGetAddressToBindToNullAddress() throws UnknownHostException {
        PerCallSocketBindingSettingsImpl settings = new PerCallSocketBindingSettingsImpl(null, 0);
        assertNull(settings.getAddressToBindTo());
    }
    
    public void testGetPortToBindTo() throws UnknownHostException {
        PerCallSocketBindingSettingsImpl settings = new PerCallSocketBindingSettingsImpl(InetAddress.getLocalHost(), 12345);
        assertEquals(12345, settings.getPortToBindTo());
    }
    
    public void testGetPortToBindToZeroPort() throws UnknownHostException {
        PerCallSocketBindingSettingsImpl settings = new PerCallSocketBindingSettingsImpl(InetAddress.getLocalHost(), 0);
        assertEquals(0, settings.getPortToBindTo());
    }
    
    public void testGetPortToBindToNegativePort() throws UnknownHostException {
        try {
            PerCallSocketBindingSettingsImpl settings = new PerCallSocketBindingSettingsImpl(InetAddress.getLocalHost(), -1);
            fail();
        } catch (IllegalArgumentException  e) {
            // expected result    
        }
    }
    
    public void testIsSocketBindingRequired() throws UnknownHostException {
        PerCallSocketBindingSettingsImpl settings = new PerCallSocketBindingSettingsImpl(InetAddress.getLocalHost(), 0);
        assertTrue(settings.isSocketBindingRequired());
        settings = new PerCallSocketBindingSettingsImpl(null, 5);
        assertTrue(settings.isSocketBindingRequired());
    }
}
