package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import junit.framework.*;
import java.io.*;

public class VendorMessageTest extends com.limegroup.gnutella.util.BaseTestCase {
    public VendorMessageTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(VendorMessageTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testCreationConstructor() throws Exception {
        VendorMessage vm = null;
        byte[] payload = null;
        byte[] vendorID = null;
        try {
            //test messed up vendor ID
            vendorID = new byte[5];
            vm = new VM(vendorID, 1, 1, new byte[0]);
            fail("bpe should have been thrown.");
        }
        catch (BadPacketException expected) {
        }
        try {
            // test bad selector
            vm = new VM(new byte[4], 0x10000000, 1, new byte[0]);
            fail("bpe should have been thrown.");
        }
        catch (BadPacketException expected) {
        }
        try {
            // test bad version
            vm = new VM(vendorID, 1, 0x00020101, new byte[0]);
            fail("bpe should have been thrown.");
        }
        catch (BadPacketException expected) {
        }
        try {
            // test bad payload
            vm = new VM(new byte[4], 1, 1, null);
            fail("bpe should have been thrown.");
        }
        catch (NullPointerException expected) {
        }
    }

    
    public void testWriteAndRead() throws Exception {
        // test network constructor....
        HopsFlowVendorMessage vm = 
            new HopsFlowVendorMessage(GUID.makeGuid(), (byte) 1, (byte) 0, 
                                      1, new byte[1]);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        VendorMessage vmRead = (VendorMessage) Message.read(bais);
        assertTrue(vm.equals(vmRead));

        // test other constructor....
        vm = new HopsFlowVendorMessage((byte)6);
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        vmRead = (VendorMessage) Message.read(bais);
        assertTrue(vm.equals(vmRead));
    }


    public void testEquals() throws Exception {
        VM vm1 = new VM("LIME".getBytes(), 1, 1, new byte[0]);
        VM vm2 = new VM("LIME".getBytes(), 1, 1, new byte[0]);
        VM vm3 = new VM("BEAR".getBytes(), 1, 1, new byte[0]);
        VM vm4 = new VM("LIMB".getBytes(), 1, 1, new byte[0]);
        VM vm5 = new VM("LIME".getBytes(), 2, 1, new byte[0]);
        VM vm6 = new VM("LIME".getBytes(), 1, 2, new byte[0]);
        VM vm7 = new VM("LIME".getBytes(), 1, 1, new byte[1]);
        assertTrue(vm1.equals(vm2));
        assertTrue(!vm1.equals(vm3));
        assertTrue(!vm1.equals(vm4));
        assertTrue(!vm1.equals(vm5));
        assertTrue(!vm1.equals(vm7));
        // versions don't effect equality....
        assertTrue(vm1.equals(vm6));
    }


    public void testHashCode() throws Exception {
        TCPConnectBackVendorMessage vmp1 = 
        new TCPConnectBackVendorMessage(1000);
        TCPConnectBackVendorMessage vmp2 = 
        new TCPConnectBackVendorMessage(1000);
        TCPConnectBackVendorMessage vmp3 = 
        new TCPConnectBackVendorMessage(1001);
        assertTrue(vmp1.hashCode() == vmp2.hashCode());
        assertTrue(vmp3.hashCode() != vmp2.hashCode());
    }


    public void testGetSpecificVendorMessages() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TCPConnectBackVendorMessage tcp = null;
        UDPConnectBackVendorMessage udp = null;
        HopsFlowVendorMessage hops = null;
        MessagesSupportedVendorMessage ms = null;
            
        tcp = new TCPConnectBackVendorMessage(6346);
        udp = new UDPConnectBackVendorMessage(6346, 
                                              new GUID(GUID.makeGuid()));
        hops = new HopsFlowVendorMessage((byte)4);

        ms = MessagesSupportedVendorMessage.instance();
        
        tcp.write(baos);
        udp.write(baos);
        ms.write(baos);
        hops.write(baos);
        
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        
        VendorMessage vm = (VendorMessage) Message.read(bais);
        assertTrue(vm.equals(tcp));

        vm = (VendorMessage) Message.read(bais);
        assertTrue(vm.equals(udp));

        vm = (VendorMessage) Message.read(bais);
        assertTrue(vm.equals(ms));
        
        vm = (VendorMessage) Message.read(bais);
        assertTrue(vm.equals(hops));
    }


    public void testBadVendorMessage() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TCPConnectBackVendorMessage tcp = 
            new TCPConnectBackVendorMessage(6346);
        VendorMessage vm = (VendorMessage) tcp;
        vm.hop();
        vm.write(baos);
        
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        
        try {
            vm = (VendorMessage) Message.read(bais);
            assertTrue(false);
        }
        catch (BadPacketException expected) {
        }
    }


    private static class VM extends VendorMessage {
        public VM(byte[] guid, byte ttl, byte hops, byte[] vendorID, 
                  int selector, int version, byte[] payload) 
            throws BadPacketException {
            super(guid, ttl, hops, vendorID, selector, version, payload);
        }

        public VM(byte[] vendorID, int selector, int version, byte[] payload) 
            throws BadPacketException {
            super(vendorID, selector, version, payload);
        }
    }


}
