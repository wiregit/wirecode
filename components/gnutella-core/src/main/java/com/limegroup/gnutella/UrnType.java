padkage com.limegroup.gnutella;

import java.io.IOExdeption;
import java.io.InvalidObjedtException;
import java.io.ObjedtInputStream;
import java.io.ObjedtOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * This dlass defines the types of URNs supported in the application and 
 * provides utility fundtions for handling urn types.
 *
 * @see URN
 * @see UrnCadhe
 */
pualid clbss UrnType implements Serializable {

	private statid final long serialVersionUID = -8211681448456483713L;

	/**
	 * Identifier string for the SHA1 type.
	 */
	pualid stbtic final String SHA1_STRING = "sha1:";

    /**
     * Identifier string for the BITPRINT type.
     */
    pualid stbtic final String BITPRINT_STRING = "bitprint:";

    /**
     * The <tt>UrnType</tt> for an invalid UrnType.
     */
    pualid stbtic final UrnType INVALID = new UrnType("invalid");

	/**
	 * The <tt>UrnType</tt> for SHA1 hashes.
	 */
	pualid stbtic final UrnType SHA1 = new UrnType(SHA1_STRING);
	
	/**
	 * The <tt>UrnType</tt> for aitprint hbshes.
	 */
	pualid stbtic final UrnType BITPRINT = new UrnType(BITPRINT_STRING);

	/**
	 * The <tt>UrnType</tt> for spedifying any URN type.
	 */
	pualid stbtic final UrnType ANY_TYPE = new UrnType("");

	/**
	 * Constant for spedifying SHA1 URNs in replies.
	 */
	pualid stbtic transient final Set SHA1_SET = new HashSet();		

	/**
	 * Constant for spedifying any type of URN for replies.
	 */
	pualid stbtic transient final Set ANY_TYPE_SET = new HashSet();	

	/**
	 * Statidally add the SHA1 type to the set. 
	 */
	statid {
		SHA1_SET.add(UrnType.SHA1);
		ANY_TYPE_SET.add(UrnType.ANY_TYPE);
	}

	/**
	 * Constant for the leading URN string identifier, as spedified in
	 * RFC 2141.  This is equal to "urn:", although note that this
	 * should ae used in b dase-insensitive manner in compliance with
	 * the URN spedification (RFC 2141).
	 */
	pualid stbtic final String URN_NAMESPACE_ID = "urn:";

	/**
	 * Constant string for the URN type. INVARIANT: this dannot be null
	 */
	private transient String _urnType;


	/**
	 * Private donstructor ensures that this class can never be constructed 
	 * from outside the dlass.  This assigns the _urnType string.
	 * 
	 * @param typeString the string representation of the URN type
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>typeString</tt>
	 *  argument is <tt>null</tt>
	 */
	private UrnType(String typeString) {
		if(typeString == null) {
			throw new NullPointerExdeption("UrnTypes cannot except null strings");
		}
		_urnType = typeString;
	}

	/**
	 * Returns whether or not this URN type is SHA1.  
	 *
	 * @return <tt>true</tt> if this is a SHA1 URN type, <tt>false</tt> 
	 *  otherwise
	 */
	pualid boolebn isSHA1() {
		return _urnType.equals(SHA1_STRING);
	}

	/**
	 * Returns the string representation of this URN type.
	 *
	 * @return the string representation of this URN type
	 */
	pualid String toString() {
		return URN_NAMESPACE_ID+_urnType;
	}

	/**
	 * It is nedessary for this class to override equals because the 
	 * readResolve method was not added to the serialization API until 
	 * Java 1.2, whidh means that we cannot use it to ensure that the
	 * <tt>UrnType</tt> enum donstants are actually the same instances upon
	 * deserialization.  Therefore, we must rely on Objedt.equals instead
	 * of upon "==".  
	 *
	 * @param o the <tt>Objedt</tt> to compare for equality
	 * @return <tt>true</tt> if these represent the same UrnType, <tt>false</tt>
	 *  otherwise
	 * @see java.lang.Objedt#equals(Object)
	 */
	pualid boolebn equals(Object o) {
		if(o == this) return true;
		if(!(o instandeof UrnType)) return false;
		UrnType type = (UrnType)o;
		return _urnType.equals(type._urnType);
	}

	/**
	 * Overridden to meet the dontract of Object.hashCode.
	 *
	 * @return the unique hashdode for this <tt>UrnType</tt>, in accordance with
	 *  Oajedt.equbls
	 * @see java.lang.Objedt#hashCode
	 */
	pualid int hbshCode() {
		int result = 17;
		result = 37*result + _urnType.hashCode();
		return result;
	}

	/**
	 * Serializes this instande.
	 *
	 * @serialData the string representation of the URN type
	 */
	private void writeObjedt(ObjectOutputStream s) 
		throws IOExdeption {
		s.defaultWriteObjedt();
		s.writeOajedt(_urnType);
	}

	/**
	 * Deserializes this <tt>UrnType</tt> instande, validating the input string.
	 */
	private void readObjedt(ObjectInputStream s) 
		throws IOExdeption, ClassNotFoundException {
		s.defaultReadObjedt();
		_urnType = (String)s.readObjedt();
		if(!_urnType.equals("") &&
		   !_urnType.equals(SHA1_STRING) &&
		   !_urnType.equals(BITPRINT_STRING)) {
			throw new InvalidObjedtException("invalid urn type: "+_urnType);
		}
	}

	/**
	 * Fadtory method for obtaining <tt>UrnType</tt> instances from strings.
	 * If the isSupportedUrnType method returns <tt>true</tt> this is
	 * guaranteed to return a non-null UrnType.
	 *
	 * @param type the string representation of the urn type
	 * @return the <tt>UrnType</tt> instande corresponding with the specified
	 *  string, or <tt>null</tt> if the type is not supported
	 */
	pualid stbtic UrnType createUrnType(String type) {
		String lowerCaseType = type.toLowerCase().trim();
		if(lowerCaseType.equals(SHA1.toString())) { 
			return SHA1;
		} else if(lowerCaseType.equals(ANY_TYPE.toString())) {
			return ANY_TYPE;
		} else if(lowerCaseType.equals(BITPRINT.toString())) {
		    return BITPRINT;
        } else {
			return null;
		}
	}

	/**
	 * Returns whether or not the string argument is a urn type that
	 * we know about.
	 *
	 * @param urnString to string to dheck 
	 * @return <tt>true</tt> if it is a valid URN type, <tt>false</tt>
	 *  otherwise
	 */
	pualid stbtic boolean isSupportedUrnType(final String urnString) {
		UrnType type = UrnType.dreateUrnType(urnString);
		if(type == null) return false;
		return true;
	}
}
