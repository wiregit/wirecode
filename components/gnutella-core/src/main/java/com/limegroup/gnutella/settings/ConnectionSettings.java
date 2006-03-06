
// Commented for the Learning branch

package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.SpeedConstants;

/**
 * Settings for Gnutella TCP connections.
 * 
 * ConnectionSettings.EVER_ACCEPTED_INCOMING is true when we get a connect-back connection, proving we're externally contactable.
 * ConnectionSettings.NUM_CONNECTIONS is 32, as an ultrapeer, we'll try to keep connected to 32 other ultrapeers.
 * ConnectionSettings.SOFT_MAX is 3, if we get a packet with hops + TTL bigger than this, we'll lower TTL to make the sum 3.
 * ConnectionSettings.TTL is 4, packets we send will hop across the Internet 4 times before they die.
 * 
 * ConnectionSettings.FORCE_IP_ADDRESS is true when we've saved our IP address and port number in settings.
 * FORCED_IP_ADDRESS_STRING is our IP address, and FORCED_PORT is our port number.
 * 
 * ConnectionSettings.EVIL_HOSTS is a list of programs like "morpheus" and "shareaza", that LimeWire won't connect to.
 * 
 * In the method names, create settable setting means LimeWire the company can adjust these settings remotely.
 * This feature is called SIMPP, and works with a digitally-signed vendor message.
 * 
 * What is an expirable setting? (ask)
 */
public final class ConnectionSettings extends LimeProps {

    /**
     * This class holds public static members.
     * Making a ConnectionSettings object wouldn't make any sense.
     * The constructor is empty and private to make sure the program never makes an object.
     */
    private ConnectionSettings() {}

	/* Constants for proxy settings, sort of like an enumeration type. */
    /** 0, there is no proxy server we have to use. */
    public static final int C_NO_PROXY     = 0;
    /** 4, we have to connect through a Socks 4 proxy server. */
    public static final int C_SOCKS4_PROXY = 4;
    /** 5, we have to connect through a Socks 5 proxy server. */
    public static final int C_SOCKS5_PROXY = 5;
    /** 1, we have to connect through a HTTP proxy server. */
    public static final int C_HTTP_PROXY   = 1;

	/** False until we accept a TCP connection, proving that we are externally contactable. */
	public static final BooleanSetting EVER_ACCEPTED_INCOMING = FACTORY.createBooleanSetting("EVER_ACCEPTED_INCOMING", false);

	/**
	 * Setting for whether we have ever determined that we are not able to
	 * do Firewall-to-firewall transfers in the past based on information
	 * received in pongs. (do)
     * 
     * UDPService.canDoFWT() uses this.
	 */
	public static final BooleanSetting LAST_FWT_STATE = FACTORY.createExpirableBooleanSetting("LAST_FWT_STATE", false);

	/** True, we'll automatically start trying to make Gnutella connections when the program starts. */
	public static final BooleanSetting CONNECT_ON_STARTUP = FACTORY.createBooleanSetting("CONNECT_ON_STARTUP", true);

	/** 32, as an ultrapeer, we'll try to keep connections to 32 other ultrapeers. */
	public static final IntSetting NUM_CONNECTIONS = FACTORY.createSettableIntSetting("NUM_CONNECTIONS", 32, "ConnectionSettings.numConnections", 96, 16);

    /*
     * Non-Lime peers.
     * 
     * When we connect to remote compuers, we'd like them to be running LimeWire too.
     * That way, we know what to expect.
     * Also, if we have a new feature, we'll be able to use it with them.
     * But, we don't want to connect only to other LimeWire programs.
     * 
     * MIN_NON_LIME_PEERS is 10% and MAX_NON_LIME_PEERS is 20%.
     * This means we'd like between 80% and 90% of our connections to be running LimeWire.
     */

	/** 0.2, we want less than 20% of our ultrapeers to be running something other than LimeWire. */
    public static final FloatSetting MAX_NON_LIME_PEERS = FACTORY.createSettableFloatSetting("MAX_NON_LIME_PEERS", 0.2f, "ConnectionSettings.maxLimePeers", 0.5f, 0f);
    /** 0.1, we want more than 10% of our ultrapeers to be running something other than LimeWire. */
    public static final FloatSetting MIN_NON_LIME_PEERS = FACTORY.createSettableFloatSetting("MIN_NON_LIME_PEERS", 0.1f, "ConnectionSettings.minLimePeers", 0.2f, 0f);

