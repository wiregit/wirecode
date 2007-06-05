package org.limewire.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * Extends {@link TreeList} to sort elements upon insertion.
 * 
 * <pre>
 * 
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

    void sampleCodeSortedList(){

        SortedList&lt;MyComparableObject&gt; sl = new SortedList&lt;MyComparableObject&gt;();
        sl.add(new MyComparableObject("B", 2));
        sl.add(new MyComparableObject("C", 3));
        sl.add(new MyComparableObject("D", 4));
        sl.add(new MyComparableObject("A", 1));
        sl.add(new MyComparableObject("B", 2));
        System.out.println("Last element: " + sl.last());   
        System.out.println(sl); 
    }

    Output:
        Last element: D=4
        [A=1, B=2, B=2, C=3, D=4]
        ***sampleCodePair***
        Compare A to B: -10
        Get element p1: A
 </pre>
 */
public class SortedList<E> extends TreeList<E> {
    private final Comparator<? super E> comparator;
    
    public SortedList() {
        super();
        comparator = new DefaultComparator<E>();
    }
    
    public SortedList(Comparator <? super E> comparator) {
        super();
        this.comparator = comparator;
    }

    public SortedList(Collection<? extends E> c) {
        this(c, new DefaultComparator<E>());
    }
    
    public SortedList(Collection<? extends E> c, Comparator<? super E>comparator) {
        super(c);
        this.comparator = comparator;
    }

    public boolean add(E e) {
        add(getIndex(e), e);
        return true;
    }
    
    private int getIndex(E e) {
        int point = Collections.binarySearch(this, e, comparator);
        if (point < 0)
            point = -(point + 1);
        return point;
    }
    
    public E first() {
        if (isEmpty())
            throw new NoSuchElementException();
        return get(0);
    }
    
    public E last() {
        if (isEmpty())
            throw new NoSuchElementException();
        return get(size() - 1);
    }
    
    public Comparator<? super E> comparator() {
        return comparator;
    }
    
    @SuppressWarnings("unchecked")
    private static class DefaultComparator<E> implements Comparator<E> {
        public int compare(E a, E b) {
            Comparable<? super E> ac = (Comparable <? super E>)a;
            return ac.compareTo(b);
        }
    }
}
