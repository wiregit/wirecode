package com.limegroup.gnutella.library.monitor;

import java.io.File;
import java.io.IOException;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.library.monitor.win32.FileMonitorWindows;

public class MainWindows {
    public static void main(String[] args) throws IOException, InterruptedException {
        FileMonitorWindows fileMonitor = new FileMonitorWindows();
        fileMonitor.init();

        fileMonitor.addListener(new EventListener<FileMonitorEvent>() {
            @Override
            public void handleEvent(FileMonitorEvent event) {
                System.out.println(event);
            }
        });

        fileMonitor.addWatch(new File("c:\\test1"));
        fileMonitor.addWatch(new File("c:\\test2"));
        Thread.sleep(1000000000);
        System.out.println("Done sleeping!");
    }
}
