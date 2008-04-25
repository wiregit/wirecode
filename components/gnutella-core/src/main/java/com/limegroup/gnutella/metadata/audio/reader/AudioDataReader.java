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
import org.limewire.util.NameValue;

import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.metadata.MetaReader;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 *  Handles the reading of most audio files. All file types supported by 
 *  jAudioTagger can use this class to read their meta data
 */
public class AudioDataReader implements MetaReader {

    public static final String ISO_LATIN_1 = "8859_1";
    public static final String UNICODE = "Unicode";
    public static final String MAGIC_KEY = "NOT CLEARED";
    
    protected final AudioMetaData audioData;
    
    public AudioDataReader(File f) throws IOException, IllegalArgumentException {
        audioData = new AudioMetaData();
        parseFile(f);
    }
    
    /**
     * Reads header information about the file. All audio formats contain
     * some sort of header information to describe how the audio file is encoded.
     * This typically includes sample rate, bit rate, length, encoding scheme, etc..
     */
    private void readHeader(AudioHeader header) {
        audioData.setVBR(header.isVariableBitRate());
        audioData.setSampleRate(header.getSampleRateAsNumber());
        audioData.setBitrate((int)header.getBitRateAsNumber());
        audioData.setLength(header.getTrackLength());
    }
    
    /**
     * Reads any metadata the user may have added to this audio format. Each audio
     * type has its own format for describing the audio file. 
     */
    protected void readTag(AudioFile audioFile, Tag tag){
        audioData.setTitle(tag.getFirstTitle());
        audioData.setArtist(tag.getFirstArtist());
        audioData.setAlbum(tag.getFirstAlbum());
        audioData.setYear(tag.getFirstYear());
        audioData.setComment(tag.getFirstComment());
        audioData.setTrack(tag.getFirstTrack());
        audioData.setGenre(tag.getFirstGenre());
    }
    
    /**
     * Handles the reading and parsing of this file
     * @param file - file to read
     * @throws IOException - thrown if the file can't be read, is corrupted, etc..
     */
    private void parseFile(File file) throws IOException, IllegalArgumentException { 
        try { 
            AudioFile audioFile = AudioFileIO.read(file);
            readHeader(audioFile.getAudioHeader());          
            readTag(audioFile, audioFile.getTag());
        } catch (CannotReadException e) { 
            throw (IOException)new IOException().initCause(e);
        } catch (TagException e) {
            throw (IOException)new IOException().initCause(e);
        } catch (ReadOnlyFileException e) {
            throw (IOException)new IOException().initCause(e);
        } catch (InvalidAudioFrameException e) {
            throw (IOException)new IOException().initCause(e);
        }
    }

    /**
     * @return the MetaData of this file
     */
    public MetaData getMetaData() {
        return audioData;
    }

    /**
     * @return the XML schema of this file
     */
    public String getSchemaURI() {
        return LimeXMLNames.AUDIO_SCHEMA;
    }

    /**
     * @return the MetaData of this file in a NameValue 
     * List representation
     */
    public List<NameValue<String>> toNameValueList() {
        return audioData.toNameValueList();
    }
}
