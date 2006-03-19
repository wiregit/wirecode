
// Commented for the Learning branch

package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.udpconnect.UDPConnection;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortSet;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * A QueryReply object represents a Gnutella query hit packet.
 * A sharing computer makes a query hit packet to return information about it and the files that match a query it has received.
 * 
 * A query hit packet contains information about:
 * A computer sharing files.
 * The files it's sharing that match a search.
 * 
 * LimeWire uses several objects to represent the information in a query hit packet:
 * QueryReply extends Message, which holds the information of the Gnutella packet header.
 * A QueryReply object represenst a query hit packet.
 * A Response object represents the information about a file described in a query hit packet.
 * A HostData object organizes the information about the sharing computer that prepared a query hit packet.
 * 
 * There are 2 reasons to make a QueryReply object:
 * Send: We're preparing a new query hit packet that we will send.
 * Receive: We've read a packet we've just received, and want to make an object to represent it.
 * This class has constructors for each case.
 * 
 * The receive constructors save the payload data in the new QueryReply object, but don't parse it right away.
 * When code calls a method like getResultsAsList() that requires parsing, parseResults2() parses it.
 * This saves us from having to parse query hits that we'll forward without looking at in detail.
 * It does mean that we won't discover that a packet is bad until we call a method that requires parsing.
 * 
 * Some of these methods throw a BadPacketException if the requested information isn't found.
 * This doesn't mean that other data, like the file hit responses, is bad.
 * MissingDataException might have been a better name.
 * 
 * A query hit packet looks like this:
 * 
 * Header 23 bytes
 * 
 *   (Represented by a Message object)
 * 
 * Payload 11 bytes
 * 
 *   N                 The number of results in the next section
 *   PPIPIP            The port number and IP address of the sharing computer
 *   SSSS              The sharing computer's upload speed
 * 
 * Results
 * 
 *   (Represented by Response objects)
 * 
 * QHD
 * 
 *   LIME              The ASCII characters of the vendor code
 *   L                 A byte with the length of the next section, which is 2 or 4 bytes long
 *   MVXX              MV are the mask and value bytes, and XX is the size of the XML
 *   C                 A byte that is 1 if the sharing computer can chat, 0 if it can't
 *   ggep              A GGEP block
 *   xml\0             Compressed XML data, and a null terminator
 * 
 * Client ID 16 bytes
 * 
 *   [Client ID GUID]  The sharing computer's Gnutella client ID GUID
 * 
 * The QHD block is optional, but LimeWire always sends it.
 * 
 * The one byte L has a value of 2 or 4.
 * If the length is 2, the next section is just MV.
 * If the length is 4, the next section is MVXX.
 * There are two ways to not have any XML.
 * Specify L of 2, and then don't have any XML length or XML.
 * Or, specify L of 4, write a length of 0 into XX, and then don't have any XML.
 * In the QueryReply packets we make, LimeWire always sets L to 4, and then writes 0 in XX if there's no XML.
 * In the packets we receive, code here is ready for either method of omitting XML.
 * 
 * MV are the mask and value bytes.
 * Together, the mask and value bytes hold 8 values that can be unspecified, true, or false.
 * 
 * For each bit:
 * mask     values
 * 00000000 00000000 is unspecified.
 * 00000001 00000000 is specified, and false.
 * 00000001 00000001 is specified, and true.
 * 
 * Here is what the bits mean:
 * PUSH_MASK     0x01 0000 0001 The sharing computer can't accept incoming TCP socket connetions, so a downloader will have to request a push.
 * BUSY_MASK     0x04 0000 0100 All of the sharing computer's upload slots are full right now.
 * UPLOADED_MASK 0x08 0000 1000 The sharing computer has successfully uploaded at least 1 file.
 * SPEED_MASK    0x10 0001 0000 The speed field has a measured upload speed, not just a speed the user entered.
 * GGEP_MASK     0x20 0010 0000 There is a GGEP block after this.
 * 
 * For the first bit, PUSH_MASK, the use of the mask and value bytes is reversed.
 * A 1 in the values byte indicates we can read the information from the mask byte.
 * 
 * Find L and MVXX by looking forward from the start of the packet.
 * Find the GGEP block by starting after MVXX, and looking forward for the GGEP start byte 0xC3.
 * Find XML and the client ID GUID by counting backwards from the end of the packet.
 * The one byte C is in the middle, called the private area.
 * A Gnutella program could put more data after C.
 * 
 * The XML is null terminated, this null terminator isn't useful.
 * It's likely deflate compressed, so it may contain a 0 byte.
 * Find it's length in XX, and then count back from the end of the packet to read it.
 * 
 * There are 2 GUIDS in a QueryReply packet.
 * At the start in the Gnutella packet header, the message GUID marks this packet unique on the network.
 * At the very end beyond the QHD, the client ID GUID identifies the sharing computer.
 */
public class QueryReply extends Message implements Serializable{

    /** 32768, The XML in the end of a query hit packet can't be more than 32 KB. */
    public static final int XML_MAX_SIZE = 32768;

    /** 4, the size of the MVXX area that contains the mask and value bytes, and then the XML size. */
    public static final int COMMON_PAYLOAD_LEN = 4;

    /**
     * This query hit packet's payload data.
     * 
     * The constructors that make a QueryHit take information for it, compose payload data, and save it here.
     * 
     * The constructors that take network data save the payload here, but don't parse it right away.
     * When code calls a method like getResultsAsList() that requires parsing, parseResults2() parses it.
     * This saves us from having to parse query hits that we'll forward without looking at in detail.
     */
    private byte[] _payload;

    /**
     * True when parseResults2() has parsed the packet payload and set all the member variables.
     * When we read the data of a query hit from the network, we don't parse it right away.
     * We might just pass the packed on never having needed to.
     * We parse it the first time a method like getResults() requires looking into the payload.
     */
    private volatile boolean _parsed = false;

    /**
     * An array of Response objects that represent the file information blocks in this query hit packet.
     * The parsing operation sets this array.
     * If the packet payload is malformed and we couldn't parse it, _responses remains null.
     */
    private volatile Response[] _responses = null;

    /** The sharing computer's vendor code like "LIME", parsed from the start of the QHD. */
    private volatile String _vendor = null;

    /*
     * Together, the mask and value bytes hold 8 values that can be unspecified, true, or false.
     * 
     * For the lowest bit:
     * 00000000 00000000 is unspecified.
     * 00000001 00000000 is specified, and false.
     * 00000001 00000001 is specified, and true.
     */

    /** From 0x01 in the mask and values bytes, TRUE, FALSE, OR UNDEFINED if the sharing computer will need a push packet. */
    private volatile int _pushFlag          = UNDEFINED;
    /** From 0x04 in the mask and values bytes, TRUE, FALSE, OR UNDEFINED if all the sharing computer's upload slots are full. */
    private volatile int _busyFlag          = UNDEFINED;
    /** From 0x08 in the mask and values bytes, TRUE, FALSE, OR UNDEFINED if the sharing computer has ever uploaded a file. */
    private volatile int _uploadedFlag      = UNDEFINED;
    /** From 0x10 in the mask and values bytes, TRUE, FALSE, OR UNDEFINED if the speed is from measured data, not user settings. */
    private volatile int _measuredSpeedFlag = UNDEFINED;

    /** The sharing computer can chat, indicated by a 1 in the private area byte. */
    private volatile boolean _supportsChat       = false;
    /** The sharing computer supports browse host, indicated by the presence of the GGEP "BH" extension. */
    private volatile boolean _supportsBrowseHost = false;
    /** This query hit is a response to a multicast query on the LAN, indicated by the presence of the GGEP "MCAST" extension. */
    private volatile boolean _replyToMulticast   = false;

    /** The sharing computer can do firewall-to-firewall transfers, indicated by the presence of the GGEP "FW" extension. */
    private volatile boolean _supportsFWTransfer = false;

    /**
     * The version number of firewall-to-firewall transfer support.
     * This is the value of the GGEP "FW" extension.
     * For LimeWire right now, it's 1.
     * 
     * If the computer doesn't support firewall-to-firewall transfers, the GGEP block won't have "FW", and _fwTransferVersion is 0.
     */
    private volatile byte _fwTransferVersion = (byte)0;

    /** 1, indicates a bit contains data and the data is true. */
    private static final int TRUE      = 1;
    /** 0, indicates a bit contains data and the data is false. */
    private static final int FALSE     = 0;
    /** -1, indicates a bit has not been set, so the data isn't defined true or false. */
    private static final int UNDEFINED = -1;

    /** 0x01 0000 0001 in the mask and value bytes, the sharing computer can't accept incoming TCP socket connetions, so a downloader will have to request a push. */
    private static final byte PUSH_MASK     = (byte)0x01;
    /** 0x04 0000 0100 in the mask and value bytes, all of the sharing computer's upload slots are full right now. */
    private static final byte BUSY_MASK     = (byte)0x04;
    /** 0x08 0000 1000 in the mask and value bytes, the sharing computer has successfully uploaded at least 1 file. */
    private static final byte UPLOADED_MASK = (byte)0x08;
    /** 0x10 0001 0000 in the mask and value bytes, the speed field has a measured upload speed, not just a speed the user entered. */
    private static final byte SPEED_MASK    = (byte)0x10;
    /** 0x20 0010 0000 in the mask and value bytes, there is a GGEP block after this. */
    private static final byte GGEP_MASK     = (byte)0x20;

    /** 0x01 0000 0001 in the chat byte, the sharing computer can chat. */
    private static final byte CHAT_MASK = (byte)0x01;

    /** The compressed XML from the end of this query hit packet. */
    private byte[] _xmlBytes = DataUtils.EMPTY_BYTE_ARRAY; // Initialize to a byte array with no length so _xmlBytes isn't null

	/** The IP address of the computer that made this query hit packet and is sharing the files it lists. */
	private byte[] _address = new byte[4];
	
	/** The Gnutella client ID GUID of the sharing computer. */
	private byte[] clientGUID = null;

    /**
     * An IpPortSet of IPPortCombo objects with the IP addresses and port numbers of the sharing computer's push proxies.
     * 
     * If the sharing computer isn't externally contactable, we can send a push message to one of its push proxies.
     * The push proxy will relay the message to the computer, which will push open a connection to us.
     * The connection with be a TCP socket connection that starts with the HTTP GIV header, and gives us the file we want.
     * 
     * In the GGEP block, the "PUSH" extension lists the IP addresses and port numbers of the sharing computer's push proxies.
     * The computer's push proxies are just its ultrapeers.
     */
    private Set _proxies;

    /**
     * True if the program has marked this QueryReply packet as the result of a browse host request.
     * There's no data in a query reply packet that marks it as a browse host result.
     * This is a flag in this object that the program sets and reads.
     */
    private boolean _browseHostReply;

    /**
     * The HostData object with information about the sharing computer.
     * After we parsed the payload, we gave this QueryReply packet to the HostData constructor.
     * A HostData object keeps information about the sharing computer, not the files it's sharing.
     * 
     * _hostData will be null until parseResults2() parses the payload.
     */
    private HostData _hostData;

