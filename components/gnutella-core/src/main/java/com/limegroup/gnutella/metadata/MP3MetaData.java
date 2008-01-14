
package com.limegroup.gnutella.metadata;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v11Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;



public class MP3MetaData extends AudioMetaData {
	
	public MP3MetaData(File f) throws IOException {
		super(f);
	}

    static final String LICENSE_ID = "TCOP";
    static final String PRIV_ID = "PRIV";
	
	
	/**
     * Returns ID3Data for the file.
     *
     * LimeWire would prefer to use ID3V2 tags, so we try to parse the ID3V2
     * tags first, and then v1 to get any missing tags.
     */
    protected void parseFile(File file) throws IOException { 
//        parseID3v2Data(file);
        
//        MP3Info mp3Info = new MP3Info(file.getCanonicalPath());
//        setBitrate(mp3Info.getBitRate());
//        setLength((int)mp3Info.getLengthInSeconds());
        
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader header = audioFile.getAudioHeader();
            
            setVBR(header.isVariableBitRate());
            setSampleRate(header.getSampleRateAsNumber());
            setBitrate((int)header.getBitRateAsNumber());
            setLength(header.getTrackLength());
            
            Tag tag = audioFile.getTag();
            if(tag != null ) {
                setTitle(tag.getFirstTitle());
                setArtist(tag.getFirstArtist());
                setAlbum(tag.getFirstAlbum());
                setYear(tag.getFirstYear());
                setComment(tag.getFirstComment());


                setGenre(tag.getFirstGenre());

//                if( !(tag instanceof ID3v1Tag && !(tag instanceof ID3v11Tag)) ) {
//                  if( tag.getFirstTrack() != null && tag.getFirstTrack().length() > 0)
//                      setTrack(Short.parseShort(tag.getFirstTrack()));
//                  else    
//                      setTrack(""); 
//                }           
            }
            if( !(tag instanceof ID3v1Tag) ) { System.out.println("searching for magic string ");
                MP3File mp3File = ((MP3File)audioFile);

                AbstractID3v2Tag vTag = mp3File.getID3v2Tag();
                if( vTag != null ) {
                    List<TagField> license = vTag.get(LICENSE_ID);
                    List<TagField> priv = vTag.get(PRIV_ID);
                    System.out.println("found " + license.size() + " " + priv.size());
                    Iterator<TagField> iter = license.iterator();
                    while(iter.hasNext()) {
                        TagField t = iter.next();
                        checkLWS(t.toString());
                    }
                    iter = priv.iterator();
                    while(iter.hasNext()) {
                        TagField t = iter.next(); System.out.println("     " + t.toString() + " " + t.getRawContent());
                        checkLWS(t.toString());
                        isPRIVCheck(t.getRawContent());
                    }
                    if(license.size() > 0) {
                        TagField t = license.get(0);
                        if( t != null )
                            setLicense( t.toString() );
                    }
                }
            }
        } catch (CannotReadException e) {
            // TODO Auto-generated catch block
//            e.printStackTrace();
        } catch (TagException e) {
            // TODO Auto-generated catch block
//            e.printStackTrace();
        } catch (ReadOnlyFileException e) {
            // TODO Auto-generated catch block
//            e.printStackTrace();
        } catch (InvalidAudioFrameException e) {
            // TODO Auto-generated catch block
//            e.printStackTrace();
        }

    }

    /**
     * Parses the file's id3 data.
     */
    private void parseID3v1Data(File file) {
        
//        // not long enough for id3v1 tag?
//        if(file.length() < 128)
//            return;
//        
//        RandomAccessFile randomAccessFile = null;        
//        try {
//            randomAccessFile = new RandomAccessFile(file, "r");
//            long length = randomAccessFile.length();
//            randomAccessFile.seek(length - 128);
//            byte[] buffer = new byte[30];
//            
//            // If tag is wrong, no id3v1 data.
//            randomAccessFile.readFully(buffer, 0, 3);
//            String tag = new String(buffer, 0, 3);
//            if(!tag.equals("TAG"))
//                return;
//            
//            // We have an ID3 Tag, now get the parts
//
//            randomAccessFile.readFully(buffer, 0, 30);
//            if (getTitle() == null || getTitle().equals(""))
//            	setTitle(getString(buffer, 30));
//            
//            randomAccessFile.readFully(buffer, 0, 30);
//            if (getArtist() == null || getArtist().equals(""))
//            	setArtist(getString(buffer, 30));
//
//            randomAccessFile.readFully(buffer, 0, 30);
//            if (getAlbum() == null || getAlbum().equals(""))
//            	setAlbum(getString(buffer, 30));
//            
//            randomAccessFile.readFully(buffer, 0, 4);
//            if (getYear() == null || getYear().equals(""))
//            	setYear(getString(buffer, 4));
//            
//            randomAccessFile.readFully(buffer, 0, 30);
//            int commentLength;
//            if (getTrack()==0 || getTrack()==-1){
//            	if(buffer[28] == 0) {
//            		setTrack((short)ByteOrder.ubyte2int(buffer[29]));
//            		commentLength = 28;
//            	} else {
//            		setTrack((short)0);
//            		commentLength = 3;
//            	}
//            	if (getComment()==null || getComment().equals(""))
//            		setComment(getString(buffer, commentLength));
//            }
//            // Genre
//            randomAccessFile.readFully(buffer, 0, 1);
//            if (getGenre() ==null || getGenre().equals(""))
//            	setGenre(
//            			MP3MetaData.getGenreString((short)ByteOrder.ubyte2int(buffer[0])));
//        } catch(IOException ignored) {
//        } finally {
//            if( randomAccessFile != null )
//                try {
//                    randomAccessFile.close();
//                } catch(IOException ignored) {}
//        }
        
    }
    
    /**
     * Helper method to generate a string from an id3v1 filled buffer.
     */
