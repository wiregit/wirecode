package com.limegroup.gnutella.messages.vendor;

import org.limewire.util.ByteOrder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.messages.BadPacketException;

@Singleton
public class ReplyNumberVendorMessageFactoryImpl implements ReplyNumberVendorMessageFactory {
    
    private final NetworkManager networkManager;
    
    @Inject
    public ReplyNumberVendorMessageFactoryImpl(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public ReplyNumberVendorMessage createFromNetwork(
            byte[] guid, byte ttl, byte hops, int version, byte[] payload)
            throws BadPacketException {
        return new ReplyNumberVendorMessage(guid, ttl, hops, version, payload);
    }

    public ReplyNumberVendorMessage create(GUID replyGUID, int numResults) {
        return new ReplyNumberVendorMessage(replyGUID, ReplyNumberVendorMessage.VERSION, numResults, derivePayload(numResults));
    }

    public ReplyNumberVendorMessage createV2ReplyNumberVendorMessage(GUID replyGUID, int numResults) {
        return new ReplyNumberVendorMessage(replyGUID, ReplyNumberVendorMessage.OLD_VERSION, numResults, derivePayload(numResults));
    }

    public ReplyNumberVendorMessage createV3ReplyNumberVendorMessage(GUID replyGUID, int numResults) {
        return new ReplyNumberVendorMessage(replyGUID, ReplyNumberVendorMessage.VERSION, numResults, derivePayload(numResults));
    }

    /** Constructs the payload from the desired number of results. */
    private byte[] derivePayload(int numResults) {
        if ((numResults < 1) || (numResults > 255))
            throw new IllegalArgumentException("Number of results too big: " +
                                               numResults);
        byte[] bytes = new byte[2];
        ByteOrder.short2leb((short) numResults, bytes, 0);
        bytes[1] = networkManager.canReceiveUnsolicited() ? ReplyNumberVendorMessage.UNSOLICITED : 0x0;
        
        return bytes;
    }
    
}