    /**
     * GGEPUtil is a nested class that keeps some GGEP blocks that QueryReply packets commonly use.
     * It serializes their data, and can return their byte arrays quickly.
     * When Java loads the QueryReply class, this line of code makes the GGEPUtil object so code here can use it.
     */
    private static final GGEPUtil _ggepUtil = new GGEPUtil();

    /**
     * Not used.
     * 
     * Creates a new query reply.  The number of responses is responses.length
     *  The Browse Host GGEP extension is ON by default.  
     *  @requires  0 < port < 2^16 (i.e., can fit in 2 unsigned bytes),
     *    ip.length==4 and ip is in <i>BIG-endian</i> byte order,
     *    0 < speed < 2^32 (i.e., can fit in 4 unsigned bytes),
     *    responses.length < 2^8 (i.e., can fit in 1 unsigned byte),
     *    clientGUID.length==16
     */
    public QueryReply(byte[] guid, byte ttl, int port, byte[] ip, long speed, Response[] responses, byte[] clientGUID, boolean isMulticastReply) {
        this(guid, ttl, port, ip, speed, responses, clientGUID, DataUtils.EMPTY_BYTE_ARRAY, false, false, false, false, false, false, true, isMulticastReply, false, Collections.EMPTY_SET);
    }

    /**
     * Not used.
     * 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code and the given busy and push flags.  Note that this
     * constructor has no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON by default.  
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots.  
     * @param finishedUpload true iff this server has successfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param supportsChat true iff the host currently allows chatting.
     */
    public QueryReply(byte[] guid, byte ttl, int port, byte[] ip, long speed, Response[] responses, byte[] clientGUID, boolean needsPush, boolean isBusy, boolean finishedUpload, boolean measuredSpeed, boolean supportsChat, boolean isMulticastReply) {
        this(guid, ttl, port, ip, speed, responses, clientGUID, DataUtils.EMPTY_BYTE_ARRAY, true, needsPush, isBusy, finishedUpload, measuredSpeed, supportsChat, true, isMulticastReply, false, Collections.EMPTY_SET);
    }

    /**
     * Not used.
     * 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code and the given busy and push flags.  Note that this
     * constructor has no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON by default.  
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots
     * @param finishedUpload true iff this server has successfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param xmlBytes The (non-null) byte[] containing aggregated
     * and indexed information regarding file metadata.  In terms of byte-size, 
     * this should not be bigger than 65535 bytes.  Anything larger will result
     * in an Exception being throw.  This String is assumed to consist of
     * compressed data.
     * @param supportsChat true iff the host currently allows chatting.
     * @exception IllegalArgumentException Thrown if 
     * xmlBytes.length > XML_MAX_SIZE
     */
    public QueryReply(byte[] guid, byte ttl, int port, byte[] ip, long speed, Response[] responses, byte[] clientGUID, byte[] xmlBytes, boolean needsPush, boolean isBusy, boolean finishedUpload, boolean measuredSpeed,boolean supportsChat, boolean isMulticastReply) throws IllegalArgumentException {
        this(guid, ttl, port, ip, speed, responses, clientGUID, xmlBytes, needsPush, isBusy,  finishedUpload, measuredSpeed, supportsChat, isMulticastReply, Collections.EMPTY_SET);
    }

    /**
     * Not used.
     * 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code and the given busy and push flags.  Note that this
     * constructor has no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON by default.  
     *
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots
     * @param finishedUpload true iff this server has successfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param xmlBytes The (non-null) byte[] containing aggregated
     * and indexed information regarding file metadata.  In terms of byte-size, 
     * this should not be bigger than 65535 bytes.  Anything larger will result
     * in an Exception being throw.  This String is assumed to consist of
     * compressed data.
     * @param supportsChat true iff the host currently allows chatting.
     * @param proxies an array of PushProxy interfaces.  will be included in 
     * the replies GGEP extension.
     * @exception IllegalArgumentException Thrown if 
     * xmlBytes.length > XML_MAX_SIZE
     */
    public QueryReply(byte[] guid, byte ttl, int port, byte[] ip, long speed, Response[] responses, byte[] clientGUID, byte[] xmlBytes, boolean needsPush, boolean isBusy, boolean finishedUpload, boolean measuredSpeed, boolean supportsChat, boolean isMulticastReply, Set proxies) throws IllegalArgumentException {
        this(guid, ttl, port, ip, speed, responses, clientGUID, xmlBytes, true, needsPush, isBusy, finishedUpload, measuredSpeed, supportsChat, true, isMulticastReply, false, proxies);
        if (xmlBytes.length > XML_MAX_SIZE) throw new IllegalArgumentException("XML bytes too big: " + xmlBytes.length);
        _xmlBytes = xmlBytes;
    }

    /**
     * Make a new QueryReply object to represent a query hit packet with all the given information.
     * Leads to the packet maker.
     * 
     * If needsPush is true, the sharing computer is firewalled, and a downloader should push right away without attempting a normal download first.
     * If measuredSpeed is true, the speed is from real measured data, not just a setting the user entered.
     * xmlBytes contains compressed data, not ASCII characters.
     * supportsChat is true if the host currently allows chatting.
     * 
     * Includes the QHD block.
     * Adds a GGEP block with "BH", indicating the sharing computer supports the browse host feature.
     * 
     * @param guid               For the header, the GUID that marks this packet unique
     * @param ttl                For the header, the number of times the packet can travel between ultrapeers
     * @param port               For the payload, the sharing computer's port number
     * @param ip                 For the payload, the sharing computer's IP address
     * @param speed              For the payload, the sharing computer's upload speed
     * @param responses          For the payload, the number of file hit results that will come next
     * @param clientGUID         For the end, the sharing computer's client ID GUID that marks it unique on the Gnutella network
     * @param xmlBytes           For the QHD, the bytes of compressed XML
     * @param needsPush          Sets 0x01 in the flags and controls bytes, the sharing computer can't accept TCP connections and will need a push message
     * @param isBusy             Sets 0x04 in the flags and controls bytes, all the sharing computer's upload slots are full right now
     * @param finishedUpload     Sets 0x08 in the flags and controls bytes, the sharing computer has actually uploaded a file
     * @param measuredSpeed      Sets 0x10 in the flags and controls bytes, the speed in this packet is from real measured data, not just a setting the user entered
     * @param supportsChat       For the chat byte, the sharing computer can chat, makes the chat byte 1 and not 0
     * @param isMulticastReply   Makes "MCAST" in the GGEP block, this query hit is responding to a multicast query
     * @param supportsFWTransfer Makes "FW" in the GGEP block, the sharing computer can do a firewall-to-firewall file transfer
     * @param proxies            Makes "PUSH" in the GGEP block, a Set of objects that implement IpPort with the addresses of the sharing computer's push proxies
     * @return                   A new QueryReply object that represents a query hit packet with all that information
     */
    public QueryReply(byte[] guid, byte ttl, int port, byte[] ip, long speed, Response[] responses, byte[] clientGUID, byte[] xmlBytes, boolean needsPush,
        boolean isBusy, boolean finishedUpload, boolean measuredSpeed, boolean supportsChat, boolean isMulticastReply, boolean supportsFWTransfer, Set proxies)
        throws IllegalArgumentException {

        // Make a new QueryReply object with the given values and 2 defaults
        this(
            guid,
            ttl,
            port,
            ip,
            speed,
            responses,
            clientGUID,
            xmlBytes,
            true,               // Include the QHD block
            needsPush,
            isBusy,
            finishedUpload,
            measuredSpeed,
            supportsChat,
            true,               // The searching computer supports the browse host feature
            isMulticastReply,
            supportsFWTransfer,
            proxies);

        // Make sure the XML isn't bigger than 32 KB, and save it in this new QueryReply object
        if (xmlBytes.length > XML_MAX_SIZE) throw new IllegalArgumentException("XML bytes too big: " + xmlBytes.length);
        _xmlBytes = xmlBytes;        
    }

    /**
     * Make a new QueryReply object to represent a query hit packet we just read from the network.
     * 
     * @param guid    The packet's GUID
     * @param ttl     The number of additional times the packet can travel between ultrapeers
     * @param hops    The number of times the packet has already traveled between ultrapeers
     * @param payload The data of the packet beyond the header
     * @return        A new QueryReply object that represents this query hit packet
     */
    public QueryReply(byte[] guid, byte ttl, byte hops, byte[] payload) throws BadPacketException {

        // Call the next constructor, passing Message.N_UNKNOWN because we don't know if we got this query hit over TCP or UDP
    	this(guid, ttl, hops, payload, Message.N_UNKNOWN);
    }

    /**
     * Make a new QueryReply object to represent a query hit packet we just read from the network.
     * 
     * @param  guid    The packet's GUID
     * @param  ttl     The number of additional times the packet can travel between ultrapeers
     * @param  hops    The number of times the packet has already traveled between ultrapeers
     * @param  payload The data of the packet beyond the header
     * @param  network The Internet protocol we got this packet on, or Message.N_UNKNOWN if we don't know
     * @return         A new QueryReply object that represents this query hit packet
     */
    public QueryReply(byte[] guid, byte ttl, byte hops, byte[] payload, int network) throws BadPacketException {

        // Save the message GUID, TTL and hops counts, and payload length in the Message part of this object
    	super(guid, Message.F_QUERY_REPLY, ttl, hops, payload.length, network);

        // Save the payload data in this object
        this._payload = payload;

        /*
         * We won't parse the payload now.
         * This query hit packet may not be for us, and we may not need to know its contents.
         * Query hit packets are complex, and parsing them takes the computer time.
         * When the program calls a method like getResultCount(), we'll parse the packet then.
         */

        // Make sure the port number isn't 0 or too big to fit in 2 bytes
        if (!NetworkUtils.isValidPort(getPort())) {
		    ReceivedErrorStat.REPLY_INVALID_PORT.incrementStat();
			throw new BadPacketException("invalid port");
		}

        // Make sure the sharing computer's upload speed fits in 4 bytes
		if ((getSpeed() & 0xFFFFFFFF00000000L) != 0) {
		    ReceivedErrorStat.REPLY_INVALID_SPEED.incrementStat();
			throw new BadPacketException("invalid speed: " + getSpeed());
		}

        // Copy the sharing computer's IP address from the start of the payload into the _address member variable
		setAddress();

        // Make sure the IP address doesn't start 0 or 255
		if (!NetworkUtils.isValidAddress(getIPBytes())) {
		    ReceivedErrorStat.REPLY_INVALID_ADDRESS.incrementStat();
		    throw new BadPacketException("invalid address");
		}
    }

    /**
     * Copy a QueryReply, giving the copy a new GUID.
     * This is the copy constructor.
     * 
     * We don't actually copy the payload data.
     * Rather, the new QueryReply copy references the same payload data byte array as the first one.
     * Since the payload data can't be changed, this shouldn't matter.
     * 
     * @param guid  The GUID for the copy
     * @param reply The QueryReply object to copy
     * @return      A new QueryReply object with all of the same information, and the new GUID
     */
    public QueryReply(byte[] guid, QueryReply reply) {

        // Call the Message constructor, giving it information from the given QueryReply object and the new GUID
        super(guid, Message.F_QUERY_REPLY, reply.getTTL(), reply.getHops(), reply.getLength());

        // Have this new copy QueryReply share payloads with the given one 
        this._payload = reply._payload;

        // Copy the sharing computer's IP address from the start of the payload into the _address member variable
        setAddress();
    }

