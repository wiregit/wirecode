
// Edited for the Learning branch

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
 * A ping reply message, aka, "pong".  This implementation provides a way
 * to "mark" pongs as being from supernodes.
 * 
 * 
 * 
 * 
 * 
 * 
 * A pong has 3 parts:
 * The 23 byte Gnutella header.
 * A 14 byte payload.
 * A GGEP block.
 * 
 * 
 * 
 * 
 * 
 */
public class PingReply extends Message implements Serializable, IpPort {

    //done

    /**
     * The IP addresses and port numbers in the pong packet of computers running Gnutella software.
     * Read from the "IPP" GGEP header.
     * This is an ArrayList of IpPortCombo objects.
     */
    private final List PACKED_IP_PORTS;

    /**
     * The IP addresses and port numbers in the pong packet of UDP host caches.
     * Read from the "PHC" GGEP header
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

    //do

    /** All the data.  We extract the port, ip address, number of files,
     *  and number of kilobytes lazily. */
    private final byte[] PAYLOAD;

    /** The IP string as extracted from payload[2..5].  Cached to avoid
     *  allocations.  LOCKING: obtain this' monitor. */
    private final InetAddress IP;

    /**
     * Constant for the port number of this pong.
     */
    private final int PORT;

    /** The IP address this pong says it's externally contactable at, read from the "IP" GGEP header. */
    private final InetAddress MY_IP;

    /** The port number this pong says it's externally contactable at, read from the "IP" GGEP header. */
    private final int MY_PORT;

    /**
     * Constant for the number of shared files reported in the pong.
     */
    private final long FILES;

    /**
     * Constant for the number of shared kilobytes reported in the pong.
     */
    private final long KILOBYTES;

    /** The number of seconds the pong says it's online on an average day, read from the "DU" GGEP header. */
    private final int DAILY_UPTIME;

    /** True if the pong says it supports UDP, indicated by the presence of the "GUE" GGEP header. */
    private final boolean SUPPORTS_UNICAST;

    /** The pong's vendor code like "LIME", read from the "VC" GGEP header. */
    private final String VENDOR;

    /** The pong's major version number, like 4 if the version is 4.9, read from the "VC" GGEP header. */
    private final int VENDOR_MAJOR_VERSION;

    /** The pong's minor version number, like 9 if the version is 4.9, read from the "VC" GGEP header. */
    private final int VENDOR_MINOR_VERSION;

    /** The QueryKey object (do), read from the "QK" GGEP header. */
    private final QueryKey QUERY_KEY;

    /** True if this pong has a GGEP block, and we've parsed it. */
    private final boolean HAS_GGEP_EXTENSION;

    //done

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
     * Make a new pong packet about us with the given GUID and TTL.
     * 
     * 
     * 
     * @param guid The GUID that will uniquely mark this new pong packet
     * @param ttl  The number of hops across the Internet that this pong packet will be able to travel
     */
    public static PingReply create(byte[] guid, byte ttl) {

        return create(guid, ttl, Collections.EMPTY_LIST);
    }

    /**
     * Creates a new <tt>PingReply</tt> for this host with the specified
     * GUID, TTL & packed hosts.
     */
    public static PingReply create(byte[] guid, byte ttl, Collection hosts) {
        return create(
            guid,
            ttl,
            RouterService.getPort(),
            RouterService.getAddress(),
            (long)RouterService.getNumSharedFiles(),
            (long)RouterService.getSharedFileSize()/1024,
            RouterService.isSupernode(),
            Statistics.instance().calculateDailyUptime(),
            UDPService.instance().isGUESSCapable(),
            ApplicationSettings.LANGUAGE.getValue().equals("") ?
                ApplicationSettings.DEFAULT_LOCALE.getValue() :
                ApplicationSettings.LANGUAGE.getValue(),
            RouterService.getConnectionManager()
                .getNumLimeWireLocalePrefSlots(),
            hosts);
    }
 
