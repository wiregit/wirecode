
package com.limegroup.gnutella.mp3;
import java.io.*;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;
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

public class MP3MetaData extends AudioMetaData {
	
	public MP3MetaData(File f) throws IOException {
		parseFile(f);
	}

	
	/**
     * Returns ID3Data for the file.
     *
     * LimeWire would prefer to use ID3V2 tags, so we try to parse the ID3V2
     * tags first, and if we were not able to find some tags using v2 we get it
     * using v1 if possible 
     */
    private void parseFile(File file) throws IOException {
        parseID3v2Data(file);
        
        MP3Info mp3Info = new MP3Info(file.getCanonicalPath());
        setBitrate(mp3Info.getBitRate());
        setLength((int)mp3Info.getLengthInSeconds());
        
        if(!isComplete()) {
            //AudioData v1 = parseID3v1Data(file);
            //data.mergeID3Data(v1);
        	parseID3v1Data(file);
        }
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
            setTitle(getString(buffer, 30));
            
            randomAccessFile.readFully(buffer, 0, 30);
            setArtist(getString(buffer, 30));

            randomAccessFile.readFully(buffer, 0, 30);
            setAlbum(getString(buffer, 30));
            
            randomAccessFile.readFully(buffer, 0, 4);
            setYear(getString(buffer, 4));
            
            randomAccessFile.readFully(buffer, 0, 30);
            int commentLength;
            if(buffer[28] == 0) {
                setTrack((short)ByteOrder.ubyte2int(buffer[29]));
                commentLength = 28;
            } else {
                setTrack((short)0);
                commentLength = 3;
            }
            setComment(getString(buffer, commentLength));
            
            // Genre
            randomAccessFile.readFully(buffer, 0, 1);
            setGenre(
                getGenreString((short)ByteOrder.ubyte2int(buffer[0])));
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
            if(idvx instanceof ID3v2BadParsingException)
                ErrorService.error(idvx); //we want to know about OutOfMemorys
            return ;
        } catch (IOException iox) {
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

            if(frameContent == null || frameContent.trim().equals(""))
                continue;
            //check which tag we are looking at
            if(MP3DataEditor.TITLE_ID.equals(frameID)) 
                setTitle(frameContent);
            else if(MP3DataEditor.ARTIST_ID.equals(frameID)) 
                setArtist(frameContent);
            else if(MP3DataEditor.ALBUM_ID.equals(frameID)) 
                setAlbum(frameContent);
            else if(MP3DataEditor.YEAR_ID.equals(frameID)) 
                setYear(frameContent);
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
            }
           else if(MP3DataEditor.TRACK_ID.equals(frameID)) {
                try {
                    setTrack(Short.parseShort(frameContent));
                } catch (NumberFormatException ignored) {} 
            }
            else if(MP3DataEditor.GENRE_ID.equals(frameID)) {
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
                    setGenre(getGenreString((short)genreCode));
                else 
                    setGenre(frameContent);
            }
        }
        
    }


}