    /**
     * Make a new QueryReply object that represents a query hit packet with all the given information.
     * This is the packet maker.
     * Only includes the QHD block if includeQHD is true.
     * 
     * A QueryReply packet has the binary structure outlined below.
     * 
     * Header 23 bytes
     * 
     *   (Represented by a Message object)
     * 
     * Payload 11 bytes
     * 
     *   N                 The number of results in the next section
     *   PPIPIP            The port number and IP address of the sharing computer
     *   SSSS              The sharing computer's upload speed
     * 
     * Results
     * 
     *   (Represented by Response objects)
     * 
     * QHD
     * 
     *   LIME              The ASCII characters of the vendor code
     *   L                 A byte with the length of the next section, which is 2 or 4 bytes long
     *   MVXX              MV are the mask and value bytes, and XX is the size of the XML
     *   C                 A byte that is 1 if the sharing computer can chat, 0 if it can't
     *   ggep              A GGEP block
     *   xml\0             XML text that's null terminated
     * 
     * Client ID 16 bytes
     * 
     *   [Client ID GUID]  The sharing computer's Gnutella client ID GUID
     * 
     * This constructor is private, making it only used internally.
     * Other constructors in this class call it, passing default values.
     * 
     * @param guid               For the header, the GUID that marks this packet unique
     * @param ttl                For the header, the number of times the packet can travel between ultrapeers
     * @param port               For the payload, the sharing computer's port number
     * @param ip                 For the payload, the sharing computer's IP address
     * @param speed              For the payload, the sharing computer's upload speed
     * @param responses          For the payload, the number of file hit results that will come next
     * @param clientGUID         For the end, the sharing computer's client ID GUID that marks it unique on the Gnutella network
     * @param xmlBytes           For the QHD, XML
     * @param includeQHD         True to include the QHD block, false to leave it out
     * @param needsPush          Sets 0x01 in the flags and controls bytes, the sharing computer can't accept TCP connections and will need a push message
     * @param isBusy             Sets 0x04 in the flags and controls bytes, all the sharing computer's upload slots are full right now
     * @param finishedUpload     Sets 0x08 in the flags and controls bytes, the sharing computer has actually uploaded a file
     * @param measuredSpeed      Sets 0x10 in the flags and controls bytes, the speed in this packet is from real measured data, not just a setting the user entered
     * @param supportsChat       For the chat byte, the sharing computer can chat, makes the chat byte 1 and not 0
     * @param supportsBH         Makes "BH" in the GGEP block, the sharing computer supports browse host
     * @param isMulticastReply   Makes "MCAST" in the GGEP block, this query hit is responding to a multicast query
     * @param supportsFWTransfer Makes "FW" in the GGEP block, the sharing computer can do a firewall-to-firewall file transfer
     * @param proxies            Makes "PUSH" in the GGEP block, a Set of objects that implement IpPort with the addresses of the sharing computer's push proxies
     * @return                   A new QueryReply object that represents a query hit packet with all that information
     */
    private QueryReply(byte[] guid, byte ttl, int port, byte[] ip, long speed, Response[] responses, byte[] clientGUID, byte[] xmlBytes, boolean includeQHD,
        boolean needsPush, boolean isBusy, boolean finishedUpload, boolean measuredSpeed, boolean supportsChat, boolean supportsBH, boolean isMulticastReply,
        boolean supportsFWTransfer, Set proxies) {

        // Call the Message constructor to save Gnutella packet header information
        super(
            guid,                  // The GUID that marks this message unique
            Message.F_QUERY_REPLY, // This is a query reply message
            ttl,                   // The number of times it will be able to travel between ultrapeers
            (byte)0,               // No hops yet
            0,                     // We don't know how long the payload will be yet, save 0 and update it later
            16);

        /*
         * 16-byte footer
         */
        
        /*
         * TODO:kfaaborg Why are we passing 16 in the call above?
         * While it's true that QueryReply packets have a 16 byte guid at their end,
         * that argument in the Message constructor is supposed to be the Internet protocol network, like Message.N_TCP or Message.N_UDP
         */

        // Make sure the XML is within 32 KB
        if (xmlBytes.length > XML_MAX_SIZE) throw new IllegalArgumentException("xml too large: " + new String(xmlBytes));

        // Make sure port isn't 0, ip is 4 bytes and doesn't start 0 or 255, speed fits in 4 bytes, and there are less than 256 hits
        final int n = responses.length;
		if      (!NetworkUtils.isValidPort(port))    throw new IllegalArgumentException("invalid port: "          + port);
		else if (ip.length != 4)                     throw new IllegalArgumentException("invalid ip length: "     + ip.length);
        else if (!NetworkUtils.isValidAddress(ip))   throw new IllegalArgumentException("invalid address: "       + NetworkUtils.ip2string(ip));
		else if ((speed & 0xFFFFFFFF00000000l) != 0) throw new IllegalArgumentException("invalid speed: "         + speed);
		else if (n >= 256)                           throw new IllegalArgumentException("invalid num responses: " + n);

        // Save proxies and supportsFWTransfer
        _proxies            = proxies;            // The sharing computer's ultrapeers we can contact to get the sharing computer to give a file to us
        _supportsFWTransfer = supportsFWTransfer; // True if the sharing computer supports firewall-to-firewall transfers

        // Make a new ByteArrayOutputStream that will grow to hold the payload data we write
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {

            /*
             * Write beginning of payload.
             * Downcasts are ok, even if they go negative
             */

            /*
             * The start of the payload is 11 bytes:
             * 
             * N
             * PPIPIP
             * SSSS
             * 
             * N is the number of results that will come after this start.
             * PP is the port number, and IPIP is the IP address
             * SSSS is the speed.
             */

            // Write the first 11 bytes of the payload before the results
            baos.write(n);                          // The number of results that will come after this, 1 byte
            ByteOrder.short2leb((short)port, baos); // The port number, 2 bytes
            baos.write(ip, 0, ip.length);           // The IP address, 4 bytes
            ByteOrder.int2leb((int)speed, baos);    // The speed, 4 bytes

            // Write the results
            for (int left = n; left > 0; left--) { // Loop n times
                Response r = responses[n - left];  // Get the next Response object from the responses array of them
                r.writeToStream(baos);             // Call writeToStream(baos) on it to get it to serialize its data into the ByteArrayOutputStream
            }

            /*
             * After the results, a query hit packet has another part called the QHD.
             * This is a QHD in the style defined by BearShare 2.2.0.
             * 
             * LIME
             * 4
             * MVXX
             * C
             * ggep
             * xml\0
             * 
             * LIME is the ASCII characters of the vendor code, like "LIME" for LimeWire.
             * 4 is a single byte holding the value 4, indicating that the FCXX bytes next take up 4 bytes of space.
             * MV are the mask and value bytes, with bits in M telling what values in V to read.
             * XX is the size of the XML text with a null terminator.
             * C is 1 if the sharing computer can chat, 0 if it can't.
             * The GGEP block starts 0xC3 and keeps its own length.
             * XML text is null terminated.
             */

            // Write the QHD
            if (includeQHD) {

                /*
                 * a) vendor code.  This is hardcoded here for simplicity,
                 * efficiency, and to prevent character decoding problems.  If you
                 * change this, be sure to change CommonUtils.QHD_VENDOR_NAME as
                 * well.
                 */

                // Write the ASCII characters "LIME", our vendor code
                baos.write(76); // 'L'
                baos.write(73); // 'I'
                baos.write(77); // 'M'
                baos.write(69); // 'E'

                // Write the number 4 in a single byte, the length of the flags and controls bytes and the XML size
                baos.write(COMMON_PAYLOAD_LEN);

                // Get the size of a GGEP block with no extensions, which is always 0 bytes
                int ggepLen = _ggepUtil.getQRGGEP(false, false, false, Collections.EMPTY_SET).length; // ggepLen is 0

                /*
                 * The mask bits tell which value bits have information.
                 * If a mask bit is 0, don't read the corresponding value bit.
                 * If a mask bit is 1, you can read it.
                 * If the corresponding value bit is 0, the value is false.
                 * If the value bit is 1, the value is true.
                 * 
                 * The first bit, PUSH_MASK 0x01, is reversed.
                 * A value bit of 1 indicates you can read the answer in the mask bit.
                 */

                // If we have push proxies to list in the GGEP block, we'll set 0x20 we have a GGEP block in the controls byte below
                boolean hasProxies = (_proxies != null) && (_proxies.size() > 0);

                // Compose the mask byte, which tells which bits in the next byte contain information we can read
                byte flags = (byte)(
                    (needsPush && !isMulticastReply ? PUSH_MASK : 0) | // If requested and not on the LAN, set 0x01 the needs push bit
                    BUSY_MASK                                        | // Bit 0x04 in the value byte will tell if all the sharing computer's upload slots are full
                    UPLOADED_MASK                                    | // Bit 0x08 in the value byte will tell if the sharing computer has ever actually uploaded a file
                    SPEED_MASK                                       | // Bit 0x10 in the value byte will tell if the speed is from real measurements, not user settings
                    GGEP_MASK);                                        // Bit 0x20 in the value byte will tell if there is a GGEP block after this

                // Compose the value byte, which has the information
                byte controls = (byte)(
                    PUSH_MASK                                            | // Bit 0x01 in the mask byte will tell if the computer can't accpt TCP and needs a push
                    (isBusy && !isMulticastReply ? BUSY_MASK : 0)        | // If requested and not on the LAN, set 0x04 the is busy bit
                    (finishedUpload ? UPLOADED_MASK : 0)                 | // If requested, set 0x08 the has uloaded bit
                    (measuredSpeed || isMulticastReply ? SPEED_MASK : 0) | // If requested, set 0x10 the real speed bit

                    // If any of the things we need to mention in a GGEP block are true set 0x20 to say there is a GGEP block here
                    (supportsBH || isMulticastReply || hasProxies || supportsFWTransfer ? GGEP_MASK : (ggepLen > 0 ? GGEP_MASK : 0)));

                /*
                 * TODO:kfaaborg Here, the first byte is named flags and the second is named controls.
                 * In parseResults2(), the names are reversed.
                 * The reversed names make more sense than their order here.
                 */

                // Write the flags and controls bytes
                baos.write(flags);
                baos.write(controls);

                // Write 2 bytes that have the size of the XML
                int xmlSize = xmlBytes.length + 1;                  // The size of the XML text and a null terminator
                if (xmlSize > XML_MAX_SIZE) xmlSize = XML_MAX_SIZE; // Truncate the size to 32 KB, even though we won't truncate the XML
                ByteOrder.short2leb(((short)xmlSize), baos);        // Write 2 bytes

                // Write 1 byte that is 0 if the sharing computer can't chat, or 1 if it can
                byte chatSupport = (byte)(supportsChat ? CHAT_MASK : 0);
                baos.write(chatSupport);

                // Write the GGEP block, which starts 0xC3 and has its own internal length
                byte[] ggepBytes = _ggepUtil.getQRGGEP(supportsBH, isMulticastReply, supportsFWTransfer, _proxies);
                baos.write(ggepBytes, 0, ggepBytes.length);

                // Write the XML, which is ASCII text terminated by a 0 byte
                baos.write(xmlBytes, 0, xmlBytes.length);
                baos.write(0); // Write the null terminator
            }

            /*
             * Beyond the QHD, a query hit packet ends with the sharing computer's 16 byte client ID GUID.
             * 
             * [Client ID GUID]
             */

            // Write the sharing computer's client ID GUID
            baos.write(clientGUID, 0, 16);

            // Save the payload data we just composed in this QueryReply object
            _payload = baos.toByteArray();

            // Now we know the length of the payload, write it in the Gnutella packet header
            updateLength(_payload.length);

        // Somehow, our ByteArrayOutputStream broke
        } catch (IOException reallyBad) { ErrorService.error(reallyBad); }

        // Now that we've composed the payload, copy the IP address at 3 to the _address member variable
		setAddress();
    }