     /**
     * Creates a new PingReply for this host with the specified
     * GUID, TTL & return address.
     */   
    public static PingReply create(byte[] guid, byte ttl, IpPort addr) {
        return create(guid, ttl, addr, Collections.EMPTY_LIST);
    }
    
    
    /**
     * Creates a new PingReply for this host with the specified
     * GUID, TTL, return address & packed hosts.
     */
    public static PingReply create(byte[] guid, byte ttl, IpPort returnAddr, Collection hosts) {

        /*
         * Make the GGEP block for our pong packet with information about us.
         */

        // Make the GGEP block for our pong packet with information about us
        // Make the GGEP block for our pong packet with information about us
        
        // Adds the DU, GUE, UP, and VC tags
        GGEP ggep = newGGEP(Statistics.instance().calculateDailyUptime(), RouterService.isSupernode(), UDPService.instance().isGUESSCapable());

        // Get our language preference, like "en" for English, and add the LOC tag
        String locale = ApplicationSettings.LANGUAGE.getValue().equals("") ? ApplicationSettings.DEFAULT_LOCALE.getValue() : ApplicationSettings.LANGUAGE.getValue();
        addLocale(ggep, locale, RouterService.getConnectionManager().getNumLimeWireLocalePrefSlots());

        addAddress(ggep, returnAddr);
        addPackedHosts(ggep, hosts);

        return create(
            guid,
            ttl,
            RouterService.getPort(),
            RouterService.getAddress(),
            (long)RouterService.getNumSharedFiles(),
            (long)RouterService.getSharedFileSize() / 1024,
            RouterService.isSupernode(),
            ggep);
    }

    /**
     * Creates a new <tt>PingReply</tt> for this host with the specified
     * GUID, ttl, and query key.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param key the <tt>QueryKey</tt> for this reply
     */                                   
    public static PingReply createQueryKeyReply(byte[] guid, byte ttl, 
                                                QueryKey key) {
        return create(guid, ttl, 
                      RouterService.getPort(),
                      RouterService.getAddress(),
                      RouterService.getNumSharedFiles(),
                      RouterService.getSharedFileSize()/1024,
                      RouterService.isSupernode(),
                      qkGGEP(key));
    }

    /**
     * Creates a new <tt>PingReply</tt> for this host with the specified
     * GUID, ttl, and query key.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param key the <tt>QueryKey</tt> for this reply
     */                                   
    public static PingReply createQueryKeyReply(byte[] guid, byte ttl, 
                                                int port, byte[] ip,
                                                long sharedFiles, 
                                                long sharedSize,
                                                boolean ultrapeer,
                                                QueryKey key) {
        return create(guid, ttl, 
                      port,
                      ip,
                      sharedFiles,
                      sharedSize,
                      ultrapeer,
                      qkGGEP(key));
    }

    /**
     * Creates a new <tt>PingReply</tt> for an external node -- the data
     * in the reply will not contain data for this node.  In particular,
     * the data fields are set to zero because we do not know these
     * statistics for the other node.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param port the port the remote host is listening on
     * @param address the address of the node
     */
    public static PingReply 
        create(byte[] guid, byte ttl, int port, byte[] address) {
        return create(guid, ttl, port, address, 0, 0, false, -1, false); 
    }

    /**
     * Creates a new <tt>PingReply</tt> for an external node -- the data
     * in the reply will not contain data for this node.  In particular,
     * the data fields are set to zero because we do not know these
     * statistics for the other node.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param port the port the remote host is listening on
     * @param address the address of the node
     * @param ultrapeer whether or not we should mark this node as
     *  being an Ultrapeer
     */
    public static PingReply 
        createExternal(byte[] guid, byte ttl, int port, byte[] address,
                       boolean ultrapeer) {
        return create(guid, ttl, port, address, 0, 0, ultrapeer, -1, false); 
    }

    /**
     * Creates a new <tt>PingReply</tt> for an external node -- the data
     * in the reply will not contain data for this node.  In particular,
     * the data fields are set to zero because we do not know these
     * statistics for the other node.  This is primarily used for testing.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param port the port the remote host is listening on
     * @param address the address of the node
     * @param ultrapeer whether or not we should mark this node as
     *  being an Ultrapeer
     */
    public static PingReply 
        createExternal(byte[] guid, byte ttl, int port, byte[] address,
                       int uptime,
                       boolean ultrapeer) {
        return create(guid, ttl, port, address, 0, 0, ultrapeer, uptime, false); 
    }

