
// Edited for the Learning branch

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
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.util.COBSUtil;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.NameValue;
import com.limegroup.gnutella.util.IOUtils;

/** 
 * A mutable GGEP extension block.  A GGEP block can be thought of as a
 * collection of key/value pairs.  A key (extension header) cannot be greater
 * than 15 bytes.  The value (extension data) can be 0 to 2^24-1 bytes.  Values
 * can be formatted as a number, boolean, or generic blob of binary data.  If
 * necessary (e.g., for query replies), GGEP will COBS-encode values to remove
 * null bytes.  The order of the extensions is immaterial.  Extensions supported
 * by LimeWire have keys specified in this class (prefixed by GGEP_HEADER...)
 * 
 * 
 * 
 * 
 * 
 * 
 */
public class GGEP {

    /*
     * (do)
     * say what the values look like
     * or what the presence of a key means
     */

    /*
     * The GGEP headers that LimeWire uses.
     * The official list is on the GDF wiki:
     * http://www.the-gdf.org/wiki/index.php?title=Known_GGEP_Extension_Blocks
     */

    /**
     * "BH" Browse Host.
     * You can browse all the files this computer is sharing.
     * This header has no value.
     */
    public static final String GGEP_HEADER_BROWSE_HOST = "BH";

    /**
     * "DU" Daily Uptime.
     * The value is the number of seconds this computer is online on an average day.
     */
    public static final String GGEP_HEADER_DAILY_AVERAGE_UPTIME = "DU";

    /**
     * "GUE" GUESS.
     * This computer supports the GUESS protocol.
     * This header has no value.
     * LimeWire doesn't use GUESS anymore.
     */
    public static final String GGEP_HEADER_UNICAST_SUPPORT = "GUE";

    /**
     * "VC" Vendor Code.
     * The value is like "LIME#".
     * The first 4 bytes are the ASCII characters of the 4-character vendor code.
     * The 5th byte has the major and minor version number squashed into a single byte.
     * For version 4.9, the 4 is in the high 4 bits, and the 9 is in the low 4 bits.
     */
    public static final String GGEP_HEADER_VENDOR_INFO = "VC";

    /**
     * "UP" Ultrapeer.
     * This computer is an ultrapeer.
     * The value is 3 bytes.
     * The first byte is the version of the ultrapeer protocol the computer supports, 0.1, squashed into a single byte.
     * The second byte is the number of free leaf slots the computer has.
     * The third byte is the number of free ultrapeer slots the computer has.
     */
    public static final String GGEP_HEADER_UP_SUPPORT = "UP";

    /**
     * "QK" QueryKey.
     * QueryKey is part of GUESS, which is no longer in use.
     */
    public static final String GGEP_HEADER_QUERY_KEY_SUPPORT = "QK";

    /**
     * "MCAST" Multicast.
     * Present when this pong is responding directly to a multicast query. (do)
     */
    public static final String GGEP_HEADER_MULTICAST_RESPONSE = "MCAST";

    /**
     * "PUSH" Push proxies.
     * A list of the computer's push proxies. (do)
     */
    public static final String GGEP_HEADER_PUSH_PROXY = "PUSH";

    /**
     * "ALT" Alternate Locations.
     * A list of IP addresses and port numbers of computers on the Internet running Gnutella software.
     * The value is an array of 6 byte chunks.
     * Each chunk is a 4 byte IP address followed by a 2 byte port number.
     */
    public static final String GGEP_HEADER_ALTS = "ALT";

    /**
     * "IP" IP address request.
     * The value is 6 bytes, a 4 byte IP address followed by a 2 byte port number.
     * If you send a ping to a remote computer, it will reply with a pong with this tag.
     * The value is your external IP address, as the remote computer sees it.
     */
    public static final String GGEP_HEADER_IPPORT = "IP";

    /**
     * "UDPHC" UDP host cache.
     * The presence of this header indicates this pong is about a UDP host cache.
     * The value is optional.
     * It's a string like "www.site.com" or "71.240.19.76", the domain name or IP address of the UDP host cache.
     */
    public static final String GGEP_HEADER_UDP_HOST_CACHE = "UDPHC";

