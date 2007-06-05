package org.limewire.collection;

import java.util.Collection;
import java.util.Comparator;

/**
 * Gives a sorted list of elements with a maximum size. Elements are sorted
 * upon insertion to the list, but only a fixed number of items are allowed. 
 * Therefore, if the list has reached the capacity, the last ordered element
 * is removed and then the new element is inserted in the proper location.
 * 
 <pre>
    public class MyComparableObject implements Comparable&lt;MyComparableObject&gt;{
        public String s;
        public int item;
        public MyComparableObject(String s, int item){
            this.s = s;
            this.item = item;
        }       

        public String toString(){
            return s + "=" + item;
        }
                
        public int compareTo(MyComparableObject other) {
            int c = this.s.compareTo(other.s);//compare by the string
            if(c == 0){//if the string is equal, compare by item
                c = this.item - other.item;
            }
            return c;
        }       
    }   

    void sampleCodeFixedSizeSortedList(){
        FixedSizeSortedList&lt;MyComparableObject&gt; fssl = new FixedSizeSortedList&lt;MyComparableObject&gt;(5);
        
        if( fssl.add(new MyComparableObject("a", 1)))
            System.out.println("1: " + fssl);
        if( fssl.add(new MyComparableObject("a", 2)))
                System.out.println("2: " + fssl);
        if( fssl.add(new MyComparableObject("b", 1)))
            System.out.println("3: " + fssl);
        if( fssl.add(new MyComparableObject("c", 1)))
            System.out.println("4: " + fssl);
        if( fssl.add(new MyComparableObject("d", 1)))
            System.out.println("5: " + fssl);
        if( fssl.add(new MyComparableObject("e", 1)))
            System.out.println("6: " + fssl);
        if( fssl.add(new MyComparableObject("a", 1)))
            System.out.println("7: " + fssl);
    }
    Output:
        1: [a=1]
        2: [a=1, a=2]
        3: [a=1, a=2, b=1]
        4: [a=1, a=2, b=1, c=1]
        5: [a=1, a=2, b=1, c=1, d=1]
        6: [a=1, a=2, b=1, c=1, e=1]
        7: [a=1, a=1, a=2, b=1, c=1]    
</pre>
*/ 
public class FixedSizeSortedList<E> extends SortedList<E> {
    private final int capacity;

    public FixedSizeSortedList(int capacity) {
        this.capacity = capacity;
    }

    public FixedSizeSortedList(Collection<? extends E> c, Comparator<? super E> comparator, int capacity) {
        super(c, comparator);
        this.capacity = capacity;
    }

    public FixedSizeSortedList(Collection<? extends E> c, int capacity) {
        super(c);
        this.capacity = capacity;
    }

    public FixedSizeSortedList(Comparator<? super E> comparator, int capacity) {
        super(comparator);
        this.capacity = capacity;
    }
    
    public boolean add(E e) {
        if (size() == capacity)
            remove(last());
        return super.add(e);
    }
    
    public E insert(E e) {
        E ret = null;
        if (size() == capacity) {
            ret = last();
            if (comparator().compare(e, ret) < 0)
                return e;
            remove(ret);
        }
        add(e);
        return ret;
    }
}

