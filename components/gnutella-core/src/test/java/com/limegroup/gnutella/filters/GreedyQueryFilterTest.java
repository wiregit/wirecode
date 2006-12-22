package com.limegroup.gnutella.filters;

import junit.framework.Test;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;
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
        msg=new PingRequest((byte)5);
        assertTrue(filter.allow(msg));

        msg=QueryRequest.createQuery("a",(byte)5);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createQuery("*", (byte)5);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createQuery("a.asf", (byte)5);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createQuery("z.mpg", (byte)5);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createQuery("z.mp", (byte)5);
        assertTrue(filter.allow(msg));

        msg=QueryRequest.createQuery("z mpg", (byte)5);
        assertTrue(filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "*.mpg".getBytes(), Message.N_UNKNOWN);
        assertTrue(!filter.allow(msg));

        msg=QueryRequest.createQuery("1.mpg", (byte)5);
        assertTrue(filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "*.mp3".getBytes(), Message.N_UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "*.*".getBytes(), Message.N_UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "*.MP3".getBytes(), Message.N_UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "*.MPG".getBytes(), Message.N_UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "mp3".getBytes(), Message.N_UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "mpg".getBytes(), Message.N_UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "MP3".getBytes(), Message.N_UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "MPG".getBytes(), Message.N_UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "a.b".getBytes(), Message.N_UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "*.*-".getBytes(), Message.N_UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)3, 
            (byte)2, "--**.*-".getBytes(), Message.N_UNKNOWN);
        assertTrue(filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)1, 
            (byte)4, "*****".getBytes(), Message.N_UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)2,
            (byte)3, "britney*.*".getBytes(), Message.N_UNKNOWN);
        assertTrue(filter.allow(msg)); 
    
        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)2,
            (byte)3, "*.*.".getBytes(), Message.N_UNKNOWN);
        assertTrue(! filter.allow(msg));

        msg=QueryRequest.createNetworkQuery(GUID.makeGuid(), (byte)1,
            (byte)6, "new order*".getBytes(), Message.N_UNKNOWN);
        assertTrue(filter.allow(msg)); 
    
    }
}
