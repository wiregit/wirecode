package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.messages.BadPacketException;
import java.io.*;

/** In Vendor Message parlance, the "message type" of this VMP is "BEAR/7".
 *  Used to ask a host you connect to do a TCP ConnectBack.
 */
public final class TCPConnectBackVendorMessage extends VendorMessage {

    public static final int VERSION = 1;

    /** The payload has a 16-bit unsigned value - the port - at which one should
     *  connect back.
     */
    private final int _port;

    TCPConnectBackVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                                byte[] payload) 
        throws BadPacketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK, version,
              payload);

        if (getVersion() > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");

        if (getPayload().length != 2)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         payload.length);
        // get the port from the payload....
        _port = ByteOrder.ubytes2int(ByteOrder.leb2short(getPayload(), 0));
    }


    /** @param port The port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     */
    public TCPConnectBackVendorMessage(int port) throws BadPacketException {
        super(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK, VERSION, 
              derivePayload(port));
        _port = port;
    }

    public int getConnectBackPort() {
        return _port;
    }

    private static byte[] derivePayload(int port) throws BadPacketException{
        try {
            // i do it during construction....
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)port,baos); // write _port
            return baos.toByteArray();
        }
        catch (IOException ioe) {
            // this should never happen!!!
            ioe.printStackTrace();
            throw new BadPacketException("Couldn't write to a ByteStream!!!");
        }
    }
}
