/*
 * NameValue.java
 *
 * Created on April 16, 2001, 12:22 PM
 */

package com.limegroup.gnutella.util;

/**
 * Holds a name value pair. The name is an instance of String, the value can
 * be any object. 
 * @author  asingla
 */
public final class NameValue {

    private final String _name;
    private final Object _value;
    
    /** Creates new NameValue */
    public NameValue(String name, Object value) {
        this._name = name;
        this._value = value;
    }
    
    public String getName() {
        return _name;
    }
    
    public Object getValue() {
        return _value;
    }
        
    public String toString() {
        return "name = " + _name + " value = " + _value;
    }    
}
