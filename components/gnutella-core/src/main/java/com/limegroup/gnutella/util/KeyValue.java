
package com.limegroup.gnutella.util;

import java.util.Map;

/**
* This class stores a pair of a property key, and its corresponding value.
* It implements Map.Entry interface, so that other classes can use it in the
* same way, there's no backing Map though, unless stated otherwise.
*/
public class KeyValue implements Map.Entry
{
/** key of the property */
	private Object key = null;

/**	Value of the property */
	private Object value = null;


/**	Constructor
	@param key key of the property
	@param value corresponding value of the property
*/
public KeyValue(Object key, Object value)
{
	this.key = key;
	this.value = value;
}//end of constructor

/**
* Default Constructor
*/
public KeyValue()
{
    this.key = null;
    this.value = null;
}


/**	
* Sets the key and value fields
* @param key key of the property
* @param value corresponding value of the property
*/
public void set(Object key, Object value)
{
	this.key = key;
	this.value = value;
}//end of constructor

/** Compares the instance of this class with another instance. 
	Returns true, if the key field is same, regardless of the 
	value. 
	@param o Another instance of the KeyValue class to which it
	has to be compared.
*/
public boolean equals(Object o)
{
	KeyValue keyValue = (KeyValue)o;

	return key.equals(keyValue.getKey());
}

/**	
* Converts the key Value pair into a string representation 
*/
public String toString()
{
	return key + " = " + value;
}

/** 
* Returns the key(key) in the key value pair 
* @return the key(key) in the key value pair
*/
public Object getKey()
{
    return key;
}

/** 
* Returns the value corresponding to this entry.  
* @return the value corresponding to this entry.
*/
public Object getValue()
{
    return value;
}
/** 
* Replaces the value corresponding to this entry with the specified
 * value. 
 * @param value new value to be stored in this entry.
 * @return old value corresponding to the entry.
 */
public Object setValue(Object value)
{
    //get the old value
    Object oldValue = this.value;
    
    //change the value
    this.value = value;
    
    //return the old value
    return oldValue;
    
}


/** Returns the hash code value for this map entry.  The hash code
 * of a map entry <tt>e</tt> is defined to be: <pre>
 *    (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
 *    (e.getValue()==null ? 0 : e.getValue().hashCode())
 * </pre>
 * This ensures that <tt>e1.equals(e2)</tt> implies that
 * <tt>e1.hashCode()==e2.hashCode()</tt> for any two Entries
 * <tt>e1</tt> and <tt>e2</tt>, as required by the general
 * contract of <tt>Object.hashCode</tt>.
 *
 * @return the hash code value for this map entry.
 * @see Object#hashCode()
 * @see Object#equals(Object)
 * @see #equals(Object)
 */
public int hashCode()
{
    return ((key == null   ? 0 : key.hashCode()) ^ 
            (value == null ? 0 : value.hashCode()));
}

}//end of class
