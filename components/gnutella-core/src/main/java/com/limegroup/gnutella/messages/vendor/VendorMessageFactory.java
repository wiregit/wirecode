package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;
import com.limegroup.gnutella.util.IntHashMap;

public class VendorMessageFactory {
    
    private static final Log LOG = LogFactory.getLog(VendorMessageFactory.class);
    
    private static final Comparator COMPARATOR = new ByteArrayComparator();
    
    /** Map (VendorID -> Map (selector -> Parser)) */
    private static volatile Map VENDORS = new TreeMap(COMPARATOR);
    
    private static final BadPacketException UNRECOGNIZED_EXCEPTION =
        new BadPacketException("Unrecognized Vendor Message");
    
    static {
        setParser(VendorMessage.F_HOPS_FLOW, VendorMessage.F_BEAR_VENDOR_ID, new HopsFlowVendorMessageParser());
        setParser(VendorMessage.F_LIME_ACK, VendorMessage.F_LIME_VENDOR_ID, new LimeACKVendorMessageParser());
        setParser(VendorMessage.F_REPLY_NUMBER, VendorMessage.F_LIME_VENDOR_ID, new ReplyNumberVendorMessageParser());
        setParser(VendorMessage.F_TCP_CONNECT_BACK, VendorMessage.F_BEAR_VENDOR_ID, new TCPConnectBackVendorMessageParser());
        setParser(VendorMessage.F_MESSAGES_SUPPORTED, VendorMessage.F_NULL_VENDOR_ID, new MessagesSupportedVendorMessageParser());
        setParser(VendorMessage.F_UDP_CONNECT_BACK, VendorMessage.F_GTKG_VENDOR_ID, new UDPConnectBackVendorMessageParser());
        setParser(VendorMessage.F_PUSH_PROXY_REQ, VendorMessage.F_LIME_VENDOR_ID, new PushProxyRequestParser());
        setParser(VendorMessage.F_PUSH_PROXY_ACK, VendorMessage.F_LIME_VENDOR_ID, new PushProxyAcknowledgementParser());
        setParser(VendorMessage.F_LIME_ACK, VendorMessage.F_BEAR_VENDOR_ID, new QueryStatusRequestParser());
        setParser(VendorMessage.F_REPLY_NUMBER, VendorMessage.F_BEAR_VENDOR_ID, new QueryStatusResponseParser());
        setParser(VendorMessage.F_TCP_CONNECT_BACK, VendorMessage.F_LIME_VENDOR_ID, new TCPConnectBackRedirectParser());
        setParser(VendorMessage.F_UDP_CONNECT_BACK_REDIR, VendorMessage.F_LIME_VENDOR_ID, new UDPConnectBackRedirectParser());
        setParser(VendorMessage.F_CAPABILITIES, VendorMessage.F_NULL_VENDOR_ID, new CapabilitiesVMParser());
        setParser(VendorMessage.F_GIVE_STATS, VendorMessage.F_LIME_VENDOR_ID, new GiveStatsVendorMessageParser());
        setParser(VendorMessage.F_STATISTICS, VendorMessage.F_LIME_VENDOR_ID, new StatisticVendorMessageParser());
        setParser(VendorMessage.F_SIMPP_REQ, VendorMessage.F_LIME_VENDOR_ID, new SimppRequestVMParser());
        setParser(VendorMessage.F_SIMPP, VendorMessage.F_LIME_VENDOR_ID, new SimppVMParser());
        setParser(VendorMessage.F_GIVE_ULTRAPEER, VendorMessage.F_LIME_VENDOR_ID, new UDPCrawlerPingParser());
        setParser(VendorMessage.F_ULTRAPEER_LIST, VendorMessage.F_LIME_VENDOR_ID, new UDPCrawlerPongParser());
        setParser(VendorMessage.F_UDP_HEAD_PING, VendorMessage.F_LIME_VENDOR_ID, new HeadPingParser());
        setParser(VendorMessage.F_UDP_HEAD_PONG, VendorMessage.F_LIME_VENDOR_ID, new HeadPongParser());
        setParser(VendorMessage.F_UPDATE_REQ, VendorMessage.F_LIME_VENDOR_ID, new UpdateRequestParser());
        setParser(VendorMessage.F_UPDATE_RESP, VendorMessage.F_LIME_VENDOR_ID, new UpdateResponseParser());
        setParser(VendorMessage.F_CONTENT_REQ, VendorMessage.F_LIME_VENDOR_ID, new ContentRequestParser());
        setParser(VendorMessage.F_CONTENT_RESP, VendorMessage.F_LIME_VENDOR_ID, new ContentResponseParser());
        setParser(VendorMessage.F_HEADER_UPDATE, VendorMessage.F_LIME_VENDOR_ID, new HeaderUpdateVendorMessageParser());
    }
    
    public static void setParser(int selector, byte[] vendorId, VendorMessageParser parser) {
        if (selector < 0 || selector > 0xFFFF) {
            throw new IllegalArgumentException("Selector is out of range: " + selector);
        }
        
        if (vendorId == null) {
            throw new NullPointerException("Vendor ID is null");
        }
        
        if (vendorId.length != 4) {
            throw new IllegalArgumentException("Vendor ID must be 4 bytes long");
        }
        
        if (parser == null) {
            throw new NullPointerException("VendorMessageParser is null");
        }
        
        Object o = null;
        synchronized (VENDORS) {
            Map vendors = copyVendors();

            IntHashMap selectors = (IntHashMap)vendors.get(vendorId);
            if (selectors == null) {
                selectors = new IntHashMap();
                vendors.put(vendorId, selectors);
            }
            
            o = selectors.put(selector, parser);
            VENDORS = vendors;
        }
        
        if (o != null && LOG.isErrorEnabled()) {
            LOG.error("There was already a VendorMessageParser of type " 
                + o.getClass() + " registered for selector " + selector);
        }
    }
    
