package com.limegroup.gnutella.metadata;

import com.limegroup.gnutella.xml.LimeXMLDocument;

public class MetaDataWriter {

    protected LimeXMLDocument correctDocument= null;
    
    /**
     * performs the actual write of the metadata to disk
     * @param filename the file that should be annotated
     * @return status code as defined in LimeWireXMLReplyCollection
     */
    public int commitMetaData(String filename){
        return -1;
    }
    
    public void populate(LimeXMLDocument document){
        
    }
    
    public void setCorrectDocument(LimeXMLDocument document) {
        this.correctDocument = document;
    }

    public LimeXMLDocument getCorrectDocument() {
        return correctDocument;
    }
    
}
