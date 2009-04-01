package com.limegroup.gnutella.library.monitor;

import java.io.File;
import java.io.IOException;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.library.monitor.fsevent.FSEvent;
import com.limegroup.gnutella.library.monitor.fsevent.FSEventMonitor;

public class MainFSEvent {
    public static void main(String[] args) throws InterruptedException, IOException {

        FSEventMonitor fEventMonitor = new FSEventMonitor();
        fEventMonitor.init();

        fEventMonitor.addListener(new EventListener<FSEvent>() {
            public void handleEvent(FSEvent event) {
                System.out.println(event);
            };
        });

        fEventMonitor.addWatch(new File("/Users/pvertenten/test1"));
    }
}
