
package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.util.BaseTestCase;

import java.util.Properties;
import java.io.IOException;


import junit.framework.Test;

import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * Tests the functionality of the Forced UP handshake responder.
 * modeled after the UltrapeerHandshakeResponder test
 */
public class ForcedUltrapeerHandshakeResponderTest extends BaseTestCase {
	public ForcedUltrapeerHandshakeResponderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ForcedUltrapeerHandshakeResponderTest.class);
	}
	
	/**
     * Always assume we're encoding & accept it.  This simplifies the testing.
     * For further tests on whether how handshaking works in response to these
     * settings, see HandshakeResponseTest.
     */    
    public void setUp() {
        ConnectionSettings.ACCEPT_DEFLATE.setValue(true);
        ConnectionSettings.ENCODE_DEFLATE.setValue(true);
    }
    
    /**
     * tests establishing an outgoing connection as a leaf.
     * although the node is a leaf, we should still mark the 
     * connection as an ultrapeer.
     */
    public void testOutgoingFromLeaf() throws Exception {
    	
    	ForcedUltrapeerHandshakeResponder responder = new 
			ForcedUltrapeerHandshakeResponder("1.2.3.4");
    	
    	//1.  Test when we receive leaf guidance.
    	Properties props = new UltrapeerHeaders("40.0.9.8");
    	props.setProperty(HeaderNames.X_ULTRAPEER_NEEDED, "False");
    	
    	HandshakeResponse headers = HandshakeResponse.createResponse(props);
    	
    	HandshakeResponse hr = 
            responder.respondUnauthenticated(headers, true);
    	
    	//should not be accepted
    	assertFalse(hr.isAccepted());
    	
    	//2.  Test when we don't receive leaf guidance, but the other side
    	//starts claiming they are a leaf
    	props = new LeafHeaders("40.0.9.8");
    	headers = HandshakeResponse.createResponse(props);
    	hr = responder.respondUnauthenticated(headers, true);
    	
    	assertFalse(hr.isAccepted());
    	
    	
    	//3. Test a proper connection that fits the required criteria
    	props = new UltrapeerHeaders("40.0.9.8");
    	props.setProperty(HeaderNames.X_ULTRAPEER_NEEDED, "True");
    	headers = HandshakeResponse.createResponse(props);
    	hr = responder.respondUnauthenticated(headers, true);
    	
    	assertTrue(hr.isAccepted());
    }
    
    public void testIncomingToUP() throws Exception {
    	ForcedUltrapeerHandshakeResponder responder = new 
			ForcedUltrapeerHandshakeResponder("1.2.3.4");
    	
    	//1. test when we receive a connection which says its a leaf.
    	Properties props = new LeafHeaders("2.3.4.5");
    	
    	HandshakeResponse headers = HandshakeResponse.createResponse(props);
    	
    	HandshakeResponse hr = 
            responder.respond(headers, false);
    	
    	assertFalse(hr.isAccepted());
    	
    	//2. test when we receive a crawler incoming connection
    	props = new UltrapeerHeaders("2.3.4.5");
    	props.setProperty(HeaderNames.CRAWLER,"0.1");
    	headers = HandshakeResponse.createResponse(props);
    	assertTrue(headers.isCrawler());
    	try{
    		hr = responder.respondUnauthenticated(headers, false);
    		fail("iox expected");
    	}catch(IOException ignored){}

    	
    	
    	//3. test when everything is ok and the connection is accepted
    	props = new UltrapeerHeaders("40.0.9.8");
    	props.setProperty(HeaderNames.CONTENT_ENCODING,HeaderNames.DEFLATE_VALUE);
    	headers = HandshakeResponse.createResponse(props);
    	hr = responder.respondUnauthenticated(headers, false);
    	
    	assertTrue(hr.isAccepted());
    	assertTrue(hr.isDeflateAccepted());
    	String myIp=NetworkUtils.ip2string(RouterService.getAddress())+":"
			+ RouterService.getPort();
    	//assertEquals(myIp,hr.getListenIP());
    	assertEquals(myIp,hr.getProperty(HeaderNames.LISTEN_IP));
    }
}
