package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.Test;

import org.limewire.util.ByteUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;

/** Tests the important MessagesSupportedVendorMessage.
 */
public class MessagesSupportedVendorMessageTest extends com.limegroup.gnutella.util.LimeTestCase {
    private MessagesSupportedVendorMessage messagesSupportedVendorMessage;
    private MessageFactory messageFactory;


    public MessagesSupportedVendorMessageTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MessagesSupportedVendorMessageTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
		Injector injector = LimeTestUtils.createInjector();
		messagesSupportedVendorMessage = injector.getInstance(MessagesSupportedVendorMessage.class);
		messageFactory = injector.getInstance(MessageFactory.class);
    }
    
    public void testStaticConstructor() throws Exception {
        MessagesSupportedVendorMessage vmp = 
            messagesSupportedVendorMessage;
        assertGreaterThan(0, vmp.supportsTCPConnectBack());
        assertGreaterThan(0, vmp.supportsUDPConnectBack());
        assertGreaterThan(0, vmp.supportsTCPConnectBackRedirect());
        assertGreaterThan(0, vmp.supportsUDPConnectBackRedirect());
        assertGreaterThan(0, vmp.supportsHopsFlow());
        assertGreaterThan(0, vmp.supportsPushProxy());
        assertGreaterThan(0, vmp.supportsLeafGuidance());
        assertGreaterThan(0, vmp.supportsMessage("BEAR".getBytes(),7));
        assertGreaterThan(0, vmp.supportsMessage("BEAR".getBytes(),4));
        assertGreaterThan(0, vmp.supportsMessage("GTKG".getBytes(),7));
        assertGreaterThan(0, vmp.supportsMessage("BEAR".getBytes(),11));
        assertGreaterThan(0, vmp.supportsMessage("LIME".getBytes(),21));
        assertGreaterThan(0, vmp.supportsMessage("LIME".getBytes(),7));
        assertGreaterThan(0, vmp.supportsMessage("LIME".getBytes(),8));
        assertGreaterThan(0, vmp.supportsMessage("LIME".getBytes(),30));
                                             
    
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vmp.write(baos);
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        MessagesSupportedVendorMessage vmpRead = 
            (MessagesSupportedVendorMessage) messageFactory.read(bais, Network.TCP);
        assertEquals(vmp, vmpRead);
        assertGreaterThan(0, vmpRead.supportsTCPConnectBack());
        assertGreaterThan(0, vmpRead.supportsUDPConnectBack());
        assertGreaterThan(0, vmpRead.supportsTCPConnectBackRedirect());
        assertGreaterThan(0, vmpRead.supportsUDPConnectBackRedirect());
        assertGreaterThan(0, vmpRead.supportsHopsFlow());
        assertGreaterThan(0, vmp.supportsPushProxy());
        assertGreaterThan(0, vmp.supportsLeafGuidance());
        assertGreaterThan(0, vmp.supportsMessage("BEAR".getBytes(),7));
        assertGreaterThan(0, vmp.supportsMessage("BEAR".getBytes(),4));
        assertGreaterThan(0, vmp.supportsMessage("GTKG".getBytes(),7));
        assertGreaterThan(0, vmp.supportsMessage("BEAR".getBytes(),11));
        assertGreaterThan(0, vmp.supportsMessage("LIME".getBytes(),21));
        assertGreaterThan(0, vmp.supportsMessage("LIME".getBytes(),7));
        assertGreaterThan(0, vmp.supportsMessage("LIME".getBytes(),8));
        assertGreaterThan(0, vmp.supportsMessage("LIME".getBytes(),30));
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
        ByteUtils.short2leb((short)4, baos);
        smp1.encode(baos);
        smp2.encode(baos);
        smp3.encode(baos);
        smp3.encode(baos);
        VendorMessage vm = new MessagesSupportedVendorMessage(guid, ttl,
                                                              hops, 0,
                                                              baos.toByteArray(), Network.UNKNOWN);
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        MessagesSupportedVendorMessage vmp = 
           (MessagesSupportedVendorMessage) messageFactory.read(bais, Network.TCP);
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
        ByteUtils.short2leb((short)3, baos);
        smp2.encode(baos);
        smp3.encode(baos);
        smp1.encode(baos);
        
        MessagesSupportedVendorMessage vmpOther = 
            new MessagesSupportedVendorMessage(guid, ttl, hops, 0,
                                               baos.toByteArray(), Network.UNKNOWN);

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
            ByteUtils.short2leb((short)4, baos);
            smp2.encode(baos);
            smp3.encode(baos);
            smp1.encode(baos);
            new MessagesSupportedVendorMessage(guid, ttl, hops, 0, 
                                                   baos.toByteArray(), Network.UNKNOWN);
            fail("bpe should have been thrown.");
        } catch (BadPacketException expected) {
        }
        try {
            // test corrupt info....
            baos = new ByteArrayOutputStream();
            ByteUtils.short2leb((short)4, baos);
            smp2.encode(baos);
            smp3.encode(baos);
            smp1.encode(baos);
            baos.write("crap".getBytes());
            new MessagesSupportedVendorMessage(guid, ttl, hops, 0, 
                                                   baos.toByteArray(), Network.UNKNOWN);
            fail("bpe should have been thrown.");
        } catch (BadPacketException expected) {
        }

        // test semantics....
        baos = new ByteArrayOutputStream();
        ByteUtils.short2leb((short)0, baos);
        smp2.encode(baos);
        smp3.encode(baos);
        smp1.encode(baos);
        MessagesSupportedVendorMessage vmpOther = 
            new MessagesSupportedVendorMessage(guid, ttl, hops, 0, 
                                               baos.toByteArray(), Network.UNKNOWN);
        baos = new ByteArrayOutputStream();
        ByteUtils.short2leb((short)3, baos);
        smp2.encode(baos);
        smp3.encode(baos);
        smp1.encode(baos);
        MessagesSupportedVendorMessage vmpOneOther = 
            new MessagesSupportedVendorMessage(guid, ttl, hops, 0, 
                                               baos.toByteArray(), Network.UNKNOWN);
        assertNotEquals(vmpOther,vmpOneOther);

    }


}
