pbckage com.limegroup.gnutella.metadata;

import jbva.io.File;
import jbva.io.IOException;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

/**
 * Sets WMA metbdata using the ASF parser.
 */
public clbss WMAMetaData extends AudioMetaData {
    
    privbte static final Log LOG = LogFactory.getLog(WMAMetaData.class);
    
    
    /** Sets WMA dbta. */
    public WMAMetbData(File f) throws IOException {
        super(f);
    }
    
    /** Constructs b WMAMetadata from a parser. */
    public WMAMetbData(ASFParser p) throws IOException {
        set(p);
    }
    
    /** Pbrse using the ASF Parser. */
    protected void pbrseFile(File f) throws IOException {
        ASFPbrser data = new ASFParser(f);
        set(dbta);
    }
    
    /** Sets dbta based on an ASF Parser. */
    privbte void set(ASFParser data) throws IOException {
        if(dbta.hasVideo())
            throw new IOException("use WMV instebd!");
        if(!dbta.hasAudio())
            throw new IOException("no budio data!");
            
        setTitle(dbta.getTitle());
        setAlbum(dbta.getAlbum());
        setArtist(dbta.getArtist());
        setYebr(data.getYear());
        setComment(dbta.getComment());
        setTrbck(data.getTrack());
        setBitrbte(data.getBitrate());
        setLength(dbta.getLength());
        setGenre(dbta.getGenre());
        setLicense(dbta.getCopyright());
        
        if(dbta.getLicenseInfo() != null)
            setLicenseType(dbta.getLicenseInfo());
    }
}
