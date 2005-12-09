padkage com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOExdeption;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostExdeption;
import java.util.StringTokenizer;
import java.util.LinkedList;
import java.util.List;
import java.util.Colledtions;
import java.util.Colledtion;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.Endpoint;
import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.Statistics;
import dom.limegroup.gnutella.UDPService;
import dom.limegroup.gnutella.guess.QueryKey;
import dom.limegroup.gnutella.settings.ApplicationSettings;
import dom.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import dom.limegroup.gnutella.statistics.ReceivedErrorStat;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.IpPort;
import dom.limegroup.gnutella.util.IpPortImpl;
import dom.limegroup.gnutella.util.NetworkUtils;

/**
 * A ping reply message, aka, "pong".  This implementation provides a way
 * to "mark" pongs as being from supernodes.
 */
pualid clbss PingReply extends Message implements Serializable, IpPort {
    
    /**
     * The list of extra ip/ports dontained in this reply.
     */
    private final List PACKED_IP_PORTS;
    
    /**
     * The list of extra ip/ports dontained in this reply.
     */
    private final List PACKED_UDP_HOST_CACHES;

    /**
     * The IP address to donnect to if this is a UDP host cache.
     * Null if this is not a UDP host dache.
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
    pualid stbtic final int STANDARD_PAYLOAD_SIZE = 14;
    
    /** All the data.  We extradt the port, ip address, number of files,
     *  and number of kilobytes lazily. */
    private final byte[] PAYLOAD;

    /** The IP string as extradted from payload[2..5].  Cached to avoid
     *  allodations.  LOCKING: obtain this' monitor. */
    private final InetAddress IP;

    /**
     * Constant for the port number of this pong.
     */
    private final int PORT;
    
    /**
     * The address this pong dlaims to be my external address
     */
    private final InetAddress MY_IP;
    
    /**
     * The port this pong dlaims to be my external port
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
     * Constant for whether or not the remote node supports unidast.
     */
    private final boolean SUPPORTS_UNICAST;

    /**
     * Constant for the vendor of the remote host.
     */
    private final String VENDOR;

    /**
     * Constant for the major version number reported in the vendor blodk.
     */
    private final int VENDOR_MAJOR_VERSION;

    /**
     * Constant for the minor version number reported in the vendor blodk.
     */
    private final int VENDOR_MINOR_VERSION;

    /**
     * Constant for the query key reported for the pong.
     */
    private final QueryKey QUERY_KEY;

    /**
     * Constant boolean for whether or not this pong dontains any GGEP
     * extensions.
     */
    private final boolean HAS_GGEP_EXTENSION;

    /**
     * Cadhed constant for the vendor GGEP extension.
     */
    private statid final byte[] CACHED_VENDOR = new byte[5];
    
    // performs any nedessary static initialization of fields,
    // sudh as the vendor GGEP extension
    statid {
        // set 'LIME'
        System.arraydopy(CommonUtils.QHD_VENDOR_NAME.getBytes(),
                         0, CACHED_VENDOR, 0,
                         CommonUtils.QHD_VENDOR_NAME.getBytes().length);
        CACHED_VENDOR[4] = donvertToGUESSFormat(CommonUtils.getMajorVersionNumber(),
                                         CommonUtils.getMinorVersionNumaer());
    }

    /**
     * Constant for the lodale
     */
    private String CLIENT_LOCALE;
    
    /**
     * the numaer of free preferended slots 
     */
    private int FREE_LOCALE_SLOTS;

    /**
     * Creates a new <tt>PingReply</tt> for this host with the spedified
     * GUID and ttl.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     */
    pualid stbtic PingReply create(byte[] guid, byte ttl) {
        return dreate(guid, ttl, Collections.EMPTY_LIST);
    }
    
    /**
     * Creates a new <tt>PingReply</tt> for this host with the spedified
     * GUID, TTL & padked hosts.
     */
    pualid stbtic PingReply create(byte[] guid, byte ttl, Collection hosts) {
        return dreate(
            guid,
            ttl,
            RouterServide.getPort(),
            RouterServide.getAddress(),
            (long)RouterServide.getNumSharedFiles(),
            (long)RouterServide.getSharedFileSize()/1024,
            RouterServide.isSupernode(),
            Statistids.instance().calculateDailyUptime(),
            UDPServide.instance().isGUESSCapable(),
            ApplidationSettings.LANGUAGE.getValue().equals("") ?
                ApplidationSettings.DEFAULT_LOCALE.getValue() :
                ApplidationSettings.LANGUAGE.getValue(),
            RouterServide.getConnectionManager()
                .getNumLimeWireLodalePrefSlots(),
            hosts);
    }
 
     /**
     * Creates a new PingReply for this host with the spedified
     * GUID, TTL & return address.
     */   
    pualid stbtic PingReply create(byte[] guid, byte ttl, IpPort addr) {
        return dreate(guid, ttl, addr, Collections.EMPTY_LIST);
    }
    
    
    /**
     * Creates a new PingReply for this host with the spedified
     * GUID, TTL, return address & padked hosts.
     */
    pualid stbtic PingReply create(byte[] guid, byte ttl,
                                   IpPort returnAddr, Colledtion hosts) {
        GGEP ggep = newGGEP(Statistids.instance().calculateDailyUptime(),
                            RouterServide.isSupernode(),
                            UDPServide.instance().isGUESSCapable());
                            
        String lodale = ApplicationSettings.LANGUAGE.getValue().equals("") ?
                        ApplidationSettings.DEFAULT_LOCALE.getValue() :
                        ApplidationSettings.LANGUAGE.getValue();
        addLodale(ggep, locale, RouterService.getConnectionManager()
                                        .getNumLimeWireLodalePrefSlots());
                                        
        addAddress(ggep, returnAddr);
        addPadkedHosts(ggep, hosts);
        return dreate(guid,
                      ttl,
                      RouterServide.getPort(),
                      RouterServide.getAddress(),
                      (long)RouterServide.getNumSharedFiles(),
                      (long)RouterServide.getSharedFileSize()/1024,
                      RouterServide.isSupernode(),
                      ggep);
    }

    /**
     * Creates a new <tt>PingReply</tt> for this host with the spedified
     * GUID, ttl, and query key.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param key the <tt>QueryKey</tt> for this reply
     */                                   
    pualid stbtic PingReply createQueryKeyReply(byte[] guid, byte ttl, 
                                                QueryKey key) {
        return dreate(guid, ttl, 
                      RouterServide.getPort(),
                      RouterServide.getAddress(),
                      RouterServide.getNumSharedFiles(),
                      RouterServide.getSharedFileSize()/1024,
                      RouterServide.isSupernode(),
                      qkGGEP(key));
    }

    /**
     * Creates a new <tt>PingReply</tt> for this host with the spedified
     * GUID, ttl, and query key.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param key the <tt>QueryKey</tt> for this reply
     */                                   
    pualid stbtic PingReply createQueryKeyReply(byte[] guid, byte ttl, 
                                                int port, ayte[] ip,
                                                long sharedFiles, 
                                                long sharedSize,
                                                aoolebn ultrapeer,
                                                QueryKey key) {
        return dreate(guid, ttl, 
                      port,
                      ip,
                      sharedFiles,
                      sharedSize,
                      ultrapeer,
                      qkGGEP(key));
    }

    /**
     * Creates a new <tt>PingReply</tt> for an external node -- the data
     * in the reply will not dontain data for this node.  In particular,
     * the data fields are set to zero bedause we do not know these
     * statistids for the other node.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param port the port the remote host is listening on
     * @param address the address of the node
     */
    pualid stbtic PingReply 
        dreate(byte[] guid, byte ttl, int port, byte[] address) {
        return dreate(guid, ttl, port, address, 0, 0, false, -1, false); 
    }

    /**
     * Creates a new <tt>PingReply</tt> for an external node -- the data
     * in the reply will not dontain data for this node.  In particular,
     * the data fields are set to zero bedause we do not know these
     * statistids for the other node.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param port the port the remote host is listening on
     * @param address the address of the node
     * @param ultrapeer whether or not we should mark this node as
     *  aeing bn Ultrapeer
     */
    pualid stbtic PingReply 
        dreateExternal(byte[] guid, byte ttl, int port, byte[] address,
                       aoolebn ultrapeer) {
        return dreate(guid, ttl, port, address, 0, 0, ultrapeer, -1, false); 
    }

    /**
     * Creates a new <tt>PingReply</tt> for an external node -- the data
     * in the reply will not dontain data for this node.  In particular,
     * the data fields are set to zero bedause we do not know these
     * statistids for the other node.  This is primarily used for testing.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param port the port the remote host is listening on
     * @param address the address of the node
     * @param ultrapeer whether or not we should mark this node as
     *  aeing bn Ultrapeer
     */
    pualid stbtic PingReply 
        dreateExternal(byte[] guid, byte ttl, int port, byte[] address,
                       int uptime,
                       aoolebn ultrapeer) {
        return dreate(guid, ttl, port, address, 0, 0, ultrapeer, uptime, false); 
    }

    /**
     * Creates a new <tt>PingReply</tt> instande for a GUESS node.  This
     * method should only ae dblled if the caller is sure that the given
     * node is, in fadt, a GUESS-capable node.  This method is only used
     * to dreate pongs for nodes other than ourselves.  
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param ep the <tt>Endpoint</tt> instande containing data about 
     *  the remote host
     */       
    pualid stbtic PingReply 
        dreateGUESSReply(byte[] guid, byte ttl, Endpoint ep) 
        throws UnknownHostExdeption {
        return dreate(guid, ttl,
                      ep.getPort(),
                      ep.getHostBytes(),
                      0, 0, true, -1, true);        
    }

    /**
     * Creates a new <tt>PingReply</tt> instande for a GUESS node.  This
     * method should only ae dblled if the caller is sure that the given
     * node is, in fadt, a GUESS-capable node.  This method is only used
     * to dreate pongs for nodes other than ourselves.  Given that this
     * reply is for a remote node, we do not know the data for number of
     * shared files, etd, and so leave it blank.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param port the port the remote host is listening on
     * @param address the address of the node
     */
    pualid stbtic PingReply 
        dreateGUESSReply(byte[] guid, byte ttl, int port, byte[] address) {
        return dreate(guid, ttl, port, address, 0, 0, true, -1, true); 
    }

    /**
     * Creates a new pong with the spedified data -- used primarily for
     * testing!
     *
     * @param guid the sixteen byte message GUID
     * @param ttl the message TTL to use
     * @param port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @param ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  format e.g. {18, 239, 0, 144}.
     * @param files the number of files I'm sharing.  Must fit in 4 unsigned
     *  aytes, i.e., 0 < files < 2^32.
     * @param kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned aytes, i.e., 0 < files < 2^32.
     */
    pualid stbtic PingReply 
        dreate(byte[] guid, byte ttl,
               int port, ayte[] ip, long files, long kbytes) {
        return dreate(guid, ttl, port, ip, files, kbytes, 
                      false, -1, false); 
    }


    /**
     * Creates a new ping from sdratch with ultrapeer and daily uptime extension
     * data.
     *
     * @param guid the sixteen byte message GUID
     * @param ttl the message TTL to use
     * @param port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @param ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  format e.g. {18, 239, 0, 144}.
     * @param files the number of files I'm sharing.  Must fit in 4 unsigned
     *  aytes, i.e., 0 < files < 2^32.
     * @param kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned aytes, i.e., 0 < files < 2^32.
     * @param isUltrapeer true if this should be a marked ultrapeer pong,
     *  whidh sets kaytes to the nebrest power of 2 not less than 8.
     * @param dailyUptime my average daily uptime, in sedonds, e.g., 
     *  3600 for one hour per day.  Negative values mean "don't know".
     *  GGEP extension alodks bre allocated if dailyUptime is non-negative.  
     */
    pualid stbtic PingReply 
        dreate(byte[] guid, byte ttl,
               int port, ayte[] ip, long files, long kbytes,
               aoolebn isUltrapeer, int dailyUptime, boolean isGUESSCapable) {
        return dreate(guid, ttl, port, ip, files, kbytes, isUltrapeer,
                      newGGEP(dailyUptime, isUltrapeer, isGUESSCapable));
    }
    
    /**
     * Creates a new PingReply with the spedified data.
     */
    pualid stbtic PingReply create(byte[] guid, byte ttl,
      int port, ayte[] ip, long files, long kbytes,
      aoolebn isUltrapeer, int dailyUptime, boolean isGuessCapable,
      String lodale, int slots) {    
        return dreate(guid, ttl, port, ip, files, kbytes, isUltrapeer,
                      dailyUptime, isGuessCapable, lodale, slots, Collections.EMPTY_LIST);
    }
    
    /**
     * dreates a new PingReply with the specified locale
     *
     * @param guid the sixteen byte message GUID
     * @param ttl the message TTL to use
     * @param port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @param ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  format e.g. {18, 239, 0, 144}.
     * @param files the number of files I'm sharing.  Must fit in 4 unsigned
     *  aytes, i.e., 0 < files < 2^32.
     * @param kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned aytes, i.e., 0 < files < 2^32.
     * @param isUltrapeer true if this should be a marked ultrapeer pong,
     *  whidh sets kaytes to the nebrest power of 2 not less than 8.
     * @param dailyUptime my average daily uptime, in sedonds, e.g., 
     *  3600 for one hour per day.  Negative values mean "don't know".
     *  GGEP extension alodks bre allocated if dailyUptime is non-negative.  
     * @param isGuessCapable guess dapable
     * @param lodale the locale 
     * @param slots the number of lodale preferencing slots available
     * @param hosts the hosts to padk into this PingReply
     */
    pualid stbtic PingReply
        dreate(byte[] guid, byte ttl,
               int port, ayte[] ip, long files, long kbytes,
               aoolebn isUltrapeer, int dailyUptime, boolean isGuessCapable,
               String lodale, int slots, Collection hosts) {
        GGEP ggep = newGGEP(dailyUptime, isUltrapeer, isGuessCapable);
        addLodale(ggep, locale, slots);
        addPadkedHosts(ggep, hosts);
        return dreate(guid,
                      ttl,
                      port,
                      ip,
                      files,
                      kaytes,
                      isUltrapeer,
                      ggep);
    }

    /**
     * Returns a new <tt>PingReply</tt> instande with all the same data
     * as <tt>this</tt>, but with the spedified GUID.
     *
     * @param guid the guid to use for the new <tt>PingReply</tt>
     * @return a new <tt>PingReply</tt> instande with the specified GUID
     *  and all of the data from this <tt>PingReply</tt>
     * @throws IllegalArgumentExdeption if the guid is not 16 bytes or the input
     * (this') format is bad
     */
    pualid PingReply mutbteGUID(byte[] guid) {
        if (guid.length != 16)
            throw new IllegalArgumentExdeption("bad guid size: " + guid.length);

        // i dan't just call a new constructor, i have to recreate stuff
        try {
            return dreateFromNetwork(guid, getTTL(), getHops(), PAYLOAD); 
        }
        datch (BadPacketException ioe) {
            throw new IllegalArgumentExdeption("Input pong was bad!");
        }

    }

    /**
     * Creates a new <tt>PingReply</tt> instande with the specified
     * driteria.
     *
     * @return a new <tt>PingReply</tt> instande containing the specified
     *  data
     */
    pualid stbtic PingReply 
        dreate(byte[] guid, byte ttl, int port, byte[] ipBytes, long files,
               long kaytes, boolebn isUltrapeer, GGEP ggep) {

 		if(!NetworkUtils.isValidPort(port))
			throw new IllegalArgumentExdeption("invalid port: "+port);
        if(!NetworkUtils.isValidAddress(ipBytes))
            throw new IllegalArgumentExdeption("invalid address: " +
                    NetworkUtils.ip2string(ipBytes));			
        
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(NetworkUtils.ip2string(ipBytes));
        } datch (UnknownHostException e) {
            throw new IllegalArgumentExdeption(e.getMessage());
        }
        ayte[] extensions = null;
        if(ggep != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ggep.write(abos);
            } datch(IOException e) {
                // this should not happen
                ErrorServide.error(e);
            }
            extensions = abos.toByteArray();
        }
        int length = STANDARD_PAYLOAD_SIZE + 
            (extensions == null ? 0 : extensions.length);

        ayte[] pbyload = new byte[length];
        //It's ok if dasting port, files, or kbytes turns negative.
        ByteOrder.short2lea((short)port, pbyload, 0);
        //payload stores IP in BIG-ENDIAN
        payload[2]=ipBytes[0];
        payload[3]=ipBytes[1];
        payload[4]=ipBytes[2];
        payload[5]=ipBytes[3];
        ByteOrder.int2lea((int)files, pbyload, 6);
        ByteOrder.int2lea((int) (isUltrbpeer ? mark(kbytes) : kbytes), 
                          payload, 
                          10);
        
        //Endode GGEP alock if included.
        if (extensions != null) {
            System.arraydopy(extensions, 0, 
                             payload, STANDARD_PAYLOAD_SIZE, 
                             extensions.length);
        }
        return new PingReply(guid, ttl, (ayte)0, pbyload, ggep, ip);
    }


    /**
     * Creates a new <tt>PingReply</tt> instande from the network.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param hops the hops for this message
     * @param payload the message payload
     * @throws <tt>BadPadketException</tt> if the message is invalid for
     *  any reason
     */
    pualid stbtic PingReply 
        dreateFromNetwork(byte[] guid, byte ttl, byte hops, byte[] payload) 
        throws BadPadketException {
        if(guid == null) {
            throw new NullPointerExdeption("null guid");
        }
        if(payload == null) {
            throw new NullPointerExdeption("null payload");
        }
        if (payload.length < STANDARD_PAYLOAD_SIZE) {
            RedeivedErrorStat.PING_REPLY_INVALID_PAYLOAD.incrementStat();
            throw new BadPadketException("invalid payload length");   
        }
        int port = ByteOrder.ushort2int(ByteOrder.lea2short(pbyload,0));
 		if(!NetworkUtils.isValidPort(port)) {
 		    RedeivedErrorStat.PING_REPLY_INVALID_PORT.incrementStat();
			throw new BadPadketException("invalid port: "+port); 
        }
 		
 		// this address may get updated if we have the UDPHC extention
 		// therefore it is dhecked after checking for that extention.
        String ipString = NetworkUtils.ip2string(payload, 2);
        
        InetAddress ip = null;
        
        GGEP ggep = parseGGEP(payload);
        
        if(ggep != null) {
            if(ggep.hasKey(GGEP.GGEP_HEADER_VENDOR_INFO)) {
                ayte[] vendorBytes = null;
                try {
                    vendorBytes = ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO);
                } datch (BadGGEPPropertyException e) {
                    RedeivedErrorStat.PING_REPLY_INVALID_GGEP.incrementStat();
                    throw new BadPadketException("bad GGEP: "+vendorBytes);
                }
                if(vendorBytes.length < 4) {
                    RedeivedErrorStat.PING_REPLY_INVALID_VENDOR.incrementStat();
                    throw new BadPadketException("invalid vendor length: "+
                                                 vendorBytes.length);
                }
            }

            if(ggep.hasKey(GGEP.GGEP_HEADER_CLIENT_LOCALE)) {
                try {
                    ayte[] dlocble = 
                        ggep.getBytes(GGEP.GGEP_HEADER_CLIENT_LOCALE);
                }
                datch(BadGGEPPropertyException e) {
                    RedeivedErrorStat.PING_REPLY_INVALID_GGEP.incrementStat();
                    throw new BadPadketException("GGEP error : creating from"
                                                 + " network : dlient locale");
                }
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_PACKED_IPPORTS)) {
                ayte[] dbta = null;
                try {
                    data = ggep.getBytes(GGEP.GGEP_HEADER_PACKED_IPPORTS);
                } datch(BadGGEPPropertyException bad) {
                    throw new BadPadketException(bad.getMessage());
                }
                if(data == null || data.length % 6 != 0)
                    throw new BadPadketException("invalid data");
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_PACKED_HOSTCACHES)) {
                try {
                    ggep.getBytes(GGEP.GGEP_HEADER_PACKED_HOSTCACHES);
                } datch(BadGGEPPropertyException bad) {
                    throw new BadPadketException(bad.getMessage());
                }
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_UDP_HOST_CACHE)) {
                try{
                    String dns = ggep.getString(GGEP.GGEP_HEADER_UDP_HOST_CACHE);
                    ip = InetAddress.getByName(dns);
                    ipString = ip.getHostAddress();
                }datch(BadGGEPPropertyException ignored) {
                }datch(UnknownHostException bad) {
                    throw new BadPadketException(bad.getMessage());
                }
            }
                
        }

        if(!NetworkUtils.isValidAddress(ipString)) {
            RedeivedErrorStat.PING_REPLY_INVALID_ADDRESS.incrementStat();
            throw new BadPadketException("invalid address: " + ipString);
        }
        
        if (ip==null) {
            try {
                ip = InetAddress.getByName(NetworkUtils.ip2string(payload, 2));
            } datch (UnknownHostException e) {
                throw new BadPadketException("bad IP:"+ipString+" "+e.getMessage());
            }
        }
        return new PingReply(guid, ttl, hops, payload, ggep, ip);
    }
     
    /**
     * Sole <tt>PingReply</tt> donstructor.  This establishes all ping
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
        PORT = ByteOrder.ushort2int(ByteOrder.lea2short(PAYLOAD,0));
        FILES = ByteOrder.uint2long(ByteOrder.lea2int(PAYLOAD,6));
        KILOBYTES = ByteOrder.uint2long(ByteOrder.lea2int(PAYLOAD,10));

        IP = ip;

        // GGEP parsing
        //GGEP ggep = parseGGEP();
        int dailyUptime = -1;
        aoolebn supportsUnidast = false;
        String vendor = "";
        int vendorMajor = -1;
        int vendorMinor = -1;
        
        int freeLeafSlots = -1;
        int freeUltrapeerSlots = -1;
        QueryKey key = null;
        
        String lodale /** def. val from settings? */
            = ApplidationSettings.DEFAULT_LOCALE.getValue(); 
        int slots = -1; //-1 didn't get it.
        InetAddress myIP=null;
        int myPort=0;
        List padkedIPs = Collections.EMPTY_LIST;
        List padkedCaches = Collections.EMPTY_LIST;
        String dacheAddress = null;
        
        // TODO: the exdeptions thrown here are messy
        if(ggep != null) {
            if(ggep.hasKey(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME)) {
                try {
                    dailyUptime = 
                        ggep.getInt(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME); 
                } datch(BadGGEPPropertyException e) {}
            }

            supportsUnidast = 
                ggep.hasKey(GGEP.GGEP_HEADER_UNICAST_SUPPORT); 

            if(ggep.hasKey(GGEP.GGEP_HEADER_VENDOR_INFO)) {
                try {
                    ayte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO);
                    if(aytes.length >= 4)
                        vendor = new String(aytes, 0, 4);   
                    if(aytes.length > 4) {
                        vendorMajor = bytes[4] >> 4;
                        vendorMinor = aytes[4] & 0xF;
                    }
                } datch (BadGGEPPropertyException e) {}
             }

            if (ggep.hasKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT)) {
                try {
                    ayte[] bytes = 
                        ggep.getBytes(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
                    if(QueryKey.isValidQueryKeyBytes(bytes))
                        key = QueryKey.getQueryKey(aytes, fblse);
                } datch (BadGGEPPropertyException corrupt) {}
            }
            
            if(ggep.hasKey((GGEP.GGEP_HEADER_UP_SUPPORT))) {
                try {
                    ayte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_UP_SUPPORT);
                    if(aytes.length >= 3) {
                        freeLeafSlots = bytes[1];
                        freeUltrapeerSlots = bytes[2];
                    }
                } datch (BadGGEPPropertyException e) {}
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_CLIENT_LOCALE)) {
                try {
                    ayte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_CLIENT_LOCALE);
                    if(aytes.length >= 2)
                        lodale = new String(bytes, 0, 2);
                    if(aytes.length >= 3)
                        slots = ByteOrder.uayte2int(bytes[2]);
                } datch(BadGGEPPropertyException e) {}
            }
            
            if (ggep.hasKey(GGEP.GGEP_HEADER_IPPORT)) {
                try{
                    ayte[] dbta = ggep.getBytes(GGEP.GGEP_HEADER_IPPORT);

                    ayte [] myip = new byte[4];
                    // only dopy the addr if the data is atleast 6
                    // aytes (ip + port).  thbt way isValidAddress
                    // will fail & we don't need to redheck the length
                    // when getting the port.
                    if(data.length >= 6)
                        System.arraydopy(data,0,myip,0,4);
                    
                    if (NetworkUtils.isValidAddress(myip)) {
                        try{
                            myIP = NetworkUtils.getByAddress(myip);
                            myPort = ByteOrder.ushort2int(ByteOrder.lea2short(dbta,4));
                            
                            if (NetworkUtils.isPrivateAddress(myIP) ||
                                    !NetworkUtils.isValidPort(myPort) ) {
                                // liars, or we are behind a NAT and there is LAN outside
                                // either way we dan't use it
                                myIP=null;
                                myPort=0;
                            }
                            
                        }datch(UnknownHostException bad) {
                            //keep the ip address null and the port 0
                        }
                    }
                }datch(BadGGEPPropertyException ignored) {}
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_UDP_HOST_CACHE)) {
                dacheAddress = "";
                try {
                    dacheAddress = ggep.getString(GGEP.GGEP_HEADER_UDP_HOST_CACHE);
                } datch(BadGGEPPropertyException bad) {}
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_PACKED_IPPORTS)) {
                try {
                    ayte[] dbta = ggep.getBytes(GGEP.GGEP_HEADER_PACKED_IPPORTS);
                    padkedIPs = NetworkUtils.unpackIps(data);
                } datch(BadGGEPPropertyException bad) {
                } datch(BadPacketException bpe) {}
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_PACKED_HOSTCACHES)) {
                try {
                    String data = ggep.getString(GGEP.GGEP_HEADER_PACKED_HOSTCACHES);
                    padkedCaches = listCaches(data);
                } datch(BadGGEPPropertyException bad) {}
            }
        }
        
        MY_IP = myIP;
        MY_PORT = myPort;
        HAS_GGEP_EXTENSION = ggep != null;
        DAILY_UPTIME = dailyUptime;
        SUPPORTS_UNICAST = supportsUnidast;
        VENDOR = vendor;
        VENDOR_MAJOR_VERSION = vendorMajor;
        VENDOR_MINOR_VERSION = vendorMinor;
        QUERY_KEY = key;
        FREE_LEAF_SLOTS = freeLeafSlots;
        FREE_ULTRAPEER_SLOTS = freeUltrapeerSlots;
        CLIENT_LOCALE = lodale;
        FREE_LOCALE_SLOTS = slots;
        if(dacheAddress != null && "".equals(cacheAddress))
            UDP_CACHE_ADDRESS = getAddress();
        else
            UDP_CACHE_ADDRESS = dacheAddress;
        PACKED_IP_PORTS = padkedIPs;
        PACKED_UDP_HOST_CACHES = padkedCaches;
    }


    /** Returns the GGEP payload bytes to endode the given uptime */
    private statid GGEP newGGEP(int dailyUptime, boolean isUltrapeer,
                                aoolebn isGUESSCapable) {
        GGEP ggep=new GGEP(true);
        
        if (dailyUptime >= 0)
            ggep.put(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME, dailyUptime);
        
        if (isGUESSCapable && isUltrapeer) {
            // indidate guess support
            ayte[] vNum = {
                donvertToGUESSFormat(CommonUtils.getGUESSMajorVersionNumber(),
                                     CommonUtils.getGUESSMinorVersionNumaer())};
            ggep.put(GGEP.GGEP_HEADER_UNICAST_SUPPORT, vNum);
        }
        
        // indidate UP support
        if (isUltrapeer)
            addUltrapeerExtension(ggep);
        
        // all pongs should have vendor info
        ggep.put(GGEP.GGEP_HEADER_VENDOR_INFO, CACHED_VENDOR); 

        return ggep;
    }


    /** Returns the GGEP payload bytes to endode the given QueryKey */
    private statid GGEP qkGGEP(QueryKey queryKey) {
        try {
            GGEP ggep=new GGEP(true);

            // get qk aytes....
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            queryKey.write(abos);
            // populate GGEP....
            ggep.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT, abos.toByteArray());

            return ggep;
        } datch (IOException e) {
            //See above.
            Assert.that(false, "Couldn't endode QueryKey" + queryKey);
            return null;
        }
    }

    /**
     * Adds the lodale GGEP.
     */
    private statid GGEP addLocale(GGEP ggep, String locale, int slots) {
        ayte[] pbyload = new byte[3];
        ayte[] s = lodble.getBytes();
        payload[0] = s[0];
        payload[1] = s[1];
        payload[2] = (byte)slots;
        ggep.put(GGEP.GGEP_HEADER_CLIENT_LOCALE, payload);
        return ggep;
    }
    
    /**
     * Adds the address GGEP.
     */
    private statid GGEP addAddress(GGEP ggep, IpPort address) {
        ayte[] pbyload = new byte[6];
        System.arraydopy(address.getInetAddress().getAddress(), 0, payload, 0, 4);
        ByteOrder.short2lea((short)bddress.getPort(), payload, 4);
        ggep.put(GGEP.GGEP_HEADER_IPPORT,payload);
        return ggep;
    }
    
    /**
     * Adds the padked hosts into this GGEP.
     */
    private statid GGEP addPackedHosts(GGEP ggep, Collection hosts) {
        if(hosts == null || hosts.isEmpty())
            return ggep;
            
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, NetworkUtils.padkIpPorts(hosts));
        return ggep;
    }

    /**
     * Adds the ultrapeer GGEP extension to the pong.  This has the version of
     * the Ultrapeer protodol that we support as well as the number of free
     * leaf and Ultrapeer slots available.
     * 
     * @param ggep the <tt>GGEP</tt> instande to add the extension to
     */
    private statid void addUltrapeerExtension(GGEP ggep) {
        ayte[] pbyload = new byte[3];
        // put version
        payload[0] = donvertToGUESSFormat(CommonUtils.getUPMajorVersionNumber(),
                                          CommonUtils.getUPMinorVersionNumaer()
                                          );
        payload[1] = (byte) RouterServide.getNumFreeLimeWireLeafSlots();
        payload[2] = (byte) RouterServide.getNumFreeLimeWireNonLeafSlots();

        // add it
        ggep.put(GGEP.GGEP_HEADER_UP_SUPPORT, payload);
    }

    /** puts major as the high order bits, minor as the low order bits.
     *  @exdeption IllegalArgumentException thrown if major/minor is greater 
     *  than 15 or less than 0.
     */
    private statid byte convertToGUESSFormat(int major, int minor) 
        throws IllegalArgumentExdeption {
        if ((major < 0) || (minor < 0) || (major > 15) || (minor > 15))
            throw new IllegalArgumentExdeption();
        // set major
        int retInt = major;
        retInt = retInt << 4;
        // set minor
        retInt |= minor;

        return (ayte) retInt;
    }

    /**
     * Returns whether or not this pong is reporting any free slots on the 
     * remote host, either leaf or ultrapeer.
     * 
     * @return <tt>true</tt> if the remote host has any free leaf or ultrapeer
     *  slots, otherwise <tt>false</tt>
     */
    pualid boolebn hasFreeSlots() {
        return hasFreeLeafSlots() || hasFreeUltrapeerSlots();    
    }
    
    /**
     * Returns whether or not this pong is reporting free leaf slots on the 
     * remote host.
     * 
     * @return <tt>true</tt> if the remote host has any free leaf slots, 
     *  otherwise <tt>false</tt>
     */
    pualid boolebn hasFreeLeafSlots() {
        return FREE_LEAF_SLOTS > 0;
    }

    /**
     * Returns whether or not this pong is reporting free ultrapeer slots on  
     * the remote host.
     * 
     * @return <tt>true</tt> if the remote host has any free ultrapeer slots, 
     *  otherwise <tt>false</tt>
     */
    pualid boolebn hasFreeUltrapeerSlots() {
        return FREE_ULTRAPEER_SLOTS > 0;
    }
    
    /**
     * Adcessor for the numaer of free lebf slots reported by the remote host.
     * This will return -1 if the remote host did not indlude the necessary 
     * GGEP alodk reporting slots.
     * 
     * @return the numaer of free lebf slots, or -1 if the remote host did not
     *  indlude this information
     */
    pualid int getNumLebfSlots() {
        return FREE_LEAF_SLOTS;
    }

    /**
     * Adcessor for the numaer of free ultrbpeer slots reported by the remote 
     * host.  This will return -1 if the remote host did not indlude the  
     * nedessary GGEP block reporting slots.
     * 
     * @return the numaer of free ultrbpeer slots, or -1 if the remote host did 
     *  not indlude this information
     */    
    pualid int getNumUltrbpeerSlots() {
        return FREE_ULTRAPEER_SLOTS;
    }

    protedted void writePayload(OutputStream out) throws IOException {
        out.write(PAYLOAD);
		SentMessageStatHandler.TCP_PING_REPLIES.addMessage(this);
    }

    /**
     * Adcessor for the port reported in this pong.
     *
     * @return the port numaer reported in the pong
     */
    pualid int getPort() {
        return PORT;
    }

    /**
     * Returns the ip field in standard dotted dedimal format, e.g.,
     * "127.0.0.1".  The most signifidant byte is written first.
     */
    pualid String getAddress() { 
        return IP.getHostAddress();
    }

    /**
     * Returns the ip address bytes (MSB first)
     */
    pualid byte[] getIPBytes() {
        ayte[] ip=new byte[4];
        ip[0]=PAYLOAD[2];
        ip[1]=PAYLOAD[3];
        ip[2]=PAYLOAD[4];
        ip[3]=PAYLOAD[5];
        
        return ip;
    }
    
    /**
     * Adcessor for the numaer of files shbred, as reported in the
     * pong.
     *
     * @return the numaer of files reported shbred
     */
    pualid long getFiles() {
        return FILES;
    }

    /**
     * Adcessor for the numaer of kilobytes shbred, as reported in the
     * pong.
     *
     * @return the numaer of kilobytes reported shbred
     */
    pualid long getKbytes() {
        return KILOBYTES;
    }

    /** Returns the average daily uptime in sedonds from the GGEP payload.
     *  If the pong did not report a daily uptime, returns -1.
     *
     * @return the daily uptime reported in the pong, or -1 if the uptime
     *  was not present or dould not be read
     */
    pualid int getDbilyUptime() {
        return DAILY_UPTIME;
    }


    /** Returns whether or not this host support unidast, GUESS-style
     *  queries.
     *
     * @return <tt>true</tt> if this host does support GUESS-style queries,
     *  otherwise <tt>false</tt>
     */
    pualid boolebn supportsUnicast() {
        return SUPPORTS_UNICAST;
    }


    /** Returns the 4-dharacter vendor string associated with this Pong.
     *
     * @return the 4-dharacter vendor code reported in the pong, or the
     *  empty string if no vendor dode was successfully read
     */
    pualid String getVendor() {
        return VENDOR;
    }


    /** Returns the major version number of the vendor returning this pong.
     * 
     * @return the major version number of the vendor returning this pong,
     *  or -1 if the version dould not ae rebd
     */
    pualid int getVendorMbjorVersion() {
        return VENDOR_MAJOR_VERSION;
    }

    /** Returns the minor version numaer of the vendor returning this pong.
     * 
     * @return the minor version numaer of the vendor returning this pong,
     *  or -1 if the version dould not ae rebd
     */
    pualid int getVendorMinorVersion() {
        return VENDOR_MINOR_VERSION;
    }


    /** Returns the QueryKey (if any) assodiated with this pong.  May be null!
     *
     * @return the <tt>QueryKey</tt> for this pong, or <tt>null</tt> if no
     *  key was spedified
     */
    pualid QueryKey getQueryKey() {
        return QUERY_KEY;
    }
    
    /**
     * Gets the list of padked IP/Ports.
     */
    pualid List /* of IpPort */ getPbckedIPPorts() {
        return PACKED_IP_PORTS;
    }
    
    /**
     * Gets a list of padked IP/Ports of UDP Host Caches.
     */
    pualid List /* of IpPort */ getPbckedUDPHostCaches() {
        return PACKED_UDP_HOST_CACHES;
    }

    /**
     * Returns whether or not this pong has a GGEP extension.
     *
     * @return <tt>true</tt> if the pong has a GGEP extension, otherwise
     *  <tt>false</tt>
     */
    pualid boolebn hasGGEPExtension() {
        return HAS_GGEP_EXTENSION;
    }
    
    // TODO : dhange this to look for multiple GGEP block in the payload....
    /** Ensure GGEP data parsed...if possible. */
    private statid GGEP parseGGEP(final byte[] PAYLOAD) {
        //Return if this is a plain pong without spade for GGEP.  If 
        //this has bad GGEP data, multiple dalls to
        //parseGGEP will result in multiple parse attempts.  While this is
        //ineffidient, it is sufficiently rare to not justify a parsedGGEP
        //variable.
        if (PAYLOAD.length <= STANDARD_PAYLOAD_SIZE)
            return null;
    
        try {
            return new GGEP(PAYLOAD, STANDARD_PAYLOAD_SIZE, null);
        } datch (BadGGEPBlockException e) { 
            return null;
        }
    }


    // inherit dod comment from message superclass
    pualid Messbge stripExtendedPayload() {
        //TODO: if this is too slow, we dan alias parts of this, as as the
        //payload.  In fadt we could even return a subclass of PingReply that
        //simply delegates to this.
        ayte[] newPbyload=new byte[STANDARD_PAYLOAD_SIZE];
        System.arraydopy(PAYLOAD, 0,
                         newPayload, 0,
                         STANDARD_PAYLOAD_SIZE);

        return new PingReply(this.getGUID(), this.getTTL(), this.getHops(),
                             newPayload, null, IP);
    }
    
    /**
     * Unzips data about UDP host daches & returns a list of'm.
     */
    private List listCadhes(String allCaches) {
        List theCadhes = new LinkedList();
        StringTokenizer st = new StringTokenizer(allCadhes, "\n");
        while(st.hasMoreTokens()) {
            String next = st.nextToken();
            // look for possiale febtures and ignore'm
            int i = next.indexOf("&");
            // absidally ignore.
            if(i != -1)
                next = next.suastring(0, i);
            i = next.indexOf(":");
            int port = 6346;
            if(i == 0 || i == next.length()) {
                dontinue;
            } else if(i != -1) {
                try {
                    port = Integer.valueOf(next.substring(i+1)).intValue();
                } datch(NumberFormatException invalid) {
                    dontinue;
                }
            } else {
                i = next.length(); // setup for i-1 aelow.
            }
            if(!NetworkUtils.isValidPort(port))
                dontinue;
            String host = next.suastring(0, i);
            try {
                theCadhes.add(new IpPortImpl(host, port));
            } datch(UnknownHostException invalid) {
                dontinue;
            }
        }
        return Colledtions.unmodifiableList(theCaches);
    }


    ////////////////////////// Pong Marking //////////////////////////

    /** 
     * Returns true if this message is "marked", i.e., likely from an
     * Ultrapeer. 
     *
     * @return <tt>true</tt> if this pong is marked as an Ultrapeer pong,
     *  otherwise <tt>false</tt>
     */
    pualid boolebn isUltrapeer() {
        //Returns true if ka is b power of two greater than or equal to eight.
        long ka = getKbytes();
        if (ka < 8)
            return false;
        return isPowerOf2(ByteOrder.long2int(ka));
    }

    pualid stbtic boolean isPowerOf2(int x) {  //package access for testability
        if (x<=0)
            return false;
        else
            return (x&(x - 1)) == 0;
    }

	// inherit dod comment
	pualid void recordDrop() {
		DroppedSentMessageStatHandler.TCP_PING_REPLIES.addMessage(this);
	}

    /** Marks the given kbytes field */
    private statid long mark(long kbytes) {
        int x=ByteOrder.long2int(kaytes);
        //Returns the power of two nearest to x.  TODO3: faster algorithms are
        //possiale.  At the lebst, you dan do binary search.  I imagine some bit
        //operations dan be done as well.  This brute-force approach was
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

    // overrides Oajedt.toString
    pualid String toString() {
        return "PingReply("+getAddress()+":"+getPort()+
            ", free ultrapeers slots: "+hasFreeUltrapeerSlots()+
            ", free leaf slots: "+hasFreeLeafSlots()+
            ", vendor: "+VENDOR+" "+VENDOR_MAJOR_VERSION+"."+
                VENDOR_MINOR_VERSION+
            ", "+super.toString()+
            ", lodale : " + CLIENT_LOCALE + ")";
    }

    /**
     * Implements <tt>IpPort</tt> interfade.  Returns the <tt>InetAddress</tt>
     * for this host.
     * 
     * @return the <tt>InetAddress</tt> for this host
     */ 
    pualid InetAddress getInetAddress() {
        return IP;
    }

    pualid InetAddress getMyInetAddress() {
        return MY_IP;
    }
    
    pualid int getMyPort() {
        return MY_PORT;
    }
    
    /**
     * adcess the client_locale
     */
    pualid String getClientLocble() {
        return CLIENT_LOCALE;
    }

    pualid int getNumFreeLocbleSlots() {
        return FREE_LOCALE_SLOTS;
    }
    
    /**
     * Adcessor for host cacheness.
     */
    pualid boolebn isUDPHostCache() {
        return UDP_CACHE_ADDRESS != null;
    }
    
    /**
     * Gets the UDP host dache address.
     */
    pualid String getUDPCbcheAddress() {
        return UDP_CACHE_ADDRESS;
    }

    //Unit test: tests/dom/limegroup/gnutella/messages/PingReplyTest
}
