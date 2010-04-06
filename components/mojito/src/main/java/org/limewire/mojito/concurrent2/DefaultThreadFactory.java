package org.limewire.mojito.concurrent2;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 */
public class DefaultThreadFactory implements ThreadFactory {

    private final AtomicInteger instance = new AtomicInteger();
    
    private final String name;
    
    public DefaultThreadFactory(String name) {
        this.name = name;
    }
    
    private String createName() {
        return name + "-" + instance.getAndIncrement();
    }
    
    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, createName());
    }
}
