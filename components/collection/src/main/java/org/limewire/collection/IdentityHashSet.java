package org.limewire.collection;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Stores elements in a <code>Set</code> with keys that are compared with ==, not equals.
 * Different key objects are considered distinct even if they have equal contents. 
 * For example, if you attempt to add <code>mo</code> (MyObject mo = new 
 * MyObject("b", 2);) twice, then the add fails. However, if you attempt to add
 * new MyObject("b", 2) twice, both adds pass.
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

    void sampleCodeIdentityHashSet(){

        IdentityHashSet&lt;MyObjectHash&gt; ihs = new IdentityHashSet&lt;MyObjectHash&gt;(4);

        MyObjectHash mo = new MyObjectHash("a", 1);
        if(ihs.add(mo))
            System.out.println("1) Size is: " + ihs.size() + " contents: " + ihs);
        if(!ihs.add(mo))
            System.out.println("Unable to add the same object twice; contents: " + ihs);
        
        if(ihs.add(new MyObjectHash("b", 2)))
            System.out.println("2) Size is: " + ihs.size() + " contents: " + ihs);
        if(ihs.add(new MyObjectHash("b", 2)))
            System.out.println("3) Size is: " + ihs.size() + " contents: " + ihs);
        if(ihs.add(new MyObjectHash("c", 3)))
            System.out.println("3) Size is: " + ihs.size() + " contents: " + ihs);
        if(ihs.add(new MyObjectHash("d", 4)))
            System.out.println("4) Size is: " + ihs.size() + " contents: " + ihs);  
    }
    Output:
        1) Size is: 1 contents: [a=1]
        Unable to add the same object twice; contents: [a=1]
        2) Size is: 2 contents: [a=1, b=2]
        3) Size is: 3 contents: [a=1, b=2, b=2]
        3) Size is: 4 contents: [a=1, c=3, b=2, b=2]
        4) Size is: 5 contents: [c=3, a=1, d=4, b=2, b=2]

</pre>
 * 
 */
public class IdentityHashSet<E>
extends AbstractSet<E>
implements Set<E>, Cloneable, java.io.Serializable
{
	static final long serialVersionUID = -5024744406713321677L;

	private transient IdentityHashMap<E,Object> map;

//	Dummy value to associate with an Object in the backing Map
	private static final Object PRESENT = new Object();

	/**
	 * Constructs a new, empty set; the backing <tt>IdentityHashMap</tt> instance has
	 * default initial capacity (16) and load factor (0.75).
	 */
	public IdentityHashSet() {
		map = new IdentityHashMap<E,Object>();
	}

	/**
	 * Constructs a new set containing the elements in the specified
	 * collection.  The <tt>IdentityHashMap</tt> is created with default load factor
	 * (0.75) and an initial capacity sufficient to contain the elements in
	 * the specified collection.
	 *
	 * @param c the collection whose elements are to be placed into this set.
	 * @throws NullPointerException   if the specified collection is null.
	 */
	public IdentityHashSet(Collection<? extends E> c) {
		map = new IdentityHashMap<E,Object>(Math.max((int) (c.size()/.75f) + 1, 16));
		addAll(c);
	}



	/**
	 * Constructs a new, empty set; the backing <tt>IdentityHashMap</tt> instance has
	 * the specified initial capacity and default load factor, which is
	 * <tt>0.75</tt>.
	 *
	 * @param      initialCapacity   the initial capacity of the hash table.
	 * @throws     IllegalArgumentException if the initial capacity is less
	 *             than zero.
	 */
	public IdentityHashSet(int initialCapacity) {
		map = new IdentityHashMap<E,Object>(initialCapacity);
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
	 * Returns a shallow copy of this <tt>IdentityHashSet</tt> instance: the elements
	 * themselves are not cloned.
	 *
	 * @return a shallow copy of this set.
	 */
	@SuppressWarnings("unchecked")
    public Object clone() {
		try { 
			IdentityHashSet<E> newSet = (IdentityHashSet<E>) super.clone();
			newSet.map = (IdentityHashMap<E, Object>) map.clone();
			return newSet;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	/**
	 * Save the state of this <tt>IdentityHashSet</tt> instance to a stream (that is,
	 * serialize this set).
	 *
	 * @serialData The capacity of the backing <tt>IdentityHashMap</tt> instance
	 *		   (int), and its load factor (float) are emitted, followed by
	 *		   the size of the set (the number of elements it contains)
	 *		   (int), followed by all of its elements (each an Object) in
	 *             no particular order.
	 */
	private void writeObject(java.io.ObjectOutputStream s)
	throws java.io.IOException {
//		Write out any hidden serialization magic
		s.defaultWriteObject();

		// Write out size
		s.writeInt(map.size());

//		Write out all elements in the proper order.
		for (Iterator i=map.keySet().iterator(); i.hasNext(); )
			s.writeObject(i.next());
	}

	/**
	 * Reconstitute the <tt>IdentityHashSet</tt> instance from a stream (that is,
	 * deserialize it).
	 */
	@SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
	throws java.io.IOException, ClassNotFoundException {
//		Read in any hidden serialization magic
		s.defaultReadObject();

		// Read in HashMap capacity and load factor and create backing HashMap

		map = new IdentityHashMap<E,Object>();

		// Read in size
		int size = s.readInt();

//		Read in all elements in the proper order.
		for (int i=0; i<size; i++) {
			E e = (E) s.readObject();
			map.put(e, PRESENT);
		}
	}
}

