package org.limewire.swarm.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.limewire.util.FileUtils;

import com.limegroup.bittorrent.BTMetaInfoTest;

public class SwarmFileSystemImplTest extends TestCase {

    public void testGetCompleteSize() {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File file1 = createFile("testGetCompleteSize.1.txt", true);
        file1.delete();
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file1, 1024));
        Assert.assertEquals(1024, swarmFileSystem.getCompleteSize());
        file1.delete();
        File file2 = createFile("testGetCompleteSize.2.txt", true);
        file2.delete();
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file2, 1023));
        Assert.assertEquals(2047, swarmFileSystem.getCompleteSize());
        file2.delete();
    }

    private File createFile(String fileName, boolean delete) {
        File testFile = new File(System.getProperty("java.io.tmpdir") + "/limetests/" + fileName);
        if (delete) {
            testFile.delete();
        }
        return testFile;
    }

    public void testWrite1() throws IOException {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File file1 = createFile("testWrite.1.txt", true);
        file1.delete();
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file1, 1024));

        String testWrite1 = "testWrite File 1";
        ByteBuffer byteBuffer = ByteBuffer.allocate(testWrite1.length());
        byteBuffer.put(testWrite1.getBytes());
        byteBuffer.flip();
        swarmFileSystem.write(byteBuffer, 0);
        Assert.assertTrue(file1.exists());
        Assert.assertEquals(testWrite1.length(), file1.length());
        Assert.assertEquals(testWrite1, new String(FileUtils.readFileFully(file1)));
    }

    public void testRead1() throws IOException {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        // made in previous test.
        File file1 = createFile("testWrite.1.txt", false);
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file1, 1024));

        String testWrite1 = "testWrite File 1";
        ByteBuffer byteBuffer = ByteBuffer.allocate(testWrite1.length());
        swarmFileSystem.read(byteBuffer, 0);
        Assert.assertTrue(file1.exists());
        Assert.assertEquals(testWrite1.length(), byteBuffer.position());
        Assert.assertEquals(testWrite1, new String(byteBuffer.array()));
        file1.delete();
    }

    public void testWriteMulti() throws IOException {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File file1 = createFile("testWriteMulti.1.txt", true);
        file1.delete();
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file1, 10));

        File file2 = createFile("testWriteMulti.2.txt", true);
        file2.delete();
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file2, 6));

        String testWrite1 = "testWrite File 1";
        ByteBuffer byteBuffer = ByteBuffer.allocate(testWrite1.length());
        byteBuffer.put(testWrite1.getBytes());
        byteBuffer.flip();
        swarmFileSystem.write(byteBuffer, 0);
        Assert.assertTrue(file1.exists());
        Assert.assertEquals(10, file1.length());
        String file1contents = new String(FileUtils.readFileFully(file1));
        Assert.assertEquals(testWrite1.substring(0, 10), file1contents);
        Assert.assertTrue(file2.exists());
        Assert.assertEquals(6, file2.length());
        String file2Contents = new String(FileUtils.readFileFully(file2));
        Assert.assertEquals(testWrite1.substring(10, 16), file2Contents);
        file1.delete();
        file2.delete();
    }

    public void testWriteMultiPartial() throws IOException {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File file1 = createFile("testWriteMultiPartial.1.txt", true);
        file1.delete();
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file1, 10));

        File file2 = createFile("testWriteMultiPartial.2.txt", true);
        file2.delete();
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file2, 6));

        String testWrite1 = "File 1";
        ByteBuffer byteBuffer = ByteBuffer.allocate(testWrite1.length());
        byteBuffer.put(testWrite1.getBytes());
        byteBuffer.flip();
        swarmFileSystem.write(byteBuffer, 10);
        Assert.assertFalse(file1.exists());
        Assert.assertEquals(0, file1.length());
        Assert.assertTrue(file2.exists());
        Assert.assertEquals(6, file2.length());
        String file2contents = new String(FileUtils.readFileFully(file2));
        Assert.assertEquals(testWrite1, file2contents);
        file1.delete();
        file2.delete();
    }

    public void testGnutellaPDF1() throws Exception {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File readFile = new File(BTMetaInfoTest.TEST_DATA_DIR
                + "/public_html/pub/gnutella_protocol_0.4.pdf");
        File testfile = createFile("testGnutella.pdf", true);
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(testfile, 44425));

        byte[] data = FileUtils.readFileFully(readFile);
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        swarmFileSystem.write(byteBuffer, 0);
        Assert.assertTrue(testfile.exists());
        Assert.assertEquals(44425, testfile.length());
        Assert.assertEquals("8055d620ba0c507c1af957b43648c99f", FileUtils.getMD5(testfile));

    }

    public void testGnutellaPDF2() throws Exception {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File readFile = new File(BTMetaInfoTest.TEST_DATA_DIR
                + "/public_html/pub/gnutella_protocol_0.4.pdf");
        File testfile = createFile("testGnutella.pdf", true);
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(testfile, 44425));

        byte[] data = FileUtils.readFileFully(readFile);
        ByteBuffer totalBuffer = ByteBuffer.wrap(data);
        totalBuffer.limit(16384);
        swarmFileSystem.write(totalBuffer, 0);
        totalBuffer.limit(32768);
        swarmFileSystem.write(totalBuffer, 16384);
        totalBuffer.limit(data.length);
        swarmFileSystem.write(totalBuffer, 32768);
        Assert.assertTrue(testfile.exists());
        Assert.assertEquals(44425, testfile.length());
        Assert.assertEquals("8055d620ba0c507c1af957b43648c99f", FileUtils.getMD5(testfile));

    }

}
