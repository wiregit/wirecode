package com.limegroup.gnutella.tigertree;

import java.io.*;

import junit.framework.Test;

import com.limegroup.gnutella.downloader.Interval;
import com.limegroup.gnutella.util.BaseTestCase;
import com.sun.java.util.collections.List;

/**
 * Unit tests for HashTree
 */
public class HashTreeTest extends BaseTestCase {

    public HashTreeTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HashTreeTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    

    // tests the HTTPDownloader's requestHashTree method
    public void testThex() throws Throwable {
        File file = new File("com/limegroup/gnutella/mp3/mpg4_golem160x90first120.avi");
        // sha1 for file from bitcollider
        String sha1 = "urn:sha1:UBJSGDTCVZDSBS4K3ZDQJV5VQ3WTBCOK";
        // tigertree root for file from bitcollider
        String root32 = "IXVJNDJ7U3NCMZE5ZWBVCXSMWMFY4ZCXG5LUYAY";
        
        HashTree tree = HashTree.createTestHashTree(file, sha1);

        // most important test:
        // if we get the root hash right, the rest will be working, too
        assertEquals(root32, tree.getRootHash());
        
        // test the reading and writing of hash tree
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        tree.getMessage().write(baos);
        
        HashTree tree2 = HashTree.createHashTree(new ByteArrayInputStream(baos.toByteArray()), sha1, root32, file.length());
        assertEquals(tree.getDepth(), tree2.getDepth());
        assertEquals(tree.getRootHash(), tree2.getRootHash());
        
        // test getCorruptRanges()
        File corrupt = new File("com/limegroup/gnutella/mp3/corruptFile.avi");
        
        List franges = tree.getCorruptRanges(new FileInputStream(file));
        List cranges = tree.getCorruptRanges(new FileInputStream(corrupt));
        assertEquals(franges.size(), 0);
        assertEquals(cranges.size(), 1);
        
        List franges2 = tree2.getCorruptRanges(new FileInputStream(file));
        List cranges2 = tree2.getCorruptRanges(new FileInputStream(corrupt));
        assertEquals(franges2.size(), 0);
        assertEquals(cranges2.size(), 1);

        assertEquals(cranges.get(0), cranges2.get(0));
        assertEquals(((Interval)cranges.get(0)).low, 0);
    }
}
