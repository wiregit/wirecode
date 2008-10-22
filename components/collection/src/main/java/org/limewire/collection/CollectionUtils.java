package org.limewire.collection;

import java.util.ArrayList;
import java.util.List;

public class CollectionUtils {
    
    private CollectionUtils() {}
    
    public static <T> List<T> listOf(Iterable<T> iterable) {
        List<T> list = new ArrayList<T>();
        for(T t : iterable) {
            list.add(t);
        }
        return list;
    }

}
