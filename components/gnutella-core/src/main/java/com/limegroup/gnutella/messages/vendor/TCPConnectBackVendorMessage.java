
// Edited for the Learning branch

package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.util.NetworkUtils;

/** In Vendor Message parlance, the "message type" of this VMP is "BEAR/7".
 *  Used to ask a host you connect to do a TCP ConnectBack.
 */
public final class TCPConnectBackVendorMessage extends VendorMessage {

    /** 1, LimeWire understands the initial version of the TCP Connect Back vendor message. */
    public static final int VERSION = 1;

    /** The payload has a 16-bit unsigned value - the port - at which one should
     *  connect back.
     */
    private final int _port;

    /**
     * Constructs a new TCPConnectBackVendorMessage with data from the network.
     */
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
        _port = ByteOrder.ushort2int(ByteOrder.leb2short(getPayload(), 0));
        if( !NetworkUtils.isValidPort(_port) )
            throw new BadPacketException("invalid port");
    }


    /**
     * Constructs a new TCPConnectBackVendorMessage to be sent out.
     * @param port The port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     */
    public TCPConnectBackVendorMessage(int port) {
        super(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK, VERSION, 
              derivePayload(port));
        _port = port;
    }

    public int getConnectBackPort() {
        return _port;
    }

    /**
     * Constructs the payload given the desired port.
     */
    private static byte[] derivePayload(int port) {
        try {
            // i do it during construction....
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)port,baos); // write _port
            return baos.toByteArray();
        } catch (IOException ioe) {
            ErrorService.error(ioe); // impossible.
            return null;
        }
    }

    /** Overridden purely for stats handling.
     */
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        SentMessageStatHandler.TCP_TCP_CONNECTBACK.addMessage(this);
    }

    /** Overridden purely for stats handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }


}
