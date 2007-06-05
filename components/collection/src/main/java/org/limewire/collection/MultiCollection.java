package org.limewire.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Contains a collection of {@link Collection Collections}.
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

    void sampleCodeMultiCollection(){
        LinkedList&lt;MyObject&gt; l1 = new LinkedList&lt;MyObject&gt;();
        for(int i = 0; i < 5; i++)
            if(!l1.add(new MyObject(String.valueOf(i), i)))
                System.out.println("add failed " + i);  
        
        LinkedList&lt;MyObject&gt; l2 = new LinkedList&lt;MyObject&gt;();
        for(int i = 80; i < 89; i++)
            if(!l2.add(new MyObject(String.valueOf(i), i)))
                System.out.println("add failed " + i);  
                
        MyObject itemToAdd = new MyObject(String.valueOf(800), 800);
        if(!l2.add(itemToAdd))
            System.out.println("itemToAdd failed.");    
                
        MultiCollection&lt;MyObject&gt; mc = new MultiCollection&lt;MyObject&gt;(l1, l2);
        System.out.println("l1 size is: " + l1.size());
        System.out.println("l2 size is: " + l2.size());
        System.out.println("mc size is: " + mc.size());
        
        if(!mc.contains(itemToAdd))
            System.out.println("contains failed."); 
        else{
            if(!mc.remove(itemToAdd))
                System.out.println("remove failed.");   
            else
                System.out.println("mc size after remove: " + mc.size());
        }
        
        if(!mc.isEmpty()) {
            LinkedList&lt;MyObject&gt; copyOfList2 = (LinkedList&lt;MyObject&gt;)l2.clone();
                        
            if(!mc.containsAll(l2))
                System.out.println("containsAll all failed.");
            else{   
                if(!mc.removeAll(copyOfList2))
                    System.out.println("remove all failed.");
                else{
                    System.out.println("mc size after removing copyOfList2: " + mc.size());
                }           
            }
        }
        mc.clear();
        System.out.println("mc size is: " + mc.size());
    }
    Output:
        l1 size is: 5
        l2 size is: 10
        mc size is: 15
        mc size after remove: 14
        mc size after removing copyOfList2: 5
        mc size is: 0
    
 </pre>
 * 
 */
public class MultiCollection<T> extends MultiIterable<T> implements Collection<T> {
	
	private final Iterable<Collection<? extends T>> collections;

	public MultiCollection(Collection<? extends T> i1, Collection<? extends T> i2) {
		super(i1, i2);
		List<Collection<? extends T>> l = new ArrayList<Collection<? extends T>>(2);
		l.add(i1);
		l.add(i2);
		this.collections = l;
	}

	@SuppressWarnings("cast")
    public MultiCollection(Collection<? extends T>... collections) {
		super((Iterable<? extends T>[])collections);
		List<Collection<? extends T>> l = new ArrayList<Collection<? extends T>>(collections.length);
		for (Collection<? extends T> o : collections)
			l.add(o);
		this.collections = l;
	}
	
	public boolean add(T o) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection<? extends T> c) {
		throw new UnsupportedOperationException();	
	}

	public void clear() {
		for (Collection c : collections)
			c.clear();
	}

	public boolean contains(Object o) {
		for (Collection c : collections) {
			if (c.contains(o))
				return true;
		}
		return false;
	}

    public boolean containsAll(Collection<?> c) { 
        for (Object obj : c) { 
            if (!contains(obj)) 
                return false; 
        } 
        return true; 
    } 

	public boolean isEmpty() {
		for (Collection c : collections) {
			if (!c.isEmpty())
				return false;
		}
		return true;
	}

	public boolean remove(Object o) {
		for (Collection c : collections) {
			if (c.remove(o))
				return true;
		}
		return false;
	}
	public boolean removeAll(Collection<?> c) {
		boolean ret = false;
		for (Object o : c) {
			if (remove(o))
				ret = true;
		}
		return ret;
	}

	public boolean retainAll(Collection<?> c) {
		boolean ret = false;
		for (Collection<? extends T> col : collections) {
			if (col.retainAll(c))
				ret = true;
		}
		return ret;
	}

	public int size() {
		int ret = 0;
		for (Collection c : collections) 
			ret += c.size();
		return ret;
	}

	@SuppressWarnings("unchecked")
	public Object[] toArray() {
		List t = new ArrayList(size());
		for (Collection c : collections) {
			t.addAll(c);
		}
		return t.toArray();
	}

	@SuppressWarnings("unchecked")
	public <B>B[] toArray(B[] a) {
		List<B> t = new ArrayList<B>(size());
		for (Collection c : collections) {
			t.addAll(c);
		}
		return t.toArray(a);
	}
}
