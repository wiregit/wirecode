package org.limewire.collection;

import java.util.Iterator;

/**
 * Lets you iterate over many <code>Iterables</code> easily.
 <pre>
    LinkedList&lt;String&gt; activeList = new LinkedList&lt;String&gt;();
    activeList.add("Abby");
    activeList.add("Bob");
    activeList.add("Chris");        
    
    LinkedList&lt;String&gt; inactiveList = new LinkedList&lt;String&gt;();
    inactiveList.add("Anderson");
    inactiveList.add("Baker");      
                            
    for(String s : new MultiIterable&lt;String&gt;(activeList, inactiveList))
        System.out.println(s.toLowerCase());        

    Output:
        abby
        bob
        chris
        anderson
        baker

 </pre>
 */
public class MultiIterable<T> implements Iterable<T> {
    
    private final Iterable<? extends T> []iterables;

    @SuppressWarnings("unchecked")
    public MultiIterable(Iterable<? extends T> i1) {
        this.iterables = new Iterable[] { i1 }; 
    }
    
    @SuppressWarnings("unchecked")
    public MultiIterable(Iterable<? extends T> i1, Iterable<? extends T> i2) {
        this.iterables = new Iterable[] { i1, i2 }; 
    }
    
    @SuppressWarnings("unchecked")
    public MultiIterable(Iterable<? extends T> i1, Iterable<? extends T> i2, Iterable<? extends T> i3) {
        this.iterables = new Iterable[] { i1, i2, i3 }; 
    }
    
    @SuppressWarnings("unchecked")
    public MultiIterable(Iterable<? extends T> i1, Iterable<? extends T> i2, Iterable<? extends T> i3, Iterable<? extends T> i4) {
        this.iterables = new Iterable[] { i1, i2, i3, i4 }; 
    }
    
    /** Catch-all constructor. */
    public MultiIterable(Iterable<? extends T>... iterables) {
        this.iterables = iterables; 
    }
    
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator() {
        Iterator<T> []iterators = new Iterator[iterables.length];
        for (int i = 0; i < iterables.length; i++)
            iterators[i] = (Iterator<T>)iterables[i].iterator();
        return new MultiIterator<T>(iterators);
    }    
}