    /**
     * 3, the soft max TTL.
     * 
     * This a maximum limit for hops + TTL.
     * We impose this limit on the Gnutella packets we receive.
     * If we get a packet with hops + TTL larger than this limit, we'll lower the TTL to make it compliant with this limit.
     */
    public static final ByteSetting SOFT_MAX = FACTORY.createByteSetting("SOFT_MAX", (byte)3);

	/**
     * True, don't connect to other computers on the same LAN as us.
     * If a remote computer has a LAN IP address, don't connect to it.
	 */
	public static final BooleanSetting LOCAL_IS_PRIVATE = FACTORY.createBooleanSetting("LOCAL_IS_PRIVATE", true);

    /** True, ask GWebCache scripts for IP addresses of computers running Gnutella software. */
	public static final BooleanSetting USE_GWEBCACHE = FACTORY.createBooleanSetting("USE_GWEBCACHE", true);

    /** The time when we last connected to retrieve more GWebCache servers. (do) */
    public static final LongSetting LAST_GWEBCACHE_FETCH_TIME = FACTORY.createLongSetting("LAST_GWEBCACHE_FETCH_TIME", 0);

    /**
     * True, activate the connection watchdog thread that will close bad connections. (do)
     * Disabling the connection watchdog is useful when testing.
	 */
	public static final BooleanSetting WATCHDOG_ACTIVE = FACTORY.createBooleanSetting("WATCHDOG_ACTIVE", true);

    /** "234.21.81.1", the multicast address. */
    public static final StringSetting MULTICAST_ADDRESS = FACTORY.createStringSetting("MULTICAST_ADDRESS", "234.21.81.1");

    /** 6347, the multicast port. */
    public static final IntSetting MULTICAST_PORT = FACTORY.createIntSetting("MULTICAST_PORT", 6347);

	/** False, don't allow multicast message loopback. (do) */
    public static final BooleanSetting ALLOW_MULTICAST_LOOPBACK = FACTORY.createBooleanSetting("ALLOW_MULTICAST_LOOPBACK", false);

	/**
     * True, look a headers from a remote computer and decide if we want to connect or not.
     * You may want to turn this off when testing.
	 */
	public static final BooleanSetting PREFERENCING_ACTIVE = FACTORY.createBooleanSetting("PREFERENCING_ACTIVE", true);

    /** False, don't make connections when the program is disconnected from the network. */
    public static final BooleanSetting ALLOW_WHILE_DISCONNECTED = FACTORY.createBooleanSetting("ALLOW_WHILE_DISCONNECTED", false);

	/**
     * True, let ConnectionManager.remove() remove a connection.
     * You may want to turn this off when testing.
	 */
	public static final BooleanSetting REMOVE_ENABLED = FACTORY.createBooleanSetting("REMOVE_ENABLED", true);

    /**
     * True, let the MessageRouter send query route tables.
     * You may want to turn this off when testing.
     */
    public static BooleanSetting SEND_QRP = FACTORY.createBooleanSetting("SEND_QRP", true);

    /* Compression. */
    /** True, we'll accept compressed Gnutella packets from a remote computer. */
    public static final BooleanSetting ACCEPT_DEFLATE = FACTORY.createBooleanSetting("ACCEPT_GNUTELLA_DEFLATE", true);
    /** True, we'll send compressed Gnutella packets to a remote computer. */
    public static final BooleanSetting ENCODE_DEFLATE = FACTORY.createBooleanSetting("ENCODE_GNUTELLA_DEFLATE", true);

    /** 4, a Gnutella packet will hop 4 times across the Internet. (do) */
    public static final ByteSetting TTL = FACTORY.createByteSetting("TTL", (byte)4);

    /** What the user told settings our Internet connection speed is, in Kbps. */
    public static final IntSetting CONNECTION_SPEED = FACTORY.createIntSetting("CONNECTION_SPEED", SpeedConstants.MODEM_SPEED_INT);

