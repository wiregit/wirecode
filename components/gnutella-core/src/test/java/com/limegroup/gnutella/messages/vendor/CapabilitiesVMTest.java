package com.limegroup.gnutella.messages.vendor;

import junit.framework.*;
import java.io.*;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.*;

/** Tests the important MessagesSupportedVendorMessage.
 */
public class CapabilitiesVMTest 
    extends com.limegroup.gnutella.util.BaseTestCase {
    public CapabilitiesVMTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(CapabilitiesVMTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    
    public void testStaticConstructor() throws Exception {
        CapabilitiesVM vmp = 
            CapabilitiesVM.instance();
        assertGreaterThan(0, vmp.supportsWhatIsCapability());
        assertTrue(vmp.supportsWhatIsNew());
        assertGreaterThan(0, vmp.supportsCapability("WHAT".getBytes()));
    
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vmp.write(baos);
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        CapabilitiesVM vmpRead = 
            (CapabilitiesVM) Message.read(bais);
        assertEquals(vmp, vmpRead);

        assertGreaterThan(0, vmpRead.supportsWhatIsCapability());
        assertTrue(vmpRead.supportsWhatIsNew());
        assertGreaterThan(0, vmpRead.supportsCapability("WHAT".getBytes()));

    }

    public void testNetworkConstructor() throws Exception {
        CapabilitiesVM.SupportedMessageBlock smp1 = 
            new CapabilitiesVM.SupportedMessageBlock("SUSH".getBytes(), 10);
        CapabilitiesVM.SupportedMessageBlock smp2 = 
            new CapabilitiesVM.SupportedMessageBlock("NEIL".getBytes(), 5);
        CapabilitiesVM.SupportedMessageBlock smp3 = 
            new CapabilitiesVM.SupportedMessageBlock("DAWG".getBytes(), 3);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] guid = GUID.makeGuid();
        byte ttl = 1, hops = 0;
        ByteOrder.short2leb((short)4, baos);
        baos.write(smp1.encode());
        baos.write(smp2.encode());
        baos.write(smp3.encode());
        baos.write(smp3.encode());
        VendorMessage vm = new CapabilitiesVM(guid, ttl, hops, 0, 
                                              baos.toByteArray());
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        CapabilitiesVM vmp = 
           (CapabilitiesVM) Message.read(bais);
        // make sure it supports everything we expect....
        assertEquals(10, vmp.supportsCapability("SUSH".getBytes()));
        assertEquals(5,  vmp.supportsCapability("NEIL".getBytes()));
        assertEquals(3,  vmp.supportsCapability("DAWG".getBytes()));
        assertEquals(-1, vmp.supportsWhatIsCapability());
        assertFalse(vmp.supportsWhatIsNew());

        // now creat another one, mix up the blocks that are supported, and
        // make sure they are equal....
        baos = new ByteArrayOutputStream();
        ByteOrder.short2leb((short)3, baos);
        baos.write(smp2.encode());
        baos.write(smp3.encode());
        baos.write(smp1.encode());
        
        CapabilitiesVM vmpOther = 
            new CapabilitiesVM(guid, ttl, hops, 0, baos.toByteArray());

        assertEquals(vmp, vmpOther);

    }


    public void testBadCases() throws Exception {
        CapabilitiesVM.SupportedMessageBlock smp1 = 
            new CapabilitiesVM.SupportedMessageBlock("SUSH".getBytes(), 10);
        CapabilitiesVM.SupportedMessageBlock smp2 = 
            new CapabilitiesVM.SupportedMessageBlock("NEIL".getBytes(), 5);
        CapabilitiesVM.SupportedMessageBlock smp3 = 
            new CapabilitiesVM.SupportedMessageBlock("DAWG".getBytes(), 3);
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
            CapabilitiesVM vmpOther = 
                new CapabilitiesVM(guid, ttl, hops, 0, baos.toByteArray());
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
            CapabilitiesVM vmpOther = 
                new CapabilitiesVM(guid, ttl, hops, 0, 
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
        CapabilitiesVM vmpOther = 
            new CapabilitiesVM(guid, ttl, hops, 0, baos.toByteArray());
        baos = new ByteArrayOutputStream();
        ByteOrder.short2leb((short)3, baos);
        baos.write(smp2.encode());
        baos.write(smp3.encode());
        baos.write(smp1.encode());
        CapabilitiesVM vmpOneOther = 
            new CapabilitiesVM(guid, ttl, hops, 0, baos.toByteArray());
        assertNotEquals(vmpOther,vmpOneOther);

    }


}
