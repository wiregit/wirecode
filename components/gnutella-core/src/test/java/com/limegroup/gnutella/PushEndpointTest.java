	
package com.limegroup.gnutella;

import junit.framework.Test;

import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.*;
import java.util.*;

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
    
    
    static Map m;
    
    public static void globalSetUp(){
        try{
            m= (Map)PrivilegedAccessor.getValue(PushEndpoint.class, "GUID_PROXY_MAP");
        }catch(Exception bad) {
            ErrorService.error(bad);
        }
    }
    
    
    public void testConstructors() throws Exception {
    	GUID guid1 = new GUID(GUID.makeGuid());
    	GUID guid2 = new GUID(GUID.makeGuid());
    	GUID guid3 = new GUID(GUID.makeGuid());
    	
    	PushProxyInterface ppi1 = new QueryReply.PushProxyContainer("1.2.3.4",1234);
    	PushProxyInterface ppi2 = new QueryReply.PushProxyContainer("1.2.3.5",1235);
		
    	Set set1 = new HashSet();
    	Set set2 = new HashSet();
    	
    	set1.add(ppi1); 
    	set2.add(ppi1);
    	set2.add(ppi2);
    	
    	PushEndpoint empty = new PushEndpoint(guid1.bytes());
    	empty.updateProxies(true);
    	assertEquals(guid1,new GUID(empty.getClientGUID()));
    	assertEquals(PushEndpoint.HEADER_SIZE,PushEndpoint.getSizeBytes(empty.getProxies()));
    	assertEquals(0,empty.getProxies().size());
    	
    	PushEndpoint one = new PushEndpoint(guid2.bytes(),set1);
    	assertEquals(PushEndpoint.HEADER_SIZE+PushEndpoint.PROXY_SIZE,
    			PushEndpoint.getSizeBytes(one.getProxies()));
    	assertEquals(1,one.getProxies().size());
    	assertEquals(0,one.supportsFWTVersion());
    	
    	PushEndpoint two = new PushEndpoint(guid2.bytes(),set2);
    	two.updateProxies(true);
    	assertEquals(PushEndpoint.HEADER_SIZE+2*PushEndpoint.PROXY_SIZE,
    			PushEndpoint.getSizeBytes(two.getProxies()));
    	assertEquals(2,two.getProxies().size());
    	assertEquals(0,two.supportsFWTVersion());
    	
    	//test features
    	PushEndpoint three = new PushEndpoint(guid3.bytes(),set2,0x0,1);
    	three.updateProxies(true);
    	assertGreaterThan(0,three.supportsFWTVersion());
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
    	one.updateProxies(true);
    	
    	assertEquals(0,one.supportsFWTVersion());
    	byte [] network = one.toBytes();
    	byte [] network2 = new byte [PushEndpoint.getSizeBytes(one.getProxies())+5];
    	one.toBytes(network2,2);
    	
    	assertEquals(PushEndpoint.getSizeBytes(one.getProxies()),network.length);
    	PushEndpoint one_prim= PushEndpoint.fromBytes(network);
    	assertEquals(one,one_prim);
    	one_prim = PushEndpoint.fromBytes(network2,2);
    	assertEquals(one,one_prim);
    	assertEquals(0,one_prim.supportsFWTVersion());
    	
    	PushEndpoint six = new PushEndpoint(guid2.bytes(),set6,
    			0,2);
    	six.updateProxies(true);
    	assertEquals(2,six.supportsFWTVersion());
    	network = six.toBytes();
    	assertEquals(PushEndpoint.getSizeBytes(six.getProxies()),network.length);
    	m.clear();
    	PushEndpoint four = PushEndpoint.fromBytes(network);
    	assertEquals(2,four.supportsFWTVersion());
    	assertEquals(4,four.getProxies().size());
    	
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
    	one.updateProxies(true);
    	
    	String httpString = one.httpStringValue();
    	
    	m.clear();
    	PushEndpoint one_prim = new PushEndpoint(httpString);
    	one_prim.updateProxies(true);
    	
    	
    	assertEquals(1,one_prim.getProxies().size());
    	set1.retainAll(one_prim.getProxies());
    	assertEquals(1,set1.size());
    	
    	//now test a bigger endpoint
       	PushEndpoint six = new PushEndpoint(guid2.bytes(),set6,0,2);
       	six.updateProxies(true);
    	httpString = six.httpStringValue();
    	
    	m.clear();
    	PushEndpoint four = new PushEndpoint(httpString);
    	four.updateProxies(true);
    	    	
    	assertEquals(2,four.supportsFWTVersion());
    	assertEquals(4,four.getProxies().size());
    	
    	Set sent = new HashSet(set6);
    	sent.retainAll(four.getProxies());
    	assertEquals(four.getProxies().size(),sent.size());
    	
    	//now an endpoint with a feature we do not understand
    	m.clear();
    	
        PushEndpoint unknown = new PushEndpoint("2A8CA57F43E6E0B7FF823F0CC7880500;someFeature/2.3;1.2.3.5:1235;1.2.3.6:1235");
        unknown.updateProxies(true);
    	assertEquals(2,unknown.getProxies().size());
    	assertEquals(0,unknown.supportsFWTVersion());
    	
    	//now an endpoint with the fwt header moved elsewhere
    	m.clear();
    	unknown = new PushEndpoint("2A8CA57F43E6E0B7FF823F0CC7880500;1.2.3.5:1235;fwt/1.3;1.2.3.6:1235");
    	unknown.updateProxies(true);
    	assertEquals(2,unknown.getProxies().size());
    	assertEquals(1,unknown.supportsFWTVersion());
    	
    	//now an endpoint only with the guid
    	m.clear();
    	unknown = new PushEndpoint("2A8CA57F43E6E0B7FF823F0CC7880500");
    	unknown.updateProxies(true);
    	assertEquals(0,unknown.getProxies().size());
    	assertEquals(0,unknown.supportsFWTVersion());
    	
    	
    }
    
}