	/**
     * Saves the IP address at _payload[3] to the _address member variable.
     * Call after you've composed the data in _payload.
	 */
	private void setAddress() {

        // Copy the bytes of the sharing computer's IP address from the payload we just composed into the _address array
		_address[0] = _payload[3];
        _address[1] = _payload[4];
        _address[2] = _payload[5];
        _address[3] = _payload[6];
	}

    /**
     * Set this QueryReply object's record of the IP address of the computer that sent us this query hit packet and is sharing the files it describes.
     * Sets _address to the new address, and writes the new port number in _payload.
     * 
     * @param addr The IP address of the sharing computer to set in this object
     * @param port The port number of the sharing computer to set in this object
     */
	public void setOOBAddress(InetAddress addr, int port) {

        /*
         * TODO:kfaaborg _payload is supposed to never change, but this method is changing it.
         */

        // Save the IP address in _address
        _address = addr.getAddress();

        // Write the 2 bytes of the port number 1 byte into the payload
		ByteOrder.short2leb((short)port, _payload, 1); // The port is 1 byte into the payload, just beyond the number of results byte
	}

    /**
     * Change the message GUID that marks this query reply as unique.
     * We need this when we want to cache query replies. (do)
     * 
     * @param guid The new message GUID to set in this object
     */
    public void setGUID(GUID guid) {

        // The message GUID is in the Gnutella packet header and handled by code in the Message class
        super.setGUID(guid);
    }

    /**
     * Write the data of this query hit packet's payload to the given OutputStream.
     * 
     * @param out An OutputStream we can call write() on to send data to the remote computer
     */
    public void writePayload(OutputStream out) throws IOException {

        // Write the data of the packet payload
        out.write(_payload);

        // Give this QueryReply object to the SentMessageStatHandler, having it count this as a query reply we sent over TCP
		SentMessageStatHandler.TCP_QUERY_REPLIES.addMessage(this);
    }

    /**
     * Mark this QueryReply as the result of a browse host request.
     * 
     * The GGEP block includes "BH" Browse Host, but that's different.
     * "BH" is present when the sharing computer supports the browse host feature.
     * 
     * Here, _broseHostReply is a flag external code can mark true when we know this packet is the result of a browse host request.
     * BrowseHostHandler.browseExchangeInternal(Socket) calls this to set it to true.
     * 
     * @param isBH True to mark this QueryReply as the result of a browse host request
     */
    public void setBrowseHostReply(boolean isBH) {

        // Save the given value
        _browseHostReply = isBH;
    }

    /**
     * Determine if this QueryRequest object has been marked as the result of a browse host request.
     * 
     * @return True if we previously called setBrowseHostReply(true), false if we didn't
     */
    public boolean isBrowseHostReply() {

        // Return the flag we may have set
        return _browseHostReply;
    }

    /**
     * Get the compressed XML from the end of the query hit packet.
     * 
     * @return The byte array of compressed XML
     */
    public byte[] getXMLBytes() {

        // Parse the payload data and set member variables if we haven't already
        parseResults();

        // Return the byte array of compressed XML
        return _xmlBytes;
    }

    /**
     * Get the number of results in this query hit packet.
     * 
     * @return The number right from the start of the payload.
     */
    public short getResultCount() {

        /*
         * The result of ubyte2int always fits in a short, so downcast is ok.
         */

        // The number is in the first byte in the payload
        return (short)ByteOrder.ubyte2int(_payload[0]);
    }

    /**
     * Get the port number of the computer that is sharing these files and prepared this query hit packet.
     * 
     * @return The sharing computer's port number
     */
    public int getPort() {

        // Read the 2 byte port number from 1 byte into the payload
        return ByteOrder.ushort2int(ByteOrder.leb2short(_payload, 1));
    }

    /**
     * Get the IP address of the computer that is sharing these files and prepared this query hit packet.
     * 
     * @return The sharing computer's IP address as a String like "1.2.3.4"
     */
    public String getIP() {

        // Compose the IP address from the start of the payload into text
        return NetworkUtils.ip2string(_address);
    }

    /**
     * Get the IP address of the computer that is sharing these files and prepared this query hit packet.
     * 
     * @return The sharing computer's IP address as a byte array of 4 bytes
     */
    public byte[] getIPBytes() {

        // Return the address bytes we got from 3 bytes into the payload
        return _address;
    }

    /**
     * Get the upload speed of the computer that is sharing these files and prepared this query hit packet.
     * 
     * @return The computer's upload speed in KB/s
     */
    public long getSpeed() {

        // Read the 4 byte speed from 7 bytes into the payload
        return ByteOrder.uint2long(ByteOrder.leb2int(_payload, 7));
    }

    /**
     * Get an array of Response objects that represent the file information blocks in this query hit packet.
     * 
     * @return                    An array of Response objects
     * @throws BadPacketException We couldn't parse this packet
     */
    public Response[] getResultsArray() throws BadPacketException {

        // Parse the payload data and set member variables if we haven't already, and make sure the parser finished
        parseResults();
        if (_responses == null) throw new BadPacketException();

        // Return the array of Response objects the parser made
        return _responses;
    }

    /**
     * Get an Iterator you can step through the Response objects that represent the file information blocks in this query hit packet.
     * 
     * @return An Iterator you can step through the Response objects we parsed from this query hit packet
     */
    public Iterator getResults() throws BadPacketException {

        // Parse the payload data and set member variables if we haven't already, and make sure the parser finished
        parseResults();
        if (_responses == null) throw new BadPacketException();

        // Make a List out of the _responses array, and return an Iterator on the start of it
        List list = Arrays.asList(_responses);
        return list.iterator();
    }

    /**
     * Get a List of Response objects that represent the file information blocks in this query hit packet.
     * 
     * @return                    A List of Response objects
     * @throws BadPacketException We couldn't parse this packet
     */
    public List getResultsAsList() throws BadPacketException {

        // Parse the payload data and set member variables if we haven't already, and make sure the parser finished
        parseResults();
        if (_responses == null) throw new BadPacketException("results are null");

        // Make a List out of the _responses array, and return it
        List list = Arrays.asList(_responses);
        return list;
    }

    /**
     * Get the sharing computer's vendor code, like "LIME" for LimeWire.
     * This code is the first 4 bytes of the QHD block after the file hit results.
     * 
     * @return The vendor code as a String
     */
    public String getVendor() throws BadPacketException {

        // Parse the payload data and set member variables if we haven't already, and make sure the parser finished
        parseResults();
        if (_vendor == null) throw new BadPacketException();

        // Return the String we parsed
        return _vendor;
    }

    /**
     * Determine if this query hit says the sharing computer can't accept an incomming TCP socket connection.
     * If so, a downloader will need to give it a push packet to get it to open a connection and give a file.
     * This is 0x01 in the mask and values bytes.
     * 
     * @return                    True if the sharing computer will need a push packet, false if it won't
     * @throws BadPacketException We couldn't parse the mask and values bytes, or the mask byte didn't have this bit set
     */
    public boolean getNeedsPush() throws BadPacketException {

        // Parse the payload data and set member variables if we haven't already
        parseResults();

        // Return true, false, or throw an exception
        switch (_pushFlag) {
        case UNDEFINED: throw new BadPacketException();
        case TRUE:      return true;
        case FALSE:     return false;
        default:
            Assert.that(false, "Bad value for push flag: " + _pushFlag);
            return false;
        }
    }

    /*
     * TODO:kfaaborg There is a copy-and-paste error here.
     * The Assert.that() calls contain _pushFlag each place instead of the appropriate member variable.
     */

    /**
     * This query hit says the sharing computer's upload slots are full.
     * A downloader will have to wait to get a file from it.
     * This is 0x04 in the mask and values bytes.
     * 
     * @return                    True if the sharing computer's upload slots are full, false if there are some slots open
     * @throws BadPacketException We couldn't parse the mask and values bytes, or the mask byte didn't have this bit set
     */
    public boolean getIsBusy() throws BadPacketException {

        // Parse the payload data and set member variables if we haven't already
        parseResults();

        // Return true, false, or throw an exception
        switch (_busyFlag) {
        case UNDEFINED: throw new BadPacketException();
        case TRUE:      return true;
        case FALSE:     return false;
        default:
            Assert.that(false, "Bad value for busy flag: " + _pushFlag);
            return false;
        }
    }

    /**
     * This query hit says the sharing computer has successfully uploaded at least 1 file.
     * This is an encouraging sign.
     * This is 0x08 in the mask and values bytes.
     * 
     * @return                    True if the sharing computer has successfully uploaded a file, false if it hasn't yet
     * @throws BadPacketException We couldn't parse the mask and values bytes, or the mask byte didn't have this bit set
     */
    public boolean getHadSuccessfulUpload() throws BadPacketException {

        // Parse the payload data and set member variables if we haven't already
        parseResults();

        // Return true, false, or throw an exception
        switch (_uploadedFlag) {
        case UNDEFINED: throw new BadPacketException();
        case TRUE:      return true;
        case FALSE:     return false;
        default:
            Assert.that(false, "Bad value for uploaded flag: " + _pushFlag);
            return false;
        }
    }

    /**
     * The sharing computer's upload speed this query hit reports comes from measured data, not just something the user configured.
     * This is 0x10 in the mask and values bytes.
     * 
     * @return                    True if the speed is from real data, false if it's from user settings
     * @throws BadPacketException We couldn't parse the mask and values bytes, or the mask byte didn't have this bit set
     */
    public boolean getIsMeasuredSpeed() throws BadPacketException {

        // Parse the payload data and set member variables if we haven't already
        parseResults();

        // Return true, false, or throw an exception
        switch (_measuredSpeedFlag) {
        case UNDEFINED: throw new BadPacketException();
        case TRUE:      return true;
        case FALSE:     return false;
        default:
            Assert.that(false, "Bad value for measured speed flag: " + _pushFlag);
            return false;
        }
    }

