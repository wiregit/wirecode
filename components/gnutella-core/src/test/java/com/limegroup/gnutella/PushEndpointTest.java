	
package com.limegroup.gnutella;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.service.ErrorService;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

/**
 * tests the PushEndpoint class.
 */
@SuppressWarnings("unchecked")
public class PushEndpointTest extends LimeTestCase {

	public PushEndpointTest(String name) {
		super(name);
	}
	static Map m;
	public static void globalSetUp () {
	    try {
	        m = (Map)PrivilegedAccessor.getValue(PushEndpoint.class,
	                "GUID_PROXY_MAP");
	    }catch(Exception bad){
	        ErrorService.error(bad);
	    }
	}
    public static Test suite() {
        return buildTestSuite(PushEndpointTest.class);
    }
    
    public void testConstructors() throws Exception {
    	GUID guid1 = new GUID(GUID.makeGuid());
    	GUID guid2 = new GUID(GUID.makeGuid());
    	GUID guid3 = new GUID(GUID.makeGuid());
    	
    	IpPort ppi1 = new IpPortImpl("1.2.3.4",1234);
    	IpPort ppi2 = new IpPortImpl("1.2.3.5",1235);
		
    	Set set1 = new HashSet();
    	Set set2 = new HashSet();
    	
    	set1.add(ppi1); 
    	set2.add(ppi1);
    	set2.add(ppi2);
    	
    	PushEndpoint empty = new PushEndpoint(guid1.bytes());
    	assertEquals(guid1,new GUID(empty.getClientGUID()));
    	assertEquals(PushEndpoint.HEADER_SIZE,PushEndpoint.getSizeBytes(empty.getProxies()));
    	assertEquals(0,empty.getProxies().size());
    	
    	PushEndpoint one = new PushEndpoint(guid2.bytes(),set1);
    	assertEquals(PushEndpoint.HEADER_SIZE+PushEndpoint.PROXY_SIZE,
    			PushEndpoint.getSizeBytes(one.getProxies()));
    	assertEquals(1,one.getProxies().size());
    	assertEquals(0,one.supportsFWTVersion());
    	
    	PushEndpoint two = new PushEndpoint(guid2.bytes(),set2);
    	assertEquals(PushEndpoint.HEADER_SIZE+2*PushEndpoint.PROXY_SIZE,
    			PushEndpoint.getSizeBytes(two.getProxies()));
    	assertEquals(2,two.getProxies().size());
    	assertEquals(0,two.supportsFWTVersion());
    	
    	//test features
    	PushEndpoint three = new PushEndpoint(guid3.bytes(),set2,0x0,1);
    	assertGreaterThan(0,three.supportsFWTVersion());
    	assertEquals("1.1.1.1",three.getAddress());
    	assertEquals(6346,three.getPort());
    	
    	//test IpPort constructor
    	IpPort ip = new IpPortImpl("1.2.3.4",5);
    	PushEndpoint four = new PushEndpoint(guid3.bytes(),set2,0x0,1,ip);
    	assertEquals("1.2.3.4",four.getAddress());
    	assertEquals(5,four.getPort());
    }
    
    
    public void testExternalization() throws Exception {
    	GUID guid1 = new GUID(GUID.makeGuid());
    	GUID guid2 = new GUID(GUID.makeGuid());
    	
    	IpPort ppi1 = new IpPortImpl("1.2.3.4",1234);
    	IpPort ppi2 = new IpPortImpl("1.2.3.5",1235);
    	IpPort ppi3 = new IpPortImpl("1.2.3.6",1235);
    	IpPort ppi4 = new IpPortImpl("1.2.3.7",1235);
    	IpPort ppi5 = new IpPortImpl("1.2.3.8",1235);
    	IpPort ppi6 = new IpPortImpl("1.2.3.9",1235);
		
    	Set set1 = new TreeSet(IpPort.COMPARATOR);
    	Set set6 = new TreeSet(IpPort.COMPARATOR);
    	
    	set1.add(ppi1); 
    	set6.add(ppi1);set6.add(ppi2);set6.add(ppi3);set6.add(ppi4);
    	set6.add(ppi5);set6.add(ppi6);
    	
    	PushEndpoint one = new PushEndpoint(guid1.bytes(),set1);
    	
    	assertEquals(0,one.supportsFWTVersion());
    	byte [] network = one.toBytes();
    	byte [] network2 = new byte [PushEndpoint.getSizeBytes(one.getProxies())+5];
    	one.toBytes(network2,2);
    	
    	assertEquals(PushEndpoint.getSizeBytes(one.getProxies()),network.length);
    	PushEndpoint one_prim= PushEndpoint.fromBytes(
                new DataInputStream(new ByteArrayInputStream(network)));
    	assertEquals(one,one_prim);
    	one_prim = PushEndpoint.fromBytes(
                new DataInputStream(new ByteArrayInputStream(network2,2,network2.length-2)));
    	assertEquals(one,one_prim);
    	assertEquals(0,one_prim.supportsFWTVersion());
    	
    	m.clear();
        // test a PE that claims it supports FWT but doesn't have external address -
        // its FWT status gets cleared
    	PushEndpoint six = new PushEndpoint(guid2.bytes(),set6,
    			0,2);
    	assertEquals(2,six.supportsFWTVersion());
    	network = six.toBytes();
    	assertEquals(PushEndpoint.getSizeBytes(six.getProxies()),network.length);
    	
    	m.clear();
    	PushEndpoint four = PushEndpoint.fromBytes(
                new DataInputStream(new ByteArrayInputStream(network)));
    	assertEquals(0,four.supportsFWTVersion());
    	assertEquals(4,four.getProxies().size());
        
    	Set sent = new TreeSet(IpPort.COMPARATOR);
        sent.addAll(set6);
    	assertTrue(set6.containsAll(four.getProxies()));
    	
    	// test a PE that carries its external address
    	m.clear();
    	PushEndpoint ext = new PushEndpoint(guid2.bytes(),set6,
    			0,2, new IpPortImpl("1.2.3.4",5));
    	network = ext.toBytes();
    	assertEquals(PushEndpoint.getSizeBytes(set6)+6,network.length);
    	
    	m.clear();
    	PushEndpoint ext2 = PushEndpoint.fromBytes(
                new DataInputStream(new ByteArrayInputStream(network)));
    	assertEquals(ext,ext2);
    	assertEquals("1.2.3.4",ext2.getAddress());
    	assertEquals(5,ext2.getPort());
    	assertEquals(4,ext2.getProxies().size());
    	
    	// test that a PE with external address which can't do FWT 
    	// does not use up the extra 6 bytes
    	
    	m.clear();
    	PushEndpoint noFWT = new PushEndpoint(guid2.bytes(),set6,
    	        0,0, new IpPortImpl("1.2.3.4",5));
    	network = noFWT.toBytes();
    	assertEquals(PushEndpoint.getSizeBytes(set6),network.length);
    	
    	m.clear();
    	PushEndpoint noFWT2 = PushEndpoint.fromBytes(
                new DataInputStream(new ByteArrayInputStream(network)));
    	assertEquals(noFWT,noFWT2);
    	assertEquals(RemoteFileDesc.BOGUS_IP,noFWT2.getAddress());
    	assertEquals(4,noFWT2.getProxies().size());
    	m.clear();
    	
    }
    