    /**
     * The port number we'll choose randomly, use UPnP to forward from the NAT, and start TCP and UDP sockets listening on.
     * The default value is 6346, the default port for Gnutella on the Internet.
     * Now that LimeWire chooses a random port number, 6346 indicates it needs to do this.
	 */
    public static final IntSetting PORT = FACTORY.createIntSetting("PORT", 6346);

    /** True if we've saved our IP address and port number in settings. */
    public static final BooleanSetting FORCE_IP_ADDRESS = FACTORY.createBooleanSetting("FORCE_IP_ADDRESS", false);

    /**
     * When a remote computer tells us what our IP address is, we store it here in settings.
     * This is our real Internet IP address that remote computers can contact us at.
     * We keep our IP address in settings so the next time the program runs, we'll know what it is right away.
     */
    public static final StringSetting FORCED_IP_ADDRESS_STRING = (StringSetting)FACTORY.createStringSetting("FORCED_IP_ADDRESS_STRING", "0.0.0.0").setPrivate(true);

    /**
     * The port number of our NAT mapping and listening sockets.
     * 
     * This is our port number.
     * Our TCP and UDP sockets listen on this port number.
     * Also, we use UPnP to try to forward this port number from the NAT to the PC we are running on.
     * Acceptor.init() may have chosen this port number randomly, or the user may have set it manually in settings.
     * 
     * Initialized to 6346, the port number for Gnutella.
     * 6346 means we haven't chosen a random port in place of this default.
     */
    public static final IntSetting FORCED_PORT = FACTORY.createIntSetting("FORCED_PORT", 6346);
    
    /**
     * The user doesn't want the program to use UPnP.
     * False by default, we can use UPnP.
     */
    public static final BooleanSetting DISABLE_UPNP = FACTORY.createBooleanSetting("DISABLE_UPNP", false);
    
    /**
     * We've created a port mapping with UPnP, and we need to remove it.
     * If the program starts and finds this setting true, then it wasn't shut down properly and should remove the mapping now.
     */
    public static final BooleanSetting UPNP_IN_USE = FACTORY.createBooleanSetting("UPNP_IN_USE", false);

    /** "GNUTELLA", the first thing a remote computer should send us are these ASCII text characters. */
    public static final String CONNECT_STRING_FIRST_WORD = "GNUTELLA";

    /** Not used. */
    public static final StringSetting CONNECT_STRING = FACTORY.createStringSetting("CONNECT_STRING", "GNUTELLA CONNECT/0.4");
    /** Not used. */
    public static final StringSetting CONNECT_OK_STRING = FACTORY.createStringSetting("CONNECT_OK_STRING", "GNUTELLA OK");
    /** Not used. */
    public static final BooleanSetting USE_NIO = FACTORY.createBooleanSetting("USE_NIO", true);

    /** The IP address of the proxy server we have to connect through. */
    public static final StringSetting PROXY_HOST = FACTORY.createStringSetting("PROXY_HOST", "");
    /** The port number of the proxy server we have to connect through. */
    public static final IntSetting PROXY_PORT = FACTORY.createIntSetting("PROXY_PORT", 0);

    /** False, don't use the proxy server for IP addresses on the LAN. */
    public static final BooleanSetting USE_PROXY_FOR_PRIVATE = FACTORY.createBooleanSetting("USE_PROXY_FOR_PRIVATE", false);

    /** C_NO_PROXY if we don't have to use a proxy server, or the type of proxy server we have to use, like C_SOCKS4_PROXY. */
    public static final IntSetting CONNECTION_METHOD = FACTORY.createIntSetting("CONNECTION_TYPE", C_NO_PROXY);

    /** True if we need to give the proxy server a user name or password. */
    public static final BooleanSetting PROXY_AUTHENTICATE = FACTORY.createBooleanSetting("PROXY_AUTHENTICATE", false);
    /** The user name to give the proxy server. */
    public static final StringSetting PROXY_USERNAME = FACTORY.createStringSetting("PROXY_USERNAME", "");
    /** The password to give the proxy server. */
    public static final StringSetting PROXY_PASS = FACTORY.createStringSetting("PROXY_PASS", "");

    /** True, the program should try to connect to remote computers with the same language preference as us. */
    public static final BooleanSetting USE_LOCALE_PREF = FACTORY.createBooleanSetting("USE_LOCALE_PREF", true);

