package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.statistics.*;
import java.io.*;

/** In Vendor Message parlance, the "message type" of this VMP is "GTKG/7".
 *  Used to ask a host you connect to do a UDP ConnectBack.
 *
 *  VERSIONING INFO:
 *  -------------------------
 *  Version 2 of this message will fold the connect back guid into the guid
 *  of the message.  In order to transition, we should follow a 3 step process:
 *  1) allow this class to accept version 2 format
 *  2) after 1) has been released for a while, start using version 2
 *  3) some time after 2), stop accepting 1) (optional)
 */
public final class UDPConnectBackVendorMessage extends VendorMessage {

    public static final int VERSION = 2;

    /** The payload has a 16-bit unsigned value - the port - at which one should
     *  connect back.
     */
    private final int _port;
 
    /** The GUID that should be used for connect back.
     */
    private final GUID _guid;

    /** The encoding of the port and the guid.
     */
   
    /** The network constructor. */
    UDPConnectBackVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                                byte[] payload) 
        throws BadPacketException {
        super(guid, ttl, hops, F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK, 
              version, payload);

        if (getVersion() > VERSION) // we don't support it!!
            throw new BadPacketException("UNSUPPORTED VERSION");

        try {
            // get the port and guid from the payload....
            ByteArrayInputStream bais = new ByteArrayInputStream(getPayload());

            // get the port....
            _port = ByteOrder.ubytes2int(ByteOrder.leb2short(bais));
            if (getVersion() == 1) {
                // get the guid....
                byte[] guidBytes = new byte[16];
                int bytesRead = bais.read(guidBytes, 0, guidBytes.length);
                if ((bytesRead != 16) || (bais.available() > 0))
                    throw new BadPacketException("MALFORMED GUID!!!");
                _guid = new GUID(guidBytes);
            }
            else if (getVersion() == 2)
                _guid = new GUID(super.getGUID());
            else 
                throw new BadPacketException("UNSUPPORTED OLD VERSION");
                
        }
        catch (IOException ioe) {
            throw new BadPacketException("Couldn't read from a ByteStream!!!");
        }
    }


    /** @param port The port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     *  @param guid The guid you want people to connect back with.  Serves as
     *  a flag that the connect back is 'unsolicited'.
     */
    public UDPConnectBackVendorMessage(int port, GUID guid) 
        throws BadPacketException {
        super(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK, 1, 
              derivePayload(port, guid));
        _port = port;
        _guid = guid;
    }

    public int getConnectBackPort() {
        return _port;
    }

    public GUID getConnectBackGUID() {
        return _guid;
    }

    private static byte[] derivePayload(int port, GUID guid) 
        throws BadPacketException {
        try {
            // do it during construction....
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)port,baos); // write port
            baos.write(guid.bytes());
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
            SentMessageStatHandler.TCP_UDP_CONNECTBACK.addMessage(this);
    }

    /** Overridden purely for stats handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }

}
