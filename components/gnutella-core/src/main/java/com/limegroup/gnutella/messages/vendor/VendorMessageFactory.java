package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntHashMap;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;

/**
 * Factory to turn binary input as read from Network to VendorMessage
 * Objects.
 */
public class VendorMessageFactory {
    
    private static final Log LOG = LogFactory.getLog(VendorMessageFactory.class);
    
    private static final Comparator<byte[]> COMPARATOR = new ByteArrayComparator();
    
    /** Map (VendorID -> Map (selector -> Parser)) */
    private static volatile Map<byte[], IntHashMap<VendorMessageParser>> VENDORS =
        new TreeMap<byte[], IntHashMap<VendorMessageParser>>(COMPARATOR);
    
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
        setParser(VendorMessage.F_SIMPP_REQ, VendorMessage.F_LIME_VENDOR_ID, new SimppRequestVMParser());
        setParser(VendorMessage.F_SIMPP, VendorMessage.F_LIME_VENDOR_ID, new SimppVMParser());
        setParser(VendorMessage.F_CRAWLER_PING, VendorMessage.F_LIME_VENDOR_ID, new UDPCrawlerPingParser());
        setParser(VendorMessage.F_UDP_HEAD_PING, VendorMessage.F_LIME_VENDOR_ID, new HeadPingParser());
        setParser(VendorMessage.F_UDP_HEAD_PONG, VendorMessage.F_LIME_VENDOR_ID, new HeadPongParser());
        setParser(VendorMessage.F_UPDATE_REQ, VendorMessage.F_LIME_VENDOR_ID, new UpdateRequestParser());
        setParser(VendorMessage.F_UPDATE_RESP, VendorMessage.F_LIME_VENDOR_ID, new UpdateResponseParser());
        setParser(VendorMessage.F_CONTENT_REQ, VendorMessage.F_LIME_VENDOR_ID, new ContentRequestParser());
        setParser(VendorMessage.F_CONTENT_RESP, VendorMessage.F_LIME_VENDOR_ID, new ContentResponseParser());
        setParser(VendorMessage.F_HEADER_UPDATE, VendorMessage.F_LIME_VENDOR_ID, new HeaderUpdateVendorMessageParser());
        setParser(VendorMessage.F_OOB_PROXYING_CONTROL, VendorMessage.F_LIME_VENDOR_ID, new OOBProxyControlVendorMessageParser());
        setParser(VendorMessage.F_INSPECTION_REQ, VendorMessage.F_LIME_VENDOR_ID, new InspectionRequestVendorMessageParser());
        setParser(VendorMessage.F_ADVANCED_TOGGLE, VendorMessage.F_LIME_VENDOR_ID, new AdvancedStatsToggleVendorMessageParser());
        setParser(VendorMessage.F_DHT_CONTACTS, VendorMessage.F_LIME_VENDOR_ID, new DHTContactsMessageParser());
    }
    
    /**
     * Registers a VendorMessageParser under the provided selector (unsigned short) 
     * and Vendor ID.
     */
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
            Map<byte[], IntHashMap<VendorMessageParser>> vendors = copyVendors();

            IntHashMap<VendorMessageParser> selectors = vendors.get(vendorId);
            if (selectors == null) {
                selectors = new IntHashMap<VendorMessageParser>();
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
    
    /** A helper method to create a deep copy of the VENDORS TreeMap. */
    private static Map<byte[], IntHashMap<VendorMessageParser>> copyVendors() {
        Map<byte[], IntHashMap<VendorMessageParser>> copy =
            new TreeMap<byte[], IntHashMap<VendorMessageParser>>(COMPARATOR);
        
        for(Map.Entry<byte[], IntHashMap<VendorMessageParser>> entry : VENDORS.entrySet()) {
            copy.put(entry.getKey(), new IntHashMap<VendorMessageParser>(entry.getValue()));
        }
        
        return copy;
    }
    
    /**
     * Returns a VendorMessageParser for the provided selector 
     * and vendor ID or null if no such parser is registered.
     */
    public static VendorMessageParser getParser(int selector, byte[] vendorId) {
        IntHashMap<VendorMessageParser> selectors = VENDORS.get(vendorId);
        if (selectors == null) {
            return null;
        }
        
        return selectors.get(selector);
    }
    
    public static VendorMessage deriveVendorMessage(byte[] guid, byte ttl,
            byte hops, byte[] fromNetwork, Network network)
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
                byte[] restOf, Network network) throws BadPacketException;
    }
    
    // HOPS FLOW MESSAGE
    private static class HopsFlowVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new HopsFlowVendorMessage(guid, ttl, hops, version, restOf);
        }
    }
    
    // LIME ACK MESSAGE
    private static class LimeACKVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new LimeACKVendorMessage(guid, ttl, hops, version, restOf);
        }
    }
    
    // REPLY NUMBER MESSAGE
    private static class ReplyNumberVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new ReplyNumberVendorMessage(guid, ttl, hops, version, restOf);
        }
    }

    // TCP CONNECT BACK
    private static class TCPConnectBackVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new TCPConnectBackVendorMessage(guid, ttl, hops, version, restOf);
        }
    }
    
    // Messages Supported Message
    private static class MessagesSupportedVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new MessagesSupportedVendorMessage(guid, ttl, hops, version, restOf);
        }
    }

    // UDP CONNECT BACK
    private static class UDPConnectBackVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new UDPConnectBackVendorMessage(guid, ttl, hops, version, restOf);
        }
    }
    
    // Push Proxy Request
    private static class PushProxyRequestParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new PushProxyRequest(guid, ttl, hops, version, restOf);
        }
    }
    
    // Push Proxy Acknowledgement
    private static class PushProxyAcknowledgementParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new PushProxyAcknowledgement(guid, ttl, hops, version, restOf);
        }
    }
    
    // Query Status Request
    private static class QueryStatusRequestParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new QueryStatusRequest(guid, ttl, hops, version, restOf);
        }
    }
    
    // Query Status Response
    private static class QueryStatusResponseParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new QueryStatusResponse(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class TCPConnectBackRedirectParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new TCPConnectBackRedirect(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class UDPConnectBackRedirectParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new UDPConnectBackRedirect(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class CapabilitiesVMParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new CapabilitiesVM(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class SimppRequestVMParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new SimppRequestVM(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class SimppVMParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new SimppVM(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class UDPCrawlerPingParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new UDPCrawlerPing(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class HeadPingParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new HeadPing(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class HeadPongParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new HeadPong(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class UpdateRequestParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new UpdateRequest(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class UpdateResponseParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new UpdateResponse(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class ContentRequestParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new ContentRequest(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class ContentResponseParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new ContentResponse(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class HeaderUpdateVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new HeaderUpdateVendorMessage(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class OOBProxyControlVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version,
                byte[] restOf, Network network) throws BadPacketException {
            return new OOBProxyControlVendorMessage(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class InspectionRequestVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version,
                byte[] restOf, Network network) throws BadPacketException {
            return new InspectionRequest(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private static class AdvancedStatsToggleVendorMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version,
                byte[] restOf, Network network) throws BadPacketException {
            return new AdvancedStatsToggle(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private static class DHTContactsMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return new DHTContactsMessage(guid, ttl, hops, version, restOf);
        }
    }
    
    private static class ByteArrayComparator implements Comparator<byte[]> {
        public int compare(byte[] a, byte[] b) {
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
