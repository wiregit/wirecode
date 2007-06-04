package org.limewire.store.storeserver.core;

import java.util.regex.Pattern;

import org.limewire.store.storeserver.util.Util;

import com.limegroup.gnutella.util.LimeTestCase;


import junit.framework.TestCase;

public class CreateCookieDateTest extends TestCase {
    
    public CreateCookieDateTest() { super("CreateCookieDateTest"); }

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
