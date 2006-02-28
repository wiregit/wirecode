package com.limegroup.gnutella.io;

import java.io.IOException;

/**
 * A simple writer that does nothing.
 * This is used primarily to allow objects that always require a non-null writer
 * to clean up references to old writer while still maintaining a non-null writer.
 */
public class NoOpWriter implements WriteObserver {
    public boolean handleWrite() throws IOException { return false; }
    public void handleIOException(IOException iox) {}
    public void shutdown() {}

}
