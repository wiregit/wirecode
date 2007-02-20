package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.limewire.collection.NameValue;
import org.limewire.io.IOUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.util.COBSUtil;

/** 
 * A mutable GGEP extension block.  A GGEP block can be thought of as a
 * collection of key/value pairs.  A key (extension header) cannot be greater
 * than 15 bytes.  The value (extension data) can be 0 to 2^24-1 bytes.  Values
 * can be formatted as a number, boolean, or generic blob of binary data.  If
 * necessary (e.g., for query replies), GGEP will COBS-encode values to remove
 * null bytes.  The order of the extensions is immaterial.  Extensions supported
 * by LimeWire have keys specified in this class (prefixed by GGEP_HEADER...)  
 */
public class GGEP {

    /** The extension header (key) for Browse Host. */
    public static final String GGEP_HEADER_BROWSE_HOST = "BH";
    /** The extension header (key) for average daily uptime. */
    public static final String GGEP_HEADER_DAILY_AVERAGE_UPTIME = "DU";
    /** The extension header (key) for unicast protocol support. */
    public static final String GGEP_HEADER_UNICAST_SUPPORT = "GUE";
    /** The extension header (key) for vendor info. */
    public static final String GGEP_HEADER_VENDOR_INFO = "VC";
    /** The extension header (key) for Ultrapeer support. */
    public static final String GGEP_HEADER_UP_SUPPORT = "UP";
    /** The extension header (key) for QueryKey support. */
    public static final String GGEP_HEADER_QUERY_KEY_SUPPORT = "QK";
    /** 
     * The extension header (key) for oob query requests to require the new
     * protocol version of oob messages that supports security tokens for
     * authentication.
     */
    public static final String GGEP_HEADER_SECURE_OOB = "SO";
    /** The extension header (key) for QueryKey support. */
    public static final String GGEP_HEADER_MULTICAST_RESPONSE = "MCAST";
    /** The extension header (key) for PushProxy support. */
    public static final String GGEP_HEADER_PUSH_PROXY = "PUSH";
    /** The extension header (key) for AlternateLocation support */
    public static final String GGEP_HEADER_ALTS = "ALT";
    /** The extention header (key) for IpPort request */
    public static final String GGEP_HEADER_IPPORT="IP";
    /** The extension header (key) for UDP HostCache pongs. */
    public static final String GGEP_HEADER_UDP_HOST_CACHE = "UDPHC";
    /** The extension header (key) for indicating support for packed ip/ports & udp host caches. */
    public static final String GGEP_HEADER_SUPPORT_CACHE_PONGS = "SCP";
    /** The extension header (key) for packed IP/Ports */
    public static final String GGEP_HEADER_PACKED_IPPORTS="IPP";
    /** The extension header (key) for packed UDP Host Caches */
    public static final String GGEP_HEADER_PACKED_HOSTCACHES="PHC";
    /** The extension header (key) for SHA1 urns. */
    public static final String GGEP_HEADER_SHA1 = "S1";
    /** The extension header (key) to determine if a SHA1 is valid. */
    public static final String GGEP_HEADER_SHA1_VALID = "SV";

    /** The extension header (key) for DHT support */
    public static final String GGEP_HEADER_DHT_SUPPORT = "DHT";
    
    /** The extension header (key) for DHT IPP requests. */
    public static final String GGEP_HEADER_DHT_IPPORTS = "DHTIPP";
    
    /**
     * The extension header (key) for a feature query.
     * This is 'WH' for legacy reasons, because 'What is New' was the first.
     */
    public static final String GGEP_HEADER_FEATURE_QUERY = "WH";
    /** The extension header disabling OOB proxying. */
    public static final String GGEP_HEADER_NO_PROXY = "NP";
    /** The extension header (key) for MetaType query support */
    public static final String GGEP_HEADER_META = "M";
    /** The extension header (key) for client locale */
    public static final String GGEP_HEADER_CLIENT_LOCALE = "LOC";
    /** The extension header (key) for creation time */
    public static final String GGEP_HEADER_CREATE_TIME = "CT";
    /** The extension header (key) for Firewalled Transfer support in Hits. */
    public static final String GGEP_HEADER_FW_TRANS = "FW";
    /** The extension header (key) indicating the GGEP block is the 'secure' block. */
    public static final String GGEP_HEADER_SECURE_BLOCK = "SB";
    /** The extension header (key) indiciating the value has a signature in it. */
    public static final String GGEP_HEADER_SIGNATURE = "SIG";
    
