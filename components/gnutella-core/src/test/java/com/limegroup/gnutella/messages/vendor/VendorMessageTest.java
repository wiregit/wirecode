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
        return new TestSuite(VendorMessageTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testCreationConstructor() {
        VendorMessage vm = null;
        byte[] payload = null;
        byte[] vendorID = null;
        try {
            //test messed up vendor ID
            vendorID = new byte[5];
            vm = new VM(vendorID, 1, 1, new byte[0]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {
        }
        try {
            // test bad selector
            vm = new VM(new byte[4], 0x10000000, 1, new byte[0]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {
        }
        try {
            // test bad version
            vm = new VM(vendorID, 1, 0x00020101, new byte[0]);
            assertTrue(false);
        }
        catch (BadPacketException expected) {
        }
        try {
            // test bad payload
            vm = new VM(new byte[4], 1, 1, null);
            assertTrue(false);
        }
        catch (NullPointerException expected) {
        }
        catch (BadPacketException why) {
            assertTrue(false);
        }
    }

    
    public void testWriteAndRead() {
        try {
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
        catch (Exception crap) {
            crap.printStackTrace();
            assertTrue(false);
        }
    }


    public void testEquals() {
        try {
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
        catch (BadPacketException what) {
            assertTrue(false);
        }
    }


    public void testHashCode() {
        try {
            TCPConnectBackVendorMessage vmp1 = 
            new TCPConnectBackVendorMessage(1000);
            TCPConnectBackVendorMessage vmp2 = 
            new TCPConnectBackVendorMessage(1000);
            TCPConnectBackVendorMessage vmp3 = 
            new TCPConnectBackVendorMessage(1001);
            assertTrue(vmp1.hashCode() == vmp2.hashCode());
            assertTrue(vmp3.hashCode() != vmp2.hashCode());
        }
        catch (BadPacketException bpe) {
            assertTrue(false);
        }
    }


    public void testGetSpecificVendorMessages() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TCPConnectBackVendorMessage tcp = null;
        UDPConnectBackVendorMessage udp = null;
        HopsFlowVendorMessage hops = null;
        MessagesSupportedVendorMessage ms = null;
            
        try {
            tcp = new TCPConnectBackVendorMessage(6346);
            udp = new UDPConnectBackVendorMessage(6346, 
                                                  new GUID(GUID.makeGuid()));
            hops = new HopsFlowVendorMessage((byte)4);

            ms = MessagesSupportedVendorMessage.instance();
            
            tcp.write(baos);
            udp.write(baos);
            ms.write(baos);
            hops.write(baos);
        }
        catch (IOException uhoh) {
            assertTrue(false);
        }
        catch (BadPacketException uhoh) {
            assertTrue(false);
        }
        
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        
        try {
            VendorMessage vm = (VendorMessage) Message.read(bais);
            assertTrue(vm.equals(tcp));
        }
        catch (Exception uhoh) {
            uhoh.printStackTrace();
            assertTrue(false);
        }
        try {
            VendorMessage vm = (VendorMessage) Message.read(bais);
            assertTrue(vm.equals(udp));
        }
        catch (Exception uhoh) {
            uhoh.printStackTrace();
            assertTrue(false);
        }
        try {
            VendorMessage vm = (VendorMessage) Message.read(bais);
            assertTrue(vm.equals(ms));
        }
        catch (Exception uhoh) {
            uhoh.printStackTrace();
            assertTrue(false);
        }
        try {
            VendorMessage vm = (VendorMessage) Message.read(bais);
            assertTrue(vm.equals(hops));
        }
        catch (Exception uhoh) {
            uhoh.printStackTrace();
            assertTrue(false);
        }

    }


    public void testBadVendorMessage() {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            TCPConnectBackVendorMessage tcp = 
                new TCPConnectBackVendorMessage(6346);
            VendorMessage vm = (VendorMessage) tcp;
            vm.hop();
            vm.write(baos);
        }
        catch (IOException uhoh) {
            assertTrue(false);
        }
        catch (BadPacketException uhoh) {
            assertTrue(false);
        }
        
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        
        try {
            VendorMessage vm = (VendorMessage) Message.read(bais);
            assertTrue(false);
        }
        catch (BadPacketException expected) {
        }
        catch (IOException nope) {
            assertTrue(false);
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
