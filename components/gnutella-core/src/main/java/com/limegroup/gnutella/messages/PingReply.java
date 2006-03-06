
// Commented for the Learning branch

package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.Collection;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.Statistics;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * A PingReply object represents a Gnutella pong packet.
 * 
 * A pong is the response to a ping.
 * A pong has information about a computer on the Internet running Gnutella software.
 * Each pong has information about one computer, but the GGEP "IPP" extension has more IP addresses to try.
 * Most pongs are about PCs like us, but a pong with the GGEP "UDPHC" extension is about a UDP host cache.
 * In the start of Gnutella, a computer would only send you a pong about itself.
 * Now, computers send groups of pongs about computers they have information about.
 * 
 * A pong has 3 parts:
 * The 23 byte Gnutella header.
 * A 14 byte payload.
 * A GGEP block.
 * 
 * The 14 byte pong payload looks like this:
 * At  0, length 2, the port number
 * At  2, length 4, the IP address
 * At  6, length 4, the number of files the computer is sharing
 * At 10, length 4, the total size in KB of the shared files
 * 
 * StandardMessageRouter.respondToPingRequest() makes a pong packet about us.
 * Here's what one looks like:
 * 
 * 74 73 28 3d 74 3a 8b f9  ts(=t:--  aaaaaaaa
 * c0 33 87 6f 76 39 e9 00  -3-ov9--  aaaaaaaa
 * 01 03 00 2d 00 00 00 e7  --------  bcdeeeef
 * 18 d8 1b 9e 4a 01 00 00  ----J---  fgggghhh
 * 00 00 20 00 00 c3 02 44  -------D  hiiiijkk
 * 55 42 35 07 03 4c 4f 43  UB5--LOC  kkkkllll
 * 43 65 6e 02 02 55 50 43  Cen--UPC  llllmmmm
 * 01 1c 1b 82 56 43 45 4c  ----VCEL  mmmnnnnn
 * 49 4d 45 49              IMEI      nnnn
 * 
 * a is the 16 byte message GUID from the ping.
 * b is 0x01, the byte code for a pong.
 * c is the TTL, here shown as 3.
 * d is the hops, 0.
 * 
 * e is the length of the payload, 0x2d, which is 45 bytes.
 * The length is 4 bytes in little endian order, like 2d 00 00 00.
 * 
 * f is the port number this computer is listening on right now, e7 18, little endian 0x18e7, port number 6375.
 * g is this computer's IP address, d8 1b 9e 4a, 216.27.158.74, with the bytes in the same order as the text.
 * h is the number of files this computer is sharing, 1, in little endian order 01 00 00 00.
 * 
 * i is total size in KB of shared data here, adjusted to the nearest power of 2.
 * The little endian 00 20 00 00 is 0x2000, 8192 KB, which is bigger than the one file I'm sharing.
 * 
 * j is 0xC3, the byte that begins a GGEP block, and the remaining lettered regions are GGEP extensions.
 * k is 0000 0010 "DU"  0100 0010 35 07.
 * l is 0000 0011 "LOC" 0100 0011 "en" 02.
 * m is 0000 0010 "UP"  0100 0011 01 1c 1b.
 * n is 1000 0010 "VC"  0100 0101 "LIME" 27.
 * 
 * The first byte in each extension contains flags and a length.
 * The first bit marks the last extension, and is set only in "VC".
 * The second bit is 0 because none of the extension values are COBS encoded.
 * We don't need to hide 0 values in a GGEP block in a pong.
 * The third bit is 0 because none of the extension values are deflate compressed.
 * The right 4 bits are the length of the extension name, like 0010 2 for "DU" and 0011 3 for "LOC".
 * 
 * The byte after the tag holds the length of the value.
 * The bytes all start 01 because the lengths all fit in 1 byte.
 * The remianing 6 bits hold the length, like 10 2 for "35 07" and "101" 5 for "LIME 27".
 * 
 * k is 0000 0010 "DU" 0100 0010 35 07, Daily Uptime.
 * The value is the number of seconds this computer is online in an average day.
 * The two bytes in the value 35 07 are little endian 0x00000735, 1845 seconds, a little over 30 minutes.
 * 
 * l is 0000 0011 "LOC" 0100 0011 "en" 02, Locale preference.
 * "en" is for English, and 2 is the number of additional ultrapeers I want that also prefer English.
 * 
 * m is 0000 0010 "UP" 0100 0011 01 1c 1b, Ultrapeer.
 * I'm sending this tag because LimeWire is running as an ultrapeer right now.
 * The value is 3 bytes, "01 1c 1b".
 * The first byte is the version of the ultrapeer protocol the computer supports, 0.1, squashed into a single byte.
 * The second byte is the number of free leaf slots I have, 28.
 * The third byte is the number of free ultrapeer slots I have, 27.
 * 
 * n is 1000 0010 "VC" 0100 0101 "LIME" 49, Vendor Code.
 * The first 4 bytes are "LIME" for LimeWire.
 * The last byte is 0x49 0100 1001 4 and 9 for LimeWire version 4.9.
 * 
 * Once we realize we're externally contactable for UDP, we'll also include the "GUE" extension.
 */
public class PingReply extends Message implements Serializable, IpPort {

    /**
     * The IP addresses and port numbers in the pong packet of computers running Gnutella software.
     * Read from the "IPP" GGEP header.
     * This is an ArrayList of IpPortCombo objects.
     */
    private final List PACKED_IP_PORTS;

    /**
     * The IP addresses and port numbers in the pong packet of UDP host caches.
     * Read from the "PHC" GGEP header.
     * This is a LinkedList of IpPortImpl objects.
     */
    private final List PACKED_UDP_HOST_CACHES;

    /**
     * If a UDP host cache send us this pong, UDP_CACHE_ADDRESS is its IP address.
     * UDP_CACHE_ADDRESS is null if the pong isn't from a UDP host cache.
     * Read from the "UDPHC" GGEP header.
     */
    private final String UDP_CACHE_ADDRESS;

    /** The number of free slots the ultrapeer pong has for ultrapeers, read from the "UP" GGEP header. */
    private final int FREE_ULTRAPEER_SLOTS;

    /** The number of free slots the ultrapeer pong has for leaves, read from the "UP" GGEP header. */
    private final int FREE_LEAF_SLOTS;

    /**
     * 14, a pong packet payload is 14 bytes.
     * The GGEP block comes after it.
     */
    public static final int STANDARD_PAYLOAD_SIZE = 14;

    /**
     * The data of the standard pong payload and the GGEP block.
     * This is what comes right after the Gnutella message header.
     * 
     * The standard pong payload is 14 bytes:
     * At  0, size 2 is the computer's port number.
     * At  2, size 4 is the computer's IP address.
     * At  6, size 4 is the number of files the computer is sharing.
     * At 10, size 4 is the total size in KB of shared data.
     */
    private final byte[] PAYLOAD;

    /**
     * The IP address the computer this pong is about is externally contactable at, read from the standard pong payload.
     * 
     * LOCKING: obtain this' monitor. (do)
     */
    private final InetAddress IP;

    /** The port number the computer this pong is about is listening on, read from the start of the standard pong payload. */
    private final int PORT;

    /**
     * The IP address the computer this pong is about is externally contactable at, read from the "IP" GGEP header.
     * The "IP" extension is used specifically when one computer pings another to see what its IP address looks like from the outside.
     */
    private final InetAddress MY_IP;

    /**
     * The port number the computer this pong is about is externally contactable at, read from the "IP" GGEP header.
     * The "IP" extension is used specifically when one computer pings another to see what its IP address looks like from the outside.
     */
    private final int MY_PORT;

    /** The number of files the computer this pong is about is sharing, read from the standard pong payload. */
    private final long FILES;

    /** The KBs of files the computer this pong is about is sharing, read from the standard pong payload. */
    private final long KILOBYTES;

    /** The number of seconds the pong says it's online on an average day, read from the "DU" GGEP header. */
    private final int DAILY_UPTIME;

    /**
     * True if the pong says it supports UDP, indicated by the presence of the "GUE" GGEP header.
     * Support for UDP makes GUESS possible, a kind of searching that LimeWire no longer uses.
     */
    private final boolean SUPPORTS_UNICAST;

    /** The pong's vendor code like "LIME", read from the "VC" GGEP header. */
    private final String VENDOR;

    /** The pong's major version number, like 4 if the version is 4.9, read from the "VC" GGEP header. */
    private final int VENDOR_MAJOR_VERSION;

    /** The pong's minor version number, like 9 if the version is 4.9, read from the "VC" GGEP header. */
    private final int VENDOR_MINOR_VERSION;

    /**
     * QueryKey is a part of GUESS, which LimeWire no longer uses.
     * The QueryKey object, read from the "QK" GGEP header.
     */
    private final QueryKey QUERY_KEY;

    /** True if this pong has a GGEP block, and we've parsed it. */
    private final boolean HAS_GGEP_EXTENSION;

    /**
     * An array of 5 bytes like "LIME#" where # is the version number, like 4.9, with the 4 in the high half and the 9 in the low half.
     * This is our vendor code in the GGEP format.
     * We'll use it as the value for the "VC" tag.
     */
    private static final byte[] CACHED_VENDOR = new byte[5];

    /*
     * performs any necessary static initialization of fields,
     * such as the vendor GGEP extension
     */

    // Code in a static box gets called when Java loads the class, probably when the program first runs
    static {

        // Copy the ASCII characters "LIME" into the first 4 bytes of the array
        System.arraycopy(CommonUtils.QHD_VENDOR_NAME.getBytes(), 0, CACHED_VENDOR, 0, CommonUtils.QHD_VENDOR_NAME.getBytes().length);

        /*
         * TODO:kfaaborg If we do a version like 4.16, the 16 won't fit in the lower 4 bits.
         */

        // In the byte after "LIME", set the version like 4.9 in one byte, with the 4 in the high half and the 9 in the low half
        CACHED_VENDOR[4] = convertToGUESSFormat(CommonUtils.getMajorVersionNumber(), CommonUtils.getMinorVersionNumber());
    }

    /** The pong's language preference like "en", read from the "LOC" GGEP header. */
    private String CLIENT_LOCALE;

    /** The number of slots the pong has for computers that match its language preference, read from the "LOC" GGEP header. */
    private int FREE_LOCALE_SLOTS;

    /**
     * Make a new PingReply object that represents a Gnutella pong packet filled with information about us.
     * 
     * @param guid  The GUID for this new packet
     * @param ttl   The TTL for the packet
     * @return      A new PingReply objet that represents a pong packet filled with information about us
     */
    public static PingReply create(byte[] guid, byte ttl) {

        // Call the next create() method, passing an empty list instead of a list of IpPort objects for the GGEP "IPP" header
        return create(guid, ttl, Collections.EMPTY_LIST);
    }

    /**
     * Make a new PingReply object that represents a Gnutella pong packet filled with information about us.
     * 
     * @param guid  The GUID for this new packet
     * @param ttl   The TTL for the packet
     * @param hosts A list of IpPort objects to put under "IPP" in this new packet's GGEP block
     * @return      A new PingReply objet that represents a pong packet filled with information about us
     */
    public static PingReply create(byte[] guid, byte ttl, Collection hosts) {

        // Make a new PingReply packet that represents a pong packet with information about us
        return create(

            // Information we'll put in the Gnutella packet header
            guid,                       // The GUID that marks this packet unique
            ttl,                        // The number of times this packet can travel from ultrapeer to ultrapeer
            RouterService.getPort(),    // The port number we're listening on
            RouterService.getAddress(), // Our external Internet IP address

            // Information we'll put in the pong payload
            (long)RouterService.getNumSharedFiles(),        // The number of files we're sharing
            (long)RouterService.getSharedFileSize() / 1024, // The total size of all the files we're sharing, in KB

            // Information we'll put in the GGEP block
            RouterService.isSupernode(),                  // Adds "UP" with information about how many slots we have, and shifts the shared file size to a power of 2
            Statistics.instance().calculateDailyUptime(), // Adds "DU" with the number of seconds we're online on an average day
            UDPService.instance().isGUESSCapable(),       // If we're externally contactable for UDP, adds "GUE" with 0.1 as the value

            // Add "LOC" with our language preference, and the number of slots we have for that language preference
            ApplicationSettings.LANGUAGE.getValue().equals("") ? ApplicationSettings.DEFAULT_LOCALE.getValue() : ApplicationSettings.LANGUAGE.getValue(),
            RouterService.getConnectionManager().getNumLimeWireLocalePrefSlots(),

            // Add "IPP" with each IpPort object in this list turned into 6 bytes in the value
            hosts);
    }

    /**
     * Make a new PingReply object that represents a Gnutella pong packet filled with information about us.
     * 
     * @param guid The GUID for this new packet
     * @param ttl  The TTL for the packet
     * @param addr Our IP address for the "IP" GGEP header
     * @return     A new PingReply objet that represents a pong packet filled with information about us
     */
    public static PingReply create(byte[] guid, byte ttl, IpPort addr) {

        // Call the next create() method, passing an empty list to not have an "IPP" GGEP extension
        return create(guid, ttl, addr, Collections.EMPTY_LIST);
    }

    /**
     * Make a new PingReply object that represents a Gnutella pong packet filled with information about us.
     * 
     * @param guid       The GUID for this new packet
     * @param ttl        The TTL for the packet
     * @param returnAddr The pinging computer's external IP address as seen by the computer prearing this pong response, the value of GGEP "IP"
     * @param hosts      Other computer's IP addresses for the "IPP" GGEP header, a list of IpPort objects
     * @return           A new PingReply objet that represents a pong packet filled with information about us
     */
    public static PingReply create(byte[] guid, byte ttl, IpPort returnAddr, Collection hosts) {

        // Make the GGEP block for this pong packet with DU, GUE, UP, and VC tags
        GGEP ggep = newGGEP(Statistics.instance().calculateDailyUptime(), RouterService.isSupernode(), UDPService.instance().isGUESSCapable());

        // Get our language preference, like "en" for English, and add the LOC tag
        String locale = ApplicationSettings.LANGUAGE.getValue().equals("") ? ApplicationSettings.DEFAULT_LOCALE.getValue() : ApplicationSettings.LANGUAGE.getValue();
        addLocale(ggep, locale, RouterService.getConnectionManager().getNumLimeWireLocalePrefSlots());

        // Add IP addresses to the GGEP block
        addAddress(ggep, returnAddr); // Add the "IP" header with our IP address
        addPackedHosts(ggep, hosts);  // Add the "IPP" header with the given list of other IP addresses

        // Make a new PingReply object
        return create(

            // Information for the Gnutella packet header
            guid, // The GUID that marks this packet unique
            ttl,  // The number of times the packet can travel between ultrapeers

            // Information for the pong payload
            RouterService.getPort(), // The IP address and port number other computers on the Internet can contact us at
            RouterService.getAddress(),
            (long)RouterService.getNumSharedFiles(), // The number of files we're sharing, and their total size in KBs
            (long)RouterService.getSharedFileSize() / 1024,
            RouterService.isSupernode(), // If we're an ultrapeer, adjust our shared file size to the nearest power of 2

            // The GGEP block
            ggep);
    }

    /**
     * QueryKey is a part of GUESS, which LimeWire no longer uses.
     * MessageRouter.sendQueryKeyPong calls this.
     * 
     * Creates a new <tt>PingReply</tt> for this host with the specified
     * GUID, ttl, and query key.
     * 
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param key the <tt>QueryKey</tt> for this reply
     */
    public static PingReply createQueryKeyReply(byte[] guid, byte ttl, QueryKey key) {
        return create(
            guid,
            ttl,
            RouterService.getPort(),
            RouterService.getAddress(),
            RouterService.getNumSharedFiles(),
            RouterService.getSharedFileSize() / 1024,
            RouterService.isSupernode(),
            qkGGEP(key));
    }

    /**
     * QueryKey is a part of GUESS, which LimeWire no longer uses.
     * Only test code calls this.
     * 
     * Creates a new <tt>PingReply</tt> for this host with the specified
     * GUID, ttl, and query key.
     * 
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param key the <tt>QueryKey</tt> for this reply
     */
    public static PingReply createQueryKeyReply(byte[] guid, byte ttl, int port, byte[] ip, long sharedFiles, long sharedSize, boolean ultrapeer, QueryKey key) {
        return create(
            guid,
            ttl,
            port,
            ip,
            sharedFiles,
            sharedSize,
            ultrapeer,
            qkGGEP(key));
    }

    /**
     * Make a new PingReply object that represents a pong packet with information about a remote computer.
     * It won't have any information about us.
     * We don't know how many files the computer is sharing, so the shared number and size will be 0.
     * 
     * @param guid    The packet GUID for its header
     * @param ttl     The packet TTL for its header
     * @param port    The port number the computer is listening on
     * @param address The IP address of the computer
     * @return        A new PingReply object that represents a Gnutella pong packet with that information
     */
    public static PingReply create(byte[] guid, byte ttl, int port, byte[] address) {

        // Make a new PingReply object
        return create(

            // Include the given information
            guid, ttl, port, address,

            // Pass default values and omit additional GGEP tags
            0,      // We're not sharing any files
            0,      // Their total size in KB is 0
            false,  // Don't add the "UP" tag
            -1,     // Don't add the "DU" tag
            false); // Don't the "GUE" tag
    }

    /**
     * Make a new PingReply object that represents a pong packet with information about a remote computer.
     * It won't have any information about us.
     * We don't know how many files the computer is sharing, so the shared number and size will be 0.
     * 
     * @param guid      The packet GUID for its header
     * @param ttl       The packet TTL for its header
     * @param port      The port number the computer is listening on
     * @param address   The IP address of the computer
     * @param ultrapeer True if the computer is an ultrapeer, the GGEP block will get the "UP" extension
     * @return          A new PingReply object that represents a Gnutella pong packet with that information
     */
    public static PingReply createExternal(byte[] guid, byte ttl, int port, byte[] address, boolean ultrapeer) {

        /*
         * TODO:kfaaborg There is a way that some information about us can sneak into the pong this method makes.
         * If ultrpeer is true, create() below will call newGGEP() with isUltrapeer true.
         * It calls addUltrapeerExtension(), which calls RouterService.getNumFreeLimeWireLeafSlots().
         * The pong's GGEP will have the "UP" extension, and its value will have our number of free slots.
         * The program uses createExternal() to make a pong packet about a remote computer.
         * It should not contain any information about us.
         */

        // Make a new PingReply object
        return create(
            guid,      // Include the given information
            ttl,
            port,
            address,
            0,         // Don't number any shared files or any KB of shared data
            0,
            ultrapeer, // If true, include the "UP" GGEP extension
            -1,        // Don't add the "DU" daily uptime extension
            false);    // Don't add the "GUE" tag
    }

    /**
     * Make a new PingReply object that represents a pong packet with information about a remote computer.
     * It won't have any information about us.
     * We don't know how many files the computer is sharing, so the shared number and size will be 0.
     * Primarily used for testing.
     * 
     * @param guid      The packet GUID for its header
     * @param ttl       The packet TTL for its header
     * @param port      The port number the computer is listening on
     * @param address   The IP address of the computer
     * @param uptime    Add "DU" with this number of seconds as the value, or -1 if we don't know and shouldn't say
     * @param ultrapeer Add "UP" with information about how many slots we have TODO:kfaaborg information about us is slipping in
     * @return          A new PingReply object that represents a Gnutella pong packet with that information
     */
    public static PingReply createExternal(byte[] guid, byte ttl, int port, byte[] address, int uptime, boolean ultrapeer) {

        // Make a new PingReply object
        return create(
            guid,      // Include the given information
            ttl,
            port,
            address,
            0,         // Don't number any shared files or any KB of shared data
            0,
            ultrapeer, // If true, include the "UP" GGEP extension
            uptime,    // If not -1, include the "DU" GGEP extension with the daily uptime seconds as its value
            false);    // Don't add the "GUE" tag
    }

    /**
     * Make a new PingReply object that represents a pong packet about a computer that is a GUESS-capable ultrapeer.
     * This method is only used to create pongs for computers other than ourselves.
     * Adds the "UP" and "GUE" extensions to the GGEP block.
     * Leaves the number of shared files and their size in KB 0.
     * 
     * @param guid    The packet GUID for its header
     * @param ttl     The packet TTL for its header
     * @param ep      The computer's IP address and port number
     * @return        A new PingReply object that represents a Gnutella pong packet with the given information
     */
    public static PingReply createGUESSReply(byte[] guid, byte ttl, Endpoint ep) throws UnknownHostException {

        /*
         * TODO:kfaaborg The Javadoc says the program uses this method to make a pong about a computer that is not us.
         * But, passing true, -1, true, leads to information about us getting into the GGEP block.
         */

        // Make and return a new PingReply object
        return create(
            guid,              // Include the given information
            ttl,
            ep.getPort(),
            ep.getHostBytes(),
            0,                 // No shared files or KBs
            0,
            true,              // Add "UP" ultrapeer
            -1,                // Omit "DU"
            true);             // Add "GUE" guess
    }

    /**
     * Make a new PingReply object that represents a pong packet about a computer that is a GUESS-capable ultrapeer.
     * This method is only used to create pongs for computers other than ourselves.
     * Adds the "UP" and "GUE" extensions to the GGEP block.
     * Leaves the number of shared files and their size in KB 0.
     * 
     * @param guid    The packet GUID for its header
     * @param ttl     The packet TTL for its header
     * @param port    The computer's port number
     * @param address The computer's IP address
     * @return        A new PingReply object that represents a Gnutella pong packet with the given information
     */
    public static PingReply createGUESSReply(byte[] guid, byte ttl, int port, byte[] address) {

        /*
         * TODO:kfaaborg The Javadoc says the program uses this method to make a pong about a computer that is not us.
         * But, passing true, -1, true, leads to information about us getting into the GGEP block.
         */

        // Make and return a new PingReply object
        return create(
            guid,    // Include the given information
            ttl,
            port,
            address,
            0,       // No shared files or KBs
            0,
            true,    // Add "UP" ultrapeer
            -1,      // Omit "DU"
            true);   // Add "GUE" guess
    }

    /**
     * Make a new PingReply object that represents a pong packet with information about us.
     * Places the given information about us in the pong header, payload, and GGEP block.
     * Calls methods within the program to get additional information specific to us.
     * Used for testing.
     * 
     * @param guid   The packet GUID for its header
     * @param ttl    The packet TTL for its header
     * @param port   Our port number for the packet header
     * @param ip     Our IP address for the packet header
     * @param files  The number of files we're sharing for the pong payload
     * @param kbytes The total size in KB of all the files we're sharing for the pong payload
     * @return       A new PingReply object that represents a Gnutella pong packet with all that information about us
     */
    public static PingReply create(byte[] guid, byte ttl, int port, byte[] ip, long files, long kbytes) {

        // Make and return a new PingReply object
        return create(
            guid,   // Include the given information
            ttl,
            port,
            ip,
            files,
            kbytes,
            false,  // Don't add "UP"
            -1,     // Don't add "DU"
            false); // Don't add "GUE"
    }

    /**
     * Make a new PingReply object that represents a pong packet with information about us.
     * Places the given information about us in the pong header, payload, and GGEP block.
     * Calls methods within the program to get additional information specific to us.
     * 
     * @param guid           The packet GUID for its header
     * @param ttl            The packet TTL for its header
     * @param port           Our port number for the packet header
     * @param ip             Our IP address for the packet header
     * @param files          The number of files we're sharing for the pong payload
     * @param kbytes         The total size in KB of all the files we're sharing for the pong payload
     * @param isUltrapeer    Adds "UP" with information about how many slots we have as the value in the GGEP block, and move kbytes to a power of 2
     * @param dailyUptime    Adds "DU" with this number of seconds as the value, or -1 if we don't know and shouldn't say
     * @param isGuessCapable If true and we're an ultrapeer, adds "GUE" with 0.1 as the value
     * @return               A new PingReply object that represents a Gnutella pong packet with all that information about us
     */
    public static PingReply create(byte[] guid, byte ttl, int port, byte[] ip, long files, long kbytes, boolean isUltrapeer, int dailyUptime, boolean isGUESSCapable) {

        // Make a new GGEP block with DU, GUE, UP, and VC, and put that and the rest of the information about us in a new PingReply object
        return create(guid, ttl, port, ip, files, kbytes, isUltrapeer, newGGEP(dailyUptime, isUltrapeer, isGUESSCapable));
    }

    /**
     * Make a new PingReply object that represents a pong packet with information about us.
     * Places the given information about us in the pong header, payload, and GGEP block.
     * This constructor calls methods within the program to get additional information about us.
     * 
     * @param guid           The packet GUID for its header
     * @param ttl            The packet TTL for its header
     * @param port           Our port number for the packet header
     * @param ip             Our IP address for the packet header
     * @param files          The number of files we're sharing for the pong payload
     * @param kbytes         The total size in KB of all the files we're sharing for the pong payload
     * @param isUltrapeer    Adds "UP" with information about how many slots we have as the value in the GGEP block, and move kbytes to a power of 2
     * @param dailyUptime    Adds "DU" with this number of seconds as the value, or -1 if we don't know and shouldn't say
     * @param isGuessCapable If true and we're an ultrapeer, adds "GUE" with 0.1 as the value
     * @param locale         Our language preference, stored in "LOC"
     * @param slots          The number of slots we have for our locale, stored in "LOC"
     * @return               A new PingReply object that represents a Gnutella pong packet with all that information about us
     */
    public static PingReply create(byte[] guid, byte ttl, int port, byte[] ip, long files, long kbytes, boolean isUltrapeer, int dailyUptime, boolean isGuessCapable, String locale, int slots) {

        // Have the create() method make the new PingReply object, passing an empty list so the GGEP block won't have an "IPP" extension
        return create(guid, ttl, port, ip, files, kbytes, isUltrapeer, dailyUptime, isGuessCapable, locale, slots, Collections.EMPTY_LIST);
    }

    /**
     * Make a new PingReply object that represents a pong packet with information about us.
     * Places the given information about us in the pong header, payload, and GGEP block.
     * create() calls methods within the program to get additional information about us.
     * 
     * @param guid           The packet GUID for its header
     * @param ttl            The packet TTL for its header
     * @param port           Our port number for the packet header
     * @param ip             Our IP address for the packet header
     * @param files          The number of files we're sharing for the pong payload
     * @param kbytes         The total size in KB of all the files we're sharing for the pong payload
     * @param isUltrapeer    Adds "UP" with information about how many slots we have as the value in the GGEP block, and move kbytes to a power of 2
     * @param dailyUptime    Adds "DU" with this number of seconds as the value, or -1 if we don't know and shouldn't say
     * @param isGuessCapable If true and we're an ultrapeer, adds "GUE" with 0.1 as the value
     * @param locale         Our language preference, stored in "LOC"
     * @param slots          The number of slots we have for our locale, stored in "LOC"
     * @param hosts          A list of IpPort objects to turn into 6 bytes each and store in "IPP"
     * @return               A new PingReply object that represents a Gnutella pong packet with all that information about us
     */
    public static PingReply create(byte[] guid, byte ttl, int port, byte[] ip, long files, long kbytes, boolean isUltrapeer, int dailyUptime, boolean isGuessCapable, String locale, int slots, Collection hosts) {

        // Make the GGEP block for our pong packet with information about us
        GGEP ggep = newGGEP(dailyUptime, isUltrapeer, isGuessCapable); // Make a new GGEP block with "DU", "GUE", "UP", and "VC", our vendor code
        addLocale(ggep, locale, slots);                                // Add "LOC"
        addPackedHosts(ggep, hosts);                                   // Add "IPP" with the IP addresses and port numbers placed together in the value

        // Make and return a pong packet with that GGEP block, putting the rest of the information in the packet header and pong payload
        return create(guid, ttl, port, ip, files, kbytes, isUltrapeer, ggep);
    }

    /**
     * Make a new PingReply object with all the same data as this one, but with the specified GUID.
     * 
     * @param guid The GUID to use in the new pong
     * @return     A new PingReply that's a copy of this one, but with the given GUID
     */
    public PingReply mutateGUID(byte[] guid) {

        // Make sure the GUID is exactly 16 bytes long
        if (guid.length != 16) throw new IllegalArgumentException("bad guid size: " + guid.length);

        /*
         * i can't just call a new constructor, i have to recreate stuff
         */

        try {

            // Make and return a new PingReply object
            return createFromNetwork(
                guid,      // The given GUID
                getTTL(),  // The TTL and hops count of this pong
                getHops(),
                PAYLOAD);  // The standard pong payload and the GGEP block

        } catch (BadPacketException ioe) { throw new IllegalArgumentException("Input pong was bad!"); }
    }

    /**
     * Make PingReply object that represents a pong packet with information about a computer online running Gnutella software.
     * Takes a GGEP object, serializes it into a byte array, and has the next constructor save that as PAYLOAD.
     * 
     * @param guid        The GUID that marks this Gnutella packet unique
     * @param ttl         The TTL for this packet
     * @param port        The port number we can contact the computer on
     * @param ipBytes     The IP address of the computer
     * @param files       How many files the computer is sharing
     * @param kbytes      The total number of KB of files the computer is sharing
     * @param isUltrapeer True if the computer is an ultrapeer
     * @param ggep        The GGEP block for this pong packet
     * @return            A new PingReply object with all that information
     */
    public static PingReply create(byte[] guid, byte ttl, int port, byte[] ipBytes, long files, long kbytes, boolean isUltrapeer, GGEP ggep) {

        // Make sure the port is 1 through 65535 and the IP address doesn't start 0 or 255
 		if (!NetworkUtils.isValidPort(port)) throw new IllegalArgumentException("invalid port: " + port);
        if (!NetworkUtils.isValidAddress(ipBytes)) throw new IllegalArgumentException("invalid address: " + NetworkUtils.ip2string(ipBytes));

        // The caller gave us the IP address as a byte array, convert it into an InetAddress object
        InetAddress ip = null;
        try { ip = InetAddress.getByName(NetworkUtils.ip2string(ipBytes)); } catch (UnknownHostException e) { throw new IllegalArgumentException(e.getMessage()); }

        // If we have a GGEP block for this pong packet, serialize it to a byte array named extensions
        byte[] extensions = null; // If we don't have a GGEP block, extensions will be null
        if (ggep != null) {

            // Serialize it into a ByteArrayOutputStream, and point extensions at it
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try { ggep.write(baos); } catch (IOException e) { ErrorService.error(e); }
            extensions = baos.toByteArray();
        }

        // Calculate the total payload length, 14 bytes for the standard pong payload, plus the length of the GGEP block
        int length = STANDARD_PAYLOAD_SIZE + (extensions == null ? 0 : extensions.length);

        // Write the payload into a new byte array named payload
        byte[] payload = new byte[length];            // Make a new byte array for the serialized payload
        ByteOrder.short2leb((short)port, payload, 0); // Start the payload with the 2 byte port number, least significant byte first
        payload[2] = ipBytes[0];                      // Next, write the IP address in big endian order
        payload[3] = ipBytes[1];
        payload[4] = ipBytes[2];
        payload[5] = ipBytes[3];
        ByteOrder.int2leb((int)files, payload, 6);    // 6 bytes into the payload buffer, write the 4 bytes of the files int in little endian order

        // If we're making this pong to describe an ultrapeer, adjust the number of shared KBs to the nearest power of 2
        ByteOrder.int2leb((int)(isUltrapeer ? mark(kbytes) : kbytes), payload, 10); // Write this 10 bytes into the payload

        // If we have a GGEP block, write it in after that
        if (extensions != null) System.arraycopy(extensions, 0, payload, STANDARD_PAYLOAD_SIZE, extensions.length);

        // Make a new PingReply object with the GUID, TTL, 0 hops, the payload we just serialized, the GGEP block we serialized, and the given IP address
        return new PingReply(guid, ttl, (byte)0, payload, ggep, ip);
    }

    /**
     * Make a new PingReply object to hold a pong we received from the Gnutella network.
     * 
     * Saves the given information from the packet header in the Message parts of this PingReply object, like guid, func, ttl, and hops.
     * Turns the payload data into a parsed GGEP object.
     * Sets the values of member variables like DAILY_UPTIME and SUPPORTS_UNICAST.
     * 
     * @param  guid               The message GUID we read from the packet header
     * @param  ttl                The TTL from the packet header
     * @param  hops               The hops count from the packet header
     * @param  payload            The pong packet's payload we read after the header
     * @return                    A new PingReply object with that information loaded, checked, and parsed
     * @throws BadPacketException The message is invalid for any reason
     */
    public static PingReply createFromNetwork(byte[] guid, byte ttl, byte hops, byte[] payload) throws BadPacketException {

        // Make sure we got a GUID and payload, and make sure the payload is long enough
        if (guid    == null) throw new NullPointerException("null guid");
        if (payload == null) throw new NullPointerException("null payload");
        if (payload.length < STANDARD_PAYLOAD_SIZE) { // If the pong has no GGEP block, the payload will be 14 bytes

            // The payload is too short
            ReceivedErrorStat.PING_REPLY_INVALID_PAYLOAD.incrementStat();
            throw new BadPacketException("invalid payload length");
        }

        // Read the port number from the first 2 bytes of the standard pong payload
        int port = ByteOrder.ushort2int(ByteOrder.leb2short(payload, 0));
 		if (!NetworkUtils.isValidPort(port)) {

            // It's 0
 		    ReceivedErrorStat.PING_REPLY_INVALID_PORT.incrementStat();
			throw new BadPacketException("invalid port: " + port);
        }

        /*
         * this address may get updated if we have the UDPHC extention
         * therefore it is checked after checking for that extention.
         */

        // After the 2-byte port number is the 4-byte IP address
        String ipString = NetworkUtils.ip2string(payload, 2);

        // The IP address of the computer this pong is from, and contains information about
        InetAddress ip = null;

        // Have the GGEP constructor parse the pong payload into a new GGEP object
        GGEP ggep = parseGGEP(payload); // Looks beyond the standard pong payload, returns null if there is no GGEP block
        if (ggep != null) { // There is a GGEP block

            /*
             * Have the GGEP object try parsing for various values.
             * Don't keep the values we get.
             * Just do this to see if we can read these values.
             * If we can't, throw a BadPacketException.
             */

            // If the GGEP block has the "VC" vendor code header, make sure we can read its value
            if (ggep.hasKey(GGEP.GGEP_HEADER_VENDOR_INFO)) {
                byte[] vendorBytes = null;
                try {
                    vendorBytes = ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO); // It will be like "LIME#" with the version number in the last byte
                } catch (BadGGEPPropertyException e) {
                    ReceivedErrorStat.PING_REPLY_INVALID_GGEP.incrementStat();
                    throw new BadPacketException("bad GGEP: " + vendorBytes);
                }
                if (vendorBytes.length < 4) { // Make sure the value is long enough to hold the "LIME" part, the version number is optional
                    ReceivedErrorStat.PING_REPLY_INVALID_VENDOR.incrementStat();
                    throw new BadPacketException("invalid vendor length: " + vendorBytes.length);
                }
            }

            // If the GGEP block has the "LOC" client locale header, make sure we can read its value
            if (ggep.hasKey(GGEP.GGEP_HEADER_CLIENT_LOCALE)) {
                try {
                    byte[] clocale = ggep.getBytes(GGEP.GGEP_HEADER_CLIENT_LOCALE);
                } catch (BadGGEPPropertyException e) {
                    ReceivedErrorStat.PING_REPLY_INVALID_GGEP.incrementStat();
                    throw new BadPacketException("GGEP error : creating from" + " network : client locale");
                }
            }

            // If the GGEP block has the "IPP" packed IP addresses and port numbers tag, make sure we can read its value
            if (ggep.hasKey(GGEP.GGEP_HEADER_PACKED_IPPORTS)) {
                byte[] data = null;
                try {
                    data = ggep.getBytes(GGEP.GGEP_HEADER_PACKED_IPPORTS);
                } catch (BadGGEPPropertyException bad) {
                    throw new BadPacketException(bad.getMessage());
                }

                // Make sure the value is a multiple of 6 bytes, 4 for the IP address and 2 for the port number
                if (data == null || data.length % 6 != 0) throw new BadPacketException("invalid data");
            }

            // If the GGEP block has the "PHC" packed UDP host cache tag, make sure we can read its value
            if (ggep.hasKey(GGEP.GGEP_HEADER_PACKED_HOSTCACHES)) {
                try {
                    ggep.getBytes(GGEP.GGEP_HEADER_PACKED_HOSTCACHES);
                } catch (BadGGEPPropertyException bad) {
                    throw new BadPacketException(bad.getMessage());
                }
            }
            
            /*
             * We're done reading values just to see if we can.
             * We'll read the value of this last one and save it.
             */

            // The GGEP block has the "UDPHC" tag, meaning this pong is from a UDP host cache
            if (ggep.hasKey(GGEP.GGEP_HEADER_UDP_HOST_CACHE)) {

                try {

                    // Get the String value, the IP address of the UDP host cache like "71.240.19.76"
                    String dns = ggep.getString(GGEP.GGEP_HEADER_UDP_HOST_CACHE);

                    // Use this as the pong's IP address instead of Turn it into an InetAddress object, and then back into a String
                    ip = InetAddress.getByName(dns);
                    ipString = ip.getHostAddress();

                } catch (BadGGEPPropertyException ignored) {
                } catch (UnknownHostException bad) {
                    throw new BadPacketException(bad.getMessage());
                }
            }
        }

        // Make sure the IP address doesn't start 0 or 255
        if (!NetworkUtils.isValidAddress(ipString)) {
            ReceivedErrorStat.PING_REPLY_INVALID_ADDRESS.incrementStat();
            throw new BadPacketException("invalid address: " + ipString);
        }

        // If this pong isn't from a UDP host cache, ip will still be null
        if (ip == null) {

            try {

                // Read the pong's IP address from its standard payload
                ip = InetAddress.getByName(NetworkUtils.ip2string(payload, 2)); // 2 bytes in, after the port number

            } catch (UnknownHostException e) { throw new BadPacketException("bad IP:" + ipString + " " + e.getMessage()); }
        }

        // Make and return a new PingReply object
        return new PingReply(
            guid,    // The given GUID, ttl, and hops
            ttl,
            hops,
            payload, // The given payload
            ggep,    // We parsed the payload and made this GGEP object from it
            ip);     // The IP address from the standard payload, or the GGEP "UDPHC" tag if present
    }

    /**
     * Make a new PingReply object to represent a pong packet with the given GUID, ttl and hops, payload, and GGEP block.
     * 
     * One of the arguments is a GGEP object named ggep.
     * This constructor reads values from it to set member variables here like DAILY_UPTIME and SUPPORTS_UNICAST.
     * Another argument is a byte array named payload.
     * This constructor just points PAYLOAD at it, it doesn't look at what's inside.
     * It also doesn't serialize ggep into payload, or parse payload into ggep.
     * 
     * @param guid    The GUID that uniquely identififes this Gnutella packet
     * @param ttl     The TTL, the number of times this packet can still travel across the Internet
     * @param hops    Hops count, the number of times this packet has already traveled across the Internet
     * @param payload A byte array with the message payload, which includes the standard pong payload and the serialized GGEP block
     * @param ggep    The GGEP block as a GGEP object
     * @param ip      The IP address of the computer this pong packet describes
     */
    private PingReply(byte[] guid, byte ttl, byte hops, byte[] payload, GGEP ggep, InetAddress ip) {

        // Call the Message constructor to set the GUID, packet type byte, TTL and hops counts, and payload length in this new PingReply object
        super(guid, Message.F_PING_REPLY, ttl, hops, payload.length);

        // Look at the given payload to save information in this PingReply object
        PAYLOAD   = payload;                                               // Point PAYLOAD at the given byte array
        PORT      = ByteOrder.ushort2int(ByteOrder.leb2short(PAYLOAD, 0)); // Read the port number from the start of the payload
        FILES     = ByteOrder.uint2long(ByteOrder.leb2int(PAYLOAD, 6));    // Read the number of shared files from 6 bytes into the payload
        KILOBYTES = ByteOrder.uint2long(ByteOrder.leb2int(PAYLOAD, 10));   // Read the total shared data size in KB from 10 bytes into the payload

        // Save the IP address of the computer this pong is about
        IP = ip;

        /*
         * GGEP parsing
         * GGEP ggep = parseGGEP();
         */

        // Make variables for the information we'll read from the GGEP block
        int         dailyUptime        = -1; // If we can't find a tag, the values will remain these defaults or -1 for not found
        boolean     supportsUnicast    = false;
        String      vendor             = "";
        int         vendorMajor        = -1;
        int         vendorMinor        = -1;
        int         freeLeafSlots      = -1;
        int         freeUltrapeerSlots = -1;
        QueryKey    key                = null; // QueryKey is a part of GUESS, which LimeWire no longer uses
        String      locale             = ApplicationSettings.DEFAULT_LOCALE.getValue(); // If the pong doesn't specify a language preference, use "en" for English
        int         slots              = -1;
        InetAddress myIP               = null;
        int         myPort             = 0;
        List        packedIPs          = Collections.EMPTY_LIST;
        List        packedCaches       = Collections.EMPTY_LIST;
        String      cacheAddress       = null;

        /*
         * TODO: the exceptions thrown here are messy
         */

        // If this pong has a GGEP block, parse it
        if (ggep != null) {

            // "DU" daily uptime
            if (ggep.hasKey(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME)) {

                // Get the value, the number of seconds the computer is online on an average day
                try {
                    dailyUptime = ggep.getInt(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME);
                } catch (BadGGEPPropertyException e) {}
            }

            // "GUE" unicast protocol support
            supportsUnicast = ggep.hasKey(GGEP.GGEP_HEADER_UNICAST_SUPPORT);

            // "VC" vendor code
            if (ggep.hasKey(GGEP.GGEP_HEADER_VENDOR_INFO)) {

                try {

                    // Get the value of the "VC" key, which will be 5 bytes like "LIME#" with the version number squashed into the last byte
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO);

                    // Read the vendor text like "LIME" as a String
                    if (bytes.length >= 4) vendor = new String(bytes, 0, 4); // Read the vendor text like "LIME" as a String
                    if (bytes.length > 4) {                                  // There's a 5th byte with the version number, like 4.9
                        vendorMajor = bytes[4] >> 4;                         // The major version number, like 4, is in the upper half of the byte
                        vendorMinor = bytes[4] & 0xF;                        // The minor version number, like 9, is in the lower half of the byte
                    }

                } catch (BadGGEPPropertyException e) {}
            }

            // "QK" QueryKey is a part of GUESS, which LimeWire no longer uses
            if (ggep.hasKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT)) {

                try {

                    // Make sure the value is between 4 and 16 bytes, and turn it into a QueryKey object
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
                    if (QueryKey.isValidQueryKeyBytes(bytes)) key = QueryKey.getQueryKey(bytes, false);

                } catch (BadGGEPPropertyException corrupt) {}
            }

            // "UP" ultrapeer
            if (ggep.hasKey((GGEP.GGEP_HEADER_UP_SUPPORT))) {

                try {

                    // Get the value, and make sure it's at least 3 bytes
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_UP_SUPPORT);
                    if (bytes.length >= 3) {

                        // Read the last 2 bytes, which contain the number of free slots the computer has for leaves and ultrapeers
                        freeLeafSlots = bytes[1];
                        freeUltrapeerSlots = bytes[2];
                    }

                } catch (BadGGEPPropertyException e) {}
            }

            // "LOC" locale
            if (ggep.hasKey(GGEP.GGEP_HEADER_CLIENT_LOCALE)) {

                try {

                    // Read the locale, like "en", and the number of slots the computer has for connections that share its locale
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_CLIENT_LOCALE);
                    if (bytes.length >= 2) locale = new String(bytes, 0, 2);
                    if (bytes.length >= 3) slots = ByteOrder.ubyte2int(bytes[2]);

                } catch (BadGGEPPropertyException e) {}
            }

            // "IP" IP address and port number request
            if (ggep.hasKey(GGEP.GGEP_HEADER_IPPORT)) {

                try {

                    // Read the value of the "IP" header, the IP address and port number in 6 bytes
                    byte[] data = ggep.getBytes(GGEP.GGEP_HEADER_IPPORT);

                    // Make a new 4 byte array for the IP address
                    byte[] myip = new byte[4];

                    /*
                     * only copy the addr if the data is atleast 6
                     * bytes (ip + port).  that way isValidAddress
                     * will fail & we don't need to recheck the length
                     * when getting the port.
                     */

                    // If the value contains the IP address and port number, copy the IP address into the myip array
                    if (data.length >= 6) System.arraycopy(data, 0, myip, 0, 4);
                    if (NetworkUtils.isValidAddress(myip)) { // The address doesn't start 0 or 255

                        try {

                            // Convert the 4 bytes of the IP address into a Java InetAddress object
                            myIP = NetworkUtils.getByAddress(myip);

                            // Read the port number 4 bytes into the value
                            myPort = ByteOrder.ushort2int(ByteOrder.leb2short(data, 4));

                            // If the IP address is a LAN address or the port number is 0
                            if (NetworkUtils.isPrivateAddress(myIP) || !NetworkUtils.isValidPort(myPort)) {

                                /*
                                 * liars, or we are behind a NAT and there is LAN outside
                                 * either way we can't use it
                                 */

                                // Clear our records of the IP address and port number
                                myIP = null;
                                myPort = 0;
                            }

                        // Keep the IP address null and the port 0
                        } catch (UnknownHostException bad) {}
                    }

                } catch (BadGGEPPropertyException ignored) {}
            }

            // "UDPHC" UDP host cache pong
            if (ggep.hasKey(GGEP.GGEP_HEADER_UDP_HOST_CACHE)) {

                // Get the value as a String
                cacheAddress = "";
                try {
                    cacheAddress = ggep.getString(GGEP.GGEP_HEADER_UDP_HOST_CACHE);
                } catch (BadGGEPPropertyException bad) {}
            }

            // "IPP" packed IP addresses and port numbers
            if (ggep.hasKey(GGEP.GGEP_HEADER_PACKED_IPPORTS)) {

                try {

                    // Get the value, a byte array a multiple of 6 bytes long, and turn it into a List of IpPortCombo objects
                    byte[] data = ggep.getBytes(GGEP.GGEP_HEADER_PACKED_IPPORTS);
                    packedIPs = NetworkUtils.unpackIps(data);

                } catch (BadGGEPPropertyException bad) {
                } catch (BadPacketException bpe) {}
            }

            // "PHC" packed UDP host caches
            if (ggep.hasKey(GGEP.GGEP_HEADER_PACKED_HOSTCACHES)) {

                try {

                    // Get the value, a String with IP addresses and port numbers stored as text, and turn it into a List of IpPortImpl objects
                    String data = ggep.getString(GGEP.GGEP_HEADER_PACKED_HOSTCACHES);
                    packedCaches = listCaches(data);

                } catch (BadGGEPPropertyException bad) {}
            }
        }

        /*
         * The "IP" extension is used specifically when one computer pings another to see what its IP address looks like from the outside.
         */

        // From the "IP" key, the IP address and port number the pong says it's computer is externally contactable at
        MY_IP   = myIP;   // The first 4 bytes of the "IP" value
        MY_PORT = myPort; // The 2 bytes after that

        // If this pong has a GGEP block and we've been parsing it, set HAS_GGEP_EXTENSION to true
        HAS_GGEP_EXTENSION = ggep != null;

        // The value of the "DU" key, the number of seconds the computer is online in an average day
        DAILY_UPTIME = dailyUptime;

        // If the block has a "GUE" tag, that indicates unicast support
        SUPPORTS_UNICAST = supportsUnicast;

        // From the "VC" vendor code tag
        VENDOR               = vendor;      // The vendor code, like "LIME", from the first 4 bytes of the "VC" value
        VENDOR_MAJOR_VERSION = vendorMajor; // The major version number, like 4 in 4.9, from the high half of the 5th byte
        VENDOR_MINOR_VERSION = vendorMinor; // The minor version number, like 9, from the low half of the byte

        // From the "QK" query key tag, the QueryKey object we made from the 4-16 byte value
        QUERY_KEY = key; // QueryKey is a part of GUESS, which LimeWire no longer uses

        // From the "UP" ultrapeer tag, the number of free slots the ultrapeer computer has for leaves and other ultrapeers
        FREE_LEAF_SLOTS      = freeLeafSlots;
        FREE_ULTRAPEER_SLOTS = freeUltrapeerSlots;

        // From the "LOC" locale tag
        CLIENT_LOCALE     = locale; // The computer's language preference, like "en"
        FREE_LOCALE_SLOTS = slots;  // The number of slots the computer has for computers that share that language preference

        // From the "UDPHC" UDP host cache tag, the String value
        if (cacheAddress != null && "".equals(cacheAddress)) UDP_CACHE_ADDRESS = getAddress(); // It's blank, use IP we set from the given value
        else UDP_CACHE_ADDRESS = cacheAddress;

        // From the "IPP" packed IP addresses and port numbers, a List of IpPortCombo objects
        PACKED_IP_PORTS = packedIPs;

        // From the "PHC" packed UDP host caches, a List of IpPortImpl objects with the addresses of UDP host caches
        PACKED_UDP_HOST_CACHES = packedCaches;
    }

    /**
     * Compose a GGEP block for our pong packet with information about us with the tags DU, GUE, UP, and VC.
     * 
     * Adds the following tags and values:
     * Daily uptime "DU" and the given dailyUptime number as the value.
     * GUESS support "GUE" and the GUESS version 0.1 squashed into a single byte as the value.
     * Ultraper information "UP" and how many free leaf and ultrapeer slots we have for other LimeWire computers to connect to.
     * Our vendor code "VC" with the value "LIME#", the last byte has our version number like 4.9 squashed into it.
     * 
     * @param dailyUptime    The number of seconds we're online on an average day
     * @param isUltrapeer    True if we're an ultrapeer, false if we're just a leaf
     * @param isGUESSCapable True, we support GUESS queries
     * @return               A new GGEP object with "DU", "GUE", "UP", and "VC" tags
     */
    private static GGEP newGGEP(int dailyUptime, boolean isUltrapeer, boolean isGUESSCapable) {

        // Make a new empty GGEP object that we'll add our GGEP tags to
        GGEP ggep = new GGEP(true); // True, don't COBS encode values to eliminate 0 bytes

        // Add the daily uptime GGEP tag "DU" with the daily uptime number as its value
        if (dailyUptime >= 0) ggep.put(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME, dailyUptime);

        // If the computer is a GUESS-capable ultrapeer, add the tag "GUE" and 0.1 in one byte as the value
        if (isGUESSCapable && isUltrapeer) {

            // vNum is a byte array with 1 byte, the high half contains 0 and the low half contains 1, this expresses the GUESS version, 0.1
            byte[] vNum = { convertToGUESSFormat(CommonUtils.getGUESSMajorVersionNumber(), CommonUtils.getGUESSMinorVersionNumber()) };

            // Add the tag "GUE" and value 0.1 in one byte, to the GGEP block we're preparing
            ggep.put(GGEP.GGEP_HEADER_UNICAST_SUPPORT, vNum);
        }

        // If we're an ultrapeer, add the "UP" tag with the number of free limewire leaf and ultrapeer slots
        if (isUltrapeer) addUltrapeerExtension(ggep);

        /*
         * all pongs should have vendor info
         */

        // Add "VC" "LIME#" with the version number like 4.9 squashed into the last byte
        ggep.put(GGEP.GGEP_HEADER_VENDOR_INFO, CACHED_VENDOR);

        // Return the GGEP block we made
        return ggep;
    }

    /**
     * QueryKey is a part of GUESS, which LimeWire no longer uses.
     * 
     * Returns the GGEP payload bytes to encode the given QueryKey.
     */
    private static GGEP qkGGEP(QueryKey queryKey) {
        try {
            GGEP ggep = new GGEP(true);
            // get qk bytes....
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            queryKey.write(baos);
            // populate GGEP....
            ggep.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT, baos.toByteArray());
            return ggep;
        } catch (IOException e) {
            //See above.
            Assert.that(false, "Couldn't encode QueryKey" + queryKey);
            return null;
        }
    }

    /**
     * Adds the tag "LOC" with a value like "en#" to our GGEP block.
     * The number # is the number of additional ultrapeers we want that share our language preference.
     * 
     * @param ggep   The GGEP block we'll modify
     * @param locale Our language preference, like "en" for English
     * @param slots  How many more ultrapeers we want that share our language preference
     * @return       A reference to the GGEP block we modified
     */
    private static GGEP addLocale(GGEP ggep, String locale, int slots) {

        // Compose the value in a 3 byte array, like "en#" where # is how many more ultrapeers we want that share our language preference
        byte[] payload = new byte[3];
        byte[] s = locale.getBytes();
        payload[0] = s[0];
        payload[1] = s[1];
        payload[2] = (byte)slots;

        // Put it in our GGEP block under the tag "LOC"
        ggep.put(GGEP.GGEP_HEADER_CLIENT_LOCALE, payload);
        return ggep;
    }

    /**
     * Add the "IP" header to the given GGEP block, with the given IP address and port number as its 6-byte value.
     * The "IP" extension is used specifically when one computer pings another to see what its IP address looks like from the outside.
     * 
     * @param ggep    The GGEP block to add the "IP" header to
     * @param address An object that supports the IpPort interface that we can get the IP address and port number from
     * @return        The same GGEP object
     */
    private static GGEP addAddress(GGEP ggep, IpPort address) {

        // Turn the given IpPort address into 6 bytes, with the IP address in the first 4 and the port number in the last 2
        byte[] payload = new byte[6];
        System.arraycopy(address.getInetAddress().getAddress(), 0, payload, 0, 4);
        ByteOrder.short2leb((short)address.getPort(), payload, 4);

        // Put the address under "IP" in the GGEP block
        ggep.put(GGEP.GGEP_HEADER_IPPORT, payload);

        // Return the reference to the GGEP block we were given, and that we edited
        return ggep;
    }

    /**
     * Add the "IPP" extension to a GGEP block with a list of IP addresses and port numbers as its value.
     * 
     * Serializes the IpPort objects into a byte array.
     * Each IpPort object becomes 6 bytes.
     * The first 4 are the IP address, and the last 2 are the port number.
     * This is the value of the "IPP" header.
     * 
     * @param ggep  A GGEP object to edit
     * @param hosts A list of IpPort objects
     * @return      The same GGEP object
     */
    private static GGEP addPackedHosts(GGEP ggep, Collection hosts) {

        // If the caller didn't give us any IP addresses and port numbers, return the GGEP object unchanged
        if (hosts == null || hosts.isEmpty()) return ggep;

        // Turn the list of IpPort objects into a byte array, and store it as the value of "IPP" in the GGEP block
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, NetworkUtils.packIpPorts(hosts));

        // Return the GGEP block we were given and that we edited
        return ggep;
    }

    /**
     * Add our ultrapeer GGEP extension to the given GGEP block.
     * 
     * Adds the tag "UP" with a 3 byte long value like "192".
     * The first byte is the version number 0.1 squashed into a single byte.
     * The second and third bytes are the number of free slots we have for LimeWire leaves, 9, and ultrapeers, 2.
     * 
     * @param ggep The GGEP block we'll add the "UP" tag to
     */
    private static void addUltrapeerExtension(GGEP ggep) {

        // Make a new byte array of 3 bytes
        byte[] payload = new byte[3];

        // Set the first byte to 0.1 squashed into a single byte
        payload[0] = convertToGUESSFormat(CommonUtils.getUPMajorVersionNumber(), CommonUtils.getUPMinorVersionNumber());

        // Set the second byte to the number of free slots we'll let other LimeWire leaves connect to
        payload[1] = (byte)RouterService.getNumFreeLimeWireLeafSlots();

        // Set the third byte to the number of free slots we'll let other LimeWire ultrapeers connect to
        payload[2] = (byte)RouterService.getNumFreeLimeWireNonLeafSlots();

        // Add the tag "UP" with the 3-byte value to the given GGEP block
        ggep.put(GGEP.GGEP_HEADER_UP_SUPPORT, payload);
    }

    /**
     * Compose a byte with a major version number in the high order 4 bits, and a minor version number in the low order 4 bits.
     * The version numbers have to be 0 through 15.
     * 
     * @param major A major version number
     * @param minor A minor version number
     * @return      A byte with the major version number in the high 4 bits, and the minor version number in the low 4 bits
     */
    private static byte convertToGUESSFormat(int major, int minor) throws IllegalArgumentException {

        // Make sure major and minor are both 0 through 15
        if ((major < 0) || (minor < 0) || (major > 15) || (minor > 15)) throw new IllegalArgumentException();

        // Load the major and minor version numbers into a single byte, and return it
        int retInt = major;
        retInt = retInt << 4; // Shift the major version number up 4 bits into the higher half of the byte
        retInt |= minor;      // Copy the minor version number in the lower 4 bits
        return (byte)retInt;
    }

    /**
     * Determine if the ultrapeer this pong is about needs more leaves or ultrapeers.
     * Looks at the number of free leaf and ultrapeer slots in the "UP" GGEP header.
     * 
     * @return True if the "UP" GGEP header has a positive number of free leaf or ultrapeer slots
     */
    public boolean hasFreeSlots() {

        // If the "UP" GGEP header listed a positive number of free leaf or ultrapeer slots, return true
        return hasFreeLeafSlots() || hasFreeUltrapeerSlots();
    }

    /**
     * Determine if the ultrapeer this pong is about needs more leaves.
     * Looks at the number of free leaf slots in the "UP" GGEP header.
     * 
     * @return True if the "UP" GGEP header has a positive number of free leaf slots
     */
    public boolean hasFreeLeafSlots() {

        // If the "UP" GGEP header listed a positive number of free leaf slots, return true
        return FREE_LEAF_SLOTS > 0;
    }

    /**
     * Determine if the ultrapeer this pong is about needs more ultrapeers.
     * Looks at the number of free ultrapeer slots in the "UP" GGEP header.
     * 
     * @return True if the "UP" GGEP header has a positive number of free ultrapeer slots
     */
    public boolean hasFreeUltrapeerSlots() {

        // If the "UP" GGEP header listed a positive number of free ultrapeer slots, return true
        return FREE_ULTRAPEER_SLOTS > 0;
    }

    /**
     * The number of additional leaf connections the ultrapeer this pong is about needs.
     * 
     * @return The number we parsed from the "UP" GGEP header.
     *         -1 if the GGEP block doesn't have "UP".
     */
    public int getNumLeafSlots() {

        // Return the number we parsed from the GGEP "UP" header
        return FREE_LEAF_SLOTS;
    }

    /**
     * The number of additional ultrapeer connections the ultrapeer this pong is about needs.
     * 
     * @return The number we parsed from the "UP" GGEP header.
     *         -1 if the GGEP block doesn't have "UP.
     */
    public int getNumUltrapeerSlots() {

        // Return the number we parsed from the GGEP "UP" header
        return FREE_ULTRAPEER_SLOTS;
    }

    /**
     * Write the pong payload, including the GGEP block
     * This is the part after the Gnutella message header.
     * The standard pong payload is 14 bytes, and contains the computer's IP address and how many files it's sharing.
     * The GGEP block contains additional information.
     * 
     * @param out The OutputStream object we'll call out.write(PAYLOAD) on.
     */
    protected void writePayload(OutputStream out) throws IOException {

        // Write the bytes of the pong payload and GGEP block
        out.write(PAYLOAD);

        // Give this entire pong to the SentMessageStatHandler, calling this method means we're sending this packet
		SentMessageStatHandler.TCP_PING_REPLIES.addMessage(this);
    }

    /**
     * The port number that the computer this pong has information about is listening on.
     * The port number is the first 2 bytes of the standard pong payload.
     * 
     * @return the port number reported in the pong
     */
    public int getPort() {

        // Return the port number the PingReply constructor parsed
        return PORT;
    }

    /**
     * The IP address of the computer that this pong has information about.
     * This IP address is in the standard pong payload.
     * It's 4 bytes long, 2 bytes into the payload.
     * We parsed it, and turned it into a String.
     * 
     * @return The pong's IP address, like "71.113.91.191"
     */
    public String getAddress() {

        // Return the IP address the PingReply constructor received
        return IP.getHostAddress();
    }

    /**
     * The IP address of the computer that this pong has information about.
     * This IP address is in the standard pong payload.
     * It's 4 bytes long, 2 bytes into the payload.
     * This method returns a byte array of its 4 bytes.
     * The most significant byte is first, just as the data is written in the pong.
     * 
     * @return A byte array of 4 bytes with the IP address in the pong payload
     */
    public byte[] getIPBytes() {

        // Copy the 4 bytes of the IP address from the pong payload to a new byte array, and return it
        byte[] ip = new byte[4]; // Make a byte array that can hold 4 bytes
        ip[0] = PAYLOAD[2];      // Copy the 4 bytes starting from 2 bytes into the pong payload
        ip[1] = PAYLOAD[3];
        ip[2] = PAYLOAD[4];
        ip[3] = PAYLOAD[5];
        return ip;               // Return the byte array we made and filled
    }

    /**
     * The number of files the computer this pong is about is sharing.
     * This information is an int 6 bytes into the standard pong payload.
     * 
     * @return The number of files this pong reports the computer is sharing
     */
    public long getFiles() {

        // Return the number the PingReply constructor parsed
        return FILES;
    }

    /**
     * The total size in KB of all the files the computer this pong is about is sharing.
     * This information is an int 10 bytes into the standard pong payload.
     * 
     * @return The size in KB of the files this pong reports the computer is sharing
     */
    public long getKbytes() {

        // Return the number the PingReply constructor parsed
        return KILOBYTES;
    }

    /**
     * The number of seconds the computer this pong is about is online in an average day.
     * This is the value of the "DU" GGEP header.
     * 
     * @return The number of seconds online in an average day.
     *         -1 If the pong doesn't have the "DU" extension.
     */
    public int getDailyUptime() {

        // Return the number the PingReply constructor parsed
        return DAILY_UPTIME;
    }

    /**
     * True if the computer this pong is about supports UDP.
     * This is indicated by the presence of the "GUE" GGEP header.
     * This means the pong supports GUESS-style queries.
     * 
     * LimeWire doesn't search with GUESS anymore.
     * This is still how a pong describes a computer that is externally contactable for unsolicited UDP.
     * 
     * @return True if the computer can receive UDP packets
     */
    public boolean supportsUnicast() {

        // Return the value the PingReply constructor parsed
        return SUPPORTS_UNICAST;
    }

    /**
     * The Gnutella software the computer this pong is about is running, like "LIME" or "BEAR".
     * This is the start of the value of the "VC" GGEP header.
     * 
     * @return The software vendor code, or blank if the pong doesn't have the "VC" extension
     */
    public String getVendor() {

        // Return the text the PingReply constructor parsed
        return VENDOR;
    }

    /**
     * The major version number of the software the computer this pong is about is running, like 4 if the version number is 4.9.
     * Read from the value of the "VC" GGEP header.
     * 
     * @return The major version number, or -1 if the pong's GGEP block doesn't have the "VC" extension
     */
    public int getVendorMajorVersion() {

        // Return the number the PingReply constructor parsed
        return VENDOR_MAJOR_VERSION;
    }

    /**
     * The minor version number of the software the computer this pong is about is running, like 9 if the version number is 4.9.
     * Read from the value of the "VC" GGEP header.
     * 
     * @return The minor version number, or -1 if the pong's GGEP block doesn't have the "VC" extension
     */
    public int getVendorMinorVersion() {

        // Return the number the PingReply constructor parsed
        return VENDOR_MINOR_VERSION;
    }

    /**
     * QueryKey is a part of GUESS, which LimeWire no longer uses.
     * 
     * Returns the QueryKey (if any) associated with this pong.  May be null!
     *
     * @return the <tt>QueryKey</tt> for this pong, or <tt>null</tt> if no
     *  key was specified
     */
    public QueryKey getQueryKey() {
        return QUERY_KEY;
    }

    /**
     * Get the IP addresses and port numbers this pong lists that are of remote PCs running Gnutella software like us.
     * 
     * Reads the "IPP" GGEP extension.
     * These are the IP addresses and port numbers of remote PCs running Gnutella software like us.
     * We can treat these IPs as we would those in the "X-Try-Ultrapeers" header.
     * 
     * @return From the "IPP" packed IP addresses and port numbers, a List of IpPortCombo objects
     */
    public List getPackedIPPorts() {

        // Return the List of IpPortCombo objects we parsed from the GGEP "IPP" extension
        return PACKED_IP_PORTS;
    }

    /**
     * Get the IP addresses and port numbers this pong lists that are of UDP host caches.
     * 
     * Reads the "PHC" GGEP extension.
     * These are the IP addresses and port numbers of UDP host caches.
     * 
     * @return From the "PHC" packed host caches, a List of IpPortCombo objects
     */
    public List getPackedUDPHostCaches() {

        // Return the list of IpPortCombo objects we parsed from the GGEP "PHC" extension
        return PACKED_UDP_HOST_CACHES;
    }

    /**
     * Determine if this pong packet has a GGEP block.
     * Returns true if the parseGGEP() method found data beyond the standard pong payload, and the GGEP constructor turned it into a GGEP block.
     * 
     * @return True if this pong has a GGEP block, false if it's just the Gnutella header and standard pong payload
     */
    public boolean hasGGEPExtension() {

        // The PingReply constructor set this to true if we have one
        return HAS_GGEP_EXTENSION;
    }

    /**
     * Take a pong payload and parse it into a GGEP object.
     * 
     * TODO: Change this to look for multiple GGEP blocks in the payload.
     * 
     * @param PAYLOAD The pong's payload, which includes the standard 14 byte payload and possibly a GGEP block after that.
     * @return        A new GGEP object with the information from the payload parsed and organized.
     *                null if the payload doesn't have a GGEP block, or there's an error parsing the GGEP block.
     */
    private static GGEP parseGGEP(final byte[] PAYLOAD) {

        /*
         * Return if this is a plain pong without space for GGEP.  If
         * this has bad GGEP data, multiple calls to
         * parseGGEP will result in multiple parse attempts.  While this is
         * inefficient, it is sufficiently rare to not justify a parsedGGEP
         * variable.
         */

        // If the payload is only 14 bytes, there is no GGEP block here, return null instead of a new GGEP object
        if (PAYLOAD.length <= STANDARD_PAYLOAD_SIZE) return null;

        try {

            // Have the GGEP constructor parse the payload data, and return the new GGEP object
            return new GGEP(PAYLOAD, STANDARD_PAYLOAD_SIZE, null);

        // The GGEP constructor couldn't understand the data, return null instead of a new GGEP object
        } catch (BadGGEPBlockException e) { return null; }
    }

    /**
     * Make a copy of this PingReply object without a GGEP block.
     * Makes a new PingReply object with the same information in the Gnutella message header and standard pong payload.
     * Doesn't copy over the GGEP block.
     * 
     * The PingReply class extends Message, which lists stripExtendedPayload() as an abstract method we can implement.
     * 
     * @return A new PingReply object that's the same as this one but with no GGEP block
     */
    public Message stripExtendedPayload() {

        /*
         * TODO: if this is too slow, we can alias parts of this, as as the
         * payload.  In fact we could even return a subclass of PingReply that
         * simply delegates to this.
         */

        // Make a new byte array to hold the 14 byte standard pong payload
        byte[] newPayload = new byte[STANDARD_PAYLOAD_SIZE];

        // Copy the first 14 bytes of PAYLOAD to get the standard pong payload and leave the GGEP block behind
        System.arraycopy(PAYLOAD, 0, newPayload, 0, STANDARD_PAYLOAD_SIZE);

        // Make a new PingReply object
        return new PingReply(
            this.getGUID(), // Same information for the Gnutella message header
            this.getTTL(),
            this.getHops(),
            newPayload,     // The standard pong payload without a GGEP block
            null,           // No GGEP block
            IP);            // The IP address of the computer this pong is about
    }

    /**
     * Parses the text of a GGEP "PHC" packed UDP host caches extension value, and returns a List of IpPortImpl objects with the IP addresses and port numbers.
     * 
     * Takes the text value of the GGEP extension "PHC", packed UDP host caches.
     * http://www.the-gdf.org/wiki/index.php?title=UHC
     * This is text like this:
     * 
     * 27.174.162.220:6346&key=value&key2=value2\n
     * 24.3.65.216:2244&key=value&key2=value2\n
     * 69.181.203.230:6346&key=value&key2=value2\n
     * 
     * @param allCaches The text value of the GGEP extension "PHC" packed UDP host caches extension
     * @return          A list of IpPortImpl objects with the IP addresses and port numbers parsed from the text
     */
    private List listCaches(String allCaches) {

        // Each time we turn a line of text into an IpPortImpl object, we'll add it to this list
        List theCaches = new LinkedList();

        // Break the "PHC" text around "\n", and loop for line
        StringTokenizer st = new StringTokenizer(allCaches, "\n");
        while (st.hasMoreTokens()) {

            // Get this line of text like "host.example.com:port&key=value&key2=value2"
            String next = st.nextToken();

            // We just want the address and port number, clip out everything before the first "&"
            int i = next.indexOf("&");
            if (i != -1) next = next.substring(0, i);

            // Find the colon that separates the address and port number
            i = next.indexOf(":");

            // If we can't read the port, use the default Gnutella port of 6346
            int port = 6346;

            // The text starts with a colon or it's beyond the end (do)
            if (i == 0 || i == next.length()) {

                // Start the loop again with the next line
                continue;

            // Colon found
            } else if (i != -1) {

                try {

                    // Look beyond the colon, and read the port number
                    port = Integer.valueOf(next.substring(i + 1)).intValue();

                // If there was an error reading the port number, go to the start of the loop and try the next line
                } catch (NumberFormatException invalid) { continue; }

            // Colon not found, the text is just an address
            } else {

                // Move i to the end so next.substring(0, i) will work below
                i = next.length();
            }

            /*
             * At this point, if the colon was found, i points to it.
             * If there isn't a colon, i points beyond the end of the String.
             */

            // If the port number we read is 0 or too big to fit in 2 bytes, go to the start of the loop and try the next line
            if (!NetworkUtils.isValidPort(port)) continue;

            // Clip out the address before the colon
            String host = next.substring(0, i);

            try {

                // Make a new IpPortImpl from the address and port number, and add it to the theCaches list
                theCaches.add(new IpPortImpl(host, port));

                /*
                 * TODO:kfaaborg What happens when host is text like "www.site.com" and not an IP address in text like "1.2.3.4"
                 */

            // The IpPortImpl tried to turn the host text into a Java InetAddress object, and couldn't
            } catch (UnknownHostException invalid) { continue; }
        }

        // Return the list of IpPortImpl objects
        return Collections.unmodifiableList(theCaches);
    }

    /*
     * ////////////////////////// Pong Marking //////////////////////////
     */

    /**
     * See if the shared size number is specially formatted to indicate the computer this pong is about is an ultrapeer.
     * 
     * A pong contains information about a computer running Gnutella software.
     * It needs to tell if the computer is an ultrapeer or a leaf.
     * The ultrapeer system was developed after the pong payload was designed, so there's no room in the pong payload for this information.
     * So, Gnutella programs hide it in a clever way.
     * The standard pong payload includes the size of shared data in KB.
     * If the computer is an ultrapeer, this size will be a power of 2 that is 8 or bigger.
     * 
     * @return True if this pong is marked as an ultrapeer pong, false if it's not
     */
    public boolean isUltrapeer() {

        // Get the shared data size int from 10 bytes into the standard pong payload
        long kb = getKbytes();

        // If the number is less than 8, this pong does not have the ultrapeer marking
        if (kb < 8) return false;

        // Return true if the number is a power of 2
        return isPowerOf2(ByteOrder.long2int(kb));
    }

    /**
     * Determine if x is a power of 2.
     * This method has package access for testing.
     * 
     * @param x A number
     * @return  True if x is a power of 2, false if it's not
     */
    public static boolean isPowerOf2(int x) {

        // Look at the bits to determine if the number is a power of 2
        if (x <= 0) return false;              // Negative and 0 are not
        else        return (x & (x - 1)) == 0; // 4 is 100 and 3 is 011, 100 & 011 is 000
    }

    /**
     * Record in the program statistics that we're not going to forward this pong packet.
     * Adds this PingReply object to the TCP_PING_REPLIES DroppedSentMessageStatHandler.
     */
	public void recordDrop() {

        // Give this PingReply object to the DroppedSentMessageStatHandler for TCP pongs
		DroppedSentMessageStatHandler.TCP_PING_REPLIES.addMessage(this);
	}

    /**
     * Adjusts a KB count to the nearest power of two.
     * This indicates that the computer is an ultrapeer.
     * 
     * This is a sneaky way of hiding an additional bit of information in a field that still estimates the total shared size.
     * 
     * @param kbytes The number of KB of file data a computer is sharing
     * @return       The power of 2, like 6, 16, 32, 64 and so on, that is closest to the given value
     */
    private static long mark(long kbytes) {

        // Turn the number of KBs into an it
        int x = ByteOrder.long2int(kbytes); // If the long value is too big, x will be Integer.MAX_VALUE

        /*
         * Returns the power of two nearest to x.  TODO3: faster algorithms are
         * possible.  At the least, you can do binary search.  I imagine some bit
         * operations can be done as well.  This brute-force approach was
         * generated with the help of the the following Python program:
         * 
         *   for i in xrange(0, 32):
         *       low=1<<i
         *       high=1<<(i+1)
         *       split=(low+high)/2
         *       print "else if (x<%d)" % split
         *       print "    return %d; //1<<%d" % (low, i)
         */

        // Return the nearest power of 2 to the given number
        if      (x <        12) return          8; // 1 <<  3
        else if (x <        24) return         16; // 1 <<  4
        else if (x <        48) return         32; // 1 <<  5
        else if (x <        96) return         64; // 1 <<  6
        else if (x <       192) return        128; // 1 <<  7
        else if (x <       384) return        256; // 1 <<  8
        else if (x <       768) return        512; // 1 <<  9
        else if (x <      1536) return       1024; // 1 << 10
        else if (x <      3072) return       2048; // 1 << 11
        else if (x <      6144) return       4096; // 1 << 12
        else if (x <     12288) return       8192; // 1 << 13
        else if (x <     24576) return      16384; // 1 << 14
        else if (x <     49152) return      32768; // 1 << 15
        else if (x <     98304) return      65536; // 1 << 16
        else if (x <    196608) return     131072; // 1 << 17
        else if (x <    393216) return     262144; // 1 << 18
        else if (x <    786432) return     524288; // 1 << 19
        else if (x <   1572864) return    1048576; // 1 << 20
        else if (x <   3145728) return    2097152; // 1 << 21
        else if (x <   6291456) return    4194304; // 1 << 22
        else if (x <  12582912) return    8388608; // 1 << 23
        else if (x <  25165824) return   16777216; // 1 << 24
        else if (x <  50331648) return   33554432; // 1 << 25
        else if (x < 100663296) return   67108864; // 1 << 26
        else if (x < 201326592) return  134217728; // 1 << 27
        else if (x < 402653184) return  268435456; // 1 << 28
        else if (x < 805306368) return  536870912; // 1 << 29
        else                    return 1073741824; // 1 << 30
    }

    /**
     * Express this PingReply object as text.
     * This method overrides Object.toString().
     * 
     * @return A String with information from this PingReply object
     */
    public String toString() {

        // Compose and return the text
        return
            "PingReply(" + getAddress() + ":" + getPort() +
            ", free ultrapeers slots: " + hasFreeUltrapeerSlots() +
            ", free leaf slots: " + hasFreeLeafSlots() +
            ", vendor: " + VENDOR + " " + VENDOR_MAJOR_VERSION + "." + VENDOR_MINOR_VERSION +
            ", " + super.toString() +
            ", locale: " + CLIENT_LOCALE + ")";
    }

    /**
     * Get the IP address of the computer this pong has information about, read from the standard pong payload.
     * The IpPort interface requires this method.
     * 
     * @return The IP address as a Java InetAddress object
     */
    public InetAddress getInetAddress() {

        // Return the object we made from the address we read from the standard pong payload
        return IP;
    }

    /**
     * Get the IP address of the computer this pong is about, read from the "IP" GGEP extension.
     * The "IP" extension is used specifically when one computer pings another to see what its IP address looks like from the outside.
     * 
     * @return The IP address as a Java InetAddress object
     */
    public InetAddress getMyInetAddress() {

        // Return the object we made from the address we parsed from the "IP" GGEP header
        return MY_IP;
    }

    /**
     * Get the port number of the computer this pong is about, read from the "IP" GGEP extension.
     * The "IP" extension is used specifically when one computer pings another to see what its IP address looks like from the outside.
     * 
     * @return The port number
     */
    public int getMyPort() {

        // Return the number we read from the "IP" GGEP header
        return MY_PORT;
    }

    /**
     * Get the language preference of the computer this pong has information about.
     * This is part of the value of the "LOC" GGEP header.
     * 
     * @return A String like "en" for English
     */
    public String getClientLocale() {

        // Return the pong's language preference, like "en", which we parsed from the "LOC" GGEP extension
        return CLIENT_LOCALE;
    }

    /**
     * Get the number of additional connections the computer this pong is about wants that share its language preference.
     * 
     * @return The number of free locale preferenced slots
     */
    public int getNumFreeLocaleSlots() {

        // Return the pong's free slots for computers that match its language preference, which we parsed from the "LOC" GGEP extension
        return FREE_LOCALE_SLOTS;
    }

    /**
     * Determine if this pong is about a UDP host cache.
     * If the GGEP block has the "UDPHC" extension, it is.
     * 
     * @return True if this pong has information about a UDP host cache
     */
    public boolean isUDPHostCache() {

        // If the GGEP block has "UDPHC", we parsed its String value, return true
        return UDP_CACHE_ADDRESS != null;
    }

    /**
     * The address of the UDP host cache this pong packet is about.
     * This is the value of the GGEP "UDPHC" extension.
     * 
     * @return The String value of the "UDPHC" extension
     */
    public String getUDPCacheAddress() {

        // Return the String value we parsed from the "UDPHC" extension
        return UDP_CACHE_ADDRESS;
    }

    /*
     * Unit test: tests/com/limegroup/gnutella/messages/PingReplyTest
     */
}
