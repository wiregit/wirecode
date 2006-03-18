
// Commented for the Learning branch

package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;

/**
 * The Gnutella message format allows for Gnutella software vendors to create their own custom messages.
 * LimeWire, BearShare, and Gtk-Gnutella created vendor messages that LimeWire understands and uses.
 * On the Gnutella network, vendor messages are never forwarded.
 * 
 * This class is marked abstract because it provides methods for the classes that extend it.
 * Classes for specific vendor messages like HopsFlowVendorMessage and HeadPing do this.
 * Making a VendorMessage object doesn't make any sense.
 * 
 * A Gnutella vendor message looks like this:
 * 
 * gnutella header 23 bytes
 * LIME
 * ss
 * vv
 * payload
 * 
 * Vendor messages start with the same 23-byte Gnutella message header as regular messages like pings and pongs do.
 * In the Gnutella message header, the type byte is 0x31, indicating a vendor-specific message.
 * The first 8 bytes of the payload tell what kind of vendor message this is.
 * The first 4 bytes are like "LIME", and name the vendor that created the message.
 * The 2 bytes ss hold a number that identifies what kind of "LIME" vendor message this is.
 * The 2 bytes vv hold a version number, letting a vendor change the format of a vendor message by making a second version.
 * 
 * Here are the vendor messages that LimeWire uses:
 * 
 * Capabilities
 * 
 *  0000   0 F_MESSAGES_SUPPORTED     MessagesSupportedVendorMessage
 *  0000  10 F_CAPABILITIES           CapabilitiesVM
 * 
 * Hops Flow
 * 
 * "BEAR"  4 F_HOPS_FLOW              HopsFlowVendorMessage
 * 
 * Connect Back
 * 
 * "BEAR"  7 F_TCP_CONNECT_BACK       TCPConnectBackVendorMessage
 * "LIME"  7 F_TCP_CONNECT_BACK       TCPConnectBackRedirect
 * "GTKG"  8 F_UDP_CONNECT_BACK       UDPConnectBackVendorMessage
 * "LIME"  8 F_UDP_CONNECT_BACK_REDIR UDPConnectBackRedirect
 * 
 * Search
 * 
 * "LIME" 11 F_LIME_ACK               LimeACKVendorMessage
 * "BEAR" 11 F_LIME_ACK               QueryStatusRequest
 * "LIME" 12 F_REPLY_NUMBER           ReplyNumberVendorMessage
 * "BEAR" 12 F_REPLY_NUMBER           QueryStatusResponse
 * 
 * Download
 * 
 * "LIME" 21 F_PUSH_PROXY_REQ         PushProxyRequest
 * "LIME" 22 F_PUSH_PROXY_ACK         PushProxyAcknowledgement
 * "LIME" 23 F_UDP_HEAD_PING          HeadPing
 * "LIME" 24 F_UDP_HEAD_PONG          HeadPong
 * 
 * Update
 * 
 * "LIME" 25 F_HEADER_UPDATE          HeaderUpdateVendorMessage, not made here
 * "LIME" 26 F_UPDATE_REQ             UpdateRequest
 * "LIME" 27 F_UPDATE_RESP            UpdateResponse
 * 
 * Statistics and Control
 * 
 * "LIME"  5 F_GIVE_ULTRAPEER         UDPCrawlerPing
 * "LIME"  6 F_ULTRAPEER_LIST         UDPCrawlerPong
 * "LIME" 14 F_GIVE_STATS             GiveStatsVendorMessage, also needs the Internet protocol
 * "LIME" 15 F_STATISTICS             StatisticVendorMessage
 * "LIME" 16 F_SIMPP_REQ              SimppRequestVM
 * "LIME" 17 F_SIMPP                  SimppVM
 */
public abstract class VendorMessage extends Message {

    /*
     * Functional IDs defined by Gnutella VendorMessage protocol....
     */

