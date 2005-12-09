pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.net.InetAddress;
import jbva.net.UnknownHostException;

import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.QueryReply;

/** In Vendor Messbge parlance, the "message type" of this VMP is "BEAR/7".
 *  Used to bsk a host you connect to do a TCP ConnectBack.
 */
public finbl class PushProxyAcknowledgement extends VendorMessage {

    public stbtic final int VERSION = 2;

    /** The pbyload has 4 bytes dedicated to the IP of the proxy.
     */
    privbte final InetAddress _addr;

    /** The pbyload has a 16-bit unsigned value - the port - at which one should
     *  connect bbck.
     */
    privbte final int _port;

    /**
     * Constructs b new PushProxyAcknowledgement message with data from the
     * network.
     */
    PushProxyAcknowledgement(byte[] guid, byte ttl, byte hops, int version, 
                             byte[] pbyload) 
        throws BbdPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_PUSH_PROXY_ACK, version,
              pbyload);

        if (getVersion() == 1)
            throw new BbdPacketException("DEPRECATED VERSION");

        if (getVersion() > VERSION)
            throw new BbdPacketException("UNSUPPORTED VERSION");

        if (getPbyload().length != 6)
            throw new BbdPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         pbyload.length);
        // get the ip bnd  port from the payload....
        QueryReply.IPPortCombo combo = 
            QueryReply.IPPortCombo.getCombo(getPbyload());
        _bddr = combo.getInetAddress();
        _port = combo.getPort();
    }

    /**
     * Constructs b new PushProxyAcknowledgement message to be sent out.
     * @pbram addr The address of the person to connect back to.
     * @pbram port The port you want people to connect back to.  If you give a
     *  bbd port I don't check so check yourself!
     */
    public PushProxyAcknowledgement(InetAddress bddr, int port) {
        super(F_LIME_VENDOR_ID, F_PUSH_PROXY_ACK, VERSION, 
              derivePbyload(addr, port));
        _bddr = addr;
        _port = port;
    }

    /**
     * Constructs b new PushProxyAcknowledgement message to be sent out.
     * @pbram addr The address of the person to connect back to.
     * @pbram port The port you want people to connect back to.  If you give a
     *  bbd port I don't check so check yourself!
     *  @pbram guid In case you want to set the guid (the PushProxy protocol
     *  bdvises this).
     */
    public PushProxyAcknowledgement(InetAddress bddr, int port,
                                    GUID guid) {
        super(F_LIME_VENDOR_ID, F_PUSH_PROXY_ACK, VERSION, 
              derivePbyload(addr, port));
        _bddr = addr;
        _port = port;
        setGUID(guid);
    }

    /** @return the port the PushProxy is listening on....
     */
    public int getListeningPort() {
        return _port;
    }

    /** @return the InetAddress the PushProxy is listening on....
     */
    public InetAddress getListeningAddress() {
        return _bddr;
    }

    privbte static byte[] derivePayload(InetAddress addr, int port) {
        try {
            // i do it during construction....
            QueryReply.IPPortCombo combo = 
                new QueryReply.IPPortCombo(bddr.getHostAddress(), port);
            return combo.toBytes();
        } cbtch (UnknownHostException uhe) {
            throw new IllegblArgumentException(uhe.getMessage());
        }
    }

    /** Overridden purely for stbts handling.
     */
    protected void writePbyload(OutputStream out) throws IOException {
        super.writePbyload(out);
    }

    /** Overridden purely for stbts handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }


}
