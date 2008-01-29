package com.limegroup.gnutella.metadata.audio.reader;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.limewire.collection.NameValue;

import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.metadata.MetaReader;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 *  Handles the reading of most file
 */
public class AudioDataReader implements MetaReader {

    public static final String ISO_LATIN_1 = "8859_1";
    public static final String UNICODE = "Unicode";
    public static final String MAGIC_KEY = "NOT CLEARED";
    
    protected final AudioMetaData audioData;
    
    public AudioDataReader(File f) throws IOException{
        audioData = new AudioMetaData();
        parseFile(f);
    }
    
    protected void readHeader(AudioHeader header) {
        audioData.setVBR(header.isVariableBitRate());
        audioData.setSampleRate(header.getSampleRateAsNumber());
        audioData.setBitrate((int)header.getBitRateAsNumber());
        audioData.setLength(header.getTrackLength());
    }
    
    protected void readTag(AudioFile audioFile, Tag tag){
        audioData.setTitle(tag.getFirstTitle());
        audioData.setArtist(tag.getFirstArtist());
        audioData.setAlbum(tag.getFirstAlbum());
        audioData.setYear(tag.getFirstYear());
        audioData.setComment(tag.getFirstComment());
        audioData.setTrack(tag.getFirstTrack());
        audioData.setGenre(tag.getFirstGenre());
    }
    
    protected void parseFile(File file) throws IOException { 
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            readHeader(audioFile.getAudioHeader());          
            readTag(audioFile, audioFile.getTag());
        } catch (CannotReadException e) {
            throw new IOException(e.getMessage());
        } catch (TagException e) {
            throw new IOException(e.getMessage());
        } catch (ReadOnlyFileException e) {
            throw new IOException(e.getMessage());
        } catch (InvalidAudioFrameException e) {
            throw new IOException(e.getMessage());
        }
    }

    public MetaData getMetaData() {
        return audioData;
    }

    public String getSchemaURI() {
        return LimeXMLNames.AUDIO_SCHEMA;
    }

    public List<NameValue<String>> toNameValueList() {
        return audioData.toNameValueList();
    }
}
