package org.limewire.store.server;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests basic communication.
 */
public class EchoMessageTest extends AbstractCommunicationTest {

    public EchoMessageTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(EchoMessageTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testEchoMsg() {
        final String pk = getPrivateKey();
        assertEquals(DispatcherSupport.Responses.OK, doAuthenticate(pk));
        final String want = "test";
        final String have = doEcho(pk, want);
        assertEquals(want, have);
    }
}
