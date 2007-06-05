
package org.limewire.collection;

import java.util.HashSet;
import java.util.Set;

/**
 * Creates a round-robin queue where types are unique.
<pre>

    public void sampleCodeRRSetQueue(){
        class myarray {
            int [] ar;
            int Index;
            
            public boolean equals(Object obj) {
                if(obj == this) return true;
                myarray other = (myarray)obj;
                int ret = 0;
                for(int i = 0; i < ar.length && i < other.ar.length; i++){
                    ret = ret + (ar[i] - other.ar[i]);
                }
                return (ret == 0);
            }
            public int hashCode() {
                int ret = 0;
                for(int i = 0; i < ar.length; i++){
                    ret = ret + (ar[i]);
                }
                return ret;
            }
                        
            public String Get(){            
                   if(Index == Size())
                       Index = 0;               
                return "[" + ar[Index++] + "]";
            }
            
            public int Size(){
                return ar.length;
            }
            
            public String toString(){
                String s = "";
                for(int i = 0; i < ar.length; i++){
                    s = s + ar[i];
                    s = s + " ";
                }
                return s;
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
        RoundRobinSetQueue&lt;myarray&gt; rrq = new RoundRobinSetQueue&lt;myarray&gt;();
        
        LinkedList&lt;myarray&gt; ll = new LinkedList&lt;myarray&gt;();
        ll.add(new myarray(0));
        ll.add(new myarray(1));
            
        ll.add(new myarray(1));//duplicate that isn't enqueue'd in rrq
        
        Iterator&lt;myarray&gt; iter = ll.iterator();

        System.out.println("Contents of iter:");
        while(iter.hasNext())
            System.out.println(iter.next());

        iter = ll.iterator();
        while(iter.hasNext())
            rrq.enqueue(iter.next());
        
        System.out.println("rrq.size(): " + rrq.size());
        System.out.println("Contents of rrq:");

        for(int i = 0; i < rrq.size() * new myarray(0).Size(); i++)
            System.out.println(rrq.next().Get());
    }
    Output:
        Contents of iter:
        1 2 
        11 12 
        11 12 
        rrq.size(): 2
        Contents of rrq:
        [1]
        [11]
        [2]
        [12]
    
</pre>
 */
public class RoundRobinSetQueue<T> extends RoundRobinQueue<T> {
	
	private Set<T> _uniqueness;
	
	public RoundRobinSetQueue() {
		super();
		_uniqueness =  new HashSet<T>();
	}

	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.util.RoundRobinQueue#enqueue(java.lang.Object)
	 */
	public synchronized void enqueue(T value) {
		if (_uniqueness.add(value)) 
			super.enqueue(value);
		
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.util.RoundRobinQueue#remove(java.lang.Object)
	 */
	public synchronized void remove(Object o) {
		if (_uniqueness.contains(o)) {
			_uniqueness.remove(o);
			super.remove(o);
		}
		
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.util.RoundRobinQueue#removeAllOccurences(java.lang.Object)
	 */
	public synchronized void removeAllOccurences(Object o) {
		remove(o);
	}
}
