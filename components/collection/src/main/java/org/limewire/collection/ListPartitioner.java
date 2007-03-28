package org.limewire.collection;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Class that partitions a list into sublists with equal size.
 * It could be easily made to implement Iterable<List<E>>
 */
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
        
        int partitionSize = list.size() / Math.min(list.size(), numPartitions);
        if (partitionSize * index >= list.size())
            return Collections.emptyList();
        
        // if the last partition is not full, extend it
        int end = partitionSize * (index + 1);
        if (list.size() - end <= partitionSize && index == numPartitions -1 )
            end = list.size();
        
        return list.subList(partitionSize * index, end);
    }
    
    public List<E> getLastPartition() {
        return getPartition(numPartitions - 1);
    }
    
    public List<E> getFirstPartition() {
        return getPartition(0);
    }
}