    /** 0, Messages Supported vendor message code. */
    protected static final int F_MESSAGES_SUPPORTED = 0;
    /** 4, Hops Flow vendor message code. */
    protected static final int F_HOPS_FLOW = 4;
    /** 7, TCP Connect Back vendor message code. */
    protected static final int F_TCP_CONNECT_BACK = 7;
    /** 7, UDP Connect Back vendor message code. */
    protected static final int F_UDP_CONNECT_BACK = 7;
    /** 8, UDP Connect Back Redirect vendor message code. */
    protected static final int F_UDP_CONNECT_BACK_REDIR = 8;
    /** 10, Capabilities vendor message code. */
    protected static final int F_CAPABILITIES = 10;
    /** 11, Lime Ack vendor message code. */
    protected static final int F_LIME_ACK = 11;
    /** 12, Reply Number vendor message code. */
    protected static final int F_REPLY_NUMBER = 12;
    /** 21, Push Proxy Request vendor message code. */
    protected static final int F_PUSH_PROXY_REQ = 21;
    /** 22, Push Proxy ACK vendor message code. */
    protected static final int F_PUSH_PROXY_ACK = 22;
    /** 14, Give Statistics vendor message code. */
    protected static final int F_GIVE_STATS = 14;
    /** 15, Statistics vendor message code. */
    protected static final int F_STATISTICS = 15;
    /** 5, Give Ultrapeer vendor message code. */
    protected static final int F_GIVE_ULTRAPEER = 5;
    /** 6, Ultrapeer List vendor message code. */
    protected static final int F_ULTRAPEER_LIST = 6;
    /** 16, SIMPP Request vendor message code. */
    protected static final int F_SIMPP_REQ = 16;
    /** 17, SIMPP vendor message code. */
    protected static final int F_SIMPP = 17;
    /** 23, UDP Head Ping vendor message code. */
    protected static final int F_UDP_HEAD_PING = 23;
    /** 24, UDP Head Pong vendor message code. */
    protected static final int F_UDP_HEAD_PONG = 24;
    /** 25, Header Update vendor message code. */
    protected static final int F_HEADER_UPDATE = 25;
    /** 26, Update Request vendor message code. */
    protected static final int F_UPDATE_REQ = 26;
    /** 27, Update Response vendor message code. */
    protected static final int F_UPDATE_RESP = 27;

    /** "LIME", the vendor code for LimeWire as an array of 4 ASCII bytes. */
    protected static final byte[] F_LIME_VENDOR_ID = {(byte)76, (byte)73, (byte)77, (byte)69};
    /** "BEAR", the vendor code for BearShare as an array of 4 ASCII bytes. */
    protected static final byte[] F_BEAR_VENDOR_ID = {(byte)66, (byte)69, (byte)65, (byte)82};
    /** "GTKG", the vendor code for Gtk-Gnutella as an array of 4 ASCII bytes. */
    protected static final byte[] F_GTKG_VENDOR_ID = {(byte)71, (byte)84, (byte)75, (byte)71};
    /** A byte array of 4 0s to use in place of a vendor code like "LIME". */
    protected static final byte[] F_NULL_VENDOR_ID = {(byte)0, (byte)0, (byte)0, (byte)0};

    /** 8 bytes, the size of the start of the vendor message payload like LIMEttvv. */
    private static final int LENGTH_MINUS_PAYLOAD = 8;

    /** A BadPacketException made with the String "Unrecognized Vendor Message", cached and static for quick use. */
    private static final BadPacketException UNRECOGNIZED_EXCEPTION = new BadPacketException("Unrecognized Vendor Message");

    /*
     * A vendor message looks like this:
     * 
     * gnutella packet header 23 bytes  Message object
     * LIME                             _vendorID
     * ss                               _selector
     * vv                               _version
     * message type specific payload    _payload
     * 
     * Vendor messages start with the same 23-byte header packets like Ping and Pong do.
     * After that is the vendor message payload.
     * The first 8 bytes are the same for all vendor messages.
     * LIME is the vendor code in 4 ASCII characters.
     * ss is the vendor message code that tells what kind of LIME vendor message this is.
     * vv is a version number that distinguishes different versions that have a different structure.
     */

