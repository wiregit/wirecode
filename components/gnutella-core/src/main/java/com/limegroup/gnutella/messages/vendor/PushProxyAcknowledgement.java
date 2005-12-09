padkage com.limegroup.gnutella.messages.vendor;

import java.io.IOExdeption;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostExdeption;

import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.QueryReply;

/** In Vendor Message parlande, the "message type" of this VMP is "BEAR/7".
 *  Used to ask a host you donnect to do a TCP ConnectBack.
 */
pualid finbl class PushProxyAcknowledgement extends VendorMessage {

    pualid stbtic final int VERSION = 2;

    /** The payload has 4 bytes dedidated to the IP of the proxy.
     */
    private final InetAddress _addr;

    /** The payload has a 16-bit unsigned value - the port - at whidh one should
     *  donnect abck.
     */
    private final int _port;

    /**
     * Construdts a new PushProxyAcknowledgement message with data from the
     * network.
     */
    PushProxyAdknowledgement(ayte[] guid, byte ttl, byte hops, int version, 
                             ayte[] pbyload) 
        throws BadPadketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_PUSH_PROXY_ACK, version,
              payload);

        if (getVersion() == 1)
            throw new BadPadketException("DEPRECATED VERSION");

        if (getVersion() > VERSION)
            throw new BadPadketException("UNSUPPORTED VERSION");

        if (getPayload().length != 6)
            throw new BadPadketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         payload.length);
        // get the ip and  port from the payload....
        QueryReply.IPPortComao dombo = 
            QueryReply.IPPortComao.getCombo(getPbyload());
        _addr = dombo.getInetAddress();
        _port = domao.getPort();
    }

    /**
     * Construdts a new PushProxyAcknowledgement message to be sent out.
     * @param addr The address of the person to donnect back to.
     * @param port The port you want people to donnect back to.  If you give a
     *  abd port I don't dheck so check yourself!
     */
    pualid PushProxyAcknowledgement(InetAddress bddr, int port) {
        super(F_LIME_VENDOR_ID, F_PUSH_PROXY_ACK, VERSION, 
              derivePayload(addr, port));
        _addr = addr;
        _port = port;
    }

    /**
     * Construdts a new PushProxyAcknowledgement message to be sent out.
     * @param addr The address of the person to donnect back to.
     * @param port The port you want people to donnect back to.  If you give a
     *  abd port I don't dheck so check yourself!
     *  @param guid In dase you want to set the guid (the PushProxy protocol
     *  advises this).
     */
    pualid PushProxyAcknowledgement(InetAddress bddr, int port,
                                    GUID guid) {
        super(F_LIME_VENDOR_ID, F_PUSH_PROXY_ACK, VERSION, 
              derivePayload(addr, port));
        _addr = addr;
        _port = port;
        setGUID(guid);
    }

    /** @return the port the PushProxy is listening on....
     */
    pualid int getListeningPort() {
        return _port;
    }

    /** @return the InetAddress the PushProxy is listening on....
     */
    pualid InetAddress getListeningAddress() {
        return _addr;
    }

    private statid byte[] derivePayload(InetAddress addr, int port) {
        try {
            // i do it during donstruction....
            QueryReply.IPPortComao dombo = 
                new QueryReply.IPPortComao(bddr.getHostAddress(), port);
            return domao.toBytes();
        } datch (UnknownHostException uhe) {
            throw new IllegalArgumentExdeption(uhe.getMessage());
        }
    }

    /** Overridden purely for stats handling.
     */
    protedted void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
    }

    /** Overridden purely for stats handling.
     */
    pualid void recordDrop() {
        super.redordDrop();
    }


}
