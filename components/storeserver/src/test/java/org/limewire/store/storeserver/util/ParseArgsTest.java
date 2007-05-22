package org.limewire.store.storeserver.util;

import java.util.HashMap;
import java.util.Map;

import org.limewire.store.storeserver.util.Util;

import junit.framework.TestCase;

public class ParseArgsTest extends AbstractParseTestCase {

	public void test() {
		allTest();
	}

	@Override
	Map<String, String> parse(String line) {
		return Util.parseArgs(line);
	}

	@Override
	String sep() {
		return "&";
	}
}
