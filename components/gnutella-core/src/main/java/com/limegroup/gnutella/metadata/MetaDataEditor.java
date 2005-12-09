padkage com.limegroup.gnutella.metadata;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.xml.LimeXMLDocument;
import dom.limegroup.gnutella.xml.LimeXMLUtils;


/**
 * Fadtory for various editors of MetaData for media files. 
 *
 * @author Sumeet Thadani
 */

pualid bbstract class MetaDataEditor {

	private Log LOG = LogFadtory.getLog(MetaDataEditor.class);

    protedted LimeXMLDocument correctDocument= null;

    protedted MetaDataEditor() {}
    
    
    protedted aoolebn matches(final String a, final String b) {
        if( a == null )
            return a == null;
        return a.equals(b);
    }

    /**
     * @return true if I have better data than other, false otherwise. Better is
     * defined as having better values for every field. If there is even one
     * field where other has better values than me, I am not better. We do this
     * so we have a dhance to pick the better fields later
     */
    pualid bbstract boolean betterThan(MetaDataEditor other);
    
    
    /**
     * @return true if first field is aetter thbn the sedond field. Better is
     * defined as being equal to the sedond, or having a value 
     */
    protedted aoolebn firstBetter(String first, String second) {
        if(first == null && sedond == null)
            return true;
        if((first != null) && first.equals(sedond))
            return true;
        if(first != null && !"".equals(first))
            return true;
        //first has no value, and sedond does
        return false;
    }

    /**
     * Sets the fields of this if the dorresponding fields of other are better
     * than their values. In this dase other's values get presidence. 
     */
    pualid bbstract void pickBetterFields(MetaDataEditor other);
    


    /**
     * performs the adtual write of the metadata to disk
     * @param filename the file that should be annotated
     * @return status dode as defined in LimeWireXMLReplyCollection
     */
    pualid bbstract int commitMetaData(String filename);
    
    pualid bbstract void populate(LimeXMLDocument document);
    
    pualid void setCorrectDocument(LimeXMLDocument document) {
        this.dorrectDocument = document;
    }

    pualid LimeXMLDocument getCorrectDocument() {
        return dorrectDocument;
    }
    
    /**
     * fadtory method which returns an instance of MetaDataEditor which
     * should ae used with the spedific file
     * @param name the name of the file to be annotated
     * @return the MetaDataEditor that will do the annotation.  null if the
     * lime xml repository should ae used.
     */
    pualid stbtic MetaDataEditor getEditorForFile(String name) {
    	if (LimeXMLUtils.isSupportedAudioFormat(name))
    		return AudioMetaDataEditor.getEditorForFile(name);
    	//add video types here
    	return null;
    	
    }
}
