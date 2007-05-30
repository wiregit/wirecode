package org.limewire.store.storeserver.util;

import org.limewire.store.storeserver.api.Server;
import org.limewire.store.storeserver.util.Util;

import junit.framework.TestCase;

public class RemoveCallbackTest extends TestCase {
	
	final static char Q = Server.Constants.CALLBACK_QUOTE;
	
	public void testNull() {
		String s = null;
		assertEquals(s, Util.removeCallback(s));
	}
	
	public void testEmpty() {
		String s = "";
		assertEquals(s, Util.removeCallback(s));
	}

	public void testNone() {
		String s = "test";
		assertEquals(s, Util.removeCallback(s));
	}
	
	public void testNoRightParen() {
		String s = "test(" + Q + "insides" + Q;
		assertEquals(s, Util.removeCallback(s));
	}
	
	public void testNoLeftParen() {
		String s = "test" + Q + "insides" + Q + ")";
		assertEquals(s, Util.removeCallback(s));
	}
	
	public void testNoRightQuote() {
		String s = "test(" + Q + "insides" + ")";
		assertEquals(s, Util.removeCallback(s));
	}
	
	public void testNoLeftQuote() {
		String s = "test(" + "insides" + Q + ")";
		assertEquals(s, Util.removeCallback(s));
	}
	
	public void testOK() {
		String s = "test(" + Q + "insides" + Q + ")";
		assertEquals("insides", Util.removeCallback(s));
	}
}
