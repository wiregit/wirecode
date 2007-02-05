package com.limegroup.gnutella.messages;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkUtils;
import org.limewire.security.QueryKey;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.Statistics;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * A ping reply message, aka, "pong".  This implementation provides a way
 * to "mark" pongs as being from supernodes.
 */
public class PingReply extends Message implements Serializable, IpPort {
    
    /**
     * The list of extra Gnutella ip/ports contained in this reply.
     */
    private final List<IpPort> PACKED_IP_PORTS;
    
    /**
     * The list of extra DHT ip/ports contained in this reply.
     */
    private final List<IpPort> PACKED_DHT_IP_PORTS;
    
    /**
     * The list of extra ip/ports contained in this reply.
     */
    private final List<IpPort> PACKED_UDP_HOST_CACHES;

    /**
     * The IP address to connect to if this is a UDP host cache.
     * Null if this is not a UDP host cache.
     */
    private final String UDP_CACHE_ADDRESS;

    /**
     * Constant for the number of ultrapeer slots for this host.
     */
    private final int FREE_ULTRAPEER_SLOTS;

    /**
     * Constant for the number of free leaf slots for this host.
     */
    private final int FREE_LEAF_SLOTS;

    /**
     * Constant for the standard size of the pong payload.
     */
    public static final int STANDARD_PAYLOAD_SIZE = 14;
    
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
    
    /**
     * The address this pong claims to be my external address
     */
    private final InetAddress MY_IP;
    
    /**
     * The port this pong claims to be my external port
     */
    private final int MY_PORT;

    /**
     * Constant for the number of shared files reported in the pong.
     */
    private final long FILES;

    /**
     * Constant for the number of shared kilobytes reported in the pong.
     */
    private final long KILOBYTES;

    /**
     * Constant int for the daily average uptime.
     */
    private final int DAILY_UPTIME;

    /**
     * Constant for whether or not the remote node supports unicast.
     */
    private final boolean SUPPORTS_UNICAST;

    /**
     * Constant for the vendor of the remote host.
     */
    private final String VENDOR;

    /**
     * Constant for the major version number reported in the vendor block.
     */
    private final int VENDOR_MAJOR_VERSION;

    /**
     * Constant for the minor version number reported in the vendor block.
     */
    private final int VENDOR_MINOR_VERSION;

    /**
     * Constant for the query key reported for the pong.
     */
    private final QueryKey QUERY_KEY;
    
    /**
     * Constant for the DHT Version. 
     */
    private final int DHT_VERSION;
    
    /**
     * Contant for the DHT mode (active/passive/none)
     */
    private final DHTMode DHT_MODE;

    /**
     * Constant boolean for whether or not this pong contains any GGEP
     * extensions.
     */
    private final boolean HAS_GGEP_EXTENSION;

    /**
     * Cached constant for the vendor GGEP extension.
     */
    private static final byte[] CACHED_VENDOR = new byte[5];
    
    // performs any necessary static initialization of fields,
    // such as the vendor GGEP extension
    static {
        // set 'LIME'
        System.arraycopy(LimeWireUtils.QHD_VENDOR_NAME.getBytes(),
                         0, CACHED_VENDOR, 0,
                         LimeWireUtils.QHD_VENDOR_NAME.getBytes().length);
        CACHED_VENDOR[4] = convertToGUESSFormat(LimeWireUtils.getMajorVersionNumber(),
                                         LimeWireUtils.getMinorVersionNumber());
    }

    /**
     * Constant for the locale
     */
    private String CLIENT_LOCALE;
    
    /**
     * the number of free preferenced slots 
     */
    private int FREE_LOCALE_SLOTS;

    /**
     * Creates a new <tt>PingReply</tt> for this host with the specified
     * GUID and ttl.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     */
    public static PingReply create(byte[] guid, byte ttl) {
        return create(guid, ttl, IpPort.EMPTY_LIST, IpPort.EMPTY_LIST);
    }
    
