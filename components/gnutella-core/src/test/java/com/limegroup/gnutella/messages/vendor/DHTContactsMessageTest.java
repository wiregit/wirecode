package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;

import junit.framework.Test;

import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.util.LimeTestCase;

public class DHTContactsMessageTest extends LimeTestCase {
    
    public DHTContactsMessageTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DHTContactsMessageTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSerialization() throws Exception {
        Contact c1 = ContactFactory.createUnknownContact(
                Vendor.UNKNOWN, Version.UNKNOWN, 
                KUID.createRandomID(), new InetSocketAddress("localhost", 3000));
        
        Contact c2 = ContactFactory.createUnknownContact(
                Vendor.UNKNOWN, Version.UNKNOWN, 
                KUID.createRandomID(), new InetSocketAddress("localhost", 3001));
        
        DHTContactsMessage msg1 = new DHTContactsMessage(Arrays.asList(c1, c2));
        
        assertEquals(2, msg1.getContacts().size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        msg1.write(baos);
        baos.close();
        
        byte[] raw = baos.toByteArray();
        
        byte[] guid = new byte[16];
        System.arraycopy(raw, 0, guid, 0, guid.length);
        byte func = raw[16];
        byte ttl = raw[17];
        byte hops = raw[18];
        byte[] payload = new byte[raw.length - 23];
        System.arraycopy(raw, 23, payload, 0, payload.length);
        
        DHTContactsMessage msg2 = (DHTContactsMessage)VendorMessageFactory.deriveVendorMessage(
                guid, ttl, hops, payload, DHTContactsMessage.N_UDP);
        
        assertEquals(2, msg2.getContacts().size());
        
        assertTrue(msg2.getContacts().contains(c1));
        assertTrue(msg2.getContacts().contains(c2));
    }
}
