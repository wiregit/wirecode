package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;

import junit.framework.Test;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;

public class VerifyingFileTest extends BaseTestCase {
    
    public VerifyingFileTest(String s) {
        super(s);
    }
    
    public static Test suite() {
        return buildTestSuite(VerifyingFileTest.class);
    }
    
    static VerifyingFile vf;
    
    private static final String filename = 
        "com/limegroup/gnutella/metadata/mpg4_golem160x90first120.avi";
    private static final File completeFile = CommonUtils.getResourceFile(filename);
    
    private static RandomAccessFile raf;
       
    private static HashTree hashTree, defaultHashTree;
    
    private static final String sha1 = 
        "urn:sha1:UBJSGDTCVZDSBS4K3ZDQJV5VQ3WTBCOK";
    
    public static void globalSetUp() throws Exception {
        try {
            defaultHashTree = (HashTree) PrivilegedAccessor.invokeMethod(
                HashTree.class, "createHashTree", 
                new Object[] { new Long(completeFile.length()), new FileInputStream(completeFile),
                            URN.createSHA1Urn(sha1) },
                new Class[] { long.class, InputStream.class, URN.class }
            );
        } catch(InvocationTargetException ite) {
            throw (Exception)ite.getCause();
        }
        
        raf = new RandomAccessFile(completeFile,"r");
    }
    
    public void setUp() {
        hashTree=defaultHashTree;
        vf = new VerifyingFile((int)completeFile.length());
        try {
        vf.open(new File("outfile"));
        vf.setHashTree(defaultHashTree);
        raf.seek(0);
        }catch(IOException e) {
            ErrorService.error(e);
        }
    }
    
    public void tearDown() {
        vf.close();
    }
    
    /**
     * tests that sequential chunks are leased.
     */
    public void testLease() throws Exception {
        int chunkSize = (int)completeFile.length()/5;
        PrivilegedAccessor.setValue(vf, "blockChooser", new TestSequentialStrategy());
        for (long i = 0;i < 5 ;i++) {
            Interval leased = vf.leaseWhite(chunkSize);
            assertEquals(i * chunkSize,leased.low);
            assertEquals((i+1)*chunkSize-1,leased.high);
        }
        
        // the last interval is shorter
        Interval last = vf.leaseWhite(chunkSize);
        assertLessThan(chunkSize,last.high - last.low+1);
        assertEquals(chunkSize*5,last.low);
        assertEquals(completeFile.length(),last.high+1);
    }
    
    /**
     * tests that every lease ends at a chunk offset  
     * @throws Exception
     */
    public void testLeaseDifferentSizes() throws Exception {
        long fileSize = completeFile.length();
        // KAM -- I think the intetion was to use DEFAULT_CHUNK_SIZE
        // rather than hard-coding the old 100,000 byte value.
        // However, running at least one test with a block size that 
        // isn't a power of two has a certain appeal for testing.
        Interval firstLease = vf.leaseWhite(100000);
        if (firstLease.high % 100000 != 99999 &&
                firstLease.high != fileSize-1) {
            assertTrue("First chunk is not aligned.", false);
        }
        
        // These tests have been re-arranged to go in order of
        // decreasing chunk size in order to reduce the number
        // of cases that need to be checked.
        
        // TODO KAM -- not really sure why this is relavant, but I have
        // modified the test to test what the javadoc claims to test
        Interval secondLease = vf.leaseWhite(512*1024+1);
        if (secondLease.high % (512 * 1024 + 1) != 512*1024 &&
                secondLease.high != firstLease.low-1 &&
                secondLease.high != fileSize-1) {
             assertTrue("Failed to assign a 512k+1 aligned chunk.",
                     false);
         }
        
        // now assume the chunk size is 512K
        Interval leased = vf.leaseWhite(512*1024);
        if (leased.high % (512 * 1024) != 512*1024-1 &&
               leased.high != firstLease.low-1 &&
               leased.high != secondLease.low-1 &&
               leased.high != fileSize-1) {
            assertTrue("Failed to assign a 512k-aligned chunk.",
                    false);
        }
        
        // now lease assuming the chunk size is 256K
        leased = vf.leaseWhite(256*1024);
        if (leased.high % (256 * 1024) != 256*1024-1 &&
               leased.high != firstLease.low-1 &&
               leased.high != secondLease.low-1 &&
               leased.high != fileSize-1) {
            assertTrue("Failed to assign a 256k-aligned chunk.",
                    false);
        } 
    }
    
