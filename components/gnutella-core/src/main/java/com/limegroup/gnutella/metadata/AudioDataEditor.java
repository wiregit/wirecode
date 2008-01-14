package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.xml.LimeXMLReplyCollection;

public abstract class AudioDataEditor extends AudioMetaDataEditor {

    protected Tag updateTag(Tag tag) throws FieldDataInvalidException { 
        if(album_ != null)
            tag.setAlbum(album_);
        if(artist_ != null)
            tag.setArtist(artist_);
        if(comment_ != null)
            tag.setComment(comment_);
        if(genre_ != null)
            tag.setGenre(genre_);
        if(title_ != null)
            tag.setTitle(title_);
        if(year_ != null )
            tag.setYear(year_);
        if(track_ != null)
            tag.setTrack(track_);
        return tag;
    }
    
    protected abstract boolean isValidFileType(String fileName); 
    
    protected abstract Tag createTag(AudioFile audioFile);
    
    @Override
    public int commitMetaData(String fileName) {
        if(!isValidFileType(fileName))
            return LimeXMLReplyCollection.INCORRECT_FILETYPE;
        
        File f = new File(fileName);
        FileUtils.setWriteable(f);

        AudioFile audioFile;
        
        Tag audioTag;
        
        try {
            audioFile = AudioFileIO.read(f);
            audioTag = createTag(audioFile);
            audioTag = updateTag(audioTag);
            audioFile.setTag(audioTag);
            audioFile.commit();
        } catch (CannotReadException e) {
            e.printStackTrace();
            return LimeXMLReplyCollection.RW_ERROR;
        } catch (IOException e) {
            e.printStackTrace();
            return LimeXMLReplyCollection.RW_ERROR;
        } catch (TagException e) {
            e.printStackTrace();
            return LimeXMLReplyCollection.FAILED_ALBUM;
        } catch (ReadOnlyFileException e) {
            e.printStackTrace();
            return LimeXMLReplyCollection.RW_ERROR;
        } catch (InvalidAudioFrameException e) {
            e.printStackTrace();
            return LimeXMLReplyCollection.FAILED_ALBUM;
        } catch (CannotWriteException e) {
            e.printStackTrace();
            return LimeXMLReplyCollection.RW_ERROR;
        }

        
        return LimeXMLReplyCollection.NORMAL;
    }

}
