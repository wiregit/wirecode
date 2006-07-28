
// Commented for the Learning branch

package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Iterator;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;
import com.limegroup.gnutella.udpconnect.UDPConnectionMessage;
import com.limegroup.gnutella.util.DataUtils;

/**
 * A Message object represents a Gnutella packet.
 * 
 * After the handshake, two Gnutella computers communicate by exchanging Gnutella packets.
 * The packets are sent one after another in the data stream of a TCP socket connection.
 * Gnutella packets can also be put inside UDP packets, and transmitted that way.
 * 
 * There are 2 kinds of packets, requests and replies.
 * Request packets are like ping and search.
 * Reply packets are like pong and hit.
 * 
 * A Gnutella packet consists of a 23-byte header.
 * After that, some packets have a payload which can be any length.
 * The header looks like this:
 * 
 * guid      At  0, length 16, the globally unique identifier of this packet.
 * function  At 16, the byte that identifies what kind of packet this is, like ping or pong.
 * ttl       At 17, the number of hops this packet can travel across the Internet from here.
 * hops      At 18, the number of hops this packet has traveled across the Internet to get here.
 * length    At 19, length 4, for a total size 23 bytes, the length of the packet payload.
 * 
 * The bytes of the length are in little endian order.
 * This means that the first byte, the one at 19, is the least significant one.
 * 
 * This class is marked abstract.
 * This means that you can't make a Message object.
 * Instead, LimeWire has a lot of classes that extend Message, like PingRequest.
 * You can make a PingRequest object, and it will use the code here.
 */
public abstract class Message implements Serializable, Comparable {

    /*
     * Functional IDs defined by Gnutella protocol.
     */

    /*
     * A byte at the start of a Gnutella message tells what kind of message it is.
     * Here are the byte values, and the kinds of messages they identify.
     */

    /** 0x00, ping. */
    public static final byte F_PING                  = (byte)0x0;

    /** 0x01, pong. */
    public static final byte F_PING_REPLY            = (byte)0x1;

    /** 0x40, push. */
    public static final byte F_PUSH                  = (byte)0x40;

    /** 0x80, query. */
    public static final byte F_QUERY                 = (byte)0x80;

    /** 0x81, query reply. */
    public static final byte F_QUERY_REPLY           = (byte)0x81;

    /** 0x30, a QRP message. */
    public static final byte F_ROUTE_TABLE_UPDATE    = (byte)0x30;

    /** 0x31, vendor message. */
    public static final byte F_VENDOR_MESSAGE        = (byte)0x31;

    /** 0x32, stable vendor message, not commonly used. */
    public static final byte F_VENDOR_MESSAGE_STABLE = (byte)0x32;

    /**
     * 0x41, UDP connection message.
     * This Gnutella message is a part of LimeWire's firewall-to-firewall file transfer feature.
     * The AckMessage, DataMessage, FinMessage, KeepAliveMessage, and SynMessage packets are have 0x41 16 bytes into the packet.
     * These classes extend UDPConnectionMessage, which extends Message.
     */
	public static final byte F_UDP_CONNECTION = (byte)0x41;

    /*
     * There are 3 ways LimeWire communicates:
     * TCP socket connections on the Internet.
     * UDP packets on the Internet.
     * UDP packets sent with multicast on the LAN.
     */

    /** -1, network unknown, we don't know if it's TCP, UDP, or multicast UDP on the LAN. */
    public static final int N_UNKNOWN = -1;
    
    /** 1, TCP. */
    public static final int N_TCP = 1;
    
    /** 2, UDP. */
    public static final int N_UDP = 2;
    
    /** 3, Multicast UDP on the LAN. (do) */
    public static final int N_MULTICAST = 3;

    /**
     * 3, the soft max TTL.
     * 
     * This a maximum limit for hops + TTL.
     * We impose this limit on the Gnutella packets we receive.
     * If we get a packet with hops + TTL larger than this limit, we'll lower the TTL to make it compliant with this limit.
     */
    public static final byte SOFT_MAX = ConnectionSettings.SOFT_MAX.getValue();

    /**
     * Generate the bytes of a new unique LimeWire GUID.
     * Makes an array of 16 bytes, fills it with random data, sets the last byte to 0, and applies LimeWire's marking at 9.
     * 
     * @return A byte array of 16 bytes holding a new LimeWire GUID value
     */
    public static byte[] makeGuid() {

        // Make a LimeWire GUID and return it in a byte array
        return GUID.makeGuid();
    }

    /*
     * ////////////////////////// Instance Data //////////////////////
     */

    /**
     * Message GUID.
     * This is the GUID that uniquely identifies this Gnutella pakcet on the Gnutella network.
     */
    private byte[] guid;

    /**
     * Message type.
     * A value like F_PING or F_QUERY_REPLY.
     * The value of the byte in the Gnutella message that identifies the message type.
     * This is also called the function.
     */
    private final byte func;

    /*
     * We do not support TTLs > 2^7, nor do we support packets
     * of length > 2^31
     */

    /**
     * The TTL, time to live.
     * This is the number of times this message can travel across the Internet from one computer to another.
     */
    private byte ttl;

    /**
     * The hops count.
     * This is the number of times this message has traveled across the Internet from one computer to another.
     */
    private byte hops;

    /**
     * The length of the message payload, in bytes.
     * The Gnutella message header contains this length.
     * After the header, the payload will be this long.
     */
    private int length;

    /**
     * The priority of sending this packet.
     * Lower numbers mean higher priority.
     * 
     * We use this for flow control.
     * The flow control algorithm will discard low priority packets to send high priority ones instead.
     * 
     * The program sets and uses this priority value.
     * It's not part of the packet as it travels on the Internet.
     * 
     * Initialized to 0.
     */
    private int priority = 0;

    /**
     * The time we made this Gnutella message.
     * This time is not written to the network.
     * 
     * Initialized to now, the time a new Message object is created.
     */
    private final long creationTime = System.currentTimeMillis();

