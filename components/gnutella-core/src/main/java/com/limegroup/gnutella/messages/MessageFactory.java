package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;
import com.limegroup.gnutella.udpconnect.UDPConnectionMessage;
import com.limegroup.gnutella.util.DataUtils;

public class MessageFactory {

    /**
     * Reads a Gnutella message from the specified input stream. The returned
     * message can be any one of the recognized Gnutella message, such as
     * queries, query hits, pings, pongs, etc.
     * 
     * @param in
     *            the <tt>InputStream</tt> instance containing message data
     * @return a new Gnutella message instance
     * @throws <tt>BadPacketException</tt> if the message is not considered
     *             valid for any reason
     * @throws <tt>IOException</tt> if there is any IO problem reading the
     *             message
     */
    public static Message read(InputStream in) throws BadPacketException,
            IOException {
        return MessageFactory.read(in, new byte[23], Message.N_UNKNOWN,
                Message.SOFT_MAX);
    }

    /**
     * @modifies in
     * @effects reads a packet from the network and returns it as an instance of
     *          a subclass of Message, unless one of the following happens:
     *          <ul>
     *          <li>No data is available: returns null
     *          <li>A bad packet is read: BadPacketException. The client should
     *          be able to recover from this.
     *          <li>A major problem occurs: IOException. This includes reading
     *          packets that are ridiculously long and half-completed messages.
     *          The client is not expected to recover from this.
     *          </ul>
     */
    public static Message read(InputStream in, byte softMax)
            throws BadPacketException, IOException {
        return MessageFactory
                .read(in, new byte[23], Message.N_UNKNOWN, softMax);
    }

    /**
     * @modifies in
     * @effects reads a packet from the network and returns it as an instance of
     *          a subclass of Message, unless one of the following happens:
     *          <ul>
     *          <li>No data is available: returns null
     *          <li>A bad packet is read: BadPacketException. The client should
     *          be able to recover from this.
     *          <li>A major problem occurs: IOException. This includes reading
     *          packets that are ridiculously long and half-completed messages.
     *          The client is not expected to recover from this.
     *          </ul>
     */
    public static Message read(InputStream in, int network)
            throws BadPacketException, IOException {
        return MessageFactory.read(in, new byte[23], network, Message.SOFT_MAX);
    }

    /**
     * @requires buf.length==23
     * @effects exactly like Message.read(in), but buf is used as scratch for
     *          reading the header. This is an optimization that lets you avoid
     *          repeatedly allocating 23-byte arrays. buf may be used when this
     *          returns, but the contents are not guaranteed to contain any
     *          useful data.
     */
    public static Message read(InputStream in, byte[] buf, byte softMax)
            throws BadPacketException, IOException {
        return MessageFactory.read(in, buf, Message.N_UNKNOWN, softMax);
    }

    /**
     * Reads a message using the specified buffer & network and the default soft
     * max.
     */
    public static Message read(InputStream in, int network, byte[] buf)
            throws BadPacketException, IOException {
        return MessageFactory.read(in, buf, network, Message.SOFT_MAX);
    }

    /**
     * @param network
     *            the network this was received from.
     * @requires buf.length==23
     * @effects exactly like Message.read(in), but buf is used as scratch for
     *          reading the header. This is an optimization that lets you avoid
     *          repeatedly allocating 23-byte arrays. buf may be used when this
     *          returns, but the contents are not guaranteed to contain any
     *          useful data.
     */
    public static Message read(InputStream in, byte[] buf, int network,
            byte softMax) throws BadPacketException, IOException {

        // 1. Read header bytes from network. If we timeout before any
        // data has been read, return null instead of throwing an
        // exception.
        for (int i = 0; i < 23;) {
            int got;
            try {
                got = in.read(buf, i, 23 - i);
            } catch (InterruptedIOException e) {
                // have we read any of the message yet?
                if (i == 0)
                    return null;
                else
                    throw e;
            }
            if (got == -1) {
                ReceivedErrorStat.CONNECTION_CLOSED.incrementStat();
                throw new IOException("Connection closed.");
            }
            i += got;
        }

        // 2. Unpack.
        int length = ByteOrder.leb2int(buf, 19);
        // 2.5 If the length is hopelessly off (this includes lengths >
        // than 2^31 bytes, throw an irrecoverable exception to
        // cause this connection to be closed.
        if (length < 0 || length > MessageSettings.MAX_LENGTH.getValue()) {
            ReceivedErrorStat.INVALID_LENGTH.incrementStat();
            throw new IOException("Unreasonable message length: " + length);
        }

        // 3. Read rest of payload. This must be done even for bad
        // packets, so we can resume reading packets.
        byte[] payload = null;
        if (length != 0) {
            payload = new byte[length];
            for (int i = 0; i < length;) {
                int got = in.read(payload, i, length - i);
                if (got == -1) {
                    ReceivedErrorStat.CONNECTION_CLOSED.incrementStat();
                    throw new IOException("Connection closed.");
                }
                i += got;
            }
        } else {
            payload = DataUtils.EMPTY_BYTE_ARRAY;
        }

        return createMessage(buf, payload, softMax, network);
    }

