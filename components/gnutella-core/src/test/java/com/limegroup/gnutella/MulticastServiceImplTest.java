package com.limegroup.gnutella;

import java.net.InetAddress;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.gnutella.tests.LimeTestCase;

public class MulticastServiceImplTest extends LimeTestCase {

    public MulticastServiceImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MulticastServiceImplTest.class);
    }

    public void testCustomInterfaceIsUsedIfSetAndEnabled() throws Exception {
        ConnectionSettings.CUSTOM_NETWORK_INTERFACE.setValue(true);
        ConnectionSettings.CUSTOM_INETADRESS.set("1.2.3.4");
        InetAddress chosen = MulticastServiceImpl.chooseInterface();
        assertEquals("Expected 1.2.3.4, got " + chosen, chosen,
                InetAddress.getByAddress(new byte[]{1, 2, 3, 4}));
    }

    public void testCustomInterfaceIsNotUsedIfSetAndDisabled() throws Exception {
        ConnectionSettings.CUSTOM_NETWORK_INTERFACE.setValue(false);
        ConnectionSettings.CUSTOM_INETADRESS.set("1.2.3.4");
        InetAddress chosen = MulticastServiceImpl.chooseInterface();
        assertTrue("Expected a local address or 0.0.0.0, got " + chosen,
                chosen.isLinkLocalAddress() || chosen.isSiteLocalAddress() ||
                chosen.equals(InetAddress.getByAddress(new byte[]{0, 0, 0, 0})));
    }

    public void testCustomInterfaceIsIgnoredIfInvalid() throws Exception {
        ConnectionSettings.CUSTOM_NETWORK_INTERFACE.setValue(true);
        ConnectionSettings.CUSTOM_INETADRESS.set("B0RKEN SETTING");
        InetAddress chosen = MulticastServiceImpl.chooseInterface();
        assertTrue("Expected a local address or 0.0.0.0, got " + chosen,
                chosen.isLinkLocalAddress() || chosen.isSiteLocalAddress() ||
                chosen.equals(InetAddress.getByAddress(new byte[]{0, 0, 0, 0})));
    }
}
