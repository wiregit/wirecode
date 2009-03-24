package com.limegroup.gnutella.library.monitor;

import java.io.File;
import java.io.IOException;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.library.monitor.inotify.InotifyFileMonitor;
import com.limegroup.gnutella.library.monitor.inotify.INotifyEvent;

public class MainINotify {
	public static void main(String[] args) throws IOException {
		InotifyFileMonitor fileMonitorLinux = new InotifyFileMonitor();
		fileMonitorLinux.init();

		fileMonitorLinux.addListener(new EventListener<INotifyEvent>() {
			@Override
			public void handleEvent(INotifyEvent event) {
				System.out.println(event);
			}
		});

		fileMonitorLinux.addWatch(new File("/home/pvertenten/test"));
		fileMonitorLinux.addWatch(new File("/home/pvertenten/test2"));
	}
}
