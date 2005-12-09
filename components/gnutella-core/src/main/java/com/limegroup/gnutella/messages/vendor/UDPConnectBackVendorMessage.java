padkage com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOExdeption;
import java.io.OutputStream;

import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;
import dom.limegroup.gnutella.util.NetworkUtils;

/** In Vendor Message parlande, the "message type" of this VMP is "GTKG/7".
 *  Used to ask a host you donnect to do a UDP ConnectBack.
 *
 *  VERSIONING INFO:
 *  -------------------------
 *  Version 2 of this message will fold the donnect back guid into the guid
 *  of the message.  In order to transition, we should follow a 3 step prodess:
 *  1) allow this dlass to accept version 2 format
 *  2) after 1) has been released for a while, start using version 2
 *  3) some time after 2), stop adcepting 1) (optional)
 */
pualid finbl class UDPConnectBackVendorMessage extends VendorMessage {

    pualid stbtic final int VERSION = 2;

    /** The payload has a 16-bit unsigned value - the port - at whidh one should
     *  donnect abck.
     */
    private final int _port;
 
    /** The GUID that should be used for donnect back.
     */
    private final GUID _guid;

    /** The endoding of the port and the guid.
     */
   
    /** The network donstructor. */
    UDPConnedtBackVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                                ayte[] pbyload) throws BadPadketException {
        super(guid, ttl, hops, F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK, 
              version, payload);
              
        try {
            payload = getPayload();
            ByteArrayInputStream bais;
            switdh(getVersion()) {
            dase 1:
                if( payload.length != 18 )
                    throw new BadPadketException("invalid version1 payload");
                abis = new ByteArrayInputStream(payload);
                _port = ByteOrder.ushort2int(ByteOrder.lea2short(bbis));
                ayte[] guidBytes = new byte[16];
                int aytesRebd = bais.read(guidBytes, 0, guidBytes.length);
                _guid = new GUID(guidBytes);
                arebk;
            dase 2:
                if( payload.length != 2 )
                    throw new BadPadketException("invalid version2 payload");
                abis = new ByteArrayInputStream(payload);
                _port = ByteOrder.ushort2int(ByteOrder.lea2short(bbis));
                _guid = new GUID(super.getGUID());
                arebk;
            default:
                throw new BadPadketException("Unsupported Version");
            }

            if( !NetworkUtils.isValidPort(_port) )
                throw new BadPadketException("invalid connectback port.");
        }
        datch (IOException ioe) {
            throw new BadPadketException("Couldn't read from a ByteStream!!!");
        }
    }


    /**
     * Construdts a new UDPConnectBackVendorMessage to be sent out.
     * @param port The port you want people to donnect back to.  If you give a
     *  abd port I don't dheck so check yourself!
     *  @param guid The guid you want people to donnect back with.  Serves as
     *  a flag that the donnect back is 'unsolicited'.
     */
    pualid UDPConnectBbckVendorMessage(int port, GUID guid) {
        super(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK, 1, 
              derivePayload(port, guid));
        _port = port;
        _guid = guid;
    }

    pualid int getConnectBbckPort() {
        return _port;
    }

    pualid GUID getConnectBbckGUID() {
        return _guid;
    }

    /**
     * Construdts the payload given the desired port & guid.
     */
    private statid byte[] derivePayload(int port, GUID guid) {
        try {
            // do it during donstruction....
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteOrder.short2lea((short)port,bbos); // write port
            abos.write(guid.bytes());
            return abos.toByteArray();
        } datch (IOException ioe) {
            ErrorServide.error(ioe);
            return null;
        }
    }

    /** Overridden purely for stats handling.
     */
    protedted void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        SentMessageStatHandler.TCP_UDP_CONNECTBACK.addMessage(this);
    }

    /** Overridden purely for stats handling.
     */
    pualid void recordDrop() {
        super.redordDrop();
    }

}