    /** The maximum size of a extension header (key). */
    public static final int MAX_KEY_SIZE_IN_BYTES = 15;

    /** The maximum size of a extension data (value). */
    public static final int MAX_VALUE_SIZE_IN_BYTES = 262143;

    /** The GGEP prefix.  A GGEP block will start with this byte value.
     */
    public static final byte GGEP_PREFIX_MAGIC_NUMBER = (byte) 0xC3;

    /** 
     * The collection of key/value pairs.  Rep. rationale: arrays of bytes are
     * convenient for values since they're easy to convert to numbers or
     * strings.  But strings are conventient for keys since they define hashCode
     * and equals.
     */
    private final Map<String, Object> _props = new TreeMap<String, Object>();

    /**
     * False iff this should COBS encode values to prevent null bytes.
     * Default is false, to be conservative.
     */
    public boolean notNeedCOBS=false;

	/**
	 * Cached hash code value to avoid calculating the hash code from the
	 * map each time.
	 */
	private volatile int hashCode = 0;


    //////////////////// Encoding/Decoding (Map <==> byte[]) ///////////////////

    /** 
     * Creates a new empty GGEP block.  Typically this is used for outgoing
     * messages and mutated before encoding.  
     *
     * @param notNeedCOBS true if nulls are allowed in extension values;false if
     *  this should activate COBS encoding if necessary to remove null bytes. 
     */
    public GGEP(boolean notNeedCOBS) {
        this.notNeedCOBS=notNeedCOBS;
    }    

    /** 
     * Creates a new empty GGEP block.  Typically this is used for outgoing
     * messages and mutated before encoding.  This does do COBS encoding.
     */
    public GGEP() {
        this(false);
    }
    
    /**
     * Constructs a new GGEP message with the given bytes & offset.
     */
    public GGEP(byte[] data, int offset) throws BadGGEPBlockException {
        this(data, offset, null);
    }

