
// Commented for the Learning branch

package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.DataUtils;

/**
 * Not used.
 * 
 * LimeWire doesn't send or receive BEAR 11 version 1 QueryStatusRequest messages.
 * But, we still list BEAR 11 1 in our MesagesSupportedVendorMessage.
 * Connection.remoteHostSupportsLeafGuidance() checks a remote computer's MessagesSupportedVendorMessage to see if it's listed.
 * We're not using the packet, but we're using its name to indicate support for the feature of dynamic querying.
 * 
 * TODO:kfaaborg Remove this class, which isn't used.
 * 
 * In Vendor Message parlance, the "message type" of this message is "BEAR/11".
 * Sent to a servent (a leaf usually) to inquire about the status of a query
 * as denoted by the GUID of this message.
 * This message has no payload - we simply set the client guid as the GUID of
 * the message.
 */
public final class QueryStatusRequest extends VendorMessage {

    public static final int VERSION = 1;

    QueryStatusRequest(byte[] guid, byte ttl, byte hops, int version, byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_LIME_ACK, version, payload);
        if (getVersion() > VERSION) throw new BadPacketException("UNSUPPORTED VERSION");
    }

    public QueryStatusRequest(GUID guid) {
        super(F_BEAR_VENDOR_ID, F_LIME_ACK, VERSION, DataUtils.EMPTY_BYTE_ARRAY);
        setGUID(guid);
    }

    public GUID getQueryGUID() {
        return new GUID(getGUID());
    }

    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
    }

    public void recordDrop() {
        super.recordDrop();
    }
}
