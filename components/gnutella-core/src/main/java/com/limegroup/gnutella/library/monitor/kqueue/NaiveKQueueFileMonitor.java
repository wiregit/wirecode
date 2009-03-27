package com.limegroup.gnutella.library.monitor.kqueue;

/* Copyright (c) 2008 Olivier Chafik, All Rights Reserved
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.  
 */

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

/**
 * Looks like the java example was initially copied from:
 * 
 * http://julipedia.blogspot.com/2004/10/example-of-kqueue.html
 * 
 * 
 * Naive KQueue implementation of FileMonitor, for BSD-derived systems
 * (including Mac OS X).<br/> This implementation is naive in that it creates
 * one thread and one kqueue per watched file, which is heavy, slow and not
 * scalable.<br/> Does not handle FileEvent.FILE_CREATED event (only existing
 * files can be watched).<br/> Recursive watching is not implemented either.
 * 
 * @author Olivier Chafik
 */
public class NaiveKQueueFileMonitor {
    /**
     * kqueue man pages http://people.freebsd.org/~jmg/kqueue.historic.man.html
     */
    private final Map<File, FileWatcher> fileWatchers;

    final EventListenerList<KQueueEvent> listeners;

    public NaiveKQueueFileMonitor() {
        fileWatchers = new HashMap<File, FileWatcher>();
        listeners = new EventListenerList<KQueueEvent>();
    }

    public void addListener(EventListener<KQueueEvent> listener) {
        listeners.addListener(listener);
    }

    public boolean removeListener(EventListener<KQueueEvent> listener) {
        return listeners.removeListener(listener);
    }

    public synchronized void removeWatch(File file) {
        FileWatcher fw = fileWatchers.get(file);
        if (fw != null) {
            fileWatchers.remove(file);
            fw.interrupt();
        }
    }

    public synchronized void addWatch(File file) throws IOException {
        addWatch(file, KQueueEventMask.ALL_EVENTS.getMask());
    }

    public synchronized void addWatch(File file, int mask) throws IOException {
        FileWatcher fw = new FileWatcher(listeners, file, mask);
        fileWatchers.put(file, fw);
        fw.start();
    }
}