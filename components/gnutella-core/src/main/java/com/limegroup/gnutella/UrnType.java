package com.limegroup.gnutella;

import java.io.*;

/**
 * This class defines the types of URNs supported in the application and 
 * provides utility functions for handling urn types.
 *
 * @see URN
 * @see UrnCache
 */
public class UrnType implements Serializable {

	private static final long serialVersionUID = -8211681448456483713L;

	/**
	 * Cached constant for a colon string.
	 */
	private static final String COLON = ":";
	/**
	 * Identifier string for the SHA1 type.
	 */
	public static final String SHA1_STRING = "sha1:";

	/**
	 * Constant for the leading URN string identifier, as specified in
	 * RFC 2141.  This is equal to "urn:", although note that this
	 * should be used in a case-insensitive manner in compliance with
	 * the URN specification (RFC 2141).
	 */
	public static final String URN_NAMESPACE_ID = "urn:";

	/**
	 * Constant string for the URN type. INVARIANT: this cannot be null
	 */
	private transient String _urnType;


	/**
	 * Private constructor ensures that this class can never be constructed 
	 * from outside the class.  This assigns the _urnType string.
	 * 
	 * @param typeString the string representation of the URN type
	 * @throws <tt>NullPointerException</tt> if the <tt>typeString</tt>
	 *  argument is <tt>null</tt>
	 */
	private UrnType(String typeString) {
		if(typeString == null) {
			throw new NullPointerException("UrnTypes cannot except null strings");
		}
		_urnType = typeString;
	}

	/**
	 * Returns whether or not this URN type is SHA1.  
	 *
	 * @return <tt>true</tt> if this is a SHA1 URN type, <tt>false</tt> 
	 *  otherwise
	 */
	public boolean isSHA1() {
		return _urnType.equals(SHA1_STRING);
	}

	/**
	 * Returns the string representation of this URN type.
	 *
	 * @return the string representation of this URN type
	 */
	public String toString() {
		return URN_NAMESPACE_ID+_urnType;
	}

	/**
	 * It is necessary for this class to override equals because the 
	 * readResolve method was not added to the serialization API until 
	 * Java 1.2, which means that we cannot use it to ensure that the
	 * <tt>UrnType</tt> enum constants are actually the same instances upon
	 * deserialization.  Therefore, we must rely on Object.equals instead
	 * of upon "==".  
	 *
	 * @param o the <tt>Object</tt> to compare for equality
	 * @return <tt>true</tt> if these represent the same UrnType, <tt>false</tt>
	 *  otherwise
	 * @see java.lang.Object#equals(Object)
	 */
	public boolean equals(Object o) {
		if(o == this) return true;
		if(!(o instanceof UrnType)) return false;
		UrnType type = (UrnType)o;
		return _urnType.equals(type._urnType);
	}

	/**
	 * Overridden to meet the contract of Object.hashCode.
	 *
	 * @return the unique hashcode for this <tt>UrnType</tt>, in accordance with
	 *  Object.equals
	 * @see java.lang.Object#hashCode
	 */
	public int hashCode() {
		int result = 17;
		result = 37*result + _urnType.hashCode();
		return result;
	}

	/**
	 * Serializes this instance.
	 *
	 * @serialData the string representation of the URN type
	 */
	private void writeObject(ObjectOutputStream s) 
		throws IOException {
		s.defaultWriteObject();
		s.writeObject(_urnType);
	}

	/**
	 * Deserializes this <tt>UrnType</tt> instance, validating the input string.
	 */
	private void readObject(ObjectInputStream s) 
		throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		_urnType = (String)s.readObject();
		if(!_urnType.equals("") && !_urnType.equals(SHA1_STRING)) {
			throw new InvalidObjectException("invalid urn type: "+_urnType);
		}
	}

	/**
	 * Factory method for obtaining <tt>UrnType</tt> instances from strings.
	 * If the isSupportedUrnType method returns <tt>true</tt> this is
	 * guaranteed to return a non-null UrnType.
	 *
	 * @param type the string representation of the urn type
	 * @return the <tt>UrnType</tt> instance corresponding with the specified
	 *  string, or <tt>null</tt> if the type is not supported
	 */
	public static UrnType createUrnType(String type) {
		String lowerCaseType = type.toLowerCase().trim();
		if(lowerCaseType.equals(SHA1.toString())) { 
			return SHA1;
		} else if(lowerCaseType.equals(ANY_TYPE.toString())) {
			return ANY_TYPE;
		} else {
			return null;
		}
	}

	/**
	 * Returns whether or not the string argument is a urn type that
	 * we know about.
	 *
	 * @param urnString to string to check 
	 * @return <tt>true</tt> if it is a valid URN type, <tt>false</tt>
	 *  otherwise
	 */
	public static boolean isSupportedUrnType(final String urnString) {
		UrnType type = UrnType.createUrnType(urnString);
		if(type == null) return false;
		return true;
	}

	// the enum constants

	/**
	 * The <tt>UrnType</tt> for SHA1 hashes.
	 */
	public static final UrnType SHA1 = new UrnType(SHA1_STRING);

	/**
	 * The <tt>UrnType</tt> for specifying any URN type.
	 */
	public static final UrnType ANY_TYPE = new UrnType("");
}
