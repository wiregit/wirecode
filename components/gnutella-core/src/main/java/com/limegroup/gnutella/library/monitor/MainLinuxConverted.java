package com.limegroup.gnutella.library.monitor;

import java.io.File;
import java.io.IOException;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.library.monitor.inotify.FileMonitorLinux;

public class MainLinuxConverted {
	public static void main(String[] args) throws IOException {
		FileMonitorLinux fileMonitorLinux = new FileMonitorLinux();
		fileMonitorLinux.init();

		fileMonitorLinux.addListener(new EventListener<FileMonitorEvent>() {
			@Override
			public void handleEvent(FileMonitorEvent event) {
				System.out.println(event);
			}
		});

		fileMonitorLinux.addWatch(new File("/home/pvertenten/test"));
		fileMonitorLinux.addWatch(new File("/home/pvertenten/test2"));
	}
}
