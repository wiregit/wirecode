package org.limewire.store.server;

import org.limewire.store.server.Util;
import org.limewire.util.BaseTestCase;

import junit.framework.Test;
import junit.textui.TestRunner;

public class KeyGenTest extends BaseTestCase {

    public KeyGenTest(String s) { super(s); }
    
    public static Test suite() {
        return buildTestSuite(KeyGenTest.class);
    }
    
    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testSimple() {
        final char[] badChars = { ' ', '\t', '\b', '\r', ';' };
        for (int i = 0; i < 5; i++) {
            String k = Util.generateKey();
            assertTrue(k.length() == DispatcherSupport.Constants.KEY_LENGTH);
            for (char c : badChars)
                assertTrue(k, k.indexOf(c) == -1);
        }

    }
}
