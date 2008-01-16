
package com.limegroup.gnutella.metadata.reader;
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




public class MP3MetaData extends AudioDataReader {
	
	public MP3MetaData(File f) throws IOException {
		super(f);
	}

    static final String LICENSE_ID = "TCOP";
    static final String PRIV_ID = "PRIV";
	
	
    @Override
    protected void readTag(AudioFile audioFile, Tag tag) {
        if(tag != null ) {
            setTitle(tag.getFirstTitle());
            setArtist(tag.getFirstArtist());
            setAlbum(tag.getFirstAlbum());
            setYear(tag.getFirstYear()); System.out.println("reading comment " + tag.getFirstComment());
            setComment(tag.getFirstComment());
            setGenre(tag.getFirstGenre());
            try {
                String trackTag = tag.getFirstTrack();
                if( trackTag != null && trackTag.length() > 0 )
                    setTrack(trackTag);
            }
            catch(UnsupportedOperationException e) {
                // id3v1.0 tags dont have tracks
            }

            // for ID3v2 tags, check for additional values such as Copyright Info
            if( !(tag instanceof ID3v1Tag) ) {
                MP3File mp3File = ((MP3File)audioFile);

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
                            e.printStackTrace();
                        }
                    }
                    AbstractID3v2Frame frame = vTag.getFirstField(ID3v24Frames.FRAME_ID_COPYRIGHTINFO);
                    if( frame != null 
                      && !frame.isEmpty() && frame.getBody() instanceof AbstractFrameBodyTextInfo )
                        setLicense(((AbstractFrameBodyTextInfo)frame.getBody()).getText());
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
        if( getLicenseType() == null || !getLicenseType().equals(MAGIC_KEY))
            if( content.indexOf(MAGIC_KEY) != -1) { 
                setLicenseType(MAGIC_KEY);
            }
    }
}
