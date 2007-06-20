package org.limewire.store.server;

import java.util.regex.Pattern;

import org.limewire.store.server.Util;
import org.limewire.util.BaseTestCase;

import junit.framework.Test;
import junit.textui.TestRunner;

public class CreateCookieDateTest extends BaseTestCase {
    
    public CreateCookieDateTest(String s) { super(s); }
    
    public static Test suite() {
        return buildTestSuite(KeyGenTest.class);
    }
    
    
    public static void main(String[] args) {
        TestRunner.run(suite());
    }

	public void test() {
		String str = Util.createCookieDate();
		//
		// Wdy, DD-Mon-YYYY HH:MM:SS GMT
		// Thu, 10-05-2007 15:53:12 GMT
		//
		boolean b = Pattern.matches(
                "[A-Z][a-z][a-z], \\d\\d-\\d\\d-\\d\\d\\d\\d \\d\\d:\\d\\d:\\d\\d GMT", 
                str);
		assertTrue(str, b);
	}
}
