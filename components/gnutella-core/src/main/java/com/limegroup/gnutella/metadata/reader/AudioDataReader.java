package com.limegroup.gnutella.metadata.reader;

import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;


public class AudioDataReader extends AudioMetaData {

    AudioDataReader(File f) throws IOException{
        super(f);
    }
    
    protected void readHeader(AudioHeader header) {
        setVBR(header.isVariableBitRate());
        setSampleRate(header.getSampleRateAsNumber());
        setBitrate((int)header.getBitRateAsNumber());
        setLength(header.getTrackLength());
    }
    
    protected void readTag(AudioFile audioFile, Tag tag){
        setTitle(tag.getFirstTitle());
        setArtist(tag.getFirstArtist());
        setAlbum(tag.getFirstAlbum());
        setYear(tag.getFirstYear());
        setComment(tag.getFirstComment());
        if( tag.getFirstTrack() != null && tag.getFirstTrack().length() > 0)
            setTrack(tag.getFirstTrack());
        setGenre(tag.getFirstGenre());
    }
    
    @Override
    protected void parseFile(File file) throws IOException { 
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            readHeader(audioFile.getAudioHeader());          
            readTag(audioFile, audioFile.getTag());
        } catch (CannotReadException e) {
            e.printStackTrace();
        } catch (TagException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ReadOnlyFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidAudioFrameException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
