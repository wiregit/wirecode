package com.limegroup.gnutella.connection;

import junit.framework.Test;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Tests the class that checks whether or not the user has a live internet 
 * connection.
 */
public class ConnectionCheckerTest extends LimeTestCase {

    private final static TestManager MANAGER = new TestManager();
    
    private static final Object LOCK = new Object();

    /**
     * Creates a new test instance.
     */
    public ConnectionCheckerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ConnectionCheckerTest.class);
    }
    
    public static void globalSetUp() throws Exception {
      //  new RouterService(new ActivityCallbackStub());
        PrivilegedAccessor.setValue(RouterService.class, "manager", MANAGER);
    }
    
    /**
     * Tests to make sure that the method for checking for a live internet
     * connection is working properly.
     * 
     * @throws Exception in any unexpected error occurs
     */
    public void testForLiveConnection() throws Exception {

        // We should quickly connect to one of our hosts.
        ConnectionChecker checker = ConnectionChecker.checkForLiveConnection();
        Thread.sleep(10000);
        assertTrue("should have successfully connected", 
            checker.hasConnected());
        
        // Now, we "pretend" we're disconnected by just trying to connect to
        // hosts that don't exist, which is effectively the same as not 
        // being connected.
        String[] dummyHosts = {
            "http://www.dummyhostsjoafds.com",
            "http://www.dummyhostsjoafdser.com",
            "http://www.dumfadfostsjoafds.com",
            "http://www.dummyhostsjafds.com",
            "http://www.dummyhostjoafdser.com",
            "http://www.dumfatsjoafds.com",
        };

        PrivilegedAccessor.setValue(ConnectionChecker.class, 
            "STANDARD_HOSTS", dummyHosts);    
        checker = ConnectionChecker.checkForLiveConnection();
        synchronized(LOCK) {
            LOCK.wait(10000);
        }
        //Thread.sleep(2000);
        assertTrue("should not have successfully connected", 
            !checker.hasConnected());   
        assertTrue("should have received callback", 
            MANAGER.hasReceivedCallback());     
    }

    /**
     * Helper class that receives the callback notifying us when there's no
     * available internet connection.
     */
    private static class TestManager extends ConnectionManager {

        private boolean _receivedCallback;

        public TestManager() {
            super(ProviderHacks.getNetworkManager());
        }
        
        public void noInternetConnection() {
            _receivedCallback = true;
            synchronized(LOCK) {
                LOCK.notify();
            }
        }
        
        /**
         * Determines whether or not we have received the callback notifying us
         * that there's no live internet connection.
         * 
         * @return <tt>true</tt> if we've received the callback, otherwise
         *  <tt>false</tt>
         */
        public boolean hasReceivedCallback() {
            return _receivedCallback;
        }
    }
}
