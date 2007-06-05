/*
 * FixedSizeForgetfulHashMap.java
 *
 * Created on December 11, 2000, 2:08 PM
 */

package org.limewire.collection;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
* Provides a better-defined replacement policy version of 
* {@link ForgetfulHashMap}. Like <code>ForgetfulHashMap</code>, this is a
* mapping that "forgets" keys and values using a FIFO replacement policy, much
* like a cache. 
* <p>
* Specifically, <code>FixedsizeForgetfulHashMap</code> allows a 
* key to be re-mapped to a different value and then "renews" this key so it 
* is the last key to be replaced (done in constant time).
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

    void sampleCodeFixedsizeForgetfulHashMap(){
        FixedsizeForgetfulHashMap&lt;String, MyObjectHash&gt; ffhm = new FixedsizeForgetfulHashMap&lt;String, MyObjectHash&gt;(3);

        ffhm.put("Mykey1", new MyObjectHash("a", 1));
        System.out.println("1) Size is: " + ffhm.size() + " contents: " + ffhm);
        ffhm.put("Mykey2", new MyObjectHash("b", 2));
        System.out.println("2) Size is: " + ffhm.size() + " contents: " + ffhm);
        ffhm.put("Mykey3", new MyObjectHash("c", 3));
        System.out.println("3) Size is: " + ffhm.size() + " contents: " + ffhm);

        ffhm.put("Mykey4", new MyObjectHash("d", 4));
        System.out.println("4) Size is: " + ffhm.size() + " contents: " + ffhm);
        
        ffhm.put("Mykey3", new MyObjectHash("replace", 3));
        System.out.println("5) Size is: " + ffhm.size() + " contents: " + ffhm);    
    }
    Output:
        1) Size is: 1 contents: {Mykey1=a=1}
        2) Size is: 2 contents: {Mykey1=a=1, Mykey2=b=2}
        3) Size is: 3 contents: {Mykey1=a=1, Mykey2=b=2, Mykey3=c=3}
        4) Size is: 3 contents: {Mykey2=b=2, Mykey3=c=3, Mykey4=d=4}
        5) Size is: 3 contents: {Mykey2=b=2, Mykey4=d=4, Mykey3=replace=3}
</pre>
*/
public class FixedsizeForgetfulHashMap<K, V> extends LinkedHashMap<K, V> {

    /**  Maximum number of elements to be stored in the underlying hashMap */
    private final int MAXIMUM_SIZE;

    /**
     * Create a new instance that holds only the last "size" entries.
     * 
     * @param size the number of entries to hold
     * @exception IllegalArgumentException if size is less < 1.
     */
    public FixedsizeForgetfulHashMap(int size) {
        this(size, (size * 4)/3 + 10, 0.75f);
    }
    
    /**
     * Create a new instance that holds only the last "size" entries,
     * using the given initialCapacity and a loadFactor of 0.75.
     * 
     * @param size the number of entries to hold
     * @exception IllegalArgumentException if size is less < 1.
     */
    public FixedsizeForgetfulHashMap(int size, int initialCapacity) {
        this(size, initialCapacity, 0.75f);
    }
    
    /**
     * Create a new instance that holds only the last "size" entries, using
     * the given initialCapacity & loadFactor.
     * 
     * @param size the number of entries to hold
     * @exception IllegalArgumentException if size is less < 1.
     */
    public FixedsizeForgetfulHashMap(int size, int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        
        //if size is < 1
        if (size < 1)
            throw new IllegalArgumentException("invalid size: " + size);
    
        //set the max size to the size specified
        MAXIMUM_SIZE = size;
    }

    /**
     * Tests if the map is full
     * 
     * @return true, if the map is full (ie if adding any other entry will
     * lead to removal of some other entry to maintain the fixed-size property
     * of the map. Returns false, otherwise
     */
    public boolean isFull() {
        return size() >= MAXIMUM_SIZE;
    }
    
    /**
     * Removes the least recently used entry from the map
     * @return A Map.Entry object
     * Modifies this.
     */
    public Map.Entry<K, V> removeLRUEntry() {
        //if there are no elements, return null.
        if(isEmpty())
            return null;
        
        Iterator<Map.Entry<K, V>> i = entrySet().iterator();
        Map.Entry<K, V> value = i.next();
        i.remove();
        return value;
    }
    
    /**
     * Returns a shallow copy of this Map instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map.
     */
    @SuppressWarnings("unchecked")
    public FixedsizeForgetfulHashMap<K, V> clone() {
        return (FixedsizeForgetfulHashMap<K, V>)super.clone();
    }

    /**
     * Returns true if the eldest entry should be removed.
     */
    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return size() > MAXIMUM_SIZE;
    }

    /**
     * Overridden to ensure that remapping a key renews the value in the
     * linked list.
     */
    @Override
    public V put(K key, V value) {
        V ret = null;
        if(containsKey(key))
            ret = remove(key);
        
        super.put(key, value);
        
        return ret;
    }
}


