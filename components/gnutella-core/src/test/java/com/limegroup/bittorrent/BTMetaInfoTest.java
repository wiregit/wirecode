package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import junit.framework.Assert;

import org.limewire.util.BaseTestCase;

public class BTMetaInfoTest extends BaseTestCase {
    /**
     * A directory containing the test data for this unit test.
     */
    private static final String TEST_DATA_DIR = System.getProperty("user.dir")
            + "/../tests/test-data";

    public BTMetaInfoTest(String name) {
        super(name);
    }

    public void testBasics() throws Exception {
        File file = getFile("test-basics.torrent");
        byte[] bytes = getBytes(file);
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        BTMetaInfo metaInfo = btm.createBTMetaInfoFromBytes(bytes);

        Assert.assertEquals(44425, metaInfo.getFileDesc().getFileSize());
        Assert.assertEquals("gnutella_protocol_0.4.pdf", metaInfo.getName());
        Assert.assertEquals(262144, metaInfo.getPieceLength());
        Assert.assertEquals((int)Math.ceil((double)metaInfo.getFileDesc().getFileSize()/metaInfo.getPieceLength()), metaInfo.getNumBlocks());
        Assert.assertEquals("http://localhost:8080/tracker/announce", metaInfo.getTrackers()[0]
                .toString());
        Assert.assertEquals(false, metaInfo.isPrivate());
        Assert.assertNull(metaInfo.getWebSeeds());
    }
    
    public void testSingleWebSeedSingleFile() throws Exception {
        File file = getFile("test-single-webseed-single-file.torrent");
        byte[] bytes = getBytes(file);
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        BTMetaInfo metaInfo = btm.createBTMetaInfoFromBytes(bytes);
        Assert.assertEquals("gnutella_protocol_0.4.pdf", metaInfo.getName());
        Assert.assertEquals(2, metaInfo.getNumBlocks());
        Assert.assertEquals(32768, metaInfo.getPieceLength());
        Assert.assertEquals("http://localhost:8080/tracker/announce", metaInfo.getTrackers()[0]
                .toString());
        Assert.assertEquals(true, metaInfo.isPrivate());
        Assert.assertNotNull(metaInfo.getWebSeeds());
        Assert.assertEquals(1, metaInfo.getWebSeeds().length);
        Assert.assertEquals("http://mirror.com/file.exe", metaInfo.getWebSeeds()[0].toString());
    }
    
    public void testMultipleWebSeedSingleFile() throws Exception {
        File file = getFile("test-multiple-webseed-single-file.torrent");
        byte[] bytes = getBytes(file);
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        BTMetaInfo metaInfo = btm.createBTMetaInfoFromBytes(bytes);
        Assert.assertEquals("gnutella_protocol_0.4.pdf", metaInfo.getName());
        Assert.assertEquals(2, metaInfo.getNumBlocks());
        Assert.assertEquals(32768, metaInfo.getPieceLength());
        Assert.assertEquals("http://localhost:8080/tracker/announce", metaInfo.getTrackers()[0]
                .toString());
        Assert.assertEquals(true, metaInfo.isPrivate());
        Assert.assertNotNull(metaInfo.getWebSeeds());
        Assert.assertEquals(3, metaInfo.getWebSeeds().length);
        Assert.assertEquals("http://mirror.com/file.exe", metaInfo.getWebSeeds()[0].toString());
        Assert.assertEquals("http://mirror1.com/file.exe", metaInfo.getWebSeeds()[1].toString());
        Assert.assertEquals("http://mirror2.com/file.exe", metaInfo.getWebSeeds()[2].toString());
    }
    
    public void testSingleWebSeedMultipleFile() throws Exception {
        File file = getFile("test-single-webseed-multiple-file.torrent");
        byte[] bytes = getBytes(file);
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        BTMetaInfo metaInfo = btm.createBTMetaInfoFromBytes(bytes);
        Assert.assertEquals("test", metaInfo.getName());
 
        Assert.assertEquals(2, metaInfo.getNumBlocks());
        Assert.assertEquals(262144, metaInfo.getPieceLength());
        Assert.assertEquals("http://localhost:8080/tracker/announce", metaInfo.getTrackers()[0]
                .toString());
        Assert.assertEquals(true, metaInfo.isPrivate());
        Assert.assertNotNull(metaInfo.getWebSeeds());
        Assert.assertEquals(1, metaInfo.getWebSeeds().length);
        Assert.assertEquals("http://mirror.com/test/", metaInfo.getWebSeeds()[0].toString());
    }
    
    public void testMultipleWebSeedMultipleFile() throws Exception {
        File file = getFile("test-multiple-webseed-multiple-file.torrent");
        byte[] bytes = getBytes(file);
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        BTMetaInfo metaInfo = btm.createBTMetaInfoFromBytes(bytes);
        Assert.assertEquals("test", metaInfo.getName());
        Assert.assertEquals(2, metaInfo.getNumBlocks());
        Assert.assertEquals(262144, metaInfo.getPieceLength());
        Assert.assertEquals("http://localhost:8080/tracker/announce", metaInfo.getTrackers()[0]
                .toString());
        Assert.assertEquals(true, metaInfo.isPrivate());
        Assert.assertNotNull(metaInfo.getWebSeeds());
        Assert.assertEquals(3, metaInfo.getWebSeeds().length);
        Assert.assertEquals("http://mirror.com/test/", metaInfo.getWebSeeds()[0].toString());
        Assert.assertEquals("http://mirror1.com/test/", metaInfo.getWebSeeds()[1].toString());
        Assert.assertEquals("http://mirror2.com/test/", metaInfo.getWebSeeds()[2].toString());
    }

    /**
     * Returns a file in the TEST_DATA_DIR by the given filename.
     * @param fileName
     * @return
     */
    private File getFile(String fileName) {
        File file = new File(TEST_DATA_DIR + "/" + fileName);
        return file;
    }

    /**
     * Returns the bytes of a file in a byte array. This method is assuming
     * smaller file sizes. It can only return up to Integer.MAXIMUM bytes.
     * 
     * @param file
     * @return
     * @throws IOException
     */
    private byte[] getBytes(File file) throws IOException {
        FileInputStream fis = null;
        ByteBuffer byteBuffer = null;
        try {
            fis = new FileInputStream(file);
            FileChannel fileChannel = fis.getChannel();
            byteBuffer = ByteBuffer.allocate((int) file.length());
            fileChannel.read(byteBuffer);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return byteBuffer.array();
    }
}