    /**
     * tests that once a chunk is released, the white spot is leased next
     */
    public void testRelease() throws Exception {
        // Lease two chunks and create a hole in between them.

        // This test assumes a sequential download strategy.
        PrivilegedAccessor.setValue(vf, "blockChooser", new TestSequentialStrategy());
        Interval leased = vf.leaseWhite(512*1024);
        vf.releaseBlock(new Interval (128*1024, 3*128*1024-1));
        
        // we should fill up everything up to the chunk offset
        leased = vf.leaseWhite(256*1024);
        assertEquals(128*1024,leased.low);
        assertEquals(256*1024-1,leased.high);
        
        // the next lease should fill up to the start of the previously leased area
        leased = vf.leaseWhite(256*1024);
        assertEquals(256*1024,leased.low);
        assertEquals(3*128*1024-1,leased.high);
    }
    
    /**
     * tests that writing full chunks of data gets them verified
     */
    public void testWriteFullChunks() throws Exception {
        vf.leaseWhite((int)completeFile.length());
        
        int pos = 0;
        int numChunks = ((int)completeFile.length() / vf.getChunkSize());
        byte [] chunk;
        while(pos < numChunks * vf.getChunkSize()) {
            
            // read the file from the original file
            chunk = new byte[vf.getChunkSize()];
            raf.read(chunk);
            raf.seek(pos + chunk.length);
            
            // write it to the verifying file
            writeImpl(pos,chunk);
            pos+=chunk.length;
            
            // give it some time to verify
            Thread.sleep(1000);
            assertEquals(pos,vf.getVerifiedBlockSize());
        }
        
        // verify the last chunk
        chunk = new byte[(int)completeFile.length() - pos];
        raf.read(chunk);
        writeImpl(pos,chunk);
        Thread.sleep(1000);
        assertEquals(completeFile.length(),vf.getVerifiedBlockSize());
    }
    
    private void writeImpl(int pos, byte[] chunk) throws Exception {
        if (chunk.length > HTTPDownloader.BUF_LENGTH) {
            int written = 0;
            while (written < chunk.length) {
                int toWrite = Math.min(chunk.length - written, HTTPDownloader.BUF_LENGTH);
                byte [] temp = new byte[toWrite];
                System.arraycopy(chunk,written,temp,0,toWrite);
                written += toWrite;
                vf.writeBlock(pos,temp);
                pos += toWrite;
            }
        } else
            vf.writeBlock(pos,chunk);
    }
    
    /**
     * test that writing partial chunks gets them assembled and verified
     * when possible
     */
    public void testWritePartialChunks() throws Exception {
        vf.leaseWhite((int)completeFile.length());
        
        byte [] chunk = new byte[200*1024];
        raf.read(chunk);
        raf.seek(200*1024);
        
        // write some data, not enough to fill a chunk
        // 0-200k
        writeImpl(0,chunk);
        Thread.sleep(1000);
        assertEquals(0,vf.getVerifiedBlockSize());
        
        // write some more data filling up the first chunk
        // 200k-400k
        raf.read(chunk);
        
        writeImpl(200*1024,chunk);
        Thread.sleep(1000);
        assertEquals(256*1024,vf.getVerifiedBlockSize());
        assertEquals(400*1024,vf.getBlockSize());
        
        // now read some data which will not fill up any chunk
        // 600k-800k
        raf.seek(600*1024);
        raf.read(chunk);
        writeImpl(600*1024,chunk);
        Thread.sleep(1000);
        assertEquals(256*1024,vf.getVerifiedBlockSize());
        assertEquals(600*1024,vf.getBlockSize());
        
        // now fill up the gap which should make two chunks verifyable
        // 400k-600k = chunks 256-512 and 512-768 verifyable
        raf.seek(400*1024);
        raf.read(chunk);
        writeImpl(400*1024,chunk);
        Thread.sleep(1000);
        assertEquals(768*1024,vf.getVerifiedBlockSize());
        assertEquals(800*1024, vf.getBlockSize());
        
        // write something in part of the last chunk, should not change anything
        int numChunks = ((int)completeFile.length() / vf.getChunkSize());
        int lastOffset = numChunks * vf.getChunkSize();
        chunk = new byte[((int)completeFile.length() - lastOffset)/2 +1];
        
        raf.seek(completeFile.length()-chunk.length);
        raf.read(chunk);
        writeImpl((int)(completeFile.length()-chunk.length),chunk);
        Thread.sleep(1000);
        assertEquals(768*1024,vf.getVerifiedBlockSize());
        assertEquals(800*1024+chunk.length, vf.getBlockSize());
        
        // write something more, enough to fill up the last chunk which should get it verified
        raf.seek(completeFile.length() - 2*chunk.length);
        raf.read(chunk);
        writeImpl((int)(completeFile.length() - 2*chunk.length),chunk);
        Thread.sleep(1000);
        assertEquals(768*1024+completeFile.length() - lastOffset,vf.getVerifiedBlockSize());
        assertEquals(800*1024 + 2*chunk.length,vf.getBlockSize());
    }
    