    /**
     * Determine if the sharing computer can chat.
     * LimeWire and Shareaza can chat this way.
     * This comes from the chat byte in the query hit's private area.
     * 
     * @return True if the computer supports chat, false if it doesn't
     */
    public boolean getSupportsChat() {

        // Parse the payload data and set member variables if we haven't already, and return the value the parser set
        parseResults();
        return _supportsChat;
    }

    /**
     * Determine if the sharing computer can do a firewall-to-firewall file transfer.
     * In the GGEP block, the "FW" extension indicates it can.
     * 
     * @return True if the computer supports firewall-to-firewall transfers
     */
    public boolean getSupportsFWTransfer() {

        // Parse the payload data and set member variables if we haven't already, and return the value the parser set
        parseResults();
        return _supportsFWTransfer;
    }

    /**
     * Determine what version of firewall-to-firewall file transfers the sharing computer supports.
     * This is GGEP "FW" extension's byte value.
     * 
     * @return The version of firewall-to-firewall transfers the sharing computer supports, like 1.
     *         0 if it can't do it.
     */
    public byte getFWTransferVersion() {

        // Parse the payload data and set member variables if we haven't already, and return the value the parser set
        parseResults();
        return _fwTransferVersion;
    }

    /**
     * Determine if the sharing computer supports the browse host feature.
     * If it does, we can get a list of all the files it's sharing.
     * In the GGEP block, the "BH" extension indicates this is possible.
     * 
     * @return True if the computer supports browse host
     */
    public boolean getSupportsBrowseHost() {

        // Parse the payload data and set member variables if we haven't already, and return the value the parser set
        parseResults();
        return _supportsBrowseHost;
    }

    /**
     * Determine if this query hit is the response to a query we sent multicast on the LAN.
     * In the GGEP block, the "MCAST" extension marks the query hit this way.
     * 
     * @return True if this is a multicast query response packet
     */
    public boolean isReplyToMulticastQuery() {

        // Parse the payload data and set member variables if we haven't already, and return the value the parser set
        parseResults();
        return _replyToMulticast;
    }

    /**
     * Get the addresses of the sharing computer's push proxies.
     * If the sharing computer isn't externally contactable, we can send a push message to one of its push proxies.
     * The push proxy will relay the message to the computer, which will push open a connection to us.
     * The connection with be a TCP socket connection that starts with the HTTP GIV header, and gives us the file we want.
     * In the GGEP block, the "PUSH" extension lists the IP addresses and port numbers of the sharing computer's push proxies.
     * 
     * @return A IpPortSet of IPPortCombo objects made from the IP addresses and port numbers from the "PUSH" extension value
     */
    public Set getPushProxies() {

        // Parse the payload data and set member variables if we haven't already, and return the HashSet the parser made
        parseResults();
        return _proxies;
    }

    /**
     * Get a HostData object with information about the sharing computer.
     * After we parsed the payload, we gave this QueryReply packet to the HostData constructor.
     * A HostData object keeps information about the sharing computer, not the files it's sharing.
     * 
     * @return The HostData object with information about the sharing computer from this query hit packet
     */
    public HostData getHostData() throws BadPacketException {

        // Parse the payload data and set member variables if we haven't already, and make sure the parser finished
        parseResults();
        if (_hostData == null) throw new BadPacketException();

        // Return the HostData object that the parser made
        return _hostData;
    }

    /**
     * Parse the payload of this query hit packet.
     * Sets member variables with information from the payload.
     */
    private void parseResults() {

        // If we've already parsed the payload, we don't have to do it again
        if (_parsed) return;

        // Record that we've parsed the payload, and parse it
        _parsed = true;
        parseResults2();
    }

