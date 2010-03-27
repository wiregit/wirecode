package com.limegroup.gnutella.browser;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;
import org.limewire.util.EncodingUtils;

import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.URN;

public class MagnetOptionsTest extends BaseTestCase {

    MagnetOptions[] validMagnets;
    private String[] guidMagnets;
    private Mockery context;
    
    @Override
    public void setUp() {
        validMagnets = MagnetOptions.parseMagnet("magnet:?xt.1=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xt.2=urn:sha1:TXGCZQTH26NL6OUQAJJPFALHG2LTGBC7");
        guidMagnets = new String[] {
                "magnet:?xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xs=" + URN.createGUIDUrn(new GUID()),
                "magnet:?xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xs=http://127.0.0.1:6346/uri-res/N2R?urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xs=" + URN.createGUIDUrn(new GUID()),
                "magnet:?xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&as=" + URN.createGUIDUrn(new GUID()),
                "magnet:?xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xs=" + URN.createGUIDUrn(new GUID()),
        };
        context = new Mockery();
    }

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
        
        // BitTorrent
		opts = MagnetOptions.parseMagnet("magnet:?xt=urn:btih:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&tr=http://127.0.0.1/tracker/");
        assertEquals("Should have parsed one magnet", 1, opts.length);
	}
	
	public void testParseInvalidMagnet() {
		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?");
		assertEquals("Wrong number of parsed magnets", 0, opts.length);
	}
	
	public void testIsGnutellaDownloadable() {
		
		// invalid magnets
		
		// is invalid because we don't have a url
		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isGnutellaDownloadable());
		
		// invalid: has empty kt
		opts = MagnetOptions.parseMagnet("magnet:?kt=&xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isGnutellaDownloadable());
		
		// invalid: has only a display name
		opts = MagnetOptions.parseMagnet("magnet:?dn=me");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isGnutellaDownloadable());
		
		// valid magnets
		
		// valid: has a url and a sha1
		opts = MagnetOptions.parseMagnet("magnet:?xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isGnutellaDownloadable());
		
		// valid: has a url and keyword topic
		opts = MagnetOptions.parseMagnet("magnet:?kt=test&xs=http://magnet2.limewire.com:6346");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isGnutellaDownloadable());
		
		// valid: has everything
		opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&dn=-weed-Soul%20Coughing-Rolling.wma&xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isGnutellaDownloadable());
		
		// downloadable: has kt and hash and filesize
		opts = MagnetOptions.parseMagnet("magnet:?kt=test&xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&xl=15");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isGnutellaDownloadable());
		
		// downloadable: has dn and hash and filesize
		opts = MagnetOptions.parseMagnet("magnet:?dn=test&xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&xl=18");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isGnutellaDownloadable());
		
	}
	
	public void testIsTorrentDownloadable() {
	    // Not downloadable: has a tracker URL but no SHA1
	    MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?xs=http://127.0.0.1/data/foo&tr=http://127.0.0.1/tracker/");
	    assertEquals("Wrong number of parsed magnets", 1, opts.length);
	    assertFalse("Should not be downloadable", opts[0].isTorrentDownloadable());

	    // Downloadable: has a tracker URL and a SHA1 (XS, urn:sha1)
	    opts = MagnetOptions.parseMagnet("magnet:?xs=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&tr=http://127.0.0.1/tracker/");
	    assertEquals("Wrong number of parsed magnets", 1, opts.length);
	    assertTrue("Should be downloadable", opts[0].isTorrentDownloadable());

	    // Downloadable: has a tracker URL and a SHA1 (XT, urn:sha1)
	    opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&tr=http://127.0.0.1/tracker/");
	    assertEquals("Wrong number of parsed magnets", 1, opts.length);
	    assertTrue("Should be downloadable", opts[0].isTorrentDownloadable());

	    // Downloadable: has a tracker URL and a SHA1 (AS, urn:sha1)
	    opts = MagnetOptions.parseMagnet("magnet:?as=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&tr=http://127.0.0.1/tracker/");
	    assertEquals("Wrong number of parsed magnets", 1, opts.length);
	    assertTrue("Should be downloadable", opts[0].isTorrentDownloadable());

	    // Downloadable: has a tracker URL and a SHA1 (XT, urn:btih)
	    opts = MagnetOptions.parseMagnet("magnet:?xt=urn:btih:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&tr=http://127.0.0.1/tracker/");
	    assertEquals("Wrong number of parsed magnets", 1, opts.length);
	    assertTrue("Should be downloadable", opts[0].isTorrentDownloadable());

	    // Not downloadable: has a SHA1 (XT, urn:btih) but no tracker URL
	    opts = MagnetOptions.parseMagnet("magnet:?xt=urn:btih:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&xs=http://127.0.0.1/data/foo");
	    assertEquals("Wrong number of parsed magnets", 1, opts.length);
	    assertTrue("Should be downloadable", opts[0].isTorrentDownloadable());

	    // Downloadable: has everything
	    opts = MagnetOptions.parseMagnet("magnet:?xs=http://127.0.0.1/data/foo&xt=urn:btih:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&tr=http://127.0.0.1/tracker/&dn=Foo&as=http://127.0.0.1/backup/foo&kt=Foo&xl=1234567");
	    assertEquals("Wrong number of parsed magnets", 1, opts.length);
	    assertTrue("Should be downloadable", opts[0].isTorrentDownloadable());
	}

	public void testIsHashOnly() {
		
		// hash only
		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?xt=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isGnutellaDownloadable());
		assertTrue("Should be hash only", opts[0].isHashOnly());
		
		// not hash only
		
		opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&dn=-weed-Soul%20Coughing-Rolling.wma&xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isGnutellaDownloadable());
		assertFalse("Should not be hash only", opts[0].isHashOnly());
		
		opts = MagnetOptions.parseMagnet("magnet:?kt=test&xs=http://magnet2.limewire.com:6346");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isGnutellaDownloadable());
		assertFalse("Should not be hash only", opts[0].isHashOnly());
		assertTrue(opts[0].getGUIDUrns().isEmpty());
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
    
    public void testParseValidMagnets() {
        String magnets = createMultiLineMagnetLinks(validMagnets);
        MagnetOptions[] opts = MagnetOptions.parseMagnets(magnets);
        assertEquals("Should have parsed " + validMagnets.length + " magnets", validMagnets.length, opts.length);
        // and parse again
        assertEquals("Should have parsed " + opts.length + " magnets", opts.length, 
                MagnetOptions.parseMagnets(createMultiLineMagnetLinks(opts)).length);
        
        // compound magnets with multiple lines
        opts = MagnetOptions.parseMagnets
            ("magnet:?xt.1=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xt.2=urn:sha1:TXGCZQTH26NL6OUQAJJPFALHG2LTGBC7"
                    + System.getProperty("line.separator")
                    + "magnet:?xt.1=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xt.2=urn:sha1:TXGCZQTH26NL6OUQAJJPFALHG2LTGBC7");
        assertEquals("Should have parsed 2 magnets", 4, opts.length);
    }
    
    public void testMagnetOptionsParsesGuidUrns() {
        String prefix = "magnet:?dn=filename&kt=keyword&xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xs=http://127.0.0.1:6346/uri-res/N2R?urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C";
        String guidUrn = "urn:guid:" + new GUID().toHexString();
        String magnet = prefix + "&xs=" + guidUrn;
        MagnetOptions opts = MagnetOptions.parseMagnet(magnet)[0];
        assertContains(opts.getXS(), guidUrn);
        
        // same for alternate sources
        magnet = prefix + "&as=" + guidUrn;
        opts = MagnetOptions.parseMagnet(magnet)[0];
        assertContains(opts.getAS(), guidUrn);
    }
    
    public void testGetGUIDUrns() {
        List<String> magnets = new ArrayList<String>(Arrays.asList(guidMagnets));
        // and add them as all upper case too
        for (String magnet : guidMagnets) {
            magnets.add(magnet.toUpperCase(Locale.US));
        }
        for (String magnet : magnets) {
            MagnetOptions options = MagnetOptions.parseMagnet(magnet)[0];
            Collection<URN> urns = options.getGUIDUrns();
            assertFalse(urns.isEmpty());
            for (URN urn : urns) {
                assertTrue(urn.isGUID());
            }
        }
        
        // magnets without GUID urns
        for (MagnetOptions magnet : validMagnets) {
            assertTrue(magnet.getGUIDUrns().isEmpty());
        }
    }
    
    public void testCreateMagnetWithGUIDUrn() throws Exception {
        final GUID guid = new GUID();
        final URN urn = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKOZWUGZQYPFB");
        final FileDetails fileDetails = context.mock(FileDetails.class);
        context.checking(new Expectations() {{
            allowing(fileDetails).getFileName();
            will(returnValue("filename"));
            allowing(fileDetails).getSHA1Urn();
            will(returnValue(urn));
            // first time a zero file size
            one(fileDetails).getSize();
            will(returnValue(-1L));
            one(fileDetails).getSize();
            will(returnValue(123456789L));
        }});
        MagnetOptions magnet = MagnetOptions.createMagnet(fileDetails, null, guid.bytes());
        assertTrue(magnet.toExternalForm().contains(guid.toHexString()));
        assertEquals(urn, magnet.getSHA1Urn());
        assertEquals(URN.createGUIDUrn(guid), magnet.getGUIDUrns().iterator().next());
        assertFalse(magnet.toExternalForm().contains("xl"));
        assertEquals(-1, magnet.getFileSize());
        
        magnet = MagnetOptions.createMagnet(fileDetails, new InetSocketAddress("127.0.0.1", 5555), guid.bytes());
        assertTrue(magnet.toExternalForm().contains(guid.toHexString()));
        assertEquals(urn, magnet.getSHA1Urn());
        assertEquals(URN.createGUIDUrn(guid), magnet.getGUIDUrns().iterator().next());
        assertEquals(2, magnet.getXS().size());
        assertTrue(magnet.toExternalForm().contains("127.0.0.1:5555"));
        assertEquals(1, magnet.getDefaultURLs().length);
        assertTrue(magnet.toExternalForm().contains("xl=123456789"));
        assertEquals(123456789, magnet.getFileSize());
    }
    
    public void testCreateMagnetOptionsWithGuidUrns() throws Exception {
        GUID guid = new GUID();
        URN guidUrn = URN.createGUIDUrn(guid);
        URN sha1Urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        
        MagnetOptions magnet = MagnetOptions.createMagnet("hello", "fileName", sha1Urn, null, Collections.singleton(guidUrn));
        
        assertEquals(Collections.singleton(guidUrn), magnet.getGUIDUrns());
        assertTrue(magnet.toExternalForm().contains(guidUrn.httpStringValue()));
    }
     
    public void testMagnetUrlsAreEncoded() {
        MagnetOptions[] magnets = MagnetOptions.parseMagnet("magnet:?xs=http://hello.world.com/path with spaces/?query=has space");
        assertEquals(1, magnets.length);
        assertEquals(1, magnets[0].getDefaultURLs().length);
        assertEquals("http://hello.world.com/path%20with%20spaces/?query=has%20space", magnets[0].getDefaultURLs()[0]);
    }
    
    public void testMagnetUrlsAreNotReencoded() {
        MagnetOptions[] magnets = MagnetOptions.parseMagnet("magnet:?xs=http%3A%2F%2Ftest.com%2Fhello%2520you%26test%3Dtrue");
        assertEquals(1, magnets.length);
        assertEquals(1, magnets[0].getDefaultURLs().length);
        assertEquals("http://test.com/hello%20you&test=true", magnets[0].getDefaultURLs()[0]);
    }
    
    public void testParseMagnetFileSizeWithTrailingSpace() {
        MagnetOptions[] magnets = MagnetOptions.parseMagnet("magnet:?dn=hello.test&xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xl=123%20");
        assertEquals(1, magnets.length);
        assertEquals(123, magnets[0].getFileSize());
        assertTrue(magnets[0].isGnutellaDownloadable());
        magnets = MagnetOptions.parseMagnet("magnet:?dn=hello.test&xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xl=123 ");
        assertEquals(1, magnets.length);
        assertEquals(123, magnets[0].getFileSize());
        assertTrue(magnets[0].isGnutellaDownloadable());
    }
    
    private String createMultiLineMagnetLinks(MagnetOptions[] opts) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < opts.length; i++) {
            if (i > 0) {
                buffer.append(System.getProperty("line.separator"));
            }
            buffer.append(opts[i].toString());
        }
        return buffer.toString();
    }    
}