    /**
     * The Internet protocol this Gnutella message uses.
     * The network that this was received on or is going to be sent to.
     * 
     * Initialized to 0.
     * Set to -1 N_UNKNOWN, 1 N_TCP, 2 N_UDP, or 3 N_MULTICAST.
     */
    private final int network;

    /**
     * Call repOk() to make sure that the things that should always be true about a Message object are.
     * Checks invariants.
     * Use for testing.
     */
    protected void repOk() {

        // Make sure the byte array for the GUID is 16 bytes long
        Assert.that(guid.length == 16);

        // Make sure that the func byte is set to a valid value that identifies one of the types of Gnutella messages
        Assert.that(
            func == F_PING ||
            func == F_PING_REPLY ||
            func == F_PUSH ||
            func == F_QUERY ||
            func == F_QUERY_REPLY ||
            func == F_VENDOR_MESSAGE ||
            func == F_VENDOR_MESSAGE_STABLE,
            "Bad function code");

        // If this is a push message, make sure it's payload is exactly 26 byts long
        if (func == F_PUSH) Assert.that(length == 26, "Bad push length: " + length);

        // Make sure that the TTL and hops counts aren't negative
        Assert.that(ttl >= 0, "Negative TTL: " + ttl);
        Assert.that(hops >= 0, "Negative hops: " + hops);

        // Make sure the payload length isn't negative
        Assert.that(length >= 0, "Negative length: " + length);
    }

    /*
     * ////////////////////// Constructors and Producers /////////////////
     */

    /**
     * Make a new Message object that represents a Gnutella packet.
     * Makes a new GUID for it, and starts its hops count at 0.
     * 
     * @param func   The byte that identifies the message type, like F_PING for a ping packet
     * @param ttl    The message TTL, the number of times it will travel across the Internet
     * @param length The payload length, the number of bytes in the message beyond the Gnutella message header
     */
    protected Message(byte func, byte ttl, int length) {

        // Call the next constructor, passing it -1 N_UNKNOWN because we don't know if we're going to us TCP or UDP yet
        this(func, ttl, length, N_UNKNOWN);
    }
    
    /**
     * Make a new Message object that represents a Gnutella packet.
     * Makes a new GUID for it, and starts its hops count at 0.
     * 
     * @param func    The byte that identifies the message type, like F_PING for a ping packet
     * @param ttl     The message TTL, the number of times it will travel across the Internet
     * @param length  The payload length, the number of bytes in the message beyond the Gnutella message header
     * @param network The Internet protocol the message travels over, like 1 N_TCP or 2 N_UDP
     */
    protected Message(byte func, byte ttl, int length, int network) {

        // Call the next constructor
        this(
            makeGuid(), // Make a new LimeWire GUID for this Gnutella packet
            func,       // Save the given message type byte, like 0x00 for a ping message
            ttl,        // Save the given TTL
            (byte)0,    // 0 hops, this message hasn't traveled across the Internet yet
            length,     // Save the given payload length
            network);   // Save the given Internet protocol, like 1 N_TCP if the message travels over a TCP socket connection
    }

    /**
     * Make a new Message object to hold information from a Gnutella packet we just received.
     * Use this constructor when reading packets from the network.
     * 
     * @param guid    The GUID that uniquely identifies this Gnutella message
     * @param func    The byte that identifies the message type, like F_PING for a ping packet
     * @param ttl     The message TTL, the number of times it will travel across the Internet
     * @param hops    The number of times this Gnutella message has traveled across the Internet
     * @param length  The payload length, the number of bytes in the message beyond the Gnutella message header
     */
    protected Message(byte[] guid, byte func, byte ttl, byte hops, int length) {

        // Make the new Message object with the given information
        this(guid, func, ttl, hops, length, N_UNKNOWN); // Network unknown, we don't know if we got this through TCP or UDP yet
    }

    /**
     * Make a new Message object that represents a Gnutella packet.
     * 
     * @param guid    The GUID that uniquely identifies this Gnutella message
     * @param func    The byte that identifies the message type, like F_PING for a ping packet
     * @param ttl     The message TTL, the number of times it will travel across the Internet
     * @param hops    The number of times this Gnutella message has traveled across the Internet
     * @param length  The payload length, the number of bytes in the message beyond the Gnutella message header
     * @param network The Internet protocol that brought us this message, like 1 N_TCP or 2 N_UDP
     */
    protected Message(byte[] guid, byte func, byte ttl, byte hops, int length, int network) {

        // Make sure the given GUID has 16 bytes
		if (guid.length != 16) throw new IllegalArgumentException("invalid guid length: " + guid.length);

        // Save the given information in this new object
        this.guid    = guid;
        this.func    = func;
        this.ttl     = ttl;
        this.hops    = hops;
        this.length  = length;
        this.network = network;
    }

    /**
     * Read one Gnutella packet from a given InputStream, and return a packet object like a PingReply that represents it.
     * 
     * @param  in                 The InputStream object we can call read() on to get data from the remote computer.
     * @return                    A new object that represents the packet.
     *                            If this reads a ping packet, returns a PingRequest object, if it reads a pong, returns a PingResponse.
     *                            There is no such thing as a Message object, but the object it returns will extend Message.
     *                            If we can't read any data, returns null.
     * @throws BadPacketException Information in the message is impossible or invalid, try the next packet.
     * @throws IOException        There's a problem with our connection to the remote computer, disconnect.
     */
    public static Message read(InputStream in) throws BadPacketException, IOException {

        // Read one Gnutella packet from the given InputStream, and return a type-specific object like a PingReply that represents it
        return Message.read(
            in,           // The InputStream object we can call read() on to get data from the remote computer
            new byte[23], // Make a 23-byte buffer the method can use to hold the Gnutella packet header
            N_UNKNOWN,    // We don't know if we got this message over TCP or UDP
            SOFT_MAX);    // Use the default soft max TTL of 3
    }

