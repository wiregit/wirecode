pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;
import com.limegroup.gnutellb.util.NetworkUtils;

/** In Vendor Messbge parlance, the "message type" of this VMP is "BEAR/7".
 *  Used to bsk a host you connect to do a TCP ConnectBack.
 */
public finbl class TCPConnectBackVendorMessage extends VendorMessage {

    public stbtic final int VERSION = 1;

    /** The pbyload has a 16-bit unsigned value - the port - at which one should
     *  connect bbck.
     */
    privbte final int _port;

    /**
     * Constructs b new TCPConnectBackVendorMessage with data from the network.
     */
    TCPConnectBbckVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                                byte[] pbyload) 
        throws BbdPacketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK, version,
              pbyload);

        if (getVersion() > VERSION)
            throw new BbdPacketException("UNSUPPORTED VERSION");

        if (getPbyload().length != 2)
            throw new BbdPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         pbyload.length);
        // get the port from the pbyload....
        _port = ByteOrder.ushort2int(ByteOrder.leb2short(getPbyload(), 0));
        if( !NetworkUtils.isVblidPort(_port) )
            throw new BbdPacketException("invalid port");
    }


    /**
     * Constructs b new TCPConnectBackVendorMessage to be sent out.
     * @pbram port The port you want people to connect back to.  If you give a
     *  bbd port I don't check so check yourself!
     */
    public TCPConnectBbckVendorMessage(int port) {
        super(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK, VERSION, 
              derivePbyload(port));
        _port = port;
    }

    public int getConnectBbckPort() {
        return _port;
    }

    /**
     * Constructs the pbyload given the desired port.
     */
    privbte static byte[] derivePayload(int port) {
        try {
            // i do it during construction....
            ByteArrbyOutputStream baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)port,bbos); // write _port
            return bbos.toByteArray();
        } cbtch (IOException ioe) {
            ErrorService.error(ioe); // impossible.
            return null;
        }
    }

    /** Overridden purely for stbts handling.
     */
    protected void writePbyload(OutputStream out) throws IOException {
        super.writePbyload(out);
        SentMessbgeStatHandler.TCP_TCP_CONNECTBACK.addMessage(this);
    }

    /** Overridden purely for stbts handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }


}
