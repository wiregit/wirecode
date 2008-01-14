package com.limegroup.gnutella.metadata;

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


public class AudioMetaDataReader extends AudioMetaData {

    public AudioMetaDataReader(File file) throws IOException {
        super(file);
    }
    
    @Override
    protected void parseFile(File file) throws IOException {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader header = audioFile.getAudioHeader();
            System.out.println(header.getBitRate() + " " + header.getBitRateAsNumber() + " " + header.getFormat() + " " + header.getEncodingType());
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
                if( tag.getFirstTrack() != null && tag.getFirstTrack().length() > 0)
                      setTrack(Short.parseShort(tag.getFirstTrack()));
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
}
