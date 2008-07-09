package com.limegroup.gnutella.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.limegroup.gnutella.FileEventListener;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.FileManagerEvent.Type;

public class FileManagerTestUtils {

    /**
     * Begins a load on FileManager and waits for the FileManager to finishing loading before 
     * continueing. Also can specify a timeout which will throw an exception if FileManager hasn't 
     * completed in a certain amount of time.
     */
    public static void waitForLoad(FileManager fileManager, int timeout) throws InterruptedException, TimeoutException {
        final CountDownLatch loadedLatch = new CountDownLatch(1);
        FileEventListener listener = new FileEventListener() {
            public void handleFileEvent(FileManagerEvent evt) {
                if (evt.getType() == Type.FILEMANAGER_LOAD_COMPLETE) {
                    loadedLatch.countDown();
                }
            }            
        };
        try {
            fileManager.addFileEventListener(listener);
            fileManager.loadSettings();
            if (!loadedLatch.await(timeout, TimeUnit.MILLISECONDS)) {
                throw new TimeoutException("Loading of FileManager settings did not complete within " + timeout + " ms");
            }
        } finally {
            fileManager.removeFileEventListener(listener);
        }
    }
}
