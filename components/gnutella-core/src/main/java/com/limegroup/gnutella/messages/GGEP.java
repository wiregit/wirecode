
// Commented for the Learning branch

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
 * A GGEP object represents a GGEP block in a Gnutella packet.
 * Make a new empty GGEP object and fill it with extensions to send a packet.
 * Or, use this class to parse data from a packet we've received into a new GGEP object.
 * You can change the names and values of this GGEP block once you've made it.
 * 
 * GGEP stands for the Gnutella Generic Extension Protocol.
 * It allows Gnutella programs to store additional information in Gnutella packets.
 * http://www.the-gdf.org/wiki/index.php?title=GGEP
 * 
 * A GGEP block looks like this:
 * 
 * 0xC3
 * #NAMEA#value
 * #NAMEB##valuevaluevalue
 * #NAMEC#
 * #NAMED###valuevaluevaluevaluevalue
 * 
 * All GGEP blocks start with the byte 0xC3.
 * After that are a number of GGEP extensions.
 * The order of the extensions doesn't matter.
 * Each extension has a header followed by a value.
 * A header looks like this:
 * 
 * #NAMED###
 * 
 * The first byte contains flags and the length of the header name.
 * Next is the header name, like "VC".
 * After that, 1, 2, or 3 bytes tell the length of this extension header's value.
 * A bit in the flag byte marks the last extension in the block.
 * 
 * A GGEP extension header name can't be more than 15 characters, and a value can't be 256 KB or more.
 * GGEP values can be deflate compressed and COBS encoded.
 * COBS encoding hides 0s in arbitrary data so it can be put somewhere where 0s aren't allowed.
 * 
 * The extensions that LimeWire uses have names in this class like GGEP_HEADER_BROWSE_HOST and GGEP_HEADER_DAILY_AVERAGE_UPTIME.
 * http://www.the-gdf.org/wiki/index.php?title=Known_GGEP_Extension_Blocks
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

    /**
     * 15, A key name like "VC" can't be more than 15 bytes.
     * 4 bits in the GGEP header express its length.
     * 2 ^ 4 - 1 = 15.
     */
    public static final int MAX_KEY_SIZE_IN_BYTES = 15;

    /**
     * 262143, A value can't be more than 262143 bytes long.
     * In a GGEP header, 18 bits in 3 bytes express its length.
     * 2 ^ 18 - 1 = 262143.
     * A 256 KB value is too long.
     */
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
     * Parse the data of a GGEP block into a new GGEP object.
     * If you're not sure if there is one GGEP block, use the read() method. (do)
     * 
     * @param  data                  The data of a Gnutella packet that contains a GGEP block
     * @param  offset                The index where the GGEP block starts with its 0xC3 byte
     * @throws BadGGEPBlockException The block isn't formed correctly for parsing
     */
    public GGEP(byte[] data, int offset) throws BadGGEPBlockException {

        // Call the next constructor
        this(data, offset, null); // We don't need to know how big the GGEP block is
    }

    /**
     * Parse the data of a GGEP block into a new GGEP object.
     * If you're not sure if there is one GGEP block, use the read() method. (do)
     * 
     * @param  messageBytes          The data of a Gnutella packet that contains a GGEP block
     * @param  beginOffset           The index where the GGEP block starts with its 0xC3 byte
     * @param  endOffset             This constructor will write the index beyond the GGEP block in endOffset[0], pass null to not do this
     * @throws BadGGEPBlockException The block isn't formed correctly for parsing
     */
    public GGEP(byte[] messageBytes, final int beginOffset, int[] endOffset) throws BadGGEPBlockException {

        /*
         * A GGEP block looks like this:
         * 
         * 0xC3
         * #VC###value
         * #IPP###valuevaluevalue
         * #PHC###valuevaluevalue
         * 
         * It starts with the 0xC3 byte.
         * After that are any number of GGEP extensions.
         * Each extension has a header followed by a value.
         * A header looks like this:
         * 
         * #VC###
         * 
         * The first byte contains flags and the length of the header name.
         * Next is the header name, like "VC".
         * After that, 1, 2, or 3 bytes tell the length of this extension header's value.
         */

        // Make sure the given byte array is at least 4 bytes
        if (messageBytes.length < 4) throw new BadGGEPBlockException();

        // All GGEP blocks begin with the byte 0xC3
        if (messageBytes[beginOffset] != GGEP_PREFIX_MAGIC_NUMBER) throw new BadGGEPBlockException();

        // When we find the header marked as the last one, we'll set onLastExtension to true
        boolean onLastExtension = false;

        // Start the index in the GGEP block just beyond the 0xC3 byte
        int currIndex = beginOffset + 1;

        // Loop until we've read the last extension in the block
        while (!onLastExtension) {

            // In the header's first byte, the 4th bit must be a 0
            try { sanityCheck(messageBytes[currIndex]); } catch (ArrayIndexOutOfBoundsException malformedInput) { throw new BadGGEPBlockException(); }

            // If the 8th bit in the header's first byte is 1, this is the last extension in this GGEP block
            onLastExtension = isLastExtension(messageBytes[currIndex]);

            // Look at the 7th and 6th bits to see if the value of this extension is COBS encoded and compressed
            boolean encoded = isEncoded(messageBytes[currIndex]);
            boolean compressed = isCompressed(messageBytes[currIndex]);

            // Read the length of the header, like 3 for "SCP" from the first byte
            int headerLen = deriveHeaderLength(messageBytes[currIndex]);

            // Read the extension header, like "SCP"
            currIndex++; // Move past the flags byte, from "#VC###" to "VC###", the header name
            String extensionHeader = null;
            try {

                // From the messageBytes byte array, at currIndex, clip out the ASCII characters to get the header name like "VC"
                extensionHeader = new String(messageBytes, currIndex, headerLen);

            // We couldn't read that far
            } catch (StringIndexOutOfBoundsException inputIsMalformed) { throw new BadGGEPBlockException(); }

            // Find out how long this extension's value is
            currIndex += headerLen; // Move past the header name, from "VC###" to "###", the length bytes

            // Read the length from the next 1, 2, or 3 bytes
            int[] toIncrement = new int[1]; // deriveDataLength will tell us how many bytes it used by setting the value of toIncrement[0]
            final int dataLength = deriveDataLength(messageBytes, currIndex, toIncrement);
            byte[] extensionData = null;    // A byte array for the value we'll read next
            currIndex += toIncrement[0];    // Move past the length bytes, beyond "###", to the start of the value

            // This extension has a value for us to read
            if (dataLength > 0) {

                // Make a new byte array of the required length
                byte[] data = new byte[dataLength];
                try {

                    // Copy in the data
                    System.arraycopy(messageBytes, currIndex, data, 0, dataLength);

                } catch (ArrayIndexOutOfBoundsException malformedInput) { throw new BadGGEPBlockException(); }

                // The header indicated that this data is COBS encoded
                if (encoded) {

                    try {

                        // Decode it
                        data = COBSUtil.cobsDecode(data);

                    } catch (IOException badCobsEncoding) { throw new BadGGEPBlockException("Bad COBS Encoding"); }
                }

                // The header indicated that this data is deflate compressed
                if (compressed) {

                    try {

                        // Decompress it
                        data = IOUtils.inflate(data);

                    } catch (IOException badData) { throw new BadGGEPBlockException("Bad compressed data"); }
                }

                // Save the data, and move past it
                extensionData = data;
                currIndex += dataLength; // Move from the start of this extension's data to the start of the next extension header
            }

            // Add the extension name and value we just read to this GGEP object's _props TreeMap list of them
            if (compressed) _props.put(extensionHeader, new NeedsCompression(extensionData)); // If we decompressed the data, wrap it in a NeedsCompression object to note that
            else            _props.put(extensionHeader, extensionData);
        }

        // If the caller wants to know where the GGEP block ends, write it in endOffset[0]
        if ((endOffset != null) && (endOffset.length > 0)) endOffset[0] = currIndex; // The index in messageBytes after the last extension's value
    }

    /**
     * Copy all the extensions from a given GGEP object into this one.
     * If this one already has a value for an extension, it will get overwritten.
     * 
     * @param other The GGEP object to grab all the extensions from
     */
    public void merge(GGEP other) {

        // Merge in the elements of the _props TreeMap
        _props.putAll(other._props);
    }

    /**
     * In the header's first byte, the 4th bit must be a 0.
     * 
     * @param  headerFlags           The header's first byte
     * @throws BadGGEPBlockException The 4th bit in the header's first byte is a 1
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
     * Loops through all the headers in _props.
     * For each one, writes the name like "#SCP###" and value data.
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

                // Variable for the length of this header's data value
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

                    /*
                     * TODO:kfaaborg currData changed when we compressed it, it might have gained a 0 byte
                     */

                    // COBS encode the compressed data so it doesn't contain any 0 bytes
                    if (shouldEncode) currData = COBSUtil.cobsEncode(currData);
                    dataLen = currData.length; // Get the compressed length
                }

                // Write the data of the header, like "#SCP###"
                writeHeader(
                    currHeader,         // The header name, like "SCP"
                    dataLen,            // The length of the value that will come after this header
                    !headers.hasNext(), // True if this is the last header in the GGEP block
                    out,                // The object we can call out.write(b) on to write data
                    shouldEncode,       // True if the value will be COBS encoded
                    shouldCompress);    // True if the value will be deflate compressed

                // If this header has data, write it
                if (dataLen > 0) out.write(currData); // The length dataLen was written in the header above
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

    /**
     * Write the data of a GGEP extension header, like "SCP".
     * dataLen is the length of the value after this header.
     * This method just writes the header, not the header's value.
     * 
     * A GGEP header looks like this:
     * 
     * #SCP###
     * 
     * The text in the middle is the name of the tag, like "VC" or "SCP".
     * It's ASCII text that isn't null terminated.
     * 
     * The first byte contains flags and the length of the tag name.
     * The first 3 bits are 1 to mark this as the last tag in the GGEP block, 1 to indicate the value after is COBS encoded, and 1 to indicate it's compressed.
     * The 5 bits after that have the length of the tag nam, like 3 for "SCP".
     * 
     * After the tag name, there are 1, 2, or 3 bytes that hold the length of the value for this tag.
     * Each length byte starts with 2 flag bits, and then has 6 to hold the length.
     * The length looks like this:
     * 
     * 10------ 10------ 01------
     * 
     * The bits that hold the length are shown as hyphens.
     * The first bit tells if there is another after it.
     * The first two bytes above start with 1s, and the last one starts with a 0.
     * writeHeader() sets the second bit in the last byte. (do)
     * 
     * @param header       The header name, like "VC"
     * @param dataLen      The length of this header's value
     * @param isLast       True if this is the last header in the GGEP block, false if there is another one after it
     * @param out          The OutputStream we'll call out.write(b) on to write the serialized data
     * @param isEncoded    True if the data in the value will be COBS encoded
     * @param isCompressed True if the data in the value will be deflate compressed
     */
    private void writeHeader(String header, final int dataLen, boolean isLast, OutputStream out, boolean isEncoded, boolean isCompressed) throws IOException {

        /*
         * 1. WRITE THE HEADER FLAGS
         * in the future, when we actually encode and compress, this code should
         * still work.  well, the code that deals with the header flags, that
         * is, you'll still need to encode/compress
         */

        // Not used
        boolean shouldCompress = false;

        // Prepare and write the flags byte
        int flags = 0x00;
        if (isLast)       flags |= 0x80;   // Set the bit 1000 0000 to mark this as the last header in the GGEP block
        if (isEncoded)    flags |= 0x40;   // Set the bit 0100 0000 if the value of this header will be COBS encoded
        if (isCompressed) flags |= 0x20;   // Set the bit 0010 0000 if the value of this header will be compressed
        flags |= header.getBytes().length; // Put the length of the header text in the low 4 bits we didn't touch above
        out.write(flags);

        // Write the text of the header
        out.write(header.getBytes()); // If String header is "SCP", writes those characters as 3 ASCII bytes without a null terminator

        /*
         * Write the length of the value, which will take 1, 2, or 3 bytes.
         * The caller gave us the length in a 4 byte int named dataLen.
         * The code below uses the & operator to pull out 6 bit long sections.
         * Here are the masks:
         * 
         * 0x0003f000 00 03 f0 00 00000000 00000011 11110000 00000000 shift to the right 12
         * 0x00000fc0 00 00 0f c0 00000000 00000000 00001111 11000000 shift to the right 6
         * 0x0000003f 00 00 00 3f 00000000 00000000 00000000 00111111 no shifting necessary
         */

        // We'll keep the value of the byte we're going to write in the int toWrite
        int toWrite;

        // Clip out the 6 highest bits of the given length
        int begin = dataLen & 0x3F000;
        if (begin != 0) { // The length is big enough to have some 1s in those bits

            // Write the first length byte like 1011 1111
            begin = begin >> 12;    // Shift them to the right 12 bits to move them all the way down to 0011 1111
            toWrite = 0x80 | begin; // Add the bit at the start like 1011 1111 to indicate there's another length byte after this
            out.write(toWrite);     // Write the first length byte
        }

        // Clip out the next 6 highest bits from the given length
        int middle = dataLen & 0xFC0;
        if (middle != 0) { // The length is big enough to have some 1s here

            // TODO:kfaaborg What if the number is big enough for begin to be nonzero, but just happens to have all 0s in middle?

            // Write the second length byte like 1011 1111
            middle = middle >> 6;    // Shift them to the right 6 bits to move them all the way down to 0011 1111
            toWrite = 0x80 | middle; // Add the bit at the start like 1011 1111 to indicate there's another length byte after this
            out.write(toWrite);      // Write the first length byte
        }

        // Clip out the lowest 6 bits from the given length
        int end = dataLen & 0x3F; // Clip out the lowest 6 bits from the given length, like 0011 1111
        toWrite = 0x40 | end;     // Make the bit right before that 1, like 0111 1111, leaving the first bit 0 (do)
        out.write(toWrite);       // Write the last or only length byte
    }

    /*
     * ////////////////////////// Key/Value Mutators and Accessors ////////////////
     */

    /**
     * Add a list of GGEP extension headers and their values.
     * 
     * @param fields A List of NameValue objects with names like "SCP" and values that are byte arrays, strings, or numbers
     */
    public void putAll(List fields) throws IllegalArgumentException {

        /*
         * TODO: Allow a value to be compressed.
         */

        // Loop for each NameValue object in the given list
        for (Iterator i = fields.iterator(); i.hasNext(); ) {
            NameValue next = (NameValue)i.next();

            // Get the key like "VC" and value, which can be one of a number of different types of objects
            String key = next.getName();
            Object value = next.getValue();

            // Call the right put() method for the kind of object value is
            if      (value == null)            put(key);                              // No value, just add the header
            else if (value instanceof byte[])  put(key, (byte[])value);               // Add the header with the given byte array value
            else if (value instanceof String)  put(key, (String)value);               // Add the header with the given String value
            else if (value instanceof Integer) put(key, ((Integer)value).intValue()); // Add the header with the given int value
            else if (value instanceof Long)    put(key, ((Long)value).longValue());   // Add the header with the given long value
            else throw new IllegalArgumentException("Unknown value: " + value);       // No other kinds of objects are allowed
        }
    }

    /**
     * Add a header name with a data value that needs to be compressed in the GGEP block.
     * 
     * @param key   A GGEP header name like "VC"
     * @param value A byte array with the data that we should compress in the GGEP block
     */
    public void putCompressed(String key, byte[] value) throws IllegalArgumentException {

        // Make sure the header name isn't blank
        validateKey(key);

        /*
         * validateValue(value); // done when writing. TODO: do here?
         */

        // Wrap the byte array value in a NeedsCompression object, and put the key and it in the _props list
        _props.put(key, new NeedsCompression(value));
    }

    /**
     * Add a GGEP header name that has some data as its value.
     * 
     * @param key   A GGEP header name, like "VC"
     * @param value A byte array with the data value
     */
    public void put(String key, byte[] value) throws IllegalArgumentException {

        // Make sure the header name isn't blank and the value isn't too long, and add them to the _props list
        validateKey(key);
        validateValue(value);
        _props.put(key, value);
    }

    /**
     * Add a GGEP header name that has a text value.
     * 
     * @param key   A GGEP header name, like "VC"
     * @param value A String value for that header
     */
    public void put(String key, String value) throws IllegalArgumentException {

        // Add the header name and value to the _props list
        put(key, value == null ? null : value.getBytes()); // Pass null or the ASCII bytes of the String
    }

    /**
     * Add a GGEP header name with a number value.
     * Expresses the number in the fewest bytes necessary in little endian order.
     * 
     * @param key   A GGEP header name, like "VC"
     * @param value The number value
     */
    public void put(String key, int value) throws IllegalArgumentException {

        // Negative values aren't allowed
        if (value < 0) throw new IllegalArgumentException("Negative value");

        // Add the header name and value data to the _props list
        put(key, ByteOrder.int2minLeb(value)); // Turn the number into a byte array of the necessary length
    }

    /**
     * Add a GGEP header name with a number value.
     * Expresses the number in the fewest bytes necessary in little endian order.
     * 
     * @param key   A GGEP header name, like "VC"
     * @param value The number value
     */
    public void put(String key, long value) throws IllegalArgumentException {

        // Negative values aren't allowed
        if (value < 0) throw new IllegalArgumentException("Negative value");

        // Add the header name and value data to the _props list
        put(key, ByteOrder.long2minLeb(value)); // Turn the number into a byte array of the necessary length
    }

    /**
     * Add a GGEP header name that doesn't have a value.
     * 
     * @param key A GGEP header name, like "VC"
     */
    public void put(String key) throws IllegalArgumentException {

        // Add the header name, passing null instead of a byte array
        put(key, (byte[])null);
    }

    /**
     * Get a GGEP header's data value.
     * 
     * @param  key                      A GGEP header name, like "VC"
     * @return                          The header's value in a byte array
     * @throws BadGGEPPropertyException The given extension was not found, or has no data
     */
    public byte[] getBytes(String key) throws BadGGEPPropertyException {

        // Look up the byte array value of the given key in the _props TreeMap, and return it
        byte[] ret = get(key);
        if (ret == null) throw new BadGGEPPropertyException(); // Not found
        return ret;
    }

    /**
     * Get a GGEP header's text value.
     * 
     * @param  key                      A GGEP header name, like "VC"
     * @return                          The header's value in a String
     * @throws BadGGEPPropertyException The given extension was not found, or has no data
     */
    public String getString(String key) throws BadGGEPPropertyException {

        // Get the byte array value, and turn it into a String
        return new String(getBytes(key));
    }

    /**
     * Get a GGEP header's int value.
     * 
     * @param  key                      A GGEP header name, like "VC"
     * @return                          The header's value in an int
     * @throws BadGGEPPropertyException The given extension was not found, or has no data
     */
    public int getInt(String key) throws BadGGEPPropertyException {

        // Get the value of the given key name as a byte array
        byte[] bytes = getBytes(key);

        // Make sure it exists and isn't too big for an int
        if (bytes.length < 1) throw new BadGGEPPropertyException("No bytes");
        if (bytes.length > 4) throw new BadGGEPPropertyException("Integer too big");

        // Convert it into an int, and return it
        return ByteOrder.leb2int(bytes, 0, bytes.length);
    }

    /**
     * Get a GGEP header's long number value.
     * 
     * @param  key                      A GGEP header name, like "VC"
     * @return                          The header's value in a long
     * @throws BadGGEPPropertyException The given extension was not found, or has no data
     */
    public long getLong(String key) throws BadGGEPPropertyException {

        // Get the value of the given key name as a byte array
        byte[] bytes = getBytes(key);

        // Make sure it exists and isn't too big for a long
        if (bytes.length < 1) throw new BadGGEPPropertyException("No bytes");
        if (bytes.length > 8) throw new BadGGEPPropertyException("Integer too big");

        // Convert it to a long, and return it
        return ByteOrder.leb2long(bytes, 0, bytes.length);
    }

    /**
     * Determine if this GGEP block has a given key.
     * 
     * @param key A GGEP header name, like "VC"
     * @return    True if this GGEP block has it, false if not found
     */
    public boolean hasKey(String key) {

        // Look for it in our _props TreeMap
        return _props.containsKey(key);
    }

    /**
     * Get a list of the header names this GGEP block has.
     * 
     * @return A Set of this GGEP block's header names, like "VC", "SCP", the set has String objects in it
     */
    public Set getHeaders() {

        // Return a list of all the GGEP headers, like "VC", "SCP", that this GGEP block has
        return _props.keySet(); // Returns a Set of all the keys in the _props TreeMap
    }

    /**
     * Get the data of a given GGEP header.
     * Looks in the _props TreeMap of GGEP headers and values.
     * 
     * @param key The name of a GGEP header, like "VC"
     * @return    The header's value data in a byte array
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

    /**
     * Make sure a GGEP header key name like "SCP" isn't null, blank, longer than 15 bytes, or contains a 0 byte.
     * 
     * @param  key                      A String like "SCP", a GGEP header name
     * @throws IllegalArgumentException The name is null, blank, longer than 15 characters, or contains a 0 byte
     */
    private void validateKey(String key) throws IllegalArgumentException {

        // Look at the String like "SCP" as a byte array of 3 ASCII characters
        byte[] bytes = key.getBytes();

        // Make sure the header name is valid for GGEP storage
        if
            ((key == null)                            // Make sure we were passed a String a not a null reference
            || key.equals("")                         // Make sure the String isn't blank
            || (bytes.length > MAX_KEY_SIZE_IN_BYTES) // Make sure the byte array isn't longer than 15 bytes
            || containsNull(bytes))                   // Make sure the byte array doesn't contain a 0 byte

            // If any of those checks failed, throw an exception
            throw new IllegalArgumentException();
    }

    /**
     * Make sure a GGEP header value isn't 256 KB or longer.
     * 
     * @param  value                    A byte array that contains the data of a GGEP header value
     * @throws IllegalArgumentException The given reference is null, or the byte array is 256 KB or longer
     */
    private void validateValue(byte[] value) throws IllegalArgumentException {

        // If there is no data for this extension header, leave without throwing an exception
        if (value == null) return;

        // Make sure the value isn't 256 KB or longer
        if (value.length > MAX_VALUE_SIZE_IN_BYTES) throw new IllegalArgumentException();
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

    /**
     * Determine if another GGEP object has the same keys and values as this one.
     * 
     * @param o Another object
     * @return  True if o is a GGEP object with exactly the same keys and values as we have
     */
    public boolean equals(Object o) {

        // If the caller gave us a reference to ourselves, yes, that's the same
		if (o == this) return true;

        // If o isn't even a GGEP object, no, different
        if (!(o instanceof GGEP)) return false;

        /*
         * This is O(n lg n) time with n keys.  It would be great if we could
         * just check that the trees are isomorphic.  I don't think this code is
         * really used anywhere, however.
         */

        // Use the subset() method twice to make sure that all the keys in both are in the other, and all the values match
        return this.subset((GGEP)o) && ((GGEP)o).subset(this);
    }

    /**
     * Determine if all the information in this GGEP block appears in another one.
     * Returns true if all of this GGEP block's keys can be found in other with the same values.
     * 
     * @param other Another GGEP block which may have all we do and more.
     * @return      True if the give GGEP block has all of our keys, and with all the same values.
     *              False if we have a key the given GGEP block is missing, or our values for a key aren't the same.
     */
    private boolean subset(GGEP other) {

        // Loop through the keys in this GGEP block
        for (Iterator iter = this._props.keySet().iterator(); iter.hasNext(); ) {
            String key = (String)iter.next();

            // Get the key's value
            byte[] v1 = this.get(key);

            // Look for the key and value in the other GGEP block
            byte[] v2 = other.get(key);

            // If the other GGEP block doesn't have this key, we're not a subset of it
            if ((v1 == null) != (v2 == null)) return false;

            // If the other GGEP block has a different value than we do for this key, we're not a subset of it
            if (v1 != null && !Arrays.equals(v1, v2)) return false;
        }

        // The given GGEP block has all of our keys, and with all the same values
        return true;
    }

    /**
     * The hash code of this GGEP object.
     * Overrides Object.hashCode to be consistent with equals().
     * 
     * @return An int that identifies the data in this object
     */
	public int hashCode() {

        // If we haven't computed and saved our hashCode yet
		if (hashCode == 0) {

            // Take the hash code of the TreeMap of GGEP header names and values, and multiply that by 37
			hashCode = 37 * _props.hashCode();
		}

        // Return the hash code we computed now, or earlier
		return hashCode;
	}

	/**
     * NeedsCompression is a class that has one member variable, a byte array.
     * The GGEP class keeps values that need compression in NeedsCompression objects.
     * Values that don't need compression are just byte arrays.
     * 
     * This class can't actually compress anything.
     * It just keeps data that needs to be compressed.
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
