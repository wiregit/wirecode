package com.limegroup.gnutella.xml;

import java.io.*;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.xml.*;

/**
 * Provides a utility method to read ID3 Tag information from MP3
 * files and creates GMLDocuments from them. 
 *
 * @author Sumeet Thadani
 */
final class ID3Reader {
    private static final String schemaURI = 
         "http://www.limewire.com/schemas/audio.xsd";

    /**
     * Attempts to read an ID3 tag from the specified file.
     * @return an null if the document has no ID3 tag
     */
    public final LimeXMLDocument readDocument (File file) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        long length = randomAccessFile.length();

        // We need to read at least 128 bytes
        if(length < 128)
            return null;

        randomAccessFile.seek(length - 128);
        byte[] buffer = new byte[30];

        // Read ID3 Tag, return null if not present
        randomAccessFile.readFully(buffer, 0, 3);
        String tag = new String(buffer, 0, 3, "Cp437");

        if(!tag.equals("TAG"))
            return null;

        // We have an ID3 Tag, now get the parts
        // Title
        randomAccessFile.readFully(buffer, 0, 30);
        String title = new String(buffer, 0, getTrimmedLength(buffer, 30));

        // Artist
        randomAccessFile.readFully(buffer, 0, 30);
        String artist = new String(buffer, 0, getTrimmedLength(buffer, 30));

        // Album
        randomAccessFile.readFully(buffer, 0, 30);
        String album = new String(buffer, 0, getTrimmedLength(buffer, 30));

        // Year
        randomAccessFile.readFully(buffer, 0, 4);
        String year = new String(buffer, 0, getTrimmedLength(buffer, 4));

        // Comment and track
        short track;
        randomAccessFile.readFully(buffer, 0, 30);
        int commentLength;
        if(buffer[28] == 0)
        {
            track = (short)ByteOrder.ubyte2int(buffer[29]);
            commentLength = 28;
        }
        else
        {
            track = 0;
            commentLength = 30;
        }
        String comment = new String(buffer, 0,
            getTrimmedLength(buffer, commentLength));

        // Genre
        randomAccessFile.readFully(buffer, 0, 1);
        short gen = (short)ByteOrder.ubyte2int(buffer[0]);

        String str = "<audio noNamespaceSchemaLocation=\""+this.schemaURI;
        String filename = file.getCanonicalPath();
        str = str+"\""+" identifier=\""+filename+"\">";
        //end of head
        str=str+"<title>"+title+"</title>";
        str = str+"<artist>"+artist+"</artist>";
        str = str+"<album>"+album+"</album>";
        str = str+"<track>"+track+"</track>";
        String genre = getGenreString(gen);
        str = str+"<genre>"+genre+"</genre>";
        str = str+"<year>"+year+"</year>";
        str = str+"<comments>"+comment+"</comments>";
        str = str+"</audio>";
        LimeXMLDocument doc;
        try{
            doc = new LimeXMLDocument(str);
        }catch(Exception e){
            return null;
        }
        return doc;
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
        case 1: return "Blues";
        case 2: return "Classic Rock";
        case 3: return "Country";
        case 4: return "Dance";
        case 5: return "Disco";
        case 6: return "Funk";
        case 7: return "Grunge";
        case 8: return "Hip-Hop";
        case 9: return "Jazz";
        case 10: return "Metal";
        case 11: return  "New Age";
        case 12: return "Oldies";
        case 13: return "Other";
        case 14: return "Pop";
        case 15 : return "R &amp; B";
        case 16: return "Rap";
        case 17: return "Reggae";
        case 18: return "Rock";
        case 19: return "Techno";
        case 20: return "Industrial";
        case 21: return "Alternative";
        case 22: return "Ska";
        case 23: return "Death Metal";
        case 24: return "Pranks";
        case 25: return "Soundtrack";
        case 26: return "Euro-Techno";
        case 27: return "Ambient";
        case 28: return "Trip-Hop";
        case 29: return "Vocal";
        case 30: return "Jazz+Funk";
        case 31: return "Fusion";
        case 32: return "Trance";
        case 33: return "Classical";
        case 34: return "Instrumental";
        case 35: return "Acid";
        case 36: return "House";
        case 37: return "Game";
        case 38: return "Sound Clip";
        case 39: return "Gospel";
        case 40: return "Noise";
        case 41: return "AlternRock";
        case 42: return "Bass";
        case 43: return "Soul";
        case 44: return "Punk";
        case 45: return "Space";
        case 46: return "Meditative";
        case 47: return "Instrumental Pop";
        case 48: return "Instrumental Rock";
        case 49: return "Ethnic";
        case 50: return "Gothic";
        case 51: return "Darkwave";
        case 52: return "Techno-Industrial";
        case 53: return "Electronic";
        case 54: return "Pop-Folk";
        case 55: return "Eurodance";
        case 56: return "Dream";
        case 57: return "Southern Rock";
        case 58: return "Comedy";
        case 59: return "Cult";
        case 60: return "Gangsta";
        case 61: return "Top 40";
        case 62: return "Christian Rap";
        case 63: return "Pop/Funk";
        case 64: return "Jungle";
        case 65: return "Native American";
        case 66: return "Cabaret";
        case 67: return "New Wave";
        case 68: return "Psychadelic";
        case 69: return "Rave";
        case 70: return "Showtunes";
        case 71: return "Trailer";
        case 72: return "Lo-Fi";
        case 73: return "Tribal";
        case 74: return "Acid Punk";
        case 75: return "Acid Jazz";
        case 76: return "Polka";
        case 77: return "Retro";
        case 78: return "Musical";
        case 79: return "Rock &amp; Roll";
        case 80: return "Hard Rock";
        case 81: return "Folk";
        case 82: return "Folk-Rock";
        case 83: return "National Folk";
        case 84: return "Swing";
        case 85: return "Fast Fusion";
        case 86: return "Bebob";
        case 87: return "Latin";
        case 88: return "Revival";
        case 89: return "Celtic";
        case 90: return "Bluegrass";
        case 91: return "Avantgarde";
        case 92: return "Gothic Rock";
        case 93: return "Progressive Rock";
        case 94: return "Psychedelic Rock";
        case 95: return "Symphonic Rock";
        case 96: return "Slow Rock";
        case 97: return "Big Band";
        case 98: return "Chorus";
        case 99: return "Easy Listening";
        case 100: return "Acoustic";
        case 101: return "Humour";
        case 102: return "Speech";
        case 103: return "Chanson";
        case 104: return "Opera";
        case 105: return "Chamber Music";
        case 106: return "Sonata";
        case 107: return "Symphony";
        case 108: return "Booty Bass";
        case 109: return "Primus";
        case 110: return "Porn Groove";
        case 111: return "Satire";
        case 112: return "Slow Jam";
        case 113: return "Club";
        case 114: return "Tango";
        case 115: return "Samba";
        case 116: return "Folklore";
        case 117: return "Ballad";
        case 118: return "Power Ballad";
        case 119: return "Rhythmic Soul";
        case 120: return "Freestyle";
        case 121: return "Duet";
        case 122: return "Punk Rock";
        case 123: return "Drum Solo";
        case 124: return "A capella";
        case 125: return "Euro-House";
        case 126: return "Dance Hall";
        default: return "";
        }
    }
}
