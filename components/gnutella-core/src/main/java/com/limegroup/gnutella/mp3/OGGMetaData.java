
package com.limegroup.gnutella.mp3;
import java.io.*;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;

import java.util.Properties;

/**
 * this file parses comments from an ogg file
 * 
 * for general packet specs see:
 * <url>http://www.xiph.org/ogg/vorbis/doc/vorbis-spec-ref.html</url>
 * 
 * for comment spec see:
 * <url>http://www.xiph.org/ogg/vorbis/doc/v-comment.html</url>
 * and
 * <url>http://reactor-core.org/ogg-tag-recommendations.html</url>
 */
public class OGGMetaData extends AudioMetaData{
	
	File _file;
	Properties _props;
	FileInputStream _fis;
	
	//a set of recommended headers by the spec:
	//note we parse only those tags relevant to the Lime XML Audio schema
	
	private static final String TITLE_TAG = "TITLE";
	private static final String TRACK_TAG = "TRACKNUMBER";
	private static final String ALBUM_TAG = "ALBUM";
	private static final String GENRE_TAG = "GENRE";
	private static final String DATE_TAG = "DATE";
	private static final String COMMENT_TAG = "COMMENT";
	private static final String ARTIST_TAG = "ARTIST";
	
	public OGGMetaData(File f) throws IOException{
		_file = f;
		_fis = new FileInputStream(_file);
		_props = new Properties();
		
		parseIdHeader();
	}
	
	private void parseIdHeader() throws IOException {
		//throw new Error("not implemented");
		//read the 0 byte header
	}
	
}
