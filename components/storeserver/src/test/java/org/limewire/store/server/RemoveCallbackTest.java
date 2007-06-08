package org.limewire.store.server;

import org.limewire.store.server.Util;
import org.limewire.util.BaseTestCase;

import junit.framework.Test;
import junit.textui.TestRunner;

public class RemoveCallbackTest extends BaseTestCase {
    
    public RemoveCallbackTest(String s) { super(s); }
    
    public static Test suite() {
        return buildTestSuite(RemoveCallbackTest.class);
    }
    
    public static void main(String[] args) {
        TestRunner.run(suite());
    }
	
	final static char Q = DispatcherSupport.Constants.CALLBACK_QUOTE;
	
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
