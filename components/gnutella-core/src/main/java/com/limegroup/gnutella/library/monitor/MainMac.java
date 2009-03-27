package com.limegroup.gnutella.library.monitor;

import java.io.File;
import java.io.IOException;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

import com.limegroup.gnutella.library.monitor.kqueue.FileWatcher;
import com.limegroup.gnutella.library.monitor.kqueue.KQueueEvent;
import com.limegroup.gnutella.library.monitor.kqueue.KQueueEventMask;

public class MainMac {
    public static void main(String[] args) throws InterruptedException, IOException {
        EventListenerList<KQueueEvent> listeners = new EventListenerList<KQueueEvent>();
        listeners.addListener(new EventListener<KQueueEvent>() {
            @Override
            public void handleEvent(KQueueEvent event) {
                System.out.println(event);
            }
        });
        FileWatcher fileWatcher = new FileWatcher(listeners, new File("/Users/pvertenten/test1"),
                KQueueEventMask.NOTE_DELETE.getMask());
        
        fileWatcher.init();
        
        fileWatcher.start();

        Thread.sleep(1000000000);
    }
}
