package org.limewire.swarm.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.util.FileUtils;

import junit.framework.Assert;
import junit.framework.TestCase;

public class SwarmFileSystemImplTest extends TestCase {

    public void testGetCompleteSize() {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File file1 = createFile("testGetCompleteSize.1.txt");
        file1.delete();
        swarmFileSystem.add(new SwarmFileImpl(file1, 1024));
        Assert.assertEquals(1024, swarmFileSystem.getCompleteSize());
        File file2 = createFile("testGetCompleteSize.2.txt");
        file2.delete();
        swarmFileSystem.add(new SwarmFileImpl(file2, 1023));
        Assert.assertEquals(2047, swarmFileSystem.getCompleteSize());
    }

    private File createFile(String fileName) {
        return new File(System.getProperty("java.io.tmpdir") + "/" + fileName);
    }

    public void testWrite1() throws IOException {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File file1 = createFile("testWrite.1.txt");
        file1.delete();
        swarmFileSystem.add(new SwarmFileImpl(file1, 1024));

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
        File file1 = createFile("testWrite.1.txt");
        swarmFileSystem.add(new SwarmFileImpl(file1, 1024));
        
        String testWrite1 = "testWrite File 1";
        ByteBuffer byteBuffer = ByteBuffer.allocate(testWrite1.length());
        swarmFileSystem.read(byteBuffer, 0);
        Assert.assertTrue(file1.exists());
        Assert.assertEquals(testWrite1.length(), byteBuffer.position());
        Assert.assertEquals(testWrite1, new String(byteBuffer.array()));
    }
    
    public void testWriteMulti() throws IOException {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File file1 = createFile("testWriteMulti.1.txt");
        file1.delete();
        swarmFileSystem.add(new SwarmFileImpl(file1, 10));
        
        File file2 = createFile("testWriteMulti.2.txt");
        file2.delete();
        swarmFileSystem.add(new SwarmFileImpl(file2, 6));
        
        

        String testWrite1 = "testWrite File 1";
        ByteBuffer byteBuffer = ByteBuffer.allocate(testWrite1.length());
        byteBuffer.put(testWrite1.getBytes());
        byteBuffer.flip();
        swarmFileSystem.write(byteBuffer, 0);
        Assert.assertTrue(file1.exists());
        Assert.assertEquals(10, file1.length());
        Assert.assertEquals(testWrite1.substring(0, 10), new String(FileUtils.readFileFully(file1)));
        Assert.assertTrue(file2.exists());
        Assert.assertEquals(6, file2.length());
        Assert.assertEquals(testWrite1.substring(10, 16), new String(FileUtils.readFileFully(file2)));

    }
    
    public void testWriteMultiPartial() throws IOException {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File file1 = createFile("testWriteMultiPartial.1.txt");
        file1.delete();
        swarmFileSystem.add(new SwarmFileImpl(file1, 10));
        
        File file2 = createFile("testWriteMultiPartial.2.txt");
        file2.delete();
        swarmFileSystem.add(new SwarmFileImpl(file2, 6));
        
        

        String testWrite1 = "File 1";
        ByteBuffer byteBuffer = ByteBuffer.allocate(testWrite1.length());
        byteBuffer.put(testWrite1.getBytes());
        byteBuffer.flip();
        swarmFileSystem.write(byteBuffer, 10);
        Assert.assertFalse(file1.exists());
        Assert.assertEquals(0, file1.length());
        Assert.assertTrue(file2.exists());
        Assert.assertEquals(6, file2.length());
        Assert.assertEquals(testWrite1, new String(FileUtils.readFileFully(file2)));

    }

}
