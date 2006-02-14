package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.util.CommonUtils;

/**
 * Test the public methods of the <tt>FileDesc</tt> class.
 */
public final class FileDescTest extends com.limegroup.gnutella.util.BaseTestCase {

	private final String[] uncontainedURNStrings = {
		"urn:sha1:CLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:XLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"Urn:sha1:ZLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"uRn:sHa1:FLRTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:DLPTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:Sha1:SLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"UrN:sha1:ALSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sHa1:QLSTIIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:WLSTXIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:ELSTTIPQGSSZTS5FJUPAKUZWUGYQYPFB"
	};

	private Set _uncontainedUrnSet;

	public FileDescTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(FileDescTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	protected void setUp() throws Exception {		
		_uncontainedUrnSet = new HashSet();
		for(int i=0; i<10; i++) {
			try {
				_uncontainedUrnSet.add(URN.createSHA1Urn(uncontainedURNStrings[i]));
			} catch(IOException e) {
				assertTrue("unexpected exception "+e+"\r\n"+
						   uncontainedURNStrings[i], false);
			}
		}
	}

	/**
	 * Tests the FileDesc construcotor.
	 */
	public void testConstructor() throws Exception {
        File file = CommonUtils.getResourceFile("build.xml");
        Set urns = calculateAndCacheURN(file);
        
		try {
			new FileDesc(null, urns, 0);
			fail("null file should not be permitted for FileDesc constructor");
		} catch(NullPointerException ignored) {}
        
        try {
            new FileDesc(file, null, 0);
            fail("null urns should not be permitted for FileDesc constructor");
        } catch(NullPointerException ignored) {}
        
        try {
            new FileDesc(file, urns, -1);
            fail("negative index should not be permitted for FileDesc constructor");
        } catch(IndexOutOfBoundsException ignored) {}
        
        try {
            new FileDesc(file, Collections.EMPTY_SET, 0);
            fail("no sha1 urn should not be permitted for FileDesc constructor");
        } catch(IllegalArgumentException ignored) {}
	}
}
