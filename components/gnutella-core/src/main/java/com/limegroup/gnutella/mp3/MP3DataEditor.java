
package com.limegroup.gnutella.mp3;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;

import com.sun.java.util.collections.ArrayList;
import com.sun.java.util.collections.Arrays;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.List;
import com.sun.java.util.collections.Vector;

import de.vdheide.mp3.ID3v2;
import de.vdheide.mp3.ID3v2DecompressionException;
import de.vdheide.mp3.ID3v2Exception;
import de.vdheide.mp3.ID3v2Frame;
import de.vdheide.mp3.NoID3v2TagException;

/**
 * an editor specifically for mp3 files with id3 tags
 */
public class MP3DataEditor extends AudioMetaDataEditor {
	
    private static final String TITLE_STRING   = "title=\"";
    private static final String ARTIST_STRING  = "artist=\"";
    private static final String ALBUM_STRING   = "album=\"";
    private static final String YEAR_STRING    = "year=\"";
    private static final String TRACK_STRING   = "track=\"";
    private static final String COMMENT_STRING = "comments=\"";
    private static final String GENRE_STRING   = "genre=\"";
    private static final String BITRATE_STRING = "bitrate=\"";
    private static final String SECONDS_STRING = "seconds=\"";

    private static final String ISO_LATIN_1 = "8859_1";
    private static final String UNICODE = "Unicode";
    
