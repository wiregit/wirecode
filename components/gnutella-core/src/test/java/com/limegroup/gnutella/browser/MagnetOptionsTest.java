package com.limegroup.gnutella.browser;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.EncodingUtils;

public class MagnetOptionsTest extends BaseTestCase {

	public MagnetOptionsTest(String name) {
		super(name);
	}
	
	public void testParseValidMagnet() {
		

		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?dn="
				+ EncodingUtils.encode("compilc.:fileNm?7ßä") + "&kt="
				+ EncodingUtils.encode("keyword topic string")
				+ "&xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xs=http://127.0.0.1:6346/uri-res/N2R?urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C");
		assertEquals("Should have parsed one magnet", 1, opts.length);
		opts = MagnetOptions.parseMagnet(opts[0].toString());
		assertEquals("Should have parsed one magnet", 1, opts.length);
		
		opts = MagnetOptions.parseMagnet("magnet:?dn=" + EncodingUtils.encode("compile me") 
		+ "&xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO59&xs=http://127.0.0.1:6346/uri-res/N2R?urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO59");
		assertEquals("Should have parsed one magnet", 1, opts.length);
		opts = MagnetOptions.parseMagnet(opts[0].toString());
		assertEquals("Should have parsed one magnet", 1, opts.length);
				
		opts = MagnetOptions.parseMagnet("magnet:?xt.1=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xt.2=urn:sha1:TXGCZQTH26NL6OUQAJJPFALHG2LTGBC7");
		assertEquals("Should have parsed 2 magnets", 2, opts.length);
		
		// compound magnets
		opts = MagnetOptions.parseMagnet("magnet:?xt.1=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xt.2=urn:sha1:TXGCZQTH26NL6OUQAJJPFALHG2LTGBC7");
		assertEquals("Should have parsed 2 magnets", 2, opts.length);
	}
	
	public void testParseInvalidMagnet() {
		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?");
		assertEquals("Wrong number of parsed magnets", 0, opts.length);
	}
	
	public void testisDownloadable() {
		
		// invalid magnets
		
		// is invalid because we don't have a url
		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		
		// invalid: has empty kt
		opts = MagnetOptions.parseMagnet("magnet:?kt=&xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		
		// invalid: has only a display name
		opts = MagnetOptions.parseMagnet("magnet:?dn=me");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		
		// valid magnets
		
		// valid: has a url and a sha1
		opts = MagnetOptions.parseMagnet("magnet:?xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		
		// valid: has a url and keyword topic
		opts = MagnetOptions.parseMagnet("magnet:?kt=test&xs=http://magnet2.limewire.com:6346");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		
		// valid: has everything
		opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&dn=-weed-Soul%20Coughing-Rolling.wma&xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isDownloadable());
		
		// downloadable: has kt and hash
		opts = MagnetOptions.parseMagnet("magnet:?kt=test&xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		
		// downloadable: has dn and hash
		opts = MagnetOptions.parseMagnet("magnet:?dn=test&xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		
	}
	
	public void testIsHashOnly() {
		
		// hash only
		
		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?xt=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		assertTrue("Should be hash only", opts[0].isHashOnly());
		
		// not hash only
		
		opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&dn=-weed-Soul%20Coughing-Rolling.wma&xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isDownloadable());
		assertFalse("Should not be hash only", opts[0].isHashOnly());
		
		opts = MagnetOptions.parseMagnet("magnet:?kt=test&xs=http://magnet2.limewire.com:6346");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isDownloadable());
		assertFalse("Should not be hash only", opts[0].isHashOnly());
	}

	public void testGetFileNameForSaving() throws Exception {

		MagnetOptions magnet = MagnetOptions.createMagnet(null, "file name", null, null);
		assertEquals("file name", magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet("keywords", null, null, null);
		assertEquals("keywords", magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet("keywords", "", null, null);
		assertEquals("keywords", magnet.getFileNameForSaving());
		
		URN urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
		
		magnet = MagnetOptions.createMagnet(null, null, urn, null);
		assertEquals(urn.toString(), magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet("", "", urn, null);
		assertEquals(urn.toString(), magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet(null, null, null, new String[] { "http://www.limewire.com:4444/test.file" });
		assertEquals("test.file", magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet(null, null, null, new String[] { "http://www.limewire.com:4444/test.file?notpartofthename" });
		assertEquals("test.file", magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet("", "", null, new String[] { "http://192.168.0.2/test.file" });
		assertEquals("test.file", magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet(null, null, null, new String[] { "http://host/" });
		assertEquals("MAGNET download from host", magnet.getFileNameForSaving());
		
		magnet = MagnetOptions.createMagnet(null, null, null, null);
		assertNotNull(magnet.getFileNameForSaving());
	}
}
