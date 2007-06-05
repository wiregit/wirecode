package org.limewire.collection;

import java.util.Iterator;

/**
 * Provides an iterator that iterates over two other iterators, in order.
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

    public void sampleCodeDualIterator(){
        LinkedList&lt;MyObject&gt; l1 = new LinkedList&lt;MyObject&gt;();
        LinkedList&lt;MyObject&gt; l2 = new LinkedList&lt;MyObject&gt;();
        for(int i = 0; i < 5; i++)
            if(!l1.add(new MyObject(String.valueOf(i), i)))
                System.out.println("add failed " + i);  
        for(int i = 10; i < 15; i++)
            if(!l2.add(new MyObject(String.valueOf(i), i)))
                System.out.println("add failed " + i);  
        DualIterator&lt;MyObject&gt; di = new DualIterator&lt;MyObject&gt;(l1.iterator(), l2.iterator());
        
        while(di.hasNext())
            System.out.println(di.next());      
    }

    Output:
        0=0
        1=1
        2=2
        3=3
        4=4
        10=10
        11=11
        12=12
        13=13
        14=14

</pre>
 * 
 * 
 */
public class DualIterator<T> implements Iterator<T> {
    
    /**
     * The primary iterator.
     */
    private final Iterator<T> i1;
    
    /**
     * The secondary iterator.
     */
    private final Iterator<T> i2;
    
    /**
     * Whether or not you have reached the secondary iterator.
     */
    private boolean onOne;
    
    /**
     * Constructs a new DualIterator backed by two iterators.
     */
    public DualIterator(Iterator<T> a, Iterator<T> b) {
        i1 = a; i2 = b;
        onOne = true;
    }
    
    /**
     * Determines if there are any elements left in either iterator.
     */
    public boolean hasNext() {
        return i1.hasNext() || i2.hasNext();
    }
    
    /**
     * Retrieves the next element from the current backing iterator.
     */
    public T next() {
        if(i1.hasNext())
            return i1.next();
        else {
            onOne = false;
            return i2.next();
        }
    }
    
    /**
     * Removes the element from the current backing iterator.
     */
    public void remove() {
        if(onOne)
            i1.remove();
        else
            i2.remove();
    }
}
