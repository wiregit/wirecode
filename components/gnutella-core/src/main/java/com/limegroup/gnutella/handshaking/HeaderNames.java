
// Commented for the Learning branch

package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderName;

/**
 * Provides names for the headers used in the Gnutella connection handshake.
 * This class contains a lot of public static final strings.
 * They hold pieces of text commonly used in Gnutella handshake headers.
 * For instance, if you want the text "X-Ultrapeer", don't type it, use HeaderNames.X_ULTRAPEER instead.
 * 
 * @author Anurag Singla
 */
public final class HeaderNames {
    
    /**
     * Give this class an empty private constructor.
     * This makes it impossible to construct and instance of this class.
     */
    private HeaderNames() {}
    
    // Headers that tell the name and verison number of the Gnutella software we're running
    
    /** "User-Agent" header name, the name of the software program and its version number */
    public static final String USER_AGENT = "User-Agent";
    /** "X-Version" header name, used for autoupdate, the authentic signed version of the software the speaker can give you */
    public static final String X_VERSION = "X-Version";
    
    // Headers that tell my IP address, yours, and more that you can reach ultrapeers at

    /** "Listen-IP" header name, the IP address the speaker is listening for connections on */
    public static final String LISTEN_IP = "Listen-IP";
    /** "X-My-Address" header name, Gnutella programs should use "Listen-IP" instead */
    public static final String X_MY_ADDRESS = "X-My-Address";
    /** One computer uses the "Remote-IP" header to tell the second what the second's Internet IP address is */
    public static final String REMOTE_IP = "Remote-IP";
    /** "X-Try-Ultrapeers" header name, IP addresses and port numbers of ultrapeers to try to connect to */
    public static final String X_TRY_ULTRAPEERS = "X-Try-Ultrapeers";

    // Headers about ultrapeers and leaves

    /** "X-Ultrapeer" header name, "true" if the speaker is an ultrapeer, "false" if the speaker is a leaf */
    public static final String X_ULTRAPEER = "X-Ultrapeer";
    /** "X-Ultrapeer-Needed" header name, the speaker needs more ultrapeer connections "true", or has enough already "false" */
    public static final String X_ULTRAPEER_NEEDED = "X-Ultrapeer-Needed";

    // Headers for supporting and sending the data stream of Gnutella packets compressed

    /** "Accept-Encoding" header name, says whether the speaker can understand compressed data or not */
    public static final String ACCEPT_ENCODING = HTTPHeaderName.ACCEPT_ENCODING.httpStringValue();
    /** "Content-Encoding" header name, says whether the packets after the handshake will be compressed or not */
    public static final String CONTENT_ENCODING = HTTPHeaderName.CONTENT_ENCODING.httpStringValue();
    /** "deflate" value, indicates a compressed data stream */
    public static final String DEFLATE_VALUE = ConstantHTTPHeaderValue.DEFLATE_VALUE.httpStringValue();

    // Headers that advertise support for advanced Gnutella features

    /** "X-Query-Routing" header name, indicates support for QRP, the query routing protocol where leaves tell ultrapeers what they don't have */
    public static final String X_QUERY_ROUTING = "X-Query-Routing";
    /** "Pong-Caching" header name, indicates support for pong caching, the method of limiting pongs on the network */
    public static final String X_PONG_CACHING = "Pong-Caching";
    /** "GGEP" header name, indicates support for GGEP extension blocks in Gnutella packets, like pongs */
    public static final String GGEP = "GGEP";
    /** "X-Guess" header name, shows the GUESS version */
    public static final String X_GUESS = "X-Guess";
    /** "X-Ultrapeer-Query-Routing" header name */
    public static final String X_ULTRAPEER_QUERY_ROUTING = "X-Ultrapeer-Query-Routing";
    /** "Vendor-Message" header name, support for vendor specific messages */
    public static final String X_VENDOR_MESSAGE = "Vendor-Message";
    /** "X-Ext-Probes" header name, support for extendable probe queries */
    public static final String X_PROBE_QUERIES = "X-Ext-Probes";
    /** "X-Dynamic-Querying" header name, indicates the version of dynamic querying supported */
    public static final String X_DYNAMIC_QUERY = "X-Dynamic-Querying";
    /** "X-Temp-Connection" header name */
    public static final String X_TEMP_CONNECTION = "X-Temp-Connection";
    /** "X-Requeries" header name, remote computers must promise not to send queries twice with "X-Requeries: false" */
    public static final String X_REQUERIES = "X-Requeries";

    // Headers about network preferences

    /**
     * "X-Max-TTL" header name, says we won't send and don't want to see packets that live longer than this.
     * Packets we get have a hops number and a TTL number.
     * If a packet's hops + TTL is greater than our maximum limit of 3 or 4, we'll lower its TTL to be compliant with our limit.
     */
    public static final String X_MAX_TTL = "X-Max-TTL";

    /**
     * "X-Degree" header name, the number of ultrapeer to ultrapeer connections to keep up.
     * If ultrapeers have more connections to other ultrapeers, packets can travel fewer hops, and the Gnutella network will be more efficient.
     * We think of a client that says "X-Degree: 15" or higher as having a high degree.
     * Running as an ultrapeer, LimeWire maintains 32 ultrapeer connections, and greets computers with "X-Degree: 32".
     */
	public static final String X_DEGREE = "X-Degree";

    /**
     * "X-Locale-Pref" header name, indicates the language of choice.
     * LimeWire once used this header to favor connections to computers that had the same language preference.
     * This hurt the Gnutella network in multilanguage regions of the world, like Europe.
     * This feature wasn't a good idea, and is now off by default.
     * 
     * TODO:kfaaborg Remove the code that has to do with the language preferencing feature.
     */
    public static final String X_LOCALE_PREF = "X-Locale-Pref";

    // Headers the crawler uses and we use to talk to the crawler

    /** "Crawler" header name, the crawler says "Crawler: 0.1" when it contacts us */
	public static final String CRAWLER = "Crawler";
	/** "Leaves" header name, we tell the crawler how many leaves we have with a header like "Leaves: 97" */
	public static final String LEAVES = "Leaves";
	/** "Peers" header name, we tell the crawler how many ultrapeers we're connected to with a header like "Peers: 3" */
	public static final String PEERS = "Peers";
}
