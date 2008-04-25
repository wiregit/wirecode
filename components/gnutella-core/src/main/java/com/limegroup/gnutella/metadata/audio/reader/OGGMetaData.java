package com.limegroup.gnutella.metadata.audio.reader;

import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagFieldKey;

public class OGGMetaData extends AudioDataReader {

    public OGGMetaData(File f) throws IOException, IllegalArgumentException {
        super(f);
    }

    @Override
    protected void readTag(AudioFile audioFile, Tag tag) {
        if(tag != null ) {
            super.readTag(audioFile, tag);
            audioData.setLicense(tag.getFirst(TagFieldKey.COPYRIGHT));
        }
    }
}
