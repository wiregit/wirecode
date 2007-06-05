package org.limewire.collection;

import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;

/**
 * Stores a fixed size of elements as a <code>Set</code> and removes elements 
 * when that size is reached. <code>FixedsizeForgetfulHashSet</code> is a 
 * <code>Set</code> version of {@link FixedsizeForgetfulHashMap}. Like 
 * <code>ForgetfulHashMap</code>, values are "forgotten" using a FIFO replacement 
 * policy.   
 * <p>
 * <code>FixedsizeForgetfulHashSet</code> works in constant time.
 * 
<pre>
    public class MyObjectHash{
        public String s;
        public int item;
        public MyObjectHash(String s, int item){
            this.s = s;
            this.item = item;
        }       

        public String toString(){
            return s + "=" + item;
        }
        
        public boolean equals(Object obj) {
            MyObjectHash other = (MyObjectHash)obj;
            return (this.s.equals(other.s) && this.item == other.item);         
        }
                
        public int hashCode() {
            return this.item * 31 + s.hashCode();
        }
    }   
    
    void sampleCodeFixedsizeForgetfulHashSet(){
        FixedsizeForgetfulHashSet&lt;MyObjectHash&gt; ffhs = new FixedsizeForgetfulHashSet&lt;MyObjectHash&gt;(4);

        MyObjectHash mo = new MyObjectHash("a", 1);
        if(ffhs.add(mo))
            System.out.println("1) Size is: " + ffhs.size() + " contents: " + ffhs);
        if(!ffhs.add(mo))
            System.out.println("Unable to add the same object twice; contents: " + ffhs);

        if(ffhs.add(new MyObjectHash("b", 2)))
            System.out.println("2) Size is: " + ffhs.size() + " contents: " + ffhs);
        if(!ffhs.add(new MyObjectHash("b", 2)))
            System.out.println("Unable to add the object; contents: " + ffhs);
        if(ffhs.add(new MyObjectHash("c", 3)))
            System.out.println("3) Size is: " + ffhs.size() + " contents: " + ffhs);
        if(ffhs.add(new MyObjectHash("d", 4)))
            System.out.println("4) Size is: " + ffhs.size() + " contents: " + ffhs);    
        if(ffhs.add(new MyObjectHash("e", 5)))
            System.out.println("5) Size is: " + ffhs.size() + " contents: " + ffhs);    
    }   
    Output:
        1) Size is: 1 contents: [a=1]
        Unable to add the same object twice; contents: [a=1]
        2) Size is: 2 contents: [a=1, b=2]
        Unable to add the object; contents: [a=1, b=2]
        3) Size is: 3 contents: [a=1, b=2, c=3]
        4) Size is: 4 contents: [a=1, b=2, c=3, d=4]
        5) Size is: 4 contents: [b=2, c=3, d=4, e=5]
    
</pre>
* 
*/
public class FixedsizeForgetfulHashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable {

    /**
     * Backing map which the set delegates.
     */
    private transient FixedsizeForgetfulHashMap<E,Object> map;

    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();

    /**
     * Constructs a new, empty set.
     */
    public FixedsizeForgetfulHashSet(int size) {
        map = new FixedsizeForgetfulHashMap<E,Object>(size);
    }
    
    /**
     * Constructs a new, empty set, using the given initialCapacity.
     */
    public FixedsizeForgetfulHashSet(int size, int initialCapacity) {
        map = new FixedsizeForgetfulHashMap<E,Object>(size, initialCapacity);
    }
    
    /**
     * Constructs a new, empty set, using the given initialCapacity & loadFactor.
     */
    public FixedsizeForgetfulHashSet(int size, int initialCapacity, float loadFactor) {
        map = new FixedsizeForgetfulHashMap<E,Object>(size, initialCapacity, loadFactor);
    }
    
    /**
     * Tests if the set is full
     * 
     * @return true, if the set is full (ie if adding any other entry will
     * lead to removal of some other entry to maintain the fixed-size property
     * of the set). Returns false, otherwise
     */
    public boolean isFull() {
        return map.isFull();
    }
    
    /**
     * Removes the least recently used entry from the set
     * @return The least recently used value from the set.
     * Modifies this.
     */
    public E removeLRUEntry() {
        if(isEmpty())
            return null;
        
        Iterator<E> i = iterator();
        E value = i.next();
        i.remove();
        return value;
    }

    /**
     * Returns an iterator over the elements in this set.  The elements
     * are returned in no particular order.
     *
     * @return an Iterator over the elements in this set.
     * @see ConcurrentModificationException
     */
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    /**
     * Returns the number of elements in this set (its cardinality).
     *
     * @return the number of elements in this set (its cardinality).
     */
    public int size() {
        return map.size();
    }

    /**
     * Returns <tt>true</tt> if this set contains no elements.
     *
     * @return <tt>true</tt> if this set contains no elements.
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns <tt>true</tt> if this set contains the specified element.
     *
     * @param o element whose presence in this set is to be tested.
     * @return <tt>true</tt> if this set contains the specified element.
     */
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    /**
     * Adds the specified element to this set if it is not already
     * present.
     *
     * @param o element to be added to this set.
     * @return <tt>true</tt> if the set did not already contain the specified
     * element.
     */
    public boolean add(E o) {
        return map.put(o, PRESENT)==null;
    }

    /**
     * Removes the specified element from this set if it is present.
     *
     * @param o object to be removed from this set, if present.
     * @return <tt>true</tt> if the set contained the specified element.
     */
    public boolean remove(Object o) {
        return map.remove(o)==PRESENT;
    }

    /**
     * Removes all of the elements from this set.
     */
    public void clear() {
        map.clear();
    }

    /**
     * Returns a shallow copy of this <tt>FixedsizeForgetfulHashSet</tt> instance: the elements
     * themselves are not cloned.
     *
     * @return a shallow copy of this set.
     */
    @SuppressWarnings("unchecked")
    public FixedsizeForgetfulHashSet<E> clone() {
        try { 
            FixedsizeForgetfulHashSet<E> newSet = (FixedsizeForgetfulHashSet<E>)super.clone();
            newSet.map = map.clone();
            return newSet;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

}
