package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import java.util.Properties;
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import java.lang.reflect.*;
import junit.framework.*;


/**
 * Tests the functionality of the <tt>HandshakeResponse</tt> class.
 */
public final class HandshakeResponseTest extends BaseTestCase {


	public HandshakeResponseTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(HandshakeResponseTest.class);
	}

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Tests the method that add Ultrapeer hosts to the X-Try-Ultrapeer
     * header.
     */
    public void testAddConnectedUltrapeers() throws Exception {

        // run a test with just a few Ultrapeers to make sure
        // they're reported in the headers correctly
        Endpoint[] hosts0 = {
            new Endpoint("165.23.8.0", 6346),
            new Endpoint("165.23.8.1", 6346),
            new Endpoint("165.23.8.2", 6346)        
        };

        runConnectedTestOnHosts(hosts0);

        // run a test with more than 10 Ultrapeers to make sure we 
        // cap how many we send
        Endpoint[] hosts1 = {
            new Endpoint("163.23.8.0", 6346),
            new Endpoint("163.23.8.1", 6346),
            new Endpoint("163.23.8.2", 6346),       
            new Endpoint("163.23.8.3", 6346),
            new Endpoint("163.23.8.4", 6346),
            new Endpoint("163.23.8.5", 6346),
            new Endpoint("163.23.8.6", 6346),       
            new Endpoint("163.23.8.7", 6346),
            new Endpoint("163.23.8.8", 6346),       
            new Endpoint("163.23.8.9", 6346),
            new Endpoint("163.23.8.10", 6346),       
            new Endpoint("163.23.8.11", 6346),
            new Endpoint("163.23.8.12", 6346),       
            new Endpoint("163.23.8.13", 6346),       
            new Endpoint("163.23.8.14", 6346),
            new Endpoint("163.23.8.15", 6346),       
        };

        runConnectedTestOnHosts(hosts1);
    }


    /**
     * Tests the method that add Ultrapeer hosts to the X-Try-Ultrapeer
     * header.
     */
    public void testAddHighHopsUltrapeers() throws Exception {

        // run a test with just a few Ultrapeers to make sure
        // they're reported in the headers correctly
        Endpoint[] hosts0 = {
            new Endpoint("165.23.8.0", 6346),
            new Endpoint("165.23.8.1", 6346),
            new Endpoint("165.23.8.2", 6346)        
        };

        runHighHopsTestOnHosts(hosts0);

        // run a test with more than 10 Ultrapeers to make sure we 
        // cap how many we send
        Endpoint[] hosts1 = {
            new Endpoint("163.23.8.0", 6346),
            new Endpoint("163.23.8.1", 6346),
            new Endpoint("163.23.8.2", 6346),       
            new Endpoint("163.23.8.3", 6346),
            new Endpoint("163.23.8.4", 6346),
            new Endpoint("163.23.8.5", 6346),
            new Endpoint("163.23.8.6", 6346),       
            new Endpoint("163.23.8.7", 6346),
            new Endpoint("163.23.8.8", 6346),       
            new Endpoint("163.23.8.9", 6346),
            new Endpoint("163.23.8.10", 6346),       
            new Endpoint("163.23.8.11", 6346),
            new Endpoint("163.23.8.12", 6346),       
            new Endpoint("163.23.8.13", 6346),       
            new Endpoint("163.23.8.14", 6346),
            new Endpoint("163.23.8.15", 6346),       
        };

        runHighHopsTestOnHosts(hosts1);
    }

    /**
     * Helper method that runs the specified array of Ultrapeer hosts
     * through the connection method that turns them into the X-Try-Ultrapeer
     * header.
     */
    private void runConnectedTestOnHosts(Endpoint[] hosts) throws Exception {
        ConnectionManager cm = new ConnectionManagerStub(hosts);
        
        Properties headers = new Properties();

		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "addConnectedUltrapeers",
                                         new Class[]{ConnectionManager.class, 
                                                     Properties.class});
        
        m.invoke(null, new Object[]{cm, headers});
        makeSureHeadersMatch(hosts, headers);
    }

    /**
     * Helper method that runs the specified array of Ultrapeer hosts
     * through the connection method that turns them into the X-Try-Ultrapeer
     * header.
     */
    private void runHighHopsTestOnHosts(Endpoint[] hosts) throws Exception {
        HostCatcher hc = new HostCatcherStub(hosts);
        
        Properties headers = new Properties();

		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "addHighHopsUltrapeers",
                                         new Class[]{HostCatcher.class, 
                                                     Properties.class});
        
        m.invoke(null, new Object[]{hc, headers});
        makeSureHeadersMatch(hosts, headers);
    }

    /**
     * Helper method to make sure that the X-Try-Ultrapeers header in the
     * given set of headers matches the set of hosts in the given array
     * of hosts.
     */
    private void makeSureHeadersMatch(Endpoint[] hosts, Properties headers) {
        String headerHosts = 
            headers.getProperty(HeaderNames.X_TRY_ULTRAPEERS);

        assertNotNull("should have X-Try-Ultrapeer header", headerHosts);
        StringTokenizer st = new StringTokenizer(headerHosts, ",");
        assertEquals("unexpected number of hosts", 
                     Math.min(hosts.length, 10), st.countTokens());
        
        int i = 0;
        while(st.hasMoreTokens()) {
            String curHost = st.nextToken();            
            boolean match = false;
            for(int j=0; j<hosts.length; j++) {
                Endpoint ep = hosts[j];
                if(curHost.equals(ep.getHostname()+":"+ep.getPort())) {
                    match = true;
                    break;
                }
            }
            assertTrue("should have been a matching endpoint to: "+curHost, match);
            i++;
        }
    }

    /**
     * Helper class that overrides the method for getting connected Ultrapeers
     * for testing purposes.
     */
    private static class ConnectionManagerStub extends ConnectionManager {
        private final Endpoint[] HOSTS;

        ConnectionManagerStub(Endpoint[] hosts) {
            super(null);
            HOSTS = hosts;
        }

        // overridden to return custom endpoints for testing
        public Set getSupernodeEndpoints() {
            Set ultrapeers = new HashSet();
            for(int i=0; i<HOSTS.length; i++) {
                ultrapeers.add(HOSTS[i]);
            }
            return ultrapeers;
        }
    }

    /**
     * Helper class that overrides the method for obtaining Ultrapeer hosts
     * for the host catcher -- used to create customized tests.
     */
    private static class HostCatcherStub extends HostCatcher {
        
        private final Endpoint[] HOSTS;

        HostCatcherStub(Endpoint[] hosts) {
            HOSTS = hosts;
        }

        // overridden to return custom endpoints for testing
        public Iterator getUltrapeerHosts(int n) {
            Set ultrapeers = new HashSet();
            int limit = Math.min(HOSTS.length, n);
            for(int i=0; i<limit; i++) {
                ultrapeers.add(HOSTS[i]);
            }
            return ultrapeers.iterator();            
        }
    }
}
