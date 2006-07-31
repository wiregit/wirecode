package com.limegroup.gnutella.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map.Entry;

import junit.framework.Test;

import com.limegroup.gnutella.util.PatriciaTrie.KeyAnalyzer;
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
    
    public void testCeilEntry() {
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
        
        Object[] results = new Object[] {
            'a', "a", 'b', "b", 'c', "c", 'd', "d", 'e', "e",
            'f', "f", 'g', "g", 'h', "h", 'i', "i", 'j', "j",
            'k', "k", 'l', "l", 'm', "m", 'n', "n", 'o', "o",
            'p', "p", 'q', "q", 'r', "r", 's', "s", 't', "t",
            'u', "u", 'v', "v", 'w', "w", 'x', "x", 'y', "y", 
            'z', "z"
        };
        
        for(int i = 0; i < results.length; i++) {
            Map.Entry<Character, String> found = charTrie.getCeilEntry((Character)results[i]);
            assertNotNull(found);
            assertEquals(results[i], found.getKey());
            assertEquals(results[++i], found.getValue());
        }
        
        // Remove some & try again...
        charTrie.remove('a');
        charTrie.remove('z');
        charTrie.remove('q');
        charTrie.remove('l');
        charTrie.remove('p');
        charTrie.remove('m');
        charTrie.remove('u');
        
        Map.Entry<Character, String> found = charTrie.getCeilEntry('u');
        assertNotNull(found);
        assertEquals((Character)'v', found.getKey());
        
        found = charTrie.getCeilEntry('a');
        assertNotNull(found);
        assertEquals((Character)'b', found.getKey());
        
        found = charTrie.getCeilEntry('z');
        assertNull(found);
        
        found = charTrie.getCeilEntry('q');
        assertNotNull(found);
        assertEquals((Character)'r', found.getKey());
        
        found = charTrie.getCeilEntry('l');
        assertNotNull(found);
        assertEquals((Character)'n', found.getKey());
        
        found = charTrie.getCeilEntry('p');
        assertNotNull(found);
        assertEquals((Character)'r', found.getKey());
        
        found = charTrie.getCeilEntry('m');
        assertNotNull(found);
        assertEquals((Character)'n', found.getKey());
        
        found = charTrie.getCeilEntry('\0');
        assertNotNull(found);
        assertEquals((Character)'b', found.getKey());
        
        charTrie.put('\0', "");
        found = charTrie.getCeilEntry('\0');
        assertNotNull(found);
        assertEquals((Character)'\0', found.getKey());      
    }
    
    public void testPrecedingEntry() {
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
        
        Object[] results = new Object[] {
            'a', "a", 'b', "b", 'c', "c", 'd', "d", 'e', "e",
            'f', "f", 'g', "g", 'h', "h", 'i', "i", 'j', "j",
            'k', "k", 'l', "l", 'm', "m", 'n', "n", 'o', "o",
            'p', "p", 'q', "q", 'r', "r", 's', "s", 't', "t",
            'u', "u", 'v', "v", 'w', "w", 'x', "x", 'y', "y", 
            'z', "z"
        };
        
        for(int i = 0; i < results.length; i+=2) {
            //System.out.println("Looking for: " + results[i]);
            Map.Entry<Character, String> found = charTrie.getPrecedingEntry((Character)results[i]);
            if(i == 0) {
                assertNull(found);
            } else {
                assertNotNull(found);
                assertEquals(results[i-2], found.getKey());
                assertEquals(results[i-1], found.getValue());
            }
        }

        Map.Entry<Character, String> found = charTrie.getPrecedingEntry((char)('z' + 1));
        assertNotNull(found);
        assertEquals((Character)'z', found.getKey());
        
        
        // Remove some & try again...
        charTrie.remove('a');
        charTrie.remove('z');
        charTrie.remove('q');
        charTrie.remove('l');
        charTrie.remove('p');
        charTrie.remove('m');
        charTrie.remove('u');
        
        found = charTrie.getPrecedingEntry('u');
        assertNotNull(found);
        assertEquals((Character)'t', found.getKey());
        
        found = charTrie.getPrecedingEntry('v');
        assertNotNull(found);
        assertEquals((Character)'t', found.getKey());
        
        found = charTrie.getPrecedingEntry('a');
        assertNull(found);
        
        found = charTrie.getPrecedingEntry('z');
        assertNotNull(found);
        assertEquals((Character)'y', found.getKey());
        
        found = charTrie.getPrecedingEntry((char)('z'+1));
        assertNotNull(found);
        assertEquals((Character)'y', found.getKey());
        
        found = charTrie.getPrecedingEntry('q');
        assertNotNull(found);
        assertEquals((Character)'o', found.getKey());
        
        found = charTrie.getPrecedingEntry('r');
        assertNotNull(found);
        assertEquals((Character)'o', found.getKey());
        
        found = charTrie.getPrecedingEntry('p');
        assertNotNull(found);
        assertEquals((Character)'o', found.getKey());
        
        found = charTrie.getPrecedingEntry('l');
        assertNotNull(found);
        assertEquals((Character)'k', found.getKey());
        
        found = charTrie.getPrecedingEntry('m');
        assertNotNull(found);
        assertEquals((Character)'k', found.getKey());
        
        found = charTrie.getPrecedingEntry('\0');
        assertNull(found);
        
        charTrie.put('\0', "");
        found = charTrie.getPrecedingEntry('\0');
        assertNull(found);      
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
                'd', "d", 'e', "e", 'f', "f", 'g', "g",
                'a', "a", 'b', "b", 'c', "c",  
                'l', "l", 'm', "m", 'n', "n", 'o', "o",
                'h', "h", 'i', "i", 'j', "j", 'k', "k", 
                't', "t", 'u', "u", 'v', "v", 'w', "w",
                'p', "p", 'q', "q", 'r', "r", 's', "s", 
                'x', "x", 'y', "y", 'z', "z");
                
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
        
        cursor.starting();
        charTrie.traverse(cursor);
        cursor.finished();
        
        // Test removing both an internal & external node.
        // 'm' is an example External node in this Trie, and 'p' is an internal.
        
        assertEquals(26, charTrie.size());
        
        Object[] toRemove = new Object[] { 'g', 'd', 'e', 'm', 'p', 'q', 'r', 's' };
        cursor.addToRemove(toRemove);
        
        cursor.starting();
        charTrie.traverse(cursor);
        cursor.finished();
            
        assertEquals(26 - toRemove.length, charTrie.size());

        cursor.starting();
        charTrie.traverse(cursor);
        cursor.finished();
        
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
        
        Object[] toRemove = new Object[] { 'e', 'm', 'p', 'q', 'r', 's' };
        
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
    
    public void testHamlet() throws Exception {
        // Make sure that Hamlet is read & stored in the same order as a SortedSet.
        List<String> original = new ArrayList();
        List<String> control = new ArrayList();
        SortedMap<String, String> sortedControl = new TreeMap<String, String>();
        PatriciaTrie<String, String> trie = new PatriciaTrie<String, String>(new CharSequenceKeyAnalyzer());
        
        File hamlet = CommonUtils.getResourceFile("com/limegroup/gnutella/util/hamlet.txt");
        BufferedReader reader = new BufferedReader(new FileReader(hamlet));
        String read = null;
        while( (read = reader.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(read);
            while(st.hasMoreTokens()) {
                String token = st.nextToken();
                original.add(token);
                sortedControl.put(token, token);
                trie.put(token, token);
            }
        }
        control.addAll(sortedControl.values());

        assertEquals(control.size(), sortedControl.size());
        assertEquals(sortedControl.size(), trie.size());
        Iterator<String> iter = trie.values().iterator();
        for(int i = 0; i < control.size(); i++) {
            assertEquals(control.get(i), iter.next());
        }
        
        Random rnd = new Random();
        int item = 0;
        iter = trie.values().iterator();
        int removed = 0;
        for(; item < control.size(); item++) {
            assertEquals(control.get(item), iter.next());
            if(rnd.nextBoolean()) {
                iter.remove();
                removed++;
            }
        }
        
        assertEquals(control.size(), item);
        assertGreaterThan(0, removed);
        assertEquals(control.size(), trie.size() + removed);
        
        // reset hamlet
        trie.clear();
        for(int i = 0; i < original.size(); i++) 
            trie.put(original.get(i), original.get(i));
        
        assertEquals(sortedControl.values().toArray(), trie.values().toArray());
        assertEquals(sortedControl.keySet().toArray(), trie.keySet().toArray());
        assertEquals(sortedControl.entrySet().toArray(), trie.entrySet().toArray());
        
        assertEquals(sortedControl.firstKey(), trie.firstKey());
        assertEquals(sortedControl.lastKey(), trie.lastKey());
        
        SortedMap<String, String> sub = trie.headMap(control.get(523));
        assertEquals(523, sub.size());
        for(int i = 0; i < control.size(); i++) {
            if(i < 523)
                assertTrue(sub.containsKey(control.get(i)));
            else
                assertFalse(sub.containsKey(control.get(i)));
        }
        // Too slow to check values on all, so just do a few.
        assertTrue(sub.containsValue(control.get(522)));
        assertFalse(sub.containsValue(control.get(523)));
        assertFalse(sub.containsValue(control.get(524)));
        
        try {
            sub.headMap(control.get(524));
            fail("should have thrown IAE");
        } catch(IllegalArgumentException expected) {}
        
        assertEquals(sub.lastKey(), control.get(522));
        assertEquals(sub.firstKey(), control.get(0));
        
        sub = sub.tailMap(control.get(234));
        assertEquals(289, sub.size());
        assertEquals(control.get(234), sub.firstKey());
        assertEquals(control.get(522), sub.lastKey());
        for(int i = 0; i < control.size(); i++) {
            if(i < 523 && i > 233)
                assertTrue(sub.containsKey(control.get(i)));
            else
                assertFalse(sub.containsKey(control.get(i)));
        }

        try {
            sub.tailMap(control.get(232));
            fail("should have thrown IAE");
        } catch(IllegalArgumentException expected) {}
        
        sub = sub.subMap(control.get(300), control.get(400));
        assertEquals(100, sub.size());
        assertEquals(control.get(300), sub.firstKey());
        assertEquals(control.get(399), sub.lastKey());
        
        for(int i = 0; i < control.size(); i++) {
            if(i < 400 && i > 299)
                assertTrue(sub.containsKey(control.get(i)));
            else
                assertFalse(sub.containsKey(control.get(i)));
        }
    }
    
    public void testVariableLengthKeys() {
        PatriciaTrie<String, String> trie 
            = new PatriciaTrie<String, String>(new CharSequenceKeyAnalyzer());
        
        final String[] keys = new String[]{
                "Albert", "Xavier", "XyZ", "Anna", "Alien", "Alberto",
                "Alberts", "Allie", "Alliese", "Alabama", "Banane",
                "Blabla", "Amber", "Ammun", "Akka", "Akko", "Albertoo",
                "Amma"
        };

        for (String key : keys) {
            trie.put(key, key);
        }
        
        SortedMap map = trie.getPrefixedBy("Al");
        System.out.println("For Al: " + map);
        
        map = trie.getPrefixedBy("Albert");
        System.out.println("For Albert: " + map);
        
        map = trie.getPrefixedBy("Alberto");
        System.out.println("For Alberto: " + map);
        
        map = trie.getPrefixedBy("X");
        System.out.println("For X: " + map);
        
        map = trie.getPrefixedBy("An");
        System.out.println("For An: " + map);
        
        map = trie.getPrefixedBy("Ban");
        System.out.println("For Ban: " + map);
        
        map = trie.getPrefixedBy("Am");
        System.out.println("For Am: " + map);
        
        map = trie.getPrefixedBy("Ak");
        System.out.println("For Ak: " + map);
        
        map = trie.getPrefixedBy("Ab");
        System.out.println("For Ab: " + map);
        
        map = trie.getPrefixedBy("Albertooo");
        System.out.println("For Albertooo: " + map);
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
          //  System.out.println("Scanning: " + entry.getKey());
            assertEquals(keys.get(index), entry.getKey());
            assertEquals(values.get(index), entry.getValue());
            index++;
            
            if(toRemove.contains(entry.getKey())) {
              // System.out.println("Removing: " + entry.getKey());
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

    private static class IntegerKeyCreator implements KeyAnalyzer<Integer> {

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

        public int bitIndex(int startAt, Integer key, Integer found) {
            if (found == null)
                found = 0;

            boolean allNull = true;
            for (int i = startAt; i < 32; i++) {
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
                return KeyAnalyzer.NULL_BIT_KEY;
            }

            return KeyAnalyzer.EQUAL_BIT_KEY;
        }

        public int compare(Integer o1, Integer o2) {
            return o1.compareTo(o2);
        }

        public int bitsPerElement() {
            return 1;
        }
    }

    private static class AlphaKeyCreator implements KeyAnalyzer<Character> {

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

        public int bitIndex(int startAt, Character key, Character found) {
            if (found == null)
                found = 0;

            boolean allNull = true;
            for (int i = startAt; i < 16; i++) {
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
                return KeyAnalyzer.NULL_BIT_KEY;
            }

            return KeyAnalyzer.EQUAL_BIT_KEY;
        }

        public int compare(Character o1, Character o2) {
            return o1.compareTo(o2);
        }

        public int bitsPerElement() {
            return 1;
        }
        
        
    }

}
