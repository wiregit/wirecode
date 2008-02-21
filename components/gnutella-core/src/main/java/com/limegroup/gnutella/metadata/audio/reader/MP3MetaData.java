
package com.limegroup.gnutella.metadata.audio.reader;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.framebody.AbstractFrameBodyTextInfo;

/**
 *  Reads MetaData from MP3 files. This extends AudioDataReader which also
 *  handles this format. However, store files need to get checked and parsed
 *  correctly so we do that here. 
 */
public class MP3MetaData extends AudioDataReader {
	
	static final String LICENSE_ID = "TCOP";
    static final String PRIV_ID = "PRIV";
	
	public MP3MetaData(File f) throws IOException, IllegalArgumentException {
		super(f);
	}
	
    @Override
    protected void readTag(AudioFile audioFile, Tag tag) {
        if(tag != null ) {
            audioData.setTitle(tag.getFirstTitle());
            audioData.setArtist(tag.getFirstArtist());
            audioData.setAlbum(tag.getFirstAlbum());
            audioData.setYear(tag.getFirstYear()); 
            audioData.setComment(tag.getFirstComment());
            audioData.setGenre(tag.getFirstGenre()); 
            try {
                audioData.setTrack(tag.getFirstTrack());
            }
            catch(UnsupportedOperationException e) {
                // id3v1.0 tags dont have tracks
            }

            // for ID3v2 tags, check for additional values such as Copyright Info
            if( !(tag instanceof ID3v1Tag) ) {
                // read the license if its a v2 tag
                audioData.setLicense(tag.getFirst(ID3v24Frames.FRAME_ID_COPYRIGHTINFO));
                
                MP3File mp3File = ((MP3File)audioFile);
                audioData.setGenre(parseGenre(tag.getFirstGenre()));
                AbstractID3v2Tag vTag = mp3File.getID3v2Tag();
                if( vTag != null ) {
                    List<TagField> license = vTag.get(PRIV_ID);
                    List<TagField> priv = vTag.get(PRIV_ID); 
                    Iterator<TagField> iter = license.iterator();
                    while(iter.hasNext()) {
                        TagField t = iter.next();
                        checkLWS(t.toString());
                    }
                    iter = priv.iterator();
                    while(iter.hasNext()) {
                        TagField t = iter.next(); 
                        checkLWS(t.toString());
                        try {
                            isPRIVCheck(t.getRawContent());
                        } catch (UnsupportedEncodingException e) {
                            // don't catch
                        }
                    }
                    AbstractID3v2Frame frame = vTag.getFirstField(ID3v24Frames.FRAME_ID_COPYRIGHTINFO);
                    if( frame != null 
                      && !frame.isEmpty() && frame.getBody() instanceof AbstractFrameBodyTextInfo )
                        audioData.setLicense(((AbstractFrameBodyTextInfo)frame.getBody()).getText());
                }
            }
        }
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
        if( audioData.getLicenseType() == null || !audioData.getLicenseType().equals(MAGIC_KEY))
            if( content.indexOf(MAGIC_KEY) != -1) { 
                audioData.setLicenseType(MAGIC_KEY);
            }
    }
    
    /**
     * Some genres in ID3v2 tags are displaying (XXX) numbers along side the genre.
     * If this exists it hides the number from the user
     * @param genre
     * @return
     */
    private String parseGenre(String genre){
        if( genre == null || genre.length() <= 0) 
            return genre;
        String cleanGenre = genre;
        if( genre.charAt(0) == '(') {
            int startIndex = 0;
            for(int i = 0; i < genre.length(); i++) {
                if( genre.charAt(i) == ')') {
                    startIndex = i + 1;
                }
            }            
            cleanGenre = genre.substring(startIndex);
        }
        return cleanGenre;
        
    }
}
