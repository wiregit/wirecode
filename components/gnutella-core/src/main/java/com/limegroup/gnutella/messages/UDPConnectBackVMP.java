package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import java.io.*;

/** In Vendor Message parlance, the "message type" of this VMP is "GTKG/7".
 *  Used to ask a host you connect to do a UDP ConnectBack.
 */
public final class UDPConnectBackVMP extends VendorMessagePayload {

    public static final int VERSION = 1;

    /** The payload has a 16-bit unsigned value - the port - at which one should
     *  connect back.
     */
    private int _port = 0;
 
    /** The GUID that should be used for connect back.
     */
    private GUID _guid = null;
   
    private byte[] _payload = null;

    UDPConnectBackVMP(int version, byte[] payload) throws BadPacketException {
        super(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK, version);
        // get the port from the payload....
        _payload = payload;
        try {
            if (version > VERSION) // we don't support it!!
                throw new BadPacketException("UNSUPPORTED VERSION");

            ByteArrayInputStream bais = new ByteArrayInputStream(_payload);
            // get the port....
            _port = ByteOrder.ubytes2int(ByteOrder.leb2short(bais));

            // get the guid....
            byte[] guidBytes = new byte[16];
            int bytesRead = bais.read(guidBytes, 0, guidBytes.length);
            if ((bytesRead != 16) || (bais.available() > 0))
                throw new BadPacketException();
            _guid = new GUID(guidBytes);
        }
        catch (IOException ioe) {
            throw new BadPacketException();
        }
    }

    /** @param port The port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     */
    public UDPConnectBackVMP(int port, GUID guid) {
        super(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK, VERSION);
        _port = port;
        _guid = guid;
    }

    public int getConnectBackPort() {
        return _port;
    }

    public GUID getConnectBackGUID() {
        return _guid;
    }

    protected byte[] getPayload() {
        if (_payload == null) {
            try {
                // i do it during construction....
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ByteOrder.short2leb((short)_port,baos); // write port
                baos.write(_guid.bytes());
                _payload = baos.toByteArray();
            }
            catch (IOException ioe) {
                // this should never happen!!!
                ioe.printStackTrace();
            }
        }
        return _payload;
    }

}
