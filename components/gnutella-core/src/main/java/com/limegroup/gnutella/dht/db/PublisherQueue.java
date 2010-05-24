package com.limegroup.gnutella.dht.db;

import java.io.Closeable;

import org.limewire.mojito2.KUID;
import org.limewire.mojito2.storage.DHTValue;

/**
 * 
 */
public interface PublisherQueue extends Closeable {

    /**
     * Returns {@code true} if the {@link PublisherQueue} is running
     */
    public boolean isRunning();

    /**
     * Starts the {@link PublisherQueue}
     */
    public void start();

    /**
     * Stops the {@link PublisherQueue}
     */
    public void stop();

    /**
     * Clears the {@link PublisherQueue} and cancels all active 
     * processes if necessary.
     */
    public void clear(boolean cancel);

    /**
     * Schedules the given {@link KUID} {@link DHTValue} pair 
     * for publication.
     */
    public boolean put(KUID key, DHTValue value);

    /**
     * Returns the {@link PublisherQueue} size.
     */
    public int size();

    /**
     * Returns {@code true} if the {@link PublisherQueue} is empty.
     */
    public boolean isEmpty();
}