    /**
     * Parse the payload of this query hit packet.
     * Sets the following member variables:
     * 
     * _responses          An array of Response objects representing the information about each hit file
     * _fwTransferVersion  The version of firewall-to-firewall transfers the sharing computer supports, probably 1
     * _supportsFWTransfer The sharing compuer can do a firewall-to-firewall file transfer
     * _xmlBytes           Compressed XML from the packet
     * _vendor             The vendor code, like "LIME"
     * _pushFlag           The sharing computer can't accept TCP connections, so downloaders should send a push packet
     * _busyFlag           All the sharing computer's upload slots are full right now
     * _uploadedFlag       The sharing computer has successfully uploaded at least 1 file its been sharing
     * _measuredSpeedFlag  The speed in this query hit packet is from data the program measured, not a setting the user entered
     * _supportsChat       The sharing computer supports LimeWire and Shareaza chat
     * _supportsBrowseHost The sharing computer supports the browse host feature
     * _replyToMulticast   This query hit packet is marked as a response to a multicast LAN query
     * _proxies            A list of the sharing computer's push proxies, which are its ultrapeers that can forward it a push request
     * _hostData           A new HostData object made from this newly parsed QueryReply object
     * 
     * Throws a BadPacketException if it finds impossible data and can't continue.
     * Doesn't save the member variable values until the end, when we're sure the packet isn't bad.
     */
    private void parseResults2() {

        /*
         * The payload begins with 11 bytes
         * 
         * N       The number of results in the next section
         * PPIPIP  The port number and IP address of the sharing computer
         * SSSS    The sharing computer's upload speed
         */

        // The index in the payload of the next response
        int i = 11; // The first response is 11 bytes into the payload, beyond the 1 byte number, 6 byte address, and 4 byte speed

        /*
         * 1. Extract responses.  These are not copied to this.responses until
         * they are verified.  Note, however that the metainformation need not be
         * verified for these to be acceptable.  Also note that exceptions are
         * silently caught.
         */

        // Parse the file hit result blocks into Response objects
        int left = getResultCount();               // Read N, the number of file information blocks we still have to read
        Response[] responses = new Response[left]; // Make an array that will hold the total number of Response objects

        try {

            // Put a ByteArrayInputStream on the payload that will let us read the data of the results set byte by byte
            InputStream bais = new ByteArrayInputStream(_payload, i, _payload.length - i); // Clip the input stream from the start of the results to the end of the payload

            // Loop until the packet doesn't have any records left to read
            for ( ; left > 0; left--) {

                // Parse the data about a shared file into a Response object
                Response r = Response.createFromStream(bais); // Have the Response.createFromStream() method parse the data into a new Response object
                responses[responses.length-left] = r;         // Save the Response object in the array
                i += r.getLength();                           // Move our index in the payload beyond the data of the result we just parsed
            }

            // Save the array of Response objects
            this._responses = responses;

        // Catch exceptions and just leave
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        } catch (IOException e) {
            return;
        }

        /*
         * 2. Extract BearShare-style metainformation, if any.  Any exceptions
         * are silently caught.  The definitive reference for this format is at
         * http://www.clip2.com/GnutellaProtocol04.pdf.  Briefly, the format is 
         *       vendor code           (4 bytes, case insensitive)
         *       common payload length (4 byte, unsigned, always>0)
         *       common payload        (length given above.  See below.)
         *       vendor payload        (length until clientGUID)
         * The normal 16 byte clientGUID follows, of course.
         * 
         * The first byte of the common payload has a one in its 0'th bit* if we
         * should try a push.  However, if there is a second byte, and if the
         * 0'th bit of this byte is zero, the 0'th bit of the first byte should
         * actually be interpreted as MAYBE.  Unfortunately LimeWire 1.4 failed
         * to set this bit in the second byte, so it should be ignored when
         * parsing, though set on writing.
         * 
         * The remaining bits of the first byte of the common payload area tell
         * whether the corresponding bits in the optional second byte is defined.
         * The idea behind having two bits per flag is to distinguish between
         * YES, NO, and MAYBE.  These bits are as followed:
         *       bit 1*  undefined, for historical reasons
         *       bit 2   1 iff server is busy
         *       bit 3   1 iff server has successfully completed an upload
         *       bit 4   1 iff server's reported speed was actually measured, not
         *               simply set by the user.
         * 
         * GGEP Stuff
         * Byte 5 and 6, if the 5th bit is set, signal that there is a GGEP
         * block.  The GGEP block will be after the common payload and will be
         * headed by the GGEP magic prefix (see the GGEP class for more details.
         * 
         * If there is a GGEP block, then we look to see what is supported.
         * 
         * Here, we use 0-(N-1) numbering.  So "0'th bit" refers to the least
         * significant bit.
         * 
         * ----------------------------------------------------------------
         * 
         * QHD UPDATE 8/17/01
         * Here is an updated QHD spec.
         * 
         * Byte 0-3 : Vendor Code
         * Byte 4   : Public area size (COMMON_PAYLOAD_LEN)
         * Byte 5-6 : Public area (as described above)
         * Byte 7-8 : Size of XML + 1 (for a null), you need to count backward
         * from the client GUID.
         * Byte 9   : private vendor flag
         * Byte 10-X: GGEP area
         * Byte X-beginning of xml : (new) private area
         * Byte (payload.length - 16 - xmlSize (above)) -
         *      (payload.length - 16 - 1) : XML!!
         * Byte (payload.length - 16 - 1) : NULL
         * Last 16 Bytes: client GUID.
         */

        /*
         * After the results is the QHD:
         * 
         * LIME   The ASCII characters of the vendor code
         * L      A byte with the length of the next section, which is 2 or 4 bytes long
         * MVXX   MV are the mask and value bytes, and XX is the size of the XML
         * C      A byte that is 1 if the sharing computer can chat, 0 if it can't
         * ggep   A GGEP block
         * xml\0  XML text that's null terminated
         */

        try {

            // Make sure we're not so far into the payload there isn't room for the 16 byte Gnutella client ID at the end
			if (i >= (_payload.length - 16)) throw new BadPacketException("No QHD");

            // Variables for values we'll find and read
            String  vendorT             = null;
            int     pushFlagT           = UNDEFINED;
            int     busyFlagT           = UNDEFINED;
            int     uploadedFlagT       = UNDEFINED;
            int     measuredSpeedFlagT  = UNDEFINED;
            boolean supportsChatT       = false;
            boolean supportsBrowseHostT = false;
            boolean replyToMulticastT   = false;
            Set     proxies             = null;

            try {

                /*
                 * Must use ISO encoding since characters are more than two
                 * bytes on other platforms.  TODO: test on different installs!
                 */

                // Read the 4 ASCII character vendor code, like "LIME"
                vendorT = new String(_payload, i, 4, "ISO-8859-1");
                Assert.that(vendorT.length() == 4, "Vendor length wrong.  Wrong character encoding?");

            } catch (UnsupportedEncodingException e) { Assert.that(false, "No support for ISO-8859-1 encoding"); }
            i += 4; // Move past the vendor code LIME to put i on L, the length byte

            /*
             * The packets we send always have a L of 4.
             * The packets we receive have L of 2 or 4.
             * A L of 2 means that the MVXX section is just MV, and there's no xml\0 at all.
             */

            // Read the length byte, which will be 2 or 4
            int length = ByteOrder.ubyte2int(_payload[i]); // length is 4
            if (length <= 0) throw new BadPacketException("Common payload length zero.");
            i++; // Move past the length byte L to put i on MVXX, the mask and value bytes and XML length

            // Make sure reading the MVXX section won't put us into the GUID at the end
            if ((i + length) > (_payload.length - 16)) throw new BadPacketException("Common payload length imprecise!");

            // The length is 2 or 4, indicating the presence of the MV mask and value bytes
            if (length > 1) {

                // Copy the mask and value bytes
                byte control = _payload[i];
                byte flags   = _payload[i + 1];

                // If the mask byte has a bit set, read the corresponding bit from the value byte
                if ((flags   & PUSH_MASK)     != 0) pushFlagT          = (control & PUSH_MASK)     == 1 ? TRUE : FALSE; // The push bits are reversed
                if ((control & BUSY_MASK)     != 0) busyFlagT          = (flags   & BUSY_MASK)     != 0 ? TRUE : FALSE;
                if ((control & UPLOADED_MASK) != 0) uploadedFlagT      = (flags   & UPLOADED_MASK) != 0 ? TRUE : FALSE;
                if ((control & SPEED_MASK)    != 0) measuredSpeedFlagT = (flags   & SPEED_MASK)    != 0 ? TRUE : FALSE;

                // The mask and value byets indicate there is a GGEP block here
                if ((control & GGEP_MASK) != 0 && (flags & GGEP_MASK) != 0) {

                    // Move magicIndex forward until you find 0xC3, the start of the GGEP block
                    int magicIndex = i + 2; // i is still on MVXX, start magicIndex on XX
                    for ( ; (_payload[magicIndex] != GGEP.GGEP_PREFIX_MAGIC_NUMBER) && (magicIndex < _payload.length); magicIndex++) ; // Move beyond XX and C

                    try {

                        // Parse the data into a new GGEP object
                        GGEP ggep = new GGEP(_payload, magicIndex, null);

                        // Look for the presence of the "BH" extension, indicating the sharing computer supports browse host
                        supportsBrowseHostT = ggep.hasKey(GGEP.GGEP_HEADER_BROWSE_HOST);

                        // The GGEP block has "FW", indicating the sharing computer can do firewall-to-firewall transfers
                        if (ggep.hasKey(GGEP.GGEP_HEADER_FW_TRANS)) {

                            // Get the version number, like 1, and set _supportsFWTransfer to true
                            _fwTransferVersion = ggep.getBytes(GGEP.GGEP_HEADER_FW_TRANS)[0]; // LimeWire supports version 1 of this feature
                            _supportsFWTransfer = _fwTransferVersion > 0;
                        }

                        // If the GGEP block has "MCAST", this query hit is a response to a multicast query on the LAN
                        replyToMulticastT = ggep.hasKey(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);

                        // Read the GGEP "PUSH" extension value, the addresses of the sharing computer's push proxies
                        proxies = _ggepUtil.getPushProxies(ggep); // Returns an IpPortSet of IPPortCombo objects

                    // Catch and ignore the exceptions
                    } catch (BadGGEPBlockException ignored) {
                    } catch (BadGGEPPropertyException bgpe) {}
                }

                // Move i past MV to put it on XX
                i += 2;
            }

            // The length is 4, indicating MVXX with XX holding the size of the XML, and i points to XX
            if (length > 2) {

                // Read the 2 bytes of the length XX, move i past them, and get the length number
                int a, b, temp;
                temp = ByteOrder.ubyte2int(_payload[i++]); // Read the first byte and then move i past it
                a = temp;
                temp = ByteOrder.ubyte2int(_payload[i++]); // Read the second byte and them move i to be beyond them both
                b = temp << 8;                             // Shift the bits of the second byte up
                int xmlSize = a | b;                       // Assemble the number length

                /*
                 * There are 2 ways to not have any XML.
                 * LimeWire always sets the length L to 4, and then puts a length of 0 in XX.
                 * Other clients have a length L of 2, meaning there's no size and no XML.
                 */

                // There is XML
                if (xmlSize > 1) {

                    // Calculate where the XML must start by counting backwards from the end
                    int xmlInPayloadIndex = // The index from the start of the payload to the start of the XML
                        _payload.length     // The length of the whole payload
                        - 16                // 16 bytes before that, the client ID GUID starts
                        - xmlSize;          // The size of the XML before that, the null terminated XML must start

                    // Copy the XML data from the packet payload
                    _xmlBytes = new byte[xmlSize - 1]; // Make an array that will hold it without the null terminator
                    System.arraycopy(_payload, xmlInPayloadIndex, _xmlBytes, 0, (xmlSize - 1)); // Copy the bytes of compressed XML, leaving behind the null terminator

                // There was a length for XML, but that length is 0
                } else {

                    // Point _xmlBytes at our 0 length byte array
                    _xmlBytes = DataUtils.EMPTY_BYTE_ARRAY;
                }
            }

            /*
             * Parse LimeWire's private area.  Currently only a single byte
             * whose LSB is 0x1 if we support chat, or 0x0 if we do.
             * Shareaza also supports our chat, don't disclude them...
             */

            /*
             * Here's what the query hit packet looks like, from the QHD to the end.
             * 
             * LIME              The ASCII characters of the vendor code
             * L                 A byte with the length of the next section, which is 2 or 4 bytes long
             * MVXX              MV are the mask and value bytes, and XX is the size of the XML
             * (private area) C  A byte that is 1 if the sharing computer can chat, 0 if it can't
             * ggep              A GGEP block
             * xml\0             XML text that's null terminated
             * GGGGUUUUIIIIDDDD  The sharing computer's client ID GUID
             * 
             * To find the GGEP block, we started on XX, and looked forward until we found 0xC3.
             * 
             * We got the XML length by checking that L was 4, and then reading the number in XX.
             * Then, we looked back from the end, past the 16 GUID bytes, and back past the XML.
             * 
             * The space between MVXX and ggep is called the private area.
             * In the query hit packets that LimeWire sends, it's just 1 byte that tells if we support chat or not.
             * Other Gnutella programs can put more or different things here.
             * 
             * At this point, i is beyond MV or MVXX, putting it on the private area.
             */

            // If this query hit packet has a private area, and it's from LimeWire or Shareaza
            int privateLength = _payload.length - i; // privateLength is the size of C, ggep, xml\0, and GGGGUUUUIIIIDDDD
            if (privateLength > 0 && (vendorT.equals("LIME") || vendorT.equals("RAZA"))) { // Shareaza supports the same chat as we do

                // Read the C byte, and see if the lowest bit is set
                byte privateFlags = _payload[i];
                supportsChatT = (privateFlags & CHAT_MASK) != 0;
            }

            // Make sure there's enough room beyond i for the client ID GUID
            if (i > _payload.length - 16) throw new BadPacketException("Common payload length too large.");

            // We made it through all that without throwing an exception, save the values we parsed
            Assert.that(vendorT != null);
            this._vendor             = vendorT.toUpperCase(Locale.US);
            this._pushFlag           = pushFlagT;
            this._busyFlag           = busyFlagT;
            this._uploadedFlag       = uploadedFlagT;
            this._measuredSpeedFlag  = measuredSpeedFlagT;
            this._supportsChat       = supportsChatT;
            this._supportsBrowseHost = supportsBrowseHostT;
            this._replyToMulticast   = replyToMulticastT;
            if (proxies == null) this._proxies = Collections.EMPTY_SET; // Point _proxies at an empty Set instead of making it null
            else                 this._proxies = proxies;

            // Give this newly parsed QueryReply object to the HostData constructor
            this._hostData = new HostData(this); // Save the HostData object that keeps information about the computer sharing these files

            // We did all that without anything throwing an exception
            debug("QR.parseResults2(): returning w/o exception.");

        // The data and lengths we found made it impossible to continue reading this packet
        } catch (BadPacketException e) {

            // Leave without having saved any of the values we parsed
            debug("QR.parseResults2(): bpe = " + e);
            return;

        // Nothing seems to throw this
        } catch (IndexOutOfBoundsException e) {

            // Leave without having saved any of the values we parsed
            debug("QR.parseResults2(): index exception = " + e);
            return;
        }
    }

    /**
     * Get the sharing computer's Gnutella client ID GUID.
     * It wrote it into the end of this query hit packet.
     * 
     * @return A 16 byte array with the client ID GUID
     */
    public byte[] getClientGUID() {

        // We haven't parsed for the client ID GUID yet
        if (clientGUID == null) {

            // Make a 16 byte array that can hold it
            byte[] result = new byte[16];

            /*
             * Copy the last 16 bytes of payload to result.  Note that there may
             * be metainformation before the client GUID.  So it is not correct
             * to simply count after the last result record.
             */

            // Copy the last 16 bytes of the payload
            int length = super.getLength();
            System.arraycopy(_payload, length - 16, result, 0, 16);

            // Save the GUID bytes in this object
            clientGUID = result;
        }

        // Return the GUID we parsed just now or previously
        return clientGUID;
    }

    /**
     * Doesn't strip the payload from this query hit.
     * 
     * @return A reference to this same QueryReply object
     */
    public Message stripExtendedPayload() {

        // Return a reference to this same object
        return this;
    }

    /**
     * Express this QueryReply object as text.
     * Composes a String with multiple lines, like:
     * 
     * QueryReply::
     * 5 hits
     * {guid=00FF00FF00FF00FF00FF00FF00FF00FF, ttl=1, hops=1, priority=1}
     * ip: 1.2.3.4
     * 
     * @return A String with information from this QueryReply object.
     */
    public String toString() {

        // Compose text with information in this QueryReply object
        return ("QueryReply::\r\n" + getResultCount() + " hits\r\n" + super.toString() + "\r\n" + "ip: " + getIP() + "\r\n");
    }