    /**
     * "SCP" Supports Cached Pongs.
     * Put SCP in a ping to get a pong back with IPP, a list of IP addresses to try to connect to.
     * Optionally include data in SCP to optimize how the ponger responds.
     * You can describe whether you want hosts with free leaf slots or hosts with free ultrapeer slots.
     * The data is one byte, and we only look at the lowest bit.
     * If it's 1, we want to know about computers with free ultrapeer slots.
     * If it's 0, we want to know about computers with free leaf slots.
     */
    public static final String GGEP_HEADER_SUPPORT_CACHE_PONGS = "SCP";

    /**
     * "IPP" IP addresses and port numbers.
     * For pong packets.
     * The value is an array of 6 byte chunks.
     * Each 6 byte chunk is a 4 byte IP address and 2 byte port number.
     * This is pong equivalent of the "X-Try-Ultrapeers" handshake header.
     */
    public static final String GGEP_HEADER_PACKED_IPPORTS = "IPP";

    /**
     * "PHC" Packed UDP Host Caches.
     * The value lists the addresses of UDP host caches.
     * The value is text like "www.site.com\n71.240.19.76\nwww.site2.com".
     * There may be more information about each one, like "host.example.com:2244&name=value&name2=value2".
     * The value may be compressed using normal GGEP compression. (do)
     */
    public static final String GGEP_HEADER_PACKED_HOSTCACHES = "PHC";

    /**
     * "WH" What is new feature query.
     * The value is 1 byte that contains the number 1. (do)
     * LimeWire puts WH in the GGEP block of a query packet.
     * The remote comptuer that gets the query should reply with information about the 3 files it started sharing most recently.
     */
    public static final String GGEP_HEADER_FEATURE_QUERY = "WH";

    /**
     * "NP" No Proxy.
     * Used in a Query packet.
     * Marks a Query packet as one that shouldn't be proxied.
     */
    public static final String GGEP_HEADER_NO_PROXY = "NP";

    /**
     * "M" Meta.
     * Used in a Query packet.
     * The value is 1 byte with bit flags that show how the user has filtered his search by different kinds of media.
     * If the bit at 0x08 is set, the user is looking for video.
     */
    public static final String GGEP_HEADER_META = "M";

    /**
     * "LOC" Locale.
     * The language preference of the computer this pong describes.
     * The value is text like "en" for English.
     */
    public static final String GGEP_HEADER_CLIENT_LOCALE = "LOC";

    /**
     * "CT" Creation Time.
     * The time when the file this query hit packet describes was made.
     * The value is 1 to 7 bytes that contain the number of milliseconds since January 1970.
     */
    public static final String GGEP_HEADER_CREATE_TIME = "CT";

    /**
     * "FW" Firewalled transfer support in query hits. (do)
     */
    public static final String GGEP_HEADER_FW_TRANS = "FW";

    /** 15, A key name like "VC" can't be more than 15 bytes. */
    public static final int MAX_KEY_SIZE_IN_BYTES = 15;

    /** 262143, A value can't be more than 262143 bytes long, which is 256 KB - 1 byte. */
    public static final int MAX_VALUE_SIZE_IN_BYTES = 262143;

    /** 0xC3. A GGEP block starts with a 0xC3 byte. */
    public static final byte GGEP_PREFIX_MAGIC_NUMBER = (byte)0xC3;

    /**
     * The list of tag names and their values in this GGEP block.
     * 
     * A TreeMap is a list that stays sorted.
     * The keys are strings like "VC" and the values are byte arrays like "LIME#".
     * 
     * Strings make good keys since they define hashCode() and equals().
     * Byte arrays make good values since they're easy to convert to numbers or strings.
     */
    private final Map _props = new TreeMap();

    /**
     * False if this GGEP block should COBS encode values.
     * False by default to be safe.
     */
    public boolean notNeedCOBS = false;

	/**
     * The hash code of the _props TreeMap.
     * We save the hash code here to avoid having to calculate it from the map each time.
	 */
	private volatile int hashCode = 0;

    /*
     * //////////////////// Encoding/Decoding (Map <==> byte[]) ///////////////////
     */

    //done

    /**
     * Make a new empty GGEP block.
     * To make a GGEP block for a packet we're going to send, use this constructor, and then add the keys and values.
     * 
     * @param notNeedCOBS True to allow 0 bytes in extension values.
     *                    False if 0 bytes aren't allowed, and all the values should be COBS encoded to remove them.
     */
    public GGEP(boolean notNeedCOBS) {

        // Save the given setting for COBS encoding
        this.notNeedCOBS = notNeedCOBS;
    }