    /** The first 4 bytes of the vendor message payload, containing the vendor code like "LIME". */
    private final byte[] _vendorID;

    /** The vendor message type number, 2 bytes located 4 bytes into the vendor message payload. */
    private final int _selector;

    /** The vendor message type version, 2 bytes located 6 bytes into the vendor message payload. */
    private final int _version;

    /** The payload of this vendor message, beyond the 23-byte Gnutella packet header and 8-byte LIMEssvv type information. */
    private final byte[] _payload;

    /** Cache the hash code to only have to compute it once. */
    private final int _hashCode;

    /*
     * ----------------------------------
     *  CONSTRUCTORS
     * ----------------------------------
     */

    /**
     * Make a new VendorMessage object with the given information.
     * Leads to the message maker.
     * 
     * @param vendorIDBytes The 4 byte vendor code of this message, like "LIME"
     * @param selector      The type number
     * @param version       The type version
     * @param payload       The payload of this vendor message, beyond the 23-byte Gnutella packet header and 8-byte LIMEssvv information
     */
    protected VendorMessage(byte[] vendorIDBytes, int selector, int version, byte[] payload) {

        // Call the next constructor with Message.N_UNKNOWN because we don't know if we'll send the message through TCP or UDP
        this(vendorIDBytes, selector, version, payload, Message.N_UNKNOWN);
    }

    /**
     * Make a new VendorMessage object with the given information.
     * This is the message maker.
     * 
     * Sets the TTL to 1.
     * Vendor messages can only travel 1 hop, and aren't forwarded.
     * 
     * @param vendorIDBytes The 4 byte vendor code of this message, like "LIME"
     * @param selector      The type number
     * @param version       The type version
     * @param payload       The payload of this vendor message, beyond the 23-byte Gnutella packet header and 8-byte LIMEssvv information
     * @param network       The Internet protocol we'll send this vendor message on, like Message.N_TCP or Message.N_UDP
     */
    protected VendorMessage(byte[] vendorIDBytes, int selector, int version, byte[] payload, int network) {

        // Save the message header data in the Message object this VendorMessage extends
        super(
            F_VENDOR_MESSAGE,                      // 0x31, the byte that identifies this as a vendor message instead of a ping or pong
            (byte)1,                               // The number of times this vendor message will be able to travel between ultrapeers
            LENGTH_MINUS_PAYLOAD + payload.length, // The payload length, 8 bytes for LIMEssvv, followed by the vendor message type-specific payload data
            network);                              // The Internet protocol we'll use to send this packet, like Message.N_TCP or Message.N_UDP

        // Make sure the vendor ID is 4 bytes like "LIME", and the vendor message type number and version fit into 2 bytes each
        if ((vendorIDBytes.length   != 4)) throw new IllegalArgumentException("wrong vendorID length: " + vendorIDBytes.length);
        if ((selector & 0xFFFF0000) != 0)  throw new IllegalArgumentException("invalid selector: " + selector);
        if ((version  & 0xFFFF0000) != 0)  throw new IllegalArgumentException("invalid version: " + version);

        // Save the given information in this object
        _vendorID = vendorIDBytes;
        _selector = selector;
        _version  = version;
        _payload  = payload;

        // Compute and save the hash code
        _hashCode = computeHashCode(_version, _selector, _vendorID, _payload);
    }

