package com.limegroup.gnutella.browser;

import com.limegroup.gnutella.util.BaseTestCase;

public class MagnetOptionsTest extends BaseTestCase {

	public MagnetOptionsTest(String name) {
		super(name);
	}
	
	public void testisDownloadable() {
		
		// invalid magnets
		
		// is invalid because we don't have a url
		MagnetOptions[] opts = ExternalControl.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should be invalid", opts[0].isDownloadable());
		
		// invalid: has empty kt
		opts = ExternalControl.parseMagnet("magnet:?kt=&xs=http://bear.limewire.com:6346");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should be invalid", opts[0].isDownloadable());
		
		// invalid: has only a display name
		opts = ExternalControl.parseMagnet("magnet:?dn=me");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should be invalid", opts[0].isDownloadable());
		
		// valid magnets
		
		// valid: has a url and a sha1
		opts = ExternalControl.parseMagnet("magnet:?xs=http://bear.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		
		// valid: has a url and keyword topic
		opts = ExternalControl.parseMagnet("magnet:?kt=test&xs=http://bear.limewire.com:6346");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		
		// valid: has everything
		opts = ExternalControl.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&dn=-weed-Soul%20Coughing-Rolling.wma&xs=http://bear.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isDownloadable());
		
	}
	
	public void testIsHashOnly() {
		
		// hash only
		
		MagnetOptions[] opts = ExternalControl.parseMagnet("magnet:?xt=http://bear.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		assertTrue("Should be hash only", opts[0].isHashOnly());
		
		// not hash only
		
		opts = ExternalControl.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&dn=-weed-Soul%20Coughing-Rolling.wma&xs=http://bear.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isDownloadable());
		assertFalse("Should not be hash only", opts[0].isHashOnly());
		
		opts = ExternalControl.parseMagnet("magnet:?kt=test&xs=http://bear.limewire.com:6346");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isDownloadable());
		assertFalse("Should not be hash only", opts[0].isHashOnly());
	}

}