    /**
     * Creates a new <tt>PingReply</tt> for this host with the specified
     * GUID, TTL & packed hosts.
     */
    public static PingReply create(byte[] guid, byte ttl, Collection<? extends IpPort> gnutHosts, 
            Collection<? extends IpPort> dhtHosts) {
        return create(
            guid,
            ttl,
            RouterService.getPort(),
            RouterService.getAddress(),
            RouterService.getNumSharedFiles(),
            RouterService.getSharedFileSize()/1024,
            RouterService.isSupernode(),
            Statistics.instance().calculateDailyUptime(),
            UDPService.instance().isGUESSCapable(),
            ApplicationSettings.LANGUAGE.getValue().equals("") ?
                ApplicationSettings.DEFAULT_LOCALE.getValue() :
                ApplicationSettings.LANGUAGE.getValue(),
            RouterService.getConnectionManager()
                .getNumLimeWireLocalePrefSlots(),
            gnutHosts, dhtHosts);
    }
 
     /**
     * Creates a new PingReply for this host with the specified
     * GUID, TTL & return address.
     */ 
    public static PingReply create(byte[] guid, byte ttl, IpPort addr) {
        return create(guid, ttl, addr, IpPort.EMPTY_LIST, IpPort.EMPTY_LIST);
    }
    
    
    /**
     * Creates a new PingReply for this host with the specified
     * GUID, TTL, return address & packed hosts. Either collection 
     * of hosts can be null!
     */
    public static PingReply create(byte[] guid, byte ttl,
                                   IpPort returnAddr, Collection<? extends IpPort> gnutHosts, 
                                   Collection<? extends IpPort> dhtHosts) {
        GGEP ggep = newGGEP(Statistics.instance().calculateDailyUptime(),
                            RouterService.isSupernode(),
                            UDPService.instance().isGUESSCapable());
                            
        String locale = ApplicationSettings.LANGUAGE.getValue().equals("") ?
                        ApplicationSettings.DEFAULT_LOCALE.getValue() :
                        ApplicationSettings.LANGUAGE.getValue();
        addLocale(ggep, locale, RouterService.getConnectionManager()
                                        .getNumLimeWireLocalePrefSlots());
                                        
        addAddress(ggep, returnAddr);
        
        addPackedHosts(ggep, gnutHosts, dhtHosts);
        
        return create(guid,
                      ttl,
                      RouterService.getPort(),
                      RouterService.getAddress(),
                      RouterService.getNumSharedFiles(),
                      RouterService.getSharedFileSize()/1024,
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
    public static PingReply 
        create(byte[] guid, byte ttl,
               int port, byte[] ip, long files, long kbytes,
               boolean isUltrapeer, int dailyUptime, boolean isGUESSCapable) {
        return create(guid, ttl, port, ip, files, kbytes, isUltrapeer,
                      newGGEP(dailyUptime, isUltrapeer, isGUESSCapable));
    }
    
    /**
     * Creates a new PingReply with the specified data.
     */
    public static PingReply create(byte[] guid, byte ttl,
      int port, byte[] ip, long files, long kbytes,
      boolean isUltrapeer, int dailyUptime, boolean isGuessCapable,
      String locale, int slots) {    
        return create(guid, ttl, port, ip, files, kbytes, isUltrapeer,
                      dailyUptime, isGuessCapable, locale, slots, IpPort.EMPTY_LIST,
                      IpPort.EMPTY_LIST);
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
     * @param gnutHosts the Gnutella hosts to pack into this PingReply
     * @param dhtHosts the DHT hosts to pack into this PingReply
     */
    public static PingReply
        create(byte[] guid, byte ttl,
               int port, byte[] ip, long files, long kbytes,
               boolean isUltrapeer, int dailyUptime, boolean isGuessCapable,
               String locale, int slots, Collection<? extends IpPort> gnutHosts, 
               Collection<? extends IpPort> dhtHosts) {
        GGEP ggep = newGGEP(dailyUptime, isUltrapeer, isGuessCapable);
        addLocale(ggep, locale, slots);
        addPackedHosts(ggep, gnutHosts , dhtHosts);
        return create(guid,
                      ttl,
                      port,
                      ip,
                      files,
                      kbytes,
                      isUltrapeer,
                      ggep);
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

    /**
     * Creates a new <tt>PingReply</tt> instance with the specified
     * criteria.
     *
     * @return a new <tt>PingReply</tt> instance containing the specified
     *  data
     */
    public static PingReply 
        create(byte[] guid, byte ttl, int port, byte[] ipBytes, long files,
               long kbytes, boolean isUltrapeer, GGEP ggep) {

 		if(!NetworkUtils.isValidPort(port))
			throw new IllegalArgumentException("invalid port: "+port);
        if(!NetworkUtils.isValidAddress(ipBytes))
            throw new IllegalArgumentException("invalid address: " +
                    NetworkUtils.ip2string(ipBytes));			
        
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(NetworkUtils.ip2string(ipBytes));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        byte[] extensions = null;
        if(ggep != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ggep.write(baos);
            } catch(IOException e) {
                // this should not happen
                ErrorService.error(e);
            }
            extensions = baos.toByteArray();
        }
        int length = STANDARD_PAYLOAD_SIZE + 
            (extensions == null ? 0 : extensions.length);

        byte[] payload = new byte[length];
        //It's ok if casting port, files, or kbytes turns negative.
        ByteOrder.short2leb((short)port, payload, 0);
        //payload stores IP in BIG-ENDIAN
        payload[2]=ipBytes[0];
        payload[3]=ipBytes[1];
        payload[4]=ipBytes[2];
        payload[5]=ipBytes[3];
        ByteOrder.int2leb((int)files, payload, 6);
        ByteOrder.int2leb((int) (isUltrapeer ? mark(kbytes) : kbytes), 
                          payload, 
                          10);
        
        //Encode GGEP block if included.
        if (extensions != null) {
            System.arraycopy(extensions, 0, 
                             payload, STANDARD_PAYLOAD_SIZE, 
                             extensions.length);
        }
        return new PingReply(guid, ttl, (byte)0, payload, ggep, ip);
    }


    /**
     * Creates a new <tt>PingReply</tt> instance from the network.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param hops the hops for this message
     * @param payload the message payload
     * @throws <tt>BadPacketException</tt> if the message is invalid for
     *  any reason
     */
    public static PingReply 
        createFromNetwork(byte[] guid, byte ttl, byte hops, byte[] payload) 
        throws BadPacketException {
        if(guid == null) {
            throw new NullPointerException("null guid");
        }
        if(payload == null) {
            throw new NullPointerException("null payload");
        }
        if (payload.length < STANDARD_PAYLOAD_SIZE) {
            ReceivedErrorStat.PING_REPLY_INVALID_PAYLOAD.incrementStat();
            throw new BadPacketException("invalid payload length");   
        }
        int port = ByteOrder.ushort2int(ByteOrder.leb2short(payload,0));
 		if(!NetworkUtils.isValidPort(port)) {
 		    ReceivedErrorStat.PING_REPLY_INVALID_PORT.incrementStat();
			throw new BadPacketException("invalid port: "+port); 
        }
 		
 		// this address may get updated if we have the UDPHC extention
 		// therefore it is checked after checking for that extention.
        String ipString = NetworkUtils.ip2string(payload, 2);
        
        InetAddress ip = null;
        
        GGEP ggep = parseGGEP(payload);
        
        if(ggep != null) {
            if(ggep.hasKey(GGEP.GGEP_HEADER_VENDOR_INFO)) {
                byte[] vendorBytes = null;
                try {
                    vendorBytes = ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO);
                } catch (BadGGEPPropertyException e) {
                    ReceivedErrorStat.PING_REPLY_INVALID_GGEP.incrementStat();
                    throw new BadPacketException("bad GGEP: "+vendorBytes);
                }
                if(vendorBytes.length < 4) {
                    ReceivedErrorStat.PING_REPLY_INVALID_VENDOR.incrementStat();
                    throw new BadPacketException("invalid vendor length: "+
                                                 vendorBytes.length);
                }
            }

            if(ggep.hasKey(GGEP.GGEP_HEADER_CLIENT_LOCALE)) {
                try {
                    ggep.getBytes(GGEP.GGEP_HEADER_CLIENT_LOCALE);
                } catch(BadGGEPPropertyException e) {
                    ReceivedErrorStat.PING_REPLY_INVALID_GGEP.incrementStat();
                    throw new BadPacketException("GGEP error : creating from"
                                                 + " network : client locale");
                }
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_PACKED_IPPORTS)) {
                byte[] data = null;
                try {
                    data = ggep.getBytes(GGEP.GGEP_HEADER_PACKED_IPPORTS);
                } catch(BadGGEPPropertyException bad) {
                    throw new BadPacketException(bad.getMessage());
                }
                if(data == null || data.length % 6 != 0)
                    throw new BadPacketException("invalid data");
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_PACKED_HOSTCACHES)) {
                try {
                    ggep.getBytes(GGEP.GGEP_HEADER_PACKED_HOSTCACHES);
                } catch(BadGGEPPropertyException bad) {
                    throw new BadPacketException(bad.getMessage());
                }
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_UDP_HOST_CACHE)) {
                try{
                    String dns = ggep.getString(GGEP.GGEP_HEADER_UDP_HOST_CACHE);
                    ip = InetAddress.getByName(dns);
                    ipString = ip.getHostAddress();
                }catch(BadGGEPPropertyException ignored) {
                }catch(UnknownHostException bad) {
                    throw new BadPacketException(bad.getMessage());
                }
            }
                
        }

