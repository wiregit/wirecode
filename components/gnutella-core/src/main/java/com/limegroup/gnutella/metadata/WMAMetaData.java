package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;

/**
 * Sets WMA metadata using the ASF parser.
 */
public class WMAMetaData extends AudioMetaData {
    
    //private static final Log LOG = LogFactory.getLog(WMAMetaData.class);
    
    
    /** Sets WMA data. */
    public WMAMetaData(File f) throws IOException {
        super(f);
    }
    
    /** Constructs a WMAMetadata from a parser. */
    public WMAMetaData(ASFParser p) throws IOException {
        set(p);
    }
    
    /** Parse using the ASF Parser. */
    protected void parseFile(File f) throws IOException {
        ASFParser data = new ASFParser(f);
        set(data);
    }
    
    /** Sets data based on an ASF Parser. */
    private void set(ASFParser data) throws IOException {
        if(data.hasVideo())
            throw new IOException("use WMV instead!");
        if(!data.hasAudio())
            throw new IOException("no audio data!");
            
        setTitle(data.getTitle());
        setAlbum(data.getAlbum());
        setArtist(data.getArtist());
        setYear(data.getYear());
        setComment(data.getComment());
        setTrack(data.getTrack());
        setBitrate(data.getBitrate());
        setLength(data.getLength());
        setGenre(data.getGenre());
        setLicense(data.getCopyright());
        
        if(data.getLicenseInfo() != null)
            setLicenseType(data.getLicenseInfo());
    }
}
