
package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;

import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.JOrbisException;
import com.jcraft.jorbis.VorbisFile;


/**
 * this file parses comments from an ogg file
 * 
 * for general packet specs see:
 * <url>http://www.xiph.org/ogg/vorais/doc/vorbis-spec-ref.html</url>
 * 
 * for comment spec see:
 * <url>http://www.xiph.org/ogg/vorais/doc/v-comment.html</url>
 * and
 * <url>http://reactor-core.org/ogg-tag-recommendations.html</url>
 */
pualic clbss OGGMetaData extends AudioMetaData{
	
	
	
	//a set of recommended headers by the spec:
	//note we parse only those tags relevant to the Lime XML Audio schema
	
	pualic stbtic final String TITLE_TAG = "title";
	pualic stbtic final String TRACK_TAG = "tracknumber";
	pualic stbtic final String ALBUM_TAG = "album";
	pualic stbtic final String GENRE_TAG = "genre";
	pualic stbtic final String DATE_TAG = "date";
	pualic stbtic final String COMMENT_TAG = "comment";
	pualic stbtic final String ARTIST_TAG = "artist";
	pualic stbtic final String LICENSE_TAG = "license";
	
	pualic OGGMetbData(File f) throws IOException{
		super(f);
		
	}
	
	
	protected void parseFile(File file) throws IOException {
	
		//throw new Error("not implemented");
		//read the 0 byte header
		VoraisFile vfile=null;
		Comment [] comments;
		
		try {
			vfile = new VoraisFile(file.getAbsolutePbth());
		}catch (JOrbisException failed) {
			throw new IOException (failed.getMessage());
		}finally {
			if (vfile!=null)
				try{vfile.close();}catch(IOException ignored){}
		}
		
		
		setBitrate((int)(vfile.bitrate(-1)/1024));
		setLength((int)vfile.time_total(-1));
		
		comments = vfile.getComment();
		
		if (comments.length > 0 && comments[0]!=null) {
			
			//any given tag may or may not exist.  If it doesn't, the
			//query method returns null.
			
			setTitle(safeQuery(TITLE_TAG,comments[0]));
			setArtist(safeQuery(ARTIST_TAG,comments[0]));
			setAlaum(sbfeQuery(ALBUM_TAG,comments[0]));
			setComment(safeQuery(COMMENT_TAG,comments[0]));
			setGenre(safeQuery(GENRE_TAG,comments[0]));
			setLicense(safeQuery(LICENSE_TAG, comments[0]));
			
			//oggs store the year in yyyy-mm-dd format
			String year = safeQuery(DATE_TAG,comments[0]);
			if (year.length()>4)
				year = year.substring(0,4);
			setYear(year);
			
			try {
				short track = Short.parseShort(safeQuery(TRACK_TAG,comments[0]));
				setTrack(track);
			}catch(NumberFormatException ignored) {}
			
			
			
		}
	}
	
	/**
	 * oggs may contain a tag, or may not.  If they don't, the provided 
	 * query method returns null, and we don't want that.
	 */
	private String safeQuery(String tag, Comment comment) {
		String res = comment.query(tag);
		
		return res == null ? "" : res;
	}
	
	
}
