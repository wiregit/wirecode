package com.limegroup.gnutella.messages;

import junit.framework.*;
import java.io.*;
import com.limegroup.gnutella.GUID;

public class VendorMessagePayloadTest extends TestCase {
    public VendorMessagePayloadTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(VendorMessagePayloadTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void test3Constructor() {
        VendorMessagePayload vm = null;
        byte[] payload = null;
        try {
            //test messed up vendor ID
            vm = new VMP(new byte[5], 1, 1);
            assertTrue(false);
        }
        catch (IllegalArgumentException expected) {
        }
        try {
            // test bad selector
            vm = new VMP(new byte[4], 0x01000000, 1);
            assertTrue(false);
        }
        catch (IllegalArgumentException expected) {
        }
        try {
            // test bad version
            vm = new VMP(new byte[4], 1, 0x00200101);
            assertTrue(false);
        }
        catch (IllegalArgumentException expected) {
        }
    }


    public void testEquals() {
        VMP vmp1 = new VMP("LIME".getBytes(), 1, 1);
        VMP vmp2 = new VMP("LIME".getBytes(), 1, 1);
        VMP vmp3 = new VMP("BEAR".getBytes(), 1, 1);
        VMP vmp4 = new VMP("LIMB".getBytes(), 1, 1);
        VMP vmp5 = new VMP("LIME".getBytes(), 2, 1);
        VMP vmp6 = new VMP("LIME".getBytes(), 1, 2);
        assertTrue(vmp1.equals(vmp2));
        assertTrue(!vmp1.equals(vmp3));
        assertTrue(!vmp1.equals(vmp4));
        assertTrue(!vmp1.equals(vmp5));
        // versions don't effect equality....
        assertTrue(vmp1.equals(vmp6));
    }


    public void testHashCode() {
        TCPConnectBackVMP vmp1 = new TCPConnectBackVMP(1000);
        TCPConnectBackVMP vmp2 = new TCPConnectBackVMP(1000);
        TCPConnectBackVMP vmp3 = new TCPConnectBackVMP(1001);
        assertTrue(vmp1.hashCode() == vmp2.hashCode());
        assertTrue(vmp3.hashCode() != vmp2.hashCode());
    }


    public void testGetSpecificVMPs() {
        TCPConnectBackVMP tcp = new TCPConnectBackVMP(6346);
        UDPConnectBackVMP udp = 
            new UDPConnectBackVMP(6346, 
                                  new GUID(GUID.makeGuid()));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            tcp.getVendorMessage().write(baos);
            udp.getVendorMessage().write(baos);
        }
        catch (IOException uhoh) {
            assertTrue(false);
        }
        
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        
        try {
            VendorMessage vm = (VendorMessage) Message.read(bais);
            VendorMessagePayload vmp = vm.getVendorMessagePayload();
            assertTrue(vmp.equals(tcp));
        }
        catch (Exception uhoh) {
            uhoh.printStackTrace();
            assertTrue(false);
        }
        try {
            VendorMessage vm = (VendorMessage) Message.read(bais);
            VendorMessagePayload vmp = vm.getVendorMessagePayload();
            assertTrue(vmp.equals(udp));
        }
        catch (Exception uhoh) {
            uhoh.printStackTrace();
            assertTrue(false);
        }

    }


    private static class VMP extends VendorMessagePayload {
        public VMP(byte[] vendorID, int selector, int version) {
            super(vendorID, selector, version);
        }

        protected byte[] getPayload() {
            return new byte[0];
        }
    }

}
