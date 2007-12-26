package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.limewire.io.IPPortCombo;
import org.limewire.io.InvalidDataException;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;

/** In Vendor Message parlance, the "message type" of this VMP is "BEAR/7".
 *  Used to ask a host you connect to do a TCP ConnectBack.
 */
public final class PushProxyAcknowledgement extends AbstractVendorMessage {

    public static final int VERSION = 2;

    /** The payload has 4 bytes dedicated to the IP of the proxy.
     */
    private final InetAddress _addr;

    /** The payload has a 16-bit unsigned value - the port - at which one should
     *  connect back.
     */
    private final int _port;

    /**
     * Constructs a new PushProxyAcknowledgement message with data from the
     * network.
     * @param network TODO
     */
    PushProxyAcknowledgement(byte[] guid, byte ttl, byte hops, int version, 
                             byte[] payload, Network network) 
        throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_PUSH_PROXY_ACK, version,
              payload, network);

        if (getVersion() == 1)
            throw new BadPacketException("DEPRECATED VERSION");

        if (getVersion() > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");

        if (getPayload().length != 6)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         payload.length);
        // get the ip and  port from the payload....
        try {
            IPPortCombo combo = 
                IPPortCombo.getCombo(getPayload());
            _addr = combo.getInetAddress();
            _port = combo.getPort();
        } catch(InvalidDataException ide) {
            throw new BadPacketException(ide);
        }
    }

    /**
     * Constructs a new PushProxyAcknowledgement message to be sent out.
     * @param addr The address of the person to connect back to.
     * @param port The port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     */
    public PushProxyAcknowledgement(InetAddress addr, int port) {
        super(F_LIME_VENDOR_ID, F_PUSH_PROXY_ACK, VERSION, 
              derivePayload(addr, port));
        _addr = addr;
        _port = port;
    }

    /**
     * Constructs a new PushProxyAcknowledgement message to be sent out.
     * @param addr The address of the person to connect back to.
     * @param port The port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     *  @param guid In case you want to set the guid (the PushProxy protocol
     *  advises this).
     */
    public PushProxyAcknowledgement(InetAddress addr, int port,
                                    GUID guid) {
        super(F_LIME_VENDOR_ID, F_PUSH_PROXY_ACK, VERSION, 
              derivePayload(addr, port));
        _addr = addr;
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
        return _addr;
    }

    private static byte[] derivePayload(InetAddress addr, int port) {
        try {
            // i do it during construction....
            IPPortCombo combo = 
                new IPPortCombo(addr.getHostAddress(), port);
            return combo.toBytes();
        } catch (UnknownHostException uhe) {
            throw new IllegalArgumentException(uhe.getMessage());
        }
    }

    /** Overridden purely for stats handling.
     */
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
    }

    /** Overridden purely for stats handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }


}