    /**
     * Only test code calls this method.
     * 
     * @modifies in
     * @effects reads a packet from the network and returns it as an
     *  instance of a subclass of Message, unless one of the following happens:
     *    <ul>
     *    <li>No data is available: returns null
     *    <li>A bad packet is read: BadPacketException.  The client should be
     *      able to recover from this.
     *    <li>A major problem occurs: IOException.  This includes reading packets
     *      that are ridiculously long and half-completed messages. The client
     *      is not expected to recover from this.
     *    </ul>
     */
    public static Message read(InputStream in, byte softMax) throws BadPacketException, IOException {
        return Message.read(in, new byte[23], N_UNKNOWN, softMax);
    }

    /**
     * Read one Gnutella packet from a given InputStream, and return a packet object like a PingReply that represents it.
     * UDPServiceStub.Receiver.receive() calls this.
     * 
     * @param  in                 The InputStream object we can call read() on to get data from the remote computer.
     * @param  network            The Internet protocol this Gnutella packet traveled to us on, like 1 N_TCP or 2 N_UDP.
     * @return                    A new object that represents the packet.
     *                            If this reads a ping packet, returns a PingRequest object, if it reads a pong, returns a PingResponse.
     *                            There is no such thing as a Message object, but the object it returns will extend Message.
     *                            If we can't read any data, returns null.
     * @throws BadPacketException Information in the message is impossible or invalid, try the next packet.
     * @throws IOException        There's a problem with our connection to the remote computer, disconnect.
     */
    public static Message read(InputStream in, int network) throws BadPacketException, IOException {

        // Read one Gnutella packet from the given InputStream, and return a type-specific object like a PingReply that represents it
        return Message.read(
            in,           // The InputStream object we can call read() on to get data from the remote computer
            new byte[23], // Make a 23-byte buffer the method can use to hold the Gnutella packet header
            network,      // The Internet protocol we got this packet on, like 1 N_TCP or 2 N_UDP
            SOFT_MAX);    // Use the default soft max TTL of 3
    }

    /**
     * Only test code calls this method.
     * 
     * @requires buf.length==23
     * @effects exactly like Message.read(in), but buf is used as scratch for
     *  reading the header.  This is an optimization that lets you avoid
     *  repeatedly allocating 23-byte arrays.  buf may be used when this returns,
     *  but the contents are not guaranteed to contain any useful data.  
     */
    public static Message read(InputStream in, byte[] buf, byte softMax) throws BadPacketException, IOException {
        return Message.read(in, buf, N_UNKNOWN, softMax);
    }

    /**
     * Read one Gnutella packet from a given InputStream, and return a packet object like a PingReply that represents it.
     * UDPService.handleRead() and MulticastService.run() use this read() method.
     * 
     * @param in                  The InputStream object we can call read() on to get data from a remote computer sending us Gnutella messages.
     * @param network             The Internet protocol this Gnutella packet traveled to us on, like 1 N_TCP or 2 N_UDP.
     * @param buf                 An already allocated 23-byte buffer we can use to hold a Gnutella packet header.
     *                            This is an optimization that lets you avoid repeatedly allocating 23-byte arrays.
     * @return                    A new object that represents the packet.
     *                            If this reads a ping packet, returns a PingRequest object, if it reads a pong, returns a PingResponse.
     *                            There is no such thing as a Message object, but the object it returns will extend Message.
     *                            If we can't read any data, returns null.
     * @throws BadPacketException Information in the message is impossible or invalid, try the next packet.
     * @throws IOException        There's a problem with our connection to the remote computer, disconnect.
     */
    public static Message read(InputStream in, int network, byte[] buf) throws BadPacketException, IOException {

        // Read one Gnutella packet from the given InputStream, and return a type-specific object like a PingReply that represents it
        return Message.read(
            in,        // The InputStream object we can call read() on to get data from the remote computer
            buf,       // An already allocated 23-byte buffer we can use to hold a Gnutella packet header
            network,   // The Internet protocol we got this packet on, like 1 N_TCP or 2 N_UDP
            SOFT_MAX); // Use the default soft max TTL of 3
    }

