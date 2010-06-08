package org.limewire.mojito.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An interface for writable Objects.
 */
public interface Writeable {

    /**
     * Writes bytes to the given {@link OutputStream} and returns the
     * number of bytes that were written.
     */
    public int write(OutputStream out) throws IOException;
}
