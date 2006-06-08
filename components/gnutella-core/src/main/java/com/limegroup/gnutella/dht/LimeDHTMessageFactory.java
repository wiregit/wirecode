package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.Collection;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.messages.FindNodeRequest;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.MessageFactory;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.StatsRequest;
import com.limegroup.mojito.messages.StatsResponse;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.messages.StoreResponse;

class LimeDHTMessageFactory implements MessageFactory {

    public FindNodeRequest createFindNodeRequest(int vendor, int version, 
            ContactNode node, KUID messageId, KUID lookupId) {
        return new LimeDHTMessage2.FindNodeRequestImpl(vendor, version, node, messageId, lookupId);
    }

    public FindNodeResponse createFindNodeResponse(int vendor, int version, 
            ContactNode node, KUID messageId, QueryKey queryKey, Collection nodes) {
        return new LimeDHTMessage2.FindNodeResponseImpl(vendor, version, node, messageId, queryKey, nodes);
    }

    public FindValueRequest createFindValueRequest(int vendor, int version, 
            ContactNode node, KUID messageId, KUID lookupId) {
        return new LimeDHTMessage2.LimeFindValueRequestImpl(vendor, version, node, messageId, lookupId);
    }

    public FindValueResponse createFindValueResponse(int vendor, int version, 
            ContactNode node, KUID messageId, Collection values) {
        return new LimeDHTMessage2.FindValueResponseImpl(vendor, version, node, messageId, values);
    }

    public PingRequest createPingRequest(int vendor, int version, 
            ContactNode node, KUID messageId) {
        return new LimeDHTMessage2.PingRequestImpl(vendor, version, node, messageId);
    }

    public PingResponse createPingResponse(int vendor, int version, 
            ContactNode node, KUID messageId, SocketAddress externalAddress, int estimatedSize) {
        return new LimeDHTMessage2.PingResponseImpl(vendor, version, node, messageId, externalAddress, estimatedSize);
    }

    public StatsRequest createStatsRequest(int vendor, int version, 
            ContactNode node, KUID messageId, int stats) {
        return new LimeDHTMessage2.StatsRequestImpl(vendor, version, node, messageId, stats);
    }

    public StatsResponse createStatsResponse(int vendor, int version, 
            ContactNode node, KUID messageId, String stats) {
        return new LimeDHTMessage2.StatsResponseImpl(vendor, version, node, messageId, stats);
    }

    public StoreRequest createStoreRequest(int vendor, int version, 
            ContactNode node, KUID messageId, QueryKey queryKey, KeyValue keyValue) {
        return new LimeDHTMessage2.StoreRequestImpl(vendor, version, node, messageId, queryKey, keyValue);
    }

    public StoreResponse createStoreResponse(int vendor, int version, 
            ContactNode node, KUID messageId, KUID valueId, int response) {
        return new LimeDHTMessage2.StoreResponseImpl(vendor, version, node, messageId, valueId, response);
    }
}
