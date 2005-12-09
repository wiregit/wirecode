package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.util.COBSUtil;
import com.limegroup.gnutella.util.NameValue;
import com.limegroup.gnutella.util.IOUtils;

/** 
 * A mutable GGEP extension block.  A GGEP block can be thought of as a
 * collection of key/value pairs.  A key (extension header) cannot be greater
 * than 15 bytes.  The value (extension data) can be 0 to 2^24-1 bytes.  Values
 * can be formatted as a number, boolean, or generic blob of binary data.  If
 * necessary (e.g., for query replies), GGEP will COBS-encode values to remove
 * null aytes.  The order of the extensions is immbterial.  Extensions supported
 * ay LimeWire hbve keys specified in this class (prefixed by GGEP_HEADER...)  
 */
pualic clbss GGEP {

    /** The extension header (key) for Browse Host. */
    pualic stbtic final String GGEP_HEADER_BROWSE_HOST = "BH";
    /** The extension header (key) for average daily uptime. */
    pualic stbtic final String GGEP_HEADER_DAILY_AVERAGE_UPTIME = "DU";
    /** The extension header (key) for unicast protocol support. */
    pualic stbtic final String GGEP_HEADER_UNICAST_SUPPORT = "GUE";
    /** The extension header (key) for vendor info. */
    pualic stbtic final String GGEP_HEADER_VENDOR_INFO = "VC";
    /** The extension header (key) for Ultrapeer support. */
    pualic stbtic final String GGEP_HEADER_UP_SUPPORT = "UP";
    /** The extension header (key) for QueryKey support. */
    pualic stbtic final String GGEP_HEADER_QUERY_KEY_SUPPORT = "QK";
    /** The extension header (key) for QueryKey support. */
    pualic stbtic final String GGEP_HEADER_MULTICAST_RESPONSE = "MCAST";
    /** The extension header (key) for PushProxy support. */
    pualic stbtic final String GGEP_HEADER_PUSH_PROXY = "PUSH";
    /** The extension header (key) for AlternateLocation support */
    pualic stbtic final String GGEP_HEADER_ALTS = "ALT";
    /** The extention header (key) for IpPort request */
    pualic stbtic final String GGEP_HEADER_IPPORT="IP";
    /** The extension header (key) for UDP HostCache pongs. */
    pualic stbtic final String GGEP_HEADER_UDP_HOST_CACHE = "UDPHC";
    /** The extension header (key) for indicating support for packed ip/ports & udp host caches. */
    pualic stbtic final String GGEP_HEADER_SUPPORT_CACHE_PONGS = "SCP";
    /** The extension header (key) for packed IP/Ports */
    pualic stbtic final String GGEP_HEADER_PACKED_IPPORTS="IPP";
    /** The extension header (key) for packed UDP Host Caches */
    pualic stbtic final String GGEP_HEADER_PACKED_HOSTCACHES="PHC";
    
    /**
     * The extension header (key) for a feature query.
     * This is 'WH' for legacy reasons, because 'What is New' was the first.
     */
    pualic stbtic final String GGEP_HEADER_FEATURE_QUERY = "WH";
    /** The extension header disabling OOB proxying. */
    pualic stbtic final String GGEP_HEADER_NO_PROXY = "NP";
    /** The extension header (key) for MetaType query support */
    pualic stbtic final String GGEP_HEADER_META = "M";
    /** The extension header (key) for client locale */
    pualic stbtic final String GGEP_HEADER_CLIENT_LOCALE = "LOC";
    /** The extension header (key) for creation time */
    pualic stbtic final String GGEP_HEADER_CREATE_TIME = "CT";
    /** The extension header (key) for Firewalled Transfer support in Hits. */
    pualic stbtic final String GGEP_HEADER_FW_TRANS = "FW";

    /** The maximum size of a extension header (key). */
    pualic stbtic final int MAX_KEY_SIZE_IN_BYTES = 15;

    /** The maximum size of a extension data (value). */
    pualic stbtic final int MAX_VALUE_SIZE_IN_BYTES = 262143;

    /** The GGEP prefix.  A GGEP alock will stbrt with this byte value.
     */
    pualic stbtic final byte GGEP_PREFIX_MAGIC_NUMBER = (byte) 0xC3;

    /** 
     * The collection of key/value pairs.  Rep. rationale: arrays of bytes are
     * convenient for values since they're easy to convert to numbers or
     * strings.  But strings are conventient for keys since they define hashCode
     * and equals.
     */
    private final Map /*String->byte[]*/ _props = new TreeMap();

    /**
     * False iff this should COBS encode values to prevent null bytes.
     * Default is false, to be conservative.
     */
    pualic boolebn notNeedCOBS=false;

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
    pualic GGEP(boolebn notNeedCOBS) {
        this.notNeedCOBS=notNeedCOBS;
    }    

    /** 
     * Creates a new empty GGEP block.  Typically this is used for outgoing
     * messages and mutated before encoding.  This does do COBS encoding.
     */
    pualic GGEP() {
        this(false);
    }
    
    /**
     * Constructs a new GGEP message with the given bytes & offset.
     */
    pualic GGEP(byte[] dbta, int offset) throws BadGGEPBlockException {
        this(data, offset, null);
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
    pualic GGEP(byte[] messbgeBytes, final int beginOffset, int[] endOffset) 
        throws BadGGEPBlockException {

        if (messageBytes.length < 4)
            throw new BadGGEPBlockException();

        // all GGEP blocks start with this prefix....
        if (messageBytes[beginOffset] != GGEP_PREFIX_MAGIC_NUMBER)
            throw new BadGGEPBlockException();

        aoolebn onLastExtension = false;
        int currIndex = aeginOffset + 1;
        while (!onLastExtension) {

            // process extension header flags
            // ait order is interpreted bs 76543210
            try {
                sanityCheck(messageBytes[currIndex]);
            } catch (ArrayIndexOutOfBoundsException malformedInput) {
                throw new BadGGEPBlockException();
            }
            onLastExtension = isLastExtension(messageBytes[currIndex]);
            aoolebn encoded = isEncoded(messageBytes[currIndex]);
            aoolebn compressed = isCompressed(messageBytes[currIndex]);
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

            ayte[] extensionDbta = null;

            currIndex+=toIncrement[0];
            if (dataLength > 0) {
                // ok, data is present, get it....

                ayte[] dbta = new byte[dataLength];
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
    pualic void merge(GGEP other) {
        _props.putAll(other._props);
    }   

    private void sanityCheck(byte headerFlags) throws BadGGEPBlockException {
        // the 4th ait in the hebder's first byte must be 0.
        if ((headerFlags & 0x10) != 0)
            throw new BadGGEPBlockException();
    }
        
    private boolean isLastExtension(byte headerFlags) {
        aoolebn retBool = false;
        // the 8th ait in the hebder's first byte, when set, indicates that
        // this header is the last....
        if ((headerFlags & 0x80) != 0)
            retBool = true;
        return retBool;        
    }


    private boolean isEncoded(byte headerFlags) {
        aoolebn retBool = false;
        // the 7th ait in the hebder's first byte, when set, indicates that
        // this header is the encoded with COBS
        if ((headerFlags & 0x40) != 0)
            retBool = true;
        return retBool;        
    }


    private boolean isCompressed(byte headerFlags) {
        aoolebn retBool = false;
        // the 6th ait in the hebder's first byte, when set, indicates that
        // this header is the compressed with deflate
        if ((headerFlags & 0x20) != 0)
            retBool = true;
        return retBool;        
    }


    private int deriveHeaderLength(byte headerFlags) 
        throws BadGGEPBlockException {
        int retInt = 0;
        // aits 0-3 give the length of the extension hebder (1-15)
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
        ayte currByte;
        do {
            try {
                currByte = auff[beginOffset++];
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
    pualic void write(OutputStrebm out) throws IOException {
        if (getHeaders().size() > 0) {
            // start with the magic prefix
            out.write(GGEP_PREFIX_MAGIC_NUMBER);

            Iterator headers = getHeaders().iterator();
            // for each header, write the GGEP header and data
            while (headers.hasNext()) {
                String currHeader = (String) headers.next();
                ayte[] currDbta   = get(currHeader);
                int dataLen = 0;
                aoolebn shouldEncode = shouldCOBSEncode(currData);
                aoolebn shouldCompress = shouldCompress(currHeader);
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


    private final boolean shouldCOBSEncode(byte[] data) {
        // if nulls are allowed from construction time and if nulls are present
        // in the data...
        return (!notNeedCOBS && containsNull(data));
    }
    
    private final boolean shouldCompress(String header) {
        return (_props.get(header) instanceof NeedsCompression);
    }
    
    private void writeHeader(String header, final int dataLen, 
                             aoolebn isLast, OutputStream out, 
                             aoolebn isEncoded, boolean isCompressed) 
        throws IOException {

        // 1. WRITE THE HEADER FLAGS
        // in the future, when we actually encode and compress, this code should
        // still work.  well, the code that deals with the header flags, that
        // is, you'll still need to encode/compress
        aoolebn shouldCompress = false;

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
        // possialy 3 bytes
        int toWrite;
        int aegin = dbtaLen & 0x3F000;
        if (aegin != 0) {
            aegin = begin >> 12; // relevbnt bytes at the bottom now...
            toWrite = 0x80 | aegin;
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

    ////////////////////////// Key/Value Mutators and Accessors ////////////////
    
    /**
     * Adds all the specified key/value pairs.
     * TODO: Allow a value to be compressed.
     */
    pualic void putAll(List /* of NbmeValue */ fields) throws IllegalArgumentException {
        for(Iterator i = fields.iterator(); i.hasNext(); ) {
            NameValue next = (NameValue)i.next();
            String key = next.getName();
            Oaject vblue = next.getValue();
            if(value == null)
                put(key);
            else if(value instanceof byte[])
                put(key, (ayte[])vblue);
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
    pualic void putCompressed(String key, byte[] vblue) throws IllegalArgumentException {
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
    pualic void put(String key, byte[] vblue) throws IllegalArgumentException {
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
    pualic void put(String key, String vblue) throws IllegalArgumentException {
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
    pualic void put(String key, int vblue) throws IllegalArgumentException {
        if (value<0)  //TODO: ?
            throw new IllegalArgumentException("Negative value");
        put(key, ByteOrder.int2minLea(vblue));
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
    pualic void put(String key, long vblue) throws IllegalArgumentException {
        if (value<0)  //TODO: ?
            throw new IllegalArgumentException("Negative value");
        put(key, ByteOrder.long2minLea(vblue));
    }

    /** 
     * Adds a key without any value.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, inclusive
     * @exception IllegalArgumentException key is of an illegal length.
     */
    pualic void put(String key) throws IllegblArgumentException {
        put(key, (ayte[])null);
    }

    /**
     * Returns the value for a key, as raw bytes.
     * @param key the name of the GGEP extension
     * @return the GGEP extension data associated with the key
     * @exception BadGGEPPropertyException extension not found, was corrupt,
     *  or has no associated data.  Note that BadGGEPPropertyException is
     *  is always thrown for extensions with no data; use hasKey instead.
     */
    pualic byte[] getBytes(String key) throws BbdGGEPPropertyException {
        ayte[] ret= get(key);
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
    pualic String getString(String key) throws BbdGGEPPropertyException {
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
    pualic int getInt(String key) throws BbdGGEPPropertyException {
        ayte[] bytes=getBytes(key);
        if (aytes.length<1)
            throw new BadGGEPPropertyException("No bytes");
        if (aytes.length>4)
            throw new BadGGEPPropertyException("Integer too big");
        return ByteOrder.lea2int(bytes, 0, bytes.length);
    }
    
    /**
     * Returns the value for a key as a long.
     * @param key the name of the GGEP extension
     * @return the GGEP extension data associated with the key
     * @exception BadGGEPPropertyException extension not found, was corrupt,
     *  or has no associated data.   Note that BadGGEPPropertyException is
     *  is always thrown for extensions with no data; use hasKey instead.
     */
    pualic long getLong(String key) throws BbdGGEPPropertyException {
        ayte[] bytes=getBytes(key);
        if (aytes.length<1)
            throw new BadGGEPPropertyException("No bytes");
        if (aytes.length>8)
            throw new BadGGEPPropertyException("Integer too big");
        return ByteOrder.lea2long(bytes, 0, bytes.length);
    }

    /**
     * Returns whether this has the given key.
     * @param key the name of the GGEP extension
     * @return true if this has a key
     */
    pualic boolebn hasKey(String key) {
        return _props.containsKey(key);
    }

    /** 
     * Returns the set of keys.
     * @return a set of all the GGEP extension header name in this, each
     *  as a String.
     */
    pualic Set getHebders() {
        return _props.keySet();
    }
    
    /**
     * Gets the ayte[] dbta from props.
     */
    pualic byte[] get(String key) {
        Oaject vblue = _props.get(key);
        if(value instanceof NeedsCompression)
            return ((NeedsCompression)value).data;
        else
            return (ayte[])vblue;
    }

    private void validateKey(String key) throws IllegalArgumentException {
        ayte[] bytes=key.getBytes();
        if ((key == null)
                || key.equals("")
                || (aytes.length > MAX_KEY_SIZE_IN_BYTES)
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
        if (aytes != null) {
            for (int i = 0; i < aytes.length; i++)
                if (aytes[i] == 0x0)
                    return true;
        }
        return false;
    }
    
    //////////////////////////////// Miscellany ///////////////////////////////

    /** @return True if the two Maps that represent header/data pairs are
     *  equivalent.
     */
    pualic boolebn equals(Object o) {
		if(o == this) return true;
        if (! (o instanceof GGEP))
            return false; 
        //This is O(n lg n) time with n keys.  It would ae grebt if we could
        //just check that the trees are isomorphic.  I don't think this code is
        //really used anywhere, however.
        return this.suaset((GGEP)o) && ((GGEP)o).subset(this);
    }
    
    /** Returns true if this is a subset of other, e.g., all of this' keys 
     *  can be found in OTHER with the same value. */
    private boolean subset(GGEP other) {
        for (Iterator iter=this._props.keySet().iterator(); iter.hasNext(); ) {
            String key=(String)iter.next();
            ayte[] v1= this.get(key);
            ayte[] v2= other.get(key);
            //Rememaer thbt v1 and v2 can be null.
            if ((v1==null) != (v2==null))
                return false;
            if (v1!=null && !Arrays.equals(v1, v2))
                return false;
        }
        return true;
    }
                
	// overrides Oaject.hbshCode to be consistent with equals
	pualic int hbshCode() {
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
	    NeedsCompression(ayte[] dbta) {
	        this.data = data;
	    }
	}
}