    /**
     * Read one Gnutella packet from a given InputStream, and return a packet object like a PingReply that represents it.
     * Calls from UDPService.handleRead() reach this method.
     * 
     * @param  in                 The InputStream object we can call read() on to get data from a remote computer sending us Gnutella messages.
     * @param  buf                An already allocated 23-byte buffer we can use to hold a Gnutella packet header.
     *                            This is an optimization that lets you avoid repeatedly allocating 23-byte arrays.
     * @param  network            The Internet protocol this Gnutella packet traveled to us on, like 1 N_TCP or 2 N_UDP.
     * @param  softMax            The hops + TTL limit we chose for this remote computer and will keep its packets within.
     * @return                    A new object that represents the packet.
     *                            If this reads a ping packet, returns a PingRequest object, if it reads a pong, returns a PingResponse.
     *                            There is no such thing as a Message object, but the object it returns will extend Message.
     *                            If we can't read any data, returns null.
     * @throws BadPacketException Information in the message is impossible or invalid, try the next packet.
     * @throws IOException        There's a problem with our connection to the remote computer, disconnect.
     */
    public static Message read(InputStream in, byte[] buf, int network, byte softMax) throws BadPacketException, IOException {

        /*
         * 1. Read header bytes from network.  If we timeout before any
         *    data has been read, return null instead of throwing an
         *    exception.
         */

        /*
         * These are the parts of a Gnutella packet header, in the right order, with each part the right size.
         * 
         * guid      At  0, length 16, the globally unique identifier of this packet.
         * function  At 16, the byte that identifies what kind of packet this is, like ping or pong.
         * ttl       At 17, the number of hops this packet can travel across the Internet from here.
         * hops      At 18, the number of hops this packet has traveled across the Internet to get here.
         * length    At 19, length 4, for a total size 23 bytes, the length of the packet payload.
         */

        // Loop until we've read all 23 bytes of a Gnutella message header
        for (int i = 0; i < 23; ) {

            // Variable for the number of bytes our calls to read() actually read
            int got;

            try {

                // Read the message header from the remote computer into the temporary buffer buf
                got = in.read( // Returns how many bytes we got
                    buf,       // Destination buffer
                    i,         // Start writing this far into buf
                    23 - i);   // Don't read more bytes than 23 - i, the amount of free space left in our destination buffer

            // Java terminated the input transfer because another thread called interrupt() on this one
            } catch (InterruptedIOException e) {

                // If we haven't read any of the message header, just return null
                if (i == 0) return null;

                // We have read some of the message header, throw the exception upwards
                else throw e;
            }

            // If the InputStream has reached its end, read() returned -1
            if (got == -1) {

                // Our connection to the remote computer is closed, count that this happend and throw an exception
                ReceivedErrorStat.CONNECTION_CLOSED.incrementStat();
                throw new IOException("Connection closed.");
            }

            // Move i forward past the bytes we wrote into the buffer
            i += got;
        }

        /*
         * 2. Unpack.
         */

        // 19 bytes into the header, read the 4 bytes there as an int, this is the payload length
        int length = ByteOrder.leb2int(buf, 19); // The least significant byte is first

        /*
         * 2.5 If the length is hopelessly off (this includes lengths >
         *     than 2^31 bytes, throw an irrecoverable exception to
         *     cause this connection to be closed.
         */

        // The length is negative, or bigger than 65536
        if (length < 0 || length > MessageSettings.MAX_LENGTH.getValue()) {

            // Count this happened and throw an IOException that will make the program close our connection to the remote computer that sent us this malformed packet
            ReceivedErrorStat.INVALID_LENGTH.incrementStat();
            throw new IOException("Unreasonable message length: " + length);
        }

        /*
         * 3. Read the payload.  This must be done even for bad
         *    packets, so we can resume reading packets.
         */

        // Make a reference we'll point to the buffer we'll make for the message payload
        byte[] payload = null;

        // This Gnutella message has a payload
        if (length != 0) {

            // Allocate a new byte array long enough to hold the message payload
            payload = new byte[length];

            // Loop until we've read all the bytes of the message payload
            for (int i = 0; i < length; ) { // The index i extends over the data in payload, stop when i reaches length

                // Read the message payload from the remote computer into the payload buffer we made
                int got = in.read( // Returns how many bytes we got
                    payload,       // Destination buffer
                    i,             // Start writing this far into the payload buffer
                    length - i);   // Don't read more bytes than length - i, the amount of free space left in our destination buffer

                // If the InputStream has reached its end, read() returned -1
                if (got == -1) {

                    // Our connection to the remote computer is closed, count that this happend and throw an exception
                    ReceivedErrorStat.CONNECTION_CLOSED.incrementStat();
                    throw new IOException("Connection closed.");
                }

                // Move i beyond the data we just wrote
                i += got;
            }

        // length is 0, this Gnutella message doesn't have a payload, it's just a 23-byte header
        } else {

            // Point payload at an empty byte array
            payload = DataUtils.EMPTY_BYTE_ARRAY; // This is better than just leaving it null
        }

        // Make a new type-specific object for this message, like a PingRequest, and return it
        return createMessage(buf, payload, softMax, network);
    }

