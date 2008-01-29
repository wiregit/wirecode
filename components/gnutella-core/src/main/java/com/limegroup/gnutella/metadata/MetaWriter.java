package com.limegroup.gnutella.metadata;

import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Writes metadata to disk
 */
public interface MetaWriter {

    public int commitMetaData(String filename);
    
    public void populate(LimeXMLDocument doc);
    
    public MetaData getMetaData();
}
