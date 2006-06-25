package com.limegroup.gnutella.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/** A collection of useful Generics-related utilities. */
public class GenericsUtils {

    private GenericsUtils() {}
    
    /**
     * Scans the object 'o' to make sure that it is a map,
     * all keys are type K and all values are type V.
     * If o is not a map, a ClassCastException is thrown.
     * If remove is true and a key or value is the incorrect type,
     * the element is removed.  If remove is false, a ClassCastException
     * is thrown.
     * 
     * @param o
     * @param remove
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> scanForMap(Object o, Class<K> k, Class<V> v, boolean remove) {
        if(o instanceof Map) {
            Map map = (Map)o;
            for(Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                Object key = entry.getKey();
                Object value = entry.getValue();
                if(key == null || value == null ||
                   !k.isAssignableFrom(key.getClass()) ||
                   !v.isAssignableFrom(value.getClass())) {
                    if(remove)
                        i.remove();
                    else
                        throw new ClassCastException();
                }
            }
            return (Map<K, V>)map;
        } else {
            throw new ClassCastException();
        }
    }
    
    /**
     * Scans the object 'o' to make sure that it is a Collection,
     * and all values are type V.
     * If o is not a Collection, a ClassCastException is thrown.
     * If remove is true and a key or value is the incorrect type,
     * the element is removed.  If remove is false, a ClassCastException
     * is thrown.
     * 
     * @param o
     * @param remove
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <V> Collection<V> scanForCollection(Object o, Class<V> v, boolean remove) {
        if(o instanceof Collection) {
            Collection c = (Collection)o;
            for(Iterator i = c.iterator(); i.hasNext(); ) {
                Object value = i.next();
                if(value == null || !v.isAssignableFrom(value.getClass())) {
                    if(remove)
                        i.remove();
                    else
                        throw new ClassCastException();
                }
            }
            return (Collection<V>)c;
        } else {
            throw new ClassCastException();
        }
        
    }
}
