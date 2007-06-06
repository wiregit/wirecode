
package org.limewire.collection;

import java.util.Iterator;

/**
 * Provides a round robin iterator, visiting one item per collection before 
 * moving to the next item in the collection.
<pre>
    void sampleCodeMultiRRIterator(){
        LinkedList&lt;Integer&gt; l1 = new LinkedList&lt;Integer&gt;();
        for(int i = 0; i < 5; i++)
            if(!l1.add(i))
                System.out.println("add failed " + i);  
        LinkedList&lt;Integer&gt; l2 = new LinkedList&lt;Integer&gt;();
        for(int i = 80; i < 85; i++)
            if(!l2.add(i))
                System.out.println("add failed " + i);  
                                
        MultiCollection&lt;Integer&gt; mc = new MultiCollection&lt;Integer&gt;(l1, l2);
        if(!mc.isEmpty()) {
            MultiRRIterator&lt;Integer&gt; mRRiterator = new MultiRRIterator&lt;Integer&gt;(l1.iterator(), l2.iterator());
            
            while(mRRiterator.hasNext()){
                System.out.println(mRRiterator.next());
            }
        }
        mc.clear();     
    }
    Output:
        0
        80
        1
        81
        2
        82
        3
        83
        4
        84
    
</pre>
 */

public class MultiRRIterator<T> extends MultiIterator<T> {
	
    public MultiRRIterator(Iterator<? extends T> i1) {
        super(i1);
        current = iterators.length - 1;
    }
    
    public MultiRRIterator(Iterator<? extends T> i1, Iterator<? extends T> i2) {
        super(i1, i2);
        current = iterators.length - 1;
    }
    
    public MultiRRIterator(Iterator<? extends T> i1, Iterator<? extends T> i2, Iterator<? extends T> i3) {
        super(i1, i2, i3);
        current = iterators.length - 1;
    }
    
    public MultiRRIterator(Iterator<? extends T> i1, Iterator<? extends T> i2, Iterator<? extends T> i3, Iterator<? extends T> i4) {
        super(i1, i2, i3, i4);
        current = iterators.length - 1;
    }
    
	public MultiRRIterator(Iterator<? extends T> ... iterators ) {
		super(iterators);
		current = iterators.length - 1;
	}
	
	protected void positionCurrent() {
		int steps = 0;
		while (steps <= iterators.length) {
			if (current == iterators.length-1)
				current = -1;
			if (iterators[++current].hasNext())
				break;
			steps++;
		}
	}
}