    /**
     * Creates a new <tt>PingReply</tt> instance for a GUESS node.  This
     * method should only be called if the caller is sure that the given
     * node is, in fact, a GUESS-capable node.  This method is only used
     * to create pongs for nodes other than ourselves.  
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param ep the <tt>Endpoint</tt> instance containing data about 
     *  the remote host
     */       
    public static PingReply 
        createGUESSReply(byte[] guid, byte ttl, Endpoint ep) 
        throws UnknownHostException {
        return create(guid, ttl,
                      ep.getPort(),
                      ep.getHostBytes(),
                      0, 0, true, -1, true);        
    }

    /**
     * Creates a new <tt>PingReply</tt> instance for a GUESS node.  This
     * method should only be called if the caller is sure that the given
     * node is, in fact, a GUESS-capable node.  This method is only used
     * to create pongs for nodes other than ourselves.  Given that this
     * reply is for a remote node, we do not know the data for number of
     * shared files, etc, and so leave it blank.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param port the port the remote host is listening on
     * @param address the address of the node
     */
    public static PingReply 
        createGUESSReply(byte[] guid, byte ttl, int port, byte[] address) {
        return create(guid, ttl, port, address, 0, 0, true, -1, true); 
    }

    /**
     * Creates a new pong with the specified data -- used primarily for
     * testing!
     *
     * @param guid the sixteen byte message GUID
     * @param ttl the message TTL to use
     * @param port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @param ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  format e.g. {18, 239, 0, 144}.
     * @param files the number of files I'm sharing.  Must fit in 4 unsigned
     *  bytes, i.e., 0 < files < 2^32.
     * @param kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned bytes, i.e., 0 < files < 2^32.
     */
    public static PingReply 
        create(byte[] guid, byte ttl,
               int port, byte[] ip, long files, long kbytes) {
        return create(guid, ttl, port, ip, files, kbytes, 
                      false, -1, false); 
    }


    /**
     * Creates a new ping from scratch with ultrapeer and daily uptime extension
     * data.
     *
     * @param guid the sixteen byte message GUID
     * @param ttl the message TTL to use
     * @param port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @param ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  format e.g. {18, 239, 0, 144}.
     * @param files the number of files I'm sharing.  Must fit in 4 unsigned
     *  bytes, i.e., 0 < files < 2^32.
     * @param kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned bytes, i.e., 0 < files < 2^32.
     * @param isUltrapeer true if this should be a marked ultrapeer pong,
     *  which sets kbytes to the nearest power of 2 not less than 8.
     * @param dailyUptime my average daily uptime, in seconds, e.g., 
     *  3600 for one hour per day.  Negative values mean "don't know".
     *  GGEP extension blocks are allocated if dailyUptime is non-negative.  
     */
    public static PingReply create(byte[] guid, byte ttl, int port, byte[] ip, long files, long kbytes, boolean isUltrapeer, int dailyUptime, boolean isGUESSCapable) {

        return create(guid, ttl, port, ip, files, kbytes, isUltrapeer,
            newGGEP(dailyUptime, isUltrapeer, isGUESSCapable)); // Make the GGEP block for our pong packet with information about us
    }

    /**
     * Creates a new PingReply with the specified data.
     */
    public static PingReply create(byte[] guid, byte ttl, int port, byte[] ip, long files, long kbytes, boolean isUltrapeer, int dailyUptime, boolean isGuessCapable, String locale, int slots) {

        return create(guid, ttl, port, ip, files, kbytes, isUltrapeer, dailyUptime, isGuessCapable, locale, slots, Collections.EMPTY_LIST);
    }
    
