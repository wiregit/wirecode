package com.limegroup.gnutella.library.monitor;

import org.limewire.inject.AbstractModule;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.library.monitor.inotify.FileMonitorLinux;
import com.limegroup.gnutella.library.monitor.win32.FileMonitorWindows;

public class LimeWireLibraryMonitorModule extends AbstractModule {
    @Override
    protected void configure() {
        if (OSUtils.isLinux()) {
            bind(FileMonitor.class).to(FileMonitorLinux.class);
        } else if (OSUtils.isWindows()) {
            bind(FileMonitor.class).to(FileMonitorWindows.class);
        } else {
            // TODO update with mac support.
            throw new UnsupportedOperationException("Bad os, ot currently supported");
        }
    }
}
