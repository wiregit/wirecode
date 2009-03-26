package com.limegroup.gnutella.library.monitor;

import java.io.File;
import java.io.IOException;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.library.monitor.win32.Win32FileMonitor;
import com.limegroup.gnutella.library.monitor.win32.W32NotifyActionEvent;

public class MainW32API {
    public static void main(String[] args) throws IOException, InterruptedException {
        Win32FileMonitor fileMonitor = new Win32FileMonitor();
        fileMonitor.init();
        
        fileMonitor.addListener(new EventListener<W32NotifyActionEvent>() {
            @Override
            public void handleEvent(W32NotifyActionEvent event) {
                System.out.println(event);
            }
        });
        
        fileMonitor.addWatch(new File("c://test1"), true);
        fileMonitor.addWatch(new File("c://test2"), true);
    }
}
