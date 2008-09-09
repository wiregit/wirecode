package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;

/**
 * Defines factory for {@link MetaReader} objects.
 */
public interface MetaReaderFactory {
    
    /**
     * Creates a new reader for parsing <code>file</code>.
     * @param file file to parse
     * @throws IOException if parsing of the file fails for some reason
     */
    MetaReader createReader(File file) throws IOException;
    
}
