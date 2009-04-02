package com.limegroup.gnutella.library.monitor.fsevent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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
        watchDirs = Collections.synchronizedList(new ArrayList<String>());
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

        private CountDownLatch started = new CountDownLatch(1);

        @Override
        public void run() {
            Pointer runLoopMode = null;
                 try {
                    int flags = 2;
                    Pointer pathsToWatch = null;

                    pathsToWatch = coreFoundation.CFArrayCreate(watchDirs
                            .toArray(new String[watchDirs.size()]));
                    streamRef = coreServices.FSEventStreamCreate(null, new StreamEventCallback(),
                            null, pathsToWatch, currentEvent, 1.0, flags);
                    runLoopMode = NativeLibrary.getInstance("CoreFoundation")
                            .getGlobalVariableAddress("kCFRunLoopDefaultMode").getPointer(0);
                    runLoop = coreFoundation.CFRunLoopGetCurrent();

                    coreServices.FSEventStreamScheduleWithRunLoop(streamRef, runLoop, runLoopMode);

                    coreServices.FSEventStreamStart(streamRef);
                } finally {
                    started.countDown();
                }
            coreFoundation.CFRunLoopRun();
        }

        private synchronized void cancel() {

            try {
                started.await();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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

            int[] myEventFlags = eventFlags.getIntArray(0, numEvents);
            int[] myEventIds = eventIds.getIntArray(0, numEvents);
            Pointer[] myPaths = eventPaths.getPointerArray(0, numEvents);
            
            for(int i = 0; i < numEvents; i++) {
                String path = myPaths[i].getString(0);
                int eventId = myEventIds[i];
                int eventFlag = myEventFlags[i];
                FSEvent event = new FSEvent(path, eventId, eventFlag);
                currentEvent = eventId;
                //TODO broadcast asynchronously
                listeners.broadcast(event);
            }

        }
    }

    public synchronized void removeWatch(File dir) {
        watchDirs.remove(dir.getAbsolutePath());
        updateStream();
    };
}
