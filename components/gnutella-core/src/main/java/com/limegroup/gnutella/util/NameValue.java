
// Commented for the Learning branch

package com.limegroup.gnutella.util;

import java.util.Map;

/**
 * A NameValue object holds one name and value pair.
 * The name is a String, and the value can be any object.
 * You can store an object under a name with an object of this class.
 * With a list of NameValue pairs, you've created a Map.
 * 
 * The PingRequest class uses NameValue objects.
 * 
 * This NameValue class implements the Map.Entry interface.
 * Map.Entry is an interface nested within the Map class.
 * It requires methods like getKey(), getValue(), and setValue().
 * 
 * NameValue.java
 * Created on April 16, 2001, 12:22 PM
 * @author asingla
 */
public final class NameValue implements Map.Entry {

    /** The name that identifies this NameValue pair, the key. */
    private final String _name;

    /** The object stored under the associated name, the value. */
    private Object _value;

    /**
     * Make a new NameValue with the given name, and a null value.
     * 
     * @param name The name for this new NameValue pair
     */
    public NameValue(String name) {

        // Call the next constructor, setting the name and leaving the value null
        this(name, null);
    }

    /**
     * Make a new NameValue with the given name and object.
     * This NameValue object will keep the value under the given name.
     * 
     * @param name  The name key
     * @param value The object value
     */
    public NameValue(String name, Object value) {

        // Save the given name key and object value
        this._name  = name;
        this._value = value;
    }

    /**
     * Get the name.
     * 
     * This is the key for this NameValue pair.
     * You can identify the object in this NameValue pair by this name.
     * 
     * The getName() and getKey() methods do the same thing.
     * 
     * @return The String name, the key for this NameValue pair
     */
    public String getName() {

        // Return the String name, the key for this NameValue pair
        return _name;
    }

    /**
     * Get the name.
     * 
     * This is the key for this NameValue pair.
     * You can identify the object in this NameValue pair by this name.
     * 
     * The getName() and getKey() methods do the same thing.
     * 
     * @return The String name, the key for this NameValue pair
     */
    public Object getKey() {

        // Return the String name, the key for this NameValue pair
        return _name;
    }

    /**
     * Get the object.
     * 
     * This is the value for this NameValue pair.
     * It's stored under the name you can get with getName().
     * 
     * @return The Object value for this NameValue pair
     */
    public Object getValue() {

        // Return the Object value for this NameValue pair
        return _value;
    }

    /**
     * Set the object value for this NameValue pair.
     * 
     * @param value The new Object to keep as the value
     * @return      The previous Object this NameValue held
     */
	public Object setValue(Object value) {

        // Save the new reference in this object, and return the old one
	    Object old = _value;
		this._value = value;
		return old;
	}

    /**
     * Express the name and value in this NameValue pair as text.
     * 
     * @return A String like "name = Name value = Object.toString()"
     */
    public String toString() {

        // Call _value.toString to compose the end of the text
        return "name = " + _name + " value = " + _value;
    }    
}
