package com.limegroup.gnutella.messages.vendor;

import junit.framework.*;
import java.io.*;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.*;

/** Tests the important MessagesSupportedVendorMessage.
 */
public class MessagesSupportedVendorMessageTest extends com.limegroup.gnutella.util.BaseTestCase {
    public MessagesSupportedVendorMessageTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MessagesSupportedVendorMessageTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    
    public void testStaticConstructor() throws Exception {
        MessagesSupportedVendorMessage vmp = 
            MessagesSupportedVendorMessage.instance();
        assertTrue(vmp.supportsTCPConnectBack() > 0);
        assertTrue(vmp.supportsUDPConnectBack() > 0);
        assertTrue(vmp.supportsHopsFlow() > 0);
        assertTrue(vmp.supportsMessage("BEAR".getBytes(),7) > 0);
        assertTrue(vmp.supportsMessage("BEAR".getBytes(),4) > 0);
        assertTrue(vmp.supportsMessage("GTKG".getBytes(),7) > 0);
                                             
    
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vmp.write(baos);
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        MessagesSupportedVendorMessage vmpRead = 
            (MessagesSupportedVendorMessage) Message.read(bais);
        assertTrue(vmp.equals(vmpRead));
        assertTrue(vmpRead.supportsTCPConnectBack() > 0);
        assertTrue(vmpRead.supportsUDPConnectBack() > 0);
        assertTrue(vmpRead.supportsHopsFlow() > 0);
        assertTrue(vmp.supportsMessage("BEAR".getBytes(),7) > 0);
        assertTrue(vmp.supportsMessage("BEAR".getBytes(),4) > 0);
        assertTrue(vmp.supportsMessage("GTKG".getBytes(),7) > 0);
    }

    public void testNetworkConstructor() throws Exception {
        MessagesSupportedVendorMessage.SupportedMessageBlock smp1 = 
            new MessagesSupportedVendorMessage.SupportedMessageBlock("SUSH".getBytes(),
                                                            10, 10);
        MessagesSupportedVendorMessage.SupportedMessageBlock smp2 = 
            new MessagesSupportedVendorMessage.SupportedMessageBlock("NEIL".getBytes(), 
                                                           5, 5);
        MessagesSupportedVendorMessage.SupportedMessageBlock smp3 = 
            new MessagesSupportedVendorMessage.SupportedMessageBlock("DAWG".getBytes(), 
                                                           3, 3);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] guid = GUID.makeGuid();
        byte ttl = 1, hops = 0;
        ByteOrder.short2leb((short)4, baos);
        baos.write(smp1.encode());
        baos.write(smp2.encode());
        baos.write(smp3.encode());
        baos.write(smp3.encode());
        VendorMessage vm = new MessagesSupportedVendorMessage(guid, ttl,
                                                              hops, 0,
                                                              baos.toByteArray());
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        MessagesSupportedVendorMessage vmp = 
           (MessagesSupportedVendorMessage) Message.read(bais);
        // make sure it supports everything we expect....
        assertTrue(vmp.supportsMessage("SUSH".getBytes(), 10) == 10);
        assertTrue(vmp.supportsMessage("NEIL".getBytes(), 5) == 5);
        assertTrue(vmp.supportsMessage("DAWG".getBytes(), 3) == 3);
        assertTrue(vmp.supportsTCPConnectBack() == -1);
        assertTrue(vmp.supportsUDPConnectBack() == -1);
        assertTrue(vmp.supportsHopsFlow() == -1);

        // now creat another one, mix up the blocks that are supported, and
        // make sure they are equal....
        baos = new ByteArrayOutputStream();
        ByteOrder.short2leb((short)3, baos);
        baos.write(smp2.encode());
        baos.write(smp3.encode());
        baos.write(smp1.encode());
        
        MessagesSupportedVendorMessage vmpOther = 
            new MessagesSupportedVendorMessage(guid, ttl, hops, 0,
                                               baos.toByteArray());

        assertTrue(vmp.equals(vmpOther));

    }


    public void testBadCases() throws Exception {
        MessagesSupportedVendorMessage.SupportedMessageBlock smp1 = 
            new MessagesSupportedVendorMessage.SupportedMessageBlock("SUSH".getBytes(),
                                                            10, 10);
        MessagesSupportedVendorMessage.SupportedMessageBlock smp2 = 
            new MessagesSupportedVendorMessage.SupportedMessageBlock("NEIL".getBytes(), 
                                                           5, 5);
        MessagesSupportedVendorMessage.SupportedMessageBlock smp3 = 
            new MessagesSupportedVendorMessage.SupportedMessageBlock("DAWG".getBytes(), 
                                                           3, 3);
        ByteArrayOutputStream baos = null;
        byte[] guid = GUID.makeGuid();
        byte ttl = 1, hops = 0;
        try {
            // test missing info....
            baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)4, baos);
            baos.write(smp2.encode());
            baos.write(smp3.encode());
            baos.write(smp1.encode());
            MessagesSupportedVendorMessage vmpOther = 
                new MessagesSupportedVendorMessage(guid, ttl, hops, 0, 
                                                   baos.toByteArray());
            fail("bpe should have been thrown.");
        } catch (BadPacketException expected) {
        }
        try {
            // test corrupt info....
            baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)4, baos);
            baos.write(smp2.encode());
            baos.write(smp3.encode());
            baos.write(smp1.encode());
            baos.write("crap".getBytes());
            MessagesSupportedVendorMessage vmpOther = 
                new MessagesSupportedVendorMessage(guid, ttl, hops, 0, 
                                                   baos.toByteArray());
            fail("bpe should have been thrown.");
        } catch (BadPacketException expected) {
        }

        // test semantics....
        baos = new ByteArrayOutputStream();
        ByteOrder.short2leb((short)0, baos);
        baos.write(smp2.encode());
        baos.write(smp3.encode());
        baos.write(smp1.encode());
        MessagesSupportedVendorMessage vmpOther = 
            new MessagesSupportedVendorMessage(guid, ttl, hops, 0, 
                                               baos.toByteArray());
        baos = new ByteArrayOutputStream();
        ByteOrder.short2leb((short)3, baos);
        baos.write(smp2.encode());
        baos.write(smp3.encode());
        baos.write(smp1.encode());
        MessagesSupportedVendorMessage vmpOneOther = 
            new MessagesSupportedVendorMessage(guid, ttl, hops, 0, 
                                               baos.toByteArray());
        assertTrue(!vmpOther.equals(vmpOneOther));

    }


}
