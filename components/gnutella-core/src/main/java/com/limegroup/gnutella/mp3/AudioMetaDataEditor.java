
package com.limegroup.gnutella.mp3;

import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * a metadata editor for audio files.
 */
public abstract class AudioMetaDataEditor extends MetaDataEditor {
	
	protected String title_;
    protected String artist_;
    protected String album_;
    protected String year_;
    protected String track_;
    protected String comment_;
    protected String genre_;
    
    
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
               matches(genre_, other.genre_);
    }
    
    public boolean betterThan(MetaDataEditor o) {
    	AudioMetaDataEditor other = (AudioMetaDataEditor)o;
        return ( firstBetter(title_, other.title_) &&
                 firstBetter(artist_, other.artist_) &&
                 firstBetter(album_, other.album_) &&
                 firstBetter(track_, other.track_) &&
                 firstBetter(comment_, other.comment_) &&
                 firstBetter(genre_, other.genre_) );                 
    }
    
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
    }
    
    public static MetaDataEditor getEditorForFile(String name) {
    	if (LimeXMLUtils.isMP3File(name))
    		return new MP3DataEditor();
    	//TODO:add oggs here
    	return null;
    }
}