    /**
     * creates a new PingReply with the specified locale
     *
     * @param guid the sixteen byte message GUID
     * @param ttl the message TTL to use
     * @param port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @param ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  format e.g. {18, 239, 0, 144}.
     * @param files the number of files I'm sharing.  Must fit in 4 unsigned
     *  bytes, i.e., 0 < files < 2^32.
     * @param kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned bytes, i.e., 0 < files < 2^32.
     * @param isUltrapeer true if this should be a marked ultrapeer pong,
     *  which sets kbytes to the nearest power of 2 not less than 8.
     * @param dailyUptime my average daily uptime, in seconds, e.g., 
     *  3600 for one hour per day.  Negative values mean "don't know".
     *  GGEP extension blocks are allocated if dailyUptime is non-negative.  
     * @param isGuessCapable guess capable
     * @param locale the locale 
     * @param slots the number of locale preferencing slots available
     * @param hosts the hosts to pack into this PingReply
     */
    public static PingReply create(byte[] guid, byte ttl, int port, byte[] ip, long files, long kbytes, boolean isUltrapeer, int dailyUptime, boolean isGuessCapable, String locale, int slots, Collection hosts) {

        // Make the GGEP block for our pong packet with information about us
        GGEP ggep = newGGEP(dailyUptime, isUltrapeer, isGuessCapable);

        addLocale(ggep, locale, slots); // Add the "LOC" tag
        addPackedHosts(ggep, hosts);
        return create(guid, ttl, port, ip, files, kbytes, isUltrapeer, ggep);
    }

    /**
     * Returns a new <tt>PingReply</tt> instance with all the same data
     * as <tt>this</tt>, but with the specified GUID.
     *
     * @param guid the guid to use for the new <tt>PingReply</tt>
     * @return a new <tt>PingReply</tt> instance with the specified GUID
     *  and all of the data from this <tt>PingReply</tt>
     * @throws IllegalArgumentException if the guid is not 16 bytes or the input
     * (this') format is bad
     */
    public PingReply mutateGUID(byte[] guid) {
        if (guid.length != 16)
            throw new IllegalArgumentException("bad guid size: " + guid.length);

        // i can't just call a new constructor, i have to recreate stuff
        try {
            return createFromNetwork(guid, getTTL(), getHops(), PAYLOAD); 
        }
        catch (BadPacketException ioe) {
            throw new IllegalArgumentException("Input pong was bad!");
        }

    }

    //done

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
        payload[2]=ipBytes[0];                        // Next, write the IP address in big endian order
        payload[3]=ipBytes[1];
        payload[4]=ipBytes[2];
        payload[5]=ipBytes[3];
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
        QueryKey    key                = null;
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

            // "QK" QueryKey
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
        QUERY_KEY = key;

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
        GGEP ggep = new GGEP(true); // True, let tags have null values

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

    //do

    /** Returns the GGEP payload bytes to encode the given QueryKey */
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

    //done

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

    //do

    /**
     * Adds the address GGEP.
     */
    private static GGEP addAddress(GGEP ggep, IpPort address) {
        byte[] payload = new byte[6];
        System.arraycopy(address.getInetAddress().getAddress(), 0, payload, 0, 4);
        ByteOrder.short2leb((short)address.getPort(), payload, 4);
        ggep.put(GGEP.GGEP_HEADER_IPPORT,payload);
        return ggep;
    }
    