    /**
     * tests externalization to http header values.
     * TODO: update this test after format is finalized.
     */
    public void testExternalizationHTTP() throws Exception {
    	GUID guid1 = new GUID(GUID.makeGuid());
    	GUID guid2 = new GUID(GUID.makeGuid());
    	
    	IpPort ppi1 = new IpPortImpl("1.2.3.4",1234);
    	IpPort ppi2 = new IpPortImpl("1.2.3.5",1235);
    	IpPort ppi3 = new IpPortImpl("1.2.3.6",1235);
    	IpPort ppi4 = new IpPortImpl("1.2.3.7",1235);
    	IpPort ppi5 = new IpPortImpl("1.2.3.8",1235);
    	IpPort ppi6 = new IpPortImpl("1.2.3.9",1235);
		
    	Set set1 = new TreeSet(IpPort.COMPARATOR);
    	Set set6 = new TreeSet(IpPort.COMPARATOR);
    	
    	set1.add(ppi1); 
    	set6.add(ppi1);set6.add(ppi2);set6.add(ppi3);set6.add(ppi4);
    	set6.add(ppi5);set6.add(ppi6);
    	
    	PushEndpoint one = new PushEndpoint(guid1.bytes(),set1);
    	
    	String httpString = one.httpStringValue();
    	
    	PushEndpoint one_prim = new PushEndpoint(httpString);
    	
    	
    	assertEquals(1,one_prim.getProxies().size());
    	set1.retainAll(one_prim.getProxies());
    	assertEquals(1,set1.size());
    	
    	//now test a bigger endpoint with an ip in it
    	IpPort ip = new IpPortImpl("1.2.3.4",5);
       	PushEndpoint six = new PushEndpoint(guid2.bytes(),set6,0,2,ip);
    	httpString = six.httpStringValue();
    	
    	PushEndpoint four = new PushEndpoint(httpString);
    	    	
    	assertEquals(2,four.supportsFWTVersion());
    	assertEquals(4,four.getProxies().size());
    	assertEquals("1.2.3.4",four.getAddress());
    	assertEquals(5,four.getPort());
    	
    	Set sent = new TreeSet(IpPort.COMPARATOR);
        sent.addAll(set6);
    	sent.retainAll(four.getProxies());
    	assertEquals(four.getProxies().size(),sent.size());
    	
    	//now test an endpoint with an ip in it, but which does not support
    	//FWT.  We should not get the ip in the http representation
    	PushEndpoint noFWT = new PushEndpoint(guid2.bytes(),set1,0,0,ip);
    	httpString = noFWT.httpStringValue();
    	
    	PushEndpoint parsed = new PushEndpoint(httpString);
    	assertEquals(RemoteFileDesc.BOGUS_IP,parsed.getAddress());
    	
    	//************* parsing tests *****************//
    	
    	//now an endpoint with a feature we do not understand
    	
        PushEndpoint unknown = new PushEndpoint("2A8CA57F43E6E0B7FF823F0CC7880500;someFeature/2.3;1.2.3.5:1235;1.2.3.6:1235");
    	assertEquals(2,unknown.getProxies().size());
    	assertEquals(0,unknown.supportsFWTVersion());
    	
    	//now an endpoint with the fwt header moved elsewhere
    	unknown = new PushEndpoint("2A8CA57F43E6E0B7FF823F0CC7880500;1.2.3.5:1235;fwt/1.3;1.2.3.6:1235");
    	assertEquals(2,unknown.getProxies().size());
    	assertEquals(1,unknown.supportsFWTVersion());
    	
    	//now an endpoint only with the guid
    	unknown = new PushEndpoint("2A8CA57F43E6E0B7FF823F0CC7880500");
    	assertEquals(0,unknown.getProxies().size());
    	assertEquals(0,unknown.supportsFWTVersion());
    	
    	//now an endpoint only guid and port:ip
    	unknown = new PushEndpoint("2A8CA57F43E6E0B7FF823F0CC7880500;5:1.2.3.4");
    	assertEquals(0,unknown.getProxies().size());
    	assertEquals(0,unknown.supportsFWTVersion());
    	assertEquals("1.2.3.4",unknown.getAddress());
    	assertEquals(5,unknown.getPort());
    	
    	//now an endpoint only guid and two port:ips.. the second one should be ignored
    	unknown = new PushEndpoint("2A8CA57F43E6E0B7FF823F0CC7880500;5:1.2.3.4;6:2.3.4.5");
    	assertEquals(0,unknown.getProxies().size());
    	assertEquals(0,unknown.supportsFWTVersion());
    	assertEquals("1.2.3.4",unknown.getAddress());
    	assertEquals(5,unknown.getPort());    	
    	
    	
    }
    
}