    /**
     * Make an object that represents the Gnutella packet from the header and payload we just read.
     * 
     * This is where the soft max TTL feature is.
     * createMessage() contains the code that enforces our soft max TTL limit.
     * The soft max TTL limit is a limit on hops + TTL for packets we receive.
     * When we connected to this remote computer, we chose a limit like 3 or 4 for it.
     * When it sends us a packet, we see if hops + TTL exceeds the limit.
     * If it does, we reduce TTL to meet the limit.
     * 
     * Sorts the message based on type, and has a constructor or factory method make a new object to represent it.
     * Returns the message that the message-type specific method created.
     * This will be a PingRequest or a QueryReply object, for instance.
     * These objects extend Message, this abstract base class.
     * 
     * Message.read() calls this method when we've received a Gnutella packet over UDP.
     * 
     * MessageReader.handleRead() calls this method.
     * A call from NIO reaches MessageReader.handleRead() when it can read data from its source.
     * MessageReader.handleRead() peeks in the header to find the payload length, and slices off the data for one packet.
     * It calls this createMessage() method, giving it the freshly-sliced header and payload byte arrays.
     * It takes back the type-specific Message object, and hands it off to ManagedConnection.processReadMessage(m).
     * 
     * @param header  The 23 byte header we read from the remote computer.
     * @param payload The payload we read after that.
     * @param softMax The hops + TTL limit we chose for this remote computer and will keep its packets within.
     * @param network The Internet protocol this Gnutella packet traveled to us on, like 1 N_TCP or 2 N_UDP.
     * @return        A new object that represents the packet.
     *                If this reads a ping packet, returns a PingRequest object, if it reads a pong, returns a PingResponse.
     *                There is no such thing as a Message object, but these objects extend Message.
     */
    public static Message createMessage(byte[] header, byte[] payload, byte softMax, int network) throws BadPacketException, IOException {

        /*
         * The header, starting at headerOffset, MUST be >= 19 bytes.
         * Additional headers bytes will be ignored and the byte[] will be discarded.
         * (Note that the header is normally 23 bytes, but we don't need the last 4 here.)
         */

        // Make sure the header is 19 bytes or longer (do) doesn't it have to be exactly 23 bytes?
        if (header.length < 19) throw new IllegalArgumentException("header must be >= 19 bytes.");

        /*
         * 4. Check values.   These are based on the recommendations from the
         *    GnutellaDev page.  This also catches those TTLs and hops whose
         *    high bit is set to 0.
         */

        // Pull the type, TTL, and hops numbers out of the message header
        byte func = header[16];
        byte ttl  = header[17];
        byte hops = header[18];

        // If the hops + TTL for this message is over 14, we'll throw a BadPacketException
        byte hardMax = (byte)14;

        // The hops count is negative or very large
        if (hops < 0) {

            // Record this happened and throw a BadPacketException
            ReceivedErrorStat.INVALID_HOPS.incrementStat();
            throw new BadPacketException("Negative (or very large) hops");

        // The TTL is negative or very large
        } else if (ttl < 0) {

            // Record this happened and throw a BadPacketException
            ReceivedErrorStat.INVALID_TTL.incrementStat();
            throw new BadPacketException("Negative (or very large) TTL");

        // This message has already hopped across the Internet more than our hops + TTL maximum for this remote computer
        } else if ((hops > softMax) && (func != F_QUERY_REPLY) && (func != F_PING_REPLY)) { // Don't restrict hits and pongs this way

            // Record this happened and throw a BadPacketException
            ReceivedErrorStat.HOPS_EXCEED_SOFT_MAX.incrementStat();
            throw new BadPacketException("func: " + func + ", ttl: " + ttl + ", hops: " + hops);
        }

        // This message's hops + TTL is more than 14
        else if (ttl + hops > hardMax) {

            ReceivedErrorStat.HOPS_AND_TTL_OVER_HARD_MAX.incrementStat();
            throw new BadPacketException("TTL+hops exceeds hard max; probably spam");

        // This message's hops + TTL exceeds the soft maximum limit we've set for this remote computer
        } else if ((ttl + hops > softMax) && (func != F_QUERY_REPLY) && (func != F_PING_REPLY)) { // Our soft max limit doesn't apply to hits and pongs

            /*
             * Tour Point
             * 
             * Soft max TTL is LimeWire's way of making sure the Gnutella packets we relay don't have TTL counts that are too high.
             * When we connect to a remote computer, we decide what soft max TTL we'll restrict it to.
             * Usually we set the soft max TTL to 3, but if the remote computer is running LimeWire, we'll set it to 4.
             * 
             * Soft max TTL is the maximum hops + TTL we'll allow.
             * When we get a Gnutella packet, we'll add its hops and TTL.
             * If that sum is over the soft max TTL limit we've set for it, we need to change it.
             * We'll lower the packet's TTL so hops + TTL is our limit.
             * 
             * The code here enforces the soft max TTL limit.
             * If (ttl + hops > softMax), we need to change the packet.
             * Setting (ttl = softMax - hops) makes the packet we received compliant to our limit.
             */

            /*
             * overzealous client
             */

            // Set ttl to comply to the soft max TTL we set for this remote computer
            ttl = (byte)(softMax - hops);

            /*
             * What happens if this leaves TTL 0? (do)
             * Can the packet leave our computer, or does it die here?
             */

            /*
             * readjust accordingly
             * should hold since hops<=softMax ==>
             * new ttl>=0
             */

            // Make sure the new TTL we set is a positive number or 0
            Assert.that(ttl >= 0);
        }

        /*
         * Delayed GUID allocation
         */

        // Copy the bytes of the message's GUID from the header into a new byte array named guid
        byte[] guid = new byte[16]; // GUIDs are 16 bytes long
        for (int i = 0; i < 16; i++) guid[i] = header[i]; // The first 16 bytes of the header are the packet's GUID

        /*
         * TODO3: can optimize
         */

        /*
         * Dispatch based on opcode.
         */

        // Get the length of the payload
        int length = payload.length; // Read the length from the byte array instead of from the packet header (do) why?

        // Do something different depending on what kind of Gnutella message this is
        switch (func) {

        /*
         * TODO: all the length checks should be encapsulated in the various
         * constructors; Message shouldn't know anything about the various
         * messages except for their function codes.  I've started this
         * refactoring with PushRequest and PingReply.
         */

        // Ping
        case F_PING:

            // This ping message has a payload
            if (length > 0) {

                /*
                 * Big ping
                 */

                // Make a new PingRequest object to represent this ping
                return new PingRequest(guid, ttl, hops, payload); // Include the payload
            }

            // Make a new PingRequest object to represent this ping
            return new PingRequest(guid, ttl, hops);

        // Pong
        case F_PING_REPLY:

            // Call the factory method PingReply.createFromNetwork to make a new PingReply object to represent this message
            return PingReply.createFromNetwork(guid, ttl, hops, payload);

        // Query
        case F_QUERY:

            // If the payload is shorter than 3 bytes, leave this switch statement to throw a BadPacketException
            if (length < 3) break;

            // Make a new QueryRequest object with the information we read
            return QueryRequest.createNetworkQuery(guid, ttl, hops, payload, network);

        // Hit
        case F_QUERY_REPLY:

            // If the payload is shorter than 26 bytes, leave this switch statement to throw a BadPacketException
            if (length < 26) break;

            // Make a new QueryReply object with the information we read
            return new QueryReply(guid, ttl, hops, payload, network);

        // Push request
        case F_PUSH:

            // Make a new PushRequest object with the information we read
            return new PushRequest(guid, ttl, hops, payload, network);

        // Query route table patch
        case F_ROUTE_TABLE_UPDATE:

            /*
             * The exact subclass of RouteTableMessage returned depends on
             * the variant stored within the payload.  So leave it to the
             * static read(..) method of RouteTableMessage to actually call
             * the right constructor.
             */

            // Give the information we read to the static RouteTableMessage.read() method, which will decide what kind of object to create
            return RouteTableMessage.read(guid, ttl, hops, payload);

        // Vendor-specific message, a Gnutella message designed and introduced by a specific Gnutella program vendor, like LimeWire or BearShare
        case F_VENDOR_MESSAGE:

            // Have the VendorMessage factory method parse the network data into a vendor message specific object that extends VendorMessage
            return VendorMessage.deriveVendorMessage(guid, ttl, hops, payload, network);

        // Stable vendor-specific message, not used
        case F_VENDOR_MESSAGE_STABLE:

            // Have the VendorMessage factory method parse the network data into a vendor message specific object that extends VendorMessage
            return VendorMessage.deriveVendorMessage(guid, ttl, hops, payload, network);

        // UDP connection message, a modified Gnutella message designed to hold data and be part of a UDP firewall-to-firewall file transfer connection
        case F_UDP_CONNECTION:

            // Have the UDPConnectionMessage factory method make a new UDPConnectionMessage object, or return null
            return UDPConnectionMessage.createMessage(guid, ttl, hops, payload);
        }

        // The byte in the message header that tells what kind of a packet this is describes some packet type we don't know about, or is a mistake
        ReceivedErrorStat.INVALID_CODE.incrementStat();
        throw new BadPacketException("Unrecognized function code: " + func);
    }

