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

        fileMonitor.addWatch(new File("c:\\test1"), true);
        fileMonitor.addWatch(new File("c:\\test2"), true);
    }
}
