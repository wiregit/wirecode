package com.limegroup.gnutella.message;

import com.sun.java.util.collections.*;
import java.io.*;
import com.limegroup.gnutella.Assert;
import java.util.Enumeration;
import com.limegroup.gnutella.*;

/** 
 * An abstraction of a GGEP Block.
 * A GGEP Block can be thought of as a collection of key/value pairs.
 * A key (extension header) cannot be greater than 15 bytes, and the value
 * (extension data) can be at most 2^24-1 bytes (and could be 0).
 * The order of the Extensions is immaterial.  Extensions supported by LimeWire
 * have keys specified in this class (prefixed by GGEP_HEADER...)
 */
public class GGEP extends Object {

    /** The extension header (key) for Browse Host (queries) */
    public static final String GGEP_HEADER_BROWSE_HOST = "BHOST";
    /** The extension header (key) for Daily Uptime (pongs) */
    public static final String GGEP_HEADER_DAILY_AVERAGE_UPTIME = "DUPTIME";    

    /** The maximum size of a extension header (key). */
    public static final int MAX_KEY_SIZE_IN_BYTES = 15;

    /** The maximum size of a extension data (value). */
    public static final int MAX_VALUE_SIZE_IN_BYTES = 262143;

    /** The GGEP prefix.  A GGEP block will start with this byte value.
     */
    public static final byte GGEP_PREFIX_MAGIC_NUMBER = (byte) 0xC3;

    /** The collection of key/value pairs this GGEP instance represents.
     */
    private Map _props = null;
    Map getProps() {
        return _props;
    }

    /** Constructs a GGEP instance with the set of specified key/val pairs.
     *  The key and values you submit should be stringable!  The key string
     *  should be non-null and not the empty string!  The value, if null, is
     *  assumed to be not-present (extension data is optional).
     *  @param props The collection of GGEP headers/keys and data/values.
     *  @exception BadGGEPPropertyException Thrown if any of the input
     *  properties are too large or if your data contains nulls (0x0)).
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
            key.equals(EMPTY_STRING) ||
            (key.getBytes().length > MAX_KEY_SIZE_IN_BYTES) ||
            containsNull(key)
            )
            throw new BadGGEPPropertyException();
    }

    private void validateValue(String value) throws BadGGEPPropertyException {
        if ((value != null) && 
            ((value.getBytes().length > MAX_VALUE_SIZE_IN_BYTES) ||
             (containsNull(value)))
            )
            throw new BadGGEPPropertyException();
    }

    private boolean containsNull(String value) {
        byte[] bytes = value.getBytes();
        for (int i = 0; i < bytes.length; i++)
            if (bytes[i] == 0x0)
                return true;
        return false;
    }
    

    /** Constructs a GGEP instance based on the GGEP block beginning at
     *  messageBytes[beginOffset].  If you are unsure of whether or not there is
     *  one GGEP Block, use the read method.
     *  @param messageBytes The bytes of the message.
     *  @param beginOffset  The begin index of the GGEP prefix.
     *  @param endOffset If you want to get the offset where the GGEP block
     *  ends (more precisely, one above the ending index), then send me a
     *  int[1].  I'll put the endOffset in endOffset[0].  If you don't care, 
     *  null will do....
     *  @exception BadGGEPBlockException Thrown if the block could not be parsed
     *  correctly.
     */
    public GGEP(byte[] messageBytes, final int beginOffset, int[] endOffset) 
        throws BadGGEPBlockException {

        _props = new HashMap();

        // all GGEP blocks start with this prefix....
        if (messageBytes[beginOffset] != GGEP_PREFIX_MAGIC_NUMBER)
            throw new BadGGEPBlockException();

        boolean onLastExtension = false;
        int currIndex = beginOffset + 1;
        while (!onLastExtension) {

            // process extension header flags
            // bit order is interpreted as 76543210            
            sanityCheck(messageBytes[currIndex]);
            onLastExtension = isLastExtension(messageBytes[currIndex]);
            boolean encoded = isEncoded(messageBytes[currIndex]);
            boolean compressed = isCompressed(messageBytes[currIndex]);
            int headerLen = deriveHeaderLength(messageBytes[currIndex]);

            // get the extension header
            currIndex++;
            String extensionHeader = new String(messageBytes,
                                                currIndex,
                                                headerLen);

            // get the data length
            currIndex += headerLen;
            final int dataLength = deriveDataLength(messageBytes, currIndex);

            String extensionData = null;

            currIndex++;
            if (dataLength > 0) {
                // ok, data is present, get it....

                byte[] data = new byte[dataLength];
                System.arraycopy(messageBytes, currIndex, data, 0, dataLength);

                // LimeWire currently does not support COBS, so anything COBS
                // encoded is just disregarded...
                if (encoded) 
                    continue;

                // LimeWire currently does not support flate/default, so
                // anything in this format is just disregarded...
                if (compressed)
                    continue;

                extensionData = new String(data);

                currIndex += dataLength;
            }

            debug("");
            debug("GGEP(): --------- block info ---------");
            debug("GGEP(): onLastExtension = " + onLastExtension);
            debug("GGEP(): encoded = " + encoded);
            debug("GGEP(): compressed = " + compressed);
            debug("GGEP(): headerLen = " + headerLen);
            debug("GGEP(): extension header = " + extensionHeader);
            debug("GGEP(): dataLength = " + dataLength);
            debug("GGEP(): extension data = " + extensionData);

            // ok, everything checks out, just slap it in the hashmapper...
            _props.put(extensionHeader, extensionData);

        }
        if ((endOffset != null) && (endOffset.length > 0))
            endOffset[0] = currIndex;
    }

