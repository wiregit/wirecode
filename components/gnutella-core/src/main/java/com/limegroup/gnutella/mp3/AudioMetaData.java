/*
 * Created on May 11, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.limegroup.gnutella.mp3;

import com.limegroup.gnutella.util.NameValue;
import com.limegroup.gnutella.xml.*;
import com.sun.java.util.collections.*;

import java.io.*;

/**
 * Simple class to encapsulate information about ID3 tags.
 * Can write the data to a NameValue list or an XML string.
 */
public abstract class AudioMetaData extends MetaData {
    private String title ;
    private String artist;
    private String album ;
    private String year;
    private String comment;
    private short track = -1;
    private String genre;
    private int bitrate = -1;
    private int length = -1;
    private short totalTracks =-1;
    
    public static final String ISO_LATIN_1 = "8859_1";
    public static final String UNICODE = "Unicode";
    
    public static String schemaURI = 
        "http://www.limewire.com/schemas/audio.xsd";
    
    public static final String KEY_PREFIX = "audios" + 
        XMLStringUtils.DELIMITER + "audio" + XMLStringUtils.DELIMITER;
    public static final String TRACK_KEY =    KEY_PREFIX + "track" + 
        XMLStringUtils.DELIMITER;
    public static final String ARTIST_KEY =   KEY_PREFIX + "artist" + 
        XMLStringUtils.DELIMITER;
    public static final String ALBUM_KEY =    KEY_PREFIX + "album" + 
        XMLStringUtils.DELIMITER;
    public static final String TITLE_KEY =    KEY_PREFIX + "title" + 
        XMLStringUtils.DELIMITER;
    public static final String GENRE_KEY =    KEY_PREFIX + "genre" + 
        XMLStringUtils.DELIMITER;
    public static final String YEAR_KEY =     KEY_PREFIX + "year" + 
        XMLStringUtils.DELIMITER;
    public static final String COMMENTS_KEY = KEY_PREFIX + "comments" + 
        XMLStringUtils.DELIMITER;
    public static final String BITRATE_KEY =  KEY_PREFIX + "bitrate" + 
        XMLStringUtils.DELIMITER;
    public static final String SECONDS_KEY =  KEY_PREFIX + "seconds" + 
        XMLStringUtils.DELIMITER;

    public AudioMetaData(File f) throws IOException{
    	parseFile(f);
    }
    
    
    public static AudioMetaData parseAudioFile(File f) throws IOException{
    	if (LimeXMLUtils.isMP3File(f))
    		return new MP3MetaData(f);
    	if (LimeXMLUtils.isOGGFile(f))
    		return new OGGMetaData(f);
    	if (LimeXMLUtils.isM4AFile(f))
    		return new M4AMetaData(f);
    	
    	//TODO: add future supported audio types here
    	
    	return null;
    	
    }
    
    public String toString() {
        return "ID3Data: title[" + title + "], artist[" + artist +
               "], album[" + album + "], year[" + year + "], comment["
               + comment + "], track[" + track + "], genre[" + genre +
               "], bitrate[" + bitrate + "], length[" + length +"]";
    }          
    
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getYear() { return year; }
    public String getComment()  { return comment; }
    public short getTrack() { return track; }
    public short getTotalTracks() {return totalTracks;}
    public String getGenre() { return genre; }
    public int getBitrate() { return bitrate; }
    public int getLength() { return length; }
    
    void setTitle(String title) {
        this.title = title;
    }
    
    void setArtist(String artist) {
        this.artist = artist;
    }
    
    void setAlbum(String album) {
        this.album = album;
    }
    
    void setYear(String year) {
        this.year = year;
    }
    
    void setComment(String comment) {
        this.comment = comment;
    }
    
    void setTrack(short track) {
        this.track = track;
    }
    
    void setTotalTracks(short total) {
    	totalTracks = total;
    }
    
    void setGenre(String genre) {
        this.genre = genre;
    }
    
