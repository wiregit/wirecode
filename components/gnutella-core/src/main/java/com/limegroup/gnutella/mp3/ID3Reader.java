package com.limegroup.gnutella.mp3;

import java.io.*;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import de.vdheide.mp3.*;

/**
 * Provides a utility method to read ID3 Tag information from MP3
 * files and creates XMLDocuments from them. 
 *
 * @author Sumeet Thadani
 */
public final class ID3Reader {
    public static final String schemaURI = 
         "http://www.limewire.com/schemas/audio.xsd";

    private static final String ISO_LATIN_1 = "8859_1";
    private static final String UNICODE = "Unicode";
    
    private static final String KEY_PREFIX = "audios" + 
        XMLStringUtils.DELIMITER + "audio" + XMLStringUtils.DELIMITER;
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
     * Generates an XML description of the file's id3 data.
     * @param id3Only true if String we return is the only document associated
     * with the file. 
     */
    public static String readDocument(File file, boolean id3Only) 
                                                            throws IOException {
        ID3Data data = parseFile(file);
        return data.toXML(file.getCanonicalPath(), id3Only);
    }

    /**
     * Generates a LimeXMLDocument of the file's id3 data.
     */
    public static LimeXMLDocument readDocument(File file) throws IOException {
        ID3Data data = parseFile(file);
        List nameValList = data.toNameValueList();
        if(nameValList.isEmpty())
            throw new IOException("invalid/no data.");

        return new LimeXMLDocument(nameValList, schemaURI);
    }

    /**
     * Returns ID3Data for the file.
     *
     * LimeWire would prefer to use ID3V2 tags, so we try to parse the ID3V2
     * tags first, and if we were not able to find some tags using v2 we get it
     * using v1 if possible 
     */
    private static ID3Data parseFile(File file) throws IOException {
        ID3Data data = parseID3v2Data(file);
        
        MP3Info mp3Info = new MP3Info(file.getCanonicalPath());
        data.setBitrate(mp3Info.getBitRate());
        data.setLength((int)mp3Info.getLengthInSeconds());
        
        if(data.isComplete()) {
            return data;
        } else {
            ID3Data v1 = parseID3v1Data(file);
            data.mergeID3Data(v1);
            return data;
        }
    }