//    private String getString(byte[] buffer, int length) {
//        try {
//            return new String(buffer, 0, getTrimmedLength(buffer, length), ISO_LATIN_1);
//        } catch (UnsupportedEncodingException err) {
//            // should never happen
//            return null;
//        }
//    }

    /**
     * Generates ID3Data from id3v2 data in the file.
     */
    private void parseID3v2Data(File file) {
//        ID3v2 id3v2Parser = null;
//        try {
//            id3v2Parser = new ID3v2(file);
//        } catch (ID3v2Exception idvx) { //can't go on
//            return ;
//        } catch (IOException iox) {
//            return ;
//        } catch(ArrayIndexOutOfBoundsException ignored) {
//            return ;
//        }
//        
//
//        Vector frames = null;
//        try {
//            frames = id3v2Parser.getFrames();
//        } catch (NoID3v2TagException ntx) {
//            return ;
//        }
//        
//        //rather than getting each frame indvidually, we can get all the frames
//        //and iterate, leaving the ones we are not concerned with
//        for(Iterator iter=frames.iterator() ; iter.hasNext() ; ) {
//            ID3v2Frame frame = (ID3v2Frame)iter.next();
//            String frameID = frame.getID();
//            
//            byte[] contentBytes = frame.getContent();
//            String frameContent = null;
//
//            if (contentBytes.length > 0) {
//                try {
//                    String enc = (frame.isISOLatin1()) ? ISO_LATIN_1 : UNICODE;
//                    frameContent = new String(contentBytes, enc).trim();
//                } catch (UnsupportedEncodingException err) {
//                    // should never happen
//                }
//            }
//
//            // need to check is PRIV field here since frameContent may be null in ISO_LATIN format
//            //  but not in UTF-8 format
//            if( (frameContent == null || frameContent.trim().equals("")) ) { 
//                // PRIV fields in LWS songs are encoded in UTF-8 format
//                if (MP3DataEditor.PRIV_ID.equals(frameID)) {
//                    isPRIVCheck(contentBytes);
//                }
//                continue;
//            }
//            //check which tag we are looking at
//            if(MP3DataEditor.TITLE_ID.equals(frameID)) 
//                setTitle(frameContent);
//            else if(MP3DataEditor.ARTIST_ID.equals(frameID)) 
//                setArtist(frameContent);
//            else if(MP3DataEditor.ALBUM_ID.equals(frameID)) 
//                setAlbum(frameContent);
//            else if(MP3DataEditor.YEAR_ID.equals(frameID)) {
//                setYear(frameContent);
//                checkLWS(frameContent );
//            }
//            else if(MP3DataEditor.COMMENT_ID.equals(frameID)) {
//                //ID3v2 comments field has null separators embedded to encode
//                //language etc, the real content begins after the last null
//                byte[] bytes = frame.getContent();
//                int startIndex = 0;
//                for(int i=bytes.length-1; i>= 0; i--) {
//                    if(bytes[i] != (byte)0)
//                        continue;
//                    //OK we are the the last 0
//                    startIndex = i;
//                    break;
//                }
//                frameContent = 
//                  new String(bytes, startIndex, bytes.length-startIndex).trim();
//                setComment(frameContent);
//                checkLWS(frameContent );
//            }
//            else if(MP3DataEditor.TRACK_ID.equals(frameID)) {
//                try {
//                    setTrack(Short.parseShort(frameContent));
//                } catch (NumberFormatException ignored) {} 
//            }
//            else if(MP3DataEditor.GENRE_ID.equals(frameID)) {
//                //ID3v2 frame for genre has the byte used in ID3v1 encoded
//                //within it -- we need to parse that out
//                int startIndex = frameContent.indexOf("(");
//                int endIndex = frameContent.indexOf(")");
//                int genreCode = -1;
//                
//                //Note: It's possible that the user entered her own genre in
//                //which case there could be spurious braces, the id3v2 braces
//                //enclose values between 0 - 127 
//                
//                // Custom genres are just plain text and default genres (known
//                // from id3v1) are referenced with values enclosed by braces and
//                // with optional refinements which I didn't implement here.
//                // http://www.id3.org/id3v2.3.0.html#TCON
//                if(startIndex > -1 &&
//                   endIndex > -1 &&
//                   startIndex < endIndex && 
//                   startIndex < frameContent.length()) { 
//                    //we have braces check if it's valid
//                    String genreByte = frameContent.substring(startIndex+1, endIndex);
//                    
//                    try {
//                        genreCode = Integer.parseInt(genreByte);
//                    } catch (NumberFormatException nfx) {
//                        genreCode = -1;
//                    }
//                }
//                
//                if(genreCode >= 0 && genreCode <= 127)
//                    setGenre(MP3MetaData.getGenreString((short)genreCode));
//                else 
//                    setGenre(frameContent);
//            }
//            else if (MP3DataEditor.LICENSE_ID.equals(frameID)) {
//                if( getLicenseType() == null || !getLicenseType().equals(MAGIC_KEY))
//                setLicense(frameContent);
//                checkLWS(frameContent );
//            }
//            else if( MP3DataEditor.PRIV_ID.equals(frameID)) { 
//                isPRIVCheck(contentBytes);
//            }
//            // another key we don't care about except for searching for 
//            //  protected content
//            else { 
//                checkLWS(frameContent );
//            }
//        }
    }
    
    /**
     * Checks the PRIV field for the magic String. This is always in UTF-8 encoding so use the 
     * content byte array instead. 
     */
    private void isPRIVCheck(byte[] contentBytes) {
        try {
            String content = new String(contentBytes,"UTF-8");
            checkLWS(content);
        } catch (UnsupportedEncodingException e) {
        }
    }

    /**
     * If the song is not a LWS already, do a substring search to see if
     * it is a LWS song
     * @param content - ID3 tag to scan for a substring
     */
    private void checkLWS(String content) { 
        if( getLicenseType() == null || !getLicenseType().equals(MAGIC_KEY))
            if( content.indexOf(MAGIC_KEY) != -1) { System.out.println("magic string  found");
                setLicenseType(MAGIC_KEY);
            }
    }
}
