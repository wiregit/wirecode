package com.limegroup.gnutella;

import java.net.Socket;

import junit.framework.Test;

import com.google.inject.Injector;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class FirewalledStatusTest extends ClientSideTestCase {

    private Injector injector;
    private NetworkManager networkManager;
    
    public FirewalledStatusTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FirewalledStatusTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void setUp() throws Exception {
        injector = LimeTestUtils.createInjector();
        super.setUp(injector);
        networkManager = injector.getInstance(NetworkManager.class);
    }
    
    @Override
    public void setSettings() {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(true);
    }        

    ///////////////////////// Actual Tests ////////////////////////////
    
    public void testStillFirewalledAfterLocalConnect() throws Exception {

        assertFalse(networkManager.acceptedIncomingConnection());

        Socket incoming = new Socket("localhost", SERVER_PORT);
        incoming.close();

        assertFalse(networkManager.acceptedIncomingConnection());
    }


    //////////////////////////////////////////////////////////////////

    @Override
    public int getNumberOfPeers() {
        return 1;
    }
}

