package org.limewire.core.impl.xmpp;

/**
 * Puts the current thread to sleep
 */
public interface ThreadSleeper {
    void sleep(int sleepTimeMillis) throws InterruptedException;
}