    private void sanityCheck(byte headerFlags) throws BadGGEPBlockException {
        // the 4th bit in the header's first byte must be 0.
        if ((headerFlags & 0x10) != 0)
            throw new BadGGEPBlockException();
    }
        
    private boolean isLastExtension(byte headerFlags) {
        boolean retBool = false;
        // the 8th bit in the header's first byte, when set, indicates that
        // this header is the last....
        if ((headerFlags & 0x80) != 0)
            retBool = true;
        return retBool;        
    }


    private boolean isEncoded(byte headerFlags) {
        boolean retBool = false;
        // the 7th bit in the header's first byte, when set, indicates that
        // this header is the encoded with COBS
        if ((headerFlags & 0x40) != 0)
            retBool = true;
        return retBool;        
    }


    private boolean isCompressed(byte headerFlags) {
        boolean retBool = false;
        // the 6th bit in the header's first byte, when set, indicates that
        // this header is the compressed with deflate
        if ((headerFlags & 0x20) != 0)
            retBool = true;
        return retBool;        
    }


    private int deriveHeaderLength(byte headerFlags) 
        throws BadGGEPBlockException {
        int retInt = 0;
        // bits 0-3 give the length of the extension header (1-15)
        retInt = headerFlags & 0x0F;
        if (retInt == 0)
            throw new BadGGEPBlockException();
        return retInt;
    }

