package com.limegroup.gnutella.messages;

import junit.framework.*;
import java.io.*;
import com.limegroup.gnutella.GUID;

/** Tests TCP/UDP ConnectBackVMP
 */
public class ConnectBackVMPTest extends TestCase {
    public ConnectBackVMPTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ConnectBackVMPTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    
    public void testUDPConnectBackConstructor() {
        try {
            // try a VERSION we don't support
            UDPConnectBackVMP udp = 
                new UDPConnectBackVMP(UDPConnectBackVMP.VERSION+1, new byte[0]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            // in the next few tests, try bad sizes of the payload....
            UDPConnectBackVMP udp = 
                new UDPConnectBackVMP(UDPConnectBackVMP.VERSION, new byte[0]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            UDPConnectBackVMP udp = 
                new UDPConnectBackVMP(UDPConnectBackVMP.VERSION, new byte[17]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            UDPConnectBackVMP udp = 
                new UDPConnectBackVMP(UDPConnectBackVMP.VERSION, new byte[19]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            // this is the correct size of the payload
            UDPConnectBackVMP udp = 
                new UDPConnectBackVMP(UDPConnectBackVMP.VERSION, new byte[18]);
        }
        catch (BadPacketException notExpected) {
            assertTrue(false);
        }

        // make sure we encode things just fine....
        GUID guid = new GUID(GUID.makeGuid());
        UDPConnectBackVMP vmp1 = new UDPConnectBackVMP(6346, guid);
        try {
            UDPConnectBackVMP vmp2 = 
                new UDPConnectBackVMP(UDPConnectBackVMP.VERSION, 
                                      vmp1.getPayload());
            assertTrue(vmp1.equals(vmp2));
            assertTrue(vmp1.getConnectBackPort() == 
                       vmp2.getConnectBackPort());
            assertTrue(vmp1.getConnectBackGUID().equals(vmp2.getConnectBackGUID()));
        }
        catch (BadPacketException what) {
            assertTrue(false);
        }
    }
    


    public void testTCPConnectBackConstructor() {
        try {
            // try a VERSION we don't support
            TCPConnectBackVMP TCP = 
                new TCPConnectBackVMP(TCPConnectBackVMP.VERSION+1, new byte[0]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            // in the next few tests, try bad sizes of the payload....
            TCPConnectBackVMP TCP = 
                new TCPConnectBackVMP(TCPConnectBackVMP.VERSION, new byte[0]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            TCPConnectBackVMP TCP = 
                new TCPConnectBackVMP(TCPConnectBackVMP.VERSION, new byte[1]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            TCPConnectBackVMP TCP = 
                new TCPConnectBackVMP(TCPConnectBackVMP.VERSION, new byte[3]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}
        try {
            // this is the correct size of the payload
            TCPConnectBackVMP TCP = 
                new TCPConnectBackVMP(TCPConnectBackVMP.VERSION, new byte[2]);
        }
        catch (BadPacketException notExpected) {
            assertTrue(false);
        }

        // make sure we encode things just fine....
        TCPConnectBackVMP vmp1 = new TCPConnectBackVMP(6346);
        try {
            TCPConnectBackVMP vmp2 = 
                new TCPConnectBackVMP(TCPConnectBackVMP.VERSION, 
                                      vmp1.getPayload());
            assertTrue(vmp1.equals(vmp2));
            assertTrue(vmp1.getConnectBackPort() == 
                       vmp2.getConnectBackPort());
        }
        catch (BadPacketException what) {
            assertTrue(false);
        }
    }


}
