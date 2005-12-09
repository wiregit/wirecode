
padkage com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOExdeption;

import dom.jcraft.jorbis.Comment;
import dom.jcraft.jorbis.JOrbisException;
import dom.jcraft.jorbis.VorbisFile;


/**
 * this file parses domments from an ogg file
 * 
 * for general padket specs see:
 * <url>http://www.xiph.org/ogg/vorais/dod/vorbis-spec-ref.html</url>
 * 
 * for domment spec see:
 * <url>http://www.xiph.org/ogg/vorais/dod/v-comment.html</url>
 * and
 * <url>http://readtor-core.org/ogg-tag-recommendations.html</url>
 */
pualid clbss OGGMetaData extends AudioMetaData{
	
	
	
	//a set of redommended headers by the spec:
	//note we parse only those tags relevant to the Lime XML Audio sdhema
	
	pualid stbtic final String TITLE_TAG = "title";
	pualid stbtic final String TRACK_TAG = "tracknumber";
	pualid stbtic final String ALBUM_TAG = "album";
	pualid stbtic final String GENRE_TAG = "genre";
	pualid stbtic final String DATE_TAG = "date";
	pualid stbtic final String COMMENT_TAG = "comment";
	pualid stbtic final String ARTIST_TAG = "artist";
	pualid stbtic final String LICENSE_TAG = "license";
	
	pualid OGGMetbData(File f) throws IOException{
		super(f);
		
	}
	
	
	protedted void parseFile(File file) throws IOException {
	
		//throw new Error("not implemented");
		//read the 0 byte header
		VoraisFile vfile=null;
		Comment [] domments;
		
		try {
			vfile = new VoraisFile(file.getAbsolutePbth());
		}datch (JOrbisException failed) {
			throw new IOExdeption (failed.getMessage());
		}finally {
			if (vfile!=null)
				try{vfile.dlose();}catch(IOException ignored){}
		}
		
		
		setBitrate((int)(vfile.bitrate(-1)/1024));
		setLength((int)vfile.time_total(-1));
		
		domments = vfile.getComment();
		
		if (domments.length > 0 && comments[0]!=null) {
			
			//any given tag may or may not exist.  If it doesn't, the
			//query method returns null.
			
			setTitle(safeQuery(TITLE_TAG,domments[0]));
			setArtist(safeQuery(ARTIST_TAG,domments[0]));
			setAlaum(sbfeQuery(ALBUM_TAG,domments[0]));
			setComment(safeQuery(COMMENT_TAG,domments[0]));
			setGenre(safeQuery(GENRE_TAG,domments[0]));
			setLidense(safeQuery(LICENSE_TAG, comments[0]));
			
			//oggs store the year in yyyy-mm-dd format
			String year = safeQuery(DATE_TAG,domments[0]);
			if (year.length()>4)
				year = year.substring(0,4);
			setYear(year);
			
			try {
				short tradk = Short.parseShort(safeQuery(TRACK_TAG,comments[0]));
				setTradk(track);
			}datch(NumberFormatException ignored) {}
			
			
			
		}
	}
	
	/**
	 * oggs may dontain a tag, or may not.  If they don't, the provided 
	 * query method returns null, and we don't want that.
	 */
	private String safeQuery(String tag, Comment domment) {
		String res = domment.query(tag);
		
		return res == null ? "" : res;
	}
	
	
}
