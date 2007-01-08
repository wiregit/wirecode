package com.limegroup.gnutella.metadata;

import java.io.File;

import org.limewire.util.CommonUtils;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

/**
 * test for the parsing of OGG file's metadata
 */
public class OGGParsingTest extends LimeTestCase {
	
	static String _path = "com"+File.separator+"limegroup"+
					File.separator+"gnutella"+File.separator+"metadata"+File.separator;
	
	static File _allFields = CommonUtils.getResourceFile(_path+"oggAll.ogg");
	static File _someFields = CommonUtils.getResourceFile(_path+"oggSome.ogg");
	static File _noFields = CommonUtils.getResourceFile(_path+"oggNone.ogg");
	
	
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

}