    /**
     * Parses the file's id3 data.
     */
    private static ID3Data parseID3v1Data(File file) {
        ID3Data data = new ID3Data();
        
        // not long enough for id3v1 tag?
        if(file.length() < 128)
            return data;
        
        RandomAccessFile randomAccessFile = null;        
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
            long length = randomAccessFile.length();
            randomAccessFile.seek(length - 128);
            byte[] buffer = new byte[30];
            
            // If tag is wrong, no id3v1 data.
            randomAccessFile.readFully(buffer, 0, 3);
            String tag = new String(buffer, 0, 3);
            if(!tag.equals("TAG"))
                return data;
            
            // We have an ID3 Tag, now get the parts

            randomAccessFile.readFully(buffer, 0, 30);
            data.setTitle(getString(buffer, 30));
            
            randomAccessFile.readFully(buffer, 0, 30);
            data.setArtist(getString(buffer, 30));

            randomAccessFile.readFully(buffer, 0, 30);
            data.setAlbum(getString(buffer, 30));
            
            randomAccessFile.readFully(buffer, 0, 4);
            data.setYear(getString(buffer, 4));
            
            randomAccessFile.readFully(buffer, 0, 30);
            int commentLength;
            if(buffer[28] == 0) {
                data.setTrack((short)ByteOrder.ubyte2int(buffer[29]));
                commentLength = 28;
            } else {
                data.setTrack((short)0);
                commentLength = 3;
            }
            data.setComment(getString(buffer, commentLength));
            
            // Genre
            randomAccessFile.readFully(buffer, 0, 1);
            data.setGenre(
                getGenreString((short)ByteOrder.ubyte2int(buffer[0])));
        } catch(IOException ignored) {
        } finally {
            if( randomAccessFile != null )
                try {
                    randomAccessFile.close();
                } catch(IOException ignored) {}
        }
        return data;
    }
    
    /**
     * Helper method to generate a string from an id3v1 filled buffer.
     */
    private static String getString(byte[] buffer, int length) {
        try {
            return new String(buffer, 0, getTrimmedLength(buffer, length), ISO_LATIN_1);
        } catch (UnsupportedEncodingException err) {
            // should never happen
            return null;
        }
    }

    /**
     * Generates ID3Data from id3v2 data in the file.
     */
    private static ID3Data parseID3v2Data(File file) {
        ID3Data data = new ID3Data();
        
        ID3v2 id3v2Parser = null;
        try {
            id3v2Parser = new ID3v2(file);
        } catch (ID3v2Exception idvx) { //can't go on
            return data;
        } catch (IOException iox) {
            return data;
        }
        
        Vector frames = null;
        try {
            frames = id3v2Parser.getFrames();
        } catch (NoID3v2TagException ntx) {
            return data;
        }
        
        //rather than getting each frame indvidually, we can get all the frames
        //and iterate, leaving the ones we are not concerned with
        for(Iterator iter=frames.iterator() ; iter.hasNext() ; ) {
            ID3v2Frame frame = (ID3v2Frame)iter.next();
            String frameID = frame.getID();
            byte[] contentBytes = frame.getContent();
            String frameContent = null;
            
            if (contentBytes.length > 0) {
                try {
                    String enc = (frame.isISOLatin1()) ? ISO_LATIN_1 : UNICODE;
                    frameContent = new String(contentBytes, enc).trim();
                } catch (UnsupportedEncodingException err) {
                    // should never happen
                }
            }

            if(frameContent == null || frameContent.trim().equals(""))
                continue;
            //check which tag we are looking at
            if(ID3Editor.TITLE_ID.equals(frameID)) 
                data.setTitle(frameContent);
            else if(ID3Editor.ARTIST_ID.equals(frameID)) 
                data.setArtist(frameContent);
            else if(ID3Editor.ALBUM_ID.equals(frameID)) 
                data.setAlbum(frameContent);
            else if(ID3Editor.YEAR_ID.equals(frameID)) 
                data.setYear(frameContent);
            else if(ID3Editor.COMMENT_ID.equals(frameID)) {
                //ID3v2 comments field has null separators embedded to encode
                //language etc, the real content begins after the last null
                byte[] bytes = frame.getContent();
                int startIndex = 0;
                for(int i=bytes.length-1; i>= 0; i--) {
                    if(bytes[i] != (byte)0)
                        continue;
                    //OK we are the the last 0
                    startIndex = i;
                    break;
                }
                frameContent = 
                  new String(bytes, startIndex, bytes.length-startIndex).trim();
                data.setComment(frameContent);
            }
           else if(ID3Editor.TRACK_ID.equals(frameID)) {
                try {
                    data.setTrack(Short.parseShort(frameContent));
                } catch (NumberFormatException ignored) {} 
            }
            else if(ID3Editor.GENRE_ID.equals(frameID)) {
                //ID3v2 frame for genre has the byte used in ID3v1 encoded
                //within it -- we need to parse that out
                int startIndex = frameContent.indexOf("(");
                int endIndex = frameContent.indexOf(")");
                int genreCode = -1;
                
                //Note: It's possible that the user entered her own genre in
                //which case there could be spurious braces, the id3v2 braces
                //enclose values between 0 - 127 
                
                // Custom genres are just plain text and default genres (known
                // from id3v1) are referenced with values enclosed by braces and
                // with optional refinements which I didn't implement here.
                // http://www.id3.org/id3v2.3.0.html#TCON
                if(startIndex > -1 && endIndex > -1) { 
                    //we have braces check if it's valid
                    String genreByte = 
                    frameContent.substring(startIndex+1, endIndex);
                    
                    try {
                        genreCode = Integer.parseInt(genreByte);
                    } catch (NumberFormatException nfx) {
                        genreCode = -1;
                    }
                }
                
                if(genreCode >= 0 && genreCode <= 127)
                    data.setGenre(getGenreString((short)genreCode));
                else 
                    data.setGenre(frameContent);
            }
        }
        return data;
    }

    /**
     * Appends the key/value & a "\" to the string buffer.
     */
    private static void appendStrings(String key, String value,
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
     * Simple class to encapsulate information about ID3 tags.
     * Can write the data to a NameValue list or an XML string.
     */
    static class ID3Data {
        private String title;
        private String artist;
        private String album;
        private String year;
        private String comment;
        private short track = -1;
        private String genre;
        private int bitrate = -1;
        private int length = -1;
        
        public String toString() {
            return "ID3Data: title[" + title + "], artist[" + artist +
                   "], album[" + album + "], year[" + year + "], comment["
                   + comment + "], track[" + track + "], genre[" + genre +
                   "], bitrate[" + bitrate + "], length[" + length +"]";
        }          
        
        String getTitle() { return title; }
        String getArtist() { return artist; }
        String getAlbum() { return album; }
        String getYear() { return year; }
        String getComment()  { return comment; }
        short getTrack() { return track; }
        String getGenre() { return genre; }
        int getBitrate() { return bitrate; }
        int getLength() { return length; }
        
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
        void mergeID3Data(ID3Data data) {
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
        boolean isComplete() {
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
        List toNameValueList() {
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
        String toXML(String path, boolean id3Only) {
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
                
    }

}
