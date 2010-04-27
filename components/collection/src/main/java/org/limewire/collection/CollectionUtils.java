package org.limewire.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionUtils {
    
    private CollectionUtils() {}
    
    public static <T> List<T> listOf(Iterator<T> iterator) {
        List<T> list = new ArrayList<T>();
        while(iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }
    
    public static <T> List<T> listOf(Iterable<T> iterable) {
        List<T> list = new ArrayList<T>();
        for(T t : iterable) {
            list.add(t);
        }
        return list;
    }

    /**
     * Converts the given Collection to a Set (if it isn't
     * already a Set).
     */
    public static <T> Set<T> toSet(Collection<T> c) {
        if (c instanceof Set) {
            return (Set<T>)c;
        }
        
        return new LinkedHashSet<T>(c);
    }

    /**
     * Converts the given Collection to a List (if it isn't
     * already a List).
     */
    public static <T> List<T> toList(Collection<T> c) {
        if (c instanceof List) {
            return (List<T>)c;
        }
        
        return new ArrayList<T>(c);
    }

    public static <T> Iterator<T> readOnlyIterator(final Iterator<T> iterator) {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }
            @Override
            public T next() {
                return iterator.next();
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException("read only iterator");
            }
        };
    }

    /**
     * Flattens the collections into a single collection.
     * The returned value is a copy, not a view.
     */
    public static <T> Collection<T> flatten(Collection<? extends Collection<? extends T>> values) {
        List<T> list = new ArrayList<T>();
        for(Collection<? extends T> collection : values) {
            list.addAll(collection);
        }
        return list;
    }


    /**
     * Randomly removes elements from a generic collection
     */
    public static void randomPurge(Collection<?> c, int elementsToRemove) {
        randomPurge(null, c, elementsToRemove);
    }
    
    /**
     * Randomly removes elements from a map
     */
    public static void randomPurge(Map<?,?> m, int elementsToRemove) {
        randomPurge(m, null, elementsToRemove);
    }
    
    @SuppressWarnings("unchecked")
    private static void randomPurge(Map<?,?> m, Collection<?> c, int elementsToRemove) {
        List keys; 
        
        if (m == null) {
            if (c instanceof List) {
                keys = (List) c;
            }
            else {
                keys = new ArrayList(c);
            }
        } else {
            keys = new ArrayList(m.keySet());
        }
        
        for ( int i=0 ; i<elementsToRemove ; i++ ) {
            int randomKeyIndex = (int) (keys.size()*Math.random());
            Object keyToRemove = keys.remove(randomKeyIndex);
            if (m == null) {
                c.remove(keyToRemove);
            } else {
                m.remove(keyToRemove);
            }
        }
    }
}
