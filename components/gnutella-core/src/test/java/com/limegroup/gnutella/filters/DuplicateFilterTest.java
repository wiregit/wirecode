package com.limegroup.gnutella.filters;

import junit.framework.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.BaseTestCase;
import com.sun.java.util.collections.*;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.HugeTestUtils;

/**
 * Unit tests for DuplicateFilter
 */
public class DuplicateFilterTest extends BaseTestCase {
    
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
        pr=new PingRequest((byte)2);
        byte[] guid=pr.getGUID();
        guid[9]++;
		byte[] payload = { 0, 0, 65 };
        qr=QueryRequest.createNetworkQuery(
            guid, pr.getTTL(), pr.getHops(), payload, Message.N_UNKNOWN);
        assertTrue(filter.allow(pr));
        assertTrue(!filter.allow(qr));
        pr=new PingRequest((byte)2);
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
        pr=new PingRequest((byte)2);
        assertTrue(filter.allow(pr));
        pr.hop(); //hack to get different hops count
        assertTrue(filter.allow(pr));
    }
        
    public void testQueryStringDuplicate() throws Exception {
        qr=QueryRequest.createQuery("search1", (byte)2);
        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));
        qr=QueryRequest.createQuery("search2", (byte)2);
        assertTrue(filter.allow(qr));

        //Now, if I wait a few seconds, it should be allowed.
        synchronized (filter) {
            try {
                filter.wait(getLag("QUERY_LAG")*4);
            } catch (InterruptedException e) { }
        }

        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));
        qr=QueryRequest.createQuery("search3",(byte)2);
        assertTrue(filter.allow(qr));
        qr.hop(); //hack to get different hops count
        assertTrue(filter.allow(qr));
    }
        
    public void testURNDuplicate() throws Exception  {
        qr=QueryRequest.createQuery(HugeTestUtils.SHA1);
        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));
        qr=QueryRequest.createQuery(HugeTestUtils.UNIQUE_SHA1);
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
        qr = QueryRequest.createQuery("tests", "<?xml");
        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));
        qr = QueryRequest.createQuery("tests");
        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));
        qr = QueryRequest.createQuery("tests", "<?xml a");
        assertTrue(filter.allow(qr));
        assertTrue(!filter.allow(qr));
    }
    
    private static int getLag(String lag) throws Exception {
        return ((Integer)PrivilegedAccessor.getValue(
            DuplicateFilter.class, lag)).intValue();
    }
}    