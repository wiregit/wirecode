package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.*;
import com.bitzi.util.*;

import junit.framework.*;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class FirewalledStatusTest extends ClientSideTestCase {

    public FirewalledStatusTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FirewalledStatusTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static void doSettings() {
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(true);
    }        

    ///////////////////////// Actual Tests ////////////////////////////
    
    public void testStillFirewalledAfterLocalConnect() throws Exception {
        assertFalse(rs.acceptedIncomingConnection());

        Socket incoming = new Socket("localhost", SERVER_PORT);
        incoming.close();

        assertFalse(rs.acceptedIncomingConnection());
    }


    //////////////////////////////////////////////////////////////////

    public static Integer numUPs() {
        return new Integer(1);
    }

    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }



}

