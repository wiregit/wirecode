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
     * Tests the method for responding to outgoing connection attempts.
     * The response we send is the final response of the handshake --
     * the third header exchange overall.
     */
    public void testRespondToOutgoing() throws Exception {
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);

        UltrapeerHandshakeResponder responder = 
            new UltrapeerHandshakeResponder("23.3.4.5");

        Properties props = new Properties();
        props.put(HeaderNames.X_ULTRAPEER_NEEDED, "true");
        HandshakeResponse headers = new HandshakeResponse(props);
        
        HandshakeResponse hr = 
            responder.respondUnauthenticated(headers, true);

        // we shouldn't send any response header in this case -- it's
        // just assumed that we're becoming an Ultrapeer
        assertTrue("should be becoming an ultrapeer", !hr.isUltrapeer());


        props = new Properties();
        props.put(HeaderNames.X_ULTRAPEER_NEEDED, "false");
        headers = new HandshakeResponse(props);        
        hr = responder.respondUnauthenticated(headers, true);
        assertTrue("should not be an Ultrapeer", !hr.isUltrapeer());
        assertTrue("should be becoming an leaf", hr.isLeaf());

    }

}
