package com.limegroup.gnutella.mp3;

import org.apache.commons.logging.*;


import com.sun.java.util.collections.*;
import com.limegroup.gnutella.xml.*;


/**
 * Used when a user wants to edit meta-information about a media file, and asks
 * to save it. 
 * 
 * For this class to work efficiently, the removeID3Tags method
 * is called before. rewriteID3Tags method is called. 
 *
 * @author Sumeet Thadani
 */

public abstract class MetaDataEditor {

	private Log LOG = LogFactory.getLog(MetaDataEditor.class);

    protected LimeXMLDocument correctDocument= null;

    protected MetaDataEditor() {}
    
    
    protected boolean matches(final String a, final String b) {
        if( a == null )
            return b == null;
        return a.equals(b);
    }

    /**
     * @return true if I have better data than other, false otherwise. Better is
     * defined as having better values for every field. If there is even one
     * field where other has better values than me, I am not better. We do this
     * so we have a chance to pick the better fields later
     */
    public abstract boolean betterThan(MetaDataEditor other);
    
    
    /**
     * @return true if first field is better than the second field. Better is
     * defined as being equal to the second, or having a value 
     */
    protected boolean firstBetter(String first, String second) {
        if(first == null && second == null)
            return true;
        if((first != null) && first.equals(second))
            return true;
        if(first != null && !"".equals(first))
            return true;
        //first has no value, and second does
        return false;
    }

    /**
     * Sets the fields of this if the corresponding fields of other are better
     * than their values. In this case other's values get presidence. 
     */
    public abstract void pickBetterFields(MetaDataEditor other);
    


    /**
     * performs the actual write of the metadata to disk
     * @param filename the file that should be annotated
     * @return status code as defined in LimeWireXMLReplyCollection
     */
    public abstract int commitMetaData(String filename);
    
    public abstract void populateFromString(String xmlString);
    
    public void setCorrectDocument(LimeXMLDocument document) {
        this.correctDocument = document;
    }

    public LimeXMLDocument getCorrectDocument() {
        return correctDocument;
    }
    
    /**
     * factory method which returns an instance of MetaDataEditor which
     * should be used with the specific file
     * @param name the name of the file to be annotated
     * @return the MetaDataEditor that will do the annotation.  null if the
     * lime xml repository should be used.
     */
    public static MetaDataEditor getEditorForFile(String name) {
    	if (!LimeXMLUtils.isSupportedFormat(name))
    		return null;
    	if (LimeXMLUtils.isSupportedAudioFormat(name))
    		return AudioMetaDataEditor.getEditorForFile(name);
    	//add video types here
    	return null;
    	
    }
}
