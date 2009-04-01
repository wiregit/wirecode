package com.limegroup.gnutella.library.monitor.fsevent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

public class FSEventMonitor {

    private final EventListenerList<FSEvent> listeners;

    private final List<String> watchDirs;

    private CoreServices coreServices;

    private CoreFoundation coreFoundation;

    private int currentEvent = -1;

    private RunLoop runLoop;

    public FSEventMonitor() {
        listeners = new EventListenerList<FSEvent>();
        watchDirs = new ArrayList<String>();
    }

    public void init() {
        coreServices = new CoreServicesWrapper();
        coreFoundation = new CoreFoundationWrapper();
        currentEvent = coreServices.FSEventsGetCurrentEventId();

    }

    public void addListener(EventListener<FSEvent> listener) {
        listeners.addListener(listener);
    }

    public boolean removeListener(EventListener<FSEvent> listener) {
        return listeners.removeListener(listener);
    }

    public synchronized void addWatch(File dir) {
        watchDirs.add(dir.getAbsolutePath());
        updateStream();
    }

    private synchronized void updateStream() {
        closeStream();
        runLoop = new RunLoop();
        runLoop.start();
    }

    public synchronized void dispose() {
        closeStream();
        currentEvent = -1;
        coreFoundation = null;
        coreServices = null;
    }

    private void closeStream() {
        if (runLoop != null) {
            runLoop.cancel();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        dispose();
    }

    private class RunLoop extends Thread {

        private Pointer runLoop;

        private Pointer streamRef;

        boolean starting = true;

        @Override
        public void run() {
            Pointer runLoopMode = null;
            synchronized (this) {

                try {
                    System.out.println("thread started");
                    int flags = 2;
                    Pointer pathsToWatch = coreFoundation.CFArrayCreate(watchDirs
                            .toArray(new String[watchDirs.size()]));
                    streamRef = coreServices.FSEventStreamCreate(null, new StreamEventCallback(),
                            null, pathsToWatch, currentEvent, 1.0, flags);
                    runLoopMode = NativeLibrary.getInstance("CoreFoundation")
                            .getGlobalVariableAddress("kCFRunLoopDefaultMode").getPointer(0);
                    runLoop = coreFoundation.CFRetain(coreFoundation.CFRunLoopGetCurrent());

                    coreServices.FSEventStreamScheduleWithRunLoop(streamRef, runLoop, runLoopMode);

                    coreServices.FSEventStreamStart(streamRef);
                } finally {
                    starting = false;
                    notifyAll();
                }
            }
            coreFoundation.CFRunLoopRun();

            System.out.println(coreFoundation.getLastError());
            System.out.println("thread stopped");
        }

        private synchronized void cancel() {
            while (starting) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (runLoop != null) {
                coreFoundation.CFRunLoopStop(runLoop);
                runLoop = null;
            }

            if (streamRef != null) {
                coreServices.FSEventStreamStop(streamRef);
                coreServices.FSEventStreamInvalidate(streamRef);
                coreServices.FSEventStreamRelease(streamRef);
                streamRef = null;
            }
        }
    }

    private class StreamEventCallback implements FSEventStreamCallback {
        public void callback(Pointer streamRef, Pointer clientCallbackInfo, int numEvents,
                Pointer eventPaths, Pointer eventFlags, Pointer eventIds) {
            synchronized (StreamEventCallback.class) {
                int[] myEventFlags = eventFlags.getIntArray(0, numEvents);
                int[] myEventIds = eventIds.getIntArray(0, numEvents);

                Pointer[] myPaths = eventPaths.getPointerArray(0, numEvents);
                for (Pointer pointer : myPaths) {
                    String path = pointer.getString(0);
                    System.out.println(path);
                }

                currentEvent = myEventIds[myEventIds.length - 1];
                System.out.println("in callback");
                System.out.println("numEvents: " + numEvents);
            }
        }
    };
}
