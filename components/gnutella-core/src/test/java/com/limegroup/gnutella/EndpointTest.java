package com.limegroup.gnutella;

import junit.framework.*;

public class EndpointTest extends TestCase {
    public EndpointTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(EndpointTest.class);
    }

    public void testLegacy() {
        Endpoint e;
        try {
            e=new Endpoint(":6347");
            assertTrue(false);
        } catch (IllegalArgumentException exc) {
            assertTrue(true);
        }
        try {
            e=new Endpoint("abc:cas");
            assertTrue(false);
        } catch (IllegalArgumentException exc) {
            assertTrue(true);
        }
        try {
            e=new Endpoint("abc");
            assertTrue(e.getHostname().equals("abc"));
            assertTrue(e.getPort()==6346);
        } catch (IllegalArgumentException exc) {
            assertTrue(false);
        }
        try {
            e=new Endpoint("abc:");
            assertTrue(e.getHostname().equals("abc"));
            assertTrue(e.getPort()==6346);
        } catch (IllegalArgumentException exc) {
            assertTrue(false);
        }
        try {
            e=new Endpoint("abc:7");
            assertTrue(e.getHostname().equals("abc"));
            assertTrue(e.getPort()==7);
        } catch (IllegalArgumentException exc) {
            assertTrue(false);
        }

        ////////////////////////// Private IP and Subnet Tests ////////////////
        //These tests are incomplete since the methods are somewhat trivial.
        e=new Endpoint("18.239.0.1",0);
        assertTrue(! e.isPrivateAddress());
        e=new Endpoint("10.0.0.0",0);
        assertTrue(e.isPrivateAddress());
        e=new Endpoint("10.255.255.255",0);
        assertTrue(e.isPrivateAddress());
        e=new Endpoint("11.0.0.0",0);
        assertTrue(! e.isPrivateAddress());
        e=new Endpoint("172.16.0.0",0);
        assertTrue(e.isPrivateAddress());
        e=new Endpoint("0.0.0.0");
        assertTrue(e.isPrivateAddress());

        Endpoint e1;
        Endpoint e2;
        e1=new Endpoint("172.16.0.0",0);    e2=new Endpoint("172.16.0.1",0);
        assertTrue(e1.isSameSubnet(e2));
        assertTrue(e2.isSameSubnet(e1));
        e2=new Endpoint("18.239.0.1",0);
        assertTrue(! e2.isSameSubnet(e1));
        assertTrue(! e1.isSameSubnet(e2));

        e1=new Endpoint("192.168.0.1",0);    e2=new Endpoint("192.168.0.2",0);
        assertTrue(e1.isSameSubnet(e2));
        assertTrue(e2.isSameSubnet(e1));
        e2=new Endpoint("192.168.1.1",0);
        assertTrue(! e2.isSameSubnet(e1));
        assertTrue(! e1.isSameSubnet(e2));
    }
        
    public void testValidatedNumeric() {
        //Allowed 
        check("www.limewire.org:6346", false, true); //no verification
        check("not a url:6346", false, true);
        check("10.255.25.0:6346", true, true);       //verified valid IP   
        check("64.61.25.172", true, true);     

        //Disallowed
        check("<html>hello</html>", true, false);    //not even close!
        check("18.239.0.144.215", true, false);      //too many dots
        check("18.A.0.144", true, false);            //not numeric
        check("18.256.0.144:6346", true, false);     //value too big
        check("18.10.-1.144:6346", true, false);     //value too small
        check("127.0.0.1:ABC", false, false);        //old rules still apply
    }

    /** Checks that an exception is thrown iff allowed==false. */
    private static void check(String hostAndPort, 
                              boolean requireNumeric, 
                              boolean allowed) {
        try {
            Endpoint e=new Endpoint(hostAndPort, requireNumeric);
            assertTrue("Allowed: \""+hostAndPort+"\"", allowed);
        } catch (IllegalArgumentException e) {
            assertTrue("Disallowed: \""+hostAndPort+"\"", !allowed);
        }
    }
}
