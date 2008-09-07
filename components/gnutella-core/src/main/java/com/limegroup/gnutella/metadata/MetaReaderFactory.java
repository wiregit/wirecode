package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;

public interface MetaReaderFactory {
    
    MetaReader createReader(File file) throws IOException;
    
}