    /**
     * Adds the packed hosts into this GGEP.
     */
    private static GGEP addPackedHosts(GGEP ggep, Collection hosts) {
        if(hosts == null || hosts.isEmpty())
            return ggep;
            
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, NetworkUtils.packIpPorts(hosts));
        return ggep;
    }

    //done

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

    //do

    /**
     * Returns whether or not this pong is reporting any free slots on the 
     * remote host, either leaf or ultrapeer.
     * 
     * @return <tt>true</tt> if the remote host has any free leaf or ultrapeer
     *  slots, otherwise <tt>false</tt>
     */
    public boolean hasFreeSlots() {
        return hasFreeLeafSlots() || hasFreeUltrapeerSlots();    
    }
    
    /**
     * Returns whether or not this pong is reporting free leaf slots on the 
     * remote host.
     * 
     * @return <tt>true</tt> if the remote host has any free leaf slots, 
     *  otherwise <tt>false</tt>
     */
    public boolean hasFreeLeafSlots() {
        return FREE_LEAF_SLOTS > 0;
    }

    /**
     * Returns whether or not this pong is reporting free ultrapeer slots on  
     * the remote host.
     * 
     * @return <tt>true</tt> if the remote host has any free ultrapeer slots, 
     *  otherwise <tt>false</tt>
     */
    public boolean hasFreeUltrapeerSlots() {
        return FREE_ULTRAPEER_SLOTS > 0;
    }
    
    /**
     * Accessor for the number of free leaf slots reported by the remote host.
     * This will return -1 if the remote host did not include the necessary 
     * GGEP block reporting slots.
     * 
     * @return the number of free leaf slots, or -1 if the remote host did not
     *  include this information
     */
    public int getNumLeafSlots() {
        return FREE_LEAF_SLOTS;
    }

    /**
     * Accessor for the number of free ultrapeer slots reported by the remote 
     * host.  This will return -1 if the remote host did not include the  
     * necessary GGEP block reporting slots.
     * 
     * @return the number of free ultrapeer slots, or -1 if the remote host did 
     *  not include this information
     */    
    public int getNumUltrapeerSlots() {
        return FREE_ULTRAPEER_SLOTS;
    }

    protected void writePayload(OutputStream out) throws IOException {
        out.write(PAYLOAD);
		SentMessageStatHandler.TCP_PING_REPLIES.addMessage(this);
    }

    /**
     * Accessor for the port reported in this pong.
     *
     * @return the port number reported in the pong
     */
    public int getPort() {
        return PORT;
    }

    /**
     * Returns the ip field in standard dotted decimal format, e.g.,
     * "127.0.0.1".  The most significant byte is written first.
     */
    public String getAddress() { 
        return IP.getHostAddress();
    }

    /**
     * Returns the ip address bytes (MSB first)
     */
    public byte[] getIPBytes() {
        byte[] ip=new byte[4];
        ip[0]=PAYLOAD[2];
        ip[1]=PAYLOAD[3];
        ip[2]=PAYLOAD[4];
        ip[3]=PAYLOAD[5];
        
        return ip;
    }
    
    /**
     * Accessor for the number of files shared, as reported in the
     * pong.
     *
     * @return the number of files reported shared
     */
    public long getFiles() {
        return FILES;
    }

    /**
     * Accessor for the number of kilobytes shared, as reported in the
     * pong.
     *
     * @return the number of kilobytes reported shared
     */
    public long getKbytes() {
        return KILOBYTES;
    }

    /** Returns the average daily uptime in seconds from the GGEP payload.
     *  If the pong did not report a daily uptime, returns -1.
     *
     * @return the daily uptime reported in the pong, or -1 if the uptime
     *  was not present or could not be read
     */
    public int getDailyUptime() {
        return DAILY_UPTIME;
    }


    /** Returns whether or not this host support unicast, GUESS-style
     *  queries.
     *
     * @return <tt>true</tt> if this host does support GUESS-style queries,
     *  otherwise <tt>false</tt>
     */
    public boolean supportsUnicast() {
        return SUPPORTS_UNICAST;
    }


    /** Returns the 4-character vendor string associated with this Pong.
     *
     * @return the 4-character vendor code reported in the pong, or the
     *  empty string if no vendor code was successfully read
     */
    public String getVendor() {
        return VENDOR;
    }


    /** Returns the major version number of the vendor returning this pong.
     * 
     * @return the major version number of the vendor returning this pong,
     *  or -1 if the version could not be read
     */
    public int getVendorMajorVersion() {
        return VENDOR_MAJOR_VERSION;
    }

    /** Returns the minor version number of the vendor returning this pong.
     * 
     * @return the minor version number of the vendor returning this pong,
     *  or -1 if the version could not be read
     */
    public int getVendorMinorVersion() {
        return VENDOR_MINOR_VERSION;
    }


    /** Returns the QueryKey (if any) associated with this pong.  May be null!
     *
     * @return the <tt>QueryKey</tt> for this pong, or <tt>null</tt> if no
     *  key was specified
     */
    public QueryKey getQueryKey() {
        return QUERY_KEY;
    }
    
    /**
     * Gets the list of packed IP/Ports.
     */
    public List /* of IpPort */ getPackedIPPorts() {
        return PACKED_IP_PORTS;
    }
    
    /**
     * Gets a list of packed IP/Ports of UDP Host Caches.
     */
    public List /* of IpPort */ getPackedUDPHostCaches() {
        return PACKED_UDP_HOST_CACHES;
    }

    /**
     * Returns whether or not this pong has a GGEP extension.
     *
     * @return <tt>true</tt> if the pong has a GGEP extension, otherwise
     *  <tt>false</tt>
     */
    public boolean hasGGEPExtension() {
        return HAS_GGEP_EXTENSION;
    }

    //done
    
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

    //do
    
    
    // inherit doc comment from message superclass
    public Message stripExtendedPayload() {
        //TODO: if this is too slow, we can alias parts of this, as as the
        //payload.  In fact we could even return a subclass of PingReply that
        //simply delegates to this.
        byte[] newPayload=new byte[STANDARD_PAYLOAD_SIZE];
        System.arraycopy(PAYLOAD, 0,
                         newPayload, 0,
                         STANDARD_PAYLOAD_SIZE);

        return new PingReply(this.getGUID(), this.getTTL(), this.getHops(),
                             newPayload, null, IP);
    }
    
    /**
     * Unzips data about UDP host caches & returns a list of'm.
     */
    private List listCaches(String allCaches) {
        List theCaches = new LinkedList();
        StringTokenizer st = new StringTokenizer(allCaches, "\n");
        while(st.hasMoreTokens()) {
            String next = st.nextToken();
            // look for possible features and ignore'm
            int i = next.indexOf("&");
            // basically ignore.
            if(i != -1)
                next = next.substring(0, i);
            i = next.indexOf(":");
            int port = 6346;
            if(i == 0 || i == next.length()) {
                continue;
            } else if(i != -1) {
                try {
                    port = Integer.valueOf(next.substring(i+1)).intValue();
                } catch(NumberFormatException invalid) {
                    continue;
                }
            } else {
                i = next.length(); // setup for i-1 below.
            }
            if(!NetworkUtils.isValidPort(port))
                continue;
            String host = next.substring(0, i);
            try {
                theCaches.add(new IpPortImpl(host, port));
            } catch(UnknownHostException invalid) {
                continue;
            }
        }
        return Collections.unmodifiableList(theCaches);
    }


    ////////////////////////// Pong Marking //////////////////////////

    /** 
     * Returns true if this message is "marked", i.e., likely from an
     * Ultrapeer. 
     *
     * @return <tt>true</tt> if this pong is marked as an Ultrapeer pong,
     *  otherwise <tt>false</tt>
     */
    public boolean isUltrapeer() {
        //Returns true if kb is a power of two greater than or equal to eight.
        long kb = getKbytes();
        if (kb < 8)
            return false;
        return isPowerOf2(ByteOrder.long2int(kb));
    }

    public static boolean isPowerOf2(int x) {  //package access for testability
        if (x<=0)
            return false;
        else
            return (x&(x - 1)) == 0;
    }

	// inherit doc comment
	public void recordDrop() {
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

    // overrides Object.toString
    public String toString() {
        return "PingReply("+getAddress()+":"+getPort()+
            ", free ultrapeers slots: "+hasFreeUltrapeerSlots()+
            ", free leaf slots: "+hasFreeLeafSlots()+
            ", vendor: "+VENDOR+" "+VENDOR_MAJOR_VERSION+"."+
                VENDOR_MINOR_VERSION+
            ", "+super.toString()+
            ", locale : " + CLIENT_LOCALE + ")";
    }

    /**
     * Implements <tt>IpPort</tt> interface.  Returns the <tt>InetAddress</tt>
     * for this host.
     * 
     * @return the <tt>InetAddress</tt> for this host
     */ 
    public InetAddress getInetAddress() {
        return IP;
    }

    public InetAddress getMyInetAddress() {
        return MY_IP;
    }
    
    public int getMyPort() {
        return MY_PORT;
    }
    
    /**
     * access the client_locale
     */
    public String getClientLocale() {
        return CLIENT_LOCALE;
    }

    public int getNumFreeLocaleSlots() {
        return FREE_LOCALE_SLOTS;
    }
    
    /**
     * Accessor for host cacheness.
     */
    public boolean isUDPHostCache() {
        return UDP_CACHE_ADDRESS != null;
    }
    
    /**
     * Gets the UDP host cache address.
     */
    public String getUDPCacheAddress() {
        return UDP_CACHE_ADDRESS;
    }

    //Unit test: tests/com/limegroup/gnutella/messages/PingReplyTest
}
