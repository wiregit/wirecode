
package org.limewire.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implements the {@link Iterator} interface. You can use 
 * <code>MultiIterator</code> for a single list, multiple lists and 
 * {@link MultiCollection MultiCollections}.
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

    void sampleCodeMultiIterator(){
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
        
        if(!mc.isEmpty()) {
            MultiIterator&lt;MyObject&gt; miterator = new MultiIterator&lt;MyObject&gt;(l1.iterator(), mc.iterator());
            
            while(miterator.hasNext()){
                System.out.println(miterator.next());
            }
        }
        mc.clear();
    }
    Output:
        0=0
        1=1
        0=0
        1=1
        80=80
        81=81
        800=800    
</pre>
 */

public class MultiIterator<T> implements Iterator<T> {

	protected final Iterator<? extends T> [] iterators;
	protected int current;
    
    @SuppressWarnings("unchecked")
    public MultiIterator(Iterator<? extends T> i1) {
        this.iterators = new Iterator[] { i1 };
    }
    
    @SuppressWarnings("unchecked")
    public MultiIterator(Iterator<? extends T> i1, Iterator<? extends T> i2) {
        this.iterators = new Iterator[] { i1, i2 };
    }
    
    @SuppressWarnings("unchecked")
    public MultiIterator(Iterator<? extends T> i1, Iterator<? extends T> i2, Iterator<? extends T> i3) {
        this.iterators = new Iterator[] { i1, i2, i3 };
    }
    
    @SuppressWarnings("unchecked")
    public MultiIterator(Iterator<? extends T> i1, Iterator<? extends T> i2, Iterator<? extends T> i3, Iterator<? extends T> i4) {
        this.iterators = new Iterator[] { i1, i2, i3, i4 };
    }
	
	public MultiIterator(Iterator<? extends T>... iterators) {
		this.iterators = iterators;
	}
	
	public void remove() {
		if (iterators.length == 0)
			throw new IllegalStateException();
		
		iterators[current].remove();
	}

	public boolean hasNext() {
		for (int i = 0; i < iterators.length; i++) {
			if (iterators[i].hasNext())
				return true;
		}
		return false;
	}

	public T next() {
		if (iterators.length == 0)
			throw new NoSuchElementException();
		
		positionCurrent();
		return iterators[current].next();
	}
	
	protected void positionCurrent() {
		while (!iterators[current].hasNext() && current < iterators.length)
			current++;
	}

}
