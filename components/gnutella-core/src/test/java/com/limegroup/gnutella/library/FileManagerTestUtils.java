package com.limegroup.gnutella.library;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.limewire.util.AssertComparisons.assertEmpty;
import static org.limewire.util.AssertComparisons.assertInstanceof;
import static org.limewire.util.AssertComparisons.assertNotNull;
import static org.limewire.util.AssertComparisons.assertNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.listener.EventListener;
import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class FileManagerTestUtils {    


    public static File createNewTestFile(int size, File dir) throws Exception {
        return createNewNamedTestFile(size, "FileManager_unit_test", "tmp", dir);
    }

    public static URN getUrn(File f) throws Exception {
        return URN.createSHA1Urn(f);
    }

    public static void addFail(String reason, FileList fileList, File... files) throws Exception {
        for (File file : files) {
            try {
                fileList.add(file).get(1, TimeUnit.SECONDS);
                fail("added!");
            } catch (ExecutionException expected) {
                assertInstanceof(FileListChangeFailedException.class, expected.getCause());
                FileListChangeFailedException cause = (FileListChangeFailedException) expected.getCause();
                assertEquals(FileListChangedEvent.Type.ADD_FAILED, cause.getEvent().getType());
                assertEquals(file, cause.getEvent().getFile());
                assertEquals(reason, cause.getReason());
            }
        }
    }

    public static void add(FileList fileList, File... files) throws Exception {
        for (File file : files) {
            assertNotNull(fileList.add(file).get(1, TimeUnit.SECONDS));
        }
    }
    
    public static void fileRenamed(ManagedFileList fileList, File old, File newFile) throws Exception {
        assertNotNull(fileList.fileRenamed(old, newFile).get(1, TimeUnit.SECONDS));
    }
    
    public static void fileRenamedFailed(String reason, ManagedFileList fileList, File old, File newFile) throws Exception {
        FileDesc oldFd = fileList.getFileDesc(old);
        Future<FileDesc> future = fileList.fileRenamed(old, newFile);
        
        if(oldFd == null) {
            assertNull(future.get(1, TimeUnit.SECONDS));
            assertNull(reason);
        } else {            
            try {
                future.get(1, TimeUnit.SECONDS);
                fail("renamed!");
            }  catch(ExecutionException expected) {
                assertInstanceof(FileListChangeFailedException.class, expected.getCause());
                FileListChangeFailedException cause = (FileListChangeFailedException) expected.getCause();
                assertEquals(FileListChangedEvent.Type.CHANGE_FAILED, cause.getEvent().getType());
                assertEquals(newFile, cause.getEvent().getFile());
                assertEquals(oldFd, cause.getEvent().getOldValue());
                assertEquals(reason, cause.getReason());
            }
        }
    }
    
    public static void fileChanged(ManagedFileList fileList, File file) throws Exception {
        assertNotNull(fileList.fileChanged(file, LimeXMLDocument.EMPTY_LIST).get(1, TimeUnit.SECONDS));
    }
    
    public static void fileChangedFailed(String reason, ManagedFileList fileList, File file) throws Exception {
        FileDesc oldFd = fileList.getFileDesc(file);
        Future<FileDesc> future = fileList.fileChanged(file, LimeXMLDocument.EMPTY_LIST);
        
        if(oldFd == null) {
            assertNull(future.get(1, TimeUnit.SECONDS));
            assertNull(reason);
        } else {            
            try {
                future.get(1, TimeUnit.SECONDS);
                fail("renamed!");
            }  catch(ExecutionException expected) {
                assertInstanceof(FileListChangeFailedException.class, expected.getCause());
                FileListChangeFailedException cause = (FileListChangeFailedException) expected.getCause();
                assertEquals(FileListChangedEvent.Type.CHANGE_FAILED, cause.getEvent().getType());
                assertEquals(file, cause.getEvent().getFile());
                assertEquals(oldFd, cause.getEvent().getOldValue());
                assertEquals(reason, cause.getReason());
            }
        }
    }
    
    
    public static void assertContainsFiles(List<FileDesc> fds, File... expectedFiles) {
        List<File> files = new ArrayList<File>(fds.size());
        for(FileDesc fd : fds) {
            files.add(fd.getFile());
        }
        
        for(File file : expectedFiles) {
            assertTrue(files.remove(file));
        }
        assertEmpty(files);
    }

    /**
     * Begins a load on FileManager and waits for the FileManager to finishing loading before 
     * continuing. Also can specify a timeout which will throw an exception if FileManager hasn't 
     * completed in a certain amount of time.
     */
    public static void waitForLoad(FileManager fileManager, int timeout) throws InterruptedException, TimeoutException {
        final CountDownLatch loadedLatch = new CountDownLatch(1);
        EventListener<ManagedListStatusEvent> listener = new EventListener<ManagedListStatusEvent>() {
            public void handleEvent(ManagedListStatusEvent evt) {
                if (evt.getType() == ManagedListStatusEvent.Type.LOAD_COMPLETE) {
                    loadedLatch.countDown();
                }
            }            
        };
        try {
            fileManager.getManagedFileList().addManagedListStatusListener(listener);
            ((ManagedFileListImpl)fileManager.getManagedFileList()).loadSettings();
            if (!loadedLatch.await(timeout, TimeUnit.MILLISECONDS)) {
                throw new TimeoutException("Loading of FileManager settings did not complete within " + timeout + " ms");
            }
        } finally {
            fileManager.getManagedFileList().removeManagedListStatusListener(listener);
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
    public static File createNewNamedTestFile(int size, String name, File directory) throws Exception {
        return createNewNamedTestFile(size, name, "tmp", directory);
    }
    
    /**
     * Helper function to create a new temporary file of the given size,
     * with the given name & extension, in the given directory.
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
    
    public static void change(File file) throws Exception {
        FileWriter fw = new FileWriter(file, true);
        fw.write("change");
        fw.flush();
        fw.close();
    }


    public static File createNewNameStoreTestFile(String name, File directory) throws Exception {

        String dir = "com/limegroup/gnutella/";

        File f = TestUtils.getResourceFile(dir + "StoreTestFile.mp3");
        assertTrue(f.exists());
        File file = File.createTempFile(name, ".mp3", directory);

        FileUtils.copy(f, file);
        assertTrue(file.exists());
        file.deleteOnExit();

        return FileUtils.getCanonicalFile(file);
    }

    public static File createNewNameStoreTestFile2(String name, File directory) throws Exception {
        String dir = "com/limegroup/gnutella/";

        File f = TestUtils.getResourceFile(dir + "StoreTestFile2.mp3");
        assertTrue(f.exists());
        File file = File.createTempFile(name, ".mp3", directory);

        FileUtils.copy(f, file);
        assertTrue(file.exists());
        file.deleteOnExit();

        return FileUtils.getCanonicalFile(file);
    }

}
