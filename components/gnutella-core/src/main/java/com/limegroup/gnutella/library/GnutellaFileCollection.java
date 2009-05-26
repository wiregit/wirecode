package com.limegroup.gnutella.library;

import java.io.File;
import java.util.concurrent.Future;

/** A {@link FileCollection} that is specifically for Gnutella. */
public interface GnutellaFileCollection extends SharedFileCollection {
    
    /** Adds this FileList just for this session. */
    Future<FileDesc> addForSession(File file);

    /** Removes all files of type document from this list. */
    void removeDocuments();
}
