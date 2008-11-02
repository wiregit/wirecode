package com.limegroup.gnutella.library;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.limewire.util.AssertComparisons.assertEmpty;
import static org.limewire.util.AssertComparisons.assertInstanceof;
import static org.limewire.util.AssertComparisons.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    public static void assertAddFails(String reason, FileList fileList, File... files) throws Exception {
        for (File file : files) {
            try {
                FileDesc fd = fileList.add(file).get(5, TimeUnit.SECONDS);
                fail("added: " + fd);
            } catch (ExecutionException expected) {
                assertInstanceof(FileListChangeFailedException.class, expected.getCause());
                FileListChangeFailedException cause = (FileListChangeFailedException) expected.getCause();
                assertEquals(FileListChangedEvent.Type.ADD_FAILED, cause.getEvent().getType());
                assertEquals(file, cause.getEvent().getFile());
                assertEquals(reason, cause.getReason());
            }
        }
    }

    public static void assertAdds(FileList fileList, File... files) throws Exception {
        for (File file : files) {
            assertNotNull(fileList.add(file).get(1, TimeUnit.SECONDS));
        }
    }
    
    public static void assertAddsForSession(GnutellaFileList fileList, File... files) throws Exception {
        for (File file : files) {
            assertNotNull(fileList.addForSession(file).get(1, TimeUnit.SECONDS));
        }
    }
    
    public static void assertFileRenames(ManagedFileList fileList, File old, File newFile) throws Exception {
        assertNotNull(fileList.fileRenamed(old, newFile).get(1, TimeUnit.SECONDS));
    }
    
    public static void assertFileRenameFails(String reason, ManagedFileList fileList, File old, File newFile) throws Exception {
        FileDesc oldFd = fileList.getFileDesc(old);
        Future<FileDesc> future = fileList.fileRenamed(old, newFile);

        try {
            future.get(1, TimeUnit.SECONDS);
            fail("renamed!");
        } catch (ExecutionException expected) {
            assertInstanceof(FileListChangeFailedException.class, expected.getCause());
            FileListChangeFailedException cause = (FileListChangeFailedException) expected.getCause();
            assertEquals(FileListChangedEvent.Type.CHANGE_FAILED, cause.getEvent().getType());
            assertEquals(old, cause.getEvent().getOldFile());
            assertEquals(newFile, cause.getEvent().getFile());
            assertEquals(oldFd, cause.getEvent().getOldValue());
            assertEquals(reason, cause.getReason());
        }
    }
    
    public static void assertFileChanges(ManagedFileList fileList, File file) throws Exception {
        assertNotNull(fileList.fileChanged(file, LimeXMLDocument.EMPTY_LIST).get(1, TimeUnit.SECONDS));
    }
    
    public static void assertFileChangedFails(String reason, ManagedFileList fileList, File file) throws Exception {
        FileDesc oldFd = fileList.getFileDesc(file);
        Future<FileDesc> future = fileList.fileChanged(file, LimeXMLDocument.EMPTY_LIST);
        
        try {
            future.get(1, TimeUnit.SECONDS);
            fail("renamed!");
        } catch (ExecutionException expected) {
            assertInstanceof(FileListChangeFailedException.class, expected.getCause());
            FileListChangeFailedException cause = (FileListChangeFailedException) expected.getCause();
            assertEquals(FileListChangedEvent.Type.CHANGE_FAILED, cause.getEvent().getType());
            assertEquals(file, cause.getEvent().getOldFile());
            assertEquals(file, cause.getEvent().getFile());
            assertEquals(oldFd, cause.getEvent().getOldValue());
            assertEquals(reason, cause.getReason());
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
    public static void waitForLoad(FileManager fileManager, int timeout) throws Exception {
        assertLoads(fileManager.getManagedFileList(), timeout, TimeUnit.MILLISECONDS);
    }
    
    public static void assertLoads(ManagedFileList managedList) throws Exception {
        assertLoads(managedList, 5, TimeUnit.SECONDS);
    }
        public static void assertLoads(ManagedFileList managedList, long timeout, TimeUnit unit) throws Exception {
        assertTrue(((ManagedFileListImpl)managedList).loadManagedFiles().await(timeout, unit));
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
    


    public static File createNewTestStoreFile(File dir) throws Exception{
        return createNewNameStoreTestFile("FileManager_unit_store_test", dir);
    }

    public static File createNewTestStoreFile2(File dir) throws Exception {
        return createNewNameStoreTestFile2("FileManager_unit_store_test", dir);
    }    

    /**
     * Same a createNewTestFile but doesn't actually allocate the requested
     * number of bytes on disk. Instead returns a subclass of File.
     */
    public static File createFakeTestFile(long size, File dir) throws Exception {
        File real = createNewTestFile(1, dir);
        return new HugeFakeFile(dir, real.getName(), size);
    }

    private static class HugeFakeFile extends File {
        private final long length;

        public HugeFakeFile(File dir, String name, long length) {
            super(dir, name);
            this.length = length;
        }

        @Override
        public long length() {
            return length;
        }

        @Override
        public File getCanonicalFile() {
            return this;
        }
        
        @Override
        public File getAbsoluteFile() {
            return this;
        }
    }
    
    
}
