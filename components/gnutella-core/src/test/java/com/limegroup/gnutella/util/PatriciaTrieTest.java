package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Test;

import com.limegroup.gnutella.util.PatriciaTrie.KeyCreator;
import com.limegroup.gnutella.util.Trie.Cursor;

public class PatriciaTrieTest extends BaseTestCase {

    public PatriciaTrieTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PatriciaTrieTest.class);
    }

    public void testSimple() {
        PatriciaTrie<Integer, String> intTrie = new PatriciaTrie<Integer, String>(new IntegerKeyCreator());
        assertTrue(intTrie.isEmpty());
        assertEquals(0, intTrie.size());
        
        intTrie.put(1, "One");
        assertFalse(intTrie.isEmpty());
        assertEquals(1, intTrie.size());
        
        assertEquals("One", intTrie.remove(1));
        assertNull(intTrie.remove(1));
        assertTrue(intTrie.isEmpty());
        assertEquals(0, intTrie.size());
        
        intTrie.put(1, "One");
        assertEquals("One", intTrie.get(1));
        assertEquals("One", intTrie.put(1, "NotOne"));
        assertEquals(1, intTrie.size());
        assertEquals("NotOne", intTrie.get(1));
        assertEquals("NotOne", intTrie.remove(1));
        assertNull(intTrie.put(1, "One"));
    }
    
    public void testIteration() {
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
        
        TestCursor cursor = new TestCursor(
                1, "One", 2, "Two", 3, "Three", 4, "Four", 5, "Five", 13, "Thirteen",
                14, "Fourteen", 15, "Fifteen", 16, "Sixteen");

        cursor.starting();
        intTrie.traverse(cursor);
        cursor.finished();
        
        cursor.starting();
        for (Map.Entry<Integer, String> entry : intTrie.entrySet())
            cursor.select(entry);
        cursor.finished();
        
        cursor.starting();
        for (Integer integer : intTrie.keySet())
            cursor.checkKey(integer);
        cursor.finished();
        
        cursor.starting();
        for (String string : intTrie.values())
            cursor.checkValue(string);
        cursor.finished();

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
        cursor = new TestCursor('a', "a", 'b', "b", 'c', "c", 'd', "d", 'e', "e",
                'f', "f", 'g', "g", 'h', "h", 'i', "i", 'j', "j",
                'k', "k", 'l', "l", 'm', "m", 'n', "n", 'o', "o",
                'p', "p", 'q', "q", 'r', "r", 's', "s", 't', "t",
                'u', "u", 'v', "v", 'w', "w", 'x', "x", 'y', "y", 
                'z', "z");

        cursor.starting();
        charTrie.traverse(cursor);
        cursor.finished();

        cursor.starting();
        for (Map.Entry<Character, String> entry : charTrie.entrySet())
            cursor.select(entry);
        cursor.finished();
        
        cursor.starting();
        for (Character character : charTrie.keySet())
            cursor.checkKey(character);
        cursor.finished();
        
        cursor.starting();
        for (String string : charTrie.values())
            cursor.checkValue(string);
        cursor.finished();

    }
    
    private static class TestCursor implements Cursor<Object, Object> {
        private List<Object> keys;
        private List<Object> values;
        private int index = 0;
        
        TestCursor(Object... objects) {
            if(objects.length % 2 != 0)
                throw new IllegalArgumentException("must be * 2");
            
            keys = new ArrayList<Object>(objects.length / 2);
            values = new ArrayList<Object>(keys.size());
            for(int i = 0; i < objects.length; i++) {
                keys.add(objects[i]);
                values.add(objects[++i]);
            }
        }
        
        void starting() {
            index = 0;
        }
        
        public void checkKey(Object k) {
            assertEquals(keys.get(index++), k);
        }
        
        public void checkValue(Object o) {
            assertEquals(values.get(index++), o);
        }

        public boolean select(Entry<? extends Object, ? extends Object> entry) {
            assertEquals(keys.get(index), entry.getKey());
            assertEquals(values.get(index), entry.getValue());
            index++;
            return false;
        }
        
        void finished() {
            assertEquals(keys.size(), index);
        }
    }

    private static class IntegerKeyCreator implements KeyCreator<Integer> {

        public static final int[] createIntBitMask(final int bitCount) {
            int[] bits = new int[bitCount];
            for (int i = 0; i < bitCount; i++) {
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
            if (found == null)
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

    private static class AlphaKeyCreator implements KeyCreator<Character> {

        public static final int[] createIntBitMask(final int bitCount) {
            int[] bits = new int[bitCount];
            for (int i = 0; i < bitCount; i++) {
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
            if (found == null)
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
