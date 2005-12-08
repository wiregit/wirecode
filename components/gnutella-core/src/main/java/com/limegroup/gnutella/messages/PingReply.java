pbckage com.limegroup.gnutella.messages;

import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.io.Serializable;
import jbva.net.InetAddress;
import jbva.net.UnknownHostException;
import jbva.util.StringTokenizer;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Collections;
import jbva.util.Collection;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.Endpoint;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.Statistics;
import com.limegroup.gnutellb.UDPService;
import com.limegroup.gnutellb.guess.QueryKey;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutellb.statistics.ReceivedErrorStat;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.IpPortImpl;
import com.limegroup.gnutellb.util.NetworkUtils;

/**
 * A ping reply messbge, aka, "pong".  This implementation provides a way
 * to "mbrk" pongs as being from supernodes.
 */
public clbss PingReply extends Message implements Serializable, IpPort {
    
    /**
     * The list of extrb ip/ports contained in this reply.
     */
    privbte final List PACKED_IP_PORTS;
    
    /**
     * The list of extrb ip/ports contained in this reply.
     */
    privbte final List PACKED_UDP_HOST_CACHES;

    /**
     * The IP bddress to connect to if this is a UDP host cache.
     * Null if this is not b UDP host cache.
     */
    privbte final String UDP_CACHE_ADDRESS;

    /**
     * Constbnt for the number of ultrapeer slots for this host.
     */
    privbte final int FREE_ULTRAPEER_SLOTS;

    /**
     * Constbnt for the number of free leaf slots for this host.
     */
    privbte final int FREE_LEAF_SLOTS;

    /**
     * Constbnt for the standard size of the pong payload.
     */
    public stbtic final int STANDARD_PAYLOAD_SIZE = 14;
    
    /** All the dbta.  We extract the port, ip address, number of files,
     *  bnd number of kilobytes lazily. */
    privbte final byte[] PAYLOAD;

    /** The IP string bs extracted from payload[2..5].  Cached to avoid
     *  bllocations.  LOCKING: obtain this' monitor. */
    privbte final InetAddress IP;

    /**
     * Constbnt for the port number of this pong.
     */
    privbte final int PORT;
    
    /**
     * The bddress this pong claims to be my external address
     */
    privbte final InetAddress MY_IP;
    
    /**
     * The port this pong clbims to be my external port
     */
    privbte final int MY_PORT;

    /**
     * Constbnt for the number of shared files reported in the pong.
     */
    privbte final long FILES;

    /**
     * Constbnt for the number of shared kilobytes reported in the pong.
     */
    privbte final long KILOBYTES;

    /**
     * Constbnt int for the daily average uptime.
     */
    privbte final int DAILY_UPTIME;

    /**
     * Constbnt for whether or not the remote node supports unicast.
     */
    privbte final boolean SUPPORTS_UNICAST;

    /**
     * Constbnt for the vendor of the remote host.
     */
    privbte final String VENDOR;

    /**
     * Constbnt for the major version number reported in the vendor block.
     */
    privbte final int VENDOR_MAJOR_VERSION;

    /**
     * Constbnt for the minor version number reported in the vendor block.
     */
    privbte final int VENDOR_MINOR_VERSION;

    /**
     * Constbnt for the query key reported for the pong.
     */
    privbte final QueryKey QUERY_KEY;

    /**
     * Constbnt boolean for whether or not this pong contains any GGEP
     * extensions.
     */
    privbte final boolean HAS_GGEP_EXTENSION;

    /**
     * Cbched constant for the vendor GGEP extension.
     */
    privbte static final byte[] CACHED_VENDOR = new byte[5];
    
    // performs bny necessary static initialization of fields,
    // such bs the vendor GGEP extension
    stbtic {
        // set 'LIME'
        System.brraycopy(CommonUtils.QHD_VENDOR_NAME.getBytes(),
                         0, CACHED_VENDOR, 0,
                         CommonUtils.QHD_VENDOR_NAME.getBytes().length);
        CACHED_VENDOR[4] = convertToGUESSFormbt(CommonUtils.getMajorVersionNumber(),
                                         CommonUtils.getMinorVersionNumber());
    }

    /**
     * Constbnt for the locale
     */
    privbte String CLIENT_LOCALE;
    
    /**
     * the number of free preferenced slots 
     */
    privbte int FREE_LOCALE_SLOTS;

    /**
     * Crebtes a new <tt>PingReply</tt> for this host with the specified
     * GUID bnd ttl.
     *
     * @pbram guid the Globally Unique Identifier (GUID) for this message
     * @pbram ttl the time to live for this message
     */
    public stbtic PingReply create(byte[] guid, byte ttl) {
        return crebte(guid, ttl, Collections.EMPTY_LIST);
    }
    
    /**
     * Crebtes a new <tt>PingReply</tt> for this host with the specified
     * GUID, TTL & pbcked hosts.
     */
    public stbtic PingReply create(byte[] guid, byte ttl, Collection hosts) {
        return crebte(
            guid,
            ttl,
            RouterService.getPort(),
            RouterService.getAddress(),
            (long)RouterService.getNumShbredFiles(),
            (long)RouterService.getShbredFileSize()/1024,
            RouterService.isSupernode(),
            Stbtistics.instance().calculateDailyUptime(),
            UDPService.instbnce().isGUESSCapable(),
            ApplicbtionSettings.LANGUAGE.getValue().equals("") ?
                ApplicbtionSettings.DEFAULT_LOCALE.getValue() :
                ApplicbtionSettings.LANGUAGE.getValue(),
            RouterService.getConnectionMbnager()
                .getNumLimeWireLocblePrefSlots(),
            hosts);
    }
 
     /**
     * Crebtes a new PingReply for this host with the specified
     * GUID, TTL & return bddress.
     */   
    public stbtic PingReply create(byte[] guid, byte ttl, IpPort addr) {
        return crebte(guid, ttl, addr, Collections.EMPTY_LIST);
    }
    
    
    /**
     * Crebtes a new PingReply for this host with the specified
     * GUID, TTL, return bddress & packed hosts.
     */
    public stbtic PingReply create(byte[] guid, byte ttl,
                                   IpPort returnAddr, Collection hosts) {
        GGEP ggep = newGGEP(Stbtistics.instance().calculateDailyUptime(),
                            RouterService.isSupernode(),
                            UDPService.instbnce().isGUESSCapable());
                            
        String locble = ApplicationSettings.LANGUAGE.getValue().equals("") ?
                        ApplicbtionSettings.DEFAULT_LOCALE.getValue() :
                        ApplicbtionSettings.LANGUAGE.getValue();
        bddLocale(ggep, locale, RouterService.getConnectionManager()
                                        .getNumLimeWireLocblePrefSlots());
                                        
        bddAddress(ggep, returnAddr);
        bddPackedHosts(ggep, hosts);
        return crebte(guid,
                      ttl,
                      RouterService.getPort(),
                      RouterService.getAddress(),
                      (long)RouterService.getNumShbredFiles(),
                      (long)RouterService.getShbredFileSize()/1024,
                      RouterService.isSupernode(),
                      ggep);
    }

    /**
     * Crebtes a new <tt>PingReply</tt> for this host with the specified
     * GUID, ttl, bnd query key.
     *
     * @pbram guid the Globally Unique Identifier (GUID) for this message
     * @pbram ttl the time to live for this message
     * @pbram key the <tt>QueryKey</tt> for this reply
     */                                   
    public stbtic PingReply createQueryKeyReply(byte[] guid, byte ttl, 
                                                QueryKey key) {
        return crebte(guid, ttl, 
                      RouterService.getPort(),
                      RouterService.getAddress(),
                      RouterService.getNumShbredFiles(),
                      RouterService.getShbredFileSize()/1024,
                      RouterService.isSupernode(),
                      qkGGEP(key));
    }

    /**
     * Crebtes a new <tt>PingReply</tt> for this host with the specified
     * GUID, ttl, bnd query key.
     *
     * @pbram guid the Globally Unique Identifier (GUID) for this message
     * @pbram ttl the time to live for this message
     * @pbram key the <tt>QueryKey</tt> for this reply
     */                                   
    public stbtic PingReply createQueryKeyReply(byte[] guid, byte ttl, 
                                                int port, byte[] ip,
                                                long shbredFiles, 
                                                long shbredSize,
                                                boolebn ultrapeer,
                                                QueryKey key) {
        return crebte(guid, ttl, 
                      port,
                      ip,
                      shbredFiles,
                      shbredSize,
                      ultrbpeer,
                      qkGGEP(key));
    }

    /**
     * Crebtes a new <tt>PingReply</tt> for an external node -- the data
     * in the reply will not contbin data for this node.  In particular,
     * the dbta fields are set to zero because we do not know these
     * stbtistics for the other node.
     *
     * @pbram guid the Globally Unique Identifier (GUID) for this message
     * @pbram ttl the time to live for this message
     * @pbram port the port the remote host is listening on
     * @pbram address the address of the node
     */
    public stbtic PingReply 
        crebte(byte[] guid, byte ttl, int port, byte[] address) {
        return crebte(guid, ttl, port, address, 0, 0, false, -1, false); 
    }

    /**
     * Crebtes a new <tt>PingReply</tt> for an external node -- the data
     * in the reply will not contbin data for this node.  In particular,
     * the dbta fields are set to zero because we do not know these
     * stbtistics for the other node.
     *
     * @pbram guid the Globally Unique Identifier (GUID) for this message
     * @pbram ttl the time to live for this message
     * @pbram port the port the remote host is listening on
     * @pbram address the address of the node
     * @pbram ultrapeer whether or not we should mark this node as
     *  being bn Ultrapeer
     */
    public stbtic PingReply 
        crebteExternal(byte[] guid, byte ttl, int port, byte[] address,
                       boolebn ultrapeer) {
        return crebte(guid, ttl, port, address, 0, 0, ultrapeer, -1, false); 
    }

    /**
     * Crebtes a new <tt>PingReply</tt> for an external node -- the data
     * in the reply will not contbin data for this node.  In particular,
     * the dbta fields are set to zero because we do not know these
     * stbtistics for the other node.  This is primarily used for testing.
     *
     * @pbram guid the Globally Unique Identifier (GUID) for this message
     * @pbram ttl the time to live for this message
     * @pbram port the port the remote host is listening on
     * @pbram address the address of the node
     * @pbram ultrapeer whether or not we should mark this node as
     *  being bn Ultrapeer
     */
    public stbtic PingReply 
        crebteExternal(byte[] guid, byte ttl, int port, byte[] address,
                       int uptime,
                       boolebn ultrapeer) {
        return crebte(guid, ttl, port, address, 0, 0, ultrapeer, uptime, false); 
    }

    /**
     * Crebtes a new <tt>PingReply</tt> instance for a GUESS node.  This
     * method should only be cblled if the caller is sure that the given
     * node is, in fbct, a GUESS-capable node.  This method is only used
     * to crebte pongs for nodes other than ourselves.  
     *
     * @pbram guid the Globally Unique Identifier (GUID) for this message
     * @pbram ttl the time to live for this message
     * @pbram ep the <tt>Endpoint</tt> instance containing data about 
     *  the remote host
     */       
    public stbtic PingReply 
        crebteGUESSReply(byte[] guid, byte ttl, Endpoint ep) 
        throws UnknownHostException {
        return crebte(guid, ttl,
                      ep.getPort(),
                      ep.getHostBytes(),
                      0, 0, true, -1, true);        
    }

    /**
     * Crebtes a new <tt>PingReply</tt> instance for a GUESS node.  This
     * method should only be cblled if the caller is sure that the given
     * node is, in fbct, a GUESS-capable node.  This method is only used
     * to crebte pongs for nodes other than ourselves.  Given that this
     * reply is for b remote node, we do not know the data for number of
     * shbred files, etc, and so leave it blank.
     *
     * @pbram guid the Globally Unique Identifier (GUID) for this message
     * @pbram ttl the time to live for this message
     * @pbram port the port the remote host is listening on
     * @pbram address the address of the node
     */
    public stbtic PingReply 
        crebteGUESSReply(byte[] guid, byte ttl, int port, byte[] address) {
        return crebte(guid, ttl, port, address, 0, 0, true, -1, true); 
    }

    /**
     * Crebtes a new pong with the specified data -- used primarily for
     * testing!
     *
     * @pbram guid the sixteen byte message GUID
     * @pbram ttl the message TTL to use
     * @pbram port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @pbram ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  formbt e.g. {18, 239, 0, 144}.
     * @pbram files the number of files I'm sharing.  Must fit in 4 unsigned
     *  bytes, i.e., 0 < files < 2^32.
     * @pbram kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned bytes, i.e., 0 < files < 2^32.
     */
    public stbtic PingReply 
        crebte(byte[] guid, byte ttl,
               int port, byte[] ip, long files, long kbytes) {
        return crebte(guid, ttl, port, ip, files, kbytes, 
                      fblse, -1, false); 
    }


    /**
     * Crebtes a new ping from scratch with ultrapeer and daily uptime extension
     * dbta.
     *
     * @pbram guid the sixteen byte message GUID
     * @pbram ttl the message TTL to use
     * @pbram port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @pbram ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  formbt e.g. {18, 239, 0, 144}.
     * @pbram files the number of files I'm sharing.  Must fit in 4 unsigned
     *  bytes, i.e., 0 < files < 2^32.
     * @pbram kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned bytes, i.e., 0 < files < 2^32.
     * @pbram isUltrapeer true if this should be a marked ultrapeer pong,
     *  which sets kbytes to the nebrest power of 2 not less than 8.
     * @pbram dailyUptime my average daily uptime, in seconds, e.g., 
     *  3600 for one hour per dby.  Negative values mean "don't know".
     *  GGEP extension blocks bre allocated if dailyUptime is non-negative.  
     */
    public stbtic PingReply 
        crebte(byte[] guid, byte ttl,
               int port, byte[] ip, long files, long kbytes,
               boolebn isUltrapeer, int dailyUptime, boolean isGUESSCapable) {
        return crebte(guid, ttl, port, ip, files, kbytes, isUltrapeer,
                      newGGEP(dbilyUptime, isUltrapeer, isGUESSCapable));
    }
    
    /**
     * Crebtes a new PingReply with the specified data.
     */
    public stbtic PingReply create(byte[] guid, byte ttl,
      int port, byte[] ip, long files, long kbytes,
      boolebn isUltrapeer, int dailyUptime, boolean isGuessCapable,
      String locble, int slots) {    
        return crebte(guid, ttl, port, ip, files, kbytes, isUltrapeer,
                      dbilyUptime, isGuessCapable, locale, slots, Collections.EMPTY_LIST);
    }
    
    /**
     * crebtes a new PingReply with the specified locale
     *
     * @pbram guid the sixteen byte message GUID
     * @pbram ttl the message TTL to use
     * @pbram port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @pbram ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  formbt e.g. {18, 239, 0, 144}.
     * @pbram files the number of files I'm sharing.  Must fit in 4 unsigned
     *  bytes, i.e., 0 < files < 2^32.
     * @pbram kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned bytes, i.e., 0 < files < 2^32.
     * @pbram isUltrapeer true if this should be a marked ultrapeer pong,
     *  which sets kbytes to the nebrest power of 2 not less than 8.
     * @pbram dailyUptime my average daily uptime, in seconds, e.g., 
     *  3600 for one hour per dby.  Negative values mean "don't know".
     *  GGEP extension blocks bre allocated if dailyUptime is non-negative.  
     * @pbram isGuessCapable guess capable
     * @pbram locale the locale 
     * @pbram slots the number of locale preferencing slots available
     * @pbram hosts the hosts to pack into this PingReply
     */
    public stbtic PingReply
        crebte(byte[] guid, byte ttl,
               int port, byte[] ip, long files, long kbytes,
               boolebn isUltrapeer, int dailyUptime, boolean isGuessCapable,
               String locble, int slots, Collection hosts) {
        GGEP ggep = newGGEP(dbilyUptime, isUltrapeer, isGuessCapable);
        bddLocale(ggep, locale, slots);
        bddPackedHosts(ggep, hosts);
        return crebte(guid,
                      ttl,
                      port,
                      ip,
                      files,
                      kbytes,
                      isUltrbpeer,
                      ggep);
    }

    /**
     * Returns b new <tt>PingReply</tt> instance with all the same data
     * bs <tt>this</tt>, but with the specified GUID.
     *
     * @pbram guid the guid to use for the new <tt>PingReply</tt>
     * @return b new <tt>PingReply</tt> instance with the specified GUID
     *  bnd all of the data from this <tt>PingReply</tt>
     * @throws IllegblArgumentException if the guid is not 16 bytes or the input
     * (this') formbt is bad
     */
    public PingReply mutbteGUID(byte[] guid) {
        if (guid.length != 16)
            throw new IllegblArgumentException("bad guid size: " + guid.length);

        // i cbn't just call a new constructor, i have to recreate stuff
        try {
            return crebteFromNetwork(guid, getTTL(), getHops(), PAYLOAD); 
        }
        cbtch (BadPacketException ioe) {
            throw new IllegblArgumentException("Input pong was bad!");
        }

    }

    /**
     * Crebtes a new <tt>PingReply</tt> instance with the specified
     * criterib.
     *
     * @return b new <tt>PingReply</tt> instance containing the specified
     *  dbta
     */
    public stbtic PingReply 
        crebte(byte[] guid, byte ttl, int port, byte[] ipBytes, long files,
               long kbytes, boolebn isUltrapeer, GGEP ggep) {

 		if(!NetworkUtils.isVblidPort(port))
			throw new IllegblArgumentException("invalid port: "+port);
        if(!NetworkUtils.isVblidAddress(ipBytes))
            throw new IllegblArgumentException("invalid address: " +
                    NetworkUtils.ip2string(ipBytes));			
        
        InetAddress ip = null;
        try {
            ip = InetAddress.getByNbme(NetworkUtils.ip2string(ipBytes));
        } cbtch (UnknownHostException e) {
            throw new IllegblArgumentException(e.getMessage());
        }
        byte[] extensions = null;
        if(ggep != null) {
            ByteArrbyOutputStream baos = new ByteArrayOutputStream();
            try {
                ggep.write(bbos);
            } cbtch(IOException e) {
                // this should not hbppen
                ErrorService.error(e);
            }
            extensions = bbos.toByteArray();
        }
        int length = STANDARD_PAYLOAD_SIZE + 
            (extensions == null ? 0 : extensions.length);

        byte[] pbyload = new byte[length];
        //It's ok if cbsting port, files, or kbytes turns negative.
        ByteOrder.short2leb((short)port, pbyload, 0);
        //pbyload stores IP in BIG-ENDIAN
        pbyload[2]=ipBytes[0];
        pbyload[3]=ipBytes[1];
        pbyload[4]=ipBytes[2];
        pbyload[5]=ipBytes[3];
        ByteOrder.int2leb((int)files, pbyload, 6);
        ByteOrder.int2leb((int) (isUltrbpeer ? mark(kbytes) : kbytes), 
                          pbyload, 
                          10);
        
        //Encode GGEP block if included.
        if (extensions != null) {
            System.brraycopy(extensions, 0, 
                             pbyload, STANDARD_PAYLOAD_SIZE, 
                             extensions.length);
        }
        return new PingReply(guid, ttl, (byte)0, pbyload, ggep, ip);
    }


    /**
     * Crebtes a new <tt>PingReply</tt> instance from the network.
     *
     * @pbram guid the Globally Unique Identifier (GUID) for this message
     * @pbram ttl the time to live for this message
     * @pbram hops the hops for this message
     * @pbram payload the message payload
     * @throws <tt>BbdPacketException</tt> if the message is invalid for
     *  bny reason
     */
    public stbtic PingReply 
        crebteFromNetwork(byte[] guid, byte ttl, byte hops, byte[] payload) 
        throws BbdPacketException {
        if(guid == null) {
            throw new NullPointerException("null guid");
        }
        if(pbyload == null) {
            throw new NullPointerException("null pbyload");
        }
        if (pbyload.length < STANDARD_PAYLOAD_SIZE) {
            ReceivedErrorStbt.PING_REPLY_INVALID_PAYLOAD.incrementStat();
            throw new BbdPacketException("invalid payload length");   
        }
        int port = ByteOrder.ushort2int(ByteOrder.leb2short(pbyload,0));
 		if(!NetworkUtils.isVblidPort(port)) {
 		    ReceivedErrorStbt.PING_REPLY_INVALID_PORT.incrementStat();
			throw new BbdPacketException("invalid port: "+port); 
        }
 		
 		// this bddress may get updated if we have the UDPHC extention
 		// therefore it is checked bfter checking for that extention.
        String ipString = NetworkUtils.ip2string(pbyload, 2);
        
        InetAddress ip = null;
        
        GGEP ggep = pbrseGGEP(payload);
        
        if(ggep != null) {
            if(ggep.hbsKey(GGEP.GGEP_HEADER_VENDOR_INFO)) {
                byte[] vendorBytes = null;
                try {
                    vendorBytes = ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO);
                } cbtch (BadGGEPPropertyException e) {
                    ReceivedErrorStbt.PING_REPLY_INVALID_GGEP.incrementStat();
                    throw new BbdPacketException("bad GGEP: "+vendorBytes);
                }
                if(vendorBytes.length < 4) {
                    ReceivedErrorStbt.PING_REPLY_INVALID_VENDOR.incrementStat();
                    throw new BbdPacketException("invalid vendor length: "+
                                                 vendorBytes.length);
                }
            }

            if(ggep.hbsKey(GGEP.GGEP_HEADER_CLIENT_LOCALE)) {
                try {
                    byte[] clocble = 
                        ggep.getBytes(GGEP.GGEP_HEADER_CLIENT_LOCALE);
                }
                cbtch(BadGGEPPropertyException e) {
                    ReceivedErrorStbt.PING_REPLY_INVALID_GGEP.incrementStat();
                    throw new BbdPacketException("GGEP error : creating from"
                                                 + " network : client locble");
                }
            }
            
            if(ggep.hbsKey(GGEP.GGEP_HEADER_PACKED_IPPORTS)) {
                byte[] dbta = null;
                try {
                    dbta = ggep.getBytes(GGEP.GGEP_HEADER_PACKED_IPPORTS);
                } cbtch(BadGGEPPropertyException bad) {
                    throw new BbdPacketException(bad.getMessage());
                }
                if(dbta == null || data.length % 6 != 0)
                    throw new BbdPacketException("invalid data");
            }
            
            if(ggep.hbsKey(GGEP.GGEP_HEADER_PACKED_HOSTCACHES)) {
                try {
                    ggep.getBytes(GGEP.GGEP_HEADER_PACKED_HOSTCACHES);
                } cbtch(BadGGEPPropertyException bad) {
                    throw new BbdPacketException(bad.getMessage());
                }
            }
            
            if(ggep.hbsKey(GGEP.GGEP_HEADER_UDP_HOST_CACHE)) {
                try{
                    String dns = ggep.getString(GGEP.GGEP_HEADER_UDP_HOST_CACHE);
                    ip = InetAddress.getByNbme(dns);
                    ipString = ip.getHostAddress();
                }cbtch(BadGGEPPropertyException ignored) {
                }cbtch(UnknownHostException bad) {
                    throw new BbdPacketException(bad.getMessage());
                }
            }
                
        }

        if(!NetworkUtils.isVblidAddress(ipString)) {
            ReceivedErrorStbt.PING_REPLY_INVALID_ADDRESS.incrementStat();
            throw new BbdPacketException("invalid address: " + ipString);
        }
        
        if (ip==null) {
            try {
                ip = InetAddress.getByNbme(NetworkUtils.ip2string(payload, 2));
            } cbtch (UnknownHostException e) {
                throw new BbdPacketException("bad IP:"+ipString+" "+e.getMessage());
            }
        }
        return new PingReply(guid, ttl, hops, pbyload, ggep, ip);
    }
     
    /**
     * Sole <tt>PingReply</tt> constructor.  This estbblishes all ping
     * reply invbriants.
     * @pbram guid the Globally Unique Identifier (GUID) for this message
     * @pbram ttl the time to live for this message
     * @pbram hops the hops for this message
     * @pbram payload the message payload
     */
    privbte PingReply(byte[] guid, byte ttl, byte hops, byte[] payload,
                      GGEP ggep, InetAddress ip) {
        super(guid, Messbge.F_PING_REPLY, ttl, hops, payload.length);
        PAYLOAD = pbyload;
        PORT = ByteOrder.ushort2int(ByteOrder.leb2short(PAYLOAD,0));
        FILES = ByteOrder.uint2long(ByteOrder.leb2int(PAYLOAD,6));
        KILOBYTES = ByteOrder.uint2long(ByteOrder.leb2int(PAYLOAD,10));

        IP = ip;

        // GGEP pbrsing
        //GGEP ggep = pbrseGGEP();
        int dbilyUptime = -1;
        boolebn supportsUnicast = false;
        String vendor = "";
        int vendorMbjor = -1;
        int vendorMinor = -1;
        
        int freeLebfSlots = -1;
        int freeUltrbpeerSlots = -1;
        QueryKey key = null;
        
        String locble /** def. val from settings? */
            = ApplicbtionSettings.DEFAULT_LOCALE.getValue(); 
        int slots = -1; //-1 didn't get it.
        InetAddress myIP=null;
        int myPort=0;
        List pbckedIPs = Collections.EMPTY_LIST;
        List pbckedCaches = Collections.EMPTY_LIST;
        String cbcheAddress = null;
        
        // TODO: the exceptions thrown here bre messy
        if(ggep != null) {
            if(ggep.hbsKey(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME)) {
                try {
                    dbilyUptime = 
                        ggep.getInt(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME); 
                } cbtch(BadGGEPPropertyException e) {}
            }

            supportsUnicbst = 
                ggep.hbsKey(GGEP.GGEP_HEADER_UNICAST_SUPPORT); 

            if(ggep.hbsKey(GGEP.GGEP_HEADER_VENDOR_INFO)) {
                try {
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO);
                    if(bytes.length >= 4)
                        vendor = new String(bytes, 0, 4);   
                    if(bytes.length > 4) {
                        vendorMbjor = bytes[4] >> 4;
                        vendorMinor = bytes[4] & 0xF;
                    }
                } cbtch (BadGGEPPropertyException e) {}
             }

            if (ggep.hbsKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT)) {
                try {
                    byte[] bytes = 
                        ggep.getBytes(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
                    if(QueryKey.isVblidQueryKeyBytes(bytes))
                        key = QueryKey.getQueryKey(bytes, fblse);
                } cbtch (BadGGEPPropertyException corrupt) {}
            }
            
            if(ggep.hbsKey((GGEP.GGEP_HEADER_UP_SUPPORT))) {
                try {
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_UP_SUPPORT);
                    if(bytes.length >= 3) {
                        freeLebfSlots = bytes[1];
                        freeUltrbpeerSlots = bytes[2];
                    }
                } cbtch (BadGGEPPropertyException e) {}
            }
            
            if(ggep.hbsKey(GGEP.GGEP_HEADER_CLIENT_LOCALE)) {
                try {
                    byte[] bytes = ggep.getBytes(GGEP.GGEP_HEADER_CLIENT_LOCALE);
                    if(bytes.length >= 2)
                        locble = new String(bytes, 0, 2);
                    if(bytes.length >= 3)
                        slots = ByteOrder.ubyte2int(bytes[2]);
                } cbtch(BadGGEPPropertyException e) {}
            }
            
            if (ggep.hbsKey(GGEP.GGEP_HEADER_IPPORT)) {
                try{
                    byte[] dbta = ggep.getBytes(GGEP.GGEP_HEADER_IPPORT);

                    byte [] myip = new byte[4];
                    // only copy the bddr if the data is atleast 6
                    // bytes (ip + port).  thbt way isValidAddress
                    // will fbil & we don't need to recheck the length
                    // when getting the port.
                    if(dbta.length >= 6)
                        System.brraycopy(data,0,myip,0,4);
                    
                    if (NetworkUtils.isVblidAddress(myip)) {
                        try{
                            myIP = NetworkUtils.getByAddress(myip);
                            myPort = ByteOrder.ushort2int(ByteOrder.leb2short(dbta,4));
                            
                            if (NetworkUtils.isPrivbteAddress(myIP) ||
                                    !NetworkUtils.isVblidPort(myPort) ) {
                                // librs, or we are behind a NAT and there is LAN outside
                                // either wby we can't use it
                                myIP=null;
                                myPort=0;
                            }
                            
                        }cbtch(UnknownHostException bad) {
                            //keep the ip bddress null and the port 0
                        }
                    }
                }cbtch(BadGGEPPropertyException ignored) {}
            }
            
            if(ggep.hbsKey(GGEP.GGEP_HEADER_UDP_HOST_CACHE)) {
                cbcheAddress = "";
                try {
                    cbcheAddress = ggep.getString(GGEP.GGEP_HEADER_UDP_HOST_CACHE);
                } cbtch(BadGGEPPropertyException bad) {}
            }
            
            if(ggep.hbsKey(GGEP.GGEP_HEADER_PACKED_IPPORTS)) {
                try {
                    byte[] dbta = ggep.getBytes(GGEP.GGEP_HEADER_PACKED_IPPORTS);
                    pbckedIPs = NetworkUtils.unpackIps(data);
                } cbtch(BadGGEPPropertyException bad) {
                } cbtch(BadPacketException bpe) {}
            }
            
            if(ggep.hbsKey(GGEP.GGEP_HEADER_PACKED_HOSTCACHES)) {
                try {
                    String dbta = ggep.getString(GGEP.GGEP_HEADER_PACKED_HOSTCACHES);
                    pbckedCaches = listCaches(data);
                } cbtch(BadGGEPPropertyException bad) {}
            }
        }
        
        MY_IP = myIP;
        MY_PORT = myPort;
        HAS_GGEP_EXTENSION = ggep != null;
        DAILY_UPTIME = dbilyUptime;
        SUPPORTS_UNICAST = supportsUnicbst;
        VENDOR = vendor;
        VENDOR_MAJOR_VERSION = vendorMbjor;
        VENDOR_MINOR_VERSION = vendorMinor;
        QUERY_KEY = key;
        FREE_LEAF_SLOTS = freeLebfSlots;
        FREE_ULTRAPEER_SLOTS = freeUltrbpeerSlots;
        CLIENT_LOCALE = locble;
        FREE_LOCALE_SLOTS = slots;
        if(cbcheAddress != null && "".equals(cacheAddress))
            UDP_CACHE_ADDRESS = getAddress();
        else
            UDP_CACHE_ADDRESS = cbcheAddress;
        PACKED_IP_PORTS = pbckedIPs;
        PACKED_UDP_HOST_CACHES = pbckedCaches;
    }


    /** Returns the GGEP pbyload bytes to encode the given uptime */
    privbte static GGEP newGGEP(int dailyUptime, boolean isUltrapeer,
                                boolebn isGUESSCapable) {
        GGEP ggep=new GGEP(true);
        
        if (dbilyUptime >= 0)
            ggep.put(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME, dbilyUptime);
        
        if (isGUESSCbpable && isUltrapeer) {
            // indicbte guess support
            byte[] vNum = {
                convertToGUESSFormbt(CommonUtils.getGUESSMajorVersionNumber(),
                                     CommonUtils.getGUESSMinorVersionNumber())};
            ggep.put(GGEP.GGEP_HEADER_UNICAST_SUPPORT, vNum);
        }
        
        // indicbte UP support
        if (isUltrbpeer)
            bddUltrapeerExtension(ggep);
        
        // bll pongs should have vendor info
        ggep.put(GGEP.GGEP_HEADER_VENDOR_INFO, CACHED_VENDOR); 

        return ggep;
    }


    /** Returns the GGEP pbyload bytes to encode the given QueryKey */
    privbte static GGEP qkGGEP(QueryKey queryKey) {
        try {
            GGEP ggep=new GGEP(true);

            // get qk bytes....
            ByteArrbyOutputStream baos=new ByteArrayOutputStream();
            queryKey.write(bbos);
            // populbte GGEP....
            ggep.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT, bbos.toByteArray());

            return ggep;
        } cbtch (IOException e) {
            //See bbove.
            Assert.thbt(false, "Couldn't encode QueryKey" + queryKey);
            return null;
        }
    }

    /**
     * Adds the locble GGEP.
     */
    privbte static GGEP addLocale(GGEP ggep, String locale, int slots) {
        byte[] pbyload = new byte[3];
        byte[] s = locble.getBytes();
        pbyload[0] = s[0];
        pbyload[1] = s[1];
        pbyload[2] = (byte)slots;
        ggep.put(GGEP.GGEP_HEADER_CLIENT_LOCALE, pbyload);
        return ggep;
    }
    
    /**
     * Adds the bddress GGEP.
     */
    privbte static GGEP addAddress(GGEP ggep, IpPort address) {
        byte[] pbyload = new byte[6];
        System.brraycopy(address.getInetAddress().getAddress(), 0, payload, 0, 4);
        ByteOrder.short2leb((short)bddress.getPort(), payload, 4);
        ggep.put(GGEP.GGEP_HEADER_IPPORT,pbyload);
        return ggep;
    }
    
    /**
     * Adds the pbcked hosts into this GGEP.
     */
    privbte static GGEP addPackedHosts(GGEP ggep, Collection hosts) {
        if(hosts == null || hosts.isEmpty())
            return ggep;
            
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, NetworkUtils.pbckIpPorts(hosts));
        return ggep;
    }

    /**
     * Adds the ultrbpeer GGEP extension to the pong.  This has the version of
     * the Ultrbpeer protocol that we support as well as the number of free
     * lebf and Ultrapeer slots available.
     * 
     * @pbram ggep the <tt>GGEP</tt> instance to add the extension to
     */
    privbte static void addUltrapeerExtension(GGEP ggep) {
        byte[] pbyload = new byte[3];
        // put version
        pbyload[0] = convertToGUESSFormat(CommonUtils.getUPMajorVersionNumber(),
                                          CommonUtils.getUPMinorVersionNumber()
                                          );
        pbyload[1] = (byte) RouterService.getNumFreeLimeWireLeafSlots();
        pbyload[2] = (byte) RouterService.getNumFreeLimeWireNonLeafSlots();

        // bdd it
        ggep.put(GGEP.GGEP_HEADER_UP_SUPPORT, pbyload);
    }

    /** puts mbjor as the high order bits, minor as the low order bits.
     *  @exception IllegblArgumentException thrown if major/minor is greater 
     *  thbn 15 or less than 0.
     */
    privbte static byte convertToGUESSFormat(int major, int minor) 
        throws IllegblArgumentException {
        if ((mbjor < 0) || (minor < 0) || (major > 15) || (minor > 15))
            throw new IllegblArgumentException();
        // set mbjor
        int retInt = mbjor;
        retInt = retInt << 4;
        // set minor
        retInt |= minor;

        return (byte) retInt;
    }

    /**
     * Returns whether or not this pong is reporting bny free slots on the 
     * remote host, either lebf or ultrapeer.
     * 
     * @return <tt>true</tt> if the remote host hbs any free leaf or ultrapeer
     *  slots, otherwise <tt>fblse</tt>
     */
    public boolebn hasFreeSlots() {
        return hbsFreeLeafSlots() || hasFreeUltrapeerSlots();    
    }
    
    /**
     * Returns whether or not this pong is reporting free lebf slots on the 
     * remote host.
     * 
     * @return <tt>true</tt> if the remote host hbs any free leaf slots, 
     *  otherwise <tt>fblse</tt>
     */
    public boolebn hasFreeLeafSlots() {
        return FREE_LEAF_SLOTS > 0;
    }

    /**
     * Returns whether or not this pong is reporting free ultrbpeer slots on  
     * the remote host.
     * 
     * @return <tt>true</tt> if the remote host hbs any free ultrapeer slots, 
     *  otherwise <tt>fblse</tt>
     */
    public boolebn hasFreeUltrapeerSlots() {
        return FREE_ULTRAPEER_SLOTS > 0;
    }
    
    /**
     * Accessor for the number of free lebf slots reported by the remote host.
     * This will return -1 if the remote host did not include the necessbry 
     * GGEP block reporting slots.
     * 
     * @return the number of free lebf slots, or -1 if the remote host did not
     *  include this informbtion
     */
    public int getNumLebfSlots() {
        return FREE_LEAF_SLOTS;
    }

    /**
     * Accessor for the number of free ultrbpeer slots reported by the remote 
     * host.  This will return -1 if the remote host did not include the  
     * necessbry GGEP block reporting slots.
     * 
     * @return the number of free ultrbpeer slots, or -1 if the remote host did 
     *  not include this informbtion
     */    
    public int getNumUltrbpeerSlots() {
        return FREE_ULTRAPEER_SLOTS;
    }

    protected void writePbyload(OutputStream out) throws IOException {
        out.write(PAYLOAD);
		SentMessbgeStatHandler.TCP_PING_REPLIES.addMessage(this);
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
     * Returns the ip field in stbndard dotted decimal format, e.g.,
     * "127.0.0.1".  The most significbnt byte is written first.
     */
    public String getAddress() { 
        return IP.getHostAddress();
    }

    /**
     * Returns the ip bddress bytes (MSB first)
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
     * Accessor for the number of files shbred, as reported in the
     * pong.
     *
     * @return the number of files reported shbred
     */
    public long getFiles() {
        return FILES;
    }

    /**
     * Accessor for the number of kilobytes shbred, as reported in the
     * pong.
     *
     * @return the number of kilobytes reported shbred
     */
    public long getKbytes() {
        return KILOBYTES;
    }

    /** Returns the bverage daily uptime in seconds from the GGEP payload.
     *  If the pong did not report b daily uptime, returns -1.
     *
     * @return the dbily uptime reported in the pong, or -1 if the uptime
     *  wbs not present or could not be read
     */
    public int getDbilyUptime() {
        return DAILY_UPTIME;
    }


    /** Returns whether or not this host support unicbst, GUESS-style
     *  queries.
     *
     * @return <tt>true</tt> if this host does support GUESS-style queries,
     *  otherwise <tt>fblse</tt>
     */
    public boolebn supportsUnicast() {
        return SUPPORTS_UNICAST;
    }


    /** Returns the 4-chbracter vendor string associated with this Pong.
     *
     * @return the 4-chbracter vendor code reported in the pong, or the
     *  empty string if no vendor code wbs successfully read
     */
    public String getVendor() {
        return VENDOR;
    }


    /** Returns the mbjor version number of the vendor returning this pong.
     * 
     * @return the mbjor version number of the vendor returning this pong,
     *  or -1 if the version could not be rebd
     */
    public int getVendorMbjorVersion() {
        return VENDOR_MAJOR_VERSION;
    }

    /** Returns the minor version number of the vendor returning this pong.
     * 
     * @return the minor version number of the vendor returning this pong,
     *  or -1 if the version could not be rebd
     */
    public int getVendorMinorVersion() {
        return VENDOR_MINOR_VERSION;
    }


    /** Returns the QueryKey (if bny) associated with this pong.  May be null!
     *
     * @return the <tt>QueryKey</tt> for this pong, or <tt>null</tt> if no
     *  key wbs specified
     */
    public QueryKey getQueryKey() {
        return QUERY_KEY;
    }
    
    /**
     * Gets the list of pbcked IP/Ports.
     */
    public List /* of IpPort */ getPbckedIPPorts() {
        return PACKED_IP_PORTS;
    }
    
    /**
     * Gets b list of packed IP/Ports of UDP Host Caches.
     */
    public List /* of IpPort */ getPbckedUDPHostCaches() {
        return PACKED_UDP_HOST_CACHES;
    }

    /**
     * Returns whether or not this pong hbs a GGEP extension.
     *
     * @return <tt>true</tt> if the pong hbs a GGEP extension, otherwise
     *  <tt>fblse</tt>
     */
    public boolebn hasGGEPExtension() {
        return HAS_GGEP_EXTENSION;
    }
    
    // TODO : chbnge this to look for multiple GGEP block in the payload....
    /** Ensure GGEP dbta parsed...if possible. */
    privbte static GGEP parseGGEP(final byte[] PAYLOAD) {
        //Return if this is b plain pong without space for GGEP.  If 
        //this hbs bad GGEP data, multiple calls to
        //pbrseGGEP will result in multiple parse attempts.  While this is
        //inefficient, it is sufficiently rbre to not justify a parsedGGEP
        //vbriable.
        if (PAYLOAD.length <= STANDARD_PAYLOAD_SIZE)
            return null;
    
        try {
            return new GGEP(PAYLOAD, STANDARD_PAYLOAD_SIZE, null);
        } cbtch (BadGGEPBlockException e) { 
            return null;
        }
    }


    // inherit doc comment from messbge superclass
    public Messbge stripExtendedPayload() {
        //TODO: if this is too slow, we cbn alias parts of this, as as the
        //pbyload.  In fact we could even return a subclass of PingReply that
        //simply delegbtes to this.
        byte[] newPbyload=new byte[STANDARD_PAYLOAD_SIZE];
        System.brraycopy(PAYLOAD, 0,
                         newPbyload, 0,
                         STANDARD_PAYLOAD_SIZE);

        return new PingReply(this.getGUID(), this.getTTL(), this.getHops(),
                             newPbyload, null, IP);
    }
    
    /**
     * Unzips dbta about UDP host caches & returns a list of'm.
     */
    privbte List listCaches(String allCaches) {
        List theCbches = new LinkedList();
        StringTokenizer st = new StringTokenizer(bllCaches, "\n");
        while(st.hbsMoreTokens()) {
            String next = st.nextToken();
            // look for possible febtures and ignore'm
            int i = next.indexOf("&");
            // bbsically ignore.
            if(i != -1)
                next = next.substring(0, i);
            i = next.indexOf(":");
            int port = 6346;
            if(i == 0 || i == next.length()) {
                continue;
            } else if(i != -1) {
                try {
                    port = Integer.vblueOf(next.substring(i+1)).intValue();
                } cbtch(NumberFormatException invalid) {
                    continue;
                }
            } else {
                i = next.length(); // setup for i-1 below.
            }
            if(!NetworkUtils.isVblidPort(port))
                continue;
            String host = next.substring(0, i);
            try {
                theCbches.add(new IpPortImpl(host, port));
            } cbtch(UnknownHostException invalid) {
                continue;
            }
        }
        return Collections.unmodifibbleList(theCaches);
    }


    ////////////////////////// Pong Mbrking //////////////////////////

    /** 
     * Returns true if this messbge is "marked", i.e., likely from an
     * Ultrbpeer. 
     *
     * @return <tt>true</tt> if this pong is mbrked as an Ultrapeer pong,
     *  otherwise <tt>fblse</tt>
     */
    public boolebn isUltrapeer() {
        //Returns true if kb is b power of two greater than or equal to eight.
        long kb = getKbytes();
        if (kb < 8)
            return fblse;
        return isPowerOf2(ByteOrder.long2int(kb));
    }

    public stbtic boolean isPowerOf2(int x) {  //package access for testability
        if (x<=0)
            return fblse;
        else
            return (x&(x - 1)) == 0;
    }

	// inherit doc comment
	public void recordDrop() {
		DroppedSentMessbgeStatHandler.TCP_PING_REPLIES.addMessage(this);
	}

    /** Mbrks the given kbytes field */
    privbte static long mark(long kbytes) {
        int x=ByteOrder.long2int(kbytes);
        //Returns the power of two nebrest to x.  TODO3: faster algorithms are
        //possible.  At the lebst, you can do binary search.  I imagine some bit
        //operbtions can be done as well.  This brute-force approach was
        //generbted with the help of the the following Python program:
        //
        //  for i in xrbnge(0, 32):
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
            ", free ultrbpeers slots: "+hasFreeUltrapeerSlots()+
            ", free lebf slots: "+hasFreeLeafSlots()+
            ", vendor: "+VENDOR+" "+VENDOR_MAJOR_VERSION+"."+
                VENDOR_MINOR_VERSION+
            ", "+super.toString()+
            ", locble : " + CLIENT_LOCALE + ")";
    }

    /**
     * Implements <tt>IpPort</tt> interfbce.  Returns the <tt>InetAddress</tt>
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
     * bccess the client_locale
     */
    public String getClientLocble() {
        return CLIENT_LOCALE;
    }

    public int getNumFreeLocbleSlots() {
        return FREE_LOCALE_SLOTS;
    }
    
    /**
     * Accessor for host cbcheness.
     */
    public boolebn isUDPHostCache() {
        return UDP_CACHE_ADDRESS != null;
    }
    
    /**
     * Gets the UDP host cbche address.
     */
    public String getUDPCbcheAddress() {
        return UDP_CACHE_ADDRESS;
    }

    //Unit test: tests/com/limegroup/gnutellb/messages/PingReplyTest
}
