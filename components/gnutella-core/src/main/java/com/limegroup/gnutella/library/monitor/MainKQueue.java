package com.limegroup.gnutella.library.monitor;

import java.io.File;
import java.io.IOException;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.library.monitor.kqueue.KQueueEvent;
import com.limegroup.gnutella.library.monitor.kqueue.NaiveKQueueFileMonitor;

public class MainKQueue {
    public static void main(String[] args) throws InterruptedException, IOException {
        NaiveKQueueFileMonitor kQueueFileMonitor = new NaiveKQueueFileMonitor();
        kQueueFileMonitor.addListener(new EventListener<KQueueEvent>() {
            public void handleEvent(KQueueEvent event) {
                System.out.println(event);
            };
        });
        
        kQueueFileMonitor.addWatch(new File("/Users/pvertenten/test1"));
        kQueueFileMonitor.addWatch(new File("/Users/pvertenten/test2"));

        Thread.sleep(1000000000);
    }
}
