package com.limegroup.gnutella.mp3;

import java.io.*;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import de.vdheide.mp3.*;

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
        Object[] info = parseFile(file);
        short track = ((Short) info[4]).shortValue();
        short gen = ((Short) info[6]).shortValue();
        int bitrate = ((Integer) info[7]).intValue();
        int seconds = ((Integer) info[8]).intValue();
        String genre = getGenreString(gen);

        List nameValList = new ArrayList();
        if(!((String)info[0]).equals(""))
            nameValList.add(new NameValue(TITLE_KEY, info[0]));
        if(!((String)info[1]).equals(""))
            nameValList.add(new NameValue(ARTIST_KEY, info[1]));
        if(!((String)info[2]).equals(""))
            nameValList.add(new NameValue(ALBUM_KEY, info[2]));
        if(!((String)info[3]).equals(""))
            nameValList.add(new NameValue(YEAR_KEY, info[3]));
        if(!((String)info[5]).equals(""))
            nameValList.add(new NameValue(COMMENTS_KEY, info[5]));
        if(track > 0)
            nameValList.add(new NameValue(TRACK_KEY, ""+track));
        if(!genre.equals("") )
            nameValList.add(new NameValue(GENRE_KEY, genre));
        if(bitrate > 0)
            nameValList.add(new NameValue(BITRATE_KEY, ""+bitrate));
        if(seconds > 0) 
            nameValList.add(new NameValue(SECONDS_KEY, ""+seconds));
        if(nameValList.isEmpty())
            throw new IOException("invalid/no data.");

        return new LimeXMLDocument(nameValList, schemaURI);
    }

    /**
     * @return a Object[] with the following order: title, artist, album, year,
     * track, comment, gen, bitrate, seconds.  Indices 0, 1, 2, 3, and 5 are
     * Strings.  Indices 4 and 6 are Shorts.  Indices 7 and 8 are Integers.  
     * <p>
     * LimeWire would prefer to use ID3V2 tags, so we try to parse the ID3V2
     * tags first, and if we were not able to find some tags using v2 we get it
     * using v1 if possible 
     */
    private static Object[] parseFile(File file) throws IOException {
        Object[] retObjs = new Object[9];
        
        String nonValueString = "";
        Short nonValueShort = new Short((short)-1);
        // default vals...
        retObjs[0] = nonValueString; //title
        retObjs[1] = nonValueString; //artist
        retObjs[2] = nonValueString; //album
        retObjs[3] = nonValueString; //year
        retObjs[5] = nonValueString; //comment
        retObjs[4] = nonValueShort; //track
        retObjs[6] = nonValueShort; //genre        

        if(parseID3v2Data(file, retObjs) )
            return retObjs;
        
        RandomAccessFile randomAccessFile = null;
        
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
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
                    if(nonValueString.equals(retObjs[0]))
                        retObjs[0] = new String(buffer, 0, 
                                               getTrimmedLength(buffer, 30));
                    
                    // Artist
                    randomAccessFile.readFully(buffer, 0, 30);
                    if(nonValueString.equals(retObjs[1]))
                    retObjs[1] = new String(buffer, 0, 
                                               getTrimmedLength(buffer, 30));
                    
                    // Album
                    randomAccessFile.readFully(buffer, 0, 30);
                    if(nonValueString.equals(retObjs[2]))
                        retObjs[2] = new String(buffer, 0, 
                                               getTrimmedLength(buffer, 30));
                    
                    // Year
                    randomAccessFile.readFully(buffer, 0, 4);
                    if(nonValueString.equals(retObjs[3]))
                        retObjs[3] = new String(buffer, 0, 
                                               getTrimmedLength(buffer, 4));
                    
                    // Comment and track
                    randomAccessFile.readFully(buffer, 0, 30);
                    int commentLength;
                    if(buffer[28] == 0)
                    {
                        if(nonValueShort.equals(retObjs[4]))
                            retObjs[4] = new Short((short)ByteOrder.ubyte2int(buffer[29]));
                        commentLength = 28;
                    }
                    else
                    {
                        if(nonValueShort.equals(retObjs[4]))
                            retObjs[4] = new Short((short)0);
                        commentLength = 3;
                    }
                    if(nonValueString.equals(retObjs[5]))
                        retObjs[5] = new String(buffer, 0,
                                               getTrimmedLength(buffer, 
                                                                commentLength));
                    
                    // Genre
                    randomAccessFile.readFully(buffer, 0, 1);
                    if(nonValueShort.equals(retObjs[6]))
                        retObjs[6] = new Short((short)ByteOrder.ubyte2int(buffer[0]));
                }
            }
    
            MP3Info mp3Info = new MP3Info(file.getCanonicalPath());
            // Bitrate
            retObjs[7] = new Integer(mp3Info.getBitRate());
            // Length
            retObjs[8] = new Integer((int) mp3Info.getLengthInSeconds());
        } finally {
            if( randomAccessFile != null )
                randomAccessFile.close();
        }
        return retObjs;
    }

    /**
     * @param audioTags this array emulates pass by reference
     * @return true if all the tags were read, false otherwise
     */
    private static boolean parseID3v2Data(File file, Object[] audioTags) {
        boolean ret = true;
        MP3File mp3File = null;
        try {
            mp3File = new MP3File(file.getParentFile(), file.getName());
        } catch (ID3v2Exception idvx) { //can't go on
            return false;
        } catch (NoMP3FrameException nmfx) {//can't go on
            return false;
        } catch (IOException iox) {
            return false;
        }
        //keep track of how many fields we have populated
        int fieldCount = 0;
        //1. title 
        String str = "";
        try {
            str = mp3File.getTitle().getTextContent();
        } catch(FrameDamagedException ignored) {}
        if(! "".equals(str)) {
            fieldCount++;
            audioTags[0] = str;
        }
        //2. artist 
        try {
            str = mp3File.getArtist().getTextContent();
        } catch(FrameDamagedException ignored) {}
        if(! "".equals(str)) {
            fieldCount++;
            audioTags[1] = str;
        }
        //3. album
        try {
            str = mp3File.getAlbum().getTextContent();
        } catch(FrameDamagedException ignored) {}
        if(! "".equals(str)) {
            fieldCount++;
            audioTags[2] = str;
        }
        //4. year
        try {
            str = mp3File.getYear().getTextContent();
        } catch(FrameDamagedException ignored) {}
        if(! "".equals(str)) {
            fieldCount++;
            audioTags[3] = str;
        }
        //5. comment
        try {
            str = mp3File.getComments().getTextContent();
        } catch(FrameDamagedException ignored) {}
        if(! "".equals(str)) {
            fieldCount++;
            audioTags[5] = str;
        }
        //6. artist 
        try {
            str = mp3File.getTrack().getTextContent();
        } catch(FrameDamagedException ignored) {}
        if(! "".equals(str)) {
            fieldCount++;
            audioTags[4] = new Short(str);
        }
        //7. genre
        //Note: The ID3V2 reader returns a string for the genre, whereas id3v1
        //tags read a byte which we convert to a string. there is no guarantee
        //that these strings are the same set. So we will stick with the v1
        //version for now, by trying to convert the string to the byte value
        try {
            str = mp3File.getTrack().getTextContent();
        } catch(FrameDamagedException ignored) {}
        if(! "".equals(str)) {
            fieldCount++;
            audioTags[6] = new Short((short)getGenreByte(str));
        }
        return fieldCount == 7;
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

    
    private static byte getGenreByte(String genre) {
        if(genre==null) return -1;
        else if(genre.equals("Blues")) return 0;
        else if(genre.equals("Classic Rock")) return 1;
        else if(genre.equals("Country")) return 2;
        else if(genre.equals("Dance")) return 3;
        else if(genre.equals("Disco")) return 4;
        else if(genre.equals("Funk")) return 5;
        else if(genre.equals("Grunge")) return 6;
        else if(genre.equals("Hop")) return 7;
        else if(genre.equals("Jazz")) return 8;
        else if(genre.equals("Metal")) return 9;
        else if (genre.equals("New Age")) return 10;
        else if(genre.equals("Oldies")) return 11;
        else if(genre.equals("Other")) return 12;
        else if(genre.equals("Pop")) return 13;
        else if (genre.equals("R &amp; B")) return 14;
        else if(genre.equals("Rap")) return 15;
        else if(genre.equals("Reggae")) return 16;
        else if(genre.equals("Rock")) return 17;
        else if(genre.equals("Techno")) return 17;
        else if(genre.equals("Industrial")) return 19;
        else if(genre.equals("Alternative")) return 20;
        else if(genre.equals("Ska")) return 21;
        else if(genre.equals("Metal")) return 22;
        else if(genre.equals("Pranks")) return 23;
        else if(genre.equals("Soundtrack")) return 24;
        else if(genre.equals("Euro-Techno")) return 25;
        else if(genre.equals("Ambient")) return 26;
        else if(genre.equals("Trip-Hop")) return 27;
        else if(genre.equals("Vocal")) return 28;
        else if (genre.equals("Jazz+Funk")) return 29;
        else if(genre.equals("Fusion")) return 30;
        else if(genre.equals("Trance")) return 31;
        else if(genre.equals("Classical")) return 32;
        else if(genre.equals("Instrumental")) return 33;
        else if(genre.equals("Acid")) return 34;
        else if(genre.equals("House")) return 35;
        else if(genre.equals("Game")) return 36;
        else if(genre.equals("Sound Clip")) return 37;
        else if(genre.equals("Gospel")) return 38;
        else if(genre.equals("Noise")) return 39;
        else if(genre.equals("AlternRock")) return 40;
        else if(genre.equals("Bass")) return 41;
        else if(genre.equals("Soul")) return 42;
        else if(genre.equals("Punk")) return 43;
        else if(genre.equals("Space")) return 44;
        else if(genre.equals("Meditative")) return 45;
        else if(genre.equals("Instrumental Pop")) return 46;
        else if(genre.equals("Instrumental Rock")) return 47;
        else if(genre.equals("Ethnic")) return 48;
        else if(genre.equals("Gothic")) return 49;
        else if(genre.equals("Darkwave")) return 50;
        else if(genre.equals("Techno-Industrial")) return 51;
        else if(genre.equals("Electronic")) return 52;
        else if(genre.equals("Pop-Folk")) return 53;
        else if(genre.equals("Eurodance")) return 54;
        else if(genre.equals("Dream")) return 55;
        else if(genre.equals("Southern Rock")) return 56;
        else if(genre.equals("Comedy")) return 57;
        else if(genre.equals("Cult")) return 58;
        else if(genre.equals("Gangsta")) return 59;
        else if(genre.equals("Top 40")) return 60;
        else if(genre.equals("Christian Rap")) return 61;
        else if(genre.equals("Pop/Funk")) return 62;
        else if(genre.equals("Jungle")) return 63;
        else if(genre.equals("Native American")) return 64;
        else if(genre.equals("Cabaret")) return 65;
        else if(genre.equals("New Wave")) return 66;
        else if(genre.equals("Psychadelic")) return 67;
        else if(genre.equals("Rave")) return 68;
        else if(genre.equals("Showtunes")) return 69;
        else if(genre.equals("Trailer")) return 70;
        else if(genre.equals("Lo-Fi")) return 71;
        else if(genre.equals("Tribal")) return 72;
        else if(genre.equals("Acid Punk")) return 73;
        else if(genre.equals("Acid Jazz")) return 74;
        else if(genre.equals("Polka")) return 75;
        else if(genre.equals("Retro")) return 76;
        else if(genre.equals("Musical")) return 77;
        else if(genre.equals("Rock &amp; Roll")) return 78;
        else if(genre.equals("Hard Rock")) return 79;
        else if(genre.equals("Folk")) return 80;
        else if(genre.equals("Folk-Rock")) return 81;
        else if(genre.equals("National Folk")) return 82;
        else if(genre.equals("Swing")) return 83;
        else if(genre.equals("Fast Fusion")) return 84;
        else if(genre.equals("Bebob")) return 85;
        else if(genre.equals("Latin")) return 86;
        else if(genre.equals("Revival")) return 87;
        else if(genre.equals("Celtic")) return 88;
        else if(genre.equals("Bluegrass")) return 89;
        else if(genre.equals("Avantgarde")) return 90;
        else if(genre.equals("Gothic Rock")) return 91;
        else if(genre.equals("Progressive Rock")) return 92;
        else if(genre.equals("Psychedelic Rock")) return 93;
        else if(genre.equals("Symphonic Rock")) return 94;
        else if(genre.equals("Slow Rock")) return 95;
        else if(genre.equals("Big Band")) return 96;
        else if(genre.equals("Chorus")) return 97;
        else if(genre.equals("Easy Listening")) return 98;
        else if(genre.equals("Acoustic")) return 99;
        else if(genre.equals("Humour")) return 100;
        else if(genre.equals("Speech")) return 101;
        else if(genre.equals("Chanson")) return 102;
        else if(genre.equals("Opera")) return 103;
        else if(genre.equals("Chamber Music")) return 104;
        else if(genre.equals("Sonata")) return 105;
        else if(genre.equals("Symphony")) return 106;
        else if(genre.equals("Booty Bass")) return 107;
        else if(genre.equals("Primus")) return 108;
        else if(genre.equals("Porn Groove")) return 109;
        else if(genre.equals("Satire")) return 110;
        else if(genre.equals("Slow Jam")) return 111;
        else if(genre.equals("Club")) return 112;
        else if(genre.equals("Tango")) return 113;
        else if(genre.equals("Samba")) return 114;
        else if(genre.equals("Folklore")) return 115;
        else if(genre.equals("Ballad")) return 116;
        else if(genre.equals("Power Ballad")) return 117;
        else if(genre.equals("Rhythmic Soul")) return 118;
        else if(genre.equals("Freestyle")) return 119;
        else if(genre.equals("Duet")) return 120;
        else if(genre.equals("Punk Rock")) return 121;
        else if(genre.equals("Drum Solo")) return 122;
        else if(genre.equals("A capella")) return 123;
        else if(genre.equals("Euro-House")) return 124;
        else if(genre.equals("Dance Hall")) return 125;
        else return -1;
    }

}