    /**
     * Write the bytes of this Gnutella packet to the given OutputStream.
     * Writes quickly, without using a temporary buffer.
     * 
     * @param out An OutputStream we can call write() on to send data to the remote computer
     */
    public void writeQuickly(OutputStream out) throws IOException {

        // Write the 23-byte Gnutella packet header to the OutputStream
        out.write(guid, 0, 16);         // At  0, length 16, the globally unique identifier of this packet
        out.write(func);                // At 16, the byte that identifies what kind of packet this is, like ping or pong
        out.write(ttl);                 // At 17, the number of hops this packet can travel across the Internet from here
        out.write(hops);                // At 18, the number of hops this packet has traveled across the Internet to get here
        ByteOrder.int2leb(length, out); // At 19, length 4, for a total size 23 bytes, the length of the packet payload, least significant byte first

        // Write the Gnutella packet payload
        writePayload(out);
    }

    /**
     * Write the bytes of this Gnutella packet to the given OutputStream.
     * Uses the given 23-byte temporary buffer to only call out.write() 2 times instead of 6.
     * 
     * @param out An OutputStream we can call write() on to send data to the remote computer
     */
    public void write(OutputStream out, byte[] buf) throws IOException {

        /*
         * TODO3: can optimize
         */

        // Write the 23-byte Gnutella packet header to the given temporary buffer
        for (int i = 0; i < 16; i++) buf[i] = guid[i]; // At  0, length 16, the globally unique identifier of this packet
        buf[16] = func;                                // At 16, the byte that identifies what kind of packet this is, like ping or pong
        buf[17] = ttl;                                 // At 17, the number of hops this packet can travel across the Internet from here
        buf[18] = hops;                                // At 18, the number of hops this packet has traveled across the Internet to get here
        ByteOrder.int2leb(length, buf, 19);            // At 19, length 4, for a total size 23 bytes, the length of the packet payload, least significant byte first

        // Write the packet header in the temporary buffer into the given OutputStream
        out.write(buf);

        // Write the Gnutella packet payload into the OutputStream
        writePayload(out);
    }

    /**
     * Write 23-byts of 0s to the given OutputStream.
     * This is the size of a Gnutella packet header.
     * 
     * Writes an encoding of this to out.
     * Does not flush out.
     * 
     * @param out An OutputStream we can call write() on to send data to the remote computer
     */
    public void write(OutputStream out) throws IOException {

        // Make a new byte array of 23 bytes which Java will initialize with 0s, and write it to the given OutputStream
        write(out, new byte[23]);
    }

    /**
     * Write the Gnutella packet payload to the given OutputStream.
     * Does not flush the OutputStream.
     * 
     * This is an abstract method.
     * Here in the Message class, it has no function body.
     * A class that extends Message, like PingRequest, will write a mesage body for it.
     * 
     * @param out An OutputStream we can call write() on to send data to the remote computer
     */
    protected abstract void writePayload(OutputStream out) throws IOException;

    /**
     * Write a separating 0x1C byte to the given OutputStream, followed by some text.
     * If you don't give this any text to write, it does nothing and reports back the same separator requirement you gave it.
     * 
     * @param out                An OutputStream we can call write() on to send data to the remote computer.
     * @param addPrefixDelimiter True to write a 0x1C byte before this next line of text to separate it from a previous line of text.
     *                           False to just write the text.
     * @param extBytes           The text to write.
     * @return                   True if this method wrote text, and you'll need to write 0x1C before the next group of text.
     *                           False if this method wrote nothing, so you don't have to write 0x1C next.
     */
    protected boolean writeGemExtension(OutputStream os, boolean addPrefixDelimiter, byte[] extBytes) throws IOException {

        // If there is no text for us to write, do nothing
        if (extBytes == null || (extBytes.length == 0)) {

            // If we would have had to add a 0x1C byte, the next call will
            return addPrefixDelimiter;
        }

        // Write the separating byte, 0x1C, if the caller requested it
        if (addPrefixDelimiter) os.write(0x1c);

        // Write the text
        os.write(extBytes);

        // Before we write the next string, we'll have to write a 0x1C byte to separate it from this one
        return true;
    }

    /**
     * Write a separating 0x1C byte to the given OutputStream, followed by some text.
     * If you don't give this any text to write, it writes a 0 byte instead.
     * 
     * @param out                An OutputStream we can call write() on to send data to the remote computer.
     * @param addPrefixDelimiter True to write a 0x1C byte before this next line of text to separate it from a previous line of text.
     *                           False to just write the text.
     * @param extBytes           The text to write.
     * @return                   True if this method wrote something, and you'll need to write 0x1C before the next group of text.
     *                           False if this method wrote nothing, so you don't have to write 0x1C next.
     */
    protected boolean writeGemExtension(OutputStream os, boolean addPrefixDelimiter, String ext) throws IOException {

        // The caller gave us some text to write
        if (ext != null) {

            // Write the prefix delimiter if necessary, and then the given text
            return writeGemExtension(os, addPrefixDelimiter, ext.getBytes());

        // There is no text for us to write
        } else {

            // Write the prefix delimiter if necessary, followed by a single 0 byte in place of the text
            return writeGemExtension(os, addPrefixDelimiter, new byte[0]);
        }
    }

