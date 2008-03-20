package com.limegroup.gnutella.metadata.video.reader;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.limewire.util.NameValue;

import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.metadata.MetaReader;
import com.limegroup.gnutella.metadata.audio.reader.ASFParser;
import com.limegroup.gnutella.metadata.video.VideoMetaData;



/**
 * Sets WMV metadata using the ASF parser.
 */
public class WMVMetaData implements MetaReader {
    
    private final VideoMetaData videoData;
    
    /** Sets WMV data. */
    public WMVMetaData(File f) throws IOException {
        videoData = new VideoMetaData();
        parseFile(f);
    }
    
    /** Constructs a WMVMetadata from a parser. */
    public WMVMetaData(ASFParser p) throws IOException {
        videoData = new VideoMetaData();
        set(p);
    }
    
    /** Parse using the ASF Parser. */
    protected void parseFile(File f) throws IOException {
        ASFParser data = new ASFParser(f);
        set(data);
    }
    
    /** Sets data based on an ASF Parser. */
    private void set(ASFParser data) throws IOException {
        if(!data.hasVideo())
            throw new IOException("no video data!");
            
        videoData.setTitle(data.getTitle());
        videoData.setYear(data.getYear());
        videoData.setComment(data.getComment());
        videoData.setLength(data.getLength());
        videoData.setWidth(data.getWidth());
        videoData.setHeight(data.getHeight());
        
        if(data.getLicenseInfo() != null)
            videoData.setLicenseType(data.getLicenseInfo());
    }

    public MetaData getMetaData() {
        return videoData;
    }

    public String getSchemaURI() {
        return videoData.getSchemaURI();
    }

    public List<NameValue<String>> toNameValueList() {
        return videoData.toNameValueList();
    }
}
