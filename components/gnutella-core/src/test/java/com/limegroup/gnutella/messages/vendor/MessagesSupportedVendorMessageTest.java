package com.limegroup.gnutella.messages.vendor;

import junit.framework.*;
import java.io.*;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.connection.BIOMessageReader;
import com.limegroup.gnutella.messages.*;

/** Tests the important MessagesSupportedVendorMessage.
 */
public class MessagesSupportedVendorMessageTest extends com.limegroup.gnutella.util.BaseTestCase {
    public MessagesSupportedVendorMessageTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MessagesSupportedVendorMessageTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    
    public void testStaticConstructor() throws Exception {
        MessagesSupportedVendorMessage vmp = 
            MessagesSupportedVendorMessage.instance();
        assertGreaterThan(0, vmp.supportsTCPConnectBack());
        assertGreaterThan(0, vmp.supportsUDPConnectBack());
        assertGreaterThan(0, vmp.supportsHopsFlow());
        assertGreaterThan(0, vmp.supportsMessage("BEAR".getBytes(),7));
        assertGreaterThan(0, vmp.supportsMessage("BEAR".getBytes(),4));
        assertGreaterThan(0, vmp.supportsMessage("GTKG".getBytes(),7));
                                             
    
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vmp.write(baos);
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        MessagesSupportedVendorMessage vmpRead = 
            (MessagesSupportedVendorMessage) BIOMessageReader.read(bais);
        assertEquals(vmp, vmpRead);
        assertGreaterThan(0, vmpRead.supportsTCPConnectBack());
        assertGreaterThan(0, vmpRead.supportsUDPConnectBack());
        assertGreaterThan(0, vmpRead.supportsHopsFlow());
        assertGreaterThan(0, vmp.supportsMessage("BEAR".getBytes(),7));
        assertGreaterThan(0, vmp.supportsMessage("BEAR".getBytes(),4));
        assertGreaterThan(0, vmp.supportsMessage("GTKG".getBytes(),7));
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
           (MessagesSupportedVendorMessage) BIOMessageReader.read(bais);
        // make sure it supports everything we expect....
        assertEquals(10, vmp.supportsMessage("SUSH".getBytes(), 10));
        assertEquals(5, vmp.supportsMessage("NEIL".getBytes(), 5));
        assertEquals(3, vmp.supportsMessage("DAWG".getBytes(), 3));
        assertEquals(-1, vmp.supportsTCPConnectBack());
        assertEquals(-1, vmp.supportsUDPConnectBack());
        assertEquals(-1, vmp.supportsHopsFlow());

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

        assertEquals(vmp, vmpOther);

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
        assertNotEquals(vmpOther,vmpOneOther);

    }


}
