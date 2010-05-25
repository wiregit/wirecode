package org.limewire.mojito2.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultThreadFactory implements ThreadFactory {
    
    private final AtomicInteger counter = new AtomicInteger();
    
    private final String name;
    
    public DefaultThreadFactory(String name) {
        this.name = name;
    }
    
    private String createName() {
        return name + "-" + counter.incrementAndGet();
    }
    
    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, createName());
    }
}
