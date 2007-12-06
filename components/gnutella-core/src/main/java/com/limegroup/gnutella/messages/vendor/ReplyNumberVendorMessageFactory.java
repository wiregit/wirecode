package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message.Network;

public interface ReplyNumberVendorMessageFactory {

    public ReplyNumberVendorMessage createFromNetwork(byte[] guid, byte ttl,
            byte hops, int version, byte[] payload, Network network) throws BadPacketException;

    public ReplyNumberVendorMessage create(GUID replyGUID, int numResults);

    public ReplyNumberVendorMessage createV2ReplyNumberVendorMessage(
            GUID replyGUID, int numResults);

    public ReplyNumberVendorMessage createV3ReplyNumberVendorMessage(
            GUID replyGUID, int numResults);

}