package com.limegroup.gnutella.messages.vendor;

import junit.framework.*;
import java.io.*;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.*;

/** Tests TCP/UDP ConnectBackVendorMessage
 */
public class ConnectBackVendorMessageTest extends com.limegroup.gnutella.util.BaseTestCase {

    private static final int UDP_VERSION = 
        UDPConnectBackVendorMessage.VERSION;

    private static final int TCP_VERSION = 
        TCPConnectBackVendorMessage.VERSION;

    public ConnectBackVendorMessageTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ConnectBackVendorMessageTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    
    public void testUDPConnectBackConstructor() {
        byte[] guid = GUID.makeGuid();
        byte ttl = 1, hops = 0;
        try {
            // try a VERSION we don't support
            UDPConnectBackVendorMessage udp = 
                new UDPConnectBackVendorMessage(guid, ttl, hops,
                                                UDP_VERSION+1, new byte[0]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            // in the next few tests, try bad sizes of the payload....
            UDPConnectBackVendorMessage udp = 
                new UDPConnectBackVendorMessage(guid, ttl, hops,
                                                UDP_VERSION, new byte[0]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            UDPConnectBackVendorMessage udp = 
                new UDPConnectBackVendorMessage(guid, ttl, hops,
                                                UDP_VERSION, new byte[17]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            UDPConnectBackVendorMessage udp = 
                new UDPConnectBackVendorMessage(guid, ttl, hops,
                                                UDP_VERSION, new byte[19]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            // this is the correct size of the payload
            UDPConnectBackVendorMessage udp = 
                new UDPConnectBackVendorMessage(guid, ttl, hops,
                                                UDP_VERSION, new byte[18]);
        }
        catch (BadPacketException notExpected) {
            assertTrue(false);
        }

        // make sure we encode things just fine....
        GUID guidObj = new GUID(GUID.makeGuid());
        try {
            UDPConnectBackVendorMessage VendorMessage1 = 
                new UDPConnectBackVendorMessage(6346, guidObj);
            UDPConnectBackVendorMessage VendorMessage2 = 
                new UDPConnectBackVendorMessage(guidObj.bytes(), ttl, hops, 
                                                UDP_VERSION, 
                                                VendorMessage1.getPayload());
            assertTrue(VendorMessage1.equals(VendorMessage2));
            assertTrue(VendorMessage1.getConnectBackPort() == 
                       VendorMessage2.getConnectBackPort());
            assertTrue(VendorMessage1.getConnectBackGUID().equals(VendorMessage2.getConnectBackGUID()));
        }
        catch (BadPacketException what) {
            assertTrue(false);
        }
    }
    


    public void testTCPConnectBackConstructor() {
        byte[] guid = GUID.makeGuid();
        byte ttl = 1, hops = 0;
        try {
            // try a VERSION we don't support
            TCPConnectBackVendorMessage TCP = 
                new TCPConnectBackVendorMessage(guid, ttl, hops,
                                                TCP_VERSION+1, new byte[0]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            // in the next few tests, try bad sizes of the payload....
            TCPConnectBackVendorMessage TCP = 
                new TCPConnectBackVendorMessage(guid, ttl, hops,
                                                TCP_VERSION, new byte[0]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            TCPConnectBackVendorMessage TCP = 
                new TCPConnectBackVendorMessage(guid, ttl, hops,
                                                TCP_VERSION, new byte[1]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            TCPConnectBackVendorMessage TCP = 
                new TCPConnectBackVendorMessage(guid, ttl, hops,
                                                TCP_VERSION, new byte[3]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            // this is the correct size of the payload
            TCPConnectBackVendorMessage TCP = 
                new TCPConnectBackVendorMessage(guid, ttl, hops,
                                                TCP_VERSION, new byte[2]);
        }
        catch (BadPacketException notExpected) {
            assertTrue(false);
        }

        // make sure we encode things just fine....
        try {
            TCPConnectBackVendorMessage VendorMessage1 = 
                new TCPConnectBackVendorMessage(6346);
            TCPConnectBackVendorMessage VendorMessage2 = 
                new TCPConnectBackVendorMessage(VendorMessage1.getGUID(),
                                                ttl, hops, TCP_VERSION, 
                                                VendorMessage1.getPayload());
            assertTrue(VendorMessage1.equals(VendorMessage2));
            assertTrue(VendorMessage1.getConnectBackPort() == 
                       VendorMessage2.getConnectBackPort());
        }
        catch (BadPacketException what) {
            assertTrue(false);
        }
    }


}
