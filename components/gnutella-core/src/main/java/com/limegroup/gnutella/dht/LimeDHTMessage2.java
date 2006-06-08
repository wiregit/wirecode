package com.limegroup.gnutella.dht;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.util.Collection;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.FindNodeRequest;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.messages.StatsRequest;
import com.limegroup.mojito.messages.StatsResponse;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.messages.StoreResponse;

public abstract class LimeDHTMessage2 extends Message implements DHTMessage {
    
    private static final byte F_DHT_MESSAGE = (byte)0x43;
    
    private int opcode;
    private int vendor;
    private int version;
    private ContactNode contactNode;
    private KUID messageId;
    
    private byte[] payload;
    
    public LimeDHTMessage2(byte func, byte ttl, int length, int network) {
        super(F_DHT_MESSAGE, ttl, length, network);
    }

    public LimeDHTMessage2(byte func, byte ttl, int length) {
        super(F_DHT_MESSAGE, ttl, length);
    }

    public LimeDHTMessage2(byte[] guid, byte func, byte ttl, byte hops, int length, int network) {
        super(guid, F_DHT_MESSAGE, ttl, hops, length, network);
    }

    public LimeDHTMessage2(byte[] guid, byte func, byte ttl, byte hops, int length) {
        super(guid, F_DHT_MESSAGE, ttl, hops, length);
    }

    public LimeDHTMessage2(int opcode, int vendor, int version, 
            ContactNode contactNode, KUID messageId) {
        super(makeGuid(), F_DHT_MESSAGE, (byte)0x01, (byte)0x00, 0, N_UNKNOWN);
        
        this.opcode = opcode;
        this.vendor = vendor;
        this.version = version;
        this.contactNode = contactNode;
        this.messageId = messageId;
    }
    
    protected void serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(640);
        MessageOutputStream out = new MessageOutputStream(baos);
        out.write(this);
        out.close();
        
