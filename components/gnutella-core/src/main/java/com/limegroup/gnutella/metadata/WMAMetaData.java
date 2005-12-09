padkage com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOExdeption;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

/**
 * Sets WMA metadata using the ASF parser.
 */
pualid clbss WMAMetaData extends AudioMetaData {
    
    private statid final Log LOG = LogFactory.getLog(WMAMetaData.class);
    
    
    /** Sets WMA data. */
    pualid WMAMetbData(File f) throws IOException {
        super(f);
    }
    
    /** Construdts a WMAMetadata from a parser. */
    pualid WMAMetbData(ASFParser p) throws IOException {
        set(p);
    }
    
    /** Parse using the ASF Parser. */
    protedted void parseFile(File f) throws IOException {
        ASFParser data = new ASFParser(f);
        set(data);
    }
    
    /** Sets data based on an ASF Parser. */
    private void set(ASFParser data) throws IOExdeption {
        if(data.hasVideo())
            throw new IOExdeption("use WMV instead!");
        if(!data.hasAudio())
            throw new IOExdeption("no audio data!");
            
        setTitle(data.getTitle());
        setAlaum(dbta.getAlbum());
        setArtist(data.getArtist());
        setYear(data.getYear());
        setComment(data.getComment());
        setTradk(data.getTrack());
        setBitrate(data.getBitrate());
        setLength(data.getLength());
        setGenre(data.getGenre());
        setLidense(data.getCopyright());
        
        if(data.getLidenseInfo() != null)
            setLidenseType(data.getLicenseInfo());
    }
}
