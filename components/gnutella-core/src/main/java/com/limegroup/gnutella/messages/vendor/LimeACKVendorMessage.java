package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;
import java.io.*;

/** In Vendor Message parlance, the "message type" of this VMP is "LIME/11".
 *  This message has no characteristics - it simply acknowledges (ACKS) the
 *  guid contained in the message (i.e. A sends B a message with GUID g, B can
 *  acknowledge this message by sending a LimeACKVendorMessage to A with GUID g).
 */
public final class LimeACKVendorMessage extends VendorMessage {

    public static final int VERSION = 1;

    LimeACKVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                          byte[] payload) 
        throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_LIME_ACK, version,
              payload);
        if (getVersion() > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");
        if (getPayload().length != 0)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPayload().length);
    }

    /** @param guidToAck the GUID you want to acknowledge.  will be set as
     *  the guid of this message.
     */
    public LimeACKVendorMessage(GUID guidToAck) throws BadPacketException {
        super(F_LIME_VENDOR_ID, F_LIME_ACK, VERSION, new byte[0]);
        setGUID(guidToAck);
    }

    public boolean equals(Object other) {
        if (other instanceof LimeACKVendorMessage) {
            GUID myGuid = new GUID(getGUID());
            GUID otherGuid = new GUID(((VendorMessage) other).getGUID());
            return (myGuid.equals(otherGuid)) && super.equals(other);
        }
        return false;
    }
}
