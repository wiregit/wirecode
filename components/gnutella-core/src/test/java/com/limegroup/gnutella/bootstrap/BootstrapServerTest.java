package com.limegroup.gnutella.bootstrap;

import junit.framework.*;
import com.sun.java.util.collections.*;
import java.net.*;
import java.text.ParseException;

/**
 * Unit tests for BootstrapServerTest.
 */
public class BootstrapServerTest extends TestCase {
    public BootstrapServerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(BootstrapServerTest.class);
    }

    public void testConstructorSimple() {        
        try {
            String s1="HTTP://server.com/dir/script.php";
            BootstrapServer e1=new BootstrapServer(s1);
            assertEquals(s1, e1.getURL().toString());
        } catch (ParseException e) {
            fail("Unexpected exception");
        }
    }

    public void testConstructorExtended() {
        try {
            String s1="http://server.com/dir/script.php";
            BootstrapServer e2=new BootstrapServer(s1+",2343,1232;233,2343;3434");
            assertEquals(s1, e2.getURL().toString());
        } catch (ParseException e) {
            fail("Unexpected exception");
        }
    }

    public void testConstructorFailure() {
        try {
            new BootstrapServer("server.com/dir/script.php");
            fail("No exception");
        } catch (ParseException pass) { }
    }

    public void testConstructorFailure2() {
        try {
            new BootstrapServer("http  server.com/   /script.php");
            fail("No exception");
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
            assertTrue(e1.equals(e2));
            assertTrue(e2.equals(e1));
        } catch (ParseException pass) { }
    }
}
