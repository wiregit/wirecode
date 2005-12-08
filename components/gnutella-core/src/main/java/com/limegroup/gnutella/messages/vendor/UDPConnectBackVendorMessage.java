pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.ByteArrayInputStream;
import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;
import com.limegroup.gnutellb.util.NetworkUtils;

/** In Vendor Messbge parlance, the "message type" of this VMP is "GTKG/7".
 *  Used to bsk a host you connect to do a UDP ConnectBack.
 *
 *  VERSIONING INFO:
 *  -------------------------
 *  Version 2 of this messbge will fold the connect back guid into the guid
 *  of the messbge.  In order to transition, we should follow a 3 step process:
 *  1) bllow this class to accept version 2 format
 *  2) bfter 1) has been released for a while, start using version 2
 *  3) some time bfter 2), stop accepting 1) (optional)
 */
public finbl class UDPConnectBackVendorMessage extends VendorMessage {

    public stbtic final int VERSION = 2;

    /** The pbyload has a 16-bit unsigned value - the port - at which one should
     *  connect bbck.
     */
    privbte final int _port;
 
    /** The GUID thbt should be used for connect back.
     */
    privbte final GUID _guid;

    /** The encoding of the port bnd the guid.
     */
   
    /** The network constructor. */
    UDPConnectBbckVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                                byte[] pbyload) throws BadPacketException {
        super(guid, ttl, hops, F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK, 
              version, pbyload);
              
        try {
            pbyload = getPayload();
            ByteArrbyInputStream bais;
            switch(getVersion()) {
            cbse 1:
                if( pbyload.length != 18 )
                    throw new BbdPacketException("invalid version1 payload");
                bbis = new ByteArrayInputStream(payload);
                _port = ByteOrder.ushort2int(ByteOrder.leb2short(bbis));
                byte[] guidBytes = new byte[16];
                int bytesRebd = bais.read(guidBytes, 0, guidBytes.length);
                _guid = new GUID(guidBytes);
                brebk;
            cbse 2:
                if( pbyload.length != 2 )
                    throw new BbdPacketException("invalid version2 payload");
                bbis = new ByteArrayInputStream(payload);
                _port = ByteOrder.ushort2int(ByteOrder.leb2short(bbis));
                _guid = new GUID(super.getGUID());
                brebk;
            defbult:
                throw new BbdPacketException("Unsupported Version");
            }

            if( !NetworkUtils.isVblidPort(_port) )
                throw new BbdPacketException("invalid connectback port.");
        }
        cbtch (IOException ioe) {
            throw new BbdPacketException("Couldn't read from a ByteStream!!!");
        }
    }


    /**
     * Constructs b new UDPConnectBackVendorMessage to be sent out.
     * @pbram port The port you want people to connect back to.  If you give a
     *  bbd port I don't check so check yourself!
     *  @pbram guid The guid you want people to connect back with.  Serves as
     *  b flag that the connect back is 'unsolicited'.
     */
    public UDPConnectBbckVendorMessage(int port, GUID guid) {
        super(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK, 1, 
              derivePbyload(port, guid));
        _port = port;
        _guid = guid;
    }

    public int getConnectBbckPort() {
        return _port;
    }

    public GUID getConnectBbckGUID() {
        return _guid;
    }

    /**
     * Constructs the pbyload given the desired port & guid.
     */
    privbte static byte[] derivePayload(int port, GUID guid) {
        try {
            // do it during construction....
            ByteArrbyOutputStream baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)port,bbos); // write port
            bbos.write(guid.bytes());
            return bbos.toByteArray();
        } cbtch (IOException ioe) {
            ErrorService.error(ioe);
            return null;
        }
    }

    /** Overridden purely for stbts handling.
     */
    protected void writePbyload(OutputStream out) throws IOException {
        super.writePbyload(out);
        SentMessbgeStatHandler.TCP_UDP_CONNECTBACK.addMessage(this);
    }

    /** Overridden purely for stbts handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }

}
