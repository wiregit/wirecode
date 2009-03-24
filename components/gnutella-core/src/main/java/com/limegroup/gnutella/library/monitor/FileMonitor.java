package com.limegroup.gnutella.library.monitor;

import java.io.File;
import java.io.IOException;

import org.limewire.listener.EventListener;

public interface FileMonitor {

    public void init() throws IOException;

    public void addWatch(File file) throws IOException;

    public void removeWatch(File file) throws IOException;

    public void addListener(final EventListener<FileMonitorEvent> listener);

    public boolean removeListener(EventListener<FileMonitorEvent> listener);

    public void dispose();
}