    /**
     * Creates a message based on the header & payload. The header, starting at
     * headerOffset, MUST be >= 19 bytes. Additional headers bytes will be
     * ignored and the byte[] will be discarded. (Note that the header is
     * normally 23 bytes, but we don't need the last 4 here.) The payload MUST
     * be a unique byte[] of that payload. Nothing can write into or change the
     * byte[].
     */
    public static Message createMessage(byte[] header, byte[] payload,
            byte softMax, int network) throws BadPacketException, IOException {
        if (header.length < 19)
            throw new IllegalArgumentException("header must be >= 19 bytes.");

        // 4. Check values. These are based on the recommendations from the
        // GnutellaDev page. This also catches those TTLs and hops whose
        // high bit is set to 0.
        byte func = header[16];
        byte ttl = header[17];
        byte hops = header[18];

        byte hardMax = (byte) 14;
        if (hops < 0) {
            ReceivedErrorStat.INVALID_HOPS.incrementStat();
            throw new BadPacketException("Negative (or very large) hops");
        } else if (ttl < 0) {
            ReceivedErrorStat.INVALID_TTL.incrementStat();
            throw new BadPacketException("Negative (or very large) TTL");
        } else if ((hops > softMax) && (func != Message.F_QUERY_REPLY)
                && (func != Message.F_PING_REPLY)) {
            ReceivedErrorStat.HOPS_EXCEED_SOFT_MAX.incrementStat();
            throw new BadPacketException("func: " + func + ", ttl: " + ttl
                    + ", hops: " + hops);
        } else if (ttl + hops > hardMax) {
            ReceivedErrorStat.HOPS_AND_TTL_OVER_HARD_MAX.incrementStat();
            throw new BadPacketException(
                    "TTL+hops exceeds hard max; probably spam");
        } else if ((ttl + hops > softMax) && (func != Message.F_QUERY_REPLY)
                && (func != Message.F_PING_REPLY)) {
            ttl = (byte) (softMax - hops); // overzealous client;
            // readjust accordingly
            Assert.that(ttl >= 0); // should hold since hops<=softMax ==>
            // new ttl>=0
        }

        // Delayed GUID allocation
        byte[] guid = new byte[16];
        for (int i = 0; i < 16; i++)
            // TODO3: can optimize
            guid[i] = header[i];

        // Dispatch based on opcode.
        int length = payload.length;
        switch (func) {
        // TODO: all the length checks should be encapsulated in the various
        // constructors; Message shouldn't know anything about the various
        // messages except for their function codes. I've started this
        // refactoring with PushRequest and PingReply.
        case Message.F_PING:
            if (length > 0) // Big ping
                return new PingRequest(guid, ttl, hops, payload);
            return new PingRequest(guid, ttl, hops);

        case Message.F_PING_REPLY:
            return PingReply.createFromNetwork(guid, ttl, hops, payload);
        case Message.F_QUERY:
            if (length < 3)
                break;
            return QueryRequest.createNetworkQuery(guid, ttl, hops, payload,
                    network);
        case Message.F_QUERY_REPLY:
            if (length < 26)
                break;
            return new QueryReply(guid, ttl, hops, payload, network);
        case Message.F_PUSH:
            return new PushRequest(guid, ttl, hops, payload, network);
        case Message.F_ROUTE_TABLE_UPDATE:
            // The exact subclass of RouteTableMessage returned depends on
            // the variant stored within the payload. So leave it to the
            // static read(..) method of RouteTableMessage to actually call
            // the right constructor.
            return RouteTableMessage.read(guid, ttl, hops, payload);
        case Message.F_VENDOR_MESSAGE:
            return VendorMessage.deriveVendorMessage(guid, ttl, hops, payload,
                    network);
        case Message.F_VENDOR_MESSAGE_STABLE:
            return VendorMessage.deriveVendorMessage(guid, ttl, hops, payload,
                    network);
        case Message.F_UDP_CONNECTION:
            return UDPConnectionMessage.createMessage(guid, ttl, hops, payload);
        }

        ReceivedErrorStat.INVALID_CODE.incrementStat();
        throw new BadPacketException("Unrecognized function code: " + func);
    }
}
