package com.limegroup.gnutella.mp3;

import java.io.*;
import java.net.*;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import java.util.StringTokenizer;
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
     * @return true if the mp3 file has license information in its ID3v2 TCOP
     * tag.
     */
    public static boolean hasVerifiedLicense(String filename) 
        throws IOException {

        // 1. see if the mp3 file has a TCOP v2 Frame with a 'verify at' and
        //    a reference url and remember the license
        // 2. connect to the reference url and get its content
        // 3. verify that the rdf has the same license as the TCOP frame did 
        try {

            // get the TCOP frame            
            MP3File mp3 = new MP3File(filename);
            TagContent content = mp3.getCopyrightText();
            String textContent = content.getTextContent();
            if (textContent == null)
                return false;

            // parse the TCOP frame and record the license and see if 
            // there is someplace to verify the file at
            StringTokenizer st = new StringTokenizer(textContent);
            boolean seenVerify = false;
            String license = null;
            while (!seenVerify && st.hasMoreTokens()) {
                String currToken = st.nextToken();
                if (currToken.startsWith("http") && (license == null))
                    license = currToken;
                if (currToken.equalsIgnoreCase("verify")) 
                    seenVerify = true;
            }
            if (!seenVerify)
                return false;

            // if there is someplace to verify the file at, verify the RDF
            if (st.nextToken().equalsIgnoreCase("at")) {
                String urlString = st.nextToken();
                URL url = new URL(urlString);
                HttpURLConnection http = 
                    (HttpURLConnection) url.openConnection();
                http.setInstanceFollowRedirects(true);
                http.connect();
                if (http.getResponseCode() != http.HTTP_ACCEPTED)
                    return false;
                
                // TODO:
                // we need to parse the source and see if license matches the
                // one as detailed by the rdf tag....
                return true;
            }
            else
                return false;

        }
        catch (MalformedURLException possible) {
            return false;
        }
        catch (NoMP3FrameException why) {
            throw new IOException();
        }
        catch (FrameDamagedException thatsucks) {
            throw new IOException();
        }
        catch (ID3v2Exception suckypoo) {
            throw new IOException();
        }
    }


    /** @return a Object[] with the following order: title, artist, album, year,
       track, comment, gen, bitrate, seconds.  Indices 0, 1, 2, 3, and 5 are
       Strings.  Indices 4 and 6 are Shorts.  Indices 7 and 8 are Integers.  
     */
    private static Object[] parseFile(File file) throws IOException {
        Object[] retObjs = new Object[9];

        // default vals...
        retObjs[0] = "";
        retObjs[1] = "";
        retObjs[2] = "";
        retObjs[3] = "";
        retObjs[5] = "";
        retObjs[4] = new Short((short)-1);
        retObjs[6] = new Short((short)-1);

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
        } finally {
            if( randomAccessFile != null )
                randomAccessFile.close();
        }
        return retObjs;
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
