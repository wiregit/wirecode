package com.limegroup.gnutella.util;

import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Test;

import com.limegroup.gnutella.util.PatriciaTrie.KeyCreator;

public class PatriciaTrieTest extends BaseTestCase {

    public PatriciaTrieTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PatriciaTrieTest.class);
    }
    
    public void testSimple() {
        PatriciaTrie<Integer, String> intTrie = new PatriciaTrie<Integer, String>(new IntegerKeyCreator());
        
        intTrie.put(1, "One");
        intTrie.put(5, "Five");
        intTrie.put(4, "Four");
        intTrie.put(2, "Two");
        intTrie.put(3, "Three");
        intTrie.put(15, "Fifteen");
        intTrie.put(13, "Thirteen");
        intTrie.put(14, "Fourteen");
        intTrie.put(16, "Sixteen");
        
        intTrie.traverse(new Trie.Cursor<Integer, String>() {
            public boolean select(Entry<? extends Integer, ? extends String> entry) {
                System.out.println("CURSOR: Key: " + entry.getKey() + ", value: " + entry.getValue());
                return false;
            }
            
        });
        /*
        for(Map.Entry<Integer, String> entry : intTrie.entrySet()) {
            System.out.println("ITERATOR: Key: " + entry.getKey() + ", value: " + entry.getValue());
        }*/
        
        
        // c, p, l, t, k, a, y, r, u, o, w, i, e, 
        // x, q, b, j, s, n, v, g, h, m, z, f, d
        
        PatriciaTrie<Character, String> charTrie = new PatriciaTrie<Character, String>(new AlphaKeyCreator());
        charTrie.put('c', "c");
        charTrie.put('p', "p");
        charTrie.put('l', "l");
        charTrie.put('t', "t");
        charTrie.put('k', "k");
        charTrie.put('a', "a");
        charTrie.put('y', "y");
        charTrie.put('r', "r");
        charTrie.put('u', "u");
        charTrie.put('o', "o");
        charTrie.put('w', "w");
        charTrie.put('i', "i");
        charTrie.put('e', "e");
        charTrie.put('x', "x");
        charTrie.put('q', "q");
        charTrie.put('b', "b");
        charTrie.put('j', "j");
        charTrie.put('s', "s");
        charTrie.put('n', "n");
        charTrie.put('v', "v");
        charTrie.put('g', "g");
        charTrie.put('h', "h");
        charTrie.put('m', "m");
        charTrie.put('z', "z");
        charTrie.put('f', "f");
        charTrie.put('d', "d");
        

        charTrie.traverse(new Trie.Cursor<Character, String>() {
            public boolean select(Entry<? extends Character, ? extends String> entry) {
                System.out.println("CURSOR: Key: " + entry.getKey() + ", value: " + entry.getValue());
                return false;
            }
            
        });
        /*
        for(Map.Entry<Character, String> entry : charTrie.entrySet()) {
            System.out.println("ITERATOR: Key: " + entry.getKey() + ", value: " + entry.getValue());
        }*/
        
    }
    
    
    public static class IntegerKeyCreator implements KeyCreator<Integer> {

        public static final int[] createIntBitMask(final int bitCount) {
            int[] bits = new int[bitCount];
            for(int i = 0; i < bitCount; i++) {
                bits[i] = 1 << (bitCount - i - 1);
            }
            return bits;
        }
        
        private static final int[] BITS = createIntBitMask(32);
        
        public int length() {
            return 32;
        }

        public boolean isBitSet(Integer key, int bitIndex) {
            return (key & BITS[bitIndex]) != 0;
        }
        
        public int bitIndex(Integer key, Integer found) {       
            if(found == null)
                found = 0;
            
            boolean allNull = true;
            for (int i = 0; i < 32; i++) {
                int a = key & BITS[i];
                int b = found & BITS[i];
                
                if (allNull && a != 0) {
                    allNull = false;
                }
                
                if (a != b) {
                    return i;
                }
            }
            
            if (allNull) {
                return KeyCreator.NULL_BIT_KEY;
            }
            
            return KeyCreator.EQUAL_BIT_KEY;
        }
    }
    
    public static class AlphaKeyCreator implements KeyCreator<Character> {

        public static final int[] createIntBitMask(final int bitCount) {
            int[] bits = new int[bitCount];
            for(int i = 0; i < bitCount; i++) {
                bits[i] = 1 << (bitCount - i - 1);
            }
            return bits;
        }
        
        private static final int[] BITS = createIntBitMask(16);
        
        public int length() {
            return 16;
        }

        public boolean isBitSet(Character key, int bitIndex) {
            return (key & BITS[bitIndex]) != 0;
        }
        
        public int bitIndex(Character key, Character found) {       
            if(found == null)
                found = 0;
            
            boolean allNull = true;
            for (int i = 0; i < 16; i++) {
                int a = key & BITS[i];
                int b = found & BITS[i];
                
                if (allNull && a != 0) {
                    allNull = false;
                }
                
                if (a != b) {
                    return i;
                }
            }
            
            if (allNull) {
                return KeyCreator.NULL_BIT_KEY;
            }
            
            return KeyCreator.EQUAL_BIT_KEY;
        }
    }

}
