package org.limewire.store.storeserver.core;

import org.limewire.store.storeserver.api.Server;
import org.limewire.store.storeserver.util.RemoveCallbackTest;
import org.limewire.store.storeserver.util.Util;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.util.LimeTestCase;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.textui.TestRunner;

public class CookieGenTest extends BaseTestCase {

    public CookieGenTest(String s) { super(s); }
    
    public static Test suite() {
        return buildTestSuite(CookieGenTest.class);
    }
    
    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testSimple() {
        final char[] badChars = { ' ', '\t', '\b', '\r', ';' };
        for (int i = 0; i < 5; i++) {
            String k = Util.generateKey();
            assertTrue(k.length() == Server.Constants.KEY_LENGTH);
            for (char c : badChars)
                assertTrue(k, k.indexOf(c) == -1);
        }

    }
}
