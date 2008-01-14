
package com.limegroup.gnutella.metadata;


import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.ID3v11Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v23Tag;

import com.limegroup.gnutella.xml.LimeXMLUtils;


/**
 * An editor specifically for mp3 files with id3 tags
 */
public class MP3DataEditor extends AudioDataEditor {

    @Override
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


    @Override
    protected boolean isValidFileType(String fileName) {
        return LimeXMLUtils.isMP3File(fileName);
    }
}
