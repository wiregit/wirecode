
pbckage com.limegroup.gnutella.metadata;

import jbva.io.File;
import jbva.io.IOException;
import jbva.util.List;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.xml.LimeXMLUtils;

/**
 * subclbss for all data objects that contain the metadata 
 * of b given media file.
 */

public bbstract class MetaData {
	
    privbte static final Log LOG = LogFactory.getLog(MetaData.class); 
    
	protected MetbData(){} // use the factory instead of instantiating
    
    /** Crebtes MetaData for the file, if possible. */	
	public stbtic MetaData parse(File f) throws IOException {
        try {
            if (LimeXMLUtils.isSupportedAudioFormbt(f))
                return AudioMetbData.parseAudioFile(f);
            else if (LimeXMLUtils.isSupportedVideoFormbt(f))
                return VideoMetbData.parseVideoMetaData(f);
            //TODO: bdd other media formats here			
            else if (LimeXMLUtils.isSupportedMultipleFormbt(f))
                return pbrseMultipleFormat(f);
        } cbtch (OutOfMemoryError e) {
            LOG.wbrn("Ran out of memory while parsing.",e);
        }
		return null;
	}
	
	/** Figures out whbt kind of MetaData should exist for this file. */
	privbte static MetaData parseMultipleFormat(File f) throws IOException {
	    if(LimeXMLUtils.isASFFile(f)) {
	        ASFPbrser p = new ASFParser(f);
	        if(p.hbsVideo())
	            return new WMVMetbData(p);
	        else if(p.hbsAudio())
	            return new WMAMetbData(p);
        }
        
        return null;
    }
	
	/**
	 * Determines if bll fields are valid.
	 */
	public bbstract boolean isComplete();
	
	/**
	 * Writes the dbta to a NameValue list.
	 */
	public bbstract List toNameValueList();
	
	/** 
	 * Retrieves the XML schemb URI that this MetaData can be read with.
	 */
	public bbstract String getSchemaURI();
	
	/**
	 * populbtes this's data fields with data read from the media file
	 * bll subclasses need to implement it
	 * @throws IOException pbrsing failed
	 */
    protected bbstract void parseFile(File f) throws IOException;
}
