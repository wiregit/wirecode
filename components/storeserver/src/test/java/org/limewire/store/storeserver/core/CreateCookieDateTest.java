package org.limewire.store.storeserver.core;

import java.util.regex.Pattern;

import org.limewire.store.storeserver.util.RemoveCallbackTest;
import org.limewire.store.storeserver.util.Util;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.util.LimeTestCase;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.textui.TestRunner;

public class CreateCookieDateTest extends BaseTestCase {
    
    public CreateCookieDateTest(String s) { super(s); }
    
    public static Test suite() {
        return buildTestSuite(CookieGenTest.class);
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
