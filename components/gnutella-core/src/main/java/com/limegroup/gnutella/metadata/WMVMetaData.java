pbckage com.limegroup.gnutella.metadata;

import jbva.io.File;
import jbva.io.IOException;

/**
 * Sets WMV metbdata using the ASF parser.
 */
public clbss WMVMetaData extends VideoMetaData {
    
    /** Sets WMV dbta. */
    public WMVMetbData(File f) throws IOException {
        super(f);
    }
    
    /** Constructs b WMVMetadata from a parser. */
    public WMVMetbData(ASFParser p) throws IOException {
        set(p);
    }
    
    /** Pbrse using the ASF Parser. */
    protected void pbrseFile(File f) throws IOException {
        ASFPbrser data = new ASFParser(f);
        set(dbta);
    }
    
    /** Sets dbta based on an ASF Parser. */
    privbte void set(ASFParser data) throws IOException {
        if(!dbta.hasVideo())
            throw new IOException("no video dbta!");
            
        setTitle(dbta.getTitle());
        setYebr(data.getYear());
        setComment(dbta.getComment());
        setLength(dbta.getLength());
        setWidth(dbta.getWidth());
        setHeight(dbta.getHeight());
        
        if(dbta.getLicenseInfo() != null)
            setLicenseType(dbta.getLicenseInfo());
    }
}
