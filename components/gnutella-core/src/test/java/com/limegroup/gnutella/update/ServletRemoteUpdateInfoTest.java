package com.limegroup.gnutella.update;

import junit.framework.*;
import junit.extensions.*;

/**
 * Class for testing updates.  This may change frequently as updates
 * change.
 */
public final class ServletRemoteUpdateInfoTest extends TestCase {

	private static final String[] OPERATING_SYSTEMS = {
		"Windows 95",
		"Windows NT",
		"Windows 98",
		"Windows 2000",
		"Windows XP",
		"Windows ME",
		"Linux",
		"Solaris",
		"Mac OS",
		"Mac OS X"
	};

	//private final String CURRENT_VERSION = CommonUtils.getLimeWireVersion();

	private static final String[] VERSIONS = {
		"2.3.3", "2.4.4" 
	};

	/**
	 * Constructs a new <tt>ServletRemoteUpdateInfoTest</tt> intance
	 * with the specified name.
	 */
	public ServletRemoteUpdateInfoTest(String name) {
		super(name);
	}

	/**
	 * Runs this suite of tests.
	 */
	public static Test suite() {
		return new TestSuite(ServletRemoteUpdateInfoTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}	

	/**
	 * Tests update with multiple values for operating system and LimeWire
	 * version.
	 */
	public void testVaryingUpdateValues() {
		try {
			for(int i=0; i<OPERATING_SYSTEMS.length; i++) {
				ServletLocalUpdateInfo localInfo = new ServletLocalUpdateInfo();
				localInfo.addKeyValuePair(localInfo.OS, OPERATING_SYSTEMS[i]);
				for(int j=0; j<VERSIONS.length; j++) {
					localInfo.addKeyValuePair(localInfo.LIMEWIRE_VERSION, VERSIONS[j]);
					ServletRemoteUpdateInfo srui = 
						new ServletRemoteUpdateInfo(localInfo);
					String urlString = srui.getURLEncodedString();
					
					ClientRemoteUpdateInfo crui = new ClientRemoteUpdateInfo();
					crui.addRemoteInfo(urlString);
					Updator updator = crui.getUpdator();
					if(OPERATING_SYSTEMS[i].startsWith("Win")) {
						assertTrue("should be a web page updator: "+updator, 
								   (updator instanceof WebPageUpdator));
					} else if(OPERATING_SYSTEMS[i].startsWith("Mac")) {
						assertTrue("should be a web page updator: "+updator, 
								   (updator instanceof WebPageUpdator));
					} else {
						assertTrue("should be a message updator: "+updator, 
								   (updator instanceof DisplayMessageUpdator));					
					}
				}
			}
		} catch(Exception e) {
			fail("unexpected exception: "+e);
		}
	}
}
