package org.limewire.collection;

import java.util.Iterator;

/**
 * Implements the {@link Iterable} interface to allow an object to be part of a
 * "for each" statement. You can use <code>MultiIterable</code> for a single 
 * list, multiple lists and {@link MultiCollection MultiCollections}.
 * 
 <pre>
    public class MyObject{
        public String s;
        public int item;
        public MyObject(String s, int item){
            this.s = s;
            this.item = item;
        }       

        public String toString(){
            return s + "=" + item ;
        }
    }   

    void sampleCodeMultiIterable(){

        LinkedList&lt;MyObject&gt; l1 = new LinkedList&lt;MyObject&gt;();
        for(int i = 0; i < 2; i++)
            if(!l1.add(new MyObject(String.valueOf(i), i)))
                System.out.println("add failed " + i);  
        
        LinkedList&lt;MyObject&gt; l2 = new LinkedList&lt;MyObject&gt;();
        for(int i = 80; i < 82; i++)
            if(!l2.add(new MyObject(String.valueOf(i), i)))
                System.out.println("add failed " + i);  
                
        MyObject itemToAdd = new MyObject(String.valueOf(800), 800);
        if(!l2.add(itemToAdd))
            System.out.println("itemToAdd failed.");    
                
        MultiCollection&lt;MyObject&gt; mc = new MultiCollection&lt;MyObject&gt;(l1, l2);
        
        mc.iterator();
        if(!mc.isEmpty()) {
            MultiIterable&lt;MyObject&gt; miterable = new MultiIterable&lt;MyObject&gt;(l2, mc);
            for(MyObject o : miterable)
                System.out.println(o);  
        }
        mc.clear();     
    }
    Output:
        80=80
        81=81
        800=800
        0=0
        1=1
        80=80
        81=81
        800=800    
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

