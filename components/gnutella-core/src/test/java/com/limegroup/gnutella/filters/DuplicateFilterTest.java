package com.limegroup.gnutella.filters;

import junit.framework.Test;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Unit tests for DuplicateFilter
 */
public class DuplicateFilterTest extends LimeTestCase {
    
    SpamFilter filter=new DuplicateFilter();
    PingRequest pr=null;
    QueryRequest qr=null;    
    
	public DuplicateFilterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(DuplicateFilterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    
    public void testGUIDDuplicate() throws Exception {        
        pr=ProviderHacks.getPingRequestFactory().createPingRequest((byte)2);
        byte[] guid=pr.getGUID();
        guid[9]++;
		byte[] payload = { 0, 0, 65 };
        qr=ProviderHacks.getQueryRequestFactory().createNetworkQuery(
            guid, pr.getTTL(), pr.getHops(), payload, Network.UNKNOWN);
        assertTrue(filter.allow(pr));
        assertTrue(!filter.allow(qr));
        pr=ProviderHacks.getPingRequestFactory().createPingRequest((byte)2);
        assertTrue(filter.allow(pr)); //since GUIDs are currently random
        assertTrue(!filter.allow(pr));
        
        //Now, if I wait a few seconds, it should be allowed.
        synchronized (filter) {
            try {
                filter.wait(getLag("GUID_LAG")*2);
            } catch (InterruptedException e) { }
        }
        
        assertTrue(filter.allow(pr));  
        assertTrue(!filter.allow(pr));
        pr=ProviderHacks.getPingRequestFactory().createPingRequest((byte)2);
        assertTrue(filter.allow(pr));
        pr.hop(); //hack to get different hops count
        assertTrue(filter.allow(pr));
    }
        
    public void testQueryStringDuplicate() throws Exception {
        qr=ProviderHacks.getQueryRequestFactory().createQuery("search1", (byte)2);
        assertTrue(filter.allow(qr));
        qr=ProviderHacks.getQueryRequestFactory().createQuery("search1", (byte)2);
        assertTrue(!filter.allow(qr));
        qr=ProviderHacks.getQueryRequestFactory().createQuery("search2", (byte)2);
        assertTrue(filter.allow(qr));

        //Now, if I wait a few seconds, it should be allowed.
        synchronized (filter) {
            try {
                filter.wait(getLag("QUERY_LAG")*4);
            } catch (InterruptedException e) { }
        }

        assertTrue(filter.allow(qr));
        qr=ProviderHacks.getQueryRequestFactory().createQuery("search2", (byte)2);
        assertTrue(!filter.allow(qr));
        qr=ProviderHacks.getQueryRequestFactory().createQuery("search3",(byte)2);
        assertTrue(filter.allow(qr));
        qr.hop(); //hack to get different hops count
        assertTrue(filter.allow(qr));
    }
        
    public void testURNDuplicate() throws Exception  {
        qr=ProviderHacks.getQueryRequestFactory().createQuery(HugeTestUtils.SHA1);
        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));
        qr=ProviderHacks.getQueryRequestFactory().createQuery(HugeTestUtils.UNIQUE_SHA1);
        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));

        //Now, if I wait a few seconds, it should be allowed.
        synchronized (filter) {
            try {
                filter.wait(getLag("QUERY_LAG")*4);
            } catch (InterruptedException e) { }
        }
        
        assertTrue(filter.allow(qr));
    }

    public void testXMLDuplicate() throws Exception {
        // Only allowed once in the timeframe ...
        qr = ProviderHacks.getQueryRequestFactory().createQuery("tests");
        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));
        // Invalid XML, considered same as plaintext.
        qr = ProviderHacks.getQueryRequestFactory().createQuery("tests", "<?xml");
        assertTrue(!filter.allow(qr));
        qr = ProviderHacks.getQueryRequestFactory().createQuery("tests",
            "<?xml version=\"1.0\"?>" +
            "<audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\">" +
            "<audio title=\"sam\" artist=\"sam's band\"></audio></audios>");
        // same plain-text, different XML, allowed ...
        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));
        qr = ProviderHacks.getQueryRequestFactory().createQuery("another test",
            "<?xml version=\"1.0\"?>" +
            "<audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\">" +
            "<audio title=\"sam\" artist=\"sam's band\"></audio></audios>");
        // same XML, different plaint-text, allowed ...
        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));
        qr = ProviderHacks.getQueryRequestFactory().createQuery("another test",
            "<?xml version=\"1.0\"?>" +
            "<audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\">" +
            "<audio title=\"sam\" artist=\"sam's choir\"></audio></audios>");        
        // different XML, allowed ...
        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));        
        qr = ProviderHacks.getQueryRequestFactory().createQuery("another test",
            "<?xml version=\"1.0\"?>" +
            "<audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\">" +
            "<audio title=\"sam\" artist=\"sam's choir\"></audio></audios>");        
        //same XML and plain-text, not allowed.
        assertTrue(!filter.allow(qr));
    }
    
    private static int getLag(String lag) throws Exception {
        return ((Integer)PrivilegedAccessor.getValue(
            DuplicateFilter.class, lag)).intValue();
    }
}    
