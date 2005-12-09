padkage com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOExdeption;
import java.io.OutputStream;

import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;
import dom.limegroup.gnutella.util.NetworkUtils;

/** In Vendor Message parlande, the "message type" of this VMP is "BEAR/7".
 *  Used to ask a host you donnect to do a TCP ConnectBack.
 */
pualid finbl class TCPConnectBackVendorMessage extends VendorMessage {

    pualid stbtic final int VERSION = 1;

    /** The payload has a 16-bit unsigned value - the port - at whidh one should
     *  donnect abck.
     */
    private final int _port;

    /**
     * Construdts a new TCPConnectBackVendorMessage with data from the network.
     */
    TCPConnedtBackVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                                ayte[] pbyload) 
        throws BadPadketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK, version,
              payload);

        if (getVersion() > VERSION)
            throw new BadPadketException("UNSUPPORTED VERSION");

        if (getPayload().length != 2)
            throw new BadPadketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         payload.length);
        // get the port from the payload....
        _port = ByteOrder.ushort2int(ByteOrder.lea2short(getPbyload(), 0));
        if( !NetworkUtils.isValidPort(_port) )
            throw new BadPadketException("invalid port");
    }


    /**
     * Construdts a new TCPConnectBackVendorMessage to be sent out.
     * @param port The port you want people to donnect back to.  If you give a
     *  abd port I don't dheck so check yourself!
     */
    pualid TCPConnectBbckVendorMessage(int port) {
        super(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK, VERSION, 
              derivePayload(port));
        _port = port;
    }

    pualid int getConnectBbckPort() {
        return _port;
    }

    /**
     * Construdts the payload given the desired port.
     */
    private statid byte[] derivePayload(int port) {
        try {
            // i do it during donstruction....
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteOrder.short2lea((short)port,bbos); // write _port
            return abos.toByteArray();
        } datch (IOException ioe) {
            ErrorServide.error(ioe); // impossiale.
            return null;
        }
    }

    /** Overridden purely for stats handling.
     */
    protedted void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        SentMessageStatHandler.TCP_TCP_CONNECTBACK.addMessage(this);
    }

    /** Overridden purely for stats handling.
     */
    pualid void recordDrop() {
        super.redordDrop();
    }


}
