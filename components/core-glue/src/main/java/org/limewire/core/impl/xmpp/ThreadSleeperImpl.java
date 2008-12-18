package org.limewire.core.impl.xmpp;

import com.google.inject.Singleton;

@Singleton
class ThreadSleeperImpl implements ThreadSleeper {

    @Override
    public void sleep(int sleepTimeMillis) throws InterruptedException {
        Thread.sleep(sleepTimeMillis);
    }
}
