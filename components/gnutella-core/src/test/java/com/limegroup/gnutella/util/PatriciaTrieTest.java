package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
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
    
    // TODO: see how select is supposed to work after it passes up it's
    //       expected entry in the cursor
    public void testSelect() {
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
        TestCursor cursor = new TestCursor(
                'd', "d", 'e', "e",
                'f', "f", 'g', "g", 'a', "a", 'b', "b", 'c', "c",  
                'h', "h", 'i', "i", 'j', "j",
                'k', "k", 'l', "l", 'm', "m", 'n', "n", 'o', "o",
                'p', "p", 'q', "q", 'r', "r", 's', "s", 't', "t",
                'u', "u", 'v', "v", 'w', "w", 'x', "x", 'y', "y", 
                'z', "z");
                
        assertEquals(26, charTrie.size());
        
        cursor.starting();
        charTrie.select('d', cursor);
        cursor.finished();
    }
    
    public void testTraverseCursorRemove() {
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
        TestCursor cursor = new TestCursor('a', "a", 'b', "b", 'c', "c", 'd', "d", 'e', "e",
                'f', "f", 'g', "g", 'h', "h", 'i', "i", 'j', "j",
                'k', "k", 'l', "l", 'm', "m", 'n', "n", 'o', "o",
                'p', "p", 'q', "q", 'r', "r", 's', "s", 't', "t",
                'u', "u", 'v', "v", 'w', "w", 'x', "x", 'y', "y", 
                'z', "z");
        
        // Test removing both an internal & external node.
        // 'm' is an example External node in this Trie, and 'p' is an internal.
        
        assertEquals(26, charTrie.size());
        
        Object[] toRemove = new Object[] { 'm', 'p', 'q', 'r', 's' };
        cursor.addToRemove(toRemove);
        
        cursor.starting();
        charTrie.traverse(cursor);
        cursor.finished();
            
        assertEquals(26 - toRemove.length, charTrie.size());
        
        cursor.starting();
        for(Iterator<Map.Entry<Character, String>> i = charTrie.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<Character,String> entry = i.next();
            cursor.select(entry);
            if(Arrays.asList(toRemove).contains(entry.getKey()))
                fail("got an: " + entry);    
        }
        cursor.finished();
    }
    
    public void testSelectCursorRemove() {
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
        
        // only the path it'll take to find 'h'.
        TestCursor cursor = new TestCursor('h', "h", 'i', "i", 'j', "j",
                'k', "k", 'l', "l", 'm', "m", 'n', "n", 'o', "o",
                'p', "p");
        
        // Test removing both an internal & external node.
        // 'm' is an example External node in this Trie, and 'p' is an internal.
        
        assertEquals(26, charTrie.size());
        
        Object[] toRemove = new Object[] { 'p', 'k', 'i' };
        cursor.addToRemove(toRemove);
        
        cursor.selectFor('p');
        cursor.starting();
        Map.Entry<Character, String> result = charTrie.select('h', cursor);
        cursor.finished();
        
        assertEquals(new Character('p'), result.getKey());
        assertEquals("p", result.getValue());
            
        assertEquals(26 - toRemove.length, charTrie.size());
        
        // get a full cursor now
        cursor = new TestCursor('a', "a", 'b', "b", 'c', "c", 'd', "d", 'e', "e",
                'f', "f", 'g', "g", 'h', "h", 'i', "i", 'j', "j",
                'k', "k", 'l', "l", 'm', "m", 'n', "n", 'o', "o",
                'p', "p", 'q', "q", 'r', "r", 's', "s", 't', "t",
                'u', "u", 'v', "v", 'w', "w", 'x', "x", 'y', "y", 
                'z', "z");
        cursor.remove(toRemove);
        cursor.starting();
        for(Iterator<Map.Entry<Character, String>> i = charTrie.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<Character,String> entry = i.next();
            cursor.select(entry);
            if(Arrays.asList(toRemove).contains(entry.getKey()))
                fail("got an: " + entry);    
        }
        cursor.finished();
    }
    
    
    public void testIteratorRemove() {
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
        TestCursor cursor = new TestCursor('a', "a", 'b', "b", 'c', "c", 'd', "d", 'e', "e",
                'f', "f", 'g', "g", 'h', "h", 'i', "i", 'j', "j",
                'k', "k", 'l', "l", 'm', "m", 'n', "n", 'o', "o",
                'p', "p", 'q', "q", 'r', "r", 's', "s", 't', "t",
                'u', "u", 'v', "v", 'w', "w", 'x', "x", 'y', "y", 
                'z', "z");
        
        // Test removing both an internal & external node.
        // 'm' is an example External node in this Trie, and 'p' is an internal.
        
        assertEquals(26, charTrie.size());
        
        Object[] toRemove = new Object[] { 'm', 'p', 'q', 'r', 's' };
        
        cursor.starting();
        for(Iterator<Map.Entry<Character, String>> i = charTrie.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<Character,String> entry = i.next();
            cursor.select(entry);
            if(Arrays.asList(toRemove).contains(entry.getKey()))
                i.remove();            
        }
        cursor.finished();
            
        assertEquals(26 - toRemove.length, charTrie.size());
        
        cursor.remove(toRemove);

        cursor.starting();
        for(Iterator<Map.Entry<Character, String>> i = charTrie.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<Character,String> entry = i.next();
            cursor.select(entry);
            if(Arrays.asList(toRemove).contains(entry.getKey()))
                fail("got an: " + entry);    
        }
        cursor.finished();
    }
    
    public void testVariableLengthKeys() {
        PatriciaTrie<String, String> trie 
            = new PatriciaTrie<String, String>(new CharSequenceKeyCreator());
        
        String[] keys = { 
                "a", "aa", "aaa", "aaaa", "aaaaa",
                "b", "bb", "bbb", "bbbb", "bbbbb"
        };
        
        trie.put("", "empty");

        for (String key : keys) {
            trie.put(key, key);
        }
        
        
        System.out.println(trie);
    }
    
    private static class TestCursor implements Cursor<Object, Object> {
        private List<Object> keys;
        private List<Object> values;
        private Object selectFor;
        private List<Object> toRemove;
        private int index = 0;
        
        TestCursor(Object... objects) {
            if(objects.length % 2 != 0)
                throw new IllegalArgumentException("must be * 2");
            
            keys = new ArrayList<Object>(objects.length / 2);
            values = new ArrayList<Object>(keys.size());
            toRemove = Collections.emptyList();
            for(int i = 0; i < objects.length; i++) {
                keys.add(objects[i]);
                values.add(objects[++i]);
            }
        }
        
        void selectFor(Object object) {
            selectFor = object;
        }
        
        void addToRemove(Object... objects) {
            toRemove = new ArrayList<Object>(Arrays.asList(objects));
        }
        
        void remove(Object... objects) {
            for(int i = 0; i < objects.length; i++) {
                int idx = keys.indexOf(objects[i]);
                keys.remove(idx);
                values.remove(idx);
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

        public SelectStatus select(Entry<? extends Object, ? extends Object> entry) {
            System.out.println("Scanning: " + entry.getKey());
            assertEquals(keys.get(index), entry.getKey());
            assertEquals(values.get(index), entry.getValue());
            index++;
            
            if(toRemove.contains(entry.getKey())) {
                System.out.println("Removing: " + entry.getKey());
                index--;
                keys.remove(index);
                values.remove(index);
                toRemove.remove(entry.getKey());
                return SelectStatus.REMOVE;
            } 
            
            if(selectFor != null && selectFor.equals(entry.getKey()))
                return SelectStatus.EXIT;
            else
                return SelectStatus.CONTINUE;
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

        public int length(Integer key) {
            return 32;
        }

        public boolean isBitSet(Integer key, int keyLength, int bitIndex) {
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

        public int length(Character key) {
            return 16;
        }

        public boolean isBitSet(Character key, int keyLength, int bitIndex) {
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
