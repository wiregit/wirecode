package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.*;
import com.limegroup.gnutella.util.NetworkUtils;
import java.io.*;
import java.net.*;

/** In Vendor Message parlance, the "message type" of this VMP is "LIME/7".
 *  Used to ask a host that you are connected to to try and connect back to a
 *  3rd pary.
 */
public final class TCPConnectBackRedirect extends VendorMessage {

    public static final int VERSION = 1;

    /** The payload has a 16-bit unsigned value - the port - at which one should
     *  connect back.
     */
    private final int _port;
    /** The payload has a 32-bit value - the host address - at which one should
     *  connect back.
     */
    private final InetAddress _addr;

    TCPConnectBackRedirect(byte[] guid, byte ttl, byte hops, int version, 
                           byte[] payload) 
        throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK, version,
              payload);

        if ((getVersion() == 1) && (getPayload().length != 6))
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         payload.length);
        // get the ip from the payload
        byte[] ip = new byte[4];
        System.arraycopy(getPayload(), 0, ip, 0, ip.length);
        if (!NetworkUtils.isValidAddress(ip))
            throw new BadPacketException("Bad Host!!");
        try {
            _addr = InetAddress.getByName(NetworkUtils.ip2string(ip));
        }
        catch (UnknownHostException uhe) {
            throw new BadPacketException("Bad InetAddress!!");
        }

        // get the port from the payload....
        _port = ByteOrder.ubytes2int(ByteOrder.leb2short(getPayload(), 
                                                         ip.length));
        if (!NetworkUtils.isValidPort(_port))
            throw new BadPacketException("invalid port");
    }


    /** @param port The port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     */
    public TCPConnectBackRedirect(InetAddress addr, 
                                  int port) throws BadPacketException {
        super(F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK, VERSION, 
              derivePayload(addr, port));
        _addr = addr;
        _port = port;
    }

    public InetAddress getConnectBackAddress() {
        return _addr;
    }

    public int getConnectBackPort() {
        return _port;
    }

    private static byte[] derivePayload(InetAddress addr, int port) 
        throws BadPacketException {
        try {
            // i do it during construction....
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] ip = addr.getAddress();
            if (ip.length != 4) throw new BadPacketException("Bad IP");
            baos.write(ip); // write _addr
            ByteOrder.short2leb((short)port,baos); // write _port
            return baos.toByteArray();
        }
        catch (IOException ioe) {
            throw new BadPacketException("Couldn't write to a ByteStream!!!");
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
