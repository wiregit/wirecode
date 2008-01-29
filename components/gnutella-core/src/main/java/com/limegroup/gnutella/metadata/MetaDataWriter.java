package com.limegroup.gnutella.metadata;

import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 *  A utility class that writes modified LimeXMLDocuments as Meta-data to 
 *  and audio or video file
 */
public class MetaDataWriter {

    private final String fileName;
    
    private final MetaWriter editor;
    
    protected LimeXMLDocument correctDocument= null;
    
    public MetaDataWriter(String fileName) {
        this.fileName = fileName;
        editor = MetaDataFactory.getEditorForFile(fileName);
    }
    
    public boolean needsToUpdate(LimeXMLDocument doc) {
        if( correctDocument == null)
            return false;
        else if ( doc == null )
            return true;
        return doc.equals(correctDocument);
    }
    
    /**
     * performs the actual write of the metadata to disk
     * @param filename the file that should be annotated
     * @return status code as defined in LimeWireXMLReplyCollection
     */
    public int commitMetaData(){
        return editor.commitMetaData(fileName);
    }
    
    /**
     * Populates the editor with the values from xmldocument
     */
    public void populate(LimeXMLDocument doc) {
        if( editor == null )
            throw new NullPointerException("Editor not created");
        correctDocument = doc;
        editor.populate(doc);
    }

    public LimeXMLDocument getCorrectDocument() {
        return correctDocument;
    }
    
    public MetaWriter getEditor(){
        return editor;
    }
}
