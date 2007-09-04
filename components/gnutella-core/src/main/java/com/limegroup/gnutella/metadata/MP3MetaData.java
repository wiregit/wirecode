
package com.limegroup.gnutella.metadata;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.limewire.util.ByteOrder;


import de.vdheide.mp3.ID3v2;
import de.vdheide.mp3.ID3v2Exception;
import de.vdheide.mp3.ID3v2Frame;
import de.vdheide.mp3.NoID3v2TagException;

/**
 * Provides a utility method to read ID3 Tag information from MP3
 * files and creates XMLDocuments from them. 
 *
 * @author Sumeet Thadani
 */

public class MP3MetaData extends AudioMetaData {
	
	public MP3MetaData(File f) throws IOException {
		super(f);
	}

	
	/**
     * Returns ID3Data for the file.
     *
     * LimeWire would prefer to use ID3V2 tags, so we try to parse the ID3V2
     * tags first, and then v1 to get any missing tags.
     */
    protected void parseFile(File file) throws IOException {
        parseID3v2Data(file);
        
        MP3Info mp3Info = new MP3Info(file.getCanonicalPath());
        setBitrate(mp3Info.getBitRate());
        setLength((int)mp3Info.getLengthInSeconds());
        
        parseID3v1Data(file);
        
    }

    /**
     * Parses the file's id3 data.
     */
    private void parseID3v1Data(File file) {
        
        // not long enough for id3v1 tag?
        if(file.length() < 128)
            return;
        
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
                return;
            
            // We have an ID3 Tag, now get the parts

            randomAccessFile.readFully(buffer, 0, 30);
            if (getTitle() == null || getTitle().equals(""))
            	setTitle(getString(buffer, 30));
            
            randomAccessFile.readFully(buffer, 0, 30);
            if (getArtist() == null || getArtist().equals(""))
            	setArtist(getString(buffer, 30));

            randomAccessFile.readFully(buffer, 0, 30);
            if (getAlbum() == null || getAlbum().equals(""))
            	setAlbum(getString(buffer, 30));
            
            randomAccessFile.readFully(buffer, 0, 4);
            if (getYear() == null || getYear().equals(""))
            	setYear(getString(buffer, 4));
            
            randomAccessFile.readFully(buffer, 0, 30);
            int commentLength;
            if (getTrack()==0 || getTrack()==-1){
            	if(buffer[28] == 0) {
            		setTrack((short)ByteOrder.ubyte2int(buffer[29]));
            		commentLength = 28;
            	} else {
            		setTrack((short)0);
            		commentLength = 3;
            	}
            	if (getComment()==null || getComment().equals(""))
            		setComment(getString(buffer, commentLength));
            }
            // Genre
            randomAccessFile.readFully(buffer, 0, 1);
            if (getGenre() ==null || getGenre().equals(""))
            	setGenre(
            			MP3MetaData.getGenreString((short)ByteOrder.ubyte2int(buffer[0])));
        } catch(IOException ignored) {
        } finally {
            if( randomAccessFile != null )
                try {
                    randomAccessFile.close();
                } catch(IOException ignored) {}
        }
        
    }
    
    /**
     * Helper method to generate a string from an id3v1 filled buffer.
     */
    private String getString(byte[] buffer, int length) {
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
    private void parseID3v2Data(File file) {
        
        ID3v2 id3v2Parser = null;
        try {
            id3v2Parser = new ID3v2(file);
        } catch (ID3v2Exception idvx) { //can't go on
            return ;
        } catch (IOException iox) {
            return ;
        } catch(ArrayIndexOutOfBoundsException ignored) {
            return ;
        }
        

        Vector frames = null;
        try {
            frames = id3v2Parser.getFrames();
        } catch (NoID3v2TagException ntx) {
            return ;
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

            // need to check is PRIV field here since frameContent may be null in ISO_LATIN format
            //  but not UTF-8 format
            if( (frameContent == null || frameContent.trim().equals("")) &&
                    !MP3DataEditor.PRIV_ID.equals(frameID)){
                continue;
            }
            //check which tag we are looking at
            if(MP3DataEditor.TITLE_ID.equals(frameID)) 
                setTitle(frameContent);
            else if(MP3DataEditor.ARTIST_ID.equals(frameID)) 
                setArtist(frameContent);
            else if(MP3DataEditor.ALBUM_ID.equals(frameID)) 
                setAlbum(frameContent);
            else if(MP3DataEditor.YEAR_ID.equals(frameID)) {
                setYear(frameContent);
                checkLWS(frameContent );
            }
            else if(MP3DataEditor.COMMENT_ID.equals(frameID)) {
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
                setComment(frameContent);
                checkLWS(frameContent );
            }
            else if(MP3DataEditor.TRACK_ID.equals(frameID)) {
                try {
                    setTrack(Short.parseShort(frameContent));
                } catch (NumberFormatException ignored) {} 
            }
            else if(MP3DataEditor.GENRE_ID.equals(frameID)) {
                if( frameContent == null )
                    continue;
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
                if(startIndex > -1 &&
                   endIndex > -1 &&
                   startIndex < endIndex && 
                   startIndex < frameContent.length()) { 
                    //we have braces check if it's valid
                    String genreByte = frameContent.substring(startIndex+1, endIndex);
                    
                    try {
                        genreCode = Integer.parseInt(genreByte);
                    } catch (NumberFormatException nfx) {
                        genreCode = -1;
                    }
                }
                
                if(genreCode >= 0 && genreCode <= 127)
                    setGenre(MP3MetaData.getGenreString((short)genreCode));
                else 
                    setGenre(frameContent);
            }
            else if (MP3DataEditor.LICENSE_ID.equals(frameID)) {
                setLicense(frameContent);
                checkLWS(frameContent );
            }
            // PRIV fields in LWS songs are encoded in UTF-8 format
            else if (MP3DataEditor.PRIV_ID.equals(frameID)) {
                try {
                    String content = new String(contentBytes,"UTF-8");
                    checkLWS(content);
                } catch (UnsupportedEncodingException e) {
                }
            }
            // another key we don't care about except for searching for 
            //  protected content
            else {
                checkLWS(frameContent );
            }
            }
        }
        
    /**
     * If the song is not a LWS already, do a substring search to see if
     * it is a LWS song
     * @param content
     */
    private void checkLWS(String content) {
        if( getEncoder() != LWS) {
            if( containsKey(content, MAGIC_KEY) ) {
                setEncoder(LWS);
            }
       }
    }

    /**
     * Determines if a substring exists in the file
     * @param file - string to search
     * @param key - substring to search for
     * 
     * @return - return true if the substring is contained at least once in the string
     */
    private static boolean containsKey(String file, String key) {
        if( file == null || key == null || file.length() == 0 )
            return false;
        Pattern pattern  = Pattern.compile(key);
        Matcher matcher = pattern.matcher(file);
        return matcher.find();
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
	    case 62: return "Pop+Funk";
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
