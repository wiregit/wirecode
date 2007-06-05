package org.limewire.collection;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Partitions a list into sublists with equal size. The remainder items in the 
 * list are included with the last sublist. For example, list = {1,2,3,4,5} and 2
 * partitions makes subList1 = {1,2} and subList2 = {3,4,5}.
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

    void sampleCodeListPartitioner(){
        LinkedList&lt;MyObject&gt; l = new LinkedList&lt;MyObject&gt;();
        for(int i = 1; i < 6; i++)
            if(!l.add(new MyObject(String.valueOf(i), i)))
                System.out.println("add failed " + i);  
        for(MyObject o : l)
            System.out.println(o);
        
        ListPartitioner&lt;MyObject&gt; lp = new ListPartitioner&lt;MyObject&gt;(l, 2);
        List&lt;MyObject&gt; p1 = lp.getPartition(0);
        List&lt;MyObject&gt; p2 = lp.getPartition(1);
        
        System.out.println("***partition 1***");
        for(MyObject o : p1)
            System.out.println(o);
        System.out.println("***partition 2***");
        for(MyObject o : p2)
            System.out.println(o);
    }
    Output:    
        1=1
        2=2
        3=3
        4=4
        5=5
        ***partition 1***
        1=1
        2=2
        ***partition 2***
        3=3
        4=4
        5=5
</pre>
 */ 

// ListPartitioner could be easily made to implement Iterable<List<E>>
 
public class ListPartitioner<E> {
    private final List<E> list;
    private final int numPartitions;
    
    public ListPartitioner(List<E> list, int numPartitions) {
        assert numPartitions > 0;
        this.list = list;
        this.numPartitions = numPartitions;
    }
    
    public List<E> getPartition(int index) {
        if (index >= numPartitions)
            throw new NoSuchElementException();
        if (numPartitions == 1)
            return list;
        if (list.isEmpty())
            return Collections.emptyList();
        
        int partitionSize = list.size() / numActivePartitions();
        if (partitionSize * index >= list.size())
            return Collections.emptyList();
        
        // if the last partition is not full, extend it
        int end = partitionSize * (index + 1);
        if (list.size() - end <= partitionSize && index == numPartitions -1 )
            end = list.size();
        
        return list.subList(partitionSize * index, end);
    }
    
    public List<E> getLastPartition() {
        return getPartition(numActivePartitions() - 1);
    }
    
    private int numActivePartitions() {
        return Math.min(list.size(), numPartitions);
    }
    
    public List<E> getFirstPartition() {
        return getPartition(0);
    }
}