    static final String TITLE_ID = "TIT2";
    static final String ARTIST_ID = "TPE1";
    static final String ALBUM_ID = "TALB";
    static final String YEAR_ID = "TYER";
    static final String TRACK_ID = "TRCK";
    static final String COMMENT_ID = "COMM";
    static final String GENRE_ID = "TCON";
	/** 
	 * @return object[0] = (Integer) index just before beginning of tag=value, 
	 * object[1] = (Integer) index just after end of tag=value, object[2] =
	 * (String) value of tag.
	 * @exception Throw if rip failed.
	 */
	private Object[] ripTag(String source, String tagToRip) throws IOException {
	
	    Object[] retObjs = new Object[3];
	
	    int begin = source.indexOf(tagToRip);
	    if (begin < 0)
	        throw new IOException("tag not found");
	
	    if (begin != 0)            
	        retObjs[0] = new Integer(begin-1);
	    if (begin == 0)            
	        retObjs[0] = new Integer(begin);
	
	    int end = begin;
	    int i, j; // begin and end of value to return
	
	    for ( ; source.charAt(end) != '='; end++)
	        ; // found '=' sign
	    
	    for ( ; source.charAt(end) != '"'; end++)
	        ; // found first '"' sign
	    i = ++end;  // set beginning of value
	
	    for ( ; source.charAt(end) != '"'; end++)
	        ; // found second '"' sign
	    j = end; // set end of value
	
	    retObjs[1] = new Integer(end+1);
	    debug("ID3Editor.ripTag(): i = " + i +
	          ", j = " + j);
	    retObjs[2] = source.substring(i,j);
	                   
	    return retObjs;
	}
	/**
	 * Actually writes the ID3 tags out to the ID3V3 section of the mp3 file
	 */
	private int writeID3V2DataToDisk(File file) throws IOException, ID3v2Exception {
	    ID3v2 id3Handler = new ID3v2(file);        
	    Vector frames = null;
	    try {
	        frames = (Vector)id3Handler.getFrames().clone();
	    } catch (NoID3v2TagException ex) {//there are no ID3v2 tags in the file
	        //fall thro' we'll deal with it later -- frames will be null
	    }
	    
	    List framesToUpdate = new ArrayList();
	    addAllNeededFrames(framesToUpdate);
	    if(framesToUpdate.size() == 0) //we have nothing to update
	        return LimeXMLReplyCollection.NORMAL;
	    if(frames != null) { //old frames present, update the differnt ones 
	        for(Iterator iter=frames.iterator(); iter.hasNext(); ) {
	            ID3v2Frame oldFrame = (ID3v2Frame)iter.next();
	            //note: equality of ID3v2Frame based on value of id
	            int index = framesToUpdate.indexOf(oldFrame);
	            ID3v2Frame newFrame = null;
	            if(index >=0) {
	                newFrame = (ID3v2Frame)framesToUpdate.remove(index);
	                if(Arrays.equals(oldFrame.getContent(), 
	                                                   newFrame.getContent()))
	                    continue;//no need to update, skip this frame
	            }
	            //we are either going to replace it if it was changed, or remove
	            //it since there is no equivalent frame in the ones we need to
	            //update, this means the user probably removed it
	            id3Handler.removeFrame(oldFrame);
	            if(newFrame != null) 
	                id3Handler.addFrame(newFrame);
	        }
	    }
	    //now we are left with the ones we need to add only, if there were no
	    //old tags this will be all the frames that need to get updated
	    for(Iterator iter = framesToUpdate.iterator(); iter.hasNext() ; ) {
	        ID3v2Frame frame = (ID3v2Frame)iter.next();
	        id3Handler.addFrame(frame);
	    }
	    
	    id3Handler.update();
	    //No Exceptions? We are home
	    return LimeXMLReplyCollection.NORMAL;
	}
	private void addAllNeededFrames(List updateList) {
	    ID3v2Frame frame = null; 
	
	    if(title_ != null && !title_.equals("")) {
	        frame = makeFrame(TITLE_ID,title_);
	        if(frame!=null)
	            updateList.add(frame);
	    }
	    if (artist_!=null && !artist_.equals("")) {
	        frame = null;
	        frame = makeFrame(ARTIST_ID,artist_);
	        if(frame != null) 
	            updateList.add(frame);
	    }
	    if(album_ != null && !album_.equals("")) {
	        frame = null;
	        frame = makeFrame(ALBUM_ID, album_);
	        if(frame != null) 
	            updateList.add(frame);
	    }
	    if (year_!=null && !year_.equals("")) { 
	        frame = null;
	        frame = makeFrame(YEAR_ID, year_);
	        if(frame != null) 
	            updateList.add(frame);
	    }
	    if(track_ != null && !track_.equals("")) {
	        frame = null;
	        frame = makeFrame(TRACK_ID, track_);
	        if(frame !=  null)
	            updateList.add(frame);
	    }
	    if (comment_!=null && !comment_.equals("")) {
	        frame = null;
	        frame = makeFrame(COMMENT_ID, comment_);
	        if(frame != null) 
	            updateList.add(frame);
	    }
	    if(genre_ != null && !genre_.equals("")) {
	        frame = null;
	        //The ID3v2 standard requires the writing out of the byte we used in
	        //id3v1, so we need to add that in
	        if(getGenreByte() > -1) {
	            String v2Genre = "("+getGenreByte()+")"+genre_;
	            frame = makeFrame(GENRE_ID, v2Genre);
	        } else {
	            frame = makeFrame(GENRE_ID, genre_);
	        }
	        if(frame != null) 
	            updateList.add(frame);
	    }
	}
	private ID3v2Frame makeFrame(String frameID, String value) {
	    
	    boolean isISOLatin1 = true;
	    
	    // Basic/ISO-Latin-1: 0x0000 ... 0x00FF
	    // Unicode: > 0x00FF ??? Even with 3byte chars?
	    for(int i = 0; i < value.length(); i++) {
	        if (value.charAt(i) > 0x00FF) {
	            isISOLatin1 = false;
	            break;
	        }
	    }
	    
	    try {
	        return new ID3v2Frame(frameID, 
	                          value.getBytes((isISOLatin1) ? ISO_LATIN_1 : UNICODE), 
	                          true, //discard tag if it's altered/unrecognized
	                          true, //discard tag if file altered/unrecognized
	                          false,//read/write
	                          ID3v2Frame.NO_COMPRESSION, //no compression
	                          (byte)0,//no encryption
	                          (byte)0, //no Group
	                          isISOLatin1);
	    } catch(ID3v2DecompressionException cx) {
	        return null;
	    } catch (UnsupportedEncodingException err) {
	        return null;
	    }
	}
	/**
	 * Actually writes the ID3 tags out to the ID3V1 section of mp3 file.
	 */
	private int writeID3V1DataToDisk(RandomAccessFile file) {
	    byte[] buffer = new byte[30];//max buffer length...drop/pickup vehicle
	        
	    //see if there are ID3 Tags in the file
	    String tag="";
	    try {
	        file.readFully(buffer,0,3);
	        tag = new String(buffer,0,3);
	    } catch(EOFException e) {
	        return LimeXMLReplyCollection.RW_ERROR;
	    } catch(IOException e) {
	        return LimeXMLReplyCollection.RW_ERROR;
	    }
	    //We are sure this is an MP3 file.Otherwise this method would never
	    //be called.
	    if(!tag.equals("TAG")) {
	        //Write the TAG
	        try {
	            byte[] tagBytes = "TAG".getBytes();//has to be len 3
	            file.seek(file.length()-128);//reset the file-pointer
	            file.write(tagBytes,0,3);//write these three bytes into the File
	        } catch(IOException ioe) {
	            return LimeXMLReplyCollection.BAD_ID3;
	        }
	    }
	    debug("about to start writing to file");
	    boolean b;
	    b = toFile(title_,30,file,buffer);
	    if(!b)
	        return LimeXMLReplyCollection.FAILED_TITLE;
	    b = toFile(artist_,30,file,buffer);
	    if(!b)
	        return LimeXMLReplyCollection.FAILED_ARTIST;
	    b = toFile(album_,30,file,buffer);
	    if(!b)
	        return LimeXMLReplyCollection.FAILED_ALBUM;
	    b = toFile(year_,4,file,buffer);
	    if(!b)
	        return LimeXMLReplyCollection.FAILED_YEAR;
	    //comment and track (a little bit tricky)
	    b = toFile(comment_,28,file,buffer);//28 bytes for comment
	    if(!b)
	        return LimeXMLReplyCollection.FAILED_COMMENT;
	    
	    byte trackByte = (byte)-1;//initialize
	    try{
	        if (track_ == null || track_.equals(""))
	            trackByte = (byte)0;
	        else
	            trackByte = Byte.parseByte(track_);
	    } catch(NumberFormatException nfe) {
	        return LimeXMLReplyCollection.FAILED_TRACK;
	    }
	    
	    try{
	        file.write(0);//separator b/w comment and track(track is optional)
	        file.write(trackByte);
	    } catch(IOException e) {
	        return LimeXMLReplyCollection.FAILED_TRACK;
	    }
	    
	    //genre
	    byte genreByte= getGenreByte();
	    try {
	        file.write(genreByte);
	    } catch(IOException e) {
	        return LimeXMLReplyCollection.FAILED_GENRE;
	    }
	    //come this far means we are OK.
	    return LimeXMLReplyCollection.NORMAL;
	    
	}
	private boolean toFile(String val, int maxLen, RandomAccessFile file, byte[] buffer) {
	    debug("writing value to file "+val);
	    byte[] fromString;
	    
	    if (val==null || val.equals("")) {
	        fromString = new byte[maxLen];
	        Arrays.fill(fromString,0,maxLen,(byte)0);//fill it all with 0
	    } else {
	        try {
	            fromString = val.getBytes(ISO_LATIN_1);
	        } catch (UnsupportedEncodingException err) {
	            // Should never happen
	            return false;
	        }
	    }
	    
	    int len = fromString.length;
	    if (len < maxLen) {
	        System.arraycopy(fromString,0,buffer,0,len);
	        Arrays.fill(buffer,len,maxLen,(byte)0);//fill the rest with 0s
	    } else//cut off the rest
	        System.arraycopy(fromString,0,buffer,0,maxLen);
	        
	    try {
	        file.write(buffer,0,maxLen);
	    } catch (IOException e) {
	        return false;
	    }
	
	    return true;
	}
	
