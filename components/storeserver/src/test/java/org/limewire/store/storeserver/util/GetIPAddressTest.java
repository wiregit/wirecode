package org.limewire.store.storeserver.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.limewire.store.storeserver.util.Util;

import junit.framework.TestCase;

public class GetIPAddressTest extends TestCase {

    public GetIPAddressTest() {
        super("GetIPAddressTest");
    }

    public void testNull() {
        InetAddress addr = null;
        String ip = Util.getIPAddress(addr);
        assertNull(ip);
    }

    public void testSimple() {
        try {
            InetAddress addr = InetAddress.getByName("localhost");
            String ip = Util.getIPAddress(addr);
            assertEquals("127.0.0.1", ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
