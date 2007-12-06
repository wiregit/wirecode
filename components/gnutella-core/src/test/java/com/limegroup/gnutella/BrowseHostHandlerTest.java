package com.limegroup.gnutella;

import junit.framework.Test;

import org.limewire.io.Connectable;
import org.limewire.io.NetworkUtils;
import org.limewire.util.BaseTestCase;

public class BrowseHostHandlerTest extends BaseTestCase {

    public BrowseHostHandlerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BrowseHostHandlerTest.class);
    }

    public void testCreateInvalidHost() throws Exception {
        Connectable host = BrowseHostHandler.createInvalidHost();
        assertNotNull(host);
        assertEquals("0.0.0.0", host.getAddress());
        assertFalse(NetworkUtils.isValidAddress(host.getAddress()));
        assertTrue(NetworkUtils.isValidPort(host.getPort()));
    }

}
