package com.limegroup.gnutella.tigertree;

import java.io.*;
import java.lang.reflect.*;

import junit.framework.Test;

import com.bitzi.util.Base32;

import com.limegroup.gnutella.downloader.Interval;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.UUID;
import com.limegroup.gnutella.dime.*;

import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.List;
import com.sun.java.util.collections.HashMap;

/**
 * Unit tests for HashTree
 */
public class HashTreeTest extends BaseTestCase {
    
    private static final String filename = 
     "com/limegroup/gnutella/mp3/mpg4_golem160x90first120.avi";
    private static final File file = CommonUtils.getResourceFile(filename);
    
    // urn & tigertree root from bitcollider
    private static final String sha1 = 
        "urn:sha1:UBJSGDTCVZDSBS4K3ZDQJV5VQ3WTBCOK";
    private static final String root32 =
        "IXVJNDJ7U3NCMZE5ZWBVCXSMWMFY4ZCXG5LUYAY";
        
    private HashTree hashTree;

    public HashTreeTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HashTreeTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }        

    public void testTHEX() throws Throwable {
        assertTrue(file.exists());
        URN urn = URN.createSHA1Urn(file);
        assertEquals(sha1, urn.toString());
                
        hashTree = createHashTree(file, urn);
        // most important test:
        // if we get the root hash right, the rest will be working, too
        assertEquals(root32, hashTree.getRootHash());
        assertEquals(4, hashTree.getDepth());
        {
            List allNodes = hashTree.getAllNodes();
            assertEquals(5, allNodes.size());
            List one, two, three, four, five;
            one = (List)allNodes.get(0);
            two = (List)allNodes.get(1);
            three = (List)allNodes.get(2);
            four = (List)allNodes.get(3);
            five = (List)allNodes.get(4);
            
            // tree looks like:
            //                 u (root)
            //               /         \
            //             t            s
            //          /      \         \
            //        q         r         s
            //     /     \   /    \      /  \
            //    l      m   n     o    p    k
            //   /\     /\  /\    /\   /\     \  
            //  a b    c d e f   g h  i j      k
            
            assertEquals(root32, Base32.encode((byte[])one.get(0)));
            assertEquals(2, two.size());
            assertEquals(3, three.size());
            assertEquals(6, four.size());
            assertEquals(11, five.size());
        }
        
        // Now make sure we can write this record out correctly.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        hashTree.write(baos);
        
        // Should be two DIME Records.
        ByteArrayInputStream in = new ByteArrayInputStream(baos.toByteArray());
        DIMEParser parser = new DIMEParser(in);
        DIMERecord xmlRecord = parser.nextRecord();
        DIMERecord treeRecord = parser.nextRecord();
        assertTrue(!parser.hasNext());
        
        UUID uuid = verifyXML(xmlRecord);
        verifyTree(treeRecord, uuid);
        
        
        // Make sure we can read the tree back in.
        HashTree treeFromNetwork = HashTree.createHashTree(
            new ByteArrayInputStream(baos.toByteArray()), sha1,
                                     root32, file.length()
        );
        
        assertEquals(hashTree.getDepth(), treeFromNetwork.getDepth());
        assertEquals(hashTree.getRootHash(), treeFromNetwork.getRootHash());

        File corrupt = new File("corruptFile");
        CommonUtils.copy(file, corrupt);
        assertTrue(corrupt.exists());
        // Now corrupt the copy.
        RandomAccessFile raf = new RandomAccessFile(corrupt, "rw");
        raf.seek(0);
        raf.write(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
        raf.close();
        
        // test getCorruptRanges()
        List franges = hashTree.getCorruptRanges(new FileInputStream(file));
        List cranges = hashTree.getCorruptRanges(new FileInputStream(corrupt));
        assertEquals(franges.size(), 0);
        assertEquals(cranges.size(), 1);
        
        List franges2 =
            treeFromNetwork.getCorruptRanges(new FileInputStream(file));
        List cranges2 =
             treeFromNetwork.getCorruptRanges(new FileInputStream(corrupt));
        assertEquals(franges2.size(), 0);
        assertEquals(cranges2.size(), 1);

        assertEquals(cranges.get(0), cranges2.get(0));
        assertEquals(((Interval)cranges.get(0)).low, 0);
    }
    
    private UUID verifyXML(DIMERecord xml) throws Throwable {
        // simple tests.
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, xml.getTypeId());
        assertEquals("text/xml", xml.getTypeString());
        assertEquals("", xml.getIdentifier());
        assertEquals(new byte[0], xml.getOptions());
        
        String data = new String(xml.getData(), "UTF-8");
        String current;
        String test = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<!DOCTYPE hashtree SYSTEM "
                    + "\"http://open-content.net/spec/thex/thex.dtd\">"
                    + "<hashtree>";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        test = "<file size='" + file.length() + "' ";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        test = "segmentsize='" + HashTree.BLOCK_SIZE + "'/>";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        test = "<digest algorithm='http://open-content.net/spec/digest/tiger'";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        test = " outputsize='24'/>";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        test = "<serializedtree depth='" + hashTree.getDepth() + "'";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        test = " type='http://open-content.net/spec/thex/breadthfirst'";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        test = " uri='";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        // the uri is the next 36 characters, grab it.
        String uri = data.substring(0, 36);
        data = data.substring(36);
        UUID uuid = new UUID(uri);
        
        test = "'/></hashtree>";
        current = data.substring(0, test.length());
        data = data.substring(test.length());
        assertEquals(test, current);
        
        return uuid;
    }

    private void verifyTree(DIMERecord tree, UUID uuid) throws Throwable {
        // simple tests.
        assertEquals(DIMERecord.TYPE_ABSOLUTE_URI, tree.getTypeId());
        assertEquals("http://open-content.net/spec/thex/breadthfirst",
            tree.getTypeString());
        assertEquals(uuid.toString(), tree.getIdentifier());
        assertEquals(new byte[0], tree.getOptions());
        
        byte[] data = tree.getData();
        int offset = 0;
        List allNodes = hashTree.getAllNodes();
        for(Iterator genIter = allNodes.iterator(); genIter.hasNext(); ) {
            for(Iterator i = ((List)genIter.next()).iterator(); i.hasNext();) {
                byte[] current = (byte[])i.next();
                for(int j = 0; j < current.length; j++)
                    assertEquals(data[offset++], current[j]);
            }
        }
        assertEquals(data.length, offset);
    }
    
    private HashTree createHashTree(File file, URN sha1) throws Throwable {
        Object ret = null;
        try {
            ret = PrivilegedAccessor.invokeMethod(
                HashTree.class, "createHashTree", 
                new Object[] { new Long(file.length()), new FileInputStream(file),
                            sha1 },
                new Class[] { long.class, InputStream.class, URN.class }
            );
        } catch(InvocationTargetException ite) {
            throw ite.getCause();
        }
        return (HashTree)ret;
    }
}
