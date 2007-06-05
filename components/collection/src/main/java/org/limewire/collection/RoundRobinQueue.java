package org.limewire.collection;

import java.util.Iterator;
import java.util.LinkedList;



/**
 * Creates a round-robin queue. {@link #next()} returns an item on the queue and then puts 
 * that item to the end of the queue.
 <pre>

    void sampleCodeRoundRobinQueue(){
        class myarray{
            int [] ar;
            int Index;
            public String Get(){
                if(Index == Size())                 
                    Index = 0;      
                return "[" + ar[Index++] + "]";
            }
            
            public int Size(){
                return ar.length;
            }

            public myarray(int a){
                Index = 0;
                ar = new int [2];
                
                switch(a){
                case 0:
                    ar[0] = 1;ar[1] = 2;
                break;
                case 1:
                    ar[0] = 11;ar[1] = 12;
                break;
                }
            }
        }
        RoundRobinQueue&lt;myarray&gt; rrq = new RoundRobinQueue&lt;myarray&gt;();
        
        LinkedList&lt;myarray&gt; ll = new LinkedList&lt;myarray&gt;();
        ll.add(new myarray(0));
        ll.add(new myarray(1));
        
        Iterator&lt;myarray&gt; iter = ll.iterator();
        while(iter.hasNext())
            rrq.enqueue(iter.next());
        
        for(int i = 0; i < rrq.size() * new myarray(0).Size() ; i++){
            System.out.println(rrq.next().Get());
        }   
    }
    Output:
        [1]
        [11]
        [2]
        [12]
 </pre>
 * 
 */
public class RoundRobinQueue<T>  {

	private LinkedList<T> _current;
	
	
	/**
	 * do not create the terminating elements
	 */
	public RoundRobinQueue() {
		_current = new LinkedList<T>();
		

	}
	
	/**
	 * enqueues the specified object in the round-robin queue.
	 * @param value the object to add to the queue
	 */
	public synchronized void enqueue(T value) {
		
		_current.addLast(value);
		
	}
	
	/**
	 * @return the next object in the round robin queue
	 */
	public synchronized T next() {
		T ret = _current.removeFirst();
		_current.addLast(ret);
		return ret;
	}
	
	/**
	 * Removes the next occurrence of the specified object
	 * @param o the object to remove from the queue. 
	 */
	public synchronized void remove (Object o) {
		_current.remove(o);
	}
	
	/**
	 * Removes all occurrences of the given object in the list.
	 * @param o the object to remove.
	 */
	public synchronized void removeAllOccurences(Object o) {
		Iterator iterator = _current.iterator();
		while(iterator.hasNext())
			if (iterator.next().equals(o))
				iterator.remove();
			
	}
	
	public synchronized int size() {
		return _current.size();
	}
	
	public synchronized void clear() {
		_current.clear();
	}
		
}
