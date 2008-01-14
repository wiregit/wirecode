package com.limegroup.gnutella.metadata;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;

import com.limegroup.gnutella.xml.LimeXMLUtils;


public class FlacDataEditor extends AudioDataEditor {

    @Override
    protected Tag createTag(AudioFile audioFile) {
        if( audioFile.getTag() == null )
            return new FlacTag(new VorbisCommentTag(), null);
        return audioFile.getTag();
    }

    @Override
    protected boolean isValidFileType(String fileName) {
        return LimeXMLUtils.isFLACFile(fileName);
    }
}
