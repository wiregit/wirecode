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

    public void testSupportsProbeQueries() throws Exception {
        Properties props = new Properties();
        HandshakeResponse hr = HandshakeResponse.createResponse(props);
        assertTrue("should not support probes", !hr.supportsProbeQueries());
        props.put(HeaderNames.X_PROBE_QUERIES, "0.0");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should not support probes", !hr.supportsProbeQueries());

        props.put(HeaderNames.X_PROBE_QUERIES, "0.1");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should support probes", hr.supportsProbeQueries());

        props.put(HeaderNames.X_PROBE_QUERIES, "0.2");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should support probes", hr.supportsProbeQueries());
    }
    
    /**
     * Tests the method for extracting the status message from a connection
     * handshake status line.
     */
    public void testExtractMessage() throws Exception {
		Method codeMethod = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "extractCode",
                                         new Class[]{String.class});

		Method messageMethod = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "extractMessage",
                                         new Class[]{String.class});
        String line = "200 OK";
        int code = ((Integer)codeMethod.invoke(null, new Object[]{line})).intValue();
        String message = (String)messageMethod.invoke(null, new Object[]{line});
        assertEquals("unexpected code", 200, code);
        assertEquals("unexpected message", "OK", message);

        line = "503 Service Unavailable";
        code = ((Integer)codeMethod.invoke(null, new Object[]{line})).intValue();
        assertEquals("unexpected code", 503, code);
        message = (String)messageMethod.invoke(null, new Object[]{line});
        assertEquals("unexpected message", "Service Unavailable", message);

        line = "503 Something Totally Different";
        code = ((Integer)codeMethod.invoke(null, new Object[]{line})).intValue();
        assertEquals("unexpected code", 503, code);
        message = (String)messageMethod.invoke(null, new Object[]{line});
        assertEquals("unexpected message", "Something Totally Different", message);

        line = "200 Something Totally Different";
        code = ((Integer)codeMethod.invoke(null, new Object[]{line})).intValue();
        assertEquals("unexpected code", 200, code);
        message = (String)messageMethod.invoke(null, new Object[]{line});
        assertEquals("unexpected message", "Something Totally Different", message);
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

    /**
     * Tests the various factory constructors.
     */
    public void testFactoryConstructors() {
        Properties props = new Properties();
        HandshakeResponse hr = 
            HandshakeResponse.createAcceptOutgoingResponse(props);
        assertTrue("should be accepted", hr.isAccepted());
        assertEquals("should not have any properties", 0, hr.props().size());
    }

    /**
     * Test to make sure that Ultrapeer headers are created correctly.
     */
    public void testUltrapeerHeaders() {
        HandshakeResponse hr = 
            HandshakeResponse.createRejectIncomingResponse(new UltrapeerHeaders("32.9.8.9"));
        runUltrapeerHeadersTest(hr);

        hr = HandshakeResponse.createAcceptIncomingResponse(new UltrapeerHeaders("32.9.8.9"));
        runUltrapeerHeadersTest(hr);

        hr = HandshakeResponse.createAcceptOutgoingResponse(new UltrapeerHeaders("32.9.8.9"));
        hr = HandshakeResponse.createRejectOutgoingResponse(new UltrapeerHeaders("32.9.8.9"));
        runUltrapeerHeadersTest(hr);
    }

    
    /**
     * Test to make sure that leaf headers are created correctly.
     */
    public void testLeafHeaders() {
        HandshakeResponse hr = 
            HandshakeResponse.createRejectIncomingResponse(new LeafHeaders("32.9.8.9"));
        runLeafHeadersTest(hr);

        hr = HandshakeResponse.createAcceptIncomingResponse(new LeafHeaders("32.9.8.9"));
        runLeafHeadersTest(hr);

        hr = HandshakeResponse.createRejectOutgoingResponse(new LeafHeaders("32.9.8.9"));
        runLeafHeadersTest(hr);

        hr = HandshakeResponse.createAcceptOutgoingResponse(new LeafHeaders("32.9.8.9"));
        runLeafHeadersTest(hr);
    }

    /**
     * Tests that the give <tt>HandshakeResponse</tt> has all of the headers
     * that it should as a leaf.
     *
     * @param hr the headers to test
     */
    private static void runLeafHeadersTest(HandshakeResponse hr) {
        runCommonHeadersTest(hr);
        assertTrue("should not be an Ultrapeer connection", 
                   !hr.isUltrapeer());
        assertTrue("should be a leaf connection", hr.isLeaf());
        assertTrue("should not be a GUESS Ultrapeer", !hr.isGUESSUltrapeer()); 
    }

    /**
     * Tests that the give <tt>HandshakeResponse</tt> has all of the headers
     * that it should as an Ultrapeer.
     *
     * @param hr the headers to test
     */
    private static void runUltrapeerHeadersTest(HandshakeResponse hr) {
        runCommonHeadersTest(hr);
        assertTrue("should be an Ultrapeer connection", hr.isUltrapeer());
        assertTrue("should not be a leaf connection", !hr.isLeaf());
        assertTrue("should be a GUESS Ultrapeer", hr.isGUESSUltrapeer());        
    }
    
    /**
     * Tests to make sure that all of the common headers are present, with the
     * expected values.  This tests headers that are sent by both leaves and
     * Ultrapeers.  If new common headers are added, they should be added to 
     * this list.
     *
     * @param hr the <tt>HandshakeResponse</tt> containing the headers to
     *  check
     */
    private static void runCommonHeadersTest(HandshakeResponse hr) {
        assertTrue("query routing should be enabled", hr.isQueryRoutingEnabled());
        assertTrue("Ultrapeer query routing should be enabled",
                   hr.isUltrapeerQueryRoutingConnection());
        assertEquals("unexpected user agent", 
                     CommonUtils.getHttpServer(), hr.getUserAgent());

        assertTrue("should be a high-degree connection", hr.isHighDegreeConnection());
        assertEquals("unexpected max ttl", 4, hr.getMaxTTL());
        assertEquals("unexpected degree", 15, hr.getNumIntraUltrapeerConnections());
        assertTrue("should be GUESS capable", hr.isGUESSCapable());
        assertTrue("should support GGEP", hr.supportsGGEP());
        assertTrue("should support vendor messages", hr.supportsVendorMessages());
        assertTrue("should use dynamic querying", hr.isDynamicQueryConnection());
    }

    /**
     * Tests to make sure that the ultrapeer needed header is interpretted
     * correctly.
     */
    public void testLeafGuidance() {
        Properties headers = new Properties();
        headers.put(HeaderNames.X_ULTRAPEER_NEEDED, "true");
        HandshakeResponse hr = 
            HandshakeResponse.createAcceptIncomingResponse(headers);
        assertTrue("should not include leaf guidance", !hr.hasLeafGuidance());

        headers.put(HeaderNames.X_ULTRAPEER_NEEDED, "false");
        hr = HandshakeResponse.createAcceptIncomingResponse(headers);
        assertTrue("should include leaf guidance", hr.hasLeafGuidance());

        hr = HandshakeResponse.createAcceptIncomingResponse(new Properties());
        assertTrue("should not include leaf guidance", !hr.hasLeafGuidance());
    }

    /**
     * Tests the header utility method that checks if a the version for a 
     * given header is greater than or equal to specific values.
     */
    public void testIsVersionOrHigher() throws Exception {
        Properties headers = new Properties();
        headers.put(HeaderNames.X_ULTRAPEER_QUERY_ROUTING, "0.1");

		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "isVersionOrHigher",
                                         new Class[]{Properties.class,
                                                     String.class, 
                                                     Float.TYPE});

        Object[] params = new Object[3];
        params[0] = headers;
        params[1] = HeaderNames.X_ULTRAPEER_QUERY_ROUTING;

        
        params[2] = new Float(0.1);
        Boolean correctVersion = (Boolean)m.invoke(null, params); 
        assertTrue("should have been the correct version", 
                   correctVersion.booleanValue());

        params[2] = new Float(0.2);
        correctVersion = (Boolean)m.invoke(null, params); 
        assertFalse("should not have been the correct version", 
                    correctVersion.booleanValue());

        params[2] = new Float(0.3);
        correctVersion = (Boolean)m.invoke(null, params); 
        assertFalse("should not have been the correct version", 
                    correctVersion.booleanValue());
    }


    /**
     * Test to make sure that the X-Max-TTL header is being created and
     * passed correctly.
     */
    public void testXMaxTTL() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "extractByteHeaderValue",
                                         new Class[]{Properties.class,
                                                     String.class, 
                                                     Byte.TYPE}); 

        Properties headers = new UltrapeerHeaders("3.7.6.8");
        Object[] params = new Object[3];
        params[0] = headers;
        params[1] = HeaderNames.X_MAX_TTL;         
        params[2] = new Byte((byte)5);
        
        Byte ttl = (Byte)m.invoke(null, params);

        assertEquals("should have X-Max-TTL: 4", (byte)4, ttl.byteValue());
    }

    /**
     * Test to make sure that the extractByteHeaderValue method is 
     * working correctly.
     */
    public void testExtractByteHeaderValue() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "extractByteHeaderValue",
                                         new Class[]{Properties.class,
                                                     String.class, 
                                                     Byte.TYPE}); 

        Properties headers = new UltrapeerHeaders("3.7.6.8");
        Object[] params = new Object[3];
        params[0] = headers;
        params[1] = HeaderNames.X_MAX_TTL; 
        params[2] = new Byte((byte)5);
        
        Byte ttl = (Byte)m.invoke(null, params);

        // should use contain 4 from the UltrapeerHeaders
        assertEquals("should have X-Max-TTL: 4", 
                     4, ttl.byteValue());

        params[0] = new Properties();
        ttl = (Byte)m.invoke(null, params);

        // should use the default value of 5, since the header is not
        // present
        assertEquals("should have X-Max-TTL: 5", 
                     ((Byte)params[2]).byteValue(), ttl.byteValue());

        ((Properties)params[0]).put(HeaderNames.X_MAX_TTL, "fjajfl");

        ttl = (Byte)m.invoke(null, params);

        // should use the default value of 5, since the header is 
        // gibberish
        assertEquals("should have X-Max-TTL: 5", 
                     ((Byte)params[2]).byteValue(), ttl.byteValue());
    }


    /**
     * Test to make sure that the extractIntHeaderValue method is 
     * working correctly.
     */
    public void testExtractIntHeaderValue() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "extractIntHeaderValue",
                                         new Class[]{Properties.class,
                                                     String.class, 
                                                     Integer.TYPE}); 

        Properties headers = new UltrapeerHeaders("3.7.6.8");

        // construct the parameters to pass to the method
        Object[] params = new Object[3];
        params[0] = headers;
        params[1] = HeaderNames.X_DEGREE; 
        params[2] = new Integer(20);
        
        Integer degree = (Integer)m.invoke(null, params);

        // should use the default degree from the UltrapeerHeaders
        assertEquals("should have different X-Degree", 
                     15, degree.intValue());

        params[0] = new Properties();
        degree = (Integer)m.invoke(null, params);

        // should use the default value of 20, since the header is not
        // present
        assertEquals("should have different X-Degree", 
                     ((Integer)params[2]).intValue(), degree.intValue());

        ((Properties)params[0]).put(HeaderNames.X_DEGREE, "fjajfl");

        degree = (Integer)m.invoke(null, params);

        // should use the default value of 5, since the header is 
        // gibberish
        assertEquals("should have different X-Degree", 
                     ((Integer)params[2]).intValue(), degree.intValue());
    }


    /**
     * Test to make sure that the extractStringHeaderValue method is 
     * working correctly.
     */
    public void testExtractStringHeaderValue() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "extractStringHeaderValue",
                                         new Class[]{Properties.class,
                                                     String.class});

        Properties headers = new Properties();

        String host = "54.78.54.8:6346";
        headers.put(HeaderNames.X_TRY_ULTRAPEERS, host);
        
        // construct the parameters to pass to the method
        Object[] params = new Object[2];
        params[0] = headers;
        params[1] = HeaderNames.X_TRY_ULTRAPEERS; 
        
        String hosts = (String)m.invoke(null, params);

        
        assertEquals("should have different X-Try-Ultrapeer", 
                     host, hosts);

        params[0] = new Properties();
        hosts = (String)m.invoke(null, params);

        // should use the default value of "", since the header is not
        // present
        assertEquals("should have different X-Try-Ultrapeer", 
                     "", hosts);
    }


    /**
     * Tests the headerExists method.
     */
    public void testHeaderExists() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "headerExists",
                                         new Class[]{Properties.class,
                                                     String.class});

        Properties headers = new Properties();

        String host = "54.78.54.8:6346";
        headers.put(HeaderNames.X_TRY_ULTRAPEERS, host);
        
        // construct the parameters to pass to the method
        Object[] params = new Object[2];
        params[0] = headers;
        params[1] = HeaderNames.X_TRY_ULTRAPEERS; 
        
        Boolean exists = (Boolean)m.invoke(null, params);
        assertTrue("header should exist", exists.booleanValue());

        // this header should not be there, so make sure it isn't!
        params[1] = HeaderNames.X_ULTRAPEER_QUERY_ROUTING;

        exists = (Boolean)m.invoke(null, params);
        assertTrue("header should not exists", !exists.booleanValue());        
    }

    /**
     * Tests the isFalseValue method to make sure it's working correctly
     */
    public void testIsFalseValue() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "isFalseValue",
                                         new Class[]{Properties.class,
                                                     String.class});

        Properties headers = new Properties();

        // construct the parameters to pass to the method
        Object[] params = new Object[2];
        params[0] = headers;
        params[1] = HeaderNames.X_ULTRAPEER; 

        // this header should not exist, and so should not be false
        Boolean isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should not be false", !isFalse.booleanValue());


        // this should be false
        headers.put(HeaderNames.X_ULTRAPEER, "false");
        isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should be false", isFalse.booleanValue());


        // this should be false
        headers.put(HeaderNames.X_ULTRAPEER, "False");
        isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should be false", isFalse.booleanValue());


        // this should not be false
        headers.put(HeaderNames.X_ULTRAPEER, "FIPEUI");
        isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should not be false", !isFalse.booleanValue());
        
        // this should not be false
        headers.put(HeaderNames.X_ULTRAPEER, "true");
        isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should not be false", !isFalse.booleanValue());        

        // this should not be false
        headers.put(HeaderNames.X_ULTRAPEER, "True");
        isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should not be false", !isFalse.booleanValue());        

        // this should not be false
        headers.put(HeaderNames.X_ULTRAPEER, "falsee");
        isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should not be false", !isFalse.booleanValue()); 

        // this should not be false
        headers.put(HeaderNames.X_ULTRAPEER, "");
        isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should not be false", !isFalse.booleanValue()); 
    }

    /**
     * Tests the isTrueValue method to make sure it's working correctly
     */
    public void testIsTrueValue() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "isTrueValue",
                                         new Class[]{Properties.class,
                                                     String.class});

        Properties headers = new Properties();

        // construct the parameters to pass to the method
        Object[] params = new Object[2];
        params[0] = headers;
        params[1] = HeaderNames.X_ULTRAPEER; 

        // this header should not exist, and so should not be true
        Boolean isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should not be true", !isTrue.booleanValue());


        // this should be true
        headers.put(HeaderNames.X_ULTRAPEER, "true");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should be true", isTrue.booleanValue());


        // this should be true
        headers.put(HeaderNames.X_ULTRAPEER, "True");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should be true", isTrue.booleanValue());


        // this should not be true
        headers.put(HeaderNames.X_ULTRAPEER, "FIPEUI");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should not be true", !isTrue.booleanValue());
        
        // this should not be true
        headers.put(HeaderNames.X_ULTRAPEER, "false");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should not be true", !isTrue.booleanValue());        

        // this should not be true
        headers.put(HeaderNames.X_ULTRAPEER, "False");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should not be true", !isTrue.booleanValue());        

        // this should not be true
        headers.put(HeaderNames.X_ULTRAPEER, "truee");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should not be true", !isTrue.booleanValue()); 

        // this should not be true
        headers.put(HeaderNames.X_ULTRAPEER, "");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should not be true", !isTrue.booleanValue()); 
    }
}