    /**
     * A helper method to create a deep copy of the VENDORS
     * TreeMap.
     */
    private static Map copyVendors() {
        Map copy = new TreeMap(COMPARATOR);
        for(Iterator it = VENDORS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            
            byte[] vendor = (byte[])entry.getKey();
            IntHashMap selectors = (IntHashMap)entry.getValue();
            
            copy.put(vendor, new IntHashMap(selectors));
        }
        return copy;
    }
    
    public static VendorMessageParser getParser(int selector, byte[] vendorId) {
        IntHashMap selectors = (IntHashMap)VENDORS.get(vendorId);
        if (selectors == null) {
            return null;
        }
        return (VendorMessageParser)selectors.get(selector);
    }
    
    public static VendorMessage deriveVendorMessage(byte[] guid, byte ttl,
            byte hops, byte[] fromNetwork, int network)
            throws BadPacketException {

        // sanity check
        if (fromNetwork.length < VendorMessage.LENGTH_MINUS_PAYLOAD) {
            ReceivedErrorStat.VENDOR_INVALID_PAYLOAD.incrementStat();
            throw new BadPacketException("Not enough bytes for a VM!!");
        }

        // get very necessary parameters....
        ByteArrayInputStream bais = new ByteArrayInputStream(fromNetwork);
        byte[] vendorID = null, restOf = null;
        int selector = -1, version = -1;
        try {
            // first 4 bytes are vendor ID
            vendorID = new byte[4];
            bais.read(vendorID, 0, vendorID.length);
            // get the selector....
            selector = ByteOrder.ushort2int(ByteOrder.leb2short(bais));
            // get the version....
            version = ByteOrder.ushort2int(ByteOrder.leb2short(bais));
            // get the rest....
            restOf = new byte[bais.available()];
            bais.read(restOf, 0, restOf.length);
        } catch (IOException ioe) {
            ErrorService.error(ioe); // impossible.
        }

        VendorMessageParser parser = getParser(selector, vendorID);
        if (parser == null) {
            ReceivedErrorStat.VENDOR_UNRECOGNIZED.incrementStat();
            throw UNRECOGNIZED_EXCEPTION;
        }
        
        return parser.parse(guid, ttl, hops, version, restOf, network);
    }
    
    /**
     * The interface for custom VendorMessageParser(s)
     */
    public static interface VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException;
    }
    
    // HOPS FLOW MESSAGE
    private static class HopsFlowVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new HopsFlowVendorMessage(guid, ttl, hops, version, restOf);
        }
    }
    
    // LIME ACK MESSAGE
    private static class LimeACKVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new LimeACKVendorMessage(guid, ttl, hops, version, restOf);
        }
    }
    
    // REPLY NUMBER MESSAGE
    private static class ReplyNumberVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new ReplyNumberVendorMessage(guid, ttl, hops, version, restOf);
        }
    }

    // TCP CONNECT BACK
    private static class TCPConnectBackVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new TCPConnectBackVendorMessage(guid, ttl, hops, version, restOf);
        }
    }
    
    // Messages Supported Message
    private static class MessagesSupportedVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new MessagesSupportedVendorMessage(guid, ttl, hops, version, restOf);
        }
    }

    // UDP CONNECT BACK
    private static class UDPConnectBackVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new UDPConnectBackVendorMessage(guid, ttl, hops, version, restOf);
        }
    }
    
    // Push Proxy Request
    private static class PushProxyRequestParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new PushProxyRequest(guid, ttl, hops, version, restOf);
        }
    }
    
    // Push Proxy Acknowledgement
    private static class PushProxyAcknowledgementParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new PushProxyAcknowledgement(guid, ttl, hops, version, restOf);
        }
    }
    
    // Query Status Request
    private static class QueryStatusRequestParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new QueryStatusRequest(guid, ttl, hops, version, restOf);
        }
    }
    
    // Query Status Response
    private static class QueryStatusResponseParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new QueryStatusResponse(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class TCPConnectBackRedirectParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new TCPConnectBackRedirect(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class UDPConnectBackRedirectParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new UDPConnectBackRedirect(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class CapabilitiesVMParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new CapabilitiesVM(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class GiveStatsVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new GiveStatsVendorMessage(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private static class StatisticVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new StatisticVendorMessage(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class SimppRequestVMParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new SimppRequestVM(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class SimppVMParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new SimppVM(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class UDPCrawlerPingParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new UDPCrawlerPing(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class UDPCrawlerPongParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new UDPCrawlerPong(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class HeadPingParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new HeadPing(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class HeadPongParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new HeadPong(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class UpdateRequestParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new UpdateRequest(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class UpdateResponseParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new UpdateResponse(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class ContentRequestParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new ContentRequest(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class ContentResponseParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new ContentResponse(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class HeaderUpdateVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new HeaderUpdateVendorMessage(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class ByteArrayComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            byte[] a = (byte[])o1;
            byte[] b = (byte[])o2;
            
            int d = 0;
            for(int i = 0; i < a.length; i++) {
                d = (a[i] & 0xFF) - (b[i] & 0xFF);
                if (d < 0) {
                    return -1;
                } else if (d > 0) {
                    return 1;
                }
            }
            
            return 0;
        }        
    }
}