    private int deriveDataLength(byte[] buff, int beginOffset) 
        throws BadGGEPBlockException {
        int length = 0, iterations = 0;
        // the length is stored in at most 3 bytes....
        final int MAX_ITERATIONS = 3;
        byte currByte;
        do {
            currByte = buff[beginOffset++];
            length = (length << 6) | (currByte & 0x3f);
            if (++iterations > MAX_ITERATIONS)
                throw new BadGGEPBlockException();
        } while (0x40 != (currByte & 0x40));
        return length;
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
     *  @return The extension data (value) for the given extension header
     *  (key). Can very well return null, headers don't HAVE to have data.
     */
    public String getData(String header) {
        return (String) _props.get(header);
    }

    /** Writes this GGEP instance as a properly formatted GGEP Block.
     *  @param out This GGEP instance is written to out.
     *  @exception IOException Thrown if had error writing to out.
     */
    public void write(OutputStream out) throws IOException {
        if (getHeaders().size() > 0) {
            // start with the magic prefix
            out.write(GGEP_PREFIX_MAGIC_NUMBER);

            Iterator headers = getHeaders().iterator();
            // for each header, write the GGEP header and data
            while (headers.hasNext()) {
                String currHeader = (String) headers.next();
                String currData   = (String) getData(currHeader);
                int dataLen = 0;
                if (currData != null)
                    dataLen = currData.getBytes().length;
                debug("GGEP.write(): dataLen for " + currHeader + 
                      " is " + dataLen);
                writeHeader(currHeader, dataLen, 
                            !headers.hasNext(), out);
                if (dataLen > 0)
                    writeData(currData, out);
            }
        }
        debug("GGEP.write(): returning " + 
              toHexString(((ByteArrayOutputStream)out).toByteArray()));
    }

    
    private void writeHeader(String header, final int dataLen, 
                             boolean isLast, OutputStream out) 
        throws IOException{

        // 1. WRITE THE HEADER FLAGS
        // in the future, when we actually encode and compress, this code should
        // still work.  well, the code that deals with the header flags, that
        // is, you'll still need to encode/compress
        boolean shouldEncode = false;
        boolean shouldCompress = false;

        int flags = 0x00;
        if (isLast)
            flags = flags | 0x80;
        if (shouldEncode)
            flags = flags | 0x40;
        if (shouldCompress)
            flags = flags | 0x20;
        flags = flags | header.getBytes().length;
        out.write(flags);

        // 2. WRITE THE HEADER
        out.write(header.getBytes());

        // 3. WRITE THE DATA LEN
        // possibly 3 bytes
        int toWrite;
        int begin = dataLen & 0x3F000;
        if (begin != 0) {
            begin = begin >> 12; // relevant bytes at the bottom now...
            toWrite = 0x80 | begin;
            out.write(toWrite);
        }
        int middle = dataLen & 0xFC0;
        if (middle != 0) {
            middle = middle >> 6; // relevant bytes at the bottom now...
            toWrite = 0x80 | middle;
            out.write(toWrite);
        }
        int end = dataLen & 0x3F; // shut off everything except last 6 bits...
        toWrite = 0x40 | end;
        out.write(toWrite);
    }

    private void writeData(String data, OutputStream out) throws IOException {
        // write now, all we need to do is write the data bytes.  but in the
        // future, we may need to encode or compress the data and then write it
        // out.
        out.write(data.getBytes());
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
                              final int beginOffset) 
        throws BadGGEPBlockException {

        GGEP[] retGGEPs = null;
        List ggeps = new ArrayList();
        int currIndex[] = {beginOffset};

        while ((messageBytes.length > currIndex[0]) && 
               (messageBytes[currIndex[0]] == GGEP_PREFIX_MAGIC_NUMBER))
            ggeps.add(new GGEP(messageBytes, currIndex[0], currIndex));
        
        Object[] array = ggeps.toArray();
        retGGEPs = new GGEP[array.length];
        for (int i = 0; i < array.length; i++)
            retGGEPs[i] = (GGEP)array[i];

        return retGGEPs;
    }

    /** @return True if the two Maps that represent header/data pairs are
     *  equivalent.
     */
    public boolean equals(Object o) {
        if (o instanceof GGEP) 
            return _props.equals(((GGEP)o).getProps());
        return false;
    }


    private String toHexString(byte[] bytes) {
        StringBuffer buf = new StringBuffer();
        String       str;
        int val;
        for (int i = 0; i < bytes.length; i++) {
            //Treating each byte as an unsigned value ensures
            //that we don't str doesn't equal things like 0xFFFF...
            val = ByteOrder.ubyte2int(bytes[i]);
            str = Integer.toHexString(val);
            while (str.length() < 2)
                str = "0" + str;
            buf.append(str);
        }
        return buf.toString().toUpperCase();
    }


    private static final boolean debugOn = false;
    private void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
}





