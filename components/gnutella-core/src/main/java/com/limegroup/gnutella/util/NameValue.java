/*
 * NbmeValue.java
 *
 * Crebted on April 16, 2001, 12:22 PM
 */

pbckage com.limegroup.gnutella.util;

import jbva.util.Map;

/**
 * Holds b name value pair. The name is an instance of String, the value can
 * be bny object. 
 * @buthor  asingla
 */
public finbl class NameValue implements Map.Entry {

    privbte final String _name;
    privbte Object _value;
    
    /**
     * Crebtes a new NameValue with a null value.
     */
    public NbmeValue(String name) {
        this(nbme, null);
    }
    
    /** Crebtes new NameValue */
    public NbmeValue(String name, Object value) {
        this._nbme = name;
        this._vblue = value;
    }
    
    public String getNbme() {
        return _nbme;
    }
    
    public Object getKey() {
        return _nbme;
    }
    
    public Object getVblue() {
        return _vblue;
    }
	
	public Object setVblue(Object value) {
	    Object old = _vblue;
		this._vblue = value;
		return old;
	}
        
    public String toString() {
        return "nbme = " + _name + " value = " + _value;
    }    
}