	/**
     * Estimate how likely it is we'll be able to get the files this query hit packet lists.
     * Weighs information the sharing computer told us about itself in the packet.
     * 
     * Looks at 0x04 in the mask and values bytes, are all the sharing computer's upload slots full.
     * Determines if we and the sharing computer can connect any number of ways:
     * If we're both on the same LAN, we can.
     * If the sharing computer is externally contactable, we can.
     * If the sharing computer has push proxys, we probably can.
     * If we and the sharing computer can do a firewall-to-firewall transfer, we probably can.
     * 
     * This method calculates the quality of service for a given host.  The
     * calculation is some function of whether or not the host is busy, whether
     * or not the host has ever received an incoming connection, etc.
     * 
     * Moved this code from SearchView to here permanently, so we avoid
     * duplication.  It makes sense from a data point of view, but this method
     * isn't really essential an essential method.
     * 
     * Takes iFirewalled switch to indicate if the client is firewalled or
     * not.  See RouterService.acceptingIncomingConnection or Acceptor for
     * details.
     * 
     * Returns a int from -1 to 3, with -1 for "never work" and 3 for "always
     * work".  Typically a return value of N means N+1 stars will be displayed
     * in the GUI.
     * 
     * @param iFirewalled True if our TCP connect back tests have failed, meaning that we're not externally contactable.
     *                    False if our TCP connect back test have succeeded, meaning remote computers can connect to us.
     * @return            -1  There is no way we can get these files.
     *                    0   It's uncertain how likely it is we can get these files.
     *                    1   We might be able to get these files.
     *                    2-4 Our chances of getting these files are better and better.
     */
	public int calculateQualityOfService(boolean iFirewalled) {

        // Values for 3 possible answers
        final int YES   = 1;  // The packet contains information, and that information is yes
        final int MAYBE = 0;  // The packet doesn't contain this piece of information
        final int NO    = -1; // The packet contains information, and that information is false

        // Determine if all the sharing computer's upload slots are full
		int busy;
		try {
			busy = this.getIsBusy() ? YES : NO; // Look at 0x04 in the mask and values bytes
		} catch (BadPacketException e) {
            busy = MAYBE; // This bit in the mask and values bytes isn't set
		}

		// Determine if the sharing computer is firewalled
		int heFirewalled;
		boolean isMCastReply = this.isReplyToMulticastQuery(); // Look for GGEP "MCAST", this query hit is in response to a multicast query we sent on the LAN

		// This query hit is in response to a multicast query we sent on the LAN
		if (isMCastReply) {

            // We're both on the LAN, and there is no firewall between us
		    iFirewalled  = false; // This sharing computer could connect to us
		    heFirewalled = NO;    // We could connect to the sharing computer

        // This isn't a multicast response, but the sharing computer is right here on our LAN
		} else if (NetworkUtils.isPrivateAddress(this.getIPBytes())) {

		    // The sharing computer is firewalled from us
			heFirewalled = YES;

        // This isn't a multicast response, and the sharing computer is far away on the Internet
		} else {

		    // Set heFirewalled from 0x01 in the mask and values bytes, is the sharing computer firewalled
			try {
				heFirewalled = this.getNeedsPush() ? YES : NO;
            } catch (BadPacketException e) {
                heFirewalled = MAYBE; // This bit in the mask and values bytes isn't set
			}
		}

        // Determine if the sharing computer has push proxies, its ultrapeers we can pass a message through to get it to give us a file, look for GGEP "PUSH"
        boolean hasPushProxies = false;
        if ((this.getPushProxies() != null) && (this.getPushProxies().size() > 1)) hasPushProxies = true;

        // Determine if the sharing computer can do a firewall-to-firewall file transfer, and if we can too
        if (getSupportsFWTransfer() && UDPService.instance().canDoFWT()) {

            // We can both do a firewall-to-firewall file transfer, set values to show no connectivity problems at all
            iFirewalled  = false;
            heFirewalled = NO;
        }

        /*
         * In the old days, busy hosts were considered bad.  Now they're ok (but
         * not great) because of alternate locations.  WARNING: before changing
         * this method, take a look at isFirewalledQuality!
         */

        // The sharing computer and we have the same external Internet IP address, indicating we're together on the same LAN
		if (Arrays.equals(_address, RouterService.getAddress())) {

            // We have an excellent chance of getting the sharing computer's files, display them in the results list
			return 3;

        // This query hit packet is in response to a multicast query we sent on the LAN
        } else if (isMCastReply) {

            // We have an excellent chance of getting the sharing computer's files, display them in the results list
            return 4;

        // The sharing computer can't connect TCP to us, and we can't connect TCP to it
        } else if (iFirewalled && heFirewalled == YES) {

            // We can never get the sharing computer's files
            return -1;

        // The sharing computer didn't say if it has free upload slots, and didn't say if it is externally contactable
        } else if (busy == MAYBE || heFirewalled == MAYBE) {

            // The sharing computer is running old Gnutella software, we don't know how likely we would be able to get its files
            return 0;

        // All the sharing computer's upload slots are full
        } else if (busy == YES) {

            // Make sure that the sharing computer is externally contactable, or we are
            Assert.that(heFirewalled == NO || !iFirewalled);

            // Report our chance of success based on if the sharing computer is externally contactable or not
            if (heFirewalled == YES) return 0; // We'd have to send a push packet to a busy computer, medium chance of success
            else return 1;                     // We can connect directly, slightly better chance of success

        // The sharing computer has free upload slots
        } else if (busy == NO) {

            // Make sure that the sharing computer is externally contactable, or we are
            Assert.that(heFirewalled == NO || !iFirewalled);

            // Report our chance of success based on how easy it will be to connect to the sharing computer
            if (heFirewalled == YES && !hasPushProxies) return 2; // The sharing computer is firewalled and doesn't have any push proxies
            else                                        return 3; // The sharing computer isn't firewalled or has some push proxies, making our chances better

        // Those options should cover every possibility
        } else {

            // Make a note we made it here, and report that we can never get these files
            Assert.that(false, "Unexpected case!");
            return -1;
        }
	}

	/**
     * Not used.
     * 
	 * Utility method for determining whether or not the given "quality"
	 * score for a <tt>QueryReply</tt> denotes that the host is firewalled
	 * or not.
	 * @param quality the quality, or score, in question
	 * @return <tt>true</tt> if the quality denotes that the host is
	 * firewalled, otherwise <tt>false</tt>
     */
	public static boolean isFirewalledQuality(int quality) {
        return quality == 0 || quality == 2;
	}

    /**
     * Record that we're dropping this query hit packet.
     * Gives this QueryReply object to the DroppedSentMessageStatHandler for TCP query replies.
     */
	public void recordDrop() {

        // Give this QueryReply object to the DroppedSentMessageStatHandler for TCP query replies
		DroppedSentMessageStatHandler.TCP_QUERY_REPLIES.addMessage(this);
	}

    /** Change debugOn to true to have code in this class print out information as the program runs. */
    public final static boolean debugOn = false;

    /**
     * If debugOn is true, writes the given text with System.out.println().
     * 
     * @param out The line to write out
     */
    public static void debug(String out) {

        // If debugging is turned on, write out the given text
        if (debugOn) System.out.println(out);
    }

    /**
     * If debugOn is true, prints a given exception's stack trace with e.printStackTrace().
     * 
     * @param e An exception
     */
    public static void debug(Exception e) {

        // If debugging is turned on, have the exception print its stack trace
        if (debugOn) e.printStackTrace();
    }

    /**
     * GGEPUtil handles the GGEP tasks the QueryReply class needs.
     * It caches GGEP blocks that we are likely to use.
     * This is faster than making them each time.
     */
    static class GGEPUtil {

        /**
         * An empty array that represents the data of a GGEP block with no extensions.
         * The length of this array is 0.
         */
        private final byte[] _standardGGEP;

        /**
         * The data of a GGEP block with the "BH" Browse Host extension, which has no value.
         * "BH" indicates the computer can be browsed.
         */
        private final byte[] _bhGGEP;

        /**
         * The data of a GGEP block with the "MCAST" Multicast extension, which has no value.
         * "MCAST" indicates this query hit is in response to a multicast query.
         */
        private final byte[] _mcGGEP;

        /**
         * The data of a GGEP block with the "BH" Browse Host and "MCAST" Multicast extensions, neither of which have values.
         * "BH" indicates the computer can be browsed, and "MCAST" indicates this query hit is in response to a multicast query.
         */
        private final byte[] _comboGGEP;

        /**
         * Make the GGEPUtil object.
         * The QueryReply class makes a new static GGEPUtil object named _ggepUtil with this constructor.
         * Makes GGEP blocks with "BH" and "MCAST", serializes them, and saves the byte arrays of data for the QueryReply class to use.
         */
        public GGEPUtil() {

            // Make a new ByteArrayOutputStream that will grow to hold the data we write to it
            ByteArrayOutputStream oStream = new ByteArrayOutputStream();

            // Set up _standardGGEP, a GGEP block with no extensions
            try {
                GGEP standard = new GGEP(false); // Hide 0s with COBS encoding
                standard.write(oStream);
            } catch (IOException writeError) {}
            _standardGGEP = oStream.toByteArray(); // Returns an empty array, with length 0

            // Set up _bhGGEP, a GGEP block with "BH" Browse Host, indicating the computer can be browsed
            oStream.reset();
            try {
                GGEP bhost = new GGEP(false);            // Hide 0s with COBS encoding
                bhost.put(GGEP.GGEP_HEADER_BROWSE_HOST); // Add "BH" Browse Host, you can browse the files the computer is sharing, no value
                bhost.write(oStream);
            } catch (IOException writeError) {}
            _bhGGEP = oStream.toByteArray();
            Assert.that(_bhGGEP != null);

            // Set up _mcGGEP, a GGEP block with "MCAST" Multicast, indicating this query hit is responding directly to a multicast query
            oStream.reset();
            try {
                GGEP mcast = new GGEP(false);                   // Hide 0s with COBS encoding
                mcast.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE); // Add "MCAST" Multicast, this query hit is responding directly to a multicast query
                mcast.write(oStream);
            } catch (IOException writeError) {}
            _mcGGEP = oStream.toByteArray();
            Assert.that(_mcGGEP != null);

