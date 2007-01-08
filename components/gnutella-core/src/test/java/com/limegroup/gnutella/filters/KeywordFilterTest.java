package com.limegroup.gnutella.filters;

import junit.framework.Test;

import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Unit tests for KeywordFilter
 */
public class KeywordFilterTest extends LimeTestCase {
    
    KeywordFilter filter=new KeywordFilter();
    QueryRequest qr=null;    
    
	public KeywordFilterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(KeywordFilterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testLegacy() {        
        qr=QueryRequest.createQuery("Britney", (byte)1);
        assertTrue(filter.allow(qr));
        filter.disallow("britney spears");
        assertTrue(filter.allow(qr));
        
        qr=QueryRequest.createQuery("pie with rhubarb", (byte)1);
        assertTrue(filter.allow(qr));
        filter.disallow("rhuBarb");
        assertTrue(!filter.allow(qr));
        qr=QueryRequest.createQuery("rhubarb.txt", (byte)1);
        assertTrue(!filter.allow(qr));
        qr=QueryRequest.createQuery("Rhubarb*", (byte)1);
        assertTrue(!filter.allow(qr));
        
        filter.disallowVbs();
        qr=QueryRequest.createQuery("test.vbs", (byte)1);
        assertTrue(!filter.allow(qr));
        
        filter.disallowHtml();
        qr=QueryRequest.createQuery("test.htm", (byte)1);
        assertTrue(!filter.allow(qr));
    }
}
