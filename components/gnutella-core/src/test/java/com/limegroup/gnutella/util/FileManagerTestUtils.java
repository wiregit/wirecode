package com.limegroup.gnutella.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.limewire.listener.EventListener;
import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

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
        EventListener<FileManagerEvent> listener = new EventListener<FileManagerEvent>() {
            public void handleEvent(FileManagerEvent evt) {
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

    // build xml string for video
    public static String buildVideoXMLString(String keyname) {
        return "<?xml version=\"1.0\"?><videos xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/video.xsd\"><video "
            + keyname
            + "></video></videos>";
    }

    public static String buildAudioXMLString(String keyname) {
        return "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio "
            + keyname
            + "></audio></audios>";
    }

    public static String buildDocumentXMLString(String keyname) {
        return "<?xml version=\"1.0\"?><documents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/document.xsd\">" +
                "<document " + keyname + " /></documents>";    
    }

    /**
     * Helper function to create a new temporary file of the given size,
     * with the given name, in the given directory.
     */
    public static File createNewNamedTestFile(int size, String name,
                                              String extension, File directory) throws Exception {
        File file = File.createTempFile(name, "." + extension, directory);
        file.deleteOnExit();
        OutputStream out = new FileOutputStream(file);

        for (int i=0; i<size; i++) {
            out.write(name.charAt(i % name.length()));
        }
        out.flush();
        out.close();
            
        //Needed for comparisons between "C:\Progra~1" and "C:\Program Files".
        return FileUtils.getCanonicalFile(file);
    }


    public static File createNewNameStoreTestFile(String name, File directory) throws Exception {

        String dir = "com/limegroup/gnutella/";

        File f = TestUtils.getResourceFile(dir + "StoreTestFile.mp3");
        Assert.assertTrue(f.exists());
        File file = File.createTempFile(name, ".mp3", directory);

        FileUtils.copy(f, file);
        Assert.assertTrue(file.exists());
        file.deleteOnExit();

        return FileUtils.getCanonicalFile(file);
    }

    public static File createNewNameStoreTestFile2(String name, File directory) throws Exception {
        String dir = "com/limegroup/gnutella/";

        File f = TestUtils.getResourceFile(dir + "StoreTestFile2.mp3");
        Assert.assertTrue(f.exists());
        File file = File.createTempFile(name, ".mp3", directory);

        FileUtils.copy(f, file);
        Assert.assertTrue(file.exists());
        file.deleteOnExit();

        return FileUtils.getCanonicalFile(file);
    }

}