    /**
     * tests that corrupt data does not get written to disk
     */
    public void testCorruptChunks() throws Exception {
        // This test assumes a sequential download strategy
        PrivilegedAccessor.setValue(vf, "blockChooser", new TestSequentialStrategy());
        vf.leaseWhite((int)completeFile.length());
        byte [] chunk = new byte[hashTree.getNodeSize()];
        
        // write a good chunk
        raf.read(chunk);
        writeImpl(0,chunk);
        Thread.sleep(1000);
        
        assertEquals(chunk.length,vf.getVerifiedBlockSize());
        assertEquals(chunk.length,vf.getBlockSize());
        
        // now try to write a corrupt chunk
        raf.read(chunk);
        for (int i = 0;i< 100;i++)
            chunk[i]=(byte)i;
        
        writeImpl(chunk.length, chunk);
        Thread.sleep(1000);
        
        // the chunk should not be verified or even written to disk
        assertEquals(chunk.length,vf.getVerifiedBlockSize());
        assertEquals(chunk.length,vf.getBlockSize());
        
        // and if we try to lease an interval, it will be from within that hole
        Interval leased = vf.leaseWhite(hashTree.getNodeSize());
        
        assertEquals(chunk.length,leased.low);
        assertEquals(chunk.length*2 -1, leased.high);
    }
    
    /**
     *  tests that if more than n % of the file needed redownloading we give up.  
     */
    public void testGiveUp() throws Exception {
        vf.leaseWhite((int)completeFile.length());
        byte [] chunk = new byte[hashTree.getNodeSize()];
        
        int j = 0;
        while (j * chunk.length < completeFile.length() * VerifyingFile.MAX_CORRUPTION) {
            assertFalse(vf.isHopeless());
            raf.read(chunk);
            for (int i = 0;i< 100;i++)
                chunk[i]=(byte)i;
            writeImpl((int)(raf.getFilePointer() - chunk.length),chunk);
            Thread.sleep(1000);
            j++;
        }
        assertTrue(vf.isHopeless());
    }
    
    public void testWriteCompleteNoTree() throws Exception {
        vf.setHashTree(null);
        vf.leaseWhite((int)completeFile.length());
        byte [] chunk = new byte[(int)completeFile.length()];
        
        raf.readFully(chunk);
        writeImpl(0,chunk);
        Thread.sleep(1000);
        assertTrue(vf.isComplete());
    }
    
    public void testWriteSomeVerifiedSomeNot() throws Exception {
        vf.leaseWhite((int)completeFile.length());
        
        vf.setDiscardUnverified(false);
        byte [] chunk = new byte[hashTree.getNodeSize()];
        raf.readFully(chunk);
        writeImpl(0,chunk);
        
        Thread.sleep(1000);
        assertEquals(chunk.length, vf.getVerifiedBlockSize());
        
        // write a bad chunk, it should not be discarded
        writeImpl(chunk.length,chunk);
        Thread.sleep(1000);
        assertEquals(chunk.length, vf.getVerifiedBlockSize());
        assertEquals(2*chunk.length, vf.getBlockSize());
        
        // the rest of the chunks will be good.
        raf.readFully(chunk);
        chunk = new byte[(int)completeFile.length() - 512*1024];
        raf.readFully(chunk);
        
        writeImpl(512*1024,chunk);
        Thread.sleep(1000);
        
        assertEquals((int)completeFile.length() - 256*1024, vf.getVerifiedBlockSize());
        assertTrue(vf.isComplete());
        
    }
    