    /**
     * Write a group of lines of text separated by 0x1C bytes.
     * If the list of lines of text is empty, does nothing.
     * 
     * @param os                 An OutputStream we can call write() on to send data to the remote computer
     * @param addPrefixDelimiter True to write a 0x1C byte before these next lines of text to separate them from previous lines of text
     * @param iter               An Iterator that is moving over a list of String objects
     */
    protected boolean writeGemExtensions(OutputStream os, boolean addPrefixDelimiter, Iterator iter) throws IOException {

        // If the caller didn't give us any text to write, return the same separator requirement it gave us
        if (iter == null) return addPrefixDelimiter;

        // Loop until we run the Iterator out of strings of text
        while (iter.hasNext()) {

            // Write a 0x1C separator byte if necessary, and then one line of text
            addPrefixDelimiter = writeGemExtension( // Returns true if it wrote something and we'll need to add a separator before the next text
                os,                                 // The OutputStream to write to
                addPrefixDelimiter,                 // Tell it if it needs to write a 0x1C separator byte before any text it writes
                iter.next().toString());            // The text it should write this time
        }

        // Return true if we wrote anything, and we'll need to write a 0x1C separator byte before writing more
        return addPrefixDelimiter;
    }

    /**
     * Reads a null-terminated string from a given InputStream, and returns a byte array with the text.
     * 
     * Reads bytes from the InputStream until it reaches a terminating 0.
     * Returns a byte array with all those bytes up to the 0.
     * Reads the 0 from the InputStream, but does not return it in the byte array.
     * 
     * TODO:kfaaborg This method isn't marked static, but it doesn't use the Message object at all.
     * 
     * @param in An InputStream we can read from
     * @return   A new byte array with all the bytes from in until we read a 0
     */
    protected byte[] readNullTerminatedBytes(InputStream is) throws IOException {

        /*
         * When you write data to a ByteArrayOutputStream, it takes it and keeps it in an internal byte array.
         * The byte array automatically grows as you add data to it.
         * You can get the data by calling b.toByteArray() or b.toString().
         */

        // Make a new ByteArrayOutputStream that will grow to hold the bytes we write to it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // We'll keep each byte we read from the InputStream in an int named i
        int i;

        // Loop until we run the InputStream out of data or reach a terminating 0
        while (
            (is.available() > 0) && // If the InputStream still has at least 1 byte left in it
            (i = is.read()) != 0) { // Read a byte from the InputStream and save it in i

            // If the byte we just read isn't a terminating 0, add it to the ByteArrayOutputStream
            baos.write(i);
        }

        // Return a byte array with the contents we read from the InputStream, not including the terminating 0
        return baos.toByteArray();
    }

    /*
     * ////////////////////////////////////////////////////////////////////
     */

    /**
     * Find out what Internet protocol this Gnutella packet travels on.
     * 
     * @return A number like -1 N_UNKNOWN, 1 N_TCP, 2 N_UDP, or 3 N_MULTICAST
     */
    public int getNetwork() {

        // Return our record of the Internet protocol this Gnutella packet uses
        return network;
    }

    /**
     * True if this Gnutella packet travels on the multicast Internet protocol.
     * 
     * @return True if network is 3 N_MULTICAST, false if it's some other protocol
     */
    public boolean isMulticast() {

        // See if the Internet protocol is 3, N_MULTICAST
        return network == N_MULTICAST;
    }

    /**
     * True if this Gnutella packet travels in a UDP packet.
     * 
     * @return True if network is 2 N_UDP, false if it's some other protocol
     */
    public boolean isUDP() {

        // See if the Internet protocol is 2, N_UDP
        return network == N_UDP;
    }

    /**
     * True if this Gnutella packet travels in a TCP stream.
     * 
     * @return True if network is 1 N_TCP, false if it's some other protocol
     */
    public boolean isTCP() {

        // See if the Internet protocol is 1, N_TCP
        return network == N_TCP;
    }

    /**
     * True if we've marked the Internet protocol this Gnutella packet travels on as unknown.
     * 
     * @return True if network is -1 N_UNKNOWN, false if it's set to a protocol
     */
    public boolean isUnknownNetwork() {

        // See if the Internet protocol is -1, N_UNKNOWN
        return network == N_UNKNOWN;
    }

    /**
     * The GUID of this Gnutella packet that marks it as unique.
     * This GUID may contain the IP address to send out of band replies to.
     * 
     * @return A 16-byte array with the message GUID
     */
    public byte[] getGUID() {

        // Return a reference to the GUID byte array
        return guid;
    }

    /**
     * Get the byte in this Gnutella packet's header that identifies what kind of a packet it is.
     * 
     * @return The packet's type byte, like 0x00 ping or 0x01 pong
     */
    public byte getFunc() {

        // Return the function byte that identifies the type of this packet
        return func;
    }

    /**
     * Get this Gnutella packet's TTL, its time to live.
     * This is the number of times this message can travel across the Internet from one computer to another.
     * 
     * @return The TTL byte from the Gnutella packet header
     */
    public byte getTTL() {

        // Return the function byte that holds the TTL
        return ttl;
    }

    /**
     * Set the TTL for this Gnutella packet.
     * This is the number of times the packet can travel across the Internet.
     * 
     * You can set the TTL to 0.
     * If you give this method a negative number, it will throw an IllegalArgumentException.
     * 
     * @param  ttl                      The new TTL value to set for this Gnutella packet
     * @throws IllegalArgumentException If the given TTL is less than 0
     */
    public void setTTL(byte ttl) throws IllegalArgumentException {

        // Make sure the given TTL isn't a negative number
        if (ttl < 0) throw new IllegalArgumentException("invalid TTL: " + ttl);

        // Save the TTL number in this object
        this.ttl = ttl;
    }

    /**
     * Set this Gnutella packet's GUID.
     * This is the GUID that marks this Gnutella packet as unique.
     * 
     * Use this when we want to cache query replies or other messages, and change the GUID for the request. (do)
     * 
     * @param guid The new GUID for this Gnutella packet
     */
    protected void setGUID(GUID guid) {

        // Save the new GUID value in this object
        this.guid = guid.bytes();
    }

