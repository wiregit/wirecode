
package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;



public class HasherSetTest extends BaseTestCase {
    
    public HasherSetTest(String name){
        super(name);
    }
    
    public static Test suite() {
		return buildTestSuite(HasherSetTest.class);
	}
    
    /**
     * a hasher that claims everybody is equal 
     */
    static Hasher communist = new Hasher() {
        public int hash(Object o){
            return 1;
        }
        
        public boolean areEqual(Object a, Object b){
            return true;
        }
    };
    
    /**
     * a hasher that claims everybody is different 
     */
    static Hasher individualist = new Hasher() {
        public int hash(Object o){
            return (int)(1000*Math.random());
        }
        
        public boolean areEqual(Object a, Object b){
            return false;
        }
    };
    
    
    /**
     * a hasher that will compare Integers, Longs and their String
     * representations.
     */
    static Hasher numeric = new Hasher() {
        
        public int hash(Object o){
            if (o instanceof Integer || o instanceof Long)
                return o.hashCode();
            
            if (o instanceof String) {
                Long l = new Long((String)o);
                return l.hashCode();
            }
            
            throw new IllegalArgumentException();
        }
        
        public boolean areEqual(Object a, Object b) {
            if ((a instanceof Integer || a instanceof Long) &&
                    (b instanceof Integer || b instanceof Long))
                return a.equals(b);
            
            if (a instanceof String || b instanceof String){
            
                String sa = a.toString();
                String sb = b.toString();
                Long al = new Long((String)sa);
                Long bl = new Long((String)sb);
                return al.equals(bl);
            }
            return false;
        }
    };
    
    Integer one = new Integer(1);
    Integer two = new Integer(2);
    Integer three = new Integer(3);
    Integer four = new Integer(4);
    Integer five = new Integer(5);
    /**
     * tests the operation of the object when no hasher is provided
     */
    public void testDefaultHasher() throws Exception {
        
        // test plain add of elements
        HasherSet set = new HasherSet();
        
        set.add(one);
        set.add(one);
        set.add(two);
        
        assertEquals(2,set.size());
        
        // test addAll with another HasherSet
        HasherSet set2 = new HasherSet();
        set2.add(two);
        set2.add(three);
        
        assertEquals(2,set2.size());
        set.addAll(set2);
        
        assertEquals(3,set.size());
        assertTrue(set.contains(two));
        assertTrue(set.contains(three));
        
        // test addAll with an ordinary collection
        List l = new LinkedList();
        l.add(three);
        l.add(four);
        l.add(four);
        
        set.addAll(l);
        
        // test contains
        assertEquals(4,set.size());
        assertTrue(set.contains(one));
        assertTrue(set.contains(two));
        assertTrue(set.contains(three));
        assertTrue(set.contains(four));
        
        // test containsAll
        assertTrue(set.containsAll(set2));
        assertTrue(set.containsAll(l));
        
        set.remove(four);
        assertTrue(set.containsAll(set2));
        assertFalse(set.containsAll(l));
        
        // test constructors
        set2 = new HasherSet(l);
        assertEquals(2,set2.size());
        assertTrue(set2.contains(three));
        assertTrue(set2.contains(four));
        assertFalse(set2.contains(two));
        
        // test adding another collection
        set2.add(set);
        
        assertEquals(3,set2.size());
        assertTrue(set2.contains(set));
        assertFalse(set2.contains(one));
        assertFalse(set2.contains(two));

	// test adding HasherSet to a normal collection
	l = new LinkedList();
	l.addAll(set2);
	assertEquals(3,l.size());
	assertTrue(l.contains(set));
	assertTrue(l.contains(three));
	assertTrue(l.contains(four));
    }
    
    public void testIteratorUnwraps() throws Exception {
        List a = new LinkedList();
        List b = new LinkedList();
        
        a.add(one);
        a.add(two);
        
        b.add(two);
        b.add(three);
        
        HasherSet sa = new HasherSet(a);
        
        HasherSet set = new HasherSet();
        
        set.addAll(sa);
        set.addAll(b);
        
        assertEquals(3,set.size());
        
        for (Iterator iter = set.iterator();iter.hasNext();)
            assertTrue(iter.next() instanceof Integer);
        
    }
    
    public void testCustomHashers() throws Exception {
     
        // test the degenerate hasher who thinks everyone is equal
        HasherSet equal = new HasherSet(communist);
        
        equal.add(one);
        equal.add(two);
        equal.add(three);
        
        assertEquals(1,equal.size());
        assertTrue(equal.contains(one));
        assertTrue(equal.contains("bad hasher"));
        
        // test degenerate hasher who thinks everyone is different
        
        HasherSet different = new HasherSet(individualist);
        
        different.add(one);
        different.add(one);
        different.add(two);
        
        assertEquals(3,different.size());
        
        assertFalse(different.contains(one));
        assertFalse(different.contains(two));
        
        // test the hasher which equalizes strings and integers.
        
        HasherSet numbers = new HasherSet(numeric);
        
        numbers.add("1");
        numbers.add(one);
        numbers.add(new Long(1));
        
        assertEquals(1,numbers.size());
        
        numbers.add(two);
        assertTrue(numbers.contains("2"));
    }

}
