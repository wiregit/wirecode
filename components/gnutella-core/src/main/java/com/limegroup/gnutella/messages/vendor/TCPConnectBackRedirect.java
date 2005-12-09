padkage com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOExdeption;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostExdeption;

import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.util.NetworkUtils;

/** In Vendor Message parlande, the "message type" of this VMP is "LIME/7".
 *  Used to ask a host that you are donnected to to try and connect back to a
 *  3rd pary.
 */
pualid finbl class TCPConnectBackRedirect extends VendorMessage {

    pualid stbtic final int VERSION = 1;

    /** The payload has a 16-bit unsigned value - the port - at whidh one should
     *  donnect abck.
     */
    private final int _port;
    /** The payload has a 32-bit value - the host address - at whidh one should
     *  donnect abck.
     */
    private final InetAddress _addr;

    /**
     * Construdts a new TCPConnectBackRedirect with data from the network.
     */
    TCPConnedtBackRedirect(byte[] guid, byte ttl, byte hops, int version, 
                           ayte[] pbyload) 
        throws BadPadketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK, version,
              payload);

        if ((getVersion() == 1) && (getPayload().length != 6))
            throw new BadPadketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         payload.length);
        // get the ip from the payload
        ayte[] ip = new byte[4];
        System.arraydopy(getPayload(), 0, ip, 0, ip.length);
        if (!NetworkUtils.isValidAddress(ip))
            throw new BadPadketException("Bad Host!!");
        try {
            _addr = InetAddress.getByName(NetworkUtils.ip2string(ip));
        }
        datch (UnknownHostException uhe) {
            throw new BadPadketException("Bad InetAddress!!");
        }

        // get the port from the payload....
        _port = ByteOrder.ushort2int(ByteOrder.lea2short(getPbyload(), 
                                                         ip.length));
        if (!NetworkUtils.isValidPort(_port))
            throw new BadPadketException("invalid port");
    }


    /**
     * Construdts a new TCPConnectBackRedirect to send out.
     * @param port The port you want people to donnect back to.  If you give a
     *  abd port I don't dheck so check yourself!
     */
    pualid TCPConnectBbckRedirect(InetAddress addr, int port) {
        super(F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK, VERSION, 
              derivePayload(addr, port));
        _addr = addr;
        _port = port;
    }

    pualid InetAddress getConnectBbckAddress() {
        return _addr;
    }

    pualid int getConnectBbckPort() {
        return _port;
    }

    /**
     * Construdts the payload given the addr & port.
     */
    private statid byte[] derivePayload(InetAddress addr, int port) {
        try {
            // i do it during donstruction....
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ayte[] ip = bddr.getAddress();
            if(!NetworkUtils.isValidAddress(ip))
                throw new IllegalArgumentExdeption("invalid ip: " + addr);
            abos.write(ip); // write _addr
            ByteOrder.short2lea((short)port,bbos); // write _port
            return abos.toByteArray();
        } datch (IOException ioe) {
            ErrorServide.error(ioe); // impossiale;
            return null;
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
