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
	 * Identifier string for the SHA1 type.
	 */
	public static final String SHA1_STRING = "sha1";

	/**
	 * Constant for the leading URN string identifier, as specified in
	 * RFC 2141.  This is equal to "urn:", although note that this
	 * should be used in a case-insensitive manner in compliance with
	 * the URN specification (RFC 2141).
	 */
	public static final String URN_NAMESPACE_ID = "urn:";

	/**
	 * Constant string for the URN type.
	 */
	private transient String _urnType;


	/**
	 * Private constructor ensures that this class can never be constructed 
	 * from outside the class.  This assigns the _urnType string.
	 * 
	 * @param typeString the string representation of the URN type
	 */
	private UrnType(String typeString) {
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
	 * The <tt>UrnType</tt> for SHA1 hashes.
	 */
	public static final UrnType SHA1 = new UrnType(SHA1_STRING);

	/**
	 * The <tt>UrnType</tt> for specifying any URN type.
	 */
	public static final UrnType ANY_TYPE = new UrnType("");

	/**
	 * Returns the string representation of this URN type.
	 *
	 * @return the string representation of this URN type
	 */
	public String toString() {
		return URN_NAMESPACE_ID+_urnType+":";
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
	 *
	 * @param type the string representation of the urn type
	 */
	public static UrnType createUrnType(String type) {
		String lowerCaseType = type.toLowerCase();
		if(lowerCaseType.equals(SHA1_STRING)) {
			return SHA1;
		} else if(lowerCaseType.equals(SHA1.toString())) { 
			return SHA1;
		}else {
			throw new IllegalArgumentException("unknown type: "+type);
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
		int colonIndex = urnString.indexOf(":");
		if(colonIndex == -1) {
			return false;
		}	   
		String typeString = urnString.toLowerCase();
		if(typeString.equals(URN_NAMESPACE_ID)) {
			return true;
		}
		typeString = typeString.substring(colonIndex+1);
		if(typeString.equals(SHA1_STRING + ":")) {
			return true;
		}
		return false;		
	}

	/**
	 * Returns whether or not the specified namespace string represents a 
	 * namespace that is supported.
	 *
	 * @return <tt>true</tt> if the namespace is supported, <tt>false</tt>
	 *  otherwise
	 */
	public static boolean isSupportedUrnNamespace(final String namespaceId) {
		// we should add other namespace identifiers to this check as
		// they become registered
		if(namespaceId.equalsIgnoreCase(SHA1_STRING)) {
			return true;
		}
		return false;
	}
}
