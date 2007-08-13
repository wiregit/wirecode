package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.MessageFactory;

public class ReplyNumberVendorMessageTest extends BaseTestCase {

    public ReplyNumberVendorMessageTest(String name) {
        super(name);
    }
    
    public void testNumResults() {
        ReplyNumberVendorMessage msg = ProviderHacks.getReplyNumberVendorMessageFactory().createV3ReplyNumberVendorMessage(new GUID(), 10);
        assertEquals(10, msg.getNumResults());
        
        for (int illegalResultNum : new int[] { 256, 0, -1 }) {
            try { 
                msg = ProviderHacks.getReplyNumberVendorMessageFactory().createV3ReplyNumberVendorMessage(new GUID(), illegalResultNum);
                fail("Expected illegal argument exception for result: " + illegalResultNum);
            }
            catch (IllegalArgumentException iae) {
            }
        }
    }
    
    public void testVersion3AllowsLargerMessagesFromNetwork() throws BadPacketException {
        ProviderHacks.getReplyNumberVendorMessageFactory().createFromNetwork(GUID.makeGuid(),
                (byte)1, (byte)1, 3, new byte[11]);
        try {
            ProviderHacks.getReplyNumberVendorMessageFactory().createFromNetwork(GUID.makeGuid(),
                    (byte)1, (byte)1, 2, new byte[11]);
            fail("BadPacketException expected, message too large");
        }
        catch (BadPacketException e) {
        }
    }
    
    public void testOldVersionIsNotAcceptedFromNetwork() {
        try {
            ProviderHacks.getReplyNumberVendorMessageFactory().createFromNetwork(GUID.makeGuid(),
                    (byte)0x01, (byte)0x01, 1, new byte[2]);
            fail("Old message versions should not be accpeted");            
        }
        catch (BadPacketException bpe) {
        }
    }
    
    public void testReplyNumber() throws Exception {
        try {
            GUID g = new GUID(GUID.makeGuid());
            ProviderHacks.getReplyNumberVendorMessageFactory().createV3ReplyNumberVendorMessage(g, 0);
            assertTrue(false);
        } catch(IllegalArgumentException expected) {}
        try {
            GUID g = new GUID(GUID.makeGuid());
            ProviderHacks.getReplyNumberVendorMessageFactory().createV3ReplyNumberVendorMessage(g, 256);
            assertTrue(false);
        } catch(IllegalArgumentException expected) {}

        for (int i = 1; i < 256; i++) {
            GUID guid = new GUID(GUID.makeGuid());
            ReplyNumberVendorMessage vm = ProviderHacks.getReplyNumberVendorMessageFactory().createV3ReplyNumberVendorMessage(guid,
                                                                       i);
            assertEquals("Simple accessor is broken!", vm.getNumResults(), i);
            assertEquals("guids aren't equal!", guid, new GUID(vm.getGUID()));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            ByteArrayInputStream bais = 
                new ByteArrayInputStream(baos.toByteArray());
            ReplyNumberVendorMessage vmRead = 
                (ReplyNumberVendorMessage) MessageFactory.read(bais);
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
            vm = ProviderHacks.getReplyNumberVendorMessageFactory().createFromNetwork(GUID.makeGuid(),
                    (byte) 1, (byte) 0, 0, payload);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};

        // first test that version 1 needs a payload of only size 1
        payload = new byte[2];
        try {
            vm = ProviderHacks.getReplyNumberVendorMessageFactory().createFromNetwork(GUID.makeGuid(),
                    (byte) 1, (byte) 0, 1, payload);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};

        //test that it can handle versions other than 1
        payload = new byte[3];
        try {
            vm = ProviderHacks.getReplyNumberVendorMessageFactory().createFromNetwork(GUID.makeGuid(),
                    (byte) 1, (byte) 0, 3, payload);
            assertEquals("Simple accessor is broken!", vm.getNumResults(), 0);
        }
        catch (BadPacketException expected) {
            assertTrue(false);
        }
        
        //test un/solicited byte
        UDPService service = ProviderHacks.getUdpService();
        PrivilegedAccessor.setValue(
                service,"_acceptedUnsolicitedIncoming",new Boolean(false));
        
        vm = ProviderHacks.getReplyNumberVendorMessageFactory().createV3ReplyNumberVendorMessage(new GUID(GUID.makeGuid()),5);
        assertFalse(vm.canReceiveUnsolicited());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ReplyNumberVendorMessage vm2 = (ReplyNumberVendorMessage) MessageFactory.read(bais);
        assertFalse(vm2.canReceiveUnsolicited());
        
        PrivilegedAccessor.setValue(
                service,"_acceptedUnsolicitedIncoming",new Boolean(true));
        
        vm = ProviderHacks.getReplyNumberVendorMessageFactory().createV3ReplyNumberVendorMessage(new GUID(GUID.makeGuid()),5);
        assertTrue(vm.canReceiveUnsolicited());
        
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        vm2 = (ReplyNumberVendorMessage) MessageFactory.read(bais);
        assertTrue(vm2.canReceiveUnsolicited());
        
        
    }



}
