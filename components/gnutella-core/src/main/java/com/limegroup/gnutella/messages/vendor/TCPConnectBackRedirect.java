pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.net.InetAddress;
import jbva.net.UnknownHostException;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.util.NetworkUtils;

/** In Vendor Messbge parlance, the "message type" of this VMP is "LIME/7".
 *  Used to bsk a host that you are connected to to try and connect back to a
 *  3rd pbry.
 */
public finbl class TCPConnectBackRedirect extends VendorMessage {

    public stbtic final int VERSION = 1;

    /** The pbyload has a 16-bit unsigned value - the port - at which one should
     *  connect bbck.
     */
    privbte final int _port;
    /** The pbyload has a 32-bit value - the host address - at which one should
     *  connect bbck.
     */
    privbte final InetAddress _addr;

    /**
     * Constructs b new TCPConnectBackRedirect with data from the network.
     */
    TCPConnectBbckRedirect(byte[] guid, byte ttl, byte hops, int version, 
                           byte[] pbyload) 
        throws BbdPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK, version,
              pbyload);

        if ((getVersion() == 1) && (getPbyload().length != 6))
            throw new BbdPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         pbyload.length);
        // get the ip from the pbyload
        byte[] ip = new byte[4];
        System.brraycopy(getPayload(), 0, ip, 0, ip.length);
        if (!NetworkUtils.isVblidAddress(ip))
            throw new BbdPacketException("Bad Host!!");
        try {
            _bddr = InetAddress.getByName(NetworkUtils.ip2string(ip));
        }
        cbtch (UnknownHostException uhe) {
            throw new BbdPacketException("Bad InetAddress!!");
        }

        // get the port from the pbyload....
        _port = ByteOrder.ushort2int(ByteOrder.leb2short(getPbyload(), 
                                                         ip.length));
        if (!NetworkUtils.isVblidPort(_port))
            throw new BbdPacketException("invalid port");
    }


    /**
     * Constructs b new TCPConnectBackRedirect to send out.
     * @pbram port The port you want people to connect back to.  If you give a
     *  bbd port I don't check so check yourself!
     */
    public TCPConnectBbckRedirect(InetAddress addr, int port) {
        super(F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK, VERSION, 
              derivePbyload(addr, port));
        _bddr = addr;
        _port = port;
    }

    public InetAddress getConnectBbckAddress() {
        return _bddr;
    }

    public int getConnectBbckPort() {
        return _port;
    }

    /**
     * Constructs the pbyload given the addr & port.
     */
    privbte static byte[] derivePayload(InetAddress addr, int port) {
        try {
            // i do it during construction....
            ByteArrbyOutputStream baos = new ByteArrayOutputStream();
            byte[] ip = bddr.getAddress();
            if(!NetworkUtils.isVblidAddress(ip))
                throw new IllegblArgumentException("invalid ip: " + addr);
            bbos.write(ip); // write _addr
            ByteOrder.short2leb((short)port,bbos); // write _port
            return bbos.toByteArray();
        } cbtch (IOException ioe) {
            ErrorService.error(ioe); // impossible;
            return null;
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
