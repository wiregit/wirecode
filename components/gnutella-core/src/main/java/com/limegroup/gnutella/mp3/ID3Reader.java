package com.limegroup.gnutella.mp3;

import java.io.*;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import de.ueberdosis.mp3info.*;

/**
 * Provides a utility method to read ID3 Tag information from MP3
 * files and creates GMLDocuments from them. 
 *
 * @author Sumeet Thadani
 */
public final class ID3Reader {
    private static final String schemaURI = 
         "http://www.limewire.com/schemas/audio.xsd";

    private static final String KEY_PREFIX = "audios" + XMLStringUtils.DELIMITER +
        "audio" + XMLStringUtils.DELIMITER;

    private static final String TRACK_KEY =    KEY_PREFIX + "track" + 
        XMLStringUtils.DELIMITER;
    private static final String ARTIST_KEY =   KEY_PREFIX + "artist" + 
        XMLStringUtils.DELIMITER;
    private static final String ALBUM_KEY =    KEY_PREFIX + "album" + 
        XMLStringUtils.DELIMITER;
    private static final String TITLE_KEY =    KEY_PREFIX + "title" + 
        XMLStringUtils.DELIMITER;
    private static final String GENRE_KEY =    KEY_PREFIX + "genre" + 
        XMLStringUtils.DELIMITER;
    private static final String YEAR_KEY =     KEY_PREFIX + "year" + 
        XMLStringUtils.DELIMITER;
    private static final String COMMENTS_KEY = KEY_PREFIX + "comments" + 
        XMLStringUtils.DELIMITER;
    private static final String BITRATE_KEY =  KEY_PREFIX + "bitrate" + 
        XMLStringUtils.DELIMITER;
    private static final String SECONDS_KEY =  KEY_PREFIX + "seconds" + 
        XMLStringUtils.DELIMITER;
        
    /**
     * This class should never be constructed.
     */
    private ID3Reader() {}
    
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

    /**
     * Attempts to read an ID3 tag from the specified file.
     * @return an null if the document has no ID3 tag
     */
    public static String readDocument(File file,boolean solo) throws IOException{
        ExtendedID3Tag id3 = parseFile(file);
        String title = id3.getTitle(), artist = id3.getArtist(), 
        album = id3.getAlbum(), year = id3.getYear(),
        comment = id3.getComment();
        short track = (short) id3.getTrack(), gen = (short) id3.getGenre();
        int bitrate = id3.getBitrate(), seconds = id3.getRuntime();

        StringBuffer strB = new StringBuffer();
        if(solo){
            appendStrings("<audios noNamespaceSchemaLocation=\"",
                          schemaURI,
                          strB);
            strB.append("><audio ");
            String filename = file.getCanonicalPath();
            //str = str+"\""+" identifier=\""+filename+"\">";
            appendStrings(" identifier=\"", filename, strB);
        }
        //end of head
        if(!title.equals(""))
            appendStrings(" title=\"", title, strB);
        if(!artist.equals(""))
            appendStrings(" artist=\"", artist, strB);
        if(!album.equals(""))
            appendStrings(" album=\"", album, strB);
        if(track>0)
            appendStrings(" track=\"", ""+track, strB);
        String genre = getGenreString(gen);
        if(!genre.equals(""))
            appendStrings(" genre=\"", genre, strB);
        if(!year.equals(""))
            appendStrings(" year=\"", year, strB);
        if(!comment.equals(""))
            appendStrings(" comments=\"", comment, strB);
        if(bitrate > 0)
            appendStrings(" bitrate=\"", ""+bitrate, strB);
        if(seconds > 0)
            appendStrings(" seconds=\"", ""+seconds, strB);
        if(solo){
            //str = str+"</audio>";
            strB.append("/>");
            strB.append("</audios>");
        }
        
        return strB.toString();
    }


    public static LimeXMLDocument readDocument(File file) throws IOException {
        ExtendedID3Tag id3 = parseFile(file);
        String title = id3.getTitle(), artist = id3.getArtist(), 
        album = id3.getAlbum(), year = id3.getYear(),
        comment = id3.getComment();
        short track = (short) id3.getTrack(), gen = (short) id3.getGenre();
        int bitrate = id3.getBitrate(), seconds = id3.getRuntime();
        String genre = getGenreString(gen);

        List nameValList = new ArrayList();
        if(!title.equals(""))
            nameValList.add(new NameValue(TITLE_KEY, title));
        if(!artist.equals(""))
            nameValList.add(new NameValue(ARTIST_KEY, artist));
        if(!album.equals(""))
            nameValList.add(new NameValue(ALBUM_KEY, album));
        if(!year.equals(""))
            nameValList.add(new NameValue(YEAR_KEY, year));
        if(!comment.equals(""))
            nameValList.add(new NameValue(COMMENTS_KEY, comment));
        if(track > 0)
            nameValList.add(new NameValue(TRACK_KEY, ""+track));
        if(!genre.equals("") )
            nameValList.add(new NameValue(GENRE_KEY, genre));
        if(bitrate > 0)
            nameValList.add(new NameValue(BITRATE_KEY, ""+bitrate));
        if(seconds > 0) 
            nameValList.add(new NameValue(SECONDS_KEY, ""+seconds));

        return new LimeXMLDocument(nameValList, schemaURI);
    }

    /** @return The object representation of the extended ID3 information.
     */
    private static ExtendedID3Tag parseFile(File file) throws IOException {
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
            return de.ueberdosis.mp3info.ID3Reader.readExtendedTag(randomAccessFile);
        } finally {
            if( randomAccessFile != null )
                randomAccessFile.close();
        }
    }


    private static void appendStrings(String key, String value,StringBuffer appendTo) {
        appendTo.append(key);
        appendTo.append(value);
        appendTo.append("\"");
    }

    /**
     * Walks back through the byte array to trim off null characters and
     * spaces.  A helper for read(...) above.
     * @return the number of bytes with nulls and spaces trimmed.
     */
    private static int getTrimmedLength(byte[] bytes, int includedLength) {
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
     * Takes a short and returns the corresponding genre string
     */
    public static String getGenreString(short genre){
        switch(genre){
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
}
