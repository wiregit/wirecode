package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.ByteOrder;
import java.io.*;

/** In Vendor Message parlance, the "message type" of this VMP is "BEAR/7".
 *  Used to ask a host you connect to do a TCP ConnectBack.
 */
public class TCPConnectBackVMP extends VendorMessagePayload {

    public static final int VERSION = 1;

    /** The payload has a 16-bit unsigned value - the port - at which one should
     *  connect back.
     */
    private int _port = 0;
    
    private byte[] _payload = null;

    TCPConnectBackVMP(int version, byte[] payload) throws BadPacketException {
        super(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK, version);
        if (version > VERSION)
            throw new BadPacketException();
        if (payload.length != 2)
            throw new BadPacketException();
        // get the port from the payload....
        _payload = payload;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(_payload);
            _port = ByteOrder.ubytes2int(ByteOrder.leb2short(bais));
        }
        catch (IOException ioe) {
            throw new BadPacketException();
        }
    }

    /** @param port The port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     */
    public TCPConnectBackVMP(int port) {
        super(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK, VERSION);
        _port = port;
    }

    public int getConnectBackPort() {
        return _port;
    }

    protected byte[] getPayload() {
        if (_payload == null) {
            try {
                // i do it during construction....
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ByteOrder.short2leb((short)_port,baos); // write minspeed
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
