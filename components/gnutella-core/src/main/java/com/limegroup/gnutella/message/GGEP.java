package com.limegroup.gnutella.message;

import com.sun.java.util.collections.*;
import java.io.*;
import com.limegroup.gnutella.Assert;
import java.util.Enumeration;

/** 
 * An abstraction of a GGEP Block.
 * A GGEP Block can be thought of as a collection of key/value pairs.
 * A key (extension header) cannot be greater than 15 bytes, and the value
 * (extension data) can be at most 2^24-1 bytes (and could be 0).
 * The order of the Extensions is immaterial.  Extensions supported by LimeWire
 * have keys specified in this class (prefixed by GGEP_HEADER...)
 */
public class GGEP extends Object {

    /** The extension header (key) for Browse Host. */
    public static final String GGEP_HEADER_BROWSE_HOST = "BHOST";

    /** The maximum size of a extension header (key). */
    public static final int MAX_KEY_SIZE_IN_BYTES = 15;

    /** The maximum size of a extension data (value). */
    public static final int MAX_VALUE_SIZE_IN_BYTES = 262143;

    /** The collection of key/value pairs this GGEP instance represents.
     */
    private Map _props = null;

    /** Constructs a GGEP instance with the set of specified key/val pairs.
     *  The key and values you submit should be stringable!  The key string
     *  should be non-null and not the empty string!  The value, if null, is
     *  assumed to be not-present (extension data is optional).
     *  @param props The collection of GGEP headers/keys and data/values.
     *  @exception BadGGEPPropertyException Thrown if any of the input
     *  properties are too large).
     */
    public GGEP(Map props) throws BadGGEPPropertyException {
        _props = new HashMap();
        Iterator keys = props.keySet().iterator();
        // we need to do the following:
        // 1) make sure everything is of the right size
        // 2) input everything into our HashMap as a String...
        while (keys.hasNext()) {
            Object currKey = keys.next();
            Object currValue = props.get(currKey);

            // check the key...
            if (!(currKey instanceof String))
                currKey = currKey.toString();
            validateKey((String)currKey);

            // check the data...
            if ((currValue != null) && !(currValue instanceof String))
                currValue = currValue.toString();
            validateValue((String)currValue);

            // everything checks out...
            _props.put(currKey, currValue);
        }
    }

    private static final String EMPTY_STRING = "";

    private void validateKey(String key) throws BadGGEPPropertyException {
        if ((key == null) ||
            (key.equals(EMPTY_STRING)) ||
            (key.getBytes().length > MAX_KEY_SIZE_IN_BYTES)
            )
            throw new BadGGEPPropertyException();
    }

    private void validateValue(String value) throws BadGGEPPropertyException {
        if ((value != null) && 
            (value.getBytes().length > MAX_VALUE_SIZE_IN_BYTES)
            )
            throw new BadGGEPPropertyException();
    }


    /** Constructs a GGEP instance based on the GGEP block beginning at
     *  messageBytes[beginOffset].  If you are unsure of whether or not there is
     *  one GGEP Block, use the read method.
     *  @param messageBytes The bytes of the message.
     *  @param beginOffset  The begin index of the GGEP prefix.
     *  @exception BadGGEPBlockException Thrown if the block could not be parsed
     *  correctly.
     */
    public GGEP(byte[] messageBytes, int beginOffset) 
        throws BadGGEPBlockException {
    }

    /** Provides access to the extension headers represented by this GGEP
     *  instance.
     *  @return An Set (Strings) of all the keys/headers in this GGEP 
     *  Block.
     */
    public Set getHeaders() {
        return _props.keySet();
    }

    /** Accesses the specific String-based extension data representation for a
     *  given extension header.
     *  @return The extension data (value) for the given extension header (key).
     */
    public String getData(String header) {
        return _props.get(header).toString();
    }

    /** Writes this GGEP instance as a properly formatted GGEP Block.
     *  @param out This GGEP instance is written to out.
     *  @exception IOException Thrown if had error writing to out.
     */
    public void write(OutputStream out) throws IOException {
    }

    /** Constructs an array of all GGEP blocks starting at
     *  messageBytes[beginOffset].
     *  @param messageBytes The InputStream to attempt to read one or more GGEP
     *  blocks from.
     *  @param beginOffset The begin index of the (first) GGEP prefix.
     *  @exception BadGGEPBlockException Thrown if the block could not be parsed
     *  correctly.
     */
    public static GGEP[] read(byte[] messageBytes,
                              int beginOffset) throws BadGGEPBlockException {
        return new GGEP[0];
    }

}





