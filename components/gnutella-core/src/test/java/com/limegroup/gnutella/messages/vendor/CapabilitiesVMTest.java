package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.Test;

import org.limewire.concurrent.AtomicLazyReference;
import org.limewire.util.ByteOrder;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.dht.DHTManagerStub;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;

/** Tests the important MessagesSupportedVendorMessage.
 */
public class CapabilitiesVMTest  extends LimeTestCase {
    public CapabilitiesVMTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(CapabilitiesVMTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void setUp() throws Exception {
        PrivilegedAccessor.setValue(CapabilitiesVM.class, "_instance", null);
    }
    
    public void testStaticConstructor() throws Exception {
        CapabilitiesVM vmp = CapabilitiesVM.instance();
        assertGreaterThan(0, vmp.supportsFeatureQueries());
        assertTrue(vmp.supportsWhatIsNew());
        assertGreaterThan(0, vmp.supportsCapability("WHAT".getBytes()));
        assertEquals(-1, vmp.supportsCapability("MDHT".getBytes()));
    
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vmp.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        CapabilitiesVM vmpRead = (CapabilitiesVM) MessageFactory.read(bais);
        assertEquals(vmp, vmpRead);

        assertGreaterThan(0, vmpRead.supportsFeatureQueries());
        assertTrue(vmpRead.supportsWhatIsNew());
        assertGreaterThan(0, vmpRead.supportsCapability("WHAT".getBytes()));
        assertEquals(-1, vmp.supportsCapability("MDHT".getBytes()));
    }
    
    public void testDHTCapability() throws Exception { 
        CapabilitiesVM vmp = CapabilitiesVM.instance();
        assertEquals(-1, vmp.supportsCapability("MDHT".getBytes()));
        
        RouterService rs = new RouterService(new ActivityCallbackStub());
        AtomicLazyReference ref = (AtomicLazyReference)PrivilegedAccessor.getValue(
                rs, "DHT_MANAGER_REFERENCE");
        PrivilegedAccessor.setValue(ref, "obj", new DHTManagerStub());
        
        CapabilitiesVM.reconstructInstance();
        vmp = CapabilitiesVM.instance();
        assertGreaterThan(-1, vmp.isActiveDHTNode());
    }
    
    public void testTLSCapability() throws Exception {
        ConnectionSettings.TLS_INCOMING.setValue(false);
        CapabilitiesVM vmp = CapabilitiesVM.instance();
        assertEquals(-1, vmp.supportsTLS());
        assertEquals(-1, vmp.supportsCapability("TLS!".getBytes()));
        
        ConnectionSettings.TLS_INCOMING.setValue(true);
        CapabilitiesVM.reconstructInstance();
        vmp = CapabilitiesVM.instance();
        assertEquals(1, vmp.supportsTLS());
        assertEquals(1, vmp.supportsCapability("TLS!".getBytes()));
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
        smp1.encode(baos);
        smp2.encode(baos);
        smp3.encode(baos);
        smp3.encode(baos);
        VendorMessage vm = new CapabilitiesVM(guid, ttl, hops, 0, 
                                              baos.toByteArray());
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        CapabilitiesVM vmp = 
           (CapabilitiesVM) MessageFactory.read(bais);
        // make sure it supports everything we expect....
        assertEquals(10, vmp.supportsCapability("SUSH".getBytes()));
        assertEquals(5,  vmp.supportsCapability("NEIL".getBytes()));
        assertEquals(3,  vmp.supportsCapability("DAWG".getBytes()));
        assertEquals(-1, vmp.supportsFeatureQueries());
        assertFalse(vmp.supportsWhatIsNew());

        // now creat another one, mix up the blocks that are supported, and
        // make sure they are equal....
        baos = new ByteArrayOutputStream();
        ByteOrder.short2leb((short)3, baos);
        smp2.encode(baos);
        smp3.encode(baos);
        smp1.encode(baos);
        
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
            smp2.encode(baos);
            smp3.encode(baos);
            smp1.encode(baos);
            new CapabilitiesVM(guid, ttl, hops, 0, baos.toByteArray());
            fail("bpe should have been thrown.");
        } catch (BadPacketException expected) {
        }
        try {
            // test corrupt info....
            baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)4, baos);
            smp2.encode(baos);
            smp3.encode(baos);
            smp1.encode(baos);
            baos.write("crap".getBytes());
            new CapabilitiesVM(guid, ttl, hops, 0, 
                                                   baos.toByteArray());
            fail("bpe should have been thrown.");
        } catch (BadPacketException expected) {
        }

        // test semantics....
        baos = new ByteArrayOutputStream();
        ByteOrder.short2leb((short)0, baos);
        smp2.encode(baos);
        smp3.encode(baos);
        smp1.encode(baos);
        CapabilitiesVM vmpOther = 
            new CapabilitiesVM(guid, ttl, hops, 0, baos.toByteArray());
        baos = new ByteArrayOutputStream();
        ByteOrder.short2leb((short)3, baos);
        smp2.encode(baos);
        smp3.encode(baos);
        smp1.encode(baos);
        CapabilitiesVM vmpOneOther = 
            new CapabilitiesVM(guid, ttl, hops, 0, baos.toByteArray());
        assertNotEquals(vmpOther,vmpOneOther);

    }
}
