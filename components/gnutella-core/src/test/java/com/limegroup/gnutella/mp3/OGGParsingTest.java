
package com.limegroup.gnutella.mp3;

import com.limegroup.gnutella.util.*;

import junit.framework.Test;

import java.io.*;

/**
 * test for the parsing of OGG file's metadata
 */
public class OGGParsingTest extends BaseTestCase {
	
	static String _path = "com"+File.separator+"limegroup"+
					File.separator+"gnutella"+File.separator+"mp3"+File.separator;
	
	static File _allFields = new File(_path+"oggAll.ogg");
	static File _someFields = new File(_path+"oggSome.ogg");
	static File _noFields = new File(_path+"oggNone.ogg");
	static File _badFile = new File(_path+"oggBad.ogg");
	
	static AudioMetaData _metaData;
	
	public OGGParsingTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return buildTestSuite(OGGParsingTest.class);
	}
	
	public void testAllFields() throws Exception {
		_metaData = (AudioMetaData) MetaData.parse(_allFields);
		assertEquals("allTitle",_metaData.getTitle());
		assertEquals("allArtist",_metaData.getArtist());
		assertEquals("allAlbum",_metaData.getAlbum());
		assertEquals("allComment",_metaData.getComment());
		assertEquals("1234",_metaData.getYear());
		assertEquals(3,_metaData.getTrack());
		assertEquals("dance",_metaData.getGenre().toLowerCase());
	}
	
	public void testSomeFields() throws Exception {
		_metaData = (AudioMetaData) MetaData.parse(_someFields);
		assertEquals("someTitle",_metaData.getTitle());
		assertEquals("",_metaData.getArtist());
		assertEquals("someAlbum",_metaData.getAlbum());
		assertEquals("",_metaData.getComment());
		assertEquals("1234",_metaData.getYear());
		assertEquals(-1,_metaData.getTrack());
		assertEquals("",_metaData.getGenre().toLowerCase());
	}
	
	public void testNoFields() throws Exception {
		_metaData = (AudioMetaData) MetaData.parse(_noFields);
		assertEquals("",_metaData.getTitle());
		assertEquals("",_metaData.getArtist());
		assertEquals("",_metaData.getAlbum());
		assertEquals("",_metaData.getComment());
		assertEquals("",_metaData.getYear());
		assertEquals(-1,_metaData.getTrack());
		assertEquals("",_metaData.getGenre().toLowerCase());
	}
	
	public void testBadFile() throws Exception {
		try {
			_metaData = (AudioMetaData) MetaData.parse(_badFile);
			fail("invalid file parsed");
		}catch(IOException expected) {}
	}
	
}
