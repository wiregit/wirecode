package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import org.limewire.collection.BitNumbers;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.security.AddressSecurityToken;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.Statistics;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.SSLSettings;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.LimeWireUtils;

public class PingReplyFactoryImpl implements PingReplyFactory {

    private final NetworkManager networkManager;

    public PingReplyFactoryImpl(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public PingReply create(byte[] guid, byte ttl,
            Collection<? extends IpPort> gnutHosts,
            Collection<? extends IpPort> dhtHosts) {
        return create(
                guid,
                ttl,
                networkManager.getPort(),
                networkManager.getAddress(),
                RouterService.getNumSharedFiles(),
                RouterService.getSharedFileSize() / 1024,
                RouterService.isSupernode(),
                Statistics.instance().calculateDailyUptime(),
                ProviderHacks.getUdpService().isGUESSCapable(),
                ApplicationSettings.LANGUAGE.getValue().equals("") ? ApplicationSettings.DEFAULT_LOCALE
                        .getValue()
                        : ApplicationSettings.LANGUAGE.getValue(),
                RouterService.getConnectionManager()
                        .getNumLimeWireLocalePrefSlots(), gnutHosts, dhtHosts);
    }

    public PingReply create(byte[] guid, byte ttl) {
        return create(guid, ttl, IpPort.EMPTY_LIST, IpPort.EMPTY_LIST);
    }

    public PingReply create(byte[] guid, byte ttl, IpPort addr) {
        return create(guid, ttl, addr, IpPort.EMPTY_LIST, IpPort.EMPTY_LIST);
    }

    public PingReply create(byte[] guid, byte ttl, IpPort returnAddr,
            Collection<? extends IpPort> gnutHosts,
            Collection<? extends IpPort> dhtHosts) {
        GGEP ggep = newGGEP(Statistics.instance().calculateDailyUptime(),
                RouterService.isSupernode(), ProviderHacks.getUdpService()
                        .isGUESSCapable());

        String locale = ApplicationSettings.LANGUAGE.getValue().equals("") ? ApplicationSettings.DEFAULT_LOCALE
                .getValue()
                : ApplicationSettings.LANGUAGE.getValue();
        addLocale(ggep, locale, RouterService.getConnectionManager()
                .getNumLimeWireLocalePrefSlots());

        addAddress(ggep, returnAddr);

        addPackedHosts(ggep, gnutHosts, dhtHosts);

        return create(guid, ttl, networkManager.getPort(), networkManager
                .getAddress(), RouterService.getNumSharedFiles(), RouterService
                .getSharedFileSize() / 1024, RouterService.isSupernode(), ggep);
    }

    public PingReply createQueryKeyReply(byte[] guid, byte ttl,
            AddressSecurityToken key) {
        return create(guid, ttl, networkManager.getPort(), networkManager
                .getAddress(), RouterService.getNumSharedFiles(), RouterService
                .getSharedFileSize() / 1024, RouterService.isSupernode(),
                qkGGEP(key));
    }

    public PingReply createQueryKeyReply(byte[] guid, byte ttl, int port,
            byte[] ip, long sharedFiles, long sharedSize, boolean ultrapeer,
            AddressSecurityToken key) {
        return create(guid, ttl, port, ip, sharedFiles, sharedSize, ultrapeer,
                qkGGEP(key));
    }

    public PingReply create(byte[] guid, byte ttl, int port, byte[] address) {
        return create(guid, ttl, port, address, 0, 0, false, -1, false);
    }

    public PingReply createExternal(byte[] guid, byte ttl, int port,
            byte[] address, boolean ultrapeer) {
        return create(guid, ttl, port, address, 0, 0, ultrapeer, -1, false);
    }

    public PingReply createExternal(byte[] guid, byte ttl, int port,
            byte[] address, int uptime, boolean ultrapeer) {
        return create(guid, ttl, port, address, 0, 0, ultrapeer, uptime, false);
    }

    public PingReply createGUESSReply(byte[] guid, byte ttl, Endpoint ep)
            throws UnknownHostException {
        return create(guid, ttl, ep.getPort(), ep.getHostBytes(), 0, 0, true,
                -1, true);
    }

    public PingReply createGUESSReply(byte[] guid, byte ttl, int port,
            byte[] address) {
        return create(guid, ttl, port, address, 0, 0, true, -1, true);
    }

    public PingReply create(byte[] guid, byte ttl, int port, byte[] ip,
            long files, long kbytes) {
        return create(guid, ttl, port, ip, files, kbytes, false, -1, false);
    }

    public PingReply create(byte[] guid, byte ttl, int port, byte[] ip,
            long files, long kbytes, boolean isUltrapeer, int dailyUptime,
            boolean isGUESSCapable) {
        return create(guid, ttl, port, ip, files, kbytes, isUltrapeer, newGGEP(
                dailyUptime, isUltrapeer, isGUESSCapable));
    }

    public PingReply create(byte[] guid, byte ttl, int port, byte[] ip,
            long files, long kbytes, boolean isUltrapeer, int dailyUptime,
            boolean isGuessCapable, String locale, int slots) {
        return create(guid, ttl, port, ip, files, kbytes, isUltrapeer,
                dailyUptime, isGuessCapable, locale, slots, IpPort.EMPTY_LIST,
                IpPort.EMPTY_LIST);
    }

    public PingReply create(byte[] guid, byte ttl, int port, byte[] ip,
            long files, long kbytes, boolean isUltrapeer, int dailyUptime,
            boolean isGuessCapable, String locale, int slots,
            Collection<? extends IpPort> gnutHosts,
            Collection<? extends IpPort> dhtHosts) {
        GGEP ggep = newGGEP(dailyUptime, isUltrapeer, isGuessCapable);
        addLocale(ggep, locale, slots);
        addPackedHosts(ggep, gnutHosts, dhtHosts);
        return create(guid, ttl, port, ip, files, kbytes, isUltrapeer, ggep);
    }

    public PingReply create(byte[] guid, byte ttl, int port, byte[] ipBytes,
            long files, long kbytes, boolean isUltrapeer, GGEP ggep) {

        if (!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentException("invalid port: " + port);
        if (!NetworkUtils.isValidAddress(ipBytes))
            throw new IllegalArgumentException("invalid address: "
                    + NetworkUtils.ip2string(ipBytes));

        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(NetworkUtils.ip2string(ipBytes));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
        byte[] extensions = null;
        if (ggep != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ggep.write(baos);
            } catch (IOException e) {
                // this should not happen
                ErrorService.error(e);
            }
            extensions = baos.toByteArray();
        }
        int length = PingReply.STANDARD_PAYLOAD_SIZE
                + (extensions == null ? 0 : extensions.length);

        byte[] payload = new byte[length];
        //It's ok if casting port, files, or kbytes turns negative.
        ByteOrder.short2leb((short) port, payload, 0);
        //payload stores IP in BIG-ENDIAN
        payload[2] = ipBytes[0];
        payload[3] = ipBytes[1];
        payload[4] = ipBytes[2];
        payload[5] = ipBytes[3];
        ByteOrder.int2leb((int) files, payload, 6);
        ByteOrder.int2leb((int) (isUltrapeer ? mark(kbytes) : kbytes), payload,
                10);

        //Encode GGEP block if included.
        if (extensions != null) {
            System.arraycopy(extensions, 0, payload,
                    PingReply.STANDARD_PAYLOAD_SIZE, extensions.length);
        }

        try {
            return new PingReply(guid, ttl, (byte) 0, payload, ggep, ip,
                    Network.UNKNOWN);
        } catch (BadPacketException e) {
            throw new IllegalStateException(e);
        }
    }

    public PingReply createFromNetwork(byte[] guid, byte ttl, byte hops,
            byte[] payload) throws BadPacketException {
        return createFromNetwork(guid, ttl, hops, payload, Network.UNKNOWN);
    }

    public PingReply createFromNetwork(byte[] guid, byte ttl, byte hops,
            byte[] payload, Network network) throws BadPacketException {
        if (guid == null) {
            throw new NullPointerException("null guid");
        }
        if (payload == null) {
            throw new NullPointerException("null payload");
        }
        if (payload.length < PingReply.STANDARD_PAYLOAD_SIZE) {
            ReceivedErrorStat.PING_REPLY_INVALID_PAYLOAD.incrementStat();
            throw new BadPacketException("invalid payload length");
        }
        int port = ByteOrder.ushort2int(ByteOrder.leb2short(payload, 0));
        if (!NetworkUtils.isValidPort(port)) {
            ReceivedErrorStat.PING_REPLY_INVALID_PORT.incrementStat();
            throw new BadPacketException("invalid port: " + port);
        }

        // this address may get updated if we have the UDPHC extention
        // therefore it is checked after checking for that extention.
        String ipString = NetworkUtils.ip2string(payload, 2);

        InetAddress ip = null;

        GGEP ggep = parseGGEP(payload);

        if (ggep != null) {
            if (ggep.hasKey(GGEP.GGEP_HEADER_VENDOR_INFO)) {
                byte[] vendorBytes = null;
                try {
                    vendorBytes = ggep.getBytes(GGEP.GGEP_HEADER_VENDOR_INFO);
                } catch (BadGGEPPropertyException e) {
                    ReceivedErrorStat.PING_REPLY_INVALID_GGEP.incrementStat();
                    throw new BadPacketException("bad GGEP: " + vendorBytes);
                }
                if (vendorBytes.length < 4) {
                    ReceivedErrorStat.PING_REPLY_INVALID_VENDOR.incrementStat();
                    throw new BadPacketException("invalid vendor length: "
                            + vendorBytes.length);
                }
            }

            if (ggep.hasKey(GGEP.GGEP_HEADER_CLIENT_LOCALE)) {
                try {
                    ggep.getBytes(GGEP.GGEP_HEADER_CLIENT_LOCALE);
                } catch (BadGGEPPropertyException e) {
                    ReceivedErrorStat.PING_REPLY_INVALID_GGEP.incrementStat();
                    throw new BadPacketException("GGEP error : creating from"
                            + " network : client locale");
                }
            }

            if (ggep.hasKey(GGEP.GGEP_HEADER_PACKED_IPPORTS)) {
                byte[] data = null;
                try {
                    data = ggep.getBytes(GGEP.GGEP_HEADER_PACKED_IPPORTS);
                } catch (BadGGEPPropertyException bad) {
                    throw new BadPacketException(bad.getMessage());
                }
                if (data == null || data.length % 6 != 0)
                    throw new BadPacketException("invalid data");
            }

            if (ggep.hasKey(GGEP.GGEP_HEADER_PACKED_HOSTCACHES)) {
                try {
                    ggep.getBytes(GGEP.GGEP_HEADER_PACKED_HOSTCACHES);
                } catch (BadGGEPPropertyException bad) {
                    throw new BadPacketException(bad.getMessage());
                }
            }

            if (ggep.hasKey(GGEP.GGEP_HEADER_UDP_HOST_CACHE)) {
                try {
                    String dns = ggep
                            .getString(GGEP.GGEP_HEADER_UDP_HOST_CACHE);
                    ip = InetAddress.getByName(dns);
                    ipString = ip.getHostAddress();
                } catch (BadGGEPPropertyException ignored) {
                } catch (UnknownHostException bad) {
                    throw new BadPacketException(bad.getMessage());
                }
            }

        }

        if (!NetworkUtils.isValidAddress(ipString)) {
            ReceivedErrorStat.PING_REPLY_INVALID_ADDRESS.incrementStat();
            throw new BadPacketException("invalid address: " + ipString);
        }

        if (ip == null) {
            try {
                ip = InetAddress.getByName(NetworkUtils.ip2string(payload, 2));
            } catch (UnknownHostException e) {
                throw new BadPacketException("bad IP:" + ipString + " "
                        + e.getMessage());
            }
        }
        return new PingReply(guid, ttl, hops, payload, ggep, ip, network);
    }

    public PingReply mutateGUID(PingReply pingReply, byte[] guid) {
        if (guid.length != 16)
            throw new IllegalArgumentException("bad guid size: " + guid.length);

        // i can't just call a new constructor, i have to recreate stuff
        try {
            return createFromNetwork(guid, pingReply.getTTL(), pingReply
                    .getHops(), pingReply.getPayload(), pingReply.getNetwork());
        } catch (BadPacketException ioe) {
            throw new IllegalArgumentException("Input pong was bad!");
        }

    }

    /** Returns the GGEP payload bytes to encode the given uptime */
    private GGEP newGGEP(int dailyUptime, boolean isUltrapeer,
            boolean isGUESSCapable) {
        GGEP ggep = new GGEP();

        if (dailyUptime >= 0)
            ggep.put(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME, dailyUptime);

        if (isGUESSCapable && isUltrapeer) {
            // indicate guess support
            byte[] vNum = { PingReply.convertToGUESSFormat(LimeWireUtils
                    .getGUESSMajorVersionNumber(), LimeWireUtils
                    .getGUESSMinorVersionNumber()) };
            ggep.put(GGEP.GGEP_HEADER_UNICAST_SUPPORT, vNum);
        }

        // indicate UP support
        if (isUltrapeer)
            addUltrapeerExtension(ggep);

        // add DHT support
        addDHTExtension(ggep);

        // all pongs should have vendor info
        ggep.put(GGEP.GGEP_HEADER_VENDOR_INFO, PingReply.CACHED_VENDOR);

        // add our support of TLS
        if (SSLSettings.isIncomingTLSEnabled())
            ggep.put(GGEP.GGEP_HEADER_TLS_CAPABLE);

        return ggep;
    }

    /** Returns the GGEP payload bytes to encode the given AddressSecurityToken */
    private GGEP qkGGEP(AddressSecurityToken addressSecurityToken) {
        try {
            GGEP ggep = new GGEP();

            // get qk bytes....
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            addressSecurityToken.write(baos);
            // populate GGEP....
            ggep.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT, baos.toByteArray());

            return ggep;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Couldn't encode AddressSecurityToken"
                            + addressSecurityToken, e);
        }
    }

    /**
     * Adds the locale GGEP.
     */
    private GGEP addLocale(GGEP ggep, String locale, int slots) {
        byte[] payload = new byte[3];
        byte[] s = locale.getBytes();
        payload[0] = s[0];
        payload[1] = s[1];
        payload[2] = (byte) slots;
        ggep.put(GGEP.GGEP_HEADER_CLIENT_LOCALE, payload);
        return ggep;
    }

    /**
     * Adds the address GGEP.
     */
    private GGEP addAddress(GGEP ggep, IpPort address) {
        byte[] payload = new byte[6];
        System.arraycopy(address.getInetAddress().getAddress(), 0, payload, 0,
                4);
        ByteOrder.short2leb((short) address.getPort(), payload, 4);
        ggep.put(GGEP.GGEP_HEADER_IPPORT, payload);
        return ggep;
    }

    /**
     * Adds the packed hosts into this GGEP.
     */
    private GGEP addPackedHosts(GGEP ggep,
            Collection<? extends IpPort> gnutHosts,
            Collection<? extends IpPort> dhtHosts) {

        if (gnutHosts != null && !gnutHosts.isEmpty()) {
            ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, NetworkUtils
                    .packIpPorts(gnutHosts));
            byte[] data = getTLSData(gnutHosts);
            if (data.length != 0)
                ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS_TLS, data);
        }

        if (dhtHosts != null && !dhtHosts.isEmpty()) {
            ggep.put(GGEP.GGEP_HEADER_DHT_IPPORTS, NetworkUtils
                    .packIpPorts(dhtHosts));
        }

        return ggep;
    }

    /**
     * Returns a byte[] of data that indicates if the hosts are TLS capable.
     * If no hosts are capable, this returns an empty array.
     * Otherwise, it returns a byte[] where each bit in the array
     * corresponds to the element (in order) of the hosts.  If the bit
     * is on, that host supports TLS.
     * 
     * For example, if this is supplied the hosts:
     *  1.2.3.4:5, 5.4.3.2.1:1, and 2.3.4.5:6
     * and the first and third hosts are TLS capable,
     * then this will return a single byte of:
     *  10100000
     * 
     */
    private byte[] getTLSData(Collection<? extends IpPort> hosts) {
        HostCatcher catcher = RouterService.getHostCatcher();
        if (catcher == null)
            return DataUtils.EMPTY_BYTE_ARRAY;

        BitNumbers bn = new BitNumbers(hosts.size());
        int i = 0;
        for (IpPort ipp : hosts) {
            if (catcher.isHostTLSCapable(ipp))
                bn.set(i);
            i++;
        }

        return bn.toByteArray();
    }

    /**
     * Adds the ultrapeer GGEP extension to the pong.  This has the version of
     * the Ultrapeer protocol that we support as well as the number of free
     * leaf and Ultrapeer slots available.
     * 
     * @param ggep the <tt>GGEP</tt> instance to add the extension to
     */
    private void addUltrapeerExtension(GGEP ggep) {
        byte[] payload = new byte[3];
        // put version
        payload[0] = PingReply.convertToGUESSFormat(LimeWireUtils
                .getUPMajorVersionNumber(), LimeWireUtils
                .getUPMinorVersionNumber());
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
    private void addDHTExtension(GGEP ggep) {
        byte[] payload = new byte[3];

        // put version
        DHTManager manager = ProviderHacks.getDHTManager();
        int version = manager.getVersion().shortValue();

        ByteOrder.short2beb((short) version, payload, 0);

        if (manager.isMemberOfDHT()) {
            DHTMode mode = manager.getDHTMode();
            assert (mode != null);
            payload[2] = mode.byteValue();
        } else {
            payload[2] = DHTMode.INACTIVE.byteValue();
        }

        // add it
        ggep.put(GGEP.GGEP_HEADER_DHT_SUPPORT, payload);
    }

    // TODO : change this to look for multiple GGEP block in the payload....
    /** Ensure GGEP data parsed...if possible. */
    private GGEP parseGGEP(final byte[] PAYLOAD) {
        //Return if this is a plain pong without space for GGEP.  If 
        //this has bad GGEP data, multiple calls to
        //parseGGEP will result in multiple parse attempts.  While this is
        //inefficient, it is sufficiently rare to not justify a parsedGGEP
        //variable.
        if (PAYLOAD.length <= PingReply.STANDARD_PAYLOAD_SIZE)
            return null;

        try {
            return new GGEP(PAYLOAD, PingReply.STANDARD_PAYLOAD_SIZE, null);
        } catch (BadGGEPBlockException e) {
            return null;
        }
    }

    /** Marks the given kbytes field */
    private long mark(long kbytes) {
        int x = ByteOrder.long2int(kbytes);
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
        if (x < 12)
            return 8; //1<<3
        else if (x < 24)
            return 16; //1<<4
        else if (x < 48)
            return 32; //1<<5
        else if (x < 96)
            return 64; //1<<6
        else if (x < 192)
            return 128; //1<<7
        else if (x < 384)
            return 256; //1<<8
        else if (x < 768)
            return 512; //1<<9
        else if (x < 1536)
            return 1024; //1<<10
        else if (x < 3072)
            return 2048; //1<<11
        else if (x < 6144)
            return 4096; //1<<12
        else if (x < 12288)
            return 8192; //1<<13
        else if (x < 24576)
            return 16384; //1<<14
        else if (x < 49152)
            return 32768; //1<<15
        else if (x < 98304)
            return 65536; //1<<16
        else if (x < 196608)
            return 131072; //1<<17
        else if (x < 393216)
            return 262144; //1<<18
        else if (x < 786432)
            return 524288; //1<<19
        else if (x < 1572864)
            return 1048576; //1<<20
        else if (x < 3145728)
            return 2097152; //1<<21
        else if (x < 6291456)
            return 4194304; //1<<22
        else if (x < 12582912)
            return 8388608; //1<<23
        else if (x < 25165824)
            return 16777216; //1<<24
        else if (x < 50331648)
            return 33554432; //1<<25
        else if (x < 100663296)
            return 67108864; //1<<26
        else if (x < 201326592)
            return 134217728; //1<<27
        else if (x < 402653184)
            return 268435456; //1<<28
        else if (x < 805306368)
            return 536870912; //1<<29
        else
            return 1073741824; //1<<30
    }

}
