package com.limegroup.mojito.messages;

import java.io.IOException;
import java.net.SocketAddress;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactory;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactory.VendorMessageParser;
import com.limegroup.mojito.io.InputOutputUtils;
import com.limegroup.mojito.io.MessageFormatException;

public class LimeDHTMessage extends VendorMessage {

    public static final int F_LIME_DHT_MESSAGE = 30;
    
    static {
        VendorMessageFactory.setParser(F_LIME_DHT_MESSAGE, F_LIME_VENDOR_ID, new LimeDHTMessageParser());
    }
    
    public LimeDHTMessage(byte[] guid, byte ttl, byte hops, byte[] vendorID, int selector, int version, byte[] payload, int network) throws BadPacketException {
        super(guid, ttl, hops, vendorID, selector, version, payload, network);
    }

    public LimeDHTMessage(byte[] guid, byte ttl, byte hops, byte[] vendorID, int selector, int version, byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, vendorID, selector, version, payload);
    }

    public LimeDHTMessage(byte[] vendorIDBytes, int selector, int version, byte[] payload, int network) {
        super(vendorIDBytes, selector, version, payload, network);
    }

    public LimeDHTMessage(byte[] vendorIDBytes, int selector, int version, byte[] payload) {
        super(vendorIDBytes, selector, version, payload);
    }
    
    public static LimeDHTMessage createMessage(DHTMessage msg) throws BadPacketException, IOException {
        byte[] payload = InputOutputUtils.serialize(msg);
        return createMessage(payload);
    }
    
    public static LimeDHTMessage createMessage(byte[] payload) throws BadPacketException, IOException {
        return new LimeDHTMessage(makeGuid(), (byte)1, (byte)0, F_LIME_VENDOR_ID, F_LIME_DHT_MESSAGE, 0, payload, N_UDP);
    }

    public DHTMessage getDHTMessage(SocketAddress src) throws MessageFormatException {
        return InputOutputUtils.deserialize(src, getPayload());
    }
    
    private static class LimeDHTMessageParser implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return new LimeDHTMessage(guid, ttl, hops, F_LIME_VENDOR_ID, F_LIME_DHT_MESSAGE, version, restOf);
        }
    }
}
