
package com.limegroup.gnutella;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.messages.*;
import com.sun.java.util.collections.*;

/**
 * tests the PushEndpoint class.
 */
public class PushEndpointTest extends BaseTestCase {

	public PushEndpointTest(String name) {
		super(name);
	}
	
    public static Test suite() {
        return buildTestSuite(PushEndpointTest.class);
    }
    
    public void testConstructors() throws Exception {
    	GUID guid1 = new GUID(GUID.makeGuid());
    	GUID guid2 = new GUID(GUID.makeGuid());
    	
    	PushProxyInterface ppi1 = new QueryReply.PushProxyContainer("1.2.3.4",1234);
    	PushProxyInterface ppi2 = new QueryReply.PushProxyContainer("1.2.3.5",1235);
		
    	Set set1 = new HashSet();
    	Set set2 = new HashSet();
    	
    	set1.add(ppi1); 
    	set2.add(ppi1);
    	set2.add(ppi2);
    	
    	PushEndpoint empty = new PushEndpoint(guid1.bytes());
    	
    	assertEquals(guid1,new GUID(empty.getClientGUID()));
    	assertEquals(PushEndpoint.HEADER_SIZE,empty.getSizeBytes());
    	assertEquals(0,empty.getProxies().size());
    	
    	PushEndpoint one = new PushEndpoint(guid2.bytes(),set1);
    	assertEquals(PushEndpoint.HEADER_SIZE+PushEndpoint.PROXY_SIZE,
    			one.getSizeBytes());
    	assertEquals(1,one.getProxies().size());
    	assertFalse(one.supportsFWTransfers());
    	
    	PushEndpoint two = new PushEndpoint(guid2.bytes(),set2);
    	assertEquals(PushEndpoint.HEADER_SIZE+2*PushEndpoint.PROXY_SIZE,
    			two.getSizeBytes());
    	assertEquals(2,two.getProxies().size());
    	assertFalse(two.supportsFWTransfers());
    	
    	//test features
    	PushEndpoint three = new PushEndpoint(guid2.bytes(),set2,0xFF);
    	assertTrue(three.supportsFWTransfers());
    	
    	//the equals method ignores the features since all we care about 
    	//is the ip address when putting in maps and sets.
    	assertEquals(two,three);
    }
    
    
    public void testExternalization() throws Exception {
    	GUID guid1 = new GUID(GUID.makeGuid());
    	GUID guid2 = new GUID(GUID.makeGuid());
    	
    	PushProxyInterface ppi1 = new QueryReply.PushProxyContainer("1.2.3.4",1234);
    	PushProxyInterface ppi2 = new QueryReply.PushProxyContainer("1.2.3.5",1235);
    	PushProxyInterface ppi3 = new QueryReply.PushProxyContainer("1.2.3.6",1235);
    	PushProxyInterface ppi4 = new QueryReply.PushProxyContainer("1.2.3.7",1235);
    	PushProxyInterface ppi5 = new QueryReply.PushProxyContainer("1.2.3.8",1235);
    	PushProxyInterface ppi6 = new QueryReply.PushProxyContainer("1.2.3.9",1235);
		
    	Set set1 = new HashSet();
    	Set set6 = new HashSet();
    	
    	set1.add(ppi1); 
    	set6.add(ppi1);set6.add(ppi2);set6.add(ppi3);set6.add(ppi4);
    	set6.add(ppi5);set6.add(ppi6);
    	
    	PushEndpoint one = new PushEndpoint(guid1.bytes(),set1);
    	assertFalse(one.supportsFWTransfers());
    	byte [] network = one.toBytes();
    	byte [] network2 = new byte [one.getSizeBytes()+5];
    	one.toBytes(network2,2);
    	
    	assertEquals(one.getSizeBytes(),network.length);
    	PushEndpoint one_prim= PushEndpoint.fromBytes(network);
    	assertEquals(one,one_prim);
    	one_prim = PushEndpoint.fromBytes(network2,2);
    	assertEquals(one,one_prim);
    	assertFalse(one_prim.supportsFWTransfers());
    	
    	PushEndpoint six = new PushEndpoint(guid2.bytes(),set6,
    			PushEndpoint.F2F_TRANSFER);
    	assertTrue(six.supportsFWTransfers());
    	network = six.toBytes();
    	assertEquals(six.getSizeBytes(),network.length);
    	PushEndpoint four = PushEndpoint.fromBytes(network);
    	assertNotEquals(six,four);
    	assertTrue(four.supportsFWTransfers());
    	
    	Set sent = new HashSet(set6);
    	sent.retainAll(four.getProxies());
    	assertEquals(four.getProxies().size(),sent.size());
    	
    }
    
    /**
     * tests externalization to http header values.
     * TODO: update this test after format is finalized.
     */
    public void testExternalizationHTTP() throws Exception {
    	GUID guid1 = new GUID(GUID.makeGuid());
    	GUID guid2 = new GUID(GUID.makeGuid());
    	
    	PushProxyInterface ppi1 = new QueryReply.PushProxyContainer("1.2.3.4",1234);
    	PushProxyInterface ppi2 = new QueryReply.PushProxyContainer("1.2.3.5",1235);
    	PushProxyInterface ppi3 = new QueryReply.PushProxyContainer("1.2.3.6",1235);
    	PushProxyInterface ppi4 = new QueryReply.PushProxyContainer("1.2.3.7",1235);
    	PushProxyInterface ppi5 = new QueryReply.PushProxyContainer("1.2.3.8",1235);
    	PushProxyInterface ppi6 = new QueryReply.PushProxyContainer("1.2.3.9",1235);
		
    	Set set1 = new HashSet();
    	Set set6 = new HashSet();
    	
    	set1.add(ppi1); 
    	set6.add(ppi1);set6.add(ppi2);set6.add(ppi3);set6.add(ppi4);
    	set6.add(ppi5);set6.add(ppi6);
    	
    	PushEndpoint one = new PushEndpoint(guid1.bytes(),set1);
    	
    	String httpString = one.httpStringValue();
    	
    	
    	PushEndpoint one_prim = new PushEndpoint(httpString);
    	
    	assertEquals(one.hashCode(),one_prim.hashCode());
    	assertEquals(one,one_prim);
    	
    	//now test a bigger endpoint
       	PushEndpoint six = new PushEndpoint(guid2.bytes(),set6);
    	httpString = six.httpStringValue();
    	
    	PushEndpoint four = new PushEndpoint(httpString);
    	assertNotEquals(six,four);
    	
    	Set sent = new HashSet(set6);
    	sent.retainAll(four.getProxies());
    	assertEquals(four.getProxies().size(),sent.size());
    	
    }
}
