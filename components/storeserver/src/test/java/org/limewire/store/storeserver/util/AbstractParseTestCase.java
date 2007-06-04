package org.limewire.store.storeserver.util;

import java.util.HashMap;
import java.util.Map;

import com.limegroup.gnutella.util.LimeTestCase;

import junit.framework.TestCase;

abstract class AbstractParseTestCase extends TestCase {
    
    public AbstractParseTestCase(String s) { super(s); }

	
	public void allTest() {
		t("", new String[]{ });
		t("p1=v1", new String[]{ "p1", "v1"});
		t("p1=", new String[]{ "p1", ""});
		t("p1", new String[]{ "p1", null});
		t("p1=v1|p2=v2", new String[]{ "p1", "v1", "p2", "v2"});
		t("p1=v1|p2=v2|p3=v3", new String[]{ "p1", "v1", "p2", "v2", "p3", "v3"});
	}	
	
	abstract Map<String, String> parse(String line);
	abstract String sep();
	
	private void t(String line, String[] arr) {
		line = line.replace("|",sep());
		Map<String, String> want = new HashMap<String, String>();
		for (int i = 0; i<arr.length; i += 2) want.put(arr[i], arr[i+1]);
		Map<String, String> have = parse(line);
		assertEquals(line, want, have);
	}	
}