            // Set up _comboGGEP, a GGEP block with "BH" Browse Host and "MCAST" Multicast
            oStream.reset();
            try {
                GGEP combo = new GGEP(false);                   // Hide 0s with COBS encoding
                combo.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE); // Add "MCAST" Multicast, this query hit is responding directly to a multicast query
                combo.put(GGEP.GGEP_HEADER_BROWSE_HOST);        // Add "BH" Browse Host, you can browse the files the computer is sharing, no value
                combo.write(oStream);
            } catch (IOException writeError) {}
            _comboGGEP = oStream.toByteArray();
            Assert.that(_comboGGEP != null);
        }

        /**
         * Get the data of a GGEP block with the "BH", "MCAST", "FW", and "PUSH" extensions.
         * Looks up the appropriate cached GGEP block data, or makes a new custom one.
         * Serializes it, and returns a byte array of its data.
         * 
         * @param supportsBH          True if you want the GGEP block to have "BH" Browse Host to indicate the computer can be browsed
         * @param isMulticastResponse True if you want the GGEP block to have "MCAST" Multicast to indicate this query reply is in response to a multicast query
         * @param supportsFWTransfer  True if you want the GGEP block to have "FW" indicating the computer supports firewall-to-firewall file transfer
         * @param proxies             The IP addresses of the computer's ultrapeers, it's push proxies, a Set of objects that support the IpPort interface
         * @return                    The serialized data of the GGEP block to use
         */
        public byte[] getQRGGEP(boolean supportsBH, boolean isMulticastResponse, boolean supportsFWTransfer, Set proxies) {

            // Start out pointing retGGEPBlock at _standardGGEP, an empty byte array that represents a GGEP block with no extensions
            byte[] retGGEPBlock = _standardGGEP;

            // The caller gave us push proxies
            if ((proxies != null) && (proxies.size() > 0)) {

                // We'll only list up to 4 push proxies in the GGEP block
                final int MAX_PROXIES = 4;

                // Make a new GGEP block and add the requested extensions to it
                GGEP retGGEP = new GGEP();
                if (supportsBH)          retGGEP.put(GGEP.GGEP_HEADER_BROWSE_HOST);                                  // Add "BH" Browse Host
                if (isMulticastResponse) retGGEP.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);                           // Add "MCAST" Multicast Response
                if (supportsFWTransfer)  retGGEP.put(GGEP.GGEP_HEADER_FW_TRANS, new byte[] {UDPConnection.VERSION}); // Add "FW" Firewall-to-firewall transfer, version 1

                /*
                 * proxies is a Set of objects that support the IpPort iterface.
                 * This makes it like a list of IP addresses and port numbers.
                 * These are the addresses of the computer's push proxies.
                 * A computer's push proxies are just its ultrapeers.
                 * 
                 * If the sharing computer isn't externally contactable, we can send a push message to one of its push proxies.
                 * The push proxy will relay the message to the computer, which will push open a connection to us.
                 * The connection with be a TCP socket connection that starts with the HTTP GIV header, and gives us the file we want.
                 */

                // Loop through the list of push proxies to compose the "PUSH" extension value
                ByteArrayOutputStream baos = new ByteArrayOutputStream(); // Make a ByteArrayOutputStream that will grow to hold the value data we write
                int numWritten = 0;                                       // Keep track of how many addresses we've written, we'll limit ourselves to MAX_PROXIES 4
                Iterator iter = proxies.iterator();
                while (iter.hasNext() && (numWritten < MAX_PROXIES)) {    // Loop until we run out of addresses or we've written 4
                    IpPort ppi = (IpPort)iter.next();
                    String host = ppi.getAddress();                       // Get the IP address and port number from this object in the list
                    int port = ppi.getPort();
                    try {
                        IPPortCombo combo = new IPPortCombo(host, port);  // Wrap it into an IPPortCombo object, and serialize it into 6 bytes of "PUSH" value
                        baos.write(combo.toBytes());
                        numWritten++;                                     // We've written one more
                    } catch (UnknownHostException bad) {                  // InetAddress.getByName() didn't like the IP address text, go on to the next one
                    } catch (IOException terrible) { ErrorService.error(terrible); } // Our ByteArrayOutputStream broke somehow
                }

                try {

                    // If we wrote a value for "PUSH", add the extension
                    if (numWritten > 0) retGGEP.put(GGEP.GGEP_HEADER_PUSH_PROXY, baos.toByteArray());

                    // Serialize the GGEP block into a byte array we'll return
                    baos.reset();
                    retGGEP.write(baos);
                    retGGEPBlock = baos.toByteArray();

                // Our ByteArrayOutputStream broke somehow
                } catch (IOException terrible) { ErrorService.error(terrible); }

                /*
                 * else if (supportsBH && supportsFWTransfer &&
                 * isMulticastResponse), since supportsFWTransfer is only helpful
                 * if we have proxies
                 */

            // The caller wants a GGEP block with "BH" and "MCAST"
            } else if (supportsBH && isMulticastResponse) {

                // Point the reference we'll return at the data of the GGEP block with those two extensions we cached
                retGGEPBlock = _comboGGEP;

            // Just "BH"
            } else if (supportsBH) {

                // Point the reference we'll return at the data of the GGEP block we cached
                retGGEPBlock = _bhGGEP;

            // Just "MCAST"
            } else if (isMulticastResponse) {

                // Point the reference we'll return at the data of the GGEP block we cached
                retGGEPBlock = _mcGGEP;
            }

            /*
             * Otherwise, retGGEPBlock will still point to _standardGGEP, which is an empty byte array
             */

            // Return the data of the GGEP block the caller requested
            return retGGEPBlock;
        }

        /**
         * Read the GGEP "PUSH" extension value, and parse it into an IpPortSet of IPPortCombo objects.
         * The "PUSH" value is a byte array of 6 byte chunks, each of which has an IP address followed by a port number.
         * 
         * @param  A GGEP block with the "PUSH" extension
         * @return A IpPortSet of IPPortCombo objects made from the IP addresses and port numbers from the "PUSH" extension value
         */
        public Set getPushProxies(GGEP ggep) {

            // We'll point proxies at the IpPortSet we make
            Set proxies = null;

            // The GGEP block has the "PUSH" extension, which lists the computer's push proxies
            if (ggep.hasKey(GGEP.GGEP_HEADER_PUSH_PROXY)) {

                try {

                    // Get the value, a byte array of 6 byte chunks, each of which has an IP address and port number
                    byte[] proxyBytes = ggep.getBytes(GGEP.GGEP_HEADER_PUSH_PROXY);

                    // Parse through the value
                    ByteArrayInputStream bais = new ByteArrayInputStream(proxyBytes); // Wrap a ByteArrayInputStream around it to keep track of what we've read
                    while (bais.available() > 0) {

                        // Read the next 6 byte chunk
                        byte[] combo = new byte[6];
                        if (bais.read(combo, 0, combo.length) == combo.length) { // If we got 6 bytes

                            try {

                                // Turn the 6 byte chunk into an IPPortCombo object, and add it to the proxies IpPortSet
                                if (proxies == null) proxies = new IpPortSet(); // An IpPortSet will keep the IPPortCombo objects sorted with no duplicates
                                proxies.add(new IPPortCombo(combo));

                            // Something wrong with this 6 byte chunk, just move on to the next one
                            } catch (BadPacketException malformedPair) {}
                        }
                    }

                // The GGEP block doesn't have a "PUSH" extension
                } catch (BadGGEPPropertyException bad) {}
            }

            // Return the IpPortSet of IPPortCombo objects we parsed from "PUSH", or an empty set if that extension isn't here
            if (proxies == null) return Collections.EMPTY_SET;
            else                 return proxies;
        }
    }

    /**
     * An IPPortCombo object keeps an IP address and port number together, and performs checks the QueryReply class needs.
     * 
     * Keep in mind that I very well could have used Endpoint here, but I
     * decided against it mainly so I could do validity checking.
     * This may be a bad decision. I'm sure someone will let me know during
     * code review.
     */
    public static class IPPortCombo implements IpPort {

        /** The port number. */
        private int _port;

        /** The IP address as a Java InetAddress object. */
        private InetAddress _addr;

        /** ":", a colon that appears in IP address and port number text like "1.2.3.4:5". */
        public static final String DELIM = ":";

        /**
         * Read 4 bytes of IP address and 2 bytes of port number and make a new IPPortCombo object.
         * 
         * @param  fromNetwork        A byte array with 6 bytes, the data of an IP address followed by the data of a port
         * @return                    A new IPPortCombo object with the IP address and port number
         * @throws BadPacketException The IP address or port number data wasn't valid
         */
        public static IPPortCombo getCombo(byte[] fromNetwork) throws BadPacketException {

            // Call the next constructor
            return new IPPortCombo(fromNetwork);
        }

        /**
         * Read 4 bytes of IP address and 2 bytes of port number and make a new IPPortCombo object.
         * 
         * @param  networkData        A byte array with 6 bytes, the data of an IP address followed by the data of a port
         * @return                    A new IPPortCombo object with the IP address and port number
         * @throws BadPacketException The IP address or port number data wasn't valid
         */
        private IPPortCombo(byte[] networkData) throws BadPacketException {

            // Make sure the given byte array is exactly 6 bytes long
            if (networkData.length != 6) throw new BadPacketException("Weird Input");

            // Read the IP address and port number
            String host = NetworkUtils.ip2string(networkData, 0); // Get the IP address as a String
            int port = ByteOrder.ushort2int(ByteOrder.leb2short(networkData, 4));

            // Make sure the port number isn't 0
            if (!NetworkUtils.isValidPort(port)) throw new BadPacketException("Bad Port: " + port);

            // Save the port number
            _port = port;

            try {

                // Save the IP address
                _addr = InetAddress.getByName(host);

            // InetAddress.getByName didn't like the host text, throw a BadPacketException
            } catch (UnknownHostException uhe) { throw new BadPacketException("bad host."); }

            // Make sure the address doesn't start 0 or 255
            if (!NetworkUtils.isValidAddress(_addr)) throw new BadPacketException("invalid addr: " + _addr);
        }

        /**
         * Make a new IPPortCombo object from given IP address text and a port number.
         * 
         * @param hostAddress An IP address in text like "1.2.3.4"
         * @param port        A port number
         * @return            A new IPPortCombo object with the IP address and port number
         */
        public IPPortCombo(String hostAddress, int port) throws UnknownHostException, IllegalArgumentException {

            // Make sure hte port number isn't 0 or too big to fit in 2 bytes
            if (!NetworkUtils.isValidPort(port)) throw new IllegalArgumentException("Bad Port: " + port);

            // Save the port number and IP address
            _port = port;
            _addr = InetAddress.getByName(hostAddress); // Turn it into a Java InetAddress object

            // Make sure the IP address doesn't start 0 or 255
            if (!NetworkUtils.isValidAddress(_addr)) throw new IllegalArgumentException("invalid addr: " + _addr);
        }

        /**
         * The port number in this IPPortCombo object.
         * The IpPort interface requires this method.
         * 
         * @return The port number
         */
        public int getPort() {

            // Return the port number
            return _port;
        }

        /**
         * The IP address in this IPPortCombo object.
         * The IpPort interface requires this method.
         * 
         * @return The IP address as a Java InetAddress object
         */
        public InetAddress getInetAddress() {

            // Return the InetAddress object with the IP address
            return _addr;
        }

        /**
         * The IP address in this IPPortCombo object.
         * The IpPort interface requires this method.
         * 
         * @return The IP address as a String like "1.2.3.4"
         */
        public String getAddress() {

            // Return the IP address as text like "1.2.3.4"
            return _addr.getHostAddress();
        }

        /**
         * Serialize the IP address and port number in this IPPortCombo object into 6 bytes of data.
         * The 4 byte IP address is first, followed by the 2 byte port number.
         * 
         * @return A byte array with 6 bytes
         */
        public byte[] toBytes() {

            /*
             * TODO if IPv6 kicks in, this may fail, don't worry so much now.
             */

            // Compose 6 bytes with the IP address and port number
            byte[] retVal = new byte[6];                                 // Make a new 6 byte long byte array that we'll return
            for (int i=0; i < 4; i++) retVal[i] = _addr.getAddress()[i]; // Copy the 4 bytes of the IP address into it
            ByteOrder.short2leb((short)_port, retVal, 4);                // At position 4, copy in the 2 bytes of the port number
            return retVal;                                               // Return it
        }

        /**
         * Compare this IPPortCombo object to another one.
         * 
         * @param o Another IPPortCombo object
         * @return  True if the IP address and port numbers are the same, false if they are different
         */
        public boolean equals(Object other) {

            // Only compare them if they are both IPPortCombo objects
            if (other instanceof IPPortCombo) {

                // Return true if the IP addresses and port numbers match
                IPPortCombo combo = (IPPortCombo)other;
                return _addr.equals(combo._addr) && (_port == combo._port);
            }

            // Different
            return false;
        }

        /**
         * Hash the information in this object into a number.
         * 
         * overridden to fulfill contract with equals for hash-based
         * collections
         * 
         * @return The hash code
         */
        public int hashCode() {

            // Hash the IP address text, and multiply that with the port number
            return _addr.hashCode() * _port;
        }

        /**
         * Express this IPPortCombo object as text.
         * 
         * @return A String like "1.2.3.4:5"
         */
        public String toString() {

            // Compose text like "1.2.3.4:5"
            return getAddress() + ":" + getPort();
        }
    }
}
