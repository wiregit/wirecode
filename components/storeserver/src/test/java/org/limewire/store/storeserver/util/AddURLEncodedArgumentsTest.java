package org.limewire.store.storeserver.util;

import java.util.HashMap;
import java.util.Map;

import org.limewire.util.BaseTestCase;

import junit.framework.Test;
import junit.textui.TestRunner;

public class AddURLEncodedArgumentsTest extends BaseTestCase {
    
    public AddURLEncodedArgumentsTest(String s) { super(s); }
    
    public static Test suite() {
        return buildTestSuite(AddURLEncodedArgumentsTest.class);
    }
    
    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testNull() {
        String cmd = null;
        Map<String, String> args = new HashMap<String, String>();
        String have = Util.addURLEncodedArguments(cmd, args);
        assertNull(have);
    }
}
