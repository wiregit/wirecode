package com.limegroup.gnutella.metadata;

import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * a metadata editor for audio files.
 */
public abstract class AudioMetaDataEditor extends MetaDataEditor {
	
	//private Log LOG = LogFactory.getLog(AudioMetaDataEditor.class);
	
	protected String title_;
    protected String artist_;
    protected String album_;
    protected String year_;
    protected String track_;
    protected String comment_;
    protected String genre_;
    protected String license_;
    
    private static final String TITLE   = "audios__audio__title__";
    private static final String ARTIST  = "audios__audio__artist__";
    private static final String ALBUM   = "audios__audio__album__";
    private static final String YEAR    = "audios__audio__year__";
    private static final String TRACK   = "audios__audio__track__";
    private static final String COMMENT = "audios__audio__comments__";
    private static final String GENRE   = "audios__audio__genre__";    
    private static final String LICENSE = "audios__audio__license__";
    
    /**
     * Determines if this editor matches every field of another.
     */
    public boolean equals(Object o) {
        if( o == this ) return true;
        if( !(o instanceof MetaDataEditor) ) return false;
        
        AudioMetaDataEditor other = (AudioMetaDataEditor)o;

        return matches(title_, other.title_) &&
               matches(artist_, other.artist_) &&
               matches(album_, other.album_) &&
               matches(year_, other.year_) &&
               matches(track_, other.track_) &&
               matches(comment_, other.comment_) &&
               matches(genre_, other.genre_) &&
               matches(license_, other.license_);
    }
    
    /**
     * Determines if this editor has all better fields than another.
     */
    public boolean betterThan(MetaDataEditor o) {
    	AudioMetaDataEditor other = (AudioMetaDataEditor)o;
        return firstBetter(title_, other.title_) &&
               firstBetter(artist_, other.artist_) &&
               firstBetter(album_, other.album_) &&
               firstBetter(track_, other.track_) &&
               firstBetter(comment_, other.comment_) &&
               firstBetter(genre_, other.genre_) &&
               firstBetter(license_, other.license_);
    }
    
    /**
     * Sets the fields in this editor to be the better of the two
     * editors.
     */
    public void pickBetterFields(MetaDataEditor o) {
    	AudioMetaDataEditor other = (AudioMetaDataEditor)o;
        if(firstBetter(other.title_, title_))
            title_ = other.title_;
        if(firstBetter(other.artist_, artist_))
           artist_ = other.artist_;
        if(firstBetter(other.album_, album_))
           album_ = other.album_;
        if(firstBetter(other.track_, track_))
           track_ = other.track_;
        if(firstBetter(other.comment_, comment_))
           comment_ = other.comment_;
        if(firstBetter(other.genre_, genre_))
           genre_ = other.genre_;
        if(firstBetter(other.license_, license_))
            license_ = other.license_;
    }
    
    /**
     * Factory method for retrieving the correct editor
     * for a given file.
     */
    public static MetaDataEditor getEditorForFile(String name) {
    	if (LimeXMLUtils.isMP3File(name))
    		return new MP3DataEditor();
    	if (LimeXMLUtils.isOGGFile(name))
    		return new OGGDataEditor();
    	return null;
    }

	/** 
	 * Populates the internal values from the document.
	 */
	public void populate(LimeXMLDocument doc) {
	    title_   = doc.getValue(TITLE);
	    artist_  = doc.getValue(ARTIST);
	    album_   = doc.getValue(ALBUM);
	    year_    = doc.getValue(YEAR);
	    track_   = doc.getValue(TRACK);
	    comment_ = doc.getValue(COMMENT);
	    genre_   = doc.getValue(GENRE);
	    license_ = doc.getValue(LICENSE);
	}
}
