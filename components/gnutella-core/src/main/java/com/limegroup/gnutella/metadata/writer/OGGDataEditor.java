
package com.limegroup.gnutella.metadata.writer;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;

import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 *  Creates a valid OggVorbis comment tag for an undescribed
 *  OGG file is written to.
 */
public class OGGDataEditor extends AudioDataEditor {
    @Override
    protected Tag createTag(AudioFile audioFile) {
        if( audioFile.getTag() == null )
            return new VorbisCommentTag();
        return audioFile.getTag();
    }

    @Override
    protected boolean isValidFileType(String fileName) {
        return LimeXMLUtils.isOGGFile(fileName);
    }
}
	
	
