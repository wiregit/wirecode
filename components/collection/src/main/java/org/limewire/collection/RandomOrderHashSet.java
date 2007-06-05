package org.limewire.collection;

import java.util.Collection;
import java.util.Iterator;

/**
 * A variant of {@link FixedSizeArrayHashSet} that allows iterations over
 * its elements in random order. 
 * 
<pre>

    void sampleCodeRandomOrderHashSet(){

        LinkedList&lt;Integer&gt; linkedlist1 = new LinkedList&lt;Integer&gt;();
        for(int i = 0; i < 5; i++)
            if(!linkedlist1.add(i))
                System.out.println("add failed " + i);  
                            
        RandomOrderHashSet&lt;Integer&gt; rohs = new RandomOrderHashSet&lt;Integer&gt;(linkedlist1);
        
        Iterator&lt;Integer&gt; iter = rohs.iterator();
        
        while(iter.hasNext()){
            System.out.println(iter.next());
        }
    }
    Random Output:
        3
        0
        2
        4
        1
</pre>
 */
public class RandomOrderHashSet<T> extends FixedSizeArrayHashSet<T> {

    public RandomOrderHashSet(Collection<? extends T> c) {
        super(c);
    }
    
    public RandomOrderHashSet(int capacity, Collection<? extends T> c) {
        super(capacity, c);
    }

    public RandomOrderHashSet(int maxSize, int initialCapacity, float loadFactor) {
        super(maxSize, initialCapacity, loadFactor);
    }

    public RandomOrderHashSet(int initialCapacity) {
        super(initialCapacity);
    }

    public Iterator<T> iterator() {
        return new RandomIterator();
    }
    
    private class RandomIterator extends UnmodifiableIterator<T> {
        private final Iterator<Integer> sequence = new RandomSequence(size()).iterator();
        
        public boolean hasNext() {
            return sequence.hasNext();
        }
        
        public T next() {
            return get(sequence.next());
        }
    }
}
