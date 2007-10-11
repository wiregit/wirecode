package org.limewire.lws.server;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.limewire.lws.server.Util;

/**
 * Tests basic communication.
 * 
 * @author jpalm
 */
public class GiveKeyTest extends AbstractCommunicationTest {

    public GiveKeyTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(GiveKeyTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testGiveKey() {
        final String privateKey = getPrivateKey();
        assertTrue(privateKey, Util.isValidPrivateKey(privateKey));
    }

}
