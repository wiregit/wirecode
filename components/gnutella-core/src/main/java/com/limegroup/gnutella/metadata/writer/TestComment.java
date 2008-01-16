package com.limegroup.gnutella.metadata.writer;

import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v11Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v23Tag;

public class TestComment {
    
    
    protected Tag createTag(AudioFile audioFile) {
        if( audioFile.getTag() == null )
            return new ID3v23Tag();
        
        MP3File mp3File = (MP3File)audioFile;
        // if v2 tag is available, use that one
        if(mp3File.hasID3v2Tag()) { 
            return mp3File.getID3v2Tag();
        } else if( mp3File.hasID3v1Tag()) { 
            ID3v1Tag tag = mp3File.getID3v1Tag();
            if( tag instanceof ID3v11Tag ) { 
                return tag;
            } else {
                // v1.0 tags don't support track or genres. Being that its used so rarely, just update
                // the tag to v1.1b to not break our implementation
                return new ID3v11Tag(tag);
            }
        } else { // this should never happen but just in case
            return new ID3v23Tag();
        }
    } 
    
    public void writeComment(String filename, String comment, String title) {
        File f = new File(filename);
        // i use custom dll code here to make it writable so for now just 
        //  return if the file can't be written
        if( !f.canWrite() ) {
            System.out.println("File can't be written");
            System.exit(0);
        }
        
        AudioFile audioFile;
        
        Tag audioTag;

            try {
                audioFile = AudioFileIO.read(f);
                audioTag = createTag(audioFile);
                audioTag.setComment(comment);
                audioTag.setTitle(title);
                audioFile.setTag(audioTag);
                System.out.println(((MP3File)audioFile).displayStructureAsPlainText());
                audioTag = createTag(audioFile);
                audioFile.commit();
                System.out.println("wrote data");
                
                audioFile = AudioFileIO.read(f);
                System.out.println(((MP3File)audioFile).displayStructureAsPlainText());
                audioTag = createTag(audioFile);
                System.out.println(audioTag.toString());
                System.out.println(audioTag.getComment().size());
            } catch (CannotReadException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
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
            } catch (CannotWriteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

    }
    
    public static void main(String args[] ) {
        String comment = "C:\\Documents and Settings\\meverett\\My Documents\\LimeWire\\LimeWire ID3 Test\\testV23.mp3";
        
        TestComment t = new TestComment();
        t.writeComment(comment, "new comment", "new title");
    }
}
