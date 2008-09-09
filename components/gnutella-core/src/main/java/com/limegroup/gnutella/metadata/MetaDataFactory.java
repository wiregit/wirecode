package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;

/**
 * Factory for returning readers and writers of metadata. 
 */
public interface MetaDataFactory {
    
    /**
     * Returns an editor for a file if one exists or null if LimeWire
     * does not support editing the file type meta data
     */
    public MetaWriter getEditorForFile(String name);
    
    /**
     * Reads the meta data from the file if the file type is supported
     * or return null if reading the file meta data if not supprted
     */
    public MetaData parse(File f) throws IOException;
    
    /**
     * Registers a reader factory for a number of file extensions
     * @param fileExtensions must not be null or empty.
     * 
     * @throws IllegalArgumentException if another factory is already registered
     * for one of the given extensions
     */
    void registerReader(MetaReader reader, String... fileExtensions);
}
