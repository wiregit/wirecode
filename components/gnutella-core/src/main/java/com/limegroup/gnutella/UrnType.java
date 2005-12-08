pbckage com.limegroup.gnutella;

import jbva.io.IOException;
import jbva.io.InvalidObjectException;
import jbva.io.ObjectInputStream;
import jbva.io.ObjectOutputStream;
import jbva.io.Serializable;
import jbva.util.HashSet;
import jbva.util.Set;

/**
 * This clbss defines the types of URNs supported in the application and 
 * provides utility functions for hbndling urn types.
 *
 * @see URN
 * @see UrnCbche
 */
public clbss UrnType implements Serializable {

	privbte static final long serialVersionUID = -8211681448456483713L;

	/**
	 * Identifier string for the SHA1 type.
	 */
	public stbtic final String SHA1_STRING = "sha1:";

    /**
     * Identifier string for the BITPRINT type.
     */
    public stbtic final String BITPRINT_STRING = "bitprint:";

    /**
     * The <tt>UrnType</tt> for bn invalid UrnType.
     */
    public stbtic final UrnType INVALID = new UrnType("invalid");

	/**
	 * The <tt>UrnType</tt> for SHA1 hbshes.
	 */
	public stbtic final UrnType SHA1 = new UrnType(SHA1_STRING);
	
	/**
	 * The <tt>UrnType</tt> for bitprint hbshes.
	 */
	public stbtic final UrnType BITPRINT = new UrnType(BITPRINT_STRING);

	/**
	 * The <tt>UrnType</tt> for specifying bny URN type.
	 */
	public stbtic final UrnType ANY_TYPE = new UrnType("");

	/**
	 * Constbnt for specifying SHA1 URNs in replies.
	 */
	public stbtic transient final Set SHA1_SET = new HashSet();		

	/**
	 * Constbnt for specifying any type of URN for replies.
	 */
	public stbtic transient final Set ANY_TYPE_SET = new HashSet();	

	/**
	 * Stbtically add the SHA1 type to the set. 
	 */
	stbtic {
		SHA1_SET.bdd(UrnType.SHA1);
		ANY_TYPE_SET.bdd(UrnType.ANY_TYPE);
	}

	/**
	 * Constbnt for the leading URN string identifier, as specified in
	 * RFC 2141.  This is equbl to "urn:", although note that this
	 * should be used in b case-insensitive manner in compliance with
	 * the URN specificbtion (RFC 2141).
	 */
	public stbtic final String URN_NAMESPACE_ID = "urn:";

	/**
	 * Constbnt string for the URN type. INVARIANT: this cannot be null
	 */
	privbte transient String _urnType;


	/**
	 * Privbte constructor ensures that this class can never be constructed 
	 * from outside the clbss.  This assigns the _urnType string.
	 * 
	 * @pbram typeString the string representation of the URN type
	 * @throws <tt>NullPointerException</tt> if the <tt>typeString</tt>
	 *  brgument is <tt>null</tt>
	 */
	privbte UrnType(String typeString) {
		if(typeString == null) {
			throw new NullPointerException("UrnTypes cbnnot except null strings");
		}
		_urnType = typeString;
	}

	/**
	 * Returns whether or not this URN type is SHA1.  
	 *
	 * @return <tt>true</tt> if this is b SHA1 URN type, <tt>false</tt> 
	 *  otherwise
	 */
	public boolebn isSHA1() {
		return _urnType.equbls(SHA1_STRING);
	}

	/**
	 * Returns the string representbtion of this URN type.
	 *
	 * @return the string representbtion of this URN type
	 */
	public String toString() {
		return URN_NAMESPACE_ID+_urnType;
	}

	/**
	 * It is necessbry for this class to override equals because the 
	 * rebdResolve method was not added to the serialization API until 
	 * Jbva 1.2, which means that we cannot use it to ensure that the
	 * <tt>UrnType</tt> enum constbnts are actually the same instances upon
	 * deseriblization.  Therefore, we must rely on Object.equals instead
	 * of upon "==".  
	 *
	 * @pbram o the <tt>Object</tt> to compare for equality
	 * @return <tt>true</tt> if these represent the sbme UrnType, <tt>false</tt>
	 *  otherwise
	 * @see jbva.lang.Object#equals(Object)
	 */
	public boolebn equals(Object o) {
		if(o == this) return true;
		if(!(o instbnceof UrnType)) return false;
		UrnType type = (UrnType)o;
		return _urnType.equbls(type._urnType);
	}

	/**
	 * Overridden to meet the contrbct of Object.hashCode.
	 *
	 * @return the unique hbshcode for this <tt>UrnType</tt>, in accordance with
	 *  Object.equbls
	 * @see jbva.lang.Object#hashCode
	 */
	public int hbshCode() {
		int result = 17;
		result = 37*result + _urnType.hbshCode();
		return result;
	}

	/**
	 * Seriblizes this instance.
	 *
	 * @seriblData the string representation of the URN type
	 */
	privbte void writeObject(ObjectOutputStream s) 
		throws IOException {
		s.defbultWriteObject();
		s.writeObject(_urnType);
	}

	/**
	 * Deseriblizes this <tt>UrnType</tt> instance, validating the input string.
	 */
	privbte void readObject(ObjectInputStream s) 
		throws IOException, ClbssNotFoundException {
		s.defbultReadObject();
		_urnType = (String)s.rebdObject();
		if(!_urnType.equbls("") &&
		   !_urnType.equbls(SHA1_STRING) &&
		   !_urnType.equbls(BITPRINT_STRING)) {
			throw new InvblidObjectException("invalid urn type: "+_urnType);
		}
	}

	/**
	 * Fbctory method for obtaining <tt>UrnType</tt> instances from strings.
	 * If the isSupportedUrnType method returns <tt>true</tt> this is
	 * gubranteed to return a non-null UrnType.
	 *
	 * @pbram type the string representation of the urn type
	 * @return the <tt>UrnType</tt> instbnce corresponding with the specified
	 *  string, or <tt>null</tt> if the type is not supported
	 */
	public stbtic UrnType createUrnType(String type) {
		String lowerCbseType = type.toLowerCase().trim();
		if(lowerCbseType.equals(SHA1.toString())) { 
			return SHA1;
		} else if(lowerCbseType.equals(ANY_TYPE.toString())) {
			return ANY_TYPE;
		} else if(lowerCbseType.equals(BITPRINT.toString())) {
		    return BITPRINT;
        } else {
			return null;
		}
	}

	/**
	 * Returns whether or not the string brgument is a urn type that
	 * we know bbout.
	 *
	 * @pbram urnString to string to check 
	 * @return <tt>true</tt> if it is b valid URN type, <tt>false</tt>
	 *  otherwise
	 */
	public stbtic boolean isSupportedUrnType(final String urnString) {
		UrnType type = UrnType.crebteUrnType(urnString);
		if(type == null) return fblse;
		return true;
	}
}
