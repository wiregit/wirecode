package org.limewire.util;

import java.io.File;

/**
 * A FileLocker is something that can hold locks on a file,
 * preventing other places from deleting or renaming it.
 * A FileLocker must expose the ability to remove the lock
 * on the file.
 */
public interface FileLocker {
    
    /** Returns true if the lock was released on the file. */
    public boolean releaseLock(File file);
}
