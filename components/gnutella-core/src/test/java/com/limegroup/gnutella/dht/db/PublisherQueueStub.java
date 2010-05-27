package com.limegroup.gnutella.dht.db;

import java.io.IOException;

import org.limewire.mojito.KUID;
import org.limewire.mojito.storage.DHTValue;

public class PublisherQueueStub implements PublisherQueue {

    @Override
    public void clear(boolean cancel) {
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public boolean put(KUID key, DHTValue value) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void close() throws IOException {
    }
}
