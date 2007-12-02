package org.limewire.collection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class TreeStorageTest extends BaseTestCase {

    public TreeStorageTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(TreeStorageTest.class);
    }
    
    /**
     * Generator that does simple xor
     */
    private final NodeGenerator xorGen = new NodeGenerator() {
        public byte [] generate(byte [] left, byte [] right) {
            byte [] ret = new byte[left.length];
            for (int i = 0; i < left.length ; i ++)
                ret[i] = (byte)(left[i] ^ right[i]);
            return ret;
        }
    };

    private static Map<Integer, byte[]> createRandomTree(int size, NodeGenerator gen) {
        final int startOffset = (0x1 << TreeStorage.log2Ceil(size));
        Map<Integer, byte[]> tree = new HashMap<Integer, byte[]>();
        Random r = new Random();
        for (int i = 0; i < size ; i++) {
            byte [] b = new byte[20];
            r.nextBytes(b);
            tree.put(i+startOffset, b);
        }
        
        for (int i =  TreeStorage.log2Ceil(size); i >= 1; i--) {
            int offset = 0x1 << i;
            for (int j = offset ; j < Math.min(offset * 2, offset + size); j+=2) {
                byte [] left = tree.get(j);
                if (left == null)
                    continue;
                byte [] right = tree.get(j+1);
                byte [] data = right == null ? left : gen.generate(left, right); 
                tree.put(j / 2, data);
            }
        }
        return tree;
    }
    
    /**
     * Test basic functionality
     */
    public void testGeneral() throws Exception {
        
        int leafs = 8;
        // create a random tree
        Map<Integer, byte[]> tree = createRandomTree(leafs, xorGen);
        
        // test the TreeStorage
        TreeStorage storage = new TreeStorage(tree.get(1), xorGen, leafs);
        assertEquals(1, storage.getVerifiedNodes().size());
        assertEquals(0, storage.getUsedNodes().size());
        assertTrue(storage.getVerifiedNodes().contains(1)); // root always there
        
        // add nodes 3, 4, and 5
        
        // 3 alone does not get verified
        storage.add(3, tree.get(3));
        assertEquals(1,storage.getVerifiedNodes().size());
        
        // adding four does change things
        storage.add(4, tree.get(4));
        assertEquals(1,storage.getVerifiedNodes().size());
//        assert !storage.map.get(3).verified;
//        assert !storage.map.get(4).verified;
        
        // when we add 5 all nodes become verified
        storage.add(5, tree.get(5));
        assertTrue(storage.getVerifiedNodes().contains(3));
        assertTrue(storage.getVerifiedNodes().contains(4));
        assertTrue(storage.getVerifiedNodes().contains(5));
//        assert storage.map.get(3).verified;
//        assert storage.map.get(4).verified;
//        assert storage.map.get(5).verified;
        
        // when we add 8 and 9 node 4 will dissapear
        storage.add(8, tree.get(8));
        assertFalse(storage.getVerifiedNodes().contains(8));
        assertTrue(storage.getVerifiedNodes().contains(4));
//        assert !storage.map.get(8).verified;
//        assert storage.map.containsKey(4);
        
        storage.add(9, tree.get(9));
        assertTrue(storage.getVerifiedNodes().contains(8));
        assertTrue(storage.getVerifiedNodes().contains(9));
        assertFalse(storage.getVerifiedNodes().contains(4));
//        assert storage.map.get(8).verified;
//        assert storage.map.get(9).verified;
//        assert !storage.map.containsKey(4);
        
        
        // if we use 8 and 9 they will dissapear
        // and 4 will appear
        storage.used(8);
        assertTrue(storage.getVerifiedNodes().contains(8));
        assertTrue(storage.getVerifiedNodes().contains(9));
        assertFalse(storage.getVerifiedNodes().contains(4));
        
        storage.used(9);
        assertTrue(storage.getVerifiedNodes().contains(4));
        assertFalse(storage.getVerifiedNodes().contains(8));
        assertFalse(storage.getVerifiedNodes().contains(9));
//        assert storage.map.containsKey(4);
//        assert !storage.map.containsKey(8);
//        assert !storage.map.containsKey(9);
        assertTrue(storage.getUsedNodes().contains(4));
//        assert storage.map.get(4).used;
        
        // if we use 5 then 4 and 5 will dissapear
        // and 2 will appear
        assertFalse(storage.getVerifiedNodes().contains(2));
//        assert !storage.map.containsKey(2);
        storage.used(5);
        assertFalse(storage.getVerifiedNodes().contains(4));
        assertFalse(storage.getVerifiedNodes().contains(5));
        assertTrue(storage.getVerifiedNodes().contains(2));
        assertTrue(storage.getUsedNodes().contains(2));
//        assert !storage.map.containsKey(4);
//        assert !storage.map.containsKey(5);
//        assert storage.map.get(2).used;
//        assert storage.map.get(2).verified;
        assertTrue(Arrays.equals(tree.get(2),storage.get(2)));
    }
    
    public void testBottomLeafs() throws Exception {
        Map<Integer, byte[]> tree = createRandomTree(7, xorGen);
        TreeStorage storage = new TreeStorage(tree.get(1),xorGen, 7);
        
        // add all the bottom leafs
        for (int i = 8; i < 15; i++) {
            storage.add(i, tree.get(i));
        }
        
        // now use them all
        for (int i = 8; i < 15; i++) 
            storage.used(i);
        assertEquals(1,storage.getVerifiedNodes().size());
        assertEquals(1,storage.getUsedNodes().size());
        assertTrue(storage.getVerifiedNodes().containsAll(storage.getUsedNodes()));
        assertTrue(storage.getVerifiedNodes().contains(1));
//        assert storage.map.size() == 1;
//        assert storage.map.containsKey(1);
        
    }
    
    public void testAdvancedUsage() throws Exception {
        // more advanced usage - some nodes get used while others are verified
        // 12 nodes, leafs are 16-28
        Map<Integer, byte[]> tree = createRandomTree(12, xorGen);
        TreeStorage storage = new TreeStorage(tree.get(1),xorGen, 12);
        
        // add 3, 5, 9, 16
        storage.add(3, tree.get(3));
        storage.add(5, tree.get(5));
        storage.add(9, tree.get(9));
        storage.add(16, tree.get(16));
        
        assertFalse(storage.getVerifiedNodes().contains(3));
        assertFalse(storage.getVerifiedNodes().contains(5));
        assertFalse(storage.getVerifiedNodes().contains(9));
        assertFalse(storage.getVerifiedNodes().contains(16));
//        assert !storage.map.get(3).verified;
//        assert !storage.map.get(5).verified;
//        assert !storage.map.get(9).verified;
//        assert !storage.map.get(16).verified;
        
        // add broken 17, nothing changes
        assertFalse(storage.add(17, tree.get(16)));
        assertFalse(storage.getVerifiedNodes().contains(3));
        assertFalse(storage.getVerifiedNodes().contains(5));
        assertFalse(storage.getVerifiedNodes().contains(9));
        assertFalse(storage.getVerifiedNodes().contains(16));
        
        // add real 17, they all become verified
        assertTrue(storage.add(17, tree.get(17)));
//        assert storage.add(17, tree.get(17));
        assertTrue(storage.getVerifiedNodes().contains(3));
        assertTrue(storage.getVerifiedNodes().contains(5));
        assertTrue(storage.getVerifiedNodes().contains(9));
        assertTrue(storage.getVerifiedNodes().contains(16));
        assertTrue(storage.getVerifiedNodes().contains(17));
        assertEquals(6, storage.getVerifiedNodes().size());
//        assert storage.map.get(3).verified;
//        assert storage.map.get(5).verified;
//        assert storage.map.get(9).verified;
//        assert storage.map.get(16).verified;
//        assert storage.map.get(17).verified;
//        assert storage.map.size() == 6;
        
        // use 3, 5, 9, 16
        storage.used(3);
        storage.used(5);
        storage.used(9);
        storage.used(16);
        assertEquals(6, storage.getVerifiedNodes().size());
        
        // use 17 and only the root remains
        storage.used(17);
        assertEquals(1,storage.getVerifiedNodes().size());
        assertEquals(1,storage.getUsedNodes().size());
        assertTrue(storage.getVerifiedNodes().containsAll(storage.getUsedNodes()));
        assertTrue(storage.getVerifiedNodes().contains(1));
    }
    
}

