package com.limegroup.bittorrent;

import java.io.File;

import junit.framework.Assert;

import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;

public class BTMetaInfoTest extends BaseTestCase {
    /**
     * A directory containing the test data for this unit test.
     */
    public static final String TEST_DATA_DIR = System.getProperty("user.dir")
            + "/tests/test-data";

    public BTMetaInfoTest(String name) {
        super(name);
    }

    public void testBasics() throws Exception {
        File file = getFile("test-basics.torrent");
        byte[] bytes = FileUtils.readFileFully(file);
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        BTMetaInfo metaInfo = btm.createBTMetaInfoFromBytes(bytes);

        Assert.assertEquals(44425, metaInfo.getFileSystem().getTotalSize());
        Assert.assertEquals("gnutella_protocol_0.4.pdf", metaInfo.getName());
        Assert.assertEquals(262144, metaInfo.getPieceLength());
        Assert.assertEquals((int)Math.ceil((double)metaInfo.getFileSystem().getTotalSize()/metaInfo.getPieceLength()), metaInfo.getNumBlocks());
        Assert.assertEquals("http://localhost:8080/tracker/announce", metaInfo.getTrackers()[0]
                .toString());
        Assert.assertEquals(false, metaInfo.isPrivate());
        Assert.assertNull(metaInfo.getWebSeeds());
    }
    
    public void testSingleWebSeedSingleFile() throws Exception {
        File file = getFile("test-single-webseed-single-file.torrent");
        byte[] bytes = FileUtils.readFileFully(file);
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        BTMetaInfo metaInfo = btm.createBTMetaInfoFromBytes(bytes);
        Assert.assertEquals("gnutella_protocol_0.4.pdf", metaInfo.getName());
        Assert.assertEquals(1, metaInfo.getNumBlocks());
        Assert.assertEquals(262144, metaInfo.getPieceLength());
        Assert.assertEquals("http://localhost/~pvertenten/tracker/announce.php", metaInfo.getTrackers()[0]
                .toString());
        Assert.assertEquals(false, metaInfo.isPrivate());
        Assert.assertNotNull(metaInfo.getWebSeeds());
        Assert.assertEquals(1, metaInfo.getWebSeeds().length);
        Assert.assertEquals("http://pvertenten-pc.limewire.com/~pvertenten/pub/gnutella_protocol_0.4.pdf", metaInfo.getWebSeeds()[0].toString());
    }
    
    public void testMultipleWebSeedSingleFile() throws Exception {
        File file = getFile("test-multiple-webseed-single-file.torrent");
        byte[] bytes = FileUtils.readFileFully(file);
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
        byte[] bytes = FileUtils.readFileFully(file);
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
        Assert.assertEquals("http://pvertenten-pc.limewire.com/~pvertenten/pub/", metaInfo.getWebSeeds()[0].toString());
    }
    
    public void testMultipleWebSeedMultipleFile() throws Exception {
        File file = getFile("test-multiple-webseed-multiple-file.torrent");
        byte[] bytes = FileUtils.readFileFully(file);
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
        Assert.assertEquals("http://mirror.com/pub/", metaInfo.getWebSeeds()[0].toString());
        Assert.assertEquals("http://mirror1.com/pub/", metaInfo.getWebSeeds()[1].toString());
        Assert.assertEquals("http://mirror2.com/pub/", metaInfo.getWebSeeds()[2].toString());
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
}
