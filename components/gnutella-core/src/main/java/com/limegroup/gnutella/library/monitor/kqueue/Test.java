package com.limegroup.gnutella.library.monitor.kqueue;

import java.io.File;
import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException, InterruptedException {
        String path = "/Users/pvertenten/test1";

        FileMonitor fileMonitor = FileMonitor.createInstance();
        
        fileMonitor.addWatch(new File(path));
        
        Thread.sleep(100000000);
    }
}