	private byte getGenreByte() {
	if(genre_==null) return -1;            
	else if(genre_.equals("Blues")) return 0;
	else if(genre_.equals("Classic Rock")) return 1;
	else if(genre_.equals("Country")) return 2;
	else if(genre_.equals("Dance")) return 3;
	else if(genre_.equals("Disco")) return 4;
	else if(genre_.equals("Funk")) return 5;
	else if(genre_.equals("Grunge")) return 6;
	else if(genre_.equals("Hop")) return 7;
	else if(genre_.equals("Jazz")) return 8;
	else if(genre_.equals("Metal")) return 9;
	else if (genre_.equals("New Age")) return 10;
	else if(genre_.equals("Oldies")) return 11;
	else if(genre_.equals("Other")) return 12;
	else if(genre_.equals("Pop")) return 13;
	else if (genre_.equals("R &amp; B")) return 14;
	else if(genre_.equals("Rap")) return 15;
	else if(genre_.equals("Reggae")) return 16;
	else if(genre_.equals("Rock")) return 17;
	else if(genre_.equals("Techno")) return 17;
	else if(genre_.equals("Industrial")) return 19;
	else if(genre_.equals("Alternative")) return 20;
	else if(genre_.equals("Ska")) return 21;
	else if(genre_.equals("Metal")) return 22;
	else if(genre_.equals("Pranks")) return 23;
	else if(genre_.equals("Soundtrack")) return 24;
	else if(genre_.equals("Euro-Techno")) return 25;
	else if(genre_.equals("Ambient")) return 26;
	else if(genre_.equals("Trip-Hop")) return 27;
	else if(genre_.equals("Vocal")) return 28;
	else if (genre_.equals("Jazz+Funk")) return 29;
	else if(genre_.equals("Fusion")) return 30;
	else if(genre_.equals("Trance")) return 31;
	else if(genre_.equals("Classical")) return 32;
	else if(genre_.equals("Instrumental")) return 33;
	else if(genre_.equals("Acid")) return 34;
	else if(genre_.equals("House")) return 35;
	else if(genre_.equals("Game")) return 36;
	else if(genre_.equals("Sound Clip")) return 37;
	else if(genre_.equals("Gospel")) return 38;
	else if(genre_.equals("Noise")) return 39;
	else if(genre_.equals("AlternRock")) return 40;
	else if(genre_.equals("Bass")) return 41;
	else if(genre_.equals("Soul")) return 42;
	else if(genre_.equals("Punk")) return 43;
	else if(genre_.equals("Space")) return 44;
	else if(genre_.equals("Meditative")) return 45;
	else if(genre_.equals("Instrumental Pop")) return 46;
	else if(genre_.equals("Instrumental Rock")) return 47;
	else if(genre_.equals("Ethnic")) return 48;
	else if(genre_.equals("Gothic")) return 49;
	else if(genre_.equals("Darkwave")) return 50;
	else if(genre_.equals("Techno-Industrial")) return 51;
	else if(genre_.equals("Electronic")) return 52;
	else if(genre_.equals("Pop-Folk")) return 53;
	else if(genre_.equals("Eurodance")) return 54;
	else if(genre_.equals("Dream")) return 55;
	else if(genre_.equals("Southern Rock")) return 56;
	else if(genre_.equals("Comedy")) return 57;
	else if(genre_.equals("Cult")) return 58;
	else if(genre_.equals("Gangsta")) return 59;
	else if(genre_.equals("Top 40")) return 60;
	else if(genre_.equals("Christian Rap")) return 61;
	else if(genre_.equals("Pop/Funk")) return 62;
	else if(genre_.equals("Jungle")) return 63;
	else if(genre_.equals("Native American")) return 64;
	else if(genre_.equals("Cabaret")) return 65;
	else if(genre_.equals("New Wave")) return 66;
	else if(genre_.equals("Psychadelic")) return 67;
	else if(genre_.equals("Rave")) return 68;
	else if(genre_.equals("Showtunes")) return 69;
	else if(genre_.equals("Trailer")) return 70;
	else if(genre_.equals("Lo-Fi")) return 71;
	else if(genre_.equals("Tribal")) return 72;
	else if(genre_.equals("Acid Punk")) return 73;
	else if(genre_.equals("Acid Jazz")) return 74;
	else if(genre_.equals("Polka")) return 75;
	else if(genre_.equals("Retro")) return 76;
	else if(genre_.equals("Musical")) return 77;
	else if(genre_.equals("Rock &amp; Roll")) return 78;
	else if(genre_.equals("Hard Rock")) return 79;
	else if(genre_.equals("Folk")) return 80;
	else if(genre_.equals("Folk-Rock")) return 81;
	else if(genre_.equals("National Folk")) return 82;
	else if(genre_.equals("Swing")) return 83;
	else if(genre_.equals("Fast Fusion")) return 84;
	else if(genre_.equals("Bebob")) return 85;
	else if(genre_.equals("Latin")) return 86;
	else if(genre_.equals("Revival")) return 87;
	else if(genre_.equals("Celtic")) return 88;
	else if(genre_.equals("Bluegrass")) return 89;
	else if(genre_.equals("Avantgarde")) return 90;
	else if(genre_.equals("Gothic Rock")) return 91;
	else if(genre_.equals("Progressive Rock")) return 92;
	else if(genre_.equals("Psychedelic Rock")) return 93;
	else if(genre_.equals("Symphonic Rock")) return 94;
	else if(genre_.equals("Slow Rock")) return 95;
	else if(genre_.equals("Big Band")) return 96;
	else if(genre_.equals("Chorus")) return 97;
	else if(genre_.equals("Easy Listening")) return 98;
	else if(genre_.equals("Acoustic")) return 99;
	else if(genre_.equals("Humour")) return 100;
	else if(genre_.equals("Speech")) return 101;
	else if(genre_.equals("Chanson")) return 102;
	else if(genre_.equals("Opera")) return 103;
	else if(genre_.equals("Chamber Music")) return 104;
	else if(genre_.equals("Sonata")) return 105;
	else if(genre_.equals("Symphony")) return 106;
	else if(genre_.equals("Booty Bass")) return 107;
	else if(genre_.equals("Primus")) return 108;
	else if(genre_.equals("Porn Groove")) return 109;
	else if(genre_.equals("Satire")) return 110;
	else if(genre_.equals("Slow Jam")) return 111;
	else if(genre_.equals("Club")) return 112;
	else if(genre_.equals("Tango")) return 113;
	else if(genre_.equals("Samba")) return 114;
	else if(genre_.equals("Folklore")) return 115;
	else if(genre_.equals("Ballad")) return 116;
	else if(genre_.equals("Power Ballad")) return 117;
	else if(genre_.equals("Rhythmic Soul")) return 118;
	else if(genre_.equals("Freestyle")) return 119;
	else if(genre_.equals("Duet")) return 120;
	else if(genre_.equals("Punk Rock")) return 121;
	else if(genre_.equals("Drum Solo")) return 122;
	else if(genre_.equals("A capella")) return 123;
	else if(genre_.equals("Euro-House")) return 124;
	else if(genre_.equals("Dance Hall")) return 125;
	else return -1;
	}
    
