package com.limegroup.gnutella.library.monitor;

import java.io.File;
import java.io.IOException;

import org.limewire.listener.EventListener;

public class NoOpFileMonitor implements FileMonitor {

    @Override
    public void addListener(EventListener<FileMonitorEvent> listener) {

    }

    @Override
    public void addWatch(File file) throws IOException {

    }

    @Override
    public void addWatch(File file, boolean recursive) throws IOException {

    }

    @Override
    public void dispose() {

    }

    @Override
    public void init() throws IOException {

    }

    @Override
    public boolean removeListener(EventListener<FileMonitorEvent> listener) {
        return true;
    }

    @Override
    public void removeWatch(File file) throws IOException {

    }
}
