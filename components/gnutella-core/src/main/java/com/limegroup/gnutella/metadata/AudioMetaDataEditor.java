
package com.limegroup.gnutella.metadata;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * a metadata editor for audio files.
 */
public abstract class AudioMetaDataEditor extends MetaDataEditor {
	
	private Log LOG = LogFactory.getLog(AudioMetaDataEditor.class);
	
	protected String title_;
    protected String artist_;
    protected String album_;
    protected String year_;
    protected String track_;
    protected String comment_;
    protected String genre_;
    
    //bunch of tags within our LimeXMLDocument
    private static final String TITLE_STRING   = "title=\"";
    private static final String ARTIST_STRING  = "artist=\"";
    private static final String ALBUM_STRING   = "album=\"";
    private static final String YEAR_STRING    = "year=\"";
    private static final String TRACK_STRING   = "track=\"";
    private static final String COMMENT_STRING = "comments=\"";
    private static final String GENRE_STRING   = "genre=\"";
    private static final String BITRATE_STRING = "bitrate=\"";
    private static final String SECONDS_STRING = "seconds=\"";
    
    
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
    	if (LimeXMLUtils.isOGGFile(name))
    		return new OGGDataEditor(name);
    	return null;
    }

	/** 
	 * The caller of this method has the xml string that represents a
	 * LimeXMLDocument, and wants to write the document out to disk. For this
	 * method to work effectively, the caller must instantiate this class and
	 * call this method first, and then call to actually write the ID3
	 * tags out.
	 * <p>
	 * This method reads the complete xml string and removes the id3 *
	 * components of the xml string, and stores the values of the id3 tags in a
	 * class variable which will later be used to write the id3 tags in the
	 * mp3file.
	 * <p>
	 * @return a parseable xml string which has the same attributes as the
	 * xmlStr paramter minus the id3 tags.
	 */
	public void populateFromString(String xmlStr) {
	    //will be used to reconstruct xmlStr after ripping stuff from it
	    int i, j;
	    Object[] rippedStuff = null;
	
	    //title        
	    try {
	        rippedStuff = ripTag(xmlStr, TITLE_STRING);
	
	        title_ = (String)rippedStuff[2];
	        if (LOG.isDebugEnabled())
	        	LOG.debug("title = "+title_);
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //artist
	    try {
	        rippedStuff = ripTag(xmlStr, ARTIST_STRING);
	
	        artist_ = (String)rippedStuff[2];
	        if (LOG.isDebugEnabled())
	        	LOG.debug("artist = "+artist_);
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //album
	    try {
	        rippedStuff = ripTag(xmlStr, ALBUM_STRING);
	
	        album_ = (String)rippedStuff[2];
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //year
	    try {
	        rippedStuff = ripTag(xmlStr, YEAR_STRING);
	
	        year_ = (String)rippedStuff[2];
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //track
	    try {
	        rippedStuff = ripTag(xmlStr, TRACK_STRING);
	
	        track_ = (String)rippedStuff[2];
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //comment
	    try {
	        rippedStuff = ripTag(xmlStr, COMMENT_STRING);
	
	        comment_ = (String)rippedStuff[2];
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //genre
	    try {
	        rippedStuff = ripTag(xmlStr, GENRE_STRING);
	
	        genre_ = (String)rippedStuff[2];
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //bitrate
	    try {
	        rippedStuff = ripTag(xmlStr, BITRATE_STRING);
	
	        // we get bitrate info from the mp3 file....
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //seconds
	    try {
	        rippedStuff = ripTag(xmlStr, SECONDS_STRING);
	
	        // we get seconds info from the mp3 file....
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	
	
	
	    //return xmlStr;//this has been suitable modified
	}

	/** 
	 * @return object[0] = (Integer) index just before beginning of tag=value, 
	 * object[1] = (Integer) index just after end of tag=value, object[2] =
	 * (String) value of tag.
	 * @exception Throw if rip failed.
	 */
	private Object[] ripTag(String source, String tagToRip) throws IOException {
	
	    Object[] retObjs = new Object[3];
	
	    int begin = source.indexOf(tagToRip);
	    if (begin < 0)
	        throw new IOException("tag not found");
	
	    if (begin != 0)            
	        retObjs[0] = new Integer(begin-1);
	    if (begin == 0)            
	        retObjs[0] = new Integer(begin);
	
	    int end = begin;
	    int i, j; // begin and end of value to return
	
	    for ( ; source.charAt(end) != '='; end++)
	        ; // found '=' sign
	    
	    for ( ; source.charAt(end) != '"'; end++)
	        ; // found first '"' sign
	    i = ++end;  // set beginning of value
	
	    for ( ; source.charAt(end) != '"'; end++)
	        ; // found second '"' sign
	    j = end; // set end of value
	
	    retObjs[1] = new Integer(end+1);
	    if (LOG.isDebugEnabled())
	    	LOG.debug("ID3Editor.ripTag(): i = " + i +
	          ", j = " + j);
	    retObjs[2] = source.substring(i,j);
	                   
	    return retObjs;
	}
}
