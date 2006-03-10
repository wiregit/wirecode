package com.limegroup.gnutella.io;

public interface Future extends Runnable {
    public void run();
    public Object getResult();
}