        if(!NetworkUtils.isValidAddress(ipString)) {
            ReceivedErrorStat.PING_REPLY_INVALID_ADDRESS.incrementStat();
            throw new BadPacketException("invalid address: " + ipString);
        }
        
        if (ip==null) {
            try {
                ip = InetAddress.getByName(NetworkUtils.ip2string(payload, 2));
            } catch (UnknownHostException e) {
                throw new BadPacketException("bad IP:"+ipString+" "+e.getMessage());
            }
        }
        return new PingReply(guid, ttl, hops, payload, ggep, ip);
    }
     
    /**
     * Sole <tt>PingReply</tt> constructor.  This establishes all ping
     * reply invariants.
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param hops the hops for this message
     * @param payload the message payload
     */
    private PingReply(byte[] guid, byte ttl, byte hops, byte[] payload,
                      GGEP ggep, InetAddress ip) {
        super(guid, Message.F_PING_REPLY, ttl, hops, payload.length);
        PAYLOAD = payload;
        PORT = ByteOrder.ushort2int(ByteOrder.leb2short(PAYLOAD,0));
        FILES = ByteOrder.uint2long(ByteOrder.leb2int(PAYLOAD,6));
        KILOBYTES = ByteOrder.uint2long(ByteOrder.leb2int(PAYLOAD,10));

        IP = ip;

        // GGEP parsing
        //GGEP ggep = parseGGEP();
        int dailyUptime = -1;
        boolean supportsUnicast = false;
        String vendor = "";
        int vendorMajor = -1;
        int vendorMinor = -1;
        
        int freeLeafSlots = -1;
        int freeUltrapeerSlots = -1;
        QueryKey key = null;
        
        String locale /** def. val from settings? */
            = ApplicationSettings.DEFAULT_LOCALE.getValue(); 
        int slots = -1; //-1 didn't get it.
        InetAddress myIP=null;
        int myPort=0;
        List<IpPort> packedIPs = Collections.emptyList();
        List<IpPort> packedDHTIPs = Collections.emptyList();
        List<IpPort> packedCaches = Collections.emptyList();
        String cacheAddress = null;
        
        int dhtVersion = -1;
        DHTMode dhtMode = null;
        
        // TODO: the exceptions thrown here are messy
        if(ggep != null) {
            if(ggep.hasKey(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME)) {
                try {
                    dailyUptime = 
                        ggep.getInt(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME); 
                } catch(BadGGEPPropertyException e) {}
            }

            supportsUnicast = 
                ggep.hasKey(GGEP.GGEP_HEADER_UNICAST_SUPPORT); 

            if(ggep.hasKey(GGEP.GGEP_HEADER_VENDOR_INFO)) {
                try {
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO);
                    if(bytes.length >= 4)
                        vendor = new String(bytes, 0, 4);   
                    if(bytes.length > 4) {
                        vendorMajor = bytes[4] >> 4;
                        vendorMinor = bytes[4] & 0xF;
                    }
                } catch (BadGGEPPropertyException e) {}
             }

            if (ggep.hasKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT)) {
                try {
                    byte[] bytes = 
                        ggep.getBytes(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
                    if(QueryKey.isValidQueryKeyBytes(bytes))
                        key = new QueryKey(bytes, false);
                } catch (BadGGEPPropertyException corrupt) {}
            }
            
            if(ggep.hasKey((GGEP.GGEP_HEADER_UP_SUPPORT))) {
                try {
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_UP_SUPPORT);
                    if(bytes.length >= 3) {
                        freeLeafSlots = bytes[1];
                        freeUltrapeerSlots = bytes[2];
                    }
                } catch (BadGGEPPropertyException e) {}
            }
            
            if(ggep.hasKey((GGEP.GGEP_HEADER_DHT_SUPPORT))) {
                try {
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_DHT_SUPPORT);
                    if(bytes.length >= 3) {
                        dhtVersion = ByteOrder.ushort2int(ByteOrder.beb2short(bytes, 0));
                        byte mode = (byte)(bytes[2] & DHTMode.DHT_MODE_MASK);
                        dhtMode = DHTMode.valueOf(mode);
                        if (dhtMode == null) {
                            // Reset the Version number if the mode
                            // is unknown
                            dhtVersion = -1;
                        }
                    }
                } catch (BadGGEPPropertyException e) {}
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_CLIENT_LOCALE)) {
                try {
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_CLIENT_LOCALE);
                    if(bytes.length >= 2)
                        locale = new String(bytes, 0, 2);
                    if(bytes.length >= 3)
                        slots = ByteOrder.ubyte2int(bytes[2]);
                } catch(BadGGEPPropertyException e) {}
            }
            
            if (ggep.hasKey(GGEP.GGEP_HEADER_IPPORT)) {
                try{
                    byte[] data = ggep.getBytes(GGEP.GGEP_HEADER_IPPORT);

                    byte [] myip = new byte[4];
                    // only copy the addr if the data is atleast 6
                    // bytes (ip + port).  that way isValidAddress
                    // will fail & we don't need to recheck the length
                    // when getting the port.
                    if(data.length >= 6)
                        System.arraycopy(data,0,myip,0,4);
                    
                    if (NetworkUtils.isValidAddress(myip)) {
                        try{
                            myIP = NetworkUtils.getByAddress(myip);
                            myPort = ByteOrder.ushort2int(ByteOrder.leb2short(data,4));
                            
                            if (NetworkUtils.isPrivateAddress(myIP) ||
                                    !NetworkUtils.isValidPort(myPort) ) {
                                // liars, or we are behind a NAT and there is LAN outside
                                // either way we can't use it
                                myIP=null;
                                myPort=0;
                            }
                            
                        }catch(UnknownHostException bad) {
                            //keep the ip address null and the port 0
                        }
                    }
                }catch(BadGGEPPropertyException ignored) {}
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_UDP_HOST_CACHE)) {
                cacheAddress = "";
                try {
                    cacheAddress = ggep.getString(GGEP.GGEP_HEADER_UDP_HOST_CACHE);
                } catch(BadGGEPPropertyException bad) {}
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_PACKED_IPPORTS)) {
                try {
                    byte[] data = ggep.getBytes(GGEP.GGEP_HEADER_PACKED_IPPORTS);
                    packedIPs = NetworkUtils.unpackIps(data);
                } catch(BadGGEPPropertyException bad) {
                } catch(InvalidDataException bpe) {}
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_DHT_IPPORTS)) {
                try {
                    byte[] data = ggep.getBytes(GGEP.GGEP_HEADER_DHT_IPPORTS);
                    packedDHTIPs = NetworkUtils.unpackIps(data);
                } catch(BadGGEPPropertyException bad) {
                } catch(InvalidDataException bpe) {
                }
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_PACKED_HOSTCACHES)) {
                try {
                    String data = ggep.getString(GGEP.GGEP_HEADER_PACKED_HOSTCACHES);
                    packedCaches = listCaches(data);
                } catch(BadGGEPPropertyException bad) {}
            }
        }
        
        MY_IP = myIP;
        MY_PORT = myPort;
        HAS_GGEP_EXTENSION = ggep != null;
        DAILY_UPTIME = dailyUptime;
        SUPPORTS_UNICAST = supportsUnicast;
        VENDOR = vendor;
        VENDOR_MAJOR_VERSION = vendorMajor;
        VENDOR_MINOR_VERSION = vendorMinor;
        QUERY_KEY = key;
        FREE_LEAF_SLOTS = freeLeafSlots;
        FREE_ULTRAPEER_SLOTS = freeUltrapeerSlots;
        CLIENT_LOCALE = locale;
        FREE_LOCALE_SLOTS = slots;
        if(cacheAddress != null && "".equals(cacheAddress))
            UDP_CACHE_ADDRESS = getAddress();
        else
            UDP_CACHE_ADDRESS = cacheAddress;
        PACKED_IP_PORTS = packedIPs;
        PACKED_DHT_IP_PORTS = packedDHTIPs;
        PACKED_UDP_HOST_CACHES = packedCaches;
        DHT_VERSION = dhtVersion;
        DHT_MODE = dhtMode;
    }


    /** Returns the GGEP payload bytes to encode the given uptime */
    private static GGEP newGGEP(int dailyUptime, boolean isUltrapeer,
                                boolean isGUESSCapable) {
        GGEP ggep=new GGEP(true);
        
        if (dailyUptime >= 0)
            ggep.put(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME, dailyUptime);
        
        if (isGUESSCapable && isUltrapeer) {
            // indicate guess support
            byte[] vNum = {
                convertToGUESSFormat(LimeWireUtils.getGUESSMajorVersionNumber(),
                                     LimeWireUtils.getGUESSMinorVersionNumber())};
            ggep.put(GGEP.GGEP_HEADER_UNICAST_SUPPORT, vNum);
        }
        
        // indicate UP support
        if (isUltrapeer)
            addUltrapeerExtension(ggep);
        
        // add DHT support
        addDHTExtension(ggep);
        
        // all pongs should have vendor info
        ggep.put(GGEP.GGEP_HEADER_VENDOR_INFO, CACHED_VENDOR); 

        return ggep;
    }


    /** Returns the GGEP payload bytes to encode the given QueryKey */
    private static GGEP qkGGEP(QueryKey queryKey) {
        try {
            GGEP ggep=new GGEP(true);

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
     * Adds the locale GGEP.
     */
    private static GGEP addLocale(GGEP ggep, String locale, int slots) {
        byte[] payload = new byte[3];
        byte[] s = locale.getBytes();
        payload[0] = s[0];
        payload[1] = s[1];
        payload[2] = (byte)slots;
        ggep.put(GGEP.GGEP_HEADER_CLIENT_LOCALE, payload);
        return ggep;
    }
    
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
    private static GGEP addPackedHosts(GGEP ggep,  Collection<? extends IpPort> gnutHosts, 
            Collection<? extends IpPort> dhtHosts) {
        
        if(gnutHosts != null && !gnutHosts.isEmpty()) {
            ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, NetworkUtils.packIpPorts(gnutHosts));
        }
        
        if(dhtHosts != null && !dhtHosts.isEmpty()) {
            ggep.put(GGEP.GGEP_HEADER_DHT_IPPORTS, NetworkUtils.packIpPorts(dhtHosts));
        }
        
        return ggep;
    }

    /**
     * Adds the ultrapeer GGEP extension to the pong.  This has the version of
     * the Ultrapeer protocol that we support as well as the number of free
     * leaf and Ultrapeer slots available.
     * 
     * @param ggep the <tt>GGEP</tt> instance to add the extension to
     */
    private static void addUltrapeerExtension(GGEP ggep) {
        byte[] payload = new byte[3];
        // put version
        payload[0] = convertToGUESSFormat(LimeWireUtils.getUPMajorVersionNumber(),
                                          LimeWireUtils.getUPMinorVersionNumber()
                                          );
        payload[1] = (byte) RouterService.getNumFreeLimeWireLeafSlots();
        payload[2] = (byte) RouterService.getNumFreeLimeWireNonLeafSlots();

        // add it
        ggep.put(GGEP.GGEP_HEADER_UP_SUPPORT, payload);
    }
    
    /**
     * Adds the DHT GGEP extension to the pong.  This has the version of
     * the DHT that we support as well as the mode of this node (active/passive).
     * A node only advertises itself as an active node if it is already bootstrapped
     * to the network!
     * 
     * @param ggep the <tt>GGEP</tt> instance to add the extension to
     */
    private static void addDHTExtension(GGEP ggep) {
        byte[] payload = new byte[3];
        
        // put version
        int version = RouterService.getDHTManager().getVersion().getVersion();
        ByteOrder.short2beb((short)version, payload, 0);
        
        if(RouterService.isMemberOfDHT()) {
            if(RouterService.isActiveDHTNode()) {
                payload[2] = DHTMode.ACTIVE.toByte();
            } else { //passive node
                payload[2] = DHTMode.PASSIVE.toByte();
            }
        } else {
            payload[2] = DHTMode.INACTIVE.toByte();
        }

        // add it
        ggep.put(GGEP.GGEP_HEADER_DHT_SUPPORT, payload);
    }

    /** puts major as the high order bits, minor as the low order bits.
     *  @exception IllegalArgumentException thrown if major/minor is greater 
     *  than 15 or less than 0.
     */
    private static byte convertToGUESSFormat(int major, int minor) 
        throws IllegalArgumentException {
        if ((major < 0) || (minor < 0) || (major > 15) || (minor > 15))
            throw new IllegalArgumentException();
        // set major
        int retInt = major;
        retInt = retInt << 4;
        // set minor
        retInt |= minor;

        return (byte) retInt;
    }

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
    public List<IpPort> getPackedIPPorts() {
        return PACKED_IP_PORTS;
    }
    
    /**
     * Gets the list of packed DHT IP/Ports.
     */
    public List<IpPort> getPackedDHTIPPorts() {
        return PACKED_DHT_IP_PORTS;
    }
    
    /**
     * Gets a list of packed IP/Ports of UDP Host Caches.
     */
    public List<IpPort> getPackedUDPHostCaches() {
        return PACKED_UDP_HOST_CACHES;
    }
    
    public DHTMode getDHTMode() {
        return DHT_MODE;
    }
    
    public int getDHTVersion() {
        return DHT_VERSION;
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
    
    // TODO : change this to look for multiple GGEP block in the payload....
    /** Ensure GGEP data parsed...if possible. */
    private static GGEP parseGGEP(final byte[] PAYLOAD) {
        //Return if this is a plain pong without space for GGEP.  If 
        //this has bad GGEP data, multiple calls to
        //parseGGEP will result in multiple parse attempts.  While this is
        //inefficient, it is sufficiently rare to not justify a parsedGGEP
        //variable.
        if (PAYLOAD.length <= STANDARD_PAYLOAD_SIZE)
            return null;
    
        try {
            return new GGEP(PAYLOAD, STANDARD_PAYLOAD_SIZE, null);
        } catch (BadGGEPBlockException e) { 
            return null;
        }
    }


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
    private List<IpPort> listCaches(String allCaches) {
        List<IpPort> theCaches = new LinkedList<IpPort>();
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

    /** Marks the given kbytes field */
    private static long mark(long kbytes) {
        int x=ByteOrder.long2int(kbytes);
        //Returns the power of two nearest to x.  TODO3: faster algorithms are
        //possible.  At the least, you can do binary search.  I imagine some bit
        //operations can be done as well.  This brute-force approach was
        //generated with the help of the the following Python program:
        //
        //  for i in xrange(0, 32):
        //      low=1<<i
        //      high=1<<(i+1)
        //      split=(low+high)/2
        //      print "else if (x<%d)" % split
        //      print "    return %d; //1<<%d" % (low, i)        
        if (x<12)
            return 8; //1<<3
        else if (x<24)
            return 16; //1<<4
        else if (x<48)
            return 32; //1<<5
        else if (x<96)
            return 64; //1<<6
        else if (x<192)
            return 128; //1<<7
        else if (x<384)
            return 256; //1<<8
        else if (x<768)
            return 512; //1<<9
        else if (x<1536)
            return 1024; //1<<10
        else if (x<3072)
            return 2048; //1<<11
        else if (x<6144)
            return 4096; //1<<12
        else if (x<12288)
            return 8192; //1<<13
        else if (x<24576)
            return 16384; //1<<14
        else if (x<49152)
            return 32768; //1<<15
        else if (x<98304)
            return 65536; //1<<16
        else if (x<196608)
            return 131072; //1<<17
        else if (x<393216)
            return 262144; //1<<18
        else if (x<786432)
            return 524288; //1<<19
        else if (x<1572864)
            return 1048576; //1<<20
        else if (x<3145728)
            return 2097152; //1<<21
        else if (x<6291456)
            return 4194304; //1<<22
        else if (x<12582912)
            return 8388608; //1<<23
        else if (x<25165824)
            return 16777216; //1<<24
        else if (x<50331648)
            return 33554432; //1<<25
        else if (x<100663296)
            return 67108864; //1<<26
        else if (x<201326592)
            return 134217728; //1<<27
        else if (x<402653184)
            return 268435456; //1<<28
        else if (x<805306368)
            return 536870912; //1<<29
        else 
            return 1073741824; //1<<30
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
