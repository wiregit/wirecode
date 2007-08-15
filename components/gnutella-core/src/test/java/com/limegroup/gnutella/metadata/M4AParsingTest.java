package com.limegroup.gnutella.metadata;

import java.io.File;

import org.limewire.util.CommonUtils;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

/**
 * test for the parsing of M4A file's metadata
 */
public class M4AParsingTest extends LimeTestCase {
	
	static String _path = "com"+File.separator+"limegroup"+
					File.separator+"gnutella"+File.separator+"metadata"+File.separator;
	
	static File _file = CommonUtils.getResourceFile(_path+"Purr.m4a");
	
	
	
	static AudioMetaData _metaData;
	
	public M4AParsingTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return buildTestSuite(M4AParsingTest.class);
	}
	
	public void testParsing() throws Exception {
		_metaData = (AudioMetaData) MetaData.parse(_file);
		
		//note: this specific file has whitespaces around the strings.
		//trim()-ing is not needed in general.
		
		assertEquals("Purr",_metaData.getTitle().trim());
		assertEquals("I am the Artist",_metaData.getArtist().trim());
		assertEquals("What do you want?",_metaData.getAlbum().trim());
		assertEquals("Some comments",_metaData.getComment().trim());
		assertEquals("don't know",_metaData.getGenre().trim().toLowerCase());
	}
	

	


}
