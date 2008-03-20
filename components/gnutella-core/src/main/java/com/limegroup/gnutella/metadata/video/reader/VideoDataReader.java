package com.limegroup.gnutella.metadata.video.reader;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.limewire.util.NameValue;

import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.metadata.MetaReader;
import com.limegroup.gnutella.metadata.video.VideoMetaData;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 *
 */
public abstract class VideoDataReader implements MetaReader{

    protected final VideoMetaData videoData;
    
    public VideoDataReader(File file) throws IOException {
        videoData = new VideoMetaData();
        parseFile(file);
    }
    
    public MetaData getMetaData() {
        return videoData;
    }

    public String getSchemaURI() {
        return LimeXMLNames.VIDEO_SCHEMA;
    }

    public List<NameValue<String>> toNameValueList() {
        return videoData.toNameValueList();
    }
    
    protected abstract void parseFile(File file) throws IOException;

}
