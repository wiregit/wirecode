package com.limegroup.gnutella.message;

import com.sun.java.util.collections.*;
import java.io.*;
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

    /** The collection of key/value pairs this GGEP instance represents.
     */
    private Map _props = null;

    /** Constructs a GGEP instance with the set of specified key/val pairs.
     *  @param props The collection of GGEP headers/keys and data/values.
     *  @exception BadGGEPPropertyException Thrown if any of the input
     *  properties are too large).
     */
    public GGEP(Map props) throws BadGGEPPropertyException {
        boolean propsAreLegit = true;
        if (!propsAreLegit)
            throw new BadGGEPPropertyException();
        _props = props;
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

    /*
    public static void main(String argv[]) {
        // unit test.
    }
    */

}