    /**
     *  Constructs a GGEP instance based on the GGEP block beginning at
     *  messageBytes[beginOffset].
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

        if (messageBytes.length < 4)
            throw new BadGGEPBlockException();

        // all GGEP blocks start with this prefix....
        if (messageBytes[beginOffset] != GGEP_PREFIX_MAGIC_NUMBER)
            throw new BadGGEPBlockException();

        boolean onLastExtension = false;
        int currIndex = beginOffset + 1;
        while (!onLastExtension) {

            // process extension header flags
            // bit order is interpreted as 76543210
            try {
                sanityCheck(messageBytes[currIndex]);
            } catch (ArrayIndexOutOfBoundsException malformedInput) {
                throw new BadGGEPBlockException();
            }
            onLastExtension = isLastExtension(messageBytes[currIndex]);
            boolean encoded = isEncoded(messageBytes[currIndex]);
            boolean compressed = isCompressed(messageBytes[currIndex]);
            int headerLen = deriveHeaderLength(messageBytes[currIndex]);

            // get the extension header
            currIndex++;
            String extensionHeader = null;
            try {
                extensionHeader = new String(messageBytes, currIndex,
                                             headerLen);
            } catch (StringIndexOutOfBoundsException inputIsMalformed) {
                throw new BadGGEPBlockException();
            }

            // get the data length
            currIndex += headerLen;
            int[] toIncrement = new int[1];
            final int dataLength = deriveDataLength(messageBytes, currIndex,
                                                    toIncrement);

            byte[] extensionData = null;

            currIndex+=toIncrement[0];
            if (dataLength > 0) {
                // ok, data is present, get it....

                byte[] data = new byte[dataLength];
                try {
                    System.arraycopy(messageBytes, currIndex, data, 0, 
                                     dataLength);
                } catch (ArrayIndexOutOfBoundsException malformedInput) {
                    throw new BadGGEPBlockException();
                }

                if (encoded) {
                    try {
                        data = COBSUtil.cobsDecode(data);
                    } catch (IOException badCobsEncoding) {
                        throw new BadGGEPBlockException("Bad COBS Encoding");
                    }
                }

                if (compressed) {
                    try {
                        data = IOUtils.inflate(data);
                    } catch(IOException badData) {
                        throw new BadGGEPBlockException("Bad compressed data");
                    }
                }

                extensionData = data;

                currIndex += dataLength;
            }

            // ok, everything checks out, just slap it in the hashmapper...
            if(compressed)
                _props.put(extensionHeader, new NeedsCompression(extensionData));
            else
                _props.put(extensionHeader, extensionData);

        }
        if ((endOffset != null) && (endOffset.length > 0))
            endOffset[0] = currIndex;
    }
    
    /**
     * Merges the other's GGEP with this' GGEP.
     */
    public void merge(GGEP other) {
        _props.putAll(other._props);
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

    /** @param increment a int array of size >0.  i'll put the number of bytes
     *  devoted to data storage in increment[0].
     */
    private int deriveDataLength(byte[] buff, int beginOffset, int increment[]) 
        throws BadGGEPBlockException {
        int length = 0, iterations = 0;
        // the length is stored in at most 3 bytes....
        final int MAX_ITERATIONS = 3;
        byte currByte;
        do {
            try {
                currByte = buff[beginOffset++];
            }
            catch (ArrayIndexOutOfBoundsException malformedInput) {
                throw new BadGGEPBlockException();
            }
            length = (length << 6) | (currByte & 0x3f);
            if (++iterations > MAX_ITERATIONS)
                throw new BadGGEPBlockException();
        } while (0x40 != (currByte & 0x40));
        increment[0] = iterations;
        return length;
    }

    /** Writes this GGEP instance as a properly formatted GGEP Block.
     *  @param out This GGEP instance is written to out.
     *  @exception IOException Thrown if had error writing to out.
     */
    public void write(OutputStream out) throws IOException {
        if (getHeaders().size() > 0) {
            // start with the magic prefix
            out.write(GGEP_PREFIX_MAGIC_NUMBER);

            Iterator<String> headers = getHeaders().iterator();
            // for each header, write the GGEP header and data
            while (headers.hasNext()) {
                String currHeader = headers.next();
                byte[] currData   = get(currHeader);
                int dataLen = 0;
                boolean shouldEncode = shouldCOBSEncode(currData);
                boolean shouldCompress = shouldCompress(currHeader);
                if (currData != null) {
                    if (shouldCompress) {
                        currData = IOUtils.deflate(currData);
                        if(currData.length > MAX_VALUE_SIZE_IN_BYTES)
                            throw new IllegalArgumentException("value for ["
                              + currHeader + "] too large after compression");
                    } if (shouldEncode)
                        currData = COBSUtil.cobsEncode(currData);
                    dataLen = currData.length;
                }
                writeHeader(currHeader, dataLen, 
                            !headers.hasNext(), out,
                            shouldEncode, shouldCompress);
                if (dataLen > 0) 
                    out.write(currData);
            }
        }
    }
    
    /**
     * Returns the GGEP as a byte array
     * @return an empty array if GGEP is empty
     */
    public byte[] toByteArray() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            write(out);
        } catch (IOException e) {
            ErrorService.error(e);
        }
        return out.toByteArray();
    }

    private final boolean shouldCOBSEncode(byte[] data) {
        // if nulls are allowed from construction time and if nulls are present
        // in the data...
        return (!notNeedCOBS && containsNull(data));
    }
    
    private final boolean shouldCompress(String header) {
        return (_props.get(header) instanceof NeedsCompression);
    }
    
    private void writeHeader(String header, final int dataLen, 
                             boolean isLast, OutputStream out, 
                             boolean isEncoded, boolean isCompressed) 
        throws IOException {

        // 1. WRITE THE HEADER FLAGS
        int flags = 0x00;
        if (isLast)
            flags |= 0x80;
        if (isEncoded)
            flags |= 0x40;
        if (isCompressed)
            flags |= 0x20;
        flags |= header.getBytes().length;
        out.write(flags);

        // 2. WRITE THE HEADER
        out.write(header.getBytes());

        // 3. WRITE THE DATA LEN
        // possibly 3 bytes
        int toWrite;
        int begin = dataLen & 0x3F000;
        if (dataLen > 0x00000fff) {
            begin = begin >> 12; // relevant bytes at the bottom now...
            toWrite = 0x80 | begin;
            out.write(toWrite);
        }
        int middle = dataLen & 0xFC0;
        if (dataLen > 0x0000003f) {
            middle = middle >> 6; // relevant bytes at the bottom now...
            toWrite = 0x80 | middle;
            out.write(toWrite);
        }
        int end = dataLen & 0x3F; // shut off everything except last 6 bits...
        toWrite = 0x40 | end;
        out.write(toWrite);
    }

    ////////////////////////// Key/Value Mutators and Accessors ////////////////
    
    /**
     * Adds all the specified key/value pairs.
     * TODO: Allow a value to be compressed.
     */
    public void putAll(List<? extends NameValue<?>> fields) throws IllegalArgumentException {
        for(NameValue<?> next : fields) {
            String key = next.getName();
            Object value = next.getValue();
            if(value == null)
                put(key);
            else if(value instanceof byte[])
                put(key, (byte[])value);
            else if(value instanceof String)
                put(key, (String)value);
            else if(value instanceof Integer)
                put(key, ((Integer)value).intValue());
            else if(value instanceof Long)
                put(key, ((Long)value).longValue());
            else
                throw new IllegalArgumentException("Unknown value: " + value);
        }
    }
    
    /**
     * Adds a key with data that should be compressed.
     */
    public void putCompressed(String key, byte[] value) throws IllegalArgumentException {
        validateKey(key);
        //validateValue(value); // done when writing.  TODO: do here?
        _props.put(key, new NeedsCompression(value));
    }

    /** 
     * Adds a key with raw byte value.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, inclusive
     * @param value the GGEP extension data
     * @exception IllegalArgumentException key is of an illegal length;
     *  or value contains a null bytes, null bytes are disallowed, and if you
     *  didn't allow nulls at construction but has nulls
     */
    public void put(String key, byte[] value) throws IllegalArgumentException {
        validateKey(key);
        validateValue(value);
        _props.put(key, value);
    }


    /** 
     * Adds a key with string value, using the default character encoding.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, inclusive
     * @param value the GGEP extension data
     * @exception IllegalArgumentException key is of an illegal length;
     *  or value contains a null bytes, null bytes are disallowed, if you
     *  didn't allow nulls at construction but has nulls
     */
    public void put(String key, String value) throws IllegalArgumentException {
        put(key, value==null ? null : value.getBytes());
    }

    /** 
     * Adds a key with integer value.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, inclusive
     * @param value the GGEP extension data, which should be an unsigned integer
     * @exception IllegalArgumentException key is of an illegal length; or value
     *  is negative; or value contains a null bytes, null bytes are disallowed,
     *  and COBS encoding is not supported 
     */
    public void put(String key, int value) throws IllegalArgumentException {
        if (value<0)  //TODO: ?
            throw new IllegalArgumentException("Negative value");
        put(key, ByteOrder.int2minLeb(value));
    }

    /** 
     * Adds a key with long value.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, inclusive
     * @param value the GGEP extension data, which should be an unsigned long
     * @exception IllegalArgumentException key is of an illegal length; or value
     *  is negative; or value contains a null bytes, null bytes are disallowed,
     *  and COBS encoding is not supported 
     */
    public void put(String key, long value) throws IllegalArgumentException {
        if (value<0)  //TODO: ?
            throw new IllegalArgumentException("Negative value");
        put(key, ByteOrder.long2minLeb(value));
    }

    /** 
     * Adds a key without any value.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, inclusive
     * @exception IllegalArgumentException key is of an illegal length.
     */
    public void put(String key) throws IllegalArgumentException {
        put(key, (byte[])null);
    }

    /**
     * Returns the value for a key, as raw bytes.
     * @param key the name of the GGEP extension
     * @return the GGEP extension data associated with the key
     * @exception BadGGEPPropertyException extension not found, was corrupt,
     *  or has no associated data.  Note that BadGGEPPropertyException is
     *  is always thrown for extensions with no data; use hasKey instead.
     */
    public byte[] getBytes(String key) throws BadGGEPPropertyException {
        byte[] ret= get(key);
        if (ret==null)
            throw new BadGGEPPropertyException();
        return ret;
    }

    /**
     * Returns the value for a key, as a string.
     * @param key the name of the GGEP extension
     * @return the GGEP extension data associated with the key
     * @exception BadGGEPPropertyException extension not found, was corrupt,
     *  or has no associated data.   Note that BadGGEPPropertyException is
     *  is always thrown for extensions with no data; use hasKey instead.
     */
    public String getString(String key) throws BadGGEPPropertyException {
        return new String(getBytes(key));
    }

    /**
     * Returns the value for a key, as an integer
     * @param key the name of the GGEP extension
     * @return the GGEP extension data associated with the key
     * @exception BadGGEPPropertyException extension not found, was corrupt,
     *  or has no associated data.   Note that BadGGEPPropertyException is
     *  is always thrown for extensions with no data; use hasKey instead.
     */
    public int getInt(String key) throws BadGGEPPropertyException {
        byte[] bytes=getBytes(key);
        if (bytes.length<1)
            throw new BadGGEPPropertyException("No bytes");
        if (bytes.length>4)
            throw new BadGGEPPropertyException("Integer too big");
        return ByteOrder.leb2int(bytes, 0, bytes.length);
    }
    
    /**
     * Returns the value for a key as a long.
     * @param key the name of the GGEP extension
     * @return the GGEP extension data associated with the key
     * @exception BadGGEPPropertyException extension not found, was corrupt,
     *  or has no associated data.   Note that BadGGEPPropertyException is
     *  is always thrown for extensions with no data; use hasKey instead.
     */
    public long getLong(String key) throws BadGGEPPropertyException {
        byte[] bytes=getBytes(key);
        if (bytes.length<1)
            throw new BadGGEPPropertyException("No bytes");
        if (bytes.length>8)
            throw new BadGGEPPropertyException("Integer too big");
        return ByteOrder.leb2long(bytes, 0, bytes.length);
    }

    /**
     * Returns whether this has the given key.
     * @param key the name of the GGEP extension
     * @return true if this has a key
     */
    public boolean hasKey(String key) {
        return _props.containsKey(key);
    }

    /** 
     * Returns the set of keys.
     * @return a set of all the GGEP extension header name in this, each
     *  as a String.
     */
    public Set<String> getHeaders() {
        return _props.keySet();
    }
    
    /**
     * Returns whether this GGEP is empty or not.
     */
    public boolean isEmpty() {
        return _props.isEmpty();
    }
    
    /**
     * Gets the byte[] data from props.
     */
    public byte[] get(String key) {
        Object value = _props.get(key);
        if(value instanceof NeedsCompression)
            return ((NeedsCompression)value).data;
        else
            return (byte[])value;
    }

    private void validateKey(String key) throws IllegalArgumentException {
        byte[] bytes=key.getBytes();
        if ((key == null)
                || key.equals("")
                || (bytes.length > MAX_KEY_SIZE_IN_BYTES)
                || containsNull(bytes))
            throw new IllegalArgumentException();
    }

    private void validateValue(byte[] value) throws IllegalArgumentException {
        if (value==null)
            return;
        if (value.length>MAX_VALUE_SIZE_IN_BYTES)
            throw new IllegalArgumentException();
    }

    private boolean containsNull(byte[] bytes) {
        if (bytes != null) {
            for (int i = 0; i < bytes.length; i++)
                if (bytes[i] == 0x0)
                    return true;
        }
        return false;
    }
    
    //////////////////////////////// Miscellany ///////////////////////////////

    /** @return True if the two Maps that represent header/data pairs are
     *  equivalent.
     */
    public boolean equals(Object o) {
		if(o == this) return true;
        if (! (o instanceof GGEP))
            return false; 
        //This is O(n lg n) time with n keys.  It would be great if we could
        //just check that the trees are isomorphic.  I don't think this code is
        //really used anywhere, however.
        return this.subset((GGEP)o) && ((GGEP)o).subset(this);
    }
    
    /** Returns true if this is a subset of other, e.g., all of this' keys 
     *  can be found in OTHER with the same value. */
    private boolean subset(GGEP other) {
        for(String key : _props.keySet()) {
            byte[] v1= this.get(key);
            byte[] v2= other.get(key);
            //Remember that v1 and v2 can be null.
            if ((v1==null) != (v2==null))
                return false;
            if (v1!=null && !Arrays.equals(v1, v2))
                return false;
        }
        return true;
    }
                
	// overrides Object.hashCode to be consistent with equals
	public int hashCode() {
		if(hashCode == 0) {
			hashCode = 37 * _props.hashCode();
		}
		return hashCode;
	}
	
	/**
	 * Marker class that wraps a byte[] value, if that value
	 * is going to require compression upon write.
	 */
	private static class NeedsCompression {
	    final byte[] data;
	    NeedsCompression(byte[] data) {
	        this.data = data;
	    }
	}
}