    /**
     * Make a new VendorMessage object with data we read from the network.
     * Leads to the message parser.
     * 
     * @param guid     The message GUID read from the message header data
     * @param ttl      The TTL read from the message header data
     * @param hops     The hops count read from the message header data
     * @param vendorID The vendor ID like "LIME" read from the first 4 bytes of the payload
     * @param selector The vendor message type number read from the next 2 bytes of the payload
     * @param version  The vendor message type version read from the next 2 bytes of the payload
     * @param payload  The vendor message type specific payload after that
     */
    protected VendorMessage(byte[] guid, byte ttl, byte hops, byte[] vendorID, int selector, int version, byte[] payload) throws BadPacketException {

        // Call the next constructor with Message.N_UNKNOWN because we don't know if we got the message through TCP or UDP
        this(guid, ttl, hops, vendorID, selector, version, payload, Message.N_UNKNOWN);
    }

    /**
     * Make a new VendorMessage object with data we read from the network.
     * This is the message parser.
     * 
     * Primarily built for the convenience of the class Message.
     * Subclasses must extend this (or the above constructor that doesn't
     * takes a network parameter) and use getPayload() to parse the payload
     * and do anything else they need to.
     * 
     * @param guid     The message GUID read from the message header data
     * @param ttl      The TTL read from the message header data
     * @param hops     The hops count read from the message header data
     * @param vendorID The vendor ID like "LIME" read from the first 4 bytes of the payload
     * @param selector The vendor message type number read from the next 2 bytes of the payload
     * @param version  The vendor message type version read from the next 2 bytes of the payload
     * @param payload  The vendor message type specific payload after that
     * @param network  What Internet protocol brought us this data, like Message.N_TCP or Message.N_UDP
     */
    protected VendorMessage(byte[] guid, byte ttl, byte hops, byte[] vendorID, int selector, int version, byte[] payload, int network) throws BadPacketException {

        // Save the message header data in the Message object this VendorMessage extends
        super(
            guid,                                  // The GUID we read from the header data
            (byte)0x31,                            // The byte that identifies this as a vendor message instead of a ping or pong, Message.F_VENDOR_MESSAGE
            ttl,                                   // The TTL we read from the header data
            hops,                                  // The hops count we read from the header data
            LENGTH_MINUS_PAYLOAD + payload.length, // The payload length, 8 bytes for LIMEssvv, followed by the vendor message type-specific payload data
            network);                              // Save the source Internet protocol in the Message object, this isn't a part of the packet when sent

        // Make sure the given vendor ID is 4 characters like "LIME"
        if ((vendorID.length != 4)) {
            ReceivedErrorStat.VENDOR_INVALID_ID.incrementStat();
            throw new BadPacketException("Vendor ID Invalid!");
        }

        // Make sure the given vendor message type number fits in 2 bytes
        if ((selector & 0xFFFF0000) != 0) {
            ReceivedErrorStat.VENDOR_INVALID_SELECTOR.incrementStat();
            throw new BadPacketException("Selector Invalid!");
        }

        // Make sure the given vendor message type version fits in 2 bytes
        if ((version & 0xFFFF0000) != 0) {
            ReceivedErrorStat.VENDOR_INVALID_VERSION.incrementStat();
            throw new BadPacketException("Version Invalid!");
        }

        // Save the given values in this object
        _vendorID = vendorID;
        _selector = selector;
        _version = version;
        _payload = payload;

        // Compute and save the hash code
        _hashCode = computeHashCode(_version, _selector, _vendorID, _payload);
    }

    /**
     * Compute a hash code number from the given information.
     * This is the number we'll save as the hash code of this VendorMessage object.
     * 
     * @param version  The type version
     * @param selector The type number
     * @param vendorID The 4 byte vendor code of this message, like "LIME"
     * @param payload  The payload of this vendor message, beyond the 23-byte Gnutella packet header and 8-byte LIMEssvv information
     */
    private static int computeHashCode(int version, int selector, byte[] vendorID, byte[] payload) {

        // Compute and return the hash code
        int hashCode = 0;
        hashCode += 17 * version;
        hashCode += 17 * selector;
        for (int i = 0; i < vendorID.length; i++) hashCode += (int)17 * vendorID[i];
        for (int i = 0; i < payload.length;  i++) hashCode += (int)17 * payload[i];
        return hashCode;
    }