        payload = baos.toByteArray();
        updateLength(payload.length);
    }
    
    /*public void write(OutputStream out) throws IOException {
        super.write(out);
    }*/
    
    public void write(OutputStream out, byte[] buf) throws IOException {
        serialize();
        super.write(out, buf);
    }

    public void writeQuickly(OutputStream out) throws IOException {
        serialize();
        super.writeQuickly(out);
    }

    protected void writePayload(OutputStream out) throws IOException {
        out.write(payload, 0, payload.length);
    }
    
    public void recordDrop() {
    }

    public Message stripExtendedPayload() {
        return this;
    }

    public int getOpCode() {
        return opcode;
    }

    public ContactNode getContactNode() {
        return contactNode;
    }

    public KUID getMessageID() {
        return messageId;
    }

    public int getVendor() {
        return vendor;
    }

    public int getVersion() {
        return version;
    }

    private static class LimeDHTRequestMessage extends LimeDHTMessage2
            implements RequestMessage {

        private int secureStatus = INSECURE;
        
        private byte[] signature;
        
        public LimeDHTRequestMessage(int opcode, int vendor, int version,
                ContactNode contactNode, KUID messageId) {
            super(opcode, vendor, version, contactNode, messageId);
        }
        
        public void setSecureStatus(int secureStatus) {
            this.secureStatus = secureStatus;
        }
        
        public int getSecureStatus() {
            return secureStatus;
        }

        public byte[] getSignature() {
            return signature;
        }

        public boolean isSecure() {
            return (secureStatus == SECURE);
        }

        public boolean isSigned() {
            return signature != null;
        }

        public void setSignature(byte[] signature) {
            this.signature = signature;
        }
    }
    
    private static class LimeDHTResponseMessage extends LimeDHTMessage2
            implements ResponseMessage {
        
        public LimeDHTResponseMessage(int opcode, int vendor, int version,
                ContactNode contactNode, KUID messageId) {
            super(opcode, vendor, version, contactNode, messageId);
        }
    }
    
    static class PingRequestImpl extends LimeDHTRequestMessage 
            implements PingRequest {
        
        public PingRequestImpl(int vendor, int version, 
                ContactNode contactNode, KUID messageId) {
            super(PING_REQUEST, vendor, version, contactNode, messageId);
        }
    }
    
    static class PingResponseImpl extends LimeDHTResponseMessage
            implements PingResponse {
        
        private SocketAddress externalAddress;
        private int estimatedSize;
        
        PingResponseImpl(int opcode, int vendor, int version, 
                ContactNode contactNode, KUID messageId, 
                SocketAddress externalAddress, int estiamtedSize) {
            super(PING_RESPONSE, vendor, version, contactNode, messageId);
            
            this.externalAddress = externalAddress;
            this.estimatedSize = estiamtedSize;
        }
        
        public int getEstimatedSize() {
            return estimatedSize;
        }
        
        public SocketAddress getExternalAddress() {
            return externalAddress;
        }
    }
    
    static class StoreRequestImpl extends LimeDHTRequestMessage
            implements StoreRequest {

        private QueryKey queryKey;
        private KeyValue keyValue;
        
        public StoreRequestImpl(int vendor, int version,
                ContactNode contactNode, KUID messageId,
                QueryKey queryKey, KeyValue keyValue) {
            super(STORE_REQUEST, vendor, version, contactNode, messageId);
            
            this.queryKey = queryKey;
            this.keyValue = keyValue;
        }
        
        public QueryKey getQueryKey() {
            return queryKey;
        }
        
        public KeyValue getKeyValue() {
            return keyValue;
        }
    }
    
    static class StoreResponseImpl extends LimeDHTResponseMessage
            implements StoreResponse {
        
        private KUID valueId;
        private int status;
        
        public StoreResponseImpl(int vendor, int version, 
                ContactNode contactNode, KUID messageId,
                KUID valueId, int status) {
            super(STORE_RESPONSE, vendor, version, contactNode, messageId);
            
            this.valueId = valueId;
            this.status = status;
        }
        
        public KUID getValueID() {
            return valueId;
        }
        
        public int getStatus() {
            return status;
        }
    }
    
    static class FindNodeRequestImpl extends LimeDHTRequestMessage 
            implements FindNodeRequest {
        
        private KUID lookupId;
        
        FindNodeRequestImpl(int vendor, int version, 
                ContactNode contactNode, KUID messageId, 
                KUID lookupId) {
            super(FIND_NODE_REQUEST, vendor, version, contactNode, messageId);
            
            this.lookupId = lookupId;
        }
        
        public KUID getLookupID() {
            return lookupId;
        }
    }
    
    static class FindNodeResponseImpl extends LimeDHTResponseMessage
            implements FindNodeResponse {

        private Collection nodes;
        private QueryKey queryKey;
        
        FindNodeResponseImpl(int vendor, int version,
                ContactNode contactNode, KUID messageId,
                QueryKey queryKey, Collection nodes) {
            super(FIND_NODE_RESPONSE, vendor, version, contactNode, messageId);
            
            this.queryKey = queryKey;
            this.nodes = nodes;
        }
        
        public QueryKey getQueryKey() {
            return queryKey;
        }
        
        public Collection getNodes() {
            return nodes;
        }
    }
    
    static class FindValueRequestImpl extends LimeDHTRequestMessage
            implements FindValueRequest {
        
        private KUID lookupId;
        
        FindValueRequestImpl(int vendor, int version,
                ContactNode contactNode, KUID messageId,
                KUID lookupId) {
            super(FIND_VALUE_REQUEST, vendor, version, contactNode, messageId);
            
            this.lookupId = lookupId;
        }
        
        public KUID getLookupID() {
            return lookupId;
        }
    }
    
    static class FindValueResponseImpl extends LimeDHTResponseMessage 
            implements FindValueResponse {

        private Collection values;

        FindValueResponseImpl(int vendor, int version, ContactNode contactNode,
                KUID messageId, Collection values) {
            super(FIND_VALUE_RESPONSE, vendor, version, contactNode, messageId);

            this.values = values;
        }

        public Collection getValues() {
            return values;
        }
    }
    
    static class StatsRequestImpl extends LimeDHTRequestMessage
            implements StatsRequest {

        private int request;
        
        StatsRequestImpl(int vendor, int version, 
                ContactNode contactNode, KUID messageId, int request) {
            super(STATS_REQUEST, vendor, version, contactNode, messageId);
            
            this.request = request;
        }

        public int getRequest() {
            return request;
        }
    }
    
    static class StatsResponseImpl extends LimeDHTResponseMessage
            implements StatsResponse {

        private String stats;
        
        public StatsResponseImpl(int vendor, int version, 
                ContactNode contactNode, KUID messageId, String stats) {
            super(STATS_RESPONSE, vendor, version, contactNode, messageId);
            
            this.stats = stats;
        }

        public String getStatistics() {
            return stats;
        }
    }
}
