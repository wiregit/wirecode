package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.*;
import java.util.Properties;
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import java.lang.reflect.*;
import junit.framework.*;


/**
 * Tests the functionality of the <tt>UltrapeerHandshakeResponderTest</tt> 
 * class.<p>
 *
 * For the Ultrapeer specifications, see:<p>
 *
 * http://groups.yahoo.com/group/the_gdf/files/Proposals/Ultrapeer/Ultrapeers_1.0_clean.html
 */
public final class UltrapeerHandshakeResponderTest extends BaseTestCase {


	public UltrapeerHandshakeResponderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UltrapeerHandshakeResponderTest.class);
	}

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
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
     * Tests the method for responding to outgoing connection attempts.
     * The response we send is the final response of the handshake --
     * the third header exchange overall.
     */
    public void testRespondToOutgoingUltrapeer() throws Exception {
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
        ConnectionSettings.IGNORE_KEEP_ALIVE.setValue(true);

        // test the 3 Ultrapeer cases -- 

        // create the Ultrapeer responder to test off of
        UltrapeerHandshakeResponder responder = 
            new UltrapeerHandshakeResponder("23.3.4.5");

        // 1) Ultrapeer-Ultrapeer::No X-Ultrapeer-Needed
        Properties props = new UltrapeerHeaders("40.0.9.8");
        HandshakeResponse headers = HandshakeResponse.createResponse(props);
        
        HandshakeResponse hr = 
            responder.respondUnauthenticated(headers, true);

        // we shouldn't send any response header in this case -- it's
        // just assumed that we're becoming an Ultrapeer
        assertTrue("should be accepted", hr.isAccepted());
        assertEquals("should only have deflate header", 1, hr.props().size());
        assertTrue("should be deflated", hr.isDeflateEnabled());


        // 2) Ultrapeer-Ultrapeer::X-Ultrapeer-Needed: true
        props = new UltrapeerHeaders("40.0.9.8");

        // this should be redundant, but make sure it's handled the way
        // we want
        props.put(HeaderNames.X_ULTRAPEER_NEEDED, "true");
        headers = HandshakeResponse.createResponse(props);
        
        hr = responder.respondUnauthenticated(headers, true);

        // we shouldn't send any response header in this case -- it's
        // just assumed that we're becoming an Ultrapeer
        assertTrue("should be accepted", hr.isAccepted());
        assertEquals("should only have deflate header", 1, hr.props().size());
        assertTrue("should be deflated", hr.isDeflateEnabled());

        // 3) Ultrapeer-Ultrapeer::X-Ultrapeer-Needed: false
        props = new UltrapeerHeaders("78.9.3.0");
        props.put(HeaderNames.X_ULTRAPEER_NEEDED, "false");
        
        headers = HandshakeResponse.createResponse(props);        
        hr = responder.respondUnauthenticated(headers, true);
        assertTrue("should not be an Ultrapeer", !hr.isUltrapeer());
        assertTrue("should be becoming an leaf", hr.isLeaf());
        assertTrue("should be accepted", hr.isAccepted());
        assertEquals("should have two headers", 2, hr.props().size());
        assertTrue("should be deflating",
                hr.isDeflateEnabled());
        ConnectionSettings.IGNORE_KEEP_ALIVE.setValue(false);
    }

    /**
     * Tests to make sure that outgoing connection responses are handled
     * correctly when the host we're responding to is a leaf.
     */
    public void testRespondToOutgoingLeaf() throws Exception {
        ConnectionSettings.IGNORE_KEEP_ALIVE.setValue(true);

        UltrapeerHandshakeResponder responder = 
            new UltrapeerHandshakeResponder("23.3.4.5");

        // Leaf-Ultrapeer  --> leaf slots available
        Properties props = new LeafHeaders("78.9.3.0");
        HandshakeResponse headers = HandshakeResponse.createResponse(props);  
        HandshakeResponse hr = responder.respondUnauthenticated(headers, true);

        assertTrue("should have returned that we accepted the connection", 
                   hr.isAccepted());
        assertEquals("should have 3 headers", 3, hr.props().size());
        assertTrue("should deflate to leaf (may change)",
                    hr.isDeflateEnabled());
        assertTrue("should have client guid.", hr.desiresProxyServices());
        assertTrue("should be push proxy.", hr.isPushProxy());
        
        UltrapeerSettings.MAX_LEAVES.setValue(0);
        hr = responder.respondUnauthenticated(headers, true);
        assertTrue("should not have accepted the connection", 
                   !hr.isAccepted());
        assertEquals("should not have any headers", 0, hr.props().size());

        // clean up settings
        ConnectionSettings.IGNORE_KEEP_ALIVE.setValue(false);
    }


    /**
     * Tests the method for responding to incoming connection attempts.
     */
    public void testRespondToIncomingUltrapeer() throws Exception {
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(true);
        ConnectionSettings.IGNORE_KEEP_ALIVE.setValue(true);

        UltrapeerHandshakeResponder responder = 
            new UltrapeerHandshakeResponder("23.3.4.5");

        // 1) check the Ultrapeer case -- leaf guidance should be used
        //    here because the Ultrapeer definitely does not have the
        //    maximum number of leaves
        HandshakeResponse up = 
            HandshakeResponse.createResponse(new UltrapeerHeaders("80.45.0.1"));
        
        HandshakeResponse hr = 
            responder.respondUnauthenticated(up, false);

        assertTrue("should report Ultrapeer true", hr.isUltrapeer());
        assertTrue("should tell the connecting Ultrapeer to become a leaf", 
                   hr.hasLeafGuidance());
        assertTrue("should be deflated (may change)", hr.isDeflateEnabled());

        //  2) check to make sure that Ultrapeers are accepted as 
        //     Ultrapeer connections when we have enough leaves -- create this
        //     artifially by setting the MAX_LEAVES to zero
        ConnectionSettings.IGNORE_KEEP_ALIVE.setValue(true);
        UltrapeerSettings.MAX_LEAVES.setValue(0);        
        hr = responder.respondUnauthenticated(up, false);        
        assertTrue("should tell the Ultrapeer to stay an Ultrapeer", 
                   !hr.hasLeafGuidance());
        assertTrue("should still be accepted as an Ultrapeer",
                   hr.isAccepted());
        assertTrue("should be deflating to leaf", hr.isDeflateEnabled());
        UltrapeerSettings.MAX_LEAVES.revertToDefault();
        ConnectionSettings.IGNORE_KEEP_ALIVE.setValue(false);
    }

    /**
     * Test to make sure that incoming leaf connections are handled correctly.
     */
    public void testRespondToIncomingLeaf() throws Exception {
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(true);
        ConnectionSettings.IGNORE_KEEP_ALIVE.setValue(true);        
        // the ultrapeer we'll be testing against
        UltrapeerHandshakeResponder responder = 
            new UltrapeerHandshakeResponder("23.3.4.5");


        //  1) check to make sure that leaves are properly accepted as
        //     leaves
        HandshakeResponse leaf = 
            HandshakeResponse.createResponse(new LeafHeaders("80.45.0.1"));
        HandshakeResponse hr = responder.respondUnauthenticated(leaf, false);
        
        assertTrue("should report Ultrapeer true", hr.isUltrapeer());
        assertTrue("should tell the leaf to be a leaf", hr.hasLeafGuidance());
        assertTrue("should be high degree connection", 
                   hr.isHighDegreeConnection());
        assertTrue("should be an Ultrapeer query routing connection", 
                   hr.isUltrapeerQueryRoutingConnection());
        assertTrue("should be deflating to leaf", hr.isDeflateEnabled());
        assertTrue("should have client guid.", hr.desiresProxyServices());
        assertTrue("should be push proxy.", hr.isPushProxy());
        

        //  2) check to make sure that leaves are rejected with X-Try-Ultrapeer
        //     headers when we alread have enough leaves -- create this 
        //     situation artificially by setting the MAX_LEAVES to zero
        UltrapeerSettings.MAX_LEAVES.setValue(0);        
        hr = responder.respondUnauthenticated(leaf, false);        
        assertTrue("should have rejected the leaf: status code was: "+
                   hr.getStatusLine(), 
                   !hr.isAccepted());

        assertTrue("should have X-Try-Ultrapeer hosts", hr.hasXTryUltrapeers());
        UltrapeerSettings.MAX_LEAVES.revertToDefault();        
        ConnectionSettings.IGNORE_KEEP_ALIVE.setValue(false);
    }

}
