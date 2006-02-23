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
public class NameValue implements Map.Entry {

    private final String _name;
    private Object _value;
    
    /**
     * Creates a new NameValue with a null value.
     */
    public NameValue(String name) {
        this(name, null);
    }
    
    /** Creates new NameValue */
    public NameValue(String name, Object value) {
        this._name = name;
        this._value = value;
    }
    
    public String getName() {
        return _name;
    }
    
    public Object getKey() {
        return _name;
    }
    
    public Object getValue() {
        return _value;
    }
	
	public Object setValue(Object value) {
	    Object old = _value;
		this._value = value;
		return old;
	}
        
    public String toString() {
        return "name = " + _name + " value = " + _value;
    }
    
    public static class ComparableByName extends NameValue implements Comparable {
        public ComparableByName(String name) {
            super(name);
        }
        
        public ComparableByName(String name, Object value) {
            super(name, value);
        }
        
        public int compareTo(Object other) {
            ComparableByName b = (ComparableByName)other;            
            if(other == null)
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
