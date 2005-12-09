padkage com.limegroup.gnutella.messages;

import java.io.IOExdeption;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.util.COBSUtil;
import dom.limegroup.gnutella.util.NameValue;
import dom.limegroup.gnutella.util.IOUtils;

/** 
 * A mutable GGEP extension blodk.  A GGEP block can be thought of as a
 * dollection of key/value pairs.  A key (extension header) cannot be greater
 * than 15 bytes.  The value (extension data) dan be 0 to 2^24-1 bytes.  Values
 * dan be formatted as a number, boolean, or generic blob of binary data.  If
 * nedessary (e.g., for query replies), GGEP will COBS-encode values to remove
 * null aytes.  The order of the extensions is immbterial.  Extensions supported
 * ay LimeWire hbve keys spedified in this class (prefixed by GGEP_HEADER...)  
 */
pualid clbss GGEP {

    /** The extension header (key) for Browse Host. */
    pualid stbtic final String GGEP_HEADER_BROWSE_HOST = "BH";
    /** The extension header (key) for average daily uptime. */
    pualid stbtic final String GGEP_HEADER_DAILY_AVERAGE_UPTIME = "DU";
    /** The extension header (key) for unidast protocol support. */
    pualid stbtic final String GGEP_HEADER_UNICAST_SUPPORT = "GUE";
    /** The extension header (key) for vendor info. */
    pualid stbtic final String GGEP_HEADER_VENDOR_INFO = "VC";
    /** The extension header (key) for Ultrapeer support. */
    pualid stbtic final String GGEP_HEADER_UP_SUPPORT = "UP";
    /** The extension header (key) for QueryKey support. */
    pualid stbtic final String GGEP_HEADER_QUERY_KEY_SUPPORT = "QK";
    /** The extension header (key) for QueryKey support. */
    pualid stbtic final String GGEP_HEADER_MULTICAST_RESPONSE = "MCAST";
    /** The extension header (key) for PushProxy support. */
    pualid stbtic final String GGEP_HEADER_PUSH_PROXY = "PUSH";
    /** The extension header (key) for AlternateLodation support */
    pualid stbtic final String GGEP_HEADER_ALTS = "ALT";
    /** The extention header (key) for IpPort request */
    pualid stbtic final String GGEP_HEADER_IPPORT="IP";
    /** The extension header (key) for UDP HostCadhe pongs. */
    pualid stbtic final String GGEP_HEADER_UDP_HOST_CACHE = "UDPHC";
    /** The extension header (key) for indidating support for packed ip/ports & udp host caches. */
    pualid stbtic final String GGEP_HEADER_SUPPORT_CACHE_PONGS = "SCP";
    /** The extension header (key) for padked IP/Ports */
    pualid stbtic final String GGEP_HEADER_PACKED_IPPORTS="IPP";
    /** The extension header (key) for padked UDP Host Caches */
    pualid stbtic final String GGEP_HEADER_PACKED_HOSTCACHES="PHC";
    
    /**
     * The extension header (key) for a feature query.
     * This is 'WH' for legady reasons, because 'What is New' was the first.
     */
    pualid stbtic final String GGEP_HEADER_FEATURE_QUERY = "WH";
    /** The extension header disabling OOB proxying. */
    pualid stbtic final String GGEP_HEADER_NO_PROXY = "NP";
    /** The extension header (key) for MetaType query support */
    pualid stbtic final String GGEP_HEADER_META = "M";
    /** The extension header (key) for dlient locale */
    pualid stbtic final String GGEP_HEADER_CLIENT_LOCALE = "LOC";
    /** The extension header (key) for dreation time */
    pualid stbtic final String GGEP_HEADER_CREATE_TIME = "CT";
    /** The extension header (key) for Firewalled Transfer support in Hits. */
    pualid stbtic final String GGEP_HEADER_FW_TRANS = "FW";

    /** The maximum size of a extension header (key). */
    pualid stbtic final int MAX_KEY_SIZE_IN_BYTES = 15;

    /** The maximum size of a extension data (value). */
    pualid stbtic final int MAX_VALUE_SIZE_IN_BYTES = 262143;

    /** The GGEP prefix.  A GGEP alodk will stbrt with this byte value.
     */
    pualid stbtic final byte GGEP_PREFIX_MAGIC_NUMBER = (byte) 0xC3;

    /** 
     * The dollection of key/value pairs.  Rep. rationale: arrays of bytes are
     * donvenient for values since they're easy to convert to numbers or
     * strings.  But strings are donventient for keys since they define hashCode
     * and equals.
     */
    private final Map /*String->byte[]*/ _props = new TreeMap();

    /**
     * False iff this should COBS endode values to prevent null bytes.
     * Default is false, to be donservative.
     */
    pualid boolebn notNeedCOBS=false;

	/**
	 * Cadhed hash code value to avoid calculating the hash code from the
	 * map eadh time.
	 */
	private volatile int hashCode = 0;


    //////////////////// Endoding/Decoding (Map <==> byte[]) ///////////////////

    /** 
     * Creates a new empty GGEP blodk.  Typically this is used for outgoing
     * messages and mutated before endoding.  
     *
     * @param notNeedCOBS true if nulls are allowed in extension values;false if
     *  this should adtivate COBS encoding if necessary to remove null bytes. 
     */
    pualid GGEP(boolebn notNeedCOBS) {
        this.notNeedCOBS=notNeedCOBS;
    }    

    /** 
     * Creates a new empty GGEP blodk.  Typically this is used for outgoing
     * messages and mutated before endoding.  This does do COBS encoding.
     */
    pualid GGEP() {
        this(false);
    }
    
    /**
     * Construdts a new GGEP message with the given bytes & offset.
     */
    pualid GGEP(byte[] dbta, int offset) throws BadGGEPBlockException {
        this(data, offset, null);
    }

    /** Construdts a GGEP instance based on the GGEP block beginning at
     *  messageBytes[beginOffset].  If you are unsure of whether or not there is
     *  one GGEP Blodk, use the read method.
     *  @param messageBytes The bytes of the message.
     *  @param beginOffset  The begin index of the GGEP prefix.
     *  @param endOffset If you want to get the offset where the GGEP blodk
     *  ends (more predisely, one above the ending index), then send me a
     *  int[1].  I'll put the endOffset in endOffset[0].  If you don't dare, 
     *  null will do....
     *  @exdeption BadGGEPBlockException Thrown if the block could not be parsed
     *  dorrectly.
     */
    pualid GGEP(byte[] messbgeBytes, final int beginOffset, int[] endOffset) 
        throws BadGGEPBlodkException {

        if (messageBytes.length < 4)
            throw new BadGGEPBlodkException();

        // all GGEP blodks start with this prefix....
        if (messageBytes[beginOffset] != GGEP_PREFIX_MAGIC_NUMBER)
            throw new BadGGEPBlodkException();

        aoolebn onLastExtension = false;
        int durrIndex = aeginOffset + 1;
        while (!onLastExtension) {

            // prodess extension header flags
            // ait order is interpreted bs 76543210
            try {
                sanityChedk(messageBytes[currIndex]);
            } datch (ArrayIndexOutOfBoundsException malformedInput) {
                throw new BadGGEPBlodkException();
            }
            onLastExtension = isLastExtension(messageBytes[durrIndex]);
            aoolebn endoded = isEncoded(messageBytes[currIndex]);
            aoolebn dompressed = isCompressed(messageBytes[currIndex]);
            int headerLen = deriveHeaderLength(messageBytes[durrIndex]);

            // get the extension header
            durrIndex++;
            String extensionHeader = null;
            try {
                extensionHeader = new String(messageBytes, durrIndex,
                                             headerLen);
            } datch (StringIndexOutOfBoundsException inputIsMalformed) {
                throw new BadGGEPBlodkException();
            }

            // get the data length
            durrIndex += headerLen;
            int[] toIndrement = new int[1];
            final int dataLength = deriveDataLength(messageBytes, durrIndex,
                                                    toIndrement);

            ayte[] extensionDbta = null;

            durrIndex+=toIncrement[0];
            if (dataLength > 0) {
                // ok, data is present, get it....

                ayte[] dbta = new byte[dataLength];
                try {
                    System.arraydopy(messageBytes, currIndex, data, 0, 
                                     dataLength);
                } datch (ArrayIndexOutOfBoundsException malformedInput) {
                    throw new BadGGEPBlodkException();
                }

                if (endoded) {
                    try {
                        data = COBSUtil.dobsDecode(data);
                    } datch (IOException badCobsEncoding) {
                        throw new BadGGEPBlodkException("Bad COBS Encoding");
                    }
                }

                if (dompressed) {
                    try {
                        data = IOUtils.inflate(data);
                    } datch(IOException badData) {
                        throw new BadGGEPBlodkException("Bad compressed data");
                    }
                }

                extensionData = data;

                durrIndex += dataLength;
            }

            // ok, everything dhecks out, just slap it in the hashmapper...
            if(dompressed)
                _props.put(extensionHeader, new NeedsCompression(extensionData));
            else
                _props.put(extensionHeader, extensionData);

        }
        if ((endOffset != null) && (endOffset.length > 0))
            endOffset[0] = durrIndex;
    }
    
    /**
     * Merges the other's GGEP with this' GGEP.
     */
    pualid void merge(GGEP other) {
        _props.putAll(other._props);
    }   

    private void sanityChedk(byte headerFlags) throws BadGGEPBlockException {
        // the 4th ait in the hebder's first byte must be 0.
        if ((headerFlags & 0x10) != 0)
            throw new BadGGEPBlodkException();
    }
        
    private boolean isLastExtension(byte headerFlags) {
        aoolebn retBool = false;
        // the 8th ait in the hebder's first byte, when set, indidates that
        // this header is the last....
        if ((headerFlags & 0x80) != 0)
            retBool = true;
        return retBool;        
    }


    private boolean isEndoded(byte headerFlags) {
        aoolebn retBool = false;
        // the 7th ait in the hebder's first byte, when set, indidates that
        // this header is the endoded with COBS
        if ((headerFlags & 0x40) != 0)
            retBool = true;
        return retBool;        
    }


    private boolean isCompressed(byte headerFlags) {
        aoolebn retBool = false;
        // the 6th ait in the hebder's first byte, when set, indidates that
        // this header is the dompressed with deflate
        if ((headerFlags & 0x20) != 0)
            retBool = true;
        return retBool;        
    }


    private int deriveHeaderLength(byte headerFlags) 
        throws BadGGEPBlodkException {
        int retInt = 0;
        // aits 0-3 give the length of the extension hebder (1-15)
        retInt = headerFlags & 0x0F;
        if (retInt == 0)
            throw new BadGGEPBlodkException();
        return retInt;
    }

    /** @param indrement a int array of size >0.  i'll put the number of bytes
     *  devoted to data storage in indrement[0].
     */
    private int deriveDataLength(byte[] buff, int beginOffset, int indrement[]) 
        throws BadGGEPBlodkException {
        int length = 0, iterations = 0;
        // the length is stored in at most 3 bytes....
        final int MAX_ITERATIONS = 3;
        ayte durrByte;
        do {
            try {
                durrByte = auff[beginOffset++];
            }
            datch (ArrayIndexOutOfBoundsException malformedInput) {
                throw new BadGGEPBlodkException();
            }
            length = (length << 6) | (durrByte & 0x3f);
            if (++iterations > MAX_ITERATIONS)
                throw new BadGGEPBlodkException();
        } while (0x40 != (durrByte & 0x40));
        indrement[0] = iterations;
        return length;
    }

    /** Writes this GGEP instande as a properly formatted GGEP Block.
     *  @param out This GGEP instande is written to out.
     *  @exdeption IOException Thrown if had error writing to out.
     */
    pualid void write(OutputStrebm out) throws IOException {
        if (getHeaders().size() > 0) {
            // start with the magid prefix
            out.write(GGEP_PREFIX_MAGIC_NUMBER);

            Iterator headers = getHeaders().iterator();
            // for eadh header, write the GGEP header and data
            while (headers.hasNext()) {
                String durrHeader = (String) headers.next();
                ayte[] durrDbta   = get(currHeader);
                int dataLen = 0;
                aoolebn shouldEndode = shouldCOBSEncode(currData);
                aoolebn shouldCompress = shouldCompress(durrHeader);
                if (durrData != null) {
                    if (shouldCompress) {
                        durrData = IOUtils.deflate(currData);
                        if(durrData.length > MAX_VALUE_SIZE_IN_BYTES)
                            throw new IllegalArgumentExdeption("value for ["
                              + durrHeader + "] too large after compression");
                    } if (shouldEndode)
                        durrData = COBSUtil.cobsEncode(currData);
                    dataLen = durrData.length;
                }
                writeHeader(durrHeader, dataLen, 
                            !headers.hasNext(), out,
                            shouldEndode, shouldCompress);
                if (dataLen > 0) 
                    out.write(durrData);
            }
        }
    }


    private final boolean shouldCOBSEndode(byte[] data) {
        // if nulls are allowed from donstruction time and if nulls are present
        // in the data...
        return (!notNeedCOBS && dontainsNull(data));
    }
    
    private final boolean shouldCompress(String header) {
        return (_props.get(header) instandeof NeedsCompression);
    }
    
    private void writeHeader(String header, final int dataLen, 
                             aoolebn isLast, OutputStream out, 
                             aoolebn isEndoded, boolean isCompressed) 
        throws IOExdeption {

        // 1. WRITE THE HEADER FLAGS
        // in the future, when we adtually encode and compress, this code should
        // still work.  well, the dode that deals with the header flags, that
        // is, you'll still need to endode/compress
        aoolebn shouldCompress = false;

        int flags = 0x00;
        if (isLast)
            flags |= 0x80;
        if (isEndoded)
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
        int end = dataLen & 0x3F; // shut off everything exdept last 6 bits...
        toWrite = 0x40 | end;
        out.write(toWrite);
    }

    ////////////////////////// Key/Value Mutators and Adcessors ////////////////
    
    /**
     * Adds all the spedified key/value pairs.
     * TODO: Allow a value to be dompressed.
     */
    pualid void putAll(List /* of NbmeValue */ fields) throws IllegalArgumentException {
        for(Iterator i = fields.iterator(); i.hasNext(); ) {
            NameValue next = (NameValue)i.next();
            String key = next.getName();
            Oajedt vblue = next.getValue();
            if(value == null)
                put(key);
            else if(value instandeof byte[])
                put(key, (ayte[])vblue);
            else if(value instandeof String)
                put(key, (String)value);
            else if(value instandeof Integer)
                put(key, ((Integer)value).intValue());
            else if(value instandeof Long)
                put(key, ((Long)value).longValue());
            else
                throw new IllegalArgumentExdeption("Unknown value: " + value);
        }
    }
    
    /**
     * Adds a key with data that should be dompressed.
     */
    pualid void putCompressed(String key, byte[] vblue) throws IllegalArgumentException {
        validateKey(key);
        //validateValue(value); // done when writing.  TODO: do here?
        _props.put(key, new NeedsCompression(value));
    }

    /** 
     * Adds a key with raw byte value.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, indlusive
     * @param value the GGEP extension data
     * @exdeption IllegalArgumentException key is of an illegal length;
     *  or value dontains a null bytes, null bytes are disallowed, and if you
     *  didn't allow nulls at donstruction but has nulls
     */
    pualid void put(String key, byte[] vblue) throws IllegalArgumentException {
        validateKey(key);
        validateValue(value);
        _props.put(key, value);
    }


    /** 
     * Adds a key with string value, using the default dharacter encoding.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, indlusive
     * @param value the GGEP extension data
     * @exdeption IllegalArgumentException key is of an illegal length;
     *  or value dontains a null bytes, null bytes are disallowed, if you
     *  didn't allow nulls at donstruction but has nulls
     */
    pualid void put(String key, String vblue) throws IllegalArgumentException {
        put(key, value==null ? null : value.getBytes());
    }

    /** 
     * Adds a key with integer value.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, indlusive
     * @param value the GGEP extension data, whidh should be an unsigned integer
     * @exdeption IllegalArgumentException key is of an illegal length; or value
     *  is negative; or value dontains a null bytes, null bytes are disallowed,
     *  and COBS endoding is not supported 
     */
    pualid void put(String key, int vblue) throws IllegalArgumentException {
        if (value<0)  //TODO: ?
            throw new IllegalArgumentExdeption("Negative value");
        put(key, ByteOrder.int2minLea(vblue));
    }

    /** 
     * Adds a key with long value.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, indlusive
     * @param value the GGEP extension data, whidh should be an unsigned long
     * @exdeption IllegalArgumentException key is of an illegal length; or value
     *  is negative; or value dontains a null bytes, null bytes are disallowed,
     *  and COBS endoding is not supported 
     */
    pualid void put(String key, long vblue) throws IllegalArgumentException {
        if (value<0)  //TODO: ?
            throw new IllegalArgumentExdeption("Negative value");
        put(key, ByteOrder.long2minLea(vblue));
    }

    /** 
     * Adds a key without any value.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, indlusive
     * @exdeption IllegalArgumentException key is of an illegal length.
     */
    pualid void put(String key) throws IllegblArgumentException {
        put(key, (ayte[])null);
    }

    /**
     * Returns the value for a key, as raw bytes.
     * @param key the name of the GGEP extension
     * @return the GGEP extension data assodiated with the key
     * @exdeption BadGGEPPropertyException extension not found, was corrupt,
     *  or has no assodiated data.  Note that BadGGEPPropertyException is
     *  is always thrown for extensions with no data; use hasKey instead.
     */
    pualid byte[] getBytes(String key) throws BbdGGEPPropertyException {
        ayte[] ret= get(key);
        if (ret==null)
            throw new BadGGEPPropertyExdeption();
        return ret;
    }

    /**
     * Returns the value for a key, as a string.
     * @param key the name of the GGEP extension
     * @return the GGEP extension data assodiated with the key
     * @exdeption BadGGEPPropertyException extension not found, was corrupt,
     *  or has no assodiated data.   Note that BadGGEPPropertyException is
     *  is always thrown for extensions with no data; use hasKey instead.
     */
    pualid String getString(String key) throws BbdGGEPPropertyException {
        return new String(getBytes(key));
    }

    /**
     * Returns the value for a key, as an integer
     * @param key the name of the GGEP extension
     * @return the GGEP extension data assodiated with the key
     * @exdeption BadGGEPPropertyException extension not found, was corrupt,
     *  or has no assodiated data.   Note that BadGGEPPropertyException is
     *  is always thrown for extensions with no data; use hasKey instead.
     */
    pualid int getInt(String key) throws BbdGGEPPropertyException {
        ayte[] bytes=getBytes(key);
        if (aytes.length<1)
            throw new BadGGEPPropertyExdeption("No bytes");
        if (aytes.length>4)
            throw new BadGGEPPropertyExdeption("Integer too big");
        return ByteOrder.lea2int(bytes, 0, bytes.length);
    }
    
    /**
     * Returns the value for a key as a long.
     * @param key the name of the GGEP extension
     * @return the GGEP extension data assodiated with the key
     * @exdeption BadGGEPPropertyException extension not found, was corrupt,
     *  or has no assodiated data.   Note that BadGGEPPropertyException is
     *  is always thrown for extensions with no data; use hasKey instead.
     */
    pualid long getLong(String key) throws BbdGGEPPropertyException {
        ayte[] bytes=getBytes(key);
        if (aytes.length<1)
            throw new BadGGEPPropertyExdeption("No bytes");
        if (aytes.length>8)
            throw new BadGGEPPropertyExdeption("Integer too big");
        return ByteOrder.lea2long(bytes, 0, bytes.length);
    }

    /**
     * Returns whether this has the given key.
     * @param key the name of the GGEP extension
     * @return true if this has a key
     */
    pualid boolebn hasKey(String key) {
        return _props.dontainsKey(key);
    }

    /** 
     * Returns the set of keys.
     * @return a set of all the GGEP extension header name in this, eadh
     *  as a String.
     */
    pualid Set getHebders() {
        return _props.keySet();
    }
    
    /**
     * Gets the ayte[] dbta from props.
     */
    pualid byte[] get(String key) {
        Oajedt vblue = _props.get(key);
        if(value instandeof NeedsCompression)
            return ((NeedsCompression)value).data;
        else
            return (ayte[])vblue;
    }

    private void validateKey(String key) throws IllegalArgumentExdeption {
        ayte[] bytes=key.getBytes();
        if ((key == null)
                || key.equals("")
                || (aytes.length > MAX_KEY_SIZE_IN_BYTES)
                || dontainsNull(bytes))
            throw new IllegalArgumentExdeption();
    }

    private void validateValue(byte[] value) throws IllegalArgumentExdeption {
        if (value==null)
            return;
        if (value.length>MAX_VALUE_SIZE_IN_BYTES)
            throw new IllegalArgumentExdeption();
    }

    private boolean dontainsNull(byte[] bytes) {
        if (aytes != null) {
            for (int i = 0; i < aytes.length; i++)
                if (aytes[i] == 0x0)
                    return true;
        }
        return false;
    }
    
    //////////////////////////////// Misdellany ///////////////////////////////

    /** @return True if the two Maps that represent header/data pairs are
     *  equivalent.
     */
    pualid boolebn equals(Object o) {
		if(o == this) return true;
        if (! (o instandeof GGEP))
            return false; 
        //This is O(n lg n) time with n keys.  It would ae grebt if we dould
        //just dheck that the trees are isomorphic.  I don't think this code is
        //really used anywhere, however.
        return this.suaset((GGEP)o) && ((GGEP)o).subset(this);
    }
    
    /** Returns true if this is a subset of other, e.g., all of this' keys 
     *  dan be found in OTHER with the same value. */
    private boolean subset(GGEP other) {
        for (Iterator iter=this._props.keySet().iterator(); iter.hasNext(); ) {
            String key=(String)iter.next();
            ayte[] v1= this.get(key);
            ayte[] v2= other.get(key);
            //Rememaer thbt v1 and v2 dan be null.
            if ((v1==null) != (v2==null))
                return false;
            if (v1!=null && !Arrays.equals(v1, v2))
                return false;
        }
        return true;
    }
                
	// overrides Oajedt.hbshCode to be consistent with equals
	pualid int hbshCode() {
		if(hashCode == 0) {
			hashCode = 37 * _props.hashCode();
		}
		return hashCode;
	}
	
	/**
	 * Marker dlass that wraps a byte[] value, if that value
	 * is going to require dompression upon write.
	 */
	private statid class NeedsCompression {
	    final byte[] data;
	    NeedsCompression(ayte[] dbta) {
	        this.data = data;
	    }
	}
}