    public void testWaitForPending() throws Exception {
        vf.leaseWhite((int)completeFile.length());
        
        byte [] chunk = new byte[hashTree.getNodeSize()];
        raf.readFully(chunk);
        
        // write a chunk.  waitForPending should return very quickly
        writeImpl(0,chunk);
        long now = System.currentTimeMillis();
        vf.waitForPendingIfNeeded();
        assertLessThanOrEquals(50,System.currentTimeMillis() - now);
        
        Thread.sleep(1000);
        assertEquals(hashTree.getNodeSize(),vf.getVerifiedBlockSize());

        // now fill in the rest of the file.  waitForPending should wait until all pending
        // chunks have been verified.
        chunk = new byte[(int)completeFile.length() - hashTree.getNodeSize()];
        raf.readFully(chunk);
        writeImpl(hashTree.getNodeSize(),chunk);
        assertFalse(vf.isComplete());
        vf.waitForPendingIfNeeded();
        assertTrue(vf.isComplete());
        
    }
    
    /**
     * tests that a file whose size is an exact multiple of the chunk size works fine
     */
    public void testExactMultiple() throws Exception {
        File exact = new File("exactSize");
        RandomAccessFile raf = new RandomAccessFile(exact,"rw");
        for (int i = 0;i < 1024*1024; i++)
            raf.write(i);
        raf.close();
        
        HashTree exactTree;
        try {
            exactTree = (HashTree) PrivilegedAccessor.invokeMethod(
                HashTree.class, "createHashTree", 
                new Object[] { new Long(exact.length()), new FileInputStream(exact),
                            URN.createSHA1Urn(exact) },
                new Class[] { long.class, InputStream.class, URN.class }
            );
        } catch(InvocationTargetException ite) {
            throw (Exception)ite.getCause();
        }
        
        assertEquals(0,exact.length() % exactTree.getNodeSize());
        raf = new RandomAccessFile(exact,"r");
        
        vf.close();
        vf = new VerifyingFile((int)exact.length());
        vf.open(new File("outfile"));
        vf.setHashTree(exactTree);
        vf.leaseWhite();
        
        // now, see if this file downloads correctly if a piece of the last chunk is added
        byte [] data = new byte[exactTree.getNodeSize()/2];
        raf.seek(exact.length() - data.length );
        raf.readFully(data);
        writeImpl((int)(exact.length() - data.length) ,data);
        
        // nothing should be verified
        Thread.sleep(1000);
        assertEquals(0,vf.getVerifiedBlockSize());
        
        // now add the second piece of the last chunk
        raf.seek(exact.length() - 2*data.length );
        raf.readFully(data);
        writeImpl((int)(exact.length() - 2*data.length) ,data);
        
        // the last chunk should be verified
        Thread.sleep(1000);
        assertEquals(exactTree.getNodeSize(),vf.getVerifiedBlockSize());
    }
    
    /**
     * Tests that if the tree is found after the entire file is downloaded,
     * we still verify accordingly
     */
    public void testTreeAddedAfterEnd() throws Exception {
        vf.setHashTree(null);
        vf.leaseWhite();
        byte [] full = new byte [(int)completeFile.length()];
        raf.readFully(full);
        writeImpl(0,full);
        
        // the file should be completed
        Thread.sleep(500);
        assertTrue(vf.isComplete());
        
        synchronized(vf) {
            // give it a hashTree
            vf.setHashTree(defaultHashTree);
            
            // now, it shouldn't be complete
            assertFalse(vf.isComplete());
        }
        
        // but things should be pending verification
        vf.waitForPendingIfNeeded();
        assertTrue(vf.isComplete());
    }
    
    /**
     * Tests that if the incomplete file had some data in it,
     * and we told the VF to use that data, it'll auto-verify
     * once it gets a hash tree.
     * @throws Exception
     */
    public void testExistingBlocksVerify() throws Exception {
        vf.setHashTree(null);
        vf.close();
        
        File outfile = new File("outfile");
        
        long wrote = 0;
        RandomAccessFile out = new RandomAccessFile(outfile, "rw");
        byte[] data = new byte[hashTree.getNodeSize()];
        for(; wrote < completeFile.length() / 2; ) {
            raf.read(data);
            out.write(data);
            wrote += hashTree.getNodeSize();
        }
        
        // null the rest of the file.
        for(long i = wrote; i < completeFile.length(); i++) {
            out.write(0);
        }
        
        out.close();
        vf.open(outfile);
        assertEquals(0, vf.getVerifiedBlockSize());
        vf.setScanForExistingBlocks(true, outfile.length());
        assertEquals(0, vf.getVerifiedBlockSize());
        vf.setHashTree(hashTree);
        Thread.sleep(1000);
        assertEquals(wrote, vf.getVerifiedBlockSize());
    }
}
