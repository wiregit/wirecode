package com.limegroup.gnutella.filters;

import junit.framework.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.BaseTestCase;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.settings.*;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.GUID;

/**
 * Unit tests for RequeryFilter
 */
public class RequeryFilterTest extends BaseTestCase {
        
	public RequeryFilterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(RequeryFilterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testLegacy() throws Exception {
	    FilterSettings.FILTER_DUPLICATES.setValue(false);
	    FilterSettings.FILTER_GREEDY_QUERIES.setValue(false);
        SpamFilter filter=SpamFilter.newRouteFilter();
        assertTrue(filter.allow(new PingRequest((byte)3)));
        assertTrue(filter.allow(QueryRequest.createQuery("Hello")));
        assertTrue(filter.allow(QueryRequest.createQuery("Hello")));
        assertTrue(filter.allow(QueryRequest.createRequery("Hel lo")));
        assertTrue(filter.allow(QueryRequest.createQuery("asd")));
 
        byte[] guid=GUID.makeGuid();   //version 1
        guid[0]=(byte)0x02;
        guid[1]=(byte)0x01;
        guid[2]=(byte)0x17;
        guid[3]=(byte)0x05;
        guid[13]=(byte)0x2E;
        guid[14]=(byte)0x05;
        assertTrue(GUID.isLimeGUID(guid));
        assertTrue(GUID.isLimeRequeryGUID(guid, 1));
        assertTrue(! GUID.isLimeRequeryGUID(guid, 0));
        QueryRequest qr = QueryRequest.createNetworkQuery(
            guid, (byte)5, (byte)0, "asdf".getBytes(), Message.N_UNKNOWN );
        assertTrue(! filter.allow(qr) );
    }
}