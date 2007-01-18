package org.limewire.nio;

public interface Future extends Runnable {
    public void run();
    public Object getResult();
}
