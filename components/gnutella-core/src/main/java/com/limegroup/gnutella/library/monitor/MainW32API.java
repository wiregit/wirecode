package com.limegroup.gnutella.library.monitor;

import java.io.File;
import java.io.IOException;

import com.limegroup.gnutella.library.monitor.win32.FileMonitorWindows;
import com.limegroup.gnutella.library.monitor.win32.FileMonitorWindows.FileEvent;
import com.limegroup.gnutella.library.monitor.win32.FileMonitorWindows.FileListener;

public class MainW32API {
    public static void main(String[] args) throws IOException, InterruptedException {
        FileMonitorWindows fileMonitor = FileMonitorWindows.getInstance();
        fileMonitor.addWatch(new File("c://test1"));
        fileMonitor.addWatch(new File("c://test2"));

        fileMonitor.addFileListener(new FileListener() {
            @Override
            public void fileChanged(FileEvent e) {
                System.out.println(e.getType() + " - " + e.getFile());
            }
        });
        
        Thread.sleep(1000000000);
        System.out.println("Done sleeping!");
    }
}
