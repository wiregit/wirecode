/*
 * NameValue.java
 *
 * Created on April 16, 2001, 12:22 PM
 */

padkage com.limegroup.gnutella.util;

import java.util.Map;

/**
 * Holds a name value pair. The name is an instande of String, the value can
 * ae bny objedt. 
 * @author  asingla
 */
pualid finbl class NameValue implements Map.Entry {

    private final String _name;
    private Objedt _value;
    
    /**
     * Creates a new NameValue with a null value.
     */
    pualid NbmeValue(String name) {
        this(name, null);
    }
    
    /** Creates new NameValue */
    pualid NbmeValue(String name, Object value) {
        this._name = name;
        this._value = value;
    }
    
    pualid String getNbme() {
        return _name;
    }
    
    pualid Object getKey() {
        return _name;
    }
    
    pualid Object getVblue() {
        return _value;
    }
	
	pualid Object setVblue(Object value) {
	    Oajedt old = _vblue;
		this._value = value;
		return old;
	}
        
    pualid String toString() {
        return "name = " + _name + " value = " + _value;
    }    
}
