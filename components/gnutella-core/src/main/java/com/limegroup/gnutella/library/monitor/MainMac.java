package com.limegroup.gnutella.library.monitor;

import java.io.File;
import java.io.IOException;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.library.monitor.fsevent.FileMonitorMac;

public class MainMac {
    public static void main(String[] args) throws IOException, InterruptedException {
        FileMonitorMac fileMonitorMac = new FileMonitorMac();
        fileMonitorMac.init();

        fileMonitorMac.addListener(new EventListener<FileMonitorEvent>() {
            @Override
            public void handleEvent(FileMonitorEvent event) {
                System.out.println(event);
            }
        });
        
        fileMonitorMac.addWatch(new File("/Users/pvertenten/test1"));
        fileMonitorMac.addWatch(new File("/Users/pvertenten/test2"));
        
        Thread.sleep(100000000);
    }
}