    /**
     * Make a new empty GGEP block.
     * To make a GGEP block for a packet we're going to send, use this constructor, and then add the keys and values.
     * This GGEP block will use COBS encoding to remove all the 0 bytes from the values.
     */
    public GGEP() {

        // Call the constructor above, passing false for notNeedCOBS
        this(false);
    }

    
    
    /**
     * Constructs a new GGEP message with the given bytes & offset.
     * 
     * @param data
     * @param offset
     */
    public GGEP(byte[] data, int offset) throws BadGGEPBlockException {
        
        
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
     * 
     * 
     * @param messageBytes
     * @param beginOffset
     * @param endOffset
     * 
     */
    public GGEP(byte[] messageBytes, final int beginOffset, int[] endOffset) throws BadGGEPBlockException {

        // Make sure the given byte array is at least 4 bytes
        if (messageBytes.length < 4) throw new BadGGEPBlockException();

        // All GGEP blocks begin with the byte 0xC3
        if (messageBytes[beginOffset] != GGEP_PREFIX_MAGIC_NUMBER) throw new BadGGEPBlockException();

        boolean onLastExtension = false;
        
        // Start the index in the GGEP block just beyond the 0xC3 byte
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

                extensionHeader = new String(messageBytes, currIndex, headerLen);

            } catch (StringIndexOutOfBoundsException inputIsMalformed) {

                throw new BadGGEPBlockException();
            }

            // get the data length
            currIndex += headerLen;
            int[] toIncrement = new int[1];
            final int dataLength = deriveDataLength(messageBytes, currIndex, toIncrement);

            byte[] extensionData = null;

            currIndex += toIncrement[0];

            if (dataLength > 0) {

                // ok, data is present, get it....

                byte[] data = new byte[dataLength];

                try {

                    System.arraycopy(messageBytes, currIndex, data, 0, dataLength);

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
            if (compressed) _props.put(extensionHeader, new NeedsCompression(extensionData));
            else            _props.put(extensionHeader, extensionData);
        }

        if ((endOffset != null) && (endOffset.length > 0)) endOffset[0] = currIndex;
    }

    
    
    /**
     * Merges the other's GGEP with this' GGEP.
     */
    public void merge(GGEP other) {
        _props.putAll(other._props);
    }   

    /**
     * In the header's first byte, the 4th bit must be a 0.
     * 
     * @param  headerFlags           The header's first byte
     * @throws BadGGEPBlockException The 4th bit in the header's first byte is a 1.
     */
    private void sanityCheck(byte headerFlags) throws BadGGEPBlockException {

        // In the header's first byte, the 4th bit must be a 0, not a 1
        if ((headerFlags & 0x10) != 0) throw new BadGGEPBlockException();
    }

    /**
     * In the header's first byte, the 8th bit indicates if this is the last entry.
     * 0 means it's not the last one, 1 means it is the last one.
     * 
     * @param headerFlags The header's first byte.
     * @return            True if the 8th bit is 1, marking this as the last entry.
     *                    False if the 8th bit is 0, indicating there is another entry after this one.
     */
    private boolean isLastExtension(byte headerFlags) {

        /*
         * the 8th bit in the header's first byte, when set, indicates that
         * this header is the last....
         */

        // Return the 8th bit
        boolean retBool = false;
        if ((headerFlags & 0x80) != 0) retBool = true;
        return retBool;
    }

    /**
     * In the header's first byte, the 7th bit indicates this header's value is COBS encoded.
     * 0 means it's not COBS encoded, 1 means it is.
     * 
     * @param headerFlags The header's first byte.
     * @return            True if the 7th bit is 1, meaning the value is COBS encoded and we'll have to decode it.
     *                    False if the 7th bit is 0, meaning the value isn't COBS encoded and we don't need to worry about it.
     */
    private boolean isEncoded(byte headerFlags) {

        /*
         * the 7th bit in the header's first byte, when set, indicates that
         * this header is the encoded with COBS
         */

        // Return the 7th bit
        boolean retBool = false;
        if ((headerFlags & 0x40) != 0) retBool = true;
        return retBool;
    }

    /**
     * In the header's first byte, the 6th bit indicates the header's value is compressed.
     * 0 means it's not compressed, 1 means it is.
     * 
     * @param headerFlags The header's first byte.
     * @return            True if the 6th bit is 1, meaning the value is compressed and we'll have to decompress it.
     *                    False if the 6th bit is 0, meaning the value isn't compressed and we don't need to worry about it.
     */
    private boolean isCompressed(byte headerFlags) {

        /*
         * the 6th bit in the header's first byte, when set, indicates that
         * this header is the compressed with deflate
         */

        // Return the 6th bit
        boolean retBool = false;
        if ((headerFlags & 0x20) != 0) retBool = true;
        return retBool;
    }

    /**
     * In the header's first byte, the first 4 bits tell the length of the header.
     * The header can be 1 to 15 bytes long.
     * 
     * @param headerFlags            The header's first byte
     * @return                       The first 4 bits of the header as an int, the length of the header
     * @throws BadGGEPBlockException The length is 0
     */
    private int deriveHeaderLength(byte headerFlags) throws BadGGEPBlockException {

        /*
         * bits 0-3 give the length of the extension header (1-15)
         */

        // Read just the first 4 bits of the header's first byte
        int retInt = 0;
        retInt = headerFlags & 0x0F;

        // Make sure it's not 0, it has to be 1 through 15
        if (retInt == 0) throw new BadGGEPBlockException();

        // Return it
        return retInt;
    }

    /**
     * Read the length of a GGEP value from 1, 2, or 3 bytes.
     * 
     * @param increment a int array of size >0.  i'll put the number of bytes
     * devoted to data storage in increment[0].
     * 
     * @param buff        The data of the GGEP block
     * @param beginOffset The index where the 1, 2, or 3 bytes with the length are located
     * @param increment   This method sets increment[0] to the number of bytes we moved past to read the length
     * @return            The length
     */
    private int deriveDataLength(byte[] buff, int beginOffset, int increment[]) throws BadGGEPBlockException {

        // Make variables for the length we'll read and the number of bytes we'll look at to read it
        int length = 0, iterations = 0;

        // The length is stored in 1, 2, or 3 bytes, but never more
        final int MAX_ITERATIONS = 3;

        // The byte we're reading another part of the length from
        byte currByte;

        do {

            // Read the byte in buff at beginOffset, and then move beginOffset forward
            try { currByte = buff[beginOffset++]; } catch (ArrayIndexOutOfBoundsException malformedInput) { throw new BadGGEPBlockException(); }

            // Shift the number in length 6 bits higher, and copy in the low 6 bits from the byte we just read
            length = (length << 6) | (currByte & 0x3f); // 0x3F is the bit mask 0011 1111

            // Count this iteration, and make sure we didn't just read the 4th byte
            if (++iterations > MAX_ITERATIONS) throw new BadGGEPBlockException();

        // If the bit just before that, 0100 0000 is 0, move on to the next byte to read more of the length
        } while (0x40 != (currByte & 0x40));

        // Write the number of bytes we read into the start of the increment array, and return the length
        increment[0] = iterations;
        return length;
    }

    /**
     * Write out this GGEP block as data we can send in a Gnutella packet.
     * 
     * @param out The OutputStream object we can call out.write(data) on to send the data
     */
    public void write(OutputStream out) throws IOException {

        // Only write something if there is at least 1 GGEP name and value pair
        if (getHeaders().size() > 0) {

            // start with the magic prefix
            out.write(GGEP_PREFIX_MAGIC_NUMBER);

            /*
             * for each header, write the GGEP header and data
             */

            // Loop for each header
            Iterator headers = getHeaders().iterator();
            while (headers.hasNext()) {
                
                // Get the name and value of this header
                String currHeader = (String)headers.next();
                byte[] currData = get(currHeader); // If the value is a NeedsCompression object, gets the byte array inside it

                int dataLen = 0;

                // Determine if we need to COBS encode and compress this header's value
                boolean shouldEncode = shouldCOBSEncode(currData);   // True if this object allows COBS encoding and currData has a 0 byte
                boolean shouldCompress = shouldCompress(currHeader); // True if the value in _props for currHeader is a NeedsCompression object

                // This GGEP header has a value
                if (currData != null) {

                    // We need to compress it
                    if (shouldCompress) {

                        // Compress the value
                        currData = IOUtils.deflate(currData); // Returns a byte array with the given data compressed

                        // Make sure the data isn't 256 KB or longer
                        if (currData.length > MAX_VALUE_SIZE_IN_BYTES) throw new IllegalArgumentException("value for [" + currHeader + "] too large after compression");
                    }

                    // 
                    if (shouldEncode) currData = COBSUtil.cobsEncode(currData);
                    dataLen = currData.length;
                }

                writeHeader(currHeader, dataLen, !headers.hasNext(), out, shouldEncode, shouldCompress);

                if (dataLen > 0) out.write(currData);
            }
        }
    }

    /**
     * Determine if we should COBS encode a given GGEP value.
     * 
     * @param data The value of a GGEP header
     * @return     True if we told the constructor to use COBS encoding and this value contains a 0 byte that needs to get hidden
     */
    private final boolean shouldCOBSEncode(byte[] data) {

        /*
         * if nulls are allowed from construction time and if nulls are present
         * in the data...
         */

        // If we've configured this GGEP object to use COBS encoding and the given GGEP value contains a 0 byte, return true
        return (!notNeedCOBS && containsNull(data));
    }

    /**
     * Determine if we need to compress the value of a given GGEP header.
     * 
     * @param header A GGEP header, like "VC".
     * @return       True if the value is a NeedsCompression object, indicating that we should compress the value.
     *               False if the value is a byte array, indicating we can include it in the data of the GGEP block as it is.
     */
    private final boolean shouldCompress(String header) {

        // Look up the header like "VC" in the _props TreeMap of them, if the value is a NeedsCompression object, return true
        return (_props.get(header) instanceof NeedsCompression);
    }

    private void writeHeader(String header, final int dataLen, 
                             boolean isLast, OutputStream out, 
                             boolean isEncoded, boolean isCompressed) 
        throws IOException {

        // 1. WRITE THE HEADER FLAGS
        // in the future, when we actually encode and compress, this code should
        // still work.  well, the code that deals with the header flags, that
        // is, you'll still need to encode/compress
        boolean shouldCompress = false;

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

    ////////////////////////// Key/Value Mutators and Accessors ////////////////
    
    /**
     * Adds all the specified key/value pairs.
     * TODO: Allow a value to be compressed.
     */
    public void putAll(List /* of NameValue */ fields) throws IllegalArgumentException {
        for(Iterator i = fields.iterator(); i.hasNext(); ) {
            NameValue next = (NameValue)i.next();
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

        if (value < 0)  //TODO: ?
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
        byte[] ret = get(key);
        if (ret == null)
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
    public Set getHeaders() {
        return _props.keySet();
    }

    /**
     * Gets the byte[] data from props.
     * 
     * @param key The name of a GGEP header, like "VC"
     * @return    The byte array with that header's value data
     */
    public byte[] get(String key) {

        // Look up the header name like "VC" in the _props TreeMap of headers this GGEP object keeps
        Object value = _props.get(key);

        // The value is a NeedsCompression object that wraps the byte array
        if (value instanceof NeedsCompression) {

            // Return the byte array inside it
            return ((NeedsCompression)value).data;

        // The value is just a byte array
        } else {

            // Return it
            return (byte[])value;
        }
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

    /**
     * Look for a 0 byte in a given byte array.
     * 
     * The byte array is a GGEP value.
     * If it contains a 0 byte, we should use COBS encoding to hide it.
     * 
     * @param bytes An array of bytes.
     * @return      True if one of the bytes is 0.
     *              False if there are no 0s, or no byte array.
     */
    private boolean containsNull(byte[] bytes) {

        // If we weren't given a byte array, return 0
        if (bytes != null) {

            // Loop for each byte in the array
            for (int i = 0; i < bytes.length; i++) {

                // If it's 0, return true
                if (bytes[i] == 0x0) return true;
            }
        }

        // No 0 found
        return false;
    }

    /*
     * //////////////////////////////// Miscellany ///////////////////////////////
     */

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
        for (Iterator iter=this._props.keySet().iterator(); iter.hasNext(); ) {
            String key=(String)iter.next();
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
     * NeedsCompression is a class that has one member variable, a byte array.
     * The GGEP class keeps value that need compression in NeedsCompression objects.
     * Values that don't need compression are just byte arrays.
     * 
     * 
	 * Marker class that wraps a byte[] value, if that value
	 * is going to require compression upon write.
	 */
	private static class NeedsCompression {

        /** The GGEP value that we'll compress before we send. */
	    final byte[] data;

        /**
         * Make a new NeedsCompression object to hold a GGEP value that we should compress before sending.
         * 
         * @param data The GGEP value in a byte array
         */
        NeedsCompression(byte[] data) {

            // Save it
	        this.data = data;
	    }
	}
}
