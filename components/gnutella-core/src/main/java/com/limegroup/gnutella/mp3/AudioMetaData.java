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
public class AudioMetaData extends MetaData {
    private String title;
    private String artist;
    private String album;
    private String year;
    private String comment;
    private short track = -1;
    private String genre;
    private int bitrate = -1;
    private int length = -1;
    
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

    
    public static AudioMetaData parseAudioFile(File f) throws IOException{
    	if (LimeXMLUtils.isMP3File(f))
    		return new MP3MetaData(f);
    	if (LimeXMLUtils.isOGGFile(f))
    		return new OGGMetaData(f);
    	
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
    
    /**
     * Writes the data to an XML string.
     *
     * If ID3Only is true, the data is a complete XML string, otherwise
     * it is an excerpt of an XML string.
     */
    public String toXML(String path, boolean id3Only) {
        StringBuffer strB = new StringBuffer();
        if(id3Only) {
            appendStrings("<audios noNamespaceSchemaLocation=\"",
            				schemaURI,
                          strB);
            strB.append("><audio ");
            appendStrings(" identifier=\"", path, strB);
        }


        if(isValid(title))
            appendStrings(" title=\"", title, strB);
        if(isValid(artist))
            appendStrings(" artist=\"", artist, strB);
        if(isValid(album))
            appendStrings(" album=\"", album, strB);
        if(isValid(track))
            appendStrings(" track=\"", ""+track, strB);
        if(isValid(genre))
            appendStrings(" genre=\"", genre, strB);
        if(isValid(year))
            appendStrings(" year=\"", year, strB);
        if(isValid(comment))
            appendStrings(" comments=\"", comment, strB);
        if(isValid(bitrate))
            appendStrings(" bitrate=\"", ""+bitrate, strB);
        if(isValid(length))
            appendStrings(" seconds=\"", ""+length, strB);

        if(id3Only) {
            strB.append("/>");
            strB.append("</audios>");
        }

        return strB.toString();
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
	 * Takes a short and returns the corresponding genre string
	 */
	public static String getGenreString(short genre) {
	    switch(genre) {
	    case 0: return "Blues";
	    case 1: return "Classic Rock";
	    case 2: return "Country";
	    case 3: return "Dance";
	    case 4: return "Disco";
	    case 5: return "Funk";
	    case 6: return "Grunge";
	    case 7: return "Hip-Hop";
	    case 8: return "Jazz";
	    case 9: return "Metal";
	    case 10: return "New Age";
	    case 11: return "Oldies";
	    case 12: return "Other";
	    case 13: return "Pop";
	    case 14: return "R &amp; B";
	    case 15: return "Rap";
	    case 16: return "Reggae";
	    case 17: return "Rock";
	    case 18: return "Techno";
	    case 19: return "Industrial";
	    case 20: return "Alternative";
	    case 21: return "Ska";
	    case 22: return "Death Metal";
	    case 23: return "Pranks";
	    case 24: return "Soundtrack";
	    case 25: return "Euro-Techno";
	    case 26: return "Ambient";
	    case 27: return "Trip-Hop";
	    case 28: return "Vocal";
	    case 29: return "Jazz+Funk";
	    case 30: return "Fusion";
	    case 31: return "Trance";
	    case 32: return "Classical";
	    case 33: return "Instrumental";
	    case 34: return "Acid";
	    case 35: return "House";
	    case 36: return "Game";
	    case 37: return "Sound Clip";
	    case 38: return "Gospel";
	    case 39: return "Noise";
	    case 40: return "AlternRock";
	    case 41: return "Bass";
	    case 42: return "Soul";
	    case 43: return "Punk";
	    case 44: return "Space";
	    case 45: return "Meditative";
	    case 46: return "Instrumental Pop";
	    case 47: return "Instrumental Rock";
	    case 48: return "Ethnic";
	    case 49: return "Gothic";
	    case 50: return "Darkwave";
	    case 51: return "Techno-Industrial";
	    case 52: return "Electronic";
	    case 53: return "Pop-Folk";
	    case 54: return "Eurodance";
	    case 55: return "Dream";
	    case 56: return "Southern Rock";
	    case 57: return "Comedy";
	    case 58: return "Cult";
	    case 59: return "Gangsta";
	    case 60: return "Top 40";
	    case 61: return "Christian Rap";
	    case 62: return "Pop/Funk";
	    case 63: return "Jungle";
	    case 64: return "Native American";
	    case 65: return "Cabaret";
	    case 66: return "New Wave";
	    case 67: return "Psychadelic";
	    case 68: return "Rave";
	    case 69: return "Showtunes";
	    case 70: return "Trailer";
	    case 71: return "Lo-Fi";
	    case 72: return "Tribal";
	    case 73: return "Acid Punk";
	    case 74: return "Acid Jazz";
	    case 75: return "Polka";
	    case 76: return "Retro";
	    case 77: return "Musical";
	    case 78: return "Rock &amp; Roll";
	    case 79: return "Hard Rock";
	    case 80: return "Folk";
	    case 81: return "Folk-Rock";
	    case 82: return "National Folk";
	    case 83: return "Swing";
	    case 84: return "Fast Fusion";
	    case 85: return "Bebob";
	    case 86: return "Latin";
	    case 87: return "Revival";
	    case 88: return "Celtic";
	    case 89: return "Bluegrass";
	    case 90: return "Avantgarde";
	    case 91: return "Gothic Rock";
	    case 92: return "Progressive Rock";
	    case 93: return "Psychedelic Rock";
	    case 94: return "Symphonic Rock";
	    case 95: return "Slow Rock";
	    case 96: return "Big Band";
	    case 97: return "Chorus";
	    case 98: return "Easy Listening";
	    case 99: return "Acoustic";
	    case 100: return "Humour";
	    case 101: return "Speech";
	    case 102: return "Chanson";
	    case 103: return "Opera";
	    case 104: return "Chamber Music";
	    case 105: return "Sonata";
	    case 106: return "Symphony";
	    case 107: return "Booty Bass";
	    case 108: return "Primus";
	    case 109: return "Porn Groove";
	    case 110: return "Satire";
	    case 111: return "Slow Jam";
	    case 112: return "Club";
	    case 113: return "Tango";
	    case 114: return "Samba";
	    case 115: return "Folklore";
	    case 116: return "Ballad";
	    case 117: return "Power Ballad";
	    case 118: return "Rhythmic Soul";
	    case 119: return "Freestyle";
	    case 120: return "Duet";
	    case 121: return "Punk Rock";
	    case 122: return "Drum Solo";
	    case 123: return "A capella";
	    case 124: return "Euro-House";
	    case 125: return "Dance Hall";
	    default: return "";
	    }
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

    public static boolean isNonID3Field(String fieldName) {
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