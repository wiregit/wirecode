package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import junit.framework.*;
import java.io.*;
import java.net.*;

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


    // tests HopsFlowVM and LimeACKVM (very simple messages)
    public void testWriteAndRead() throws Exception {
        // HOPS FLOW
        // -----------------------------
        // test network constructor....
        VendorMessage vm = new HopsFlowVendorMessage(GUID.makeGuid(), (byte) 1, 
                                                     (byte) 0, 1, new byte[1]);
        testWrite(vm);
        // test other constructor....
        vm = new HopsFlowVendorMessage((byte)6);
        testRead(vm);

        // Lime ACK
        // -----------------------------
        // test network constructor....
        vm = new LimeACKVendorMessage(GUID.makeGuid(), (byte) 1, (byte) 0, 
                                      2, new byte[1]);
        testWrite(vm);
        // test other constructor....
        vm = new LimeACKVendorMessage(new GUID(GUID.makeGuid()), 5);
        testRead(vm);

        // Reply Number
        // -----------------------------
        // test network constructor....
        vm = new ReplyNumberVendorMessage(GUID.makeGuid(), (byte) 1, 
                                          (byte) 0, 1, new byte[1]);
        testWrite(vm);
        // test other constructor....
        vm = new ReplyNumberVendorMessage(new GUID(GUID.makeGuid()), 5);
        testRead(vm);

        // Push Proxy Request
        // -----------------------------
        // test network constructor....
        vm = new PushProxyRequest(GUID.makeGuid(), (byte) 1, 
                                  (byte) 0, 1, new byte[0]);
        testWrite(vm);
        // test other constructor....
        vm = new PushProxyRequest(new GUID(GUID.makeGuid()));
        testRead(vm);

        // Push Proxy Acknowledgement
        // -----------------------------
        // test network constructor....

        byte[] bytes = new byte[6];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = (byte)33;

        // make sure deprecation is working....
        try {
            vm = new PushProxyAcknowledgement(GUID.makeGuid(), (byte) 1, 
                                              (byte) 0, 1, bytes);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}

        vm = new PushProxyAcknowledgement(GUID.makeGuid(), (byte) 1, 
                                          (byte) 0, 2, bytes);
        testWrite(vm);
        // test other constructor....
        vm = new PushProxyAcknowledgement(InetAddress.getLocalHost(), 5);
        testRead(vm);

        // Query Status Request
        // -----------------------------
        // test network constructor....
        vm = new QueryStatusRequest(GUID.makeGuid(), (byte) 1, 
                                    (byte) 0, 1, new byte[0]);
        testWrite(vm);
        // test other constructor....
        vm = new QueryStatusRequest(new GUID(GUID.makeGuid()));
        testRead(vm);

        // Query Status Response
        // -----------------------------
        // test network constructor....
        vm = new QueryStatusResponse(GUID.makeGuid(), (byte) 1, 
                                     (byte) 0, 1, new byte[2]);
        testWrite(vm);
        // test other constructor....
        vm = new QueryStatusResponse(new GUID(GUID.makeGuid()), 65535);
        testRead(vm);
    }


    public void testReplyNumber() throws Exception {
        try {
            GUID g = new GUID(GUID.makeGuid());
            ReplyNumberVendorMessage vm = new ReplyNumberVendorMessage(g, 0);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};
        try {
            GUID g = new GUID(GUID.makeGuid());
            ReplyNumberVendorMessage vm = new ReplyNumberVendorMessage(g, 256);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};

        for (int i = 1; i < 256; i++) {
            GUID guid = new GUID(GUID.makeGuid());
            ReplyNumberVendorMessage vm = new ReplyNumberVendorMessage(guid,
                                                                       i);
            assertEquals("Simple accessor is broken!", vm.getNumResults(), i);
            assertEquals("guids aren't equal!", guid, new GUID(vm.getGUID()));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            ByteArrayInputStream bais = 
                new ByteArrayInputStream(baos.toByteArray());
            ReplyNumberVendorMessage vmRead = 
                (ReplyNumberVendorMessage) Message.read(bais);
            assertEquals(vm, vmRead);
            assertEquals("Read accessor is broken!", vmRead.getNumResults(), i);
            assertEquals("after Read guids aren't equal!", guid, 
                         new GUID(vmRead.getGUID()));
        }

        // test that the VM can be backwards compatible....
        byte[] payload = null;
        ReplyNumberVendorMessage vm = null;
        
        // first test that it needs a payload of at least size 1
        payload = new byte[0];
        try {
            vm = new ReplyNumberVendorMessage(GUID.makeGuid(), (byte) 1, 
                                              (byte) 0, 0, payload);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};

        // first test that version 1 needs a payload of only size 1
        payload = new byte[2];
        try {
            vm = new ReplyNumberVendorMessage(GUID.makeGuid(), (byte) 1, 
                                              (byte) 0, 1, payload);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};

        // test that it can handle versions other than 1
        payload = new byte[3];
        try {
            vm = new ReplyNumberVendorMessage(GUID.makeGuid(), (byte) 1, 
                                              (byte) 0, 2, payload);
            assertEquals("Simple accessor is broken!", vm.getNumResults(), 0);
        }
        catch (BadPacketException expected) {
            assertTrue(false);
        }
    }


    public void testLimeACK() throws Exception {
        try {
            GUID g = new GUID(GUID.makeGuid());
            LimeACKVendorMessage vm = new LimeACKVendorMessage(g, -1);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};
        try {
            GUID g = new GUID(GUID.makeGuid());
            LimeACKVendorMessage vm = new LimeACKVendorMessage(g, 256);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};

        for (int i = 0; i < 256; i++) {
            GUID guid = new GUID(GUID.makeGuid());
            LimeACKVendorMessage vm = new LimeACKVendorMessage(guid,
                                                                       i);
            assertEquals("Simple accessor is broken!", vm.getNumResults(), i);
            assertEquals("guids aren't equal!", guid, new GUID(vm.getGUID()));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            ByteArrayInputStream bais = 
                new ByteArrayInputStream(baos.toByteArray());
            LimeACKVendorMessage vmRead = 
                (LimeACKVendorMessage) Message.read(bais);
            assertEquals(vm, vmRead);
            assertEquals("Read accessor is broken!", vmRead.getNumResults(), i);
            assertEquals("after Read guids aren't equal!", guid, 
                         new GUID(vmRead.getGUID()));
        }

        // test that the VM can be backwards compatible....
        byte[] payload = null;
        LimeACKVendorMessage vm = null;
        
        // first test that it needs a payload of at least size 1
        payload = new byte[0];
        try {
            vm = new LimeACKVendorMessage(GUID.makeGuid(), (byte) 1, 
                                              (byte) 0, 0, payload);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};

        // first test that it rejects all versions of 1
        payload = new byte[1];
        try {
            vm = new LimeACKVendorMessage(GUID.makeGuid(), (byte) 1, 
                                              (byte) 0, 1, payload);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};

        // first test that version 2 needs a payload of only size 1
        payload = new byte[2];
        try {
            vm = new LimeACKVendorMessage(GUID.makeGuid(), (byte) 1, 
                                              (byte) 0, 2, payload);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};

        // test that it can handle versions other than 1
        payload = new byte[3];
        try {
            vm = new LimeACKVendorMessage(GUID.makeGuid(), (byte) 1, 
                                              (byte) 0, 3, payload);
            assertEquals("Simple accessor is broken!", vm.getNumResults(), 0);
        }
        catch (BadPacketException expected) {
            assertTrue(false);
        }
    }


    public void testPushProxyVMs() throws Exception {
        GUID guid = new GUID(GUID.makeGuid());
        PushProxyRequest req = new PushProxyRequest(guid);
        assertTrue(req.getClientGUID().equals(new GUID(req.getGUID())));
        assertTrue(req.getClientGUID().equals(guid));
        testWrite(req);
        testRead(req);

        InetAddress addr = InetAddress.getLocalHost();
        PushProxyAcknowledgement ack = new PushProxyAcknowledgement(addr, 6346);
        assertEquals(InetAddress.getLocalHost(), ack.getListeningAddress());
        assertTrue(ack.getListeningPort() == 6346);
        testWrite(ack);
        testRead(req);
    }


    private void testWrite(VendorMessage vm) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        VendorMessage vmRead = (VendorMessage) Message.read(bais);
        assertEquals(vm, vmRead);
    }

    private void testRead(VendorMessage vm) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        VendorMessage vmRead = (VendorMessage) Message.read(bais);
        assertEquals(vm,vmRead);
    }

    public void testEquals() throws Exception {
        VM vm1 = new VM("LIME".getBytes(), 1, 1, new byte[0]);
        VM vm2 = new VM("LIME".getBytes(), 1, 1, new byte[0]);
        VM vm3 = new VM("BEAR".getBytes(), 1, 1, new byte[0]);
        VM vm4 = new VM("LIMB".getBytes(), 1, 1, new byte[0]);
        VM vm5 = new VM("LIME".getBytes(), 2, 1, new byte[0]);
        VM vm6 = new VM("LIME".getBytes(), 1, 2, new byte[0]);
        VM vm7 = new VM("LIME".getBytes(), 1, 1, new byte[1]);
        assertEquals(vm1,vm2);
        assertNotEquals(vm1,(vm3));
        assertNotEquals(vm1,(vm4));
        assertNotEquals(vm1,(vm5));
        assertNotEquals(vm1,(vm7));
        // versions don't effect equality....
        assertEquals(vm1,(vm6));
    }


    public void testHashCode() throws Exception {
        TCPConnectBackVendorMessage vmp1 = 
        new TCPConnectBackVendorMessage(1000);
        TCPConnectBackVendorMessage vmp2 = 
        new TCPConnectBackVendorMessage(1000);
        TCPConnectBackVendorMessage vmp3 = 
        new TCPConnectBackVendorMessage(1001);
        assertEquals(vmp1.hashCode() , vmp2.hashCode());
        assertNotEquals(vmp3.hashCode() , vmp2.hashCode());
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
        assertEquals(vm,(tcp));

        vm = (VendorMessage) Message.read(bais);
        assertEquals(vm,(udp));

        vm = (VendorMessage) Message.read(bais);
        assertEquals(vm,(ms));
        
        vm = (VendorMessage) Message.read(bais);
        assertEquals(vm,(hops));
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