    /**
     * Set the hops count for this Gnutella packet.
     * This is the number of times the packet has already traveled across the Internet.
     * 
     * Use this when you want to make a packet look like it's traveled further than it really has. (do)
     * 
     * If you give this method a negative number, it will throw an IllegalArgumentException.
     * 
     * @param  ttl                      The new TTL value to set for this Gnutella packet
     * @throws IllegalArgumentException If the given TTL is less than 0
     */
    public void setHops(byte hops) throws IllegalArgumentException {

        // Make sure the given hops count isn't a negative number
        if (hops < 0) throw new IllegalArgumentException("invalid hops: " + hops);

        // Save the hops count in this object
        this.hops = hops;
    }

    /**
     * Get the hops count of this Gnutella packet.
     * This is the number of times this message has traveled across the Internet from one computer to another.
     * 
     * @return The hops count
     */
    public byte getHops() {

        // Return the hops count
        return hops;
    }

    /**
     * Get the size of the payload part of this Gnutella packet.
     * 
     * A Gnutella packet starts with a 23-byte header.
     * After that, there is a payload.
     * An int in the header tells the length of the payload that follows.
     * If there isn't a payload, the length is 0.
     * 
     * @return The payload length, in bytes
     */
    public int getLength() {

        // Return the payload length
        return length;
    }

    /**
     * Change the length of the payload.
     * This length is stored in the Gnutella packet header.
     * The payload comes after the 23-byte header, and has the specified length.
     * 
     * @param l The new payload length to set, in bytes
     */
    protected void updateLength(int l) {

        // Change the payload length to the given value
        length = l;
    }

    /**
     * Get how long this Gnutella packet is, in bytes.
     * When we send this packet to a remote computer, this is how many bytes we'll send.
     * The total size is 23 bytes for the header, plus the size of the payload after that.
     * 
     * @return The size of this Gnutella packet, in bytes
     */
    public int getTotalLength() {

        // Add the size of the header, 23 bytes, to the payload length
        return 23 + length;
    }

    /**
     * Record that this packet has traveled across the Internet.
     * Makes hops one more, and TTL one less.
     * 
     * Increments hops.
     * If ttl is positive, returns its current value and then decrements it.
     * If ttl is 0, returns 0 without changing it.
     * 
     * @return The TTL before this function decrements it
     */
    public byte hop() {

        // Record this packet has traveled another hop across the Internet
        hops++;

        // This packet had enough life to survive that hop
        if (ttl > 0) {

            // Return the TTL before changing it
            return ttl--; // If ttl starts out 1, this method returns 1 and then sets ttl to 0

        // This packet already has a TTL of 0, it should be dead (do)
        } else {

            // Return the TTL of 0, unchanged
            return ttl;
        }
    }

    /**
     * Get the time we made this Message object.
     * This is the time we got the packet, and can tell us how long we've had it.
     * This time isn't part of the information we send when we send the Gnutella packet.
     * 
     * @return The time in milliseconds since 1970 that the program made this Message object
     */
    public long getCreationTime() {

        // Return the time Java set when it created this Message object
        return creationTime;
    }

    /**
     * Get our priority for sending this packet.
     * A lower number means a higher priority.
     * 
     * We use this for flow control.
     * The flow control algorithm will discard low priority packets to send high priority ones instead.
     * 
     * The program sets and uses this priority value.
     * It's not part of the packet as it travels on the Internet.
     * 
     * Initialized to 0.
     * 
     * @return This Gnutella packet's sending priority
     */
    public int getPriority() {

        // Return the priority we set
        return priority;
    }

    /**
     * Set our priority for sending this packet.
     * A lower number means a higher priority.
     * 
     * We use this for flow control.
     * The flow control algorithm will discard low priority packets to send high priority ones instead.
     * 
     * The program sets and uses this priority value.
     * It's not part of the packet as it travels on the Internet.
     * 
     * @param priority The sending priority for this Gnutella packet
     */
    public void setPriority(int priority) {

        // Save the given priority in this object
        this.priority = priority;
    }

    /**
     * Return a Message object like this one, but without a GGEP extension block.
     * May return a copy of this Message object, or may return a reference to this same Message object.
     * 
     * Returns a message identical to this but without any extended (typically
     * GGEP) data.  Since Message's are mostly immutable, the returned message
     * may alias parts of this; in fact the returned message could even be this.
     * The caveat is that the hops and TTL field of Message can be mutated for
     * efficiency reasons.  Hence you must not call hop() on either this or the
     * returned value.  Typically this is not a problem, as hop() is called
     * before forwarding/broadcasting a message.
     * 
     * Returns an instance of this without any dangerous extended payload
     * 
     * @return A Message object like this one, but without a GGEP block
     */
    public abstract Message stripExtendedPayload();

    /**
     * Compare the priority we set for this Gnutella packet to another one.
     * Lower priority numbers indicate better priority.
     * 
     * @param message Another Message object to compare this one to.
     * @return        Negative if the given object has a smaller priority number, which is better.
     *                0 if they have the same priority.
     *                Positive if this object has a smaller priority number, which is better.
     */
    public int compareTo(Object message) {

        // Cast the given Object to a Message object
        Message m = (Message)message;

        // Subtract the priorities
        return m.getPriority() - this.getPriority();
    }

    /**
     * Express this Gnutella packet as text.
     * Composes text like "{guid=00FF00FF00FF00FF00FF00FF00FF00FF, ttl=1, hops=1, priority=1}"
     * 
     * This isn't the same thing as turning it into bytes to send to a computer.
     * 
     * @return A String with the packet's GUID, TTL, hops count, and priority
     */
    public String toString() {

        // Compose text like "{guid=00FF00FF00FF00FF00FF00FF00FF00FF, ttl=1, hops=1, priority=1}"
        return "{guid=" + (new GUID(guid)).toString() + ", ttl=" + ttl + ", hops=" + hops + ", priority=" + getPriority() + "}";
    }

	/**
     * Call recordDrop() to record that we're dropping this Gnutella packet in the program's statistics.
     * This method in Message is abstract, a class that extends Message will implement it.
	 */
	public abstract void recordDrop();
}
