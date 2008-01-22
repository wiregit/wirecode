package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;

import org.limewire.util.TestUtils;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

/**
 * test for the parsing of ASF files.
 */
public class ASFParsingTest extends LimeTestCase {
	
	public ASFParsingTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return buildTestSuite(ASFParsingTest.class);
	}
	
	public void testWMASimpleDescription() throws IOException {
	    File f = TestUtils.getResourceFile("com/limegroup/gnutella/metadata/simple description.wma");
	    assertTrue(f.exists());
	    
	    ASFParser parser = new ASFParser(f);
	    assertEquals("Normal Author", parser.getArtist());
	    assertEquals("Normal Title", parser.getTitle());
	    assertEquals("Normal Copyright", parser.getCopyright());
	    assertEquals("Normal Description", parser.getComment());
	    assertEquals("Normal Rating", parser.getRating());
	    assertEquals(315, parser.getBitrate());
	    assertEquals(null, parser.getLicenseInfo());
	    assertEquals(-1, parser.getTrack());
	    assertEquals(null, parser.getAlbum());
	    assertEquals(null, parser.getYear());
	    assertEquals(null, parser.getGenre());
	    assertEquals(0, parser.getLength());
	    assertEquals(-1, parser.getWidth());
	    assertEquals(-1, parser.getHeight());
	    assertEquals(null, parser.getWeedInfo());
	    assertEquals(null, parser.getWRMXML());
	    assertTrue(parser.hasAudio());
	    assertFalse(parser.hasVideo());
	    
	    AudioMetaData data = (AudioMetaData)MetaData.parse(f);
	    assertEquals("Normal Author", data.getArtist());
	    assertEquals("Normal Title", data.getTitle());
	    assertEquals("Normal Copyright", data.getLicense());
	    assertEquals("Normal Description", data.getComment());
	    assertEquals(315, data.getBitrate());
	    assertEquals(null, data.getLicenseType());
	    assertEquals(-1, data.getTrack());
	    assertEquals(null, data.getAlbum());
	    assertEquals(null, data.getYear());
	    assertEquals(null, data.getGenre());
	    assertEquals(0, data.getLength());
	    assertEquals(-1, data.getTotalTracks());
	    assertEquals(-1, data.getDisk());
	    assertEquals(-1, data.getTotalDisks());
    }


	public void testWMAExtendedDescription() throws IOException {
	    File f = TestUtils.getResourceFile("com/limegroup/gnutella/metadata/extended description.wma");
	    assertTrue(f.exists());
	    
	    ASFParser parser = new ASFParser(f);
	    assertEquals("An Artist", parser.getArtist());
	    assertEquals("A Title", parser.getTitle());
	    assertEquals("This is a copyright", parser.getCopyright());
	    assertEquals("This is a comment", parser.getComment());
	    assertEquals("", parser.getRating());
	    assertEquals(192, parser.getBitrate());
	    assertEquals(null, parser.getLicenseInfo());
	    assertEquals(1, parser.getTrack());
	    assertEquals("An Album", parser.getAlbum());
	    assertEquals("2005", parser.getYear());
	    assertEquals("Acoustic", parser.getGenre());
	    assertEquals(0, parser.getLength());
	    assertEquals(-1, parser.getWidth());
	    assertEquals(-1, parser.getHeight());
	    assertEquals(null, parser.getWeedInfo());
	    assertEquals(null, parser.getWRMXML());
	    assertTrue(parser.hasAudio());
	    assertFalse(parser.hasVideo());
	    
	    AudioMetaData data = (AudioMetaData)MetaData.parse(f);
	    assertEquals("An Artist", data.getArtist());
	    assertEquals("A Title", data.getTitle());
	    assertEquals("This is a copyright", data.getLicense());
	    assertEquals("This is a comment", data.getComment());
	    assertEquals(192, data.getBitrate());
	    assertEquals(null, data.getLicenseType());
	    assertEquals(1, data.getTrack());
	    assertEquals("An Album", data.getAlbum());
	    assertEquals("2005", data.getYear());
	    assertEquals("Acoustic", data.getGenre());
	    assertEquals(0, data.getLength());
	    assertEquals(-1, data.getTotalTracks());
	    assertEquals(-1, data.getDisk());
	    assertEquals(-1, data.getTotalDisks());
    }

	public void testASFVBR() throws IOException {
	    File f = TestUtils.getResourceFile("com/limegroup/gnutella/metadata/vbr encoding.asf");
	    assertTrue(f.exists());
	    
	    ASFParser parser = new ASFParser(f);
	    assertEquals("Another Artist", parser.getArtist());
	    assertEquals("Another Title", parser.getTitle());
	    assertEquals("None", parser.getCopyright());
	    assertEquals("This is a small comment.", parser.getComment());
	    assertEquals("", parser.getRating());
	    assertEquals(7686, parser.getBitrate());
	    assertEquals(null, parser.getLicenseInfo());
	    assertEquals(-1, parser.getTrack());
	    assertEquals("Another Album", parser.getAlbum());
	    assertEquals("2001", parser.getYear());
	    assertEquals("My Own", parser.getGenre());
	    assertEquals(0, parser.getLength());
	    assertEquals(-1, parser.getWidth());
	    assertEquals(-1, parser.getHeight());
	    assertEquals(null, parser.getWeedInfo());
	    assertEquals(null, parser.getWRMXML());
	    assertTrue(parser.hasAudio());
	    assertFalse(parser.hasVideo());
	    
	    AudioMetaData data = (AudioMetaData)MetaData.parse(f);
	    assertEquals("Another Artist", data.getArtist());
	    assertEquals("Another Title", data.getTitle());
	    assertEquals("None", data.getLicense());
	    assertEquals("This is a small comment.", data.getComment());
	    assertEquals(7686, data.getBitrate());
	    assertEquals(null, data.getLicenseType());
	    assertEquals(-1, data.getTrack());
	    assertEquals("Another Album", data.getAlbum());
	    assertEquals("2001", data.getYear());
	    assertEquals("My Own", data.getGenre());
	    assertEquals(0, data.getLength());
	    assertEquals(-1, data.getTotalTracks());
	    assertEquals(-1, data.getDisk());
	    assertEquals(-1, data.getTotalDisks());
    }
}
