package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;

/**
 * Sets WMV metadata using the ASF parser.
 */
public class WMVMetaData extends VideoMetaData {
    
    /** Sets WMV data. */
    public WMVMetaData(File f) throws IOException {
        super(f);
    }
    
    /** Constructs a WMVMetadata from a parser. */
    public WMVMetaData(ASFParser p) throws IOException {
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
            
        setTitle(data.getTitle());
        setYear(data.getYear());
        setComment(data.getComment());
        setLength(data.getLength());
        setWidth(data.getWidth());
        setHeight(data.getHeight());
        
        if(data.getLicenseInfo() != null)
            setLicenseType(data.getLicenseInfo());
    }
}
