package com.limegroup.gnutella.handshaking;

import java.util.Properties;

import junit.framework.Test;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.util.BaseTestCase;


/**
 * Tests the functionality of the <tt>LeafHandshakeResponderTest</tt> 
 * class.<p>
 *
 * For the Ultrapeer specifications, see:<p>
 *
 * http://groups.yahoo.com/group/the_gdf/files/Proposals/Ultrapeer/Ultrapeers_1.0_clean.html
 */
public final class LeafHandshakeResponderTest extends BaseTestCase {


	public LeafHandshakeResponderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(LeafHandshakeResponderTest.class);
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
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);

        // test the 3 Ultrapeer cases -- 

        // create the Ultrapeer responder to test off of
        LeafHandshakeResponder responder = 
            new LeafHandshakeResponder("23.3.4.5");

        // 1) Ultrapeer-Leaf::No X-Ultrapeer-Needed -- we should ignore 
        //    the ultrapeer needed, and it should never be sent, but
        //    test it anyway
        Properties props = new UltrapeerHeaders("40.0.9.8");
        HandshakeResponse headers = HandshakeResponse.createResponse(props);
        
        HandshakeResponse hr = responder.respond(headers, true);

        // we shouldn't send any response header in this case
        // we should not accept any incoming connections
        assertTrue("should have been accepted", hr.isAccepted());
        assertEquals("should only have deflate header", 1, hr.props().size());
        assertTrue("should be deflated", hr.isDeflateEnabled());


        // 2) Ultrapeer-Leaf::X-Ultrapeer-Needed: true
        props = new UltrapeerHeaders("40.0.9.8");

        // this should be redundant, but make sure it's handled the way
        // we want
        props.put(HeaderNames.X_ULTRAPEER_NEEDED, "true");
        headers = HandshakeResponse.createResponse(props);
        
        hr = responder.respond(headers, true);

        // we shouldn't send any response header in this case -- it's
        // just assumed that we're becoming an Ultrapeer
        assertTrue("should have been accepted", hr.isAccepted());
        assertEquals("should only have deflate header", 1, hr.props().size());
        assertTrue("should be deflated", hr.isDeflateEnabled());

        // 3) Ultrapeer-Leaf::X-Ultrapeer-Needed: false
        props = new UltrapeerHeaders("78.9.3.0");
        props.put(HeaderNames.X_ULTRAPEER_NEEDED, "false");
        
        headers = HandshakeResponse.createResponse(props);        
        hr = responder.respond(headers, true);
        assertTrue("should have been accepted", hr.isAccepted());
        assertEquals("should only have deflate header", 1, hr.props().size());
        assertTrue("should be deflated", hr.isDeflateEnabled());
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(false);
    }

    /**
     * Tests to make sure that outgoing connection responses are handled
     * correctly when the host we're responding to is a leaf.  Leaves 
     * should never connect directly to other leaves, so all of these
     * connection attempts should fail.
     */
    public void testRespondToOutgoingLeaf() throws Exception {
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);

        LeafHandshakeResponder responder = 
            new LeafHandshakeResponder("23.3.4.5");

        // Leaf-Leaf  --> never allowed, regardless of slots
        UltrapeerSettings.MAX_LEAVES.setValue(1);
        Properties props = new LeafHeaders("78.9.3.0");
        HandshakeResponse headers = HandshakeResponse.createResponse(props);  
        HandshakeResponse hr = responder.respond(headers, true);

        assertTrue("should not accept connections to other leaves: "+
                   hr.getStatusLine(), 
                   !hr.isAccepted());
        assertEquals("should not have any headers", 0, hr.props().size());

        // Leaf-Leaf --> never allowed, but check anyway with no slots
        UltrapeerSettings.MAX_LEAVES.setValue(0);
        hr = responder.respond(headers, true);
        assertTrue("should not have accepted the connection", 
                   !hr.isAccepted());
        assertEquals("should not have any headers", 0, hr.props().size());

        // clean up settings
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(false);
    }


    /**
     * Tests the method for responding to incoming connection attempts.
     */
    public void testRespondToIncomingUltrapeer() throws Exception {
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(true);
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);

        LeafHandshakeResponder responder = 
            new LeafHandshakeResponder("23.3.4.5");

        // 1) check the Ultrapeer case.  Here, the "leaf" does
        //    not have any leaf connections yet, so it should just
        //    accept the incoming connection
        HandshakeResponse up = 
            HandshakeResponse.createResponse(new UltrapeerHeaders("80.45.0.1"));
        
        HandshakeResponse hr = responder.respond(up, false);
        assertTrue("should accept incoming connections as a leaf -- "+
                   "this could change in the future!",
                   hr.isAccepted());
        assertTrue("should be deflated", hr.isDeflateEnabled());
    }

    /**
     * Test to make sure that incoming leaf connections are handled correctly.
     */
    public void testRespondToIncomingLeaf() throws Exception {
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(true);
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);        
        // the ultrapeer we'll be testing against
        LeafHandshakeResponder responder = 
            new LeafHandshakeResponder("23.3.4.5");


        //  1) check to make sure that leaves are properly accepted as
        //     leaves
        HandshakeResponse leaf = 
            HandshakeResponse.createResponse(new LeafHeaders("80.45.0.1"));
        HandshakeResponse hr = responder.respond(leaf, false);
        
        assertFalse("should never accept leaves if we're a leaf.", hr.isAccepted());
    }

}
