package com.limegroup.gnutella.mp3;

import java.io.*;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;

/**
 * Provides a utility method to read ID3 Tag information from MP3
 * files and creates GMLDocuments from them. 
 *
 * @author Sumeet Thadani
 */
public final class ID3Reader {
    private static final String schemaURI = 
         "http://www.limewire.com/schemas/audio.xsd";

    private final String KEY_PREFIX = "audios" + XMLStringUtils.DELIMITER +
        "audio" + XMLStringUtils.DELIMITER;

    private final String TRACK_KEY =    KEY_PREFIX + "track" + 
        XMLStringUtils.DELIMITER;
    private final String ARTIST_KEY =   KEY_PREFIX + "artist" + 
        XMLStringUtils.DELIMITER;
    private final String ALBUM_KEY =    KEY_PREFIX + "album" + 
        XMLStringUtils.DELIMITER;
    private final String TITLE_KEY =    KEY_PREFIX + "title" + 
        XMLStringUtils.DELIMITER;
    private final String GENRE_KEY =    KEY_PREFIX + "genre" + 
        XMLStringUtils.DELIMITER;
    private final String YEAR_KEY =     KEY_PREFIX + "year" + 
        XMLStringUtils.DELIMITER;
    private final String COMMENTS_KEY = KEY_PREFIX + "comments" + 
        XMLStringUtils.DELIMITER;
    private final String BITRATE_KEY =  KEY_PREFIX + "bitrate" + 
        XMLStringUtils.DELIMITER;
    private final String SECONDS_KEY =  KEY_PREFIX + "seconds" + 
        XMLStringUtils.DELIMITER;


    /**
     * Attempts to read an ID3 tag from the specified file.
     * @return an null if the document has no ID3 tag
     */
    public String readDocument(File file,boolean solo) throws IOException{
        Object[] info = parseFile(file);
        String title = (String) info[0], artist = (String) info[1], 
        album = (String) info[2], year = (String) info[3], 
        comment = (String) info[5];
        short track = ((Short) info[4]).shortValue(), 
        gen = ((Short) info[6]).shortValue();
        int bitrate = ((Integer) info[7]).intValue(),
        seconds = ((Integer) info[8]).intValue();

        StringBuffer strB = new StringBuffer();
        if(solo){
            appendStrings("<audios noNamespaceSchemaLocation=\"",
                          this.schemaURI,
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


    public LimeXMLDocument readDocument(File file) throws IOException {
        Object[] info = parseFile(file);
        short track = ((Short) info[4]).shortValue(), 
        gen = ((Short) info[6]).shortValue();
        int bitrate = ((Integer) info[7]).intValue(),
        seconds = ((Integer) info[8]).intValue();

        List nameValList = new ArrayList();
        nameValList.add(new NameValue(TITLE_KEY, info[0]));
        nameValList.add(new NameValue(ARTIST_KEY, info[1]));
        nameValList.add(new NameValue(ALBUM_KEY, info[2]));
        nameValList.add(new NameValue(YEAR_KEY, info[3]));
        nameValList.add(new NameValue(COMMENTS_KEY, info[5]));
        nameValList.add(new NameValue(TRACK_KEY, ""+track));
        nameValList.add(new NameValue(GENRE_KEY, ""+gen));
        nameValList.add(new NameValue(BITRATE_KEY, ""+bitrate));
        nameValList.add(new NameValue(SECONDS_KEY, ""+seconds));

        return new LimeXMLDocument(nameValList, schemaURI);
    }

    /** @return a Object[] with the following order: title, artist, album, year,
       track, comment, gen, bitrate, seconds.  Indices 0, 1, 2, 3, and 5 are
       Strings.  Indices 4 and 6 are Shorts.  Indices 7 and 8 are Integers.  
     */
    private Object[] parseFile(File file) throws IOException {
        Object[] retObjs = new Object[9];

        // default vals...
        retObjs[0] = "";
        retObjs[1] = "";
        retObjs[2] = "";
        retObjs[3] = "";
        retObjs[5] = "";
        retObjs[4] = new Short((short)-1);
        retObjs[6] = new Short((short)-1);

        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        long length = randomAccessFile.length();

        // short circuit to bitrate if not ID3 tag can be properly read...
        // We need to read at least 128 bytes
        if(length >= 128) {

            randomAccessFile.seek(length - 128);
            byte[] buffer = new byte[30];
            
            // Read ID3 Tag, return null if not present
            randomAccessFile.readFully(buffer, 0, 3);
            String tag = new String(buffer, 0, 3);
            
            if (tag.equals("TAG")) {
            
                // We have an ID3 Tag, now get the parts
                // Title
                randomAccessFile.readFully(buffer, 0, 30);
                retObjs[0] = new String(buffer, 0, 
                                           getTrimmedLength(buffer, 30));
                
                // Artist
                randomAccessFile.readFully(buffer, 0, 30);
                retObjs[1] = new String(buffer, 0, 
                                           getTrimmedLength(buffer, 30));
                
                // Album
                randomAccessFile.readFully(buffer, 0, 30);
                retObjs[2] = new String(buffer, 0, 
                                           getTrimmedLength(buffer, 30));
                
                // Year
                randomAccessFile.readFully(buffer, 0, 4);
                retObjs[3] = new String(buffer, 0, 
                                           getTrimmedLength(buffer, 4));
                
                // Comment and track
                randomAccessFile.readFully(buffer, 0, 30);
                int commentLength;
                if(buffer[28] == 0)
                {
                    retObjs[4] = new Short((short)ByteOrder.ubyte2int(buffer[29]));
                    commentLength = 28;
                }
                else
                {
                    retObjs[4] = new Short((short)0);
                    commentLength = 3;
                }
                retObjs[5] = new String(buffer, 0,
                                           getTrimmedLength(buffer, 
                                                            commentLength));
                
                // Genre
                randomAccessFile.readFully(buffer, 0, 1);
                retObjs[6] = new Short((short)ByteOrder.ubyte2int(buffer[0]));
            }
        }

        MP3Info mp3Info = new MP3Info(file.getCanonicalPath());
        // Bitrate
        retObjs[7] = new Integer(mp3Info.getBitRate());
        // Length
        retObjs[8] = new Integer((int) mp3Info.getLengthInSeconds());

        randomAccessFile.close();
        return retObjs;
    }


    private void appendStrings(String key, String value,StringBuffer appendTo) {
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
        return i + 1;
    }
    
    /**
     * Takes a short and returns the corresponding genre string
     */
    private String getGenreString(short genre){
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
        case 10: return  "New Age";
        case 11: return "Oldies";
        case 12: return "Other";
        case 13: return "Pop";
        case 14 : return "R &amp; B";
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
