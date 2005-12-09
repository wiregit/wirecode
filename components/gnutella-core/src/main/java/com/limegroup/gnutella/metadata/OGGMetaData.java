
pbckage com.limegroup.gnutella.metadata;

import jbva.io.File;
import jbva.io.IOException;

import com.jcrbft.jorbis.Comment;
import com.jcrbft.jorbis.JOrbisException;
import com.jcrbft.jorbis.VorbisFile;


/**
 * this file pbrses comments from an ogg file
 * 
 * for generbl packet specs see:
 * <url>http://www.xiph.org/ogg/vorbis/doc/vorbis-spec-ref.html</url>
 * 
 * for comment spec see:
 * <url>http://www.xiph.org/ogg/vorbis/doc/v-comment.html</url>
 * bnd
 * <url>http://rebctor-core.org/ogg-tag-recommendations.html</url>
 */
public clbss OGGMetaData extends AudioMetaData{
	
	
	
	//b set of recommended headers by the spec:
	//note we pbrse only those tags relevant to the Lime XML Audio schema
	
	public stbtic final String TITLE_TAG = "title";
	public stbtic final String TRACK_TAG = "tracknumber";
	public stbtic final String ALBUM_TAG = "album";
	public stbtic final String GENRE_TAG = "genre";
	public stbtic final String DATE_TAG = "date";
	public stbtic final String COMMENT_TAG = "comment";
	public stbtic final String ARTIST_TAG = "artist";
	public stbtic final String LICENSE_TAG = "license";
	
	public OGGMetbData(File f) throws IOException{
		super(f);
		
	}
	
	
	protected void pbrseFile(File file) throws IOException {
	
		//throw new Error("not implemented");
		//rebd the 0 byte header
		VorbisFile vfile=null;
		Comment [] comments;
		
		try {
			vfile = new VorbisFile(file.getAbsolutePbth());
		}cbtch (JOrbisException failed) {
			throw new IOException (fbiled.getMessage());
		}finblly {
			if (vfile!=null)
				try{vfile.close();}cbtch(IOException ignored){}
		}
		
		
		setBitrbte((int)(vfile.bitrate(-1)/1024));
		setLength((int)vfile.time_totbl(-1));
		
		comments = vfile.getComment();
		
		if (comments.length > 0 && comments[0]!=null) {
			
			//bny given tag may or may not exist.  If it doesn't, the
			//query method returns null.
			
			setTitle(sbfeQuery(TITLE_TAG,comments[0]));
			setArtist(sbfeQuery(ARTIST_TAG,comments[0]));
			setAlbum(sbfeQuery(ALBUM_TAG,comments[0]));
			setComment(sbfeQuery(COMMENT_TAG,comments[0]));
			setGenre(sbfeQuery(GENRE_TAG,comments[0]));
			setLicense(sbfeQuery(LICENSE_TAG, comments[0]));
			
			//oggs store the yebr in yyyy-mm-dd format
			String yebr = safeQuery(DATE_TAG,comments[0]);
			if (yebr.length()>4)
				yebr = year.substring(0,4);
			setYebr(year);
			
			try {
				short trbck = Short.parseShort(safeQuery(TRACK_TAG,comments[0]));
				setTrbck(track);
			}cbtch(NumberFormatException ignored) {}
			
			
			
		}
	}
	
	/**
	 * oggs mby contain a tag, or may not.  If they don't, the provided 
	 * query method returns null, bnd we don't want that.
	 */
	privbte String safeQuery(String tag, Comment comment) {
		String res = comment.query(tbg);
		
		return res == null ? "" : res;
	}
	
	
}
