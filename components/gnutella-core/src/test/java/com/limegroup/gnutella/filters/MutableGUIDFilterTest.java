package com.limegroup.gnutella.filters;

import java.net.InetAddress;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.messages.QueryReply;

public class MutableGUIDFilterTest extends BaseTestCase {

    private byte[] address; 
    
    public MutableGUIDFilterTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        address = InetAddress.getLocalHost().getAddress();
    }
    
    public static Test suite() {
        return buildTestSuite(MutableGUIDFilterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     * Tests if more than one guid can be added by seeing if messages
     * for it are filterted. Also test removal.
     */
    public void testAddRemoveGUID() {
        MutableGUIDFilter filter = MutableGUIDFilter.instance();
        
        GUID guid = new GUID();
        filter.addGUID(guid.bytes());
        
        QueryReply qr = KeywordFilterTest.createReply(ProviderHacks.getResponseFactory().createResponse(1, 2, "sex"), guid, 777, address);
        QueryReply unrelated = KeywordFilterTest.createReply(ProviderHacks.getResponseFactory().createResponse(1, 2, "sex"), new GUID(), 777, address);
        assertFalse(filter.allow(qr));
        assertTrue(filter.allow(unrelated));
        
        GUID guid2 = new GUID();
        filter.addGUID(guid2.bytes());
        
        QueryReply qr2 = KeywordFilterTest.createReply(ProviderHacks.getResponseFactory().createResponse(1, 2, "sex"), guid2, 777, address);
        
        assertFalse(filter.allow(qr2));
        assertFalse(filter.allow(qr));
        assertTrue(filter.allow(unrelated));
        
        filter.removeGUID(guid.bytes());
        assertFalse(filter.allow(qr2));
        assertTrue(filter.allow(qr));
        assertTrue(filter.allow(unrelated));
        
        // remove element that's not filtered
        filter.removeGUID(guid.bytes());
        assertFalse(filter.allow(qr2));
        assertTrue(filter.allow(qr));
        assertTrue(filter.allow(unrelated));
        
        filter.removeGUID(guid2.bytes());
        assertTrue(filter.allow(qr));
        assertTrue(filter.allow(qr2));
        assertTrue(filter.allow(unrelated));
    }

}
