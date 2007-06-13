package org.limewire.store.server;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.limewire.store.server.Util;

/**
 * Tests basic communication.
 * 
 * @author jpalm
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
