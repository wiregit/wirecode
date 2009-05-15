package org.limewire.libtorrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class NoOpReadWriteLock implements ReadWriteLock {

    @Override
    public Lock readLock() {
        return new NoOpLock();
    }

    @Override
    public Lock writeLock() {
        return new NoOpLock();
    }

}
