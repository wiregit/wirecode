package com.limegroup.gnutella.bootstrap;

import java.text.ParseException;

import junit.framework.Test;

/**
 * Unit tests for BootstrapServerTest.
 */
public class BootstrapServerTest extends com.limegroup.gnutella.util.LimeTestCase {
    public BootstrapServerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BootstrapServerTest.class);
    }

    public void testConstructorSimple() throws Exception {        
        String s1="HTTP://server.com/dir/script.php";
        BootstrapServer e1=new BootstrapServer(s1);
        assertEquals(s1.toLowerCase(), e1.getURLString());
    }

    public void testConstructorExtended() throws Exception {
        String s1="http://server.com/dir/script.php";
        BootstrapServer e2=new BootstrapServer(s1+",2343,1232;233,2343;3434");
        assertEquals(s1, e2.getURLString());
    }

    public void testConstructorFailure() {
        try {
            new BootstrapServer("server.com/dir/script.php");
            fail("shouldn't have created bootstrap server");
        } catch (ParseException pass) { }
    }

    public void testConstructorFailure2() {
        try {
            new BootstrapServer("http  server.com/   /script.php");
            fail("shouldn't have created bootstrap server");
        } catch (ParseException pass) { }
    }

    public void testToString() {
        try {
            String s1="http://server.com/dir/script.php";
            BootstrapServer e1=new BootstrapServer(s1);
            assertEquals(s1, e1.toString());
        } catch (ParseException pass) { }
    }

    public void testEquals() {
        try {
            BootstrapServer e1=new BootstrapServer("http://server.com/dir/script.php");
            BootstrapServer e2=new BootstrapServer("http://server.com/dir/script.php");
            assertEquals(e1, e2);
            assertEquals(e2, e1);
        } catch (ParseException pass) { }
    }
}
