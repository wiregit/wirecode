package org.limewire.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CollectionUtils {
    
    private CollectionUtils() {}
    
    public static <T> List<T> listOf(Iterable<T> iterable) {
        List<T> list = new ArrayList<T>();
        for(T t : iterable) {
            list.add(t);
        }
        return list;
    }

    /**
     * Converts the given Collection to a Set (if it isn't
     * already a Set)
     */
    public static <T> Set<T> toSet(Collection<T> c) {
        if (c instanceof Set) {
            return (Set<T>)c;
        }
        
        return new LinkedHashSet<T>(c);
    }

    /**
     * Converts the given Collection to a List (if it isn't
     * already a List)
     */
    public static <T> List<T> toList(Collection<T> c) {
        if (c instanceof List) {
            return (List<T>)c;
        }
        
        return new ArrayList<T>(c);
    }

}
