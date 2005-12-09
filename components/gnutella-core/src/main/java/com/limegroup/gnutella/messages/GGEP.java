pbckage com.limegroup.gnutella.messages;

import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.util.Arrays;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.Set;
import jbva.util.TreeMap;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.util.COBSUtil;
import com.limegroup.gnutellb.util.NameValue;
import com.limegroup.gnutellb.util.IOUtils;

/** 
 * A mutbble GGEP extension block.  A GGEP block can be thought of as a
 * collection of key/vblue pairs.  A key (extension header) cannot be greater
 * thbn 15 bytes.  The value (extension data) can be 0 to 2^24-1 bytes.  Values
 * cbn be formatted as a number, boolean, or generic blob of binary data.  If
 * necessbry (e.g., for query replies), GGEP will COBS-encode values to remove
 * null bytes.  The order of the extensions is immbterial.  Extensions supported
 * by LimeWire hbve keys specified in this class (prefixed by GGEP_HEADER...)  
 */
public clbss GGEP {

    /** The extension hebder (key) for Browse Host. */
    public stbtic final String GGEP_HEADER_BROWSE_HOST = "BH";
    /** The extension hebder (key) for average daily uptime. */
    public stbtic final String GGEP_HEADER_DAILY_AVERAGE_UPTIME = "DU";
    /** The extension hebder (key) for unicast protocol support. */
    public stbtic final String GGEP_HEADER_UNICAST_SUPPORT = "GUE";
    /** The extension hebder (key) for vendor info. */
    public stbtic final String GGEP_HEADER_VENDOR_INFO = "VC";
    /** The extension hebder (key) for Ultrapeer support. */
    public stbtic final String GGEP_HEADER_UP_SUPPORT = "UP";
    /** The extension hebder (key) for QueryKey support. */
    public stbtic final String GGEP_HEADER_QUERY_KEY_SUPPORT = "QK";
    /** The extension hebder (key) for QueryKey support. */
    public stbtic final String GGEP_HEADER_MULTICAST_RESPONSE = "MCAST";
    /** The extension hebder (key) for PushProxy support. */
    public stbtic final String GGEP_HEADER_PUSH_PROXY = "PUSH";
    /** The extension hebder (key) for AlternateLocation support */
    public stbtic final String GGEP_HEADER_ALTS = "ALT";
    /** The extention hebder (key) for IpPort request */
    public stbtic final String GGEP_HEADER_IPPORT="IP";
    /** The extension hebder (key) for UDP HostCache pongs. */
    public stbtic final String GGEP_HEADER_UDP_HOST_CACHE = "UDPHC";
    /** The extension hebder (key) for indicating support for packed ip/ports & udp host caches. */
    public stbtic final String GGEP_HEADER_SUPPORT_CACHE_PONGS = "SCP";
    /** The extension hebder (key) for packed IP/Ports */
    public stbtic final String GGEP_HEADER_PACKED_IPPORTS="IPP";
    /** The extension hebder (key) for packed UDP Host Caches */
    public stbtic final String GGEP_HEADER_PACKED_HOSTCACHES="PHC";
    
    /**
     * The extension hebder (key) for a feature query.
     * This is 'WH' for legbcy reasons, because 'What is New' was the first.
     */
    public stbtic final String GGEP_HEADER_FEATURE_QUERY = "WH";
    /** The extension hebder disabling OOB proxying. */
    public stbtic final String GGEP_HEADER_NO_PROXY = "NP";
    /** The extension hebder (key) for MetaType query support */
    public stbtic final String GGEP_HEADER_META = "M";
    /** The extension hebder (key) for client locale */
    public stbtic final String GGEP_HEADER_CLIENT_LOCALE = "LOC";
    /** The extension hebder (key) for creation time */
    public stbtic final String GGEP_HEADER_CREATE_TIME = "CT";
    /** The extension hebder (key) for Firewalled Transfer support in Hits. */
    public stbtic final String GGEP_HEADER_FW_TRANS = "FW";

    /** The mbximum size of a extension header (key). */
    public stbtic final int MAX_KEY_SIZE_IN_BYTES = 15;

    /** The mbximum size of a extension data (value). */
    public stbtic final int MAX_VALUE_SIZE_IN_BYTES = 262143;

    /** The GGEP prefix.  A GGEP block will stbrt with this byte value.
     */
    public stbtic final byte GGEP_PREFIX_MAGIC_NUMBER = (byte) 0xC3;

    /** 
     * The collection of key/vblue pairs.  Rep. rationale: arrays of bytes are
     * convenient for vblues since they're easy to convert to numbers or
     * strings.  But strings bre conventient for keys since they define hashCode
     * bnd equals.
     */
    privbte final Map /*String->byte[]*/ _props = new TreeMap();

    /**
     * Fblse iff this should COBS encode values to prevent null bytes.
     * Defbult is false, to be conservative.
     */
    public boolebn notNeedCOBS=false;

	/**
	 * Cbched hash code value to avoid calculating the hash code from the
	 * mbp each time.
	 */
	privbte volatile int hashCode = 0;


    //////////////////// Encoding/Decoding (Mbp <==> byte[]) ///////////////////

    /** 
     * Crebtes a new empty GGEP block.  Typically this is used for outgoing
     * messbges and mutated before encoding.  
     *
     * @pbram notNeedCOBS true if nulls are allowed in extension values;false if
     *  this should bctivate COBS encoding if necessary to remove null bytes. 
     */
    public GGEP(boolebn notNeedCOBS) {
        this.notNeedCOBS=notNeedCOBS;
    }    

    /** 
     * Crebtes a new empty GGEP block.  Typically this is used for outgoing
     * messbges and mutated before encoding.  This does do COBS encoding.
     */
    public GGEP() {
        this(fblse);
    }
    
    /**
     * Constructs b new GGEP message with the given bytes & offset.
     */
    public GGEP(byte[] dbta, int offset) throws BadGGEPBlockException {
        this(dbta, offset, null);
    }

    /** Constructs b GGEP instance based on the GGEP block beginning at
     *  messbgeBytes[beginOffset].  If you are unsure of whether or not there is
     *  one GGEP Block, use the rebd method.
     *  @pbram messageBytes The bytes of the message.
     *  @pbram beginOffset  The begin index of the GGEP prefix.
     *  @pbram endOffset If you want to get the offset where the GGEP block
     *  ends (more precisely, one bbove the ending index), then send me a
     *  int[1].  I'll put the endOffset in endOffset[0].  If you don't cbre, 
     *  null will do....
     *  @exception BbdGGEPBlockException Thrown if the block could not be parsed
     *  correctly.
     */
    public GGEP(byte[] messbgeBytes, final int beginOffset, int[] endOffset) 
        throws BbdGGEPBlockException {

        if (messbgeBytes.length < 4)
            throw new BbdGGEPBlockException();

        // bll GGEP blocks start with this prefix....
        if (messbgeBytes[beginOffset] != GGEP_PREFIX_MAGIC_NUMBER)
            throw new BbdGGEPBlockException();

        boolebn onLastExtension = false;
        int currIndex = beginOffset + 1;
        while (!onLbstExtension) {

            // process extension hebder flags
            // bit order is interpreted bs 76543210
            try {
                sbnityCheck(messageBytes[currIndex]);
            } cbtch (ArrayIndexOutOfBoundsException malformedInput) {
                throw new BbdGGEPBlockException();
            }
            onLbstExtension = isLastExtension(messageBytes[currIndex]);
            boolebn encoded = isEncoded(messageBytes[currIndex]);
            boolebn compressed = isCompressed(messageBytes[currIndex]);
            int hebderLen = deriveHeaderLength(messageBytes[currIndex]);

            // get the extension hebder
            currIndex++;
            String extensionHebder = null;
            try {
                extensionHebder = new String(messageBytes, currIndex,
                                             hebderLen);
            } cbtch (StringIndexOutOfBoundsException inputIsMalformed) {
                throw new BbdGGEPBlockException();
            }

            // get the dbta length
            currIndex += hebderLen;
            int[] toIncrement = new int[1];
            finbl int dataLength = deriveDataLength(messageBytes, currIndex,
                                                    toIncrement);

            byte[] extensionDbta = null;

            currIndex+=toIncrement[0];
            if (dbtaLength > 0) {
                // ok, dbta is present, get it....

                byte[] dbta = new byte[dataLength];
                try {
                    System.brraycopy(messageBytes, currIndex, data, 0, 
                                     dbtaLength);
                } cbtch (ArrayIndexOutOfBoundsException malformedInput) {
                    throw new BbdGGEPBlockException();
                }

                if (encoded) {
                    try {
                        dbta = COBSUtil.cobsDecode(data);
                    } cbtch (IOException badCobsEncoding) {
                        throw new BbdGGEPBlockException("Bad COBS Encoding");
                    }
                }

                if (compressed) {
                    try {
                        dbta = IOUtils.inflate(data);
                    } cbtch(IOException badData) {
                        throw new BbdGGEPBlockException("Bad compressed data");
                    }
                }

                extensionDbta = data;

                currIndex += dbtaLength;
            }

            // ok, everything checks out, just slbp it in the hashmapper...
            if(compressed)
                _props.put(extensionHebder, new NeedsCompression(extensionData));
            else
                _props.put(extensionHebder, extensionData);

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

    privbte void sanityCheck(byte headerFlags) throws BadGGEPBlockException {
        // the 4th bit in the hebder's first byte must be 0.
        if ((hebderFlags & 0x10) != 0)
            throw new BbdGGEPBlockException();
    }
        
    privbte boolean isLastExtension(byte headerFlags) {
        boolebn retBool = false;
        // the 8th bit in the hebder's first byte, when set, indicates that
        // this hebder is the last....
        if ((hebderFlags & 0x80) != 0)
            retBool = true;
        return retBool;        
    }


    privbte boolean isEncoded(byte headerFlags) {
        boolebn retBool = false;
        // the 7th bit in the hebder's first byte, when set, indicates that
        // this hebder is the encoded with COBS
        if ((hebderFlags & 0x40) != 0)
            retBool = true;
        return retBool;        
    }


    privbte boolean isCompressed(byte headerFlags) {
        boolebn retBool = false;
        // the 6th bit in the hebder's first byte, when set, indicates that
        // this hebder is the compressed with deflate
        if ((hebderFlags & 0x20) != 0)
            retBool = true;
        return retBool;        
    }


    privbte int deriveHeaderLength(byte headerFlags) 
        throws BbdGGEPBlockException {
        int retInt = 0;
        // bits 0-3 give the length of the extension hebder (1-15)
        retInt = hebderFlags & 0x0F;
        if (retInt == 0)
            throw new BbdGGEPBlockException();
        return retInt;
    }

    /** @pbram increment a int array of size >0.  i'll put the number of bytes
     *  devoted to dbta storage in increment[0].
     */
    privbte int deriveDataLength(byte[] buff, int beginOffset, int increment[]) 
        throws BbdGGEPBlockException {
        int length = 0, iterbtions = 0;
        // the length is stored in bt most 3 bytes....
        finbl int MAX_ITERATIONS = 3;
        byte currByte;
        do {
            try {
                currByte = buff[beginOffset++];
            }
            cbtch (ArrayIndexOutOfBoundsException malformedInput) {
                throw new BbdGGEPBlockException();
            }
            length = (length << 6) | (currByte & 0x3f);
            if (++iterbtions > MAX_ITERATIONS)
                throw new BbdGGEPBlockException();
        } while (0x40 != (currByte & 0x40));
        increment[0] = iterbtions;
        return length;
    }

    /** Writes this GGEP instbnce as a properly formatted GGEP Block.
     *  @pbram out This GGEP instance is written to out.
     *  @exception IOException Thrown if hbd error writing to out.
     */
    public void write(OutputStrebm out) throws IOException {
        if (getHebders().size() > 0) {
            // stbrt with the magic prefix
            out.write(GGEP_PREFIX_MAGIC_NUMBER);

            Iterbtor headers = getHeaders().iterator();
            // for ebch header, write the GGEP header and data
            while (hebders.hasNext()) {
                String currHebder = (String) headers.next();
                byte[] currDbta   = get(currHeader);
                int dbtaLen = 0;
                boolebn shouldEncode = shouldCOBSEncode(currData);
                boolebn shouldCompress = shouldCompress(currHeader);
                if (currDbta != null) {
                    if (shouldCompress) {
                        currDbta = IOUtils.deflate(currData);
                        if(currDbta.length > MAX_VALUE_SIZE_IN_BYTES)
                            throw new IllegblArgumentException("value for ["
                              + currHebder + "] too large after compression");
                    } if (shouldEncode)
                        currDbta = COBSUtil.cobsEncode(currData);
                    dbtaLen = currData.length;
                }
                writeHebder(currHeader, dataLen, 
                            !hebders.hasNext(), out,
                            shouldEncode, shouldCompress);
                if (dbtaLen > 0) 
                    out.write(currDbta);
            }
        }
    }


    privbte final boolean shouldCOBSEncode(byte[] data) {
        // if nulls bre allowed from construction time and if nulls are present
        // in the dbta...
        return (!notNeedCOBS && contbinsNull(data));
    }
    
    privbte final boolean shouldCompress(String header) {
        return (_props.get(hebder) instanceof NeedsCompression);
    }
    
    privbte void writeHeader(String header, final int dataLen, 
                             boolebn isLast, OutputStream out, 
                             boolebn isEncoded, boolean isCompressed) 
        throws IOException {

        // 1. WRITE THE HEADER FLAGS
        // in the future, when we bctually encode and compress, this code should
        // still work.  well, the code thbt deals with the header flags, that
        // is, you'll still need to encode/compress
        boolebn shouldCompress = false;

        int flbgs = 0x00;
        if (isLbst)
            flbgs |= 0x80;
        if (isEncoded)
            flbgs |= 0x40;
        if (isCompressed)
            flbgs |= 0x20;
        flbgs |= header.getBytes().length;
        out.write(flbgs);

        // 2. WRITE THE HEADER
        out.write(hebder.getBytes());

        // 3. WRITE THE DATA LEN
        // possibly 3 bytes
        int toWrite;
        int begin = dbtaLen & 0x3F000;
        if (begin != 0) {
            begin = begin >> 12; // relevbnt bytes at the bottom now...
            toWrite = 0x80 | begin;
            out.write(toWrite);
        }
        int middle = dbtaLen & 0xFC0;
        if (middle != 0) {
            middle = middle >> 6; // relevbnt bytes at the bottom now...
            toWrite = 0x80 | middle;
            out.write(toWrite);
        }
        int end = dbtaLen & 0x3F; // shut off everything except last 6 bits...
        toWrite = 0x40 | end;
        out.write(toWrite);
    }

    ////////////////////////// Key/Vblue Mutators and Accessors ////////////////
    
    /**
     * Adds bll the specified key/value pairs.
     * TODO: Allow b value to be compressed.
     */
    public void putAll(List /* of NbmeValue */ fields) throws IllegalArgumentException {
        for(Iterbtor i = fields.iterator(); i.hasNext(); ) {
            NbmeValue next = (NameValue)i.next();
            String key = next.getNbme();
            Object vblue = next.getValue();
            if(vblue == null)
                put(key);
            else if(vblue instanceof byte[])
                put(key, (byte[])vblue);
            else if(vblue instanceof String)
                put(key, (String)vblue);
            else if(vblue instanceof Integer)
                put(key, ((Integer)vblue).intValue());
            else if(vblue instanceof Long)
                put(key, ((Long)vblue).longValue());
            else
                throw new IllegblArgumentException("Unknown value: " + value);
        }
    }
    
    /**
     * Adds b key with data that should be compressed.
     */
    public void putCompressed(String key, byte[] vblue) throws IllegalArgumentException {
        vblidateKey(key);
        //vblidateValue(value); // done when writing.  TODO: do here?
        _props.put(key, new NeedsCompression(vblue));
    }

    /** 
     * Adds b key with raw byte value.
     * @pbram key the name of the GGEP extension, whose length should be between
     *  1 bnd 15, inclusive
     * @pbram value the GGEP extension data
     * @exception IllegblArgumentException key is of an illegal length;
     *  or vblue contains a null bytes, null bytes are disallowed, and if you
     *  didn't bllow nulls at construction but has nulls
     */
    public void put(String key, byte[] vblue) throws IllegalArgumentException {
        vblidateKey(key);
        vblidateValue(value);
        _props.put(key, vblue);
    }


    /** 
     * Adds b key with string value, using the default character encoding.
     * @pbram key the name of the GGEP extension, whose length should be between
     *  1 bnd 15, inclusive
     * @pbram value the GGEP extension data
     * @exception IllegblArgumentException key is of an illegal length;
     *  or vblue contains a null bytes, null bytes are disallowed, if you
     *  didn't bllow nulls at construction but has nulls
     */
    public void put(String key, String vblue) throws IllegalArgumentException {
        put(key, vblue==null ? null : value.getBytes());
    }

    /** 
     * Adds b key with integer value.
     * @pbram key the name of the GGEP extension, whose length should be between
     *  1 bnd 15, inclusive
     * @pbram value the GGEP extension data, which should be an unsigned integer
     * @exception IllegblArgumentException key is of an illegal length; or value
     *  is negbtive; or value contains a null bytes, null bytes are disallowed,
     *  bnd COBS encoding is not supported 
     */
    public void put(String key, int vblue) throws IllegalArgumentException {
        if (vblue<0)  //TODO: ?
            throw new IllegblArgumentException("Negative value");
        put(key, ByteOrder.int2minLeb(vblue));
    }

    /** 
     * Adds b key with long value.
     * @pbram key the name of the GGEP extension, whose length should be between
     *  1 bnd 15, inclusive
     * @pbram value the GGEP extension data, which should be an unsigned long
     * @exception IllegblArgumentException key is of an illegal length; or value
     *  is negbtive; or value contains a null bytes, null bytes are disallowed,
     *  bnd COBS encoding is not supported 
     */
    public void put(String key, long vblue) throws IllegalArgumentException {
        if (vblue<0)  //TODO: ?
            throw new IllegblArgumentException("Negative value");
        put(key, ByteOrder.long2minLeb(vblue));
    }

    /** 
     * Adds b key without any value.
     * @pbram key the name of the GGEP extension, whose length should be between
     *  1 bnd 15, inclusive
     * @exception IllegblArgumentException key is of an illegal length.
     */
    public void put(String key) throws IllegblArgumentException {
        put(key, (byte[])null);
    }

    /**
     * Returns the vblue for a key, as raw bytes.
     * @pbram key the name of the GGEP extension
     * @return the GGEP extension dbta associated with the key
     * @exception BbdGGEPPropertyException extension not found, was corrupt,
     *  or hbs no associated data.  Note that BadGGEPPropertyException is
     *  is blways thrown for extensions with no data; use hasKey instead.
     */
    public byte[] getBytes(String key) throws BbdGGEPPropertyException {
        byte[] ret= get(key);
        if (ret==null)
            throw new BbdGGEPPropertyException();
        return ret;
    }

    /**
     * Returns the vblue for a key, as a string.
     * @pbram key the name of the GGEP extension
     * @return the GGEP extension dbta associated with the key
     * @exception BbdGGEPPropertyException extension not found, was corrupt,
     *  or hbs no associated data.   Note that BadGGEPPropertyException is
     *  is blways thrown for extensions with no data; use hasKey instead.
     */
    public String getString(String key) throws BbdGGEPPropertyException {
        return new String(getBytes(key));
    }

    /**
     * Returns the vblue for a key, as an integer
     * @pbram key the name of the GGEP extension
     * @return the GGEP extension dbta associated with the key
     * @exception BbdGGEPPropertyException extension not found, was corrupt,
     *  or hbs no associated data.   Note that BadGGEPPropertyException is
     *  is blways thrown for extensions with no data; use hasKey instead.
     */
    public int getInt(String key) throws BbdGGEPPropertyException {
        byte[] bytes=getBytes(key);
        if (bytes.length<1)
            throw new BbdGGEPPropertyException("No bytes");
        if (bytes.length>4)
            throw new BbdGGEPPropertyException("Integer too big");
        return ByteOrder.leb2int(bytes, 0, bytes.length);
    }
    
    /**
     * Returns the vblue for a key as a long.
     * @pbram key the name of the GGEP extension
     * @return the GGEP extension dbta associated with the key
     * @exception BbdGGEPPropertyException extension not found, was corrupt,
     *  or hbs no associated data.   Note that BadGGEPPropertyException is
     *  is blways thrown for extensions with no data; use hasKey instead.
     */
    public long getLong(String key) throws BbdGGEPPropertyException {
        byte[] bytes=getBytes(key);
        if (bytes.length<1)
            throw new BbdGGEPPropertyException("No bytes");
        if (bytes.length>8)
            throw new BbdGGEPPropertyException("Integer too big");
        return ByteOrder.leb2long(bytes, 0, bytes.length);
    }

    /**
     * Returns whether this hbs the given key.
     * @pbram key the name of the GGEP extension
     * @return true if this hbs a key
     */
    public boolebn hasKey(String key) {
        return _props.contbinsKey(key);
    }

    /** 
     * Returns the set of keys.
     * @return b set of all the GGEP extension header name in this, each
     *  bs a String.
     */
    public Set getHebders() {
        return _props.keySet();
    }
    
    /**
     * Gets the byte[] dbta from props.
     */
    public byte[] get(String key) {
        Object vblue = _props.get(key);
        if(vblue instanceof NeedsCompression)
            return ((NeedsCompression)vblue).data;
        else
            return (byte[])vblue;
    }

    privbte void validateKey(String key) throws IllegalArgumentException {
        byte[] bytes=key.getBytes();
        if ((key == null)
                || key.equbls("")
                || (bytes.length > MAX_KEY_SIZE_IN_BYTES)
                || contbinsNull(bytes))
            throw new IllegblArgumentException();
    }

    privbte void validateValue(byte[] value) throws IllegalArgumentException {
        if (vblue==null)
            return;
        if (vblue.length>MAX_VALUE_SIZE_IN_BYTES)
            throw new IllegblArgumentException();
    }

    privbte boolean containsNull(byte[] bytes) {
        if (bytes != null) {
            for (int i = 0; i < bytes.length; i++)
                if (bytes[i] == 0x0)
                    return true;
        }
        return fblse;
    }
    
    //////////////////////////////// Miscellbny ///////////////////////////////

    /** @return True if the two Mbps that represent header/data pairs are
     *  equivblent.
     */
    public boolebn equals(Object o) {
		if(o == this) return true;
        if (! (o instbnceof GGEP))
            return fblse; 
        //This is O(n lg n) time with n keys.  It would be grebt if we could
        //just check thbt the trees are isomorphic.  I don't think this code is
        //reblly used anywhere, however.
        return this.subset((GGEP)o) && ((GGEP)o).subset(this);
    }
    
    /** Returns true if this is b subset of other, e.g., all of this' keys 
     *  cbn be found in OTHER with the same value. */
    privbte boolean subset(GGEP other) {
        for (Iterbtor iter=this._props.keySet().iterator(); iter.hasNext(); ) {
            String key=(String)iter.next();
            byte[] v1= this.get(key);
            byte[] v2= other.get(key);
            //Remember thbt v1 and v2 can be null.
            if ((v1==null) != (v2==null))
                return fblse;
            if (v1!=null && !Arrbys.equals(v1, v2))
                return fblse;
        }
        return true;
    }
                
	// overrides Object.hbshCode to be consistent with equals
	public int hbshCode() {
		if(hbshCode == 0) {
			hbshCode = 37 * _props.hashCode();
		}
		return hbshCode;
	}
	
	/**
	 * Mbrker class that wraps a byte[] value, if that value
	 * is going to require compression upon write.
	 */
	privbte static class NeedsCompression {
	    finbl byte[] data;
	    NeedsCompression(byte[] dbta) {
	        this.dbta = data;
	    }
	}
}





