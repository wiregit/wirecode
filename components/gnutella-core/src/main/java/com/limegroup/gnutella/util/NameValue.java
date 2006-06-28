/*
 * NameValue.java
 *
 * Created on April 16, 2001, 12:22 PM
 */

package com.limegroup.gnutella.util;

import java.util.Map;

/**
 * Holds a name value pair. The name is an instance of String, the value can
 * be any object. 
 * @author  asingla
 */
public class NameValue <V> implements Map.Entry<String, V> {

    private final String _name;
    private V _value;
    
    /**
     * Creates a new NameValue with a null value.
     */
    public NameValue(String name) {
        this(name, null);
    }
    
    /** Creates new NameValue */
    public NameValue(String name, V value) {
        this._name = name;
        this._value = value;
    }
    
    public String getName() {
        return _name;
    }
    
    public String getKey() {
        return _name;
    }
    
    public V getValue() {
        return _value;
    }
	
	public V setValue(V value) {
	    V old = _value;
		this._value = value;
		return old;
	}
        
    public String toString() {
        return "name = " + _name + " value = " + _value;
    }
    
    public static class ComparableByName<V> extends NameValue<V> implements Comparable<ComparableByName> {
        public ComparableByName(String name) {
            super(name);
        }
        
        public ComparableByName(String name, V value) {
            super(name, value);
        }
        
        public int compareTo(ComparableByName b) {            
            if(b == null)
                return 1;
            String nameB = b.getName();
            String name = getName();
            if(name == null && nameB == null)
                return 0;
            else if(name == null)
                return -1;
            else if(nameB == null)
                return 1;
            else
                return name.compareTo(nameB);
        }
    }
}
