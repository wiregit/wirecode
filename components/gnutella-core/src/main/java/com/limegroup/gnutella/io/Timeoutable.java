package com.limegroup.gnutella.io;

/** Something that can be timed out. */
public interface Timeoutable {
    public void notifyTimeout(long now, long expired, long timeoutLength);
}