    /** 2, the number of slots we've reserved for remote computers with our language preference. */
    public static final IntSetting NUM_LOCALE_PREF = FACTORY.createIntSetting("NUMBER_LOCALE_PREF", 2);

    /** 50, If 50 of our connection attemtps fail, we'll start accepting non-LimeWire remote computers as ultrapeers. */
    public static final IntSetting LIME_ATTEMPTS = FACTORY.createIntSetting("LIME_ATTEMPTS", 50);

    /**
     * How long we believe firewalls will let us send solicited UDP traffic. (do)
     * Field tests show at least a minute with most firewalls, so lets try 55 seconds.
     */
    public static final LongSetting SOLICITED_GRACE_PERIOD = FACTORY.createLongSetting("SOLICITED_GRACE_PERIOD", 85000l); // This is 14 minutes? (ask)

    /** 10, if we get a UDP ping with "SCP", we'll send back 10 addresses in "IPP". */
    public static final IntSetting NUM_RETURN_PONGS = FACTORY.createSettableIntSetting("NUM_RETURN_PONGS", 10, "pings", 25, 5);

    /**
     * False, allow bootstrapping.
     * You can turn this feature off for testing purposes.
     */
    public static final BooleanSetting DO_NOT_BOOTSTRAP = FACTORY.createBooleanSetting("DO_NOT_BOOTSTRAP", false);

    /**
     * False, allow the program to send a multicast bootstrap ping.
     * You can prevent the program from doing this for testing purposes.
     */
    public static final BooleanSetting DO_NOT_MULTICAST_BOOTSTRAP = FACTORY.createBooleanSetting("DO_NOT_MULTICAST_BOOTSTRAP", false);

    /**
     * False, we'll only use connect-back requests to determine that we are externally contactable.
     * When a remote computer connects to our listening socket in Acceptor, that's not proof enough for us.
     */
    public static final BooleanSetting UNSET_FIREWALLED_FROM_CONNECTBACK =
        FACTORY.createSettableBooleanSetting("UNSET_FIREWALLED_FROM_CONNECTBACK", false, "connectbackfirewall");

    /** Not used. */
    public static final LongSetting FLUSH_DELAY_TIME = FACTORY.createSettableLongSetting("FLUSH_DELAY_TIME", 0, "flushdelay", 300, 0);

    /**
     * An array of program names like "morpheus" and "shareaza" that we won't connect to.
     * Code in ConnectionManager compares a remote computer's "User-Agent" header value to the strings in this array.
     * 
     * This setting is an array of strings.
     * It can hold multiple strings, like "morpheus", "shareaza", and so on.
     * They are product names in all lowercase letters.
     * 
     * This is a settable setting, using the SIMPP feature.
     * The company LimeWire can send a digitally signed vendor message onto the network.
     * It will make all the LimeWire programs stop connecting to a brand of Gnutella program.
     * A new version of some Gnutella program may have an error that could harm the network.
     * This ability lets LimeWire protect its users from it.
     * 
     * "evil_hosts" is the SIMPP key for the setting, and not default text.
     * By default, the list is empty, and the program won't refuse a remote computer based on its "User-Agent" name.
     */
    public static final StringArraySetting EVIL_HOSTS = FACTORY.createSettableStringArraySetting("EVIL_HOSTS", new String[0], "evil_hosts");

    /**
     * 1, if we're a leaf and the user leaves the computer for 30 minutes, we'll drop down from having 3 ultrapeer connections to just 1.
     */
    public static final IntSetting IDLE_CONNECTIONS = FACTORY.createSettableIntSetting("IDLE_CONNECTIONS", 1, "ConnectionSettings.IdleConnections", 3, 1);

    /** Not used. */
    public static final int getMaxConnections() {
        /*
         * Helper method left from Settings Manager.
         * Returns the maximum number of connections for the given connection speed.
         */
        int speed = CONNECTION_SPEED.getValue();
        if (speed <= SpeedConstants.MODEM_SPEED_INT) {
            return 3;
        } else if (speed <= SpeedConstants.CABLE_SPEED_INT) {
            return 6;
        } else if (speed <= SpeedConstants.T1_SPEED_INT) {
            return 10;
        } else { // T3: no limit
            return 12;
        }
    }
}
