package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.statistics.*;
import java.io.*;
import java.net.*;

/** In Vendor Message parlance, the "message type" of this VMP is "BEAR/7".
 *  Used to ask a host you connect to do a TCP ConnectBack.
 */
public final class PushProxyAcknowledgement extends VendorMessage {

    public static final int VERSION = 2;

    /** The payload has 4 bytes dedicated to the IP of the proxy.
     */
    private final InetAddress _addr;

    /** The payload has a 16-bit unsigned value - the port - at which one should
     *  connect back.
     */
    private final int _port;

    PushProxyAcknowledgement(byte[] guid, byte ttl, byte hops, int version, 
                             byte[] payload) 
        throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_PUSH_PROXY_ACK, version,
              payload);

        if (getVersion() == 1)
            throw new BadPacketException("DEPRECATED VERSION");

        if (getVersion() > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");

        if (getPayload().length != 6)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         payload.length);
        // get the ip and  port from the payload....
        QueryReply.IPPortCombo combo = 
            QueryReply.IPPortCombo.getCombo(getPayload());
        _addr = combo.getAddress();
        _port = combo.getPort();
    }


    /** @param port The port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     */
    public PushProxyAcknowledgement(InetAddress addr, int port) 
        throws BadPacketException {
        super(F_LIME_VENDOR_ID, F_PUSH_PROXY_ACK, VERSION, 
              derivePayload(addr, port));
        _addr = addr;
        _port = port;
    }

    /** @param port The port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     *  @param guid In case you want to set the guid (the PushProxy protocol
     *  advises this).
     */
    public PushProxyAcknowledgement(InetAddress addr, int port,
                                    GUID guid) throws BadPacketException {
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

    private static byte[] derivePayload(InetAddress addr, int port) 
        throws BadPacketException{
        try {
            // i do it during construction....
            QueryReply.IPPortCombo combo = 
                new QueryReply.IPPortCombo(addr.getHostAddress(), port);
            return combo.toBytes();
        }
        catch (UnknownHostException uhe) {
            // this should never happen!!!
            ErrorService.error(uhe);
            throw new BadPacketException("Bad host - should never happen!!!");
        }
    }

    /** Overridden purely for stats handling.
     */
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        if (RECORD_STATS)
            ;
    }

    /** Overridden purely for stats handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }


}
