package org.limewire.util;

import java.io.File;

/**
 * 
 * Defines an interface class method whether or not the lock on a file was released.
 * 
 */
public interface FileLocker {
    
    /** Returns true if the lock was released on the file. */
    public boolean releaseLock(File file);
}
