package com.limegroup.gnutella.filters;

import junit.framework.Test;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Unit tests for GreedyQueryFilter
 */
public class GreedyQueryFilterTest extends LimeTestCase {
    
    SpamFilter filter=new GreedyQueryFilter();
    Message msg = null;
    
	public GreedyQueryFilterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(GreedyQueryFilterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    public void testLegacy() throws Exception {
        msg=ProviderHacks.getPingRequestFactory().createPingRequest((byte)5);
        assertTrue(filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createQuery("a",(byte)5);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createQuery("*", (byte)5);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createQuery("a.asf", (byte)5);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createQuery("z.mpg", (byte)5);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createQuery("z.mp", (byte)5);
        assertTrue(filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createQuery("z mpg", (byte)5);
        assertTrue(filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "*.mpg".getBytes(), Network.UNKNOWN);
        assertTrue(!filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createQuery("1.mpg", (byte)5);
        assertTrue(filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "*.mp3".getBytes(), Network.UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "*.*".getBytes(), Network.UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "*.MP3".getBytes(), Network.UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "*.MPG".getBytes(), Network.UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "mp3".getBytes(), Network.UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "mpg".getBytes(), Network.UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "MP3".getBytes(), Network.UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "MPG".getBytes(), Network.UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "a.b".getBytes(), Network.UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "*.*-".getBytes(), Network.UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)3, 
            (byte)2, "--**.*-".getBytes(), Network.UNKNOWN);
        assertTrue(filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "*****".getBytes(), Network.UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)2,
            (byte)3, "britney*.*".getBytes(), Network.UNKNOWN);
        assertTrue(filter.allow(msg)); 
    
        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)2,
            (byte)3, "*.*.".getBytes(), Network.UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=ProviderHacks.getQueryRequestFactory().createNetworkQuery(GUID.makeGuid(), (byte)1,
            (byte)6, "new order*".getBytes(), Network.UNKNOWN);
        assertTrue(filter.allow(msg)); 
    
    }
}