    /*
     * ----------------------------------
     *  ACCESSOR methods
     * ----------------------------------
     */

    /**
     * Get the payload of this vendor message.
     * This is the data beyond the 23-byte Gnutella packet header and 8-byte LIMEssvv type information.
     * 
     * Allows subclasses to make changes gain access to the payload.  They
     * can:
     * 1) change the contents
     * 2) parse the contents.
     * In general, 1) is discouraged, 2) is necessary.  Subclasses CANNOT
     * re-init the payload.
     */
    protected byte[] getPayload() {

        // Return the payload
        return _payload;
    }

    /**
     * Get the vendor messsage type version number.
     * 
     * 3 pieces of information identify what type of vendor message this is:
     * The vendor ID of the company that invented the message, like "LIME".
     * The number they assigned their vendor message, like 4 for Hops Flow.
     * The version number of their message, like 1, we haven't needed to change this message yet.
     * This method returns the version number.
     * 
     * @return The version number
     */
    protected int getVersion() {

        // Return the version number
        return _version;
    }

    /*
     * ----------------------------------
     *  Methods for all subclasses....
     * ----------------------------------
     */

    /**
     * Make a new object that extends VendorMessage and is specific to the type of vendor message we read, like a HopsFlowVendorMessage or a HeadPing.
     * Message.createMessage() calls this.
     * It's read a Gnutella packet header, and found 0x31 as the type.
     * 
     * @param guid        The message GUID from the Gnutella packet header
     * @param ttl         The TTL from the header
     * @param hops        The hops count from the header
     * @param fromNetwork The Gnutella packet payload
     * @param network     The Internet protocol we got the data from, like Message.N_TCP or Message.N_UDP
     * @return            A new object that represents the vendor message and extends VendorMessage of a specific type, like a HopsFlowVendorMessage
     */
    public static VendorMessage deriveVendorMessage(byte[] guid, byte ttl, byte hops, byte[] fromNetwork, int network) throws BadPacketException {

        // Make sure the payload is at least 8 bytes for the LIMEssvv type information
        if (fromNetwork.length < LENGTH_MINUS_PAYLOAD) {
            ReceivedErrorStat.VENDOR_INVALID_PAYLOAD.incrementStat();
            throw new BadPacketException("Not enough bytes for a VM!!");
        }

        // Read the first 8 bytes like LIMEssvv
        ByteArrayInputStream bais = new ByteArrayInputStream(fromNetwork); // Wrap a ByteArrayInputStream around the given byte array which will keep track of the index we're on
        byte[] vendorID = null, restOf = null; // Variables for the values we'll read
        int selector = -1, version = -1;
        try {

            // Read the first 4 bytes, the vendor ID like "LIME" which tells what company defined this vendor message
            vendorID = new byte[4];
            bais.read(vendorID, 0, vendorID.length);

            // Read the next 2 bytes, the vendor message type number, like 4 for Hops Flow
            selector = ByteOrder.ushort2int(ByteOrder.leb2short(bais));

            // Read the next 2 bytes, the version of that vendor message, like 1 if the company hasn't needed to change the message yet
            version = ByteOrder.ushort2int(ByteOrder.leb2short(bais));

            // Read the payload beyond those 8 bytes, which has data specific to the kind of vendor message it is
            restOf = new byte[bais.available()];
            bais.read(restOf, 0, restOf.length);

        // There was an error reading from the ByteArrayInputStream
        } catch (IOException ioe) { ErrorService.error(ioe); }

        // Make and return a new object that extends VendorMessage and is specific to the type of vendor message we read, like a HopsFlowVendorMessage or a HeadPing
        if ((selector == F_HOPS_FLOW)              && (Arrays.equals(vendorID, F_BEAR_VENDOR_ID))) return new HopsFlowVendorMessage(guid, ttl, hops, version, restOf);           // BEAR  4 Hops Flow
        if ((selector == F_LIME_ACK)               && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new LimeACKVendorMessage(guid, ttl, hops, version, restOf);            // LIME 11 Lime Ack
        if ((selector == F_REPLY_NUMBER)           && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new ReplyNumberVendorMessage(guid, ttl, hops, version, restOf);        // LIME 12 Reply Number
        if ((selector == F_TCP_CONNECT_BACK)       && (Arrays.equals(vendorID, F_BEAR_VENDOR_ID))) return new TCPConnectBackVendorMessage(guid, ttl, hops, version, restOf);     // BEAR  7 TCP Connect Back
        if ((selector == F_MESSAGES_SUPPORTED)     && (Arrays.equals(vendorID, F_NULL_VENDOR_ID))) return new MessagesSupportedVendorMessage(guid, ttl, hops, version, restOf);  // 0000  0 Messages Supported
        if ((selector == F_UDP_CONNECT_BACK)       && (Arrays.equals(vendorID, F_GTKG_VENDOR_ID))) return new UDPConnectBackVendorMessage(guid, ttl, hops, version, restOf);     // GTKG  7 UDP Connect Back
        if ((selector == F_PUSH_PROXY_REQ)         && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new PushProxyRequest(guid, ttl, hops, version, restOf);                // LIME 21 Push Proxy Request
        if ((selector == F_PUSH_PROXY_ACK)         && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new PushProxyAcknowledgement(guid, ttl, hops, version, restOf);        // LIME 22 Push Proxy Acknowledgement
        if ((selector == F_LIME_ACK)               && (Arrays.equals(vendorID, F_BEAR_VENDOR_ID))) return new QueryStatusRequest(guid, ttl, hops, version, restOf);              // BEAR 11 Query Status Request, not used
        if ((selector == F_REPLY_NUMBER)           && (Arrays.equals(vendorID, F_BEAR_VENDOR_ID))) return new QueryStatusResponse(guid, ttl, hops, version, restOf);             // BEAR 12 Query Status Response
        if ((selector == F_TCP_CONNECT_BACK)       && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new TCPConnectBackRedirect(guid, ttl, hops, version, restOf);          // LIME  7 TCP Connect Back Redirect
        if ((selector == F_UDP_CONNECT_BACK_REDIR) && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new UDPConnectBackRedirect(guid, ttl, hops, version, restOf);          // LIME  8 UDP Connect Back Redirect
        if ((selector == F_CAPABILITIES)           && (Arrays.equals(vendorID, F_NULL_VENDOR_ID))) return new CapabilitiesVM(guid, ttl, hops, version, restOf);                  // 0000 10 Capabilities
        if ((selector == F_GIVE_STATS)             && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new GiveStatsVendorMessage(guid, ttl, hops, version, restOf, network); // LIME 14 Give Statistics
        if ((selector == F_STATISTICS)             && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new StatisticVendorMessage(guid, ttl, hops, version, restOf);          // LIME 15 Statistic
        if ((selector == F_SIMPP_REQ)              && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new SimppRequestVM(guid, ttl, hops, version, restOf);                  // LIME 16 SIMPP Request
        if ((selector == F_SIMPP)                  && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new SimppVM(guid, ttl, hops, version, restOf);                         // LIME 17 SIMPP
        if ((selector == F_GIVE_ULTRAPEER)         && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new UDPCrawlerPing(guid, ttl, hops, version, restOf);                  // LIME  5 UDP Crawler Ping
        if ((selector == F_ULTRAPEER_LIST)         && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new UDPCrawlerPong(guid, ttl, hops, version, restOf);                  // LIME  6 UDP Crawler Pong
        if ((selector == F_UDP_HEAD_PING)          && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new HeadPing(guid, ttl, hops, version, restOf);                        // LIME 23 Head Ping
        if ((selector == F_UDP_HEAD_PONG)          && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new HeadPong(guid, ttl, hops, version, restOf);                        // LIME 24 Head Pong
        if ((selector == F_UPDATE_REQ)             && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new UpdateRequest(guid, ttl, hops, version, restOf);                   // LIME 26 Update Request
        if ((selector == F_UPDATE_RESP)            && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new UpdateResponse(guid, ttl, hops, version, restOf);                  // LIME 27 Update Response
        if ((selector == F_HEADER_UPDATE)          && (Arrays.equals(vendorID, F_LIME_VENDOR_ID))) return new HeaderUpdateVendorMessage(guid, ttl, hops, version, restOf);       // LIME 25 Header Update

        /*
         * TODO:kfaaborg Remove BEAR 11 1 QueryStatusRequest, which is not used.
         */

        // We read a vendor type number we didn't expect
        ReceivedErrorStat.VENDOR_UNRECOGNIZED.incrementStat();
        throw UNRECOGNIZED_EXCEPTION;
    }

    /**
     * Determine if a given VendorMessage object is the same as this one.
     * Compares the vendor IDs, type numbers, and payload data.
     * Doesn't compare the version numbers.
     * 
     * @param other Another VendorMessage object
     * @return      True if it's the same as this one, false if it's different
     */
    public boolean equals(Object other) {

        // Only compare them if the given object is also a VendorMessage
        if (other instanceof VendorMessage) {

            // Compare the vendor IDs, type numbers, and payload data, but not the version numbers
            VendorMessage vmp = (VendorMessage)other;
            return
                ((_selector == vmp._selector)             && // They are the same type of vendor message, and
                (Arrays.equals(_vendorID, vmp._vendorID)) && // The vendor IDs like "LIME" match, and
                (Arrays.equals(_payload, vmp._payload)));    // The payload data is exactly the same
        }

        // Different
        return false;
    }

    /**
     * Get the hash code of this VendorMessage object.
     * We computed this number from the type information like LIMEssvv, as well as the data of the payload.
     * 
     * @return The hash code
     */
    public int hashCode() {

        // Return the hash code that we generated
        return _hashCode;
    }

    /*
     * ----------------------------------
     *  ABSTRACT METHODS
     *  FULFILL abstract Message methods
     * ----------------------------------
     */

    /**
     * Write the vendor packet payload to the given OutputStream.
     * Does not flush the OutputStream.
     * 
     * @param out An OutputStream we can call write() on to send data to the remote computer
     */
    protected void writePayload(OutputStream out) throws IOException {

        /*
         * A vendor message payload looks like this:
         * 
         * LIME
         * ss
         * vv
         * (payload)
         * 
         * The first 4 bytes are the vendor code, like "LIME" or "BEAR".
         * ss keeps the number that identifies what kind of vendor message this is.
         * vv is the version number.
         * After that is the payload, which is different for different kinds of vendor messages.
         */

        // Write the vendor message payload to the given OutputStream
        out.write(_vendorID);                       // Write the 4 byte vendor code, like "LIME"
        ByteOrder.short2leb((short)_selector, out); // Write the vendor message type number in 2 bytes
        ByteOrder.short2leb((short)_version, out);  // Write the version number of the message in 2 bytes
        out.write(getPayload());                    // Write the payload
    }

    /**
     * Doesn't change this object, and returns a reference to it.
     * Call stripExtendedPayload() to return a Message object like this one, but without a GGEP extension block.
     * 
     * @return A reference to this same object, unchanged
     */
    public Message stripExtendedPayload() {

        /*
         * doesn't make sense for VendorMessage to strip anything....
         */

        // Return the this reference
        return this;
    }

    /**
     * Does nothing.
     * Call recordDrop() to record that we're dropping this Gnutella packet in the program's statistics.
     */
    public void recordDrop() {}
}
