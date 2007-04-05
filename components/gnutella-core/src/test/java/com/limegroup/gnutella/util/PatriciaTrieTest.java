package com.limegroup.gnutella.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.limegroup.gnutella.util.PatriciaTrie.Cursor;
import com.limegroup.gnutella.util.PatriciaTrie.KeyAnalyzer;

import junit.framework.Test;


public class PatriciaTrieTest extends BaseTestCase {

    public PatriciaTrieTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PatriciaTrieTest.class);
    }

    public void testSimple() {
        PatriciaTrie intTrie = new PatriciaTrie(new IntegerKeyCreator());
        assertTrue(intTrie.isEmpty());
        assertEquals(0, intTrie.size());
        
        intTrie.put(new Integer(1), "One");
        assertFalse(intTrie.isEmpty());
        assertEquals(1, intTrie.size());
        
        assertEquals("One", intTrie.remove(new Integer(1)));
        assertNull(intTrie.remove(new Integer(1)));
        assertTrue(intTrie.isEmpty());
        assertEquals(0, intTrie.size());
        
        intTrie.put(new Integer(1), "One");
        assertEquals("One", intTrie.get(new Integer(1)));
        assertEquals("One", intTrie.put(new Integer(1), "NotOne"));
        assertEquals(1, intTrie.size());
        assertEquals("NotOne", intTrie.get(new Integer(1)));
        assertEquals("NotOne", intTrie.remove(new Integer(1)));
        assertNull(intTrie.put(new Integer(1), "One"));
    }
    
    public void testCeilingEntry() {
        PatriciaTrie charTrie = new PatriciaTrie(new AlphaKeyCreator());
        charTrie.put(new Character('c'), "c");
        charTrie.put(new Character('p'), "p");
        charTrie.put(new Character('l'), "l");
        charTrie.put(new Character('t'), "t");
        charTrie.put(new Character('k'), "k");
        charTrie.put(new Character('a'), "a");
        charTrie.put(new Character('y'), "y");
        charTrie.put(new Character('r'), "r");
        charTrie.put(new Character('u'), "u");
        charTrie.put(new Character('o'), "o");
        charTrie.put(new Character('w'), "w");
        charTrie.put(new Character('i'), "i");
        charTrie.put(new Character('e'), "e");
        charTrie.put(new Character('x'), "x");
        charTrie.put(new Character('q'), "q");
        charTrie.put(new Character('b'), "b");
        charTrie.put(new Character('j'), "j");
        charTrie.put(new Character('s'), "s");
        charTrie.put(new Character('n'), "n");
        charTrie.put(new Character('v'), "v");
        charTrie.put(new Character('g'), "g");
        charTrie.put(new Character('h'), "h");
        charTrie.put(new Character('m'), "m");
        charTrie.put(new Character('z'), "z");
        charTrie.put(new Character('f'), "f");
        charTrie.put(new Character('d'), "d");
        
        Object[] results = new Object[] {
                new Character('a'), "a", new Character('b'), "b", 
                new Character('c'), "c", 
                new Character('d'), "d", new Character('e'), "e",
                new Character('f'), "f", new Character('g'), "g", new Character('h'), "h", new Character('i'), "i", new Character('j'), "j",
                new Character('k'), "k", new Character('l'), "l", new Character('m'), "m", new Character('n'), "n", new Character('o'), "o",
                new Character('p'), "p", new Character('q'), "q", new Character('r'), "r", new Character('s'), "s", new Character('t'), "t",
                new Character('u'), "u", new Character('v'), "v", new Character('w'), "w", new Character('x'), "x", new Character('y'), "y", 
                new Character('z'), "z"
        };
        
        for(int i = 0; i < results.length; i++) {
            Map.Entry found = charTrie.ceilingEntry((Character)results[i]);
            assertNotNull(found);
            assertEquals(results[i], found.getKey());
            assertEquals(results[++i], found.getValue());
        }
        
        // Remove some & try again...
        charTrie.remove(new Character('a'));
        charTrie.remove(new Character('z'));
        charTrie.remove(new Character('q'));
        charTrie.remove(new Character('l'));
        charTrie.remove(new Character('p'));
        charTrie.remove(new Character('m'));
        charTrie.remove(new Character('u'));
        
        Map.Entry found = charTrie.ceilingEntry(new Character('u'));
        assertNotNull(found);
        assertEquals(new Character('v'), found.getKey());
        
        found = charTrie.ceilingEntry(new Character('a'));
        assertNotNull(found);
        assertEquals(new Character('b'), found.getKey());
        
        found = charTrie.ceilingEntry(new Character('z'));
        assertNull(found);
        
        found = charTrie.ceilingEntry(new Character('q'));
        assertNotNull(found);
        assertEquals(new Character('r'), found.getKey());
        
        found = charTrie.ceilingEntry(new Character('l'));
        assertNotNull(found);
        assertEquals(new Character('n'), found.getKey());
        
        found = charTrie.ceilingEntry(new Character('p'));
        assertNotNull(found);
        assertEquals(new Character('r'), found.getKey());
        
        found = charTrie.ceilingEntry(new Character('m'));
        assertNotNull(found);
        assertEquals(new Character('n'), found.getKey());
        
        found = charTrie.ceilingEntry(new Character('\0'));
        assertNotNull(found);
        assertEquals(new Character('b'), found.getKey());
        
        charTrie.put(new Character('\0'), "");
        found = charTrie.ceilingEntry(new Character('\0'));
        assertNotNull(found);
        assertEquals(new Character('\0'), found.getKey());      
    }
    
    public void testLowerEntry() {
        PatriciaTrie charTrie = new PatriciaTrie(new AlphaKeyCreator());
        charTrie.put(new Character('c'), "c");
        charTrie.put(new Character('p'), "p");
        charTrie.put(new Character('l'), "l");
        charTrie.put(new Character('t'), "t");
        charTrie.put(new Character('k'), "k");
        charTrie.put(new Character('a'), "a");
        charTrie.put(new Character('y'), "y");
        charTrie.put(new Character('r'), "r");
        charTrie.put(new Character('u'), "u");
        charTrie.put(new Character('o'), "o");
        charTrie.put(new Character('w'), "w");
        charTrie.put(new Character('i'), "i");
        charTrie.put(new Character('e'), "e");
        charTrie.put(new Character('x'), "x");
        charTrie.put(new Character('q'), "q");
        charTrie.put(new Character('b'), "b");
        charTrie.put(new Character('j'), "j");
        charTrie.put(new Character('s'), "s");
        charTrie.put(new Character('n'), "n");
        charTrie.put(new Character('v'), "v");
        charTrie.put(new Character('g'), "g");
        charTrie.put(new Character('h'), "h");
        charTrie.put(new Character('m'), "m");
        charTrie.put(new Character('z'), "z");
        charTrie.put(new Character('f'), "f");
        charTrie.put(new Character('d'), "d");
        
        Object[] results = new Object[] {
                new Character('a'), "a", new Character('b'), "b", 
                new Character('c'), "c", 
                new Character('d'), "d", new Character('e'), "e",
                new Character('f'), "f", new Character('g'), "g", new Character('h'), "h", new Character('i'), "i", new Character('j'), "j",
                new Character('k'), "k", new Character('l'), "l", new Character('m'), "m", new Character('n'), "n", new Character('o'), "o",
                new Character('p'), "p", new Character('q'), "q", new Character('r'), "r", new Character('s'), "s", new Character('t'), "t",
                new Character('u'), "u", new Character('v'), "v", new Character('w'), "w", new Character('x'), "x", new Character('y'), "y", 
                new Character('z'), "z"
        };
        
        for(int i = 0; i < results.length; i+=2) {
            //System.out.println("Looking for: " + results[i]);
            Map.Entry found = charTrie.lowerEntry((Character)results[i]);
            if(i == 0) {
                assertNull(found);
            } else {
                assertNotNull(found);
                assertEquals(results[i-2], found.getKey());
                assertEquals(results[i-1], found.getValue());
            }
        }

        Map.Entry found = charTrie.lowerEntry(new Character((char)('z' + 1)));
        assertNotNull(found);
        assertEquals((new Character('z')), found.getKey());
        
        
        // Remove some & try again...
        charTrie.remove(new Character('a'));
        charTrie.remove(new Character('z'));
        charTrie.remove(new Character('q'));
        charTrie.remove(new Character('l'));
        charTrie.remove(new Character('p'));
        charTrie.remove(new Character('m'));
        charTrie.remove(new Character('u'));
        
        found = charTrie.lowerEntry(new Character('u'));
        assertNotNull(found);
        assertEquals(new Character('t'), found.getKey());
        
        found = charTrie.lowerEntry(new Character('v'));
        assertNotNull(found);
        assertEquals(new Character('t'), found.getKey());
        
        found = charTrie.lowerEntry(new Character('a'));
        assertNull(found);
        
        found = charTrie.lowerEntry(new Character('z'));
        assertNotNull(found);
        assertEquals(new Character('y'), found.getKey());
        
        found = charTrie.lowerEntry(new Character((char)('z'+1)));
        assertNotNull(found);
        assertEquals(new Character('y'), found.getKey());
        
        found = charTrie.lowerEntry(new Character('q'));
        assertNotNull(found);
        assertEquals(new Character('o'), found.getKey());
        
        found = charTrie.lowerEntry(new Character('r'));
        assertNotNull(found);
        assertEquals(new Character('o'), found.getKey());
        
        found = charTrie.lowerEntry(new Character('p'));
        assertNotNull(found);
        assertEquals(new Character('o'), found.getKey());
        
        found = charTrie.lowerEntry(new Character('l'));
        assertNotNull(found);
        assertEquals(new Character('k'), found.getKey());
        
        found = charTrie.lowerEntry(new Character('m'));
        assertNotNull(found);
        assertEquals(new Character('k'), found.getKey());
        
        found = charTrie.lowerEntry(new Character('\0'));
        assertNull(found);
        
        charTrie.put(new Character('\0'), "");
        found = charTrie.lowerEntry(new Character('\0'));
        assertNull(found);      
    }
    
    public void testIteration() {
        PatriciaTrie intTrie = new PatriciaTrie(new IntegerKeyCreator());
        intTrie.put(new Integer(1), "One");
        intTrie.put(new Integer(5), "Five");
        intTrie.put(new Integer(4), "Four");
        intTrie.put(new Integer(2), "Two");
        intTrie.put(new Integer(3), "Three");
        intTrie.put(new Integer(15), "Fifteen");
        intTrie.put(new Integer(13), "Thirteen");
        intTrie.put(new Integer(14), "Fourteen");
        intTrie.put(new Integer(16), "Sixteen");
        
        TestCursor cursor = new TestCursor(new Object[] {
                new Integer(1), "One", new Integer(2), "Two", new Integer(3), "Three", new Integer(4), "Four", new Integer(5), "Five", new Integer(13), "Thirteen",
                new Integer(14), "Fourteen", new Integer(15), "Fifteen", new Integer(16), "Sixteen"});

        cursor.starting();
        intTrie.traverse(cursor);
        cursor.finished();
        
        cursor.starting();
        for (Iterator iter = intTrie.entrySet().iterator();iter.hasNext();) 
            cursor.select((Map.Entry)iter.next());
        cursor.finished();
        
        cursor.starting();
        for (Iterator iter = intTrie.keySet().iterator();iter.hasNext();)
            cursor.checkKey(iter.next());
        cursor.finished();
        
        cursor.starting();
        for (Iterator iter = intTrie.values().iterator(); iter.hasNext();)
            cursor.checkValue(iter.next());
        cursor.finished();

        PatriciaTrie charTrie = new PatriciaTrie(new AlphaKeyCreator());
        charTrie.put(new Character('c'), "c");
        charTrie.put(new Character('p'), "p");
        charTrie.put(new Character('l'), "l");
        charTrie.put(new Character('t'), "t");
        charTrie.put(new Character('k'), "k");
        charTrie.put(new Character('a'), "a");
        charTrie.put(new Character('y'), "y");
        charTrie.put(new Character('r'), "r");
        charTrie.put(new Character('u'), "u");
        charTrie.put(new Character('o'), "o");
        charTrie.put(new Character('w'), "w");
        charTrie.put(new Character('i'), "i");
        charTrie.put(new Character('e'), "e");
        charTrie.put(new Character('x'), "x");
        charTrie.put(new Character('q'), "q");
        charTrie.put(new Character('b'), "b");
        charTrie.put(new Character('j'), "j");
        charTrie.put(new Character('s'), "s");
        charTrie.put(new Character('n'), "n");
        charTrie.put(new Character('v'), "v");
        charTrie.put(new Character('g'), "g");
        charTrie.put(new Character('h'), "h");
        charTrie.put(new Character('m'), "m");
        charTrie.put(new Character('z'), "z");
        charTrie.put(new Character('f'), "f");
        charTrie.put(new Character('d'), "d");
        
        cursor = new TestCursor (new Object[] {
                new Character('a'), "a", new Character('b'), "b", 
                new Character('c'), "c", 
                new Character('d'), "d", new Character('e'), "e",
                new Character('f'), "f", new Character('g'), "g", new Character('h'), "h", new Character('i'), "i", new Character('j'), "j",
                new Character('k'), "k", new Character('l'), "l", new Character('m'), "m", new Character('n'), "n", new Character('o'), "o",
                new Character('p'), "p", new Character('q'), "q", new Character('r'), "r", new Character('s'), "s", new Character('t'), "t",
                new Character('u'), "u", new Character('v'), "v", new Character('w'), "w", new Character('x'), "x", new Character('y'), "y", 
                new Character('z'), "z"
        });
        
        cursor.starting();
        charTrie.traverse(cursor);
        cursor.finished();

        cursor.starting();
        for (Iterator iter = charTrie.entrySet().iterator(); iter.hasNext();)
            cursor.select((Map.Entry)iter.next());
        cursor.finished();
        
        cursor.starting();
        for (Iterator iter = charTrie.keySet().iterator(); iter.hasNext();)
            cursor.checkKey(iter.next());
        cursor.finished();
        
        cursor.starting();
        for (Iterator iter = charTrie.values().iterator(); iter.hasNext();)
            cursor.checkValue(iter.next());
        cursor.finished();
    }
    
    public void testSelect() {
        PatriciaTrie charTrie = new PatriciaTrie(new AlphaKeyCreator());
        charTrie.put(new Character('c'), "c");
        charTrie.put(new Character('p'), "p");
        charTrie.put(new Character('l'), "l");
        charTrie.put(new Character('t'), "t");
        charTrie.put(new Character('k'), "k");
        charTrie.put(new Character('a'), "a");
        charTrie.put(new Character('y'), "y");
        charTrie.put(new Character('r'), "r");
        charTrie.put(new Character('u'), "u");
        charTrie.put(new Character('o'), "o");
        charTrie.put(new Character('w'), "w");
        charTrie.put(new Character('i'), "i");
        charTrie.put(new Character('e'), "e");
        charTrie.put(new Character('x'), "x");
        charTrie.put(new Character('q'), "q");
        charTrie.put(new Character('b'), "b");
        charTrie.put(new Character('j'), "j");
        charTrie.put(new Character('s'), "s");
        charTrie.put(new Character('n'), "n");
        charTrie.put(new Character('v'), "v");
        charTrie.put(new Character('g'), "g");
        charTrie.put(new Character('h'), "h");
        charTrie.put(new Character('m'), "m");
        charTrie.put(new Character('z'), "z");
        charTrie.put(new Character('f'), "f");
        charTrie.put(new Character('d'), "d");
        
        TestCursor cursor = new TestCursor (new Object[] {
                new Character('d'), "d", new Character('e'), "e",
                new Character('f'), "f", new Character('g'), "g",
                new Character('a'), "a", new Character('b'), "b", 
                new Character('c'), "c",
                new Character('l'), "l", new Character('m'), "m", new Character('n'), "n", new Character('o'), "o",
                new Character('h'), "h", new Character('i'), "i", new Character('j'), "j",
                new Character('k'), "k",
                new Character('t'), "t",
                new Character('u'), "u", new Character('v'), "v", new Character('w'), "w",
                new Character('p'), "p", new Character('q'), "q", new Character('r'), "r", new Character('s'), "s",  new Character('x'), "x", new Character('y'), "y", 
                new Character('z'), "z"
        });
                
        assertEquals(26, charTrie.size());
        System.out.println(charTrie);
        cursor.starting();
        charTrie.select(new Character('d'), cursor);
        cursor.finished();
    }
    
    public void testTraverseCursorRemove() {
        PatriciaTrie charTrie = new PatriciaTrie(new AlphaKeyCreator());
        charTrie.put(new Character('c'), "c");
        charTrie.put(new Character('p'), "p");
        charTrie.put(new Character('l'), "l");
        charTrie.put(new Character('t'), "t");
        charTrie.put(new Character('k'), "k");
        charTrie.put(new Character('a'), "a");
        charTrie.put(new Character('y'), "y");
        charTrie.put(new Character('r'), "r");
        charTrie.put(new Character('u'), "u");
        charTrie.put(new Character('o'), "o");
        charTrie.put(new Character('w'), "w");
        charTrie.put(new Character('i'), "i");
        charTrie.put(new Character('e'), "e");
        charTrie.put(new Character('x'), "x");
        charTrie.put(new Character('q'), "q");
        charTrie.put(new Character('b'), "b");
        charTrie.put(new Character('j'), "j");
        charTrie.put(new Character('s'), "s");
        charTrie.put(new Character('n'), "n");
        charTrie.put(new Character('v'), "v");
        charTrie.put(new Character('g'), "g");
        charTrie.put(new Character('h'), "h");
        charTrie.put(new Character('m'), "m");
        charTrie.put(new Character('z'), "z");
        charTrie.put(new Character('f'), "f");
        charTrie.put(new Character('d'), "d");
        
        TestCursor cursor = new TestCursor (new Object[] {
                new Character('a'), "a", new Character('b'), "b", 
                new Character('c'), "c", 
                new Character('d'), "d", new Character('e'), "e",
                new Character('f'), "f", new Character('g'), "g", new Character('h'), "h", new Character('i'), "i", new Character('j'), "j",
                new Character('k'), "k", new Character('l'), "l", new Character('m'), "m", new Character('n'), "n", new Character('o'), "o",
                new Character('p'), "p", new Character('q'), "q", new Character('r'), "r", new Character('s'), "s", new Character('t'), "t",
                new Character('u'), "u", new Character('v'), "v", new Character('w'), "w", new Character('x'), "x", new Character('y'), "y", 
                new Character('z'), "z"
        });
        
        cursor.starting();
        charTrie.traverse(cursor);
        cursor.finished();
        
        // Test removing both an internal & external node.
        // 'm' is an example External node in this Trie, and 'p' is an internal.
        
        assertEquals(26, charTrie.size());
        
        Object[] toRemove = new Object[] { new Character('g'), new Character('d'), new Character('e'), new Character('m'), new Character('p'), new Character('q'), new Character('r'), new Character('s') };
        cursor.addToRemove(toRemove);
        
        cursor.starting();
        charTrie.traverse(cursor);
        cursor.finished();
            
        assertEquals(26 - toRemove.length, charTrie.size());

        cursor.starting();
        charTrie.traverse(cursor);
        cursor.finished();
        
        cursor.starting();
        for(Iterator i = charTrie.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) i.next();
            cursor.select(entry);
            if(Arrays.asList(toRemove).contains(entry.getKey()))
                fail("got an: " + entry);    
        }
        cursor.finished();
    }
    
    public void testIteratorRemove() {
        PatriciaTrie charTrie = new PatriciaTrie(new AlphaKeyCreator());
        charTrie.put(new Character('c'), "c");
        charTrie.put(new Character('p'), "p");
        charTrie.put(new Character('l'), "l");
        charTrie.put(new Character('t'), "t");
        charTrie.put(new Character('k'), "k");
        charTrie.put(new Character('a'), "a");
        charTrie.put(new Character('y'), "y");
        charTrie.put(new Character('r'), "r");
        charTrie.put(new Character('u'), "u");
        charTrie.put(new Character('o'), "o");
        charTrie.put(new Character('w'), "w");
        charTrie.put(new Character('i'), "i");
        charTrie.put(new Character('e'), "e");
        charTrie.put(new Character('x'), "x");
        charTrie.put(new Character('q'), "q");
        charTrie.put(new Character('b'), "b");
        charTrie.put(new Character('j'), "j");
        charTrie.put(new Character('s'), "s");
        charTrie.put(new Character('n'), "n");
        charTrie.put(new Character('v'), "v");
        charTrie.put(new Character('g'), "g");
        charTrie.put(new Character('h'), "h");
        charTrie.put(new Character('m'), "m");
        charTrie.put(new Character('z'), "z");
        charTrie.put(new Character('f'), "f");
        charTrie.put(new Character('d'), "d");
        
        TestCursor cursor = new TestCursor (new Object[] {
                new Character('a'), "a", new Character('b'), "b", 
                new Character('c'), "c", 
                new Character('d'), "d", new Character('e'), "e",
                new Character('f'), "f", new Character('g'), "g", new Character('h'), "h", new Character('i'), "i", new Character('j'), "j",
                new Character('k'), "k", new Character('l'), "l", new Character('m'), "m", new Character('n'), "n", new Character('o'), "o",
                new Character('p'), "p", new Character('q'), "q", new Character('r'), "r", new Character('s'), "s", new Character('t'), "t",
                new Character('u'), "u", new Character('v'), "v", new Character('w'), "w", new Character('x'), "x", new Character('y'), "y", 
                new Character('z'), "z"
        });
        
        // Test removing both an internal & external node.
        // 'm' is an example External node in this Trie, and 'p' is an internal.
        
        assertEquals(26, charTrie.size());
        
        Object[] toRemove = new Object[] { new Character('e'), new Character('m'), new Character('p'), new Character('q'), new Character('r'), new Character('s') };
        
        cursor.starting();
        for(Iterator i = charTrie.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            cursor.select(entry);
            if(Arrays.asList(toRemove).contains(entry.getKey()))
                i.remove();            
        }
        cursor.finished();
            
        assertEquals(26 - toRemove.length, charTrie.size());
        
        cursor.remove(toRemove);

        cursor.starting();
        for(Iterator i = charTrie.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) i.next();
            cursor.select(entry);
            if(Arrays.asList(toRemove).contains(entry.getKey()))
                fail("got an: " + entry);    
        }
        cursor.finished();
    }
    
    public void testHamlet() throws Exception {
        // Make sure that Hamlet is read & stored in the same order as a SortedSet.
        List original = new ArrayList();
        List control = new ArrayList();
        SortedMap sortedControl = new TreeMap();
        PatriciaTrie trie = new PatriciaTrie(new CharSequenceKeyAnalyzer());
        
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
        Iterator iter = trie.values().iterator();
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
        
        SortedMap sub = trie.headMap(control.get(523));
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
    
    public void testPrefixedBy() {
        PatriciaTrie trie 
            = new PatriciaTrie(new CharSequenceKeyAnalyzer());
        
        final String[] keys = new String[]{
                "", 
                "Albert", "Xavier", "XyZ", "Anna", "Alien", "Alberto",
                "Alberts", "Allie", "Alliese", "Alabama", "Banane",
                "Blabla", "Amber", "Ammun", "Akka", "Akko", "Albertoo",
                "Amma"
        };

        for (int i = 0; i < keys.length; i++) {
            trie.put(keys[i], keys[i]);
        }
        
        SortedMap map;
        Iterator iterator;
        Iterator entryIterator;
        Map.Entry entry;
        
        map = trie.getPrefixedBy("Al");
        assertEquals(8, map.size());
        assertEquals("Alabama", map.firstKey());
        assertEquals("Alliese", map.lastKey());
        assertEquals("Albertoo", map.get("Albertoo"));
        assertNotNull(trie.get("Xavier"));
        assertNull(map.get("Xavier"));
        assertNull(trie.get("Alice"));
        assertNull(map.get("Alice"));
        iterator = map.values().iterator();
        assertEquals("Alabama", iterator.next());
        assertEquals("Albert", iterator.next());
        assertEquals("Alberto", iterator.next());
        assertEquals("Albertoo", iterator.next());
        assertEquals("Alberts", iterator.next());
        assertEquals("Alien", iterator.next());
        assertEquals("Allie", iterator.next());
        assertEquals("Alliese", iterator.next());
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("Albert");
        iterator = map.keySet().iterator();
        assertEquals("Albert", iterator.next());
        assertEquals("Alberto", iterator.next());
        assertEquals("Albertoo", iterator.next());
        assertEquals("Alberts", iterator.next());
        assertFalse(iterator.hasNext());
        assertEquals(4, map.size());
        assertEquals("Albert", map.firstKey());
        assertEquals("Alberts", map.lastKey());
        assertNull(trie.get("Albertz"));
        map.put("Albertz", "Albertz");
        assertEquals("Albertz", trie.get("Albertz"));
        assertEquals(5, map.size());
        assertEquals("Albertz", map.lastKey());
        iterator = map.keySet().iterator();
        assertEquals("Albert", iterator.next());
        assertEquals("Alberto", iterator.next());
        assertEquals("Albertoo", iterator.next());
        assertEquals("Alberts", iterator.next());
        assertEquals("Albertz", iterator.next());
        assertFalse(iterator.hasNext());
        assertEquals("Albertz", map.remove("Albertz"));
        
        map = trie.getPrefixedBy("Alberto");
        assertEquals(2, map.size());
        assertEquals("Alberto", map.firstKey());
        assertEquals("Albertoo", map.lastKey());
        entryIterator = map.entrySet().iterator();
        entry = (Map.Entry)entryIterator.next();
        assertEquals("Alberto", entry.getKey());
        assertEquals("Alberto", entry.getValue());
        entry = (Map.Entry)entryIterator.next();
        assertEquals("Albertoo", entry.getKey());
        assertEquals("Albertoo", entry.getValue());
        assertFalse(entryIterator.hasNext());
        trie.put("Albertoad", "Albertoad");
        assertEquals(3, map.size());
        assertEquals("Alberto", map.firstKey());
        assertEquals("Albertoo", map.lastKey());
        entryIterator = map.entrySet().iterator();
        entry = (Map.Entry)entryIterator.next();
        assertEquals("Alberto", entry.getKey());
        assertEquals("Alberto", entry.getValue());
        entry = (Map.Entry)entryIterator.next();
        assertEquals("Albertoad", entry.getKey());
        assertEquals("Albertoad", entry.getValue());
        entry = (Map.Entry)entryIterator.next();
        assertEquals("Albertoo", entry.getKey());
        assertEquals("Albertoo", entry.getValue());
        assertFalse(entryIterator.hasNext());
        assertEquals("Albertoo", trie.remove("Albertoo"));
        assertEquals("Alberto", map.firstKey());
        assertEquals("Albertoad", map.lastKey());
        assertEquals(2, map.size());
        entryIterator = map.entrySet().iterator();
        entry = (Map.Entry)entryIterator.next();
        assertEquals("Alberto", entry.getKey());
        assertEquals("Alberto", entry.getValue());
        entry = (Map.Entry)entryIterator.next();
        assertEquals("Albertoad", entry.getKey());
        assertEquals("Albertoad", entry.getValue());
        assertFalse(entryIterator.hasNext());
        assertEquals("Albertoad", trie.remove("Albertoad"));
        trie.put("Albertoo", "Albertoo");
        
        map = trie.getPrefixedBy("X");
        assertEquals(2, map.size());
        assertFalse(map.containsKey("Albert"));
        assertTrue(map.containsKey("Xavier"));
        assertFalse(map.containsKey("Xalan"));
        iterator = map.values().iterator();
        assertEquals("Xavier", iterator.next());
        assertEquals("XyZ", iterator.next());
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("An");
        assertEquals(1, map.size());
        assertEquals("Anna", map.firstKey());
        assertEquals("Anna", map.lastKey());
        iterator = map.keySet().iterator();
        assertEquals("Anna", iterator.next());
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("Ban");
        assertEquals(1, map.size());
        assertEquals("Banane", map.firstKey());
        assertEquals("Banane", map.lastKey());
        iterator = map.keySet().iterator();
        assertEquals("Banane", iterator.next());
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("Am");
        assertFalse(map.isEmpty());
        assertEquals(3, map.size());
        assertEquals("Amber", trie.remove("Amber"));
        iterator = map.keySet().iterator();
        assertEquals("Amma", iterator.next());
        assertEquals("Ammun", iterator.next());
        assertFalse(iterator.hasNext());
        iterator = map.keySet().iterator();
        map.put("Amber", "Amber");
        assertEquals(3, map.size());
        try {
            iterator.next();
            fail("CME expected");
        } catch(ConcurrentModificationException expected) {}
        assertEquals("Amber", map.firstKey());
        assertEquals("Ammun", map.lastKey());
        
        map = trie.getPrefixedBy("Ak\0");
        assertTrue(map.isEmpty());
        
        map = trie.getPrefixedBy("Ak");
        assertEquals(2, map.size());
        assertEquals("Akka", map.firstKey());
        assertEquals("Akko", map.lastKey());
        map.put("Ak", "Ak");
        assertEquals("Ak", map.firstKey());
        assertEquals("Akko", map.lastKey());
        assertEquals(3, map.size());
        trie.put("Al", "Al");
        assertEquals(3, map.size());
        assertEquals("Ak", map.remove("Ak"));
        assertEquals("Akka", map.firstKey());
        assertEquals("Akko", map.lastKey());
        assertEquals(2, map.size());
        iterator = map.keySet().iterator();
        assertEquals("Akka", iterator.next());
        assertEquals("Akko", iterator.next());
        assertFalse(iterator.hasNext());
        assertEquals("Al", trie.remove("Al"));
        
        map = trie.getPrefixedBy("Akka");
        assertEquals(1, map.size());
        assertEquals("Akka", map.firstKey());
        assertEquals("Akka", map.lastKey());
        iterator = map.keySet().iterator();
        assertEquals("Akka", iterator.next());
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("Ab");
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        try {
            Object o = map.firstKey();
            fail("got a first key: " + o);
        } catch(NoSuchElementException nsee) {}
        try {
            Object o = map.lastKey();
            fail("got a last key: " + o);
        } catch(NoSuchElementException nsee) {}
        iterator = map.values().iterator();
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("Albertooo");
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        try {
            Object o = map.firstKey();
            fail("got a first key: " + o);
        } catch(NoSuchElementException nsee) {}
        try {
            Object o = map.lastKey();
            fail("got a last key: " + o);
        } catch(NoSuchElementException nsee) {}
        iterator = map.values().iterator();
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("");
        assertSame(trie, map); // stricter than necessary, but a good check
        
        map = trie.getPrefixedBy("\0");
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        try {
            Object o = map.firstKey();
            fail("got a first key: " + o);
        } catch(NoSuchElementException nsee) {}
        try {
            Object o = map.lastKey();
            fail("got a last key: " + o);
        } catch(NoSuchElementException nsee) {}
        iterator = map.values().iterator();
        assertFalse(iterator.hasNext());
    }
    
    public void testPrefixByOffsetAndLength() {
        PatriciaTrie trie 
            = new PatriciaTrie(new CharSequenceKeyAnalyzer());
        
        final String[] keys = new String[]{
                "Albert", "Xavier", "XyZ", "Anna", "Alien", "Alberto",
                "Alberts", "Allie", "Alliese", "Alabama", "Banane",
                "Blabla", "Amber", "Ammun", "Akka", "Akko", "Albertoo",
                "Amma"
        };
    
        for (int i = 0; i < keys.length; i++) {
            trie.put(keys[i], keys[i]);
        }
        
        SortedMap map;
        Iterator iterator;
        
        map = trie.getPrefixedBy("Alice", 2);
        assertEquals(8, map.size());
        assertEquals("Alabama", map.firstKey());
        assertEquals("Alliese", map.lastKey());
        assertEquals("Albertoo", map.get("Albertoo"));
        assertNotNull(trie.get("Xavier"));
        assertNull(map.get("Xavier"));
        assertNull(trie.get("Alice"));
        assertNull(map.get("Alice"));
        iterator = map.values().iterator();
        assertEquals("Alabama", iterator.next());
        assertEquals("Albert", iterator.next());
        assertEquals("Alberto", iterator.next());
        assertEquals("Albertoo", iterator.next());
        assertEquals("Alberts", iterator.next());
        assertEquals("Alien", iterator.next());
        assertEquals("Allie", iterator.next());
        assertEquals("Alliese", iterator.next());
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("BAlice", 1, 2);
        assertEquals(8, map.size());
        assertEquals("Alabama", map.firstKey());
        assertEquals("Alliese", map.lastKey());
        assertEquals("Albertoo", map.get("Albertoo"));
        assertNotNull(trie.get("Xavier"));
        assertNull(map.get("Xavier"));
        assertNull(trie.get("Alice"));
        assertNull(map.get("Alice"));
        iterator = map.values().iterator();
        assertEquals("Alabama", iterator.next());
        assertEquals("Albert", iterator.next());
        assertEquals("Alberto", iterator.next());
        assertEquals("Albertoo", iterator.next());
        assertEquals("Alberts", iterator.next());
        assertEquals("Alien", iterator.next());
        assertEquals("Allie", iterator.next());
        assertEquals("Alliese", iterator.next());
        assertFalse(iterator.hasNext());
    }
    
    public void testPrefixedByRemoval() {
        PatriciaTrie trie 
            = new PatriciaTrie(new CharSequenceKeyAnalyzer());
        
        final String[] keys = new String[]{
                "Albert", "Xavier", "XyZ", "Anna", "Alien", "Alberto",
                "Alberts", "Allie", "Alliese", "Alabama", "Banane",
                "Blabla", "Amber", "Ammun", "Akka", "Akko", "Albertoo",
                "Amma"
        };

        for (int i = 0; i < keys.length; i++) {
            trie.put(keys[i], keys[i]);
        }
        
        SortedMap map = trie.getPrefixedBy("Al");
        assertEquals(8, map.size());
        Iterator iter = map.keySet().iterator();
        assertEquals("Alabama", iter.next());
        assertEquals("Albert", iter.next());
        assertEquals("Alberto", iter.next());
        assertEquals("Albertoo", iter.next());
        assertEquals("Alberts", iter.next());
        assertEquals("Alien", iter.next());
        iter.remove();
        assertEquals(7, map.size());
        assertEquals("Allie", iter.next());
        assertEquals("Alliese", iter.next());
        assertFalse(iter.hasNext());
        
        map = trie.getPrefixedBy("Ak");
        assertEquals(2, map.size());
        iter = map.keySet().iterator();
        assertEquals("Akka", iter.next());
        iter.remove();
        assertEquals(1, map.size());
        assertEquals("Akko", iter.next());
        if(iter.hasNext())
            fail("shouldn't have next (but was: " + iter.next() + ")");
        assertFalse(iter.hasNext());
    }

    public void testTraverseWithAllNullBitKey() {
        PatriciaTrie trie 
            = new PatriciaTrie(new CharSequenceKeyAnalyzer());
        
        //
        // One entry in the Trie
        // Entry is stored at the root
        //
        
        // trie.put("", "All Bits Are Zero");
        trie.put("\0", "All Bits Are Zero");
        
        //
        //  / ("")   <-- root
        //  \_/  \
        //       null
        //
        
        final List strings = new ArrayList();
        trie.traverse(new Cursor() {
            public int select(Entry entry) {
                strings.add(entry.getValue());
                return Cursor.CONTINUE;
            }
        });
        
        assertEquals(1, strings.size());
        
        strings.clear();
        for (Iterator iter = trie.values().iterator(); iter.hasNext();) {
            strings.add(iter.next());
        }
        assertEquals(1, strings.size());
    }
    
    public void testSelectWithAllNullBitKey() {
        PatriciaTrie trie 
            = new PatriciaTrie(new CharSequenceKeyAnalyzer());
        
        // trie.put("", "All Bits Are Zero");
        trie.put("\0", "All Bits Are Zero");
        
        final List strings = new ArrayList();
        trie.select("Hello", new Cursor() {
            public int select(Entry entry) {
                strings.add(entry.getValue());
                return Cursor.CONTINUE;
            }
        });
        assertEquals(1, strings.size());
    }
    
    private static class TestCursor implements PatriciaTrie.Cursor {
        private List keys;
        private List values;
        private Object selectFor;
        private List toRemove;
        private int index = 0;
        
        TestCursor(Object[] objects) {
            if(objects.length % 2 != 0)
                throw new IllegalArgumentException("must be * 2");
            
            keys = new ArrayList(objects.length / 2);
            values = new ArrayList(keys.size());
            toRemove = Collections.emptyList();
            for(int i = 0; i < objects.length; i++) {
                keys.add(objects[i]);
                values.add(objects[++i]);
            }
        }
        
        void selectFor(Object object) {
            selectFor = object;
        }
        
        void addToRemove(Object[] objects) {
            toRemove = new ArrayList(Arrays.asList(objects));
        }
        
        void remove(Object[] objects) {
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

        public int select(Entry entry) {
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
                return Cursor.REMOVE;
            } 
            
            if(selectFor != null && selectFor.equals(entry.getKey()))
                return Cursor.EXIT;
            else
                return Cursor.CONTINUE;
        }
        
        void finished() {
            assertEquals(keys.size(), index);
        }
    }

    private static class IntegerKeyCreator implements KeyAnalyzer {

        public static final int[] createIntBitMask(final int bitCount) {
            int[] bits = new int[bitCount];
            for (int i = 0; i < bitCount; i++) {
                bits[i] = 1 << (bitCount - i - 1);
            }
            return bits;
        }

        private static final int[] BITS = createIntBitMask(32);

        public int length(Object key) {
            return 32;
        }

        public boolean isBitSet(Object key, int keyLength, int bitIndex) {
            return (((Integer)key).intValue() & BITS[bitIndex]) != 0;
        }

        public int bitIndex(Object key,   int keyOff, int keyLength,
                            Object found, int foundOff, int foundKeyLength) {
            if (found == null)
                found = new Integer(0);
            
            if(keyOff != 0 || foundOff != 0)
                throw new IllegalArgumentException("offsets must be 0 for fixed-size keys");

            boolean allNull = true;
            
            int length = Math.max(keyLength, foundKeyLength);
            
            for (int i = 0; i < length; i++) {
                int a = ((Integer)key).intValue() & BITS[i];
                int b = ((Integer)found).intValue() & BITS[i];

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

        public int compare(Object o1, Object o2) {
            Integer i1 = (Integer)o1;
            Integer i2 = (Integer)o2;
            return i1.compareTo(i2);
        }

        public int bitsPerElement() {
            return 1;
        }
        
        public boolean isPrefix(Object prefix, int offset, int length, Object key) {
            int addr1 = ((Integer)prefix).intValue();
            int addr2 = ((Integer)key).intValue();
            addr1 = addr1 << offset;
            
            int mask = 0;
            for(int i = 0; i < length; i++) {
                mask |= (0x1 << i);
            }
            
            addr1 &= mask;
            addr2 &= mask;
            
            return addr1 == addr2;
        }
    }

    private static class AlphaKeyCreator implements KeyAnalyzer {

        public static final int[] createIntBitMask(final int bitCount) {
            int[] bits = new int[bitCount];
            for (int i = 0; i < bitCount; i++) {
                bits[i] = 1 << (bitCount - i - 1);
            }
            return bits;
        }

        private static final int[] BITS = createIntBitMask(16);

        public int length(Object key) {
            return 16;
        }

        public boolean isBitSet(Object key, int keyLength, int bitIndex) {
            return (((Character)key).charValue() & BITS[bitIndex]) != 0;
        }

        public int bitIndex(Object key,   int keyOff, int keyLength,
                            Object found, int foundOff, int foundKeyLength) {
            if (found == null)
                found = new Character((char)0);
            
            if(keyOff != 0 || foundOff != 0)
                throw new IllegalArgumentException("offsets must be 0 for fixed-size keys");
            
            int length = Math.max(keyLength, foundKeyLength);

            boolean allNull = true;
            for (int i = 0; i < length; i++) {
                int a = ((Character)key).charValue() & BITS[i];
                int b = ((Character)found).charValue() & BITS[i];

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

        public int compare(Object o1, Object o2) {
            Character c1 = (Character) o1;
            Character c2 = (Character) o2;
            return c1.compareTo(c2);
        }

        public int bitsPerElement() {
            return 1;
        }
        
        public boolean isPrefix(Object prefix, int offset, int length, Object key) {
            int addr1 = ((Character)prefix).charValue();
            int addr2 = ((Character)key).charValue();
            addr1 = addr1 << offset;
            
            int mask = 0;
            for(int i = 0; i < length; i++) {
                mask |= (0x1 << i);
            }
            
            addr1 &= mask;
            addr2 &= mask;
            
            return addr1 == addr2;
        }
        
        
    }

}

