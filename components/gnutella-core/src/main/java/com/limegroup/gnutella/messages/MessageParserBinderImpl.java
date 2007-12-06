package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.net.SocketAddress;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.MessageFactory.MessageParser;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactory;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;

@Singleton
public class MessageParserBinderImpl implements MessageParserBinder {

    private final PingReplyFactory pingReplyFactory;
    private final QueryReplyFactory queryReplyFactory;
    private final QueryRequestFactory queryRequestFactory;
    private final VendorMessageFactory vendorMessageFactory;
    private final PingRequestFactory pingRequestFactory;

    @Inject
    public MessageParserBinderImpl(PingReplyFactory pingReplyFactory,
            QueryRequestFactory queryRequestFactory,
            QueryReplyFactory queryReplyFactory,
            VendorMessageFactory vendorMessageFactory,
            PingRequestFactory pingRequestFactory) {
        this.pingReplyFactory = pingReplyFactory;
        this.queryRequestFactory = queryRequestFactory;
        this.queryReplyFactory = queryReplyFactory;
        this.vendorMessageFactory = vendorMessageFactory;
        this.pingRequestFactory = pingRequestFactory;
    }
    
    public void bind(MessageFactory messageFactory) {
        messageFactory.setParser(Message.F_PING, new PingRequestParser());
        messageFactory.setParser(Message.F_PING_REPLY, new PingReplyParser());
        messageFactory.setParser(Message.F_QUERY, new QueryRequestParser());
        messageFactory.setParser(Message.F_QUERY_REPLY, new QueryReplyParser());
        messageFactory.setParser(Message.F_PUSH, new PushRequestParser());
        messageFactory.setParser(Message.F_ROUTE_TABLE_UPDATE, new RouteTableUpdateParser());
        messageFactory.setParser(Message.F_VENDOR_MESSAGE, new VendorMessageParser());
        messageFactory.setParser(Message.F_VENDOR_MESSAGE_STABLE, new VendorMessageStableParser());
    }
    
    /**
     * An abstract class for Gnutella Message parsers
     */
    public static abstract class GnutellaMessageParser implements MessageParser {
        
        public Message parse(byte[] header, byte[] payload,
                Network network, byte softMax, SocketAddress address) throws BadPacketException, IOException {
            
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
                assert(ttl >= 0); // should hold since hops<=softMax ==>
                // new ttl>=0
            }

            // Delayed GUID allocation
            byte[] guid = new byte[16];
            System.arraycopy(header, 0, guid, 0, guid.length /* 16 */);
            
            return parse(guid, ttl, hops, payload, network);
        }
        
        protected abstract Message parse(byte[] guid, byte ttl, byte hops, 
                byte[] payload, Network network) throws BadPacketException;
    }
    
    private class PingRequestParser extends GnutellaMessageParser {
        protected Message parse(byte[] guid, byte ttl, byte hops, byte[] payload, Network network)
                throws BadPacketException {
            return pingRequestFactory.createFromNetwork(guid, ttl, hops, payload, network);
        }
    }
    
    private class PingReplyParser extends GnutellaMessageParser {
        protected Message parse(byte[] guid, byte ttl, byte hops, 
                byte[] payload, Network network) throws BadPacketException {
            return pingReplyFactory.createFromNetwork(guid, ttl, hops, payload, network);
        }
    }
    
    private class QueryRequestParser extends GnutellaMessageParser {
        protected Message parse(byte[] guid, byte ttl, byte hops, 
                byte[] payload, Network network) throws BadPacketException {
            if (payload.length < 3) {
                throw new BadPacketException("Query request too short: " + payload.length);
            }
            
            return queryRequestFactory.createNetworkQuery(guid, ttl, hops, payload, network);
        }
    }
    
    private class QueryReplyParser extends GnutellaMessageParser {
        protected Message parse(byte[] guid, byte ttl, byte hops, 
                byte[] payload, Network network) throws BadPacketException {
            if (payload.length < 26) {
                throw new BadPacketException("Query reply too short: " + payload.length);
            }
            
            return queryReplyFactory.createFromNetwork(guid, ttl, hops,
                    payload, network);
        }
    }
    
    private static class PushRequestParser extends GnutellaMessageParser {
        protected Message parse(byte[] guid, byte ttl, byte hops, 
                byte[] payload, Network network) throws BadPacketException {
            return new PushRequestImpl(guid, ttl, hops, payload, network);
        }
    }
    
    private static class RouteTableUpdateParser extends GnutellaMessageParser {
        protected Message parse(byte[] guid, byte ttl, byte hops, 
                byte[] payload, Network network) throws BadPacketException {
            // The exact subclass of RouteTableMessage returned depends on
            // the variant stored within the payload. So leave it to the
            // static read(..) method of RouteTableMessage to actually call
            // the right constructor.
            return RouteTableMessage.read(guid, ttl, hops, payload, network);
        }
    }
    
    private class VendorMessageParser extends GnutellaMessageParser {
        protected Message parse(byte[] guid, byte ttl, byte hops, 
                byte[] payload, Network network) throws BadPacketException {
            return vendorMessageFactory.deriveVendorMessage(guid, ttl, hops, payload, network);
        }
    }
    
    private class VendorMessageStableParser extends GnutellaMessageParser {
        protected Message parse(byte[] guid, byte ttl, byte hops, 
                byte[] payload, Network network) throws BadPacketException {
            return vendorMessageFactory.deriveVendorMessage(guid, ttl, hops, payload, network);
        }
    }
}