    void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }
    
    void setLength(int length) {
        this.length = length;
    }
    
    /**
     * Updates this' information with data's information
     * for any fields that are currently unspecified.
     */
    void mergeID3Data(AudioMetaData data) {
        if(!isValid(title))
            title = data.title;
        if(!isValid(artist))
            artist = data.artist;
        if(!isValid(album))
            album = data.album;
        if(!isValid(year))
            year = data.year;
        if(!isValid(comment))
            comment = data.comment;
        if(!isValid(track))
            track = data.track;
        if(!isValid(genre))
            genre = data.genre;
        if(!isValid(bitrate))
            bitrate = data.bitrate;
        if(!isValid(length))
            length = data.length;
    }
    
    /**
     * Determines if all fields are valid.
     */
    public boolean isComplete() {
        return isValid(title)
            && isValid(artist)
            && isValid(album)
            && isValid(year)
            && isValid(comment)
            && isValid(track)
            && isValid(genre)
            && isValid(bitrate)
            && isValid(length);
    }

    /**
     * Writes the data to a NameValue list.
     */
    public List toNameValueList() {
        List list = new ArrayList();
        add(list, title, TITLE_KEY);
        add(list, artist, ARTIST_KEY);
        add(list, album, ALBUM_KEY);
        add(list, year, YEAR_KEY);
        add(list, comment, COMMENTS_KEY);
        add(list, track, TRACK_KEY);
        add(list, genre, GENRE_KEY);
        add(list, bitrate, BITRATE_KEY);
        add(list, length, SECONDS_KEY);
        return list;
    }
    
    private void add(List list, String value, String key) {
        if(isValid(value))
            list.add(new NameValue(key, value.trim()));
    }
    
    private void add(List list, int value, String key) {
        if(isValid(value))
            list.add(new NameValue(key, "" + value));
    }
    
    private boolean isValid(String s) {
        return s != null && !s.trim().equals("");
    }
    
    private boolean isValid(int i) {
        return i >= 0;
    }
    
    /**
     * Appends the key/value & a "\" to the string buffer.
     */
    protected void appendStrings(String key, String value,
                                                        StringBuffer appendTo) {
        appendTo.append(key);
        appendTo.append(value);
        appendTo.append("\"");
    }

	/**
	 * Walks back through the byte array to trim off null characters and
	 * spaces.  A helper for read(...) above.
	 * @return the number of bytes with nulls and spaces trimmed.
	 */
	protected int getTrimmedLength(byte[] bytes, int includedLength) {
	    int i;
	    for(i = includedLength - 1;
	        (i >= 0) && ((bytes[i] == 0) || (bytes[i] == 32));
	        i--);
	    //replace the nulls with spaces in the array upto i
	    for(int j=0; j<=i; j++) 
	        if(bytes[j]==0)
	            bytes[j]=(byte)32;
	    return i + 1;
	}
	
    /**
     * Determines whether a LimeXMLDocument was corrupted by
     * ID3Editor in the past.
     */
    public static boolean isCorrupted(LimeXMLDocument doc) {
        if(!schemaURI.equals(doc.getSchemaURI()))
            return false;

        Set existing = doc.getNameValueSet();
        for(Iterator i = existing.iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            final String name = (String)entry.getKey();
            String value = (String)entry.getValue();
            // album & artist were the corrupted fields ...
            if( name.equals(ALBUM_KEY) || name.equals(ARTIST_KEY) ) {
                if( value.length() == 30 ) {
                    // if there is a value in the 29th char, but not
                    // in the 28th, it's corrupted. 
                    if( value.charAt(29) != ' ' && value.charAt(28) == ' ' )
                        return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Creates a new LimeXMLDocument without corruption.
     */
    public static LimeXMLDocument fixCorruption(LimeXMLDocument oldDoc) {
        Set existing = oldDoc.getNameValueSet();
        List info = new ArrayList(existing.size());
        for(Iterator i = existing.iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            final String name = (String)entry.getKey();
            String value = (String)entry.getValue();
            // album & artist were the corrupted fields ...
            if( name.equals(ALBUM_KEY) || name.equals(ARTIST_KEY) ) {
                if( value.length() == 30 ) {
                    // if there is a value in the 29th char, but not
                    // in the 28th, it's corrupted erase & trim.
                    if( value.charAt(29) != ' ' && value.charAt(28) == ' ' )
                        value = value.substring(0, 29).trim();
                }
            }
            info.add(new NameValue(name, value));
        }
        return new LimeXMLDocument(info, oldDoc.getSchemaURI());
    }

    public static boolean isNonLimeAudioField(String fieldName) {
        return (!fieldName.equals(TRACK_KEY) &&
                !fieldName.equals(ARTIST_KEY) &&
                !fieldName.equals(ALBUM_KEY) &&
                !fieldName.equals(TITLE_KEY) &&
                !fieldName.equals(GENRE_KEY) &&
                !fieldName.equals(YEAR_KEY) &&
                !fieldName.equals(COMMENTS_KEY) &&
                !fieldName.equals(BITRATE_KEY) &&
                !fieldName.equals(SECONDS_KEY) );
    }
    
}