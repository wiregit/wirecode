package org.limewire.collection;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a fixed size {@link HashMap}. If <code>FixedsizeHashMap</code> 
 * gets full, no new entry can be inserted into it, except by removing an 
 * entry first. An attempt to add new entry throws a {@link NoMoreStorageException}.
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

    void sampleCodeFixedsizeHashMap(){
        try{
            FixedsizeHashMap&lt;String, MyObjectHash&gt; fhm = new FixedsizeHashMap&lt;String, MyObjectHash&gt;(3);
            
            MyObjectHash mohReturn;
            mohReturn = fhm.put("Mykey1", new MyObjectHash("a", 1));
            if(mohReturn != null)
                System.out.println("error with put1");
            else
                System.out.println("1) " + fhm);    

            mohReturn = fhm.put("Mykey2", new MyObjectHash("b", 2));
            if(mohReturn != null)
                System.out.println("error with put2");
            else
                System.out.println("2) " + fhm);    

            mohReturn = fhm.put("Mykey3", new MyObjectHash("c", 3));
            if(mohReturn != null)
                System.out.println("error with put3");
            else
                System.out.println("3) " + fhm);    
    
            mohReturn = fhm.put("Mykey3", new MyObjectHash("replace", 3));
            if(mohReturn != null)
                System.out.println("put returned: " + mohReturn);
            System.out.println("4) " + fhm);    

            mohReturn = fhm.put("Mykey4", new MyObjectHash("d", 4));
            if(mohReturn != null)
                System.out.println("Error with put, because of maximum size." + mohReturn);
            else
                System.out.println("5) " + fhm);    
        }
        catch(Exception e){
            System.out.println("Exception because of maximum size upon put Mykey4 ... 
            " + e.toString() );
        }   
    }
    Output:
        1) {Mykey1=a=1}
        2) {Mykey2=b=2, Mykey1=a=1}
        3) {Mykey2=b=2, Mykey3=c=3, Mykey1=a=1}
        put returned: c=3
        4) {Mykey2=b=2, Mykey3=replace=3, Mykey1=a=1}
        Exception because of maximum size upon put Mykey4 ... 
                        org.limewire.collection.NoMoreStorageException

</pre>
 * 
 */
public class FixedsizeHashMap<K, V> {
    
    /**
     * The underlying storage
     */
    private final Map<K, V> hashMap;
    
    /**
     * The max number of elements that can be stored
     */
    private final int maxSize;
    
    /**
     * Create a new hashMap that stores only the specified number of entries
     *
     * @param size the number of entries to hold
     * @exception IllegalArgumentException if size is less < 1.
     */
    public FixedsizeHashMap(int size)
    {
        hashMap = new HashMap<K, V>(size * 4/3);
        this.maxSize = size;
    }
    
    /**
     * Maps the given key to the given value. If adding the key
     * would make this contain more elements than the size given at
     * construction, the passed entry is not stored and NoMoreStorageException
     * gets thrown.
     * @exception NoMoreStorageException when no more space left in the storage
     * ideally, before calling put method, it should be checked whether the map is
     * already full or not
     */
    public synchronized V put(K key, V value) throws NoMoreStorageException
    {
        V retValue = null;
        
        //check if the count is less than size, in that case no problem
        //inserting this new entry
        if(hashMap.size() < maxSize) 
            retValue = hashMap.put(key,value);
        else {
            //if the entry already existed, we can safely add this new pair
            //without affecting the size
            retValue = hashMap.get(key);
            
            if(retValue != null) //mapping existed, so update the mapping 
                retValue = hashMap.put(key,value);
            else //no space to enter anything more 
                throw new NoMoreStorageException();
        }
        
        return retValue;
    }
    
    /**
     * Returns the value mapped to the given key
     * @param key The given key
     * @return the value given key maps to
     */
    public synchronized V get(K key) {
        return hashMap.get(key);
    }
    
    /**
     * clears all entries from the map.
     */
    public synchronized void clear() {
        hashMap.clear();
    }
    
    /**
     * Returns the string representation of the mappings
     * @return the string representation of the mappings
     */
    public synchronized String toString() {
        return hashMap.toString();
    }
    
    
}
