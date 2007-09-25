package com.limegroup.gnutella;

import java.net.Socket;

import junit.framework.Test;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

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
    
    @SuppressWarnings("unused")
    private static void doSettings() {
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(true);
    }        

    ///////////////////////// Actual Tests ////////////////////////////
    
    public void testStillFirewalledAfterLocalConnect() throws Exception {
        assertFalse(ProviderHacks.getNetworkManager().acceptedIncomingConnection());

        Socket incoming = new Socket("localhost", SERVER_PORT);
        incoming.close();

        assertFalse(ProviderHacks.getNetworkManager().acceptedIncomingConnection());
    }


    //////////////////////////////////////////////////////////////////

    public static Integer numUPs() {
        return new Integer(1);
    }

    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }



}

