
package com.limegroup.gnutella.mp3;
import java.io.*;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;

import com.jcraft.jogg.*;
import com.jcraft.jorbis.*;
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
	
	//a set of recommended headers by the spec:
	//note we parse only those tags relevant to the Lime XML Audio schema
	
	private static final String TITLE_TAG = "title";
	private static final String TRACK_TAG = "tracknumber";
	private static final String ALBUM_TAG = "album";
	private static final String GENRE_TAG = "genre";
	private static final String DATE_TAG = "date";
	private static final String COMMENT_TAG = "comment";
	private static final String ARTIST_TAG = "artist";
	
	public OGGMetaData(File f) throws IOException{
		_file = f;
		
		parseOGGFile();
		
	}
	
	/**
	 * populates the Properties object with data read from the ogg file
	 * also populates length and bitrate fields
	 * @throws IOException parsing failed
	 */
	private void parseOGGFile() throws IOException {
	
		//throw new Error("not implemented");
		//read the 0 byte header
		VorbisFile vfile;
		Comment [] comments;
		
		try {
			vfile = new VorbisFile(_file.getAbsolutePath());
			
		}catch (JOrbisException failed) {
			throw new IOException (failed.getMessage());
		}
		
		
		setBitrate((int)(vfile.bitrate(-1)/1024));
		setLength((int)vfile.time_total(-1));
		
		comments = vfile.getComment();
		if (comments.length > 0) {
			
			
			setTitle(comments[0].query(TITLE_TAG));
			setArtist(comments[0].query(ARTIST_TAG));
			setAlbum(comments[0].query(ALBUM_TAG));
			setComment(comments[0].query(COMMENT_TAG));
			setGenre(comments[0].query(GENRE_TAG));
			
			//oggs store the year in yyyy-mm-dd format
			String year = comments[0].query(DATE_TAG);
			if (year != null && year.length()>4)
				year = year.substring(4);
			setYear(year);
			
			try {
				short track = Short.parseShort(comments[0].query(TRACK_TAG));
				setTrack(track);
			}catch(NumberFormatException ignored) {}
			
			
			
		}
	}
	
	
	
}