	public int commitMetaData(String filename) {
        if(! LimeXMLUtils.isMP3File(filename))
            return LimeXMLReplyCollection.INCORRECT_FILETYPE;
        File f= null;
        RandomAccessFile file = null;        
        try {
            try {
                f = new File(filename);
                FileUtils.setWriteable(f);
                file = new RandomAccessFile(f,"rw");
            } catch(IOException e) {
                return LimeXMLReplyCollection.FILE_DEFECTIVE;
            }
            long length=0;
            try{
                length = file.length();
                if(length < 128) //could not write - file too small
                    return LimeXMLReplyCollection.FILE_DEFECTIVE;
                file.seek(length - 128);
            } catch(IOException ee) {
                return LimeXMLReplyCollection.RW_ERROR;
            }
            //1. Try to write out the ID3v2 data first
            int ret = -1;
            try {
                ret = writeID3V2DataToDisk(f);
            }  catch (IOException iox ) {
                return LimeXMLReplyCollection.RW_ERROR;  
            } catch (ID3v2Exception e) { //catches both ID3v2 related exceptions
                ret = writeID3V1DataToDisk(file);
            } 
            return ret;
        } 
        finally {
            if( file != null ) {
                try {
                    file.close();
                } catch(IOException ignored) {}
            }
        }
    }
	/** 
	 * The caller of this method has the xml string that represents a
	 * LimeXMLDocument, and wants to write the document out to disk. For this
	 * method to work effectively, the caller must instantiate this class and
	 * call this method first, and then call to actually write the ID3
	 * tags out.
	 * <p>
	 * This method reads the complete xml string and removes the id3 *
	 * components of the xml string, and stores the values of the id3 tags in a
	 * class variable which will later be used to write the id3 tags in the
	 * mp3file.
	 * <p>
	 * @return a parseable xml string which has the same attributes as the
	 * xmlStr paramter minus the id3 tags.
	 */
	public String removeID3Tags(String xmlStr) {
	    //will be used to reconstruct xmlStr after ripping stuff from it
	    int i, j;
	    Object[] rippedStuff = null;
	
	    //title        
	    try {
	        rippedStuff = ripTag(xmlStr, TITLE_STRING);
	
	        title_ = (String)rippedStuff[2];
	        debug("title = "+title_);
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //artist
	    try {
	        rippedStuff = ripTag(xmlStr, ARTIST_STRING);
	
	        artist_ = (String)rippedStuff[2];
	        debug("artist = "+artist_);
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //album
	    try {
	        rippedStuff = ripTag(xmlStr, ALBUM_STRING);
	
	        album_ = (String)rippedStuff[2];
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //year
	    try {
	        rippedStuff = ripTag(xmlStr, YEAR_STRING);
	
	        year_ = (String)rippedStuff[2];
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //track
	    try {
	        rippedStuff = ripTag(xmlStr, TRACK_STRING);
	
	        track_ = (String)rippedStuff[2];
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //comment
	    try {
	        rippedStuff = ripTag(xmlStr, COMMENT_STRING);
	
	        comment_ = (String)rippedStuff[2];
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //genre
	    try {
	        rippedStuff = ripTag(xmlStr, GENRE_STRING);
	
	        genre_ = (String)rippedStuff[2];
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //bitrate
	    try {
	        rippedStuff = ripTag(xmlStr, BITRATE_STRING);
	
	        // we get bitrate info from the mp3 file....
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	    //seconds
	    try {
	        rippedStuff = ripTag(xmlStr, SECONDS_STRING);
	
	        // we get seconds info from the mp3 file....
	
	        i = ((Integer)rippedStuff[0]).intValue();
	        j = ((Integer)rippedStuff[1]).intValue();        
	        xmlStr = xmlStr.substring(0,i) + xmlStr.substring(j,xmlStr.length());
	    } 
	    catch (IOException e) {};
	
	
	
	    return xmlStr;//this has been suitable modified
	}

	
}
