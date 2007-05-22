package org.limewire.store.storeserver.util;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class AddURLEncodedArgumentsTest extends TestCase {

    public void testNull() {
        String cmd = null;
        Map<String, String> args = new HashMap<String, String>();
        String want = null;
        String have = Util.addURLEncodedArguments(cmd, args);
        assertNull(have);
    }
}
