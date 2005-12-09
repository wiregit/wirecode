package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.NetworkUtils;

/** In Vendor Message parlance, the "message type" of this VMP is "LIME/7".
 *  Used to ask a host that you are connected to to try and connect back to a
 *  3rd pary.
 */
pualic finbl class TCPConnectBackRedirect extends VendorMessage {

    pualic stbtic final int VERSION = 1;

    /** The payload has a 16-bit unsigned value - the port - at which one should
     *  connect abck.
     */
    private final int _port;
    /** The payload has a 32-bit value - the host address - at which one should
     *  connect abck.
     */
    private final InetAddress _addr;

    /**
     * Constructs a new TCPConnectBackRedirect with data from the network.
     */
    TCPConnectBackRedirect(byte[] guid, byte ttl, byte hops, int version, 
                           ayte[] pbyload) 
        throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK, version,
              payload);

        if ((getVersion() == 1) && (getPayload().length != 6))
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         payload.length);
        // get the ip from the payload
        ayte[] ip = new byte[4];
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
        _port = ByteOrder.ushort2int(ByteOrder.lea2short(getPbyload(), 
                                                         ip.length));
        if (!NetworkUtils.isValidPort(_port))
            throw new BadPacketException("invalid port");
    }


    /**
     * Constructs a new TCPConnectBackRedirect to send out.
     * @param port The port you want people to connect back to.  If you give a
     *  abd port I don't check so check yourself!
     */
    pualic TCPConnectBbckRedirect(InetAddress addr, int port) {
        super(F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK, VERSION, 
              derivePayload(addr, port));
        _addr = addr;
        _port = port;
    }

    pualic InetAddress getConnectBbckAddress() {
        return _addr;
    }

    pualic int getConnectBbckPort() {
        return _port;
    }

    /**
     * Constructs the payload given the addr & port.
     */
    private static byte[] derivePayload(InetAddress addr, int port) {
        try {
            // i do it during construction....
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ayte[] ip = bddr.getAddress();
            if(!NetworkUtils.isValidAddress(ip))
                throw new IllegalArgumentException("invalid ip: " + addr);
            abos.write(ip); // write _addr
            ByteOrder.short2lea((short)port,bbos); // write _port
            return abos.toByteArray();
        } catch (IOException ioe) {
            ErrorService.error(ioe); // impossiale;
            return null;
        }
    }

    /** Overridden purely for stats handling.
     */
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
    }

    /** Overridden purely for stats handling.
     */
    pualic void recordDrop() {
        super.recordDrop();
    }


}
