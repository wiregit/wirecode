package com.limegroup.gnutella.metadata;

import java.util.List;

import org.limewire.util.NameValue;

/**
 *  Reads meta-data from a file. Each type of media type reader
 *  must implement this interface
 */
public interface MetaReader {
    
    public List<NameValue<String>> toNameValueList();
    
    public String getSchemaURI();
    
    public MetaData getMetaData();
}
