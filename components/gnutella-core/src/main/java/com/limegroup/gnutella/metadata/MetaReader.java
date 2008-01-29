package com.limegroup.gnutella.metadata;

import java.util.List;

import org.limewire.collection.NameValue;

/**
 *  Reads meta-data from a file
 */
public interface MetaReader {
    
    public List<NameValue<String>> toNameValueList();
    
    public String getSchemaURI();
    
    public MetaData getMetaData();
}
